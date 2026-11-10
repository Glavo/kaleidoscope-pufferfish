// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

public final class PufferfishZombieHeadgear {
    public enum Variant { NORMAL, COPPER, IRON, GOLDEN, DIAMOND }

    private static final String PENDING = KaleidoscopePufferfish.MODID + ":pending_zombie_headgear";

    private PufferfishZombieHeadgear() {}

    public static void prepare(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() == EntityType.ZOMBIE && event.getSpawnType() == MobSpawnType.NATURAL) {
            event.getEntity().getPersistentData().putBoolean(PENDING, true);
        }
    }

    public static void equip(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Zombie zombie)
                || zombie.getType() != EntityType.ZOMBIE) return;
        boolean pending = zombie.getPersistentData().getBoolean(PENDING);
        zombie.getPersistentData().remove(PENDING);
        if (!pending || event.loadedFromDisk() || !zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || zombie.getRandom().nextDouble() >= PufferfishMobConfig.value(PufferfishMobConfig.ZOMBIE_HEADGEAR_CHANCE)) return;
        var weights = new java.util.EnumMap<Variant, Integer>(Variant.class);
        PufferfishMobConfig.ZOMBIE_HEADGEAR_WEIGHTS.forEach((variant, weight) -> weights.put(variant, PufferfishMobConfig.value(weight)));
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return;
        int roll = zombie.getRandom().nextInt(total);
        for (var entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll >= 0) continue;
            var item = switch (entry.getKey()) {
                case NORMAL -> Items.PUFFERFISH;
                case COPPER -> PufferfishItems.COPPER_VARIANTS.get(zombie.getRandom().nextInt(PufferfishItems.COPPER_VARIANTS.size())).get();
                case IRON -> PufferfishItems.IRON.get();
                case GOLDEN -> PufferfishItems.GOLDEN.get();
                case DIAMOND -> PufferfishItems.DIAMOND.get();
            };
            var fish = new ItemStack(item);
            PufferfishDurability.activate(fish);
            zombie.setItemSlot(EquipmentSlot.HEAD, fish);
            return;
        }
    }
}
