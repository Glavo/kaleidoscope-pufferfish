// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class PufferfishColors {
    public static final String COLOR_TAG = KaleidoscopePufferfish.MODID + ":color";

    private PufferfishColors() {}

    public static boolean supports(ItemStack stack) {
        return stack.is(Items.PUFFERFISH) || stack.is(Items.PUFFERFISH_BUCKET);
    }

    @Nullable
    public static DyeColor get(ItemStack stack) {
        return supports(stack) ? stack.get(KaleidoscopePufferfish.COLOR.get()) : null;
    }

    public static ItemStack dye(ItemStack stack, DyeColor color) {
        stack.set(KaleidoscopePufferfish.COLOR.get(), color);
        if (stack.is(Items.PUFFERFISH_BUCKET)) {
            CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, tag -> tag.putString(COLOR_TAG, color.getName()));
        }
        return stack;
    }

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Pufferfish fish) || event.getEntity().isSpectator()
                || !event.getEntity().mayBuild()) {
            return;
        }
        DyeColor color = DyeColor.getColor(event.getItemStack());
        ColoredPufferfish colored = (ColoredPufferfish) fish;
        if (color == null || color == colored.kaleidoscope$getColor()) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            colored.kaleidoscope$setColor(color);
            fish.setPersistenceRequired();
            event.getLevel().playSound(null, fish, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!event.getEntity().getAbilities().instabuild) {
                event.getItemStack().shrink(1);
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }

    public static void colorDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Pufferfish fish) {
            DyeColor color = ((ColoredPufferfish) fish).kaleidoscope$getColor();
            if (color != null) {
                for (var drop : event.getDrops()) {
                    if (drop.getItem().is(Items.PUFFERFISH)) {
                        drop.setItem(dye(drop.getItem().copy(), color));
                    }
                }
            }
        }
    }
}
