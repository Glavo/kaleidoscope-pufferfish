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
            float health = target.getHealth();
            player.attack(target);
            helper.assertTrue(health - target.getHealth() == 8, "Radiant melee should deal eight base damage");
            assertGlowing(helper, target);
            var effect = target.getEffect(MobEffects.WITHER);
            helper.assertTrue(effect != null && effect.getAmplifier() == Math.min(hit, 9) && effect.getDuration() == 100
                    && !target.hasEffect(MobEffects.POISON), "Melee wither did not advance exactly once or exceeded level ten");
            var blindness = target.getEffect(MobEffects.BLINDNESS);
            helper.assertTrue(hit < 10 ? blindness == null : blindness != null && blindness.getDuration() == 60 && blindness.getAmplifier() == 0,
                    "Blindness should start only on hits against existing wither ten");
        }
        target.removeAllEffects();
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
        target.invulnerableTime = 0;
        var projectile = new ThrownPufferfish(helper.getLevel(), player);
        projectile.setItem(fish);
        projectile.setPos(target.getX() - 1, target.getY() + 0.5, target.getZ());
        projectile.setDeltaMovement(2, 0, 0);
        float health = target.getHealth();
        projectile.tick();
        helper.assertTrue(health - target.getHealth() == 8, "Radiant throws should deal eight base damage");
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
    public static void cappedWitherBlindsOnThrowsAndFlailsButNotRetaliation(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 2));
        target.setNoAi(true);
        for (var weapon : new net.minecraft.world.item.Item[] {PufferfishItems.RADIANT.get(), PufferfishItems.RADIANT_FLAIL.get()}) {
            var stack = new ItemStack(weapon);
            target.removeAllEffects();
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 9));
            PufferfishCombat.applyPassiveEffect(stack, target, player);
            helper.assertTrue(!target.hasEffect(MobEffects.BLINDNESS), "Passive radiant effects must not blind");
            if (PufferfishItems.isFlail(stack)) {
                PufferfishCombat.hitEffects(stack, target, player, Vec3.ZERO);
            } else {
                var projectile = new ThrownPufferfish(helper.getLevel(), player);
                projectile.setItem(stack);
                projectile.setPos(target.getX() - 1, target.getY() + 0.5, target.getZ());
                projectile.setDeltaMovement(2, 0, 0);
                projectile.tick();
            }
            var blindness = target.getEffect(MobEffects.BLINDNESS);
            helper.assertTrue(blindness != null && blindness.getAmplifier() == 0 && blindness.getDuration() == 60,
                    "Active radiant hits against wither ten should blind for three seconds");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void headgearArmorAndShieldApplyWitherOneOnlyAfterSuccessfulThorns(GameTestHelper helper) {
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
            int reflected = 0;
            for (int hit = 0; hit < 64; hit++) {
                player.setHealth(20);
                player.invulnerableTime = 0;
                for (var slot : EquipmentSlot.values()) {
                    var stack = player.getItemBySlot(slot);
                    if (stack.isDamageableItem()) stack.setDamageValue(0);
                }
                attacker.setHealth(1000);
                attacker.invulnerableTime = 0;
                attacker.removeAllEffects();
                player.hurt(player.damageSources().mobAttack(attacker), 2);
                if (attacker.getHealth() < 1000) {
                    reflected++;
                    assertGlowing(helper, attacker);
                    var wither = attacker.getEffect(MobEffects.WITHER);
                    helper.assertTrue(wither != null && wither.getAmplifier() == 0 && wither.getDuration() == 100,
                            "Successful radiant thorns did not apply five seconds of Wither I: " + variant);
                } else {
                    helper.assertTrue(!attacker.hasEffect(MobEffects.WITHER) && !attacker.hasEffect(MobEffects.GLOWING),
                            "Radiant effects triggered without thorns damage: " + variant);
                }
                helper.assertTrue(!attacker.hasEffect(MobEffects.POISON), "Radiant equipment applied poison");
            }
            helper.assertTrue(reflected > 0 && reflected < 64, "Radiant retaliation did not roll thorns: " + variant);
            attacker.removeAllEffects();
            attacker.invulnerableTime = 0;
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> cancel = event -> {
                if (event.getEntity() == attacker && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                attacker.hurt(attacker.damageSources().thorns(player), 1);
                helper.assertTrue(!attacker.hasEffect(MobEffects.WITHER) && !attacker.hasEffect(MobEffects.GLOWING),
                        "Canceled radiant thorns applied effects");
            } finally {
                NeoForge.EVENT_BUS.unregister(cancel);
            }
            attacker.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(4);
            attacker.setHealth(1000);
            attacker.setAbsorptionAmount(4);
            for (int hit = 0; hit < 3; hit++) {
                attacker.invulnerableTime = 0;
                attacker.hurt(attacker.damageSources().thorns(player), 1);
                helper.assertTrue(attacker.getHealth() == 1000 && attacker.getAbsorptionAmount() == 3 - hit
                        && attacker.getEffect(MobEffects.WITHER) != null && attacker.getEffect(MobEffects.WITHER).getAmplifier() == 0,
                        "Absorbed radiant thorns failed to apply fixed Wither I");
                assertGlowing(helper, attacker);
            }
            attacker.setAbsorptionAmount(0);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void radiantFishAndSkinApplyContactWitherOne(GameTestHelper helper) {
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
                helper.assertTrue(target.hasEffect(MobEffects.WITHER) && target.getEffect(MobEffects.WITHER).getAmplifier() == 0 && target.getEffect(MobEffects.WITHER).getDuration() == 100
                        && !target.hasEffect(MobEffects.POISON), "Contact should apply fixed wither one");
            }
        }
        helper.succeed();
    }
}
