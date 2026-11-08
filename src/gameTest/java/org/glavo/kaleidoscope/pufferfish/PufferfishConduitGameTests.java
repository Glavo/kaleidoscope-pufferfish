// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConduitBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishConduitGameTests {
    private static final BlockPos CENTER = new BlockPos(2, 3, 2);

    @GameTest(template = "empty")
    public static void allFishAndMixedColorsCraftTheSameConduit(GameTestHelper helper) {
        var variants = new ArrayList<ItemStack>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        variants.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        for (var fish : variants) {
            var grid = new ArrayList<>(Collections.nCopies(9, fish));
            grid.set(4, new ItemStack(Items.HEART_OF_THE_SEA));
            checkRecipe(helper, grid);
        }
        var mixed = new ArrayList<>(variants.subList(0, 9));
        mixed.set(0, variants.getLast());
        mixed.set(4, new ItemStack(Items.HEART_OF_THE_SEA));
        checkRecipe(helper, mixed);
        var recipe = helper.getLevel().getRecipeManager().byKey(KaleidoscopePufferfish.id("pufferfish_conduit")).orElseThrow();
        for (int index = 0; index < 9; index++) {
            for (var wrong : List.of(ItemStack.EMPTY, new ItemStack(Items.COD), new ItemStack(Items.PUFFERFISH_BUCKET))) {
                var invalid = new ArrayList<>(mixed);
                invalid.set(index, wrong);
                var matched = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, invalid), helper.getLevel());
                helper.assertTrue(matched.isEmpty() || !matched.get().id().equals(recipe.id()), "Invalid conduit recipe matched");
            }
        }
        var vanilla = new ArrayList<>(Collections.nCopies(9, new ItemStack(Items.NAUTILUS_SHELL)));
        vanilla.set(4, new ItemStack(Items.HEART_OF_THE_SEA));
        var input = CraftingInput.of(3, 3, vanilla);
        var result = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value()
                .assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(Items.CONDUIT), "Vanilla conduit recipe changed");
        helper.succeed();
    }

    private static void checkRecipe(GameTestHelper helper, List<ItemStack> grid) {
        var input = CraftingInput.of(3, 3, grid);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
        var output = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(output.is(PufferfishBlocks.CONDUIT.get().asItem()) && output.getCount() == 1, "Wrong conduit output");
        helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Conduit recipe returned consumed fish");
        helper.assertTrue(grid.stream().allMatch(stack -> stack.getCount() == 1), "Conduit recipe mutated its input");
    }

    @GameTest(template = "empty")
    public static void placementWaterloggingSavingAndDropsPreserveTheConduit(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(CENTER);
        for (boolean water : new boolean[] {false, true}) {
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, (water ? Blocks.WATER : Blocks.AIR).defaultBlockState());
            var stack = new ItemStack(PufferfishBlocks.CONDUIT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
            helper.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction() && stack.isEmpty(), "Conduit placement failed");
            var state = level.getBlockState(pos);
            helper.assertTrue(state.is(PufferfishBlocks.CONDUIT.get()) && state.getValue(ConduitBlock.WATERLOGGED) == water
                    && state.getFluidState().is(FluidTags.WATER) == water, "Conduit waterlogging was wrong");
            helper.assertTrue(state.getLightEmission(level, pos) == Blocks.CONDUIT.defaultBlockState().getLightEmission(level, pos), "Conduit light changed");
            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "Conduit did not support pickaxe mining");
            var entity = level.getBlockEntity(pos);
            helper.assertTrue(entity instanceof PufferfishConduitBlockEntity && entity.getType() == PufferfishBlocks.CONDUIT_ENTITY.get(), "Wrong conduit block entity type");
            var tag = entity.saveWithFullMetadata(level.registryAccess());
            var restored = BlockEntity.loadStatic(pos, state, tag, level.registryAccess());
            helper.assertTrue(restored instanceof PufferfishConduitBlockEntity && restored.isValidBlockState(state), "Conduit failed to reload");
            helper.assertTrue(((PufferfishConduitBlockEntity) entity).getUpdatePacket().getType() == PufferfishBlocks.CONDUIT_ENTITY.get(), "Conduit packet used vanilla entity type");
            helper.assertTrue(PufferfishBlocks.CONDUIT.get().getTicker(level, state, BlockEntityType.CONDUIT) == null, "Conduit accepted wrong ticker type");
            level.destroyBlock(pos, true, player);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(PufferfishBlocks.CONDUIT.get().asItem())
                    && drops.getFirst().getItem().getCount() == 1, "Conduit broke into the wrong drop");
            drops.forEach(ItemEntity::discard);
        }
        var vanillaState = Blocks.CONDUIT.defaultBlockState();
        var vanilla = new ConduitBlockEntity(pos, vanillaState);
        helper.assertTrue(vanilla.getType() == BlockEntityType.CONDUIT && vanilla.isValidBlockState(vanillaState), "Vanilla conduit entity changed");
        helper.succeed();
    }

    private static void frame(GameTestHelper helper, boolean full) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean perimeter = z == 0 && (Math.abs(x) == 2 || Math.abs(y) == 2)
                            || full && (x == 0 && (Math.abs(y) == 2 || Math.abs(z) == 2)
                            || y == 0 && (Math.abs(x) == 2 || Math.abs(z) == 2));
                    if (perimeter) helper.setBlock(CENTER.offset(x, y, z), Blocks.PRISMARINE);
                }
            }
        }
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void vanillaActivationPowerHuntingAndDeactivationWork(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTER);
        for (var water : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            level.setBlock(water, Blocks.WATER.defaultBlockState(), 2);
        }
        helper.setBlock(CENTER, PufferfishBlocks.CONDUIT.get());
        frame(helper, false);
        var entity = (PufferfishConduitBlockEntity) level.getBlockEntity(pos);
        var wet = helper.makeMockPlayer(GameType.SURVIVAL);
        wet.setPos(Vec3.atBottomCenterOf(pos.east()));
        wet.setNoGravity(true);
        level.addFreshEntity(wet);
        var dry = helper.makeMockPlayer(GameType.SURVIVAL);
        dry.setPos(Vec3.atBottomCenterOf(pos.above(5)));
        dry.setNoGravity(true);
        level.addFreshEntity(dry);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(entity.tickCount > 0 && entity.isActive() && !entity.isHunting(), "16-block frame did not activate its ticker");
            var power = wet.getEffect(MobEffects.CONDUIT_POWER);
            helper.assertTrue(power != null && power.getAmplifier() == 0 && power.getDuration() > 200, "Wet player did not receive vanilla conduit power");
            helper.assertTrue(!dry.hasEffect(MobEffects.CONDUIT_POWER), "Dry player received conduit power");
            wet.discard();
            dry.discard();
            helper.setBlock(CENTER.offset(2, 0, 0), Blocks.AIR);
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(!entity.isActive(), "15-block frame remained active");
                frame(helper, true);
                var enemy = helper.spawn(EntityType.DROWNED, CENTER.east());
                enemy.setNoAi(true);
                enemy.setNoGravity(true);
                helper.runAfterDelay(45, () -> {
                    helper.assertTrue(entity.isActive() && entity.isHunting(), "42-block frame did not enable hunting");
                    helper.assertTrue(enemy.getHealth() <= enemy.getMaxHealth() - 4, "Conduit did not attack a submerged hostile mob");
                    enemy.discard();
                    helper.setBlock(CENTER.above(), Blocks.STONE);
                    helper.runAfterDelay(45, () -> {
                        helper.assertTrue(!entity.isActive() && !entity.isHunting(), "Conduit stayed active with blocked surrounding water");
                        helper.succeed();
                    });
                });
            });
        });
    }
}
