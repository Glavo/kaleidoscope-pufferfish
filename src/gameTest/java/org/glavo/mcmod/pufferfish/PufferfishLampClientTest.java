// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.RenderTestScene;

public final class PufferfishLampClientTest {
    public static void verify(Minecraft minecraft) {
        try (var scene = new RenderTestScene(minecraft, 1200, 180, 0.08F, 0.14F, 0.2F)) {
            var target = scene.target();
            Lighting.setupFor3DItems();

            var names = new String[] {"copper", "exposed_copper", "weathered_copper", "oxidized_copper", "heavy", "heavy", "heavy", "heavy"};
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int index = 0; index < names.length; index++) {
                var block = index >= 6 ? PufferfishBlocks.PLACED_HEAVY_FISH.get()
                        : PufferfishLamps.VARIANTS.get(names[index] + "_pufferfish_lamp").block().get();
                var state = block.defaultBlockState();
                var pose = new PoseStack();
                pose.translate(75 + index * 150, 120, 0);
                pose.scale(130, -130, 130);
                pose.mulPose(Axis.XP.rotationDegrees(20));
                pose.mulPose(Axis.YP.rotationDegrees(30));
                if (index == 5 || index == 7) {
                    var falling = new FallingBlockEntity(EntityType.FALLING_BLOCK, ClientTestLevel.level(minecraft));
                    var data = new CompoundTag();
                    data.put("BlockState", NbtUtils.writeBlockState(state));
                    falling.load(data);
                    minecraft.getEntityRenderDispatcher().getRenderer(falling).render(falling, 0, 0, pose, buffers, 0);
                } else {
                    pose.translate(-0.5, 0, -0.5);
                    var entity = (PlacedPufferfishBlockEntity) block.newBlockEntity(BlockPos.ZERO, state);
                    minecraft.getBlockEntityRenderDispatcher().getRenderer(entity).render(entity, 0, pose, buffers, 0, OverlayTexture.NO_OVERLAY);
                }
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                int background = image.getPixelRGBA(0, 0);
                for (int start : new int[] {600, 900}) {
                    int visible = 0;
                    for (int y = 0; y < 180; y++) {
                        for (int x = 0; x < 150; x++) {
                            int placed = image.getPixelRGBA(start + x, y);
                            int falling = image.getPixelRGBA(start + 150 + x, y);
                            if (falling != background) visible++;
                            if (placed != falling) throw new IllegalStateException("Falling fish differs from its placed model");
                        }
                    }
                    if (visible < 100) throw new IllegalStateException("Falling fish is invisible");
                }
            }
        }
    }
}
