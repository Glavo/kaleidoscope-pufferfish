// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.glavo.mcmod.pufferfish.test.ClientTestLevel;

public final class PufferfishBoatClientTest {
    public static void verify(Minecraft minecraft) {
        verifySpeed(minecraft);
        for (var entry : PufferfishBoats.VARIANTS.entrySet()) {
            var stack = new ItemStack(entry.getValue().item().get());
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()
                    || !model.isCustomRenderer()
                    || !model.getParticleIcon().contents().name().equals(PufferfishPlease.id("block/" + entry.getKey().replace("_boat", "")))
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing pufferfish boat model or translation");
            }
            var boat = entry.getValue().entity().get().create(ClientTestLevel.level(minecraft));
            var renderer = (org.glavo.mcmod.pufferfish.client.PufferfishBoatRenderer) minecraft.getEntityRenderDispatcher().getRenderer(boat);
            var texture = renderer.getModelWithLocation(boat).getFirst();
            var kayak = renderer.getModelWithLocation(boat).getSecond();
            var player = new net.minecraft.client.player.RemotePlayer((net.minecraft.client.multiplayer.ClientLevel) boat.level(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "kayak-render-test"));
            player.startRiding(boat, true);
            for (int input = 1; input <= 3; input++) {
                boat.setPaddleState((input & 1) != 0, (input & 2) != 0);
                kayak.setupAnim(boat, 0.25F, 0, 0, 0, 0);
                float angle = kayak.parts().iterator().next().xRot;
                kayak.setupAnim(boat, 0.75F, 0, 0, 0, 0);
                if (angle == kayak.parts().iterator().next().xRot) throw new IllegalStateException("Kayak paddle did not animate for steering input");
            }
            boat.setPaddleState(false, false);
            kayak.setupAnim(boat, 0, 0, 0, 0, 0);
            if (kayak.parts().iterator().next().xRot != 0) throw new IllegalStateException("Idle paddle did not return to the deck");
            player.stopRiding();
            if (!(minecraft.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic)
                    || dynamic.getPixels() == null || dynamic.getPixels().getWidth() != 160) {
                throw new IllegalStateException("Missing generated pufferfish boat texture");
            }
        }
    }

    private static void verifySpeed(Minecraft minecraft) {
        var level = ClientTestLevel.level(minecraft);
        try {
            var control = Boat.class.getDeclaredMethod("controlBoat");
            var drift = Boat.class.getDeclaredMethod("floatBoat");
            var status = Boat.class.getDeclaredField("status");
            var oldStatus = Boat.class.getDeclaredField("oldStatus");
            control.setAccessible(true);
            drift.setAccessible(true);
            status.setAccessible(true);
            oldStatus.setAccessible(true);
            for (var variant : PufferfishBoats.VARIANTS.values()) {
                for (int input = 0; input < 5; input++) {
                    var vanilla = new Boat(level, 0, 0, 0);
                    var kayak = variant.entity().get().create(level);
                    double multiplier = variant == PufferfishBoats.VARIANTS.get("radiant_pufferfish_skin_boat") ? 1.5 : 1.25;
                    for (var boat : new Boat[] {vanilla, kayak}) {
                        status.set(boat, Boat.Status.IN_WATER);
                        oldStatus.set(boat, Boat.Status.IN_WATER);
                        if (boat == kayak && kayak.isHeavy()) status.set(boat, switch (input % 3) {
                            case 1 -> Boat.Status.UNDER_WATER;
                            case 2 -> Boat.Status.UNDER_FLOWING_WATER;
                            default -> Boat.Status.IN_WATER;
                        });
                        var player = new net.minecraft.client.player.RemotePlayer(level,
                                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "kayak-speed-test"));
                        player.startRiding(boat, true);
                        boat.setInput(input == 2 || input == 4, input == 3, input == 0 || input == 4, input == 1);
                    }
                    for (int tick = 0; tick < 140; tick++) {
                        if (tick == 100) {
                            vanilla.setInput(false, false, false, false);
                            kayak.setInput(false, false, false, false);
                        }
                        drift.invoke(vanilla);
                        drift.invoke(kayak);
                        control.invoke(vanilla);
                        control.invoke(kayak);
                        var expected = vanilla.getDeltaMovement().multiply(multiplier, 1, multiplier);
                        var difference = kayak.getDeltaMovement().subtract(expected);
                        if (difference.horizontalDistance() > 1.0E-7 || (!kayak.isHeavy() && Math.abs(difference.y) > 1.0E-7)
                                || kayak.getYRot() != vanilla.getYRot() || kayak.isHeavy() && kayak.getDeltaMovement().y >= 0) {
                            throw new IllegalStateException("Kayak speed or steering differed from the expected boat ratio");
                        }
                        if (input == 0 && tick == 99 && Math.abs(kayak.getDeltaMovement().horizontalDistance() - 0.4 * multiplier) > 0.0001) {
                            throw new IllegalStateException("Kayak did not reach the expected cruising speed: " + kayak.skinBoatName());
                        }
                    }
                    vanilla.ejectPassengers();
                    kayak.ejectPassengers();
                    vanilla.setDeltaMovement(new Vec3(0.1, 0.2, -0.3));
                    kayak.setDeltaMovement(vanilla.getDeltaMovement());
                    drift.invoke(vanilla);
                    drift.invoke(kayak);
                    control.invoke(vanilla);
                    control.invoke(kayak);
                    var difference = kayak.getDeltaMovement().subtract(vanilla.getDeltaMovement());
                    if (difference.horizontalDistance() > 1.0E-7 || (!kayak.isHeavy() && Math.abs(difference.y) > 1.0E-7)) {
                        throw new IllegalStateException("Unoccupied kayak amplified external movement");
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not verify kayak movement", exception);
        }
    }

}
