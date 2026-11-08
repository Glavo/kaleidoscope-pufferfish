// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(KaleidoscopePufferfish.MODID)
public class KaleidoscopePufferfish {
    public static final String MODID = "kaleidoscope_pufferfish";
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> COLOR = COMPONENTS.register("color",
            () -> DataComponentType.<DyeColor>builder().persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC).build());
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PufferfishDyeRecipe>> DYE_RECIPE =
            RECIPES.register("dye_pufferfish", () -> new SimpleCraftingRecipeSerializer<>(PufferfishDyeRecipe::new));

    static {
        TABS.register("pufferfish", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.kaleidoscope_pufferfish"))
                .icon(() -> PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), DyeColor.PURPLE))
                .displayItems((parameters, output) -> {
                    for (DyeColor color : DyeColor.values()) {
                        output.accept(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
                        output.accept(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), color));
                    }
                }).build());
    }

    public KaleidoscopePufferfish(IEventBus bus) {
        COMPONENTS.register(bus);
        RECIPES.register(bus);
        TABS.register(bus);
        NeoForge.EVENT_BUS.addListener(PufferfishColors::interact);
        NeoForge.EVENT_BUS.addListener(PufferfishColors::colorDrops);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
