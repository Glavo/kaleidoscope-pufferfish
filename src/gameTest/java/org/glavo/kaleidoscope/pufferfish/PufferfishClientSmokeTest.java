// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishItemModel;
import org.glavo.kaleidoscope.pufferfish.client.PufferfishTextures;

@EventBusSubscriber(modid = KaleidoscopePufferfish.MODID, value = Dist.CLIENT)
public final class PufferfishClientSmokeTest {
    private static int phase;
    private static boolean projectileRendererChecked;

    @SubscribeEvent
    public static void checkProjectileRenderer(EntityRenderersEvent.AddLayers event) {
        if (Boolean.getBoolean("kaleidoscope_pufferfish.clientSmokeTest")) {
            if (!(event.getRenderer(KaleidoscopePufferfish.THROWN_PUFFERFISH.get()) instanceof ThrownItemRenderer<?>)) {
                throw new IllegalStateException("Missing pufferfish projectile renderer");
            }
            projectileRendererChecked = true;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Boolean.getBoolean("kaleidoscope_pufferfish.clientSmokeTest") || phase == 2
                || minecraft.screen == null || minecraft.getOverlay() != null) {
            return;
        }
        for (DyeColor color : DyeColor.values()) {
            for (boolean bucket : new boolean[] {false, true}) {
                ItemStack stack = PufferfishColors.dye(new ItemStack(bucket ? Items.PUFFERFISH_BUCKET : Items.PUFFERFISH), color);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                var expected = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(PufferfishTextures.item(color, bucket)));
                if (model != expected || model == minecraft.getModelManager().getMissingModel()
                        || model.getQuads(null, null, RandomSource.create()).isEmpty()
                        || !model.getParticleIcon().contents().name().equals(PufferfishTextures.item(color, bucket))) {
                    throw new IllegalStateException("Missing dyed pufferfish model or sprite: " + color + ", bucket=" + bucket);
                }
                if (stack.getHoverName().getString().contains("color.minecraft.")) {
                    throw new IllegalStateException("Missing dye color translation: " + color);
                }
            }
            var texture = PufferfishTextures.entity(color);
            if (!texture.getNamespace().equals(KaleidoscopePufferfish.MODID)) {
                throw new IllegalStateException("Missing dyed pufferfish entity texture: " + color);
            }
        }
        verifyBlock(minecraft, null);
        for (DyeColor color : DyeColor.values()) {
            verifyBlock(minecraft, color);
        }
        verifyBlock(minecraft, PufferfishBlocks.NETHERITE.get(), "netherite_pufferfish_block");
        var netheriteModel = minecraft.getItemRenderer().getModel(new ItemStack(PufferfishItems.NETHERITE.get()), null, null, 0);
        if (netheriteModel == minecraft.getModelManager().getMissingModel()
                || netheriteModel.getQuads(null, null, RandomSource.create()).isEmpty()
                || !netheriteModel.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/netherite_pufferfish"))) {
            throw new IllegalStateException("Missing netherite pufferfish item model or texture");
        }
        verifyModelDelegation(minecraft);
        PufferfishHeadgearClientTest.verify(minecraft);
        PufferfishGloveClientTest.verify(minecraft);
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            org.glavo.kaleidoscope.pufferfish.compat.create.PufferfishEngineClientTest.verify(minecraft);
        }
        if (!projectileRendererChecked) {
            throw new IllegalStateException("Projectile renderer registration was not checked");
        }
        phase++;
        if (phase == 1) {
            projectileRendererChecked = false;
            minecraft.reloadResourcePacks();
        } else {
            LogUtils.getLogger().info("Pufferfish client smoke test passed: 33 item models, 18 blocks, headgear and glove rendering, projectile renderer, model override delegation, and resource reload");
            minecraft.stop();
        }
    }

    private static void verifyBlock(Minecraft minecraft, DyeColor color) {
        verifyBlock(minecraft, PufferfishBlocks.get(color), PufferfishBlocks.name(color));
    }

    private static void verifyBlock(Minecraft minecraft, net.minecraft.world.level.block.Block block, String name) {
        var state = block.defaultBlockState();
        var stack = new ItemStack(block);
        var blockModel = minecraft.getBlockRenderer().getBlockModel(state);
        var itemModel = minecraft.getItemRenderer().getModel(stack, null, null, 0);
        var texture = KaleidoscopePufferfish.id("block/" + name);
        for (var model : new BakedModel[] {blockModel, itemModel}) {
            if (model == minecraft.getModelManager().getMissingModel()
                    || !model.getParticleIcon().contents().name().equals(texture)) {
                throw new IllegalStateException("Missing pufferfish block model or texture: " + name);
            }
            for (var direction : Direction.values()) {
                if (model.getQuads(state, direction, RandomSource.create()).isEmpty()) {
                    throw new IllegalStateException("Missing pufferfish block face: " + name + ", " + direction);
                }
            }
            var image = model.getParticleIcon().contents().getOriginalImage();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (image.getPixelRGBA(x, y) >>> 24 != 255) {
                        throw new IllegalStateException("Pufferfish block texture is not opaque: " + name);
                    }
                }
            }
        }
        if (stack.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Missing pufferfish block translation: " + name);
        }
    }

    private static void verifyModelDelegation(Minecraft minecraft) {
        var plainStack = new ItemStack(Items.PUFFERFISH);
        BakedModel plain = minecraft.getItemRenderer().getModel(plainStack, null, null, 0);
        BakedModel red = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(PufferfishTextures.item(DyeColor.RED, false)));
        var wrapper = new PufferfishItemModel(plain, Map.of(DyeColor.RED, red));
        if (wrapper.getOverrides().resolve(wrapper, plainStack, null, null, 0) != plain) {
            throw new IllegalStateException("Undyed item model was replaced");
        }
        BakedModel external = new BakedModelWrapper<BakedModel>(plain) {};
        BakedModel original = new BakedModelWrapper<BakedModel>(plain) {
            @Override
            public ItemOverrides getOverrides() {
                return new ItemOverrides() {
                    @Override
                    public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                        return external;
                    }
                };
            }
        };
        wrapper = new PufferfishItemModel(original, Map.of(DyeColor.RED, red));
        var redStack = PufferfishColors.dye(new ItemStack(Items.PUFFERFISH), DyeColor.RED);
        if (wrapper.getOverrides().resolve(wrapper, redStack, null, null, 0) != external) {
            throw new IllegalStateException("External model override was replaced");
        }
    }
}
