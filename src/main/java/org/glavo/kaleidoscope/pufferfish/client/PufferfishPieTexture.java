// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.server.packs.resources.ResourceManager;

/// Supplies separate tiles for upward-facing fish, pastry, filling, and a wooden dish.
public final class PufferfishPieTexture {
    private PufferfishPieTexture() {}

    public static SpriteContents create(ResourceLocation id, ResourceManager manager) {
        var image = new NativeImage(32, 32, true);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int u = x % 8;
                int v = y % 8;
                int tile = y / 8 * 4 + x / 8;
                int rgb = switch (tile) {
                    case 2 -> (u + v * 3) % 7 == 0 ? 0xD99C4C : (v % 3 == 0 ? 0xA86029 : 0xC9873C);
                    case 3 -> (u * 3 + v) % 9 == 0 ? 0xDFB186 : 0xF4DBAF;
                    case 4 -> v % 3 == 0 ? 0x684222 : 0x8D6031;
                    case 5 -> v % 3 == 0 ? 0x3C8FBD : 0x83CEE2;
                    case 9 -> v < 3 ? 0xFFF2BD : 0xD5B55C;
                    default -> 0xF4DBAF;
                };
                image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
            }
        }
        try (var stream = manager.open(ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"));
             var fish = NativeImage.read(stream)) {
            copyFishTile(image, fish, 0, 8, 8);
            copyFishTile(image, fish, 1, 8, 0);
            copyFishTile(image, fish, 6, 16, 0);
            copyFishTile(image, fish, 7, 0, 8);
            copyFishTile(image, fish, 8, 16, 8);
        } catch (IOException exception) {
            image.close();
            throw new UncheckedIOException("Could not load the vanilla pufferfish texture for the pie", exception);
        }
        return new SpriteContents(id, new FrameSize(32, 32), image, ResourceMetadata.EMPTY);
    }

    private static void copyFishTile(NativeImage atlas, NativeImage fish, int tile, int sourceX, int sourceY) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                atlas.setPixelRGBA(tile % 4 * 8 + x, tile / 4 * 8 + y,
                        fish.getPixelRGBA((sourceX + x) * fish.getWidth() / 32, (sourceY + y) * fish.getHeight() / 32));
            }
        }
    }
}
