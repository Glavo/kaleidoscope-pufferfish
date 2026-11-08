// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishHeadgear;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

public final class PufferfishHeadRenderer {
    private static final ResourceLocation ORIGINAL = ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private static final Map<PlayerModel<?>, Visibility> VISIBILITY = new IdentityHashMap<>();
    private final PufferfishBigModel<LivingEntity> model = new PufferfishBigModel<>(
            Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PUFFERFISH_BIG));

    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        if (PufferfishHeadgear.isWearing(event.getEntity())) {
            var model = event.getRenderer().getModel();
            VISIBILITY.put(model, new Visibility(model.head.visible, model.hat.visible));
            model.head.visible = false;
            model.hat.visible = false;
        }
    }

    public static void afterPlayer(RenderPlayerEvent.Post event) {
        var model = event.getRenderer().getModel();
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
        var texture = PufferfishItems.isNetherite(stack) ? PufferfishTextures.netheriteEntity()
                : color == null ? ORIGINAL : PufferfishTextures.entity(color);
        pose.pushPose();
        head.translateAndRotate(pose);
        pose.translate(0.0F, -22.0F / 16.0F, 0.0F);
        model.setupAnim(wearer, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                LivingEntityRenderer.getOverlayCoords(wearer, 0.0F));
        pose.popPose();
    }

    private record Visibility(boolean head, boolean hat) {}
}
