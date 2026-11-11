// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0
package org.glavo.mcmod.pufferfish;

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
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class RadiantPufferfishEffectsGameTests {
    private static void assertGlowing(GameTestHelper helper, net.minecraft.world.entity.LivingEntity target) {
        var glow = target.getEffect(MobEffects.GLOWING);
        helper.assertTrue(glow != null && glow.getDuration() == 200 && glow.getAmplifier() == 0, "Radiant damage did not apply ten seconds of glowing");
    }

    private static final java.util.List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> BONUS_EFFECTS =
            java.util.List.of(MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.HUNGER, MobEffects.WEAKNESS);

    static int prepareHit(net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.Entity source, long seed) {
        var current = target.getEffect(MobEffects.WITHER);
        int previous = current == null ? -1 : current.getAmplifier();
        target.removeAllEffects();
        if (previous >= 0) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, previous));
        source.getRandom().setSeed(seed);
        return previous;
    }

    static void assertHit(GameTestHelper helper, net.minecraft.world.entity.LivingEntity target, int previous, long seed) {
        var random = net.minecraft.util.RandomSource.create(seed);
        int expected = previous;
        if (previous < 2 && random.nextFloat() < 0.6F) expected++;
        var wither = target.getEffect(MobEffects.WITHER);
        helper.assertTrue(expected < 0 ? wither == null : wither != null
                        && wither.getAmplifier() == expected && wither.getDuration() == 100,
                "Radiant hit did not roll a 60 percent increase or refresh existing wither: " + previous + ", seed=" + seed);
        for (int i = 0; i < BONUS_EFFECTS.size(); i++) {
            boolean triggered = previous >= 2 && random.nextFloat() < 0.05F;
            var effect = target.getEffect(BONUS_EFFECTS.get(i));
            helper.assertTrue(triggered ? effect != null && effect.getAmplifier() == 0 && effect.getDuration() == (i < 2 ? 100 : 200) : effect == null,
                    "Capped radiant hit did not independently roll bonus effect " + i + ", seed=" + seed);
        }
        assertGlowing(helper, target);
        helper.assertTrue(!target.hasEffect(MobEffects.POISON), "Radiant attack applied poison");
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
        var seeds = net.minecraft.util.RandomSource.create(46291);
        for (int hit = 0; hit < 12; hit++) {
            target.invulnerableTime = 0;
            for (int tick = 0; tick < 25; tick++) player.tick();
            long seed = seeds.nextLong();
            int previous = prepareHit(target, player, seed);
            float health = target.getHealth();
            player.attack(target);
            helper.assertTrue(health - target.getHealth() == 8, "Radiant melee should deal eight base damage");
            assertHit(helper, target, previous, seed);
        }
        target.removeAllEffects();
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
        target.invulnerableTime = 0;
        var projectile = new ThrownPufferfish(helper.getLevel(), player);
        projectile.setItem(fish);
        projectile.setPos(target.getX() - 1, target.getY() + 0.5, target.getZ());
        projectile.setDeltaMovement(2, 0, 0);
        long seed = seeds.nextLong();
        int previous = prepareHit(target, player, seed);
        float health = target.getHealth();
        projectile.tick();
        helper.assertTrue(health - target.getHealth() == 8, "Radiant throws should deal eight base damage");
        assertHit(helper, target, previous, seed);
        target.removeAllEffects();
        seed = seeds.nextLong();
        previous = prepareHit(target, player, seed);
        PufferfishCombat.hitEffects(new ItemStack(PufferfishItems.RADIANT_FLAIL.get()), target, player, Vec3.ZERO);
        assertHit(helper, target, previous, seed);
        target.removeAllEffects();
        PufferfishCombat.hitEffects(new ItemStack(PufferfishItems.NETHERITE.get()), target, player, Vec3.ZERO);
        helper.assertTrue(!target.hasEffect(MobEffects.GLOWING), "Non-radiant attacks applied glowing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void radiantProbabilityRefreshAndCapRespectAllStartingLevels(GameTestHelper helper) {
        var target = EntityType.COW.create(helper.getLevel());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var seeds = net.minecraft.util.RandomSource.create(70129);
        for (var item : new net.minecraft.world.item.Item[] {PufferfishItems.RADIANT.get(), PufferfishItems.RADIANT_FLAIL.get()}) {
            for (int previous = -1; previous <= 2; previous++) {
                int advanced = 0;
                for (int attempt = 0; attempt < 128; attempt++) {
                    target.removeAllEffects();
                    if (previous >= 0) target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, previous));
                    long seed = seeds.nextLong();
                    prepareHit(target, player, seed);
                    PufferfishCombat.hitEffects(new ItemStack(item), target, player, Vec3.ZERO);
                    assertHit(helper, target, previous, seed);
                    var wither = target.getEffect(MobEffects.WITHER);
                    if (wither != null && wither.getAmplifier() > previous) advanced++;
                }
                helper.assertTrue(previous == 2 ? advanced == 0 : advanced > 0 && advanced < 128,
                        "Radiant wither never failed, never advanced, or exceeded III");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cappedWitherRollsAllDebuffsIndependentlyButPassiveEffectsDoNot(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var target = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 2));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        var seeds = net.minecraft.util.RandomSource.create(92711);
        var combinations = new java.util.HashSet<Integer>();
        for (var weapon : new net.minecraft.world.item.Item[] {PufferfishItems.RADIANT.get(), PufferfishItems.RADIANT_FLAIL.get()}) {
            for (int attempt = 0; attempt < 256; attempt++) {
                long seed = seeds.nextLong();
                target.removeAllEffects();
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 2));
                prepareHit(target, player, seed);
                var stack = new ItemStack(weapon);
                PufferfishCombat.applyPassiveEffect(stack, target, player);
                helper.assertTrue(BONUS_EFFECTS.stream().noneMatch(target::hasEffect), "Passive radiant effects rolled bonus debuffs");
                target.setHealth(1000);
                target.invulnerableTime = 0;
                if (PufferfishItems.isFlail(stack)) {
                    PufferfishCombat.hitEffects(stack, target, player, Vec3.ZERO);
                } else {
                    var projectile = new ThrownPufferfish(helper.getLevel(), player);
                    projectile.setItem(stack);
                    projectile.setPos(target.getX() - 1, target.getY() + 0.5, target.getZ());
                    projectile.setDeltaMovement(2, 0, 0);
                    projectile.tick();
                }
                assertHit(helper, target, 2, seed);
                int mask = 0;
                for (int i = 0; i < BONUS_EFFECTS.size(); i++) if (target.hasEffect(BONUS_EFFECTS.get(i))) mask |= 1 << i;
                combinations.add(mask);
            }
        }
        helper.assertTrue(combinations.containsAll(java.util.List.of(0, 1, 2, 4, 8))
                        && combinations.stream().anyMatch(mask -> Integer.bitCount(mask) > 1),
                "Independent debuffs never occurred alone or together");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void headgearAndArmorApplyWitherOneOnlyAfterSuccessfulThorns(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        attacker.setHealth(1000);
        for (var slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
        for (int variant = 0; variant < 5; variant++) {
            for (var slot : EquipmentSlot.values()) player.setItemSlot(slot, ItemStack.EMPTY);
            player.stopUsingItem();
            attacker.removeAllEffects();
            if (variant == 0) player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PufferfishItems.RADIANT.get()));
            else if (variant < 5) {
                var type = PufferfishArmor.TYPES.get(variant - 1);
                player.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("radiant_pufferfish_skin_" + type.getName()).get()));
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
                    helper.assertTrue(wither != null && wither.getAmplifier() == 0 && wither.getDuration() == 120,
                            "Successful radiant thorns did not apply six seconds of Wither I: " + variant);
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
