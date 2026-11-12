// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FastColor;

/// A golden pearl with a pufferfish seal and traces of marine patina.
public final class AncientPufferfishPearlTexture {
    private static final String[] PIXELS = {
            "................", ".......oo.......", "......ohso......", "....oosbbsoo....",
            "...oshhhbbbso...", "..oshhwwhbbgso..", "..ohhwwhhbgbso..", ".osbhhebbebbbso.",
            ".osbhhbbobbbbso.", "..osbhbbobbbso..", "..osbbggbbbsso..", "...osbbbbssso...",
            "....oossssoo....", "......osso......", ".......oo.......", "................"
    };

    private AncientPufferfishPearlTexture() {}

    public static SpriteContents create(ResourceLocation id) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int rgb = switch (PIXELS[y].charAt(x)) {
                case 'o' -> 0x51432D;
                case 's' -> 0xA37D3C;
                case 'b' -> 0xE0B85A;
                case 'h' -> 0xF7DFA0;
                case 'w' -> 0xFFF9E4;
                case 'g' -> 0x698E78;
                case 'e' -> 0x354E49;
                default -> -1;
            };
            if (rgb >= 0) image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(0xFF000000 | rgb));
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
