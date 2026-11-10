// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

public enum PufferfishMaterial {
    NORMAL(64), COPPER(128), IRON(250), GOLDEN(32), DIAMOND(1561), NETHERITE(2031), HEAVY(3047), RADIANT(2031);

    private final int fishDurability;

    PufferfishMaterial(int fishDurability) { this.fishDurability = fishDurability; }

    public int fishDurability() { return fishDurability; }

    public boolean fireResistant() { return this == NETHERITE || this == HEAVY || this == RADIANT; }

    public Holder<ArmorMaterial> armorMaterial() {
        return switch (this) {
            case IRON -> ArmorMaterials.IRON;
            case GOLDEN -> ArmorMaterials.GOLD;
            case DIAMOND -> ArmorMaterials.DIAMOND;
            case NETHERITE, HEAVY, RADIANT -> ArmorMaterials.NETHERITE;
            default -> ArmorMaterials.LEATHER;
        };
    }

    public int headThorns() {
        return switch (this) {
            case NETHERITE, HEAVY, RADIANT -> 3;
            default -> skinThorns();
        };
    }

    public int skinThorns() { return this == DIAMOND ? 2 : 1; }

    public int mobThorns() {
        return switch (this) {
            case NORMAL -> 0;
            case COPPER, IRON, GOLDEN -> 1;
            case DIAMOND, NETHERITE -> 2;
            case HEAVY, RADIANT -> 3;
        };
    }
}
