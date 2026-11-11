// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishFeedingGameTests {
    @GameTest(template = "empty")
    public static void rawFishHealsEveryVariantAndPreservesItsIdentity(GameTestHelper helper) {
        var player = player(helper);
        for (var variant : PufferfishVariant.ALL) {
            var fish = variant.material() == PufferfishMaterial.NORMAL ? EntityType.PUFFERFISH.create(helper.getLevel())
                    : PufferfishMobs.VARIANTS.get(variant.fishName()).entity().get().create(helper.getLevel());
            if (variant.color() != null) PufferfishColors.set(fish, variant.color());
            fish.setHealth(1);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SALMON, 2));
            helper.assertTrue(interact(player, fish).isCanceled(), "Fish rejected raw salmon: " + variant.fishName());
            helper.assertTrue(fish.getHealth() == Math.min(3, fish.getMaxHealth()) && player.getMainHandItem().getCount() == 1,
                    "Feeding did not heal by nutrition or consume exactly one fish");
            helper.assertTrue(fish.isPersistenceRequired(), "Fed fish could despawn");
            helper.assertTrue(PufferfishColors.get(fish) == variant.color(), "Feeding changed the fish color");
            fish.setHealth(fish.getMaxHealth() - 0.5F);
            interact(player, fish);
            helper.assertTrue(fish.getHealth() == fish.getMaxHealth() && player.getMainHandItem().isEmpty(), "Healing did not clamp to maximum health");
            fish.discard();
        }
        var fish = PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get().create(helper.getLevel());
        for (var id : new String[] {"pufferfish_please:pufferfish_sashimi", "kaleidoscope_cookery:sashimi", "kaleidoscope_chinesefood:yellow_croaker"}) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item == Items.AIR) continue;
            var food = new ItemStack(item);
            helper.assertTrue(food.is(PufferfishMobs.FOOD), "Compatible raw fish is missing from the feeding tag: " + id);
            float healing = food.getFoodProperties(fish).nutrition();
            fish.setHealth(1);
            player.setItemInHand(InteractionHand.MAIN_HAND, food);
            helper.assertTrue(interact(player, fish).isCanceled() && fish.getHealth() == Math.min(1 + healing, fish.getMaxHealth()),
                    "Compatible raw fish did not heal: " + id);
        }
        fish.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void feedingRejectsFullHealthInvalidFoodAndRestrictedPlayers(GameTestHelper helper) {
        var player = player(helper);
        var fish = EntityType.PUFFERFISH.create(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 2));
        helper.assertTrue(!interact(player, fish).isCanceled() && player.getMainHandItem().getCount() == 2, "Full-health fish consumed food");
        fish.setHealth(1);
        for (var item : new net.minecraft.world.item.Item[] {Items.BREAD, Items.COOKED_COD, Items.STICK, Items.WATER_BUCKET}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            helper.assertTrue(!interact(player, fish).isCanceled() && player.getMainHandItem().is(item) && fish.getHealth() == 1,
                    "Feeding accepted an unrelated item or intercepted bucket capture");
        }
        var emptyFood = new ItemStack(Items.COD);
        emptyFood.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(0).build());
        player.setItemInHand(InteractionHand.MAIN_HAND, emptyFood);
        helper.assertTrue(!interact(player, fish).isCanceled(), "Food without nutrition was consumed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COD, 2));
        for (var mode : new GameType[] {GameType.SPECTATOR, GameType.ADVENTURE}) {
            player.setGameMode(mode);
            helper.assertTrue(!interact(player, fish).isCanceled() && fish.getHealth() == 1 && player.getMainHandItem().getCount() == 2,
                    "Restricted player fed a fish");
        }
        player.setGameMode(GameType.SURVIVAL);
        fish.setHealth(0);
        helper.assertTrue(!interact(player, fish).isCanceled() && player.getMainHandItem().getCount() == 2, "Feeding revived a dead fish");
        fish.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void feedingReturnsContainersAndRespectsCreativeMode(GameTestHelper helper) {
        var player = player(helper);
        var fish = EntityType.PUFFERFISH.create(helper.getLevel());
        var food = new ItemStack(Items.COD);
        food.set(DataComponents.FOOD, new FoodProperties.Builder().nutrition(2).usingConvertsTo(Items.BOWL).build());
        fish.setHealth(1);
        player.setItemInHand(InteractionHand.OFF_HAND, food.copy());
        var event = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.OFF_HAND, fish));
        helper.assertTrue(event.isCanceled() && fish.getHealth() == 3 && player.getOffhandItem().is(Items.BOWL), "Offhand feeding lost its container");
        fish.setHealth(1);
        player.setItemInHand(InteractionHand.MAIN_HAND, food.copyWithCount(2));
        interact(player, fish);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && player.getInventory().countItem(Items.BOWL) == 2,
                "Stacked food did not return one container to the inventory");
        for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 64));
        player.setItemInHand(InteractionHand.MAIN_HAND, food.copyWithCount(2));
        fish.setHealth(1);
        interact(player, fish);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4), entity -> entity.getItem().is(Items.BOWL));
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                "Full inventory lost or duplicated the food container");
        drops.forEach(Entity::discard);
        player.getInventory().clearContent();
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, food.copy());
        fish.setHealth(1);
        interact(player, fish);
        helper.assertTrue(fish.getHealth() == 3 && ItemStack.matches(player.getMainHandItem(), food) && player.getInventory().countItem(Items.BOWL) == 0,
                "Creative feeding consumed food or created a container");
        fish.discard();
        helper.succeed();
    }

    private static PlayerInteractEvent.EntityInteract interact(ServerPlayer player, Entity target) {
        return NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "feeding-test"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        return player;
    }
}
