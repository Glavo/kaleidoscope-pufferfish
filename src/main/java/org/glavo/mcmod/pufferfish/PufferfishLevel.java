// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/// Declaration order follows cultivation rank; persistent IDs do not depend on ordinals.
public enum PufferfishLevel implements StringRepresentable {
    LEVEL_0(0),
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5),
    LEVEL_6(6),
    LEVEL_7(7),
    LEVEL_8(8),
    LEVEL_9(9),
    LEVEL_10(10);

    public static final Codec<PufferfishLevel> CODEC = StringRepresentable.fromEnum(PufferfishLevel::values);

    private final int level;

    PufferfishLevel(int level) { this.level = level; }

    @Override
    public String getSerializedName() { return "level." + level; }

    public Component displayName() { return Component.translatable("level.pufferfish_please." + level); }
}
