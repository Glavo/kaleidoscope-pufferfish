// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.function.Supplier;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import org.glavo.mcmod.pufferfish.entity.PufferfishBoat;

public final class PufferfishBoatItem extends Item {
    private final Supplier<EntityType<PufferfishBoat>> type;

    public PufferfishBoatItem(Supplier<EntityType<PufferfishBoat>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        var search = player.getBoundingBox().expandTowards(player.getViewVector(1).scale(5)).inflate(1);
        for (var entity : level.getEntities(player, search, EntitySelector.NO_SPECTATORS.and(Entity::isPickable))) {
            if (entity.getBoundingBox().inflate(entity.getPickRadius()).contains(player.getEyePosition())) {
                return InteractionResultHolder.pass(stack);
            }
        }
        var boat = type.get().create(level);
        if (boat == null) return InteractionResultHolder.fail(stack);
        boat.setPos(hit.getLocation());
        boat.setYRot(player.getYRot());
        if (level instanceof ServerLevel server) EntityType.createDefaultStackConfig(server, stack, player).accept(boat);
        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide) {
            if (!level.addFreshEntity(boat)) return InteractionResultHolder.fail(stack);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static final class Dispenser extends DefaultDispenseItemBehavior {
        private final Supplier<EntityType<PufferfishBoat>> type;

        public Dispenser(Supplier<EntityType<PufferfishBoat>> type) { this.type = type; }

        @Override
        public ItemStack execute(BlockSource source, ItemStack stack) {
            var direction = source.state().getValue(DispenserBlock.FACING);
            var level = source.level();
            var pos = source.pos().relative(direction);
            var boat = type.get().create(level);
            if (boat == null) return super.execute(source, stack);
            double lift;
            if (boat.canBoatInFluid(level.getFluidState(pos))) lift = 1;
            else if (level.getBlockState(pos).isAir() && boat.canBoatInFluid(level.getFluidState(pos.below()))) lift = 0;
            else return super.execute(source, stack);
            double offset = 0.5625 + boat.getBbWidth() / 2.0;
            var center = source.center();
            boat.setPos(center.x + direction.getStepX() * offset, center.y + direction.getStepY() * 1.125 + lift,
                    center.z + direction.getStepZ() * offset);
            boat.setYRot(direction.toYRot());
            EntityType.createDefaultStackConfig(level, stack, null).accept(boat);
            if (level.addFreshEntity(boat)) stack.shrink(1);
            return stack;
        }
    }
}
