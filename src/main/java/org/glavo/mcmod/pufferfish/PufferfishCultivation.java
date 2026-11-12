// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

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

    /// Reading a level never creates cultivation data or changes stacking.
    @Nullable
    public static PufferfishLevel visibleLevel(ItemStack stack) {
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        if (cultivation != null) return cultivation.level();
        return PufferfishItems.isWeapon(stack) && stack.has(DataComponents.MAX_DAMAGE) ? PufferfishLevel.LEVEL_0 : null;
    }
}
