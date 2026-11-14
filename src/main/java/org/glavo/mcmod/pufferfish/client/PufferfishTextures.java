// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

public final class PufferfishTextures implements ResourceManagerReloadListener {
    public static final int NETHERITE_COLOR = 0x594F52;
    public static final int NETHERITE_EYE_COLOR = 0xE5DED1;
    public static final int HEAVY_EYE_COLOR = 0xE5F0FA;
    private static final ResourceLocation ORIGINAL = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private static final Map<DyeColor, ResourceLocation> ENTITY_TEXTURES = new EnumMap<>(DyeColor.class);
    private static final Map<String, ResourceLocation> MATERIAL_TEXTURES = new java.util.HashMap<>();
    private static ResourceLocation netheriteTexture;
    /// The loaded heavy fish texture, released on resource reload.
    private static ResourceLocation heavyTexture;

    public static ResourceLocation item(DyeColor color, boolean bucket) {
        return PufferfishPlease.id("item/" + color.getName() + (bucket ? "_pufferfish_bucket" : "_pufferfish"));
    }

    public static ResourceLocation entity(DyeColor color) {
        return ENTITY_TEXTURES.computeIfAbsent(color, PufferfishTextures::loadEntityTexture);
    }

    /// Chooses the body texture consistently across wearable, placed, and projectile fish.
    public static ResourceLocation entity(ItemStack fish) {
        if (PufferfishItems.isHeavy(fish)) return heavyEntity();
        if (PufferfishItems.isNetherite(fish)) return netheriteEntity();
        var material = PufferfishItems.materialName(fish);
        if (material != null) return materialEntity(material);
        var color = PufferfishColors.get(fish);
        return color == null ? ORIGINAL : entity(color);
    }

    private static ResourceLocation loadEntityTexture(DyeColor color) {
        return loadEntityTexture(color.getName(), color.getTextureDiffuseColor());
    }

    public static ResourceLocation netheriteEntity() {
        if (netheriteTexture == null) {
            netheriteTexture = loadEntityTexture("netherite", NETHERITE_COLOR);
        }
        return netheriteTexture;
    }

    public static ResourceLocation materialEntity(String material) {
        return MATERIAL_TEXTURES.computeIfAbsent(material, name -> {
            var minecraft = Minecraft.getInstance();
            var id = PufferfishPlease.id("textures/entity/" + name + "_pufferfish.png");
            if (minecraft.getResourceManager().getResource(id).isPresent()) return id;
            try (var stream = minecraft.getResourceManager().open(ORIGINAL)) {
                var image = NativeImage.read(stream);
                MaterialPufferfishTexture.recolor(image, name, true);
                minecraft.getTextureManager().register(id, new DynamicTexture(image));
                return id;
            } catch (IOException exception) {
                LogUtils.getLogger().error("Failed to load material pufferfish texture: {}", name, exception);
                return ORIGINAL;
            }
        });
    }

    public static ResourceLocation radiantGlow() {
        return MATERIAL_TEXTURES.computeIfAbsent("radiant_glow", ignored -> {
            var minecraft = Minecraft.getInstance();
            var id = PufferfishPlease.id("textures/entity/radiant_glow.png");
            if (minecraft.getResourceManager().getResource(id).isEmpty()) {
                minecraft.getTextureManager().register(id, new DynamicTexture(RadiantPufferfishVisuals.createGlowTexture()));
            }
            return id;
        });
    }

