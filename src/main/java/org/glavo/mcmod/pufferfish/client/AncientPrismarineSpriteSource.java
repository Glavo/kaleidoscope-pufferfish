// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FastColor;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

public final class AncientPrismarineSpriteSource implements SpriteSource {
    public static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(AncientPrismarineSpriteSource::new));
    private static final int[] STONE = {0x263F41, 0x345153, 0x416261, 0x537571};
    private static final int[] SEDIMENT = {0x67766B, 0x829080, 0xA2AB92};

    @Override
    public void run(ResourceManager manager, Output output) {
        for (int stage = -1; stage < 4; stage++) {
            int dusted = stage;
            String name = stage < 0 ? "ancient_prismarine" : "suspicious_ancient_prismarine_" + stage;
            var id = PufferfishPlease.id("block/" + name);
            var replacement = manager.getResource(PufferfishPlease.id("textures/block/" + name + ".png"));
            if (replacement.isPresent()) output.add(id, replacement.get());
            else output.add(id, loader -> new SpriteContents(id, new FrameSize(16, 16), texture(dusted), ResourceMetadata.EMPTY));
        }
    }

    private static NativeImage texture(int dusted) {
        var image = new NativeImage(16, 16, false);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int noise = Math.floorMod(x * 37 + y * 61 + x * y * 13, 101);
            int row = y / 8;
            int u = (x + row * 4) % 8;
            int v = y % 8;
            int color = STONE[1 + noise % 3];
            if (u == 0 || v == 0) color = 0x203739;
            else if (u == 7 || v == 7) color = STONE[0];
            else if (u == 1 || v == 1) color = STONE[3];
            if (x == 5 + y / 3 && y > 2 && y < 14 || y == 10 && x > 9 && x < 14) color = 0x294344;
            if (dusted >= 0) {
                int coverage = 22 - dusted * 5;
                if (noise < coverage) color = SEDIMENT[noise % SEDIMENT.length];
                if (dusted > 0 && y >= 6 && y <= 9 && x >= 7 - dusted && x <= 8 + dusted) {
                    color = (x == 7 - dusted || y == 6) ? 0x263735 : 0x142B2E;
                }
            }
            image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(0xFF000000 | color));
        }
        return image;
    }

    @Override
    public SpriteSourceType type() { return TYPE; }
}
