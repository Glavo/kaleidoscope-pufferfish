// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

/// Verifies golden pufferfish crafting, food effects, melee, and headgear without throwing.
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class GoldenPufferfishGameTests {
    private GoldenPufferfishGameTests() {}

    /// Builds a crafting grid with the supplied fish surrounded by eight gold ingots.
    private static CraftingInput input(ItemStack fish) {
        var items = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) items.add(i == 4 ? fish : new ItemStack(Items.GOLD_INGOT));
        return CraftingInput.of(3, 3, items);
    }

    /// Checks that all ordinary fish colors craft one golden fish and that the full ingot ring is required.
    @GameTest(template = "empty")
    public static void goldIngotRingCraftsOneGoldenPufferfish(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var variants = new ArrayList<ItemStack>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        for (var fish : variants) {
            var grid = input(fish);
            var recipe = manager.getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow().value();
            var result = recipe.assemble(grid, helper.getLevel().registryAccess());
            helper.assertTrue(ItemStack.matches(result, new ItemStack(PufferfishItems.GOLDEN.get())), "Wrong golden pufferfish recipe output");
            helper.assertTrue(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty)
                    && grid.items().stream().allMatch(stack -> stack.getCount() == 1), "Golden recipe returned or mutated ingredients");
        }
        for (int slot = 0; slot < 9; slot++) {
            var items = new ArrayList<>(input(new ItemStack(Items.PUFFERFISH)).items());
            items.set(slot, ItemStack.EMPTY);
            helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, items), helper.getLevel()).isEmpty(),
                    "Golden recipe accepted a missing ingredient");
        }
        for (var fish : List.of(Items.COD, Items.PUFFERFISH_BUCKET, PufferfishItems.NETHERITE.get(), PufferfishItems.GOLDEN.get())) {
            helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, input(new ItemStack(fish)), helper.getLevel()).isEmpty(),
                    "Golden recipe accepted an invalid center ingredient");
        }
        var nuggets = new ArrayList<>(Collections.nCopies(9, new ItemStack(Items.GOLD_NUGGET)));
        nuggets.set(4, new ItemStack(Items.PUFFERFISH));
        helper.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, nuggets), helper.getLevel()).isEmpty(),
                "Golden recipe accepted nuggets instead of ingots");
        helper.succeed();
    }

    /// Verifies golden apple nutrition, eating at full hunger, and the exact four food effects.
    @GameTest(template = "empty")
    public static void eatingAppliesGoldenAppleWaterBreathingAndPoison(GameTestHelper helper) {
        for (int hunger : new int[] {10, 20}) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getFoodData().setFoodLevel(hunger);
            player.getFoodData().setSaturation(0);
            var stack = new ItemStack(PufferfishItems.GOLDEN.get(), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var food = stack.getFoodProperties(player);
            helper.assertTrue(food != null && food.nutrition() == Foods.GOLDEN_APPLE.nutrition()
                    && food.saturation() == Foods.GOLDEN_APPLE.saturation() && food.canAlwaysEat()
                    && food.eatDurationTicks() == Foods.GOLDEN_APPLE.eatDurationTicks(), "Golden fish differs from golden apple food properties");
            helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction()
                    && player.isUsingItem(), "Golden fish could not be eaten at the current hunger level");
            stack.finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(stack.getCount() == 1 && player.getFoodData().getFoodLevel() == Math.min(20, hunger + 4)
                    && Math.abs(player.getFoodData().getSaturationLevel() - 9.6F) < 0.001F, "Eating consumed the wrong amount or restored incorrect food values");
            effect(helper, player, MobEffects.REGENERATION, 100, 1);
            effect(helper, player, MobEffects.ABSORPTION, 2400, 0);
            effect(helper, player, MobEffects.WATER_BREATHING, 9600, 0);
            effect(helper, player, MobEffects.POISON, 1200, 1);
            helper.assertTrue(player.getActiveEffects().size() == 4 && player.getAbsorptionAmount() == 4,
                    "Golden fish applied unexpected effects or absorption");
        }
        helper.succeed();
    }

    /// Checks an effect's duration in ticks and zero-based amplifier.
    private static void effect(GameTestHelper helper, Player player, Holder<MobEffect> type, int ticks, int amplifier) {
        var effect = player.getEffect(type);
        helper.assertTrue(effect != null && effect.getDuration() == ticks && effect.getAmplifier() == amplifier,
                "Incorrect golden pufferfish effect: " + type.getRegisteredName());
    }

    /// Verifies right-click remains edible while melee and headgear use the fish mechanics.
    @GameTest(template = "empty")
    public static void goldenPufferfishCanAttackAndEquipWithoutThrowing(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(PufferfishItems.GOLDEN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled() && stack.getCount() == 1
                && helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, player.getBoundingBox().inflate(4)).isEmpty(),
                "Golden pufferfish use was intercepted for throwing");
        player.tick();
        helper.assertTrue(PufferfishItems.isWeapon(stack) && !PufferfishItems.canThrow(stack) && !PufferfishColors.supports(stack)
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 2 && player.getAttributeValue(Attributes.ATTACK_SPEED) == 3,
                "Golden pufferfish did not receive golden pickaxe damage and ordinary fish speed");
        helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction()
                && player.isUsingItem(), "Golden pufferfish right-click did not start eating");
        player.stopUsingItem();
        helper.assertTrue(player.getEquipmentSlotForItem(stack) == EquipmentSlot.HEAD
                && player.inventoryMenu.getSlot(5).mayPlace(stack), "Golden pufferfish could not be equipped on the head");
        player.setItemSlot(EquipmentSlot.HEAD, stack);
        PufferfishHeadgear.breathe(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        helper.assertTrue(PufferfishHeadgear.isWearing(player) && player.getEffect(MobEffects.WATER_BREATHING).getDuration() == 200,
                "Golden pufferfish did not grant the breathing reserve");
        helper.succeed();
    }
}
