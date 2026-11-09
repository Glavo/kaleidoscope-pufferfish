// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishArmorGameTests {
    @GameTest(template = "empty")
    public static void variantsCraftRepairEquipAndAcceptArmorEnchantments(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (var entry : PufferfishArmor.VARIANTS.entrySet()) {
            var skin = new ItemStack(BuiltInRegistries.ITEM.get(KaleidoscopePufferfish.id(entry.getKey().replace("_chestplate", ""))));
            var grid = new ArrayList<>(Collections.nCopies(9, skin));
            grid.set(1, ItemStack.EMPTY);
            var input = CraftingInput.of(3, 3, grid);
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
            var armor = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(armor.is(entry.getValue().get()) && armor.getCount() == 1 && armor.getMaxDamage() == 80,
                    "Armor recipe or durability was incorrect");
            helper.assertTrue(armor.is(ItemTags.CHEST_ARMOR) && armor.getItem().isValidRepairItem(armor, skin)
                    && !armor.getItem().isValidRepairItem(armor, new ItemStack(Items.LEATHER)), "Armor tags or repair ingredient were incorrect");
            for (var enchantment : java.util.List.of(Enchantments.PROTECTION, Enchantments.THORNS, Enchantments.UNBREAKING,
                    Enchantments.MENDING, Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE)) {
                helper.assertTrue(enchantments.getHolderOrThrow(enchantment).value().canEnchant(armor), "Armor rejected an armor enchantment");
            }
            player.setItemSlot(EquipmentSlot.CHEST, armor);
            player.tick();
            helper.assertTrue(player.getArmorValue() == 3 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0,
                    "Armor did not match leather chestplate protection");
            grid.set(0, new ItemStack(Items.LEATHER));
            helper.assertTrue(!recipe.matches(CraftingInput.of(3, 3, grid), helper.getLevel()), "Armor accepted an incorrect ingredient");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wornArmorPoisonsAndRollsThornsButHeldArmorDoesNot(GameTestHelper helper) {
        int reflected = 0;
        int attempts = 0;
        for (var item : PufferfishArmor.VARIANTS.values()) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
            attacker.setNoAi(true);
            player.getRandom().setSeed(4321);
            var armor = new ItemStack(item.get());
            player.setItemSlot(EquipmentSlot.CHEST, armor);
            player.tick();
            for (int attempt = 0; attempt < 48; attempt++) {
                player.setHealth(20);
                player.invulnerableTime = 0;
                attacker.setHealth(20);
                attacker.invulnerableTime = 0;
                attacker.removeAllEffects();
                armor.setDamageValue(0);
                player.hurt(player.damageSources().mobAttack(attacker), 2);
                var poison = attacker.getEffect(MobEffects.POISON);
                helper.assertTrue(poison != null && poison.getDuration() == 60 && poison.getAmplifier() == 0,
                        "Skin armor did not inflict three seconds of Poison I");
                float damage = 20 - attacker.getHealth();
                helper.assertTrue(damage == 0 || damage >= 1 && damage <= 5, "Armor exceeded native Thorns I damage");
                if (damage > 0) reflected++;
                attempts++;
            }
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.MAINHAND, armor);
            player.invulnerableTime = 0;
            attacker.removeAllEffects();
            attacker.setHealth(20);
            player.hurt(player.damageSources().mobAttack(attacker), 2);
            helper.assertTrue(!attacker.hasEffect(MobEffects.POISON) && attacker.getHealth() == 20, "Held armor retaliated");
            attacker.discard();
        }
        helper.assertTrue(reflected > 0 && reflected < attempts, "Armor thorns did not use a chance roll");
        helper.succeed();
    }
}
