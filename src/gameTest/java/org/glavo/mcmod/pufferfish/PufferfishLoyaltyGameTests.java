// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishLoyaltyGameTests {
    private static List<ItemStack> variants() {
        var result = new ArrayList<ItemStack>();
        result.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) {
            result.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        }
        result.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        return result;
    }

    private static ItemStack enchant(GameTestHelper helper, ItemStack stack, int level) {
        var loyalty = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(loyalty, level));
        return stack;
    }

    private static Player owner(GameTestHelper helper) {
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 1, 0));
        owner.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return owner;
    }

    private static ThrownPufferfish hitBlock(GameTestHelper helper, Player owner, ItemStack stack) {
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        var fish = new ThrownPufferfish(helper.getLevel(), owner);
        fish.setItem(stack);
        fish.setDeltaMovement(0, 0, 4);
        helper.getLevel().addFreshEntity(fish);
        fish.tick();
        helper.assertTrue(fish.isReturning() && !fish.isRemoved(), "Loyal fish did not return after block collision");
        return fish;
    }

    private static int finishReturn(GameTestHelper helper, ThrownPufferfish fish) {
        int ticks = 0;
        while (!fish.isRemoved() && ticks++ < 300) fish.tick();
        helper.assertTrue(fish.isRemoved(), "Loyal fish did not finish returning");
        return ticks;
    }

    @GameTest(template = "empty")
    public static void anvilAppliesAndUpgradesLoyaltyForEveryVariant(GameTestHelper helper) {
        var player = owner(helper);
        var loyalty = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY);
        for (var variant : variants()) {
            variant.set(DataComponents.CUSTOM_NAME, Component.literal("Loyal operator"));
            for (int level = 1; level <= 3; level++) {
                player.experienceLevel = 30;
                var menu = new AnvilMenu(1, player.getInventory());
                var book = enchant(helper, new ItemStack(Items.ENCHANTED_BOOK), level == 1 ? 1 : level - 1);
                menu.getSlot(0).set(variant.copy());
                menu.getSlot(1).set(book);
                menu.setItemName("Loyal operator");
                helper.assertTrue(menu.getSlot(2).hasItem() && menu.getSlot(2).mayPickup(player), "Anvil did not offer loyal fish");
                var result = menu.getSlot(2).remove(1);
                int cost = menu.getCost();
                menu.getSlot(2).onTake(player, result);
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(loyalty, result) == level, "Loyalty upgrade used the wrong level");
                helper.assertTrue(result.getCount() == 1 && result.is(variant.getItem())
                        && PufferfishColors.get(result) == PufferfishColors.get(variant)
                        && result.getHoverName().getString().equals("Loyal operator"), "Anvil lost fish identity");
                helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty()
                        && player.experienceLevel == 30 - cost && cost > 0, "Anvil did not charge ingredients and XP");
                variant = result;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilRejectsBatchesAndHonorsCostsAndOtherEnchantments(GameTestHelper helper) {
        var player = owner(helper);
        var menu = new AnvilMenu(1, player.getInventory());
        var book = enchant(helper, new ItemStack(Items.ENCHANTED_BOOK), 3);
        for (var invalid : List.of(new ItemStack(Items.PUFFERFISH, 2), new ItemStack(Items.PUFFERFISH_BUCKET), new ItemStack(Items.COD))) {
            menu.getSlot(0).set(invalid);
            menu.getSlot(1).set(book.copy());
            helper.assertTrue(!menu.getSlot(2).hasItem(), "Anvil accepted an invalid loyalty target");
        }
        var expensive = new ItemStack(Items.PUFFERFISH);
        expensive.set(DataComponents.REPAIR_COST, 40);
        menu.getSlot(0).set(expensive);
        helper.assertTrue(!menu.getSlot(2).hasItem(), "Anvil bypassed the too-expensive limit");
        menu.getSlot(0).set(new ItemStack(Items.PUFFERFISH));
        player.experienceLevel = 0;
        helper.assertTrue(menu.getSlot(2).hasItem() && !menu.getSlot(2).mayPickup(player), "Anvil ignored insufficient XP");
        var impaling = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.IMPALING);
        var mixed = book.copy();
        EnchantmentHelper.updateEnchantments(mixed, enchantments -> enchantments.set(impaling, 5));
        menu.getSlot(1).set(mixed);
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(impaling, menu.getSlot(2).getItem()) == 0, "Loyalty support enabled unrelated enchantments");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loyalImpactsDamageOnceAndReturnEveryVariant(GameTestHelper helper) {
        var player = owner(helper);
        for (var stack : variants()) {
            player.getInventory().clearContent();
            enchant(helper, stack, 3);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Loyal keepsake"));
            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 3));
            target.setNoAi(true);
            var projectile = new ThrownPufferfish(helper.getLevel(), player);
            projectile.setItem(stack);
            projectile.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
            projectile.setDeltaMovement(0, 0, 3);
            helper.getLevel().addFreshEntity(projectile);
            projectile.tick();
            float health = target.getHealth();
            helper.assertTrue(health == 20 - (PufferfishItems.isNetherite(stack) ? 6 : 1), "Loyalty changed impact damage");
            helper.assertTrue(projectile.isReturning() && !projectile.isRemoved(), "Entity hit dropped loyal fish");
            target.invulnerableTime = 0;
            projectile.setPos(target.position().add(0, 0.8, 1));
            finishReturn(helper, projectile);
            helper.assertTrue(target.getHealth() == health, "Returning fish damaged its target again");
            helper.assertTrue(player.getInventory().items.stream().filter(s -> ItemStack.isSameItemSameComponents(PufferfishDurabilityGameTests.worn(stack), s))
                    .mapToInt(ItemStack::getCount).sum() == 1, "Return lost components or duplicated fish");
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loyaltyLevelsReturnFasterAndPassThroughWalls(GameTestHelper helper) {
        var player = owner(helper);
        int previous = Integer.MAX_VALUE;
        for (int level = 1; level <= 3; level++) {
            player.getInventory().clearContent();
            helper.setBlock(new BlockPos(2, 2, 1), Blocks.AIR);
            var fish = hitBlock(helper, player, enchant(helper, new ItemStack(Items.PUFFERFISH), level));
            helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
            fish.setPos(player.getX(), player.getEyeY(), player.getZ() + 10);
            int duration = finishReturn(helper, fish);
            helper.assertTrue(duration < previous, "Higher loyalty did not return faster");
            helper.assertTrue(player.getInventory().countItem(Items.PUFFERFISH) == 1, "Wall blocked returning fish");
            previous = duration;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void returningFishSurvivesSaveAndSynchronization(GameTestHelper helper) {
        var player = owner(helper);
        var stack = enchant(helper, PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.CYAN), 2);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Saved loyalty"));
        var fish = hitBlock(helper, player, stack);
        var receiver = PufferfishPlease.THROWN_PUFFERFISH.get().create(helper.getLevel());
        receiver.getEntityData().assignValues(fish.getEntityData().getNonDefaultValues());
        helper.assertTrue(receiver.isReturning() && ItemStack.matches(receiver.getItem(), PufferfishDurabilityGameTests.worn(stack)), "Return state or enchantments failed to synchronize");
        var tag = new CompoundTag();
        fish.saveWithoutId(tag);
        fish.discard();
        var restored = PufferfishPlease.THROWN_PUFFERFISH.get().create(helper.getLevel());
        restored.load(tag);
        helper.assertTrue(restored.isReturning() && restored.isNoGravity() && ItemStack.matches(restored.getItem(), PufferfishDurabilityGameTests.worn(stack)), "Saved return state was lost");
        // Mock players are not registered in the level's UUID lookup.
        restored.setOwner(player);
        helper.getLevel().addFreshEntity(restored);
        finishReturn(helper, restored);
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(0), PufferfishDurabilityGameTests.worn(stack)), "Reloaded fish failed to return intact");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryAndMissingOwnersLeaveOneRecoverableFish(GameTestHelper helper) {
        for (int scenario = 0; scenario < 3; scenario++) {
            var player = owner(helper);
            var stack = enchant(helper, new ItemStack(PufferfishItems.NETHERITE.get()), 3);
            var fish = hitBlock(helper, player, stack);
            if (scenario == 0) {
                for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            } else if (scenario == 1) {
                player.setHealth(0);
            } else {
                player.discard();
            }
            finishReturn(helper, fish);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(2, 2, 2))).inflate(5))
                    .stream().filter(drop -> ItemStack.isSameItemSameComponents(drop.getItem(), PufferfishDurabilityGameTests.worn(stack))).toList();
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "Failed return lost or duplicated fish");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeThrowsDoNotDuplicateAndVoidThrowsReturn(GameTestHelper helper) {
        var player = owner(helper);
        player.getAbilities().instabuild = true;
        var stack = enchant(helper, new ItemStack(Items.PUFFERFISH), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        var fish = hitBlock(helper, player, stack);
        finishReturn(helper, fish);
        helper.assertTrue(player.getInventory().countItem(Items.PUFFERFISH) == 1, "Creative loyalty return duplicated its original item");
        player.getInventory().clearContent();
        player.getAbilities().instabuild = false;
        fish = new ThrownPufferfish(helper.getLevel(), player);
        fish.setItem(stack);
        fish.setPos(player.getX(), helper.getLevel().getMinBuildHeight() - 1, player.getZ());
        helper.getLevel().addFreshEntity(fish);
        finishReturn(helper, fish);
        helper.assertTrue(player.getInventory().countItem(Items.PUFFERFISH) == 1, "Loyal fish was lost below the build limit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realThrowConsumesAndReturnsExactlyOneEnchantedFish(GameTestHelper helper) {
        var player = owner(helper);
        var stack = enchant(helper, PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 3), DyeColor.ORANGE), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
        var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, player.getBoundingBox().inflate(2));
        helper.assertTrue(projectiles.size() == 1 && stack.getCount() == 2, "Loyal throw did not consume exactly one item");
        var fish = projectiles.getFirst();
        fish.setDeltaMovement(0, 0, 4);
        fish.tick();
        helper.assertTrue(fish.isReturning(), "Actual thrown enchanted fish did not begin returning");
        finishReturn(helper, fish);
        helper.assertTrue(stack.getCount() == 2 && player.getInventory().countItem(stack.getItem()) == 3
                && ItemStack.isSameItemSameComponents(PufferfishDurabilityGameTests.worn(stack), fish.getItem()), "Return did not preserve one worn fish and two pristine fish");
        helper.succeed();
    }
}