    public static ResourceLocation ancientConduit() {
        return MATERIAL_TEXTURES.computeIfAbsent("ancient_conduit", ignored -> {
            var minecraft = Minecraft.getInstance();
            var id = PufferfishPlease.id("textures/entity/ancient_pufferfish_conduit.png");
            if (minecraft.getResourceManager().getResource(id).isPresent()) return id;
            try (var stream = minecraft.getResourceManager().open(ORIGINAL)) {
                var image = NativeImage.read(stream);
                int[] stone = {0x182B30, 0x2F484A, 0x4F6867, 0x7E9290};
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    if (pixel >>> 24 == 0) continue;
                    int brightness = Math.max(pixel & 255, Math.max(pixel >>> 8 & 255, pixel >>> 16 & 255));
                    int u = x * 32 / image.getWidth();
                    int v = y % image.getWidth() * 32 / image.getWidth();
                    int shade = brightness < 100 ? 0 : brightness < 170 ? 1 : brightness < 225 ? 2 : 3;
                    boolean crack = (u + v / 3) % 11 == 4 && v % 9 < 7;
                    int rgb = crack ? 0x111E24 : stone[shade];
                    image.setPixelRGBA(x, y, net.minecraft.util.FastColor.ABGR32.fromArgb32(pixel & 0xFF000000 | rgb));
                }
                whiteEyes(image, true, 0xBD9ACD);
                minecraft.getTextureManager().register(id, new DynamicTexture(image));
                return id;
            } catch (IOException exception) {
                LogUtils.getLogger().error("Failed to load ancient pufferfish conduit texture", exception);
                return ORIGINAL;
            }
        });
    }

    /// Returns the heavy fish entity texture, loading a resource-pack replacement or generated steel-and-wind texture.
    public static ResourceLocation heavyEntity() {
        if (heavyTexture == null) {
            var minecraft = Minecraft.getInstance();
            var id = PufferfishPlease.id("textures/entity/heavy_pufferfish.png");
            if (minecraft.getResourceManager().getResource(id).isPresent()) {
                heavyTexture = id;
            } else {
                try (var stream = minecraft.getResourceManager().open(ORIGINAL)) {
                    var image = NativeImage.read(stream);
                    HeavyPufferfishVisuals.recolor(image, true);
                    minecraft.getTextureManager().register(id, new DynamicTexture(image));
                    heavyTexture = id;
                } catch (IOException exception) {
                    LogUtils.getLogger().error("Failed to load heavy pufferfish texture", exception);
                    heavyTexture = ORIGINAL;
                }
            }
        }
        return heavyTexture;
    }

    private static ResourceLocation loadEntityTexture(String variant, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation location = PufferfishPlease.id("textures/entity/" + variant + "_pufferfish.png");
        if (minecraft.getResourceManager().getResource(location).isPresent()) return location;
        try (var stream = minecraft.getResourceManager().open(ORIGINAL)) {
            NativeImage image = NativeImage.read(stream);
            recolor(image, color);
            if (variant.equals("netherite")) whiteEyes(image, true, NETHERITE_EYE_COLOR);
            minecraft.getTextureManager().register(location, new DynamicTexture(image));
            return location;
        } catch (IOException exception) {
            LogUtils.getLogger().error("Failed to load pufferfish texture", exception);
            return ORIGINAL;
        }
    }

    public static void recolor(NativeImage image, DyeColor color) {
        recolor(image, color.getTextureDiffuseColor());
    }

    /// Replaces eye interiors at vanilla UV positions, preserving surrounding pixels and alpha at any resolution.
    /// Square animation frames are processed independently; item and entity layouts use different eye masks.
    public static void whiteEyes(NativeImage image, boolean entity, int color) {
        int logicalSize = entity ? 32 : 16;
        int rgba = (color & 255) << 16 | color & 0xFF00 | color >>> 16 & 255;
        for (int y = 0; y < image.getHeight(); y++) {
            int v = (y % image.getWidth()) * logicalSize / image.getWidth();
            for (int x = 0; x < image.getWidth(); x++) {
                int u = x * logicalSize / image.getWidth();
                boolean eye = entity
                        ? v == 9 && (u == 9 || u == 10 || u == 13 || u == 14)
                            || v == 28 && (u == 17 || u == 21)
                            || v == 29 && (u == 3 || u == 5)
                            || (v == 6 || v == 7) && (u == 25 || u == 29)
                        : v == 5 && (u == 9 || u == 10) || v == 6 && (u == 4 || u == 5);
                if (eye) {
                    int pixel = image.getPixelRGBA(x, y);
                    if ((pixel >>> 24) != 0) image.setPixelRGBA(x, y, pixel & 0xFF000000 | rgba);
                }
            }
        }
    }

    public static void recolor(NativeImage image, int target) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int red = pixel & 255;
                int green = pixel >>> 8 & 255;
                int blue = pixel >>> 16 & 255;
                // Recolor the warm body pixels while preserving eyes, belly, water, and metal.
                if ((pixel >>> 24) != 0 && red > blue + 12 && green > blue + 8) {
                    int brightness = Math.max(red, green);
                    int r = (target >>> 16 & 255) * brightness / 255;
                    int g = (target >>> 8 & 255) * brightness / 255;
                    int b = (target & 255) * brightness / 255;
                    image.setPixelRGBA(x, y, pixel & 0xFF000000 | b << 16 | g << 8 | r);
                }
            }
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        var textures = Minecraft.getInstance().getTextureManager();
        ENTITY_TEXTURES.values().stream().filter(id -> !id.equals(ORIGINAL)).forEach(textures::release);
        ENTITY_TEXTURES.clear();
        MATERIAL_TEXTURES.values().stream().filter(id -> !id.equals(ORIGINAL)).forEach(textures::release);
        MATERIAL_TEXTURES.clear();
        if (netheriteTexture != null && !netheriteTexture.equals(ORIGINAL)) {
            textures.release(netheriteTexture);
        }
        netheriteTexture = null;
        if (heavyTexture != null && !heavyTexture.equals(ORIGINAL)) textures.release(heavyTexture);
        heavyTexture = null;
    }
}
