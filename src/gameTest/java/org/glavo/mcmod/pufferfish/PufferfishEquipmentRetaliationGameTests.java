// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNullByDefault;

/// Equipment retaliation uses one native Thorns roll at the strongest applicable tier.
@NotNullByDefault
@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishEquipmentRetaliationGameTests {
    /// Compares individual slots, mixed and full armor sets, and either shield hand with vanilla Thorns.
    @GameTest(template = "empty")
    public static void equipmentRetaliationMatchesOneNativeThornsRoll(GameTestHelper helper) {
        var level = helper.getLevel();
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        for (var variant : List.of(PufferfishVariant.NORMAL, PufferfishVariant.DIAMOND, PufferfishVariant.HEAVY)) for (int scenario = 0; scenario < 9; scenario++) {
            var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
            wearer.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            wearer.setNoGravity(true);
            wearer.setYRot(0);
            if (scenario == 0) wearer.setItemSlot(EquipmentSlot.HEAD, variant.fish());
            else if (scenario <= 4) {
                var type = PufferfishArmor.TYPES.get(scenario - 1);
                wearer.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get(variant.skinName() + "_" + type.getName()).get()));
            } else if (scenario == 5) {
                wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(PufferfishArmor.VARIANTS.get("pufferfish_skin_chestplate").get()));
                wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(PufferfishArmor.VARIANTS.get(variant.skinName() + "_boots").get()));
            } else if (scenario == 6) {
                for (var type : PufferfishArmor.TYPES) {
                    wearer.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get(variant.skinName() + "_" + type.getName()).get()));
                }
            } else {
                var hand = scenario == 7 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                var shield = new ItemStack(PufferfishShields.VARIANTS.get(variant.skinName() + "_shield").get());
                wearer.setItemInHand(hand, shield);
                shield.use(helper.getLevel(), wearer, hand);
            }
            for (int tick = 0; tick < 6; tick++) wearer.tick();
            var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
            attacker.setNoAi(true);
            int strength = variant == PufferfishVariant.DIAMOND ? 2 : variant == PufferfishVariant.HEAVY && scenario == 0 ? 3 : 1;
            int reflected = 0;
            for (int hit = 0; hit < 64; hit++) {
                long seed = 923L + hit * 7919L;
                level.getRandom().setSeed(seed);
                wearer.getRandom().setSeed(seed);
                attacker.getRandom().setSeed(seed);
                var source = wearer.damageSources().mobAttack(attacker);
                thorns.value().doPostAttack(level, strength,
                        new EnchantedItemInUse(ItemStack.EMPTY, null, wearer, item -> {}), EnchantmentTarget.VICTIM, wearer, source);
                float expectedDamage = attacker.getMaxHealth() - attacker.getHealth();
                level.getRandom().setSeed(seed);
                wearer.getRandom().setSeed(seed);
                attacker.getRandom().setSeed(seed);
                wearer.setHealth(wearer.getMaxHealth());
                wearer.invulnerableTime = 0;
                for (var slot : EquipmentSlot.values()) {
                    var stack = wearer.getItemBySlot(slot);
                    if (stack.isDamageableItem()) stack.setDamageValue(0);
                }
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                attacker.setDeltaMovement(Vec3.ZERO);
                attacker.removeAllEffects();
                wearer.hurt(source, 2);
                float damage = attacker.getMaxHealth() - attacker.getHealth();
                helper.assertTrue(damage == expectedDamage,
                        "Equipment differed from one native Thorns " + strength + " roll: " + variant.fishName() + ", scenario=" + scenario + ", seed=" + seed);
                if (damage > 0) reflected++;
                attacker.setHealth(attacker.getMaxHealth());
                attacker.invulnerableTime = 0;
                attacker.removeAllEffects();
            }
            helper.assertTrue(reflected > 0 && reflected < 64, "Thorns comparison did not exercise both successful and failed rolls");
            attacker.discard();
        }
        helper.succeed();
    }
}
