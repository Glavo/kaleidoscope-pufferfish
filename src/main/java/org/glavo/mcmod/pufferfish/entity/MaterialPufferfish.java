// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import java.util.UUID;
import org.glavo.mcmod.pufferfish.PufferfishPlease;
import org.glavo.mcmod.pufferfish.PufferfishMobConfig;
import org.glavo.mcmod.pufferfish.PufferfishHeadgear;
import net.neoforged.neoforge.common.ItemAbilities;
import org.glavo.mcmod.pufferfish.CopperPufferfish;
import org.glavo.mcmod.pufferfish.PufferfishCombat;
import org.glavo.mcmod.pufferfish.PufferfishMobs;
import org.glavo.mcmod.pufferfish.PufferfishReinforcements;

/// A swimming material fish whose stored item survives oxidation, capture and release.
public final class MaterialPufferfish extends Pufferfish {
    private static final EntityDataAccessor<ItemStack> FISH = SynchedEntityData.defineId(MaterialPufferfish.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> ATTACK_PHASE = SynchedEntityData.defineId(MaterialPufferfish.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, WINDUP = 1, RUSH = 2, RECOVERY = 3, PULSE = 4, COMBO_GAP = 5, REST = 6;
    private static final net.minecraft.resources.ResourceLocation EXPOSED = PufferfishPlease.id("fish_exposed");
    private int attackCooldown;
    private UUID provokerId;
    private LivingEntity provoker;
    private int angerTicks;
    private int reinforcementCooldown;
    private int reinforcementCalls;
    private boolean reinforcement;
    private BlockPos guardPos;

    public MaterialPufferfish(EntityType<? extends MaterialPufferfish> type, Level level, Item item) {
        super(type, level);
        setFish(new ItemStack(item));
        setHealth(getMaxHealth());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FISH, ItemStack.EMPTY);
        builder.define(ATTACK_PHASE, IDLE);
    }

    public ItemStack fish() { return entityData.get(FISH); }

    public void setFish(ItemStack fish) {
        if (PufferfishMobs.variant(fish) != null) {
            entityData.set(FISH, fish.copyWithCount(1));
            if (!level().isClientSide) applyProfile();
        }
    }

    public PufferfishMobConfig.Kind kind() { return PufferfishMobConfig.kind(fish()); }
    public PufferfishMobConfig.Settings settings() { return PufferfishMobConfig.settings(fish()); }
    public boolean hasActiveAttacks() { return kind() != PufferfishMobConfig.Kind.GOLDEN || reinforcement; }
    public boolean isHostileToPlayers() { return guardPos != null || kind() != PufferfishMobConfig.Kind.GOLDEN && kind() != PufferfishMobConfig.Kind.COPPER; }
    public int attackPhase() { return entityData.get(ATTACK_PHASE); }
    public void setAttackPhase(int phase) {
        entityData.set(ATTACK_PHASE, phase);
        updateDefense();
    }
    public boolean canCapture() { return getHealth() <= getMaxHealth() * PufferfishMobConfig.value(settings().capture()); }
    public boolean readyToAttack() { return attackCooldown == 0; }
    public int attackInterval() { return PufferfishMobConfig.value(settings().cooldown()); }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !org.glavo.mcmod.pufferfish.PufferfishEffects.isPoison(effect) && !(effect.is(MobEffects.WITHER) && kind() == PufferfishMobConfig.Kind.RADIANT)
                && super.canBeAffected(effect);
    }

    private void applyProfile() {
        var config = settings();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(PufferfishMobConfig.value(config.health()));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(PufferfishMobConfig.value(config.damage()));
        getAttribute(Attributes.ARMOR).setBaseValue(PufferfishMobConfig.value(config.armor()));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(PufferfishMobConfig.value(config.speed()));
        updateDefense();
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(-1, new MaterialPufferfishCombatGoal(this));
        goalSelector.addGoal(0, new MoveTowardsRestrictionGoal(this, 1));
    }

