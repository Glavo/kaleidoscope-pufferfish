// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public class PufferfishGameTests {
    @GameTest(template = "empty")
    public static void bucketColorsAreReadyBeforeTracking(GameTestHelper helper) {
        List<DyeColor> colorsAtJoin = new ArrayList<>();
        Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof Pufferfish fish) {
                colorsAtJoin.add(((ColoredPufferfish) fish).kaleidoscope$getColor());
            }
        };
        NeoForge.EVENT_BUS.addListener(observer);
        try {
            for (DyeColor color : DyeColor.values()) {
                colorsAtJoin.clear();
                ItemStack bucket = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), color);
                BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
                ((MobBucketItem) bucket.getItem()).checkExtraContent(null, helper.getLevel(), bucket, pos);
                helper.getLevel().getEntitiesOfClass(Pufferfish.class, new AABB(pos).inflate(1)).forEach(Pufferfish::discard);
                helper.assertTrue(colorsAtJoin.size() == 1 && colorsAtJoin.getFirst() == color,
                        "Bucket color was not initialized before entity tracking: " + color);
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(observer);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allColorsSurviveSaveAndBucketRelease(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            Pufferfish fish = EntityType.PUFFERFISH.create(helper.getLevel());
            ((ColoredPufferfish) fish).kaleidoscope$setColor(color);
            fish.setCustomName(Component.literal("Keepsake"));
            fish.setHealth(2.0F);
            CompoundTag saved = new CompoundTag();
            fish.saveWithoutId(saved);
            Pufferfish restored = EntityType.PUFFERFISH.create(helper.getLevel());
            restored.getEntityData().assignValues(fish.getEntityData().getNonDefaultValues());
            helper.assertTrue(((ColoredPufferfish) restored).kaleidoscope$getColor() == color, "Entity color did not synchronize");
            restored.load(saved);
            helper.assertTrue(((ColoredPufferfish) restored).kaleidoscope$getColor() == color, "Entity color was not saved");
            ItemStack bucket = restored.getBucketItemStack();
            restored.saveToBucketTag(bucket);
            helper.assertTrue(PufferfishColors.get(bucket) == color, "Captured bucket lost its color");
            bucket = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) bucket.save(helper.getLevel().registryAccess()));
            helper.assertTrue(PufferfishColors.get(bucket) == color, "Item component did not survive serialization");
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            ((MobBucketItem) bucket.getItem()).checkExtraContent(null, helper.getLevel(), bucket, pos);
            var released = helper.getLevel().getEntitiesOfClass(Pufferfish.class, new AABB(pos).inflate(1));
            helper.assertTrue(released.size() == 1, "Bucket did not release exactly one fish");
            Pufferfish result = released.getFirst();
            helper.assertTrue(((ColoredPufferfish) result).kaleidoscope$getColor() == color, "Released fish lost its color");
            helper.assertTrue(result.fromBucket() && result.getHealth() == 2.0F, "Bucket entity data was damaged");
            helper.assertTrue(Component.literal("Keepsake").equals(result.getCustomName()), "Custom name was lost");
            result.discard();
        }
        Pufferfish plain = EntityType.PUFFERFISH.create(helper.getLevel());
        helper.assertTrue(((ColoredPufferfish) plain).kaleidoscope$getColor() == null, "Vanilla fish must remain undyed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesPreserveDataAndDoNotDuplicateBuckets(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (DyeColor color : DyeColor.values()) {
            for (var item : List.of(Items.PUFFERFISH, Items.PUFFERFISH_BUCKET)) {
                ItemStack original = new ItemStack(item, item == Items.PUFFERFISH ? 12 : 1);
                original.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                if (item == Items.PUFFERFISH_BUCKET) {
                    CustomData.update(DataComponents.BUCKET_ENTITY_DATA, original, tag -> tag.putFloat("Health", 2.0F));
                }
                PufferfishColors.dye(original, DyeColor.BLACK);
                CraftingInput input = CraftingInput.of(2, 1, List.of(original, new ItemStack(DyeItem.byColor(color))));
                var holder = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
                helper.assertTrue(holder.id().equals(KaleidoscopePufferfish.id("dye_pufferfish")), "Dye recipe was not selected");
                var recipe = holder.value();
                ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
                helper.assertTrue(result.is(item) && result.getCount() == 1 && PufferfishColors.get(result) == color, "Incorrect dye recipe output");
                helper.assertTrue(result.getHoverName().equals(original.getHoverName()), "Custom name was lost");
                helper.assertTrue(PufferfishColors.get(original) == DyeColor.BLACK, "Recipe mutated its input");
                helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Recipe duplicated a container");
                helper.assertTrue(Ingredient.of(item).test(result), "Vanilla ingredient compatibility was lost");
                if (item == Items.PUFFERFISH_BUCKET) {
                    var data = result.get(DataComponents.BUCKET_ENTITY_DATA).copyTag();
                    helper.assertTrue(data.getFloat("Health") == 2.0F, "Recipe lost bucket data");
                    helper.assertTrue(data.getString(PufferfishColors.COLOR_TAG).equals(color.getName()), "Bucket data color is stale");
                }
            }
        }
        var recipe = new PufferfishDyeRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        for (var input : List.of(
                List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(Items.SALMON)),
                List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(Items.PUFFERFISH), new ItemStack(Items.RED_DYE)),
                List.of(new ItemStack(Items.PUFFERFISH), new ItemStack(Items.RED_DYE), new ItemStack(Items.BLUE_DYE)),
                List.of(new ItemStack(Items.PUFFERFISH)))) {
            helper.assertTrue(!recipe.matches(CraftingInput.of(input.size(), 1, input), helper.getLevel()), "Invalid recipe accepted");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void interactionConsumesDyeOnlyWhenNeeded(GameTestHelper helper) {
        Pufferfish fish = EntityType.PUFFERFISH.create(helper.getLevel());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getAbilities().mayBuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE, 4));
        var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, fish);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.isCanceled() && ((ColoredPufferfish) fish).kaleidoscope$getColor() == DyeColor.RED, "Live dye interaction failed");
        helper.assertTrue(player.getMainHandItem().getCount() == 3 && fish.isPersistenceRequired(), "Survival dye cost or persistence is incorrect");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, fish));
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "Same-color dye was consumed");
        player.getAbilities().instabuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_DYE));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, fish));
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && ((ColoredPufferfish) fish).kaleidoscope$getColor() == DyeColor.BLUE, "Creative dye interaction failed");
        player.getAbilities().mayBuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, fish));
        helper.assertTrue(((ColoredPufferfish) fish).kaleidoscope$getColor() == DyeColor.BLUE, "Restricted player dyed a fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void deathDropsRetainEveryColor(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            Pufferfish fish = helper.spawn(EntityType.PUFFERFISH, new BlockPos(1, 1, 1));
            ((ColoredPufferfish) fish).kaleidoscope$setColor(color);
            fish.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, fish.getBoundingBox().inflate(2));
            var food = drops.stream().filter(drop -> drop.getItem().is(Items.PUFFERFISH)).toList();
            helper.assertTrue(food.size() == 1 && PufferfishColors.get(food.getFirst().getItem()) == color, "Death drop lost its color");
            drops.forEach(ItemEntity::discard);
            fish.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cookeryRecipesAcceptDyedIngredients(GameTestHelper helper) {
        if (net.neoforged.fml.ModList.get().isLoaded("kaleidoscope_cookery")) {
            for (String path : List.of("stockpot/pufferfish_soup", "flex_stockpot/pufferfish_soup")) {
                var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", path);
                var recipe = helper.getLevel().getRecipeManager().byKey(id).orElseThrow().value();
                for (DyeColor color : DyeColor.values()) {
                    var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color);
                    helper.assertTrue(recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(stack)), "Cookery rejected a dyed pufferfish");
                }
            }
        }
        helper.succeed();
    }
}
