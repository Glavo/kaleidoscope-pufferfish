// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/// Stores the complete inserted fish and preserves the pump item's metadata.
public final class PufferfishPumpBlockEntity extends BlockEntity {
    public static final int REBOUND_TICKS = 8;
    public static final int BURST_PRESSURE = 5;
    private static final int RELEASE_DELAY = 40;
    private static final int RELEASE_INTERVAL = 20;
    private ItemStack fish = ItemStack.EMPTY;
    private ItemStack pump = PufferfishItems.PUMP.toStack();
    private long lastPress = Long.MIN_VALUE;
    private long animationStart = Long.MIN_VALUE;
    private int pressure;
    private int releaseTicks;

    public PufferfishPumpBlockEntity(BlockPos pos, BlockState state) { super(PufferfishBlocks.PUMP_ENTITY.get(), pos, state); }
    public ItemStack fish() { return fish.copy(); }
    public ItemStack pump() { return pump.copy(); }
    public int pressure() { return pressure; }

    public void setFish(ItemStack stack) {
        fish = PufferfishItems.isPufferfish(stack) ? stack.copyWithCount(1) : ItemStack.EMPTY;
        pressure = releaseTicks = 0;
        changed();
    }

    public void setPump(ItemStack stack) {
        if (stack.is(PufferfishItems.PUMP.get())) pump = stack.copyWithCount(1);
        changed();
    }

    public ItemStack removeFish() {
        var result = fish;
        if (pressure > 0) vent();
        fish = ItemStack.EMPTY;
        pressure = releaseTicks = 0;
        changed();
        return result;
    }

    /// Repairs a worn fish or increases excess pressure, crediting only successful repairs.
    public boolean press(Player player) {
        if (!(level instanceof ServerLevel server) || player.isSpectator() || pump.isEmpty()) return false;
        long time = level.getGameTime();
        if (lastPress != Long.MIN_VALUE && time - lastPress < REBOUND_TICKS) return false;
        lastPress = time;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        if (PufferfishPumpItem.canInflate(fish)) {
            fish.setDamageValue(Math.max(0, fish.getDamageValue() - (fish.getMaxDamage() + 9) / 10));
            pressure = releaseTicks = 0;
            if (player instanceof ServerPlayer serverPlayer) PufferfishAdvancements.INFLATE_PUFFERFISH.get().trigger(serverPlayer);
        } else if (PufferfishItems.isPufferfish(fish)) {
            if (++pressure >= BURST_PRESSURE) {
                burst(server, player);
                return true;
            }
            releaseTicks = RELEASE_DELAY;
            if (pressure >= 3) vent();
        } else return false;
        level.playSound(null, worldPosition, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.BLOCKS, 0.6F, 1.1F + pressure * 0.2F);
        changed();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PufferfishPumpBlockEntity entity) {
        if (entity.pressure == 0) return;
        if (--entity.releaseTicks <= 0) {
            entity.pressure--;
            entity.releaseTicks = entity.pressure > 0 ? RELEASE_INTERVAL : 0;
            entity.vent();
            entity.changed();
        } else entity.setChanged();
    }

    private void vent() {
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.BLOCKS, 0.5F, 1.7F);
            var center = worldPosition.getCenter();
            server.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 4, 0.15, 0.1, 0.15, 0.025);
        }
    }

    private void burst(ServerLevel server, Player player) {
        var released = fish;
        fish = ItemStack.EMPTY;
        pressure = releaseTicks = 0;
        changed();
        var center = worldPosition.getCenter();
        var source = burstDamageSource(server, player, center);
        server.gameEvent(player, GameEvent.EXPLODE, center);
        server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1.3F);
        server.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 24, 0.3, 0.3, 0.3, 0.15);
        for (var target : server.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(3),
                entity -> entity.isAlive() && !entity.isSpectator() && !(entity instanceof Pufferfish)
                        && !(entity instanceof net.minecraft.world.entity.animal.Cat))) {
            double strength = (1 - Math.sqrt(target.distanceToSqr(center)) / 3) * Explosion.getSeenPercent(center, target);
            if (strength <= 0 || !target.hurt(source, (float) (6 * strength))) continue;
            PufferfishCombat.applyPassiveEffect(released, target, player);
            var direction = target.position().subtract(center);
            target.knockback(strength, -direction.x, -direction.z);
            target.hurtMarked = true;
        }
        PufferfishDurability.activate(released);
        int maximum = released.getMaxDamage();
        released.setDamageValue(maximum - Math.max(1, maximum / 10));
        double angle = server.random.nextDouble() * Math.PI * 2;
        var drop = new ItemEntity(server, center.x, center.y + 0.2, center.z, released,
                Math.cos(angle) * 0.24, 0.35, Math.sin(angle) * 0.24);
        drop.setDefaultPickUpDelay();
        server.addFreshEntity(drop);
    }

    public float compression(float partialTick) {
        if (level == null || animationStart == Long.MIN_VALUE) return 0;
        return Math.max(0, 1 - (level.getGameTime() - animationStart + partialTick) / REBOUND_TICKS);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1 && level != null) { animationStart = level.getGameTime(); return true; }
        return super.triggerEvent(id, param);
    }

    static DamageSource burstDamageSource(ServerLevel server, @Nullable Player player, Vec3 center) {
        return new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.EXPLOSION), null, player, center) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity victim) {
                String key = "death.attack.pufferfish_please.pufferfish_burst";
                return player == null || player == victim
                        ? Component.translatable(key, victim.getDisplayName())
                        : Component.translatable(key + ".player", victim.getDisplayName(), player.getDisplayName());
            }
        };
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fish.isEmpty()) tag.put("Fish", fish.save(registries));
        if (!pump.isEmpty()) tag.put("Pump", pump.save(registries));
        tag.putInt("Pressure", pressure);
        tag.putInt("ReleaseTicks", releaseTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        var storedFish = ItemStack.parseOptional(registries, tag.getCompound("Fish"));
        // Previously inserted flails remain recoverable, but cannot be inflated.
        fish = PufferfishItems.isWeapon(storedFish) ? storedFish.copyWithCount(1) : ItemStack.EMPTY;
        var storedPump = ItemStack.parseOptional(registries, tag.getCompound("Pump"));
        pump = storedPump.is(PufferfishItems.PUMP.get()) ? storedPump.copyWithCount(1) : PufferfishItems.PUMP.toStack();
        pressure = PufferfishItems.isPufferfish(fish) && !fish.isDamaged()
                ? Math.clamp(tag.getInt("Pressure"), 0, BURST_PRESSURE - 1) : 0;
        releaseTicks = pressure == 0 ? 0 : Math.clamp(tag.getInt("ReleaseTicks"), 1, RELEASE_DELAY);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
