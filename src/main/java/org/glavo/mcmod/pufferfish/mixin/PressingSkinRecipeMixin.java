// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishSkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.simibubi.create.foundation.recipe.RecipeApplier", remap = false)
public abstract class PressingSkinRecipeMixin {
    @Inject(method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void pufferfishPlease$inheritSkinLevel(Level level, ItemStack input, Recipe<?> recipe, boolean includeRemainders,
            CallbackInfoReturnable<List<ItemStack>> callback) {
        if (recipe instanceof com.simibubi.create.content.kinetics.press.PressingRecipe && PufferfishItems.isPufferfish(input)) {
            var results = new java.util.ArrayList<>(callback.getReturnValue());
            results.replaceAll(result -> PufferfishSkinBlock.inheritLevel(input, result));
            callback.setReturnValue(results);
        }
    }
}
