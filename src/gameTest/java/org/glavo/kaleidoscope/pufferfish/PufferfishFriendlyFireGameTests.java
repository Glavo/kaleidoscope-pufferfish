// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishFriendlyFireGameTests {
    private static ArrayList<Pufferfish> fishes(GameTestHelper helper) {
        var fishes = new ArrayList<Pufferfish>();
        fishes.add(EntityType.PUFFERFISH.create(helper.getLevel()));
        for (var color : DyeColor.values()) {
            var fish = EntityType.PUFFERFISH.create(helper.getLevel());
            PufferfishColors.set(fish, color);
            fishes.add(fish);
        }
        for (var variant : PufferfishMobs.VARIANTS.values()) fishes.add(variant.entity().get().create(helper.getLevel()));
        for (var fish : fishes) {
            fish.setNoAi(true);
            fish.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        }
        return fishes;
    }

    @GameTest(template = "empty")
    public static void everyFishRejectsDamageAndEffectsFromOtherFish(GameTestHelper helper) {
        var fishes = fishes(helper);
        var arrow = EntityType.ARROW.create(helper.getLevel());
        for (var target : fishes) for (var attacker : fishes) {
            if (target == attacker) continue;
            float health = target.getHealth();
            var source = helper.getLevel().damageSources().mobAttack(attacker);
            helper.assertTrue(!target.hurt(source, 2) && target.getHealth() == health,
                    "Fish accepted direct damage from another fish");
            helper.assertTrue(!target.hurt(new net.minecraft.world.damagesource.DamageSource(source.typeHolder(), arrow, attacker), 2)
                    && target.getHealth() == health, "Fish accepted indirect damage attributed to another fish");
            for (var effect : java.util.List.of(MobEffects.POISON, MobEffects.WITHER, MobEffects.GLOWING, MobEffects.BLINDNESS, MobEffects.REGENERATION)) {
                helper.assertTrue(!target.addEffect(new MobEffectInstance(effect, 100), attacker) && !target.hasEffect(effect),
                        "Fish accepted a status effect from another fish");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishImmunityPreservesUnrelatedDamageAndExistingEffects(GameTestHelper helper) {
        var other = EntityType.PIG.create(helper.getLevel());
        var attacker = EntityType.PUFFERFISH.create(helper.getLevel());
        for (var target : fishes(helper)) {
            helper.assertTrue(target.hurt(helper.getLevel().damageSources().mobAttack(other), 1),
                    "Fish became immune to attacks from unrelated mobs");
            helper.assertTrue(target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200), other),
                    "Fish rejected a status effect from an unrelated mob");
            helper.assertTrue(!target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 1), attacker)
                    && target.getEffect(MobEffects.BLINDNESS).getDuration() == 200
                    && target.getEffect(MobEffects.BLINDNESS).getAmplifier() == 0,
                    "Rejected fish effect replaced or removed an existing effect");
            helper.assertTrue(target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100)),
                    "Fish rejected a source-free beneficial effect");
        }
        helper.succeed();
    }
}
