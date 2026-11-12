// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import static org.glavo.mcmod.pufferfish.test.TestItems.enchant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;
import org.glavo.mcmod.pufferfish.test.TestItems;
import org.glavo.mcmod.pufferfish.test.TestPlayers;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishExtraEnchantmentGameTests {
    /// Uses server-side enchantment damage and death drops, which mock Player omits.
    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        var player = TestPlayers.serverPlayer(helper);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 0))));
        return player;
    }

    private static List<ItemStack> fish() {
        var stacks = new ArrayList<ItemStack>();
        stacks.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) stacks.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        stacks.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        stacks.add(new ItemStack(PufferfishItems.HEAVY.get()));
        return stacks;
    }

    @GameTest(template = "empty")
    public static void creatureDamageEnchantmentsAffectMeleeAndProjectiles(GameTestHelper helper) {
        for (var item : List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get(),
                PufferfishItems.FLAIL.get(), PufferfishItems.NETHERITE_FLAIL.get(), PufferfishItems.HEAVY_FLAIL.get())) {
            for (var key : List.of(Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS)) {
                for (int rank = 1; rank <= 5; rank++) {
                    for (var type : List.of(EntityType.ZOMBIE, EntityType.SPIDER, EntityType.VILLAGER)) {
                        for (boolean ranged : new boolean[] {false, true}) {
                            var stack = enchant(helper, new ItemStack(item), key, rank);
                            var player = serverPlayer(helper);
                            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                            for (int tick = 0; tick < 40; tick++) player.doTick();
                            player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 0))));
                            player.setOnGround(true);
                            player.fallDistance = 0;
                            Mob target = helper.spawn(type, new BlockPos(2, 2, 3));
                            target.setNoAi(true);
                            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
                            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                            target.setHealth(1000);
                            boolean affected = key == Enchantments.SMITE ? type == EntityType.ZOMBIE : type == EntityType.SPIDER;
                            double expected = (ranged ? PufferfishCombat.damage(stack) : player.getAttributeValue(Attributes.ATTACK_DAMAGE))
                                    + (affected ? 2.5 * rank : 0);
                            if (ranged && PufferfishItems.isFlail(stack)) {
                                expected = ((PufferfishItems.hasHeavyCombat(stack) ? 10 : PufferfishItems.hasNetheriteCombat(stack) ? 6 : 1) + (affected ? 2.5 * rank : 0))
                                        * (PufferfishItems.hasHeavyCombat(stack) ? 1.6 : 2);
                            }
                            if (ranged) {
                                net.minecraft.world.entity.projectile.ThrowableItemProjectile projectile;
                                if (PufferfishItems.isFlail(stack)) {
                                    projectile = new PufferfishFlailHead(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                                } else {
                                    projectile = new ThrownPufferfish(helper.getLevel(), player);
                                    projectile.setItem(stack);
                                    player.setItemInHand(InteractionHand.MAIN_HAND, enchant(helper, new ItemStack(Items.DIAMOND_SWORD),
                                            key == Enchantments.SMITE ? Enchantments.BANE_OF_ARTHROPODS : Enchantments.SMITE, 5));
                                }
                                projectile.setPos(target.getX(), target.getY() + 0.5, target.getZ() - 2);
                                projectile.setDeltaMovement(0, 0, 3);
                                helper.getLevel().addFreshEntity(projectile);
                                projectile.tick();
                                projectile.discard();
                            } else {
                                if (PufferfishItems.isFlail(stack)) player.setPos(player.position().add(0, 0, 0.8));
                                player.attack(target);
                                if (PufferfishItems.isFlail(stack)) {
                                    var head = PufferfishFlailItem.activeHead(player);
                                    helper.assertTrue(head != null && head.isShortCast() && target.getHealth() == 1000,
                                            "Flail left click did not replace direct melee");
                                    head.setPos(target.getX(), target.getY() + 0.5, target.getZ() - 1.5);
                                    head.setDeltaMovement(0, 0, 2);
                                    head.tick();
                                    head.discard();
                                }
                            }
                            helper.assertTrue(Math.abs(1000 - target.getHealth() - expected) < 0.001,
                                    "Incorrect creature enchantment damage: " + item + ", " + key + ", rank=" + rank
                                            + ", target=" + type + ", ranged=" + ranged + ", expected=" + expected
                                            + ", actual=" + (1000 - target.getHealth()));
                            if (!ranged && !PufferfishItems.isFlail(stack) && affected && key == Enchantments.BANE_OF_ARTHROPODS) {
                                var slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                                helper.assertTrue(slow != null && slow.getAmplifier() == 3 && slow.getDuration() >= 30,
                                        "Bane melee hit did not apply vanilla Slowness IV");
                            }
                            helper.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3)).forEach(ItemEntity::discard);
                            target.discard();
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void windBurstRetainsItsFallConditionWithoutOrdinarySmashProtection(GameTestHelper helper) {
        int[] bursts = {0};
        Consumer<ExplosionEvent.Start> capture = event -> { bursts[0]++; event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            for (var original : fish()) {
                for (float distance : new float[] {0, 3}) {
                    var player = serverPlayer(helper);
                    player.setItemInHand(InteractionHand.MAIN_HAND, enchant(helper, original.copy(), Enchantments.WIND_BURST, 3));
                    for (int tick = 0; tick < 40; tick++) player.doTick();
                    player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 3, 1))));
                    player.setOnGround(distance == 0);
                    player.fallDistance = distance;
                    var target = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 3));
                    target.setNoAi(true);
                    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                    target.setHealth(100);
                    int before = bursts[0];
                    player.attack(target);
                    helper.assertTrue(bursts[0] - before == (distance > 0 ? 1 : 0)
                            && player.fallDistance == (PufferfishItems.isHeavy(original) ? 0 : distance),
                            "Wind Burst changed its fall condition or ordinary fish gained smash protection: bursts="
                                    + (bursts[0] - before) + ", fall=" + player.fallDistance);
                    target.discard();
                }
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(capture);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bindingPreventsRemovingWornFishButAllowsCreativeRemoval(GameTestHelper helper) {
        for (var stack : fish()) {
            enchant(helper, stack, Enchantments.BINDING_CURSE, 1);
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            var slot = player.inventoryMenu.getSlot(5);
            helper.assertTrue(!slot.mayPickup(player), "Binding allowed survival headgear removal");
            player.inventoryMenu.clicked(5, 0, ClickType.PICKUP, player);
            helper.assertTrue(ItemStack.matches(player.getItemBySlot(EquipmentSlot.HEAD), stack)
                    && player.inventoryMenu.getCarried().isEmpty(), "Inventory click bypassed Binding");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.getInventory().setItem(9, stack.copy());
            helper.assertTrue(player.inventoryMenu.getSlot(9).mayPickup(player), "Binding trapped unequipped fish");
            var creative = helper.makeMockPlayer(GameType.CREATIVE);
            creative.setItemSlot(EquipmentSlot.HEAD, stack.copy());
            helper.assertTrue(creative.inventoryMenu.getSlot(5).mayPickup(creative), "Binding prevented creative removal");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanishingRemovesFishOnDeathAndRespectsKeepInventory(GameTestHelper helper) {
        var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean previous = rule.get();
        try {
            for (boolean keep : new boolean[] {false, true}) {
                rule.set(keep, helper.getLevel().getServer());
                for (var stack : fish()) {
                    var player = serverPlayer(helper);
                    player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
                    enchant(helper, stack, Enchantments.VANISHING_CURSE, 1);
                    player.setItemSlot(EquipmentSlot.HEAD, stack.copy());
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                    player.getInventory().setItem(9, new ItemStack(Items.STICK));
                    player.setHealth(0);
                    player.die(helper.getLevel().damageSources().generic());
                    helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() != keep
                            && player.getMainHandItem().isEmpty() != keep, "Vanishing ignored death or keepInventory");
                    var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(3));
                    helper.assertTrue(drops.stream().noneMatch(drop -> drop.getItem().is(stack.getItem()))
                            && (keep ? drops.isEmpty() : drops.stream().anyMatch(drop -> drop.getItem().is(Items.STICK))),
                            "Vanishing dropped cursed fish or removed ordinary loot");
                    drops.forEach(ItemEntity::discard);
                }
            }
        } finally {
            rule.set(previous, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void channelingChecksWeatherSkyAndDamageAndPreservesRecovery(GameTestHelper helper) {
        var level = helper.getLevel();
        float rain = level.getRainLevel(1);
        float thunder = level.getThunderLevel(1);
        var roof = new BlockPos(2, 6, 3);
        helper.setBlock(roof, Blocks.STONE);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                !level.canSeeSky(helper.absolutePos(new BlockPos(2, 2, 3)))
                        && level.canSeeSky(helper.absolutePos(new BlockPos(4, 2, 3))), "Sky lighting is not ready"))
                .thenExecute(() -> {
            try {
                for (var original : fish()) {
                    for (int scenario = 0; scenario < 6; scenario++) {
                        for (boolean loyal : new boolean[] {false, true}) {
                            level.setRainLevel(scenario == 0 ? 0 : 1);
                            level.setThunderLevel(scenario < 2 ? 0 : 1);
                            var stack = TestItems.awakened(original);
                            if (scenario != 4) enchant(helper, stack, Enchantments.CHANNELING, 1);
                            if (loyal) enchant(helper, stack, Enchantments.LOYALTY, 3);
                            var owner = serverPlayer(helper);
                            var target = helper.spawn(EntityType.VILLAGER, new BlockPos(scenario == 3 ? 2 : 4, 2, 3));
                            target.setNoAi(true);
                            target.setInvulnerable(scenario == 5);
                            var projectile = new ThrownPufferfish(level, owner);
                            projectile.setItem(stack);
                            projectile.setPos(target.getX(), target.getY() + 0.5, target.getZ() - 2);
                            projectile.setDeltaMovement(0, 0, 3);
                            level.addFreshEntity(projectile);
                            projectile.tick();
                            var bolts = level.getEntitiesOfClass(LightningBolt.class, target.getBoundingBox().inflate(4));
                            helper.assertTrue(bolts.size() == (scenario == 2 ? 1 : 0),
                                    "Channeling ignored weather, sky, enchantment, or damage: scenario=" + scenario);
                            for (var bolt : bolts) {
                                helper.assertTrue(bolt.getCause() == owner && bolt.position().distanceToSqr(target.position()) < 0.001,
                                        "Channeling lost its owner or struck the wrong position");
                                bolt.discard();
                            }
                            if (loyal) {
                                helper.assertTrue(projectile.isReturning(), "Channeling prevented Loyalty return");
                                for (int tick = 0; tick < 100 && !projectile.isRemoved(); tick++) projectile.tick();
                                helper.assertTrue(projectile.isRemoved() && ItemStack.isSameItemSameComponents(owner.getInventory().getItem(0), TestItems.worn(stack)),
                                        "Loyalty lost the fish or its enchantments");
                            }
                            var drops = level.getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(4));
                            helper.assertTrue(loyal ? drops.isEmpty() : drops.size() == 1
                                            && ItemStack.isSameItemSameComponents(drops.getFirst().getItem(), TestItems.worn(stack)),
                                    "Channeling changed immediate fish recovery");
                            drops.forEach(ItemEntity::discard);
                            target.discard();
                            projectile.discard();
                        }
                    }
                }
            } finally {
                level.setRainLevel(rain);
                level.setThunderLevel(thunder);
                helper.setBlock(roof, Blocks.AIR);
            }
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void channelingStrikesOnlyExposedLightningRods(GameTestHelper helper) {
        var level = helper.getLevel();
        float rain = level.getRainLevel(1);
        float thunder = level.getThunderLevel(1);
        var covered = new BlockPos(2, 2, 3);
        var exposed = new BlockPos(4, 2, 3);
        var roof = covered.above(4);
        helper.setBlock(roof, Blocks.STONE);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                !level.canSeeSky(helper.absolutePos(covered)) && level.canSeeSky(helper.absolutePos(exposed)),
                "Sky lighting is not ready")).thenExecute(() -> {
            try {
                for (boolean flail : new boolean[] {false, true}) {
                    for (int scenario = 0; scenario < 5; scenario++) {
                        level.setRainLevel(1);
                        level.setThunderLevel(scenario == 1 ? 0 : 1);
                        var block = scenario == 2 ? covered : exposed;
                        helper.setBlock(block, scenario == 3 ? Blocks.STONE : Blocks.LIGHTNING_ROD);
                        var owner = serverPlayer(helper);
                        var stack = new ItemStack(flail ? PufferfishItems.FLAIL.get() : Items.PUFFERFISH);
                        if (scenario != 4) enchant(helper, stack, Enchantments.CHANNELING, 1);
                        owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        var projectile = flail ? new PufferfishFlailHead(level, owner, InteractionHand.MAIN_HAND)
                                : new ThrownPufferfish(level, owner);
                        projectile.setItem(stack);
                        var pos = helper.absolutePos(block);
                        projectile.setPos(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
                        projectile.setDeltaMovement(0, -3, 0);
                        level.addFreshEntity(projectile);
                        projectile.tick();
                        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(4);
                        var bolts = level.getEntitiesOfClass(LightningBolt.class, bounds);
                        helper.assertTrue(bolts.size() == (scenario == 0 ? 1 : 0),
                                "Channeling ignored lightning rod conditions: scenario=" + scenario + ", flail=" + flail);
                        bolts.forEach(LightningBolt::discard);
                        level.getEntitiesOfClass(ItemEntity.class, bounds).forEach(ItemEntity::discard);
                        projectile.discard();
                    }
                }
            } finally {
                level.setRainLevel(rain);
                level.setThunderLevel(thunder);
                helper.setBlock(covered, Blocks.AIR);
                helper.setBlock(exposed, Blocks.AIR);
                helper.setBlock(roof, Blocks.AIR);
            }
        }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void thornsAddsRetaliationToWornAndHeldFish(GameTestHelper helper) {
        var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 3));
        attacker.setNoAi(true);
        attacker.getAttribute(Attributes.ARMOR).setBaseValue(0);
        try {
            for (var item : List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get(), PufferfishItems.HEAVY.get())) {
                for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)) {
                    int baseline = 0;
                    for (int rank : new int[] {0, 3}) {
                        var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
                        var stack = new ItemStack(item);
                        if (rank > 0) enchant(helper, stack, Enchantments.THORNS, rank);
                        wearer.setItemSlot(slot, stack);
                        helper.getLevel().getRandom().setSeed(81357);
                        int reflected = 0;
                        for (int hit = 0; hit < 128; hit++) {
                            if (stack.isDamageableItem()) stack.setDamageValue(0);
                            wearer.setHealth(wearer.getMaxHealth());
                            wearer.invulnerableTime = 0;
                            attacker.setHealth(attacker.getMaxHealth());
                            attacker.invulnerableTime = 0;
                            attacker.removeAllEffects();
                            helper.assertTrue(attacker.doHurtTarget(wearer), "Attack did not hit fish wearer");
                            if (attacker.getHealth() < attacker.getMaxHealth()) reflected++;
                        }
                        if (rank == 0) {
                            baseline = reflected;
                            helper.assertTrue(slot == EquipmentSlot.HEAD ? baseline > 0 : baseline == 0,
                                    "Unenchanted fish changed its innate retaliation");
                        } else {
                            helper.assertTrue(reflected > baseline && reflected < 128,
                                    "Thorns did not add chance-based retaliation: " + item + ", slot=" + slot);
                            helper.assertTrue(stack.getCount() == 1, "Thorns consumed a freshly repaired fish");
                        }
                    }
                }
            }
        } finally {
            attacker.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void newDamageBooksRespectExclusivity(GameTestHelper helper) {
        var keys = List.of(Enchantments.DENSITY, Enchantments.BREACH, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var first : keys) {
            for (var second : keys) {
                if (first == second) continue;
                var menu = new AnvilMenu(1, player.getInventory());
                menu.getSlot(0).set(enchant(helper, new ItemStack(Items.PUFFERFISH), first, 1));
                menu.getSlot(1).set(enchant(helper, new ItemStack(Items.ENCHANTED_BOOK), second, 1));
                menu.createResult();
                helper.assertTrue(!menu.getSlot(2).hasItem(), "Mutually exclusive damage books combined");
            }
        }
        helper.succeed();
    }
}
