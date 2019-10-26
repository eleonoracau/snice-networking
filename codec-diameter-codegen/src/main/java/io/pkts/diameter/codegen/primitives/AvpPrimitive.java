package io.pkts.diameter.codegen.primitives;

import io.pkts.diameter.codegen.CodeGenParseException;
import io.pkts.diameter.codegen.DiameterCollector;
import io.pkts.diameter.codegen.Typedef;
import io.pkts.diameter.codegen.builders.AttributeContext;
import io.pkts.diameter.codegen.builders.DiameterSaxBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.pkts.diameter.codegen.Typedef.ENUMERATED;
import static io.pkts.diameter.codegen.Typedef.GROUPED;
import static io.pkts.diameter.codegen.Typedef.INTEGER_32;

public interface AvpPrimitive extends DiameterPrimitive {

    /**
     * The name of the XML element.
     */
    String NAME = "avp";

    @Override
    default String getElementName() {
        return NAME;
    }

    @Override
    default AvpPrimitive toAvpPrimitive() throws ClassCastException {
        return this;
    }

    default boolean isGrouped() {
        return false;
    }

    default GroupedAvpPrimitive toGrouped() throws ClassCastException {
        throw new ClassCastException("Unable to cast a " + this.getClass().getName() + " into a " + GroupedAvpPrimitive.class.getName());
    }

    default boolean isTyped() {
        return false;
    }

    default TypedAvpPrimitive toTyped() throws ClassCastException {
        throw new ClassCastException("Unable to cast a " + this.getClass().getName() + " into a " + TypedAvpPrimitive.class.getName());
    }

    default boolean isEnumerated() {
        return false;
    }

    default EnumeratedAvpPrimitive toEnumerated() throws ClassCastException {
        throw new ClassCastException("Unable to cast a " + this.getClass().getName() + " into a " + EnumeratedAvpPrimitive.class.getName());
    }

    /**
     * The name of the actual AVP
     *
     * @return
     */
    String getName();

    /**
     * The AVP Code
     *
     * @return
     */
    long getCode();

    static Builder of(final AttributeContext ctx) throws CodeGenParseException {
        ctx.ensureElementName(NAME);

        final String name = ctx.getString("name");
        final long code = ctx.getLong("code");

        return new Builder(ctx, name, code);
    }

    class Builder extends DiameterSaxBuilder.BaseBuilder<AvpPrimitive> {

        private final String name;
        private final long code;

        /**
         * Those elements that we have builders for and that we should accept.
         */
        private static final List<String> acceptableChildElements = new ArrayList<>();

        static {
            acceptableChildElements.add(TypePrimitive.NAME);
            acceptableChildElements.add(EnumPrimitive.NAME);
            acceptableChildElements.add(GroupedPrimitive.NAME);
        }

        private Builder(final AttributeContext ctx, final String name, final long code) {
            super(ctx);
            this.name = name;
            this.code = code;
        }

        @Override
        protected List<String> getKnownChildElements() {
            return acceptableChildElements;
        }

        @Override
        public String getElementName() {
            return NAME;
        }

        @Override
        public AvpPrimitive build(final DiameterCollector ctx) {
            final Map<String, List<DiameterPrimitive>> primitives = buildChildren(ctx);

            final Optional<Typedef> typedef = getType(primitives);
            final Optional<GroupedPrimitive> grouped = getGrouped(primitives);
            final List<EnumPrimitive> enums = getEnums(primitives);

            // can only be either or.
            if (grouped.isPresent() && isNotEmpty(enums)) {
                final String msg = String.format("Expected either a %s elements or one or more elements of type %s",
                        GroupedPrimitive.NAME, EnumPrimitive.NAME);
                throw createException(msg);
            }

            // if grouped, then we don't expect a type
            if (grouped.isPresent() && typedef.isPresent()) {
                throw createException("For a Grouped AVP, we don't expect a type");
            } else if (grouped.isPresent()) {
                final AvpPrimitive avp = new GroupedAvpPrimitive(name, code, grouped.get());
                ctx.collectAvp(avp);
                return avp;
            }

            // if we have enums then the type must be enumerated or integer32 or, as it turns out, unsigned32
            // (note, using OCTET_STRING as a cheat to make the code more readable)
            final Typedef base = typedef.orElse(Typedef.OCTET_STRING).getBaseType();
            final boolean isInteger32 = base.isInteger32() || base.isUnsigned32();
            if (isNotEmpty(enums) && isInteger32) {
                final AvpPrimitive avp = new EnumeratedAvpPrimitive(name, code, enums);
                ctx.collectAvp(avp);
                return avp;
            } else if (isNotEmpty(enums)) {
                final String msg = String.format("When 'enum' is present the type is expected to " +
                        "be %s or %s", ENUMERATED.getName(), INTEGER_32.getName());
                throw createException(msg);
            }

            // only one thing left and that is a regular typed AVP so make sure
            // that the type is indeed specified and if so, create the AVP
            final AvpPrimitive avp = new TypedAvpPrimitive(name, code,
                    typedef.orElseThrow(() -> createException("The AVP must specify the type")));
            ctx.collectAvp(avp);
            return avp;
        }
    }

    abstract class BaseAvpPrimitive implements AvpPrimitive {
        private final String name;
        private final long code;

        private BaseAvpPrimitive(final String name, final long code) {
            this.name = name;
            this.code = code;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getCode() {
            return code;
        }
    }

    class TypedAvpPrimitive extends BaseAvpPrimitive {
        private final Typedef typedef;

        @Override
        public boolean isTyped() {
            return true;
        }

        private TypedAvpPrimitive(final String name, final long code, final Typedef typedef) {
            super(name, code);
            this.typedef = typedef;
        }

        @Override
        public TypedAvpPrimitive toTyped() throws ClassCastException {
            return this;
        }

        @Override
        public Typedef getTypedef() {
            return typedef;
        }

    }

    class GroupedAvpPrimitive extends BaseAvpPrimitive {
        private final GroupedPrimitive grouped;

        @Override
        public boolean isGrouped() {
            return false;
        }

        @Override
        public GroupedAvpPrimitive toGrouped() throws ClassCastException {
            return this;
        }

        private GroupedAvpPrimitive(final String name, final long code, final GroupedPrimitive grouped) {
            super(name, code);
            this.grouped = grouped;
        }

        @Override
        public Typedef getTypedef() {
            return GROUPED;
        }
    }

    class EnumeratedAvpPrimitive extends BaseAvpPrimitive {
        final List<EnumPrimitive> enums;

        @Override
        public boolean isEnumerated() {
            return true;
        }

        @Override
        public EnumeratedAvpPrimitive toEnumerated() throws ClassCastException {
            return this;
        }

        /**
         * Get all the enums in a sorted list based on the code.
         *
         * @return
         */
        public List<EnumPrimitive> getSortedEnums() {
            enums.sort((o1, o2) -> {
                final long res = o1.getEnumCode() - o2.getEnumCode();
                if (res < 0) {
                    return -1;
                } else if (res == 0) {
                    return 0;
                }
                return 1;
            });
            return enums;
        }

        private EnumeratedAvpPrimitive(final String name, final long code, final List<EnumPrimitive> enums) {
            super(name, code);
            this.enums = enums;
        }

        @Override
        public Typedef getTypedef() {
            return ENUMERATED;
        }
    }

}
