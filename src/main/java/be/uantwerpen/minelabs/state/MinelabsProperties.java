package be.uantwerpen.minelabs.state;

import be.uantwerpen.minelabs.block.BohrPart;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;

public class MinelabsProperties {
    public static final EnumProperty<BohrPart> BOHR_PART = EnumProperty.of("part", BohrPart.class);

    public static final BooleanProperty ZOOMED = BooleanProperty.of("zoom");
    public static final BooleanProperty COUNTER = BooleanProperty.of("counter");
}
