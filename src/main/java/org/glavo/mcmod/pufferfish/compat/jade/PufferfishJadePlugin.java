// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.compat.jade;

import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pufferfish;
import org.glavo.mcmod.pufferfish.PufferfishCultivation;
import org.glavo.mcmod.pufferfish.PufferfishData;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class PufferfishJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(LevelProvider.INSTANCE, Pufferfish.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(LevelProvider.INSTANCE, Pufferfish.class);
    }

    private enum LevelProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
        INSTANCE;

        private static final ResourceLocation ID = PufferfishPlease.id("level");
        private static final MapCodec<Component> DISPLAY = ComponentSerialization.CODEC.fieldOf(ID.toString());

        @Override public ResourceLocation getUid() { return ID; }

        /// Synchronizes only the localized display structure, without talent or raw experience.
        @Override
        public void appendServerData(CompoundTag data, EntityAccessor accessor) {
            var name = PufferfishCultivation.visibleName(PufferfishData.stored((Pufferfish) accessor.getEntity()));
            if (name != null) accessor.writeData(DISPLAY, name);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            accessor.readData(DISPLAY).ifPresent(name -> tooltip.add(Component.translatable("tooltip.pufferfish_please.level", name)));
        }
    }
}
