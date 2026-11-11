// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

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
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.PufferfishBoats;

public final class PufferfishBoatTextures implements ResourceManagerReloadListener {
    private final Set<ResourceLocation> generated = new HashSet<>();

    private static int tint(String name) {
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
        if (name.startsWith("golden_")) return GoldenSkinMaterial.shade(pixel | 0xFF000000, 255, x, y, belly);
        int color = belly ? -1 : tint(name);
        if (color == -1) return pixel | 0xFF000000;
        int brightness = Math.max(pixel & 255, Math.max(pixel >>> 8 & 255, pixel >>> 16 & 255));
        return rgba(color, brightness);
    }

    private static int rgba(int rgb, int brightness) {
        return 0xFF000000 | (rgb & 255) * brightness / 255 << 16
                | (rgb >>> 8 & 255) * brightness / 255 << 8 | (rgb >>> 16 & 255) * brightness / 255;
    }

    private static NativeImage hull(ResourceManager manager, String name) throws IOException {
        var image = new NativeImage(160, 64, false);
        try (var fish = fish(manager)) {
            PufferfishSkinTexture.recolorSource(fish, name);
            for (int y = 0; y < 64; y++) for (int x = 0; x < 160; x++) {
                int pixel;
                if (x >= 80) pixel = rgba(x % 5 == 0 ? 0x6E492F : 0xBC915F, 255);
                else if (x >= 48 && name.startsWith("golden_")) pixel = GoldenSkinMaterial.shade(0xFFE0E0E0, 255, x, y, true);
                else if (x >= 48) pixel = rgba((x + y) % 7 == 0 ? 0x6B543A : 0xA38C62, 255);
                else if (x >= 32) pixel = rgba((x + y) % 9 == 0 ? 0x46443D : 0x292E2B, 255);
                else if (x >= 16) pixel = skin(fish, name, x, y, true);
                else pixel = skin(fish, name, x, y, false);
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
            var id = PufferfishPlease.id("textures/entity/boat/" + name + ".png");
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
