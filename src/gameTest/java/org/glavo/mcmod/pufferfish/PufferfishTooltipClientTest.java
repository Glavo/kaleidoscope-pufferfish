// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/// Exercises the registered render event using the actual creative-tab title.
public final class PufferfishTooltipClientTest {
    private PufferfishTooltipClientTest() {}

    public static void verify() {
        verifyCultivation();
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(PufferfishPlease.id("pufferfish"));
        var category = tab.getDisplayName().copy().withStyle(ChatFormatting.BLUE);
        var modName = Component.literal(category.getString()).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
        var literalLore = Component.literal(category.getString()).withStyle(ChatFormatting.BLUE);
        var otherCategory = Component.translatable("itemGroup.tools").withStyle(ChatFormatting.BLUE);
        var enchantment = Component.translatable("enchantment.minecraft.loyalty");
        var items = new ArrayList<ItemStack>();
        BuiltInRegistries.ITEM.stream().filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(PufferfishPlease.MODID))
                .forEach(item -> items.add(new ItemStack(item)));
        items.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.BLUE));
        items.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH_BUCKET), DyeColor.BLUE));
        for (var stack : items) {
            var title = stack.getHoverName();
            var event = event(stack, List.of(title, category, otherCategory, enchantment, literalLore, modName));
            NeoForge.EVENT_BUS.post(event);
            var actual = event.getTooltipElements().stream().map(element -> element.left().orElseThrow()).toList();
            if (!actual.equals(List.of(title, otherCategory, enchantment, literalLore, modName))) {
                throw new IllegalStateException("Creative category filtering changed item details or mod attribution");
            }
            var nameOnly = event(stack, List.of(category, modName));
            NeoForge.EVENT_BUS.post(nameOnly);
            if (nameOnly.getTooltipElements().size() != 2) {
                throw new IllegalStateException("Creative category filtering removed an item title");
            }
        }
        var tabTooltip = event(ItemStack.EMPTY, List.of(Component.literal("Tab"), category));
        NeoForge.EVENT_BUS.post(tabTooltip);
        if (tabTooltip.getTooltipElements().size() != 2 || !tab.getDisplayName().getString().equals(category.getString())) {
            throw new IllegalStateException("Creative category filtering changed the tab's own tooltip");
        }
    }

    private static RenderTooltipEvent.GatherComponents event(ItemStack stack, List<Component> lines) {
        var elements = new ArrayList<Either<FormattedText, TooltipComponent>>();
        lines.forEach(line -> elements.add(Either.left(line)));
        return new RenderTooltipEvent.GatherComponents(stack, 400, 240, elements, -1);
    }

    private static void verifyCultivation() {
        for (var fish : org.glavo.mcmod.pufferfish.test.TestItems.fishVariants()) {
            var original = fish.copy();
            if (tooltip(fish).contains(Component.translatable("tooltip.pufferfish_please.realm", PufferfishRealm.LEVEL_0.displayName()).getString())
                    || !ItemStack.matches(fish, original)) {
                throw new IllegalStateException("Fresh fish displayed a realm or changed during tooltip generation");
            }
            PufferfishDurability.activate(fish);
            var initialRealm = Component.translatable("tooltip.pufferfish_please.realm", PufferfishRealm.LEVEL_0.displayName()).getString();
            if (!tooltip(fish).contains(initialRealm) || fish.has(PufferfishPlease.CULTIVATION.get())) {
                throw new IllegalStateException("Activated fish did not display level zero without cultivation data");
            }
            fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishRealm.LEVEL_2, 0.25, 12345));
            var first = tooltip(fish);
            var advancedRealm = Component.translatable("tooltip.pufferfish_please.realm", PufferfishRealm.LEVEL_2.displayName()).getString();
            if (!first.contains(advancedRealm) || first.contains(initialRealm)) throw new IllegalStateException("Cultivation realm did not replace level zero");
            fish.set(PufferfishPlease.CULTIVATION.get(), new PufferfishCultivation(PufferfishRealm.LEVEL_2, 0.875, 98765));
            if (!first.equals(tooltip(fish))) throw new IllegalStateException("Tooltip exposed talent or unspecified experience progress");
        }
    }

    private static List<String> tooltip(ItemStack stack) {
        return stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, null,
                net.minecraft.world.item.TooltipFlag.NORMAL).stream().map(Component::getString).toList();
    }
}
