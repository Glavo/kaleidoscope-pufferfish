// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;

/// Exercises flail crafting, combat, launch ownership, and item conservation.
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishFlailGameTests {
    /// Prevents construction of the test holder.
    private PufferfishFlailGameTests() {}

    /// Creates a stationary survival player holding the supplied weapon above the fixture floor.
    private static Player player(GameTestHelper helper, ItemStack stack) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2))));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setNoGravity(true);
        return player;
    }

    /// Constructs the requested diagonal recipe or its horizontal mirror.
    private static CraftingInput input(ItemStack fish, boolean mirrored) {
        var items = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        items.set(mirrored ? 0 : 2, fish);
        items.set(4, new ItemStack(Items.CHAIN));
        items.set(mirrored ? 8 : 6, new ItemStack(Items.CHAIN));
        return CraftingInput.of(3, 3, items);
    }

    /// Verifies both recipes, all dye colors, component preservation, and invalid inputs.
    @GameTest(template = "empty")
    public static void diagonalRecipesPreserveFishComponents(GameTestHelper helper) {
        var variants = new ArrayList<ItemStack>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        variants.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PUNCH);
        for (var fish : variants) {
            fish.setCount(8);
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Meteor"));
            EnchantmentHelper.updateEnchantments(fish, values -> values.set(enchantment, 2));
            var original = fish.copy();
            for (boolean mirrored : new boolean[] {false, true}) {
                var grid = input(fish, mirrored);
                var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel()).orElseThrow().value();
                var result = recipe.assemble(grid, helper.getLevel().registryAccess());
                helper.assertTrue(result.is(PufferfishItems.isNetherite(fish) ? PufferfishItems.NETHERITE_FLAIL.get() : PufferfishItems.FLAIL.get())
                        && result.getCount() == 1 && result.getMaxStackSize() == 1, "Wrong flail recipe output");
                helper.assertTrue(result.get(KaleidoscopePufferfish.COLOR.get()) == PufferfishColors.get(fish)
                        && result.getHoverName().equals(fish.getHoverName()) && result.getEnchantments().getLevel(enchantment) == 2,
                        "Flail recipe lost fish components");
                helper.assertTrue(!result.has(DataComponents.ITEM_NAME), "Flail retained a generated fish name");
                helper.assertTrue(ItemStack.matches(original, fish) && recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                        "Flail recipe mutated its ingredient or duplicated materials");
                helper.assertTrue(!recipe.canCraftInDimensions(2, 2), "Flail recipe fitted a player crafting grid");
            }
        }
        for (var wrong : List.of(new ItemStack(Items.COD), new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(PufferfishItems.FLAIL.get()))) {
            helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input(wrong, false), helper.getLevel()).isEmpty(),
                    "Flail recipe accepted an invalid fish ingredient");
        }
        var invalid = new ArrayList<>(input(new ItemStack(Items.PUFFERFISH), false).items());
        invalid.set(0, new ItemStack(Items.CHAIN));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, invalid), helper.getLevel()).isEmpty(),
                "Flail recipe accepted an extra ingredient");
        helper.succeed();
    }

    /// Verifies melee damage and poison without treating a flail as wearable fish or engine fuel.
    @GameTest(template = "empty")
    public static void flailsDealMeleeDamageWithoutFishEquipmentBehavior(GameTestHelper helper) {
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get())) {
            var stack = new ItemStack(item);
            var player = player(helper, stack);
            for (int i = 0; i < 22; i++) player.tick();
            player.setOnGround(true);
            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 3, 3));
            target.setNoAi(true);
            boolean netherite = item == PufferfishItems.NETHERITE_FLAIL.get();
            helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == (netherite ? 12 : 6)
                    && player.getAttributeValue(Attributes.ATTACK_SPEED) == 1, "Incorrect flail melee attributes");
            player.attack(target);
            var poison = target.getEffect(MobEffects.POISON);
            helper.assertTrue(target.getHealth() == (netherite ? 8 : 14) && poison != null
                    && poison.getDuration() == 120 && poison.getAmplifier() == (netherite ? 1 : 0), "Flail melee damage or poison failed");
            helper.assertTrue(!PufferfishItems.isPufferfish(stack), "Flail was classified as a fish ingredient");
            player.setItemSlot(EquipmentSlot.HEAD, stack);
            helper.assertTrue(!PufferfishHeadgear.isWearing(player), "Flail granted fish headgear effects");
            target.discard();
        }
        helper.succeed();
    }

    /// Verifies successful launches preserve the item and share a cooldown across both variants.
    @GameTest(template = "empty")
    public static void launchPreservesTheWeaponAndRespectsCooldown(GameTestHelper helper) {
        for (var hand : InteractionHand.values()) {
            var stack = new ItemStack(PufferfishItems.FLAIL.get());
            stack.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.BLUE);
            var player = player(helper, ItemStack.EMPTY);
            player.setItemInHand(hand, stack);
            var original = stack.copy();
            var result = stack.use(helper.getLevel(), player, hand);
            var heads = helper.getLevel().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(3));
            helper.assertTrue(result.getResult().consumesAction() && heads.size() == 1, "Flail did not launch exactly one head");
            var head = heads.getFirst();
            helper.assertTrue(head.getOwner() == player && head.getHand() == hand && ItemStack.matches(head.getItem(), original)
                    && ItemStack.matches(stack, original), "Launch lost ownership or changed the held item");
            helper.assertTrue(player.getCooldowns().isOnCooldown(PufferfishItems.FLAIL.get())
                    && player.getCooldowns().isOnCooldown(PufferfishItems.NETHERITE_FLAIL.get()), "Flail variants did not share cooldown");
            helper.assertTrue(stack.use(helper.getLevel(), player, hand).getResult() == InteractionResult.FAIL, "Cooldown allowed another launch");
            head.discard();
        }
        helper.succeed();
    }

    /// Verifies cancellation leaves both the held weapon and cooldown unchanged.
    @GameTest(template = "empty")
    public static void canceledLaunchDoesNotConsumeOrCoolDown(GameTestHelper helper) {
        var stack = new ItemStack(PufferfishItems.FLAIL.get());
        var player = player(helper, stack);
        Consumer<EntityJoinLevelEvent> cancel = event -> {
            if (event.getEntity() instanceof PufferfishFlailHead) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.getResult() == InteractionResult.FAIL && stack.getCount() == 1
                    && !player.getCooldowns().isOnCooldown(stack.getItem()), "Canceled launch consumed or cooled down the weapon");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.succeed();
    }

    /// Verifies the tether limits outward flight and returns without creating an item entity.
    @GameTest(template = "empty")
    public static void tetherReturnsWithoutDroppingAnotherWeapon(GameTestHelper helper) {
        var player = player(helper, new ItemStack(PufferfishItems.FLAIL.get()));
        var head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        head.setDeltaMovement(0, 1.2, 0);
        helper.getLevel().addFreshEntity(head);
        boolean returned = false;
        for (int i = 0; i < 26 && !head.isRemoved(); i++) {
            head.tick();
            returned |= head.isReturning();
            helper.assertTrue(head.position().distanceTo(player.getEyePosition()) <= PufferfishFlailHead.REACH, "Head exceeded its tether range");
        }
        helper.assertTrue(returned && head.isRemoved() && player.getMainHandItem().getCount() == 1, "Head did not return to its retained weapon");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8)).isEmpty(), "Return duplicated the weapon");
        head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.getLevel().addFreshEntity(head);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        head.tick();
        helper.assertTrue(head.isRemoved(), "Unequipping a flail left its head active");
        helper.succeed();
    }

    /// Verifies the flying head damages once and does not damage again while returning.
    @GameTest(template = "empty")
    public static void flyingHeadHitsOnceAndReturns(GameTestHelper helper) {
        for (var item : List.of(PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get())) {
            var player = player(helper, new ItemStack(item));
            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 3, 4));
            target.setNoAi(true);
            target.setNoGravity(true);
            var head = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            head.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
            head.setDeltaMovement(0, 0, 3);
            helper.getLevel().addFreshEntity(head);
            head.tick();
            boolean netherite = item == PufferfishItems.NETHERITE_FLAIL.get();
            helper.assertTrue(head.isReturning() && target.getHealth() == (netherite ? 8 : 14)
                    && target.hasEffect(MobEffects.POISON), "Flying head did not hit and start returning");
            float health = target.getHealth();
            target.invulnerableTime = 0;
            head.setPos(target.position().add(0, 0.8, 0));
            head.tick();
            helper.assertTrue(target.getHealth() == health, "Returning head dealt another hit");
            head.discard();
            target.discard();
        }
        helper.succeed();
    }
}
