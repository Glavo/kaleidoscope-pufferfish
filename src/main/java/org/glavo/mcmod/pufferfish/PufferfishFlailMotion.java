// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/// Shared orbit and release geometry for server collisions and client rendering.
public final class PufferfishFlailMotion {
    private PufferfishFlailMotion() {}

    public static Vec3 grip(Player player, InteractionHand hand, float yaw, float partialTick) {
        return center(player, hand, yaw, partialTick, 0.45, 0.35, -0.65);
    }

    private static Vec3 center(Player player, InteractionHand hand, float yaw, float partialTick, double lateral, double forward, double vertical) {
        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? -lateral : lateral;
        double radians = yaw * Mth.DEG_TO_RAD;
        double scale = player.getScale();
        return player.getEyePosition(partialTick).add((side * Math.cos(radians) - forward * Math.sin(radians)) * scale,
                vertical * scale, (side * Math.sin(radians) + forward * Math.cos(radians)) * scale);
    }

    public static Vec3 orbit(Player player, InteractionHand hand, ItemStack stack, double age, float yaw, float partialTick) {
        double angle = PufferfishFlailItem.spinAngle(stack, (float) age);
        double forward = -1.10 * Math.sin(angle) * player.getScale();
        double radians = yaw * Mth.DEG_TO_RAD;
        return center(player, hand, yaw, partialTick, 0.95, 0, -0.35).add(-forward * Math.sin(radians),
                -1.10 * Math.cos(angle) * player.getScale(), forward * Math.cos(radians));
    }

    /// Finds the next point whose forward tangent matches the requested pitch.
    public static float releaseAge(ItemStack stack, float age, float pitch) {
        double angle = PufferfishFlailItem.spinAngle(stack, age);
        double phase = Math.PI + Mth.clamp(pitch, -90, 90) * Mth.DEG_TO_RAD;
        double target = phase + Math.ceil((angle - phase) / (Math.PI * 2)) * Math.PI * 2;
        double duration = PufferfishFlailItem.chargeTicks(stack);
        double rampAngle = 0.315 * duration;
        double result = target <= rampAngle
                ? 2 * target / (0.08 + Math.sqrt(0.08 * 0.08 + 0.94 * target / duration))
                : duration + (target - rampAngle) / 0.55;
        return (float) Math.max(age, result);
    }

    public static Vec3 tangent(ItemStack stack, float age, float yaw) {
        double angle = PufferfishFlailItem.spinAngle(stack, age);
        double radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(Math.sin(radians) * Math.cos(angle), Math.sin(angle), -Math.cos(radians) * Math.cos(angle));
    }
}
