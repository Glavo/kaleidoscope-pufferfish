// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.WeatheringCopper;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CopperPufferfish {
    private CopperPufferfish() {}

    public static int index(ItemStack stack) {
        for (int i = 0; i < PufferfishItems.COPPER_VARIANTS.size(); i++) {
            if (stack.is(PufferfishItems.COPPER_VARIANTS.get(i).get())) return i;
        }
        return -1;
    }

    public static String material(ItemStack stack) {
        return switch (index(stack) % 4) {
            case 1 -> "exposed_copper";
            case 2 -> "weathered_copper";
            case 3 -> "oxidized_copper";
            default -> "copper";
        };
    }

    public static int skinIndex(ItemStack stack) {
        for (int i = 0; i < PufferfishBlocks.COPPER_SKINS.size(); i++) {
            if (stack.is(PufferfishBlocks.COPPER_SKINS.get(i).get().asItem())) return i;
        }
        return -1;
    }

    private static int placedIndex(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity placed ? index(placed.fish())
                : skinIndex(level.getBlockState(pos).getBlock().asItem().getDefaultInstance());
    }

    private static void changePlaced(net.minecraft.world.level.Level level, BlockPos pos, int index) {
        if (level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity placed) {
            var fish = convert(placed.fish(), index);
            if (PufferfishLamps.isLamp(level.getBlockState(pos).getBlock())) {
                var block = ((net.minecraft.world.item.BlockItem) PufferfishLamps.lamp(fish).getItem()).getBlock();
                level.setBlockAndUpdate(pos, block.withPropertiesOf(level.getBlockState(pos)));
                placed = (PlacedPufferfishBlockEntity) level.getBlockEntity(pos);
            }
            placed.setFish(fish);
        } else {
            level.setBlockAndUpdate(pos, PufferfishBlocks.COPPER_SKINS.get(index).get().withPropertiesOf(level.getBlockState(pos)));
        }
    }

    public static ItemStack convert(ItemStack stack, int index) {
        return stack.transmuteCopy(PufferfishItems.COPPER_VARIANTS.get(index).get());
    }

    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, event.getPos())
                || event.getUseItem() == net.neoforged.neoforge.common.util.TriState.FALSE) return;
        int index = placedIndex(level, event.getPos());
        if (index < 0) return;
        var tool = event.getItemStack();
        boolean wax = tool.is(Items.HONEYCOMB) && index < 4;
        boolean scrape = tool.canPerformAction(ItemAbilities.AXE_WAX_OFF) && index >= 4
                || tool.canPerformAction(ItemAbilities.AXE_SCRAPE) && index > 0 && index < 4;
        if (!wax && !scrape) return;
        if (!level.isClientSide()) {
            changePlaced(level, event.getPos(), wax ? index + 4 : index >= 4 ? index - 4 : index - 1);
            level.levelEvent(player, wax ? 3003 : index >= 4 ? 3004 : 3005, event.getPos(), 0);
            level.playSound(null, event.getPos(), wax ? net.minecraft.sounds.SoundEvents.HONEYCOMB_WAX_ON
                    : index >= 4 ? net.minecraft.sounds.SoundEvents.AXE_WAX_OFF : net.minecraft.sounds.SoundEvents.AXE_SCRAPE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
            if (wax) tool.consume(1, player);
            else tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
    }

    public static void randomTick(ServerLevel level, BlockPos pos, RandomSource random) {
        int age = placedIndex(level, pos);
        if (age < 0 || age >= 3 || random.nextFloat() >= 0.05688889F) return;
        int same = 0;
        int older = 0;
        for (var neighbor : BlockPos.withinManhattan(pos, 4, 4, 4)) {
            if (neighbor.distManhattan(pos) > 4) break;
            if (neighbor.equals(pos)) continue;
            int other = placedIndex(level, neighbor);
            if (other >= 4) other = -1;
            if (other < 0 && level.getBlockState(neighbor).getBlock() instanceof WeatheringCopper copper) {
                other = copper.getAge().ordinal();
            }
            if (other < 0) continue;
            if (other < age) return;
            if (other == age) same++;
            else older++;
        }
        float chance = (float) (older + 1) / (older + same + 1);
        if (random.nextFloat() < chance * chance * (age == 0 ? 0.75F : 1)) {
            changePlaced(level, pos, age + 1);
        }
    }
}
