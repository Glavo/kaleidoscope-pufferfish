// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void kaleidoscope$model(ItemStack stack, Level level, LivingEntity entity, int seed,
                                    CallbackInfoReturnable<BakedModel> cir) {
        var color = PufferfishColors.get(stack);
        if (color != null) {
            var model = ModelResourceLocation.standalone(PufferfishTextures.item(color, stack.is(Items.PUFFERFISH_BUCKET)));
            cir.setReturnValue(Minecraft.getInstance().getModelManager().getModel(model));
        }
    }
}
