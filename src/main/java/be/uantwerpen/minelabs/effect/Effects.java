package be.uantwerpen.minelabs.effect;

import be.uantwerpen.minelabs.Minelabs;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Effects {
    public static final StatusEffect FLYING = register(new HeliumFlight(), "flying");

    /**
     * Register an statusEffect
     *
     * @param statusEffect:       Item Object to register
     * @param identifier: String name of the StatusEffect
     * @return {@link StatusEffect}
     */
    private static StatusEffect register(StatusEffect statusEffect, String identifier) {
        return Registry.register(Registries.STATUS_EFFECT, new Identifier(Minelabs.MOD_ID, identifier), statusEffect);
    }

    /**
     * Main class method<br>
     * Registers all statusEffect
     */
    public static void registerStatusEffects() {
        Minelabs.LOGGER.info("registering StatusEffects");
    }
}
