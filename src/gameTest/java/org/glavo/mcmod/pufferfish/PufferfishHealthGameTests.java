// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishHealthGameTests {
    private static List<EntityType<? extends Pufferfish>> types() {
        var types = new ArrayList<EntityType<? extends Pufferfish>>();
        types.add(EntityType.PUFFERFISH);
        PufferfishMobs.VARIANTS.values().forEach(variant -> types.add(variant.entity().get()));
        return types;
    }

    @GameTest(template = "empty")
    public static void rankHealthPreservesInjuriesAndExternalModifiers(GameTestHelper helper) {
        double[] multipliers = {1, 1.2, 1.5, 2, 2.7, 3.6, 4.8, 6.2, 7.5, 8.7, 10};
        for (var type : types()) {
            var fish = type.create(helper.getLevel());
            var original = PufferfishData.copy(fish);
            float base = fish.getMaxHealth();
            double armor = fish.getAttributeValue(Attributes.ARMOR);
            fish.setHealth(base * 0.25F);
            for (int rank = 0; rank <= 10; rank++) {
                var stack = original.copy();
                if (rank > 0) stack.set(PufferfishPlease.CULTIVATION.get(),
                        new PufferfishCultivation(PufferfishLevel.values()[rank], 0.5, Long.MAX_VALUE, Long.MAX_VALUE));
                PufferfishData.set(fish, stack);
                PufferfishHealth.update(fish);
                PufferfishHealth.update(fish);
                assertHealth(helper, fish, base * multipliers[rank], 0.25);
                helper.assertTrue(fish.getAttributeValue(Attributes.ARMOR) == armor, "Rank increased armor");
                if (fish instanceof MaterialPufferfish material) {
                    helper.assertTrue(material.canCapture(), "Capture threshold did not scale with maximum health");
                }
            }
            var attribute = fish.getAttribute(Attributes.MAX_HEALTH);
            var external = PufferfishPlease.id("test_health_bonus");
            attribute.addTransientModifier(new AttributeModifier(external, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            fish.setHealth(fish.getMaxHealth() * 0.25F);
            PufferfishHealth.update(fish);
            assertHealth(helper, fish, Math.min(base * 15, 1024), 0.25);
            PufferfishData.set(fish, original);
            assertHealth(helper, fish, base * 1.5, 0.25);
            helper.assertTrue(attribute.hasModifier(external), "Removing rank erased external health modifiers");
            fish.setHealth(0);
            var ranked = original.copy();
            ranked.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, 0, 0));
            PufferfishData.set(fish, ranked);
            helper.assertTrue(fish.getHealth() == 0, "Rank change revived a dead fish");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rankedHealthSurvivesWorldAndBucketRoundTrips(GameTestHelper helper) {
        for (var type : types()) {
            var fish = type.create(helper.getLevel());
            float base = fish.getMaxHealth();
            var stack = PufferfishData.copy(fish);
            if (type == EntityType.PUFFERFISH) stack = PufferfishColors.dye(stack, DyeColor.BLUE);
            stack.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.25, 0, 0));
            PufferfishData.set(fish, stack);
            assertHealth(helper, fish, base * 10, 1);
            fish.setHealth(fish.getMaxHealth() * 0.25F);
            for (int cycle = 0; cycle < 3; cycle++) {
                var saved = new CompoundTag();
                fish.saveWithoutId(saved);
                fish.discard();
                fish = type.create(helper.getLevel());
                fish.load(saved);
                helper.getLevel().addFreshEntity(fish);
                assertHealth(helper, fish, base * 10, 0.25);
                var bucket = fish.getBucketItemStack();
                fish.saveToBucketTag(bucket);
                fish.discard();
                fish = type.create(helper.getLevel());
                fish.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
                helper.getLevel().addFreshEntity(fish);
                assertHealth(helper, fish, base * 10, 0.25);
                if (type == EntityType.PUFFERFISH) helper.assertTrue(PufferfishColors.get(fish) == DyeColor.BLUE,
                        "Bucket release lost the fish color");
            }
            fish.discard();
        }
        helper.succeed();
    }

    private static void assertHealth(GameTestHelper helper, Pufferfish fish, double maximum, double fraction) {
        helper.assertTrue(Math.abs(fish.getMaxHealth() - maximum) < 0.001
                && Math.abs(fish.getHealth() / fish.getMaxHealth() - fraction) < 0.0001,
                "Incorrect rank health for " + fish.getType() + ": " + fish.getHealth() + "/" + fish.getMaxHealth()
                        + "; expected maximum " + maximum + " at fraction " + fraction);
    }
}
