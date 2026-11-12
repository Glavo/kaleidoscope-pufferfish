// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCultivationGameTests {
    @GameTest(template = "empty")
    public static void cultivationSurvivesStorageAndSynchronization(GameTestHelper helper) {
        for (var realm : PufferfishRealm.values()) {
            if (realm == PufferfishRealm.LEVEL_0) continue;
            var state = new PufferfishCultivation(realm, 0.1234567890123456, 9_000_000_001L);
            var fish = new ItemStack(Items.PUFFERFISH);
            PufferfishDurability.activate(fish);
            fish.set(PufferfishPlease.CULTIVATION.get(), state);
            var loaded = ItemStack.parse(helper.getLevel().registryAccess(), fish.save(helper.getLevel().registryAccess())).orElseThrow();
            helper.assertTrue(state.equals(loaded.get(PufferfishPlease.CULTIVATION.get())), "Saving lost cultivation precision or realm");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, fish);
                var synced = ItemStack.STREAM_CODEC.decode(buffer);
                helper.assertTrue(ItemStack.matches(fish, synced), "Synchronization changed cultivation data");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cultivationRejectsInvalidState(GameTestHelper helper) {
        for (var json : java.util.List.of(
                "{\"realm\":\"level.0\",\"talent\":0.5,\"experience\":0}",
                "{\"realm\":\"unknown\",\"talent\":0.5,\"experience\":0}",
                "{\"realm\":\"level.1\",\"talent\":-0.1,\"experience\":0}",
                "{\"realm\":\"level.1\",\"talent\":1.1,\"experience\":0}",
                "{\"realm\":\"level.1\",\"talent\":1e999,\"experience\":0}",
                "{\"realm\":\"level.1\",\"talent\":0.5,\"experience\":-1}")) {
            helper.assertTrue(PufferfishCultivation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(),
                    "Invalid cultivation data was accepted: " + json);
        }
        for (double talent : new double[] {0, 1}) {
            var state = new PufferfishCultivation(PufferfishRealm.LEVEL_1, talent, 0);
            var encoded = PufferfishCultivation.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            helper.assertTrue(encoded.getAsJsonObject().get("realm").getAsString().equals("level.1"),
                    "Cultivation persisted a realm name instead of a level ID");
            helper.assertTrue(PufferfishCultivation.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(state),
                    "Valid talent boundary was rejected");
        }
        helper.succeed();
    }
}
