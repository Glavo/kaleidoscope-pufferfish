// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

/// Supplies steel-and-wind skin markings and client-side gust particles for heavy pufferfish.
public final class HeavyPufferfishVisuals {
    /// Repeating eight-pixel spiral used on warm body pixels while preserving the fish's facial features.
    private static final String[] WIND = {
            "........", "..www...", ".w...w..", ".w.ww.w.",
            ".w.w..w.", "..w.www.", "...w....", "........"
    };

    /// Prevents construction of this visual utility.
    private HeavyPufferfishVisuals() {}

    /// Recolors warm body pixels in place with mace-like steel shading and pale breeze spirals.
    /// The logical frame size is 32 pixels for entity UVs and 16 for item sprites; alpha is retained.
    public static void recolor(NativeImage image, boolean entity) {
        int scale = Math.max(1, image.getWidth() / (entity ? 32 : 16));
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int red = pixel & 255;
                int green = pixel >>> 8 & 255;
                int blue = pixel >>> 16 & 255;
                if ((pixel >>> 24) == 0 || red <= blue + 12 || green <= blue + 8) continue;
                int brightness = Math.max(red, green);
                boolean wind = WIND[(y / scale) % 8].charAt((x / scale) % 8) == 'w';
                int color = wind ? 0xB8D9DF : brightness < 160 ? 0x505066 : 0x72798F;
                int r = (color >>> 16 & 255) * brightness / 255;
                int g = (color >>> 8 & 255) * brightness / 255;
                int b = (color & 255) * brightness / 255;
                image.setPixelRGBA(x, y, pixel & 0xFF000000 | b << 16 | g << 8 | r);
            }
        }
        PufferfishTextures.whiteEyes(image, entity, PufferfishTextures.HEAVY_EYE_COLOR);
    }

    /// Emits occasional local gusts around equipped fish and along thrown heavy fish trajectories.
    public static void tick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!entity.level().isClientSide() || entity.tickCount % 8 != 0) return;
        if (entity instanceof Player player && !player.isSpectator() && !player.isInvisible()) {
            boolean head = PufferfishItems.isHeavy(player.getItemBySlot(EquipmentSlot.HEAD));
            if (head || PufferfishItems.isHeavy(player.getMainHandItem()) || PufferfishItems.isHeavy(player.getOffhandItem())) {
                double angle = player.tickCount * 0.2 + player.getId();
                player.level().addParticle(ParticleTypes.SMALL_GUST,
                        player.getX() + Math.cos(angle) * 0.45,
                        player.getY() + (head ? player.getEyeHeight() + 0.15 : player.getBbHeight() * 0.6),
                        player.getZ() + Math.sin(angle) * 0.45, 0, 0, 0);
            }
        } else if (entity instanceof ThrownPufferfish fish && PufferfishItems.isHeavy(fish.getItem())) {
            fish.level().addParticle(ParticleTypes.SMALL_GUST, fish.getX(), fish.getY(), fish.getZ(), 0, 0, 0);
        }
    }
}
