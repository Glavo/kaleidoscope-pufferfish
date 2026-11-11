// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.neoforged.neoforge.common.ModConfigSpec;

/// Defines server-authoritative melee, falling-smash, and throwing settings for handheld pufferfish.
public final class PufferfishCombatConfig {
    /// The combat configuration, synchronized from the server to connected clients.
    public static final ModConfigSpec SPEC;
    /// Base damage shared by normal and dyed fish melee attacks and projectiles.
    public static final ModConfigSpec.DoubleValue NORMAL_DAMAGE;
    /// Base damage shared by netherite fish melee attacks and projectiles.
    public static final ModConfigSpec.DoubleValue NETHERITE_DAMAGE;
    public static final ModConfigSpec.DoubleValue RADIANT_DAMAGE;
    public static final ModConfigSpec.DoubleValue COPPER_DAMAGE;
    public static final ModConfigSpec.DoubleValue IRON_DAMAGE;
    public static final ModConfigSpec.DoubleValue DIAMOND_DAMAGE;
    public static final ModConfigSpec.DoubleValue GOLDEN_DAMAGE;
    public static final ModConfigSpec.DoubleValue NORMAL_ATTACK_SPEED;
    public static final ModConfigSpec.DoubleValue NETHERITE_ATTACK_SPEED;
    /// Multiplier applied to mace base damage and the unenchanted falling curve for heavy fish.
    public static final ModConfigSpec.DoubleValue HEAVY_DAMAGE_MULTIPLIER;
    /// Whether heavy pufferfish perform falling smash attacks.
    public static final ModConfigSpec.BooleanValue SMASH_ENABLED;
    /// Unscaled damage per fallen block within the first three blocks.
    public static final ModConfigSpec.DoubleValue FIRST_THREE_BLOCKS_DAMAGE;
    /// Unscaled damage per fallen block between three and eight blocks.
    public static final ModConfigSpec.DoubleValue NEXT_FIVE_BLOCKS_DAMAGE;
    /// Unscaled damage per fallen block beyond eight blocks.
    public static final ModConfigSpec.DoubleValue REMAINING_BLOCKS_DAMAGE;
    /// Heavy fish Density damage per enchantment level per fallen block.
    public static final ModConfigSpec.DoubleValue HEAVY_DENSITY_DAMAGE;
    /// Maximum heavy-fish falling damage bonus before critical hits and armor; zero means uncapped.
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
    public static final ModConfigSpec.IntValue HEAVY_FLAIL_CHARGE_TICKS;
    public static final ModConfigSpec.DoubleValue FLAIL_DENSITY_CHARGE_PENALTY;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Pufferfish weapon settings. One damage point is half a heart.",
                "Fish base damage also applies to thrown fish and corresponding flail projectiles.",
                "Reload the world/server after editing.").push("melee");
        NORMAL_DAMAGE = builder.comment("Normal and dyed pufferfish base melee and throwing damage, before enchantments.")
                .worldRestart().defineInRange("normalDamage", 1.0, 0.0, 1024.0);
        NETHERITE_DAMAGE = builder.comment("Netherite pufferfish base melee and throwing damage, before enchantments.")
                .worldRestart().defineInRange("netheriteDamage", 6.0, 0.0, 1024.0);
        RADIANT_DAMAGE = builder.comment("Radiant pufferfish base melee and throwing damage, also used by its flail before charge scaling.")
                .worldRestart().defineInRange("radiantDamage", 8.0, 0.0, 1024.0);
        COPPER_DAMAGE = builder.comment("Copper pufferfish melee and throwing damage; defaults to stone pickaxe damage.")
                .worldRestart().defineInRange("copperDamage", 3.0, 0.0, 1024.0);
        IRON_DAMAGE = builder.comment("Iron pufferfish melee and throwing damage; defaults to iron pickaxe damage.")
                .worldRestart().defineInRange("ironDamage", 4.0, 0.0, 1024.0);
        DIAMOND_DAMAGE = builder.comment("Diamond pufferfish melee and throwing damage; defaults to diamond pickaxe damage.")
                .worldRestart().defineInRange("diamondDamage", 5.0, 0.0, 1024.0);
        GOLDEN_DAMAGE = builder.comment("Golden pufferfish melee damage; defaults to golden pickaxe damage. Golden fish cannot be thrown.")
                .worldRestart().defineInRange("goldenDamage", 2.0, 0.0, 1024.0);
        NORMAL_ATTACK_SPEED = builder.comment("Normal and dyed pufferfish attacks per second.")
                .worldRestart().defineInRange("normalAttackSpeed", 3.0, 0.1, 20.0);
        NETHERITE_ATTACK_SPEED = builder.comment("Netherite pufferfish attacks per second.")
                .worldRestart().defineInRange("netheriteAttackSpeed", 2.0, 0.1, 20.0);
        HEAVY_DAMAGE_MULTIPLIER = builder.comment("Heavy pufferfish multiplier for mace base damage (6) and the unenchanted falling curve.")
                .worldRestart().defineInRange("heavyDamageMultiplier", 1.0, 0.01, 128.0);
        builder.pop().push("smash");
        SMASH_ENABLED = builder.comment("Enable heavy pufferfish mace-style attacks after falling more than 1.5 blocks, excluding elytra flight.")
                .worldRestart().define("enabled", true);
        FIRST_THREE_BLOCKS_DAMAGE = builder.comment("Unscaled bonus per fallen block within the first three blocks; multiplied by the fish damage multiplier.")
                .worldRestart().defineInRange("firstThreeBlocksDamage", 4.0, 0.0, 1024.0);
        NEXT_FIVE_BLOCKS_DAMAGE = builder.comment("Unscaled bonus per fallen block between three and eight blocks; multiplied by the fish damage multiplier.")
                .worldRestart().defineInRange("nextFiveBlocksDamage", 2.0, 0.0, 1024.0);
        REMAINING_BLOCKS_DAMAGE = builder.comment("Unscaled bonus per fallen block beyond eight blocks; multiplied by the fish damage multiplier.")
                .worldRestart().defineInRange("remainingBlocksDamage", 1.0, 0.0, 1024.0);
        HEAVY_DENSITY_DAMAGE = builder.comment("Heavy fish Density bonus per level per fallen block. Level V adds 2.5 damage per block by default.",
                "Only applies to smash attacks and is not multiplied by heavyDamageMultiplier.")
                .worldRestart().defineInRange("heavyDensityDamagePerLevel", 0.5, 0.0, 1024.0);
        MAX_SMASH_BONUS = builder.comment("Maximum combined falling bonus after the heavy fish multiplier and Density, before critical hits and armor. Zero disables the cap.")
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
                "Also sets the cooldown for short flail casts; charged flail casts keep their own cooldown.",
                "20 ticks equal one second. Zero disables the cooldown.")
                .worldRestart().defineInRange("cooldownTicks", 20, 0, 1200);
        builder.pop().comment("Flail charge times. Weight affects charging, not launch speed or tether reach.").push("flail");
        NORMAL_FLAIL_CHARGE_TICKS = builder.comment("Ticks to fully charge ordinary and dyed flails.")
                .worldRestart().defineInRange("normalChargeTicks", 20, 3, 1200);
        NETHERITE_FLAIL_CHARGE_TICKS = builder.comment("Ticks to fully charge netherite flails.")
                .worldRestart().defineInRange("netheriteChargeTicks", 30, 3, 1200);
        HEAVY_FLAIL_CHARGE_TICKS = builder.comment("Ticks to fully charge heavy flails.")
                .worldRestart().defineInRange("heavyChargeTicks", 40, 3, 1200);
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
