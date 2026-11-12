// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import snownee.jade.impl.BlockAccessorImpl;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import snownee.jade.impl.EntityAccessorImpl;
import snownee.jade.impl.WailaCommonRegistration;

@PrefixGameTestTemplate(false)
public final class PufferfishJadeGameTests {
    @GameTest(template = "empty", templateNamespace = PufferfishPlease.MODID, batch = "jade")
    public static void jadeExportsPlacedFishAndLampRanksWithoutChangingTheirData(GameTestHelper helper) {
        var blocks = new java.util.ArrayList<PlacedPufferfishBlock>();
        blocks.add(PufferfishBlocks.PLACED_FISH.get());
        blocks.add(PufferfishBlocks.PLACED_HEAVY_FISH.get());
        PufferfishLamps.VARIANTS.values().forEach(variant -> blocks.add(variant.block().get()));
        var id = PufferfishPlease.id("level");
        for (var block : blocks) {
            var state = block.defaultBlockState();
            var entity = (PlacedPufferfishBlockEntity) block.newBlockEntity(BlockPos.ZERO, state);
            var provider = WailaCommonRegistration.instance().getBlockNBTProviders(block, entity).stream()
                    .filter(value -> value.getUid().equals(id)).findFirst().orElseThrow();
            var base = PufferfishLamps.fish(block);
            if (base.isEmpty()) base = new ItemStack(block == PufferfishBlocks.PLACED_HEAVY_FISH.get() ? PufferfishItems.HEAVY.get() : Items.PUFFERFISH);
            for (int scenario = 0; scenario < 4; scenario++) {
                var stack = base.copy();
                var cultivation = switch (scenario) {
                    case 2 -> new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.5, 4000, 8000);
                    case 3 -> new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.25, 20_000_000, 40_000_000);
                    default -> null;
                };
                if (scenario == 1) PufferfishDurability.activate(stack);
                if (cultivation != null) stack.set(PufferfishPlease.CULTIVATION.get(), cultivation);
                entity.setFish(stack);
                var accessor = new BlockAccessorImpl.Builder().level(helper.getLevel()).blockState(state).blockEntity(() -> entity).build();
                provider.appendServerData(accessor.getServerData(), accessor);
                var data = accessor.getServerData();
                if (scenario == 0) helper.assertTrue(data.isEmpty(), "Jade invented a rank for an inactive decoration");
                else {
                    var displayed = ComponentSerialization.CODEC.parse(accessor.nbtOps(), data.get(id.toString())).getOrThrow();
                    var expected = cultivation == null ? PufferfishLevel.LEVEL_0.displayName() : cultivation.displayName();
                    helper.assertTrue(data.getAllKeys().equals(Set.of(id.toString())) && displayed.equals(expected),
                            "Jade lost a decoration's rank or exported hidden data");
                }
                helper.assertTrue(ItemStack.matches(stack, entity.fish()), "Jade inspection changed the decoration's fish");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = PufferfishPlease.MODID, batch = "jade")
    public static void jadeExportsOnlyVisibleRankForOrdinaryAndMaterialFish(GameTestHelper helper) {
        var id = PufferfishPlease.id("level");
        for (var type : List.of(EntityType.PUFFERFISH, PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get())) {
            var fish = type.create(helper.getLevel());
            var provider = WailaCommonRegistration.instance().getEntityNBTProviders(fish).stream()
                    .filter(value -> value.getUid().equals(id)).findFirst().orElseThrow();
            for (int scenario = 0; scenario < 5; scenario++) {
                var stack = PufferfishData.copy(fish);
                if (scenario > 0) PufferfishDurability.activate(stack);
                var cultivation = switch (scenario) {
                    case 2 -> new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.875, 12, 34);
                    case 3 -> new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.5, 4000, 8000);
                    case 4 -> new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.25, 20_000_000, 40_000_000);
                    default -> null;
                };
                if (cultivation != null) stack.set(PufferfishPlease.CULTIVATION.get(), cultivation);
                if (scenario > 0) PufferfishData.set(fish, stack);
                var accessor = new EntityAccessorImpl.Builder().level(helper.getLevel()).entity(() -> fish).build();
                var before = PufferfishData.stored(fish).copy();
                provider.appendServerData(accessor.getServerData(), accessor);
                var data = accessor.getServerData();
                if (scenario == 0) helper.assertTrue(data.isEmpty(), "Jade exposed a rank for an inactive fish");
                else {
                    var displayed = ComponentSerialization.CODEC.parse(accessor.nbtOps(), data.get(id.toString())).getOrThrow();
                    var expected = cultivation == null ? PufferfishLevel.LEVEL_0.displayName() : cultivation.displayName();
                    helper.assertTrue(data.getAllKeys().equals(Set.of(id.toString())) && displayed.equals(expected),
                            "Jade exported hidden state or lost ready/star display");
                }
                helper.assertTrue(net.minecraft.world.item.ItemStack.matches(before, PufferfishData.stored(fish)), "Jade inspection changed fish data");
            }
            fish.discard();
        }
        helper.succeed();
    }
}
