// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.glavo.mcmod.pufferfish.PufferfishCultivation;

/// Adds cultivation ranks and hides duplicate creative category annotations.
public final class PufferfishTooltips {
    private PufferfishTooltips() {}

    public static void cultivation(ItemTooltipEvent event) {
        var name = PufferfishCultivation.visibleName(event.getItemStack());
        if (name != null) event.getToolTip().add(Component.translatable("tooltip.pufferfish_please.level", name)
                .withStyle(ChatFormatting.GRAY));
    }

    public static void hideCreativeCategory(RenderTooltipEvent.GatherComponents event) {
        if (event.getItemStack().isEmpty()) return;
        var lines = event.getTooltipElements();
        for (int index = lines.size() - 1; index > 0; index--) {
            var text = lines.get(index).left().orElse(null);
            if (text instanceof Component component
                    && component.getContents() instanceof TranslatableContents translation
                    && translation.getKey().equals("itemGroup.pufferfish_please")
                    && TextColor.fromLegacyFormat(ChatFormatting.BLUE).equals(component.getStyle().getColor())
                    && !component.getStyle().isItalic()) {
                lines.remove(index);
            }
        }
    }
}
