/**
 * 
 */
package io.snice.networking.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.snice.networking.common.Connection;
import io.snice.networking.common.Transport;
import io.snice.networking.config.NetworkInterfaceConfiguration;
import io.snice.networking.core.ListeningPoint;
import io.snice.networking.core.NetworkInterface;
import io.snice.networking.core.NetworkLayer;
import io.snice.time.Clock;
import io.snice.time.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static io.snice.preconditions.PreConditions.ensureNotEmpty;
import static io.snice.preconditions.PreConditions.ensureNotNull;

/**
 * The {@link NettyNetworkLayer} is the glue between the network (netty) and
 * the rest of the SIP stack and eventually the users own io.sipstack.application.application.
 * 
 * The purpose of this {@link NettyNetworkLayer} is simply to read/write from/to
 * the channels and then dispatch the result of those operations to
 * the actual SIP Stack.
 * 
 * @author jonas@jonasborjesson.com
 */
public class NettyNetworkLayer<T> implements NetworkLayer<T> {

    private static final Logger logger = LoggerFactory.getLogger(NettyNetworkLayer.class);

    private final CompletableFuture<Void> shutdownStage = new CompletableFuture<>();

    /**
     * Every {@link ListeningPoint} has a reference to the very same {@link CountDownLatch}
     * and each of those {@link ListeningPoint}s will call {@link CountDownLatch#countDown()}
     * when they are finished shutting down the socket again. Hence, we can use this
     * to hang on the latch until everything is shut down again.
     */
    private final CountDownLatch latch;

    private final List<NettyNetworkInterface> interfaces;

    private final NettyNetworkInterface defaultInterface;

    /**
     * 
     */
    private NettyNetworkLayer(final CountDownLatch latch, final List<NettyNetworkInterface> ifs) {
        this.latch = latch;
        this.interfaces = ifs;

        // TODO: make this configurable. For now, it is simply the
        // first one...
        this.defaultInterface = ifs.get(0);
    }

