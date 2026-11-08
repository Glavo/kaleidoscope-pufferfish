// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishEnchantmentGameTests {
    private static final List<ResourceKey<Enchantment>> SUPPORTED = List.of(
            Enchantments.AQUA_AFFINITY, Enchantments.LOYALTY, Enchantments.DENSITY, Enchantments.BREACH, Enchantments.PUNCH,
            Enchantments.WIND_BURST, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS,
            Enchantments.VANISHING_CURSE, Enchantments.BINDING_CURSE);

    private static ItemStack enchant(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(holder, level));
        return stack;
    }

    private static int level(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key) {
        var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }

    private static List<ItemStack> variants() {
        var result = new ArrayList<ItemStack>();
        result.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) result.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        result.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        result.add(new ItemStack(PufferfishItems.HEAVY.get()));
        return result;
    }

    private static Player owner(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 1, 0));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return player;
    }

    private static LivingEntity target(GameTestHelper helper) {
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        return target;
    }

    private static ThrownPufferfish hit(GameTestHelper helper, Player owner, LivingEntity target, ItemStack stack) {
        var fish = new ThrownPufferfish(helper.getLevel(), owner);
        fish.setItem(stack);
        fish.setPos(target.getX(), target.getY() + 0.8, target.getZ() - 2);
        fish.setDeltaMovement(0, 0, 3);
        helper.getLevel().addFreshEntity(fish);
        fish.tick();
        return fish;
    }

    private static void clearDrops(GameTestHelper helper, LivingEntity target) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
        target.discard();
    }

    @GameTest(template = "empty")
    public static void allSupportedBooksApplyAndUpgradeEveryFishVariant(GameTestHelper helper) {
        var player = owner(helper);
        for (var original : variants()) {
            original.set(DataComponents.CUSTOM_NAME, Component.literal("Enchanted keepsake"));
            var rejected = new AnvilMenu(1, player.getInventory());
            rejected.getSlot(0).set(original.copy());
            rejected.getSlot(1).set(enchant(helper, new ItemStack(Items.ENCHANTED_BOOK), Enchantments.RESPIRATION, 3));
            helper.assertTrue(!rejected.getSlot(2).hasItem(), "Respiration is still accepted on pufferfish");
            for (var key : SUPPORTED) {
                int max = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key).value().getMaxLevel();
                for (int rank = 1; rank <= max; rank++) {
                    var input = original.copy();
                    if (rank > 1) enchant(helper, input, key, rank - 1);
                    var menu = new AnvilMenu(1, player.getInventory());
                    player.experienceLevel = 30;
                    menu.getSlot(0).set(input);
                    menu.getSlot(1).set(enchant(helper, new ItemStack(Items.ENCHANTED_BOOK), key, Math.max(1, rank - 1)));
                    menu.setItemName("Enchanted keepsake");
                    helper.assertTrue(menu.getSlot(2).hasItem() && menu.getSlot(2).mayPickup(player), "Supported book was rejected: " + key);
                    var result = menu.getSlot(2).remove(1);
                    int cost = menu.getCost();
                    menu.getSlot(2).onTake(player, result);
                    helper.assertTrue(level(helper, result, key) == rank && result.getCount() == 1, "Wrong enchantment upgrade level");
                    helper.assertTrue(result.is(original.getItem()) && PufferfishColors.get(result) == PufferfishColors.get(original)
                            && result.getHoverName().equals(original.getHoverName()), "Enchanting changed fish identity");
                    helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty()
                            && player.experienceLevel == 30 - cost && cost > 0, "Enchantment costs were not charged");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixedBooksRespectDamageExclusivityAndPreserveCompatibleEnchantments(GameTestHelper helper) {
        var player = owner(helper);
        for (var first : List.of(Enchantments.DENSITY, Enchantments.BREACH)) {
            var second = first == Enchantments.DENSITY ? Enchantments.BREACH : Enchantments.DENSITY;
            var menu = new AnvilMenu(1, player.getInventory());
            var fish = enchant(helper, new ItemStack(Items.PUFFERFISH), first, 1);
            menu.getSlot(0).set(fish);
            var book = enchant(helper, new ItemStack(Items.ENCHANTED_BOOK), second, 1);
            menu.getSlot(1).set(book);
            helper.assertTrue(!menu.getSlot(2).hasItem(), "Density and Breach were combined");
            enchant(helper, book, Enchantments.PUNCH, 2);
            enchant(helper, book, Enchantments.LOYALTY, 3);
            enchant(helper, book, Enchantments.AQUA_AFFINITY, 1);
            enchant(helper, book, Enchantments.SHARPNESS, 5);
            enchant(helper, book, Enchantments.RESPIRATION, 3);
            menu.createResult();
            var result = menu.getSlot(2).getItem();
            helper.assertTrue(!result.isEmpty() && level(helper, result, first) == 1 && level(helper, result, second) == 0,
                    "Mixed book replaced an existing incompatible enchantment");
            helper.assertTrue(level(helper, result, Enchantments.PUNCH) == 2 && level(helper, result, Enchantments.LOYALTY) == 3
                    && level(helper, result, Enchantments.AQUA_AFFINITY) == 1 && level(helper, result, Enchantments.SHARPNESS) == 0
                    && level(helper, result, Enchantments.RESPIRATION) == 0,
                    "Mixed book lost supported enchantments or enabled Sharpness");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void densityAddsSharpnessStyleDamageIndependentOfFallingOrHeldItems(GameTestHelper helper) {
        var player = owner(helper);
        player.fallDistance = 100;
        player.setItemInHand(InteractionHand.MAIN_HAND, enchant(helper, new ItemStack(Items.MACE), Enchantments.DENSITY, 5));
        for (boolean netherite : new boolean[] {false, true}) {
            for (int rank = 0; rank <= 5; rank++) {
                var stack = new ItemStack(netherite ? PufferfishItems.NETHERITE.get() : Items.PUFFERFISH);
                if (rank > 0) enchant(helper, stack, Enchantments.DENSITY, rank);
                var target = target(helper);
                float before = target.getHealth();
                hit(helper, player, target, stack);
                float expected = (netherite ? 10 : 2) + (rank == 0 ? 0 : 0.5F + 0.5F * rank);
                helper.assertTrue(Math.abs(before - target.getHealth() - expected) < 0.0001F, "Incorrect Density damage at level " + rank);
                clearDrops(helper, target);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breachUsesProjectileEnchantmentsAndOnlyReducesArmorProtection(GameTestHelper helper) {
        var player = owner(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, enchant(helper, new ItemStack(Items.MACE), Enchantments.BREACH, 4));
        for (boolean netherite : new boolean[] {false, true}) {
            for (boolean armored : new boolean[] {false, true}) {
                for (int rank = 0; rank <= 4; rank++) {
                    var stack = new ItemStack(netherite ? PufferfishItems.NETHERITE.get() : Items.PUFFERFISH);
                    if (rank > 0) enchant(helper, stack, Enchantments.BREACH, rank);
                    var target = target(helper);
                    target.getAttribute(Attributes.ARMOR).setBaseValue(armored ? 20 : 0);
                    target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(armored ? 8 : 0);
                    float before = target.getHealth();
                    hit(helper, player, target, stack);
                    float base = netherite ? 10 : 2;
                    float reduction = armored ? (20 - base / 4) / 25 : 0;
                    float expected = base * (1 - Math.max(0, reduction - rank * 0.15F));
                    helper.assertTrue(Math.abs(before - target.getHealth() - expected) < 0.0001F, "Incorrect Breach armor reduction at level " + rank);
                    clearDrops(helper, target);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void punchAddsDirectionalKnockbackAndRespectsResistanceAndInvulnerability(GameTestHelper helper) {
        var player = owner(helper);
        for (double resistance : new double[] {0, 0.5, 1}) {
            double baseline = 0;
            for (int rank = 0; rank <= 2; rank++) {
                var stack = new ItemStack(Items.PUFFERFISH);
                if (rank > 0) enchant(helper, stack, Enchantments.PUNCH, rank);
                var target = target(helper);
                target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(resistance);
                hit(helper, player, target, stack);
                double movement = target.getDeltaMovement().z;
                if (rank == 0) baseline = movement;
                helper.assertTrue(Math.abs(movement - baseline - rank * 0.6 * (1 - resistance)) < 0.0001,
                        "Punch ignored level, direction, or knockback resistance");
                clearDrops(helper, target);
            }
        }
        var target = target(helper);
        target.setInvulnerable(true);
        hit(helper, player, target, enchant(helper, new ItemStack(Items.PUFFERFISH), Enchantments.PUNCH, 2));
        helper.assertTrue(target.getDeltaMovement().lengthSqr() == 0, "Failed hit applied Punch knockback");
        clearDrops(helper, target);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aquaAffinityRestoresUnderwaterMiningSpeedOnlyWhileWorn(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(2, y, 0), Blocks.WATER);
        for (var stack : variants()) {
            var player = owner(helper);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            player.tick();
            player.tick();
            player.setOnGround(true);
            helper.assertTrue(player.isEyeInFluid(FluidTags.WATER), "Mining test player was not submerged");
            float baseline = player.getDigSpeed(Blocks.STONE.defaultBlockState(), player.blockPosition());
            enchant(helper, stack, Enchantments.AQUA_AFFINITY, 1);
            player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            player.tick();
            player.setOnGround(true);
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) - 1) < 0.0001,
                    "Aqua Affinity did not remove the submerged mining penalty");
            helper.assertTrue(Math.abs(player.getDigSpeed(Blocks.STONE.defaultBlockState(), player.blockPosition()) - baseline * 5) < 0.0001,
                    "Wearing enchanted fish did not restore underwater mining speed");
            var breathe = new LivingBreatheEvent(player, false, 1, 0);
            NeoForge.EVENT_BUS.post(breathe);
            helper.assertTrue(breathe.canBreathe(), "Aqua Affinity headgear lost unlimited breathing");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.tick();
            player.setOnGround(true);
            helper.assertTrue(Math.abs(player.getDigSpeed(Blocks.STONE.defaultBlockState(), player.blockPosition()) - baseline) < 0.0001,
                    "Mining bonus remained after removing headgear");
            player.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
            player.tick();
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) - 0.2) < 0.0001,
                    "Holding enchanted fish applied the head-only mining bonus");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void combinedEnchantmentsSurviveImpactReloadAndLoyaltyReturn(GameTestHelper helper) {
        var player = owner(helper);
        for (var damageEnchantment : List.of(Enchantments.DENSITY, Enchantments.BREACH)) {
            for (var stack : variants()) {
                player.getInventory().clearContent();
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Combined keepsake"));
                enchant(helper, stack, damageEnchantment, 4);
                enchant(helper, stack, Enchantments.LOYALTY, 3);
                enchant(helper, stack, Enchantments.AQUA_AFFINITY, 1);
                enchant(helper, stack, Enchantments.PUNCH, 2);
                var target = target(helper);
                var fish = hit(helper, player, target, stack);
                var tag = new CompoundTag();
                fish.saveWithoutId(tag);
                fish.discard();
                var restored = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
                restored.load(tag);
                restored.setOwner(player);
                helper.getLevel().addFreshEntity(restored);
                for (int tick = 0; tick < 200 && !restored.isRemoved(); tick++) restored.tick();
                helper.assertTrue(restored.isRemoved() && ItemStack.matches(player.getInventory().getItem(0), stack),
                        "Combined enchantments were lost during impact, save, or return");
                clearDrops(helper, target);
            }
        }
        helper.succeed();
    }
}
