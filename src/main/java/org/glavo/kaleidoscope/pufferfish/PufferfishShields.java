// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/// Shields with vanilla mechanics and a permanent pufferfish skin facing.
public final class PufferfishShields {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KaleidoscopePufferfish.MODID);
    public static final Map<String, DeferredItem<ShieldItem>> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, DeferredItem<ShieldItem>>();
        register(variants, "");
        for (var color : DyeColor.values()) register(variants, color.getName() + "_");
        register(variants, "golden_");
        register(variants, "copper_");
        register(variants, "iron_");
        register(variants, "diamond_");
        register(variants, "netherite_");
        register(variants, "radiant_");
        register(variants, "heavy_");
        VARIANTS = java.util.Collections.unmodifiableMap(variants);
    }

    private PufferfishShields() {}

    private static void register(Map<String, DeferredItem<ShieldItem>> variants, String prefix) {
        variants.put(prefix + "pufferfish_skin_shield", ITEMS.register(prefix + "pufferfish_skin_shield",
                () -> {
                    var properties = new Item.Properties().durability(336);
                    if (prefix.equals("netherite_") || prefix.equals("radiant_") || prefix.equals("heavy_")) properties.fireResistant();
                    return new ShieldItem(properties) {
                        @Override
                        public String getDescriptionId(ItemStack stack) { return getDescriptionId(); }
                    };
                }));
    }

    public static void register(IEventBus bus) { ITEMS.register(bus); }

    /// Successful blocks apply the skin effect and roll vanilla Thorns I using the blocking hand.
    public static void retaliate(LivingShieldBlockEvent event) {
        var defender = event.getEntity();
        var source = event.getDamageSource();
        var shield = defender.getUseItem();
        if (!(defender.level() instanceof ServerLevel level) || defender.isSpectator()
                || !event.getBlocked() || event.getBlockedDamage() <= 0 || source.is(DamageTypes.THORNS)
                || !(source.getEntity() instanceof LivingEntity attacker) || attacker == defender
                || VARIANTS.values().stream().noneMatch(item -> shield.is(item.get()))) return;
        attacker.addEffect(new MobEffectInstance(PufferfishItems.isRadiant(shield) ? MobEffects.WITHER : MobEffects.POISON,
                PufferfishCombat.POISON_DURATION_TICKS, PufferfishItems.isRadiant(shield) ? 1 : 0), defender);
        var thorns = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.THORNS);
        thorns.value().doPostAttack(level, 1,
                new EnchantedItemInUse(shield, LivingEntity.getSlotForHand(defender.getUsedItemHand()), defender),
                EnchantmentTarget.VICTIM, defender, source);
    }
}
