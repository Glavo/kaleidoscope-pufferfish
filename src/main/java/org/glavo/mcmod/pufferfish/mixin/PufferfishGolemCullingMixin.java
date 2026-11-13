// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.IronGolem;
import org.glavo.mcmod.pufferfish.PufferfishHeadgear;
import org.glavo.mcmod.pufferfish.client.PufferfishGolemHeadLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
abstract class PufferfishGolemCullingMixin {
    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private boolean pufferfishPlease$includeTower(boolean original, Entity entity, Frustum frustum, double x, double y, double z) {
        if (original || !(entity instanceof IronGolem golem) || !PufferfishHeadgear.isWearing(golem)) return original;
        var bounds = PufferfishGolemHeadLayer.bounds(golem);
        double range = 64 * Entity.getViewScale();
        double dx = Math.max(bounds.minX - x, Math.max(0, x - bounds.maxX));
        double dy = Math.max(bounds.minY - y, Math.max(0, y - bounds.maxY));
        double dz = Math.max(bounds.minZ - z, Math.max(0, z - bounds.maxZ));
        return dx * dx + dy * dy + dz * dz < range * range && frustum.isVisible(bounds);
    }
}
