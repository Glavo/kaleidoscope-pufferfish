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
    private static final String[] SPINES = {
            "..t.....", ".sbs....", "........", "......t.",
            ".....sbs", "........", "........", "........"
    };
    private final Set<ResourceLocation> generated = new HashSet<>();

    public static void skin(ResourceManager manager, NativeImage mask, String name, boolean worn) {
        String prefix = name.substring(0, name.indexOf("pufferfish_skin"));
        if (prefix.startsWith("waxed_")) prefix = prefix.substring("waxed_".length());
        int ventral = switch (prefix) {
            case "heavy_" -> 0xCADFE4;
            case "netherite_" -> 0xBAB4B3;
            case "radiant_" -> 0xE7DAE7;
            case "copper_" -> 0xFFD3AC;
            case "exposed_copper_" -> 0xD5BA8B;
            case "weathered_copper_" -> 0xADD6B8;
            case "oxidized_copper_" -> 0x8BD6BC;
            case "iron_" -> 0xEDF1EE;
            case "diamond_" -> 0xBAFFF1;
            default -> 0xF4E4B0;
        };
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
                boolean belly = belly(u, v, name, worn);
                int sx = belly ? u % 8 : 8 + u % 8;
                int sy = belly ? 12 + v % 4 : v % 8;
                int pixel = fish.getPixelRGBA(sx * fish.getWidth() / 32, sy * fish.getWidth() / 32);
                if (prefix.equals("golden_")) {
                    int shade = Math.max(original & 255, Math.max(original >>> 8 & 255, original >>> 16 & 255));
                    int gold = GoldenSkinMaterial.shade(pixel, shade, u, v, belly);
                    mask.setPixelRGBA(x, y, original & 0xFF000000 | detail(gold, u, v, belly, true, ventral) & 0xFFFFFF);
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
                int shade = 160 + Math.max(original & 255, Math.max(original >>> 8 & 255, original >>> 16 & 255)) * 95 / 255;
                int shaded = blue * shade / 255 << 16 | green * shade / 255 << 8 | red * shade / 255;
                mask.setPixelRGBA(x, y, original & 0xFF000000 | detail(shaded, u, v, belly, false, ventral) & 0xFFFFFF);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate pufferfish armor texture", exception);
        }
    }

    private static boolean belly(int u, int v, String name, boolean worn) {
        if (!worn) {
            if (name.endsWith("_helmet")) return v >= 7 && u >= 4 && u < 12;
            if (name.endsWith("_chestplate")) return v >= 6 && v < 14 && u >= (v < 8 ? 6 : 5) && u < (v < 8 ? 10 : 11);
            if (name.endsWith("_leggings")) return v >= 7 && v < 12 && (u == 5 || u == 10);
            return v >= 11 && (u >= 3 && u < 7 || u >= 9 && u < 13);
        }
        if (v < 16) return u >= 8 && u < 16 && v >= 12;
        if (u >= 20 && u < 28 && v >= 22) {
            int inset = v == 22 || v >= 30 ? 2 : 1;
            return u >= 20 + inset && u < 28 - inset;
        }
        return u >= 5 && u < 7 && v >= 29 && v < 31;
    }

    /// Small shaded spine roots keep the hide recognizable at the native pixel scale.
    private static int detail(int pixel, int u, int v, boolean belly, boolean golden, int ventral) {
        int r = pixel & 255;
        int g = pixel >>> 8 & 255;
        int b = pixel >>> 16 & 255;
        if (belly) {
            if (!golden) {
                r = (r + 3 * (ventral >>> 16 & 255)) / 4;
                g = (g + 3 * (ventral >>> 8 & 255)) / 4;
                b = (b + 3 * (ventral & 255)) / 4;
            }
            if (v % 4 == 0) { r = r * 9 / 10; g = g * 9 / 10; b = b * 9 / 10; }
        } else {
            char mark = SPINES[v % 8].charAt(u % 8);
            if (mark == 'b') { r = r * 45 / 100; g = g * 45 / 100; b = b * 45 / 100; }
            else if (mark == 's') { r = r * 75 / 100; g = g * 75 / 100; b = b * 75 / 100; }
            else if (mark == 't') {
                r = (r + (ventral >>> 16 & 255)) / 2;
                g = (g + (ventral >>> 8 & 255)) / 2;
                b = (b + (ventral & 255)) / 2;
            }
        }
        return b << 16 | g << 8 | r;
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
