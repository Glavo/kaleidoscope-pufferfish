// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.cookery;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModFoods;
import com.github.ysbbbbbb.kaleidoscopecookery.item.BowlFoodBlockItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;

/// Registers placeable meals using Cookery's portion, quality, and container behavior.
public final class CookeryFoodIntegration {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);

    public static final DeferredBlock<FoodBiteBlock> STARGAZY_PIE = BLOCKS.register("pufferfish_stargazy_pie",
            () -> new FoodBiteBlock(withPoison(ModFoods.STARGAZY_PIE_BLOCK), 4, null).setAABB(Block.box(1, 0, 1, 15, 12, 15)));
    public static final DeferredItem<BowlFoodBlockItem> STARGAZY_PIE_ITEM = ITEMS.register("pufferfish_stargazy_pie",
            () -> new BowlFoodBlockItem(STARGAZY_PIE.get(), withPoison(ModFoods.STARGAZY_PIE_ITEM), Items.BOWL));

    private CookeryFoodIntegration() {}

    /// Uses pufferfish's food poison before Cookery applies food quality scaling.
    private static FoodProperties withPoison(FoodProperties food) {
        var effects = new ArrayList<>(food.effects());
        effects.add(Foods.PUFFERFISH.effects().stream()
                .filter(effect -> effect.effect().getEffect().equals(MobEffects.POISON)).findFirst().orElseThrow());
        return new FoodProperties(food.nutrition(), food.saturation(), food.canAlwaysEat(), food.eatSeconds(),
                food.usingConvertsTo(), List.copyOf(effects));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
