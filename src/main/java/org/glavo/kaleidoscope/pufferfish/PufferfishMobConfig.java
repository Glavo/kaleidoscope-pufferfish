// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

/// Living fish attributes are independent of item weapon balance.
public final class PufferfishMobConfig {
    public enum Kind { COPPER, IRON, GOLDEN, DIAMOND, NETHERITE, HEAVY, RADIANT }

    public static final ModConfigSpec SPEC;
    private static final Map<Kind, Settings> SETTINGS = new EnumMap<>(Kind.class);

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Living pufferfish balance. Reload the world after editing; existing fish update when loaded.");
        add(builder, Kind.COPPER, 16, 4, 2, 1, 0, 24, 1);
        add(builder, Kind.IRON, 20, 5, 4, 0.85, 0.15, 30, 1);
        add(builder, Kind.GOLDEN, 30, 3, 2, 1, 0, 30, 1);
        add(builder, Kind.DIAMOND, 40, 8, 8, 1, 0.6, 30, 0.5);
        add(builder, Kind.NETHERITE, 60, 10, 12, 0.9, 0.75, 26, 0.25);
        add(builder, Kind.HEAVY, 80, 12, 16, 0.5, 0.9, 50, 0.25);
        add(builder, Kind.RADIANT, 60, 8, 8, 0.9, 0.3, 30, 0.25);
        SPEC = builder.build();
    }

    private PufferfishMobConfig() {}

    private static void add(ModConfigSpec.Builder builder, Kind kind, double health, double damage, double armor,
            double speed, double resistance, int cooldown, double capture) {
        builder.push(kind.name().toLowerCase(java.util.Locale.ROOT));
        SETTINGS.put(kind, new Settings(
                builder.worldRestart().defineInRange("health", health, 1, 1024),
                builder.worldRestart().defineInRange("attackDamage", damage, 0, 1024),
                builder.worldRestart().defineInRange("armor", armor, 0, 30),
                builder.worldRestart().defineInRange("movementSpeed", speed, 0.05, 2),
                builder.worldRestart().defineInRange("knockbackResistance", resistance, 0, 1),
                builder.comment("Shared contact and attack cooldown, in ticks.").worldRestart().defineInRange("attackCooldownTicks", cooldown, 1, 1200),
                builder.comment("Maximum health fraction for bucket capture, inclusive; 1 allows capture at full health.")
                        .worldRestart().defineInRange("captureHealthFraction", capture, 0.01, 1)));
        builder.pop();
    }

    public static Kind kind(ItemStack fish) {
        if (fish.is(PufferfishItems.IRON.get())) return Kind.IRON;
        if (fish.is(PufferfishItems.GOLDEN.get())) return Kind.GOLDEN;
        if (fish.is(PufferfishItems.DIAMOND.get())) return Kind.DIAMOND;
        if (fish.is(PufferfishItems.NETHERITE.get())) return Kind.NETHERITE;
        if (fish.is(PufferfishItems.HEAVY.get())) return Kind.HEAVY;
        if (fish.is(PufferfishItems.RADIANT.get())) return Kind.RADIANT;
        return Kind.COPPER;
    }

    public static Settings settings(ItemStack fish) { return SETTINGS.get(kind(fish)); }

    public static <T> T value(ModConfigSpec.ConfigValue<T> value) { return SPEC.isLoaded() ? value.get() : value.getDefault(); }

    public static AttributeSupplier attributes(ItemStack fish) {
        var config = settings(fish);
        return AbstractFish.createAttributes()
                .add(Attributes.MAX_HEALTH, value(config.health()))
                .add(Attributes.ATTACK_DAMAGE, value(config.damage()))
                .add(Attributes.ARMOR, value(config.armor()))
                .add(Attributes.MOVEMENT_SPEED, value(config.speed()))
                .add(Attributes.KNOCKBACK_RESISTANCE, value(config.resistance()))
                .add(Attributes.FOLLOW_RANGE, 12).build();
    }

    public record Settings(ModConfigSpec.DoubleValue health, ModConfigSpec.DoubleValue damage, ModConfigSpec.DoubleValue armor,
            ModConfigSpec.DoubleValue speed, ModConfigSpec.DoubleValue resistance, ModConfigSpec.IntValue cooldown,
            ModConfigSpec.DoubleValue capture) {}
}
