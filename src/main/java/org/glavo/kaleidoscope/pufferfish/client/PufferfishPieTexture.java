// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies separate tiles for upward-facing fish, pastry, filling, and a wooden dish.
public final class PufferfishPieTexture {
    private static final String[] FACE = {
            "yyyyyyyy", "ywwyywwy", "ywbyybwy", "yyyyyyyy",
            "yyymmyyy", "yyymmyyy", "ycyyyycy", "yyyyyyyy"
    };

    private PufferfishPieTexture() {}

    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(32, 32, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int u = x % 8;
                int v = y % 8;
                int tile = y / 8 * 4 + x / 8;
                int rgb = switch (tile) {
                    case 0 -> switch (FACE[v].charAt(u)) {
                        case 'w' -> 0xFFFBEA;
                        case 'b' -> 0x25211C;
                        case 'm' -> 0x6B482F;
                        case 'c' -> 0xCC9A42;
                        default -> 0xF4DA7B;
                    };
                    case 1 -> (u * 3 + v * 5) % 11 == 0 ? 0xAC8238 : 0xE7C15D;
                    case 2 -> (u + v * 3) % 7 == 0 ? 0xD99C4C : (v % 3 == 0 ? 0xA86029 : 0xC9873C);
                    case 3 -> (u * 3 + v) % 9 == 0 ? 0xDFB186 : 0xF4DBAF;
                    case 4 -> v % 3 == 0 ? 0x684222 : 0x8D6031;
                    case 5 -> 0xD6A34E;
                    default -> 0xF4DBAF;
                };
                image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
            }
        }
        return new SpriteContents(id, new FrameSize(32, 32), image, ResourceMetadata.EMPTY);
    }
}
