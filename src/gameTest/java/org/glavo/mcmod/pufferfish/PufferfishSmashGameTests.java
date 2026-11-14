// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import static org.glavo.mcmod.pufferfish.test.TestItems.enchant;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestEffects;
import org.glavo.mcmod.pufferfish.test.TestItems;

/// Exercises pufferfish smash damage through player attacks and the native successful-hit hooks.
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSmashGameTests {
    private PufferfishSmashGameTests() {}

    /// Creates a fully charged player holding the supplied stack and falling the specified distance.
    private static Player attacker(GameTestHelper helper, ItemStack stack, float distance) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 4, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setNoGravity(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int tick = 0; tick < 36; tick++) player.tick();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setOnGround(distance == 0);
        player.setDeltaMovement(0, -0.4, 0);
        player.fallDistance = distance;
        return player;
    }

    /// Creates an unarmored stationary target with enough health to survive the tested smash.
    private static LivingEntity target(GameTestHelper helper) {
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        return target;
    }

    /// Every non-heavy fish retains normal critical-hit scaling and fall distance across smash boundaries.
    @GameTest(template = "empty")
    public static void nonHeavyFishRetainNormalCriticalHitsAtEveryFallHeight(GameTestHelper helper) {
        for (var stack : TestItems.fishVariants()) {
            if (stack.is(PufferfishItems.HEAVY.get())) continue;
            float groundedDamage = attackDamage(helper, stack.copy(), 0);
            helper.assertTrue(groundedDamage > 0, "Grounded control attack did not damage its target: " + stack);
            for (float distance : new float[] {1.5F, 1.5001F, 3, 8, 12}) {
                var player = attacker(helper, stack.copy(), distance);
                var target = target(helper);
                player.attack(target);
                helper.assertTrue(Math.abs(1000 - target.getHealth() - groundedDamage * 1.5F) < 0.001F,
                        "Non-heavy fish gained smash damage: " + stack + ", distance=" + distance);
                helper.assertTrue(player.fallDistance == distance, "Non-heavy fish reset fall distance: " + stack);
                target.discard();
            }
        }
        helper.succeed();
    }

    /// Measures immediate damage from a fully charged player attack, excluding later poison ticks.
    private static float attackDamage(GameTestHelper helper, ItemStack stack, float distance) {
        var player = attacker(helper, stack, distance);
        var target = target(helper);
        player.attack(target);
        float damage = 1000 - target.getHealth();
        target.discard();
        return damage;
    }

    /// Verifies every Density level adds the same flat bonus before normal critical-hit scaling.
    @GameTest(template = "empty")
    public static void ordinaryDensityAddsFlatDamageAtEveryFallHeight(GameTestHelper helper) {
        for (var original : List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(PufferfishItems.NETHERITE.get()),
                PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.RED))) {
            boolean netherite = PufferfishItems.isNetherite(original);
            for (float distance : new float[] {0, 1.5F, 3, 8, 15}) {
                float baseline = attackDamage(helper, original.copy(), distance);
                for (int rank = 1; rank <= 5; rank++) {
                    var stack = enchant(helper, original.copy(), Enchantments.DENSITY, rank);
                    var player = attacker(helper, stack, distance);
                    var target = target(helper);
                    player.attack(target);
                    float expectedBonus = (0.5F + 0.5F * rank) * (distance > 0 ? 1.5F : 1);
                    helper.assertTrue(Math.abs(1000 - target.getHealth() - baseline - expectedBonus) < 0.001,
                            "Density gained a fall-dependent bonus");
                    helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == ((netherite ? 6 : 1) + 0.5F + 0.5F * rank),
                            "Density did not add the expected flat main-hand damage");
                    target.discard();
                }
            }
        }
        var player = attacker(helper, new ItemStack(Items.PUFFERFISH), 8);
        player.setItemInHand(InteractionHand.OFF_HAND, enchant(helper, new ItemStack(PufferfishItems.NETHERITE.get()), Enchantments.DENSITY, 5));
        var target = target(helper);
        player.attack(target);
        helper.assertTrue(Math.abs(1000 - target.getHealth() - 1.5F) < 0.001, "Offhand Density changed main-hand smash damage");
        target.discard();
        helper.succeed();
    }

    /// Verifies configurable ordinary weapon damage and attack speed without restoring smash behavior.
    @GameTest(template = "empty")
    public static void ordinaryWeaponSettingsApplyToMeleeAndThrowing(GameTestHelper helper) {
        double normal = PufferfishCombatConfig.NORMAL_DAMAGE.get();
        double netherite = PufferfishCombatConfig.NETHERITE_DAMAGE.get();
        double normalSpeed = PufferfishCombatConfig.NORMAL_ATTACK_SPEED.get();
        double netheriteSpeed = PufferfishCombatConfig.NETHERITE_ATTACK_SPEED.get();
        try {
            configure(PufferfishCombatConfig.NORMAL_DAMAGE, 2.5);
            configure(PufferfishCombatConfig.NETHERITE_DAMAGE, 8.0);
            configure(PufferfishCombatConfig.NORMAL_ATTACK_SPEED, 4.0);
            configure(PufferfishCombatConfig.NETHERITE_ATTACK_SPEED, 2.5);
            for (var item : List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get())) {
                var stack = new ItemStack(item);
                double expected = item == Items.PUFFERFISH ? 2.5 : 8;
                var player = attacker(helper, stack, 0);
                helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == expected
                        && PufferfishCombat.damage(stack) == expected
                        && player.getAttributeValue(Attributes.ATTACK_SPEED) == (item == Items.PUFFERFISH ? 4 : 2.5),
                        "Ordinary weapon settings were ignored");
                helper.assertTrue(Math.abs(attackDamage(helper, stack, 12) - expected * 1.5) < 0.001,
                        "Configured ordinary weapon acquired smash damage");
            }
        } finally {
            configure(PufferfishCombatConfig.NORMAL_DAMAGE, normal);
            configure(PufferfishCombatConfig.NETHERITE_DAMAGE, netherite);
            configure(PufferfishCombatConfig.NORMAL_ATTACK_SPEED, normalSpeed);
            configure(PufferfishCombatConfig.NETHERITE_ATTACK_SPEED, netheriteSpeed);
        }
        helper.succeed();
    }

    /// Verifies canceled hits retain fall distance and flight or unrelated items cannot smash.
    @GameTest(template = "empty")
    public static void rejectedHitsAndOtherItemsDoNotGrantSmashProtection(GameTestHelper helper) {
        var player = attacker(helper, new ItemStack(PufferfishItems.HEAVY.get()), 6);
        var target = target(helper);
        Consumer<LivingIncomingDamageEvent> cancel = event -> {
            if (event.getEntity() == target) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            player.attack(target);
            helper.assertTrue(target.getHealth() == 1000 && player.fallDistance == 6
                    && !TestEffects.hasPoison(target), "Canceled hit granted smash effects");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
            target.discard();
        }
        player.startFallFlying();
        helper.assertTrue(!PufferfishCombat.canSmash(player, player.getMainHandItem()), "Elytra flight enabled a smash");
        player.stopFallFlying();
        for (var stack : List.of(new ItemStack(PufferfishItems.GOLDEN.get()), new ItemStack(PufferfishItems.SASHIMI.get()),
                new ItemStack(PufferfishItems.FLAIL.get()), new ItemStack(PufferfishItems.NETHERITE_FLAIL.get()), new ItemStack(PufferfishItems.HEAVY_FLAIL.get()), new ItemStack(Items.COD))) {
            helper.assertTrue(!PufferfishCombat.canSmash(player, stack), "Unrelated item acquired fish smash behavior");
        }
        helper.succeed();
    }

    /// Verifies the server-player hook supplies native mace landing protection and area knockback.
    @GameTest(template = "empty")
    public static void successfulSmashUsesNativeLandingProtection(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "smash-test-player"));
        var pos = helper.absolutePos(new BlockPos(2, 4, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var stack = new ItemStack(PufferfishItems.HEAVY.get(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.fallDistance = 6;
        player.setDeltaMovement(0, -1, 0);
        var target = target(helper);
        target.setOnGround(true);
        var nearby = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 3));
        nearby.setNoAi(true);
        nearby.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(stack.hurtEnemy(target, player), "Fish did not opt into post-hit processing");
        helper.assertTrue(player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos.equals(player.position())
                && Math.abs(player.getDeltaMovement().y - 0.01) < 0.0001, "Native mace landing protection was not applied");
        helper.assertTrue(nearby.getDeltaMovement().lengthSqr() > 0, "Smash did not knock back nearby entities");
        stack.postHurtEnemy(target, player);
        helper.assertTrue(player.fallDistance == 0 && stack.getCount() == 3 && !stack.isDamaged(), "Smash consumed or damaged the fish");
        target.discard();
        nearby.discard();
        helper.succeed();
    }

    /// Changes a test setting and clears its world-restart cache to emulate reopening the world.
    private static <T> void configure(ModConfigSpec.ConfigValue<T> setting, T value) {
        setting.set(value);
        setting.clearCache();
    }

}
