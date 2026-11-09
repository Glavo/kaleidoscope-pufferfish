// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishHeadgearGameTests {
    private static List<DyeColor> colors() {
        var colors = new ArrayList<DyeColor>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        return colors;
    }

    private static ItemStack fish(DyeColor color) {
        var fish = new ItemStack(Items.PUFFERFISH);
        return color == null ? fish : PufferfishColors.dye(fish, color);
    }

    @GameTest(template = "empty")
    public static void allColorsEquipOneFishInHeadSlot(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var color : colors()) {
            player.getInventory().clearContent();
            var stack = fish(color).copyWithCount(12);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Headgear"));
            var slot = player.inventoryMenu.getSlot(5);
            helper.assertTrue(slot.mayPlace(stack) && slot.getMaxStackSize() == 1, "Head slot rejected fish or allowed a stack");
            helper.assertTrue(!player.inventoryMenu.getSlot(6).mayPlace(stack), "Fish fit in the chest slot");
            player.inventoryMenu.getSlot(9).set(stack);
            player.inventoryMenu.quickMoveStack(player, 9);
            var equipped = player.getItemBySlot(EquipmentSlot.HEAD);
            helper.assertTrue(equipped.getCount() == 1 && stack.getCount() == 11
                    && ItemStack.isSameItemSameComponents(equipped, stack), "Quick equip lost data or consumed the wrong count");
            helper.assertTrue(PufferfishHeadgear.isWearing(player), "Equipped fish was not recognized");
        }
        helper.assertTrue(!player.inventoryMenu.getSlot(5).mayPlace(new ItemStack(Items.PUFFERFISH_BUCKET)), "Bucket became headgear");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, fish(DyeColor.RED));
        helper.assertTrue(!PufferfishHeadgear.isWearing(player), "Held fish granted equipment effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void headgearRefreshesTenSecondsOnlyAboveWater(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int y = 0; y < 3; y++) helper.getLevel().setBlockAndUpdate(pos.above(y), Blocks.WATER.defaultBlockState());
        var variants = new ArrayList<ItemStack>();
        for (var color : colors()) variants.add(fish(color));
        variants.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        variants.add(new ItemStack(PufferfishItems.HEAVY.get()));
        for (var stack : variants) {
            player.removeAllEffects();
            player.setItemSlot(EquipmentSlot.HEAD, stack);
            player.setPos(pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5);
            player.setNoGravity(true);
            player.tick();
            helper.assertTrue(player.getEffect(MobEffects.WATER_BREATHING) != null
                    && player.getEffect(MobEffects.WATER_BREATHING).getDuration() == 200, "Headgear did not refresh ten seconds in air");
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            for (int tick = 0; tick < 205; tick++) player.tick();
            helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING)
                    && player.getAirSupply() < player.getMaxAirSupply(), "Headgear kept breathing indefinitely underwater");
            player.setPos(pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5);
            player.tick();
            helper.assertTrue(player.getEffect(MobEffects.WATER_BREATHING).getDuration() == 200, "Surfacing did not refresh breathing");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            for (int tick = 0; tick < 205; tick++) player.tick();
            helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING), "Unequipped fish kept refreshing breathing");
        }
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));
        player.setItemSlot(EquipmentSlot.HEAD, fish(null));
        player.tick();
        helper.assertTrue(player.getEffect(MobEffects.WATER_BREATHING).getDuration() > 200, "Headgear shortened an existing potion effect");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void livePufferfishCannotStingWearers(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "headgear-test-player"), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND),
                player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        for (int i = 0; i < 61; i++) {
            player.tick();
        }
        var previousHead = player.getItemBySlot(EquipmentSlot.HEAD);
        var fish = EntityType.PUFFERFISH.create(helper.getLevel());
        fish.setPuffState(2);
        try {
            player.setInvulnerable(false);
            player.getAbilities().invulnerable = false;
            player.setHealth(player.getMaxHealth());
            player.removeEffect(MobEffects.POISON);
            for (var color : colors()) {
                player.setItemSlot(EquipmentSlot.HEAD, fish(color));
                player.invulnerableTime = 0;
                fish.playerTouch(player);
                helper.assertTrue(player.getHealth() == player.getMaxHealth() && !player.hasEffect(MobEffects.POISON), "Pufferfish sting bypassed headgear");
            }
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.invulnerableTime = 0;
            fish.playerTouch(player);
            helper.assertTrue(player.getHealth() < player.getMaxHealth() && player.hasEffect(MobEffects.POISON), "Unequipped player remained immune");
        } finally {
            player.setItemSlot(EquipmentSlot.HEAD, previousHead);
            player.removeEffect(MobEffects.POISON);
            player.setHealth(player.getMaxHealth());
            player.invulnerableTime = 0;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void successfulAttacksPoisonAndOccasionallyReflect(GameTestHelper helper) {
        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = EntityType.PIG.create(helper.getLevel());
        int reflected = 0;
        helper.getLevel().getRandom().setSeed(4291);
        attacker.getRandom().setSeed(8301);
        for (var color : colors()) {
            wearer.setItemSlot(EquipmentSlot.HEAD, fish(color));
            for (int i = 0; i < 16; i++) {
                wearer.setHealth(wearer.getMaxHealth());
                wearer.invulnerableTime = 0;
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                attacker.removeEffect(MobEffects.POISON);
                helper.assertTrue(wearer.hurt(wearer.damageSources().mobAttack(attacker), 1), "Attack did not hit wearer");
                var poison = attacker.getEffect(MobEffects.POISON);
                helper.assertTrue(poison != null && poison.getDuration() == 60 && poison.getAmplifier() == 0, "Attacker did not receive exactly three seconds of Poison I");
                float damage = attacker.getMaxHealth() - attacker.getHealth();
                if (damage > 0) {
                    reflected++;
                    helper.assertTrue(damage >= 1 && damage <= 5 && attacker.getLastDamageSource().is(DamageTypes.THORNS), "Reflection differed from vanilla thorns damage");
                }
            }
        }
        helper.assertTrue(reflected > 0 && reflected < 272, "Thorns did not have a chance-based trigger");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void retaliationRespectsCanceledHitsAndDoesNotRecurse(GameTestHelper helper) {
        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        wearer.setItemSlot(EquipmentSlot.HEAD, fish(null));
        attacker.setItemSlot(EquipmentSlot.HEAD, fish(DyeColor.BLUE));
        Consumer<LivingIncomingDamageEvent> cancel = event -> {
            if (event.getEntity() == wearer) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            wearer.hurt(wearer.damageSources().playerAttack(attacker), 2);
            helper.assertTrue(!attacker.hasEffect(MobEffects.POISON) && attacker.getHealth() == attacker.getMaxHealth(), "Canceled attack triggered retaliation");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        wearer.invulnerableTime = 0;
        wearer.hurt(wearer.damageSources().thorns(attacker), 1);
        helper.assertTrue(!attacker.hasEffect(MobEffects.POISON) && attacker.getHealth() == attacker.getMaxHealth(), "Thorns recursively triggered retaliation");
        wearer.invulnerableTime = 0;
        wearer.setAbsorptionAmount(4);
        wearer.hurt(wearer.damageSources().playerAttack(attacker), 1);
        helper.assertTrue(attacker.hasEffect(MobEffects.POISON), "Absorption prevented retaliation");
        attacker.removeEffect(MobEffects.POISON);
        wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        wearer.invulnerableTime = 0;
        wearer.hurt(wearer.damageSources().playerAttack(attacker), 1);
        helper.assertTrue(!attacker.hasEffect(MobEffects.POISON), "Retaliation persisted after removal");
        helper.succeed();
    }
}
