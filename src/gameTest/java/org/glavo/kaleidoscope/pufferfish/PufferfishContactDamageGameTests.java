// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(KaleidoscopePufferfish.MODID)
@PrefixGameTestTemplate(false)
public final class PufferfishContactDamageGameTests {
    @GameTest(template = "empty")
    public static void canceledOrRejectedContactDamageDoesNotApplyEffects(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (boolean radiant : new boolean[] {false, true}) {
            for (boolean skin : new boolean[] {false, true}) {
                var block = skin ? radiant ? PufferfishBlocks.RADIANT_SKIN.get() : PufferfishBlocks.skin(null)
                        : PufferfishBlocks.PLACED_FISH.get();
                level.setBlockAndUpdate(pos, block.defaultBlockState());
                if (level.getBlockEntity(pos) instanceof PlacedPufferfishBlockEntity placed) {
                    placed.setFish(new net.minecraft.world.item.ItemStack(radiant ? PufferfishItems.RADIANT.get() : net.minecraft.world.item.Items.PUFFERFISH));
                }
                var target = EntityType.COW.create(level);
                target.setPos(Vec3.atBottomCenterOf(pos));
                java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> cancel = event -> {
                    if (event.getEntity() == target && event.getSource().is(PufferfishContactDamage.DAMAGE_TYPE)) event.setCanceled(true);
                };
                NeoForge.EVENT_BUS.addListener(cancel);
                try {
                    target.tickCount = 20;
                    NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target));
                    helper.assertTrue(target.getHealth() == target.getMaxHealth() && target.getActiveEffects().isEmpty(),
                            "Canceled contact damage applied poison, wither, or glowing");
                    var existing = new MobEffectInstance(radiant ? MobEffects.WITHER : MobEffects.POISON, 1, 0);
                    target.addEffect(existing);
                    target.tickCount = 40;
                    NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target));
                    helper.assertTrue(target.getEffect(existing.getEffect()).getDuration() == 1 && !target.hasEffect(MobEffects.GLOWING),
                            "Canceled contact damage refreshed an existing effect");
                } finally {
                    NeoForge.EVENT_BUS.unregister(cancel);
                }
                target.removeAllEffects();
                target.tickCount = 60;
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target));
                helper.assertTrue(target.getHealth() == target.getMaxHealth() - 1
                        && target.hasEffect(radiant ? MobEffects.WITHER : MobEffects.POISON), "Successful contact did not apply its effect");
                target.removeAllEffects();
                target.invulnerableTime = 20;
                target.tickCount = 80;
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target));
                helper.assertTrue(target.getActiveEffects().isEmpty(), "Rejected contact damage applied effects during hurt immunity");
                target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(2);
                target.setAbsorptionAmount(2);
                target.invulnerableTime = 0;
                target.tickCount = 100;
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(target));
                helper.assertTrue(target.getHealth() == target.getMaxHealth() - 1 && target.getAbsorptionAmount() == 1
                        && target.hasEffect(radiant ? MobEffects.WITHER : MobEffects.POISON), "Absorbed contact damage did not apply its effect");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyVariantHurtsOnContactWithAnyFace(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (var direction : Direction.values()) level.setBlockAndUpdate(pos.relative(direction), Blocks.STONE.defaultBlockState());
        for (var variant : PufferfishLamps.VARIANTS.values()) {
            var fish = variant.fish().get();
            int copper = CopperPufferfish.index(fish);
            int duration = copper > 0 && copper < 4 ? new int[] {60, 120, 240, 360}[copper] : 60;
            int amplifier = copper > 0 && copper < 4 ? copper - 1 : PufferfishItems.hasNetheriteCombat(fish) ? 1 : 0;
            var poison = copper >= 4 ? null : new MobEffectInstance(MobEffects.POISON, duration, amplifier);
            for (var block : new PlacedPufferfishBlock[] {PufferfishItems.isHeavy(fish)
                    ? PufferfishBlocks.PLACED_HEAVY_FISH.get() : PufferfishBlocks.PLACED_FISH.get(), variant.block().get()}) {
                var state = block.defaultBlockState();
                level.setBlockAndUpdate(pos, state);
                ((PlacedPufferfishBlockEntity) level.getBlockEntity(pos)).setFish(fish);
                assertContact(helper, pos, state, poison);
            }
            var skin = PufferfishBlocks.skinForFish(fish);
            for (var face : AttachFace.values()) {
                for (var facing : Direction.Plane.HORIZONTAL) {
                    var state = skin.defaultBlockState().setValue(PufferfishSkinBlock.FACE, face).setValue(PufferfishSkinBlock.FACING, facing);
                    level.setBlockAndUpdate(pos, state);
                    assertContact(helper, pos, state, copper >= 4
                            ? null : new MobEffectInstance(MobEffects.POISON, copper >= 0 ? duration : 120, copper >= 0 ? amplifier : 0));
                }
            }
        }
        helper.succeed();
    }

    private static void assertContact(GameTestHelper helper, BlockPos pos, BlockState state, MobEffectInstance expectedPoison) {
        var level = helper.getLevel();
        var shape = state.getCollisionShape(level, pos).bounds().move(pos);
        for (var side : Direction.values()) {
            var entity = EntityType.COW.create(level);
            var center = shape.getCenter();
            double x = side == Direction.EAST ? shape.maxX + entity.getBbWidth() / 2
                    : side == Direction.WEST ? shape.minX - entity.getBbWidth() / 2 : center.x;
            double y = side == Direction.UP ? shape.maxY : side == Direction.DOWN ? shape.minY - entity.getBbHeight()
                    : center.y - entity.getBbHeight() / 2;
            double z = side == Direction.SOUTH ? shape.maxZ + entity.getBbWidth() / 2
                    : side == Direction.NORTH ? shape.minZ - entity.getBbWidth() / 2 : center.z;
            entity.setPos(x, y, z);
            entity.tickCount = 20;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getHealth() == entity.getMaxHealth() - 1, "Contact did not deal one point: " + state + " / " + side);
            var poison = entity.getEffect(MobEffects.POISON);
            helper.assertTrue(expectedPoison == null ? poison == null : poison != null
                    && poison.getDuration() == expectedPoison.getDuration() && poison.getAmplifier() == expectedPoison.getAmplifier(),
                    "Incorrect contact poison: " + state + " / " + side);
            entity.removeEffect(MobEffects.POISON);
            entity.invulnerableTime = 0;
            entity.setPos(entity.position().add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.02)));
            entity.tickCount = 40;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getHealth() == entity.getMaxHealth() - 1, "Nearby entity was hurt without touching the shape");
            helper.assertTrue(!entity.hasEffect(MobEffects.POISON), "Nearby entity was poisoned without touching the shape");
        }
    }

    @GameTest(template = "empty", timeoutTicks = 90)
    public static void multipleContactsDealOnePointPerSecondAndStopAfterLeaving(GameTestHelper helper) {
        var block = PufferfishLamps.VARIANTS.get("waxed_copper_pufferfish_lamp").block().get();
        helper.setBlock(new BlockPos(2, 2, 2), block);
        helper.setBlock(new BlockPos(3, 2, 2), block);
        var entity = helper.spawn(EntityType.COW, 3, 2.625F, 2.5F);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        float health = entity.getHealth();
        helper.runAfterDelay(25, () -> helper.assertTrue(entity.getHealth() == health - 1, "Contact damage was not one point per second"));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(entity.getHealth() == health - 2, "Overlapping contacts stacked damage");
            entity.setPos(entity.position().add(0, 2, 0));
        });
        helper.runAfterDelay(65, () -> {
            helper.assertTrue(entity.getHealth() == health - 2, "Contact damage continued after leaving");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void overlappingContactsSelectTheStrongestPoison(GameTestHelper helper) {
        for (var weaker : new String[] {"waxed_copper", "copper", "weathered_copper"}) {
            for (boolean reversed : new boolean[] {false, true}) {
                var strong = PufferfishLamps.VARIANTS.get("oxidized_copper_pufferfish_lamp").block().get();
                var weak = PufferfishLamps.VARIANTS.get(weaker + "_pufferfish_lamp").block().get();
                helper.setBlock(new BlockPos(2, 2, 2), reversed ? strong : weak);
                helper.setBlock(new BlockPos(3, 2, 2), reversed ? weak : strong);
                var entity = EntityType.COW.create(helper.getLevel());
                entity.setPos(helper.absoluteVec(new Vec3(3, 2.625, 2.5)));
                entity.tickCount = 20;
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
                var poison = entity.getEffect(MobEffects.POISON);
                helper.assertTrue(poison != null && poison.getDuration() == 360 && poison.getAmplifier() == 2,
                        "Contact order hid or weakened the strongest poison");
                helper.assertTrue(entity.getHealth() == entity.getMaxHealth() - 1, "Poisonous contacts stacked prick damage");
                for (var existing : new MobEffectInstance[] {
                        new MobEffectInstance(MobEffects.POISON, 400, 2),
                        new MobEffectInstance(MobEffects.POISON, 20, 3),
                        new MobEffectInstance(MobEffects.POISON, -1, 0)}) {
                    entity.removeEffect(MobEffects.POISON);
                    entity.addEffect(existing);
                    NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
                    poison = entity.getEffect(MobEffects.POISON);
                    helper.assertTrue(poison.getDuration() == existing.getDuration() && poison.getAmplifier() == existing.getAmplifier(),
                            "Contact overwrote a longer, stronger, or infinite poison");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void lampPoisonDealsDamageAndExpiresAfterLeaving(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 2), PufferfishLamps.VARIANTS.get("copper_pufferfish_lamp").block().get());
        helper.setBlock(new BlockPos(3, 2, 2), PufferfishLamps.VARIANTS.get("waxed_copper_pufferfish_lamp").block().get());
        var poisoned = helper.spawn(EntityType.COW, 1.5F, 2.625F, 2.5F);
        var control = helper.spawn(EntityType.COW, 3.5F, 2.625F, 2.5F);
        for (var entity : new net.minecraft.world.entity.animal.Cow[] {poisoned, control}) {
            entity.setNoAi(true);
            entity.setNoGravity(true);
        }
        int[] poisonHits = {0};
        Consumer<LivingDamageEvent.Post> listener = event -> {
            if (event.getEntity() == poisoned && event.getSource().is(NeoForgeMod.POISON_DAMAGE) && event.getNewDamage() > 0) poisonHits[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        helper.runAfterDelay(100, () -> {
            NeoForge.EVENT_BUS.unregister(listener);
            helper.assertTrue(poisoned.hasEffect(MobEffects.POISON) && !control.hasEffect(MobEffects.POISON)
                    && poisonHits[0] > 0, "Contact poison did not damage or was continuously reset");
            poisoned.setPos(poisoned.position().add(0, 2, 0));
        });
        helper.runAfterDelay(165, () -> {
            helper.assertTrue(!poisoned.hasEffect(MobEffects.POISON), "Lamp poison did not expire after leaving");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void contactKillsWithItsOwnMessageAndRespectsInvulnerability(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, PufferfishLamps.VARIANTS.get("waxed_copper_pufferfish_lamp").block().get().defaultBlockState());
        var entity = EntityType.COW.create(helper.getLevel());
        entity.setPos(Vec3.atBottomCenterOf(pos).add(0, 0.625, 0));
        entity.tickCount = 20;
        entity.setInvulnerable(true);
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        helper.assertTrue(entity.getHealth() == entity.getMaxHealth(), "Contact bypassed invulnerability");
        entity.setInvulnerable(false);
        entity.setHealth(1);
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        var source = entity.getLastDamageSource();
        helper.assertTrue(entity.isDeadOrDying() && source != null && source.is(PufferfishContactDamage.DAMAGE_TYPE), "Contact did not kill with its own damage type");
        helper.assertTrue(source.getLocalizedDeathMessage(entity).getContents() instanceof TranslatableContents message
                && message.getKey().equals("death.attack.kaleidoscope_pufferfish.pufferfish_prick"), "Incorrect prick death message");
        var item = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.625, pos.getZ() + 0.5, new ItemStack(Items.APPLE));
        for (int tick = 20; tick <= 100; tick += 20) {
            item.tickCount = tick;
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(item));
        }
        helper.assertTrue(item.isRemoved(), "Contact did not damage dropped items like cactus");
        helper.succeed();
    }
}
