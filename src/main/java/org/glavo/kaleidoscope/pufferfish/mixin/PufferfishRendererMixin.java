// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PufferfishRenderer.class)
public abstract class PufferfishRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Pufferfish;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void kaleidoscope$texture(Pufferfish fish, CallbackInfoReturnable<ResourceLocation> cir) {
        var color = PufferfishColors.get(fish);
        if (color != null) {
            cir.setReturnValue(PufferfishTextures.entity(color));
        }
    }
}
