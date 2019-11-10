package io.snice.networking.common.event;

import io.snice.networking.common.ChannelContext;
import io.snice.networking.common.Connection;

/**
 * @author jonas@jonasborjesson.com
 */
public interface IOEvent<T> {

    /**
     * The {@link Connection} over which this event took place.
     *
     * @return
     */
    Connection<T> connection();

    ChannelContext<T> channelContext();

    /**
     * The time at which this event took place. If the event came off of the network
     * then this is the time at which that event had been read off of the socket.
     *
     * @return
     */
    long arrivalTime();

    /**
     * Events concerning the state of a connection will be delivered via ConnectionIOEvents.
     * Check if this IOEvent is a connection event.
     *
     * @return
     */
    default boolean isConnectionIOEvent() {
        return false;
    }

    default ConnectionIOEvent toConnectionIOEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + ConnectionIOEvent.class.getName());
    }

    default boolean isConnectionOpenedIOEvent() {
        return false;
    }

    default boolean isConnectionClosedIOEvent() {
        return false;
    }

    default boolean isConnectionCloseIOEvent() {
        return false;
    }

    default boolean isConnectionActiveIOEvent() {
        return false;
    }

    default boolean isConnectionInactiveIOEvent() {
        return false;
    }

    default boolean isConnectionBoundIOEvent() {
        return false;
    }

    default boolean isTimeoutEvent() {
        return false;
    }

    default boolean isMessageIOEvent() {
        return false;
    }

    default MessageIOEvent<T> toMessageIOEvent() {
        throw new ClassCastException("Cannot cast " + getClass().getName() + " into a " + MessageIOEvent.class.getName());
    }

}