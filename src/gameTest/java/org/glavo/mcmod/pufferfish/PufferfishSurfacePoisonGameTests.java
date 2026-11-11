// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Verifies actual support detection, poison damage, refreshing, and expiration after leaving.
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSurfacePoisonGameTests {
    /// Prevents construction of the test holder.
    private PufferfishSurfacePoisonGameTests() {}

    /// Places a surface and settles a stationary villager onto its collision shape.
    private static LivingEntity stand(GameTestHelper helper, BlockState state) {
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, state);
        var entity = helper.spawn(EntityType.VILLAGER, relative.above());
        entity.setNoAi(true);
        entity.setPos(pos.getX() + 0.5, pos.getY() + state.getCollisionShape(level, pos).max(Direction.Axis.Y), pos.getZ() + 0.5);
        entity.move(MoverType.SELF, new Vec3(0, -0.1, 0));
        helper.assertTrue(entity.onGround() && entity.getOnPos().equals(pos), "Entity did not settle on the tested surface");
        return entity;
    }

    /// Verifies ordinary and dyed skins poison grounded entities through the event bus.
    @GameTest(template = "empty")
    public static void everyPufferfishSurfacePoisonsStandingEntities(GameTestHelper helper) {
        var states = new ArrayList<BlockState>();
        states.add(PufferfishBlocks.skin(null).defaultBlockState());
        states.add(PufferfishBlocks.GOLDEN_SKIN.get().defaultBlockState());
        states.add(PufferfishBlocks.NETHERITE_SKIN.get().defaultBlockState());
        states.add(PufferfishBlocks.HEAVY_SKIN.get().defaultBlockState());
        for (int stage = 0; stage < 4; stage++) {
            states.add(PufferfishBlocks.COPPER_SKINS.get(stage).get().defaultBlockState());
        }
        for (var color : DyeColor.values()) {
            states.add(PufferfishBlocks.skin(color).defaultBlockState());
        }
        for (var state : states) {
            var entity = stand(helper, state);
            entity.invulnerableTime = 0;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            var poison = entity.getEffect(MobEffects.POISON);
            int copper = CopperPufferfish.skinIndex(state.getBlock().asItem().getDefaultInstance());
            int amplifier = Math.max(0, copper - 1);
            helper.assertTrue(poison != null && poison.getDuration() == 120 && poison.getAmplifier() == amplifier,
                    "Standing on a pufferfish surface applied an incorrect material effect");
            entity.removeEffect(MobEffects.POISON);
            entity.setOnGround(false);
            entity.setPos(entity.position().add(0, 0.1, 0));
            entity.invulnerableTime = 0;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(!entity.hasEffect(MobEffects.POISON), "An airborne entity was poisoned");
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waxedCopperSkinsNeitherApplyNorRefreshPoison(GameTestHelper helper) {
        for (int stage = 0; stage < 4; stage++) {
            var entity = stand(helper, PufferfishBlocks.COPPER_SKINS.get(stage + 4).get().defaultBlockState());
            entity.invulnerableTime = 0;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(!entity.hasEffect(MobEffects.POISON), "Waxed skin applied poison");
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 20, 0));
            entity.invulnerableTime = 0;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            var poison = entity.getEffect(MobEffects.POISON);
            helper.assertTrue(poison != null && poison.getDuration() == 20 && poison.getAmplifier() == 0,
                    "Waxed skin changed existing poison");
            entity.removeEffect(MobEffects.POISON);
            helper.getLevel().setBlockAndUpdate(entity.getOnPos(), PufferfishBlocks.COPPER_SKINS.get(stage).get().defaultBlockState());
            entity.invulnerableTime = 0;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.hasEffect(MobEffects.POISON), "Unwaxed skin no longer applied poison");
            entity.discard();
        }
        helper.succeed();
    }

    /// Verifies ordinary surfaces and poison-immune mobs do not acquire poison.
    @GameTest(template = "empty")
    public static void unrelatedSurfacesAndImmuneEntitiesAreUnaffected(GameTestHelper helper) {
        var entity = stand(helper, Blocks.STONE.defaultBlockState());
        var pos = entity.getOnPos();
        var level = helper.getLevel();
        entity.invulnerableTime = 0;
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        helper.assertTrue(!entity.hasEffect(MobEffects.POISON), "Ordinary stone poisoned an entity");
        entity.discard();
        level.setBlockAndUpdate(pos, PufferfishBlocks.skin(null).defaultBlockState());
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 3, 2));
        zombie.setNoAi(true);
        zombie.setPos(pos.getX() + 0.5, pos.getY() + 1.0 / 16, pos.getZ() + 0.5);
        zombie.move(MoverType.SELF, new Vec3(0, -0.1, 0));
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(zombie));
        helper.assertTrue(!zombie.hasEffect(MobEffects.POISON), "Undead poison immunity was bypassed");
        zombie.discard();
        helper.succeed();
    }

    /// Verifies existing stronger or infinite poison is retained and short Poison I is refreshed.
    @GameTest(template = "empty")
    public static void poisonRefreshPreservesStrongerEffects(GameTestHelper helper) {
        var entity = stand(helper, PufferfishBlocks.skin(null).defaultBlockState());
        for (var effect : new MobEffectInstance[] {
                new MobEffectInstance(MobEffects.POISON, 20, 1), new MobEffectInstance(MobEffects.POISON, -1, 0)}) {
            entity.addEffect(effect);
            entity.invulnerableTime = 0;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getEffect(MobEffects.POISON).getDuration() == effect.getDuration()
                    && entity.getEffect(MobEffects.POISON).getAmplifier() == effect.getAmplifier(), "Existing poison was overwritten");
            entity.removeEffect(MobEffects.POISON);
        }
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        entity.invulnerableTime = 0;
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        helper.assertTrue(entity.getEffect(MobEffects.POISON).getDuration() == 120, "Short poison was not refreshed");
        entity.discard();
        helper.succeed();
    }

    /// Runs real server ticks to verify stationary poisoning deals damage and expires after leaving.
    @GameTest(template = "empty", timeoutTicks = 250)
    public static void stationaryPoisonDamagesAndExpiresAfterLeaving(GameTestHelper helper) {
        var entity = stand(helper, PufferfishBlocks.skin(DyeColor.BLUE).defaultBlockState());
        float health = entity.getHealth();
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(entity.hasEffect(MobEffects.POISON) && entity.getHealth() < health,
                    "Stationary surface poison was absent or its damage timer was repeatedly reset");
            helper.getLevel().setBlockAndUpdate(entity.getOnPos(), Blocks.STONE.defaultBlockState());
            var safe = helper.absolutePos(new BlockPos(2, 3, 2));
            entity.setPos(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
            helper.runAfterDelay(125, () -> {
                helper.assertTrue(!entity.hasEffect(MobEffects.POISON), "Surface poison did not expire after leaving");
                entity.discard();
                helper.succeed();
            });
        });
    }
}
