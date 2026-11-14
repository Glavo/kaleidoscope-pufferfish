// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;

/// A reusable weapon with short left-click casts and charged right-click casts.
public final class PufferfishFlailItem extends Item {
    /// The cooldown in ticks shared by all flail variants after a successful launch.
    public static final int COOLDOWN = 30;
    public static final int MIN_CHARGE_TICKS = 3;
    private final java.util.function.Supplier<? extends Item> fish;

    /// Creates a flail with the supplied item properties.
    ///
    /// @param properties the item properties, including a maximum stack size of one
    public PufferfishFlailItem(Properties properties, java.util.function.Supplier<? extends Item> fish) {
        super(properties);
        this.fish = fish;
    }

    /// Returns the fish item represented by this flail without copying stack components.
    public Item fishItem() { return fish.get(); }

    /// Copies the flail's components onto its corresponding fish item.
    public ItemStack fish(ItemStack stack) { return stack.transmuteCopy(fishItem(), 1); }

    public static int chargeTicks(ItemStack stack) {
        int base = PufferfishCombatConfig.value(PufferfishItems.hasHeavyCombat(stack) ? PufferfishCombatConfig.HEAVY_FLAIL_CHARGE_TICKS
                : PufferfishItems.hasNetheriteCombat(stack)
                ? PufferfishCombatConfig.NETHERITE_FLAIL_CHARGE_TICKS : PufferfishCombatConfig.NORMAL_FLAIL_CHARGE_TICKS);
        int density = stack.getEnchantments().entrySet().stream().filter(entry -> entry.getKey().is(Enchantments.DENSITY))
                .mapToInt(entry -> entry.getIntValue()).findFirst().orElse(0);
        return (int) Math.min(72000, Math.ceil(base * (1 + density * PufferfishCombatConfig.value(PufferfishCombatConfig.FLAIL_DENSITY_CHARGE_PENALTY))));
    }

    public static float charge(ItemStack stack, float ticks) {
        return Mth.clamp(ticks / chargeTicks(stack), 0, 1);
    }

    /// Integrates a steadily increasing angular speed, continuing smoothly after full charge.
    public static double spinAngle(ItemStack stack, float ticks) {
        double duration = chargeTicks(stack);
        double ramp = Math.min(Math.max(0, ticks), duration);
        return 0.08 * ticks + 0.47 * (ramp * ramp / (2 * duration) + Math.max(0, ticks - duration));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    /// Starts charging without consuming the item or applying a cooldown.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(this) || activeHead(player) != null) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            var head = new PufferfishFlailHead(level, player, hand, 0);
            head.beginCharging();
            if (!level.addFreshEntity(head)) return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public static PufferfishFlailHead activeHead(Player player) {
        return player.level().getEntitiesOfClass(PufferfishFlailHead.class, player.getBoundingBox().inflate(16),
                head -> head.getOwner() == player && !head.isRemoved()).stream().findFirst().orElse(null);
    }

    /// Casts once from the main hand without consuming the weapon or dealing direct melee damage.
    public static boolean attack(Player player) {
        var stack = player.getMainHandItem();
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator() || player.isUsingItem()
                || !PufferfishItems.isFlail(stack) || player.getCooldowns().isOnCooldown(stack.getItem())
                || activeHead(player) != null) return false;
        var head = new PufferfishFlailHead(player.level(), player, InteractionHand.MAIN_HAND);
        head.beginShortCast();
        if (!player.level().addFreshEntity(head)) return false;
        int cooldown = PufferfishCombatConfig.value(PufferfishCombatConfig.THROW_COOLDOWN_TICKS);
        if (cooldown > 0) {
            PufferfishItems.FLAIL_VARIANTS.forEach(item -> player.getCooldowns().addCooldown(item.get(), cooldown));
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.resetAttackStrengthTicker();
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        attack(player);
        return true;
    }

    /// Locks damage and aim, allowing the same head to finish its swing before release.
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTicks) {
        if (!(entity instanceof Player player) || player.isSpectator()
                || player.getCooldowns().isOnCooldown(this) || player.getUseItem() != stack
                || player.getItemInHand(player.getUsedItemHand()) != stack) return;
        int elapsed = getUseDuration(stack, player) - remainingTicks;
        var head = activeHead(player);
        if (head == null || !head.isCharging()) return;
        if (elapsed < MIN_CHARGE_TICKS) head.discard();
        else head.queueRelease(elapsed, player.getYRot(), player.getXRot());
    }
}
