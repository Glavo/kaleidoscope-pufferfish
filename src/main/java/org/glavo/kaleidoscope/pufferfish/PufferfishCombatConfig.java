// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.neoforged.neoforge.common.ModConfigSpec;

/// Defines server-authoritative melee, falling-smash, and throwing settings for handheld pufferfish.
public final class PufferfishCombatConfig {
    /// The combat configuration, synchronized from the server to connected clients.
    public static final ModConfigSpec SPEC;
    /// Multiplier applied to mace base damage and the unenchanted falling curve for normal and dyed fish.
    public static final ModConfigSpec.DoubleValue NORMAL_DAMAGE_MULTIPLIER;
    /// Multiplier applied to mace base damage and the unenchanted falling curve for netherite fish.
    public static final ModConfigSpec.DoubleValue NETHERITE_DAMAGE_MULTIPLIER;
    /// Multiplier applied to mace base damage and the unenchanted falling curve for heavy fish.
    public static final ModConfigSpec.DoubleValue HEAVY_DAMAGE_MULTIPLIER;
    /// Whether handheld pufferfish perform falling smash attacks.
    public static final ModConfigSpec.BooleanValue SMASH_ENABLED;
    /// Unscaled damage per fallen block within the first three blocks.
    public static final ModConfigSpec.DoubleValue FIRST_THREE_BLOCKS_DAMAGE;
    /// Unscaled damage per fallen block between three and eight blocks.
    public static final ModConfigSpec.DoubleValue NEXT_FIVE_BLOCKS_DAMAGE;
    /// Unscaled damage per fallen block beyond eight blocks.
    public static final ModConfigSpec.DoubleValue REMAINING_BLOCKS_DAMAGE;
    /// Density damage per enchantment level per fallen block for normal and dyed fish.
    public static final ModConfigSpec.DoubleValue NORMAL_DENSITY_DAMAGE;
    /// Density damage per enchantment level per fallen block for netherite fish.
    public static final ModConfigSpec.DoubleValue NETHERITE_DENSITY_DAMAGE;
    /// Density damage per enchantment level per fallen block for heavy fish.
    public static final ModConfigSpec.DoubleValue HEAVY_DENSITY_DAMAGE;
    /// Maximum falling damage bonus, including Density, before critical hits and armor; zero means uncapped.
    public static final ModConfigSpec.DoubleValue MAX_SMASH_BONUS;
    /// Initial normal and dyed fish projectile speed in blocks per tick, before spread and inherited player motion.
    public static final ModConfigSpec.DoubleValue NORMAL_THROW_SPEED;
    /// Initial netherite fish projectile speed in blocks per tick, before spread and inherited player motion.
    public static final ModConfigSpec.DoubleValue NETHERITE_THROW_SPEED;
    /// Initial heavy fish projectile speed in blocks per tick, before spread and inherited player motion.
    public static final ModConfigSpec.DoubleValue HEAVY_THROW_SPEED;
    /// Per-level Density penalty in the launch-speed divisor; zero disables the penalty.
    public static final ModConfigSpec.DoubleValue DENSITY_THROW_SPEED_PENALTY;
    /// Shared cooldown after a successful fish throw, in ticks; zero disables it.
    public static final ModConfigSpec.IntValue THROW_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue NORMAL_FLAIL_CHARGE_TICKS;
    public static final ModConfigSpec.IntValue NETHERITE_FLAIL_CHARGE_TICKS;
    public static final ModConfigSpec.DoubleValue FLAIL_DENSITY_CHARGE_PENALTY;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Handheld pufferfish melee settings. One damage point is half a heart.",
                "Does not change thrown pufferfish or flail damage. Reload the world/server after editing.").push("melee");
        NORMAL_DAMAGE_MULTIPLIER = builder.comment("Normal and dyed pufferfish multiplier for mace base damage (6) and the unenchanted falling curve.")
                .worldRestart().defineInRange("normalDamageMultiplier", 0.25, 0.01, 128.0);
        NETHERITE_DAMAGE_MULTIPLIER = builder.comment("Netherite pufferfish multiplier for mace base damage (6) and the unenchanted falling curve.")
                .worldRestart().defineInRange("netheriteDamageMultiplier", 0.5, 0.01, 128.0);
        HEAVY_DAMAGE_MULTIPLIER = builder.comment("Heavy pufferfish multiplier for mace base damage (6) and the unenchanted falling curve.")
                .worldRestart().defineInRange("heavyDamageMultiplier", 1.0, 0.01, 128.0);
        builder.pop().push("smash");
        SMASH_ENABLED = builder.comment("Enable mace-style attacks after falling more than 1.5 blocks, excluding elytra flight.")
                .worldRestart().define("enabled", true);
        FIRST_THREE_BLOCKS_DAMAGE = builder.comment("Unscaled bonus per fallen block within the first three blocks; multiplied by the fish damage multiplier.")
                .worldRestart().defineInRange("firstThreeBlocksDamage", 4.0, 0.0, 1024.0);
        NEXT_FIVE_BLOCKS_DAMAGE = builder.comment("Unscaled bonus per fallen block between three and eight blocks; multiplied by the fish damage multiplier.")
                .worldRestart().defineInRange("nextFiveBlocksDamage", 2.0, 0.0, 1024.0);
        REMAINING_BLOCKS_DAMAGE = builder.comment("Unscaled bonus per fallen block beyond eight blocks; multiplied by the fish damage multiplier.")
                .worldRestart().defineInRange("remainingBlocksDamage", 1.0, 0.0, 1024.0);
        NORMAL_DENSITY_DAMAGE = builder.comment("Normal and dyed fish Density bonus per level per fallen block. Level V adds 1 damage per block by default.",
                "Only applies to smash attacks and is not multiplied by normalDamageMultiplier.")
                .worldRestart().defineInRange("normalDensityDamagePerLevel", 0.2, 0.0, 1024.0);
        NETHERITE_DENSITY_DAMAGE = builder.comment("Netherite fish Density bonus per level per fallen block. Level V adds 1.5 damage per block by default.",
                "Only applies to smash attacks and is not multiplied by netheriteDamageMultiplier.")
                .worldRestart().defineInRange("netheriteDensityDamagePerLevel", 0.3, 0.0, 1024.0);
        HEAVY_DENSITY_DAMAGE = builder.comment("Heavy fish Density bonus per level per fallen block. Level V adds 2.5 damage per block by default.",
                "Only applies to smash attacks and is not multiplied by heavyDamageMultiplier.")
                .worldRestart().defineInRange("heavyDensityDamagePerLevel", 0.5, 0.0, 1024.0);
        MAX_SMASH_BONUS = builder.comment("Maximum combined falling bonus after the fish multiplier and Density, before critical hits and armor. Zero disables the cap.")
                .worldRestart().defineInRange("maxBonusDamage", 0.0, 0.0, 1048576.0);
        builder.pop().comment("Projectile launch speeds in blocks per tick, before spread and inherited player motion.",
                "Lower speed gives a shorter range at the same launch angle. Reload the world/server after editing.").push("throwing");
        NORMAL_THROW_SPEED = builder.comment("Normal and dyed pufferfish launch speed.")
                .worldRestart().defineInRange("normalSpeed", 1.5, 0.1, 3.0);
        NETHERITE_THROW_SPEED = builder.comment("Netherite pufferfish launch speed.")
                .worldRestart().defineInRange("netheriteSpeed", 1.0, 0.1, 3.0);
        HEAVY_THROW_SPEED = builder.comment("Heavy pufferfish launch speed.")
                .worldRestart().defineInRange("heavySpeed", 0.8, 0.1, 3.0);
        DENSITY_THROW_SPEED_PENALTY = builder.comment("Launch speed is base speed / (1 + this value * Density level), before spread and inherited player motion.",
                "Applies to normal, dyed, netherite, and heavy pufferfish. Zero disables the penalty; 0.2 halves speed at Density V.")
                .worldRestart().defineInRange("densitySpeedPenaltyPerLevel", 0.2, 0.0, 16.0);
        THROW_COOLDOWN_TICKS = builder.comment("Cooldown after a successful throw, shared by normal, dyed, netherite, and heavy pufferfish in both hands.",
                "20 ticks equal one second. Zero disables the cooldown. Does not affect flails.")
                .worldRestart().defineInRange("cooldownTicks", 20, 0, 1200);
        builder.pop().comment("Flail charge times. Weight affects charging, not launch speed or tether reach.").push("flail");
        NORMAL_FLAIL_CHARGE_TICKS = builder.comment("Ticks to fully charge ordinary and dyed flails.")
                .worldRestart().defineInRange("normalChargeTicks", 20, 3, 1200);
        NETHERITE_FLAIL_CHARGE_TICKS = builder.comment("Ticks to fully charge netherite flails.")
                .worldRestart().defineInRange("netheriteChargeTicks", 30, 3, 1200);
        FLAIL_DENSITY_CHARGE_PENALTY = builder.comment("Charge time multiplier is 1 + this value * Density level.")
                .worldRestart().defineInRange("densityChargePenaltyPerLevel", 0.2, 0.0, 16.0);
        builder.pop();
        SPEC = builder.build();
    }

    /// Prevents construction of the configuration holder.
    private PufferfishCombatConfig() {}

    /// Returns the configured value, or its default before the server configuration has loaded.
    public static <T> T value(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
