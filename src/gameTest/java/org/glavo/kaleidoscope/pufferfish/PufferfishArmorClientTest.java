// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import org.joml.Matrix4f;
import com.mojang.math.Axis;

public final class PufferfishArmorClientTest {
    public static void verify(Minecraft minecraft) {
        for (var entry : PufferfishArmor.VARIANTS.entrySet()) {
            var stack = new ItemStack(entry.getValue().get());
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()
                    || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/" + entry.getKey()))
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing pufferfish armor model or translation");
            }
            var item = entry.getValue().get();
            var texture = item.getMaterial().value().layers().getFirst().texture(item.getType() == ArmorItem.Type.LEGGINGS);
            if (!(minecraft.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic)
                    || dynamic.getPixels() == null || dynamic.getPixels().getWidth() != 64) {
                throw new IllegalStateException("Missing generated pufferfish armor texture");
            }
        }
        preview(minecraft);
    }

    private static void preview(Minecraft minecraft) {
        var target = new TextureTarget(900, 1080, true, Minecraft.ON_OSX);
        target.setClearColor(0.16F, 0.18F, 0.21F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 900, 1080, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        var outer = new HumanoidModel<LivingEntity>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        var inner = new HumanoidModel<LivingEntity>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        outer.young = inner.young = false;
        outer.leftArm.zRot = -0.15F;
        outer.rightArm.zRot = 0.15F;
        try {
            int index = 0;
            for (var entry : PufferfishArmor.VARIANTS.entrySet()) {
                if (entry.getValue().get().getType() != ArmorItem.Type.HELMET) continue;
                var buffers = minecraft.renderBuffers().bufferSource();
                String skin = entry.getKey().replace("_helmet", "");
                int column = 0;
                for (var type : PufferfishArmor.TYPES) {
                    var item = PufferfishArmor.VARIANTS.get(skin + "_" + type.getName()).get();
                    var pose = new PoseStack();
                    pose.translate(24 + column++ * 34 + index % 6 * 150, 24 + index / 6 * 270, 0);
                    pose.scale(26, -26, 26);
                    minecraft.getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.GUI, 15728880,
                            OverlayTexture.NO_OVERLAY, pose, buffers, null, 0);
                    pose = new PoseStack();
                    pose.translate(75 + index % 6 * 150, 99 + index / 6 * 270, 0);
                    pose.scale(82, 82, 82);
                    pose.mulPose(Axis.YP.rotationDegrees(155));
                    var armor = type == ArmorItem.Type.LEGGINGS ? inner : outer;
                    armor.setAllVisible(false);
                    switch (type) {
                        case HELMET -> armor.head.visible = armor.hat.visible = true;
                        case CHESTPLATE -> armor.body.visible = armor.leftArm.visible = armor.rightArm.visible = true;
                        case LEGGINGS -> armor.body.visible = armor.leftLeg.visible = armor.rightLeg.visible = true;
                        case BOOTS -> armor.leftLeg.visible = armor.rightLeg.visible = true;
                        default -> throw new AssertionError("Unexpected armor type");
                    }
                    var texture = item.getMaterial().value().layers().getFirst().texture(type == ArmorItem.Type.LEGGINGS);
                    armor.renderToBuffer(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), 15728880,
                            OverlayTexture.NO_OVERLAY, -1);
                }
                buffers.endBatch();
                index++;
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-armor-preview.png"));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save armor preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }
}
