// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// A stone template with a recessed, spiny pufferfish engraving.
public final class PufferfishUpgradeTemplateTexture {
    private static final String[] PLATE = {
            "................",
            "....aaaaaaaaa...",
            "...abbbbbbbbba..",
            "...abbbdbddddc..",
            "...cdddddddddc..",
            "...cdddddddddc..",
            "...cdddddddddc..",
            "...cdddddddddc..",
            "...cdddddddddc..",
            "...cdddddddddc..",
            "...cdddddddddc..",
            "...cadddddddec..",
            "...ceaddaaaaec..",
            "....ceeaaeccc...",
            ".....kkkkk......",
            "................"
    };
    private static final String[] ENGRAVING = {
            "................",
            "........A.......",
            "......ABCBA..D..",
            ".....BCEEABBFDD.",
            "...ABAEEACCCDDG.",
            "...BCCAACHICGD..",
            "..CBIHCFFFFAFJKF",
            "DDBCFCFLLCFKKMB.",
            "GDDKFFLIIMKMNNO.",
            ".GJKCKCMMLLNLMO.",
            "...OLNNLNLNMNOM.",
            "..KOMNNNLNLNMO..",
            "....OLNMNLNMOM..",
            "....POMLLMOO....",
            "......OOOOM.....",
            "................"
    };

    private PufferfishUpgradeTemplateTexture() {}

    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(32, 32, true);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            char pixel = PLATE[y / 2].charAt(x / 2);
            int rgb = switch (pixel) {
                case 'a' -> 0x647184;
                case 'b' -> 0x8CA3AC;
                case 'c' -> 0x485665;
                case 'd' -> 0x72858C;
                case 'e' -> 0x5E707C;
                case 'k' -> 0x2F3E51;
                default -> -1;
            };
            if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < ENGRAVING.length; y++) for (int x = 0; x < ENGRAVING[y].length(); x++) {
                char pixel = ENGRAVING[y].charAt(x);
                if (pixel == '.') continue;
                int pixelX = x + 9;
                int pixelY = y + 7;
                if (pass == 0) image.setPixelRGBA(pixelX + 1, pixelY + 1, 0xFFADB3A5);
                else {
                    int rgb = switch (pixel) {
                        case 'B', 'O' -> 0x354954;
                        case 'A', 'C', 'F', 'K' -> 0x4B626D;
                        case 'D', 'G', 'J' -> 0x586E78;
                        case 'E', 'M' -> 0x607782;
                        case 'H' -> 0x99ACAC;
                        case 'I' -> 0x243640;
                        case 'L', 'P' -> 0x6F858D;
                        case 'N' -> 0x7D9297;
                        default -> throw new IllegalStateException("Unknown engraving pixel: " + pixel);
                    };
                    if ((y == 0 || ENGRAVING[y - 1].charAt(x) == '.'
                            || x == 0 || ENGRAVING[y].charAt(x - 1) == '.') && pixel != 'I') rgb = 0x354954;
                    image.setPixelRGBA(pixelX, pixelY, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
                }
            }
        }
        return new SpriteContents(id, new FrameSize(32, 32), image, ResourceMetadata.EMPTY);
    }
}
