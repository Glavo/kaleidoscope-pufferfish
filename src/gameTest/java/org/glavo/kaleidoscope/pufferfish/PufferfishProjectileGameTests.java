// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public class PufferfishProjectileGameTests {
    @GameTest(template = "empty")
    public static void throwingConsumesOneFishAndPreservesColor(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        for (InteractionHand hand : InteractionHand.values()) {
            for (DyeColor color : DyeColor.values()) {
                var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 3), color);
                player.setItemInHand(hand, stack);
                var event = new PlayerInteractEvent.RightClickItem(player, hand);
                NeoForge.EVENT_BUS.post(event);
                var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
                helper.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction(), "Throw interaction was not handled");
                helper.assertTrue(stack.getCount() == 2 && projectiles.size() == 1, "Throw did not consume and spawn exactly one fish");
                var projectile = projectiles.getFirst();
                helper.assertTrue(PufferfishColors.get(projectile.getItem()) == color && projectile.getItem().getCount() == 1,
                        "Projectile did not preserve its colored item");
                helper.assertTrue(projectile.getOwner() == player && projectile.getDeltaMovement().length() > 1.0,
                        "Projectile owner or launch velocity is incorrect");
                projectile.discard();
            }
        }
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        creative.setPos(pos.getX(), pos.getY(), pos.getZ());
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(creative, InteractionHand.MAIN_HAND));
        var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
        helper.assertTrue(creative.getMainHandItem().getCount() == 1 && projectiles.size() == 1, "Creative throw consumed a fish");
        helper.assertTrue(PufferfishColors.get(projectiles.getFirst().getItem()) == null, "Undyed projectile gained a color");
        projectiles.forEach(ThrownPufferfish::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void throwingRespectsOtherUsesAndCanceledSpawns(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.setShiftKeyDown(true);
        var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled() && player.getMainHandItem().getCount() == 1, "Sneaking no longer allows eating");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH_BUCKET));
        event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled(), "Bucket use was intercepted");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.getCooldowns().addCooldown(Items.PUFFERFISH, 20);
        event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled() && player.getMainHandItem().getCount() == 1, "Item cooldown was bypassed");
        player.getCooldowns().removeCooldown(Items.PUFFERFISH);
        Consumer<EntityJoinLevelEvent> denySpawn = join -> {
            if (join.getEntity() instanceof ThrownPufferfish) {
                join.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(denySpawn);
        try {
            event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.getCancellationResult() == InteractionResult.FAIL && player.getMainHandItem().getCount() == 1,
                    "Canceled spawn consumed a fish");
        } finally {
            NeoForge.EVENT_BUS.unregister(denySpawn);
        }
        var spectator = helper.makeMockPlayer(GameType.SPECTATOR);
        spectator.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        event = new PlayerInteractEvent.RightClickItem(spectator, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled(), "Spectator threw a fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void collisionDealsTwoDamageAndSixSecondsOfPoison(GameTestHelper helper) {
        var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        float health = target.getHealth();
        var projectile = hit(helper, target);
        helper.assertTrue(target.getHealth() == health - 2.0F, "Impact did not deal exactly two base damage");
        var effect = target.getEffect(MobEffects.POISON);
        helper.assertTrue(effect != null && effect.getDuration() == 120 && effect.getAmplifier() == 0,
                "Impact did not apply Poison I for 120 ticks");
        helper.assertTrue(projectile.isRemoved(), "Projectile survived its impact");
        helper.assertTrue(target.getLastHurtByMob() == projectile.getOwner(), "Damage was not attributed to the thrower");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void impactsRespectInvulnerabilityAndPoisonImmunity(GameTestHelper helper) {
        var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 3));
        target.setInvulnerable(true);
        float health = target.getHealth();
        var projectile = hit(helper, target);
        helper.assertTrue(target.getHealth() == health && !target.hasEffect(MobEffects.POISON), "Invulnerable target was damaged or poisoned");
        helper.assertTrue(projectile.isRemoved(), "Blocked projectile was not removed");
        target.discard();
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        float zombieHealth = zombie.getHealth();
        hit(helper, zombie);
        helper.assertTrue(zombie.getHealth() < zombieHealth && !zombie.hasEffect(MobEffects.POISON), "Undead poison immunity was bypassed");
        zombie.discard();
        helper.succeed();
    }

    private static ThrownPufferfish hit(GameTestHelper helper, LivingEntity target) {
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var projectile = new ThrownPufferfish(helper.getLevel(), owner);
        projectile.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ() - 2.0);
        projectile.setDeltaMovement(0.0, 0.0, 3.0);
        helper.getLevel().addFreshEntity(projectile);
        projectile.tick();
        return projectile;
    }

    @GameTest(template = "empty")
    public static void blockImpactsRemoveProjectiles(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        var projectile = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 1));
        projectile.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        projectile.setDeltaMovement(0.0, 0.0, 3.0);
        helper.getLevel().addFreshEntity(projectile);
        projectile.tick();
        helper.assertTrue(projectile.isRemoved(), "Projectile survived a block impact");
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 3));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileColorsSurviveSaveAndSynchronization(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            var projectile = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
            var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 8), color);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
            projectile.setItem(stack);
            var receiver = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
            receiver.getEntityData().assignValues(projectile.getEntityData().getNonDefaultValues());
            helper.assertTrue(PufferfishColors.get(receiver.getItem()) == color, "Projectile color did not synchronize");
            var tag = new CompoundTag();
            projectile.saveWithoutId(tag);
            var restored = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
            restored.load(tag);
            helper.assertTrue(PufferfishColors.get(restored.getItem()) == color && restored.getItem().getCount() == 1,
                    "Saved projectile lost its color or duplicated its item");
            helper.assertTrue(restored.getItem().getHoverName().getString().equals("Keepsake"), "Saved projectile lost its name");
        }
        helper.succeed();
    }
}
