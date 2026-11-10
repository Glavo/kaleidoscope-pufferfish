// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class DiamondPufferfishThornsGameTests {
    @GameTest(template = "empty")
    public static void diamondEquipmentAndMixedSetsUseThornsTwo(GameTestHelper helper) {
        for (int scenario = 0; scenario < 9; scenario++) {
            var wearer = helper.makeMockPlayer(GameType.SURVIVAL);
            wearer.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            wearer.setNoGravity(true);
            wearer.setYRot(0);
            if (scenario == 0) wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PufferfishItems.DIAMOND.get()));
            else if (scenario <= 4) {
                var type = PufferfishArmor.TYPES.get(scenario - 1);
                wearer.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("diamond_pufferfish_skin_" + type.getName()).get()));
            } else if (scenario == 5) {
                wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(PufferfishArmor.VARIANTS.get("pufferfish_skin_chestplate").get()));
                wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(PufferfishArmor.VARIANTS.get("diamond_pufferfish_skin_boots").get()));
            } else if (scenario == 6) {
                for (var type : PufferfishArmor.TYPES) {
                    wearer.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("diamond_pufferfish_skin_" + type.getName()).get()));
                }
            } else {
                var hand = scenario == 7 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                var shield = new ItemStack(PufferfishShields.VARIANTS.get("diamond_pufferfish_skin_shield").get());
                wearer.setItemInHand(hand, shield);
                shield.use(helper.getLevel(), wearer, hand);
            }
            for (int tick = 0; tick < 6; tick++) wearer.tick();
            var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 4));
            attacker.setNoAi(true);
            int reflected = 0;
            for (int hit = 0; hit < 512; hit++) {
                long seed = 923L + hit * 7919L;
                helper.getLevel().getRandom().setSeed(seed);
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
                wearer.hurt(wearer.damageSources().mobAttack(attacker), 2);
                if (attacker.getHealth() < attacker.getMaxHealth()) reflected++;
            }
            helper.assertTrue(reflected >= 113 && reflected <= 194,
                    "Diamond equipment did not use a single Thorns II roll: scenario=" + scenario + ", reflected=" + reflected);
            attacker.discard();
        }
        helper.succeed();
    }
}
