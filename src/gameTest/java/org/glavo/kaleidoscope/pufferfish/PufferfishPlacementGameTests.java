// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishPlacementGameTests {
    public static List<ItemStack> variants() {
        var fish = new ArrayList<ItemStack>();
        fish.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) fish.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        for (var item : List.of(PufferfishItems.COPPER, PufferfishItems.IRON, PufferfishItems.DIAMOND,
                PufferfishItems.GOLDEN, PufferfishItems.NETHERITE, PufferfishItems.HEAVY)) fish.add(new ItemStack(item.get()));
        return fish;
    }

    private static PlayerInteractEvent.RightClickBlock use(Player player, BlockPos floor) {
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, floor,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    private static Player player(GameTestHelper helper, GameType mode) {
        var player = helper.makeMockPlayer(mode);
        mode.updatePlayerAbilities(player.getAbilities());
        player.setShiftKeyDown(true);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 2, 0))));
        player.setYRot(0);
        return player;
    }

    @GameTest(template = "empty")
    public static void allFishPreserveComponentsThroughPlacementSavingAndDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var floor = helper.absolutePos(new BlockPos(2, 1, 2));
        var pos = floor.above();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        var player = player(helper, GameType.SURVIVAL);
        for (var stack : variants()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Display fish"));
            stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.RESPIRATION), 3);
            stack.setCount(2);
            var expected = stack.copyWithCount(1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var event = use(player, floor);
            helper.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction() && stack.getCount() == 1,
                    "Sneak-use did not place exactly one fish");
            var placed = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(placed != null && ItemStack.matches(expected, placed.fish())
                    && placed.getBlockState().getValue(PlacedPufferfishBlock.FACING) == player.getDirection().getOpposite(),
                    "Placement lost fish components or did not face the player");
            var restored = new PlacedPufferfishBlockEntity(pos, placed.getBlockState());
            restored.loadWithComponents(placed.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            helper.assertTrue(ItemStack.matches(expected, restored.fish()), "Saved fish did not survive loading");
            restored.loadWithComponents(placed.getUpdateTag(level.registryAccess()), level.registryAccess());
            helper.assertTrue(ItemStack.matches(expected, restored.fish()), "Fish update packet lost components");
            helper.assertTrue(ItemStack.matches(expected, placed.getBlockState().getBlock().getCloneItemStack(level, pos, placed.getBlockState())),
                    "Pick-block did not return the original fish");
            level.destroyBlock(pos, true);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5));
            helper.assertTrue(drops.size() == 1 && ItemStack.matches(expected, drops.getFirst().getItem()), "Broken fish did not drop exactly its stored item");
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placementRespectsCancellationAndUseModes(GameTestHelper helper) {
        var level = helper.getLevel();
        var floor = helper.absolutePos(new BlockPos(2, 1, 2));
        var pos = floor.above();
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        var player = player(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishItems.GOLDEN.get(), 2));
        Consumer<BlockEvent.EntityPlaceEvent> deny = event -> { if (event.getPos().equals(pos)) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            helper.assertTrue(!use(player, floor).getCancellationResult().consumesAction() && level.isEmptyBlock(pos)
                    && player.getMainHandItem().getCount() == 2, "Denied placement changed the world or consumed a fish");
        } finally { NeoForge.EVENT_BUS.unregister(deny); }
        player.setShiftKeyDown(false);
        helper.assertTrue(!use(player, floor).isCanceled() && level.isEmptyBlock(pos), "Normal right-click was intercepted");
        for (var mode : List.of(GameType.ADVENTURE, GameType.SPECTATOR)) {
            var restricted = player(helper, mode);
            restricted.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
            use(restricted, floor);
            helper.assertTrue(level.isEmptyBlock(pos) && restricted.getMainHandItem().getCount() == 1, "Restricted player placed a fish");
        }
        var creative = player(helper, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PufferfishItems.GOLDEN.get()));
        helper.assertTrue(use(creative, floor).getCancellationResult().consumesAction() && creative.getMainHandItem().getCount() == 1,
                "Creative placement consumed a fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishCanFloatAndBeWaterloggedWithoutReplacingSolidBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var floor = helper.absolutePos(new BlockPos(2, 1, 2));
        var pos = floor.above();
        var player = player(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH, 3));
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!use(player, floor).getCancellationResult().consumesAction() && player.getMainHandItem().getCount() == 3,
                "Placement overwrote an occupied block");
        level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        helper.assertTrue(use(player, floor).getCancellationResult().consumesAction()
                && level.getBlockState(pos).getValue(PlacedPufferfishBlock.WATERLOGGED), "Underwater placement did not retain water");
        level.destroyBlock(floor, false);
        helper.assertTrue(level.getBlockState(pos).is(PufferfishBlocks.PLACED_FISH.get()), "Floating fish disappeared after removing support");
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.5));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(Items.PUFFERFISH), "Breaking a floating fish lost or duplicated its item");
        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        var side = floor.east();
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, floor,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0.5, 0, 0), Direction.EAST, floor, false));
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getCancellationResult().consumesAction() && level.getBlockState(side).is(PufferfishBlocks.PLACED_FISH.get()),
                "Fish could not be placed beside a block without floor support");
        helper.succeed();
    }
}
