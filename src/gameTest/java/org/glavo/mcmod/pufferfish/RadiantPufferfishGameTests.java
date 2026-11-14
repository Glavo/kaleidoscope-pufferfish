// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/// Verifies radiant fish upgrading and retained components.
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class RadiantPufferfishGameTests {
    private RadiantPufferfishGameTests() {}

    /// Verifies template-gated smithing, preserved components, ingredient consumption, and recipe synchronization.
    @GameTest(template = "empty")
    public static void netherStarUpgradesOneNetheriteFish(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = new ItemStack(PufferfishItems.NETHERITE.get());
        base.set(DataComponents.CUSTOM_NAME, Component.literal("Starlight"));
        var density = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        EnchantmentHelper.updateEnchantments(base, entries -> entries.set(density, 5));
        PufferfishDurability.activate(base);
        var input = new SmithingRecipeInput(PufferfishItems.UPGRADE_TEMPLATE.toStack(2), base, new ItemStack(Items.NETHER_STAR, 2));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
        var output = recipe.assemble(input, level.registryAccess());
        helper.assertTrue(output.is(PufferfishItems.RADIANT.get()) && output.getCount() == 1
                && output.getHoverName().getString().equals("Starlight") && output.getEnchantmentLevel(density) == 5
                && base.getCount() == 1 && !output.isStackable() && output.has(DataComponents.FIRE_RESISTANT), "Upgrade lost identity, components, or input counts");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var codec = PufferfishPlease.RADIANT_UPGRADE.get().streamCodec();
            codec.encode(buffer, (PufferfishUpgradeRecipe) recipe);
            var synced = codec.decode(buffer);
            helper.assertTrue(!synced.isIncomplete() && synced.matches(input, level)
                    && ItemStack.matches(output, synced.assemble(input, level.registryAccess())), "Upgrade recipe failed network synchronization");
        } finally {
            buffer.release();
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new SmithingMenu(1, player.getInventory());
        helper.assertTrue(menu.getSlot(0).mayPlace(input.template()) && menu.getSlot(1).mayPlace(base) && menu.getSlot(2).mayPlace(input.addition()), "Smithing slots rejected upgrade ingredients");
        menu.getSlot(0).set(input.template().copy());
        menu.getSlot(1).set(base.copy());
        menu.getSlot(2).set(input.addition().copy());
        menu.createResult();
        var result = menu.getSlot(3);
        helper.assertTrue(result.mayPickup(player) && ItemStack.matches(output, result.getItem()), "Smithing table rejected the pufferfish upgrade template");
        var taken = result.remove(1);
        result.onTake(player, taken);
        helper.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().isEmpty()
                && menu.getSlot(2).getItem().getCount() == 1, "Upgrade did not consume exactly one template, one fish and one star");
        for (var invalid : List.of(
                new SmithingRecipeInput(ItemStack.EMPTY, base, input.addition()),
                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, input.addition()),
                new SmithingRecipeInput(input.template(), new ItemStack(Items.PUFFERFISH), input.addition()),
                new SmithingRecipeInput(input.template(), output, input.addition()),
                new SmithingRecipeInput(input.template(), base, new ItemStack(Items.BREEZE_ROD)))) {
            helper.assertTrue(!recipe.matches(invalid, level) && recipe.assemble(invalid, level.registryAccess()).isEmpty(),
                    "Upgrade accepted invalid or already-upgraded ingredients");
        }
        helper.assertTrue(PufferfishItems.canThrow(output) && PufferfishItems.hasNetheriteCombat(output)
                && !PufferfishItems.isHeavy(output) && output.hasFoil(), "Radiant fish lost its combat or visual properties");
        player.setItemSlot(EquipmentSlot.HEAD, output);
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 3
                && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 3, "Radiant headgear lost netherite protection");
        helper.assertTrue(PufferfishCombat.damage(output) == 11, "Radiant fish should deal eight base damage plus three from Density V");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishItems.RADIANT.get(), 2));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
        helper.assertTrue(player.getMainHandItem().getCount() == 1
                && player.getCooldowns().isOnCooldown(PufferfishItems.RADIANT.get())
                && player.getCooldowns().isOnCooldown(PufferfishItems.NETHERITE.get()), "Radiant throwing lost consumption or shared cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nightVisionLastsWhileWornAndPreservesPotions(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setNoGravity(true);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        var effect = net.minecraft.world.effect.MobEffects.NIGHT_VISION;
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PufferfishItems.RADIANT.get()));
        for (int tick = 0; tick < 800; tick++) {
            player.tick();
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        }
        helper.assertTrue(player.hasEffect(effect) && player.getEffect(effect).getDuration() >= 299, "Night vision expired while worn");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        for (int tick = 0; tick < 301; tick++) {
            player.tick();
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        }
        helper.assertTrue(!player.hasEffect(effect), "Night vision remained permanently after unequipping");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 2400));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PufferfishItems.RADIANT.get()));
        player.tick();
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.tick();
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
        helper.assertTrue(player.getEffect(effect).getDuration() > 2300, "Headgear shortened potion night vision");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedRadiantFishLightsWithoutALamp(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, PufferfishBlocks.PLACED_FISH.get().defaultBlockState());
        var entity = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
        entity.setFish(new ItemStack(PufferfishItems.RADIANT.get()));
        helper.assertTrue(level.getBlockState(pos).getLightEmission() == 15, "Radiant fish did not emit full light");
        var saved = entity.saveWithFullMetadata(level.registryAccess());
        entity.setFish(new ItemStack(Items.PUFFERFISH));
        helper.assertTrue(level.getBlockState(pos).getLightEmission() == 0, "Ordinary fish retained radiant light");
        entity.loadWithComponents(saved, level.registryAccess());
        entity.onLoad();
        helper.assertTrue(level.getBlockState(pos).getLightEmission() == 15 && entity.fish().is(PufferfishItems.RADIANT.get()), "Reload lost radiant illumination");
        helper.assertTrue(PufferfishLamps.lamp(entity.fish()).isEmpty(), "Unnecessary radiant lamp was registered");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos.above()) == 14, "Radiant light did not propagate");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void radiantAccessoriesHaveRecipesAndTags(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var name : List.of("radiant_pufferfish_flail", "radiant_pufferfish_skin_shield", "radiant_pufferfish_skin_boat",
                "radiant_pufferfish_skin_helmet", "radiant_pufferfish_skin_chestplate", "radiant_pufferfish_skin_leggings", "radiant_pufferfish_skin_boots")) {
            helper.assertTrue(level.getRecipeManager().byKey(PufferfishPlease.id(name)).isPresent(), "Missing accessory recipe: " + name);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(PufferfishPlease.id(name)), "Missing accessory item: " + name);
        }
        helper.assertTrue(PufferfishBlocks.skinForFish(new ItemStack(PufferfishItems.RADIANT.get())) == PufferfishBlocks.RADIANT_SKIN.get(), "Radiant cutting has no skin");
        helper.assertTrue(PufferfishItems.hasNetheriteCombat(new ItemStack(PufferfishItems.RADIANT_FLAIL.get())), "Radiant flail lost its combat tier");
        helper.succeed();
    }
}
