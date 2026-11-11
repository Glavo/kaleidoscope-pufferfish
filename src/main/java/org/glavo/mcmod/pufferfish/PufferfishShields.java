// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/// Shields with vanilla mechanics and a permanent pufferfish skin facing.
public final class PufferfishShields {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    public static final Map<String, DeferredItem<ShieldItem>> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, DeferredItem<ShieldItem>>();
        PufferfishVariant.ALL.stream().filter(PufferfishVariant::hasShield).forEach(variant -> register(variants, variant));
        VARIANTS = java.util.Collections.unmodifiableMap(variants);
    }

    private PufferfishShields() {}

    private static void register(Map<String, DeferredItem<ShieldItem>> variants, PufferfishVariant variant) {
        variants.put(variant.skinName() + "_shield", ITEMS.register(variant.skinName() + "_shield",
                () -> {
                    var properties = new Item.Properties().durability(336);
                    if (variant.material().fireResistant()) properties.fireResistant();
                    return variant.bind(new ShieldItem(properties) {
                        @Override
                        public String getDescriptionId(ItemStack stack) { return getDescriptionId(); }
                    });
                }));
    }

    public static void register(IEventBus bus) { ITEMS.register(bus); }

    /// Successful blocks apply material effects and independently roll material-specific thorns.
    public static void retaliate(LivingShieldBlockEvent event) {
        var defender = event.getEntity();
        var source = event.getDamageSource();
        var shield = defender.getUseItem();
        if (!(defender.level() instanceof ServerLevel level) || defender.isSpectator()
                || !event.getBlocked() || event.getBlockedDamage() <= 0 || source.is(DamageTypes.THORNS)
                || !(source.getEntity() instanceof LivingEntity attacker) || attacker == defender
                || !(shield.getItem() instanceof ShieldItem) || PufferfishVariant.of(shield) == null) return;
        PufferfishCombat.applyPassiveEffect(shield, attacker, defender);
        PufferfishRetaliation.thorns(level, PufferfishRetaliation.thornsLevel(shield),
                new EnchantedItemInUse(shield, LivingEntity.getSlotForHand(defender.getUsedItemHand()), defender), defender, source);
    }
}
