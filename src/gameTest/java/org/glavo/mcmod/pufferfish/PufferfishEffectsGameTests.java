// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishEffectsGameTests {
    static MobEffectInstance poison(net.minecraft.world.entity.LivingEntity target) {
        var normal = target.getEffect(MobEffects.POISON);
        var lethal = target.getEffect(PufferfishEffects.LETHAL_TOXIN);
        if (normal != null && lethal != null) throw new AssertionError("Normal poison and lethal toxin coexist");
        return lethal == null ? normal : lethal;
    }

    static MobEffectInstance effect(net.minecraft.world.entity.LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type) {
        return type.equals(MobEffects.POISON) ? poison(target) : target.getEffect(type);
    }

    static boolean hasPoison(net.minecraft.world.entity.LivingEntity target) { return poison(target) != null; }

    static void clearPoison(net.minecraft.world.entity.LivingEntity target) {
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(PufferfishEffects.LETHAL_TOXIN);
    }

    @GameTest(template = "empty")
    public static void toxinMergesIndependentMaximaInBothDirections(GameTestHelper helper) {
        var target = EntityType.COW.create(helper.getLevel());
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 2));
        target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 60, 0));
        assertToxin(helper, target, 60, 2);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        assertToxin(helper, target, 120, 2);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 20, 3));
        assertToxin(helper, target, 120, 3);
        target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 200, 0));
        assertToxin(helper, target, 200, 3);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 10, 0));
        assertToxin(helper, target, 200, 3);
        target.removeAllEffects();
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 1200, 1));
        target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 120, 0));
        assertToxin(helper, target, 1200, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toxinPreservesInfiniteDurationAndMilkResetsConversion(GameTestHelper helper) {
        var target = helper.makeMockPlayer(GameType.SURVIVAL);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, -1, 0));
        target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 60, 1));
        assertToxin(helper, target, -1, 1);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
        assertToxin(helper, target, -1, 2);
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), helper.getLevel(), target);
        helper.assertTrue(!hasPoison(target), "Milk left a merged poison effect");
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        helper.assertTrue(target.hasEffect(MobEffects.POISON) && !target.hasEffect(PufferfishEffects.LETHAL_TOXIN), "Conversion persisted after curing");
        target.removeAllEffects();
        target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 60, 0));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, -1, 1));
        assertToxin(helper, target, -1, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toxinConversionRespectsCanceledApplicationsAndImmunity(GameTestHelper helper) {
        var target = EntityType.COW.create(helper.getLevel());
        var blocked = new java.util.concurrent.atomic.AtomicReference<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>>(MobEffects.POISON);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable> listener = event -> {
            if (event.getEntity() == target && event.getEffectInstance().is(blocked.get())) {
                event.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        try {
            target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 60, 0));
            helper.assertTrue(!target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2)), "Canceled poison was accepted");
            assertToxin(helper, target, 60, 0);
            target.removeAllEffects();
            blocked.set(PufferfishEffects.LETHAL_TOXIN);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
            helper.assertTrue(!target.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 60, 0)), "Canceled toxin was accepted");
            helper.assertTrue(target.getEffect(MobEffects.POISON).getAmplifier() == 2 && !target.hasEffect(PufferfishEffects.LETHAL_TOXIN), "Rejected conversion removed poison");
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(!EntityType.ZOMBIE.create(helper.getLevel()).addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 120)), "Toxin bypassed undead poison immunity");
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            helper.assertTrue(!variant.entity().get().create(helper.getLevel()).addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 120)), "Toxin bypassed rare fish immunity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialToxinChancesAndLevelsMatchTheTable(GameTestHelper helper) {
        var chances = java.util.Map.of("copper_pufferfish", 25, "iron_pufferfish", 25, "golden_pufferfish", 25,
                "exposed_copper_pufferfish", 50, "diamond_pufferfish", 50, "weathered_copper_pufferfish", 75,
                "netherite_pufferfish", 75, "oxidized_copper_pufferfish", 100, "heavy_pufferfish", 100);
        var strong = java.util.Set.of("weathered_copper_pufferfish", "netherite_pufferfish", "oxidized_copper_pufferfish", "heavy_pufferfish");
        var source = helper.makeMockPlayer(GameType.SURVIVAL);
        var target = EntityType.COW.create(helper.getLevel());
        for (var variant : PufferfishVariant.ALL) {
            if (variant.material() == PufferfishMaterial.RADIANT) continue;
            int chance = chances.getOrDefault(variant.fishName(), 0);
            int amplifier = strong.contains(variant.fishName()) ? 1 : 0;
            source.getRandom().setSeed(903719);
            var random = net.minecraft.util.RandomSource.create(903719);
            int lethalHits = 0;
            for (int hit = 0; hit < 400; hit++) {
                target.removeAllEffects();
                boolean lethal = chance == 100 || chance > 0 && random.nextFloat() < chance / 100F;
                PufferfishCombat.applyAttackEffect(variant.fish(), target, source);
                var effect = poison(target);
                helper.assertTrue(variant.waxed() ? effect == null : effect != null && effect.getDuration() == 120
                                && effect.getAmplifier() == amplifier && effect.is(lethal ? PufferfishEffects.LETHAL_TOXIN : MobEffects.POISON),
                        "Incorrect poison roll or level: " + variant.fishName() + ", hit=" + hit);
                if (target.hasEffect(PufferfishEffects.LETHAL_TOXIN)) lethalHits++;
            }
            helper.assertTrue(chance == 0 ? lethalHits == 0 : chance == 100 ? lethalHits == 400 : lethalHits > 0 && lethalHits < 400, "Missing probability outcome");
        }
        helper.succeed();
    }

    private static void assertToxin(GameTestHelper helper, net.minecraft.world.entity.LivingEntity target, int duration, int amplifier) {
        var effect = poison(target);
        helper.assertTrue(effect != null && effect.is(PufferfishEffects.LETHAL_TOXIN)
                && effect.getDuration() == duration && effect.getAmplifier() == amplifier, "Toxin failed to merge independent maxima");
    }

    @GameTest(template = "empty")
    public static void lethalToxinKillsWherePoisonStops(GameTestHelper helper) {
        var poisoned = EntityType.COW.create(helper.getLevel());
        var intoxicated = EntityType.COW.create(helper.getLevel());
        poisoned.setHealth(1);
        intoxicated.setHealth(1);
        var poison = new MobEffectInstance(MobEffects.POISON, 100);
        var toxin = new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 100);
        for (int tick = 0; tick < 20; tick++) {
            poison.tick(poisoned, () -> {});
            toxin.tick(intoxicated, () -> {});
        }
        helper.assertTrue(poisoned.getHealth() == 1 && poisoned.isAlive(), "Normal poison killed at one health");
        helper.assertTrue(intoxicated.isDeadOrDying() && intoxicated.getLastDamageSource().is(NeoForgeMod.POISON_DAMAGE),
                "Lethal toxin did not kill with poison damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lethalToxinIntervalsScaleWithoutWrapping(GameTestHelper helper) {
        var effect = PufferfishEffects.LETHAL_TOXIN.get();
        int[] amplifiers = {0, 1, 2, 3, 4, 5, 32, 255};
        int[] expectedTicks = {1, 2, 4, 10, 20, 20, 20, 20};
        for (int index = 0; index < amplifiers.length; index++) {
            int ticks = 0;
            for (int duration = 1; duration <= 20; duration++) {
                if (effect.shouldApplyEffectTickThisTick(duration, amplifiers[index])) ticks++;
            }
            helper.assertTrue(ticks == expectedTicks[index], "Incorrect toxin interval at amplifier " + amplifiers[index]);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void milkCuresLethalToxin(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(player.addEffect(new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 200)), "Toxin could not be applied");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), helper.getLevel(), player);
        helper.assertTrue(!player.hasEffect(PufferfishEffects.LETHAL_TOXIN), "Milk did not cure lethal toxin");
        helper.succeed();
    }
}
