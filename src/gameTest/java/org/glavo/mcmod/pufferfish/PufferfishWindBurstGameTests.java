// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishWindBurstGameTests {
    private static ItemStack enchant(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int rank) {
        var holder = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        EnchantmentHelper.updateEnchantments(stack, entries -> entries.set(holder, rank));
        return stack;
    }

    private static List<ItemStack> fish() {
        var stacks = new ArrayList<ItemStack>();
        stacks.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) stacks.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        stacks.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        stacks.add(new ItemStack(PufferfishItems.HEAVY.get()));
        return stacks;
    }

    private static ThrownPufferfish throwFish(GameTestHelper helper, Player owner, ItemStack stack, Vec3 from, Vec3 velocity) {
        var projectile = new ThrownPufferfish(helper.getLevel(), owner);
        projectile.setItem(stack);
        projectile.setPos(from);
        projectile.setDeltaMovement(velocity);
        helper.getLevel().addFreshEntity(projectile);
        projectile.tick();
        return projectile;
    }

    private static void clearDrops(GameTestHelper helper, Vec3 center) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(5)).forEach(ItemEntity::discard);
    }

    @GameTest(template = "empty")
    public static void everyFishHasOneScalingGustWithoutExtraDamage(GameTestHelper helper) {
        int[] bursts = {0};
        Consumer<ExplosionEvent.Start> capture = event -> {
            if (event.getExplosion().getDirectSourceEntity() instanceof ThrownPufferfish) {
                bursts[0]++;
                helper.assertTrue(event.getExplosion().radius() == 1.2F
                        && event.getExplosion().getBlockInteraction() == Explosion.BlockInteraction.KEEP,
                        "Fish gust changed its radius or block protection");
            }
        };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            for (var original : fish()) {
                double previous = 0;
                double baseSpeed = 0;
                for (int rank = 0; rank <= 3; rank++) {
                    var stack = original.copy();
                    if (rank > 0) enchant(helper, stack, Enchantments.WIND_BURST, rank);
                    var owner = helper.makeMockPlayer(GameType.SURVIVAL);
                    owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 0))));
                    var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 3));
                    var nearby = helper.spawn(EntityType.VILLAGER, new BlockPos(3, 2, 3));
                    var distant = helper.spawn(EntityType.VILLAGER, new BlockPos(6, 2, 3));
                    for (var mob : List.of(target, nearby, distant)) { mob.setNoAi(true); mob.setDeltaMovement(Vec3.ZERO); }
                    int before = bursts[0];
                    var projectile = throwFish(helper, owner, stack, target.position().add(0, 0.5, -2), new Vec3(0, 0, 3));
                    helper.assertTrue(bursts[0] - before == (rank > 0 ? 1 : 0) && projectile.isRemoved(),
                            "An impact did not trigger exactly one gust and recover the fish");
                    helper.assertTrue(target.getHealth() == 20 - PufferfishCombat.damage(stack)
                            && PufferfishEffectsGameTests.poison(target) != null && nearby.getHealth() == 20,
                            "Gust changed direct damage, poison, or bystander health");
                    double speed = nearby.getDeltaMovement().length();
                    helper.assertTrue(rank == 0 ? speed == 0 : speed > previous,
                            "Wind Burst levels did not progressively increase knockback");
                    if (rank == 1) baseSpeed = speed;
                    if (rank > 1) helper.assertTrue(Math.abs(speed / baseSpeed - (rank == 2 ? 1.22 : 1.40)) < 0.0001,
                            "Wind Burst knockback did not match the configured level strength");
                    helper.assertTrue(distant.getDeltaMovement().lengthSqr() == 0, "Higher gust levels enlarged the range");
                    previous = speed;
                    var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(4));
                    helper.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.getFirst().getItem(), PufferfishDurabilityGameTests.worn(stack)),
                            "Gust lost or changed the recovered fish");
                    drops.forEach(ItemEntity::discard);
                    target.discard(); nearby.discard(); distant.discard();
                }
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
        }
        helper.succeed();
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper, Vec3 position) {
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "gust-test-player"), ClientInformation.createDefault());
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), new Connection(PacketFlow.SERVERBOUND),
                player, CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(Packet<?> packet) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(position);
        for (int tick = 0; tick < 61; tick++) player.tick();
        player.setPos(position);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    @GameTest(template = "empty")
    public static void groundGustLaunchesOwnerWithNativeLandingProtection(GameTestHelper helper) {
        var block = new BlockPos(2, 1, 2);
        helper.setBlock(block, Blocks.OAK_PLANKS);
        var position = Vec3.atBottomCenterOf(helper.absolutePos(block.above())).add(0, 0.1, 0);
        double previous = 0;
        for (int rank = 1; rank <= 3; rank++) {
            var owner = serverPlayer(helper, position);
            try {
                var stack = enchant(helper, new ItemStack(Items.PUFFERFISH), Enchantments.WIND_BURST, rank);
                throwFish(helper, owner, stack, position.add(0, 1, 0), new Vec3(0, -2, 0));
                helper.assertTrue(owner.getDeltaMovement().y > previous && owner.getHealth() == 20,
                        "Ground gust failed to launch its owner without damage");
                previous = owner.getDeltaMovement().y;
                helper.assertTrue(owner.isIgnoringFallDamageFromCurrentImpulse() && position.equals(owner.currentImpulseImpactPos),
                        "Gust did not retain native landing-height protection");
                helper.assertTrue(!owner.causeFallDamage(20, 1, owner.damageSources().fall()) && owner.getHealth() == 20,
                        "Returning to the launch height caused fall damage");
                owner.setPos(position.add(0, -6, 0));
                helper.assertTrue(owner.causeFallDamage(20, 1, owner.damageSources().fall()) && owner.getHealth() == 17,
                        "Gust incorrectly protected falls below the launch height");
                helper.assertBlockPresent(Blocks.OAK_PLANKS, block);
                helper.assertBlockPresent(Blocks.AIR, block.above());
            } finally {
                clearDrops(helper, position);
                owner.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void windBurstAndChannelingKeepLoyaltyAndDoNotRepeatOnReturn(GameTestHelper helper) {
        var level = helper.getLevel();
        float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
        level.setRainLevel(1); level.setThunderLevel(1);
        int[] bursts = {0};
        Consumer<ExplosionEvent.Start> capture = event -> { bursts[0]++; };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            for (var original : fish()) {
                var stack = enchant(helper, original.copy(), Enchantments.WIND_BURST, 3);
                enchant(helper, stack, Enchantments.LOYALTY, 3);
                enchant(helper, stack, Enchantments.CHANNELING, 1);
                var owner = helper.makeMockPlayer(GameType.SURVIVAL);
                owner.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 0))));
                var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 3));
                target.setNoAi(true);
                int before = bursts[0];
                var projectile = throwFish(helper, owner, stack, target.position().add(0, 0.5, -2), new Vec3(0, 0, 3));
                var bolts = level.getEntitiesOfClass(LightningBolt.class, target.getBoundingBox().inflate(4));
                helper.assertTrue(bolts.size() == 1 && bursts[0] == before + 1 && projectile.isReturning(),
                        "Channeling, Wind Burst, and Loyalty did not trigger together");
                bolts.forEach(LightningBolt::discard);
                for (int tick = 0; tick < 100 && !projectile.isRemoved(); tick++) projectile.tick();
                helper.assertTrue(projectile.isRemoved() && bursts[0] == before + 1
                        && ItemStack.isSameItemSameComponents(owner.getInventory().getItem(0), PufferfishDurabilityGameTests.worn(stack)),
                        "Return flight repeated the gust or lost enchanted fish");
                target.discard();
                clearDrops(helper, target.position());
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
            level.setRainLevel(rain); level.setThunderLevel(thunder);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void canceledGustPreservesRecoveryWithoutKnockback(GameTestHelper helper) {
        Consumer<ExplosionEvent.Start> cancel = event -> {
            if (event.getExplosion().getDirectSourceEntity() instanceof ThrownPufferfish) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            var owner = helper.makeMockPlayer(GameType.SURVIVAL);
            var stack = enchant(helper, new ItemStack(Items.PUFFERFISH), Enchantments.WIND_BURST, 3);
            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 3));
            target.setNoAi(true);
            target.setInvulnerable(true);
            target.setDeltaMovement(Vec3.ZERO);
            var projectile = throwFish(helper, owner, stack, target.position().add(0, 0.5, -2), new Vec3(0, 0, 3));
            helper.assertTrue(projectile.isRemoved() && target.getDeltaMovement().lengthSqr() == 0 && target.getHealth() == 20,
                    "Canceled gust applied knockback or prevented recovery");
            clearDrops(helper, target.position());
            target.discard();
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.succeed();
    }
}
