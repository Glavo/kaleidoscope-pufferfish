// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.DyeColor;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishBoats;

public final class PufferfishBoatTextures implements ResourceManagerReloadListener {
    private final Set<ResourceLocation> generated = new HashSet<>();

    /// Covers the hull in fish skin while preserving the wooden paddles.
    public static void skin(ResourceManager manager, NativeImage image, String name, boolean entity) {
        String prefix = name.substring(0, name.indexOf("pufferfish_skin"));
        int tint = prefix.equals("golden_") ? 0xFFC52B : -1;
        for (var color : DyeColor.values()) if (prefix.equals(color.getName() + "_")) tint = color.getTextureDiffuseColor();
        try (var stream = manager.open(ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png"));
             var fish = NativeImage.read(stream)) {
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int original = image.getPixelRGBA(x, y);
                int u = x * (entity ? 128 : 16) / image.getWidth();
                int v = y * (entity ? 64 : 16) / image.getHeight();
                if ((original >>> 24) == 0 || (entity ? u >= 62 : v < 7)) continue;
                boolean edge = entity ? v % 8 == 0 || v % 8 == 1 : v >= 12;
                int sx = edge ? u % 8 : 8 + u % 8;
                int sy = edge ? 12 + v % 4 : v % 8;
                int pixel = fish.getPixelRGBA(sx * fish.getWidth() / 32, sy * fish.getWidth() / 32);
                int r = pixel & 255;
                int g = pixel >>> 8 & 255;
                int b = pixel >>> 16 & 255;
                int color = edge ? (prefix.equals("golden_") ? 0xFFF2A6 : -1) : tint;
                if (color != -1) {
                    int brightness = Math.max(r, Math.max(g, b));
                    r = (color >>> 16 & 255) * brightness / 255;
                    g = (color >>> 8 & 255) * brightness / 255;
                    b = (color & 255) * brightness / 255;
                }
                int shade = 160 + Math.max(original & 255, Math.max(original >>> 8 & 255, original >>> 16 & 255)) * 95 / 255;
                image.setPixelRGBA(x, y, original & 0xFF000000 | b * shade / 255 << 16 | g * shade / 255 << 8 | r * shade / 255);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate pufferfish boat texture", exception);
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
            try (var stream = manager.open(ResourceLocation.withDefaultNamespace("textures/entity/boat/oak.png"))) {
                var image = NativeImage.read(stream);
                try {
                    skin(manager, image, name, true);
                } catch (RuntimeException exception) {
                    image.close();
                    throw exception;
                }
                textures.register(id, new DynamicTexture(image));
                generated.add(id);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load boat texture", exception);
            }
        }
    }
}
