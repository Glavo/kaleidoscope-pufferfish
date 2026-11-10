// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.PufferfishPumpItem;

/// A copper hand pump with a sliding iron piston and wooden cross handle.
public final class PufferfishPumpRenderer extends BlockEntityWithoutLevelRenderer implements IClientItemExtensions {
    private final ModelPart barrel;
    private final ModelPart piston;
    private final ModelPart handle;

    public PufferfishPumpRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        barrel = part(CubeListBuilder.create().texOffs(0, 0).addBox(-2, -5, -2, 4, 9, 4)
                .texOffs(0, 0).addBox(-3.5F, -6, -2.5F, 7, 1, 5)
                .texOffs(0, 0).addBox(2, -3, -1, 3, 1, 2)
                .texOffs(0, 0).addBox(4, -3, -1, 1, 3, 2));
        piston = part(CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, 2, -0.5F, 1, 7, 1));
        handle = part(CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, 8, -1, 7, 1.5F, 2));
    }

    private static ModelPart part(CubeListBuilder cubes) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("part", cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() { return this; }

    public static float pistonOffset(float ticks) {
        return (float) (2 * (1 - Math.cos(ticks * Math.PI * 2 / PufferfishPumpItem.STROKE_TICKS))) / 16;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float offset = 0;
        var minecraft = Minecraft.getInstance();
        boolean held = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if (held && minecraft.level != null) {
            for (var player : minecraft.level.players()) {
                if (player.isUsingItem() && player.getUseItem() == stack) {
                    offset = pistonOffset(player.getTicksUsingItem() + minecraft.getTimer().getGameTimeDeltaPartialTick(false));
                    break;
                }
            }
        }
        pose.pushPose();
        pose.translate(0.5, 0.40, 0.5);
        render(barrel, "copper_block", stack, pose, buffers, light, overlay);
        pose.translate(0, -offset, 0);
        render(piston, "iron_block", stack, pose, buffers, light, overlay);
        render(handle, "stripped_dark_oak_log", stack, pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void render(ModelPart part, String texture, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        part.render(pose, ItemRenderer.getFoilBufferDirect(buffers,
                RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/block/" + texture + ".png")),
                false, stack.hasFoil()), light, overlay);
    }
}
