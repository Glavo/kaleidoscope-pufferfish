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
    public static final ModConfigSpec.DoubleValue ZOMBIE_HEADGEAR_CHANCE;
    public static final Map<PufferfishZombieHeadgear.Variant, ModConfigSpec.IntValue> ZOMBIE_HEADGEAR_WEIGHTS;
    public static final ModConfigSpec.DoubleValue REINFORCEMENT_CHANCE;
    public static final ModConfigSpec.IntValue REINFORCEMENT_COOLDOWN;
    public static final ModConfigSpec.IntValue REINFORCEMENT_MAX_CALLS;
    public static final ModConfigSpec.IntValue REINFORCEMENT_NEARBY_LIMIT;
    public static final ModConfigSpec.IntValue REINFORCEMENT_RADIUS;
    public static final Map<Kind, ModConfigSpec.IntValue> REINFORCEMENT_WEIGHTS;
    private static final Map<Kind, Settings> SETTINGS = new EnumMap<>(Kind.class);

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Living pufferfish balance. Reload the world after editing; existing fish update when loaded.");
        add(builder, Kind.COPPER, 16, 4, 2, 1, 0, 0.3, 24, 1);
        add(builder, Kind.IRON, 20, 5, 4, 0.85, 0, 0.3, 30, 1);
        add(builder, Kind.GOLDEN, 30, 3, 2, 1, 0, 0.3, 30, 1);
        add(builder, Kind.DIAMOND, 40, 8, 8, 1, 0, 0.5, 30, 0.5);
        add(builder, Kind.NETHERITE, 60, 10, 12, 0.9, 0, 0.75, 26, 0.25);
        add(builder, Kind.HEAVY, 80, 12, 16, 0.5, 0.75, 1, 50, 0.25);
        add(builder, Kind.RADIANT, 60, 8, 8, 0.9, 0, 0.5, 30, 0.25);
        builder.push("reinforcements");
        REINFORCEMENT_CHANCE = builder.comment("Chance per successful hit against a living heavy or radiant fish; 0 disables reinforcements.")
                .defineInRange("chance", 0.25, 0, 1);
        REINFORCEMENT_COOLDOWN = builder.comment("Ticks between successful calls.").defineInRange("cooldownTicks", 400, 1, 72000);
        REINFORCEMENT_MAX_CALLS = builder.comment("Lifetime successful calls per elite, preserved through saving and bucket capture.")
                .defineInRange("maxCalls", 3, 0, 100);
        REINFORCEMENT_NEARBY_LIMIT = builder.comment("Maximum nearby living material fish, excluding the caller.")
                .defineInRange("nearbyLimit", 8, 1, 100);
        REINFORCEMENT_RADIUS = builder.comment("Horizontal search radius in blocks; spawn positions must be at least two blocks from the caller.")
                .defineInRange("radius", 8, 2, 32);
        builder.push("weights");
        var weights = new EnumMap<Kind, ModConfigSpec.IntValue>(Kind.class);
        weights.put(Kind.COPPER, builder.defineInRange("copper", 40, 0, 10000));
        weights.put(Kind.IRON, builder.defineInRange("iron", 30, 0, 10000));
        weights.put(Kind.GOLDEN, builder.defineInRange("golden", 15, 0, 10000));
        weights.put(Kind.DIAMOND, builder.defineInRange("diamond", 12, 0, 10000));
        weights.put(Kind.NETHERITE, builder.defineInRange("netherite", 3, 0, 10000));
        REINFORCEMENT_WEIGHTS = java.util.Collections.unmodifiableMap(weights);
        builder.pop(2);
        builder.push("zombieHeadgear");
        ZOMBIE_HEADGEAR_CHANCE = builder.comment("Chance for a naturally spawned ordinary zombie with an empty head slot to wear a pufferfish; 0 disables this.")
                .defineInRange("chance", 0.05, 0, 1);
        builder.comment("Relative headgear weights after the chance roll. Zero excludes a variant; all zero disables headgear.")
                .push("weights");
        var headgearWeights = new EnumMap<PufferfishZombieHeadgear.Variant, ModConfigSpec.IntValue>(PufferfishZombieHeadgear.Variant.class);
        headgearWeights.put(PufferfishZombieHeadgear.Variant.NORMAL, builder.defineInRange("normal", 80, 0, 10000));
        headgearWeights.put(PufferfishZombieHeadgear.Variant.COPPER,
                builder.comment("Shared weight for all eight copper variants, selected uniformly.").defineInRange("copper", 10, 0, 10000));
        headgearWeights.put(PufferfishZombieHeadgear.Variant.IRON, builder.defineInRange("iron", 5, 0, 10000));
        headgearWeights.put(PufferfishZombieHeadgear.Variant.GOLDEN, builder.defineInRange("golden", 4, 0, 10000));
        headgearWeights.put(PufferfishZombieHeadgear.Variant.DIAMOND, builder.defineInRange("diamond", 1, 0, 10000));
        ZOMBIE_HEADGEAR_WEIGHTS = java.util.Collections.unmodifiableMap(headgearWeights);
        builder.pop(2);
        SPEC = builder.build();
    }

    private PufferfishMobConfig() {}

    private static void add(ModConfigSpec.Builder builder, Kind kind, double health, double damage, double armor,
            double speed, double resistance, double inflatedResistance, int cooldown, double capture) {
        builder.push(kind.name().toLowerCase(java.util.Locale.ROOT));
        SETTINGS.put(kind, new Settings(
                builder.worldRestart().defineInRange("health", health, 1, 1024),
                builder.worldRestart().defineInRange("attackDamage", damage, 0, 1024),
                builder.worldRestart().defineInRange("armor", armor, 0, 30),
                builder.worldRestart().defineInRange("movementSpeed", speed, 0.05, 2),
                builder.comment("Knockback resistance while deflated.").worldRestart().defineInRange("knockbackResistance", resistance, 0, 1),
                builder.comment("Total knockback resistance while partially or fully inflated.").worldRestart()
                        .defineInRange("inflatedKnockbackResistance", inflatedResistance, 0, 1),
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
            ModConfigSpec.DoubleValue speed, ModConfigSpec.DoubleValue resistance, ModConfigSpec.DoubleValue inflatedResistance, ModConfigSpec.IntValue cooldown,
            ModConfigSpec.DoubleValue capture) {}
}
