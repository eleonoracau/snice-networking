package io.snice.networking.event;


import io.snice.networking.common.Connection;
import io.snice.networking.event.impl.IOEventImpl;

/**
 * @author jonas@jonasborjesson.com
 */
public interface ConnectionBoundIOEvent extends ConnectionIOEvent {
    @Override
    default boolean isConnectionBoundIOEvent() {
        return true;
    }

    static ConnectionBoundIOEvent create(final Connection connection, final long arrivalTime) {
        return new ConnectionBoundIOEventImpl(connection, arrivalTime);
    }

    class ConnectionBoundIOEventImpl extends IOEventImpl implements ConnectionBoundIOEvent {
        private ConnectionBoundIOEventImpl(final Connection connection, final long arrivalTime) {
            super(connection, arrivalTime);
        }
    }

}
