// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

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
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

public final class PufferfishTextures implements ResourceManagerReloadListener {
    public static final int NETHERITE_COLOR = 0x594F52;
    private static final ResourceLocation ORIGINAL = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private static final Map<DyeColor, ResourceLocation> ENTITY_TEXTURES = new EnumMap<>(DyeColor.class);
    private static ResourceLocation netheriteTexture;

    public static ResourceLocation item(DyeColor color, boolean bucket) {
        return KaleidoscopePufferfish.id("item/" + color.getName() + (bucket ? "_pufferfish_bucket" : "_pufferfish"));
    }

    public static ResourceLocation entity(DyeColor color) {
        return ENTITY_TEXTURES.computeIfAbsent(color, PufferfishTextures::loadEntityTexture);
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

    private static ResourceLocation loadEntityTexture(String variant, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        try (var stream = minecraft.getResourceManager().open(ORIGINAL)) {
            NativeImage image = NativeImage.read(stream);
            recolor(image, color);
            ResourceLocation location = KaleidoscopePufferfish.id("textures/entity/" + variant + "_pufferfish.png");
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
        if (netheriteTexture != null && !netheriteTexture.equals(ORIGINAL)) {
            textures.release(netheriteTexture);
        }
        netheriteTexture = null;
    }
}
