// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestEffects;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishPumpPressureGameTests {
    private static PufferfishPumpBlockEntity placed(GameTestHelper helper, ItemStack fish) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, PufferfishBlocks.PUMP.get().defaultBlockState());
        var pump = (PufferfishPumpBlockEntity) helper.getLevel().getBlockEntity(pos);
        pump.setFish(fish);
        return pump;
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void repairFinishesSafelyBeforeFiveExtraStrokes(GameTestHelper helper) {
        var fish = new ItemStack(Items.PUFFERFISH);
        PufferfishDurability.activate(fish);
        fish.setDamageValue(1);
        var pump = placed(helper, fish);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(pump.press(player) && pump.pressure() == 0 && !pump.fish().isDamaged(), "Final repair stroke added pressure");
        for (int stroke = 1; stroke <= 5; stroke++) {
            int expected = stroke;
            helper.runAfterDelay(stroke * 8L, () -> {
                helper.assertTrue(pump.press(player), "Pressure stroke was rejected");
                if (expected < 5) {
                    helper.assertTrue(pump.pressure() == expected && !pump.fish().isEmpty(), "Fish burst before the fifth extra stroke");
                    helper.assertTrue(!pump.press(player) && pump.pressure() == expected, "Duplicate landing bypassed rebound");
                } else {
                    helper.assertTrue(pump.fish().isEmpty() && pump.pressure() == 0, "Fifth extra stroke did not empty the pump");
                    helper.assertTrue(helper.getLevel().getBlockState(pump.getBlockPos()).is(PufferfishBlocks.PUMP.get()), "Burst destroyed the pump");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void burstPreservesEveryFishVariantAndItsComponents(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var expected = new ArrayList<ItemStack>();
        var pumps = new ArrayList<PufferfishPumpBlockEntity>();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        for (var variant : PufferfishVariant.ALL) {
            var fish = variant.fish();
            fish.enchant(mending, 1);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Pressure test " + expected.size()));
            var pump = new PufferfishPumpBlockEntity(pos, PufferfishBlocks.PUMP.get().defaultBlockState());
            pump.setLevel(helper.getLevel());
            pump.setFish(fish);
            pumps.add(pump);
            PufferfishDurability.activate(fish);
            fish.setDamageValue(fish.getMaxDamage() - Math.max(1, fish.getMaxDamage() / 10));
            expected.add(fish);
        }
        for (int stroke = 1; stroke <= 5; stroke++) {
            int completed = stroke;
            helper.runAfterDelay(stroke * 8L, () -> {
                for (var pump : pumps) pump.press(player);
                if (completed != 5) return;
                var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
                helper.assertTrue(drops.size() == expected.size(), "Burst lost or duplicated fish drops");
                for (var fish : expected) {
                    var drop = drops.stream().filter(entity -> ItemStack.matches(entity.getItem(), fish)).findFirst().orElseThrow();
                    helper.assertTrue(drop.isAlive() && drop.getDeltaMovement().y > 0 && drop.getDeltaMovement().horizontalDistance() > 0,
                            "Fish did not survive and launch upwards");
                    helper.assertTrue(PufferfishDurability.deflationStage(drop.getItem()) == 3, "Burst fish is not fully deflated");
                }
                helper.succeed();
            });
        }
    }

    @GameTest(template = "empty", timeoutTicks = 130)
    public static void pressurePersistsLeaksAndClearsOnRetrieval(GameTestHelper helper) {
        var fish = new ItemStack(Items.PUFFERFISH);
        var pump = placed(helper, fish);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int stroke = 0; stroke < 4; stroke++) {
            helper.runAfterDelay(stroke * 8L + 1, () -> pump.press(player));
        }
        helper.runAfterDelay(30, () -> {
            var saved = pump.saveWithoutMetadata(helper.getLevel().registryAccess());
            pump.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(pump.pressure() == 4, "Saving lost pressure");
        });
        helper.runAfterDelay(63, () -> helper.assertTrue(pump.pressure() == 4, "Pressure leaked before two seconds"));
        helper.runAfterDelay(67, () -> helper.assertTrue(pump.pressure() == 3, "Pressure did not start leaking after two seconds"));
        helper.runAfterDelay(87, () -> {
            helper.assertTrue(pump.pressure() == 2, "Pressure did not leak once per second");
            helper.assertTrue(ItemStack.matches(pump.removeFish(), fish) && pump.pressure() == 0, "Retrieval lost fish or retained pressure");
            pump.setFish(fish);
            helper.assertTrue(pump.press(player) && pump.pressure() == 1, "Reinserted fish retained old pressure");
        });
        helper.runAfterDelay(129, () -> {
            helper.assertTrue(pump.pressure() == 0 && ItemStack.matches(pump.fish(), fish), "Venting consumed durability or failed to finish");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void flailsAreRejectedAndPreviouslyStoredFlailsRemainRecoverable(GameTestHelper helper) {
        var pump = placed(helper, ItemStack.EMPTY);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var hit = new net.minecraft.world.phys.BlockHitResult(pump.getBlockPos().getCenter(), net.minecraft.core.Direction.UP, pump.getBlockPos(), false);
        for (var item : PufferfishItems.FLAIL_VARIANTS) {
            for (boolean damaged : new boolean[] {false, true}) {
                var flail = item.toStack();
                if (damaged) {
                    PufferfishDurability.activate(flail);
                    flail.setDamageValue(1);
                }
                var expected = flail.copy();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, flail);
                var result = pump.getBlockState().useItemOn(flail, helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(result == net.minecraft.world.ItemInteractionResult.FAIL && pump.fish().isEmpty()
                        && ItemStack.matches(flail, expected), "Pump accepted or consumed a flail");
                pump.setFish(flail);
                helper.assertTrue(pump.fish().isEmpty() && !PufferfishPumpItem.canInflate(flail), "Flail bypassed pump validation");
                var saved = pump.saveWithoutMetadata(helper.getLevel().registryAccess());
                saved.put("Fish", flail.save(helper.getLevel().registryAccess()));
                var restored = new PufferfishPumpBlockEntity(pump.getBlockPos(), pump.getBlockState());
                restored.loadWithComponents(saved, helper.getLevel().registryAccess());
                restored.setLevel(helper.getLevel());
                helper.assertTrue(!restored.press(player) && restored.pressure() == 0
                        && ItemStack.matches(restored.removeFish(), expected), "Stored flail was repaired or lost on recovery");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 70)
    public static void burstDamagesNearbyTargetsButRespectsFishAndCover(GameTestHelper helper) {
        var pump = placed(helper, new ItemStack(Items.PUFFERFISH));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var near = helper.spawn(EntityType.COW, new Vec3(3.25, 2.5, 2.5));
        var far = helper.spawn(EntityType.COW, new Vec3(2.5, 2.5, 4));
        var covered = helper.spawn(EntityType.COW, new Vec3(0.5, 2.5, 2.5));
        var fish = helper.spawn(EntityType.PUFFERFISH, new Vec3(2.5, 2.5, 1.5));
        var cat = helper.spawn(EntityType.CAT, new Vec3(4, 2.5, 1.5));
        for (var entity : java.util.List.of(near, far, covered, fish, cat)) {
            entity.setNoAi(true);
            entity.setNoGravity(true);
        }
        for (int y = 2; y <= 4; y++) for (int z = 1; z <= 3; z++) helper.setBlock(new BlockPos(1, y, z), Blocks.STONE);
        float initial = near.getHealth();
        float fishHealth = fish.getHealth();
        for (int stroke = 1; stroke <= 5; stroke++) {
            int completed = stroke;
            helper.runAfterDelay(stroke * 8L, () -> {
                if (completed == 5) {
                    helper.assertTrue(near.getHealth() == initial && far.getHealth() == initial && covered.getHealth() == initial,
                            "Burst targets were damaged before the explosion: " + near.getHealth() + ", " + far.getHealth() + ", " + covered.getHealth());
                }
                var catMovement = cat.getDeltaMovement();
                pump.press(player);
                if (completed != 5) return;
                helper.assertTrue(near.getHealth() < far.getHealth() && far.getHealth() < initial && initial - near.getHealth() <= 6,
                        "Burst damage did not fall off with distance: " + near.getHealth() + ", " + far.getHealth() + ", " + initial
                                + "; positions: " + near.position() + ", " + far.position() + "; center: " + pump.getBlockPos().getCenter());
                helper.assertTrue(TestEffects.hasPoison(near) && near.getDeltaMovement().lengthSqr() > 0, "Burst omitted poison or knockback");
                helper.assertTrue(covered.getHealth() == initial && !TestEffects.hasPoison(covered), "Burst passed through solid cover");
                helper.assertTrue(fish.getHealth() == fishHealth && fish.getActiveEffects().isEmpty(), "Burst damaged or poisoned another pufferfish");
                helper.assertTrue(cat.getHealth() == cat.getMaxHealth() && cat.getActiveEffects().isEmpty()
                        && cat.getDeltaMovement().equals(catMovement), "Burst damaged, poisoned or pushed a cat");
                helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).is(Blocks.STONE), "Burst destroyed blocks");
                helper.succeed();
            });
        }
    }
}
