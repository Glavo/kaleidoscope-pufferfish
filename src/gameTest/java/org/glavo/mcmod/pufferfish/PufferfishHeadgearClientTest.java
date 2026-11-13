// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import static org.glavo.mcmod.pufferfish.test.ClientTestLevel.level;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;

public final class PufferfishHeadgearClientTest {
    public static void verify(Minecraft minecraft) {
        var player = new RemotePlayer(level(minecraft), new GameProfile(UUID.randomUUID(), "headgear-render-test")) {
            @Override
            protected PlayerInfo getPlayerInfo() {
                return null;
            }
        };
        var context = new EntityRendererProvider.Context(minecraft.getEntityRenderDispatcher(), minecraft.getItemRenderer(),
                minecraft.getBlockRenderer(), minecraft.getEntityRenderDispatcher().getItemInHandRenderer(),
                minecraft.getResourceManager(), minecraft.getEntityModels(), minecraft.font);
        verifyScopedVisibility(minecraft, context);
        verifyZombie(minecraft, context);
        verifyArmorStand(minecraft, context);
        var dispatcher = minecraft.getEntityRenderDispatcher();
        var camera = dispatcher.camera;
        var previousPlayer = minecraft.player;
        try {
            dispatcher.camera = new net.minecraft.client.Camera();
            minecraft.player = new net.minecraft.client.player.LocalPlayer(minecraft, level(minecraft),
                    org.glavo.mcmod.pufferfish.test.ClientTestLevel.connection(minecraft),
                    new net.minecraft.stats.StatsCounter(), new net.minecraft.client.ClientRecipeBook(), false, false);
            verifyGolem(minecraft);
        } finally {
            dispatcher.camera = camera;
            minecraft.player = previousPlayer;
        }
        for (boolean slim : new boolean[] {false, true}) {
            var renderer = new PlayerRenderer(context, slim);
            var model = renderer.getModel();
            var layer = new CustomHeadLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>(
                    renderer, minecraft.getEntityModels(), context.getItemInHandRenderer());
            List<DyeColor> colors = new ArrayList<>();
            colors.add(null);
            colors.addAll(List.of(DyeColor.values()));
            List<ItemStack> headgear = new ArrayList<>();
            for (var color : colors) {
                var fish = new ItemStack(Items.PUFFERFISH);
                headgear.add(color == null ? fish : PufferfishColors.dye(fish, color));
            }
            headgear.add(new ItemStack(PufferfishItems.NETHERITE.get()));
            headgear.add(new ItemStack(PufferfishItems.HEAVY.get()));
            headgear.add(new ItemStack(PufferfishItems.GOLDEN.get()));
            headgear.addAll(List.of(new ItemStack(PufferfishItems.COPPER.get()), new ItemStack(PufferfishItems.IRON.get()), new ItemStack(PufferfishItems.DIAMOND.get())));
            for (var stack : headgear) {
                player.setItemSlot(EquipmentSlot.HEAD, stack);
                model.head.visible = true;
                model.hat.visible = true;
                var capture = new CapturedVertices();
                var pose = new PoseStack();
                layer.render(pose, capture, 15728880, player, 0, 0, 0, 0, 0, 0);
                var color = PufferfishColors.get(stack);
                var texture = PufferfishItems.isHeavy(stack) ? PufferfishTextures.heavyEntity()
                        : PufferfishItems.isNetherite(stack) ? PufferfishTextures.netheriteEntity()
                        : PufferfishItems.materialName(stack) != null ? PufferfishTextures.materialEntity(PufferfishItems.materialName(stack))
                        : color == null ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png") : PufferfishTextures.entity(color);
                if (capture.type != RenderType.entityCutoutNoCull(texture) || capture.vertices < 100
                        || capture.minY < -0.65F || capture.maxY > 0.15F) {
                    throw new IllegalStateException("Pufferfish head model, texture, or alignment was incorrect: " + stack);
                }
                if (!model.head.visible || !model.hat.visible) {
                    throw new IllegalStateException("Player head visibility was not restored");
                }
            }
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            model.hat.visible = false;
            var pose = new PoseStack();
            var capture = new CapturedVertices();
            layer.render(pose, capture, 15728880, player, 0, 0, 0, 0, 0, 0);
            if (capture.vertices != 0 || !model.head.visible || model.hat.visible) {
                throw new IllegalStateException("Unequipped player retained headgear rendering");
            }
        }
    }

