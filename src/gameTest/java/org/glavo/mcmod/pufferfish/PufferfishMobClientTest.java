// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.glavo.mcmod.pufferfish.client.MaterialPufferfishRenderer;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.RenderCapture;

public final class PufferfishMobClientTest {
    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        java.util.function.Consumer<net.neoforged.neoforge.client.event.RenderNameTagEvent> hideNames = event ->
                event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(hideNames);
        try {
            for (var variant : PufferfishMobs.VARIANTS.values()) {
                var fish = variant.entity().get().create(level);
                var renderer = minecraft.getEntityRenderDispatcher().getRenderer(fish);
                if (!(renderer instanceof MaterialPufferfishRenderer material)) throw new IllegalStateException("Missing material fish entity renderer");
                var texture = material.getTextureLocation(fish);
                if (!texture.getNamespace().equals(PufferfishPlease.MODID)) {
                    throw new IllegalStateException("Material fish used the vanilla body texture");
                }
                for (int puff = 0; puff < 3; puff++) {
                    fish.setPuffState(puff);
                    if (PufferfishItems.isRadiant(fish.fish()) && puff == 2) {
                        fish.setAttackPhase(org.glavo.mcmod.pufferfish.entity.MaterialPufferfish.PULSE);
                    }
                    var pose = new PoseStack();
                    var capture = new RenderCapture();
                    renderer.render(fish, 0, 0, pose, capture, 15728880);
                    if (capture.count(RenderType.entityCutoutNoCull(texture)) == 0 || !pose.clear()) {
                        throw new IllegalStateException("Fish puff state lost its body or leaked its transform: " + variant + ", " + puff);
                    }
                }
            }
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(hideNames);
        }
    }
}
