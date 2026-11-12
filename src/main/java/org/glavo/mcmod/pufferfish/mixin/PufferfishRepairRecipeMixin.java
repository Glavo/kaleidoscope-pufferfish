// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
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
        for (var ingredient : input.items()) {
            if (!ingredient.isEmpty()) {
                var state = ingredient.get(PufferfishPlease.MANAGED_DURABILITY.get());
                if (state != null && state.matches(ingredient) && state.matches(result)
                        && input.items().stream().filter(stack -> !stack.isEmpty()).allMatch(stack ->
                                state.equals(stack.get(PufferfishPlease.MANAGED_DURABILITY.get())) && state.matches(stack))) {
                    result.set(PufferfishPlease.MANAGED_DURABILITY.get(), state);
                }
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
