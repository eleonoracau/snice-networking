package io.snice.networking.codec.diameter.impl;

import io.snice.buffer.Buffer;
import io.snice.buffer.ReadableBuffer;
import io.snice.networking.codec.diameter.AvpHeader;
import io.snice.networking.codec.diameter.DiameterHeader;
import io.snice.networking.codec.diameter.DiameterMessage;
import io.snice.networking.codec.diameter.DiameterParseException;

import java.io.IOException;

/**
 * @author jonas@jonasborjesson.com
 */
public class DiameterParser {

    public static DiameterMessage frame(final Buffer buffer) throws DiameterParseException, IOException {

        return null;
    }

    public static DiameterHeader frameHeader(final Buffer buffer) throws DiameterParseException, IOException {
        if (!couldBeDiameterHeader(buffer)) {
            throw new DiameterParseException(0, "Cannot be a Diameter message because the header is less than 20 bytes");
        }

        final Buffer header = buffer.slice(20);
        return new ImmutableDiameterHeader(header);
    }


    public static AvpHeader frameAvpHeader(final Buffer buffer) throws DiameterParseException, IOException {
        try {
            final Buffer base = buffer.toReadableBuffer().readBytes(8);

        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new DiameterParseException("Unable to read 8 bytes from the buffer, not enough data to parse AVP.");
        }
        return null;
    }

    /**
     * Helper function to see if the supplied byte-buffer could be a diameter message. Even if this method
     * returns true, there is no guarantee that it indeed is a Diameter message but if it doesn't go through,
     * then it is definitely NOT a Diameter message.
     *
     * @param buffer
     * @return true if this could potentially be a diameter message, false if it def is not a diameter message.
     * @throws IOException
     */
    public static boolean couldBeDiameterMessage(final Buffer buffer) throws IOException {

        if (!couldBeDiameterHeader(buffer)) {
            return false;
        }

        // perhaps more stuff? Checking version?
        return true;
    }

    /**
     * A diameter message must be at least 20 bytes long. This is then just
     * diameter header and no AVPs. I guess one could argue there must also
     * be at least one AVP but we'll add that later if that is necessary.
     */
    public static boolean couldBeDiameterHeader(final Buffer buffer) throws IOException {
        return couldBeDiameterHeader(buffer.toReadableBuffer());
    }

    public static boolean couldBeDiameterHeader(final ReadableBuffer buffer) throws IOException {
        return buffer.getReadableBytes() >= 20;
    }
}
