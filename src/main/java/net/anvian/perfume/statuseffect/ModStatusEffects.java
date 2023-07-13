package net.anvian.perfume.statuseffect;

import net.anvian.perfume.PerfumeMod;
import net.anvian.perfume.statuseffect.effect.*;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModStatusEffects {
    public static final StatusEffect CARROT_EFFECT = registerStatusEffects("carrot_effect", new ModEffect(0xffa500));
    public static final StatusEffect WHEAT_EFFECT = registerStatusEffects("wheat_effect", new ModEffect(0xcdb159));
    public static final StatusEffect FLOWER_EFFECT = registerStatusEffects("flower_effect", new ModEffect(0xe7bde6));
    public static final StatusEffect IRON_EFFECT = registerStatusEffects("iron_effect", new ModEffect(0xa3a8ab));
    public static final StatusEffect FISH_EFFECT = registerStatusEffects("fish_effect", new ModEffect(0x60856b));

    private static StatusEffect registerStatusEffects(String name, StatusEffect statusEffect){
        return Registry.register(Registry.STATUS_EFFECT, new Identifier(PerfumeMod.MOD_ID, name), statusEffect);
    }

    public static void registerEffects(){
        PerfumeMod.LOGGER.debug("Registering Mod StatusEffects for " + PerfumeMod.MOD_ID);
    }
}
