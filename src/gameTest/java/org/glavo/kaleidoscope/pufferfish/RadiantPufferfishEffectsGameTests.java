// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0
package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class RadiantPufferfishEffectsGameTests {
    private static void assertGlowing(GameTestHelper helper, net.minecraft.world.entity.LivingEntity target) {
        var glow = target.getEffect(MobEffects.GLOWING);
        helper.assertTrue(glow != null && glow.getDuration() == 200 && glow.getAmplifier() == 0, "Radiant damage did not apply ten seconds of glowing");
    }

    @GameTest(template = "empty")
    public static void meleeEscalatesWitherAndProjectilesContinueIt(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        var fish = new ItemStack(PufferfishItems.RADIANT.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, fish);
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 2));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        for (int hit = 0; hit < 12; hit++) {
            target.invulnerableTime = 0;
            for (int tick = 0; tick < 25; tick++) player.tick();
            player.attack(target);
            assertGlowing(helper, target);
            var effect = target.getEffect(MobEffects.WITHER);
            helper.assertTrue(effect != null && effect.getAmplifier() == Math.min(hit, 9) && effect.getDuration() == 100
                    && !target.hasEffect(MobEffects.POISON), "Melee wither did not advance exactly once or exceeded level ten");
        }
        target.removeAllEffects();
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
        target.invulnerableTime = 0;
        var projectile = new ThrownPufferfish(helper.getLevel(), player);
        projectile.setItem(fish);
        projectile.setPos(target.getX() - 1, target.getY() + 0.5, target.getZ());
        projectile.setDeltaMovement(2, 0, 0);
        projectile.tick();
        assertGlowing(helper, target);
        helper.assertTrue(target.getEffect(MobEffects.WITHER).getAmplifier() == 2
                && target.getEffect(MobEffects.WITHER).getDuration() == 100, "Thrown fish did not escalate existing wither");
        target.removeAllEffects();
        PufferfishCombat.hitEffects(new ItemStack(PufferfishItems.RADIANT_FLAIL.get()), target, player, Vec3.ZERO);
        helper.assertTrue(target.getEffect(MobEffects.WITHER).getAmplifier() == 0, "Flail did not start with wither one");
        assertGlowing(helper, target);
        target.removeAllEffects();
        PufferfishCombat.hitEffects(new ItemStack(PufferfishItems.NETHERITE.get()), target, player, Vec3.ZERO);
        helper.assertTrue(!target.hasEffect(MobEffects.GLOWING), "Non-radiant attacks applied glowing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void headgearArmorAndShieldRetaliateWithFixedWitherTwo(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        attacker.setHealth(1000);
        for (var slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
        for (int variant = 0; variant < 6; variant++) {
            for (var slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
            player.stopUsingItem();
            attacker.removeAllEffects();
            if (variant == 0) player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PufferfishItems.RADIANT.get()));
            else if (variant < 5) {
                var type = PufferfishArmor.TYPES.get(variant - 1);
                player.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("radiant_pufferfish_skin_" + type.getName()).get()));
            } else {
                var shield = new ItemStack(PufferfishShields.VARIANTS.get("radiant_pufferfish_skin_shield").get());
                player.setYRot(0);
                player.setItemInHand(InteractionHand.MAIN_HAND, shield);
                shield.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            }
            for (int tick = 0; tick < 6; tick++) player.tick();
            for (int hit = 0; hit < 3; hit++) {
                player.setHealth(20);
                player.invulnerableTime = 0;
                player.hurt(player.damageSources().mobAttack(attacker), 2);
                assertGlowing(helper, attacker);
                helper.assertTrue(attacker.hasEffect(MobEffects.WITHER) && attacker.getEffect(MobEffects.WITHER).getAmplifier() == 1 && attacker.getEffect(MobEffects.WITHER).getDuration() == 100
                        && !attacker.hasEffect(MobEffects.POISON), "Passive retaliation escalated or applied poison: " + variant);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void radiantFishAndSkinApplyContactWitherTwo(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var block : new net.minecraft.world.level.block.Block[] {PufferfishBlocks.PLACED_FISH.get(), PufferfishBlocks.RADIANT_SKIN.get()}) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            if (level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity entity) entity.setFish(new ItemStack(PufferfishItems.RADIANT.get()));
            var target = EntityType.COW.create(level);
            target.setPos(Vec3.atBottomCenterOf(pos));
            for (int tick : new int[] {20, 40, 60}) {
                target.tickCount = tick;
                target.invulnerableTime = 0;
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target));
                assertGlowing(helper, target);
                helper.assertTrue(target.hasEffect(MobEffects.WITHER) && target.getEffect(MobEffects.WITHER).getAmplifier() == 1 && target.getEffect(MobEffects.WITHER).getDuration() == 100
                        && !target.hasEffect(MobEffects.POISON), "Contact should apply fixed wither two");
            }
        }
        helper.succeed();
    }
}