    public boolean isAngryAt(LivingEntity entity) { return angerTicks > 0 && entity.getUUID().equals(provokerId); }

    public boolean isDisguisedFrom(LivingEntity entity) { return PufferfishHeadgear.isWearing(entity) && !isAngryAt(entity); }

    public boolean canFight(LivingEntity entity) {
        return entity != null && entity != this && entity.isAlive() && !entity.isSpectator() && !isAlliedTo(entity)
                && !(entity instanceof Pufferfish) && !(entity instanceof Cat)
                && !(entity instanceof Player player && (player.isCreative() || level().getDifficulty() == Difficulty.PEACEFUL));
    }

    private static boolean isHuntedHostile(LivingEntity entity) {
        return !(entity instanceof Creeper) && !(entity instanceof Drowned)
                && (entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER);
    }

    public LivingEntity findCombatTarget() {
        if (!hasActiveAttacks()) return null;
        if (angerTicks > 0 && level() instanceof ServerLevel server) {
            if (provoker == null && server.getEntity(provokerId) instanceof LivingEntity living) provoker = living;
            if (validTarget(provoker)) return provoker;
        }
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(getAttributeValue(Attributes.FOLLOW_RANGE)),
                        target -> (isHuntedHostile(target) || target instanceof Player && isHostileToPlayers()) && validTarget(target))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    public boolean validTarget(LivingEntity target) {
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        return hasActiveAttacks() && canFight(target) && !isDisguisedFrom(target)
                && (guardPos != null || kind() != PufferfishMobConfig.Kind.COPPER || isHuntedHostile(target) || reinforcement && isAngryAt(target))
                && isWithinRestriction(target.blockPosition())
                && target.level() == level() && target.isInWater() && distanceToSqr(target) <= range * range && hasLineOfSight(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            provokerId = attacker.getUUID();
            provoker = attacker;
            angerTicks = 400;
            if (isAlive() && !isNoAi() && isAddedToLevel() && level() instanceof ServerLevel
                    && (kind() == PufferfishMobConfig.Kind.HEAVY || kind() == PufferfishMobConfig.Kind.RADIANT)
                    && isInWater() && canFight(attacker) && level().getDifficulty() != Difficulty.PEACEFUL
                    && level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                    && reinforcementCooldown == 0 && reinforcementCalls < PufferfishMobConfig.value(PufferfishMobConfig.REINFORCEMENT_MAX_CALLS)
                    && random.nextDouble() < PufferfishMobConfig.value(PufferfishMobConfig.REINFORCEMENT_CHANCE)
                    && PufferfishReinforcements.summon(this, attacker)) {
                reinforcementCooldown = PufferfishMobConfig.value(PufferfishMobConfig.REINFORCEMENT_COOLDOWN);
                reinforcementCalls++;
            }
        }
        return damaged;
    }

    @Override
    protected Component getTypeName() { return fish().getHoverName(); }

    @Override
    public ItemStack getBucketItemStack() { return new ItemStack(PufferfishMobs.variant(fish()).bucket().get()); }

    @Override
    public ItemStack getPickResult() { return getBucketItemStack(); }

    /// Ruin residents remain near their home without natural despawning; bucket capture releases them from it.
    public void setHome(BlockPos pos) {
        restrictTo(pos.immutable(), 12);
        setPersistenceRequired();
    }

    /// Guards also defend their home against players.
    public void guard(BlockPos pos) {
        guardPos = pos.immutable();
        setHome(guardPos);
    }

    public void joinReinforcement(LivingEntity attacker) {
        reinforcement = true;
        provokerId = attacker.getUUID();
        provoker = attacker;
        angerTicks = 400;
        setTarget(attacker);
    }

