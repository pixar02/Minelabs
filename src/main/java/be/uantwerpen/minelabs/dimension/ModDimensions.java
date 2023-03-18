package be.uantwerpen.minelabs.dimension;

import be.uantwerpen.minelabs.Minelabs;
import be.uantwerpen.minelabs.block.Blocks;
import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.api.CustomPortalBuilder;
import net.kyrptonaught.customportalapi.portal.frame.VanillaPortalAreaHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

public class ModDimensions {
    private static final RegistryKey<DimensionOptions> DIMENSION_KEY = RegistryKey.of(Registry.DIMENSION_KEY, new Identifier(Minelabs.MOD_ID, "subatom"));
    public static RegistryKey<World> SUBATOM_KEY = RegistryKey.of(Registry.WORLD_KEY, DIMENSION_KEY.getValue());
    public static final RegistryKey<DimensionType> DIMENSION_TYPE_KEY = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, new Identifier(Minelabs.MOD_ID, "subatom_type"));

    public static Identifier SUBATOMICPORTAL_FRAMETESTER = new Identifier(Minelabs.MOD_ID, "subatom");


    public static void register() {
        Minelabs.LOGGER.info("Dimensions done");
        registerPortals();
        Minelabs.LOGGER.info("Portals done");
    }

    private static void registerPortals() {
        //TODO CUSTOM TEXTURE ?? -> custom portal block (must extend CustomPortalBlock)
        // Light Item -> ATOM ?
        // SOUNDS ??

        CustomPortalApiRegistry.registerPortalFrameTester(SUBATOMICPORTAL_FRAMETESTER, () -> new VanillaPortalAreaHelper() {
            /**
             * We want to place the portal one block higher so the subatomic floor isn't replaced.
             */
            @Override
            public BlockPos doesPortalFitAt(World world, BlockPos attemptPos, Direction.Axis axis) {
                BlockPos pos = super.doesPortalFitAt(world, attemptPos, axis);
                if (pos == null) return pos;
                // Super check only covers normal portal positions, we manually check the extra position at the top that is needed when the portal is placed one higher.
                if (!isEmptySpace(world.getBlockState(attemptPos.up(3))) || !isEmptySpace(world.getBlockState(attemptPos.offset(axis, 1).up(3)))){
                    return null;
                }
                return pos.up();
            }
        });

        CustomPortalBuilder.beginPortal()
                .frameBlock(Blocks.SALT_BLOCK)
                .destDimID(SUBATOM_KEY.getValue())
                .onlyLightInOverworld()
                //.customPortalBlock((CustomPortalBlock) Blocks.PORTAL_BLOCK)
                .tintColor(153, 180, 181)
                .customFrameTester(SUBATOMICPORTAL_FRAMETESTER)
                .registerPortal();
    }
}
