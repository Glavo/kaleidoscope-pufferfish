// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import snownee.jade.impl.EntityAccessorImpl;
import snownee.jade.impl.WailaCommonRegistration;

@PrefixGameTestTemplate(false)
public final class PufferfishJadeGameTests {
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
