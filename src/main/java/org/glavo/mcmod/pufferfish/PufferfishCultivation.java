// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/// Post-awakening state. Total experience is a lifetime statistic, independent of unspent progress.
public record PufferfishCultivation(PufferfishLevel level, double talent, long experience, long totalExperience) {
    private static final Codec<PufferfishLevel> AWAKENED_LEVEL_CODEC = PufferfishLevel.CODEC.validate(level ->
            level == PufferfishLevel.LEVEL_0 ? DataResult.error(() -> "Level zero has no cultivation data") : DataResult.success(level));
    private static final Codec<Double> TALENT_CODEC = Codec.DOUBLE.validate(talent ->
            Double.isFinite(talent) && talent >= 0 && talent <= 1
                    ? DataResult.success(talent) : DataResult.error(() -> "Talent must be finite and between zero and one"));
    private static final Codec<Long> EXPERIENCE_CODEC = Codec.LONG.validate(experience -> experience >= 0
            ? DataResult.success(experience) : DataResult.error(() -> "Experience must not be negative"));
    public static final Codec<PufferfishCultivation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AWAKENED_LEVEL_CODEC.fieldOf("level").forGetter(PufferfishCultivation::level),
            TALENT_CODEC.fieldOf("talent").forGetter(PufferfishCultivation::talent),
            EXPERIENCE_CODEC.fieldOf("experience").forGetter(PufferfishCultivation::experience),
            EXPERIENCE_CODEC.fieldOf("total_experience").forGetter(PufferfishCultivation::totalExperience)
    ).apply(instance, PufferfishCultivation::new));
    public static final StreamCodec<ByteBuf, PufferfishCultivation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(AWAKENED_LEVEL_CODEC), PufferfishCultivation::level,
            ByteBufCodecs.DOUBLE, PufferfishCultivation::talent,
            ByteBufCodecs.VAR_LONG, PufferfishCultivation::experience,
            ByteBufCodecs.VAR_LONG, PufferfishCultivation::totalExperience,
            PufferfishCultivation::new);

    public PufferfishCultivation {
        if (level == null || level == PufferfishLevel.LEVEL_0) throw new IllegalArgumentException("Cultivation requires an awakened level");
        if (!Double.isFinite(talent) || talent < 0 || talent > 1) throw new IllegalArgumentException("Talent must be finite and between zero and one");
        if (experience < 0) throw new IllegalArgumentException("Experience must not be negative");
        if (totalExperience < 0) throw new IllegalArgumentException("Total experience must not be negative");
    }

    /// Both counters saturate independently; experience alone never changes level or talent.
    public PufferfishCultivation addExperience(int amount) {
        if (amount <= 0) return this;
        return new PufferfishCultivation(level, talent,
                experience + Math.min((long) amount, Long.MAX_VALUE - experience),
                totalExperience + Math.min((long) amount, Long.MAX_VALUE - totalExperience));
    }

    /// Awards the full orb value to each equipped awakened weapon, including XP consumed by Mending.
    public static void pickupExperience(PlayerXpEvent.PickupXp event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        int amount = event.getOrb().getValue();
        if (amount <= 0) return;
        var recipients = new ArrayList<ItemStack>(3);
        for (var slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD)) {
            var stack = player.getItemBySlot(slot);
            if (stack.getCount() == 1 && PufferfishItems.isWeapon(stack) && stack.has(PufferfishPlease.CULTIVATION.get())) {
                recipients.add(stack);
            }
        }
        if (recipients.isEmpty()) return;
        // Wait for all pickup listeners, including later cancellation, while retaining the original recipients.
        player.server.tell(new TickTask(player.server.getTickCount(), () -> {
            if (event.isCanceled()) return;
            for (var stack : recipients) {
                var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
                if (!stack.isEmpty() && cultivation != null) {
                    stack.set(PufferfishPlease.CULTIVATION.get(), cultivation.addExperience(amount));
                }
            }
        }));
    }

    /// Reading a level never creates cultivation data or changes stacking.
    @Nullable
    public static PufferfishLevel visibleLevel(ItemStack stack) {
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        if (cultivation != null) return cultivation.level();
        return PufferfishItems.isWeapon(stack) && stack.has(DataComponents.MAX_DAMAGE) ? PufferfishLevel.LEVEL_0 : null;
    }
}
