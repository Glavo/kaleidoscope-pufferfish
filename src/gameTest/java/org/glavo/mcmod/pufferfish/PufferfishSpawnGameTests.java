// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishSpawnGameTests {
    @GameTest(template = "empty", batch = "natural_cultivation")
    public static void naturalRankChancesRespectConduitsAndIgnoreMaterial(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 40, 2));
        var types = new ArrayList<EntityType<? extends Pufferfish>>();
        types.add(EntityType.PUFFERFISH);
        PufferfishMobs.VARIANTS.values().forEach(variant -> types.add(variant.entity().get()));
        try {
            for (boolean conduit : new boolean[] {false, true}) {
                PufferfishConduitSpawning.update(level, pos, conduit ? 16 : 0);
                int bound = conduit ? 2000 : 500;
                int[][] outcomes = {{0, 1}, {49, 1}, {50, 2}, {59, 2}, {60, 3}, {61, 0}, {bound - 1, 0}};
                for (var outcome : outcomes) {
                    long normalSeed = seedForRoll(true, bound, outcome[0]);
                    long materialSeed = seedForRoll(false, bound, outcome[0]);
                    for (var type : types) {
                        var fish = type.create(level);
                        fish.setPos(pos.getCenter());
                        var original = PufferfishData.stored(fish).copy();
                        fish.getRandom().setSeed(type == EntityType.PUFFERFISH ? normalSeed : materialSeed);
                        spawn(helper, fish, MobSpawnType.NATURAL);
                        var stack = PufferfishData.stored(fish);
                        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
                        if (outcome[1] == 0) {
                            helper.assertTrue(ItemStack.matches(original, stack), "Unawakened spawning created individual item data");
                        } else {
                            var expected = PufferfishLevel.values()[outcome[1]];
                            helper.assertTrue(cultivation != null && cultivation.level() == expected
                                            && cultivation.experience() == 0 && cultivation.totalExperience() == 0
                                            && cultivation.talent() >= 0 && cultivation.talent() < 1,
                                    "Incorrect natural rank, talent or initial experience: " + type + ", conduit=" + conduit + ", roll=" + outcome[0]);
                            helper.assertTrue(stack.getCount() == 1 && stack.isDamageableItem() && stack.getDamageValue() == 0
                                            && Math.abs(fish.getScale() - expected.scale()) < 0.0001,
                                    "Natural cultivation did not activate durability and body growth");
                            if (type == EntityType.PUFFERFISH) helper.assertTrue(PufferfishColors.get(stack) == PufferfishColors.get(fish),
                                    "Natural cultivation lost the fish's dye");
                        }
                        fish.discard();
                    }
                }
            }
        } finally { PufferfishConduitSpawning.update(level, pos, 0); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "natural_cultivation")
    public static void otherSpawnReasonsAndBucketReloadNeverRerollCultivation(GameTestHelper helper) {
        for (var type : List.of(EntityType.PUFFERFISH, PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get())) {
            long seed = seedForRoll(type == EntityType.PUFFERFISH, 500, 60);
            for (var reason : MobSpawnType.values()) {
                if (reason == MobSpawnType.NATURAL) continue;
                var fresh = type.create(helper.getLevel());
                fresh.getRandom().setSeed(seed);
                var before = PufferfishData.stored(fresh).copy();
                spawn(helper, fresh, reason);
                helper.assertTrue(ItemStack.matches(before, PufferfishData.stored(fresh)), "Non-natural spawn rolled cultivation: " + reason);
                fresh.discard();
            }
            var fish = type.create(helper.getLevel());
            fish.getRandom().setSeed(seed);
            spawn(helper, fish, MobSpawnType.NATURAL);
            var expected = PufferfishData.copy(fish);
            helper.assertTrue(expected.has(PufferfishPlease.CULTIVATION.get()), "Fixture did not generate a cultivated fish");
            var saved = new CompoundTag();
            fish.saveWithoutId(saved);
            var loaded = type.create(helper.getLevel());
            loaded.load(saved);
            helper.assertTrue(ItemStack.matches(expected, PufferfishData.copy(loaded)), "World reload lost natural cultivation");
            var bucket = loaded.getBucketItemStack();
            loaded.saveToBucketTag(bucket);
            var released = type.create(helper.getLevel());
            spawn(helper, released, MobSpawnType.BUCKET);
            released.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
            helper.assertTrue(ItemStack.matches(expected, PufferfishData.copy(released)), "Bucket release rerolled natural cultivation");
            var prior = PufferfishData.stored(released).get(PufferfishPlease.CULTIVATION.get());
            released.getRandom().setSeed(seed);
            spawn(helper, released, MobSpawnType.NATURAL);
            helper.assertTrue(prior.equals(PufferfishData.stored(released).get(PufferfishPlease.CULTIVATION.get())),
                    "Spawning overwrote an existing cultivated individual");
            fish.discard();
            loaded.discard();
            released.discard();
        }
        helper.succeed();
    }

    private static void spawn(GameTestHelper helper, Pufferfish fish, MobSpawnType reason) {
        NeoForge.EVENT_BUS.post(new FinalizeSpawnEvent(fish, helper.getLevel(), fish.getX(), fish.getY(), fish.getZ(),
                helper.getLevel().getCurrentDifficultyAt(fish.blockPosition()), reason, null, null));
    }

    private static long seedForRoll(boolean ordinary, int bound, int roll) {
        for (long seed = 0; seed < 100_000; seed++) {
            var random = RandomSource.create(seed);
            if (ordinary && random.nextInt(4) == 0) random.nextInt(16);
            if (random.nextInt(bound) == roll) return seed;
        }
        throw new IllegalStateException("No spawn seed for roll " + roll);
    }
}
