// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PufferfishPlease.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishVariantGameTests {
    @GameTest(template = "empty")
    public static void registeredFamiliesRetainTheirVariantIdentity(GameTestHelper helper) {
        helper.assertTrue(!PufferfishVariant.ALL.isEmpty(), "No variants were registered");
        helper.assertTrue(PufferfishVariant.ALL.stream().distinct().count() == PufferfishVariant.ALL.size(), "Duplicate variant identity");
        helper.assertTrue(PufferfishArmor.VARIANTS.size() == PufferfishVariant.ALL.size() * PufferfishArmor.TYPES.size(), "Incomplete armor families");
        helper.assertTrue(PufferfishShields.VARIANTS.size() == PufferfishVariant.ALL.stream().filter(PufferfishVariant::hasShield).count(), "Incomplete shield families");
        helper.assertTrue(PufferfishBoats.VARIANTS.size() == PufferfishVariant.ALL.size(), "Incomplete boat families");
        helper.assertTrue(PufferfishLamps.VARIANTS.size() == PufferfishVariant.ALL.stream().filter(PufferfishVariant::hasLamp).count(), "Incomplete lamp families");
        for (var variant : PufferfishVariant.ALL) {
            helper.assertTrue(PufferfishVariant.of(variant.fish()) == variant, "Fish identity: " + variant.fishName());
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(variant.skin()).equals(PufferfishPlease.id(variant.skinName())),
                    "Skin block and item IDs differ: " + variant.skinName());
            checkItem(helper, variant, variant.skinName());
            for (var suffix : List.of("helmet", "chestplate", "leggings", "boots", "boat")) {
                checkItem(helper, variant, variant.skinName() + "_" + suffix);
            }
            if (variant.hasShield()) checkItem(helper, variant, variant.skinName() + "_shield");
            if (variant.hasLamp()) checkItem(helper, variant, variant.fishName() + "_lamp");
        }
        PufferfishMobs.VARIANTS.values().forEach(mob -> {
            var variant = PufferfishVariant.of(mob.fish().toStack());
            checkItem(helper, variant, variant.fishName() + "_bucket");
            checkItem(helper, variant, variant.fishName() + "_spawn_egg");
        });
        helper.succeed();
    }

    private static void checkItem(GameTestHelper helper, PufferfishVariant variant, String name) {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(PufferfishPlease.id(name)));
        helper.assertTrue(!stack.isEmpty() && PufferfishVariant.of(stack) == variant, "Missing or incorrect variant: " + name);
        helper.assertTrue(!PufferfishItems.isPufferfish(stack), "Derived item classified as a whole fish: " + name);
        helper.assertTrue(CopperPufferfish.index(stack) == -1, "Derived item classified as a copper fish: " + name);
    }

    @GameTest(template = "empty")
    public static void wholeFishAndFlailsPreserveColorAndCopperState(GameTestHelper helper) {
        for (int i = 0; i < 8; i++) {
            var fish = PufferfishItems.COPPER_VARIANTS.get(i).toStack();
            var variant = PufferfishVariant.of(fish);
            helper.assertTrue(CopperPufferfish.index(fish) == i && variant.oxidation() == i % 4
                    && variant.waxed() == (i >= 4), "Copper state changed: " + i);
            helper.assertTrue(CopperPufferfish.skinIndex(new ItemStack(variant.skin())) == i, "Copper skin state changed: " + i);
            helper.assertTrue(PufferfishItems.materialName(fish).equals(
                    List.of("copper", "exposed_copper", "weathered_copper", "oxidized_copper").get(i % 4)),
                    "Waxed copper must reuse its oxidation stage texture: " + i);
        }
        for (var flail : PufferfishItems.FLAIL_VARIANTS) {
            var stack = flail.toStack();
            helper.assertTrue(!PufferfishItems.isPufferfish(stack) && PufferfishItems.isWeapon(stack), "Flail classification changed");
            helper.assertTrue(PufferfishVariant.of(stack) == PufferfishVariant.of(flail.get().fish(stack)), "Flail lost its material");
        }
        for (var color : DyeColor.values()) {
            var expected = PufferfishVariant.DYED.get(color);
            var flail = PufferfishItems.FLAIL.toStack();
            flail.set(PufferfishPlease.COLOR.get(), color);
            helper.assertTrue(PufferfishVariant.of(flail) == expected, "Flail lost its dye color");
            helper.assertTrue(PufferfishVariant.of(PufferfishMobs.dyedEgg(color)) == expected, "Egg lost its dye color");
            var bucket = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), color);
            helper.assertTrue(PufferfishVariant.of(bucket) == expected, "Bucket lost its dye color");
        }
        helper.assertTrue(PufferfishVariant.of(PufferfishItems.SASHIMI.toStack()) == null, "Food classified as equipment");
        helper.assertTrue(PufferfishVariant.of(new ItemStack(Items.COPPER_INGOT)) == null, "Vanilla metal classified as equipment");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialOrderIsSharedAcrossRegisteredFamilies(GameTestHelper helper) {
        var expected = PufferfishVariant.ALL.stream().filter(variant -> variant.material() != PufferfishMaterial.NORMAL)
                .map(PufferfishVariant::fishName).toList();
        helper.assertTrue(List.copyOf(PufferfishMobs.VARIANTS.keySet()).equals(expected), "Mob order differs from equipment");
        var skins = PufferfishVariant.ALL.stream().map(PufferfishVariant::skinName).toList();
        helper.assertTrue(PufferfishBoats.VARIANTS.keySet().stream().map(name -> name.replace("_boat", "")).toList().equals(skins),
                "Boat order differs from skins");
        helper.succeed();
    }
}
