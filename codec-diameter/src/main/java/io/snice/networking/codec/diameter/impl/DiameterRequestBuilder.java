package io.snice.networking.codec.diameter.impl;

import io.snice.buffer.Buffer;
import io.snice.networking.codec.diameter.DiameterHeader;
import io.snice.networking.codec.diameter.DiameterRequest;
import io.snice.networking.codec.diameter.avp.FramedAvp;

import java.util.List;

public class DiameterRequestBuilder extends DiameterMessageBuilder<DiameterRequest> implements DiameterRequest.Builder {

    private DiameterRequestBuilder(final DiameterHeader.Builder header) {
        super(header);
    }

    public static DiameterRequestBuilder createCER() {
        final var header = createHeader(257);
        header.withApplicationId(0L);
        return new DiameterRequestBuilder(header);
    }

    public static DiameterRequestBuilder createRequest(final int commandCode) {
        final var header = createHeader(257);
        return new DiameterRequestBuilder(header);
    }

    private static DiameterHeader.Builder createHeader(final int commandCode) {
        final var header = DiameterHeader.of();
        header.withCommandCode(commandCode);
        header.isRequest();
        return header;
    }

    @Override
    protected DiameterRequest internalBuild(final Buffer message, final DiameterHeader header,
                                            final List<FramedAvp> avps, final short indexOfOriginHost,
                                            final short indexOfOriginRealm, final short indexOfDestinationHost,
                                            final short indexOfDestinationRealm) {
        return new ImmutableDiameterRequest(message, header, avps, indexOfOriginHost, indexOfOriginRealm,
                indexOfDestinationHost, indexOfDestinationRealm);
    }
}
