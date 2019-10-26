package io.pkts.diameter;

import io.snice.buffer.ReadableBuffer;
import io.pkts.diameter.avp.FramedAvp;
import io.pkts.diameter.avp.OriginHost;
import io.pkts.diameter.avp.OriginRealm;
import io.pkts.diameter.impl.DiameterParser;

import java.io.IOException;
import java.util.List;

/**
 * @author jonas@jonasborjesson.com
 */
public interface DiameterMessage extends Cloneable {

    DiameterHeader getHeader();

    List<FramedAvp> getAllAvps();

    DiameterMessage clone();

    /**
     * The {@link OriginHost} MUST be present in all diameter messages.
     */
    OriginHost getOriginHost();

    /**
     * The {@link OriginRealm} MUST be present in all diameter messages.
     */
    OriginRealm getOriginRealm();

    static DiameterMessage frame(final ReadableBuffer buffer) throws IOException {
        return DiameterParser.frame(buffer);
    }
}
