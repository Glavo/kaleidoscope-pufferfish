// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishArmor {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, PufferfishPlease.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    public static final List<ArmorItem.Type> TYPES = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
            ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);
    public static final Map<String, DeferredItem<ArmorItem>> VARIANTS;
    private static final List<EquipmentSlot> THORNS_SLOT_PRIORITY = List.of(
            EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    static {
        var variants = new LinkedHashMap<String, DeferredItem<ArmorItem>>();
        PufferfishVariant.ALL.forEach(variant -> add(variants, variant));
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishArmor() {}

    private static void add(Map<String, DeferredItem<ArmorItem>> variants, PufferfishVariant variant) {
        String name = variant.skinName();
        var kind = variant.material();
        var material = MATERIALS.register(name, () -> {
            var protection = kind.armorMaterial();
            return new ArmorMaterial(protection.value().defense(), 15,
                    SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(variant.skin()),
                    List.of(new ArmorMaterial.Layer(PufferfishPlease.id(name))), protection.value().toughness(),
                    kind == PufferfishMaterial.HEAVY ? 0.15F : 0);
        });
        for (var type : TYPES) {
            String itemName = name + "_" + type.getName();
            variants.put(itemName, ITEMS.register(itemName, () -> {
                int durability = kind == PufferfishMaterial.HEAVY ? (type.getDurability(37) * 3 + 1) / 2 : type.getDurability(5);
                var properties = new Item.Properties().durability(durability);
                if (kind.fireResistant()) properties.fireResistant();
                if (kind == PufferfishMaterial.HEAVY) return variant.bind(new ArmorItem(material, type, properties) {
                    private final ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers().withModifierAdded(
                            Attributes.MOVEMENT_SPEED, new AttributeModifier(PufferfishPlease.id(itemName + "_weight"),
                                    -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.bySlot(type.getSlot()));

                    @Override
                    public ItemAttributeModifiers getDefaultAttributeModifiers() { return modifiers; }
                });
                return variant.bind(new ArmorItem(material, type, properties));
            }));
        }
    }

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
        ITEMS.register(bus);
    }

    /// Worn skin armor retaliates once per hit, regardless of the number of equipped pieces.
    public static void retaliate(LivingDamageEvent.Post event) {
        var wearer = event.getEntity();
        var source = event.getSource();
        if (!(wearer.level() instanceof ServerLevel level) || wearer.isSpectator()
                || !PufferfishRetaliation.canRetaliate(event)) return;
        EnchantedItemInUse thornsItem = null;
        int strength = 0;
        for (var slot : THORNS_SLOT_PRIORITY) {
            var stack = wearer.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot
                    || PufferfishVariant.of(stack) == null) continue;
            int levelForItem = PufferfishRetaliation.thornsLevel(stack);
            if (levelForItem > strength) {
                strength = levelForItem;
                thornsItem = new EnchantedItemInUse(stack, slot, wearer);
            }
        }
        if (thornsItem == null) return;
        PufferfishRetaliation.thorns(level, strength, thornsItem, wearer, source);
    }

    @javax.annotation.Nullable
    public static PufferfishPoison poison(LivingEntity wearer) {
        PufferfishPoison poison = null;
        for (var type : TYPES) {
            var stack = wearer.getItemBySlot(type.getSlot());
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getType() != type || PufferfishItems.isRadiant(stack)
                    || PufferfishVariant.of(stack) == null) continue;
            poison = PufferfishPoison.strongest(poison, PufferfishPoison.of(stack));
        }
        return poison;
    }

    public static boolean isWearingRadiant(LivingEntity wearer) {
        return TYPES.stream().anyMatch(type -> {
            var stack = wearer.getItemBySlot(type.getSlot());
            return stack.getItem() instanceof ArmorItem armor && armor.getType() == type && PufferfishItems.isRadiant(stack);
        });
    }

}
