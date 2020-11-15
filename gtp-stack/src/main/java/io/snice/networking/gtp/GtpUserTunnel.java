package io.snice.networking.gtp;

import io.snice.buffer.Buffer;
import io.snice.codecs.codec.gtp.gtpc.v2.tliv.Paa;
import io.snice.networking.common.Connection;
import io.snice.networking.gtp.event.GtpEvent;
import io.snice.networking.gtp.impl.DefaultGtpUserTunnel;

public interface GtpUserTunnel {

    static GtpUserTunnel of(final Connection<GtpEvent> connection, final Paa paa, final Bearer localBearer, final Bearer remoteBearer) {
        return DefaultGtpUserTunnel.of(connection, paa, localBearer, remoteBearer);
    }


    /**
     * Send the given message across this tunnel.
     * This will be send as a UDP packet to the given remote host:port.
     *
     * @param remoteIp
     * @param remotePort
     * @param msg
     */
    void send(String remoteIp, int remotePort, String msg);

    void send(String remoteIp, int remotePort, Buffer data);

    String getIPv4Address();

    Bearer getDefaultLocalBearer();

    Bearer getDefaultRemoteBearer();
}
