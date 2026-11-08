// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    /// An always-edible food with golden apple nutrition and effects, eight minutes of
    /// Water Breathing I, and one minute of Poison II. It is not a pufferfish weapon.
    public static final DeferredItem<Item> GOLDEN = ITEMS.register("golden_pufferfish", () -> new Item(
            new Item.Properties().rarity(Rarity.RARE).food(new FoodProperties.Builder()
                    .nutrition(4).saturationModifier(1.2F).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0), 1)
                    .effect(() -> new MobEffectInstance(MobEffects.WATER_BREATHING, 9600, 0), 1)
                    .effect(() -> new MobEffectInstance(MobEffects.POISON, 1200, 1), 1)
                    .build())));
    /// Raw, edible pufferfish meat with two nutrition points and no status effects.
    public static final DeferredItem<Item> MEAT = ITEMS.register("pufferfish_meat", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build())));
    /// The reusable flail crafted from an ordinary or dyed pufferfish and two chains.
    public static final DeferredItem<PufferfishFlailItem> FLAIL = ITEMS.register("pufferfish_flail",
            () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1)));
    /// The fire-resistant flail crafted from a netherite pufferfish and two chains.
    public static final DeferredItem<PufferfishFlailItem> NETHERITE_FLAIL = ITEMS.register("netherite_pufferfish_flail",
            () -> new PufferfishFlailItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final DeferredItem<Item> NETHERITE = ITEMS.register("netherite_pufferfish", () -> new Item(
            new Item.Properties().fireResistant().attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(KaleidoscopePufferfish.id("netherite_head_armor"),
                            3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(KaleidoscopePufferfish.id("netherite_head_toughness"),
                            3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .build())) {
        @Override
        public EquipmentSlot getEquipmentSlot(ItemStack stack) {
            return EquipmentSlot.HEAD;
        }
    });

    /// A fire-resistant mace-strength fish granting head armor, toughness, knockback resistance, and reduced movement speed.
    public static final DeferredItem<HeavyPufferfishItem> HEAVY = ITEMS.register("heavy_pufferfish", () -> new HeavyPufferfishItem(
            new Item.Properties().rarity(Rarity.EPIC).fireResistant().attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(KaleidoscopePufferfish.id("heavy_head_armor"),
                            4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(KaleidoscopePufferfish.id("heavy_head_toughness"),
                            4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(KaleidoscopePufferfish.id("heavy_head_knockback_resistance"),
                            0.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
                    .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(KaleidoscopePufferfish.id("heavy_head_movement_speed"),
                            -0.30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.HEAD)
                    .build())));

    /// Returns whether the stack contains a heavy pufferfish.
    public static boolean isHeavy(ItemStack stack) {
        return stack.is(HEAVY.get());
    }

    private PufferfishItems() {}

    public static boolean isNetherite(ItemStack stack) {
        return stack.is(NETHERITE.get());
    }

    public static boolean isPufferfish(ItemStack stack) {
        return stack.is(Items.PUFFERFISH) || isNetherite(stack) || isHeavy(stack);
    }

    /// Returns whether the stack is either flail variant.
    public static boolean isFlail(ItemStack stack) {
        return stack.is(FLAIL.get()) || stack.is(NETHERITE_FLAIL.get());
    }

    /// Returns whether the stack is a fish or flail with netherite combat properties.
    public static boolean hasNetheriteCombat(ItemStack stack) {
        return isNetherite(stack) || isHeavy(stack) || stack.is(NETHERITE_FLAIL.get());
    }

    /// Returns whether the stack supports pufferfish melee damage and poison.
    public static boolean isWeapon(ItemStack stack) {
        return isPufferfish(stack) || isFlail(stack);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
