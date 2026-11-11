// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PufferfishCatArmor {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, PufferfishPlease.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PufferfishPlease.MODID);
    public static final Map<String, DeferredItem<PufferfishCatArmorItem>> VARIANTS;

    static {
        var variants = new LinkedHashMap<String, DeferredItem<PufferfishCatArmorItem>>();
        for (var variant : PufferfishVariant.ALL) {
            String name = variant.skinName() + "_cat_armor";
            var kind = variant.material();
            var material = MATERIALS.register(name, () -> {
                var protection = kind.armorMaterial().value();
                return new ArmorMaterial(Map.of(ArmorItem.Type.BODY, protection.defense().get(ArmorItem.Type.CHESTPLATE)),
                        0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(variant.skin()), List.of(),
                        protection.toughness(), kind == PufferfishMaterial.HEAVY ? 0.15F : 0);
            });
            variants.put(name, ITEMS.register(name, () -> {
                var properties = new Item.Properties().durability(kind == PufferfishMaterial.HEAVY ? 888 : 80);
                if (kind.fireResistant()) properties.fireResistant();
                return variant.bind(new PufferfishCatArmorItem(material, properties));
            }));
        }
        VARIANTS = Collections.unmodifiableMap(variants);
    }

    private PufferfishCatArmor() {}

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
        ITEMS.register(bus);
        NeoForge.EVENT_BUS.addListener(PufferfishCatArmor::interact);
        NeoForge.EVENT_BUS.addListener(PufferfishCatArmor::retaliate);
    }

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        var player = event.getEntity();
        if (!(event.getTarget() instanceof Cat cat) || !cat.isOwnedBy(player) || player.isSpectator() || !player.mayBuild()) return;
        var held = event.getItemStack();
        var armor = cat.getBodyArmorItem();
        boolean equip = held.getItem() instanceof PufferfishCatArmorItem && armor.isEmpty();
        boolean remove = armor.getItem() instanceof PufferfishCatArmorItem && held.is(Items.SHEARS);
        boolean repair = armor.getItem() instanceof PufferfishCatArmorItem && armor.isDamaged()
                && armor.getItem().isValidRepairItem(armor, held);
        if (!equip && !remove && !repair) return;
        if (!player.level().isClientSide()) {
            if (equip) {
                cat.setBodyArmorItem(held.copyWithCount(1));
                held.consume(1, player);
                if (player instanceof ServerPlayer serverPlayer) PufferfishAdvancements.EQUIP_CAT_ARMOR.get().trigger(serverPlayer);
            } else if (remove) {
                cat.setBodyArmorItem(ItemStack.EMPTY);
                player.getInventory().add(armor);
                if (!armor.isEmpty()) player.drop(armor, false);
                held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
            } else {
                armor.setDamageValue(Math.max(0, armor.getDamageValue() - (armor.getMaxDamage() + 3) / 4));
                held.consume(1, player);
            }
            cat.playSound(remove ? SoundEvents.SHEEP_SHEAR : SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1, 1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
    }

    public static void retaliate(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide() || !PufferfishRetaliation.canRetaliate(event)) return;
        var armor = cat.getBodyArmorItem();
        var source = event.getSource();
        if (!(armor.getItem() instanceof PufferfishCatArmorItem)
                || !(source.getDirectEntity() instanceof LivingEntity attacker)
                || !source.is(DamageTypes.MOB_ATTACK) && !source.is(DamageTypes.PLAYER_ATTACK)
                || cat.isAlliedTo(attacker) || attacker instanceof TamableAnimal pet && cat.getOwnerUUID() != null
                && cat.getOwnerUUID().equals(pet.getOwnerUUID())) return;
        if (attacker.hurt(cat.damageSources().thorns(cat), PufferfishRetaliation.thornsLevel(armor))) {
            if (PufferfishItems.isRadiant(armor)) {
                attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, PufferfishCombat.STATUS_EFFECT_DURATION_TICKS, 1), cat);
                PufferfishCombat.applyRadiantGlow(attacker, cat);
            } else PufferfishCombat.applyPassiveEffect(armor, attacker, cat);
        }
    }
}
