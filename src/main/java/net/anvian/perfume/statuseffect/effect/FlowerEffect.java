package net.anvian.perfume.statuseffect.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class FlowerEffect extends StatusEffect {
    public FlowerEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x55ab2d);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % 7 == 0;
    }
}