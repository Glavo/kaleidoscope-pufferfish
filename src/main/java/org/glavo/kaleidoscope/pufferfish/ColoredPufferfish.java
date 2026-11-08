// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import javax.annotation.Nullable;
import net.minecraft.world.item.DyeColor;

public interface ColoredPufferfish {
    @Nullable
    DyeColor kaleidoscope$getColor();

    void kaleidoscope$setColor(@Nullable DyeColor color);
}
