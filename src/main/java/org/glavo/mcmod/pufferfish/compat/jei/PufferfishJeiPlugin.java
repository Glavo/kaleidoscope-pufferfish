// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

@JeiPlugin
public final class PufferfishJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return PufferfishPlease.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(PufferfishItems.FLAIL.get(), new ISubtypeInterpreter<>() {
            @Override
            public DyeColor getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(PufferfishPlease.COLOR.get());
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                var color = getSubtypeData(stack, context);
                return color == null ? "" : color.getName();
            }
        });
    }
}
