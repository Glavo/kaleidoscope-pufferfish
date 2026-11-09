// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishContactDamageGameTests {
    @GameTest(template = "empty")
    public static void everyVariantHurtsOnContactWithAnyFace(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var direction : Direction.values()) level.setBlockAndUpdate(pos.relative(direction), Blocks.STONE.defaultBlockState());
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            var fish = variant.fish().get();
            for (var block : new PlacedPufferfishBlock[] {PufferfishItems.isHeavy(fish)
                    ? PufferfishBlocks.PLACED_HEAVY_FISH.get() : PufferfishBlocks.PLACED_FISH.get(), variant.block().get()}) {
                var state = block.defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                ((PlacedPufferfishBlockEntity) level.getBlockEntity(pos)).setFish(fish);
                assertContact(helper, pos, state);
            }
            var skin = PufferfishBlocks.skinForFish(fish);
            for (var face : AttachFace.values()) {
                for (var facing : Direction.Plane.HORIZONTAL) {
                    var state = skin.defaultBlockState().setValue(PufferfishSkinBlock.FACE, face).setValue(PufferfishSkinBlock.FACING, facing);
                    level.setBlockAndUpdate(pos, state);
                    assertContact(helper, pos, state);
                }
            }
        }
        helper.succeed();
    }

    private static void assertContact(GameTestHelper helper, BlockPos pos, BlockState state) {
        var level = helper.getLevel();
        var shape = state.getCollisionShape(level, pos).bounds().move(pos);
        for (var side : Direction.values()) {
            var entity = EntityType.COW.create(level);
            var center = shape.getCenter();
            double x = side == Direction.EAST ? shape.maxX + entity.getBbWidth() / 2
                    : side == Direction.WEST ? shape.minX - entity.getBbWidth() / 2 : center.x;
            double y = side == Direction.UP ? shape.maxY : side == Direction.DOWN ? shape.minY - entity.getBbHeight()
                    : center.y - entity.getBbHeight() / 2;
            double z = side == Direction.SOUTH ? shape.maxZ + entity.getBbWidth() / 2
                    : side == Direction.NORTH ? shape.minZ - entity.getBbWidth() / 2 : center.z;
            entity.setPos(x, y, z);
            entity.tickCount = 20;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getHealth() == entity.getMaxHealth() - 1, "Contact did not deal one point: " + state + " / " + side);
            entity.invulnerableTime = 0;
            entity.setPos(entity.position().add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.02)));
            entity.tickCount = 40;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getHealth() == entity.getMaxHealth() - 1, "Nearby entity was hurt without touching the shape");
        }
    }

    @GameTest(template = "empty", timeoutTicks = 90)
    public static void multipleContactsDealOnePointPerSecondAndStopAfterLeaving(GameTestHelper helper) {
        var block = PufferfishLamps.VARIANTS.get("golden_pufferfish_lamp").block().get();
        helper.setBlock(new BlockPos(2, 2, 2), block);
        helper.setBlock(new BlockPos(3, 2, 2), block);
        var entity = helper.spawn(EntityType.COW, 3, 2.625F, 2.5F);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        float health = entity.getHealth();
        helper.runAfterDelay(25, () -> helper.assertTrue(entity.getHealth() == health - 1, "Contact damage was not one point per second"));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(entity.getHealth() == health - 2, "Overlapping contacts stacked damage");
            entity.setPos(entity.position().add(0, 2, 0));
        });
        helper.runAfterDelay(65, () -> {
            helper.assertTrue(entity.getHealth() == health - 2, "Contact damage continued after leaving");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void contactKillsWithItsOwnMessageAndRespectsInvulnerability(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, PufferfishLamps.VARIANTS.get("waxed_copper_pufferfish_lamp").block().get().defaultBlockState());
        var entity = EntityType.COW.create(helper.getLevel());
        entity.setPos(Vec3.atBottomCenterOf(pos).add(0, 0.625, 0));
        entity.tickCount = 20;
        entity.setInvulnerable(true);
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        helper.assertTrue(entity.getHealth() == entity.getMaxHealth(), "Contact bypassed invulnerability");
        entity.setInvulnerable(false);
        entity.setHealth(1);
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        var source = entity.getLastDamageSource();
        helper.assertTrue(entity.isDeadOrDying() && source != null && source.is(PufferfishContactDamage.DAMAGE_TYPE), "Contact did not kill with its own damage type");
        helper.assertTrue(source.getLocalizedDeathMessage(entity).getContents() instanceof TranslatableContents message
                && message.getKey().equals("death.attack.kaleidoscope_pufferfish.pufferfish_prick"), "Incorrect prick death message");
        var item = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.625, pos.getZ() + 0.5, new ItemStack(Items.APPLE));
        for (int tick = 20; tick <= 100; tick += 20) {
            item.tickCount = tick;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(item));
        }
        helper.assertTrue(item.isRemoved(), "Contact did not damage dropped items like cactus");
        helper.succeed();
    }
}
