// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/// Repairs one offhand fish per completed pump stroke; interrupted strokes restore nothing.
public final class PufferfishPumpItem extends Item {
    public static final int STROKE_TICKS = 20;

    public PufferfishPumpItem(Properties properties) { super(properties); }

    public static boolean canInflate(ItemStack fish) {
        return PufferfishItems.isWeapon(fish) && fish.getCount() == 1 && fish.isDamaged();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !canInflate(player.getOffhandItem())) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack pump, int remaining) {
        if (!user.isUsingItem() || user.getUseItem() != pump) return;
        if (user.getUsedItemHand() != InteractionHand.MAIN_HAND || !canInflate(user.getOffhandItem())) {
            user.stopUsingItem();
            return;
        }
        int elapsed = getUseDuration(pump, user) - remaining;
        if (!(level instanceof ServerLevel server) || elapsed <= 0 || elapsed % STROKE_TICKS != 0) return;
        var fish = user.getOffhandItem();
        fish.setDamageValue(Math.max(0, fish.getDamageValue() - (fish.getMaxDamage() + 3) / 4));
        PufferfishDurability.normalize(fish);
        pump.hurtAndBreak(1, server, user, item -> user.onEquippedItemBroken(item, EquipmentSlot.MAINHAND));
        level.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PUFFER_FISH_BLOW_UP,
                SoundSource.PLAYERS, 0.6F, 1.1F);
        if (pump.isEmpty() || !canInflate(fish)) user.stopUsingItem();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) { return 10; }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return ingredient.is(Items.COPPER_INGOT); }
}
