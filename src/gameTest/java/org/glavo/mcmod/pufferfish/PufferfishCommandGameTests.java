// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCommandGameTests {
    @GameTest(template = "empty", batch = "cultivation_commands")
    public static void editsRanksWithoutChangingOtherItemData(GameTestHelper helper) throws CommandSyntaxException {
        var player = TestPlayers.serverPlayer(helper);
        var source = player.createCommandSourceStack().withPermission(2);
        for (var item : BuiltInRegistries.ITEM) {
            var stack = new ItemStack(item);
            if (PufferfishVariant.of(stack) == null || item instanceof MobBucketItem || item instanceof SpawnEggItem) continue;
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Command fixture"));
            stack.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.75, 123, 456));
            PufferfishDurability.activate(stack);
            if (stack.isDamageableItem()) stack.setDamageValue(7);
            var before = stack.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            execute(source, "pufferfish level 10 20000000");
            var result = player.getMainHandItem();
            helper.assertTrue(result.get(PufferfishPlease.CULTIVATION.get()).equals(
                    new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.75, 20_000_000, 456)), "Rank edit lost talent or lifetime experience: " + item);
            var withoutRank = result.copy();
            withoutRank.set(PufferfishPlease.CULTIVATION.get(), before.get(PufferfishPlease.CULTIVATION.get()));
            helper.assertTrue(ItemStack.matches(before, withoutRank), "Rank edit changed unrelated item data: " + item);
            execute(source, "pufferfish level 4");
            helper.assertTrue(player.getMainHandItem().get(PufferfishPlease.CULTIVATION.get()).equals(
                    new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.75, 0, 456)), "Omitted experience was not reset");
            execute(source, "pufferfish level 0");
            helper.assertTrue(!player.getMainHandItem().has(PufferfishPlease.CULTIVATION.get()), "Rank zero retained cultivation data");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cultivation_commands")
    public static void splitsOnlyOneItemAndActivatesFreshFish(GameTestHelper helper) throws CommandSyntaxException {
        var player = TestPlayers.serverPlayer(helper);
        var source = player.createCommandSourceStack().withPermission(2);
        var original = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 64), DyeColor.BLUE);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
        execute(source, "pufferfish level 1");
        var result = player.getMainHandItem();
        helper.assertTrue(result.getCount() == 1 && result.isDamageableItem() && result.getMaxStackSize() == 1
                        && PufferfishColors.get(result) == DyeColor.BLUE
                        && result.get(PufferfishPlease.CULTIVATION.get()).equals(new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.5, 0, 0)),
                "Fresh fish was not activated as a single colored individual");
        int remainder = 0;
        for (var stack : player.getInventory().items) {
            if (stack == result || stack.isEmpty()) continue;
            helper.assertTrue(ItemStack.isSameItemSameComponents(original, stack), "Splitting changed the remaining fish");
            remainder += stack.getCount();
        }
        helper.assertTrue(remainder == 63, "Rank editing lost or duplicated fish");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        execute(source, "pufferfish level 0");
        helper.assertTrue(player.getMainHandItem().isDamageableItem()
                && PufferfishCultivation.visibleName(player.getMainHandItem()) != null, "Rank zero did not activate fresh fish");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cultivation_commands")
    public static void bucketEditsSurviveReleaseAndPreserveCapturedData(GameTestHelper helper) throws CommandSyntaxException {
        var player = TestPlayers.serverPlayer(helper);
        var source = player.createCommandSourceStack().withPermission(2);
        for (var type : List.of(EntityType.PUFFERFISH, PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get())) {
            var fish = type.create(helper.getLevel());
            if (type == EntityType.PUFFERFISH) PufferfishColors.set(fish, DyeColor.BLUE);
            var bucket = fish.getBucketItemStack();
            for (boolean captured : new boolean[] {false, true}) {
                if (captured) {
                    var stored = PufferfishData.copy(fish);
                    stored.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.75, 123, 456));
                    PufferfishDurability.activate(stored);
                    stored.setDamageValue(7);
                    PufferfishData.set(fish, stored);
                    fish.setNoAi(true);
                    fish.saveToBucketTag(bucket);
                }
                var before = bucket.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
                player.setItemInHand(InteractionHand.MAIN_HAND, bucket.copy());
                execute(source, "pufferfish level 4 4000");
                var result = player.getMainHandItem();
                var data = result.get(DataComponents.BUCKET_ENTITY_DATA).copyTag();
                var released = type.create(helper.getLevel());
                released.loadFromBucketTag(data);
                var stored = PufferfishData.copy(released);
                helper.assertTrue(stored.get(PufferfishPlease.CULTIVATION.get()).equals(new PufferfishCultivation(
                                PufferfishLevel.LEVEL_4, captured ? 0.75 : 0.5, 4000, captured ? 456 : 0))
                                && Math.abs(released.getScale() - 1.2) < 0.0001,
                        "Bucket release lost the requested rank or advanced it automatically");
                if (captured) helper.assertTrue(stored.getDamageValue() == 7 && released.isNoAi()
                                && (type != EntityType.PUFFERFISH || PufferfishColors.get(released) == DyeColor.BLUE),
                        "Bucket editing lost captured fish state");
                before.remove("Fish");
                data.remove("Fish");
                helper.assertTrue(before.equals(data), "Bucket editing changed unrelated entity data");
                released.discard();
            }
            fish.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cultivation_commands")
    public static void invalidCommandsLeaveTheInventoryUntouched(GameTestHelper helper) throws CommandSyntaxException {
        var player = TestPlayers.serverPlayer(helper);
        var source = player.createCommandSourceStack().withPermission(2);
        for (var stack : List.of(ItemStack.EMPTY, new ItemStack(Items.STONE), new ItemStack(Items.PUFFERFISH_SPAWN_EGG))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            reject(helper, source, "pufferfish level 1");
            helper.assertTrue(ItemStack.matches(stack, player.getMainHandItem()), "Rejected item was mutated");
        }
        var original = new ItemStack(Items.PUFFERFISH, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
        reject(helper, source.withPermission(0), "pufferfish level 1");
        for (String args : List.of("-1", "11", "1 -1", "0 1")) reject(helper, source, "pufferfish level " + args);
        helper.assertTrue(ItemStack.matches(original, player.getMainHandItem()), "Rejected arguments or permissions mutated the held stack");
        var bucket = new ItemStack(Items.PUFFERFISH_BUCKET);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.put("Fish", new ItemStack(Items.STONE).save(player.registryAccess())));
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket.copy());
        reject(helper, source, "pufferfish level 1");
        helper.assertTrue(ItemStack.matches(bucket, player.getMainHandItem()), "Invalid bucket data was overwritten");
        helper.succeed();
    }

    private static void execute(CommandSourceStack source, String command) throws CommandSyntaxException {
        source.getServer().getCommands().getDispatcher().execute(command, source);
    }

    private static void reject(GameTestHelper helper, CommandSourceStack source, String command) throws CommandSyntaxException {
        try {
            execute(source, command);
            helper.fail("Command should have been rejected: " + command);
        } catch (CommandSyntaxException expected) {
            // Rejection must occur before any item mutation.
        }
    }
}
