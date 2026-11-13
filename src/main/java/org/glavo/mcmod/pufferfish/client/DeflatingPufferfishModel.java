// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.PufferfishBigModel;
import net.minecraft.client.model.PufferfishMidModel;
import net.minecraft.client.model.PufferfishSmallModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishDurability;

/// Uses vanilla puff states, aligned around the same center for every item rendering context.
public final class DeflatingPufferfishModel<T extends Entity> extends HierarchicalModel<T> {
    private final HierarchicalModel<T> inflated;
    private final HierarchicalModel<T> intermediate;
    private final HierarchicalModel<T> deflated;
    private HierarchicalModel<T> current;

    public DeflatingPufferfishModel(EntityModelSet models) {
        inflated = new PufferfishBigModel<>(models.bakeLayer(ModelLayers.PUFFERFISH_BIG));
        intermediate = new PufferfishMidModel<>(models.bakeLayer(ModelLayers.PUFFERFISH_MEDIUM));
        deflated = new PufferfishSmallModel<>(models.bakeLayer(ModelLayers.PUFFERFISH_SMALL));
        current = inflated;
    }

    public void prepare(ItemStack stack) {
        prepare(stack, 0);
    }

    public void prepare(ItemStack stack, float pressure) {
        int stage = PufferfishDurability.deflationStage(stack);
        current = stage == 0 ? inflated : stage == 3 ? deflated : intermediate;
        current.root().getAllParts().forEach(ModelPart::resetPose);
        float scale = stage == 1 ? 1.3F : 1;
        float center = stage == 0 ? 18 : stage == 3 ? 21.5F : 19.5F;
        var root = current.root();
        root.xScale = root.yScale = root.zScale = scale;
        root.y = 18 - center * scale;
        if (stage == 0 && pressure > 0) {
            float swelling = pressureScale(pressure);
            var body = root.getChild("body");
            body.xScale = body.yScale = body.zScale = swelling;
            root.getAllParts().filter(part -> part != root).forEach(part -> {
                part.x *= swelling;
                part.y = 18 + (part.y - 18) * swelling;
                part.z *= swelling;
            });
        }
    }

    /// Enlarges the body while keeping the attached fins and spines their original size.
    public static float pressureScale(float pressure) { return 1 + Math.clamp(pressure, 0, 4) * 0.15F; }

    /// Relative height including the small fish's raised eyes, used to keep its underside supported.
    public static float height(int stage) {
        return switch (stage) { case 1 -> 6.5F / 8; case 2 -> 5.0F / 8; case 3 -> 3.0F / 8; default -> 1; };
    }

    /// Lowest vertex relative to the placed model's support plane, including the angled lower fins.
    public static float floorOffset(int stage) {
        return org.glavo.mcmod.pufferfish.PufferfishSize.floorOffset(stage);
    }

    @Override public ModelPart root() { return current.root(); }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        current.setupAnim(entity, limbSwing, limbSwingAmount, age, yaw, pitch);
    }
}
