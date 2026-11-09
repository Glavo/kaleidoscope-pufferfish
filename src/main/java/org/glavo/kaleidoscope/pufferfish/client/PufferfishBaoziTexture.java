// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies pleated buns with small fish fins, viewed from above and to the side.
public final class PufferfishBaoziTexture {
    private static final String[] PIXELS = {
            "................",
            "................",
            "......ss........",
            "....shwwhs......",
            "...shhshhwhs....",
            "..shwwshshwws...",
            "..hwwwhhhwwwhs..",
            ".shwwwwwwwwwhs..",
            "sshwwhwwwshwhss.",
            "shshwwwwwwhhsfs.",
            "sfshtwwwwtthss..",
            ".sshttttttths...",
            "..ssttttttss....",
            "....ssssss......",
            "................",
            "................"
    };

    private PufferfishBaoziTexture() {}

    public static SpriteContents create(ResourceLocation id, boolean raw) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int rgb = switch (PIXELS[y].charAt(x)) {
                    case 'w' -> raw ? 0xE1DACA : 0xFFF8E5;
                    case 'h' -> raw ? 0xC8BDA9 : 0xECDDAD;
                    case 't' -> raw ? 0xB2A28A : 0xD7BC84;
                    case 's' -> raw ? 0x82715D : 0x98754D;
                    case 'f' -> raw ? 0xB59D70 : 0xD8B265;
                    default -> -1;
                };
                if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
