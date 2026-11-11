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
    @GameTest(template = "empty")
    public static void lethalToxinKillsWherePoisonStops(GameTestHelper helper) {
        var poisoned = EntityType.COW.create(helper.getLevel());
        var intoxicated = EntityType.COW.create(helper.getLevel());
        poisoned.setHealth(1);
        intoxicated.setHealth(1);
        var poison = new MobEffectInstance(MobEffects.POISON, 100);
        var toxin = new MobEffectInstance(PufferfishEffects.LETHAL_TOXIN, 100);
        poison.tick(poisoned, () -> {});
        toxin.tick(intoxicated, () -> {});
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
