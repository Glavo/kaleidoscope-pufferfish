// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class AncientPrismarineGameTests {
    @GameTest(template = "empty", timeoutTicks = 150)
    public static void ancientFrameRequiresSixteenBlocksAndSupportsArchaeology(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 132, 2));
        for (var water : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            level.setBlock(water, Blocks.WATER.defaultBlockState(), 2);
        }
        level.setBlockAndUpdate(pos, PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState());
        var core = (PufferfishConduitBlockEntity) level.getBlockEntity(pos);
        var frame = new ArrayList<BlockPos>();
        helper.assertTrue(!PufferfishConduitBlockEntity.updateFrame(level, pos, frame), "Water alone activated the core");
        var ring = new ArrayList<BlockPos>();
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
            if (Math.abs(x) == 2 || Math.abs(y) == 2) {
                var block = pos.offset(x, y, 0);
                ring.add(block);
                level.setBlockAndUpdate(block, Blocks.PRISMARINE.defaultBlockState());
            }
        }
        helper.assertTrue(PufferfishConduitBlockEntity.updateFrame(level, pos, frame), "Vanilla prismarine did not activate the core");
        for (int i = 0; i < ring.size(); i += 2) {
            level.setBlockAndUpdate(ring.get(i), PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
        }
        var missing = ring.getFirst();
        level.removeBlock(missing, false);
        helper.assertTrue(!PufferfishConduitBlockEntity.updateFrame(level, pos, frame) && frame.size() == 15,
                "Fifteen blocks activated the core");
        level.setBlockAndUpdate(missing, PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE.get().defaultBlockState());
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(core.isActive(), "Mixed sixteen-block frame did not activate the core");
            var brushable = (AncientPrismarineBlockEntity) level.getBlockEntity(missing);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            for (int i = 0; i < 10; i++) brushable.brush(level.getGameTime() + i * 10, player, Direction.UP);
            helper.assertTrue(level.getBlockState(missing).is(PufferfishBlocks.ANCIENT_PRISMARINE.get())
                    && PufferfishConduitBlockEntity.updateFrame(level, pos, frame), "Brushing broke the frame");
            level.removeBlock(missing, false);
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(!core.isActive() && !PufferfishConduitSpawning.isInAncientRange(level, pos),
                        "Broken frame left the ancient aura active");
                for (var block : ring) level.removeBlock(block, false);
                for (var block : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) level.removeBlock(block, false);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void treasureSurvivesReloadAndBrushingOnlyDropsOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 6, 2));
        var state = PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var entity = (AncientPrismarineBlockEntity) level.getBlockEntity(pos);
        var saved = entity.saveWithFullMetadata(level.registryAccess());
        helper.assertTrue(saved.getString("LootTable").equals(AncientPrismarineBlockEntity.LOOT_TABLE.location().toString()),
                "Fresh archaeology block has no loot table");
        var loaded = (AncientPrismarineBlockEntity) BlockEntity.loadStatic(pos, state, saved, level.registryAccess());
        helper.assertTrue(loaded != null && loaded.isValidBlockState(state), "Brushable block type did not survive saving");
        level.setBlockEntity(loaded);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        long tick = level.getGameTime();
        loaded.brush(tick, player, Direction.UP);
        var treasure = loaded.getItem().copy();
        helper.assertTrue(!treasure.isEmpty() && level.getBlockState(pos).getValue(BlockStateProperties.DUSTED) == 1,
                "First brush did not reveal treasure");
        saved = loaded.saveWithFullMetadata(level.registryAccess());
        helper.assertTrue(!saved.contains("LootTable"), "Resolved treasure retained its loot table");
        var resumed = (AncientPrismarineBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        level.setBlockEntity(resumed);
        helper.assertTrue(ItemStack.matches(treasure, resumed.getItem()), "Reload changed the revealed treasure");
        for (int i = 0; i < 10; i++) resumed.brush(tick + 10 + i * 10, player, Direction.UP);
        helper.assertTrue(level.getBlockState(pos).is(PufferfishBlocks.ANCIENT_PRISMARINE.get()), "Finished excavation did not restore stone");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.5));
        helper.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), treasure), "Excavation rerolled or duplicated treasure");
        drops.forEach(ItemEntity::discard);
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void stoneDoesNotFallAndMiningCannotRecoverBuriedTreasure(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 6, 2));
        var state = PufferfishBlocks.SUSPICIOUS_ANCIENT_PRISMARINE.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var tool = new ItemStack(Items.NETHERITE_PICKAXE);
        tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        helper.assertTrue(Block.getDrops(state, level, pos, level.getBlockEntity(pos), helper.makeMockPlayer(GameType.SURVIVAL), tool).isEmpty(),
                "Silk touch recovered an archaeology block");
        for (var block : java.util.List.of(PufferfishBlocks.ANCIENT_PRISMARINE.get(), state.getBlock())) {
            var stone = block.defaultBlockState();
            helper.assertTrue(stone.getDestroySpeed(level, pos) == Blocks.OBSIDIAN.defaultBlockState().getDestroySpeed(level, pos)
                    && block.getExplosionResistance() == Blocks.OBSIDIAN.getExplosionResistance(), "Ancient stone is weaker than obsidian");
            helper.assertTrue(tool.isCorrectToolForDrops(stone) && !new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(stone),
                    "Ancient stone does not require a diamond-tier pickaxe");
        }
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(level.getBlockState(pos).is(state.getBlock()), "Suspended archaeology stone fell");
            level.removeBlock(pos, false);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void archaeologyTableContainsTemplatesRareFishAndOrdinaryTreasure(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2)))
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.ARCHAEOLOGY);
        var table = level.getServer().reloadableRegistries().getLootTable(AncientPrismarineBlockEntity.LOOT_TABLE);
        var random = net.minecraft.util.RandomSource.create(74019L);
        var seen = new java.util.HashSet<net.minecraft.world.item.Item>();
        int templates = 0, rareFish = 0, diamonds = 0;
        for (int roll = 0; roll < 2048; roll++) {
            var items = table.getRandomItems(params, random);
            helper.assertTrue(items.size() == 1 && items.getFirst().getCount() == 1, "Archaeology must produce one treasure");
            var stack = items.getFirst();
            int copperStage = CopperPufferfish.index(stack);
            helper.assertTrue(copperStage == -1 || copperStage >= 1 && copperStage <= 3,
                    "Archaeology produced fresh or waxed copper pufferfish");
            boolean materialFish = copperStage >= 1 && copperStage <= 3 || stack.is(PufferfishItems.IRON)
                    || stack.is(PufferfishItems.GOLDEN) || stack.is(PufferfishItems.DIAMOND);
            helper.assertTrue(materialFish || stack.is(PufferfishItems.UPGRADE_TEMPLATE) || stack.is(Items.PRISMARINE_SHARD)
                    || stack.is(Items.PRISMARINE_CRYSTALS) || stack.is(Items.GOLD_NUGGET), "Archaeology produced unexpected treasure");
            seen.add(stack.getItem());
            if (stack.is(PufferfishItems.UPGRADE_TEMPLATE)) templates++;
            if (materialFish) rareFish++;
            if (stack.is(PufferfishItems.DIAMOND)) diamonds++;
        }
        helper.assertTrue(seen.size() == 10, "Archaeology treasure variety or an aged copper stage is missing");
        helper.assertTrue(templates > 0 && templates < 200, "Template loot is missing or not rare");
        helper.assertTrue(rareFish > 0 && rareFish < 320, "Material fish are missing or too common");
        helper.assertTrue(diamonds > 0 && diamonds < 30, "Diamond pufferfish are missing or too common");
        helper.succeed();
    }
}
