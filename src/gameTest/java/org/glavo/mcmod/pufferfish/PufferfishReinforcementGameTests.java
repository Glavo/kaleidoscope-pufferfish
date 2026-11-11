// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishReinforcementGameTests {
    private static void pool(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 8; y++) {
            helper.setBlock(new BlockPos(x, y, z), x == 0 || x == 15 || z == 0 || z == 15 || y == 0 || y == 7 ? Blocks.GLASS : Blocks.WATER);
        }
    }

    private static MaterialPufferfish caller(GameTestHelper helper, String name) {
        var fish = PufferfishMobs.VARIANTS.get(name + "_pufferfish").entity().get().create(helper.getLevel());
        fish.setPos(helper.absolutePos(new BlockPos(7, 3, 7)).getCenter());
        fish.setNoGravity(true);
        fish.tick();
        fish.getRandom().setSeed(9042);
        helper.getLevel().addFreshEntity(fish);
        return fish;
    }

    private static Player attacker(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(9, 3, 7)).getCenter());
        player.setNoGravity(true);
        player.tick();
        return player;
    }

    private static void hit(MaterialPufferfish fish, LivingEntity attacker) {
        fish.invulnerableTime = 0;
        fish.hurt(fish.damageSources().mobAttack(attacker), 0.25F);
    }

    private static List<MaterialPufferfish> recruits(MaterialPufferfish caller) {
        return caller.level().getEntitiesOfClass(MaterialPufferfish.class, caller.getBoundingBox().inflate(10), fish -> fish != caller && fish.isAlive());
    }

    private static CompoundTag save(MaterialPufferfish fish) {
        var tag = new CompoundTag();
        fish.saveWithoutId(tag);
        return tag;
    }

    private static final class Config implements AutoCloseable {
        private final net.minecraft.server.level.ServerLevel level;
        private final boolean spawning;
        private final double chance = PufferfishMobConfig.REINFORCEMENT_CHANCE.get();
        private final int maxCalls = PufferfishMobConfig.REINFORCEMENT_MAX_CALLS.get();
        private final int limit = PufferfishMobConfig.REINFORCEMENT_NEARBY_LIMIT.get();
        private final java.util.Map<PufferfishMobConfig.Kind, Integer> weights = new java.util.EnumMap<>(PufferfishMobConfig.Kind.class);

        Config(GameTestHelper helper) {
            level = helper.getLevel();
            spawning = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, level.getServer());
            PufferfishMobConfig.REINFORCEMENT_WEIGHTS.forEach((kind, weight) -> weights.put(kind, weight.get()));
            PufferfishMobConfig.REINFORCEMENT_CHANCE.set(1.0);
        }

        void select(PufferfishMobConfig.Kind selected) {
            PufferfishMobConfig.REINFORCEMENT_WEIGHTS.forEach((kind, weight) -> weight.set(kind == selected ? 1 : 0));
        }

        @Override
        public void close() {
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(spawning, level.getServer());
            PufferfishMobConfig.REINFORCEMENT_CHANCE.set(chance);
            PufferfishMobConfig.REINFORCEMENT_MAX_CALLS.set(maxCalls);
            PufferfishMobConfig.REINFORCEMENT_NEARBY_LIMIT.set(limit);
            PufferfishMobConfig.REINFORCEMENT_WEIGHTS.forEach((kind, weight) -> weight.set(weights.get(kind)));
        }
    }

    @GameTest(template = "combat_pool")
    public static void bothElitesSummonEveryAllowedVariantAndShareTheirAttacker(GameTestHelper helper) {
        pool(helper);
        var attacker = attacker(helper);
        try (var config = new Config(helper)) {
            for (String elite : List.of("heavy", "radiant")) for (var kind : PufferfishMobConfig.REINFORCEMENT_WEIGHTS.keySet()) {
                config.select(kind);
                var caller = caller(helper, elite);
                try {
                    hit(caller, attacker);
                    var recruits = recruits(caller);
                    helper.assertTrue(recruits.size() == 1 && recruits.getFirst().kind() == kind, "Elite did not summon the selected reinforcement");
                    var recruit = recruits.getFirst();
                    helper.assertTrue(recruit.hasActiveAttacks() && recruit.getTarget() == attacker && recruit.findCombatTarget() == attacker,
                            "Reinforcement did not assist against the caller's attacker");
                    helper.assertTrue(recruit.getPuffState() > 0 && helper.getLevel().getFluidState(recruit.blockPosition()).is(net.minecraft.tags.FluidTags.WATER)
                            && helper.getLevel().noCollision(recruit), "Reinforcement spawned outside water or inside a block");
                    hit(recruit, attacker);
                    helper.assertTrue(recruits(caller).size() == 1, "Reinforcement recursively summoned more fish");
                } finally { recruits(caller).forEach(MaterialPufferfish::discard); caller.discard(); }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void reinforcementCooldownAndLifetimeLimitSurviveSavingAndBuckets(GameTestHelper helper) {
        pool(helper);
        var attacker = attacker(helper);
        try (var config = new Config(helper)) {
            config.select(PufferfishMobConfig.Kind.IRON);
            PufferfishMobConfig.REINFORCEMENT_MAX_CALLS.set(2);
            var caller = caller(helper, "radiant");
            try {
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).size() == 1, "Initial reinforcement was not summoned");
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).size() == 1, "Repeated hits bypassed reinforcement cooldown");
                var state = save(caller);
                var restored = PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get().create(helper.getLevel());
                restored.load(state);
                var bucket = caller.getBucketItemStack();
                caller.saveToBucketTag(bucket);
                var released = PufferfishMobs.VARIANTS.get("radiant_pufferfish").entity().get().create(helper.getLevel());
                released.loadFromBucketTag(bucket.get(DataComponents.BUCKET_ENTITY_DATA).copyTag());
                for (var copy : List.of(restored, released)) {
                    var data = save(copy);
                    helper.assertTrue(data.getInt("FishReinforcementCalls") == 1
                            && data.getInt("FishReinforcementCooldown") == PufferfishMobConfig.REINFORCEMENT_COOLDOWN.get(),
                            "Saving or bucket capture reset reinforcement limits");
                }
                state.putInt("FishReinforcementCooldown", 0);
                caller.readAdditionalSaveData(state);
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).size() == 2, "Reinforcements remained blocked after cooldown");
                state = save(caller);
                state.putInt("FishReinforcementCooldown", 0);
                caller.readAdditionalSaveData(state);
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).size() == 2, "Lifetime reinforcement limit was exceeded");
            } finally { recruits(caller).forEach(MaterialPufferfish::discard); caller.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "combat_pool")
    public static void reinforcementSpawningRespectsRulesSpaceAndNearbyLimit(GameTestHelper helper) {
        pool(helper);
        var attacker = attacker(helper);
        var rules = helper.getLevel().getGameRules();
        boolean spawning = rules.getBoolean(GameRules.RULE_DOMOBSPAWNING);
        try (var config = new Config(helper)) {
            config.select(PufferfishMobConfig.Kind.IRON);
            var caller = caller(helper, "heavy");
            try {
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, helper.getLevel().getServer());
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).isEmpty(), "Reinforcement ignored doMobSpawning");
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(true, helper.getLevel().getServer());
                PufferfishMobConfig.REINFORCEMENT_CHANCE.set(0.0);
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).isEmpty(), "Disabled reinforcement chance still spawned fish");
                PufferfishMobConfig.REINFORCEMENT_CHANCE.set(1.0);
                var neighbor = caller(helper, "iron");
                PufferfishMobConfig.REINFORCEMENT_NEARBY_LIMIT.set(1);
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).size() == 1 && save(caller).getInt("FishReinforcementCalls") == 0,
                        "Nearby population limit was ignored or consumed a call");
                neighbor.discard();
                for (int x = 1; x < 15; x++) for (int z = 1; z < 15; z++) for (int y = 1; y < 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
                hit(caller, attacker);
                helper.assertTrue(recruits(caller).isEmpty() && save(caller).getInt("FishReinforcementCalls") == 0,
                        "Blocked spawn positions created a fish or consumed a call");
                caller.invulnerableTime = 0;
                caller.hurt(caller.damageSources().mobAttack(attacker), 1000);
                helper.assertTrue(recruits(caller).isEmpty(), "Dead elite summoned reinforcements");
            } finally { recruits(caller).forEach(MaterialPufferfish::discard); caller.discard(); }
        } finally { rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(spawning, helper.getLevel().getServer()); }
        helper.succeed();
    }
}
