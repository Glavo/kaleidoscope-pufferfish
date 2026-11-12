// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/// Declaration order follows cultivation rank; persistent IDs do not depend on ordinals.
public enum PufferfishRealm implements StringRepresentable {
    LEVEL_0("level.0"),
    LEVEL_1("level.1"),
    LEVEL_2("level.2"),
    LEVEL_3("level.3"),
    LEVEL_4("level.4"),
    LEVEL_5("level.5"),
    LEVEL_6("level.6"),
    LEVEL_7("level.7"),
    LEVEL_8("level.8"),
    LEVEL_9("level.9"),
    LEVEL_10("level.10");

    public static final Codec<PufferfishRealm> CODEC = StringRepresentable.fromEnum(PufferfishRealm::values);

    private final String id;

    PufferfishRealm(String id) { this.id = id; }

    @Override
    public String getSerializedName() { return id; }

    public Component displayName() { return Component.translatable("realm.pufferfish_please." + id); }
}
