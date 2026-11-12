// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.glavo.mcmod.pufferfish.client.PufferfishFlailHeadRenderer;

@EventBusSubscriber(modid = PufferfishPlease.MODID, value = Dist.CLIENT)
public final class PufferfishClientSmokeTest {
    private static int phase;
    private static boolean projectileRendererChecked;

    @SubscribeEvent
    public static void checkProjectileRenderer(EntityRenderersEvent.AddLayers event) {
        if (Boolean.getBoolean("pufferfish_please.clientSmokeTest")) {
            if (!(event.getRenderer(PufferfishPlease.THROWN_PUFFERFISH.get()) instanceof ThrownItemRenderer<?>)) {
                throw new IllegalStateException("Missing pufferfish projectile renderer");
            }
            if (!(event.getRenderer(PufferfishPlease.FLAIL_HEAD.get()) instanceof PufferfishFlailHeadRenderer)) {
                throw new IllegalStateException("Missing flail head renderer");
            }
            projectileRendererChecked = true;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Boolean.getBoolean("pufferfish_please.clientSmokeTest") || phase == 3
                || minecraft.screen == null || minecraft.getOverlay() != null) {
            return;
        }
        PufferfishResourcesClientTest.verify(minecraft);
        PufferfishDyedTextureClientTest.verify(minecraft, phase == 1);
        RadiantPufferfishClientTest.verify(minecraft);
        PufferfishSpawnEggClientTest.verify(minecraft);
        PufferfishMobClientTest.verify(minecraft);
        PufferfishPowerClientTest.verify(minecraft);
        PufferfishCurseClientTest.verify(minecraft);
        PufferfishTooltipClientTest.verify();
        PufferfishHeadgearClientTest.verify(minecraft);
        PufferfishCompatibilityClientTest.verify(minecraft);
        PufferfishGloveClientTest.verify(minecraft);
        PufferfishDeflationClientTest.verify(minecraft);
        PufferfishSizeClientTest.verify(minecraft);
        PufferfishPumpClientTest.verify(minecraft);
        PufferfishConduitClientTest.verify(minecraft);
        PufferfishPlacementClientTest.verify(minecraft);
        PufferfishLampClientTest.verify(minecraft);
        PufferfishFlailClientTest.verify(minecraft);
        PufferfishShieldClientTest.verify(minecraft);
        PufferfishArmorClientTest.verify(minecraft);
        PufferfishCatArmorClientTest.verify(minecraft);
        PufferfishBoatClientTest.verify(minecraft);
        PufferfishRowingClientTest.verify(minecraft);
        PufferfishFlailFirstPersonClientTest.verify(minecraft);
        PufferfishFlailOrbitClientTest.verify(minecraft);
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            org.glavo.mcmod.pufferfish.compat.create.PufferfishEngineClientTest.verify(minecraft);
        }
        if (!projectileRendererChecked) {
            throw new IllegalStateException("Projectile renderer registration was not checked");
        }
        phase++;
        if (phase < 3) {
            if (phase == 1) PufferfishDyedTextureClientTest.enablePack(minecraft);
            else PufferfishDyedTextureClientTest.disablePack(minecraft);
            projectileRendererChecked = false;
            minecraft.reloadResourcePacks();
        } else {
            LogUtils.getLogger().info("Pufferfish client smoke test passed: resource contracts, rendering behavior, optional integrations and resource reload");
            minecraft.stop();
        }
    }

}
