// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.kaleidoscope.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public class PufferfishProjectileGameTests {
    /// Throws a fish through the interaction event from a stationary player in unobstructed air.
    private static ThrownPufferfish launch(GameTestHelper helper, ItemStack stack, InteractionHand hand) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 40, 2));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setOnGround(true);
        player.setXRot(0);
        player.setYRot(0);
        player.setItemInHand(hand, stack);
        var event = new PlayerInteractEvent.RightClickItem(player, hand);
        NeoForge.EVENT_BUS.post(event);
        var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
        helper.assertTrue(event.getCancellationResult().consumesAction() && projectiles.size() == 1,
                "Throw did not spawn exactly one projectile");
        return projectiles.getFirst();
    }

    /// Verifies netherite fish cover less horizontal distance under the same launch conditions.
    @GameTest(template = "empty")
    public static void netheriteThrowsHaveShorterFlightRange(GameTestHelper helper) {
        for (var hand : InteractionHand.values()) {
            double normalDistance = 0;
            for (boolean netherite : new boolean[] {false, true}) {
                var projectile = launch(helper, new ItemStack(netherite ? PufferfishItems.NETHERITE.get() : Items.PUFFERFISH), hand);
                double expectedSpeed = netherite ? 1.0 : 1.5;
                helper.assertTrue(Math.abs(projectile.getDeltaMovement().length() - expectedSpeed) < 0.06,
                        "Incorrect default launch speed");
                var start = projectile.position();
                for (int tick = 0; tick < 20; tick++) projectile.tick();
                helper.assertTrue(!projectile.isRemoved() && projectile.getY() < start.y, "Projectile did not follow its falling flight path");
                double distance = projectile.position().subtract(start).horizontalDistance();
                if (!netherite) normalDistance = distance;
                else helper.assertTrue(distance > 0 && distance < normalDistance * 0.8,
                        "Netherite fish did not fly a shorter distance");
                projectile.discard();
            }
        }
        helper.succeed();
    }

    /// Verifies independent speed settings are honored by both hands and all dyed fish use normal speed.
    @GameTest(template = "empty")
    public static void throwingSpeedsCanBeConfiguredIndependently(GameTestHelper helper) {
        double normal = PufferfishCombatConfig.NORMAL_THROW_SPEED.get();
        double netherite = PufferfishCombatConfig.NETHERITE_THROW_SPEED.get();
        try {
            PufferfishCombatConfig.NORMAL_THROW_SPEED.set(1.2);
            PufferfishCombatConfig.NORMAL_THROW_SPEED.clearCache();
            PufferfishCombatConfig.NETHERITE_THROW_SPEED.set(0.6);
            PufferfishCombatConfig.NETHERITE_THROW_SPEED.clearCache();
            var stacks = new ArrayList<ItemStack>();
            stacks.add(new ItemStack(Items.PUFFERFISH));
            stacks.add(new ItemStack(PufferfishItems.NETHERITE.get()));
            for (var color : DyeColor.values()) stacks.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
            for (var hand : InteractionHand.values()) {
                for (var stack : stacks) {
                    var projectile = launch(helper, stack.copy(), hand);
                    double expected = PufferfishItems.isNetherite(stack) ? 0.6 : 1.2;
                    helper.assertTrue(Math.abs(projectile.getDeltaMovement().length() - expected) < 0.06,
                            "Configured launch speed was not used for the fish type");
                    projectile.discard();
                }
            }
        } finally {
            PufferfishCombatConfig.NORMAL_THROW_SPEED.set(normal);
            PufferfishCombatConfig.NORMAL_THROW_SPEED.clearCache();
            PufferfishCombatConfig.NETHERITE_THROW_SPEED.set(netherite);
            PufferfishCombatConfig.NETHERITE_THROW_SPEED.clearCache();
        }
        helper.succeed();
    }

    /// Verifies real throws shorten progressively at every Density level for all fish variants and both hands.
    @GameTest(template = "empty")
    public static void densityProgressivelyShortensFishFlight(GameTestHelper helper) {
        var density = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        var variants = new ArrayList<ItemStack>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        variants.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        variants.add(new ItemStack(PufferfishItems.HEAVY.get()));
        for (var color : DyeColor.values()) variants.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        for (var hand : InteractionHand.values()) {
            for (var variant : variants) {
                double previousDistance = Double.POSITIVE_INFINITY;
                for (int rank = 0; rank <= 5; rank++) {
                    var stack = variant.copyWithCount(3);
                    int level = rank;
                    EnchantmentHelper.updateEnchantments(stack, entries -> entries.set(density, level));
                    var original = stack.copyWithCount(1);
                    var projectile = launch(helper, stack, hand);
                    double expectedSpeed = (PufferfishItems.isHeavy(variant) ? 0.8 : variant.is(Items.PUFFERFISH) ? 1.5 : 1.0) / (1 + 0.2 * rank);
                    helper.assertTrue(Math.abs(projectile.getDeltaMovement().length() - expectedSpeed) < expectedSpeed * 0.04,
                            "Density did not scale the launched fish's speed");
                    helper.assertTrue(stack.getCount() == 2 && ItemStack.matches(projectile.getItem(), original),
                            "Density throw lost components or consumed the wrong count");
                    var start = projectile.position();
                    for (int tick = 0; tick < 20; tick++) projectile.tick();
                    double distance = projectile.position().subtract(start).horizontalDistance();
                    helper.assertTrue(!projectile.isRemoved() && distance > 0 && distance < previousDistance
                            && projectile.getY() < start.y, "Higher Density did not shorten falling flight");
                    previousDistance = distance;
                    projectile.discard();
                }
            }
        }
        helper.succeed();
    }

    /// Verifies the penalty combines with custom base speeds, can be disabled, and stays positive at command-level enchantments.
    @GameTest(template = "empty")
    public static void densitySpeedPenaltySupportsConfigurationAndHighLevels(GameTestHelper helper) {
        var settings = List.of(PufferfishCombatConfig.NORMAL_THROW_SPEED, PufferfishCombatConfig.NETHERITE_THROW_SPEED,
                PufferfishCombatConfig.HEAVY_THROW_SPEED, PufferfishCombatConfig.DENSITY_THROW_SPEED_PENALTY);
        var previous = settings.stream().map(setting -> setting.get()).toList();
        var items = List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get());
        double[] speeds = {1.2, 0.8, 0.4};
        var density = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        try {
            for (int index = 0; index < 3; index++) {
                settings.get(index).set(speeds[index]);
                settings.get(index).clearCache();
            }
            for (double penalty : new double[] {0, 0.5, 16}) {
                settings.getLast().set(penalty);
                settings.getLast().clearCache();
                for (int index = 0; index < 3; index++) {
                    for (int rank : new int[] {0, 5, 255}) {
                        var stack = new ItemStack(items.get(index));
                        EnchantmentHelper.updateEnchantments(stack, entries -> entries.set(density, rank));
                        var projectile = launch(helper, stack, InteractionHand.MAIN_HAND);
                        double actual = projectile.getDeltaMovement().length();
                        double expected = speeds[index] / (1 + penalty * rank);
                        helper.assertTrue(Double.isFinite(actual) && actual > 0 && Math.abs(actual - expected) < expected * 0.04,
                                "Configured Density penalty produced the wrong launch speed");
                        projectile.discard();
                    }
                }
            }
        } finally {
            for (int index = 0; index < settings.size(); index++) {
                settings.get(index).set(previous.get(index));
                settings.get(index).clearCache();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void throwingConsumesOneFishAndPreservesColor(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        for (InteractionHand hand : InteractionHand.values()) {
            for (DyeColor color : DyeColor.values()) {
                var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 3), color);
                player.setItemInHand(hand, stack);
                var event = new PlayerInteractEvent.RightClickItem(player, hand);
                NeoForge.EVENT_BUS.post(event);
                var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
                helper.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction(), "Throw interaction was not handled");
                helper.assertTrue(stack.getCount() == 2 && projectiles.size() == 1, "Throw did not consume and spawn exactly one fish");
                var projectile = projectiles.getFirst();
                helper.assertTrue(PufferfishColors.get(projectile.getItem()) == color && projectile.getItem().getCount() == 1,
                        "Projectile did not preserve its colored item");
                helper.assertTrue(projectile.getOwner() == player && projectile.getDeltaMovement().length() > 1.0,
                        "Projectile owner or launch velocity is incorrect");
                projectile.discard();
            }
        }
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        creative.setPos(pos.getX(), pos.getY(), pos.getZ());
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(creative, InteractionHand.MAIN_HAND));
        var projectiles = helper.getLevel().getEntitiesOfClass(ThrownPufferfish.class, new AABB(pos).inflate(3));
        helper.assertTrue(creative.getMainHandItem().getCount() == 1 && projectiles.size() == 1, "Creative throw consumed a fish");
        helper.assertTrue(PufferfishColors.get(projectiles.getFirst().getItem()) == null, "Undyed projectile gained a color");
        projectiles.forEach(ThrownPufferfish::discard);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void throwingRespectsOtherUsesAndCanceledSpawns(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.setShiftKeyDown(true);
        var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled() && player.getMainHandItem().getCount() == 1, "Sneaking no longer allows eating");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH_BUCKET));
        event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled(), "Bucket use was intercepted");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        player.getCooldowns().addCooldown(Items.PUFFERFISH, 20);
        event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled() && player.getMainHandItem().getCount() == 1, "Item cooldown was bypassed");
        player.getCooldowns().removeCooldown(Items.PUFFERFISH);
        Consumer<EntityJoinLevelEvent> denySpawn = join -> {
            if (join.getEntity() instanceof ThrownPufferfish) {
                join.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(denySpawn);
        try {
            event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.getCancellationResult() == InteractionResult.FAIL && player.getMainHandItem().getCount() == 1,
                    "Canceled spawn consumed a fish");
        } finally {
            NeoForge.EVENT_BUS.unregister(denySpawn);
        }
        var spectator = helper.makeMockPlayer(GameType.SPECTATOR);
        spectator.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PUFFERFISH));
        event = new PlayerInteractEvent.RightClickItem(spectator, InteractionHand.MAIN_HAND);
        NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(!event.isCanceled(), "Spectator threw a fish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void collisionDealsTwoDamageAndSixSecondsOfPoison(GameTestHelper helper) {
        var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 3));
        target.setNoAi(true);
        float health = target.getHealth();
        var projectile = hit(helper, target);
        helper.assertTrue(target.getHealth() == health - 2.0F, "Impact did not deal exactly two base damage");
        var effect = target.getEffect(MobEffects.POISON);
        helper.assertTrue(effect != null && effect.getDuration() == 120 && effect.getAmplifier() == 0,
                "Impact did not apply Poison I for 120 ticks");
        helper.assertTrue(projectile.isRemoved(), "Projectile survived its impact");
        helper.assertTrue(target.getLastHurtByMob() == projectile.getOwner(), "Damage was not attributed to the thrower");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void impactsRespectInvulnerabilityAndPoisonImmunity(GameTestHelper helper) {
        var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 3));
        target.setInvulnerable(true);
        float health = target.getHealth();
        var projectile = hit(helper, target);
        helper.assertTrue(target.getHealth() == health && !target.hasEffect(MobEffects.POISON), "Invulnerable target was damaged or poisoned");
        helper.assertTrue(projectile.isRemoved(), "Blocked projectile was not removed");
        target.discard();
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 3));
        float zombieHealth = zombie.getHealth();
        hit(helper, zombie);
        helper.assertTrue(zombie.getHealth() < zombieHealth && !zombie.hasEffect(MobEffects.POISON), "Undead poison immunity was bypassed");
        zombie.discard();
        helper.succeed();
    }

    private static ThrownPufferfish hit(GameTestHelper helper, LivingEntity target) {
        return hit(helper, target, new ItemStack(Items.PUFFERFISH));
    }

    private static ThrownPufferfish hit(GameTestHelper helper, LivingEntity target, ItemStack stack) {
        return hit(helper, target, stack, true);
    }

    private static ThrownPufferfish hit(GameTestHelper helper, LivingEntity target, ItemStack stack, boolean hasOwner) {
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        owner.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        var projectile = hasOwner ? new ThrownPufferfish(helper.getLevel(), owner)
                : KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
        projectile.setItem(stack);
        projectile.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ() - 2.0);
        projectile.setDeltaMovement(0.0, 0.0, 3.0);
        helper.getLevel().addFreshEntity(projectile);
        projectile.tick();
        return projectile;
    }

    @GameTest(template = "empty")
    public static void blockImpactsLeaveRecoverableItems(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        var projectile = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
        var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.RED);
        projectile.setItem(stack);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 1));
        projectile.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        projectile.setDeltaMovement(0.0, 0.0, 3.0);
        helper.getLevel().addFreshEntity(projectile);
        projectile.tick();
        helper.assertTrue(projectile.isRemoved(), "Projectile survived a block impact");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4))
                .stream().filter(drop -> drop.getItem().is(Items.PUFFERFISH)).toList();
        helper.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.getFirst().getItem(), stack),
                "Block impact did not recover exactly one matching fish");
        helper.assertTrue(helper.getLevel().getBlockState(drops.getFirst().blockPosition()).isAir(),
                "Recovered fish was embedded in the block");
        drops.forEach(ItemEntity::discard);
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 3));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void entityImpactsRecoverAllColorsAndNames(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 3));
            var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
            var projectile = hit(helper, target, stack);
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3))
                    .stream().filter(drop -> drop.getItem().is(Items.PUFFERFISH)).toList();
            helper.assertTrue(projectile.isRemoved() && drops.size() == 1, "Impact did not recover exactly one fish");
            var recovered = drops.getFirst();
            helper.assertTrue(recovered.getItem().getCount() == 1 && ItemStack.isSameItemSameComponents(recovered.getItem(), stack),
                    "Recovered fish lost its color or name");
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            recovered.setNoPickUpDelay();
            recovered.playerTouch(player);
            helper.assertTrue(recovered.isRemoved() && ItemStack.isSameItemSameComponents(player.getInventory().getItem(0), stack),
                    "Recovered fish could not be picked up");
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fatalImpactsUseDedicatedDeathMessages(GameTestHelper helper) {
        for (boolean hasOwner : new boolean[] {false, true}) {
            for (boolean namedItem : new boolean[] {false, true}) {
                var target = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 3));
                target.setCustomName(Component.literal("Target"));
                target.setHealth(1.0F);
                var stack = new ItemStack(Items.PUFFERFISH);
                if (namedItem) {
                    stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
                }
                List<Component> messages = new ArrayList<>();
                Consumer<LivingDeathEvent> onDeath = event -> {
                    if (event.getEntity() == target) {
                        messages.add(target.getCombatTracker().getDeathMessage());
                    }
                };
                NeoForge.EVENT_BUS.addListener(onDeath);
                ThrownPufferfish projectile;
                try {
                    projectile = hit(helper, target, stack, hasOwner);
                } finally {
                    NeoForge.EVENT_BUS.unregister(onDeath);
                }
                helper.assertTrue(target.isDeadOrDying(), "Pufferfish impact was not fatal");
                var source = target.getLastDamageSource();
                helper.assertTrue(source != null && source.is(ThrownPufferfish.DAMAGE_TYPE) && source.is(DamageTypeTags.IS_PROJECTILE),
                        "Impact did not use the dedicated projectile damage type");
                helper.assertTrue(source.getEntity() == projectile.getOwner() && source.getDirectEntity() == projectile,
                        "Damage attribution was lost");
                helper.assertTrue(messages.size() == 1, "Expected exactly one death message");
                var message = messages.getFirst();
                String expectedKey = "death.attack.kaleidoscope_pufferfish.thrown_pufferfish" + (hasOwner ? ".player" : "");
                helper.assertTrue(message.getContents() instanceof TranslatableContents translated && translated.getKey().equals(expectedKey),
                        "Combat tracker selected the wrong death message");
                String expectedMessage = hasOwner
                        ? "Target was struck down by " + projectile.getOwner().getDisplayName().getString() + " with a pufferfish"
                        : "Target was struck down by a pufferfish";
                helper.assertTrue(message.getString().equals(expectedMessage), "Death message did not resolve correctly");
                helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
                target.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileColorsSurviveSaveAndSynchronization(GameTestHelper helper) {
        for (DyeColor color : DyeColor.values()) {
            var projectile = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
            var stack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH, 8), color);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
            projectile.setItem(stack);
            var receiver = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
            receiver.getEntityData().assignValues(projectile.getEntityData().getNonDefaultValues());
            helper.assertTrue(PufferfishColors.get(receiver.getItem()) == color, "Projectile color did not synchronize");
            var tag = new CompoundTag();
            projectile.saveWithoutId(tag);
            var restored = KaleidoscopePufferfish.THROWN_PUFFERFISH.get().create(helper.getLevel());
            restored.load(tag);
            helper.assertTrue(PufferfishColors.get(restored.getItem()) == color && restored.getItem().getCount() == 1,
                    "Saved projectile lost its color or duplicated its item");
            helper.assertTrue(restored.getItem().getHoverName().getString().equals("Keepsake"), "Saved projectile lost its name");
        }
        helper.succeed();
    }
}
