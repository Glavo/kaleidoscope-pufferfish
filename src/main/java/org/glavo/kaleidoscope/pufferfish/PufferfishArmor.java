// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishArmor {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, KaleidoscopePufferfish.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    public static final List<ArmorItem.Type> TYPES = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
            ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);
    public static final Map<String, DeferredItem<ArmorItem>> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, DeferredItem<ArmorItem>>();
        add(variants, "", () -> PufferfishBlocks.skin(null).asItem());
        for (var color : DyeColor.values()) add(variants, color.getName() + "_", () -> PufferfishBlocks.skin(color).asItem());
        add(variants, "golden_", () -> PufferfishBlocks.GOLDEN_SKIN.get().asItem());
        for (var skin : PufferfishBlocks.COPPER_SKINS) {
            String prefix = skin.getId().getPath().replace("pufferfish_skin", "");
            add(variants, prefix, () -> skin.get().asItem());
        }
        add(variants, "iron_", () -> PufferfishBlocks.IRON_SKIN.get().asItem());
        add(variants, "diamond_", () -> PufferfishBlocks.DIAMOND_SKIN.get().asItem());
        add(variants, "netherite_", () -> PufferfishBlocks.NETHERITE_SKIN.get().asItem());
        add(variants, "radiant_", () -> PufferfishBlocks.RADIANT_SKIN.get().asItem());
        add(variants, "heavy_", () -> PufferfishBlocks.HEAVY_SKIN.get().asItem());
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishArmor() {}

    private static void add(Map<String, DeferredItem<ArmorItem>> variants, String prefix, Supplier<Item> skin) {
        String name = prefix + "pufferfish_skin";
        var material = MATERIALS.register(name, () -> {
            var protection = switch (prefix) {
                case "golden_" -> ArmorMaterials.GOLD;
                case "iron_" -> ArmorMaterials.IRON;
                case "diamond_" -> ArmorMaterials.DIAMOND;
                case "netherite_", "radiant_", "heavy_" -> ArmorMaterials.NETHERITE;
                default -> ArmorMaterials.LEATHER;
            };
            return new ArmorMaterial(protection.value().defense(), 15,
                    SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(skin.get()),
                    List.of(new ArmorMaterial.Layer(KaleidoscopePufferfish.id(name))), protection.value().toughness(),
                    prefix.equals("heavy_") ? 0.15F : 0);
        });
        for (var type : TYPES) {
            String itemName = name + "_" + type.getName();
            variants.put(itemName, ITEMS.register(itemName, () -> {
                int durability = prefix.equals("heavy_") ? (type.getDurability(37) * 3 + 1) / 2 : type.getDurability(5);
                var properties = new Item.Properties().durability(durability);
                if (prefix.equals("netherite_") || prefix.equals("radiant_") || prefix.equals("heavy_")) properties.fireResistant();
                if (prefix.equals("heavy_")) return new ArmorItem(material, type, properties) {
                    private final ItemAttributeModifiers modifiers = super.getDefaultAttributeModifiers().withModifierAdded(
                            Attributes.MOVEMENT_SPEED, new AttributeModifier(KaleidoscopePufferfish.id(itemName + "_weight"),
                                    -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.bySlot(type.getSlot()));

                    @Override
                    public ItemAttributeModifiers getDefaultAttributeModifiers() { return modifiers; }
                };
                return new ArmorItem(material, type, properties);
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
                || source.is(DamageTypes.THORNS) || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == wearer || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        EnchantedItemInUse thornsItem = null;
        for (var slot : List.of(EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            var stack = wearer.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot
                    || VARIANTS.values().stream().noneMatch(item -> stack.is(item.get()))) continue;
            if (thornsItem == null) thornsItem = new EnchantedItemInUse(stack, slot, wearer);
        }
        if (thornsItem == null) return;
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        thorns.value().doPostAttack(level, 1, thornsItem, EnchantmentTarget.VICTIM, wearer, source);
    }

    @javax.annotation.Nullable
    public static MobEffectInstance poison(LivingEntity wearer) {
        MobEffectInstance poison = null;
        for (var type : TYPES) {
            var stack = wearer.getItemBySlot(type.getSlot());
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getType() != type || PufferfishItems.isRadiant(stack)
                    || VARIANTS.values().stream().noneMatch(item -> stack.is(item.get()))) continue;
            poison = PufferfishRetaliation.strongestPoison(poison, PufferfishRetaliation.effect(stack));
        }
        return poison;
    }

    public static boolean isWearingRadiant(LivingEntity wearer) {
        return TYPES.stream().anyMatch(type -> wearer.getItemBySlot(type.getSlot())
                .is(VARIANTS.get("radiant_pufferfish_skin_" + type.getName()).get()));
    }

}
