// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.entity.animal.Cat;
import org.glavo.mcmod.pufferfish.PufferfishCatArmorItem;
import org.glavo.mcmod.pufferfish.PufferfishMaterial;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.PufferfishVariant;

public final class PufferfishCatArmorLayer extends RenderLayer<Cat, CatModel<Cat>> {
    private final ArmorModel model = new ArmorModel(false);
    private final ArmorModel heavyModel = new ArmorModel(true);

    public PufferfishCatArmorLayer(RenderLayerParent<Cat, CatModel<Cat>> renderer) { super(renderer); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, Cat cat, float limbSwing,
            float limbSwingAmount, float partialTick, float age, float headYaw, float headPitch) {
        var stack = cat.getBodyArmorItem();
        if (cat.isInvisible() || !(stack.getItem() instanceof PufferfishCatArmorItem)) return;
        var variant = PufferfishVariant.of(stack);
        var armor = variant.material() == PufferfishMaterial.HEAVY ? heavyModel : model;
        getParentModel().copyPropertiesTo(armor);
        armor.prepareMobModel(cat, limbSwing, limbSwingAmount, partialTick);
        armor.setupAnim(cat, limbSwing, limbSwingAmount, age, headYaw, headPitch);
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(PufferfishPlease.id("block/" + variant.skinName()));
        var vertices = sprite.wrap(buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)));
        int overlay = LivingEntityRenderer.getOverlayCoords(cat, 0);
        armor.renderToBuffer(pose, vertices, light, overlay, -1);
    }

    private static final class ArmorModel extends CatModel<Cat> {
        private ArmorModel(boolean heavy) { super(mesh(heavy).bakeRoot()); }

        private static LayerDefinition mesh(boolean heavy) {
            var mesh = CatModel.createBodyMesh(CubeDeformation.NONE);
            var root = mesh.getRoot();
            for (var part : new String[] {"head", "tail1", "tail2", "left_hind_leg", "right_hind_leg", "left_front_leg", "right_front_leg"}) {
                root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
            }
            var thickness = new CubeDeformation(heavy ? 0.25F : 0);
            var shell = CubeListBuilder.create().texOffs(8, 0)
                    .addBox(-2.5F, 4, -2.3F, 5, 12, 1, thickness)
                    .addBox(-2.7F, 4, -7.5F, 1, 12, 5.5F, thickness)
                    .addBox(1.7F, 4, -7.5F, 1, 12, 5.5F, thickness)
                    .texOffs(0, 16)
                    .addBox(-2.5F, 5, -8.3F, 5, 2, 1, thickness)
                    .addBox(-2.5F, 13, -8.3F, 5, 2, 1, thickness);
            root.addOrReplaceChild("body", shell, PartPose.offsetAndRotation(0, 12, -10, (float) Math.PI / 2, 0, 0));
            return LayerDefinition.create(mesh, 32, 32);
        }
    }
}
