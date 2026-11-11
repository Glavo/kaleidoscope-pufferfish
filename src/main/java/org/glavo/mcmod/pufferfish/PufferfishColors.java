// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
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
    public static final String COLOR_TAG = PufferfishPlease.MODID + ":color";

    private PufferfishColors() {}

    public static boolean supports(ItemStack stack) {
        return stack.is(Items.PUFFERFISH) || stack.is(Items.PUFFERFISH_BUCKET);
    }

    @Nullable
    public static DyeColor get(ItemStack stack) {
        return supports(stack) ? stack.get(PufferfishPlease.COLOR.get()) : null;
    }

    @Nullable
    public static DyeColor get(Pufferfish fish) {
        return fish.getExistingDataOrNull(PufferfishPlease.ENTITY_COLOR);
    }

    public static void set(Pufferfish fish, @Nullable DyeColor color) {
        if (!Objects.equals(get(fish), color)) {
            if (color == null) {
                fish.removeData(PufferfishPlease.ENTITY_COLOR);
            } else {
                fish.setData(PufferfishPlease.ENTITY_COLOR, color);
            }
        }
    }

    public static MutableComponent name(ItemStack stack, DyeColor color) {
        String key = stack.is(Items.PUFFERFISH_BUCKET) ? "dyed_pufferfish_bucket" : "dyed_pufferfish";
        return Component.translatable("item.pufferfish_please." + key,
                Component.translatable("color.minecraft." + color.getName()));
    }

    public static ItemStack dye(ItemStack stack, DyeColor color) {
        stack.set(PufferfishPlease.COLOR.get(), color);
        Component previousName = stack.get(DataComponents.ITEM_NAME);
        if (previousName == null || previousName.getContents() instanceof TranslatableContents translated
                && (translated.getKey().equals("item.pufferfish_please.dyed_pufferfish")
                || translated.getKey().equals("item.pufferfish_please.dyed_pufferfish_bucket"))) {
            stack.set(DataComponents.ITEM_NAME, name(stack, color));
        }
        if (stack.is(Items.PUFFERFISH_BUCKET)) {
            CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, tag -> tag.putString(COLOR_TAG, color.getName()));
        }
        return stack;
    }

    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Pufferfish fish) || fish instanceof org.glavo.mcmod.pufferfish.entity.MaterialPufferfish || event.getEntity().isSpectator()
                || !event.getEntity().mayBuild()) {
            return;
        }
        DyeColor color = DyeColor.getColor(event.getItemStack());
        if (color == null || color == get(fish)) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            set(fish, color);
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
        if (event.getEntity() instanceof org.glavo.mcmod.pufferfish.entity.MaterialPufferfish fish) {
            for (var drop : event.getDrops()) {
                if (PufferfishItems.isPufferfish(drop.getItem())) drop.setItem(fish.fish().copyWithCount(drop.getItem().getCount()));
            }
            return;
        }
        if (event.getEntity() instanceof Pufferfish fish) {
            DyeColor color = get(fish);
            var stored = fish.getExistingDataOrNull(PufferfishPlease.FISH_ITEM);
            if (stored != null && !stored.isEmpty()) {
                for (var drop : event.getDrops()) {
                    if (drop.getItem().is(Items.PUFFERFISH)) drop.setItem(stored.copyWithCount(1));
                }
            }
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
