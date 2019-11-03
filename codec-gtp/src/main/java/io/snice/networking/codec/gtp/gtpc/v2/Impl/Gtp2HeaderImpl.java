package io.snice.networking.codec.gtp.gtpc.v2.Impl;

import io.snice.buffer.Buffer;
import io.snice.buffer.ReadableBuffer;
import io.snice.networking.codec.gtp.GtpVersionException;
import io.snice.networking.codec.gtp.Teid;
import io.snice.networking.codec.gtp.gtpc.v2.Gtp2Header;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class Gtp2HeaderImpl implements Gtp2Header {

    /**
     * Contains the entire raw GTPv2 header, which is always 8 or 12
     * bytes long, depending on whether the TEID is present or not.
     * <p>
     * And the f@#$4 dumb thing is that instead of having the optional
     * TEID at the end of those bytes, it is in the middle, which means that
     * the sequence no etc, which are always present, are now in different
     * directions. The people that defines standards are certainly not
     * developers!
     */
    private final Buffer header;
    private final Optional<Teid> teid;
    private final Buffer seqNo;

    /**
     * Frame the buffer into a {@link Gtp2Header}. A {@link Gtp2Header} is either 8 or 12 bytes long
     * depending if the TEID is present or not.
     *
     * @param buffer
     * @return
     */
    public static Gtp2Header frame(final ReadableBuffer buffer) {
        assertNotNull(buffer, "The buffer cannot be null");
        assertArgument(buffer.capacity() >= 8, "The minimum no of bytes for a GTP header is 8 bytes. This buffer contains less");

        final byte flags = buffer.getByte(0);
        final int version = (flags & 0b11100000) >> 5;
        if (version != 2) {
            throw new GtpVersionException(2, version);
        }

        final boolean teidFlag = (flags & 0b00001000) == 0b00001000;
        final Buffer header = teidFlag ? buffer.readBytes(12) : buffer.readBytes(8);

        final Optional<Teid> teid;
        if (teidFlag) {
            teid = Optional.of(Teid.of(header.slice(4, 8)));
        } else {
            teid = Optional.empty();
        }

        final Buffer seqNo = teidFlag ? header.slice(8, 11) : header.slice(4, 7);
        return new Gtp2HeaderImpl(header, teid, seqNo);
    }

    private Gtp2HeaderImpl(final Buffer header, final Optional<Teid> teid, final Buffer seqNo) {
        this.header = header;
        this.teid = teid;
        this.seqNo = seqNo;
    }

    @Override
    public int getLength() {
        return header.getUnsignedShort(2);
    }

    @Override
    public int getBodyLength() {
        return getLength() - header.capacity() + 4;
    }

    @Override
    public int getTotalLength() {
        return getLength() + 4;
    }

    @Override
    public int getMessageTypeDecimal() {
        return Byte.toUnsignedInt(header.getByte(1));
    }

    @Override
    public Optional<Teid> getTeid() {
        return teid;
    }

    @Override
    public Buffer getSequenceNo() {
        return seqNo;
    }

    @Override
    public int getSequenceNoAsDecimal() {
        return seqNo.getIntFromThreeOctets(0);
    }


}
