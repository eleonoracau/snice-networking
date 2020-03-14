package io.snice.networking.codec.diameter.avp.api;

import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;
import io.snice.networking.codec.diameter.avp.Avp;
import io.snice.networking.codec.diameter.avp.AvpMandatory;
import io.snice.networking.codec.diameter.avp.AvpParseException;
import io.snice.networking.codec.diameter.avp.AvpProtected;
import io.snice.networking.codec.diameter.avp.FramedAvp;
import io.snice.networking.codec.diameter.avp.Vendor;

import static io.snice.preconditions.PreConditions.assertNotNull;

import io.snice.networking.codec.diameter.avp.impl.DiameterIpAddressAvp;
import io.snice.networking.codec.diameter.avp.type.IpAddress;

/**
 * This is an autogenerated class - do not edit
 * 
 */
public interface HostIpAddress extends Avp<IpAddress> {

    int CODE = 257;

    
    static HostIpAddress of(final Buffer value) {
        final IpAddress v = IpAddress.parse(value);
        return of(v);
    }

    static HostIpAddress of(final String value) {
        return of(Buffers.wrap(value));
    }

    static HostIpAddress of(final IpAddress value) {
        assertNotNull(value);
        final Builder<IpAddress> builder =
                Avp.ofType(IpAddress.class)
                        .withValue(value)
                        .withAvpCode(CODE)
                        .isMandatory(AvpMandatory.MUST.isMandatory())
                        .isProtected(AvpProtected.MAY.isProtected())
                        .withVendor(Vendor.NONE);

        return new DefaultHostIpAddress(builder.build());
    }

    

    @Override
    default long getCode() {
        return CODE;
    }

    default boolean isHostIpAddress() {
        return true;
    }

    default HostIpAddress toHostIpAddress() {
        return this;
    }

    static HostIpAddress parse(final FramedAvp raw) {
        if (CODE != raw.getCode()) {
            throw new AvpParseException("AVP Code mismatch - unable to parse the AVP into a " + HostIpAddress.class.getName());
        }
        return new DefaultHostIpAddress(raw);
    }

    class DefaultHostIpAddress extends DiameterIpAddressAvp implements HostIpAddress {
        private DefaultHostIpAddress(final FramedAvp raw) {
            super(raw);
        }

        @Override
        public HostIpAddress ensure() {
            return this;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }

            if (other == null) {
                return false;
            }

            try {
                final HostIpAddress o = (HostIpAddress)other;
                final IpAddress v = getValue();
                return v.equals(o.getValue());
            } catch (final ClassCastException e) {
                return false;
            }
        }
    }
}
