// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.CuttingAssertions;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class FishProcessingTableGameTests {
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void completedDowngradesDropExactExperienceOnceAndSkinsDropNone(GameTestHelper helper) {
        var table = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        var fish = new ItemStack(Items.PUFFERFISH);
        PufferfishDurability.activate(fish);
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.5, 83, Long.MAX_VALUE));
        table.insert(fish);
        var bounds = new net.minecraft.world.phys.AABB(table.getBlockPos()).inflate(4);
        for (int stroke = 0; stroke < 3; stroke++) {
            int current = stroke;
            helper.runAfterDelay(1 + stroke * 10, () -> {
                table.brush(player, InteractionHand.MAIN_HAND);
                table.brush(player, InteractionHand.MAIN_HAND);
                var orbs = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, bounds);
                helper.assertTrue(current < 2 ? orbs.isEmpty() : orbs.size() == 1 && orbs.getFirst().getValue() == 60,
                        "Partial or repeated strokes duplicated experience, or the refund was incorrect");
                if (current == 2) orbs.forEach(net.minecraft.world.entity.Entity::discard);
            });
        }
        for (int stroke = 0; stroke < 3; stroke++) {
            helper.runAfterDelay(41 + stroke * 10, () -> {
                table.brush(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, bounds).isEmpty(),
                        "Rank one refunded an awakening cost or previously refunded XP");
            });
        }
        helper.runAfterDelay(70, () -> {
            table.removeItem();
            var skin = new ItemStack(PufferfishBlocks.RADIANT_SKIN.get());
            PufferfishSkinBlock.setLevel(skin, PufferfishLevel.LEVEL_10);
            table.insert(skin);
        });
        for (int stroke = 0; stroke < 3; stroke++) {
            int current = stroke;
            helper.runAfterDelay(71 + stroke * 10, () -> {
                table.brush(player, InteractionHand.MAIN_HAND);
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, bounds).isEmpty(), "Skin generated experience without cultivation data");
                if (current == 2) {
                    helper.assertTrue(table.item().get(PufferfishPlease.SKIN_LEVEL.get()) == PufferfishLevel.LEVEL_9, "Skin did not lose a rank");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void largeRefundsUsePersistentMergedOrbsAndSupportMending(GameTestHelper helper) {
        var table = place(helper);
        var player = org.glavo.mcmod.pufferfish.test.TestPlayers.serverPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        var fish = new ItemStack(Items.PUFFERFISH);
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0, 0, 0));
        table.insert(fish);
        for (int stroke = 0; stroke < 3; stroke++) {
            int current = stroke;
            helper.runAfterDelay(1 + stroke * 10, () -> {
                table.brush(player, InteractionHand.MAIN_HAND);
                if (current != 2) return;
                var orbs = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
                        new net.minecraft.world.phys.AABB(table.getBlockPos()).inflate(2));
                helper.assertTrue(orbs.size() <= 2 && !orbs.isEmpty(), "Large refunds spawned excessive orb entities");
                var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                pickaxe.setDamageValue(100);
                pickaxe.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
                long total = 0;
                for (var orb : orbs) {
                    var saved = new net.minecraft.nbt.CompoundTag();
                    orb.addAdditionalSaveData(saved);
                    total += (long) saved.getShort("Value") * saved.getInt("Count");
                    var restored = new net.minecraft.world.entity.ExperienceOrb(helper.getLevel(), 0, 0, 0, 0);
                    restored.readAdditionalSaveData(saved);
                    helper.assertTrue(restored.getValue() > 0 && restored.getValue() <= Short.MAX_VALUE, "Orb value did not survive vanilla persistence");
                    for (int count = saved.getInt("Count"); count > 0; count--) {
                        player.takeXpDelay = 0;
                        restored.playerTouch(player);
                    }
                    helper.assertTrue(restored.isRemoved(), "Merged orb did not finish after its stored pickup count");
                    orb.discard();
                }
                helper.assertTrue(total == 5_000_000 && player.totalExperience == 4_999_950 && !pickaxe.isDamaged(),
                        "Refund was truncated, failed to reach the player, or bypassed Mending");
                helper.succeed();
            });
        }
    }

    private static FishProcessingTableBlockEntity place(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, PufferfishBlocks.PROCESSING_TABLE.get());
        return (FishProcessingTableBlockEntity) helper.getBlockEntity(pos);
    }

    @GameTest(template = "empty")
    public static void processingRecipesPreserveOnlySkinRankForEveryVariant(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (var variant : PufferfishVariant.ALL) {
            var fish = CuttingAssertions.rankedFish(variant, 1);
            var input = new FishProcessingRecipe.Input(fish, new ItemStack(Items.SHEARS));
            var recipe = recipes.getRecipeFor(PufferfishPlease.FISH_PROCESSING_TYPE.get(), input, helper.getLevel()).orElseThrow().value();
            var results = recipe.process(fish);
            var expectedSkin = new ItemStack(variant.skin());
            PufferfishSkinBlock.setLevel(expectedSkin, PufferfishLevel.LEVEL_10);
            helper.assertTrue(results.size() == 2 && ItemStack.matches(results.get(0), expectedSkin)
                    && ItemStack.matches(results.get(1), PufferfishItems.SASHIMI.toStack()), "Processing must preserve rank only on the matching skin");
            results.getFirst().setCount(30);
            helper.assertTrue(recipe.process(fish).getFirst().getCount() == 1, "Processing mutated the shared recipe result");
            helper.assertTrue(!recipe.matches(new FishProcessingRecipe.Input(fish, new ItemStack(Items.STICK)), helper.getLevel()), "Invalid tool matched");
            var encoded = FishProcessingRecipe.CODEC.codec().encodeStart(helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), recipe).getOrThrow();
            var decoded = FishProcessingRecipe.CODEC.codec().parse(helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), encoded).getOrThrow();
            helper.assertTrue(decoded.matches(input, helper.getLevel()) && ItemStack.matches(decoded.process(fish).getFirst(), expectedSkin), "Recipe round-trip lost its contract");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void cuttingSurvivesReloadAndCancellationPreservesTheInput(GameTestHelper helper) {
        var table = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var fish = CuttingAssertions.rankedFish(PufferfishVariant.RADIANT, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        helper.assertTrue(!table.insert(new ItemStack(Items.PUFFERFISH_BUCKET)) && !table.insert(PufferfishItems.FLAIL.toStack()), "Unsupported derivative entered the table");
        helper.assertTrue(table.insert(fish) && !table.insert(fish), "Table accepted a second input");
        table.cut(player, InteractionHand.MAIN_HAND);
        var returned = table.removeItem();
        helper.assertTrue(ItemStack.matches(returned, fish), "Cancelled processing changed the individual fish");
        table.insert(returned);
        table.cut(player, InteractionHand.MAIN_HAND);
        table.cut(player, InteractionHand.MAIN_HAND);
        var saved = table.saveWithFullMetadata(helper.getLevel().registryAccess());
        var pos = table.getBlockPos();
        helper.getLevel().removeBlockEntity(pos);
        var reloaded = new FishProcessingTableBlockEntity(pos, table.getBlockState());
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(reloaded);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reloaded.cut(player, InteractionHand.MAIN_HAND) && !reloaded.item().isEmpty(), "Duplicate or cancelled strokes completed the recipe early");
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reloaded.cut(player, InteractionHand.MAIN_HAND) && reloaded.item().isEmpty(), "Reloaded processing failed");
            CuttingAssertions.verifyDrops(helper, pos.above(), PufferfishBlocks.RADIANT_SKIN.get(), PufferfishLevel.LEVEL_10);
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 1 && !reloaded.cut(player, InteractionHand.MAIN_HAND), "Processing charged the wrong tool wear or duplicated outputs");
            reloaded.insert(fish);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2));
            helper.assertTrue(drops.size() == 1 && ItemStack.matches(drops.getFirst().getItem(), fish), "Breaking the table lost its input");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 450)
    public static void brushingLowersOneRankAtATimeAndRestoresStacking(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        var tables = new ArrayList<FishProcessingTableBlockEntity>();
        var originals = new ArrayList<ItemStack>();
        for (var variant : PufferfishVariant.ALL) for (boolean skin : new boolean[] {false, true}) {
            var original = skin ? new ItemStack(variant.skin()) : variant.fish();
            originals.add(original.copy());
            if (skin) PufferfishSkinBlock.setLevel(original, PufferfishLevel.LEVEL_10);
            else {
                PufferfishDurability.activate(original);
                original.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.7, 20_000_000, 80_000_000));
            }
            var table = new FishProcessingTableBlockEntity(helper.absolutePos(new BlockPos(2, 2, 2)), PufferfishBlocks.PROCESSING_TABLE.get().defaultBlockState());
            table.setLevel(helper.getLevel());
            table.insert(original);
            tables.add(table);
        }
        for (int step = 0; step < 10; step++) {
            int expected = 9 - step;
            for (int stroke = 0; stroke < 3; stroke++) {
                int currentStroke = stroke;
                helper.runAfterDelay(1 + step * 40 + stroke * 10, () -> {
                    for (int i = 0; i < tables.size(); i++) {
                        var table = tables.get(i);
                        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
                        helper.assertTrue(table.brush(player, InteractionHand.MAIN_HAND), "Brush refused a valid rank");
                        table.brush(player, InteractionHand.MAIN_HAND);
                        var result = table.item();
                        int rank = PufferfishSkinBlock.isSkin(result) ? result.getOrDefault(PufferfishPlease.SKIN_LEVEL.get(), PufferfishLevel.LEVEL_0).number()
                                : result.has(PufferfishPlease.CULTIVATION.get()) ? result.get(PufferfishPlease.CULTIVATION.get()).level().number() : 0;
                        helper.assertTrue(rank == (currentStroke == 2 ? expected : expected + 1), "Brush skipped a rank or bypassed its cadence");
                        if (currentStroke == 2 && expected > 0 && !PufferfishSkinBlock.isSkin(result)) {
                            var data = result.get(PufferfishPlease.CULTIVATION.get());
                            helper.assertTrue(data.experience() == 0 && data.totalExperience() == 80_000_000 && data.talent() == 0.7, "Rank loss changed lifetime data or retained current experience");
                        }
                        if (currentStroke == 2 && expected == 0) {
                            helper.assertTrue(ItemStack.matches(result, originals.get(i)) && result.getMaxStackSize() == 64, "Final brushing failed to restore an ordinary stackable item");
                        }
                    }
                    if (currentStroke == 2 && expected == 0) helper.succeed();
                });
            }
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void restorationNeverRepairsDisenchantsOrOverwritesForeignDurability(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        var tables = new ArrayList<FishProcessingTableBlockEntity>();
        var inputs = new ArrayList<ItemStack>();
        for (int kind = 0; kind < 4; kind++) {
            var fish = PufferfishVariant.COPPER.get(6).fish();
            PufferfishDurability.activate(fish);
            if (kind == 0) fish.setDamageValue(5);
            if (kind == 1) fish.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING), 1);
            if (kind == 2) fish.set(DataComponents.MAX_DAMAGE, 9999);
            if (kind == 3) {
                fish.remove(PufferfishPlease.MANAGED_DURABILITY.get());
                fish.remove(DataComponents.MAX_DAMAGE);
                fish.remove(DataComponents.DAMAGE);
                fish.set(DataComponents.MAX_STACK_SIZE, 16);
            }
            fish.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Keepsake"));
            inputs.add(fish.copy());
            fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.5, 10, 100));
            var table = new FishProcessingTableBlockEntity(helper.absolutePos(new BlockPos(2, 2, 2)), PufferfishBlocks.PROCESSING_TABLE.get().defaultBlockState());
            table.setLevel(helper.getLevel());
            table.insert(fish);
            tables.add(table);
        }
        for (int stroke = 0; stroke < 3; stroke++) {
            int current = stroke;
            helper.runAfterDelay(1 + stroke * 10, () -> {
                for (int i = 0; i < tables.size(); i++) {
                    tables.get(i).brush(player, InteractionHand.MAIN_HAND);
                    if (current == 2) helper.assertTrue(ItemStack.matches(tables.get(i).item(), inputs.get(i)) && !tables.get(i).canBrush(player), "Restoration repaired, disenchanted or overwrote foreign data");
                }
            });
        }
        helper.runAfterDelay(50, () -> {
            var repaired = tables.getFirst().removeItem();
            repaired.setDamageValue(0);
            tables.getFirst().insert(repaired);
            helper.assertTrue(tables.getFirst().canBrush(player), "Repaired rank-zero fish cannot be restored");
            tables.getFirst().brush(player, InteractionHand.MAIN_HAND);
            tables.getFirst().removeItem();
            tables.getFirst().insert(repaired);
        });
        for (int stroke = 0; stroke < 3; stroke++) {
            int current = stroke;
            helper.runAfterDelay(60 + stroke * 10, () -> {
                tables.getFirst().brush(player, InteractionHand.MAIN_HAND);
                helper.assertTrue((tables.getFirst().item().getMaxStackSize() == 64) == (current == 2), "Taking the input out did not reset brush progress");
                if (current == 2) {
                    helper.assertTrue(tables.getFirst().item().getHoverName().getString().equals("Keepsake"), "Restoration erased the custom name");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void heldBrushUsesTheTargetedTableAndStopsWhenLookingAway(GameTestHelper helper) {
        var table = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = table.getBlockPos();
        var fish = new ItemStack(Items.PUFFERFISH);
        PufferfishDurability.activate(fish);
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.4, 300, 900));
        player.setItemInHand(InteractionHand.MAIN_HAND, fish.copyWithCount(2));
        var hit = new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false);
        var block = PufferfishBlocks.PROCESSING_TABLE.get();
        block.useItemOn(player.getMainHandItem(), table.getBlockState(), helper.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && ItemStack.matches(table.item(), fish), "Insertion must split exactly one fish");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BRUSH));
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 2.5);
        player.setYRot(180);
        player.setXRot((float) Math.toDegrees(Math.atan2(player.getEyeY() - (pos.getY() + 0.75), 2)));
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
        player.yHeadRot = player.yHeadRotO = player.getYRot();
        block.useItemOn(player.getMainHandItem(), table.getBlockState(), helper.getLevel(), pos, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(player.isUsingItem(), "Brush interaction did not begin continuous use");
        for (int stroke = 0; stroke < 3; stroke++) {
            int current = stroke;
            helper.runAfterDelay(1 + stroke * 10, () -> {
                var target = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnViewVector(player,
                        entity -> !entity.isSpectator() && entity.isPickable(), player.blockInteractionRange());
                helper.assertTrue(target instanceof net.minecraft.world.phys.BlockHitResult b && b.getBlockPos().equals(pos)
                        && target.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK,
                        "Brush fixture missed the table: " + target.getType() + " at " + target.getLocation());
                helper.assertTrue(helper.getLevel().mayInteract(player, pos) && player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, player.getMainHandItem()), "Brush fixture is protected");
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick(
                        player, player.getMainHandItem(), 196 - current * 10));
                helper.assertTrue(table.item().get(PufferfishPlease.CULTIVATION.get()).level() == (current == 2 ? PufferfishLevel.LEVEL_1 : PufferfishLevel.LEVEL_2),
                        "Held brush did not follow the vanilla stroke cadence at stroke " + current + ": " + table.item());
            });
        }
        helper.runAfterDelay(50, () -> {
            player.setXRot(-90);
            player.xRotO = player.getXRot();
            for (int duration : new int[] {146, 136, 126}) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick(player, player.getMainHandItem(), duration));
            helper.assertTrue(table.item().get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_1 && player.getMainHandItem().getDamageValue() == 1,
                    "Brush continued processing after losing its target");
            helper.succeed();
        });
    }
}
