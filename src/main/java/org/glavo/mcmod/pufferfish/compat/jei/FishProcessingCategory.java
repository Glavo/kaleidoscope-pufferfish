// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.FishProcessingRecipe;
import org.glavo.mcmod.pufferfish.PufferfishBlocks;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

public final class FishProcessingCategory implements IRecipeCategory<FishProcessingRecipe> {
    public static final RecipeType<FishProcessingRecipe> TYPE = RecipeType.create(PufferfishPlease.MODID, "fish_processing", FishProcessingRecipe.class);
    private final IDrawable icon;

    public FishProcessingCategory(IGuiHelper gui) { icon = gui.createDrawableItemStack(new ItemStack(PufferfishBlocks.PROCESSING_TABLE.get())); }
    @Override public RecipeType<FishProcessingRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.pufferfish_please.fish_processing"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 82; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, FishProcessingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 27).addIngredients(recipe.ingredient()).setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.CATALYST, 38, 27).addIngredients(recipe.tool()).setStandardSlotBackground();
        for (int i = 0; i < recipe.results().size(); i++) {
            builder.addOutputSlot(82 + i % 4 * 20, 17 + i / 4 * 22).addItemStack(recipe.results().get(i)).setStandardSlotBackground();
        }
    }

    @Override public void draw(FishProcessingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font, ">", 63, 31, 0x555555, false);
        graphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.pufferfish_please.processing_strokes", recipe.strokes()), 8, 66, 0x555555, false);
    }
}
