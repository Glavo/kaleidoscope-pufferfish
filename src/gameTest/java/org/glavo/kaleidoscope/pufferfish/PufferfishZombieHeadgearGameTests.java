// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishZombieHeadgearGameTests {
    @GameTest(template = "empty")
    public static void naturalZombiesRespectEquipmentSpawnReasonAndChance(GameTestHelper helper) {
        var chance = PufferfishMobConfig.ZOMBIE_HEADGEAR_CHANCE;
        double previous = chance.get();
        try {
            chance.set(1.0);
            for (var type : java.util.List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER)) {
                for (var reason : MobSpawnType.values()) {
                    var zombie = type.create(helper.getLevel());
                    zombie.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), 0, 0);
                    EventHooks.finalizeMobSpawn(zombie, helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()), reason, null);
                    zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    helper.getLevel().addFreshEntity(zombie);
                    boolean expected = type == EntityType.ZOMBIE && reason == MobSpawnType.NATURAL;
                    helper.assertTrue(PufferfishItems.isPufferfish(zombie.getItemBySlot(EquipmentSlot.HEAD)) == expected,
                            "Unexpected headgear for " + type + " / " + reason);
                    if (expected) helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD).isDamageableItem(), "Spawned headgear had no durability");
                    zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(zombie, helper.getLevel(), true));
                    helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "Reload rerolled equipment");
                    zombie.discard();
                }
            }
            for (boolean helmet : new boolean[] {true, false}) {
                chance.set(helmet ? 1.0 : 0.0);
                var zombie = EntityType.ZOMBIE.create(helper.getLevel());
                EventHooks.finalizeMobSpawn(zombie, helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.NATURAL, null);
                var head = helmet ? new ItemStack(Items.DIAMOND_HELMET) : ItemStack.EMPTY;
                zombie.setItemSlot(EquipmentSlot.HEAD, head);
                NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(zombie, helper.getLevel()));
                helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD) == head, "Headgear replaced a helmet or ignored zero chance");
            }
        } finally {
            chance.set(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zombieHeadgearWeightsSupportAllVariantsAndDisabling(GameTestHelper helper) {
        var weights = PufferfishMobConfig.ZOMBIE_HEADGEAR_WEIGHTS;
        var previous = new java.util.EnumMap<PufferfishZombieHeadgear.Variant, Integer>(PufferfishZombieHeadgear.Variant.class);
        weights.forEach((variant, weight) -> previous.put(variant, weight.get()));
        double previousChance = PufferfishMobConfig.ZOMBIE_HEADGEAR_CHANCE.get();
        try {
            PufferfishMobConfig.ZOMBIE_HEADGEAR_CHANCE.set(1.0);
            for (var selected : PufferfishZombieHeadgear.Variant.values()) {
                weights.forEach((variant, weight) -> weight.set(variant == selected ? 1 : 0));
                var seen = new java.util.HashSet<net.minecraft.world.item.Item>();
                for (int i = 0; i < 128; i++) {
                    var fish = generateHeadgear(helper, i);
                    boolean matches = switch (selected) {
                        case NORMAL -> fish.is(Items.PUFFERFISH);
                        case COPPER -> PufferfishItems.COPPER_VARIANTS.stream().anyMatch(item -> fish.is(item.get()));
                        case IRON -> fish.is(PufferfishItems.IRON.get());
                        case GOLDEN -> fish.is(PufferfishItems.GOLDEN.get());
                        case DIAMOND -> fish.is(PufferfishItems.DIAMOND.get());
                    };
                    helper.assertTrue(matches, "Selected a disabled headgear variant: " + fish);
                    helper.assertTrue(fish.getCount() == 1 && fish.isDamageableItem()
                            && fish.getMaxDamage() == PufferfishDurability.maximum(fish), "Variant headgear had incorrect durability");
                    seen.add(fish.getItem());
                }
                helper.assertTrue(seen.size() == (selected == PufferfishZombieHeadgear.Variant.COPPER ? 8 : 1),
                        "Not all enabled headgear variants were selected");
            }
            weights.values().forEach(weight -> weight.set(0));
            helper.assertTrue(generateHeadgear(helper, 0).isEmpty(), "All-zero weights did not disable headgear");
        } finally {
            previous.forEach((variant, weight) -> weights.get(variant).set(weight));
            PufferfishMobConfig.ZOMBIE_HEADGEAR_CHANCE.set(previousChance);
        }
        helper.succeed();
    }

    private static ItemStack generateHeadgear(GameTestHelper helper, long seed) {
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        zombie.moveTo(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)), 0, 0);
        EventHooks.finalizeMobSpawn(zombie, helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.NATURAL, null);
        zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        zombie.getRandom().setSeed(seed);
        NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(zombie, helper.getLevel()));
        return zombie.getItemBySlot(EquipmentSlot.HEAD);
    }

    @GameTest(template = "empty")
    public static void zombieHeadgearRetaliatesAndWearsOut(GameTestHelper helper) {
        var zombie = EntityType.ZOMBIE.create(helper.getLevel());
        var fish = new ItemStack(Items.PUFFERFISH);
        zombie.setItemSlot(EquipmentSlot.HEAD, fish);
        var attacker = EntityType.PIG.create(helper.getLevel());
        helper.assertTrue(PufferfishHeadgear.isWearing(zombie), "Zombie headgear was not recognized");
        helper.assertTrue(zombie.hurt(zombie.damageSources().mobAttack(attacker), 1), "Attack did not hit zombie");
        helper.assertTrue(attacker.hasEffect(MobEffects.POISON) && attacker.getEffect(MobEffects.POISON).getDuration() == 60,
                "Zombie headgear did not retaliate with three seconds of poison");
        helper.assertTrue(fish.getDamageValue() >= 1, "Zombie headgear did not lose durability");
        helper.succeed();
    }
}
