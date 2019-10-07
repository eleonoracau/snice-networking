package io.snice.networking.codec.gtp.control.impl;

import io.snice.buffer.Buffer;
import io.snice.buffer.ReadableBuffer;
import io.snice.networking.codec.gtp.GtpVersionException;
import io.snice.networking.codec.gtp.Teid;
import io.snice.networking.codec.gtp.control.Gtp1Header;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class Gtp1HeaderImpl implements Gtp1Header {

    private final Buffer header;
    private final Teid teid;
    private final Optional<Buffer> seqNo;

    private Gtp1HeaderImpl(final Buffer header, final Teid teid, final Optional<Buffer> seqNo) {
        this.header = header;
        this.teid = teid;
        this.seqNo = seqNo;
    }

    /**
     * Frame the buffer into a {@link Gtp1Header}. A {@link Gtp1Header} is either 8 or 12 bytes long
     * depending if any of the optional sequence no, extension header or n-pdu flags are present. Note
     * that even if a single one of those flags are present, the header will be an extra 4 bytes long because,
     * according to TS 29.274 section 5.1:
     * <p>
     * "Control Plane GTP header length shall be a multiple of 4 octets"
     *
     * @param buffer
     * @return
     */
    public static Gtp1Header frame(final ReadableBuffer buffer) {
        assertNotNull(buffer, "The buffer cannot be null");
        assertArgument(buffer.capacity() >= 8, "The minimum no of bytes for a GTP header is 8 bytes. This buffer contains less");

        final byte flags = buffer.getByte(0);
        final int version = (flags & 0b11100000) >> 5;
        if (version != 1) {
            throw new GtpVersionException(1, version);
        }

        // if any of the sequence no, extension or n-pdu flags are "on" then we have an additional
        // 4 bytes in the header, hence, a "long" header
        final boolean nPduNoFlag = (flags & 0b00000100) == 0b00000100;
        final boolean seqNoFlag = (flags & 0b00000010) == 0b00000010;
        final boolean extHeaderFlag = (flags & 0b00000001) == 0b00000001;
        final boolean longHeader = seqNoFlag || nPduNoFlag || extHeaderFlag;

        final Buffer header = longHeader ? buffer.readBytes(12) : buffer.readBytes(8);
        final Teid teid = Teid.of(header.slice(4, 8));
        final Optional<Buffer> seqNo = seqNoFlag ? Optional.of(header.slice(8, 10)) : Optional.empty();

        return new Gtp1HeaderImpl(header, teid, seqNo);
    }

    @Override
    public Teid getTeid() {
        return teid;
    }

    @Override
    public Optional<Buffer> getSequenceNo() {
        return seqNo;
    }

    @Override
    public Optional<Integer> getSequenceNoAsDecimal() {
        return seqNo.map(b -> b.getUnsignedShort(0));
    }

    @Override
    public int getLength() {
        return header.getUnsignedShort(2);
    }

    @Override
    public int getBodyLength() {
        return getLength() - header.capacity() + 8;
    }

    @Override
    public int getTotalLength() {
        return getLength() + 8;
    }

    @Override
    public int getMessageTypeDecimal() {
        return Byte.toUnsignedInt(header.getByte(1));
    }

}
