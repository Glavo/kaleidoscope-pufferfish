// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestItems;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishPumpGameTests {
    @GameTest(template = "empty")
    public static void stagesAndGlovesRespectExactQuarterBoundaries(GameTestHelper helper) {
        var fish = new ItemStack(Items.PUFFERFISH);
        helper.assertTrue(PufferfishDurability.deflationStage(fish) == 0 && PufferfishDurability.canWearAsGlove(fish), "Pristine fish is deflated");
        PufferfishDurability.activate(fish);
        for (int damage = 0; damage < 64; damage++) {
            fish.setDamageValue(damage);
            helper.assertTrue(PufferfishDurability.deflationStage(fish) == damage / 16, "Wrong deflation boundary");
            helper.assertTrue(PufferfishDurability.canWearAsGlove(fish) == (damage <= 32), "Glove cutoff must be strictly below half durability");
        }
        var heavy = PufferfishItems.HEAVY.toStack();
        PufferfishDurability.activate(heavy);
        heavy.setDamageValue(1523);
        helper.assertTrue(PufferfishDurability.canWearAsGlove(heavy), "Odd maximum rounded glove cutoff down");
        heavy.setDamageValue(1524);
        helper.assertTrue(!PufferfishDurability.canWearAsGlove(heavy) && PufferfishDurability.deflationStage(heavy) == 2, "Odd maximum rounded glove cutoff up");
        for (var item : PufferfishItems.FLAIL_VARIANTS) {
            var flail = item.toStack();
            PufferfishDurability.activate(flail);
            flail.setDamageValue(flail.getMaxDamage() - 1);
            helper.assertTrue(flail.isDamaged() && PufferfishDurability.deflationStage(flail) == 0,
                    "Flail durability must not deflate its head");
        }
        helper.succeed();
    }

    private static PufferfishPumpBlockEntity placed(GameTestHelper helper) {
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, PufferfishBlocks.PUMP.get().defaultBlockState());
        return (PufferfishPumpBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void tenPercentStrokesRepairAllVariantsWithoutLosingComponents(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var variants = new java.util.ArrayList<ItemStack>();
        variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), net.minecraft.world.item.DyeColor.CYAN));
        PufferfishMobs.VARIANTS.values().forEach(variant -> variants.add(variant.fish().toStack()));
        var pumps = new java.util.ArrayList<PufferfishPumpBlockEntity>();
        var mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        for (var fish : variants) {
            fish.enchant(mending, 1);
            fish.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Inflatable"));
            PufferfishDurability.activate(fish);
            var worn = fish.copy();
            worn.setDamageValue(worn.getMaxDamage() - 1);
            var pump = new PufferfishPumpBlockEntity(helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2)), PufferfishBlocks.PUMP.get().defaultBlockState());
            pump.setLevel(helper.getLevel());
            pump.setFish(worn);
            pumps.add(pump);
        }
        for (int stroke = 1; stroke <= 10; stroke++) {
            final int completed = stroke;
            helper.runAfterDelay(stroke * 8L, () -> {
                for (int i = 0; i < pumps.size(); i++) {
                    var pump = pumps.get(i);
                    int oldDamage = pump.fish().getDamageValue();
                    int amount = (PufferfishDurability.maximum(variants.get(i)) + 9) / 10;
                    pump.press(player);
                    helper.assertTrue(pump.fish().getDamageValue() == Math.max(0, oldDamage - amount), "Stroke must restore ten percent, rounded up");
                    if (completed == 10) {
                        helper.assertTrue(ItemStack.matches(pump.fish(), variants.get(i)), "Repair lost fish components or durable state");
                        helper.assertTrue(ItemStack.matches(pump.pump(), PufferfishItems.PUMP.toStack()), "Inflation changed the reusable pump");
                    }
                }
                if (completed == 10) helper.succeed();
            });
        }
    }

    @GameTest(template = "empty")
    public static void landingPumpsOnceButWalkingAndDuplicateLandingsDoNot(GameTestHelper helper) {
        var pump = placed(helper);
        var fish = TestItems.worn(new ItemStack(Items.PUFFERFISH));
        fish.setDamageValue(60);
        pump.setFish(fish);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = pump.getBlockPos();
        var state = pump.getBlockState();
        PufferfishBlocks.PUMP.get().stepOn(helper.getLevel(), pos, state, player);
        PufferfishBlocks.PUMP.get().fallOn(helper.getLevel(), state, pos, player, 0);
        helper.assertTrue(pump.fish().getDamageValue() == 60, "Walking triggered inflation");
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        player.setOnGround(false);
        player.fallDistance = 1;
        player.setDeltaMovement(0, -1.5, 0);
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -1.5, 0));
        helper.assertTrue(player.onGround() && pump.fish().getDamageValue() == 53 && !pump.pump().isDamageableItem(),
                "Actual landing did not pump exactly once");
        PufferfishBlocks.PUMP.get().fallOn(helper.getLevel(), state, pos, player, 1);
        helper.assertTrue(pump.fish().getDamageValue() == 53, "Duplicate landing bypassed pedal rebound");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void insertionRemovalAndSavingPreserveBothStacks(GameTestHelper helper) {
        var pump = placed(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tool = PufferfishItems.PUMP.toStack();
        tool.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Workshop pump"));
        PufferfishBlocks.PUMP.get().setPlacedBy(helper.getLevel(), pump.getBlockPos(), pump.getBlockState(), player, tool);
        var fish = TestItems.worn(PufferfishItems.HEAVY.toStack());
        fish.setDamageValue(2000);
        var expected = fish.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, fish);
        var hit = new net.minecraft.world.phys.BlockHitResult(pump.getBlockPos().getCenter(), net.minecraft.core.Direction.UP, pump.getBlockPos(), false);
        pump.getBlockState().useItemOn(fish, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(fish.isEmpty() && ItemStack.matches(pump.fish(), expected), "Insertion lost or duplicated fish");
        var saved = pump.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored = new PufferfishPumpBlockEntity(pump.getBlockPos(), pump.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.matches(restored.fish(), expected) && ItemStack.matches(restored.pump(), tool), "Saving lost fish or pump components");
        pump.getBlockState().useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(pump.fish().isEmpty() && player.getInventory().items.stream().anyMatch(stack -> ItemStack.matches(stack, expected)), "Empty-hand retrieval failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingPumpDropsFishAndPreservesPumpName(GameTestHelper helper) {
        var pump = placed(helper);
        var tool = PufferfishItems.PUMP.toStack();
        tool.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Workshop pump"));
        pump.setPump(tool);
        var fish = TestItems.worn(PufferfishItems.DIAMOND.toStack());
        pump.setFish(fish);
        var area = new net.minecraft.world.phys.AABB(pump.getBlockPos()).inflate(2);
        helper.getLevel().destroyBlock(pump.getBlockPos(), true);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area);
        helper.assertTrue(drops.stream().filter(drop -> ItemStack.matches(drop.getItem(), tool)).count() == 1
                && drops.stream().filter(drop -> ItemStack.matches(drop.getItem(), fish)).count() == 1, "Breaking lost or duplicated stored items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void usingPumpPlacesBlockAndRetainsItemName(GameTestHelper helper) {
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(base, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = PufferfishItems.PUMP.toStack();
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Workshop pump"));
        var expected = stack.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atBottomCenterOf(base.above()),
                net.minecraft.core.Direction.UP, base, false);
        var result = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(result.consumesAction() && stack.isEmpty()
                && helper.getLevel().getBlockEntity(base.above()) instanceof PufferfishPumpBlockEntity pump
                && ItemStack.matches(pump.pump(), expected), "Placing the pump failed or lost its name");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpRejectsDurabilityEnchantmentsAndCopperRepair(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var registry = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (var key : java.util.List.of(Enchantments.UNBREAKING, Enchantments.MENDING)) {
            var enchantment = registry.getOrThrow(key);
            var pump = PufferfishItems.PUMP.toStack();
            var book = new ItemStack(Items.ENCHANTED_BOOK);
            book.enchant(enchantment, 1);
            var menu = new AnvilMenu(0, player.getInventory());
            menu.getSlot(0).set(pump);
            menu.getSlot(1).set(book);
            menu.createResult();
            helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Reusable pump accepted a durability enchantment");
            helper.assertTrue(!pump.isPrimaryItemFor(enchantment), "Reusable pump accepted enchanting table durability enchantments");
            helper.assertTrue(!pump.getItem().isValidRepairItem(pump, new ItemStack(Items.COPPER_INGOT)), "Reusable pump still accepts copper repair");
        }
        helper.succeed();
    }

    /// All fish and skin variants share the pump recipe and its fixed output.
    @GameTest(template = "empty")
    public static void pumpRecipeAcceptsFishOrSkinWithWoodenSlabsNeedleAndCopperBase(GameTestHelper helper) {
        var empty = ItemStack.EMPTY;
        var copper = new ItemStack(Items.COPPER_INGOT);
        var slabs = net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item.getDefaultInstance().is(net.minecraft.tags.ItemTags.WOODEN_SLABS)).toList();
        var skins = net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof PufferfishSkinBlock).toList();
        helper.assertTrue(!slabs.isEmpty() && !skins.isEmpty(), "Pump recipe ingredients are missing");
        var materials = new java.util.ArrayList<ItemStack>();
        skins.forEach(skin -> materials.add(new ItemStack(skin)));
        PufferfishVariant.ALL.forEach(variant -> materials.add(variant.fish()));
        var recipe = (net.minecraft.world.item.crafting.CraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(PufferfishPlease.id("pufferfish_pump")).orElseThrow().value();
        for (var slab : slabs) for (var material : materials) {
            var input = CraftingInput.of(3, 3, java.util.List.of(empty, new ItemStack(slab), empty,
                    empty, material, new ItemStack(Items.IRON_NUGGET), copper, copper, copper));
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "Pump recipe rejected a wooden slab, fish or skin variant");
            var output = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(output.is(PufferfishItems.PUMP.get()) && !output.isDamageableItem() && !output.isBarVisible()
                    && output.getMaxStackSize() == 1 && output.getCount() == 1, "Wrong pump recipe output");
        }
        helper.succeed();
    }
}
