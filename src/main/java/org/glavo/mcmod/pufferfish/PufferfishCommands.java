// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/// Operator-only editing of a single held item, including the fish stored inside a bucket.
public final class PufferfishCommands {
    private static final SimpleCommandExceptionType UNSUPPORTED_ITEM = error("unsupported_item");
    private static final SimpleCommandExceptionType INVALID_BUCKET = error("invalid_bucket");
    private static final SimpleCommandExceptionType UNAWAKENED_EXPERIENCE = error("unawakened_experience");

    private PufferfishCommands() {}

    private static SimpleCommandExceptionType error(String name) {
        return new SimpleCommandExceptionType(Component.translatable("commands.pufferfish_please." + name));
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pufferfish")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 10))
                                .executes(context -> setLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"), 0))
                                .then(Commands.argument("experience", LongArgumentType.longArg(0))
                                        .executes(context -> setLevel(context.getSource(), IntegerArgumentType.getInteger(context, "level"),
                                                LongArgumentType.getLong(context, "experience")))))));
    }

    private static int setLevel(CommandSourceStack source, int number, long experience) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var held = player.getMainHandItem();
        var variant = PufferfishVariant.of(held);
        if (held.isEmpty() || variant == null || held.getItem() instanceof SpawnEggItem) throw UNSUPPORTED_ITEM.create();
        var level = PufferfishLevel.values()[number];
        if (level == PufferfishLevel.LEVEL_0 && experience != 0) throw UNAWAKENED_EXPERIENCE.create();

        var result = held.copyWithCount(1);
        var target = result;
        boolean bucket = result.getItem() instanceof MobBucketItem;
        if (bucket) {
            var data = result.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
            target = data.contains("Fish") ? ItemStack.parse(player.registryAccess(), data.get("Fish")).orElse(ItemStack.EMPTY) : variant.fish();
            if (target.isEmpty() || !target.is(variant.fishItem()) || target.getCount() != 1) throw INVALID_BUCKET.create();
        }

        var previous = target.get(PufferfishPlease.CULTIVATION.get());
        if (level == PufferfishLevel.LEVEL_0) {
            target.remove(PufferfishPlease.CULTIVATION.get());
        } else {
            target.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(level,
                    previous == null ? 0.5 : previous.talent(), experience, previous == null ? 0 : previous.totalExperience()));
        }
        PufferfishDurability.activate(target);
        if (bucket) {
            var saved = target.save(player.registryAccess());
            CustomData.update(DataComponents.BUCKET_ENTITY_DATA, result, data -> data.put("Fish", saved));
        }

        var remainder = held.copyWithCount(held.getCount() - 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, result);
        if (!remainder.isEmpty()) {
            player.getInventory().add(remainder);
            if (!remainder.isEmpty()) player.drop(remainder, false);
        }
        player.containerMenu.broadcastChanges();
        var name = PufferfishCultivation.visibleName(target);
        var displayName = name == null ? level.displayName() : name;
        source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.level.success", result.getHoverName(), displayName), true);
        return 1;
    }
}
