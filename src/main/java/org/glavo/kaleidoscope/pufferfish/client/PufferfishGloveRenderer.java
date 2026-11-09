// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

public final class PufferfishGloveRenderer {
    private static final ResourceLocation ORIGINAL = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private static PufferfishGloveRenderer firstPerson;
    private final PufferfishBigModel<LivingEntity> model = new PufferfishBigModel<>(
            Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));

    public static void reset() {
        firstPerson = null;
    }

    public static void renderHand(RenderHandEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !PufferfishItems.isPufferfish(event.getItemStack())
                || player.isUsingItem() && player.getUsedItemHand() == event.getHand()) return;
        if (firstPerson == null) firstPerson = new PufferfishGloveRenderer();
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        firstPerson.renderFirstPerson(player, event.getItemStack(), arm, event.getEquipProgress(), event.getSwingProgress(),
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        event.setCanceled(true);
    }

    public void renderAttached(ArmedModel parent, LivingEntity player, ItemStack stack, HumanoidArm arm,
            PoseStack pose, MultiBufferSource buffers, int light, float age) {
        pose.pushPose();
        parent.translateToHand(arm, pose);
        pose.translate((arm == HumanoidArm.RIGHT ? -1 : 1) / 16.0F, 8.5F / 16, 0);
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.translate(0, -18.0F / 16, 0);
        var color = PufferfishColors.get(stack);
        var texture = PufferfishItems.isHeavy(stack) ? PufferfishTextures.heavyEntity()
                : PufferfishItems.isNetherite(stack) ? PufferfishTextures.netheriteEntity()
                : PufferfishItems.materialName(stack) != null ? PufferfishTextures.materialEntity(PufferfishItems.materialName(stack))
                : color == null ? ORIGINAL : PufferfishTextures.entity(color);
        model.setupAnim(player, 0, 0, age, 0, 0);
        model.renderToBuffer(pose, ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(texture),
                false, stack.hasFoil()), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    public void renderFirstPerson(AbstractClientPlayer player, ItemStack stack, HumanoidArm arm, float equip, float swing,
            PoseStack pose, MultiBufferSource buffers, int light) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float rootSwing = Mth.sqrt(swing);
        float arc = Mth.sin(rootSwing * Mth.PI);
        pose.pushPose();
        // Follow the vanilla bare-hand transform so the glove stays attached throughout a punch.
        pose.translate(side * (-0.3F * arc + 0.64000005F),
                0.4F * Mth.sin(rootSwing * Mth.TWO_PI) - 0.6F - equip * 0.6F,
                -0.4F * Mth.sin(swing * Mth.PI) - 0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45));
        pose.mulPose(Axis.YP.rotationDegrees(side * arc * 70));
        pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(swing * swing * Mth.PI) * -20));
        pose.translate(side * -1, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120));
        pose.mulPose(Axis.XP.rotationDegrees(200));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135));
        pose.translate(side * 5.6F, 0, 0);
        var renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!player.isInvisible()) {
            if (arm == HumanoidArm.RIGHT) renderer.renderRightHand(pose, buffers, light, player);
            else renderer.renderLeftHand(pose, buffers, light, player);
        } else {
            var parent = renderer.getModel();
            parent.attackTime = 0;
            parent.crouching = false;
            parent.swimAmount = 0;
            parent.setupAnim(player, 0, 0, 0, 0, 0);
            (arm == HumanoidArm.RIGHT ? parent.rightArm : parent.leftArm).xRot = 0;
        }
        renderAttached(renderer.getModel(), player, stack, arm, pose, buffers, light, player.tickCount);
        pose.popPose();
    }
}
