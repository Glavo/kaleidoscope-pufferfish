// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishArmorGameTests {
    @GameTest(template = "empty")
    public static void everySkinHasACompleteArmorSet(GameTestHelper helper) {
        int skins = 0;
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof PufferfishSkinBlock)) continue;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(KaleidoscopePufferfish.MODID)) continue;
            for (var type : PufferfishArmor.TYPES) {
                helper.assertTrue(PufferfishArmor.VARIANTS.containsKey(id.getPath() + "_" + type.getName()),
                        "Missing armor for skin: " + id);
            }
            skins++;
        }
        helper.assertTrue(PufferfishArmor.VARIANTS.size() == skins * 4, "Skin and armor variants do not match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixedArmorUsesStrongestUnwaxedPoisonAfterThorns(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var attacker = EntityType.VILLAGER.create(helper.getLevel());
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(PufferfishArmor.VARIANTS.get("waxed_oxidized_copper_pufferfish_skin_chestplate").get()));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PufferfishArmor.VARIANTS.get("weathered_copper_pufferfish_skin_helmet").get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(PufferfishArmor.VARIANTS.get("pufferfish_skin_leggings").get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(PufferfishArmor.VARIANTS.get("oxidized_copper_pufferfish_skin_boots").get()));
        attacker.hurt(attacker.damageSources().thorns(player), 1);
        var poison = attacker.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null && poison.getDuration() == 360 && poison.getAmplifier() == 2,
                "Mixed armor did not use the strongest unwaxed poison");
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        player.invulnerableTime = 0;
        attacker.removeAllEffects();
        attacker.invulnerableTime = 0;
        attacker.hurt(attacker.damageSources().thorns(player), 1);
        poison = attacker.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null && poison.getDuration() == 240 && poison.getAmplifier() == 1,
                "Waxed armor suppressed another piece's poison");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void variantsCraftRepairEquipAndAcceptArmorEnchantments(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var enchantments = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (var entry : PufferfishArmor.VARIANTS.entrySet()) {
            var type = entry.getValue().get().getType();
            var slot = type.getSlot();
            var skin = new ItemStack(BuiltInRegistries.ITEM.get(KaleidoscopePufferfish.id(entry.getKey().replace("_" + type.getName(), ""))));
            var pattern = switch (type) {
                case HELMET -> List.of("SSS", "S S");
                case CHESTPLATE -> List.of("S S", "SSS", "SSS");
                case LEGGINGS -> List.of("SSS", "S S", "S S");
                case BOOTS -> List.of("S S", "S S");
                default -> throw new AssertionError("Unexpected armor type");
            };
            var grid = new ArrayList<ItemStack>();
            for (var row : pattern) for (char ingredient : row.toCharArray()) grid.add(ingredient == 'S' ? skin : ItemStack.EMPTY);
            var input = CraftingInput.of(3, pattern.size(), grid);
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
            var armor = recipe.assemble(input, helper.getLevel().registryAccess());
            boolean heavy = entry.getKey().startsWith("heavy_");
            int durability = switch (type) {
                case HELMET -> heavy ? 611 : 55;
                case CHESTPLATE -> heavy ? 888 : 80;
                case LEGGINGS -> heavy ? 833 : 75;
                case BOOTS -> heavy ? 722 : 65;
                default -> throw new AssertionError("Unexpected armor type");
            };
            var tag = switch (type) {
                case HELMET -> ItemTags.HEAD_ARMOR;
                case CHESTPLATE -> ItemTags.CHEST_ARMOR;
                case LEGGINGS -> ItemTags.LEG_ARMOR;
                case BOOTS -> ItemTags.FOOT_ARMOR;
                default -> throw new AssertionError("Unexpected armor type");
            };
            helper.assertTrue(armor.is(entry.getValue().get()) && armor.getCount() == 1 && armor.getMaxDamage() == durability,
                    "Armor recipe or durability was incorrect");
            helper.assertTrue(armor.is(tag) && armor.getItem().isValidRepairItem(armor, skin)
                    && !armor.getItem().isValidRepairItem(armor, new ItemStack(Items.LEATHER)), "Armor tags or repair ingredient were incorrect");
            for (var enchantment : java.util.List.of(Enchantments.PROTECTION, Enchantments.THORNS, Enchantments.UNBREAKING,
                    Enchantments.MENDING, Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE)) {
                helper.assertTrue(enchantments.getHolderOrThrow(enchantment).value().canEnchant(armor), "Armor rejected an armor enchantment");
            }
            List<ResourceKey<Enchantment>> specialized = switch (type) {
                case HELMET -> List.of(Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY);
                case LEGGINGS -> List.of(Enchantments.SWIFT_SNEAK);
                case BOOTS -> List.of(Enchantments.FEATHER_FALLING, Enchantments.DEPTH_STRIDER,
                        Enchantments.FROST_WALKER, Enchantments.SOUL_SPEED);
                default -> List.of();
            };
            for (var enchantment : specialized) helper.assertTrue(enchantments.getHolderOrThrow(enchantment).value().canEnchant(armor),
                    "Armor rejected a slot-specific enchantment");
            player.setItemSlot(slot, armor);
            player.tick();
            boolean golden = entry.getKey().startsWith("golden_");
            boolean netherite = entry.getKey().startsWith("netherite_") || entry.getKey().startsWith("radiant_") || heavy;
            boolean diamond = entry.getKey().startsWith("diamond_");
            boolean iron = entry.getKey().startsWith("iron_");
            int defense = switch (type) {
                case HELMET -> netherite || diamond ? 3 : golden || iron ? 2 : 1;
                case CHESTPLATE -> netherite || diamond ? 8 : iron ? 6 : golden ? 5 : 3;
                case LEGGINGS -> netherite || diamond ? 6 : iron ? 5 : golden ? 3 : 2;
                case BOOTS -> netherite || diamond ? 3 : iron ? 2 : 1;
                default -> throw new AssertionError("Unexpected armor type");
            };
            helper.assertTrue(player.getArmorValue() == defense && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == (netherite ? 3 : diamond ? 2 : 0),
                    "Armor did not match its expected protection");
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.tick();
            grid.set(0, new ItemStack(Items.LEATHER));
            helper.assertTrue(!recipe.matches(CraftingInput.of(3, pattern.size(), grid), helper.getLevel()), "Armor accepted an incorrect ingredient");
        }
        for (String prefix : List.of("golden_", "netherite_", "radiant_", "heavy_", "copper_", "iron_", "diamond_")) {
            for (var type : PufferfishArmor.TYPES) {
                player.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get(prefix + "pufferfish_skin_" + type.getName()).get()));
            }
            player.tick();
            int protection = switch (prefix) { case "golden_" -> 11; case "copper_" -> 7; case "iron_" -> 15; default -> 20; };
            int toughness = switch (prefix) { case "netherite_", "radiant_", "heavy_" -> 12; case "diamond_" -> 8; default -> 0; };
            helper.assertTrue(player.getArmorValue() == protection,
                    "Metal skin armor set had incorrect total protection");
            helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == toughness,
                    "Metal skin armor set had incorrect total toughness");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heavyArmorWeightAndKnockbackResistanceStackOnlyWhenWorn(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        for (int count = 1; count <= 4; count++) {
            var type = PufferfishArmor.TYPES.get(count - 1);
            player.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("heavy_pufferfish_skin_" + type.getName()).get()));
            player.tick();
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * (1 - 0.1 * count)) < 1.0E-6,
                    "Heavy armor speed penalty did not stack additively per piece");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.15 * count) < 1.0E-6,
                    "Heavy armor knockback resistance did not stack per piece");
        }
        for (int count = 3; count >= 0; count--) {
            var type = PufferfishArmor.TYPES.get(count);
            player.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("netherite_pufferfish_skin_" + type.getName()).get()));
            player.tick();
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * (1 - 0.1 * count)) < 1.0E-6
                            && Math.abs(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) - 0.15 * count) < 1.0E-6,
                    "Replacing heavy armor retained its weight or knockback resistance");
        }
        for (var type : PufferfishArmor.TYPES) for (var hand : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)) {
            player.setItemSlot(hand, new ItemStack(PufferfishArmor.VARIANTS.get("heavy_pufferfish_skin_" + type.getName()).get()));
            player.tick();
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1.0E-6
                            && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0,
                    "Held heavy armor applied worn attributes");
            player.setItemSlot(hand, ItemStack.EMPTY);
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
            var slot = item.get().getEquipmentSlot();
            player.setItemSlot(slot, armor);
            player.tick();
            for (int attempt = 0; attempt < 48; attempt++) {
                player.setHealth(20);
                player.invulnerableTime = 0;
                attacker.setHealth(20);
                attacker.invulnerableTime = 0;
                attacker.removeAllEffects();
                armor.setDamageValue(0);
                player.hurt(player.damageSources().mobAttack(attacker), 2);
                var poison = attacker.getEffect(PufferfishItems.isRadiant(armor) ? MobEffects.WITHER : MobEffects.POISON);
                String name = item.getId().getPath();
                int duration = PufferfishItems.isRadiant(armor) ? 120 : name.startsWith("oxidized_copper_") ? 360 : name.startsWith("weathered_copper_") ? 240
                        : name.startsWith("exposed_copper_") ? 120 : name.startsWith("netherite_") || name.startsWith("heavy_") ? 100 : 60;
                int amplifier = name.startsWith("oxidized_copper_") ? 2 : name.startsWith("weathered_copper_") ? 1 : 0;
                helper.assertTrue(name.startsWith("waxed_") || attacker.getHealth() == 20 ? poison == null
                        : poison != null && poison.getDuration() == duration && poison.getAmplifier() == amplifier,
                        "Skin armor applied the wrong poison effect");
                float damage = 20 - attacker.getHealth();
                helper.assertTrue(damage == 0 || damage >= 1 && damage <= 5, "Armor exceeded native Thorns I damage");
                if (damage > 0) reflected++;
                attempts++;
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
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

    @GameTest(template = "empty")
    public static void fullSetRetaliatesOnlyOncePerHit(GameTestHelper helper) {
        var single = helper.makeMockPlayer(GameType.SURVIVAL);
        var full = helper.makeMockPlayer(GameType.SURVIVAL);
        single.setItemSlot(EquipmentSlot.CHEST, new ItemStack(PufferfishArmor.VARIANTS.get("pufferfish_skin_chestplate").get()));
        for (var type : PufferfishArmor.TYPES) {
            full.setItemSlot(type.getSlot(), new ItemStack(PufferfishArmor.VARIANTS.get("pufferfish_skin_" + type.getName()).get()));
        }
        single.tick();
        full.tick();
        helper.assertTrue(full.getArmorValue() == 7, "Full skin armor set has incorrect protection");
        var attacker = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        attacker.setNoAi(true);
        for (int attempt = 0; attempt < 64; attempt++) {
            float damage = -1;
            for (var player : List.of(single, full)) {
                long seed = 9123L + attempt * 7919L;
                helper.getLevel().getRandom().setSeed(seed);
                player.getRandom().setSeed(seed);
                attacker.getRandom().setSeed(seed);
                player.setHealth(20);
                player.invulnerableTime = 0;
                for (var stack : player.getArmorSlots()) stack.setDamageValue(0);
                attacker.setHealth(20);
                attacker.invulnerableTime = 0;
                attacker.removeAllEffects();
                player.hurt(player.damageSources().mobAttack(attacker), 2);
                if (damage == -1) damage = 20 - attacker.getHealth();
                else helper.assertTrue(damage == 20 - attacker.getHealth(), "Full set multiplied intrinsic retaliation");
            }
        }
        attacker.discard();
        helper.succeed();
    }
}
