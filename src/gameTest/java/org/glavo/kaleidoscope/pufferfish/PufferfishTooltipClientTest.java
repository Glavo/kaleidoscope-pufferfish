// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

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
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(KaleidoscopePufferfish.id("pufferfish"));
        var category = tab.getDisplayName().copy().withStyle(ChatFormatting.BLUE);
        var modName = Component.literal(category.getString()).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
        var literalLore = Component.literal(category.getString()).withStyle(ChatFormatting.BLUE);
        var otherCategory = Component.translatable("itemGroup.tools").withStyle(ChatFormatting.BLUE);
        var enchantment = Component.translatable("enchantment.minecraft.loyalty");
        var items = new ArrayList<ItemStack>();
        BuiltInRegistries.ITEM.stream().filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(KaleidoscopePufferfish.MODID))
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
}
