package io.pkts.diameter.avp.impl;

import io.snice.buffer.Buffer;
import io.pkts.diameter.avp.AvpHeader;
import io.pkts.diameter.impl.DiameterParser;

import java.io.IOException;
import java.util.Optional;

/**
 * @author jonas@jonasborjesson.com
 */
public class ImmutableAvpHeader implements AvpHeader {

    private final Buffer buffer;
    private final Optional<Long> vendorId;

    public ImmutableAvpHeader(final Buffer buffer, final Optional<Long> vendorId) {
        this.buffer = buffer;
        this.vendorId = vendorId;
    }

    @Override
    public int getHeaderLength() {
        // the AVP header length is always at least 8 bytes plus an additional 4 if
        // the optional vendor id is set.
        return vendorId.isPresent() ? 12 : 8;
    }

    @Override
    public long getCode() {
        return buffer.getUnsignedInt(0);
    }

    @Override
    public int getLength() {
        try {
            return DiameterParser.getIntFromThreeOctets(buffer.getByte(5), buffer.getByte(6), buffer.getByte(7));
        } catch (final Exception e) {
            return -1;
        }
    }

    @Override
    public Optional<Long> getVendorId() {
        return vendorId;
    }

    @Override
    public boolean isVendorSpecific() {
        // 5th byte is the command flags
        return checkFirstFlag(buffer, 4);
    }

    @Override
    public boolean isMandatory() {
        return checkSecondFlag(buffer, 4);
    }

    @Override
    public boolean isProtected() {
        return checkThirdFlag(buffer, 4);
    }

    @Override
    public String toString() {
        final String separator = ":";
        final StringBuffer sb = new StringBuffer();
        sb.append(getCode()).append(separator);
        sb.append(isVendorSpecific() ? "V" : "o");
        sb.append(isMandatory() ? "M" : "o");
        sb.append(isProtected() ? "P" : "o");
        sb.append(separator);
        sb.append(getLength());
        sb.append(separator);
        vendorId.ifPresent(sb::append);
        sb.append(separator);
        return sb.toString();
    }

    private static boolean checkFirstFlag(final Buffer buffer, final int index) {
        try {
            return (buffer.getByte(index) & 0b10000000) == 0b10000000;
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Unable to read byte from underlying buffer", e);
        }
    }

    private static boolean checkSecondFlag(final Buffer buffer, final int index) {
        try {
            return (buffer.getByte(index) & 0b01000000) == 0b01000000;
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Unable to read byte from underlying buffer", e);
        }
    }

    private static boolean checkThirdFlag(final Buffer buffer, final int index) {
        try {
            return (buffer.getByte(index) & 0b00100000) == 0b00100000;
        } catch (final IndexOutOfBoundsException e) {
            throw new RuntimeException("Unable to read byte from underlying buffer", e);
        }
    }

}
