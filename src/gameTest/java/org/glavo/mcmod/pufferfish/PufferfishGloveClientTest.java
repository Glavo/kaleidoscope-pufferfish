// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.glavo.mcmod.pufferfish.client.PufferfishGloveRenderer;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;
import org.joml.Matrix4f;

public final class PufferfishGloveClientTest {
    private static final int LIGHT = 15728880;

    public static void verify(Minecraft minecraft) {
        var player = new RemotePlayer(PufferfishHeadgearClientTest.level(minecraft), new GameProfile(UUID.randomUUID(), "glove-test")) {
            @Override
            protected PlayerInfo getPlayerInfo() { return null; }
        };
        var context = new EntityRendererProvider.Context(minecraft.getEntityRenderDispatcher(), minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(), minecraft.getEntityRenderDispatcher().getItemInHandRenderer(),
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        var stacks = new ArrayList<ItemStack>();
        stacks.add(new ItemStack(Items.PUFFERFISH));
        for (var color : DyeColor.values()) stacks.add(PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), color));
        stacks.add(new ItemStack(PufferfishItems.NETHERITE.get()));
        stacks.add(new ItemStack(PufferfishItems.HEAVY.get()));
        stacks.add(new ItemStack(PufferfishItems.GOLDEN.get()));
        stacks.add(new ItemStack(PufferfishItems.RADIANT.get()));
        stacks.addAll(java.util.List.of(new ItemStack(PufferfishItems.COPPER.get()), new ItemStack(PufferfishItems.IRON.get()), new ItemStack(PufferfishItems.DIAMOND.get())));
        var glove = new PufferfishGloveRenderer();
        for (boolean slim : new boolean[] {false, true}) {
            var renderer = new PlayerRenderer(context, slim);
            var model = renderer.getModel();
            model.young = false;
            var layer = new ItemInHandLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(renderer, context.getItemInHandRenderer());
            for (var arm : HumanoidArm.values()) {
                player.setMainArm(arm);
                for (var stack : stacks) {
                    player.setItemSlot(EquipmentSlot.MAINHAND, stack);
                    var capture = new Capture();
                    var pose = new PoseStack();
                    layer.render(pose, capture, LIGHT, player, 0, 0, 0, 0, 0, 0);
                    var vertices = capture.parts.get(type(stack));
                    float center = arm == HumanoidArm.RIGHT ? -0.36F : 0.36F;
                    if (capture.parts.size() != (PufferfishItems.isRadiant(stack) ? 2 : 1) || vertices == null || vertices.count < 100
                            || Math.abs((vertices.minX + vertices.maxX) / 2 - center) > 0.05F
                            || vertices.minY < 0.28F || vertices.maxY > 0.95F || !pose.clear()) {
                        throw new IllegalStateException("Glove did not replace held fish or cover the hand: " + stack + ", " + arm
                                + ", slim=" + slim + ", parts=" + capture.parts.size() + ", vertices=" + vertices);
                    }
                    var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                    part.xRot = -1.2F;
                    capture = new Capture();
                    layer.render(pose, capture, LIGHT, player, 0, 0, 0, 0, 0, 0);
                    if (Math.abs(capture.parts.get(type(stack)).minY - vertices.minY) < 0.1F) {
                        throw new IllegalStateException("Glove did not follow the swinging arm");
                    }
                    part.xRot = 0;
                    for (float swing : new float[] {0, 0.4F, 0.9F}) {
                        capture = new Capture();
                        glove.renderFirstPerson(player, stack, arm, 0, swing, pose, capture, LIGHT);
                        if (!capture.parts.containsKey(type(stack)) || capture.parts.get(type(stack)).count < 100 || !pose.clear()) {
                            throw new IllegalStateException("First-person glove failed during swing");
                        }
                    }
                }
            }
            for (var original : stacks) {
                var deflated = original.copy();
                PufferfishDurability.activate(deflated);
                deflated.setDamageValue(deflated.getMaxDamage() / 2 + 1);
                player.setItemSlot(EquipmentSlot.MAINHAND, deflated);
                var capture = new Capture();
                var pose = new PoseStack();
                layer.render(pose, capture, LIGHT, player, 0, 0, 0, 0, 0, 0);
                var wornVertices = capture.parts.get(type(deflated));
                var gloveCapture = new Capture();
                glove.renderAttached(model, player, deflated, player.getMainArm(), new PoseStack(), gloveCapture, LIGHT, 0);
                var gloveVertices = gloveCapture.parts.get(type(deflated));
                if (wornVertices == null || !pose.clear()
                        || Math.abs((wornVertices.maxY - wornVertices.minY) - (gloveVertices.maxY - gloveVertices.minY)) < 0.01F) {
                    throw new IllegalStateException("Deflated fish remained a glove instead of a held item: " + deflated);
                }
            }
            var foil = stacks.getLast().copy();
            foil.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            player.setItemSlot(EquipmentSlot.MAINHAND, foil);
            player.setItemSlot(EquipmentSlot.OFFHAND, stacks.getFirst());
            var capture = new Capture();
            layer.render(new PoseStack(), capture, LIGHT, player, 0, 0, 0, 0, 0, 0);
            if (!capture.parts.containsKey(type(foil)) || !capture.parts.containsKey(type(stacks.getFirst())) || capture.parts.size() < 3) {
                throw new IllegalStateException("Dual gloves or enchantment glint did not render");
            }
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            capture = new Capture();
            layer.render(new PoseStack(), capture, LIGHT, player, 0, 0, 0, 0, 0, 0);
            if (capture.parts.isEmpty() || capture.parts.containsKey(type(stacks.getFirst()))) {
                throw new IllegalStateException("Non-pufferfish held item rendering changed");
            }
        }
        renderPreview(minecraft, player, context, glove);
    }

    private static RenderType type(ItemStack stack) {
        var color = PufferfishColors.get(stack);
        return RenderType.entityCutoutNoCull(PufferfishItems.isHeavy(stack) ? PufferfishTextures.heavyEntity()
                : PufferfishItems.isNetherite(stack) ? PufferfishTextures.netheriteEntity()
                : PufferfishItems.materialName(stack) != null ? PufferfishTextures.materialEntity(PufferfishItems.materialName(stack))
                : color == null ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png") : PufferfishTextures.entity(color));
    }

    private static void renderPreview(Minecraft minecraft, AbstractClientPlayer player, EntityRendererProvider.Context context,
            PufferfishGloveRenderer glove) {
        var target = new TextureTarget(1200, 800, true, Minecraft.ON_OSX);
        target.setClearColor(0.12F, 0.15F, 0.18F, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1200, 800, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        try {
            var buffers = minecraft.renderBuffers().bufferSource();
            for (int i = 0; i < 4; i++) {
                var model = new PlayerRenderer(context, i % 2 == 1).getModel();
                model.young = false;
                model.rightArm.xRot = i >= 2 ? -1.3F : 0;
                model.leftArm.xRot = i >= 2 ? -0.5F : 0;
                var pose = new PoseStack();
                pose.translate(150 + i * 300, 90, 0);
                pose.scale(150, 150, -150);
                pose.mulPose(Axis.YP.rotationDegrees(-25));
                model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture())), LIGHT, OverlayTexture.NO_OVERLAY);
                glove.renderAttached(model, player, PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.RED), HumanoidArm.RIGHT,
                        pose, buffers, LIGHT, 0);
                glove.renderAttached(model, player, new ItemStack(i < 2 ? PufferfishItems.NETHERITE.get() : PufferfishItems.RADIANT.get()),
                        HumanoidArm.LEFT, pose, buffers, LIGHT, 0);
                buffers.endBatch();
            }
            for (int i = 0; i < 4; i++) {
                var pose = new PoseStack();
                pose.translate(150 + i * 300, 620, 0);
                pose.scale(160, -160, 160);
                glove.renderFirstPerson(player, new ItemStack(i < 2 ? Items.PUFFERFISH : PufferfishItems.RADIANT.get()), i % 2 == 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT,
                        0, i < 2 ? 0 : 0.4F, pose, buffers, LIGHT);
                buffers.endBatch();
            }
            try (var image = Screenshot.takeScreenshot(target)) {
                image.writeToFile(Path.of("pufferfish-glove-preview.png"));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save glove preview", exception);
        } finally {
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.destroyBuffers();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private static final class Capture implements MultiBufferSource {
        private final Map<RenderType, Vertices> parts = new HashMap<>();
        @Override
        public VertexConsumer getBuffer(RenderType type) { return parts.computeIfAbsent(type, key -> new Vertices()); }
    }

    private static final class Vertices implements VertexConsumer {
        private int count;
        private float minX = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        @Override
        public String toString() { return count + " vertices, x=" + minX + ".." + maxX + ", y=" + minY + ".." + maxY; }
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            count++;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
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
