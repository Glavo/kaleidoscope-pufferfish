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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/// Operator commands for live pufferfish or players' main-hand cultivation data.
public final class PufferfishCommands {
    private static final DynamicCommandExceptionType UNSUPPORTED_ITEM = error("unsupported_item");
    private static final DynamicCommandExceptionType UNSUPPORTED_ENTITY = error("unsupported_entity");
    private static final DynamicCommandExceptionType INVALID_BUCKET = error("invalid_bucket");
    private static final DynamicCommandExceptionType STACKED_ITEM = error("stacked_item");
    private static final DynamicCommandExceptionType UNAWAKENED_EXPERIENCE = error("unawakened_experience");
    private static final DynamicCommandExceptionType RANK_ONLY = error("rank_only");

    private PufferfishCommands() {}

    private static DynamicCommandExceptionType error(String name) {
        return new DynamicCommandExceptionType(player -> Component.translatable("commands.pufferfish_please." + name, player));
    }

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("pufferfish").requires(source -> source.hasPermission(2));
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
            root.then(Commands.literal(property.key)
                    .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.entities())
                            .then(value.executes(context -> set(context.getSource(), EntityArgument.getEntities(context, "targets"),
                                    property, context.getArgument("value", Number.class).longValue())))))
                    .then(Commands.literal("query").then(Commands.argument("target", EntityArgument.entity())
                            .executes(context -> query(context.getSource(), EntityArgument.getEntity(context, "target"), property)))));
        }
        event.getDispatcher().register(root);
    }

    private static int query(CommandSourceStack source, Entity entity, Property property) throws CommandSyntaxException {
        var target = Target.read(entity);
        var cultivation = target.fish.get(PufferfishPlease.CULTIVATION.get());
        long value = cultivation == null ? 0 : property == Property.LEVEL ? cultivation.level().number() : cultivation.experience();
        if (PufferfishSkinBlock.isSkin(target.fish)) {
            if (property == Property.EXPERIENCE) throw RANK_ONLY.create(target.description());
            value = target.fish.getOrDefault(PufferfishPlease.SKIN_LEVEL.get(), cultivation == null ? PufferfishLevel.LEVEL_0 : cultivation.level()).number();
        }
        long result = value;
        source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.query." + property.key,
                target.description(), result, property.display(result)), false);
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static int set(CommandSourceStack source, Collection<? extends Entity> entities, Property property, long value) throws CommandSyntaxException {
        var edits = new ArrayList<Target>();
        for (var entity : entities) {
            var target = Target.read(entity);
            if (target.item.getCount() != 1) throw STACKED_ITEM.create(entity.getDisplayName());
            if (PufferfishSkinBlock.isSkin(target.fish)) {
                if (property == Property.EXPERIENCE) throw RANK_ONLY.create(target.description());
                PufferfishSkinBlock.setLevel(target.fish, PufferfishLevel.values()[(int) value]);
                edits.add(target);
                continue;
            }
            var previous = target.fish.get(PufferfishPlease.CULTIVATION.get());
            if (property == Property.LEVEL) {
                var level = PufferfishLevel.values()[(int) value];
                if (level == PufferfishLevel.LEVEL_0) {
                    target.fish.remove(PufferfishPlease.CULTIVATION.get());
                } else {
                    target.fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(level,
                            previous == null ? 0.5 : previous.talent(), previous == null ? 0 : previous.experience(),
                            previous == null ? 0 : previous.totalExperience()));
                }
                PufferfishDurability.activate(target.fish);
            } else {
                if (previous == null) throw UNAWAKENED_EXPERIENCE.create(target.description());
                target.fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(
                        previous.level(), previous.talent(), value, previous.totalExperience()));
            }
            edits.add(target);
        }
        // Validate every target before changing any item or entity.
        edits.forEach(Target::apply);
        if (edits.size() == 1) {
            source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.set." + property.key + ".single",
                    edits.getFirst().description(), property.display(value)), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.pufferfish_please.set." + property.key + ".multiple",
                    edits.size(), property.display(value)), true);
        }
        return edits.size();
    }

    private enum Property {
        LEVEL("level"), EXPERIENCE("experience");

        private final String key;

        Property(String key) { this.key = key; }

        Component display(long value) {
            return this == LEVEL ? PufferfishLevel.values()[(int) value].displayName() : Component.literal(Long.toString(value));
        }
    }

    private record Target(Entity entity, ItemStack item, ItemStack fish) {
        static Target read(Entity entity) throws CommandSyntaxException {
            if (entity instanceof Pufferfish live) {
                var fish = PufferfishData.copy(live);
                return new Target(live, fish, fish);
            }
            if (!(entity instanceof ServerPlayer player)) throw UNSUPPORTED_ENTITY.create(entity.getDisplayName());
            var item = player.getMainHandItem().copy();
            var variant = PufferfishVariant.of(item);
            if (item.isEmpty() || variant == null || item.getItem() instanceof SpawnEggItem) throw UNSUPPORTED_ITEM.create(player.getDisplayName());
            var fish = item;
            if (item.getItem() instanceof MobBucketItem) {
                var data = item.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
                fish = data.contains("Fish") ? ItemStack.parse(player.registryAccess(), data.get("Fish")).orElse(ItemStack.EMPTY) : variant.fish();
                if (fish.isEmpty() || !fish.is(variant.fishItem()) || fish.getCount() != 1) throw INVALID_BUCKET.create(player.getDisplayName());
            }
            return new Target(player, item, fish);
        }

        Component description() {
            return entity instanceof ServerPlayer
                    ? Component.translatable("commands.pufferfish_please.target.held", entity.getDisplayName(), item.getHoverName())
                    : Component.translatable("commands.pufferfish_please.target.entity", entity.getDisplayName());
        }

        void apply() {
            if (entity instanceof Pufferfish live) {
                PufferfishData.set(live, fish);
                return;
            }
            var player = (ServerPlayer) entity;
            if (item.getItem() instanceof MobBucketItem) {
                var saved = fish.save(player.registryAccess());
                CustomData.update(DataComponents.BUCKET_ENTITY_DATA, item, data -> data.put("Fish", saved));
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            player.containerMenu.broadcastChanges();
        }
    }
}
