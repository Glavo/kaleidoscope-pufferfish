// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.worldgen.PufferfishRuinPiece;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishAdvancementGameTests {
    @GameTest(template = "empty")
    public static void collectionEquipmentEffectsAndBoatsUnlockTheirAdvancements(GameTestHelper helper) {
        var player = player(helper);
        var level = helper.getLevel();
        for (String id : List.of("root", "careful_with_spikes", "wax_on", "ocean_visitor", "unfriendly_fish",
                "pufferfish_voyage", "pufferfish_blessing", "pufferfish_curse", "restored_glory", "radiant_promise")) {
            helper.assertTrue(advancement(player, id) != null && advancement(player, id).value().display().isPresent(), "Missing visible advancement: " + id);
        }
        helper.assertTrue(advancement(player, "radiant_promise").value().display().orElseThrow().getType() == AdvancementType.CHALLENGE,
                "Radiant advancement is not a challenge");
        var ordinary = new ItemStack(Items.PUFFERFISH);
        player.getInventory().add(ordinary.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), ordinary);
        helper.assertTrue(done(player, "root"), "Obtaining a pufferfish did not unlock the root");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        CriteriaTriggers.TICK.trigger(player);
        helper.assertTrue(!done(player, "careful_with_spikes"), "Ordinary armor granted the pufferfish headgear advancement");
        player.setItemSlot(EquipmentSlot.HEAD, PufferfishItems.COPPER.toStack());
        CriteriaTriggers.TICK.trigger(player);
        helper.assertTrue(done(player, "careful_with_spikes"), "Wearing a material pufferfish did not grant progress");
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_CURSE, 260));
        helper.assertTrue(done(player, "pufferfish_blessing") && done(player, "pufferfish_curse"), "Effects did not grant their advancements");
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), PufferfishItems.RADIANT.toStack());
        helper.assertTrue(done(player, "radiant_promise"), "Radiant pufferfish did not grant its challenge");
        BuiltInRegistries.ITEM.getOptional(PufferfishPlease.id("pufferfish_aspic")).ifPresent(item -> {
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), new ItemStack(item));
            helper.assertTrue(done(player, "fish_aspic"), "Aspic did not grant its advancement");
        });
        var vanillaBoat = EntityType.BOAT.create(level);
        player.startRiding(vanillaBoat, true);
        CriteriaTriggers.START_RIDING_TRIGGER.trigger(player);
        helper.assertTrue(!done(player, "pufferfish_voyage"), "Vanilla boat granted the pufferfish voyage");
        player.stopRiding();
        for (var variant : PufferfishBoats.VARIANTS.values()) {
            var boat = variant.entity().get().create(level);
            player.startRiding(boat, true);
            CriteriaTriggers.START_RIDING_TRIGGER.trigger(player);
            helper.assertTrue(done(player, "pufferfish_voyage"), "Missing boat advancement variant: " + variant.entity().getId());
            player.stopRiding();
            player.getAdvancements().revoke(advancement(player, "pufferfish_voyage"), "ride");
        }
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waxingRequiresAnActionAndSupportsCraftingBlocksAndLivingFish(GameTestHelper helper) {
        var player = player(helper);
        var level = helper.getLevel();
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), PufferfishItems.COPPER_VARIANTS.get(4).toStack());
        helper.assertTrue(!done(player, "wax_on"), "Merely obtaining a waxed fish granted waxing progress");
        CriteriaTriggers.RECIPE_CRAFTED.trigger(player, PufferfishPlease.id("wax_copper_pufferfish"),
                List.of(PufferfishItems.COPPER.toStack(), new ItemStack(Items.HONEYCOMB)));
        helper.assertTrue(done(player, "wax_on"), "Wax crafting did not grant progress");
        player.getAdvancements().revoke(advancement(player, "wax_on"), "craft");
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        level.setBlockAndUpdate(pos, PufferfishBlocks.PLACED_FISH.get().defaultBlockState());
        ((PlacedPufferfishBlockEntity) level.getBlockEntity(pos)).setFish(PufferfishItems.COPPER.toStack());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
        CopperPufferfish.interact(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(pos.getCenter(), Direction.UP, pos, false)));
        helper.assertTrue(done(player, "wax_on"), "Waxing a placed fish did not grant progress");
        player.getAdvancements().revoke(advancement(player, "wax_on"), "wax");
        var fish = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
        fish.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(done(player, "wax_on"), "Waxing a living fish did not grant progress");
        level.removeBlock(pos, false);
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void discoveryRequiresEnteringTheMatchingRuin(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        for (boolean ancient : new boolean[] {false, true}) {
            var floor = helper.absolutePos(new BlockPos(2, ancient ? 50 : 30, 2));
            String id = ancient ? "unfriendly_fish" : "ocean_visitor";
            var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .get(PufferfishPlease.id(ancient ? "ancient_pufferfish_ruin" : "pufferfish_ruin"));
            var chunk = level.getChunkAt(floor);
            var start = new StructureStart(structure, new ChunkPos(floor), 0,
                    new PiecesContainer(List.of(new PufferfishRuinPiece(floor, ancient, 74392L))));
            level.structureManager().setStartForStructure(SectionPos.of(floor), structure, start, chunk);
            level.structureManager().addReferenceForStructure(SectionPos.of(floor), structure, new ChunkPos(floor).toLong(), chunk);
            player.setPos(floor.above(12).getCenter());
            CriteriaTriggers.LOCATION.trigger(player);
            helper.assertTrue(!done(player, id), "Standing outside a ruin granted discovery");
            player.setPos(floor.above(4).getCenter());
            CriteriaTriggers.LOCATION.trigger(player);
            helper.assertTrue(done(player, id), "Entering a ruin did not grant discovery");
            level.structureManager().setStartForStructure(SectionPos.of(floor), structure, StructureStart.INVALID_START, chunk);
        }
        player.getAdvancements().stopListening();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void restorationRequiresPersonallyCompletingAnUncanceledFrame(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        var observer = player(helper);
        var center = helper.absolutePos(new BlockPos(2, 100, 2));
        for (var offset : BlockPos.betweenClosed(-2, -2, -2, 2, 2, 2)) {
            int x = Math.abs(offset.getX()), y = Math.abs(offset.getY()), z = Math.abs(offset.getZ());
            boolean frame = x == 0 && (y == 2 || z == 2) || y == 0 && (x == 2 || z == 2) || z == 0 && (x == 2 || y == 2);
            level.setBlock(center.offset(offset), (frame ? Blocks.PRISMARINE : Blocks.WATER).defaultBlockState(), 2);
        }
        level.setBlockAndUpdate(center, PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState());
        var last = center.east(2);
        var previous = BlockSnapshot.create(level.dimension(), level, last);
        level.setBlockAndUpdate(last, Blocks.PRISMARINE_BRICKS.defaultBlockState());
        NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(previous, Blocks.PRISMARINE.defaultBlockState(), player));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!done(player, "restored_glory") && !done(observer, "restored_glory"),
                    "An already complete frame or a nearby observer earned restoration");
            level.setBlockAndUpdate(last, Blocks.WATER.defaultBlockState());
            var snapshot = BlockSnapshot.create(level.dimension(), level, last);
            level.setBlockAndUpdate(last, Blocks.PRISMARINE.defaultBlockState());
            var canceled = new BlockEvent.EntityPlaceEvent(snapshot, Blocks.PRISMARINE.defaultBlockState(), player);
            NeoForge.EVENT_BUS.post(canceled);
            canceled.setCanceled(true);
            level.setBlockAndUpdate(last, Blocks.WATER.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!done(player, "restored_glory"), "Canceled placement granted restoration");
                var valid = BlockSnapshot.create(level.dimension(), level, last);
                level.setBlockAndUpdate(last, PufferfishBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
                NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(valid, Blocks.PRISMARINE.defaultBlockState(), player));
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(done(player, "restored_glory") && !done(observer, "restored_glory"),
                            "Restoration did not credit only the player who completed the frame");
                    for (var pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) level.removeBlock(pos, false);
                    player.getAdvancements().stopListening();
                    observer.getAdvancements().stopListening();
                    helper.succeed();
                });
            });
        });
    }

    private static AdvancementHolder advancement(ServerPlayer player, String id) {
        return player.server.getAdvancements().get(PufferfishPlease.id("adventure/" + id));
    }

    private static boolean done(ServerPlayer player, String id) {
        var advancement = advancement(player, id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "advancement-test"), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        return player;
    }
}
