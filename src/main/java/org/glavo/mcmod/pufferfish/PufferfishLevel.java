// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/// Declaration order follows cultivation rank; persistent IDs do not depend on ordinals.
public enum PufferfishLevel implements StringRepresentable {
    LEVEL_0(0, 0, 1.00F, 1.0),
    LEVEL_1(1, 160, 1.05F, 1.2),
    LEVEL_2(2, 400, 1.10F, 1.5),
    LEVEL_3(3, 1_200, 1.15F, 2.0),
    LEVEL_4(4, 4_000, 1.20F, 2.7),
    LEVEL_5(5, 16_000, 1.30F, 3.6),
    LEVEL_6(6, 80_000, 1.40F, 4.8),
    LEVEL_7(7, 500_000, 1.50F, 6.2),
    LEVEL_8(8, 2_000_000, 1.65F, 7.5),
    LEVEL_9(9, 10_000_000, 1.80F, 8.7),
    LEVEL_10(10, 0, 2.00F, 10.0);

    public static final Codec<PufferfishLevel> CODEC = StringRepresentable.fromEnum(PufferfishLevel::values);

    private final int level;
    private final long baseExperienceRequired;
    private final float scale;
    private final double healthMultiplier;

    PufferfishLevel(int level, long baseExperienceRequired, float scale, double healthMultiplier) {
        this.level = level;
        this.baseExperienceRequired = baseExperienceRequired;
        this.scale = scale;
        this.healthMultiplier = healthMultiplier;
    }

    public int number() { return level; }

    /// Zero denotes a level without an experience-based breakthrough requirement.
    public long baseExperienceRequired() { return baseExperienceRequired; }

    public float scale() { return scale; }

    public double healthMultiplier() { return healthMultiplier; }

    /// Null means experience alone cannot advance this rank.
    @Nullable
    public PufferfishLevel automaticSuccessor() {
        return switch (this) {
            case LEVEL_0, LEVEL_4, LEVEL_10 -> null;
            case LEVEL_1 -> LEVEL_2;
            case LEVEL_2 -> LEVEL_3;
            case LEVEL_3 -> LEVEL_4;
            case LEVEL_5 -> LEVEL_6;
            case LEVEL_6 -> LEVEL_7;
            case LEVEL_7 -> LEVEL_8;
            case LEVEL_8 -> LEVEL_9;
            case LEVEL_9 -> LEVEL_10;
        };
    }

    @Override
    public String getSerializedName() { return "level." + level; }

    public Component displayName() { return Component.translatable("level.pufferfish_please." + level); }
}
