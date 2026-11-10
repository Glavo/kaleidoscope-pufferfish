// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishHeadgear;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

public final class PufferfishHeadRenderer {
    private static final ResourceLocation ORIGINAL = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private static final Map<HumanoidModel<?>, Visibility> VISIBILITY = new IdentityHashMap<>();
    private final DeflatingPufferfishModel<LivingEntity> model = new DeflatingPufferfishModel<>(
            Minecraft.getInstance().getEntityModels());

    public static boolean shouldRender(LivingEntity entity) {
        return PufferfishHeadgear.isWearing(entity) || entity instanceof ArmorStand
                && PufferfishItems.isPufferfish(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        if (shouldRender(event.getEntity())) {
            hide(event.getRenderer().getModel());
        }
    }

    public static void afterPlayer(RenderPlayerEvent.Post event) {
        restore(event.getRenderer().getModel());
    }

    public static void beforeLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)
                && shouldRender(event.getEntity())
                && event.getRenderer().getModel() instanceof HumanoidModel<?> model) hide(model);
    }

    public static void afterLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)
                && event.getRenderer().getModel() instanceof HumanoidModel<?> model) restore(model);
    }

    private static void hide(HumanoidModel<?> model) {
        VISIBILITY.put(model, new Visibility(model.head.visible, model.hat.visible));
        model.head.visible = false;
        model.hat.visible = false;
    }

    private static void restore(HumanoidModel<?> model) {
        var visibility = VISIBILITY.remove(model);
        if (visibility != null) {
            model.head.visible = visibility.head();
            model.hat.visible = visibility.hat();
        }
    }

    public void render(ModelPart head, PoseStack pose, MultiBufferSource buffers, int light,
            LivingEntity wearer, float ageInTicks) {
        var stack = wearer.getItemBySlot(EquipmentSlot.HEAD);
        var color = PufferfishColors.get(stack);
        var texture = PufferfishItems.isHeavy(stack) ? PufferfishTextures.heavyEntity()
                : PufferfishItems.isNetherite(stack) ? PufferfishTextures.netheriteEntity()
                : PufferfishItems.materialName(stack) != null ? PufferfishTextures.materialEntity(PufferfishItems.materialName(stack))
                : color == null ? ORIGINAL : PufferfishTextures.entity(color);
        pose.pushPose();
        if (wearer.isBaby()) {
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0, 1, 0);
        }
        head.translateAndRotate(pose);
        pose.translate(0.0F, -22.0F / 16.0F, 0.0F);
        pose.translate(0, (1 - DeflatingPufferfishModel.height(org.glavo.kaleidoscope.pufferfish.PufferfishDurability.deflationStage(stack))) / 4, 0);
        model.prepare(stack);
        model.setupAnim(wearer, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                LivingEntityRenderer.getOverlayCoords(wearer, 0.0F));
        RadiantPufferfishVisuals.render(stack, pose, buffers, LivingEntityRenderer.getOverlayCoords(wearer, 0.0F));
        pose.popPose();
    }

    private record Visibility(boolean head, boolean hat) {}
}
