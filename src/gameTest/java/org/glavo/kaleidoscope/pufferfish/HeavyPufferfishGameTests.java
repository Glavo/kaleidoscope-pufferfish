// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

/// Verifies heavy fish upgrading, native mace combat, equipment penalties, and retained pufferfish behavior.
@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class HeavyPufferfishGameTests {
    /// Prevents construction of the test holder.
    private HeavyPufferfishGameTests() {}

    /// Verifies empty-template smithing, preserved components, ingredient consumption, and recipe synchronization.
    @GameTest(template = "empty")
    public static void heavyCoreUpgradesOneNetheriteFish(GameTestHelper helper) {
        var level = helper.getLevel();
        var base = new ItemStack(PufferfishItems.NETHERITE.get(), 3);
        base.set(DataComponents.CUSTOM_NAME, Component.literal("Tempest"));
        var density = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        EnchantmentHelper.updateEnchantments(base, entries -> entries.set(density, 5));
        var input = new SmithingRecipeInput(ItemStack.EMPTY, base, new ItemStack(Items.HEAVY_CORE, 2));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level).orElseThrow().value();
        var output = recipe.assemble(input, level.registryAccess());
        helper.assertTrue(output.is(PufferfishItems.HEAVY.get()) && output.getCount() == 1
                && output.getHoverName().getString().equals("Tempest") && output.getEnchantmentLevel(density) == 5
                && base.getCount() == 3 && output.has(DataComponents.FIRE_RESISTANT), "Upgrade lost identity, components, or input counts");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            var codec = KaleidoscopePufferfish.HEAVY_UPGRADE.get().streamCodec();
            codec.encode(buffer, (HeavyPufferfishUpgradeRecipe) recipe);
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
                && menu.getSlot(2).getItem().getCount() == 1, "Upgrade did not consume exactly one fish and one core");
        for (var invalid : List.of(
                new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, input.addition()),
                new SmithingRecipeInput(ItemStack.EMPTY, new ItemStack(Items.PUFFERFISH), input.addition()),
                new SmithingRecipeInput(ItemStack.EMPTY, output, input.addition()),
                new SmithingRecipeInput(ItemStack.EMPTY, base, new ItemStack(Items.BREEZE_ROD)))) {
            helper.assertTrue(!recipe.matches(invalid, level) && recipe.assemble(invalid, level.registryAccess()).isEmpty(),
                    "Upgrade accepted invalid or already-upgraded ingredients");
        }
        helper.succeed();
    }

    /// Measures immediate damage from a fully charged attack against a stationary armored or unarmored target.
    private static float hit(GameTestHelper helper, ItemStack stack, float distance, boolean armored) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int tick = 0; tick < 36; tick++) player.tick();
        var pos = helper.absolutePos(new BlockPos(2, 4, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setOnGround(distance == 0);
        player.fallDistance = distance;
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        if (armored) {
            target.getAttribute(Attributes.ARMOR).setBaseValue(20);
            target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
        }
        player.attack(target);
        if (PufferfishItems.isHeavy(stack)) {
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 6) < 0.0001
                    && Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 0.6) < 0.0001, "Heavy fish did not use mace attack attributes");
            var poison = target.getEffect(MobEffects.POISON);
            helper.assertTrue(poison != null && poison.getAmplifier() == 1 && poison.getDuration() == 120,
                    "Heavy fish lost pufferfish poison effects");
            helper.assertTrue(stack.getCount() == 1 && !stack.isDamaged(), "Heavy fish attack consumed or damaged the item");
            helper.assertTrue(player.fallDistance == (distance > 1.5F ? 0 : distance), "Heavy fish did not reset successful smash fall distance");
        }
        float damage = 1000 - target.getHealth();
        target.discard();
        return damage;
    }

    /// Compares heavy fish with actual vanilla mace hits across all Density levels, fall boundaries, and armor.
    @GameTest(template = "empty")
    public static void heavyFishDamageMatchesVanillaMace(GameTestHelper helper) {
        var density = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        for (boolean armored : new boolean[] {false, true}) {
            for (float distance : new float[] {0, 1.5F, 2, 3, 8, 15}) {
                for (int rank = 0; rank <= 5; rank++) {
                    var heavy = new ItemStack(PufferfishItems.HEAVY.get());
                    var mace = new ItemStack(Items.MACE);
                    int level = rank;
                    if (rank > 0) for (var stack : List.of(heavy, mace)) EnchantmentHelper.updateEnchantments(stack, entries -> entries.set(density, level));
                    helper.assertTrue(Math.abs(hit(helper, heavy, distance, armored) - hit(helper, mace, distance, armored)) < 0.001,
                            "Heavy fish damage differs from vanilla mace damage");
                }
            }
        }
        boolean enabled = PufferfishCombatConfig.SMASH_ENABLED.get();
        double cap = PufferfishCombatConfig.MAX_SMASH_BONUS.get();
        try {
            PufferfishCombatConfig.SMASH_ENABLED.set(false);
            PufferfishCombatConfig.SMASH_ENABLED.clearCache();
            PufferfishCombatConfig.MAX_SMASH_BONUS.set(1.0);
            PufferfishCombatConfig.MAX_SMASH_BONUS.clearCache();
            helper.assertTrue(hit(helper, new ItemStack(PufferfishItems.HEAVY.get()), 8, false)
                    == hit(helper, new ItemStack(Items.MACE), 8, false), "Scaled fish settings modified the heavy fish's native mace behavior");
        } finally {
            PufferfishCombatConfig.SMASH_ENABLED.set(enabled);
            PufferfishCombatConfig.SMASH_ENABLED.clearCache();
            PufferfishCombatConfig.MAX_SMASH_BONUS.set(cap);
            PufferfishCombatConfig.MAX_SMASH_BONUS.clearCache();
        }
        helper.succeed();
    }

    /// Verifies head-only armor, toughness, knockback resistance, movement reduction, and cleanup after removal.
    @GameTest(template = "empty")
    public static void heavyHeadgearProtectsAndSlowsOnlyWhileWorn(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        player.inventoryMenu.getSlot(9).set(new ItemStack(PufferfishItems.HEAVY.get(), 2));
        player.inventoryMenu.quickMoveStack(player, 9);
        player.tick();
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(PufferfishItems.HEAVY.get())
                && player.getItemBySlot(EquipmentSlot.HEAD).getCount() == 1 && player.inventoryMenu.getSlot(9).getItem().getCount() == 1,
                "Heavy fish did not equip exactly one item in the head slot");
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 4 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 4
                && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.5
                && Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 0.85) < 0.000001,
                "Heavy headgear attributes are incorrect");
        var breathe = new LivingBreatheEvent(player, false, 1, 0);
        NeoForge.EVENT_BUS.post(breathe);
        helper.assertTrue(breathe.canBreathe(), "Heavy headgear lost pufferfish underwater breathing");
        var fish = EntityType.PUFFERFISH.create(helper.getLevel());
        helper.assertTrue(!player.hurt(player.damageSources().mobAttack(fish), 3), "Heavy headgear lost sting protection");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishItems.HEAVY.get()));
        player.tick();
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 0 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0
                && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0 && player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed,
                "Headgear protection or slowdown leaked into the hand slot");
        helper.succeed();
    }

    /// Verifies heavy fish accept Wind Burst and retain their item identity and netherite throwing speed.
    @GameTest(template = "empty")
    public static void heavyFishSupportWindBurstAndThrowing(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new AnvilMenu(1, player.getInventory());
        var wind = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.WIND_BURST);
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, entries -> entries.set(wind, 3));
        menu.getSlot(0).set(new ItemStack(PufferfishItems.HEAVY.get()));
        menu.getSlot(1).set(book);
        menu.createResult();
        var output = menu.getSlot(2).getItem();
        helper.assertTrue(output.is(PufferfishItems.HEAVY.get()) && output.getEnchantmentLevel(wind) == 3, "Heavy fish rejected Wind Burst");
        var pos = helper.absolutePos(new BlockPos(2, 10, 2));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishItems.HEAVY.get(), 2));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
        var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
        helper.assertTrue(projectiles.size() == 1 && projectiles.getFirst().getItem().is(PufferfishItems.HEAVY.get())
                && player.getMainHandItem().getCount() == 1
                && Math.abs(projectiles.getFirst().getDeltaMovement().length() - PufferfishCombatConfig.NETHERITE_THROW_SPEED.get()) < 0.06,
                "Heavy fish throwing lost identity, count, or netherite launch speed");
        projectiles.forEach(ThrownPufferfish::discard);
        helper.succeed();
    }
}
