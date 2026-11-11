// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishPowerGameTests {
    @GameTest(template = "empty")
    public static void playersReceiveOnlyConduitAbilitiesAndMilkCuresPower(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        helper.assertTrue(MobEffectUtil.hasWaterBreathing(player) && MobEffectUtil.hasDigSpeed(player)
                && MobEffectUtil.getDigSpeedAmplification(player) == 0, "Power did not grant conduit abilities");
        helper.assertTrue(!player.hasEffect(MobEffects.CONDUIT_POWER) && player.getActiveEffects().size() == 1,
                "Power created extra status effects");
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1));
        helper.assertTrue(MobEffectUtil.getDigSpeedAmplification(player) == 1, "Power suppressed stronger Haste");
        player.removeEffect(MobEffects.DIG_SPEED);
        player.setHealth(10);
        player.tickCount = 50;
        PufferfishEffects.PUFFERFISH_POWER.get().applyEffectTick(player, 0);
        helper.assertTrue(player.getHealth() == 10, "Power regenerated player health");
        player.hurt(player.damageSources().magic(), 5);
        helper.assertTrue(player.getHealth() == 5, "Power granted player damage resistance");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), helper.getLevel(), player);
        helper.assertTrue(!MobEffectUtil.hasWaterBreathing(player) && !MobEffectUtil.hasDigSpeed(player)
                && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Power survived milk");
        var cow = EntityType.COW.create(helper.getLevel());
        cow.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        helper.assertTrue(!MobEffectUtil.hasWaterBreathing(cow) && !MobEffectUtil.hasDigSpeed(cow), "Power empowered unrelated mobs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void auraReachesEveryFishVariantButNotOtherMobsOrDistantFish(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var fish = new ArrayList<Pufferfish>();
        fish.add(EntityType.PUFFERFISH.create(level));
        for (var color : DyeColor.values()) {
            var dyed = EntityType.PUFFERFISH.create(level);
            PufferfishColors.set(dyed, color);
            fish.add(dyed);
        }
        for (var variant : PufferfishMobs.VARIANTS.values()) fish.add(variant.entity().get().create(level));
        for (var target : fish) {
            target.setPos(Vec3.atCenterOf(pos.above()));
            target.setNoAi(true);
            level.addFreshEntity(target);
        }
        var distant = helper.spawn(EntityType.PUFFERFISH, new BlockPos(2, 3, 2));
        distant.setPos(Vec3.atCenterOf(pos.east(32)));
        var cod = helper.spawn(EntityType.COD, new BlockPos(2, 3, 2));
        PufferfishConduitBlockEntity.applyPower(level, pos, 16);
        for (var target : fish) {
            var power = target.getEffect(PufferfishEffects.PUFFERFISH_POWER);
            helper.assertTrue(power != null && power.getDuration() == 260 && power.getAmplifier() == 0,
                    "Fish variant did not receive power: " + target.getType());
            target.discard();
        }
        helper.assertTrue(!cod.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                && !distant.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Aura empowered unrelated or distant fish");
        cod.discard();
        distant.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishGainRegenerationResistanceAndStrengthWithoutStacking(GameTestHelper helper) {
        var fish = new ArrayList<Pufferfish>();
        fish.add(EntityType.PUFFERFISH.create(helper.getLevel()));
        for (var variant : PufferfishMobs.VARIANTS.values()) fish.add(variant.entity().get().create(helper.getLevel()));
        for (var target : fish) {
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setHealth(50);
            target.setNoAi(true);
            target.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
            for (int tick = 1; tick <= 100; tick++) {
                target.tickCount = tick;
                if (tick % 40 == 0) target.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
                PufferfishEffects.PUFFERFISH_POWER.get().applyEffectTick(target, 0);
            }
            helper.assertTrue(target.getHealth() == 52, "Aura refresh changed regeneration rate");
            target.hurt(target.damageSources().magic(), 10);
            helper.assertTrue(Math.abs(target.getHealth() - 44) < 0.001, "Power did not reduce damage by twenty percent");
            target.invulnerableTime = 0;
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            target.hurt(target.damageSources().magic(), 10);
            helper.assertTrue(Math.abs(target.getHealth() - 38) < 0.001, "Power stacked with stronger Resistance");
            target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            target.invulnerableTime = 0;
            target.hurt(target.damageSources().fellOutOfWorld(), 10);
            helper.assertTrue(Math.abs(target.getHealth() - 28) < 0.001, "Power resisted bypass damage");
            var victim = EntityType.COW.create(helper.getLevel());
            victim.hurt(victim.damageSources().mobAttack(target), 2);
            helper.assertTrue(victim.getHealth() == 5, "Power did not add three points of attack damage");
            target.removeAllEffects();
            target.invulnerableTime = 0;
            target.hurt(target.damageSources().magic(), 10);
            helper.assertTrue(Math.abs(target.getHealth() - 18) < 0.001, "Resistance persisted after power was removed");
            var unbuffedVictim = EntityType.COW.create(helper.getLevel());
            unbuffedVictim.hurt(unbuffedVictim.damageSources().mobAttack(target), 2);
            helper.assertTrue(unbuffedVictim.getHealth() == 8, "Strength persisted after power was removed");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void vanillaConduitStillGrantsVanillaPower(GameTestHelper helper) {
        var center = new BlockPos(2, 3, 2);
        var pos = helper.absolutePos(center);
        for (var water : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            helper.getLevel().setBlock(water, Blocks.WATER.defaultBlockState(), 2);
        }
        helper.setBlock(center, Blocks.CONDUIT);
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
            if (Math.abs(x) == 2 || Math.abs(y) == 2) helper.setBlock(center.offset(x, y, 0), Blocks.PRISMARINE);
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(pos.east()));
        player.setNoGravity(true);
        helper.getLevel().addFreshEntity(player);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(((ConduitBlockEntity) helper.getLevel().getBlockEntity(pos)).isActive(), "Vanilla conduit did not activate");
            helper.assertTrue(player.hasEffect(MobEffects.CONDUIT_POWER), "Vanilla conduit lost its effect");
            player.discard();
            helper.succeed();
        });
    }
}
