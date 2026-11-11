// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishMobGameTests {
    @GameTest(template = "empty")
    public static void allMaterialFishSurviveSaveCaptureAndRelease(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(level);
            fish.setPos(pos.getX(), pos.getY(), pos.getZ());
            fish.setNoAi(true);
            fish.setCustomName(Component.literal("Keepsake"));
            fish.setHealth(2);
            var stored = fish.fish().copy();
            stored.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            fish.setFish(stored);
            var tag = new CompoundTag();
            fish.saveWithoutId(tag);
            var restored = variant.entity().get().create(level);
            restored.load(tag);
            helper.assertTrue(ItemStack.isSameItemSameComponents(restored.fish(), stored), "Saved fish lost its variant or components");
            level.addFreshEntity(restored);
            var player = FakePlayerFactory.getMinecraft(level);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            restored.interact(player, InteractionHand.MAIN_HAND);
            var bucket = player.getMainHandItem().copy();
            helper.assertTrue(restored.isRemoved() && bucket.is(variant.bucket().get()), "Capture did not return the matching fish bucket");
            ((MobBucketItem) bucket.getItem()).checkExtraContent(null, level, bucket, pos);
            var released = level.getEntitiesOfClass(MaterialPufferfish.class, new AABB(pos).inflate(2));
            helper.assertTrue(released.size() == 1, "Bucket did not release exactly one material fish");
            var result = released.getFirst();
            helper.assertTrue(ItemStack.isSameItemSameComponents(result.fish(), stored) && result.fromBucket()
                    && result.isNoAi() && result.getHealth() == 2 && "Keepsake".equals(result.getName().getString()), "Released fish lost saved data");
            result.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketCraftingConsumesOneBucketAndPreservesFish(GameTestHelper helper) {
        var recipe = new PufferfishBucketRecipe(CraftingBookCategory.MISC);
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = new ItemStack(variant.fish().get());
            fish.set(DataComponents.CUSTOM_NAME, Component.literal("Crafted fish"));
            var input = CraftingInput.of(2, 1, List.of(fish, new ItemStack(Items.WATER_BUCKET)));
            var result = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(result.is(variant.bucket().get()) && result.getHoverName().getString().equals("Crafted fish")
                    && recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Fish bucket recipe duplicated a bucket or lost its name");
        }
        for (var color : DyeColor.values()) {
            var input = CraftingInput.of(2, 1, List.of(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color), new ItemStack(Items.WATER_BUCKET)));
            helper.assertTrue(PufferfishColors.get(recipe.assemble(input, helper.getLevel().registryAccess())) == color, "Fish bucket recipe lost dye");
        }
        helper.assertTrue(!recipe.matches(CraftingInput.of(2, 1, List.of(new ItemStack(PufferfishItems.RADIANT.get()), new ItemStack(Items.BUCKET))), helper.getLevel()),
                "Empty buckets should not revive fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperFishOxidizeWaxAndScrapeWithoutLosingTheirState(GameTestHelper helper) {
        var level = helper.getLevel();
        var fish = PufferfishMobs.variant(PufferfishItems.COPPER.toStack()).entity().get().create(level);
        var player = FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fish.oxidize();
        helper.assertTrue(CopperPufferfish.index(fish.fish()) == 1, "Copper fish did not oxidize");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 2));
        fish.interact(player, InteractionHand.MAIN_HAND);
        fish.oxidize();
        helper.assertTrue(CopperPufferfish.index(fish.fish()) == 5 && player.getMainHandItem().getCount() == 1, "Wax did not stop oxidation");
        var bucket = fish.getBucketItemStack();
        fish.saveToBucketTag(bucket);
        helper.assertTrue(bucket.is(PufferfishMobs.variant(fish.fish()).bucket().get()), "Oxidized fish returned the wrong bucket");
        var restored = PufferfishMobs.VARIANTS.get("waxed_exposed_copper_pufferfish").entity().get().create(level);
        restored.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
        helper.assertTrue(CopperPufferfish.index(restored.fish()) == 5, "Bucket lost the waxed oxidation stage");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        fish.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(CopperPufferfish.index(fish.fish()) == 1, "Axe did not remove wax first");
        fish.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(CopperPufferfish.index(fish.fish()) == 0 && player.getMainHandItem().getDamageValue() == 2, "Axe did not scrape oxidation");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialStingsUseTheirMatchingPassiveEffects(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2));
        target.setNoAi(true);
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(level);
            fish.setNoAi(true);
            fish.setPuffState(2);
            fish.setPos(target.position());
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            target.removeAllEffects();
            fish.aiStep();
            var expected = PufferfishCombat.passiveEffect(fish.fish());
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Inflated material fish did not sting nearby mobs");
            if (expected == null) helper.assertTrue(!target.hasEffect(MobEffects.POISON), "Waxed fish poisoned a mob");
            else {
                var actual = target.getEffect(expected.getEffect());
                int amplifier = PufferfishItems.isRadiant(fish.fish()) ? 0 : expected.getAmplifier();
                helper.assertTrue(PufferfishItems.isRadiant(fish.fish()) && actual == null
                                || actual != null && actual.getAmplifier() == amplifier && actual.getDuration() == expected.getDuration(),
                        "Material fish applied the wrong attack status effect");
            }
            if (PufferfishItems.isRadiant(fish.fish())) helper.assertTrue(target.hasEffect(MobEffects.GLOWING) && !target.hasEffect(MobEffects.POISON), "Radiant sting lost glowing or applied poison");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialFishDropTheirCurrentVariant(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(level);
            fish.setPos(pos.getX(), pos.getY(), pos.getZ());
            fish.oxidize();
            level.addFreshEntity(fish);
            fish.hurt(level.damageSources().genericKill(), 100);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(fish.fish().getItem())), "Material fish dropped the wrong variant");
            drops.forEach(ItemEntity::discard);
            fish.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void naturalSpawnsAndDyedEggsCoverAllVariants(GameTestHelper helper) {
        var level = helper.getLevel();
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.WARM_OCEAN);
        var spawns = biome.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap();
        for (var entry : PufferfishMobs.VARIANTS.entrySet()) {
            helper.assertTrue(spawns.stream().anyMatch(spawn -> spawn.type == entry.getValue().entity().get()) == !entry.getKey().startsWith("waxed_"),
                    "Natural spawn list should include uncoated material fish only");
        }
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var color : DyeColor.values()) {
            var fish = EntityType.PUFFERFISH.spawn(level, PufferfishMobs.dyedEgg(color), null, pos, MobSpawnType.SPAWN_EGG, false, false);
            helper.assertTrue(PufferfishColors.get(fish) == color, "Dyed spawn egg lost its color");
            fish.discard();
        }
        var colors = new java.util.HashSet<DyeColor>();
        var random = net.minecraft.util.RandomSource.create(741);
        for (int i = 0; i < 512; i++) {
            var fish = EntityType.PUFFERFISH.create(level);
            fish.getRandom().setSeed(random.nextLong());
            PufferfishMobs.naturalColors(new FinalizeSpawnEvent(fish, level, pos.getX(), pos.getY(), pos.getZ(),
                    level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null));
            if (PufferfishColors.get(fish) != null) colors.add(PufferfishColors.get(fish));
        }
        helper.assertTrue(colors.size() == 16, "Natural spawning did not cover all dye colors");
        helper.succeed();
    }
}
