// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.glavo.kaleidoscope.pufferfish.PufferfishItems;

@PrefixGameTestTemplate(false)
public final class PufferfishEngineGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 1);

    private static PufferfishEngineBlockEntity engine(GameTestHelper helper) {
        helper.setBlock(POS, CreateIntegration.ENGINE.get());
        return (PufferfishEngineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static void use(PufferfishEngineBlockEntity engine, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var pos = engine.getBlockPos();
        engine.getBlockState().useItemOn(stack, engine.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    private static void take(PufferfishEngineBlockEntity engine, Player player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        var pos = engine.getBlockPos();
        engine.getBlockState().useWithoutItem(engine.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        player.setShiftKeyDown(false);
    }

    private static void turnAround(PufferfishEngineBlockEntity engine, Player player) {
        var wrench = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench")));
        player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
        var pos = engine.getBlockPos();
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        wrench.getItem().useOn(context);
        wrench.getItem().useOn(context);
    }

    private static List<ItemStack> variants() {
        var result = new ArrayList<ItemStack>();
        result.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) {
            result.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        }
        result.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        result.add(new ItemStack(PufferfishItems.HEAVY.get()));
        return result;
    }

    @GameTest(template = "empty", templateNamespace = KaleidoscopePufferfish.MODID)
    public static void everyFishPreservesComponentsAndProducesPower(GameTestHelper helper) {
        var engine = engine(helper);
        var player = player(helper);
        use(engine, player, new ItemStack(Items.COD));
        helper.assertTrue(engine.getFish().isEmpty(), "Engine accepted another fish species");
        for (var variant : variants()) {
            player.getInventory().clearContent();
            variant.set(DataComponents.CUSTOM_NAME, Component.literal("Engine operator"));
            variant.setCount(5);
            use(engine, player, variant);
            helper.assertTrue(variant.getCount() == 4 && engine.getFish().getCount() == 1, "Engine did not consume exactly one fish");
            helper.assertTrue(ItemStack.isSameItemSameComponents(variant, engine.getFish()), "Engine lost fish components");
            if (!engine.hasWater()) {
                helper.assertTrue(engine.getGeneratedSpeed() == 0, "Dry engine produced power");
                use(engine, player, new ItemStack(Items.WATER_BUCKET));
                helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Water bucket was not returned");
            }
            int capacity = PufferfishItems.hasNetheriteCombat(variant) ? 1024 : 256;
            helper.assertTrue(engine.getGeneratedSpeed() == 16 && engine.calculateAddedStressCapacity() * 16 == capacity,
                    "Wrong engine speed or total capacity");
            var extra = new ItemStack(Items.PUFFERFISH, 2);
            use(engine, player, extra);
            helper.assertTrue(extra.getCount() == 2 && ItemStack.isSameItemSameComponents(variant, engine.getFish()), "Occupied engine replaced its fish");
            take(engine, player);
            helper.assertTrue(engine.getFish().isEmpty() && engine.hasWater() && engine.getGeneratedSpeed() == 0, "Extraction did not stop engine or consumed water");
            helper.assertTrue(player.getInventory().items.stream().anyMatch(s -> s.getCount() == 1
                    && ItemStack.isSameItemSameComponents(s, variant)), "Extraction lost fish components");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = KaleidoscopePufferfish.MODID)
    public static void bucketsRoundTripWithoutDuplication(GameTestHelper helper) {
        var engine = engine(helper);
        var player = player(helper);
        for (var color : DyeColor.values()) {
            var bucket = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), color);
            bucket.set(DataComponents.CUSTOM_NAME, Component.literal("Bucket operator"));
            CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.putInt("Air", 187));
            var expected = bucket.copy();
            use(engine, player, bucket);
            helper.assertTrue(engine.hasWater() && engine.isRunning() && player.getMainHandItem().is(Items.BUCKET), "Fish bucket did not fill engine");
            use(engine, player, player.getMainHandItem());
            helper.assertTrue(!engine.hasWater() && engine.getFish().isEmpty(), "Bucket extraction left fish or water behind");
            helper.assertTrue(ItemStack.matches(player.getMainHandItem(), expected), "Bucket round trip lost color, name, or entity data");
        }
        use(engine, player, new ItemStack(Items.WATER_BUCKET));
        use(engine, player, new ItemStack(Items.BUCKET));
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && !engine.hasWater(), "Empty tank water extraction failed");
        player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getAbilities().instabuild = true;
        var stack = new ItemStack(Items.PUFFERFISH, 4);
        use(engine, player, stack);
        helper.assertTrue(stack.getCount() == 4 && engine.getFish().getCount() == 1, "Creative insertion consumed fish");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = KaleidoscopePufferfish.MODID, timeoutTicks = 160)
    public static void realNetworkRunsStopsAndUpgrades(GameTestHelper helper) {
        var engine = engine(helper);
        var player = player(helper);
        var shaftPos = POS.south();
        var drillPos = POS.south(2);
        Block shaftBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "shaft"));
        Block drillBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_drill"));
        helper.setBlock(shaftPos, shaftBlock.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
        helper.setBlock(drillPos, drillBlock.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH));
        use(engine, player, new ItemStack(Items.PUFFERFISH_BUCKET));
        helper.runAfterDelay(15, () -> {
            var shaft = (KineticBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(shaftPos));
            var drill = (KineticBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(drillPos));
            helper.assertTrue(shaft.getSpeed() == 16 && drill.getSpeed() == 16, "Engine failed to drive a shaft and drill");
            helper.assertTrue(engine.getOrCreateNetwork().calculateCapacity() == 256, "Network capacity was not 256 SU");
            helper.setBlock(POS.west(), Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(engine.getGeneratedSpeed() == 0 && shaft.getSpeed() == 0 && drill.getSpeed() == 0, "Redstone failed to stop network");
                helper.setBlock(POS.west(), Blocks.AIR);
                take(engine, player);
                use(engine, player, new ItemStack(PufferfishItems.NETHERITE.get()));
                helper.runAfterDelay(15, () -> {
                    helper.assertTrue(shaft.getSpeed() == 16 && drill.getSpeed() == 16, "Upgrade changed network speed");
                    helper.assertTrue(engine.getOrCreateNetwork().calculateCapacity() == 1024, "Upgrade failed to provide 1024 SU");
                    helper.runAfterDelay(60, () -> {
                        helper.assertTrue(engine.getSpeed() == 16 && drill.getSpeed() == 16
                                && engine.getOrCreateNetwork().calculateCapacity() == 1024, "Output fluctuated during inflation cycle");
                        turnAround(engine, player);
                        helper.runAfterDelay(10, () -> {
                            helper.assertTrue(shaft.getSpeed() == 0, "Turning the output away left the old shaft powered");
                            turnAround(engine, player);
                            helper.runAfterDelay(10, () -> {
                                helper.assertTrue(drill.getSpeed() == 16 && PufferfishItems.isNetherite(engine.getFish()), "Rotating the engine lost its fish or connections");
                                helper.succeed();
                            });
                        });
                    });
                });
            });
        });
    }

    @GameTest(template = "empty", templateNamespace = KaleidoscopePufferfish.MODID)
    public static void persistenceSyncAndDestructionPreserveOneFish(GameTestHelper helper) {
        var engine = engine(helper);
        var player = player(helper);
        var bucket = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), DyeColor.PURPLE);
        bucket.set(DataComponents.CUSTOM_NAME, Component.literal("Persistent operator"));
        use(engine, player, bucket);
        var fish = engine.getFish();
        var registries = helper.getLevel().registryAccess();
        var saved = engine.saveWithFullMetadata(registries);
        var restored = new PufferfishEngineBlockEntity(engine.getBlockPos(), engine.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(saved, registries);
        helper.assertTrue(ItemStack.matches(fish, restored.getFish()) && restored.hasWater() && restored.getGeneratedSpeed() == 16,
                "Save/load lost fish or power state");
        var clientCopy = new PufferfishEngineBlockEntity(engine.getBlockPos(), engine.getBlockState());
        clientCopy.handleUpdateTag(engine.getUpdateTag(registries), registries);
        helper.assertTrue(ItemStack.matches(fish, clientCopy.getFish()) && clientCopy.hasWater(), "Client sync lost fish or water");
        var pos = engine.getBlockPos();
        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.stream().filter(e -> ItemStack.isSameItemSameComponents(e.getItem(), fish))
                .mapToInt(e -> e.getItem().getCount()).sum() == 1, "Breaking engine lost or duplicated fish");
        helper.assertTrue(drops.stream().filter(e -> e.getItem().is(CreateIntegration.ENGINE_ITEM.get()))
                .mapToInt(e -> e.getItem().getCount()).sum() == 1, "Breaking engine lost or duplicated block");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = KaleidoscopePufferfish.MODID)
    public static void craftingAndShaftOrientation(GameTestHelper helper) {
        var copper = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "copper_sheet")));
        var casing = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "andesite_casing")));
        var shaft = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "shaft")));
        var glass = new ItemStack(Items.GLASS);
        var input = CraftingInput.of(3, 3, List.of(copper, glass, copper, glass, casing, glass, copper, shaft, copper));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
        helper.assertTrue(recipe.assemble(input, helper.getLevel().registryAccess()).is(CreateIntegration.ENGINE_ITEM.get()), "Engine recipe failed");
        var block = CreateIntegration.ENGINE.get();
        for (var facing : Direction.Plane.HORIZONTAL) {
            var state = block.defaultBlockState().setValue(PufferfishEngineBlock.HORIZONTAL_FACING, facing);
            for (var side : Direction.values()) {
                helper.assertTrue(block.hasShaftTowards(helper.getLevel(), helper.absolutePos(POS), state, side) == (side == facing.getOpposite()),
                        "Engine exposes a shaft on the wrong face");
            }
            helper.assertTrue(block.getRotationAxis(state) == facing.getAxis(), "Wrong rotation axis");
        }
        helper.succeed();
    }
}
