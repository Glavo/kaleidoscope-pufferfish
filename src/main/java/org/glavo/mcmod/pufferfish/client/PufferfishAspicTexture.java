// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

public final class PufferfishAspicTexture {
    private PufferfishAspicTexture() {}

    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(16, 16, true);
        int[] palette = {0xE5E8DE, 0x648BA7, 0xEFCE80, 0xF8DB95, 0xF4E7BA, 0xA17846, 0xD2AD70, 0x6F8C4B};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int tile = y / 4 * 4 + x / 4;
                int color = palette[tile % palette.length];
                if (tile == 5 && (x + 2 * y) % 4 == 0) color = 0x54402E;
                if (tile == 3 && (x % 4 == 0 || y % 4 == 0)) color = 0xFFF1C7;
                int alpha = tile == 3 ? 190 : 255;
                image.setPixelRGBA(x, y, alpha << 24 | (color & 255) << 16 | color & 0xFF00 | color >>> 16);
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
