// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FastColor;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

public final class PufferfishEffectSpriteSource implements SpriteSource {
    public static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(PufferfishEffectSpriteSource::new));

    @Override
    public void run(ResourceManager manager, Output output) {
        var id = PufferfishPlease.id("pufferfish_curse");
        var replacement = manager.getResource(PufferfishPlease.id("textures/mob_effect/pufferfish_curse.png"));
        if (replacement.isPresent()) {
            output.add(id, replacement.get());
            return;
        }
        manager.getResource(ResourceLocation.withDefaultNamespace("textures/item/pufferfish.png")).ifPresent(resource ->
                output.add(id, loader -> loader.loadSprite(id, resource, (name, size, fish, metadata) -> {
                    try (fish) {
                        recolor(fish);
                        PufferfishTextures.whiteEyes(fish, false, 0xFF477E);
                        var icon = new NativeImage(18, 18, true);
                        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                            int pixel = fish.getPixelRGBA(x * size.width() / 16, y * size.height() / 16);
                            if (pixel >>> 24 != 0) icon.setPixelRGBA(x + 1, y + 1, pixel);
                        }
                        return new SpriteContents(id, new FrameSize(18, 18), icon, ResourceMetadata.EMPTY);
                    }
                })));
    }

    private static void recolor(NativeImage fish) {
        int[] body = {0x32253F, 0x58416F, 0x8E70AD, 0xB89CD0};
        int[] belly = {0x655571, 0x9B87AB, 0xCDBBD7, 0xF0E5F2};
        int[] fins = {0x303149, 0x525776, 0x7D88A5, 0xACB9D2};
        for (int y = 0; y < fish.getHeight(); y++) for (int x = 0; x < fish.getWidth(); x++) {
            int pixel = fish.getPixelRGBA(x, y);
            if (pixel >>> 24 == 0) continue;
            int red = pixel & 255;
            int green = pixel >>> 8 & 255;
            int blue = pixel >>> 16 & 255;
            int brightness = Math.max(red, Math.max(green, blue));
            int u = x * 16 / fish.getWidth();
            int v = y % fish.getWidth() * 16 / fish.getWidth();
            int[] palette = blue > red ? fins : v >= 9 && u >= 4 && u <= 11 ? belly : body;
            int shade = brightness < 100 ? 0 : brightness < 170 ? 1 : brightness < 225 ? 2 : 3;
            int rgb = brightness < 65 ? 0x251B30 : palette[shade];
            fish.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(pixel & 0xFF000000 | rgb));
        }
    }

    @Override
    public SpriteSourceType type() { return TYPE; }
}
