// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSizeGameTests {
    @GameTest(template = "empty")
    public static void growthScalesEveryPuffStateAndComposesWithExternalAttributes(GameTestHelper helper) {
        var types = new java.util.ArrayList<EntityType<? extends Pufferfish>>();
        types.add(EntityType.PUFFERFISH);
        PufferfishMobs.VARIANTS.values().forEach(variant -> types.add(variant.entity().get()));
        float[] expected = {1, 1.05F, 1.10F, 1.15F, 1.20F, 1.30F, 1.40F, 1.50F, 1.65F, 1.80F, 2};
        for (var type : types) {
            var fish = type.create(helper.getLevel());
            var original = fish instanceof MaterialPufferfish material ? material.fish().copy() : new ItemStack(Items.PUFFERFISH);
            float[] widths = new float[3];
            float[] heights = new float[3];
            for (int puff = 0; puff < 3; puff++) {
                fish.setPuffState(puff);
                widths[puff] = fish.getBbWidth();
                heights[puff] = fish.getBbHeight();
            }
            for (var rank : PufferfishLevel.values()) {
                var stack = original.copy();
                if (rank != PufferfishLevel.LEVEL_0) stack.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(rank, 0.5, Long.MAX_VALUE, Long.MAX_VALUE));
                store(fish, stack);
                PufferfishSize.update(fish);
                PufferfishSize.update(fish);
                for (int puff = 0; puff < 3; puff++) {
                    fish.setPuffState(puff);
                    helper.assertTrue(Math.abs(fish.getBbWidth() - widths[puff] * expected[rank.ordinal()]) < 0.0001
                            && Math.abs(fish.getBbHeight() - heights[puff] * expected[rank.ordinal()]) < 0.0001,
                            "Rank scaling changed puff proportions, depended on stars, or accumulated modifiers");
                }
            }
            var attribute = fish.getAttribute(Attributes.SCALE);
            var external = PufferfishPlease.id("test_external_scale");
            attribute.addTransientModifier(new AttributeModifier(external, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            PufferfishSize.update(fish);
            helper.assertTrue(Math.abs(attribute.getValue() - 3) < 0.0001, "Growth replaced an external scale modifier");
            store(fish, original);
            PufferfishSize.update(fish);
            helper.assertTrue(Math.abs(attribute.getValue() - 1.5) < 0.0001 && attribute.hasModifier(external), "Removing growth erased external scale");
            fish.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void liveGrowthSurvivesWorldAndBucketReload(GameTestHelper helper) {
        for (var type : java.util.List.of(EntityType.PUFFERFISH, PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get())) {
            var fish = type.create(helper.getLevel());
            var stack = fish instanceof MaterialPufferfish material ? material.fish().copy() : new ItemStack(Items.PUFFERFISH);
            stack.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.25, 0, 0));
            store(fish, stack);
            PufferfishSize.update(fish);
            var saved = new CompoundTag();
            fish.saveWithoutId(saved);
            var restored = type.create(helper.getLevel());
            restored.load(saved);
            helper.getLevel().addFreshEntity(restored);
            helper.assertTrue(restored.getScale() == 2, "World reload lost or doubled growth");
            var bucket = restored.getBucketItemStack();
            restored.saveToBucketTag(bucket);
            var released = type.create(helper.getLevel());
            released.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
            helper.assertTrue(released.getScale() == 2, "Bucket release lost growth");
            fish.discard();
            restored.discard();
            released.discard();
        }
        helper.succeed();
    }

    private static void store(Pufferfish fish, ItemStack stack) {
        if (fish instanceof MaterialPufferfish material) material.setFish(stack);
        else fish.setData(PufferfishPlease.FISH_ITEM, stack);
    }
}