    private static void verifyScopedVisibility(Minecraft minecraft, EntityRendererProvider.Context context) {
        var entity = net.minecraft.world.entity.EntityType.ZOMBIE.create(level(minecraft));
        entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PUFFERFISH));
        Runnable[] drawing = {() -> {}};
        var model = new net.minecraft.client.model.HumanoidModel<net.minecraft.world.entity.LivingEntity>(
                minecraft.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.ZOMBIE)) {
            @Override
            public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
                drawing[0].run();
            }
        };
        var renderer = new net.minecraft.client.renderer.entity.LivingEntityRenderer<>(context, model, 0) {
            @Override public ResourceLocation getTextureLocation(net.minecraft.world.entity.LivingEntity target) {
                return ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
            }
            @Override protected boolean shouldShowName(net.minecraft.world.entity.LivingEntity target) { return false; }
        };
        model.head.visible = true;
        model.hat.visible = false;
        int[] calls = {0};
        drawing[0] = () -> {
            if (model.head.visible || model.hat.visible) throw new IllegalStateException("Head was visible during body rendering");
            if (++calls[0] == 1) renderer.render(entity, 0, 0, new PoseStack(), new CapturedVertices(), 15728880);
            if (model.head.visible || model.hat.visible) throw new IllegalStateException("Nested rendering restored the outer head too early");
        };
        renderer.render(entity, 0, 0, new PoseStack(), new CapturedVertices(), 15728880);
        if (calls[0] != 2 || !model.head.visible || model.hat.visible) {
            throw new IllegalStateException("Nested rendering lost the original head visibility");
        }
        java.util.function.Consumer<net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?>> cancel = event -> {
            if (event.getEntity() == entity) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            renderer.render(entity, 0, 0, new PoseStack(), new CapturedVertices(), 15728880);
            if (calls[0] != 2 || !model.head.visible || model.hat.visible) {
                throw new IllegalStateException("Canceled rendering changed head visibility");
            }
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        var failure = new IllegalStateException("Expected rendering failure");
        drawing[0] = () -> { throw failure; };
        try {
            renderer.render(entity, 0, 0, new PoseStack(), new CapturedVertices(), 15728880);
            throw new IllegalStateException("Rendering failure was swallowed");
        } catch (IllegalStateException exception) {
            if (exception != failure) throw exception;
        }
        if (!model.head.visible || model.hat.visible) throw new IllegalStateException("Failed rendering leaked head visibility");
    }

    private static void verifyZombie(Minecraft minecraft, EntityRendererProvider.Context context) {
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level(minecraft));
        var renderer = new net.minecraft.client.renderer.entity.ZombieRenderer(context);
        var model = renderer.getModel();
        var layer = new CustomHeadLayer<>(renderer, minecraft.getEntityModels(), context.getItemInHandRenderer());
        var variants = new ArrayList<ItemStack>();
        variants.add(new ItemStack(Items.PUFFERFISH));
        PufferfishItems.COPPER_VARIANTS.forEach(item -> variants.add(new ItemStack(item.get())));
        variants.add(new ItemStack(PufferfishItems.IRON.get()));
        variants.add(new ItemStack(PufferfishItems.GOLDEN.get()));
        variants.add(new ItemStack(PufferfishItems.DIAMOND.get()));
        for (var stack : variants) {
            var material = PufferfishItems.materialName(stack);
            var texture = material == null ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png")
                    : PufferfishTextures.materialEntity(material);
            float adultMinY = 0;
            float adultMaxY = 0;
            for (boolean baby : new boolean[] {false, true}) {
                zombie.setBaby(baby);
                model.young = baby;
                zombie.setItemSlot(EquipmentSlot.HEAD, stack.copy());
                var pose = new PoseStack();
                var capture = new CapturedVertices();
                layer.render(pose, capture, 15728880, zombie, 0, 0, 0, 0, 0, 0);
                if (capture.vertices < 100 || capture.type != RenderType.entityCutoutNoCull(texture)) {
                    throw new IllegalStateException("Zombie pufferfish head did not render");
                }
                if (baby) {
                    if (Math.abs(capture.minY - (adultMinY * 0.75F + 0.75F)) > 0.001F
                            || Math.abs(capture.maxY - (adultMaxY * 0.75F + 0.75F)) > 0.001F) {
                        throw new IllegalStateException("Baby zombie headgear did not follow the humanoid head transform");
                    }
                } else {
                    adultMinY = capture.minY;
                    adultMaxY = capture.maxY;
                }
                if (!model.head.visible || !model.hat.visible) throw new IllegalStateException("Zombie head visibility was not restored");
            }
        }
    }

    private static void verifyArmorStand(Minecraft minecraft, EntityRendererProvider.Context context) {
        var renderer = new net.minecraft.client.renderer.entity.ArmorStandRenderer(context);
        var model = renderer.getModel();
        var layer = new CustomHeadLayer<>(renderer, minecraft.getEntityModels(), context.getItemInHandRenderer());
        for (boolean small : new boolean[] {false, true}) {
            var stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(level(minecraft));
            var data = new net.minecraft.nbt.CompoundTag();
            data.putBoolean("Small", small);
            stand.readAdditionalSaveData(data);
            model.young = small;
            for (var variant : PufferfishVariant.ALL) {
                for (int damage : new int[] {0, 3}) {
                    var stack = variant.fish();
                    if (damage > 0) {
                        PufferfishDurability.activate(stack);
                        stack.setDamageValue((stack.getMaxDamage() * damage + 3) / 4);
                    }
                    stand.setItemSlot(EquipmentSlot.HEAD, stack);
                    if (PufferfishHeadgear.isWearing(stand)) {
                        throw new IllegalStateException("Armor stand display enabled living headgear effects");
                    }
                    for (var rotation : List.of(new net.minecraft.core.Rotations(0, 0, 0), new net.minecraft.core.Rotations(25, 40, -15))) {
                        stand.setHeadPose(rotation);
                        var pose = new PoseStack();
                        var capture = new CapturedVertices();
                        model.setupAnim(stand, 0, 0, 0, 0, 0);
                        layer.render(pose, capture, 15728880, stand, 0, 0, 0, 0, 0, 0);
                        var texture = switch (variant.material()) {
                            case NORMAL -> variant.color() == null ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png")
                                    : PufferfishTextures.entity(variant.color());
                            case NETHERITE -> PufferfishTextures.netheriteEntity();
                            case HEAVY -> PufferfishTextures.heavyEntity();
                            default -> PufferfishTextures.materialEntity(PufferfishItems.materialName(stack));
                        };
                        if (capture.vertices < 100 || !capture.types.contains(RenderType.entityCutoutNoCull(texture))
                                || !Float.isFinite(capture.minY) || capture.minY < -1 || capture.maxY > 1.5F) {
                            throw new IllegalStateException("Incorrect armor stand fish model, texture or head transform: " + variant.fishName());
                        }
                        if (!model.head.visible || model.hat.visible) throw new IllegalStateException("Armor stand head visibility was not restored");
                    }
                }
            }
            stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PUFFERFISH));
            var pose = new PoseStack();
            var capture = new CapturedVertices();
            layer.render(pose, capture, 15728880, stand, 0, 0, 0, 0, 0, 0);
            if (capture.vertices != 0 || !model.head.visible || model.hat.visible) {
                throw new IllegalStateException("Armor stand hand item incorrectly replaced its head");
            }
        }
    }

    private static void verifyGolem(Minecraft minecraft) {
        var golem = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level(minecraft));
        var renderer = (net.minecraft.client.renderer.entity.IronGolemRenderer) minecraft.getEntityRenderDispatcher().getRenderer(golem);
        for (var variant : PufferfishVariant.ALL) {
            golem.setItemSlot(EquipmentSlot.HEAD, variant.fish());
            var capture = new CapturedVertices();
            renderer.render(golem, 0, 0, new PoseStack(), capture, 15728880);
            var texture = switch (variant.material()) {
                case NORMAL -> variant.color() == null ? ResourceLocation.withDefaultNamespace("textures/entity/fish/pufferfish.png")
                        : PufferfishTextures.entity(variant.color());
                case NETHERITE -> PufferfishTextures.netheriteEntity();
                case HEAVY -> PufferfishTextures.heavyEntity();
                default -> PufferfishTextures.materialEntity(PufferfishItems.materialName(golem.getItemBySlot(EquipmentSlot.HEAD)));
            };
            if (!capture.types.contains(RenderType.entityCutoutNoCull(texture))) {
                throw new IllegalStateException("Registered golem renderer did not draw fish headgear: " + variant.fishName());
            }
        }
        var layer = new org.glavo.mcmod.pufferfish.client.PufferfishGolemHeadLayer(renderer);
        golem.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PUFFERFISH));
        var model = renderer.getModel();
        model.setupAnim(golem, 0, 0, 0, 0, 0);
        var capture = new CapturedVertices();
        layer.render(new PoseStack(), capture, 15728880, golem, 0, 0, 0, 0, 0, 0);
        if (capture.vertices < 100 || capture.maxY > -1.15F || capture.minY < -1.9F) {
            throw new IllegalStateException("Golem fish was not seated above the head");
        }
        model.root().getChild("head").y += 2;
        var moved = new CapturedVertices();
        layer.render(new PoseStack(), moved, 15728880, golem, 0, 0, 0, 0, 0, 0);
        model.root().getChild("head").y -= 2;
        if (Math.abs(moved.minY - capture.minY - 0.125F) > 0.001F) {
            throw new IllegalStateException("Golem headgear did not follow the head transform");
        }
        golem.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        capture = new CapturedVertices();
        layer.render(new PoseStack(), capture, 15728880, golem, 0, 0, 0, 0, 0, 0);
        if (capture.vertices != 0) throw new IllegalStateException("Removed golem headgear still rendered");
    }

    private static final class CapturedVertices implements MultiBufferSource, VertexConsumer {
        private final List<RenderType> types = new ArrayList<>();
        private RenderType type;
        private int vertices;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            types.add(type);
            this.type = type;
            return this;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            vertices++;
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