    @Override
    public void start() {
        final List<CompletionStage<Void>> futures = new CopyOnWriteArrayList<>();
        this.interfaces.forEach(i -> futures.add(i.up()));
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Override
    public void stop() {
        try {
            latch.await();
            shutdownStage.complete(null);
        } catch (final InterruptedException e) {
            shutdownStage.completeExceptionally(e);
        }
    }

    @Override
    public CompletionStage<Connection<T>> connect(final Transport transport, final InetSocketAddress address) {
        return this.defaultInterface.connect(transport, address);
    }

    @Override
    public CompletionStage<Void> sync() {
        return shutdownStage;
    }

    @Override
    public NetworkInterface getDefaultNetworkInterface() {
        return this.defaultInterface;
    }

    @Override
    public Optional<? extends NetworkInterface> getNetworkInterface(final String name) {
        return interfaces.stream()
                .filter(i -> i.getName().equals(name))
                .findFirst();
    }

    @Override
    public List<? extends NetworkInterface> getNetworkInterfaces() {
        return interfaces;
    }

    @Override
    public Optional<ListeningPoint> getListeningPoint(final Transport transport) {
        return Optional.ofNullable(defaultInterface.getListeningPoint(transport));
    }

    @Override
    public Optional<ListeningPoint> getListeningPoint(final String networkInterfaceName, final Transport transport) {
        return interfaces.stream()
                .filter(i -> i.getName().equals(networkInterfaceName))
                .findFirst()
                .map(i -> i.getListeningPoint(transport));
    }

    public static Builder with(final List<NetworkInterfaceConfiguration> ifs) throws IllegalArgumentException {
        return new Builder(ensureNotNull(ifs));
    }

    public static Builder with(final NetworkInterfaceConfiguration i) throws IllegalArgumentException {
        return new Builder(List.of(ensureNotNull(i)));
    }

    private static class Handler {
        private final Supplier<ChannelHandler> handler;
        private final String name;

        /**
         * The transports to which this handler is supposed to be installed.
         */
        private final List<Transport> transports;

        private Handler(final String name, final ChannelHandler handler, final Transport... transports) {
            this(name, () -> handler, transports);
        }

        private Handler(final String name, final Supplier<ChannelHandler> handler, final Transport... transports) {
            this.name = name;
            this.handler = handler;
            this.transports = Arrays.asList(transports);
        }

        public String getName() {
            return name;
        }

        public ChannelHandler getHandler() {
            return handler.get();
        }

        public boolean hasTransport(final Transport transport) {
            return transports.contains(transport);
        }
    }

    public static class Builder {

        private final List<NetworkInterfaceConfiguration> ifs;

        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;
        private EventLoopGroup udpGroup;
        private Clock clock;

        /**
         * The TCP based bootstrap.
         */
        private ServerBootstrap serverBootstrap;

        /**
         * Our UDP based bootstrap.
         */
        private Bootstrap bootstrap;

        private final Channel udpListeningPoint = null;

        private final List<Handler> handlers = new ArrayList<>();
        // private final List<ChannelHandler> handlers = new ArrayList<>();
        // private final List<String> handlerNames = new ArrayList<>();

        private Builder(final List<NetworkInterfaceConfiguration> ifs) {
            this.ifs = ifs;
        }

        public Builder withHandler(final String handlerName, final ChannelHandler handler) {
            return withHandler(handlerName, handler, Transport.values());
        }

        public Builder withHandler(final String handlerName, final ChannelHandler handler, final Transport... transports) {
            ensureNotEmpty(handlerName, "The name of the handler cannot be null or the empty string");
            ensureNotNull(handler, "The handler cannot be null");
            ensureNotNull(transports, "You must specify at least one transport that this handler will be installed");
            handlers.add(new Handler(handlerName, handler, transports));
            return this;
        }

        public Builder withHandler(final String handlerName, final Supplier<ChannelHandler> handler, final Transport... transports) {
            ensureNotEmpty(handlerName, "The name of the handler cannot be null or the empty string");
            ensureNotNull(handler, "The handler cannot be null");
            ensureNotNull(transports, "You must specify at least one transport that this handler will be installed");
            handlers.add(new Handler(handlerName, handler, transports));
            return this;
        }

        public Builder withClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withBossEventLoopGroup(final EventLoopGroup group) {
            this.bossGroup = group;
            return this;
        }

        public Builder withTCPEventLoopGroup(final EventLoopGroup group) {
            this.workerGroup = group;
            return this;
        }

        public Builder withUDPEventLoopGroup(final EventLoopGroup group) {
            this.udpGroup = group;
            return this;
        }

        public NettyNetworkLayer build() {

            // TODO: check that if you e.g. specify dialog layer then you must also specify transaction layer

            if (bossGroup == null) {
                bossGroup = new NioEventLoopGroup();
            }

            if (workerGroup == null && udpGroup == null) {
                workerGroup = new NioEventLoopGroup();
                udpGroup = workerGroup;
            } else if (workerGroup != null && udpGroup == null) {
                udpGroup = workerGroup;
            } else if (udpGroup != null && workerGroup != null) {
                workerGroup = udpGroup;
            }

            final Clock clock = this.clock != null ? this.clock : new SystemClock();

            final List<NettyNetworkInterface.Builder> builders = new ArrayList<>();
            if (this.ifs.isEmpty()) {
                final Inet4Address address = findPrimaryAddress();
                final String ip = address.getHostAddress();
                throw new IllegalArgumentException("Sorry, but I haven't finished implementing this so you have to " +
                        "specify at least one listening point in your configuration");
            } else {
                this.ifs.forEach(i -> {
                    final NettyNetworkInterface.Builder ifBuilder = NettyNetworkInterface.with(i);
                    ifBuilder.udpBootstrap(ensureUDPBootstrap());
                    ifBuilder.tcpBootstrap(ensureTCPBootstrap());
                    ifBuilder.tcpServerBootstrap(ensureTCPServerBootstrap(clock, i.getVipAddress()));
                    builders.add(ifBuilder);
                });
            }

            final CountDownLatch latch = new CountDownLatch(builders.size());
            final List<NettyNetworkInterface> ifs = new ArrayList<>();
            builders.forEach(ifBuilder -> ifs.add(ifBuilder.latch(latch).build()));
            return new NettyNetworkLayer(latch, Collections.unmodifiableList(ifs));
        }

        private Bootstrap ensureUDPBootstrap() {
            // TODO: this won't be correct when we listen to multiple ports
            // and they may have different vip addresses etc. we'll deal with that
            // later...
            if (this.bootstrap == null) {
                final Bootstrap b = new Bootstrap();
                b.group(this.udpGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(final DatagramChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        handlers.stream()
                                .filter(h -> h.hasTransport(Transport.udp))
                                .forEach(h -> pipeline.addLast(h.getName(), h.getHandler()));
                    }
                });

                // this allows you to setup connections from the
                // same listening point
                // .option(ChannelOption.SO_REUSEADDR, true);

                this.bootstrap = b;
            }
            return this.bootstrap;
        }

        private Bootstrap ensureTCPBootstrap() {
            final Bootstrap tcpBootstrap = new Bootstrap();
            tcpBootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(final SocketChannel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            handlers.stream()
                                    .filter(h -> h.hasTransport(Transport.tcp))
                                    .forEach(h -> pipeline.addLast(h.getName(), h.getHandler()));
                        }
                    });
            return tcpBootstrap;
        }

        private ServerBootstrap ensureTCPServerBootstrap(final Clock clock, final URI vipAddress) {
            if (this.serverBootstrap == null) {
                final ServerBootstrap b = new ServerBootstrap();

                b.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        handlers.stream()
                                .filter(h -> h.hasTransport(Transport.tcp))
                                .forEach(h -> pipeline.addLast(h.getName(), h.getHandler()));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
                // TODO: should make all the above TCP options configurable
                this.serverBootstrap = b;
            }
            return this.serverBootstrap;
        }

        private static Inet4Address findPrimaryAddress() {
            java.net.NetworkInterface loopback = null;
            java.net.NetworkInterface primary = null;
            try {
                final Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    final java.net.NetworkInterface i = interfaces.nextElement();
                    if (i.isLoopback()) {
                        loopback = i;
                    } else if (i.isUp() && !i.isVirtual()) {
                        primary = i;
                        break;
                    }
                }
            } catch (final SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            final java.net.NetworkInterface network = primary != null ? primary : loopback;
            final Inet4Address address = getInet4Address(network);
            return address;
        }

        private static Inet4Address getInet4Address(final java.net.NetworkInterface network) {
            final Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                final InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    return (Inet4Address) address;
                }
            }
            return null;
        }
    }

}
