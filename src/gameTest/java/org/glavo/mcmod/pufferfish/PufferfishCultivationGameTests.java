// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestItems;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCultivationGameTests {
    @GameTest(template = "empty")
    public static void downgradeRefundUsesThePreviousThresholdAndRoundsWithoutOverflow(GameTestHelper helper) {
        for (var sample : java.util.List.of(
                new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0, 83, 0),
                new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.5, 83, Long.MAX_VALUE),
                new PufferfishCultivation(PufferfishLevel.LEVEL_2, 1, 83, 1))) {
            long expected = sample.talent() == 0 ? 100 : sample.talent() == 0.5 ? 60 : 40;
            helper.assertTrue(sample.downgradeRefund() == expected, "Refund used lifetime XP, the next rank's cost, or the wrong talent multiplier");
        }
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.25, 1, 0).downgradeRefund() == 57,
                "Refund must round the talent-adjusted threshold up before rounding the combined quarter down");
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.75, 3, Long.MAX_VALUE).downgradeRefund() == 0
                && new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0, 4, 0).downgradeRefund() == 1,
                "Awakening must not invent an experience threshold");
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, 20_000_000, 80_000_000).downgradeRefund() == 7_500_000,
                "Immortal stars were counted separately from current XP");
        var extreme = new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0, Long.MAX_VALUE, Long.MAX_VALUE);
        long expected = java.math.BigInteger.valueOf(Long.MAX_VALUE).add(java.math.BigInteger.valueOf(20_000_000))
                .divide(java.math.BigInteger.valueOf(4)).longValueExact();
        helper.assertTrue(extreme.downgradeRefund() == expected, "Maximum stored experience overflowed the refund");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakthroughExperienceDependsOnLevelAndTalent(GameTestHelper helper) {
        long normalTotal = 0;
        long bestTotal = 0;
        long worstTotal = 0;
        long previous = 0;
        for (var level : PufferfishLevel.values()) {
            if (level == PufferfishLevel.LEVEL_0 || level == PufferfishLevel.LEVEL_10) continue;
            var normal = new PufferfishCultivation(level, 0.5, 0, Long.MAX_VALUE);
            long required = normal.requiredExperience();
            helper.assertTrue(required > previous && !normal.isExperienceComplete(), "Requirements did not rise or lifetime XP counted toward breakthrough");
            previous = required;
            normalTotal += required;
            bestTotal += new PufferfishCultivation(level, 1, 0, 0).requiredExperience();
            worstTotal += new PufferfishCultivation(level, 0, 0, 0).requiredExperience();
            var ready = normal.addExperience((int) required - 1);
            helper.assertTrue(!ready.isExperienceComplete() && ready.displayName().equals(level.displayName()),
                    "Breakthrough experience was complete below the threshold");
            var readyName = Component.translatable("level.pufferfish_please.ready", level.displayName());
            var complete = new PufferfishCultivation(level, 0.5, required, Long.MAX_VALUE);
            helper.assertTrue(complete.isExperienceComplete() && complete.displayName().equals(readyName),
                    "Stored experience at the threshold did not display readiness");
            var promoted = ready.addExperience(1);
            boolean gated = level == PufferfishLevel.LEVEL_4;
            helper.assertTrue(promoted.level() == (gated ? level : PufferfishLevel.values()[level.ordinal() + 1]) && promoted.experience() == (gated ? required : 0)
                    && promoted.totalExperience() == Long.MAX_VALUE && promoted.talent() == 0.5,
                    "Exact threshold did not advance one rank while preserving lifetime XP and talent");
            helper.assertTrue(ready.addExperience(2).experience() == (gated ? required + 1 : 1), "Promotion discarded surplus experience");
        }
        helper.assertTrue(normalTotal == 12_601_760 && bestTotal == 6_300_880 && worstTotal == 25_203_520,
                "Lifetime cultivation costs do not match the agreed curve and talent range");
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.5, 0, 0).requiredExperience() == 160
                && new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.75, 0, 0).requiredExperience() == 114
                && new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.25, 0, 0).requiredExperience() == 227,
                "Initial breakthrough cost or upward rounding is incorrect");
        var highest = new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, Long.MAX_VALUE, Long.MAX_VALUE);
        helper.assertTrue(highest.requiredExperience() == 0 && !highest.isExperienceComplete(), "Highest level offered another experience breakthrough");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void promotionConsumesEachThresholdAndPreservesLifetimeExperience(GameTestHelper helper) {
        for (double talent : new double[] {0, 0.5, 1}) {
            var initial = new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 123);
            int totalCost = talent == 0 ? 25_203_520 : talent == 0.5 ? 12_601_760 : 6_300_880;
            var waiting = initial.addExperience(totalCost + 23_000_000);
            helper.assertTrue(waiting.level() == PufferfishLevel.LEVEL_4 && waiting.isExperienceComplete(), "Experience bypassed the conduit requirement");
            var promoted = waiting.advanceAtConduit();
            helper.assertTrue(promoted.level() == PufferfishLevel.LEVEL_10 && promoted.talent() == talent
                    && promoted.experience() == 23_000_000 && promoted.stars() == 2
                    && promoted.totalExperience() == 123L + totalCost + 23_000_000,
                    "Consecutive promotions used the wrong talent costs, spent lifetime XP or counted spent XP as stars");
        }
        var ready = new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.5, 560, 900);
        helper.assertTrue(ready.addExperience(0) == ready && ready.addExperience(-1) == ready,
                "Nonpositive awards changed cultivation");
        helper.assertTrue(ready.addExperience(1).equals(new PufferfishCultivation(PufferfishLevel.LEVEL_3, 0.5, 1, 901)),
                "Previously accumulated experience did not advance on the next award");
        var saturated = new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.5, Long.MAX_VALUE - 1, Long.MAX_VALUE - 1).addExperience(7).advanceAtConduit();
        helper.assertTrue(saturated.level() == PufferfishLevel.LEVEL_10 && saturated.experience() == Long.MAX_VALUE - 12_601_760
                && saturated.totalExperience() == Long.MAX_VALUE, "Consecutive promotions overflowed experience counters");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void highestRankStarsUseCurrentExperienceWithoutSpendingOrTalentScaling(GameTestHelper helper) {
        for (double talent : new double[] {0, 0.5, 1}) {
            var state = new PufferfishCultivation(PufferfishLevel.LEVEL_10, talent, 9_999_999, 50_000_000);
            helper.assertTrue(state.stars() == 0 && state.displayName().equals(PufferfishLevel.LEVEL_10.displayName()),
                    "An incomplete star or lifetime experience changed the rank name");
            state = state.addExperience(1);
            helper.assertTrue(state.stars() == 1 && state.experience() == 10_000_000 && state.totalExperience() == 50_000_001,
                    "The first star changed its threshold with talent or consumed experience");
            state = state.addExperience(23_000_000);
            helper.assertTrue(state.stars() == 3 && state.experience() == 33_000_000 && state.totalExperience() == 73_000_001
                    && state.level() == PufferfishLevel.LEVEL_10 && state.talent() == talent,
                    "Multiple stars lost surplus experience or changed cultivation state");
        }
        for (var level : PufferfishLevel.values()) {
            if (level == PufferfishLevel.LEVEL_0 || level == PufferfishLevel.LEVEL_10) continue;
            var state = new PufferfishCultivation(level, 0.5, Long.MAX_VALUE, Long.MAX_VALUE);
            helper.assertTrue(state.stars() == 0 && state.displayName().equals(Component.translatable("level.pufferfish_please.ready", level.displayName())),
                    "A completed lower rank displayed stars instead of readiness");
        }
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, Long.MAX_VALUE, Long.MAX_VALUE).stars() == 922_337_203_685L,
                "Stars overflowed at the experience limit");
        helper.succeed();
    }

    private static ItemStack awakened(ItemStack fish) {
        PufferfishDurability.activate(fish);
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.75, 10, 100));
        return fish;
    }

    @GameTest(template = "empty")
    public static void orbPickupCreditsEachEquippedFishWithoutSharingOrConsumingPlayerXp(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        var main = awakened(PufferfishItems.FLAIL.toStack());
        var off = awakened(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), net.minecraft.world.item.DyeColor.BLUE));
        var head = awakened(PufferfishItems.HEAVY.toStack());
        for (var fish : java.util.List.of(main, off, head)) {
            fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.75, 110, 200));
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, main);
        player.setItemSlot(EquipmentSlot.OFFHAND, off);
        player.setItemSlot(EquipmentSlot.HEAD, head);
        new ExperienceOrb(helper.getLevel(), 0, 0, 0, 7).playerTouch(player);
        helper.runAfterDelay(1, () -> {
            var expected = new PufferfishCultivation(PufferfishLevel.LEVEL_2, 0.75, 3, 207);
            for (var fish : java.util.List.of(main, off, head)) {
                helper.assertTrue(expected.equals(fish.get(PufferfishPlease.CULTIVATION.get())),
                        "Equipped fish lost XP, shared XP, changed talent or received duplicate credit");
            }
            helper.assertTrue(player.totalExperience == 7, "Cultivation consumed player XP");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void mendingDoesNotReduceCultivationExperience(GameTestHelper helper) {
        var fishes = new java.util.ArrayList<ItemStack>();
        for (int damage : new int[] {10, 20}) {
            var player = TestPlayers.serverPlayer(helper);
            var fish = awakened(new ItemStack(Items.PUFFERFISH));
            fish.setDamageValue(damage);
            TestItems.enchant(helper, fish, Enchantments.MENDING, 1);
            player.setItemSlot(EquipmentSlot.MAINHAND, fish);
            new ExperienceOrb(helper.getLevel(), 0, 0, 0, 8).playerTouch(player);
            helper.assertTrue(fish.getDamageValue() == (damage == 10 ? 0 : 4)
                    && player.totalExperience == (damage == 10 ? 3 : 0), "Cultivation interfered with Mending or leftover player XP");
            fishes.add(fish);
        }
        helper.runAfterDelay(1, () -> {
            var expected = new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.75, 18, 108);
            for (var fish : fishes) {
                helper.assertTrue(expected.equals(fish.get(PufferfishPlease.CULTIVATION.get())), "Mending consumed cultivation XP or caused duplicate credit");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void canceledPickupsAndIneligibleItemsDoNotGainCultivation(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        var fish = awakened(new ItemStack(Items.PUFFERFISH));
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_1, 0.75, 110, 200));
        var expected = fish.copy();
        player.setItemSlot(EquipmentSlot.MAINHAND, fish);
        var orb = new ExperienceOrb(helper.getLevel(), 0, 0, 0, 7);
        java.util.function.Consumer<PlayerXpEvent.PickupXp> cancel = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
        try {
            orb.playerTouch(player);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(!orb.isRemoved() && player.totalExperience == 0, "Canceled pickup consumed the orb or granted player XP");
        orb.discard();

        var other = TestPlayers.serverPlayer(helper);
        var fresh = new ItemStack(Items.PUFFERFISH, 64);
        var used = TestItems.worn(new ItemStack(Items.PUFFERFISH));
        var helmet = new ItemStack(Items.IRON_HELMET);
        helmet.set(PufferfishPlease.CULTIVATION.get(), expected.get(PufferfishPlease.CULTIVATION.get()));
        var reserve = expected.copy();
        other.setItemSlot(EquipmentSlot.MAINHAND, fresh);
        other.setItemSlot(EquipmentSlot.OFFHAND, used);
        other.setItemSlot(EquipmentSlot.HEAD, helmet);
        other.getInventory().setItem(9, reserve);
        new ExperienceOrb(helper.getLevel(), 0, 0, 0, 7).playerTouch(other);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(ItemStack.matches(fish, expected), "Late pickup cancellation still granted cultivation XP");
            helper.assertTrue(!fresh.has(PufferfishPlease.CULTIVATION.get()) && fresh.getCount() == 64
                    && !used.has(PufferfishPlease.CULTIVATION.get()), "Orb pickup awakened fish or changed food stacking");
            helper.assertTrue(ItemStack.matches(reserve, expected)
                    && helmet.get(PufferfishPlease.CULTIVATION.get()).equals(expected.get(PufferfishPlease.CULTIVATION.get())),
                    "Inventory reserves or non-fish equipment gained cultivation XP");
            helper.assertTrue(other.totalExperience == 7, "Ineligible equipment prevented player XP gain");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void highestLevelContinuesRecordingExperienceWithoutOverflow(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        var fish = awakened(new ItemStack(Items.PUFFERFISH));
        fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5,
                Long.MAX_VALUE - 3, Long.MAX_VALUE - 1));
        player.setItemSlot(EquipmentSlot.MAINHAND, fish);
        new ExperienceOrb(helper.getLevel(), 0, 0, 0, 7).playerTouch(player);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_10, 0.5, Long.MAX_VALUE, Long.MAX_VALUE)
                    .equals(fish.get(PufferfishPlease.CULTIVATION.get())), "Maximum-level cultivation stopped recording or overflowed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void cultivationSurvivesStorageAndSynchronization(GameTestHelper helper) {
        for (var level : PufferfishLevel.values()) {
            if (level == PufferfishLevel.LEVEL_0) continue;
            var state = new PufferfishCultivation(level, 0.1234567890123456, 9_000_000_001L, Long.MAX_VALUE);
            var fish = new ItemStack(Items.PUFFERFISH);
            PufferfishDurability.activate(fish);
            fish.set(PufferfishPlease.CULTIVATION.get(), state);
            var loaded = ItemStack.parse(helper.getLevel().registryAccess(), fish.save(helper.getLevel().registryAccess())).orElseThrow();
            helper.assertTrue(state.equals(loaded.get(PufferfishPlease.CULTIVATION.get())), "Saving lost cultivation precision or level");
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
                "{\"level\":\"level.0\",\"talent\":0.5,\"experience\":0,\"total_experience\":0}",
                "{\"level\":\"unknown\",\"talent\":0.5,\"experience\":0,\"total_experience\":0}",
                "{\"level\":\"level.1\",\"talent\":-0.1,\"experience\":0,\"total_experience\":0}",
                "{\"level\":\"level.1\",\"talent\":1.1,\"experience\":0,\"total_experience\":0}",
                "{\"level\":\"level.1\",\"talent\":1e999,\"experience\":0,\"total_experience\":0}",
                "{\"level\":\"level.1\",\"talent\":0.5,\"experience\":-1,\"total_experience\":0}",
                "{\"level\":\"level.1\",\"talent\":0.5,\"experience\":0,\"total_experience\":-1}")) {
            helper.assertTrue(PufferfishCultivation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(),
                    "Invalid cultivation data was accepted: " + json);
        }
        for (double talent : new double[] {0, 1}) {
            var state = new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 0);
            var encoded = PufferfishCultivation.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            helper.assertTrue(encoded.getAsJsonObject().get("level").getAsString().equals("level.1"),
                    "Cultivation persisted a level name instead of a level ID");
            helper.assertTrue(PufferfishCultivation.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(state),
                    "Valid talent boundary was rejected");
        }
        helper.succeed();
    }
}
