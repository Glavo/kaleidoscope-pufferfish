// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailHeadRenderer;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailModel;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishFlailHead;
import org.joml.Matrix4f;

/// Checks flail item geometry and resource reloads.
public final class PufferfishFlailClientTest {
    /// Full-bright lighting used by geometry checks and previews.
    private static final int LIGHT = 15728880;

    /// Prevents construction of the test holder.
    private PufferfishFlailClientTest() {}

    /// Verifies all variants in every display context and saves inventory previews.
    public static void verify(Minecraft minecraft) {
        for (var item : PufferfishItems.FLAIL_VARIANTS.stream().map(holder -> holder.get()).toList()) {
            var stack = new ItemStack(item);
            stack.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.RED);
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (!model.isCustomRenderer() || model == minecraft.getModelManager().getMissingModel()
                    || stack.getHoverName().getString().startsWith("item.")) {
                throw new IllegalStateException("Missing flail model or translation");
            }
            for (var context : ItemDisplayContext.values()) {
                var pose = new PoseStack();
                var capture = new Capture();
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, context, pose, capture, LIGHT, OverlayTexture.NO_OVERLAY);
                if (capture.vertices < 300 || capture.types.size() != 3 || !pose.clear()) {
                    throw new IllegalStateException("Flail head, grip, or chain failed to render: " + context);
                }
                if (item == PufferfishItems.HEAVY_FLAIL.get() && !capture.types.contains(RenderType.entityCutoutNoCull(
                        org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures.heavyEntity()))) {
                    throw new IllegalStateException("Heavy flail used the wrong fish texture");
                }
            }
        }
        verifyFlight(minecraft);
        preview(minecraft);
        previewSlots(minecraft);
    }

    /// Renders at native inventory resolutions rather than shrinking a large preview.
    private static void previewSlots(Minecraft minecraft) {
        var target = new TextureTarget(480, 180, true, Minecraft.ON_OSX);
        target.setClearColor(0.55F, 0.55F, 0.55F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 480, 180, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupForFlatItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var red = new ItemStack(PufferfishItems.FLAIL.get());
            red.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.RED);
            var stacks = new ItemStack[] {new ItemStack(PufferfishItems.FLAIL.get()), red, new ItemStack(PufferfishItems.NETHERITE_FLAIL.get()), new ItemStack(PufferfishItems.HEAVY_FLAIL.get())};
            for (int row = 0; row < 3; row++) {
                int size = 16 << row;
                for (int column = 0; column < stacks.length; column++) {
                    var pose = new PoseStack();
                    pose.translate(60 + column * 120, new int[] {20, 60, 130}[row], 0);
                    pose.scale(size, -size, size);
                    var buffers = minecraft.renderBuffers().bufferSource();
                    minecraft.getItemRenderer().renderStatic(stacks[column], ItemDisplayContext.GUI, LIGHT, OverlayTexture.NO_OVERLAY,
                            pose, buffers, null, 0);
                    buffers.endBatch();
                }
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-flail-slots-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save flail slot preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static void verifyFlight(Minecraft minecraft) {
        var previousLevel = minecraft.level;
        var level = PufferfishHeadgearClientTest.level(minecraft);
        var player = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), "flail-render-test")) {
            @Override
            public boolean isSpectator() { return false; }
        };
        player.setPos(0, 64, 0);
        player.setOldPosAndRot();
        level.addEntity(player);
        minecraft.level = level;
        var context = new EntityRendererProvider.Context(minecraft.getEntityRenderDispatcher(), minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(), minecraft.getEntityRenderDispatcher().getItemInHandRenderer(),
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        var renderer = new PufferfishFlailHeadRenderer(context);
        try {
            for (var arm : HumanoidArm.values()) {
                player.setMainArm(arm);
                for (var hand : InteractionHand.values()) {
                    var stack = new ItemStack(PufferfishItems.FLAIL.get());
                    player.setItemInHand(hand, stack);
                    var head = new PufferfishFlailHead(level, player, hand);
                    int side = (hand == InteractionHand.MAIN_HAND ? arm : arm.getOpposite()) == HumanoidArm.RIGHT ? 1 : -1;
                    var attachment = player.getEyePosition().add(-side * 0.35, -0.45, 0.8);
                    head.setPos(attachment.add(2, 0, 0));
                    head.setOldPosAndRot();
                    level.addEntity(head);
                    try {
                        for (var display : new ItemDisplayContext[] {ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                                ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND}) {
                            var grip = new Capture();
                            var pose = new PoseStack();
                            IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(stack, display, pose, grip, LIGHT, OverlayTexture.NO_OVERLAY);
                            if (grip.types.size() != 2 || !pose.clear()) throw new IllegalStateException("Flying flail retained a duplicate held head");
                        }
                        long previousGeometry = 0;
                        for (float direction : new float[] {90, -90}) {
                            head.setYRot(direction);
                            head.yRotO = direction;
                            var capture = tetherCapture(attachment.subtract(head.getPosition(0.5F)));
                            var pose = new PoseStack();
                            renderer.render(head, direction, 0.5F, pose, capture, LIGHT);
                            if (capture.vertices < 300 || capture.types.size() != 1 || !pose.clear()
                                    || capture.geometry == previousGeometry) throw new IllegalStateException("Flying flail failed to turn or rendered a duplicate chain");
                            previousGeometry = capture.geometry;
                        }
                        head.tickCount = 8;
                        head.tick();
                        if (!head.isReturning()) throw new IllegalStateException("Flail render test did not enter return flight");
                        var returning = tetherCapture(attachment.subtract(head.getPosition(0.5F)));
                        var pose = new PoseStack();
                        renderer.render(head, head.getYRot(), 0.5F, pose, returning, LIGHT);
                        if (returning.vertices < 300 || !pose.clear()) throw new IllegalStateException("Returning flail failed to render");
                    } finally {
                        head.discard();
                    }
                }
            }
            var model = new PufferfishFlailModel();
            var stack = new ItemStack(PufferfishItems.FLAIL.get());
            for (var offset : new Vec3[] {Vec3.ZERO, new Vec3(Double.NaN, 0, 0), new Vec3(0, 0, Double.POSITIVE_INFINITY)}) {
                var capture = new Capture();
                var pose = new PoseStack();
                model.renderChain(stack, offset, 0.2, pose, capture, LIGHT);
                if (capture.vertices != 0 || !pose.clear()) throw new IllegalStateException("Invalid chain emitted geometry");
            }
            var straight = tetherCapture(new Vec3(3, 1, 2));
            model.renderChain(stack, new Vec3(3, 1, 2), 0, new PoseStack(), straight, LIGHT);
            verifyTautChain(straight);
        } finally {
            player.discard();
            minecraft.level = previousLevel;
        }
    }

    private static Capture tetherCapture(Vec3 end) {
        var capture = new Capture();
        capture.tetherEnd = end;
        return capture;
    }

    private static void verifyTautChain(Capture capture) {
        if (capture.tetherVertices == 0 || capture.tetherDeviation > 0.034) {
            throw new IllegalStateException("Moving flail chain is too thick or sags under tension: " + capture.tetherDeviation);
        }
    }

    /// Saves ordinary, dyed, netherite, and heavy flails using their actual GUI display transforms.
    private static void preview(Minecraft minecraft) {
        var target = new TextureTarget(1200, 440, true, Minecraft.ON_OSX);
        target.setClearColor(0.08F, 0.14F, 0.2F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1200, 440, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var red = new ItemStack(PufferfishItems.FLAIL.get());
            red.set(KaleidoscopePufferfish.COLOR.get(), DyeColor.RED);
            var stacks = new ItemStack[] {new ItemStack(PufferfishItems.FLAIL.get()), red, new ItemStack(PufferfishItems.NETHERITE_FLAIL.get()), new ItemStack(PufferfishItems.HEAVY_FLAIL.get())};
            for (int i = 0; i < stacks.length; i++) {
                var pose = new PoseStack();
                pose.translate(150 + i * 300, 220, 0);
                pose.scale(260, -260, 260);
                var buffers = minecraft.renderBuffers().bufferSource();
                minecraft.getItemRenderer().renderStatic(stacks[i], ItemDisplayContext.GUI, LIGHT, OverlayTexture.NO_OVERLAY,
                        pose, buffers, null, 0);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-flail-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save flail preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    /// Counts submitted vertices and distinct render types without allocating GPU buffers.
    private static final class Capture implements MultiBufferSource, VertexConsumer {
        /// Render types observed during the current item render.
        private final Set<RenderType> types = new HashSet<>();
        /// Number of vertices emitted by the item renderer.
        private int vertices;
        private long geometry;
        private RenderType currentType;
        private Vec3 tetherEnd;
        private double tetherDeviation;
        private int tetherVertices;

        /// Creates an empty geometry capture.
        private Capture() {}

        @Override
        public VertexConsumer getBuffer(RenderType type) { types.add(type); currentType = type; return this; }
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalStateException("Non-finite flail vertex");
            }
            vertices++;
            if (tetherEnd != null && currentType == RenderType.entityCutoutNoCull(
                    ResourceLocation.withDefaultNamespace("textures/block/iron_block.png"))) {
                var vertex = new Vec3(x, y, z);
                var projection = tetherEnd.scale(vertex.dot(tetherEnd) / tetherEnd.lengthSqr());
                tetherDeviation = Math.max(tetherDeviation, vertex.distanceTo(projection));
                tetherVertices++;
            }
            geometry = 31 * geometry + Float.floatToIntBits(x);
            geometry = 31 * geometry + Float.floatToIntBits(y);
            geometry = 31 * geometry + Float.floatToIntBits(z);
            return this;
        }
        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override
        public VertexConsumer setUv(float u, float v) { return this; }
        @Override
        public VertexConsumer setUv1(int u, int v) { return this; }
        @Override
        public VertexConsumer setUv2(int u, int v) { return this; }
        @Override
        public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
