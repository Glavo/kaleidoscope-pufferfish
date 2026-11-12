// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.DamageEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishAwakeningGameTests {
    private static double seed(LivingEntity owner, double min, double max) {
        return seed(owner, min, max, 0);
    }

    private static double seed(LivingEntity owner, double min, double max, int priorFloats) {
        for (long seed = 0; seed < 100_000; seed++) {
            var random = RandomSource.create(seed);
            for (int i = 0; i < priorFloats; i++) random.nextFloat();
            double roll = random.nextDouble();
            if (roll >= min && roll < max) {
                owner.getRandom().setSeed(seed);
                return random.nextDouble();
            }
        }
        throw new IllegalStateException("No deterministic awakening seed found");
    }

    @GameTest(template = "empty")
    public static void directDamageUsesOneOutcomeSpecificRoll(GameTestHelper helper) {
        record Trial(boolean hostile, boolean lethal, double min, double max, boolean awakens) {}
        var trials = List.of(new Trial(false, false, 0.01, 0.04, true), new Trial(false, false, 0.06, 0.09, false),
                new Trial(false, true, 0.06, 0.09, true), new Trial(false, true, 0.15, 0.4, false),
                new Trial(true, true, 0.15, 0.4, true), new Trial(true, true, 0.6, 0.9, false));
        var stacks = new ArrayList<ItemStack>();
        var talents = new ArrayList<Double>();
        for (var trial : trials) {
            var player = TestPlayers.serverPlayer(helper);
            var fish = new ItemStack(Items.PUFFERFISH);
            PufferfishDurability.activate(fish);
            player.setItemSlot(EquipmentSlot.MAINHAND, fish);
            LivingEntity target = trial.hostile() ? helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2))
                    : helper.spawn(EntityType.COW, new BlockPos(2, 2, 2));
            ((net.minecraft.world.entity.Mob) target).setNoAi(true);
            if (trial.lethal()) target.setHealth(1);
            talents.add(seed(player, trial.min(), trial.max()));
            target.hurt(player.damageSources().playerAttack(player), 2);
            seed(player, trial.min(), trial.max());
            stacks.add(fish);
        }
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < trials.size(); i++) {
                var cultivation = stacks.get(i).get(PufferfishPlease.CULTIVATION.get());
                helper.assertTrue(trials.get(i).awakens() ? new PufferfishCultivation(PufferfishLevel.LEVEL_1, talents.get(i), 0, 0).equals(cultivation)
                        : cultivation == null, "Direct damage used the wrong chance, rerolled, or retained pre-awakening progress: " + i);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void projectileAwakeningSurvivesRecoveryAndFlailReturn(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 0)).getCenter());
        var reserve = new ItemStack(Items.PUFFERFISH, 32);
        player.setItemInHand(InteractionHand.MAIN_HAND, reserve);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        target.setNoAi(true);
        target.setHealth(1);
        var thrown = new ThrownPufferfish(helper.getLevel(), player);
        thrown.setItem(PufferfishItems.NETHERITE.toStack());
        thrown.setPos(target.position().add(0, 0.5, -1));
        thrown.setDeltaMovement(0, 0, 2);
        helper.getLevel().addFreshEntity(thrown);
        double talent = seed(player, 0.15, 0.4, 1);
        thrown.tick();
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3),
                item -> item.getItem().is(PufferfishItems.NETHERITE.get()));
        helper.assertTrue(drops.size() == 1 && new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 0)
                .equals(drops.getFirst().getItem().get(PufferfishPlease.CULTIVATION.get())), "Recovered projectile lost awakening");
        helper.assertTrue(reserve.getCount() == 32 && !reserve.has(PufferfishPlease.CULTIVATION.get()), "Projectile awakened the reserve stack");
        drops.forEach(ItemEntity::discard);
        target.discard();

        var held = PufferfishItems.FLAIL.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var other = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        other.setNoAi(true);
        other.setHealth(0.1F);
        var flail = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        flail.setPos(other.position().add(0, 0.5, -1));
        flail.setDeltaMovement(0, 0, 2);
        helper.getLevel().addFreshEntity(flail);
        talent = seed(player, 0.15, 0.4);
        flail.tick();
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 0).equals(held.get(PufferfishPlease.CULTIVATION.get()))
                && PufferfishDurability.sameWeapon(held, flail.getItem()), "Flail awakening changed only the projectile or broke the tether identity");
        int promotionCost = (int) held.get(PufferfishPlease.CULTIVATION.get()).requiredExperience();
        held.set(PufferfishPlease.CULTIVATION.get(), held.get(PufferfishPlease.CULTIVATION.get()).addExperience(promotionCost + 7));
        flail.tick();
        helper.assertTrue(!flail.isRemoved() && held.get(PufferfishPlease.CULTIVATION.get()).experience() == 7
                && held.get(PufferfishPlease.CULTIVATION.get()).level() == PufferfishLevel.LEVEL_2,
                "Promotion severed the returning flail or lost progress");
        flail.discard();
        other.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thornsCreditsItsActualItemAndNeverTheOtherEquipment(GameTestHelper helper) {
        var wearer = TestPlayers.serverPlayer(helper);
        var head = new ItemStack(Items.PUFFERFISH);
        var main = PufferfishItems.NETHERITE.toStack();
        wearer.setItemSlot(EquipmentSlot.HEAD, head);
        wearer.setItemSlot(EquipmentSlot.MAINHAND, main);
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2));
        target.setNoAi(true);
        var thorns = new DamageEntity(LevelBasedValue.constant(1), LevelBasedValue.constant(1),
                helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.THORNS));
        seed(wearer, 0.01, 0.04);
        thorns.apply(helper.getLevel(), 1, new EnchantedItemInUse(new ItemStack(Items.LEATHER_CHESTPLATE), EquipmentSlot.CHEST, wearer),
                target, target.position());
        helper.assertTrue(!head.has(PufferfishPlease.CULTIVATION.get()), "Armor thorns awakened the head fish");
        target.invulnerableTime = 0;
        double talent = seed(wearer, 0.01, 0.04);
        thorns.apply(helper.getLevel(), 1, new EnchantedItemInUse(head, EquipmentSlot.HEAD, wearer), target, target.position());
        var expected = new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 0);
        helper.assertTrue(expected.equals(head.get(PufferfishPlease.CULTIVATION.get())) && !main.has(PufferfishPlease.CULTIVATION.get()),
                "Head thorns credited the wrong item");
        target.invulnerableTime = 0;
        seed(wearer, 0.01, 0.04);
        thorns.apply(helper.getLevel(), 1, new EnchantedItemInUse(head, EquipmentSlot.HEAD, wearer), target, target.position());
        helper.assertTrue(expected.equals(head.get(PufferfishPlease.CULTIVATION.get())), "Repeated thorns rerolled talent");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void liveFishAwakeningSurvivesSavingAndCapture(GameTestHelper helper) {
        var ordinary = helper.spawn(EntityType.PUFFERFISH, new BlockPos(1, 2, 1));
        PufferfishColors.set(ordinary, net.minecraft.world.item.DyeColor.BLUE);
        var material = PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get().create(helper.getLevel());
        helper.getLevel().addFreshEntity(material);
        var checks = new ArrayList<Runnable>();
        for (Pufferfish fish : List.of(ordinary, material)) {
            fish.setNoAi(true);
            var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2));
            target.setNoAi(true);
            double talent = seed(fish, 0.01, 0.04);
            target.hurt(fish.damageSources().mobAttack(fish), 1);
            seed(fish, 0.01, 0.04);
            checks.add(() -> {
                var saved = new net.minecraft.nbt.CompoundTag();
                fish.saveWithoutId(saved);
                var loaded = (Pufferfish) fish.getType().create(helper.getLevel());
                loaded.load(saved);
                var bucket = loaded.getBucketItemStack();
                loaded.saveToBucketTag(bucket);
                var item = ItemStack.parseOptional(helper.getLevel().registryAccess(), bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag().getCompound("Fish"));
                helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 0).equals(item.get(PufferfishPlease.CULTIVATION.get())),
                        "Live fish saving or capture lost awakening");
                if (!(fish instanceof MaterialPufferfish)) helper.assertTrue(PufferfishColors.get(item) == net.minecraft.world.item.DyeColor.BLUE, "Awakening lost live fish color");
                loaded.discard();
                fish.discard();
                target.discard();
            });
        }
        helper.runAfterDelay(2, () -> {
            checks.forEach(Runnable::run);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void liveIntrinsicThornsStoresAwakeningOnTheFish(GameTestHelper helper) {
        var fish = PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get().create(helper.getLevel());
        fish.setNoAi(true);
        helper.getLevel().addFreshEntity(fish);
        var attacker = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2));
        attacker.setNoAi(true);
        double talent = seed(fish, 0.01, 0.04);
        helper.getLevel().getRandom().setSeed(4096);
        fish.hurt(attacker.damageSources().mobAttack(attacker), 1);
        helper.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "Seeded live thorns did not trigger");
        helper.assertTrue(new PufferfishCultivation(PufferfishLevel.LEVEL_1, talent, 0, 0)
                .equals(fish.fish().get(PufferfishPlease.CULTIVATION.get())), "Live thorns only awakened a discarded item copy");
        fish.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void canceledDeathsTotemsAndDamageOverTimeDoNotGrantKillChance(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        var fish = new ItemStack(Items.PUFFERFISH);
        player.setItemSlot(EquipmentSlot.MAINHAND, fish);
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        target.setNoAi(true);
        target.setHealth(1);
        java.util.function.Consumer<LivingDeathEvent> cancel = event -> {
            if (event.getEntity() == target) { event.setCanceled(true); target.setHealth(10); }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
        try {
            seed(player, 0.15, 0.4);
            target.hurt(player.damageSources().playerAttack(player), 2);
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        var totemPlayer = TestPlayers.serverPlayer(helper);
        var totemFish = new ItemStack(Items.PUFFERFISH);
        totemPlayer.setItemSlot(EquipmentSlot.MAINHAND, totemFish);
        var saved = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        saved.setNoAi(true);
        saved.setHealth(1);
        saved.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        seed(totemPlayer, 0.15, 0.4);
        saved.hurt(totemPlayer.damageSources().playerAttack(totemPlayer), 2);
        var dotPlayer = TestPlayers.serverPlayer(helper);
        var dotFish = new ItemStack(Items.PUFFERFISH);
        dotPlayer.setItemSlot(EquipmentSlot.MAINHAND, dotFish);
        seed(dotPlayer, 0.01, 0.04);
        target.invulnerableTime = 0;
        target.hurt(new net.minecraft.world.damagesource.DamageSource(helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.WITHER), dotPlayer), 1);
        target.setInvulnerable(true);
        target.hurt(dotPlayer.damageSources().playerAttack(dotPlayer), 1);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!fish.has(PufferfishPlease.CULTIVATION.get()) && !totemFish.has(PufferfishPlease.CULTIVATION.get())
                    && !dotFish.has(PufferfishPlease.CULTIVATION.get()), "Canceled death, totem, DOT or invulnerability granted awakening");
            helper.succeed();
        });
    }
}
