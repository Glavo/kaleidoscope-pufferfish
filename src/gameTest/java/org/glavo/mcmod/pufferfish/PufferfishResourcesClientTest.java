// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.glavo.mcmod.pufferfish.client.PufferfishItemModel;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;

public final class PufferfishResourcesClientTest {
    private PufferfishResourcesClientTest() {}

    public static void verify(Minecraft minecraft) {
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(PufferfishPlease.MODID)) continue;
            var stack = new ItemStack(item);
            var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
            if (model == minecraft.getModelManager().getMissingModel()
                    || !model.isCustomRenderer() && model.getParticleIcon().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())
                    || stack.getHoverName().getString().startsWith("item.") || stack.getHoverName().getString().startsWith("block.")) {
                throw new IllegalStateException("Missing item model, sprite or translation: " + id);
            }
        }
        for (var name : PufferfishCatArmor.VARIANTS.keySet()) {
            var texture = PufferfishPlease.id("entity/cat/" + name);
            var sprite = minecraft.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(texture);
            if (sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())) {
                throw new IllegalStateException("Missing worn cat armor texture: " + texture);
            }
        }
        for (var color : DyeColor.values()) {
            for (boolean bucket : new boolean[] {false, true}) {
                var stack = PufferfishColors.dye(new ItemStack(bucket ? Items.PUFFERFISH_BUCKET : Items.PUFFERFISH), color);
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                var expected = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(PufferfishTextures.item(color, bucket)));
                if (model != expected || model == minecraft.getModelManager().getMissingModel()
                        || !model.getParticleIcon().contents().name().equals(PufferfishTextures.item(color, bucket))
                        || stack.getHoverName().getString().contains("color.minecraft.")) {
                    throw new IllegalStateException("Missing dyed fish resources: " + color + ", bucket=" + bucket);
                }
            }
        }
        for (var entry : net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.entrySet()) {
            if (!entry.getKey().location().getNamespace().equals(PufferfishPlease.MODID)) continue;
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(entry.getKey());
            if (!minecraft.getMobEffectTextures().get(holder).contents().name().equals(entry.getKey().location())
                    || holder.value().getDisplayName().getString().startsWith("effect.")) {
                throw new IllegalStateException("Missing effect icon or translation: " + entry.getKey().location());
            }
        }
        for (var variant : PufferfishVariant.ALL) verifySkin(minecraft, variant.skin(), variant.skinName());
        verifyCuttingModels(minecraft);
        verifyDishModels(minecraft);
        verifyModelDelegation(minecraft);
    }

    private static void verifyDishModels(Minecraft minecraft) {
        if (!net.neoforged.fml.ModList.get().isLoaded("kaleidoscope_cookery")) return;
        org.glavo.mcmod.pufferfish.compat.cooking.CookeryPieClientTest.verify(minecraft);
        for (var item : java.util.List.of(PufferfishItems.RAW_BAOZI.get(), PufferfishItems.BAOZI.get())) {
            var stack = new ItemStack(item);
            var name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
            var model = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(PufferfishPlease.id("item/" + name + "_steamer")));
            if (model == minecraft.getModelManager().getMissingModel() || model.getQuads(null, null, RandomSource.create(0)).isEmpty()
                    || com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacer.getModel(null, stack,
                    com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacerReloadListener.INSTANCE.steamer()) != model) {
                throw new IllegalStateException("Steamer did not select the baozi model: " + name);
            }
        }
    }

    /// Verifies chopping stages and their output textures across resource reloads.
    private static void verifyCuttingModels(Minecraft minecraft) {
        if (net.neoforged.fml.ModList.get().isLoaded("kaleidoscope_cookery")) {
            var names = new java.util.ArrayList<String>();
            names.add("pufferfish_skin");
            for (var color : DyeColor.values()) names.add(PufferfishBlocks.skinName(color));
            names.addAll(java.util.List.of("golden_pufferfish", "netherite_pufferfish_skin", "radiant_pufferfish_skin", "heavy_pufferfish_skin"));
            for (var name : names) {
                for (int stage = 0; stage <= 3; stage++) {
                    var id = PufferfishPlease.id("chopping_board/" + name + "/" + stage);
                    var cuttingModel = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(id));
                    if (cuttingModel == minecraft.getModelManager().getMissingModel()
                            || cuttingModel.getQuads(null, null, RandomSource.create()).isEmpty()) {
                        throw new IllegalStateException("Missing chopping board model: " + id);
                    }
                    if (stage == 3) {
                        var textures = cuttingModel.getQuads(null, null, RandomSource.create()).stream()
                                .map(quad -> quad.getSprite().contents().name()).toList();
                        var skin = name.equals("golden_pufferfish") ? "golden_pufferfish_skin" : name;
                        if (!textures.contains(PufferfishPlease.id("item/pufferfish_sashimi"))
                                || !textures.contains(PufferfishPlease.id("block/" + skin))) {
                            throw new IllegalStateException("Chopping board outputs use incorrect textures: " + id);
                        }
                    }
                }
            }
        }
    }

    /// Checks that each skin's block and item models use the matching skin texture and a thin top face.

    private static void verifySkin(Minecraft minecraft, PufferfishSkinBlock block, String color) {
        var state = block.defaultBlockState();
        var stack = new ItemStack(block);
        var texture = PufferfishPlease.id("block/" + color);
        var blockModel = minecraft.getBlockRenderer().getBlockModel(state);
        var itemModel = minecraft.getItemRenderer().getModel(stack, null, null, 0);
        for (var model : new BakedModel[] {blockModel, itemModel}) {
            if (model == minecraft.getModelManager().getMissingModel() || !model.getParticleIcon().contents().name().equals(texture)) {
                throw new IllegalStateException("Missing skin model or color texture: " + color);
            }
            var top = model.getQuads(state, null, RandomSource.create());
            if (top.isEmpty()) throw new IllegalStateException("Missing skin top face: " + color);
            for (var quad : top) {
                var vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    float y = Float.intBitsToFloat(vertices[vertex * stride + 1]);
                    if (Math.abs(y - 1.0F / 16) > 0.0001F) throw new IllegalStateException("Incorrect skin model height: " + color);
                }
            }
        }
        verifySkinOrientations(minecraft, block);
        var image = blockModel.getParticleIcon().contents().getOriginalImage();
        for (int y = 0; y < image.getHeight(); y++) {
            if (image.getPixelRGBA(0, y) != image.getPixelRGBA(image.getWidth() - 1, y)) {
                throw new IllegalStateException("Skin does not tile horizontally: " + color);
            }
        }
        for (int x = 0; x < image.getWidth(); x++) {
            if (image.getPixelRGBA(x, 0) != image.getPixelRGBA(x, image.getHeight() - 1)) {
                throw new IllegalStateException("Skin does not tile vertically: " + color);
            }
        }
        if (stack.getHoverName().getString().startsWith("block.")) {
            throw new IllegalStateException("Missing skin translation: " + color);
        }
    }

    /// Checks every attached model's outward face, thickness, texture, and horizontal rotation.
    private static void verifySkinOrientations(Minecraft minecraft, PufferfishSkinBlock block) {
        var reference = minecraft.getBlockRenderer().getBlockModel(block.defaultBlockState())
                .getQuads(block.defaultBlockState(), null, RandomSource.create()).getFirst().getVertices();
        for (var state : block.getStateDefinition().getPossibleStates()) {
            var model = minecraft.getBlockRenderer().getBlockModel(state);
            var normal = switch (state.getValue(PufferfishSkinBlock.FACE)) {
                case FLOOR -> Direction.UP;
                case CEILING -> Direction.DOWN;
                case WALL -> state.getValue(PufferfishSkinBlock.FACING);
            };
            var quads = model.getQuads(state, null, RandomSource.create());
            if (model == minecraft.getModelManager().getMissingModel() || quads.isEmpty()) {
                throw new IllegalStateException("Missing attached skin model: " + state);
            }
            float coordinate = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F / 16 : 15.0F / 16;
            int axis = switch (normal.getAxis()) { case X -> 0; case Y -> 1; case Z -> 2; };
            for (var quad : quads) {
                if (quad.getDirection() != normal) throw new IllegalStateException("Incorrect skin face normal: " + state);
                var vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    if (Math.abs(Float.intBitsToFloat(vertices[vertex * stride + axis]) - coordinate) > 0.0001F) {
                        throw new IllegalStateException("Skin model does not match its attachment shape: " + state);
                    }
                }
            }
            if (normal == Direction.UP && state.getValue(PufferfishSkinBlock.FACING) != Direction.NORTH
                    && java.util.Arrays.equals(reference, quads.getFirst().getVertices())) {
                throw new IllegalStateException("Skin texture orientation is locked: " + state);
            }
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
