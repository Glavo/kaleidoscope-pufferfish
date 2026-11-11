// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.glavo.mcmod.pufferfish.PufferfishEffects;

public final class TestEffects {
    private static final List<Holder<MobEffect>> BONUS_EFFECTS =
            List.of(MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.HUNGER, MobEffects.WEAKNESS);

    private TestEffects() {}

    public static MobEffectInstance poison(LivingEntity target) {
        var normal = target.getEffect(MobEffects.POISON);
        var lethal = target.getEffect(PufferfishEffects.LETHAL_TOXIN);
        if (normal != null && lethal != null) throw new AssertionError("Normal poison and lethal toxin coexist");
        return lethal == null ? normal : lethal;
    }

    public static MobEffectInstance effect(LivingEntity target, Holder<MobEffect> type) {
        return type.equals(MobEffects.POISON) ? poison(target) : target.getEffect(type);
    }

    public static boolean hasPoison(LivingEntity target) { return poison(target) != null; }

    public static void clearPoison(LivingEntity target) {
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(PufferfishEffects.LETHAL_TOXIN);
    }

    public static void assertGlowing(GameTestHelper helper, LivingEntity target) {
        var glow = target.getEffect(MobEffects.GLOWING);
        helper.assertTrue(glow != null && glow.getDuration() == 200 && glow.getAmplifier() == 0, "Radiant damage did not apply ten seconds of glowing");
    }

    public static int prepareWitherHit(LivingEntity target, Entity source, long seed) {
        var current = target.getEffect(MobEffects.WITHER);
        int previous = current == null ? -1 : current.getAmplifier();
        target.removeAllEffects();
        if (previous >= 0) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, previous));
        source.getRandom().setSeed(seed);
        return previous;
    }

    public static void assertWitherHit(GameTestHelper helper, LivingEntity target, int previous, long seed) {
        assertWitherHit(helper, target, previous, seed, 120);
    }

    public static void assertWitherHit(GameTestHelper helper, LivingEntity target, int previous, long seed, int duration) {
        assertWitherHit(helper, target, previous, seed, duration, false);
    }

    public static void assertWitherHit(GameTestHelper helper, LivingEntity target, int previous, long seed, int duration, boolean guaranteedWither) {
        var random = RandomSource.create(seed);
        int expected = previous;
        if (previous < 2 && (guaranteedWither || random.nextFloat() < 0.6F)) expected++;
        var wither = target.getEffect(MobEffects.WITHER);
        helper.assertTrue(expected < 0 ? wither == null : wither != null
                        && wither.getAmplifier() == expected && wither.getDuration() == duration,
                "Radiant hit did not apply its wither chance or refresh duration: " + previous + ", seed=" + seed);
        for (int i = 0; i < BONUS_EFFECTS.size(); i++) {
            boolean triggered = previous >= 2 && random.nextFloat() < 0.05F;
            var effect = target.getEffect(BONUS_EFFECTS.get(i));
            helper.assertTrue(triggered ? effect != null && effect.getAmplifier() == 0 && effect.getDuration() == (i < 2 ? 100 : 200) : effect == null,
                    "Capped radiant hit did not independently roll bonus effect " + i + ", seed=" + seed);
        }
        assertGlowing(helper, target);
        helper.assertTrue(!hasPoison(target), "Radiant attack applied poison");
    }
}
