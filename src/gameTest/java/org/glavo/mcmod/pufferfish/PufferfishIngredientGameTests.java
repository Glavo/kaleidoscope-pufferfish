// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishIngredientGameTests {
    private static List<DyeColor> colors() {
        var colors = new ArrayList<DyeColor>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        return colors;
    }

    private static ItemStack fish(DyeColor color) {
        var stack = new ItemStack(Items.PUFFERFISH);
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    @GameTest(template = "empty")
    public static void ingredientNetworkSyncPreservesColorMatching(GameTestHelper helper) {
        for (var color : colors()) {
            Ingredient original = new PufferfishIngredient(Optional.ofNullable(color)).toVanilla();
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, original);
                var decoded = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                helper.assertTrue(!decoded.isSimple() && decoded.getItems().length == 1, "Ingredient lost component matching or display stack");
                helper.assertTrue(ItemStack.isSameItemSameComponents(decoded.getItems()[0], fish(color)), "Ingredient display lost color");
                for (var candidate : colors()) {
                    helper.assertTrue(decoded.test(fish(candidate)) == (candidate == color), "Synced ingredient accepted the wrong color");
                }
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

}
