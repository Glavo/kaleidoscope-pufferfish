// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishSkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe", remap = false)
public abstract class CuttingBoardRecipeMixin {
    @Inject(method = "rollResults", at = @At("RETURN"), cancellable = true)
    private void pufferfishPlease$inheritSkinLevel(RandomSource random, int fortune, RecipeWrapper input,
            CallbackInfoReturnable<List<ItemStack>> callback) {
        var fish = input.getItem(0);
        if (PufferfishItems.isPufferfish(fish)) {
            var results = new java.util.ArrayList<>(callback.getReturnValue());
            results.replaceAll(result -> PufferfishSkinBlock.inheritLevel(fish, result));
            callback.setReturnValue(results);
        }
    }
}
