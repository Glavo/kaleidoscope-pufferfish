// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
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

    private PufferfishItems() {}

    public static boolean isNetherite(ItemStack stack) {
        return stack.is(NETHERITE.get());
    }

    public static boolean isPufferfish(ItemStack stack) {
        return stack.is(Items.PUFFERFISH) || isNetherite(stack);
    }

    /// Returns whether the stack is either flail variant.
    public static boolean isFlail(ItemStack stack) {
        return stack.is(FLAIL.get()) || stack.is(NETHERITE_FLAIL.get());
    }

    /// Returns whether the stack is a fish or flail with netherite combat properties.
    public static boolean hasNetheriteCombat(ItemStack stack) {
        return isNetherite(stack) || stack.is(NETHERITE_FLAIL.get());
    }

    /// Returns whether the stack supports pufferfish melee damage and poison.
    public static boolean isWeapon(ItemStack stack) {
        return isPufferfish(stack) || isFlail(stack);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
