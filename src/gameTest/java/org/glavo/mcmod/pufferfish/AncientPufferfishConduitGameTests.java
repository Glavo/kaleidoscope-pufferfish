// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class AncientPufferfishConduitGameTests {
    @GameTest(template = "empty", timeoutTicks = 210)
    public static void ancientFrameActivatesConduitWithFixedRange(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 82, 2));
        var normalPos = pos.east(8);
        for (var center : java.util.List.of(pos, normalPos)) {
            for (var water : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                level.setBlock(water, Blocks.WATER.defaultBlockState(), 2);
            }
        }
        level.setBlockAndUpdate(pos, PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState());
        level.setBlockAndUpdate(normalPos, PufferfishBlocks.CONDUIT.get().defaultBlockState());
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
            if (Math.abs(x) == 2 || Math.abs(y) == 2) {
                level.setBlockAndUpdate(pos.offset(x, y, 0), PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
                level.setBlockAndUpdate(normalPos.offset(x, y, 0), PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
            }
        }
        var ancient = (PufferfishConduitBlockEntity) level.getBlockEntity(pos);
        var normal = (PufferfishConduitBlockEntity) level.getBlockEntity(normalPos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.east().getBottomCenter());
        player.setNoGravity(true);
        level.addFreshEntity(player);
        var fish = EntityType.PUFFERFISH.create(level);
        fish.setPos(pos.west().getCenter());
        fish.setNoAi(true);
        fish.setNoGravity(true);
        level.addFreshEntity(fish);
        var distant = EntityType.PUFFERFISH.create(level);
        distant.setPos(pos.above(40).getCenter());
        distant.setNoAi(true);
        distant.setNoGravity(true);
        level.addFreshEntity(distant);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(ancient.isActive() && !normal.isActive(), "Ancient frame activated the wrong conduit");
            helper.assertTrue(PufferfishConduitSpawning.isInAncientRange(level, pos.north(31))
                    && !PufferfishConduitSpawning.isInAncientRange(level, pos.above(32)), "Ancient range is not thirty-two blocks");
            helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                    && fish.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Ancient conduit did not empower players and fish");
            helper.assertTrue(PufferfishConduitSpawning.isInRange(level, pos), "Ancient conduit did not register spawn replacement");
            var attacker = PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get().create(level);
            player.hurt(player.damageSources().mobAttack(attacker), 1);
            var curse = player.getEffect(PufferfishEffects.PUFFERFISH_CURSE);
            helper.assertTrue(curse != null, "Active ancient conduit did not enable curse conversion");
            int remaining = curse.getDuration();
            PufferfishConduitBlockEntity.applyPower(level, normalPos, 42);
            helper.assertTrue(curse.getDuration() == remaining && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER),
                    "Overlapping normal conduit extended curse or restored power");
            distant.removeEffect(PufferfishEffects.PUFFERFISH_POWER);
            PufferfishConduitBlockEntity.applyPower(level, pos, 42);
            helper.assertTrue(curse.getDuration() == 260, "Ancient conduit did not renew curse");
            helper.assertTrue(!distant.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Ancient conduit exceeded its fixed range");
            for (var offset : BlockPos.betweenClosed(-2, -2, -2, 2, 2, 2)) {
                int x = Math.abs(offset.getX()), y = Math.abs(offset.getY()), z = Math.abs(offset.getZ());
                if (x == 0 && (y == 2 || z == 2) || y == 0 && (x == 2 || z == 2) || z == 0 && (x == 2 || y == 2)) {
                    level.setBlockAndUpdate(pos.offset(offset), PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
                }
            }
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(ancient.isActive() && !PufferfishConduitSpawning.isInAncientRange(level, pos.above(32)),
                        "Frame enlarged the ancient conduit range");
                level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                helper.runAfterDelay(45, () -> {
                    helper.assertTrue(!ancient.isActive() && !PufferfishConduitSpawning.isInAncientRange(level, pos),
                            "Blocked water did not deactivate the ancient conduit");
                    level.setBlockAndUpdate(pos.above(), Blocks.WATER.defaultBlockState());
                    helper.runAfterDelay(45, () -> {
                        helper.assertTrue(ancient.isActive(), "Restored water did not reactivate the ancient conduit");
                        ancient.onChunkUnloaded();
                        helper.assertTrue(!PufferfishConduitSpawning.isInAncientRange(level, pos), "Unloaded ancient aura remained registered");
                        player.discard(); fish.discard(); distant.discard();
                        for (var center : java.util.List.of(pos, normalPos)) {
                            for (var block : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) level.removeBlock(block, false);
                        }
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void normalAndDistantCoresNeverEnableCurses(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 182, 2));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getCenter());
        var fish = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(level);
        level.setBlockAndUpdate(pos, PufferfishBlocks.CONDUIT.get().defaultBlockState());
        PufferfishConduitSpawning.update(level, pos, 96);
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(!player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Ordinary core enabled curses");
        level.setBlockAndUpdate(pos, PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState());
        PufferfishConduitSpawning.update(level, pos, 32);
        player.setPos(pos.above(32).getCenter());
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(!player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Distant ancient core enabled curses");
        player.setPos(pos.getCenter());
        level.removeBlock(pos, false);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                && !player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Removed ancient core still enabled curses");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ancientCorePersistsButCannotBeCraftedOrHarvested(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var state = PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var entity = (PufferfishConduitBlockEntity) level.getBlockEntity(pos);
        var saved = entity.saveWithFullMetadata(level.registryAccess());
        var restored = BlockEntity.loadStatic(pos, state, saved, level.registryAccess());
        helper.assertTrue(restored instanceof PufferfishConduitBlockEntity conduit && conduit.isAncient()
                && restored.isValidBlockState(state), "Ancient core identity did not survive saving");
        for (boolean silk : new boolean[] {false, true}) {
            var tool = new ItemStack(Items.NETHERITE_PICKAXE);
            if (silk) tool.enchant(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            helper.assertTrue(Block.getDrops(state, level, pos, entity, helper.makeMockPlayer(GameType.SURVIVAL), tool).isEmpty(),
                    "Ancient core dropped loot");
        }
        helper.assertTrue(level.getRecipeManager().getRecipes().stream().noneMatch(recipe ->
                recipe.value().getResultItem(level.registryAccess()).is(PufferfishBlocks.ANCIENT_CONDUIT.get().asItem())),
                "Ancient core has a crafting recipe");
        level.destroyBlock(pos, true);
        helper.succeed();
    }
}
