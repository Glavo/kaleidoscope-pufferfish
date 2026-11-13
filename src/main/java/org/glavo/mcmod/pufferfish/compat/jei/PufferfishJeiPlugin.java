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
    public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FishProcessingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(FishProcessingCategory.TYPE,
                level.getRecipeManager().getAllRecipesFor(PufferfishPlease.FISH_PROCESSING_TYPE.get()).stream().map(net.minecraft.world.item.crafting.RecipeHolder::value).toList());
        registration.addItemStackInfo(new ItemStack(org.glavo.mcmod.pufferfish.PufferfishBlocks.PROCESSING_TABLE.get()),
                net.minecraft.network.chat.Component.translatable("tooltip.pufferfish_please.processing_table.cut"),
                net.minecraft.network.chat.Component.translatable("tooltip.pufferfish_please.processing_table.brush"),
                net.minecraft.network.chat.Component.translatable("tooltip.pufferfish_please.processing_table.retrieve"));
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(org.glavo.mcmod.pufferfish.PufferfishBlocks.PROCESSING_TABLE.get()), FishProcessingCategory.TYPE);
    }
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
