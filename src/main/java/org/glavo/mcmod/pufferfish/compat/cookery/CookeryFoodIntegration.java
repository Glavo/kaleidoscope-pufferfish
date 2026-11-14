// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.cookery;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModEffects;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModFoods;
import com.github.ysbbbbbb.kaleidoscopecookery.item.BowlFoodBlockItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.FoodWithEffectsItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

/// Creates meals using Cookery's quality, effects, and placeable portion behavior.
public final class CookeryFoodIntegration {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PufferfishPlease.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);

    public static final DeferredBlock<FoodBiteBlock> STARGAZY_PIE = BLOCKS.register("pufferfish_stargazy_pie",
            () -> new FoodBiteBlock(withPoison(ModFoods.STARGAZY_PIE_BLOCK), 4, null).setAABB(Block.box(1, 0, 1, 15, 12, 15)));
    public static final DeferredItem<BowlFoodBlockItem> STARGAZY_PIE_ITEM = ITEMS.register("pufferfish_stargazy_pie",
            () -> new BowlFoodBlockItem(STARGAZY_PIE.get(), withPoison(ModFoods.STARGAZY_PIE_ITEM), Items.BOWL));
    public static final DeferredBlock<FoodBiteBlock> ASPIC = BLOCKS.register("pufferfish_aspic",
            () -> new FoodBiteBlock(withPoison(new FoodProperties.Builder().nutrition(2).saturationModifier(0.6F).build()), 4, null)
                    .setAABB(Block.box(2, 0, 2, 14, 7, 14)));
    public static final DeferredItem<BowlFoodBlockItem> ASPIC_ITEM = ITEMS.register("pufferfish_aspic",
            () -> new BowlFoodBlockItem(ASPIC.get(), withPoison(new FoodProperties.Builder().nutrition(8).saturationModifier(0.6F).build()), Items.BOWL));

    private CookeryFoodIntegration() {}

    /// Applies Cookery's nutrition, effect-duration, and tooltip handling without changing bowl remainders.
    public static Item meal(FoodProperties.Builder food) {
        return new FoodWithEffectsItem(food.build());
    }

    /// Matches the three-minute satiated shield supplied by Cookery's rice bowls.
    public static Item riceBowl(FoodProperties.Builder food) {
        return meal(food.effect(() -> new MobEffectInstance(ModEffects.SATIATED_SHIELD, 3 * 60 * 20), 1));
    }

    /// Retains the ten-minute mustard effect of Cookery's pufferfish soup.
    public static Item zosui(FoodProperties.Builder food) {
        return meal(food.effect(() -> new MobEffectInstance(ModEffects.MUSTARD, 10 * 60 * 20), 1));
    }

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
