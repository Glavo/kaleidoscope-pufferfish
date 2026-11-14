// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;

/// Material poison strength, lethal conversion chance, and the separate handheld melee application chance.
public record PufferfishPoison(int amplifier, float lethalChance, float meleeChance) {
    @Nullable
    public static PufferfishPoison of(ItemStack stack) {
        var variant = PufferfishVariant.of(stack);
        if (variant == null || variant.waxed()) return null;
        return switch (variant.material()) {
            case NORMAL -> new PufferfishPoison(0, 0, 0.6F);
            case COPPER -> switch (variant.oxidation()) {
                case 0 -> new PufferfishPoison(0, 0.25F, 0.6F);
                case 1 -> new PufferfishPoison(0, 0.5F, 0.6F);
                case 2 -> new PufferfishPoison(1, 0.75F, 0.8F);
                default -> new PufferfishPoison(1, 1, 1);
            };
            case IRON, GOLDEN -> new PufferfishPoison(0, 0.25F, 0.6F);
            case DIAMOND -> new PufferfishPoison(0, 0.5F, 0.6F);
            case NETHERITE -> new PufferfishPoison(1, 0.75F, 0.6F);
            case HEAVY -> new PufferfishPoison(1, 1, 1);
            case RADIANT -> null;
        };
    }

    /// Mixed equipment and overlapping surfaces select one profile before rolling.
    @Nullable
    public static PufferfishPoison strongest(@Nullable PufferfishPoison first, @Nullable PufferfishPoison second) {
        if (second == null) return first;
        return first == null || second.amplifier > first.amplifier
                || second.amplifier == first.amplifier && second.lethalChance > first.lethalChance ? second : first;
    }

    public void apply(LivingEntity target, @Nullable Entity source, int duration) {
        if (target instanceof Cat) return;
        var random = source == null ? target.getRandom() : source.getRandom();
        boolean lethal = lethalChance >= 1 || lethalChance > 0 && random.nextFloat() < lethalChance;
        target.addEffect(new MobEffectInstance(lethal ? PufferfishEffects.LETHAL_TOXIN : MobEffects.POISON, duration, amplifier), source);
    }

    /// Contact only rerolls when an effect is due for refresh or its strength increases.
    public boolean shouldRefresh(LivingEntity target) {
        var toxin = target.getEffect(PufferfishEffects.LETHAL_TOXIN);
        if (toxin != null) return !toxin.isInfiniteDuration() && (toxin.getAmplifier() < amplifier || toxin.getDuration() <= 20);
        var poison = target.getEffect(MobEffects.POISON);
        return poison == null || !poison.isInfiniteDuration() && (poison.getAmplifier() < amplifier
                || poison.getAmplifier() == amplifier && poison.getDuration() <= 20);
    }
}
