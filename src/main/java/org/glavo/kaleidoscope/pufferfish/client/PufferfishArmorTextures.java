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
import net.minecraft.world.item.ArmorItem;
import org.glavo.kaleidoscope.pufferfish.PufferfishArmor;

/// Applies pufferfish skin to vanilla armor silhouettes and UV layouts.
public final class PufferfishArmorTextures implements ResourceManagerReloadListener {
    private static final ResourceLocation FISH = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private final Set<ResourceLocation> generated = new HashSet<>();

    public static void skin(ResourceManager manager, NativeImage mask, String name, boolean worn) {
        String prefix = name.substring(0, name.indexOf("pufferfish_skin"));
        int tint = -1;
        for (var color : DyeColor.values()) if (prefix.equals(color.getName() + "_")) tint = color.getTextureDiffuseColor();
        String overlayPath = worn ? "textures/models/armor/leather_layer_" + (name.endsWith("_leggings") ? 2 : 1) + "_overlay.png"
                : "textures/item/leather_" + name.substring(name.lastIndexOf('_') + 1) + "_overlay.png";
        try (var overlayStream = manager.open(ResourceLocation.withDefaultNamespace(overlayPath));
                var overlay = NativeImage.read(overlayStream);
                var stream = manager.open(FISH); var fish = NativeImage.read(stream)) {
            PufferfishSkinTexture.recolorSource(fish, name);
            for (int y = 0; y < mask.getHeight(); y++) for (int x = 0; x < mask.getWidth(); x++) {
                mask.blendPixel(x, y, overlay.getPixelRGBA(x * overlay.getWidth() / mask.getWidth(), y * overlay.getHeight() / mask.getHeight()));
                int original = mask.getPixelRGBA(x, y);
                if ((original >>> 24) == 0) continue;
                int u = x * (worn ? 64 : 16) / mask.getWidth();
                int v = y * (worn ? 32 : 16) / mask.getHeight();
                boolean belly = worn ? u >= 22 && u < 26 && v >= 22 : u >= 6 && u < 10 && v >= 7;
                int sx = belly ? u % 8 : 8 + u % 8;
                int sy = belly ? 12 + v % 4 : v % 8;
                int pixel = fish.getPixelRGBA(sx * fish.getWidth() / 32, sy * fish.getWidth() / 32);
                if (prefix.equals("golden_")) {
                    int shade = Math.max(original & 255, Math.max(original >>> 8 & 255, original >>> 16 & 255));
                    int gold = GoldenSkinMaterial.shade(pixel, shade, u, v, belly);
                    mask.setPixelRGBA(x, y, original & 0xFF000000 | gold & 0xFFFFFF);
                    continue;
                }
                int red = pixel & 255;
                int green = pixel >>> 8 & 255;
                int blue = pixel >>> 16 & 255;
                int color = belly ? -1 : tint;
                if (color != -1) {
                    int brightness = Math.max(red, Math.max(green, blue));
                    red = (color >>> 16 & 255) * brightness / 255;
                    green = (color >>> 8 & 255) * brightness / 255;
                    blue = (color & 255) * brightness / 255;
                }
                int shade = Math.max(original & 255, Math.max(original >>> 8 & 255, original >>> 16 & 255));
                mask.setPixelRGBA(x, y, original & 0xFF000000 | blue * shade / 255 << 16 | green * shade / 255 << 8 | red * shade / 255);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate pufferfish armor texture", exception);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        var textures = Minecraft.getInstance().getTextureManager();
        generated.forEach(textures::release);
        generated.clear();
        for (var entry : PufferfishArmor.VARIANTS.entrySet()) {
            boolean inner = entry.getValue().get().getType() == ArmorItem.Type.LEGGINGS;
            var id = entry.getValue().get().getMaterial().value().layers().getFirst().texture(inner);
            if (generated.contains(id) || manager.getResource(id).isPresent()) continue;
            try (var stream = manager.open(ResourceLocation.withDefaultNamespace("textures/models/armor/leather_layer_" + (inner ? 2 : 1) + ".png"))) {
                var image = NativeImage.read(stream);
                try {
                    skin(manager, image, entry.getKey(), true);
                } catch (RuntimeException exception) {
                    image.close();
                    throw exception;
                }
                textures.register(id, new DynamicTexture(image));
                generated.add(id);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load armor silhouette", exception);
            }
        }
    }
}
