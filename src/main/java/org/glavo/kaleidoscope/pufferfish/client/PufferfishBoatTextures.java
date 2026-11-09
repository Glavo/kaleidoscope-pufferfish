// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.item.DyeColor;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishBoats;

public final class PufferfishBoatTextures implements ResourceManagerReloadListener {
    private final Set<ResourceLocation> generated = new HashSet<>();

    private static int tint(String name) {
        if (name.startsWith("golden_")) return 0xFFC52B;
        for (var color : DyeColor.values()) if (name.startsWith(color.getName() + "_")) return color.getTextureDiffuseColor();
        return -1;
    }

    private static NativeImage fish(ResourceManager manager) throws IOException {
        try (var stream = manager.open(ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"))) {
            return NativeImage.read(stream);
        }
    }

    private static int skin(NativeImage fish, String name, int x, int y, boolean belly) {
        int pixel = fish.getPixelRGBA((belly ? x % 8 : 8 + x % 8) * fish.getWidth() / 32,
                (belly ? 12 + y % 4 : y % 8) * fish.getWidth() / 32);
        int color = belly ? (name.startsWith("golden_") ? 0xFFF2A6 : -1) : tint(name);
        if (color == -1) return pixel | 0xFF000000;
        int brightness = Math.max(pixel & 255, Math.max(pixel >>> 8 & 255, pixel >>> 16 & 255));
        return rgba(color, brightness);
    }

    private static int rgba(int rgb, int brightness) {
        return 0xFF000000 | (rgb & 255) * brightness / 255 << 16
                | (rgb >>> 8 & 255) * brightness / 255 << 8 | (rgb >>> 16 & 255) * brightness / 255;
    }

    /// A pointed hull and detached paddle remain distinct at inventory scale.
    public static SpriteContents item(ResourceManager manager, ResourceLocation id, String name) {
        var image = new NativeImage(32, 32, true);
        try (var fish = fish(manager)) {
            for (int x = 1; x < 31; x++) {
                int height = Math.max(1, Math.min(7, (int) ((15.5 - Math.abs(x - 15.5)) * 0.8)));
                for (int y = 20 - height; y <= 20 + height; y++) {
                    boolean outline = y == 20 - height || y == 20 + height || x == 1 || x == 30;
                    boolean rim = y == 21 - height || y == 19 + height;
                    int pixel = outline ? rgba(0x3B3028, 255) : skin(fish, name, x, y, rim);
                    if (x >= 8 && x <= 23 && y >= 18 && y <= 22) {
                        pixel = y == 18 || y == 22 || x == 8 || x == 23 ? rgba(0xB8A482, 255) : rgba(0x514337, 255);
                    }
                    image.setPixelRGBA(x, y, pixel);
                }
            }
            for (int x = 4; x <= 27; x++) {
                int y = 3 + (x - 4) / 4;
                for (int dy = -1; dy <= 1; dy++) image.setPixelRGBA(x, y + dy, rgba(dy == 0 ? 0xCFAB70 : 0x493522, 255));
            }
            for (int x = 23; x <= 29; x++) for (int y = 6; y <= 9; y++) {
                image.setPixelRGBA(x, y, rgba(x == 23 || x == 29 || y == 6 || y == 9 ? 0x493522 : 0xC99D58, 255));
            }
            return new SpriteContents(id, new FrameSize(32, 32), image, ResourceMetadata.EMPTY);
        } catch (IOException | RuntimeException exception) {
            image.close();
            throw new IllegalStateException("Could not generate skin boat icon", exception);
        }
    }

    private static NativeImage hull(ResourceManager manager, String name) throws IOException {
        var image = new NativeImage(128, 64, false);
        try (var fish = fish(manager)) {
            for (int y = 0; y < 64; y++) for (int x = 0; x < 128; x++) {
                int pixel;
                if (x >= 80) pixel = rgba(x % 4 == 0 ? 0x795634 : 0xBD955B, 255);
                else if (y >= 40 && y < 48) pixel = y == 42 && x % 4 < 2
                        ? rgba(0x6B543A, 255) : skin(fish, name, x, y, true);
                else pixel = skin(fish, name, x, y, y < 18);
                image.setPixelRGBA(x, y, pixel);
            }
            return image;
        } catch (IOException | RuntimeException exception) {
            image.close();
            throw exception;
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        var textures = Minecraft.getInstance().getTextureManager();
        generated.forEach(textures::release);
        generated.clear();
        for (var name : PufferfishBoats.VARIANTS.keySet()) {
            var id = KaleidoscopePufferfish.id("textures/entity/boat/" + name + ".png");
            if (manager.getResource(id).isPresent()) continue;
            try {
                textures.register(id, new DynamicTexture(hull(manager, name)));
                generated.add(id);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load skin boat texture", exception);
            }
        }
    }
}
