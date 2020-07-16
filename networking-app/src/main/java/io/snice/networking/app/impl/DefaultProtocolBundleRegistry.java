package io.snice.networking.app.impl;

import io.snice.networking.bundles.ProtocolBundle;
import io.snice.networking.bundles.ProtocolBundleRegistry;
import io.snice.networking.bundles.StringBundle;
import io.snice.networking.common.Connection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.preconditions.PreConditions.checkIfNotEmpty;

public class DefaultProtocolBundleRegistry implements ProtocolBundleRegistry {

    private final Map<Class, ProtocolBundle<? extends Connection<?>, ?>> bundles = new HashMap<>();

    // not sure this is doable actually.
    private final Map<String, ProtocolBundle<? extends Connection<?>, ?>> schemeBundles = new HashMap<>();

    private static final ProtocolBundleRegistry DEFAULT_REGISTRY = new DefaultProtocolBundleRegistry();

    static {
        DEFAULT_REGISTRY.registerBundle(new StringBundle(), String.class, "str");
    }

    public static ProtocolBundleRegistry getDefaultRegistry() {
        return DEFAULT_REGISTRY;
    }

    @Override
    public <K extends Connection<T>, T> void registerBundle(final ProtocolBundle<K, T> bundle, final Class klass) {
        registerBundle(bundle, klass, null);
    }

    @Override
    public <K extends Connection<T>, T> void registerBundle(final ProtocolBundle<K, T> bundle, final Class klass, final String scheme) {
        assertNotNull(bundle, "The bundle cannot be null");
        assertNotNull(klass, "The Class cannot be null");
        bundles.put(klass, bundle);
        if (checkIfNotEmpty(scheme)) {
            schemeBundles.put(scheme.toLowerCase(), bundle);
        }
    }

    @Override
    public <K extends Connection<T>, T> Optional<ProtocolBundle<K, T>> find(final Class<T> type) {
        assertNotNull(type, "The type cannot be null");
        final ProtocolBundle<K, T> bundle = (ProtocolBundle<K, T>) bundles.get(type);
        return Optional.ofNullable(bundle);
    }

    /*
    @Override
    public <K extends Connection<T>, T> Optional<AppBundle<K, T>> find(final URI uri) throws IllegalArgumentException {
        final String scheme = uri == null ? null : uri.getScheme();
        assertNotEmpty(scheme, "The URI Scheme, or the URI itself, is null or the empty String");
        final AppBundle<K, T> bundle = (AppBundle<K, T>) schemeBundles.get(scheme);
        return Optional.ofNullable(bundle);
    }
     */
}