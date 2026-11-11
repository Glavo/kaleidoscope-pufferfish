// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCurseGameTests {
    @GameTest(template = "empty")
    public static void everyRareFishConvertsPowerOnlyOnSuccessfulAttacks(GameTestHelper helper) {
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var packets = new ArrayList<Packet<?>>();
            var player = player(helper, packets);
            var fish = variant.entity().get().create(helper.getLevel());
            player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 123));
            player.hurt(player.damageSources().mobAttack(fish), 1);
            var curse = player.getEffect(PufferfishEffects.PUFFERFISH_CURSE);
            helper.assertTrue(curse != null && curse.getDuration() == 123 && curse.getAmplifier() == 0
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
        }
        var fish = PufferfishMobs.VARIANTS.get("copper_pufferfish").entity().get().create(helper.getLevel());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(!player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Unempowered player was cursed");
        player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_POWER, 260));
        helper.assertTrue(!player.hurt(player.damageSources().mobAttack(fish), 1), "Invulnerability fixture failed");
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Rejected attack converted power");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().thorns(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Thorns converted power");
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(EntityType.PUFFERFISH.create(helper.getLevel())), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Ordinary fish converted power");
        player.invulnerableTime = 0;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 260));
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Absorbed attack did not convert power");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void curseMatchesVanillaFatigueAndWeaknessAndExpiresCleanly(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
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
            PufferfishConduitBlockEntity.applyPower(helper.getLevel(), pos, 16);
            helper.assertTrue(player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).getDuration() == 260
                    && !player.hasEffect(PufferfishEffects.PUFFERFISH_POWER), "Core restored power instead of renewing curse");
        }
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(apparitions(packets) == 1, "Refresh or repeated attack replayed the apparition");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), helper.getLevel(), player);
        PufferfishConduitBlockEntity.applyPower(helper.getLevel(), pos, 16);
        helper.assertTrue(player.hasEffect(PufferfishEffects.PUFFERFISH_POWER)
                && !player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE), "Milk did not restore normal core behavior");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void canceledCursePreservesPowerAndInfinitePowerStaysInfinite(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
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
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(fish), 1);
        helper.assertTrue(player.getEffect(PufferfishEffects.PUFFERFISH_CURSE).isInfiniteDuration(), "Infinite power lost its duration");
        helper.succeed();
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
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND),
                player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(Packet<?> packet) { packets.add(packet); }
        };
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not remove spawn invulnerability", exception);
        }
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
        return player;
    }
}
