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
        var fish = new ItemStack(Items.PUFFERFISH);
        PufferfishDurability.activate(fish);
        zombie.setItemSlot(EquipmentSlot.HEAD, fish);
    }
}
