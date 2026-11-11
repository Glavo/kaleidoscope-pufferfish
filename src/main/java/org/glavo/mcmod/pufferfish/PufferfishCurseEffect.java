// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.glavo.mcmod.pufferfish.entity.MaterialPufferfish;

public final class PufferfishCurseEffect extends MobEffect {
    public PufferfishCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x706184);
        addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, PufferfishPlease.id("effect.pufferfish_curse"),
                -0.7, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, PufferfishPlease.id("effect.pufferfish_curse"),
                -4, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void onAttack(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !player.isAlive()
                || !event.getSource().is(DamageTypes.MOB_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof Pufferfish fish)
                || event.getNewDamage() + event.getReduction(DamageContainer.Reduction.ABSORPTION) <= 0) return;
        var power = player.getEffect(PufferfishEffects.PUFFERFISH_POWER);
        if (!PufferfishConduitSpawning.isInAncientRange(player.level(), player.blockPosition())) return;
        boolean alreadyCursed = player.hasEffect(PufferfishEffects.PUFFERFISH_CURSE);
        if (!player.addEffect(new MobEffectInstance(PufferfishEffects.PUFFERFISH_CURSE, power == null ? 260 : power.getDuration(), 0,
                power == null || power.isAmbient(), true), event.getSource().getEntity())) return;
        player.removeEffect(PufferfishEffects.PUFFERFISH_POWER);
        if (!alreadyCursed && player instanceof ServerPlayer serverPlayer) {
            var appearance = fish instanceof MaterialPufferfish material ? material.fish() : new ItemStack(Items.PUFFERFISH);
            if (!(fish instanceof MaterialPufferfish) && PufferfishColors.get(fish) != null) {
                PufferfishColors.dye(appearance, PufferfishColors.get(fish));
            }
            serverPlayer.serverLevel().sendParticles(serverPlayer,
                    new ItemParticleOption(PufferfishEffects.CURSE_APPEARANCE.get(), appearance), true,
                    player.getX(), player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
            serverPlayer.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6F, 1.2F);
        }
    }
}
