package io.pkts.diameter.avp.type;

import io.snice.buffer.Buffer;

public interface Integer64 extends DiameterType {

    static Integer64 parse(final Buffer data) {
        return new DefaultInteger64(data.getUnsignedInt(0));
    }

    long getValue();

    class DefaultInteger64 implements Integer64 {
        private final long value;

        private DefaultInteger64(final long value) {
            this.value = value;
        }

        @Override
        public long getValue() {
            return value;
        }
    }
}