    private void saveReinforcements(CompoundTag tag) {
        tag.putInt("FishReinforcementCooldown", reinforcementCooldown);
        tag.putInt("FishReinforcementCalls", reinforcementCalls);
        tag.putBoolean("FishReinforcement", reinforcement);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Fish", fish().save(registryAccess()));
        tag.putInt("FishAttackCooldown", attackCooldown);
        saveReinforcements(tag);
        if (guardPos != null) tag.putLong("FishGuardPos", guardPos.asLong());
        else if (hasRestriction()) tag.putLong("FishHomePos", getRestrictCenter().asLong());
        if (angerTicks > 0 && provokerId != null) {
            tag.putUUID("FishProvoker", provokerId);
            tag.putInt("FishAngerTicks", angerTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadFish(tag);
        guardPos = null;
        clearRestriction();
        if (tag.contains("FishGuardPos")) guard(BlockPos.of(tag.getLong("FishGuardPos")));
        else if (tag.contains("FishHomePos")) setHome(BlockPos.of(tag.getLong("FishHomePos")));
        attackCooldown = Math.max(0, Math.min(1200, tag.getInt("FishAttackCooldown")));
        if (tag.hasUUID("FishProvoker")) {
            provokerId = tag.getUUID("FishProvoker");
            angerTicks = Math.max(0, Math.min(400, tag.getInt("FishAngerTicks")));
        }
    }

    public void loadFish(CompoundTag tag) {
        if (tag.contains("Fish")) ItemStack.parse(registryAccess(), tag.getCompound("Fish")).ifPresent(this::setFish);
        reinforcementCooldown = Math.max(0, Math.min(72000, tag.getInt("FishReinforcementCooldown")));
        reinforcementCalls = Math.max(0, Math.min(100, tag.getInt("FishReinforcementCalls")));
        reinforcement = tag.getBoolean("FishReinforcement");
    }

    @Override
    public void saveToBucketTag(ItemStack stack) {
        super.saveToBucketTag(stack);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, stack, tag -> {
            tag.put("Fish", fish().save(registryAccess()));
            saveReinforcements(tag);
        });
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        super.loadFromBucketTag(tag);
        loadFish(tag);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (attackCooldown > 0) attackCooldown--;
            if (reinforcementCooldown > 0) reinforcementCooldown--;
            if (angerTicks > 0 && --angerTicks == 0) { provoker = null; provokerId = null; }
        }
        super.tick();
        if (!level().isClientSide && isAlive() && tickCount % 20 == 0 && random.nextInt(1100) == 0) oxidize();
        if (!level().isClientSide && isAlive()) {
            updateDefense();
            if (!isNoAi() && kind() == PufferfishMobConfig.Kind.HEAVY && isInWater() && getPuffState() > 0
                    && attackPhase() != RUSH && attackPhase() != WINDUP) setDeltaMovement(getDeltaMovement().add(0, -0.035, 0));
        }
    }

    @Override
    public void setPuffState(int state) {
        super.setPuffState(state);
        updateDefense();
    }

