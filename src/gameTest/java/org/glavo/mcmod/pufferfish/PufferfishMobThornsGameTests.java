// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishMobThornsGameTests {
    @GameTest(template = "empty")
    public static void materialFishRetaliationMatchesVanillaThornsAtEachTier(GameTestHelper helper) {
        var level = helper.getLevel();
        var attacker = EntityType.PIG.create(level);
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(level);
            fish.setNoAi(true);
            fish.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
            var storedFish = org.glavo.mcmod.pufferfish.test.TestItems.awakened(fish.fish());
            fish.setFish(storedFish);
            int strength = switch (fish.kind()) {
                case COPPER, IRON, GOLDEN -> 1;
                case DIAMOND, NETHERITE -> 2;
                case RADIANT, HEAVY -> 3;
            };
            int reflected = 0;
            for (int trial = 0; trial < 64; trial++) {
                long seed = 1039L + trial * 71L;
                var source = level.damageSources().mobAttack(attacker);
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                attacker.getRandom().setSeed(seed);
                level.getRandom().setSeed(seed);
                thorns.value().doPostAttack(level, strength, new EnchantedItemInUse(storedFish.copy(), null, fish, item -> {}),
                        EnchantmentTarget.VICTIM, fish, source);
                float expected = attacker.getHealth();
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                attacker.getRandom().setSeed(seed);
                level.getRandom().setSeed(seed);
                fish.setHealth(fish.getMaxHealth());
                fish.invulnerableTime = 0;
                fish.setPuffState(trial % 3);
                helper.assertTrue(fish.hurt(source, 1), "Test attack did not damage fish");
                helper.assertTrue(Math.abs(attacker.getHealth() - expected) < 0.0001,
                        "Fish retaliation differed from vanilla Thorns " + strength + " for " + variant.fish().getId());
                if (attacker.getHealth() < attacker.getMaxHealth()) {
                    reflected++;
                    helper.assertTrue(attacker.getLastDamageSource().is(DamageTypes.THORNS)
                            && attacker.getLastDamageSource().getEntity() == fish, "Reflected damage lost its thorns type or fish attribution");
                }
            }
            helper.assertTrue(reflected > 0 && reflected < 64, "Thorns was missing or triggered on every hit");
            helper.assertTrue(ItemStack.isSameItemSameComponents(storedFish, fish.fish()), "Natural thorns modified the stored fish item");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thornsIgnoresReflectionEnvironmentalDamageAndBlockedHits(GameTestHelper helper) {
        var level = helper.getLevel();
        var attacker = EntityType.PIG.create(level);
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(level);
            fish.setNoAi(true);
            for (int trial = 0; trial < 32; trial++) {
                fish.setHealth(fish.getMaxHealth());
                fish.invulnerableTime = 0;
                fish.hurt(level.damageSources().thorns(attacker), 1);
                helper.assertTrue(attacker.getHealth() == attacker.getMaxHealth(), "Thorns caused recursive retaliation");
                fish.invulnerableTime = 0;
                fish.hurt(level.damageSources().generic(), 1);
                fish.setInvulnerable(true);
                fish.invulnerableTime = 0;
                helper.assertTrue(!fish.hurt(level.damageSources().mobAttack(attacker), 1), "Invulnerable fish accepted a hit");
                fish.setInvulnerable(false);
                helper.assertTrue(attacker.getHealth() == attacker.getMaxHealth(), "An unsuccessful or environmental hit caused retaliation");
            }
        }
        helper.succeed();
    }
}
