// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/// Declaration order follows cultivation rank; persistent IDs do not depend on ordinals.
public enum PufferfishLevel implements StringRepresentable {
    LEVEL_0(0, 0),
    LEVEL_1(1, 160),
    LEVEL_2(2, 400),
    LEVEL_3(3, 1_200),
    LEVEL_4(4, 4_000),
    LEVEL_5(5, 16_000),
    LEVEL_6(6, 80_000),
    LEVEL_7(7, 500_000),
    LEVEL_8(8, 2_000_000),
    LEVEL_9(9, 10_000_000),
    LEVEL_10(10, 0);

    public static final Codec<PufferfishLevel> CODEC = StringRepresentable.fromEnum(PufferfishLevel::values);

    private final int level;
    private final long baseExperienceRequired;

    PufferfishLevel(int level, long baseExperienceRequired) {
        this.level = level;
        this.baseExperienceRequired = baseExperienceRequired;
    }

    /// Zero denotes a level without an experience-based breakthrough requirement.
    public long baseExperienceRequired() { return baseExperienceRequired; }

    @Override
    public String getSerializedName() { return "level." + level; }

    public Component displayName() { return Component.translatable("level.pufferfish_please." + level); }
}
