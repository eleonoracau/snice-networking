package io.snice.networking.app.impl;

import io.netty.channel.ChannelHandler;
import io.snice.networking.app.ConnectionContext;
import io.snice.networking.app.NetworkAppConfig;
import io.snice.networking.app.NetworkApplication;
import io.snice.networking.app.NetworkStack;
import io.snice.networking.codec.FramerFactory;
import io.snice.networking.common.Transport;
import io.snice.networking.netty.NettyNetworkLayer;
import io.snice.time.Clock;
import io.snice.time.SystemClock;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertArray;
import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.ensureNotNull;

@ChannelHandler.Sharable
public class NettyNetworkStack<T, C extends NetworkAppConfig> implements NetworkStack<T, C> {

    private final Class<T> type;
    private final FramerFactory<T> framerFactory;
    private final C config;
    private final NetworkApplication<T, C> app;
    private final List<ConnectionContext> ctxs;
    private NettyNetworkLayer network;
    private final Clock clock = new SystemClock();

    private NettyNetworkStack(final Class<T> type, final C config, final FramerFactory<T> framerFactory, final NetworkApplication<T, C> app, final List<ConnectionContext> ctxs) {
        this.type = type;
        this.framerFactory = framerFactory;
        this.config = config;
        this.app = app;
        this.ctxs = ctxs;
    }

    public static <T> FramerFactoryStep<T> ofType(final Class<T> type) {
        assertNotNull(type, "The type cannot be null");
        return framerFactory -> {
            assertNotNull(framerFactory, "The Framer Factory cannot be null");
            return new ConfigurationStep<T>() {
                @Override
                public <C extends NetworkAppConfig> Builder<T, C> withConfiguration(C config) {
                    assertNotNull(config, "The configuration for the network stack cannot be null");
                    return new Builder(type, framerFactory, config);
                }
            };

        };

    }

    @Override
    public void start() {
        network = NettyNetworkLayer.with(config.getNetworkInterfaces())
                .withHandler("udp-adapter", () -> new NettyUdpInboundAdapter(clock, framerFactory, Optional.empty(), ctxs), Transport.udp)
                .withHandler("tcp-adapter", () -> new NettyTcpInboundAdapter(clock, Optional.empty(), ctxs), Transport.tcp)
                .build();
        network.start();
    }

    @Override
    public CompletionStage<Void> sync() {
        return network.sync();
    }

    @Override
    public void stop() {
        network.stop();
    }

    private static class Builder<T, C extends NetworkAppConfig> implements NetworkStack.Builder<T, C> {

        private final Class<T> type;
        private final FramerFactory<T> framerFactory;
        private final C config;
        private NetworkApplication application;
        private List<ConnectionContext> ctxs;

        private Builder(final Class<T> type, final FramerFactory<T> framerFactory, final C config) {
            this.type = type;
            this.framerFactory = framerFactory;
            this.config = config;
        }

        @Override
        public NetworkStack.Builder<T, C> withApplication(final NetworkApplication<T, C> application) {
            assertNotNull(application, "The application cannot be null");
            this.application = application;
            return this;
        }


        @Override
        public NetworkStack.Builder<T, C> withConnectionContexts(final List<ConnectionContext> ctxs) {
            assertArgument(ctxs != null && !ctxs.isEmpty(), "You cannot have a null or empty list of " + ConnectionContext.class.getSimpleName());
            this.ctxs = ctxs;
            return this;
        }

        @Override
        public NetworkStack<T, C> build() {
            ensureNotNull(application, "You must specify the Sip Application");
            return new NettyNetworkStack(type, config, framerFactory, application, ctxs);
        }
    }
}
