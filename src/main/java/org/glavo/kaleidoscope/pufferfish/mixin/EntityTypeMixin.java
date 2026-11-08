// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.mixin;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.glavo.kaleidoscope.pufferfish.ColoredPufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    @Inject(method = "createDefaultStackConfig", at = @At("RETURN"), cancellable = true)
    private static void kaleidoscope$initializeBucketColor(ServerLevel level, ItemStack stack, Player player,
                                                          CallbackInfoReturnable<Consumer<Entity>> cir) {
        if (stack.is(Items.PUFFERFISH_BUCKET)) {
            var tag = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
            DyeColor color = DyeColor.byName(tag.getString(PufferfishColors.COLOR_TAG), null);
            // Include the color in the initial tracking data, before the entity enters the world.
            cir.setReturnValue(cir.getReturnValue().andThen(entity -> {
                if (entity instanceof ColoredPufferfish fish) {
                    fish.kaleidoscope$setColor(color);
                }
            }));
        }
    }
}
