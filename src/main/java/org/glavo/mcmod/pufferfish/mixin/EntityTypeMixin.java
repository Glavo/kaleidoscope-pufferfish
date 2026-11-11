// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.component.CustomData;
import org.glavo.mcmod.pufferfish.PufferfishColors;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.PufferfishVariant;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin {
    @ModifyReturnValue(method = "createDefaultStackConfig", at = @At("RETURN"))
    private static Consumer<Entity> pufferfishPlease$initializeFish(Consumer<Entity> original, ServerLevel level,
                                                                  ItemStack stack, Player player) {
        if (stack.is(Items.PUFFERFISH_BUCKET)) {
            var tag = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
            DyeColor color = DyeColor.byName(tag.getString(PufferfishColors.COLOR_TAG), null);
            // Include the color in the initial tracking data, before the entity enters the world.
            return original.andThen(entity -> {
                if (entity instanceof Pufferfish fish) {
                    PufferfishColors.set(fish, color);
                }
            });
        }
        if (stack.is(Items.PUFFERFISH_SPAWN_EGG) && stack.has(PufferfishPlease.COLOR.get())) {
            var color = stack.get(PufferfishPlease.COLOR.get());
            return original.andThen(entity -> {
                if (entity instanceof Pufferfish fish) PufferfishColors.set(fish, color);
            });
        }
        if (stack.getItem() instanceof MobBucketItem && PufferfishVariant.of(stack) != null) {
            var tag = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
            return original.andThen(entity -> {
                if (entity instanceof MaterialPufferfish fish) fish.loadFish(tag);
            });
        }
        return original;
    }
}
