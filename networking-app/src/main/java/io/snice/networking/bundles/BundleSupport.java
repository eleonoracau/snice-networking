package io.snice.networking.bundles;

import com.fasterxml.jackson.databind.Module;
import io.hektor.fsm.Data;
import io.snice.networking.app.Environment;
import io.snice.networking.app.NetworkAppConfig;
import io.snice.networking.app.NetworkStack;
import io.snice.networking.app.impl.DefaultEnvironment;
import io.snice.networking.common.Connection;
import io.snice.networking.common.fsm.FsmFactory;
import io.snice.networking.common.fsm.NetworkContext;

import java.util.Optional;

public abstract class BundleSupport<K extends Connection<T>, T> implements ProtocolBundle<K, T> {

    private final Class<T> type;

    public BundleSupport(final Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public Optional<Module> getObjectMapModule() {
        return Optional.empty();
    }

    @Override
    public K wrapConnection(final Connection<T> connection) {
        return (K) connection;
    }

    @Override
    public <C extends NetworkAppConfig> Environment<K, T, C> createEnvironment(final NetworkStack<K, T, C> stack, final C configuration) {
        return new DefaultEnvironment<>(stack, configuration);
    }

    @Override
    public <S extends Enum<S>, C extends NetworkContext<T>, D extends Data> Optional<FsmFactory<T, S, C, D>> getFsmFactory() {
        return Optional.empty();
    }
}