// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/// Hides the creative category annotation while preserving names and mod attribution.
public final class PufferfishTooltips {
    private PufferfishTooltips() {}

    public static void hideCreativeCategory(RenderTooltipEvent.GatherComponents event) {
        if (event.getItemStack().isEmpty()) return;
        var lines = event.getTooltipElements();
        for (int index = lines.size() - 1; index > 0; index--) {
            var text = lines.get(index).left().orElse(null);
            if (text instanceof Component component
                    && component.getContents() instanceof TranslatableContents translation
                    && translation.getKey().equals("itemGroup.kaleidoscope_pufferfish")
                    && TextColor.fromLegacyFormat(ChatFormatting.BLUE).equals(component.getStyle().getColor())
                    && !component.getStyle().isItalic()) {
                lines.remove(index);
            }
        }
    }
}
