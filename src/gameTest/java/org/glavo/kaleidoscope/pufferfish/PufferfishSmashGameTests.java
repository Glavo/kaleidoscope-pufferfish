// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Exercises pufferfish smash damage through player attacks and the native successful-hit hooks.
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSmashGameTests {
    /// Prevents construction of the test holder.
    private PufferfishSmashGameTests() {}

    /// Creates a fully charged player holding the supplied stack and falling the specified distance.
    private static Player attacker(GameTestHelper helper, ItemStack stack, float distance) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int tick = 0; tick < 22; tick++) player.tick();
        var pos = helper.absolutePos(new BlockPos(2, 4, 1));
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

    /// Verifies all fish variants and the exact fall threshold and damage-curve boundaries.
    @GameTest(template = "empty")
    public static void fishSmashDamageFollowsTheConfiguredCurve(GameTestHelper helper) {
        var stacks = new ArrayList<ItemStack>();
        stacks.add(new ItemStack(Items.PUFFERFISH, 4));
        stacks.add(new ItemStack(PufferfishItems.NETHERITE.get(), 4));
        for (var color : DyeColor.values()) stacks.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 4), color));
        for (var stack : stacks) {
            for (float distance : new float[] {0, 1.5F, 1.5001F, 3, 8, 12}) {
                var player = attacker(helper, stack.copy(), distance);
                var target = target(helper);
                player.attack(target);
                float extra = distance <= 1.5F ? 0 : distance <= 3 ? 4 * distance
                        : distance <= 8 ? 12 + 2 * (distance - 3) : 22 + distance - 8;
                float expected = ((PufferfishItems.isNetherite(stack) ? 10 : 2) + extra) * (distance > 0 ? 1.5F : 1);
                helper.assertTrue(Math.abs(1000 - target.getHealth() - expected) < 0.001F, "Incorrect smash damage at distance " + distance);
                helper.assertTrue(player.fallDistance == (distance > 1.5F ? 0 : distance), "Fall distance reset without a successful smash");
                helper.assertTrue(ItemStack.matches(player.getMainHandItem(), stack) && target.hasEffect(MobEffects.POISON),
                        "Smash consumed fish or lost existing poison effects");
                target.discard();
            }
        }
        helper.succeed();
    }

    /// Verifies canceled hits retain fall distance and flight or unrelated items cannot smash.
    @GameTest(template = "empty")
    public static void rejectedHitsAndOtherItemsDoNotGrantSmashProtection(GameTestHelper helper) {
        var player = attacker(helper, new ItemStack(Items.PUFFERFISH), 6);
        var target = target(helper);
        Consumer<LivingIncomingDamageEvent> cancel = event -> {
            if (event.getEntity() == target) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            player.attack(target);
            helper.assertTrue(target.getHealth() == 1000 && player.fallDistance == 6
                    && !target.hasEffect(MobEffects.POISON), "Canceled hit granted smash effects");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
            target.discard();
        }
        player.startFallFlying();
        helper.assertTrue(!PufferfishCombat.canSmash(player, player.getMainHandItem()), "Elytra flight enabled a smash");
        player.stopFallFlying();
        for (var stack : List.of(new ItemStack(PufferfishItems.GOLDEN.get()), new ItemStack(PufferfishItems.MEAT.get()),
                new ItemStack(PufferfishItems.FLAIL.get()), new ItemStack(PufferfishItems.NETHERITE_FLAIL.get()), new ItemStack(Items.COD))) {
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
        var stack = new ItemStack(Items.PUFFERFISH, 3);
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

    /// Verifies configured melee damage, per-block bonuses, caps, and disabling without changing projectile damage.
    @GameTest(template = "empty")
    public static void customSmashSettingsAffectOnlyFishMelee(GameTestHelper helper) {
        double normal = PufferfishCombatConfig.NORMAL_MELEE_DAMAGE.get();
        double netherite = PufferfishCombatConfig.NETHERITE_MELEE_DAMAGE.get();
        double first = PufferfishCombatConfig.FIRST_THREE_BLOCKS_DAMAGE.get();
        double next = PufferfishCombatConfig.NEXT_FIVE_BLOCKS_DAMAGE.get();
        double remaining = PufferfishCombatConfig.REMAINING_BLOCKS_DAMAGE.get();
        double cap = PufferfishCombatConfig.MAX_SMASH_BONUS.get();
        boolean enabled = PufferfishCombatConfig.SMASH_ENABLED.get();
        try {
            configure(PufferfishCombatConfig.NORMAL_MELEE_DAMAGE, 5.0);
            configure(PufferfishCombatConfig.NETHERITE_MELEE_DAMAGE, 15.0);
            configure(PufferfishCombatConfig.FIRST_THREE_BLOCKS_DAMAGE, 1.0);
            configure(PufferfishCombatConfig.NEXT_FIVE_BLOCKS_DAMAGE, 1.0);
            configure(PufferfishCombatConfig.REMAINING_BLOCKS_DAMAGE, 1.0);
            configure(PufferfishCombatConfig.MAX_SMASH_BONUS, 7.0);
            for (var item : List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get())) {
                var stack = new ItemStack(item);
                var player = attacker(helper, stack, 12);
                var target = target(helper);
                player.attack(target);
                float expected = (item == Items.PUFFERFISH ? 5 + 7 : 15 + 7) * 1.5F;
                helper.assertTrue(Math.abs(1000 - target.getHealth() - expected) < 0.001, "Configured damage or cap was ignored");
                helper.assertTrue(PufferfishCombat.damage(stack) == (item == Items.PUFFERFISH ? 2 : 10), "Melee configuration changed projectile damage");
                target.discard();
            }
            configure(PufferfishCombatConfig.SMASH_ENABLED, false);
            var player = attacker(helper, new ItemStack(Items.PUFFERFISH), 12);
            var target = target(helper);
            player.attack(target);
            helper.assertTrue(target.getHealth() == 992.5F && player.fallDistance == 12, "Disabled smash still added damage or reset falling");
            target.discard();
        } finally {
            configure(PufferfishCombatConfig.NORMAL_MELEE_DAMAGE, normal);
            configure(PufferfishCombatConfig.NETHERITE_MELEE_DAMAGE, netherite);
            configure(PufferfishCombatConfig.FIRST_THREE_BLOCKS_DAMAGE, first);
            configure(PufferfishCombatConfig.NEXT_FIVE_BLOCKS_DAMAGE, next);
            configure(PufferfishCombatConfig.REMAINING_BLOCKS_DAMAGE, remaining);
            configure(PufferfishCombatConfig.MAX_SMASH_BONUS, cap);
            configure(PufferfishCombatConfig.SMASH_ENABLED, enabled);
        }
        helper.succeed();
    }
}
