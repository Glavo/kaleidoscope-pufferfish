// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.mcmod.pufferfish.client.PufferfishPumpBlockRenderer;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;
import org.glavo.mcmod.pufferfish.test.RenderCapture;

public final class PufferfishPumpClientTest {
    public static void verify(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        var state = PufferfishBlocks.PUMP.get().defaultBlockState().setValue(PufferfishPumpBlock.FACING, Direction.SOUTH);
        var entity = new PufferfishPumpBlockEntity(BlockPos.ZERO, state);
        entity.setLevel(level);
        var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof PufferfishPumpBlockRenderer)
                || minecraft.getBlockRenderer().getBlockModel(state) == minecraft.getModelManager().getMissingModel()) {
            throw new IllegalStateException("Foot pump renderer or block model is missing");
        }
        if (entity.compression(0) != 0 || !entity.triggerEvent(1, 0) || entity.compression(0) != 1
                || entity.compression(4) != 0.5F || entity.compression(8) != 0) {
            throw new IllegalStateException("Pedal did not compress and rebound after a synchronized block event");
        }
        for (int stage = 0; stage < 4; stage++) {
            var fish = new ItemStack(Items.PUFFERFISH);
            PufferfishDurability.activate(fish);
            fish.setDamageValue(fish.getMaxDamage() * stage / 4);
            var mouth = org.glavo.mcmod.pufferfish.client.PufferfishPumpModel.mouth(fish);
            double[] heights = {0.175, 0.14375, 0.125, 0.1};
            double[] fronts = {0.1, 0.08125, 0.0625, 0.0375};
            if (Math.abs(mouth.x - 5.0 / 16) > 0.000001 || Math.abs(mouth.y - heights[stage]) > 0.000001
                    || Math.abs(mouth.z - fronts[stage]) > 0.000001) {
                throw new IllegalStateException("Pump needle missed the vanilla mouth position at stage " + stage);
            }
            var displayed = new PufferfishPumpBlockEntity(BlockPos.ZERO, state);
            displayed.setFish(fish);
            displayed.setLevel(level);
            displayed.triggerEvent(1, 0);
            for (int elapsed : new int[] {0, 4, 8}) verifyGeometry(minecraft, displayed, elapsed);
        }
        var geometry = new org.glavo.mcmod.pufferfish.client.DeflatingPufferfishModel<net.minecraft.world.entity.Entity>(minecraft.getEntityModels());
        for (var item : java.util.List.of(Items.PUFFERFISH, PufferfishItems.NETHERITE.get())) {
            for (int pressure = 0; pressure <= 4; pressure++) {
                var fish = new ItemStack(item);
                geometry.prepare(fish, pressure);
                var root = geometry.root();
                float scale = 1 + pressure * 0.15F;
                if (Math.abs(root.getChild("body").xScale - scale) > 0.00001
                        || root.getChild("left_blue_fin").xScale != 1 || root.getChild("top_front_fin").yScale != 1
                        || Math.abs(root.getChild("left_blue_fin").x - 4 * scale) > 0.00001) {
                    throw new IllegalStateException("Overpressure scaled the fins or detached them from the body");
                }
                var mouth = org.glavo.mcmod.pufferfish.client.PufferfishPumpModel.mouth(fish, pressure);
                if (Math.abs(mouth.y - (1.0 / 16 + 0.1125 * scale)) > 0.00001 || Math.abs(mouth.z - 0.1 * scale) > 0.00001) {
                    throw new IllegalStateException("Needle did not follow the swollen mouth");
                }
                var displayed = new PufferfishPumpBlockEntity(BlockPos.ZERO, state);
                displayed.setFish(fish);
                var saved = displayed.saveWithoutMetadata(level.registryAccess());
                saved.putInt("Pressure", pressure);
                saved.putInt("ReleaseTicks", 40);
                displayed.loadWithComponents(saved, level.registryAccess());
                displayed.setLevel(level);
                if (displayed.pressure() != pressure) throw new IllegalStateException("Client lost synchronized pump pressure");
                verifyGeometry(minecraft, displayed, 0);
            }
        }
        geometry.prepare(new ItemStack(Items.PUFFERFISH));
        if (geometry.root().getChild("body").xScale != 1 || geometry.root().getChild("left_blue_fin").x != 4) {
            throw new IllegalStateException("Overpressure leaked into ordinary fish rendering");
        }
    }

    private static void verifyGeometry(Minecraft minecraft, PufferfishPumpBlockEntity entity, int elapsed) {
        var pose = new PoseStack();
        var capture = new RenderCapture();
        minecraft.getBlockEntityRenderDispatcher().getRenderer(entity).render(entity, elapsed, pose, capture, 15728880, OverlayTexture.NO_OVERLAY);
        if (capture.count() == 0 || !pose.clear()) throw new IllegalStateException("Pump lost its geometry or leaked its transform");
    }
}
