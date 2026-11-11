// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCurseGameTests {
    @GameTest(template = "empty")
    public static void everyRareFishConvertsPowerOnlyOnSuccessfulAttacks(GameTestHelper helper) {
        var ancient = ancientAura(helper);
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var packets = new ArrayList<Packet<?>>();
            var player = player(helper, packets);
            var fish = variant.entity().get().create(helper.getLevel());
            player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 123));
            player.hurt(player.damageSources().mobAttack(fish), 1);
            var curse = player.getEffect(PufferfishEffects.PUFFERFISH_CURSE);
            helper.assertTrue(curse != null && curse.getDuration() == 260 && curse.getAmplifier() == 0 && curse.isAmbient()
                    && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Rare fish did not convert power: " + variant.fish().getId());
            helper.assertTrue(!MobEffectUtil.hasWaterBreathing(player) && !MobEffectUtil.hasDigSpeed(player),
                    "Cursed player retained conduit abilities");
            var packet = packets.stream().filter(value -> value instanceof ClientboundLevelParticlesPacket particle
                    && particle.getParticle().getType() == PufferfishEffects.CURSE_APPEARANCE.get()).findFirst().orElseThrow();
            var options = (net.minecraft.core.particles.ItemParticleOption) ((ClientboundLevelParticlesPacket) packet).getParticle();
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                var codec = PufferfishEffects.CURSE_APPEARANCE.get().streamCodec();
                codec.encode(buffer, options);
                var decoded = codec.decode(buffer);
                helper.assertTrue(ItemStack.isSameItemSameComponents(fish.fish(), decoded.getItem()),
                        "Apparition packet lost its attacking fish variant: " + variant.fish().getId());
            } finally {
                buffer.release();
            }
            var unempowered = player(helper, new ArrayList<>());
            unempowered.hurt(unempowered.damageSources().mobAttack(fish), 1);
            helper.assertTrue(unempowered.hasEffect(PufferfishEffects.PUFFERFISH_CURSE)
                    && unempowered.getEffect(PufferfishEffects.PUFFERFISH_CURSE).getDuration() == 260,
                    "Material fish required power to curse: " + variant.fish().getId());
        }
        var fish = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(helper.getLevel());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 82, 2)).getCenter());
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Unempowered player was not cursed");
        player.removeEffect(PufferfishEffects.PUFFERFISH_CURSE);
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        helper.assertTrue(!player.hurt(player.damageSources().mobAttack(fish), 1), "Invulnerability fixture failed");
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Rejected attack converted power");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().thorns(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Thorns converted power");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(EntityType.PUFFERFISH.create(helper.getLevel())), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE)
                && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Ordinary fish did not convert power");
        player.removeEffect(PufferfishEffects.PUFFERFISH_CURSE);
        player.invulnerableTime = 0;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 260));
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Absorbed attack did not curse an unempowered player");
        helper.getLevel().removeBlock(ancient, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryAndDyedFishStingsCurseWithoutPower(GameTestHelper helper) {
        var ancient = ancientAura(helper);
        var colors = DyeColor.values();
        for (int i = -1; i < colors.length; i++) {
            var packets = new ArrayList<Packet<?>>();
            var player = player(helper, packets);
            var fish = EntityType.PUFFERFISH.create(helper.getLevel());
            fish.setPuffState(2);
            if (i >= 0) PufferfishColors.set(fish, colors[i]);
            fish.playerTouch(player);
            var curse = player.getEffect(PufferfishEffects.PUFFERFISH_CURSE);
            helper.assertTrue(curse != null && curse.getDuration() == 260 && curse.getAmplifier() == 0,
                    "Ordinary or dyed fish sting did not curse an unempowered player");
            helper.assertTrue(apparitions(packets) == 1, "Fish sting did not send exactly one apparition");
            var packet = packets.stream().filter(value -> value instanceof ClientboundLevelParticlesPacket particle
                    && particle.getParticle().getType() == PufferfishEffects.CURSE_APPEARANCE.get()).findFirst().orElseThrow();
            var appearance = ((net.minecraft.core.particles.ItemParticleOption) ((ClientboundLevelParticlesPacket) packet).getParticle()).getItem();
            helper.assertTrue(appearance.is(Items.PUFFERFISH) && PufferfishColors.get(appearance) == (i < 0 ? null : colors[i]),
                    "Curse apparition lost the attacking fish color");
        }
        var player = player(helper, new ArrayList<>());
        player.hurt(player.damageSources().mobAttack(EntityType.ZOMBIE.create(helper.getLevel())), 1);
        helper.assertTrue(!player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Non-pufferfish attack triggered a curse");
        helper.getLevel().removeBlock(ancient, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curseMatchesVanillaFatigueAndWeaknessAndExpiresCleanly(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 82, 2)).getCenter());
        player.setOnGround(true);
        player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
        float speed = player.getDestroySpeed(Blocks.STONE.defaultBlockState());
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 260));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 260));
        float vanillaSpeed = player.getDestroySpeed(Blocks.STONE.defaultBlockState());
        double vanillaDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_CURSE, 2));
        helper.assertTrue(Math.abs(player.getDestroySpeed(Blocks.STONE.defaultBlockState()) - vanillaSpeed) < 0.00001
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == vanillaDamage && vanillaDamage == 6,
                "Curse differs from Mining Fatigue I and Weakness I");
        helper.assertTrue(player.getActiveEffects().size() == 1, "Curse created additional status effects");
        player.tick();
        player.tick();
        helper.assertTrue(!player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE)
                && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == 10, "Curse attributes survived expiry");
        player.setOnGround(true);
        helper.assertTrue(Math.abs(player.getDestroySpeed(Blocks.STONE.defaultBlockState()) - speed) < 0.00001,
                "Mining speed did not recover after expiry");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coreRenewsCurseUntilCuredWithoutRepeatingTheApparition(GameTestHelper helper) {
        var ancient = ancientAura(helper);
        var packets = new ArrayList<Packet<?>>();
        var player = player(helper, packets);
        var fish = PufferfishMobs.VARIANTS.get("iron_pufferfish").entity().get().create(helper.getLevel());
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 80));
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Server player was not cursed");
        helper.assertTrue(apparitions(packets) == 1, "Initial conversion did not send exactly one apparition");
        helper.getLevel().addFreshEntity(player);
        var pos = player.blockPosition();
        for (int i = 0; i < 3; i++) {
            player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).tick(player, () -> {});
            helper.assertTrue(player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).getDuration() < 260, "Curse did not count down");
            PufferfishConduitBlockEntity.applyPower(helper.getLevel(), pos, 16);
            helper.assertTrue(player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).getDuration() == 260
                    && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Core restored power instead of renewing curse");
        }
        player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).tick(player, () -> {});
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).getDuration() == 260,
                "Repeated attack did not refresh the curse to thirteen seconds");
        helper.assertTrue(apparitions(packets) == 1, "Refresh or repeated attack replayed the apparition");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), helper.getLevel(), player);
        PufferfishConduitBlockEntity.applyPower(helper.getLevel(), pos, 16);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                && !player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Milk did not restore normal core behavior");
        player.discard();
        helper.getLevel().removeBlock(ancient, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void canceledCursePreservesPowerAndAttacksUseFixedDuration(GameTestHelper helper) {
        var ancient = ancientAura(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(new BlockPos(2, 82, 2)).getCenter());
        var fish = PufferfishMobs.VARIANTS.get("diamond_pufferfish").entity().get().create(helper.getLevel());
        java.util.function.Consumer<MobEffectEvent.Applicable> listener = event -> {
            if (event.getEntity() == player && event.getEffectInstance().is(PufferfishEffects.PUFFERFISH_CURSE)) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        };
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, -1));
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            player.hurt(player.damageSources().mobAttack(fish), 1);
            helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                    && !player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Canceled curse removed power");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        for (int duration : new int[] {-1, 1200}) {
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, duration));
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().mobAttack(fish), 1);
            var curse = player.getEffect(PufferfishEffects.PUFFERFISH_CURSE);
            helper.assertTrue(curse != null && curse.getDuration() == 260 && curse.isAmbient()
                    && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Curse inherited power duration or retained power");
        }
        helper.getLevel().removeBlock(ancient, false);
        helper.succeed();
    }

    private static BlockPos ancientAura(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 82, 2));
        helper.getLevel().setBlockAndUpdate(pos, PufferfishBlocks.ANCIENT_CONDUIT.get().defaultBlockState());
        PufferfishConduitSpawning.update(helper.getLevel(), pos, 32);
        return pos;
    }

    private static long apparitions(ArrayList<Packet<?>> packets) {
        return packets.stream().filter(packet -> packet instanceof ClientboundLevelParticlesPacket particle
                && particle.getParticle().getType() == PufferfishEffects.CURSE_APPEARANCE.get()).count();
    }

    private static ServerPlayer player(GameTestHelper helper, ArrayList<Packet<?>> packets) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "curse-test"), ClientInformation.createDefault()) {
            @Override public boolean isInWaterOrRain() { return true; }
        };
        TestPlayers.connect(player, packets::add);
        TestPlayers.allowDamage(player);
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(2, 82, 2)).getCenter());
        return player;
    }
}
