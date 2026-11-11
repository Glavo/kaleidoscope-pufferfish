// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Arrays;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;

public final class PufferfishPowerClientTest {
    public static void verify(Minecraft minecraft) {
        var oldLevel = minecraft.level;
        var oldPlayer = minecraft.player;
        var light = minecraft.gameRenderer.lightTexture();
        try {
            var level = ClientTestLevel.level(minecraft);
            var vision = new float[] {1};
            var player = new LocalPlayer(minecraft, level, ClientTestLevel.connection(minecraft),
                    new StatsCounter(), new ClientRecipeBook(), false, false) {
                @Override
                public float getWaterVision() { return vision[0]; }
            };
            minecraft.level = level;
            minecraft.player = player;
            var baseline = pixels(light);
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260));
            var vanilla = pixels(light);
            player.removeEffect(MobEffects.CONDUIT_POWER);
            player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
            var power = pixels(light);
            if (Arrays.equals(baseline, power) || !Arrays.equals(vanilla, power)) {
                throw new IllegalStateException("Pufferfish power lighting differs from conduit power");
            }
            vision[0] = 0;
            if (!Arrays.equals(baseline, pixels(light))) {
                throw new IllegalStateException("Pufferfish power grants night vision outside water");
            }
            vision[0] = 1;
            player.removeEffect(PufferfishEffects.PUFFERFISH_POWER);
            if (!Arrays.equals(baseline, pixels(light))) {
                throw new IllegalStateException("Pufferfish power lighting survived effect removal");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not verify pufferfish power lighting", exception);
        } finally {
            minecraft.level = oldLevel;
            minecraft.player = oldPlayer;
            light.tick();
        }
    }

    private static int[] pixels(LightTexture light) throws ReflectiveOperationException {
        var update = LightTexture.class.getDeclaredField("updateLightTexture");
        update.setAccessible(true);
        update.setBoolean(light, true);
        light.updateLightTexture(0);
        var field = LightTexture.class.getDeclaredField("lightPixels");
        field.setAccessible(true);
        var image = (NativeImage) field.get(light);
        var pixels = new int[256];
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) pixels[y * 16 + x] = image.getPixelRGBA(x, y);
        return pixels;
    }
}
