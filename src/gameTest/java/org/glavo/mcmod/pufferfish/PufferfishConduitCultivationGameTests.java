// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishConduitCultivationGameTests {
    @GameTest(template = "empty", timeoutTicks = 180, batch = "cultivation_conduit")
    public static void normalConduitAdvancesReadyFishWithoutPower(GameTestHelper helper) { verify(helper, false); }

    @GameTest(template = "empty", timeoutTicks = 180, batch = "cultivation_conduit")
    public static void ancientConduitAdvancesReadyFishDespiteCurse(GameTestHelper helper) { verify(helper, true); }

    private static ItemStack fish(long experience) {
        var stack = new ItemStack(Items.PUFFERFISH);
        PufferfishDurability.activate(stack);
        stack.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishLevel.LEVEL_4, 0.5, experience, 50_000));
        return stack;
    }

    private static void verify(GameTestHelper helper, boolean ancient) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, ancient ? 112 : 8, 2));
        for (var water : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(pos, (ancient ? PufferfishBlocks.ANCIENT_CONDUIT : PufferfishBlocks.CONDUIT).get().defaultBlockState());
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
            if (Math.abs(x) == 2 || Math.abs(y) == 2) level.setBlockAndUpdate(pos.offset(x, y, 0), Blocks.PRISMARINE.defaultBlockState());
        }
        var missing = pos.offset(-2, -2, 0);
        level.removeBlock(missing, false);
        var player = TestPlayers.serverPlayer(helper);
        player.setPos(pos.north(5).getCenter());
        player.setNoGravity(true);
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_CURSE, 1000));
        var main = fish(4003);
        var off = fish(3999);
        var head = fish(20_003);
        player.setItemSlot(EquipmentSlot.MAINHAND, main);
        player.setItemSlot(EquipmentSlot.OFFHAND, off);
        player.setItemSlot(EquipmentSlot.HEAD, head);
        level.addFreshEntity(player);
        var outside = TestPlayers.serverPlayer(helper);
        outside.setPos(pos.north(32).getCenter());
        outside.setNoGravity(true);
        var excluded = fish(4003);
        outside.setItemSlot(EquipmentSlot.MAINHAND, excluded);
        outside.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 1000));
        level.addFreshEntity(outside);
        var ordinary = EntityType.PUFFERFISH.create(level);
        ordinary.setPos(pos.west(5).getCenter());
        ordinary.setNoAi(true);
        ordinary.setNoGravity(true);
        ordinary.setData(PufferfishPlease.FISH_ITEM, fish(4003));
        level.addFreshEntity(ordinary);
        var material = PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get().create(level);
        material.setPos(pos.east(5).getCenter());
        material.setNoAi(true);
        material.setNoGravity(true);
        var iron = PufferfishItems.IRON.toStack();
        iron.set(PufferfishPlease.CULTIVATION.get(), main.get(PufferfishPlease.CULTIVATION.get()));
        material.setFish(iron);
        level.addFreshEntity(material);
        var placedPos = pos.north(6);
        level.setBlockAndUpdate(placedPos, PufferfishBlocks.PLACED_FISH.get().defaultBlockState());
        var placed = (PlacedPufferfishBlockEntity) level.getBlockEntity(placedPos);
        placed.setFish(fish(4003));

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(main.get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_4
                    && placed.fish().get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_4,
                    "An inactive core advanced cultivation");
            level.setBlockAndUpdate(missing, Blocks.PRISMARINE.defaultBlockState());
            helper.runAfterDelay(45, () -> {
                var expected = new PufferfishCultivation(PufferfishLevel.LEVEL_5, 0.5, 3, 50_000);
                helper.assertTrue(PufferfishAdvancementGameTests.done(player, "cultivation_level_5")
                        && !PufferfishAdvancementGameTests.done(outside, "cultivation_level_5"),
                        "Core-driven promotion credited the wrong player");
                for (var stack : java.util.List.of(main, ordinary.getExistingDataOrNull(PufferfishPlease.FISH_ITEM), material.fish(), placed.fish())) {
                    helper.assertTrue(expected.equals(stack.get(PufferfishPlease.CULTIVATION.get())),
                            "Core range did not advance equipped, live or placed fish while preserving progress");
                }
                helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_6, 0.5, 3, 50_000).equals(head.get(PufferfishPlease.CULTIVATION.get())),
                        "Conduit breakthrough did not continue unrestricted promotions");
                helper.assertTrue(off.get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_4
                        && excluded.get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_4,
                        "Core advanced an unready fish or treated a lingering power effect as range");
                helper.assertTrue(!player.isInWaterOrRain() && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                        && player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Promotion changed dry-player power or curse behavior");
                new ExperienceOrb(level, 0, 0, 0, 1).playerTouch(player);
                helper.runAfterDelay(1, () -> {
                    helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_5, 0.5, 0, 50_001).equals(off.get(PufferfishPlease.CULTIVATION.get())),
                            "Reaching the threshold inside a core did not immediately advance");
                    level.removeBlock(missing, false);
                    var waiting = fish(4003);
                    player.setItemSlot(EquipmentSlot.MAINHAND, waiting);
                    helper.runAfterDelay(45, () -> {
                        helper.assertTrue(!PufferfishConduitSpawning.isInRange(level, pos)
                                && waiting.get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_4,
                                "A deactivated core continued advancing fish");
                        player.discard();
                        outside.discard();
                        player.getAdvancements().stopListening();
                        outside.getAdvancements().stopListening();
                        ordinary.discard();
                        material.discard();
                        level.removeBlock(placedPos, false);
                        for (var block : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) level.removeBlock(block, false);
                        helper.succeed();
                    });
                });
            });
        });
    }
}
