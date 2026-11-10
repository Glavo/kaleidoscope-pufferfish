// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

/// Two perpendicular gold halos centered on the inflated fish model.
public final class RadiantPufferfishVisuals {
    private static final ModelPart HALO = createHalo();
    private static final ResourceLocation GOLD = ResourceLocation.withDefaultNamespace("textures/block/gold_block.png");

    private RadiantPufferfishVisuals() {}

    private static ModelPart createHalo() {
        var mesh = new MeshDefinition();
        for (int segment = 0; segment < 32; segment++) {
            float angle = (float) (segment * Math.PI / 16);
            mesh.getRoot().addOrReplaceChild("segment_" + segment,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -1.0F, -0.5F, 1, 2, 1),
                    PartPose.offsetAndRotation(10 * (float) Math.cos(angle), 10 * (float) Math.sin(angle), 0, 0, 0, angle));
        }
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    public static void render(ItemStack fish, PoseStack pose, MultiBufferSource buffers, int overlay) {
        if (!PufferfishItems.isRadiant(fish)) return;
        pose.pushPose();
        pose.translate(0, 18.0 / 16, 0);
        pose.mulPose(Axis.YP.rotationDegrees(45));
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        HALO.render(pose, buffer, 15728880, overlay);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        HALO.render(pose, buffer, 15728880, overlay);
        pose.popPose();
    }

    public static void drawIconHalos(NativeImage image) {
        int width = image.getWidth();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < width; x++) {
                double px = (x + 0.5) * 16 / width - 8;
                double py = (y % width + 0.5) * 16 / width - 8;
                for (int tilt : new int[] {-1, 1}) {
                    double u = (px + tilt * py) / Math.sqrt(2);
                    double v = (py - tilt * px) / Math.sqrt(2);
                    double radius = Math.sqrt(u * u / 72.25 + v * v / 12.25);
                    if (Math.abs(radius - 1) > 0.11) continue;
                    if (v < 0 && (image.getPixelRGBA(x, y) >>> 24) != 0) continue;
                    int rgb = py < -2 ? 0xFFF2A3 : px < 0 ? 0xEFBF3F : 0xD59219;
                    image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
                }
            }
        }
    }
}
