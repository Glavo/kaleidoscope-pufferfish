// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.jade;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.item.ItemStack;
import org.glavo.mcmod.pufferfish.PlacedPufferfishBlock;
import org.glavo.mcmod.pufferfish.PlacedPufferfishBlockEntity;
import org.glavo.mcmod.pufferfish.PufferfishCultivation;
import org.glavo.mcmod.pufferfish.PufferfishData;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class PufferfishJadePlugin implements IWailaPlugin {
    private static final ResourceLocation ID = PufferfishPlease.id("level");
    private static final MapCodec<Component> DISPLAY = ComponentSerialization.CODEC.fieldOf(ID.toString());
    private static final LevelProvider<EntityAccessor> ENTITY = new LevelProvider<>(
            accessor -> PufferfishData.stored((Pufferfish) accessor.getEntity()));
    private static final LevelProvider<BlockAccessor> BLOCK = new LevelProvider<>(
            accessor -> accessor.getBlockEntity() instanceof PlacedPufferfishBlockEntity fish ? fish.fish() : ItemStack.EMPTY);

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(ENTITY, Pufferfish.class);
        registration.registerBlockDataProvider(BLOCK, PlacedPufferfishBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(ENTITY, Pufferfish.class);
        registration.registerBlockComponent(BLOCK, PlacedPufferfishBlock.class);
    }

    private record LevelProvider<T extends Accessor<?>>(Function<T, ItemStack> source)
            implements IComponentProvider<T>, IServerDataProvider<T> {
        @Override public ResourceLocation getUid() { return ID; }

        /// Synchronizes only the localized display structure, without talent or raw experience.
        @Override
        public void appendServerData(CompoundTag data, T accessor) {
            var name = PufferfishCultivation.visibleName(source.apply(accessor));
            if (name != null) accessor.writeData(DISPLAY, name);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, T accessor, IPluginConfig config) {
            accessor.readData(DISPLAY).ifPresent(name -> tooltip.add(Component.translatable("tooltip.pufferfish_please.level", name)));
        }
    }
}
