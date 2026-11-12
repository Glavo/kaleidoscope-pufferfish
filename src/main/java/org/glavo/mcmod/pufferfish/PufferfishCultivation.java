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

/// Post-awakening state. Experience is unspent cultivation progress; talent is never shown in tooltips.
public record PufferfishCultivation(PufferfishRealm realm, double talent, long experience) {
    private static final Codec<PufferfishRealm> AWAKENED_REALM_CODEC = PufferfishRealm.CODEC.validate(realm ->
            realm == PufferfishRealm.LEVEL_0 ? DataResult.error(() -> "Level zero has no cultivation data") : DataResult.success(realm));
    private static final Codec<Double> TALENT_CODEC = Codec.DOUBLE.validate(talent ->
            Double.isFinite(talent) && talent >= 0 && talent <= 1
                    ? DataResult.success(talent) : DataResult.error(() -> "Talent must be finite and between zero and one"));
    public static final Codec<PufferfishCultivation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AWAKENED_REALM_CODEC.fieldOf("realm").forGetter(PufferfishCultivation::realm),
            TALENT_CODEC.fieldOf("talent").forGetter(PufferfishCultivation::talent),
            Codec.LONG.validate(experience -> experience >= 0 ? DataResult.success(experience)
                    : DataResult.error(() -> "Experience must not be negative")).fieldOf("experience").forGetter(PufferfishCultivation::experience)
    ).apply(instance, PufferfishCultivation::new));
    public static final StreamCodec<ByteBuf, PufferfishCultivation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(AWAKENED_REALM_CODEC), PufferfishCultivation::realm,
            ByteBufCodecs.DOUBLE, PufferfishCultivation::talent,
            ByteBufCodecs.VAR_LONG, PufferfishCultivation::experience,
            PufferfishCultivation::new);

    public PufferfishCultivation {
        if (realm == null || realm == PufferfishRealm.LEVEL_0) throw new IllegalArgumentException("Cultivation requires an awakened realm");
        if (!Double.isFinite(talent) || talent < 0 || talent > 1) throw new IllegalArgumentException("Talent must be finite and between zero and one");
        if (experience < 0) throw new IllegalArgumentException("Experience must not be negative");
    }

    /// Reading a realm never creates cultivation data or changes stacking.
    @Nullable
    public static PufferfishRealm visibleRealm(ItemStack stack) {
        var cultivation = stack.get(PufferfishPlease.CULTIVATION.get());
        if (cultivation != null) return cultivation.realm();
        return PufferfishItems.isWeapon(stack) && stack.has(DataComponents.MAX_DAMAGE) ? PufferfishRealm.LEVEL_0 : null;
    }
}
