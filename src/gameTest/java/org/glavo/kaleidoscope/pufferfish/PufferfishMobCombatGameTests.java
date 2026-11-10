// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.MaterialPufferfish;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishMobCombatGameTests {
    private static void pool(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 8; y++) {
            boolean wall = x == 0 || z == 0 || x == 15 || z == 15 || y == 0 || y == 7;
            helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS : Blocks.WATER);
        }
    }

    private static MaterialPufferfish fish(GameTestHelper helper, String name) {
        var fish = PufferfishMobs.VARIANTS.get(name + "_pufferfish").entity().get().create(helper.getLevel());
        var pos = helper.absolutePos(new BlockPos(5, 3, 7));
        fish.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return fish;
    }

    @GameTest(template = "empty")
    public static void materialFishRejectPoisonAndRadiantRejectsWither(GameTestHelper helper) {
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(helper.getLevel());
            helper.assertTrue(!fish.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1))
                    && !fish.hasEffect(MobEffects.POISON), "Material fish accepted poison: " + variant.fish().getId());
            boolean wither = fish.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            helper.assertTrue(wither == (fish.kind() != PufferfishMobConfig.Kind.RADIANT), "Incorrect wither immunity");
            helper.assertTrue(fish.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200)), "Immunity blocked a beneficial effect");
        }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void headgearSuppressesProximityInflationOnly(GameTestHelper helper) {
        pool(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var fishPos = helper.absolutePos(new BlockPos(5, 3, 7)).getCenter();
        player.setPos(fishPos.add(1.5, 0, 0));
        player.setNoGravity(true);
        player.tick();
        helper.getLevel().addFreshEntity(player);
        var fishes = new java.util.ArrayList<net.minecraft.world.entity.animal.Pufferfish>();
        fishes.add(EntityType.PUFFERFISH.create(helper.getLevel()));
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            var fish = EntityType.PUFFERFISH.create(helper.getLevel());
            PufferfishColors.set(fish, color);
            fishes.add(fish);
        }
        for (var variant : PufferfishMobs.VARIANTS.values()) fishes.add(variant.entity().get().create(helper.getLevel()));
        try {
            for (var fish : fishes) {
                var headgear = fish instanceof MaterialPufferfish material ? material.fish().copy() : new ItemStack(Items.PUFFERFISH);
                if (PufferfishColors.get(fish) != null) PufferfishColors.dye(headgear, PufferfishColors.get(fish));
                player.setItemSlot(EquipmentSlot.HEAD, headgear);
                fish.setNoGravity(true);
                for (int tick = 0; tick < 60; tick++) {
                    fish.setPos(fishPos);
                    fish.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    fish.tick();
                    helper.assertTrue(fish.getPuffState() == 0, "Headgear wearer triggered inflation: " + fish.getType());
                }
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                boolean inflated = false;
                for (int tick = 0; tick < 60; tick++) {
                    fish.setPos(fishPos);
                    fish.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    fish.tick();
                    inflated |= fish.getPuffState() > 0;
                }
                helper.assertTrue(inflated, "Removing headgear did not restore inflation: " + fish.getType());
                fish.discard();
            }
        } finally { player.discard(); fishes.forEach(net.minecraft.world.entity.Entity::discard); }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void materialFishHuntHostilesExceptCreepersAndDrowned(GameTestHelper helper) {
        pool(helper);
        var creeper = helper.spawn(EntityType.CREEPER, new BlockPos(6, 3, 7));
        var drowned = helper.spawn(EntityType.DROWNED, new BlockPos(6, 3, 8));
        for (var mob : java.util.List.of(creeper, drowned)) { mob.setNoAi(true); mob.setNoGravity(true); mob.tick(); }
        try {
            for (var type : java.util.List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.SLIME)) {
                var target = helper.spawn(type, new BlockPos(9, 3, 7));
                target.setNoAi(true);
                target.setNoGravity(true);
                target.tick();
                try {
                    for (var variant : PufferfishMobs.VARIANTS.values()) {
                        var fish = variant.entity().get().create(helper.getLevel());
                        fish.setPos(helper.absolutePos(new BlockPos(5, 3, 7)).getCenter());
                        helper.assertTrue(fish.findCombatTarget() == (fish.kind() == PufferfishMobConfig.Kind.GOLDEN ? null : target),
                                "Incorrect hostile target: " + variant.fish().getId() + " / " + type);
                    }
                } finally { target.discard(); }
            }
            for (var variant : PufferfishMobs.VARIANTS.values()) {
                var fish = variant.entity().get().create(helper.getLevel());
                fish.setPos(helper.absolutePos(new BlockPos(5, 3, 7)).getCenter());
                helper.assertTrue(fish.findCombatTarget() == null, "Fish proactively targeted a creeper or drowned");
            }
        } finally { creeper.discard(); drowned.discard(); }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void copperRushesHostilesButDoesNotHuntPlayers(GameTestHelper helper) {
        pool(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(7, 3, 7)).getCenter());
        player.setNoGravity(true);
        player.tick();
        helper.getLevel().addFreshEntity(player);
        var fish = fish(helper, "copper");
        var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, 3, 7));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.tick();
        try {
            helper.assertTrue(!fish.validTarget(player) && fish.findCombatTarget() == target, "Copper fish hunted a nearby player");
            player.discard();
            boolean windup = false, rush = false;
            for (int tick = 0; tick < 130; tick++) {
                fish.tick();
                windup |= fish.attackPhase() == MaterialPufferfish.WINDUP;
                rush |= fish.attackPhase() == MaterialPufferfish.RUSH;
            }
            helper.assertTrue(windup && rush && target.getHealth() < target.getMaxHealth(), "Copper fish did not attack a hostile mob");
        } finally { player.discard(); fish.discard(); target.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mobProfilesAreIndependentOfItemDamageAndPreservedOnLoad(GameTestHelper helper) {
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(helper.getLevel());
            double[] expected = switch (fish.kind()) {
                case COPPER -> new double[]{16, 4, 2};
                case IRON -> new double[]{20, 5, 4};
                case GOLDEN -> new double[]{30, 3, 2};
                case DIAMOND -> new double[]{40, 8, 8};
                case NETHERITE -> new double[]{60, 10, 12};
                case HEAVY -> new double[]{80, 12, 16};
                case RADIANT -> new double[]{60, 8, 8};
            };
            helper.assertTrue(fish.getMaxHealth() == expected[0] && fish.getHealth() == expected[0]
                    && fish.getAttributeValue(Attributes.ATTACK_DAMAGE) == expected[1]
                    && fish.getArmorValue() == expected[2], "Incorrect living fish profile: " + variant.fish().getId());
            var item = fish.fish().copy();
            item.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY), 5);
            fish.setFish(item);
            helper.assertTrue(fish.getAttributeValue(Attributes.ATTACK_DAMAGE) == expected[1], "Item enchantments changed mob damage");
            fish.setHealth(7);
            var data = new net.minecraft.nbt.CompoundTag();
            fish.saveWithoutId(data);
            var restored = variant.entity().get().create(helper.getLevel());
            restored.load(data);
            helper.assertTrue(restored.getMaxHealth() == expected[0] && restored.getHealth() == 7
                    && restored.getArmorValue() == expected[2], "Reloading changed mob balance or healed the fish");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketCaptureUsesInclusiveHealthLimitsAndPreservesHealth(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "fish-capture-test"));
        player.setGameMode(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(level);
            fish.setNoAi(true);
            fish.setPos(pos.getX(), pos.getY(), pos.getZ());
            var stack = fish.fish().copy();
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Captured elite"));
            fish.setFish(stack);
            level.addFreshEntity(fish);
            float limit = switch (fish.kind()) { case DIAMOND -> 20; case NETHERITE, RADIANT -> 15; case HEAVY -> 20; default -> fish.getMaxHealth(); };
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            if (limit < fish.getMaxHealth()) {
                fish.setHealth(limit + 0.25F);
                helper.assertTrue(!fish.interact(player, InteractionHand.MAIN_HAND).consumesAction() && !fish.isRemoved()
                        && player.getMainHandItem().is(Items.WATER_BUCKET), "Healthy rare fish was captured or consumed the bucket");
            }
            fish.setHealth(limit);
            fish.interact(player, InteractionHand.MAIN_HAND);
            var bucket = player.getMainHandItem().copy();
            helper.assertTrue(fish.isRemoved() && bucket.is(variant.bucket().get()), "Capture failed at the inclusive health boundary");
            ((MobBucketItem) bucket.getItem()).checkExtraContent(null, level, bucket, pos);
            var released = level.getEntitiesOfClass(MaterialPufferfish.class, new AABB(pos).inflate(2));
            helper.assertTrue(released.size() == 1 && released.getFirst().getHealth() == limit
                    && ItemStack.isSameItemSameComponents(released.getFirst().fish(), stack), "Bucket release lost health or fish components");
            released.forEach(MaterialPufferfish::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void contactAndActiveAttacksShareCooldown(GameTestHelper helper) {
        var target = helper.spawn(EntityType.COW, new BlockPos(2, 2, 2));
        target.setNoAi(true);
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var fish = variant.entity().get().create(helper.getLevel());
            fish.setNoAi(true);
            fish.setPuffState(2);
            fish.setPos(target.position());
            target.removeAllEffects();
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            fish.sting(target);
            float health = target.getHealth();
            helper.assertTrue(health < target.getMaxHealth(), "Contact attack did no damage");
            target.invulnerableTime = 0;
            fish.sting(target);
            helper.assertTrue(!fish.strike(target, 1, fish.attackInterval()) && target.getHealth() == health,
                    "Contact and deliberate attacks dealt duplicate damage during cooldown");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disguiseDoesNotPreventStingsOrRetaliation(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PUFFERFISH));
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        var fish = fish(helper, "diamond");
        fish.setPos(player.position());
        fish.setPuffState(2);
        helper.assertTrue(fish.isDisguisedFrom(player), "Headgear was not recognized before contact");
        fish.sting(player);
        helper.assertTrue(player.getHealth() < player.getMaxHealth(), "Headgear prevented contact damage from an inflated material fish");
        fish.hurt(level.damageSources().playerAttack(player), 1);
        helper.assertTrue(!fish.isDisguisedFrom(player) && fish.isAngryAt(player), "Attacking did not break disguise");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void activeFishRecognizeDisguiseWaterAndLineOfSight(GameTestHelper helper) {
        pool(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(9, 3, 7)).getCenter());
        player.tick();
        helper.getLevel().addFreshEntity(player);
        var fish = fish(helper, "iron");
        try {
            helper.assertTrue(fish.findCombatTarget() == player, "Active fish did not acquire an unprovoked nearby player");
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PUFFERFISH));
            helper.assertTrue(fish.findCombatTarget() == null, "Disguised player attracted an unprovoked attack");
            fish.hurt(helper.getLevel().damageSources().playerAttack(player), 0.1F);
            helper.assertTrue(fish.findCombatTarget() == player, "Angry fish did not pursue the disguised attacker");
            for (int y = 1; y <= 6; y++) for (int z = 1; z <= 14; z++) helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
            helper.assertTrue(fish.findCombatTarget() == null, "Fish acquired a target through a wall");
            fish.setNoAi(true);
            fish.setPuffState(2);
            fish.tick();
            helper.assertTrue(Math.abs(fish.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.65) < 0.0001,
                    "Inflated iron fish did not gain defensive knockback resistance");
            fish.setPuffState(0);
            fish.tick();
            helper.assertTrue(Math.abs(fish.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.15) < 0.0001,
                    "Iron fish retained its inflated defense after shrinking");
        } finally { player.discard(); fish.discard(); }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void neutralFishNeverPursueAndActiveFishPerformTelegraphedRushes(GameTestHelper helper) {
        pool(helper);
        var target = helper.spawn(EntityType.COW, new BlockPos(9, 3, 7));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        target.tick();
        for (String kind : java.util.List.of("copper", "golden", "iron", "diamond", "netherite", "heavy", "radiant")) {
            var fish = fish(helper, kind);
            fish.getRandom().setSeed(514);
            fish.hurt(helper.getLevel().damageSources().mobAttack(target), 0.1F);
            var phases = new HashSet<Integer>();
            double closest = Double.MAX_VALUE;
            for (int tick = 0; tick < (kind.equals("radiant") ? 320 : 130); tick++) {
                fish.tick();
                phases.add(fish.attackPhase());
                closest = Math.min(closest, fish.distanceToSqr(target));
                target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                target.invulnerableTime = 0;
                if (!fish.isHostileToPlayers()) helper.assertTrue(fish.getTarget() == null && fish.attackPhase() == MaterialPufferfish.IDLE,
                        "Neutral fish pursued its attacker: " + kind);
            }
            if (fish.isHostileToPlayers()) helper.assertTrue(phases.contains(MaterialPufferfish.WINDUP) && phases.contains(MaterialPufferfish.RUSH)
                    && (phases.contains(MaterialPufferfish.REST) || phases.contains(MaterialPufferfish.RECOVERY)) && closest < 9,
                    "Active fish did not pursue, telegraph, rush and recover: " + kind + " " + phases);
            if (kind.equals("radiant")) helper.assertTrue(phases.contains(MaterialPufferfish.PULSE), "Radiant fish never telegraphed its pulse");
            fish.discard();
        }
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void heavyMissExposesArmorAndNetheriteCanChainRushes(GameTestHelper helper) {
        pool(helper);
        var target = helper.spawn(EntityType.COW, new BlockPos(9, 3, 7));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        target.tick();
        var heavy = fish(helper, "heavy");
        heavy.hurt(helper.getLevel().damageSources().mobAttack(target), 0.1F);
        boolean dodged = false, exposed = false, recovered = false;
        for (int tick = 0; tick < 120; tick++) {
            heavy.tick();
            if (!dodged && heavy.attackPhase() == MaterialPufferfish.RUSH) {
                var pos = helper.absolutePos(new BlockPos(9, 3, 11));
                target.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                target.tick();
                dodged = true;
            }
            if (heavy.attackPhase() == MaterialPufferfish.RECOVERY) {
                exposed = true;
                helper.assertTrue(heavy.getArmorValue() == 8 && heavy.getPuffState() == 0, "Heavy miss did not expose its armor");
            } else if (exposed && heavy.getArmorValue() == 16) recovered = true;
        }
        helper.assertTrue(dodged && exposed && recovered, "Heavy rush did not commit its direction and recover after a miss");
        heavy.discard();
        target.setPos(helper.absolutePos(new BlockPos(9, 3, 7)).getCenter());
        target.tick();
        var netherite = fish(helper, "netherite");
        netherite.getRandom().setSeed(29);
        netherite.hurt(helper.getLevel().damageSources().mobAttack(target), 0.1F);
        boolean gap = false, secondRush = false;
        for (int tick = 0; tick < 700; tick++) {
            netherite.tick();
            target.invulnerableTime = 0;
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if (netherite.attackPhase() == MaterialPufferfish.COMBO_GAP) gap = true;
            if (gap && netherite.attackPhase() == MaterialPufferfish.RUSH) { secondRush = true; break; }
        }
        helper.assertTrue(gap && secondRush, "Netherite never performed a telegraphed second rush");
        netherite.discard();
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void radiantPulseHasRangeCoverAndFixedWither(GameTestHelper helper) {
        pool(helper);
        var fish = fish(helper, "radiant");
        var near = helper.spawn(EntityType.COW, new BlockPos(7, 3, 7));
        var far = helper.spawn(EntityType.COW, new BlockPos(10, 3, 7));
        var covered = helper.spawn(EntityType.COW, new BlockPos(5, 3, 9));
        for (var target : java.util.List.of(near, far, covered)) { target.setNoAi(true); target.setNoGravity(true); }
        for (int x = 4; x <= 6; x++) for (int y = 2; y <= 5; y++) helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
        fish.pulse();
        helper.assertTrue(near.getHealth() < near.getMaxHealth() && near.hasEffect(MobEffects.GLOWING)
                && near.getEffect(MobEffects.WITHER).getAmplifier() == 1 && near.getEffect(MobEffects.WITHER).getDuration() == 100,
                "Radiant pulse did not apply fixed Wither II and glowing");
        helper.assertTrue(far.getHealth() == far.getMaxHealth() && covered.getHealth() == covered.getMaxHealth(),
                "Pulse passed through cover or exceeded its radius");
        float health = near.getHealth();
        near.invulnerableTime = 0;
        fish.pulse();
        fish.sting(near);
        helper.assertTrue(near.getHealth() == health && !near.hasEffect(MobEffects.BLINDNESS), "Pulse duplicated damage or used item-only blindness");
        fish.discard();
        helper.succeed();
    }
}
