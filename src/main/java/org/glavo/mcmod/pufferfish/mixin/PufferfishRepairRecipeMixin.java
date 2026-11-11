// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishDurability;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class PufferfishRepairRecipeMixin {
    /// Vanilla repair creates a fresh stack with max damage but retains its default stack limit.
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void pufferfishPlease$repairFish(CraftingInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> callback) {
        var result = callback.getReturnValue();
        if (!PufferfishItems.isWeapon(result)) return;
        result.set(DataComponents.MAX_STACK_SIZE, 1);
        PufferfishDurability.normalize(result);
        for (var ingredient : input.items()) {
            if (!ingredient.isEmpty()) {
                var color = ingredient.get(org.glavo.mcmod.pufferfish.PufferfishPlease.COLOR.get());
                if (color != null) {
                    if (PufferfishItems.isFlail(result)) result.set(org.glavo.mcmod.pufferfish.PufferfishPlease.COLOR.get(), color);
                    else PufferfishColors.dye(result, color);
                }
                break;
            }
        }
    }
}
