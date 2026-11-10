// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;

/// Shades crafted fish with copper, iron, or diamond highlights while retaining their eyes and mouth.
public final class MaterialPufferfishTexture {
    private static final int[] COPPER = {0x653322, 0x985035, 0xC5724B, 0xE5A076, 0xFFD3AC, 0xFFF0D5};
    private static final int[] IRON = {0x45474C, 0x73787F, 0xA3A9AD, 0xCDD2D3, 0xEDF1EE, 0xFFFFFF};
    private static final int[] DIAMOND = {0x155B63, 0x258C91, 0x39B8B3, 0x6EE1D6, 0xBAFFF1, 0xE9FFFA};
    private static final int[] GOLDEN = {0x752802, 0xB26411, 0xDC9613, 0xFAD64A, 0xFDF55F, 0xFFFDE0};

    private MaterialPufferfishTexture() {}

    public static void recolor(NativeImage image, String material, boolean entity) {
        int[] palette = switch (material) {
            case "copper" -> COPPER;
            case "exposed_copper" -> new int[] {0x4B4931, 0x77704C, 0xA08056, 0xB9996C, 0xD5BA8B, 0xE5D7AC};
            case "weathered_copper" -> new int[] {0x294E3D, 0x3C7257, 0x589779, 0x7DB393, 0xADD6B8, 0xD1E8CF};
            case "oxidized_copper" -> new int[] {0x174E43, 0x267566, 0x399B85, 0x58B69C, 0x8BD6BC, 0xBEEBDC};
            case "iron" -> IRON;
            case "diamond" -> DIAMOND;
            case "golden" -> GOLDEN;
            case "radiant" -> new int[] {0x656080, 0x9A92AE, 0xC8BDD3, 0xE7DAE7, 0xFFF1D5, 0xFFFFFF};
            default -> throw new IllegalArgumentException("Unknown pufferfish material: " + material);
        };
        int size = entity ? 32 : 16;
        for (int y = 0; y < image.getHeight(); y++) {
            int v = y % image.getWidth() * size / image.getWidth();
            for (int x = 0; x < image.getWidth(); x++) {
                int u = x * size / image.getWidth();
                int pixel = image.getPixelRGBA(x, y);
                int brightness = Math.max(pixel & 255, Math.max(pixel >>> 8 & 255, pixel >>> 16 & 255));
                boolean eye = entity
                        ? v == 9 && (u == 9 || u == 10 || u == 13 || u == 14)
                            || v == 28 && (u == 17 || u == 21) || v == 29 && (u == 3 || u == 5)
                            || (v == 6 || v == 7) && (u == 25 || u == 29)
                        : v == 5 && (u == 9 || u == 10) || v == 6 && (u == 4 || u == 5);
                if ((pixel >>> 24) == 0 || brightness < 75 || eye) continue;
                int shade = brightness < 150 ? 0 : brightness < 190 ? 1 : brightness < 220 ? 2 : brightness < 245 ? 3 : 4;
                if (!entity && brightness >= 210 && v >= 2 && v <= 5 && (u + v == 9 || u + v == 10)) shade = 5;
                int rgb = palette[shade];
                image.setPixelRGBA(x, y, pixel & 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
            }
        }
    }
}
