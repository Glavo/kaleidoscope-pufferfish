// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;

public final class PufferfishHeadgearClientTest {
    private static ClientLevel level;

    private static <T> Registry<T> copyRegistry(HolderLookup.Provider lookup, ResourceKey<? extends Registry<T>> key) {
        var registry = new MappedRegistry<T>(key, Lifecycle.stable());
        lookup.lookupOrThrow(key).listElements().forEach(holder -> registry.register(holder.key(), holder.value(), RegistrationInfo.BUILT_IN));
        return registry.freeze();
    }

    public static ClientLevel level(Minecraft minecraft) {
        if (level == null) {
            var lookup = VanillaRegistries.createLookup();
            var dynamic = Stream.of(
                    new RegistryAccess.RegistryEntry<>(Registries.DAMAGE_TYPE, copyRegistry(lookup, Registries.DAMAGE_TYPE)),
                    new RegistryAccess.RegistryEntry<>(Registries.DIMENSION_TYPE, copyRegistry(lookup, Registries.DIMENSION_TYPE)),
                    new RegistryAccess.RegistryEntry<>(Registries.BIOME, copyRegistry(lookup, Registries.BIOME)));
            var access = new RegistryAccess.ImmutableRegistryAccess(Stream.concat(
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).registries(), dynamic)).freeze();
            var cookie = new CommonListenerCookie(new GameProfile(UUID.randomUUID(), "headgear-client-test"), null,
                    access, FeatureFlags.DEFAULT_FLAGS, null, null, null, Map.of(), null, false, Map.of(),
                    ServerLinks.EMPTY, ConnectionType.NEOFORGE);
            var connection = new ClientPacketListener(minecraft, new Connection(PacketFlow.CLIENTBOUND), cookie);
            level = new ClientLevel(connection, new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false),
                    Level.OVERWORLD, access.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                    2, 2, minecraft::getProfiler, minecraft.levelRenderer, false, 0);
        }
        return level;
    }

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
        verifyZombie(minecraft, context);
        verifyArmorStand(minecraft, context);
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
                NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Pre(player, renderer, 0, pose, capture, 15728880));
                if (model.head.visible || model.hat.visible) {
                    throw new IllegalStateException("Player head was not hidden");
                }
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
                NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(player, renderer, 0, pose, capture, 15728880));
                if (!model.head.visible || !model.hat.visible) {
                    throw new IllegalStateException("Player head visibility was not restored");
                }
            }
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            model.hat.visible = false;
            var pose = new PoseStack();
            var capture = new CapturedVertices();
            NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Pre(player, renderer, 0, pose, capture, 15728880));
            layer.render(pose, capture, 15728880, player, 0, 0, 0, 0, 0, 0);
            NeoForge.EVENT_BUS.post(new RenderPlayerEvent.Post(player, renderer, 0, pose, capture, 15728880));
            if (capture.vertices != 0 || !model.head.visible || model.hat.visible) {
                throw new IllegalStateException("Unequipped player retained headgear rendering");
            }
        }
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
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<>(zombie, renderer, 0, pose, capture, 15728880));
                if (model.head.visible || model.hat.visible) throw new IllegalStateException("Zombie head was not hidden");
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
                NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post<>(zombie, renderer, 0, pose, capture, 15728880));
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
                        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<>(stand, renderer, 0, pose, capture, 15728880));
                        model.setupAnim(stand, 0, 0, 0, 0, 0);
                        if (model.head.visible || model.hat.visible) throw new IllegalStateException("Armor stand head was not hidden");
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
                        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post<>(stand, renderer, 0, pose, capture, 15728880));
                        if (!model.head.visible || model.hat.visible) throw new IllegalStateException("Armor stand head visibility was not restored");
                    }
                }
            }
            stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PUFFERFISH));
            var pose = new PoseStack();
            var capture = new CapturedVertices();
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<>(stand, renderer, 0, pose, capture, 15728880));
            layer.render(pose, capture, 15728880, stand, 0, 0, 0, 0, 0, 0);
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post<>(stand, renderer, 0, pose, capture, 15728880));
            if (capture.vertices != 0 || !model.head.visible || model.hat.visible) {
                throw new IllegalStateException("Armor stand hand item incorrectly replaced its head");
            }
        }
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
