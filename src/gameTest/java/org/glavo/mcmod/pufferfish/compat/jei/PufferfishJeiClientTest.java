// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.jei;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.glavo.mcmod.pufferfish.PufferfishItems;
import org.glavo.mcmod.pufferfish.PufferfishPlease;

@JeiPlugin
public final class PufferfishJeiClientTest implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return PufferfishPlease.id("jei_client_test");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!Boolean.getBoolean("pufferfish_please.jeiSmokeTest")) return;
        var manager = runtime.getIngredientManager();
        var flails = manager.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .filter(stack -> stack.is(PufferfishItems.FLAIL.get())).toList();
        var colors = new HashSet<DyeColor>();
        flails.forEach(stack -> colors.add(stack.get(PufferfishPlease.COLOR.get())));
        if (flails.size() != 17 || colors.size() != 17 || !colors.contains(null)) {
            throw new IllegalStateException("JEI must index the plain flail and all 16 dyed flails");
        }
        var helper = manager.getIngredientHelper(VanillaTypes.ITEM_STACK);
        for (var context : UidContext.values()) {
            var ids = new HashSet<Object>();
            for (var stack : flails) {
                var id = helper.getUid(stack, context);
                if (!ids.add(id)) throw new IllegalStateException("JEI merged distinct flail colors: " + context);
                var damaged = stack.copy();
                damaged.setDamageValue(1);
                if (!id.equals(helper.getUid(damaged, context))) {
                    throw new IllegalStateException("Flail damage must not change its JEI subtype: " + context);
                }
            }
        }
        LogUtils.getLogger().info("Pufferfish JEI smoke test passed: all 17 flail variants are distinct and ignore damage");
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
    }
}
