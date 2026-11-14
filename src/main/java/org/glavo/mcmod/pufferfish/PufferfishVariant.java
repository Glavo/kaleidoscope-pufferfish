// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/// Shared variant identity and presentation order. Copper indices retain the oxidation and wax layout.
public record PufferfishVariant(String fishName, PufferfishMaterial material, @Nullable DyeColor color, int copperIndex) {
    public static final PufferfishVariant NORMAL = material("pufferfish", PufferfishMaterial.NORMAL);
    public static final Map<DyeColor, PufferfishVariant> DYED;
    public static final List<PufferfishVariant> COPPER;
    public static final PufferfishVariant IRON = material("iron_pufferfish", PufferfishMaterial.IRON);
    public static final PufferfishVariant GOLDEN = material("golden_pufferfish", PufferfishMaterial.GOLDEN);
    public static final PufferfishVariant DIAMOND = material("diamond_pufferfish", PufferfishMaterial.DIAMOND);
    public static final PufferfishVariant NETHERITE = material("netherite_pufferfish", PufferfishMaterial.NETHERITE);
    public static final PufferfishVariant HEAVY = material("heavy_pufferfish", PufferfishMaterial.HEAVY);
    public static final PufferfishVariant RADIANT = material("radiant_pufferfish", PufferfishMaterial.RADIANT);
    public static final List<PufferfishVariant> ALL;
    private static final Map<Item, PufferfishVariant> ITEMS = new IdentityHashMap<>();

    static {
        var dyed = new LinkedHashMap<DyeColor, PufferfishVariant>();
        for (var color : List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK,
                DyeColor.BROWN, DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW,
                DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE,
                DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK)) {
            dyed.put(color, new PufferfishVariant(color.getName() + "_pufferfish", PufferfishMaterial.NORMAL, color, -1));
        }
        DYED = java.util.Collections.unmodifiableMap(dyed);
        var copper = new ArrayList<PufferfishVariant>();
        for (var name : List.of("copper", "exposed_copper", "weathered_copper", "oxidized_copper",
                "waxed_copper", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper")) {
            copper.add(new PufferfishVariant(name + "_pufferfish", PufferfishMaterial.COPPER, null, copper.size()));
        }
        COPPER = List.copyOf(copper);
        var all = new ArrayList<PufferfishVariant>();
        all.add(NORMAL);
        all.addAll(DYED.values());
        all.addAll(COPPER);
        all.addAll(List.of(IRON, GOLDEN, DIAMOND, NETHERITE, HEAVY, RADIANT));
        ALL = List.copyOf(all);
    }

    private static PufferfishVariant material(String name, PufferfishMaterial material) {
        return new PufferfishVariant(name, material, null, -1);
    }

    public String skinName() { return fishName + "_skin"; }

    public boolean waxed() { return copperIndex >= 4; }

    public int oxidation() { return copperIndex < 0 ? 0 : copperIndex % 4; }

    /// Only the base copper shield is currently registered; all skin and boat variants remain available.
    public boolean hasShield() { return copperIndex <= 0; }

    public boolean hasLamp() { return material != PufferfishMaterial.RADIANT; }

    /// Associates an item at construction time, without resolving other deferred registrations.
    public <T extends Item> T bind(T item) {
        var previous = ITEMS.putIfAbsent(item, this);
        if (previous != null && previous != this) throw new IllegalStateException("Conflicting pufferfish variant: " + fishName);
        return item;
    }

    /// Resolves fish and derivative identity without allocating temporary stacks.
    @Nullable
    public static PufferfishVariant of(ItemStack stack) {
        var item = stack.getItem() instanceof PufferfishFlailItem flail ? flail.fishItem() : stack.getItem();
        if (item == Items.PUFFERFISH || item == Items.PUFFERFISH_BUCKET || item == Items.PUFFERFISH_SPAWN_EGG) {
            @Nullable var color = stack.get(PufferfishPlease.COLOR.get());
            return color == null ? NORMAL : DYED.get(color);
        }
        return ITEMS.get(item);
    }

    public Item fishItem() {
        return switch (material) {
            case NORMAL -> Items.PUFFERFISH;
            case COPPER -> PufferfishItems.COPPER_VARIANTS.get(copperIndex).get();
            case IRON -> PufferfishItems.IRON.get();
            case GOLDEN -> PufferfishItems.GOLDEN.get();
            case DIAMOND -> PufferfishItems.DIAMOND.get();
            case NETHERITE -> PufferfishItems.NETHERITE.get();
            case HEAVY -> PufferfishItems.HEAVY.get();
            case RADIANT -> PufferfishItems.RADIANT.get();
        };
    }

    public ItemStack fish() {
        var stack = new ItemStack(fishItem());
        return color == null ? stack : PufferfishColors.dye(stack, color);
    }

    public PufferfishSkinBlock skin() {
        return switch (material) {
            case NORMAL -> PufferfishBlocks.skin(color);
            case COPPER -> PufferfishBlocks.COPPER_SKINS.get(copperIndex).get();
            case IRON -> PufferfishBlocks.IRON_SKIN.get();
            case GOLDEN -> PufferfishBlocks.GOLDEN_SKIN.get();
            case DIAMOND -> PufferfishBlocks.DIAMOND_SKIN.get();
            case NETHERITE -> PufferfishBlocks.NETHERITE_SKIN.get();
            case HEAVY -> PufferfishBlocks.HEAVY_SKIN.get();
            case RADIANT -> PufferfishBlocks.RADIANT_SKIN.get();
        };
    }
}
