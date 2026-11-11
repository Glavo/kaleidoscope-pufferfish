// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.create;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.PufferfishBlocks;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

/// Verifies the mechanical press processes fish into skins without losing their colors.
@PrefixGameTestTemplate(false)
public final class PufferfishSkinPressingGameTests {
    private PufferfishSkinPressingGameTests() {}

    /// Verifies recipe lookup, simulation, and actual world-item processing for all skin variants.
    @GameTest(template = "empty", templateNamespace = PufferfishPlease.MODID)
    public static void pressingOneFishProducesOneMatchingSkin(GameTestHelper helper) {
        var pos = new BlockPos(1, 3, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_press")));
        var press = (MechanicalPressBlockEntity) helper.getBlockEntity(pos);
        var colors = new ArrayList<DyeColor>();
        colors.add(null);
        colors.addAll(Arrays.asList(DyeColor.values()));
        var itemPos = Vec3.atCenterOf(helper.absolutePos(pos.below(2)));
        var variants = new java.util.LinkedHashMap<ItemStack, org.glavo.mcmod.pufferfish.PufferfishSkinBlock>();
        for (var color : colors) {
            var fish = new ItemStack(Items.PUFFERFISH);
            if (color != null) PufferfishColors.dye(fish, color);
            variants.put(fish, PufferfishBlocks.skin(color));
        }
        variants.put(new ItemStack(PufferfishItems.GOLDEN.get()), PufferfishBlocks.GOLDEN_SKIN.get());
        variants.put(new ItemStack(PufferfishItems.COPPER.get()), PufferfishBlocks.COPPER_SKIN.get());
        variants.put(new ItemStack(PufferfishItems.IRON.get()), PufferfishBlocks.IRON_SKIN.get());
        variants.put(new ItemStack(PufferfishItems.DIAMOND.get()), PufferfishBlocks.DIAMOND_SKIN.get());
        variants.put(new ItemStack(PufferfishItems.NETHERITE.get()), PufferfishBlocks.NETHERITE_SKIN.get());
        variants.put(new ItemStack(PufferfishItems.HEAVY.get()), PufferfishBlocks.HEAVY_SKIN.get());
        for (var entry : variants.entrySet()) {
            var fish = entry.getKey();
            var skin = entry.getValue();
            var skinName = BuiltInRegistries.BLOCK.getKey(skin).getPath();
            var recipe = press.getRecipe(fish).orElseThrow();
            helper.assertTrue(recipe.id().equals(PufferfishPlease.id("pressing/" + skinName)),
                    "Press selected the wrong skin color");
            var entity = new ItemEntity(helper.getLevel(), itemPos.x, itemPos.y, itemPos.z, fish.copy());
            helper.getLevel().addFreshEntity(entity);
            helper.assertTrue(press.tryProcessInWorld(entity, true) && ItemStack.matches(fish, entity.getItem()),
                    "Press simulation consumed or changed its fish");
            helper.assertTrue(press.tryProcessInWorld(entity, false), "Press failed to process its fish");
            var items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(1));
            helper.assertTrue(items.size() == 1 && items.getFirst().getItem().is(skin.asItem())
                    && items.getFirst().getItem().getCount() == 1, "Press produced the wrong skin or quantity");
            items.forEach(ItemEntity::discard);
        }
        for (var item : List.of(Items.COD, Items.PUFFERFISH_BUCKET, PufferfishItems.SASHIMI.get())) {
            helper.assertTrue(press.getRecipe(new ItemStack(item)).isEmpty(), "Press accepted an unsupported fish item");
        }
        helper.succeed();
    }
}
