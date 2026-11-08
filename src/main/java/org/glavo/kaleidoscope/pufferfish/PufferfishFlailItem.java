// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;

/// A reusable melee weapon whose tethered head can be launched without consuming the item.
public final class PufferfishFlailItem extends Item {
    /// The cooldown in ticks shared by both flail variants after a successful launch.
    public static final int COOLDOWN = 30;

    /// Creates a flail with the supplied item properties.
    ///
    /// @param properties the item properties, including a maximum stack size of one
    public PufferfishFlailItem(Properties properties) {
        super(properties);
    }

    /// Launches a head from the used hand and applies the cooldown only if spawning succeeds.
    /// Spectators and players whose flail is cooling down cannot launch a head.
    /// The held stack is neither consumed nor transferred to the projectile.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            var head = new PufferfishFlailHead(level, player, hand);
            head.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.2F, 0);
            if (!level.addFreshEntity(head)) return InteractionResultHolder.fail(stack);
            player.getCooldowns().addCooldown(PufferfishItems.FLAIL.get(), COOLDOWN);
            player.getCooldowns().addCooldown(PufferfishItems.NETHERITE_FLAIL.get(), COOLDOWN);
            player.awardStat(Stats.ITEM_USED.get(this));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_HIT,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
