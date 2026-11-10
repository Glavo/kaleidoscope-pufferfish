// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
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
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fourStrokesRepairEveryMaterialAndFlailWithoutLosingComponents(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var variants = new java.util.ArrayList<ItemStack>();
        variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), net.minecraft.world.item.DyeColor.CYAN));
        PufferfishMobs.VARIANTS.values().forEach(variant -> variants.add(variant.fish().toStack()));
        PufferfishItems.FLAIL_VARIANTS.forEach(item -> variants.add(item.toStack()));
        var mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
        for (var fish : variants) {
            fish.enchant(mending, 1);
            fish.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Inflatable"));
            var expected = fish.copy();
            PufferfishDurability.activate(fish);
            fish.setDamageValue(fish.getMaxDamage() - 1);
            var pump = PufferfishItems.PUMP.toStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, pump);
            player.setItemInHand(InteractionHand.OFF_HAND, fish);
            PufferfishItems.PUMP.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            for (int stroke = 1; stroke <= 4; stroke++) {
                PufferfishItems.PUMP.get().onUseTick(helper.getLevel(), player, pump, 72000 - stroke * 20);
                helper.assertTrue(PufferfishDurability.deflationStage(fish) <= Math.max(0, 3 - stroke), "Stroke did not advance inflation");
            }
            helper.assertTrue(ItemStack.matches(fish, expected), "Pump lost enchantments, dye, name, or pristine stacking");
            helper.assertTrue(pump.getDamageValue() == 4 && !player.isUsingItem(), "Completed repair did not stop or spent wrong pump durability");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void incompleteStrokesAndInvalidTargetsCostNothing(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pump = PufferfishItems.PUMP.toStack();
        var fish = PufferfishDurabilityGameTests.worn(new ItemStack(Items.PUFFERFISH));
        player.setItemInHand(InteractionHand.MAIN_HAND, pump);
        player.setItemInHand(InteractionHand.OFF_HAND, fish);
        var item = PufferfishItems.PUMP.get();
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 20; tick++) item.onUseTick(helper.getLevel(), player, pump, 72000 - tick);
        player.stopUsingItem();
        helper.assertTrue(fish.getDamageValue() == 1 && !pump.isDamaged(), "Interrupted stroke repaired fish or consumed pump");
        for (var invalid : java.util.List.of(new ItemStack(Items.PUFFERFISH, 64), new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY)) {
            player.setItemInHand(InteractionHand.OFF_HAND, invalid);
            helper.assertTrue(!item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Pump accepted an invalid target");
        }
        player.setItemInHand(InteractionHand.OFF_HAND, fish);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        item.onUseTick(helper.getLevel(), player, pump, 71980);
        helper.assertTrue(!fish.isDamaged() && pump.getDamageValue() == 1, "Partial final stroke overcharged or failed to repair");
        item.onUseTick(helper.getLevel(), player, pump, 71960);
        helper.assertTrue(pump.getDamageValue() == 1, "Full fish consumed pump durability");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpCanBreakOnItsLastSuccessfulStroke(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pump = PufferfishItems.PUMP.toStack();
        pump.setDamageValue(pump.getMaxDamage() - 1);
        var fish = PufferfishDurabilityGameTests.worn(new ItemStack(Items.PUFFERFISH));
        player.setItemInHand(InteractionHand.MAIN_HAND, pump);
        player.setItemInHand(InteractionHand.OFF_HAND, fish);
        PufferfishItems.PUMP.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        PufferfishItems.PUMP.get().onUseTick(helper.getLevel(), player, pump, 71980);
        helper.assertTrue(pump.isEmpty() && !fish.isDamaged() && !player.isUsingItem(), "Last stroke failed to repair or break the pump");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpAcceptsUnbreakingButRejectsMendingInAnvil(GameTestHelper helper) {
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
            boolean allowed = key.equals(Enchantments.UNBREAKING);
            helper.assertTrue(!menu.getSlot(2).getItem().isEmpty() == allowed, "Wrong pump anvil enchantment support");
            helper.assertTrue(pump.isPrimaryItemFor(enchantment) == allowed, "Wrong pump enchanting table support");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pumpRecipeUsesCopperStickAndPufferfishSkin(GameTestHelper helper) {
        var empty = ItemStack.EMPTY;
        var copper = new ItemStack(Items.COPPER_INGOT);
        var input = CraftingInput.of(3, 3, java.util.List.of(empty, new ItemStack(Items.STICK), empty,
                copper, new ItemStack(PufferfishBlocks.skin(null)), copper, empty, copper, empty));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        var output = recipe.value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(PufferfishItems.PUMP.get()) && output.getMaxDamage() == 256 && output.getCount() == 1, "Wrong pump recipe output");
        helper.succeed();
    }
}
