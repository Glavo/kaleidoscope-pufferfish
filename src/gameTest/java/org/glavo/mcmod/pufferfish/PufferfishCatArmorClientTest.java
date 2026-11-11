// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.EntityType;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.RenderCapture;

public final class PufferfishCatArmorClientTest {
    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        var armorType = RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
        java.util.function.Consumer<net.neoforged.neoforge.client.event.RenderNameTagEvent> hideNames = event ->
                event.setCanRender(net.neoforged.neoforge.common.util.TriState.FALSE);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(hideNames);
        try {
            for (var variant : PufferfishCatArmor.VARIANTS.values()) {
                for (boolean baby : new boolean[] {false, true}) {
                    for (int posture = 0; posture < 3; posture++) {
                        var cat = EntityType.CAT.create(level);
                        cat.setTame(true, false);
                        cat.setAge(baby ? -24000 : 0);
                        cat.setBodyArmorItem(variant.toStack());
                        cat.setInSittingPose(posture == 1);
                        if (posture == 2) {
                            cat.setNoAi(true);
                            cat.setNoGravity(true);
                            cat.setSilent(true);
                            cat.setLying(true);
                            for (int tick = 0; tick < 15; tick++) cat.tick();
                            if (cat.getLieDownAmount(0) < 1) throw new IllegalStateException("Cat did not enter the lying pose");
                        }
                        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(cat);
                        var pose = new PoseStack();
                        var capture = new RenderCapture();
                        renderer.render(cat, 0, 0, pose, capture, 15728880);
                        if (capture.count(armorType) == 0 || !pose.clear()) {
                            throw new IllegalStateException("Cat armor did not render or leaked its transform: " + variant);
                        }
                        cat.setBodyArmorItem(net.minecraft.world.item.ItemStack.EMPTY);
                        capture = new RenderCapture();
                        renderer.render(cat, 0, 0, pose, capture, 15728880);
                        if (capture.count(armorType) != 0 || !pose.clear()) {
                            throw new IllegalStateException("Removed cat armor retained its geometry");
                        }
                    }
                }
            }
        } finally {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(hideNames);
        }
    }
}
