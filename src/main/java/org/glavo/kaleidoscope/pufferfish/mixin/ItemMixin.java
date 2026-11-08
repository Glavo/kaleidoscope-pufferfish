// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void kaleidoscope$coloredName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        var color = PufferfishColors.get(stack);
        if (color != null) {
            String key = stack.is(Items.PUFFERFISH_BUCKET) ? "dyed_pufferfish_bucket" : "dyed_pufferfish";
            cir.setReturnValue(Component.translatable("item.kaleidoscope_pufferfish." + key,
                    Component.translatable("color.minecraft." + color.getName())));
        }
    }
}