    private void updateDefense() {
        if (level().isClientSide || fish().isEmpty()) return;
        var config = settings();
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(PufferfishMobConfig.value(
                getPuffState() > 0 ? config.inflatedResistance() : config.resistance()));
        var armor = getAttribute(Attributes.ARMOR);
        boolean exposed = kind() == PufferfishMobConfig.Kind.HEAVY && attackPhase() == RECOVERY;
        if (exposed && !armor.hasModifier(EXPOSED)) armor.addTransientModifier(new AttributeModifier(EXPOSED, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (!exposed) armor.removeModifier(EXPOSED);
    }

    public void oxidize() {
        int index = CopperPufferfish.index(fish());
        if (index >= 0 && index < 3) setFish(CopperPufferfish.convert(fish(), index + 1));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSpectator() || !player.mayBuild()) return super.mobInteract(player, hand);
        int index = CopperPufferfish.index(fish());
        var tool = player.getItemInHand(hand);
        if (tool.is(Items.WATER_BUCKET) && !canCapture()) {
            if (!level().isClientSide) player.displayClientMessage(Component.translatable("message.pufferfish_please.capture_too_healthy"), true);
            return InteractionResult.FAIL;
        }
        boolean wax = index >= 0 && index < 4 && tool.is(Items.HONEYCOMB);
        boolean scrape = index >= 4 && tool.canPerformAction(ItemAbilities.AXE_WAX_OFF)
                || index > 0 && index < 4 && tool.canPerformAction(ItemAbilities.AXE_SCRAPE);
        if (!wax && !scrape) return super.mobInteract(player, hand);
        if (!level().isClientSide) {
            setFish(CopperPufferfish.convert(fish(), wax ? index + 4 : index >= 4 ? index - 4 : index - 1));
            if (wax && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                org.glavo.mcmod.pufferfish.PufferfishAdvancements.WAX_PUFFERFISH.get().trigger(serverPlayer);
            }
            setPersistenceRequired();
            level().levelEvent(player, wax ? 3003 : index >= 4 ? 3004 : 3005, blockPosition(), 0);
            level().playSound(null, this, wax ? SoundEvents.HONEYCOMB_WAX_ON : index >= 4 ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE,
                    SoundSource.NEUTRAL, 1, 1);
            if (wax) tool.consume(1, player);
            else tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void sting(LivingEntity target) {
        if (getPuffState() > 0 && attackPhase() == IDLE) strike(target, 0, attackInterval());
    }

    /// Living fish poison and wither last three seconds per inflation stage.
    private void applyAttackEffect(LivingEntity target) {
        if (getPuffState() > 0) PufferfishCombat.applyAttackEffect(fish(), target, this, 60 * getPuffState());
    }

    private DamageSource attackDamageSource() {
        var source = damageSources().mobAttack(this);
        String message = switch (kind()) {
            case COPPER, IRON -> "tetanus";
            case DIAMOND -> "diamond_pufferfish";
            case NETHERITE -> "netherite_pufferfish";
            case RADIANT -> "radiant_pufferfish";
            case HEAVY -> "heavy_pufferfish";
            case GOLDEN -> null;
        };
        if (message == null) return source;
        return new DamageSource(source.typeHolder(), this) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity victim) {
                return Component.translatable("death.attack.pufferfish_please." + message, victim.getDisplayName());
            }
        };
    }

    /// Contact and deliberate attacks share one damage cooldown and attack status effects.
    public boolean strike(LivingEntity target, double knockback, int cooldown) {
        if (level().isClientSide || !isAlive() || !readyToAttack() || !canFight(target) || !hasLineOfSight(target)) return false;
        attackCooldown = cooldown;
        if (target.hurt(attackDamageSource(), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            applyAttackEffect(target);
            if (knockback > 0) target.knockback(knockback, getX() - target.getX(), getZ() - target.getZ());
            playSound(SoundEvents.PUFFER_FISH_STING, 1, 1);
            return true;
        }
        return false;
    }

    /// A pulse heals all fish within three blocks and applies attack effects to visible enemies.
    public void pulse() {
        if (!(level() instanceof ServerLevel server) || !readyToAttack() || !isAlive()) return;
        attackCooldown = attackInterval();
        server.sendParticles(ParticleTypes.FLASH, getX(), getY(0.5), getZ(), 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 36, 1.5, 1.5, 1.5, 0.05);
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.7F);
        heal(20);
        for (var fish : level().getEntitiesOfClass(Pufferfish.class, getBoundingBox().inflate(3),
                fish -> fish != this && fish.isAlive() && distanceToSqr(fish) <= 9)) {
            fish.heal(20);
        }
        for (var target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3), this::canFight)) {
            if (distanceToSqr(target) <= 9 && hasLineOfSight(target)
                    && target.hurt(attackDamageSource(), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                applyAttackEffect(target);
                target.knockback(0.6, getX() - target.getX(), getZ() - target.getZ());
            }
        }
    }

    @Override
    public void playerTouch(Player player) { sting(player); }
}
