// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.glavo.mcmod.pufferfish.entity.PufferfishFlailHead;
import org.glavo.mcmod.pufferfish.entity.ThrownPufferfish;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishCatImmunityGameTests {
    @GameTest(template = "empty")
    public static void catsIgnoreFishWeaponsAndEffectsFromEitherHand(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var cat = cat(helper);
        player.setPos(cat.position().add(-2, 0, 0));
        var weapons = new ArrayList<ItemStack>();
        for (var variant : PufferfishVariant.ALL) {
            var stack = new ItemStack(variant.fishItem());
            if (variant.color() != null) stack = PufferfishColors.dye(stack, variant.color());
            weapons.add(stack);
        }
        PufferfishItems.FLAIL_VARIANTS.forEach(item -> weapons.add(item.toStack()));
        for (var weapon : weapons) {
            weapon.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FIRE_ASPECT), 2);
            weapon.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PUNCH), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
            player.attack(cat);
            helper.assertTrue(!cat.hurt(level.damageSources().playerAttack(player), 4), "Cat accepted a fish melee damage source");
            for (var hand : InteractionHand.values()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.setItemInHand(hand, weapon.copy());
                var projectile = PufferfishItems.isFlail(weapon) ? new PufferfishFlailHead(level, player, hand) : new ThrownPufferfish(level, player);
                projectile.setItem(weapon.copy());
                var source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ThrownPufferfish.DAMAGE_TYPE), projectile, player);
                helper.assertTrue(!cat.hurt(source, 8), "Cat accepted a fish projectile from " + hand);
                PufferfishCombat.thrownHitEffects(weapon, cat, player, new Vec3(1, 0, 0));
                PufferfishCombat.hitEffects(weapon, cat, player, new Vec3(1, 0, 0));
                PufferfishCombat.applyPassiveEffect(weapon, cat, player);
                projectile.discard();
                assertProtected(helper, cat);
            }
        }
        cat.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catsIgnoreLiveFishAndTheirStatusEffects(GameTestHelper helper) {
        var cat = cat(helper);
        var fish = EntityType.PUFFERFISH.create(helper.getLevel());
        helper.assertTrue(!cat.hurt(fish.damageSources().mobAttack(fish), 3), "Cat accepted a vanilla fish sting");
        helper.assertTrue(!cat.addEffect(new MobEffectInstance(MobEffects.POISON, 100), fish), "Cat accepted vanilla fish poison");
        fish.discard();
        for (var variant : PufferfishMobs.VARIANTS.values()) {
            var material = variant.entity().get().create(helper.getLevel());
            material.setPos(cat.position());
            helper.assertTrue(!material.canFight(cat) && !material.strike(cat, 1, 0), "Material fish attacked a cat");
            helper.assertTrue(!cat.hurt(material.damageSources().mobAttack(material), 3), "Cat accepted material fish damage");
            helper.assertTrue(!cat.hurt(material.damageSources().thorns(material), 3), "Cat accepted live fish thorns");
            for (var effect : java.util.List.of(MobEffects.POISON, PufferfishEffects.LETHAL_TOXIN, MobEffects.WITHER,
                    MobEffects.GLOWING, MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.HUNGER, MobEffects.WEAKNESS)) {
                helper.assertTrue(!cat.addEffect(new MobEffectInstance(effect, 100), material), "Cat accepted a live fish effect");
            }
            assertProtected(helper, cat);
            material.discard();
        }
        cat.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catsIgnoreWornFishSkinArmorShieldsAndCatArmor(GameTestHelper helper) {
        var cat = cat(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var armoredCat = cat(helper);
        var equipment = new ArrayList<ItemStack>();
        for (var variant : PufferfishVariant.ALL) equipment.add(new ItemStack(variant.fishItem()));
        PufferfishArmor.VARIANTS.values().forEach(item -> equipment.add(item.toStack()));
        PufferfishCatArmor.VARIANTS.values().forEach(item -> equipment.add(item.toStack()));
        PufferfishShields.VARIANTS.values().forEach(item -> equipment.add(item.toStack()));
        for (var stack : equipment) {
            net.minecraft.world.entity.LivingEntity wearer = stack.getItem() instanceof PufferfishCatArmorItem ? armoredCat : player;
            for (var slot : EquipmentSlot.values()) wearer.setItemSlot(slot, ItemStack.EMPTY);
            wearer.stopUsingItem();
            if (stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                wearer.setItemInHand(InteractionHand.OFF_HAND, stack);
                wearer.startUsingItem(InteractionHand.OFF_HAND);
            } else {
                var slot = stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor ? armor.getEquipmentSlot() : EquipmentSlot.HEAD;
                wearer.setItemSlot(slot, stack);
            }
            helper.assertTrue(!cat.hurt(wearer.damageSources().thorns(wearer), 2), "Cat accepted equipment thorns: " + stack);
            PufferfishCombat.applyPassiveEffect(stack, cat, wearer);
            wearer.invulnerableTime = 0;
            wearer.setHealth(wearer.getMaxHealth());
            wearer.hurt(wearer.damageSources().mobAttack(cat), 1);
            assertProtected(helper, cat);
        }
        armoredCat.discard();
        cat.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catsIgnoreDisplaysLampsSkinsAndFallingHeavyFish(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        var cat = cat(helper);
        for (var variant : PufferfishVariant.ALL) {
            var stack = new ItemStack(variant.fishItem());
            var blocks = new ArrayList<net.minecraft.world.level.block.Block>();
            blocks.add(variant.skin());
            blocks.add(variant == PufferfishVariant.HEAVY ? PufferfishBlocks.PLACED_HEAVY_FISH.get() : PufferfishBlocks.PLACED_FISH.get());
            if (variant.hasLamp()) blocks.add(PufferfishLamps.VARIANTS.get(variant.fishName() + "_lamp").block().get());
            for (var block : blocks) {
                level.setBlockAndUpdate(pos, block.defaultBlockState());
                if (level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity placed) placed.setFish(stack);
                cat.setPos(Vec3.atBottomCenterOf(pos));
                cat.tickCount = 20;
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(cat));
                assertProtected(helper, cat);
            }
        }
        for (var block : new HeavyPufferfishBlock[] {PufferfishBlocks.PLACED_HEAVY_FISH.get(),
                (HeavyPufferfishBlock) PufferfishLamps.VARIANTS.get("heavy_pufferfish_lamp").block().get()}) {
            var falling = FallingBlockEntity.fall(level, pos, block.defaultBlockState());
            helper.assertTrue(!cat.hurt(block.getFallDamageSource(falling), 12), "Heavy fish crushed a cat");
            assertProtected(helper, cat);
            falling.discard();
        }
        cat.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishWindBurstsDoNotPushCats(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var cat = cat(helper);
        level.addFreshEntity(cat);
        var nearby = EntityType.COW.create(level);
        nearby.setPos(cat.position().add(0, 0, 1));
        nearby.setNoAi(true);
        nearby.setNoGravity(true);
        level.addFreshEntity(nearby);
        var projectile = new ThrownPufferfish(level, player);
        var stack = new ItemStack(Items.PUFFERFISH);
        stack.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.WIND_BURST), 3);
        projectile.setItem(stack);
        PufferfishWindBurst.burst(projectile, cat.position().add(0.5, 0.2, 0));
        assertProtected(helper, cat);
        helper.assertTrue(nearby.getDeltaMovement().lengthSqr() > 0, "Fish wind burst stopped affecting other animals");
        nearby.discard();
        cat.discard();
        projectile.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heavyFishSmashesProtectCatsWithoutChangingVanillaMaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var cat = cat(helper);
        level.addFreshEntity(cat);
        var target = EntityType.COW.create(level);
        target.setPos(cat.position().add(1, 0, 0));
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "cat-smash-test"));
        player.setPos(target.position().add(0, 2, 0));
        player.setOnGround(false);
        var fish = PufferfishItems.HEAVY.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, fish);
        player.fallDistance = 5;
        fish.getItem().hurtEnemy(fish, target, player);
        assertProtected(helper, cat);
        var mace = new ItemStack(Items.MACE);
        player.setItemInHand(InteractionHand.MAIN_HAND, mace);
        player.fallDistance = 5;
        mace.getItem().hurtEnemy(mace, target, player);
        helper.assertTrue(cat.getDeltaMovement().lengthSqr() > 0, "Cat immunity also disabled vanilla mace knockback");
        cat.discard();
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fishChannelingProtectsCatsWithoutChangingOtherLightning(GameTestHelper helper) {
        var level = helper.getLevel();
        var cat = cat(helper);
        level.addFreshEntity(cat);
        var target = EntityType.COW.create(level);
        target.setPos(cat.position());
        var projectile = new ThrownPufferfish(level, helper.makeMockPlayer(GameType.SURVIVAL));
        var stack = new ItemStack(Items.PUFFERFISH);
        stack.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.CHANNELING), 1);
        float rain = level.getRainLevel(1), thunder = level.getThunderLevel(1);
        try {
            level.setRainLevel(1);
            level.setThunderLevel(1);
            PufferfishChanneling.hitEntity(projectile, stack, target);
            var bolts = level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt.class, cat.getBoundingBox().inflate(2));
            helper.assertTrue(bolts.size() == 1, "Fish Channeling did not create a lightning bolt");
            var bolt = bolts.getFirst();
            helper.assertTrue(NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent(cat, bolt)).isCanceled(),
                    "Fish lightning could strike a cat");
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent(target, bolt)).isCanceled(),
                    "Fish lightning stopped affecting other animals");
            assertProtected(helper, cat);
            bolt.discard();
            var natural = EntityType.LIGHTNING_BOLT.create(level);
            helper.assertTrue(!NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent(cat, natural)).isCanceled(),
                    "Cat immunity blocked unrelated lightning");
            natural.discard();
        } finally {
            level.setRainLevel(rain);
            level.setThunderLevel(thunder);
            cat.discard();
            target.discard();
            projectile.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catImmunityPreservesUnrelatedDamageAndExistingEffects(GameTestHelper helper) {
        var cat = cat(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, PufferfishItems.RADIANT.toStack());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
        helper.assertTrue(cat.hurt(cat.damageSources().playerAttack(player), 1), "Offhand fish prevented ordinary sword damage");
        cat.invulnerableTime = 0;
        helper.assertTrue(cat.hurt(cat.damageSources().thorns(player), 1), "Cat became immune to ordinary thorns");
        helper.assertTrue(cat.addEffect(new MobEffectInstance(MobEffects.POISON, 200), player), "Cat became immune to unrelated poison");
        helper.assertTrue(cat.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2)), "Cat became immune to unrelated wither");
        PufferfishCombat.applyAttackEffect(PufferfishItems.RADIANT.toStack(), cat, player);
        PufferfishCombat.applyPassiveEffect(PufferfishItems.HEAVY.toStack(), cat, player);
        helper.assertTrue(cat.getEffect(MobEffects.POISON).getDuration() == 200 && cat.getEffect(MobEffects.WITHER).getDuration() == 200
                && cat.getActiveEffects().size() == 2, "Fish immunity removed, refreshed or added to existing effects");
        cat.discard();
        helper.succeed();
    }

    private static Cat cat(GameTestHelper helper) {
        var cat = EntityType.CAT.create(helper.getLevel());
        cat.setNoAi(true);
        cat.setNoGravity(true);
        cat.setPos(helper.absolutePos(new BlockPos(2, 3, 2)).getCenter());
        return cat;
    }

    private static void assertProtected(GameTestHelper helper, Cat cat) {
        helper.assertTrue(cat.getHealth() == cat.getMaxHealth() && cat.getActiveEffects().isEmpty()
                && !cat.isOnFire() && cat.getDeltaMovement().lengthSqr() == 0, "Pufferfish damage or effects reached a cat");
    }
}
