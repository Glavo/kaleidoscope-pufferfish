// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/// Operator commands for the selected players' main-hand cultivation data.
public final class PufferfishCommands {
    private static final DynamicCommandExceptionType UNSUPPORTED_ITEM = error("unsupported_item");
    private static final DynamicCommandExceptionType INVALID_BUCKET = error("invalid_bucket");
    private static final DynamicCommandExceptionType STACKED_ITEM = error("stacked_item");
    private static final DynamicCommandExceptionType UNAWAKENED_EXPERIENCE = error("unawakened_experience");

    private PufferfishCommands() {}

    private static DynamicCommandExceptionType error(String name) {
        return new DynamicCommandExceptionType(player -> Component.translatable("commands.pufferfish_please." + name, player));
    }

    public static void register(RegisterCommandsEvent event) {
        var set = Commands.argument("targets", EntityArgument.players());
        var query = Commands.argument("target", EntityArgument.player());
        for (var property : Property.values()) {
            var value = property == Property.LEVEL
                    ? Commands.argument("value", IntegerArgumentType.integer(0, 10))
                    : Commands.argument("value", LongArgumentType.longArg(0));
            if (property == Property.LEVEL) value.suggests((context, builder) -> {
                for (var level : PufferfishLevel.values()) {
                    String number = Integer.toString(level.number());
                    if (number.startsWith(builder.getRemaining())) builder.suggest(number, level.displayName());
                }
                return builder.buildFuture();
            });
            set.then(Commands.literal(property.key).then(value.executes(context -> set(context.getSource(),
                    EntityArgument.getPlayers(context, "targets"), property, context.getArgument("value", Number.class).longValue()))));
            query.then(Commands.literal(property.key).executes(context -> query(context.getSource(),
                    EntityArgument.getPlayer(context, "target"), property)));
        }
        event.getDispatcher().register(Commands.literal("pufferfish")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(set))
                .then(Commands.literal("query").then(query)));
    }

    private static int query(CommandSourceStack source, ServerPlayer player, Property property) throws CommandSyntaxException {
        var held = HeldFish.read(player);
        var cultivation = held.fish.get(PufferfishPlease.CULTIVATION.get());
        long value = cultivation == null ? 0 : property == Property.LEVEL ? cultivation.level().number() : cultivation.experience();
        source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.query." + property.key,
                player.getDisplayName(), value, property.display(value)), false);
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static int set(CommandSourceStack source, Collection<ServerPlayer> players, Property property, long value) throws CommandSyntaxException {
        var edits = new ArrayList<HeldFish>();
        for (var player : players) {
            var held = HeldFish.read(player);
            if (held.item.getCount() != 1) throw STACKED_ITEM.create(player.getDisplayName());
            var previous = held.fish.get(PufferfishPlease.CULTIVATION.get());
            if (property == Property.LEVEL) {
                var level = PufferfishLevel.values()[(int) value];
                if (level == PufferfishLevel.LEVEL_0) {
                    held.fish.remove(PufferfishPlease.CULTIVATION.get());
                } else {
                    held.fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(level,
                            previous == null ? 0.5 : previous.talent(), previous == null ? 0 : previous.experience(),
                            previous == null ? 0 : previous.totalExperience()));
                }
                PufferfishDurability.activate(held.fish);
            } else {
                if (previous == null) throw UNAWAKENED_EXPERIENCE.create(player.getDisplayName());
                held.fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(
                        previous.level(), previous.talent(), value, previous.totalExperience()));
            }
            edits.add(held);
        }
        // Validate every target before changing any inventory.
        edits.forEach(HeldFish::apply);
        if (players.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.set." + property.key + ".single",
                    players.iterator().next().getDisplayName(), property.display(value)), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.set." + property.key + ".multiple",
                    players.size(), property.display(value)), true);
        }
        return players.size();
    }

    private enum Property {
        LEVEL("level"), EXPERIENCE("experience");

        private final String key;

        Property(String key) { this.key = key; }

        Component display(long value) {
            return this == LEVEL ? PufferfishLevel.values()[(int) value].displayName() : Component.literal(Long.toString(value));
        }
    }

    private record HeldFish(ServerPlayer player, ItemStack item, ItemStack fish) {
        static HeldFish read(ServerPlayer player) throws CommandSyntaxException {
            var item = player.getMainHandItem().copy();
            var variant = PufferfishVariant.of(item);
            if (item.isEmpty() || variant == null || item.getItem() instanceof SpawnEggItem) throw UNSUPPORTED_ITEM.create(player.getDisplayName());
            var fish = item;
            if (item.getItem() instanceof MobBucketItem) {
                var data = item.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
                fish = data.contains("Fish") ? ItemStack.parse(player.registryAccess(), data.get("Fish")).orElse(ItemStack.EMPTY) : variant.fish();
                if (fish.isEmpty() || !fish.is(variant.fishItem()) || fish.getCount() != 1) throw INVALID_BUCKET.create(player.getDisplayName());
            }
            return new HeldFish(player, item, fish);
        }

        void apply() {
            if (item.getItem() instanceof MobBucketItem) {
                var saved = fish.save(player.registryAccess());
                CustomData.update(DataComponents.BUCKET_ENTITY_DATA, item, data -> data.put("Fish", saved));
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            player.containerMenu.broadcastChanges();
        }
    }
}
