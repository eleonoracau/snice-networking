package io.snice.networking.app.impl;

import io.snice.buffer.Buffer;
import io.snice.networking.app.ConnectionContext;
import io.snice.networking.codec.Framer;
import io.snice.networking.common.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ConnectionAdapter<C extends Connection, T, R> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAdapter.class);

    private final C connection;
    private final ConnectionContext<C, T, R> ctx;
    private final Framer<T> framer;

    public ConnectionAdapter(final C connection, Framer<T> framer, final ConnectionContext<C, T, R> ctx) {
        this.connection = connection;
        this.framer = framer;
        this.ctx = ctx;
    }

    public Optional<T> frame(final Buffer data) {
        return framer.frame(data);
    }

    public void process(final T data) {
        ctx.match(connection, data).apply(connection, data);
    }
}
