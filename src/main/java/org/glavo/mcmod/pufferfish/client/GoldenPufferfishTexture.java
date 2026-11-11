// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;

/// Shades pufferfish item sprites with the vanilla gold ingot palette and directional highlights.
public final class GoldenPufferfishTexture {
    /// Prevents construction of this texture utility.
    private GoldenPufferfishTexture() {}

    /// Recolors the body, belly, and fins in place, retaining alpha and dark facial features.
    /// Eye interiors use the vanilla 16-pixel item layout, scaled to the image width;
    /// vertically stacked square animation frames are shaded independently.
    public static void recolor(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            int v = (y % image.getWidth()) * 16 / image.getWidth();
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int red = pixel & 255;
                int green = pixel >>> 8 & 255;
                int blue = pixel >>> 16 & 255;
                int brightness = Math.max(red, Math.max(green, blue));
                int u = x * 16 / image.getWidth();
                boolean eye = v == 5 && (u == 9 || u == 10) || v == 6 && (u == 4 || u == 5);
                if ((pixel >>> 24) == 0 || brightness < 75 || eye) continue;

                int color = brightness < 160 ? 0x752802
                        : brightness < 195 ? 0xB26411
                        : brightness < 225 ? 0xDC9613
                        : brightness < 245 ? 0xFAD64A : 0xFDF55F;
                if (blue > red && green > red) {
                    color = blue >= 189 ? 0xFDF55F : blue >= 180 ? 0xE9B115 : 0xB26411;
                }
                // Narrow, bright reflections contrast with the amber midtones and copper shadows.
                boolean upperReflection = v >= 2 && v <= 5 && (u + v == 9 || u + v == 10);
                boolean bellyReflection = (v == 9 || v == 10) && u >= 5 && u <= 7;
                if ((brightness >= 210 && upperReflection) || (brightness >= 245 && bellyReflection)) {
                    color = 0xFFFDE0;
                }
                int rgba = (color & 255) << 16 | color & 0xFF00 | color >>> 16 & 255;
                image.setPixelRGBA(x, y, pixel & 0xFF000000 | rgba);
            }
        }
    }
}
