// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelPart;
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
                .apply(PufferfishPlease.id("entity/cat/" + variant.skinName() + "_cat_armor"));
        var vertices = sprite.wrap(buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)));
        int overlay = LivingEntityRenderer.getOverlayCoords(cat, 0);
        armor.renderToBuffer(pose, vertices, light, overlay, -1);
    }

    private static final class ArmorModel extends CatModel<Cat> {
        private ArmorModel(boolean heavy) { super(mesh(heavy)); }

        private static ModelPart mesh(boolean heavy) {
            var mesh = CatModel.createBodyMesh(CubeDeformation.NONE);
            var root = mesh.getRoot();
            for (var part : new String[] {"tail1", "tail2"}) {
                root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
            }
            var thickness = new CubeDeformation(heavy ? 0.4F : 0.2F);
            var head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0, 15, -9));
            head.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-3, -3, -2, 6, 6, 4, thickness), PartPose.offset(0, 0, -0.5F));
            var body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                    PartPose.offsetAndRotation(0, 12, -10, (float) Math.PI / 2, 0, 0));
            body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(21, 0)
                    .addBox(-4, 0, -3.5F, 8, 6, 7, thickness), PartPose.offset(0, 3, -5));
            body.addOrReplaceChild("back", CubeListBuilder.create().texOffs(18, 14)
                    .addBox(-3, 0, -3, 6, 9, 6, thickness), PartPose.offset(0, 9, -5));
            for (boolean front : new boolean[] {true, false}) {
                for (boolean left : new boolean[] {true, false}) {
                    String name = (left ? "left" : "right") + (front ? "_front_leg" : "_hind_leg");
                    var leg = root.addOrReplaceChild(name, CubeListBuilder.create(),
                            PartPose.offset((left ? 1 : -1) * (front ? 1.2F : 1.1F), front ? 14.1F : 18, front ? -5 : 5));
                    leg.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 18)
                            .addBox(-1, 0, -1, 2, 8, 2, thickness), PartPose.offset(0, 0, front ? 1 : 2));
                }
            }
            var baked = LayerDefinition.create(mesh, 64, 32).bakeRoot();
            // Keep the wolf armor UV layout while fitting each plate to the cat's animated parts.
            var headPlate = baked.getChild("head").getChild("plate");
            headPlate.xScale = 5.0F / 6;
            headPlate.yScale = 4.0F / 6;
            headPlate.zScale = 5.0F / 4;
            var chest = baked.getChild("body").getChild("chest");
            chest.xScale = 0.5F;
            chest.zScale = 6.0F / 7;
            var back = baked.getChild("body").getChild("back");
            back.xScale = 4.0F / 6;
            back.yScale = 10.0F / 9;
            for (var side : new String[] {"left", "right"}) {
                baked.getChild(side + "_front_leg").getChild("plate").yScale = 10.0F / 8;
                baked.getChild(side + "_hind_leg").getChild("plate").yScale = 6.0F / 8;
            }
            return baked;
        }
    }
}
