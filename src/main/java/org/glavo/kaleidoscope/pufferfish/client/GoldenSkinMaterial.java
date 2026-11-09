// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

/// Gold-ingot colors with narrow reflections and contrasting amber shadows.
public final class GoldenSkinMaterial {
    private GoldenSkinMaterial() {}

    public static int shade(int skin, int silhouette, int u, int v, boolean raised) {
        int brightness = Math.max(skin & 255, Math.max(skin >>> 8 & 255, skin >>> 16 & 255));
        int value = (brightness * 2 + silhouette) / 3;
        int color = value < 110 ? 0x752802 : value < 165 ? 0xB26411
                : value < 205 ? 0xDC9613 : value < 235 ? 0xFAD64A : 0xFDF55F;
        int reflection = Math.floorMod(u - v, 16);
        if (silhouette > 90 && value >= 165) {
            if (reflection == 1) color = 0xFFFDE0;
            else if (reflection == 2 || raised && reflection == 0) color = 0xFFF3A2;
            else if (reflection == 3) color = 0xFDF55F;
            else if (reflection == 5) color = 0xB26411;
            else if (reflection == 6) color = 0xDC9613;
        }
        return skin & 0xFF000000 | (color & 255) << 16 | color & 0xFF00 | color >>> 16 & 255;
    }
}
