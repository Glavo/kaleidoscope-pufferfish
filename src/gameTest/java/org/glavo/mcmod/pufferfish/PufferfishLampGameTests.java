// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestEffects;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishLampGameTests {
    @GameTest(template = "empty")
    public static void everyFishCraftsOnlyItsMatchingLamp(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(PufferfishLamps.VARIANTS.size() == 30, "Missing lamp variants");
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            var fish = variant.fish().get();
            var cultivation = new PufferfishCultivation(PufferfishLevel.LEVEL_5, 0.75, 123, 456);
            for (boolean reversed : new boolean[] {false, true}) {
                if (reversed) {
                    PufferfishDurability.activate(fish);
                    fish.set(PufferfishPlease.CULTIVATION.get(), cultivation);
                }
                var torch = new ItemStack(Items.TORCH);
                var input = CraftingInput.of(2, 1, reversed ? List.of(torch, fish) : List.of(fish, torch));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
                var result = recipe.assemble(input, level.registryAccess());
                helper.assertTrue(result.is(variant.block().get().asItem()) && result.getCount() == 1,
                        "Lamp recipe lost the fish variant");
                helper.assertTrue(java.util.Objects.equals(result.get(PufferfishPlease.CULTIVATION.get()), reversed ? cultivation : null)
                        && !result.has(DataComponents.MAX_DAMAGE), "Lamp lost cultivation or inherited weapon durability");
                helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Lamp recipe returned ingredients");
            }
        }
        for (var invalid : List.of(new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(Items.COD),
                new ItemStack(PufferfishItems.FLAIL.get()), new ItemStack(PufferfishItems.SASHIMI.get()))) {
            helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                    CraftingInput.of(2, 1, List.of(new ItemStack(Items.TORCH), invalid)), level).isEmpty(), "Lamp accepted an invalid fish");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lampsPlaceUnderwaterSaveAndDropTheirVariant(GameTestHelper helper) {
        var level = helper.getLevel();
        var relative = new BlockPos(2, 2, 2);
        var pos = helper.absolutePos(relative);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(90);
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            helper.setBlock(relative.below(), Blocks.STONE);
            helper.setBlock(relative, Blocks.WATER);
            var stack = new ItemStack(variant.block().get(), 2);
            var cultivation = new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, 123, 456);
            stack.set(PufferfishPlease.CULTIVATION.get(), cultivation);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
            helper.assertTrue(((BlockItem) stack.getItem()).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Lamp placement failed");
            var state = level.getBlockState(pos);
            int copper = CopperPufferfish.index(variant.fish().get());
            int light = copper < 0 ? 15 : new int[] {15, 12, 8, 4}[copper % 4];
            helper.assertTrue(state.is(variant.block().get()) && state.getLightEmission(level, pos) == light
                    && state.getFluidState().getType() == Fluids.WATER && stack.getCount() == 1, "Lamp placement or light was incorrect");
            var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            var expected = variant.fish().get();
            expected.set(PufferfishPlease.CULTIVATION.get(), cultivation);
            helper.assertTrue(ItemStack.isSameItemSameComponents(entity.fish(), expected), "Lamp lost its variant or cultivation on placement");
            var saved = entity.saveWithFullMetadata(level.registryAccess());
            var restored = (PlacedPufferfishBlockEntity) BlockEntity.loadStatic(pos, state, saved, level.registryAccess());
            helper.assertTrue(restored != null && ItemStack.matches(entity.fish(), restored.fish()), "Lamp lost its fish on reload");
            var drops = Block.getDrops(state, level, pos, entity);
            helper.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.getFirst(), stack)
                    && ItemStack.isSameItemSameComponents(state.getBlock().getCloneItemStack(level, pos, state), stack),
                    "Lamp lost its variant or cultivation when dropped or cloned");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperLampWaxingPreservesLightAndWater(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, PufferfishLamps.VARIANTS.get("oxidized_copper_pufferfish_lamp").block().get().defaultBlockState()
                .setValue(PlacedPufferfishBlock.WATERLOGGED, true).setValue(PlacedPufferfishBlock.FACING, Direction.EAST));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tools = List.of(Items.HONEYCOMB, Items.IRON_AXE, Items.IRON_AXE);
        for (int step = 0; step < tools.size(); step++) {
            var item = tools.get(step);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
            var state = level.getBlockState(pos);
            var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(state.getLightEmission(level, pos) == (step == 2 ? 8 : 4) && state.getValue(PlacedPufferfishBlock.WATERLOGGED)
                    && state.getValue(PlacedPufferfishBlock.FACING) == Direction.EAST, "Waxing changed lamp properties");
            helper.assertTrue(PufferfishLamps.lamp(entity.fish()).is(state.getBlock().asItem()), "Lamp block and fish variant disagree");
        }
        helper.assertTrue(level.getBlockState(pos).is(PufferfishLamps.VARIANTS.get("weathered_copper_pufferfish_lamp").block().get()), "Lamp scraping failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperLampsDimAsTheyOxidizeAndWaxStopsOxidation(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var random = RandomSource.create(42);
        level.setBlockAndUpdate(pos, PufferfishLamps.VARIANTS.get("copper_pufferfish_lamp").block().get().defaultBlockState());
        for (int stage = 1; stage <= 3; stage++) {
            var previous = level.getBlockState(pos);
            for (int attempt = 0; attempt < 10000 && level.getBlockState(pos) == previous; attempt++) {
                previous.randomTick(level, pos, random);
            }
            var state = level.getBlockState(pos);
            var fish = ((PlacedPufferfishBlockEntity) level.getBlockEntity(pos)).fish();
            helper.assertTrue(CopperPufferfish.index(fish) == stage && state.getLightEmission(level, pos) == new int[] {15, 12, 8, 4}[stage],
                    "Natural oxidation did not dim the lamp");
        }
        for (int stage = 4; stage < 8; stage++) {
            var lamp = (BlockItem) PufferfishLamps.lamp(new ItemStack(PufferfishItems.COPPER_VARIANTS.get(stage).get())).getItem();
            var state = lamp.getBlock().defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            for (int attempt = 0; attempt < 1000; attempt++) state.randomTick(level, pos, random);
            helper.assertTrue(level.getBlockState(pos) == state, "Waxed lamp oxidized");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void heavyLampFallsPoisonsAndPreservesItsData(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(2, 6, 2);
        var landing = new BlockPos(2, 1, 2);
        var block = PufferfishLamps.VARIANTS.get("heavy_pufferfish_lamp").block().get();
        var state = block.defaultBlockState().setValue(PlacedPufferfishBlock.FACING, Direction.EAST);
        helper.setBlock(landing.below(), Blocks.STONE);
        helper.setBlock(origin.below(), Blocks.STONE);
        helper.setBlock(origin, state);
        var fish = new ItemStack(PufferfishItems.HEAVY.get());
        fish.set(DataComponents.CUSTOM_NAME, Component.literal("Falling lamp"));
        ((PlacedPufferfishBlockEntity) level.getBlockEntity(helper.absolutePos(origin))).setFish(fish);
        var target = helper.spawn(EntityType.COW, 2.5F, 1, 2.5F);
        target.setNoAi(true);
        helper.runAfterDelay(5, () -> {
            helper.assertBlockPresent(block, origin);
            helper.setBlock(origin.below(), Blocks.AIR);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertBlockPresent(Blocks.AIR, origin);
            helper.assertBlockPresent(block, landing);
            var pos = helper.absolutePos(landing);
            var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(ItemStack.matches(entity.fish(), fish) && level.getBlockState(pos) == state,
                    "Falling lamp lost its fish or orientation");
            var poison = TestEffects.poison(target);
            helper.assertTrue(target.getHealth() < target.getMaxHealth() && poison != null && poison.getAmplifier() == 1,
                    "Falling lamp did not damage and poison its target");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void heavyLampUsesAnvilDamageAndPoisonsOnlyDamagedTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var lamp = PufferfishLamps.VARIANTS.get("heavy_pufferfish_lamp").block().get();
        for (var block : List.of(lamp, PufferfishBlocks.PLACED_HEAVY_FISH.get(), Blocks.ANVIL)) {
            for (float distance : new float[] {0.5F, 5, 50}) {
                var falling = FallingBlockEntity.fall(level, pos, block.defaultBlockState());
                falling.setHurtsEntities(2, 40);
                var target = helper.spawn(EntityType.COW, 2.5F, 2, 2.5F);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                target.setHealth(100);
                var immune = helper.spawn(EntityType.COW, 2.5F, 2, 2.5F);
                immune.setInvulnerable(true);
                falling.causeFallDamage(distance, 1, level.damageSources().fall());
                float damage = distance < 1 ? 0 : distance == 5 ? 8 : 40;
                helper.assertTrue(target.getHealth() == 100 - damage, "Incorrect falling lamp damage or damage cap");
                var poison = TestEffects.poison(target);
                if (block instanceof HeavyPufferfishBlock && damage > 0) {
                    helper.assertTrue(poison != null && poison.getAmplifier() == 1 && poison.getDuration() == 120, "Incorrect impact poison");
                    var source = target.getLastDamageSource();
                    helper.assertTrue(source != null && source.getLocalizedDeathMessage(helper.makeMockPlayer(GameType.SURVIVAL)).getContents()
                            instanceof TranslatableContents message && message.getKey().equals("death.attack.pufferfish_please.falling_pufferfish"),
                            "Heavy fish or lamp used the anvil death message");
                } else helper.assertTrue(poison == null, "Non-damaging fall or vanilla anvil applied poison");
                helper.assertTrue(!TestEffects.hasPoison(immune), "Invulnerable target was poisoned");
                target.discard();
                immune.discard();
                falling.discard();
            }
        }
        helper.succeed();
    }
}
