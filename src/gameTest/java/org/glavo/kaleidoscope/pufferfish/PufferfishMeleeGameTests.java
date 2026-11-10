// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishMeleeGameTests {
    private static List<ItemStack> variants() {
        var result = new ArrayList<ItemStack>();
        result.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) result.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        result.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        return result;
    }

    private static Player player(GameTestHelper helper, ItemStack stack) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 1, 0));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        charge(player);
        return player;
    }

    private static void charge(Player player) {
        for (int tick = 0; tick < 22; tick++) player.tick();
        player.setOnGround(true);
        player.fallDistance = 0;
    }

    private static LivingEntity target(GameTestHelper helper) {
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        return target;
    }

    private static void enchant(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int rank) {
        var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(holder, rank));
    }

    @GameTest(template = "empty")
    public static void everyOrdinaryFishHasConfiguredWeaponDamageSpeedAndPoison(GameTestHelper helper) {
        for (var stack : variants()) {
            var player = player(helper, stack.copyWithCount(4));
            var target = target(helper);
            boolean netherite = PufferfishItems.isNetherite(stack);
            helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == (netherite ? 6 : 1)
                    && player.getAttributeValue(Attributes.ATTACK_SPEED) == (netherite ? 2 : 3), "Incorrect pufferfish melee attributes");
            player.attack(target);
            helper.assertTrue(target.getHealth() == 20 - (netherite ? 6 : 1), "Charged melee damage does not match the configured base damage");
            var poison = target.getEffect(MobEffects.POISON);
            helper.assertTrue(poison != null && poison.getDuration() == 60 && poison.getAmplifier() == (netherite ? 1 : 0),
                    "Melee poison differs from throwing");
            helper.assertTrue(player.getMainHandItem().getCount() == 1 && player.getInventory().countItem(stack.getItem()) == 4
                    && ItemStack.isSameItemSameComponents(PufferfishDurabilityGameTests.worn(stack), player.getMainHandItem()),
                    "Melee did not separate and wear exactly one fish");
            target.discard();
        }
        helper.succeed();
    }

    /// Verifies flat Density and Breach damage against armor.
    @GameTest(template = "empty")
    public static void meleeEnchantmentsRespectArmorAndFlatDensity(GameTestHelper helper) {
        for (boolean netherite : new boolean[] {false, true}) {
            float baseline = 0;
            for (var key : List.of(Enchantments.DENSITY, Enchantments.BREACH)) {
                int max = key == Enchantments.DENSITY ? 5 : 4;
                float previous = 0;
                for (int rank = 0; rank <= max; rank++) {
                    var stack = new ItemStack(netherite ? PufferfishItems.NETHERITE.get() : Items.PUFFERFISH);
                    if (rank > 0) enchant(helper, stack, key, rank);
                    var player = player(helper, stack);
                    var melee = target(helper);
                    melee.getAttribute(Attributes.ARMOR).setBaseValue(20);
                    melee.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
                    player.attack(melee);
                    float damage = 20 - melee.getHealth();
                    if (rank == 0) baseline = damage;
                    if (key == Enchantments.DENSITY) {
                        float base = (netherite ? 6 : 1) + (rank == 0 ? 0 : 0.5F + 0.5F * rank);
                        float expected = base * (1 - (20 - base / 4) / 25);
                        helper.assertTrue(Math.abs(damage - expected) < 0.0001, "Density ignored flat damage or armor");
                    } else if (rank > 0) {
                        helper.assertTrue(damage > previous && damage <= (netherite ? 6 : 1), "Breach did not progressively reduce armor protection");
                    }
                    previous = damage;
                    melee.discard();
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cooldownMatchesWeaponSpeedAndOffhandDoesNotGrantMeleeBonuses(GameTestHelper helper) {
        var player = player(helper, new ItemStack(Items.PUFFERFISH));
        float delay = player.getCurrentItemAttackStrengthDelay();
        player.resetAttackStrengthTicker();
        for (int i = 0; i < 3; i++) player.tick();
        float half = player.getAttackStrengthScale(0);
        helper.assertTrue(Math.abs(delay - 20F / 3) < 0.001 && Math.abs(half - 0.45F) < 0.001, "Normal fish did not recharge at three attacks per second");
        var target = target(helper);
        player.setOnGround(true);
        player.attack(target);
        helper.assertTrue(target.getHealth() > 18 && target.getHealth() < 20, "Uncharged attacks ignored damage scaling");
        target.discard();
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(PufferfishItems.NETHERITE.get()));
        player.tick();
        helper.assertTrue(player.getCurrentItemAttackStrengthDelay() == 10, "Netherite fish did not recharge at two attacks per second");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(PufferfishItems.NETHERITE.get()));
        charge(player);
        helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 1 && player.getAttributeValue(Attributes.ATTACK_SPEED) == 4,
                "Offhand pufferfish granted mainhand attack attributes");
        target = target(helper);
        player.attack(target);
        helper.assertTrue(target.getHealth() == 19 && !target.hasEffect(MobEffects.POISON), "Offhand fish poisoned an empty-handed attack");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void meleePunchRespectsResistanceAndFailedHits(GameTestHelper helper) {
        for (double resistance : new double[] {0, 0.5, 1}) {
            double baseline = 0;
            for (int rank = 0; rank <= 2; rank++) {
                var stack = new ItemStack(Items.PUFFERFISH);
                enchant(helper, stack, Enchantments.PUNCH, rank);
                var player = player(helper, stack);
                var target = target(helper);
                target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(resistance);
                player.attack(target);
                double movement = target.getDeltaMovement().z;
                if (rank == 0) baseline = movement;
                // Vanilla melee knockback halves existing horizontal velocity after the damage callback.
                helper.assertTrue(Math.abs(movement - baseline - rank * 0.3 * (1 - resistance)) < 0.0001, "Melee Punch applied the wrong knockback");
                target.discard();
            }
        }
        var stack = new ItemStack(PufferfishItems.NETHERITE.get());
        enchant(helper, stack, Enchantments.PUNCH, 2);
        var player = player(helper, stack);
        var target = target(helper);
        Consumer<LivingIncomingDamageEvent> cancel = event -> { if (event.getEntity() == target) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            player.attack(target);
            helper.assertTrue(target.getHealth() == 20 && !target.hasEffect(MobEffects.POISON) && target.getDeltaMovement().equals(Vec3.ZERO),
                    "Canceled melee hit applied damage, poison, or Punch");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        target.discard();
        helper.succeed();
    }
}
