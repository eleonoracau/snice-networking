package io.snice.networking.diameter.peer;

import io.snice.codecs.codec.diameter.DiameterMessage;
import io.snice.networking.app.NetworkStack;
import io.snice.networking.common.fsm.FsmFactory;
import io.snice.networking.diameter.DiameterAppConfig;
import io.snice.networking.diameter.DiameterConfig;
import io.snice.networking.diameter.Peer;
import io.snice.networking.diameter.peer.impl.DefaultPeerTable;

import java.util.concurrent.CompletionStage;

import static io.snice.preconditions.PreConditions.ensureNotNull;

public interface PeerTable<C extends DiameterAppConfig> extends FsmFactory<DiameterMessage, PeerState, PeerContext, PeerData> {

    /**
     * Create a new {@link PeerTable} based on the {@link DiameterConfig} settings.
     */
    static PeerTable create(final DiameterConfig conf) {
        ensureNotNull(conf, "The configuration object cannot be null");
        return new DefaultPeerTable(conf);
    }

    void start(final NetworkStack<Peer, DiameterMessage, C> stack);

    /**
     * Add a {@link Peer} to the {@link PeerTable} based on the given
     * configuration. If the configuration indicates that {@link Peer}
     * is active and the diameter stack is already running, the stack will make
     * and attempt to actually establish the underlying protocol session as well.
     * <p>
     * If the peer is marked as passive or the diameter stack is currently not running,
     * the peer will just be added to the internal table.
     *
     * @param config
     * @return a configured {@link Peer}
     */
    CompletionStage<Peer> addPeer(PeerConfiguration config);

    /**
     * Remove the {@link Peer} from this {@link PeerTable}. If the {@link Peer} has an active
     * connection with its remote party, that connection will be shut down. If the {@link Peer}
     * has any outstanding transactions, they may be allowed to finish first
     * before shutting the peer down completely depending on whether the 'now' flag is set or not
     *
     * @param peer the actual peer to remove and shut down.
     * @param now  if true the {@link Peer} will forcibly be shutdown irrespective if the {@link Peer} has
     *             any outstanding transactions or not. If false, it is a "nice" shutdown that will allow
     *             the peer to cleanup before going down.
     * @return once completed, the {@link CompletionStage} will contain a reference to the {@link Peer} that
     * was just shutdown, which will be the same peer as passed into the method (note, may not be the same reference
     * so do not not not rely on that).
     */
    CompletionStage<Peer> removePeer(Peer peer, boolean now);
}
