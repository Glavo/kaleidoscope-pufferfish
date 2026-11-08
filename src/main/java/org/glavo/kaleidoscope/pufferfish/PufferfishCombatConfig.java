// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.neoforged.neoforge.common.ModConfigSpec;

/// Defines server-authoritative melee damage and falling-smash settings for handheld pufferfish.
public final class PufferfishCombatConfig {
    /// The combat configuration, synchronized from the server to connected clients.
    public static final ModConfigSpec SPEC;
    /// Total unenchanted normal and dyed pufferfish melee damage at full attack strength.
    public static final ModConfigSpec.DoubleValue NORMAL_MELEE_DAMAGE;
    /// Total unenchanted netherite pufferfish melee damage at full attack strength.
    public static final ModConfigSpec.DoubleValue NETHERITE_MELEE_DAMAGE;
    /// Whether handheld pufferfish perform falling smash attacks.
    public static final ModConfigSpec.BooleanValue SMASH_ENABLED;
    /// Added damage per fallen block within the first three blocks.
    public static final ModConfigSpec.DoubleValue FIRST_THREE_BLOCKS_DAMAGE;
    /// Added damage per fallen block between three and eight blocks.
    public static final ModConfigSpec.DoubleValue NEXT_FIVE_BLOCKS_DAMAGE;
    /// Added damage per fallen block beyond eight blocks.
    public static final ModConfigSpec.DoubleValue REMAINING_BLOCKS_DAMAGE;
    /// Maximum falling damage bonus before critical-hit and armor calculations; zero means uncapped.
    public static final ModConfigSpec.DoubleValue MAX_SMASH_BONUS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Handheld pufferfish melee settings. One damage point is half a heart.",
                "Does not change thrown pufferfish or flail damage. Reload the world/server after editing.").push("melee");
        NORMAL_MELEE_DAMAGE = builder.comment("Normal and dyed pufferfish total base melee damage, before enchantments and critical hits.")
                .worldRestart().defineInRange("normalDamage", 2.0, 1.0, 1024.0);
        NETHERITE_MELEE_DAMAGE = builder.comment("Netherite pufferfish total base melee damage, before enchantments and critical hits.")
                .worldRestart().defineInRange("netheriteDamage", 10.0, 1.0, 1024.0);
        builder.pop().push("smash");
        SMASH_ENABLED = builder.comment("Enable mace-style attacks after falling more than 1.5 blocks, excluding elytra flight.")
                .worldRestart().define("enabled", true);
        FIRST_THREE_BLOCKS_DAMAGE = builder.comment("Bonus damage per fallen block within the first three blocks.")
                .worldRestart().defineInRange("firstThreeBlocksDamage", 4.0, 0.0, 1024.0);
        NEXT_FIVE_BLOCKS_DAMAGE = builder.comment("Bonus damage per fallen block between three and eight blocks.")
                .worldRestart().defineInRange("nextFiveBlocksDamage", 2.0, 0.0, 1024.0);
        REMAINING_BLOCKS_DAMAGE = builder.comment("Bonus damage per fallen block beyond eight blocks.")
                .worldRestart().defineInRange("remainingBlocksDamage", 1.0, 0.0, 1024.0);
        MAX_SMASH_BONUS = builder.comment("Maximum falling bonus before critical hits and armor. Zero disables the cap.",
                "Existing Density flat-damage bonuses remain separate from this falling bonus.")
                .worldRestart().defineInRange("maxBonusDamage", 0.0, 0.0, 1048576.0);
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
