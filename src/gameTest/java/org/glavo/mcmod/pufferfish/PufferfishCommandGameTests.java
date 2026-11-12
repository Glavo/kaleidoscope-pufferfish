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
            execute(source, "pufferfish set @s level 10");
            helper.assertTrue(player.getMainHandItem().get(PufferfishPlease.CULTIVATION.get()).experience() == 123,
                    "Changing rank reset current experience");
            execute(source, "pufferfish set @s experience 20000000");
            var result = player.getMainHandItem();
            helper.assertTrue(result.get(PufferfishPlease.CULTIVATION.get()).equals(
                    new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.75, 20_000_000, 456)), "Rank edit lost talent or lifetime experience: " + item);
            var withoutRank = result.copy();
            withoutRank.set(PufferfishPlease.CULTIVATION.get(), before.get(PufferfishPlease.CULTIVATION.get()));
            helper.assertTrue(ItemStack.matches(before, withoutRank), "Rank edit changed unrelated item data: " + item);
            execute(source, "pufferfish set @s level 4");
            helper.assertTrue(player.getMainHandItem().get(PufferfishPlease.CULTIVATION.get()).equals(
                    new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.75, 20_000_000, 456)), "Rank editing changed other cultivation fields");
            helper.assertTrue(execute(source, "pufferfish query @s level") == 4
                    && execute(source, "pufferfish query @s experience") == 20_000_000, "Query returned incorrect values");
            execute(source, "pufferfish set @s level 0");
            helper.assertTrue(!player.getMainHandItem().has(PufferfishPlease.CULTIVATION.get()), "Rank zero retained cultivation data");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cultivation_commands")
    public static void queryingIsReadOnlyAndEditingRequiresOneItem(GameTestHelper helper) throws CommandSyntaxException {
        var player = TestPlayers.serverPlayer(helper);
        var source = player.createCommandSourceStack().withPermission(2);
        var original = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 64), DyeColor.BLUE);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
        helper.assertTrue(execute(source, "pufferfish query @s level") == 0
                && execute(source, "pufferfish query @s experience") == 0, "Fresh fish query was not zero");
        helper.assertTrue(ItemStack.matches(original, player.getMainHandItem()), "Query activated or split fresh fish");
        reject(helper, source, "pufferfish set @s level 1");
        helper.assertTrue(ItemStack.matches(original, player.getMainHandItem()), "Editing a stack split or mutated it");
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copyWithCount(1));
        execute(source, "pufferfish set @s level 1");
        var result = player.getMainHandItem();
        helper.assertTrue(result.getCount() == 1 && result.isDamageableItem() && result.getMaxStackSize() == 1
                        && PufferfishColors.get(result) == DyeColor.BLUE
                        && result.get(PufferfishPlease.CULTIVATION.get()).equals(new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.5, 0, 0)),
                "Fresh fish was not activated as a single colored individual");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        execute(source, "pufferfish set @s level 0");
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
                helper.assertTrue(execute(source, "pufferfish query @s level") == (captured ? 2 : 0), "Bucket query ignored the stored fish");
                helper.assertTrue(ItemStack.matches(bucket, player.getMainHandItem()), "Bucket query changed entity data");
                execute(source, "pufferfish set @s level 4");
                execute(source, "pufferfish set @s experience 4000");
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
            reject(helper, source, "pufferfish set @s level 1");
            helper.assertTrue(ItemStack.matches(stack, player.getMainHandItem()), "Rejected item was mutated");
        }
        var original = new ItemStack(Items.PUFFERFISH, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
        reject(helper, source.withPermission(0), "pufferfish set @s level 1");
        reject(helper, source.withPermission(0), "pufferfish query @s level");
        for (String args : List.of("level -1", "level 11", "experience -1")) reject(helper, source, "pufferfish set @s " + args);
        helper.assertTrue(ItemStack.matches(original, player.getMainHandItem()), "Rejected arguments or permissions mutated the held stack");
        player.setItemInHand(InteractionHand.MAIN_HAND, original.copyWithCount(1));
        reject(helper, source, "pufferfish set @s experience 1");
        helper.assertTrue(ItemStack.matches(original.copyWithCount(1), player.getMainHandItem()), "Setting experience awakened fresh fish");
        var bucket = new ItemStack(Items.PUFFERFISH_BUCKET);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.put("Fish", new ItemStack(Items.STONE).save(player.registryAccess())));
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket.copy());
        reject(helper, source, "pufferfish set @s level 1");
        helper.assertTrue(ItemStack.matches(bucket, player.getMainHandItem()), "Invalid bucket data was overwritten");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "cultivation_commands")
    public static void selectorsConsoleAndCompletionFollowCommandConventions(GameTestHelper helper) throws CommandSyntaxException {
        var first = TestPlayers.serverPlayer(helper);
        var second = TestPlayers.serverPlayer(helper);
        var server = helper.getLevel().getServer();
        var level = helper.getLevel();
        var position = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)).getCenter();
        var source = server.createCommandSourceStack().withLevel(level).withPosition(position).withPermission(2);
        String tag = "pufferfish_command_fixture";
        first.addTag(tag);
        second.addTag(tag);
        first.setPos(position);
        second.setPos(position.add(1, 0, 0));
        String targets = "@a[tag=" + tag + ",distance=..16]";
        String target = "@a[tag=" + tag + ",distance=..16,sort=nearest,limit=1]";
        first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        second.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        var original = first.getMainHandItem().copy();
        level.addNewPlayer(first);
        level.addNewPlayer(second);
        try {
            reject(helper, source, "pufferfish set " + targets + " level 5");
            helper.assertTrue(ItemStack.matches(original, first.getMainHandItem()), "Batch failure partially edited an inventory");
            second.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
            helper.assertTrue(execute(source, "pufferfish set " + targets + " level 5") == 2, "Batch result did not count changed targets");
            for (var player : List.of(first, second)) {
                helper.assertTrue(player.getMainHandItem().get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_5,
                        "Console selection did not edit every target");
            }
            reject(helper, source, "pufferfish query " + targets + " level");
            helper.assertTrue(execute(source, "pufferfish query " + target + " level") == 5, "Console query did not resolve the requested player");
            execute(source, "pufferfish set " + target + " experience 9223372036854775807");
            helper.assertTrue(execute(source, "pufferfish query " + target + " experience") == Integer.MAX_VALUE,
                    "Large experience query overflowed its command result");
            var dispatcher = server.getCommands().getDispatcher();
            var playerSource = first.createCommandSourceStack().withPermission(2);
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("pufferfish set @s level ", playerSource)).join().getList();
            helper.assertTrue(suggestions.stream().anyMatch(suggestion -> suggestion.getText().equals("10") && suggestion.getTooltip() != null),
                    "Rank completion omitted the highest rank or its display name");
            var properties = dispatcher.getCompletionSuggestions(dispatcher.parse("pufferfish set @s ", playerSource)).join().getList();
            helper.assertTrue(properties.stream().anyMatch(suggestion -> suggestion.getText().equals("level"))
                    && properties.stream().anyMatch(suggestion -> suggestion.getText().equals("experience")), "Property names did not complete");
        } finally {
            level.removePlayerImmediately(first, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            level.removePlayerImmediately(second, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }

    private static int execute(CommandSourceStack source, String command) throws CommandSyntaxException {
        return source.getServer().getCommands().getDispatcher().execute(command, source);
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
