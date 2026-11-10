// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Verifies radiant fish upgrading and retained components.
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class RadiantPufferfishGameTests {
    /// Prevents construction of the test holder.
    private RadiantPufferfishGameTests() {}

    /// Verifies empty-template smithing, preserved components, ingredient consumption, and recipe synchronization.
    @GameTest(template = "empty")
    public static void netherStarUpgradesOneNetheriteFish(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = new ItemStack(PufferfishItems.NETHERITE.get(), 3);
        base.set(DataComponents.CUSTOM_NAME, Component.literal("Starlight"));
        var density = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        EnchantmentHelper.updateEnchantments(base, entries -> entries.set(density, 5));
        var input = new SmithingRecipeInput(ItemStack.EMPTY, base, new ItemStack(Items.NETHER_STAR, 2));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
        var output = recipe.assemble(input, level.registryAccess());
        helper.assertTrue(output.is(PufferfishItems.RADIANT.get()) && output.getCount() == 1
                && output.getHoverName().getString().equals("Starlight") && output.getEnchantmentLevel(density) == 5
                && base.getCount() == 3 && output.has(DataComponents.FIRE_RESISTANT), "Upgrade lost identity, components, or input counts");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var codec = KaleidoscopePufferfish.RADIANT_UPGRADE.get().streamCodec();
            codec.encode(buffer, (RadiantPufferfishUpgradeRecipe) recipe);
            var synced = codec.decode(buffer);
            helper.assertTrue(!synced.isIncomplete() && synced.matches(input, level)
                    && ItemStack.matches(output, synced.assemble(input, level.registryAccess())), "Upgrade recipe failed network synchronization");
        } finally {
            buffer.release();
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new SmithingMenu(1, player.getInventory());
        helper.assertTrue(menu.getSlot(1).mayPlace(base) && menu.getSlot(2).mayPlace(input.addition()), "Smithing slots rejected upgrade ingredients");
        menu.getSlot(1).set(base.copy());
        menu.getSlot(2).set(input.addition().copy());
        menu.createResult();
        var result = menu.getSlot(3);
        helper.assertTrue(result.mayPickup(player) && ItemStack.matches(output, result.getItem()), "Smithing table required an unwanted template");
        var taken = result.remove(1);
        result.onTake(player, taken);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount() == 2
                && menu.getSlot(2).getItem().getCount() == 1, "Upgrade did not consume exactly one fish and one star");
        for (var invalid : List.of(
                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, input.addition()),
                new SmithingRecipeInput(ItemStack.EMPTY, new ItemStack(Items.PUFFERFISH), input.addition()),
                new SmithingRecipeInput(ItemStack.EMPTY, output, input.addition()),
                new SmithingRecipeInput(ItemStack.EMPTY, base, new ItemStack(Items.BREEZE_ROD)))) {
            helper.assertTrue(!recipe.matches(invalid, level) && recipe.assemble(invalid, level.registryAccess()).isEmpty(),
                    "Upgrade accepted invalid or already-upgraded ingredients");
        }
        helper.assertTrue(PufferfishItems.canThrow(output) && PufferfishItems.hasNetheriteCombat(output)
                && !PufferfishItems.isHeavy(output) && output.hasFoil(), "Radiant fish lost its combat or visual properties");
        player.setItemSlot(EquipmentSlot.HEAD, output);
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 3
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 3, "Radiant headgear lost netherite protection");
        helper.assertTrue(PufferfishCombat.damage(output) == PufferfishCombat.damage(base), "Radiant fish lost netherite attack damage");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishItems.RADIANT.get(), 2));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
        helper.assertTrue(player.getMainHandItem().getCount() == 1
                && player.getCooldowns().isOnCooldown(PufferfishItems.RADIANT.get())
                && player.getCooldowns().isOnCooldown(PufferfishItems.NETHERITE.get()), "Radiant throwing lost consumption or shared cooldown");
        helper.succeed();
    }

}
