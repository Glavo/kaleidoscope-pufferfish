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
import org.glavo.kaleidoscope.pufferfish.client.PufferfishFlailHeadRenderer;

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
            if (!(event.getRenderer(KaleidoscopePufferfish.FLAIL_HEAD.get()) instanceof PufferfishFlailHeadRenderer)) {
                throw new IllegalStateException("Missing flail head renderer");
            }
            projectileRendererChecked = true;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Boolean.getBoolean("kaleidoscope_pufferfish.clientSmokeTest") || phase == 3
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
        PufferfishDyedTextureClientTest.verify(minecraft, phase == 1);
        verifySkin(minecraft, null);
        var upgradeTemplate = PufferfishItems.UPGRADE_TEMPLATE.toStack();
        var templateModel = minecraft.getItemRenderer().getModel(upgradeTemplate, null, null, 0);
        if (templateModel == minecraft.getModelManager().getMissingModel()
                || !templateModel.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/pufferfish_upgrade_smithing_template"))
                || templateModel.getQuads(null, null, RandomSource.create()).isEmpty()
                || upgradeTemplate.getHoverName().getString().startsWith("item.")) {
            throw new IllegalStateException("Missing pufferfish upgrade template model or translation");
        }
        var templateImage = templateModel.getParticleIcon().contents().getOriginalImage();
        try (var preview = new com.mojang.blaze3d.platform.NativeImage(256, 256, true)) {
            for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
                preview.setPixelRGBA(x, y, templateImage.getPixelRGBA(x * templateImage.getWidth() / 256, y * templateImage.getHeight() / 256));
            }
            preview.writeToFile(java.nio.file.Path.of("pufferfish-upgrade-template-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save pufferfish upgrade template preview", exception);
        }
        MaterialPufferfishClientTest.verify(minecraft);
        PufferfishMobClientTest.verify(minecraft);
        verifySkin(minecraft, PufferfishBlocks.GOLDEN_SKIN.get(), "golden_pufferfish_skin");
        verifySkin(minecraft, PufferfishBlocks.NETHERITE_SKIN.get(), "netherite_pufferfish_skin");
        verifySkin(minecraft, PufferfishBlocks.RADIANT_SKIN.get(), "radiant_pufferfish_skin");
        verifySkin(minecraft, PufferfishBlocks.HEAVY_SKIN.get(), "heavy_pufferfish_skin");
        verifySkin(minecraft, PufferfishBlocks.COPPER_SKIN.get(), "copper_pufferfish_skin");
        verifySkin(minecraft, PufferfishBlocks.IRON_SKIN.get(), "iron_pufferfish_skin");
        verifySkin(minecraft, PufferfishBlocks.DIAMOND_SKIN.get(), "diamond_pufferfish_skin");
        for (DyeColor color : DyeColor.values()) {
            verifySkin(minecraft, color);
        }
        PufferfishSkinClientPreview.save(minecraft);
        var netheriteModel = minecraft.getItemRenderer().getModel(new ItemStack(PufferfishItems.NETHERITE.get()), null, null, 0);
        if (netheriteModel == minecraft.getModelManager().getMissingModel()
                || netheriteModel.getQuads(null, null, RandomSource.create()).isEmpty()
                || !netheriteModel.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/netherite_pufferfish"))) {
            throw new IllegalStateException("Missing netherite pufferfish item model or texture");
        }
        var goldenStack = new ItemStack(PufferfishItems.GOLDEN.get());
        var goldenModel = minecraft.getItemRenderer().getModel(goldenStack, null, null, 0);
        if (goldenModel == minecraft.getModelManager().getMissingModel()
                || goldenModel.getQuads(null, null, RandomSource.create()).isEmpty()
                || !goldenModel.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/golden_pufferfish"))
                || goldenStack.getHoverName().getString().startsWith("item.")) {
            throw new IllegalStateException("Missing golden pufferfish item model, texture, or translation");
        }
        var goldenSprite = goldenModel.getParticleIcon().contents().getOriginalImage();
        try (var preview = new com.mojang.blaze3d.platform.NativeImage(256, 256, true)) {
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    preview.setPixelRGBA(x, y, goldenSprite.getPixelRGBA(
                            x * goldenSprite.getWidth() / 256, y * goldenSprite.getHeight() / 256));
                }
            }
            preview.writeToFile(java.nio.file.Path.of("golden-pufferfish-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save golden pufferfish sprite preview", exception);
        }
        verifyCuttingModels(minecraft);
        PufferfishTooltipClientTest.verify();
        verifyDishModels(minecraft);
        verifyHeavyModel(minecraft);
        verifyWhiteEyes(minecraft);
        PufferfishEyesClientPreview.save(minecraft);
        verifyModelDelegation(minecraft);
        PufferfishHeadgearClientTest.verify(minecraft);
        PufferfishGloveClientTest.verify(minecraft);
        PufferfishDeflationClientTest.verify(minecraft);
        PufferfishPumpClientTest.verify(minecraft);
        PufferfishConduitClientTest.verify(minecraft);
        PufferfishPlacementClientTest.verify(minecraft);
        PufferfishLampClientTest.verify(minecraft);
        PufferfishFlailClientTest.verify(minecraft);
        PufferfishShieldClientTest.verify(minecraft);
        PufferfishArmorClientTest.verify(minecraft);
        PufferfishBoatClientTest.verify(minecraft);
        PufferfishRowingClientTest.verify(minecraft);
        PufferfishFlailFirstPersonClientTest.verify(minecraft);
        PufferfishFlailOrbitClientTest.verify(minecraft);
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            org.glavo.kaleidoscope.pufferfish.compat.create.PufferfishEngineClientTest.verify(minecraft);
        }
        if (!projectileRendererChecked) {
            throw new IllegalStateException("Projectile renderer registration was not checked");
        }
        phase++;
        if (phase < 3) {
            if (phase == 1) PufferfishDyedTextureClientTest.enablePack(minecraft);
            else PufferfishDyedTextureClientTest.disablePack(minecraft);
            projectileRendererChecked = false;
            minecraft.reloadResourcePacks();
        } else {
            LogUtils.getLogger().info("Pufferfish client smoke test passed: fish models, 23 skins, cutting models, headgear, gloves, flails, shields, armor, boats and conduit rendering, projectile renderers, model override delegation, and resource reload");
            minecraft.stop();
        }
    }

    /// Verifies cooked dish models and saves their stitched sprites side by side for visual inspection.
    private static void verifyDishModels(Minecraft minecraft) {
        if (net.neoforged.fml.ModList.get().isLoaded("kaleidoscope_cookery")) {
            org.glavo.kaleidoscope.pufferfish.compat.cooking.CookeryBaoziClientTest.preview(minecraft);
            org.glavo.kaleidoscope.pufferfish.compat.cooking.CookeryPieClientTest.verify(minecraft);
        }
        try (var preview = new com.mojang.blaze3d.platform.NativeImage(1280, 256, true)) {
            var items = java.util.List.of(PufferfishItems.BRAISED.get(), PufferfishItems.BRAISED_RICE_BOWL.get(), PufferfishItems.ZOSUI.get(),
                    PufferfishItems.RAW_BAOZI.get(), PufferfishItems.BAOZI.get());
            for (int index = 0; index < items.size(); index++) {
                var stack = new ItemStack(items.get(index));
                var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
                var name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(items.get(index)).getPath();
                if (model == minecraft.getModelManager().getMissingModel()
                        || model.getQuads(null, null, RandomSource.create()).isEmpty()
                        || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/" + name))
                        || stack.getHoverName().getString().startsWith("item.")) {
                    throw new IllegalStateException("Missing cooked dish model, sprite, or translation: " + name);
                }
                var sprite = model.getParticleIcon().contents().getOriginalImage();
                if (name.endsWith("baozi")) {
                    var steamerModel = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(
                            KaleidoscopePufferfish.id("item/" + name + "_steamer")));
                    if (steamerModel == minecraft.getModelManager().getMissingModel()
                            || steamerModel.getQuads(null, null, RandomSource.create()).size() < 30) {
                        throw new IllegalStateException("Missing three-dimensional baozi model: " + name);
                    }
                    if (net.neoforged.fml.ModList.get().isLoaded("kaleidoscope_cookery")
                            && com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacer.getModel(null, stack,
                            com.github.ysbbbbbb.kaleidoscopecookery.client.resources.ItemRenderReplacerReloadListener.INSTANCE.steamer()) != steamerModel) {
                        throw new IllegalStateException("Steamer did not select the custom baozi model: " + name);
                    }
                }
                for (int y = 0; y < 256; y++) {
                    for (int x = 0; x < 256; x++) {
                        preview.setPixelRGBA(index * 256 + x, y, sprite.getPixelRGBA(x * sprite.getWidth() / 256, y * sprite.getHeight() / 256));
                    }
                }
            }
            preview.writeToFile(java.nio.file.Path.of("pufferfish-dishes-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save cooked dish preview", exception);
        }
    }

    /// Verifies the heavy fish's item sprite, translation, and generated entity texture on each resource load.
    private static void verifyHeavyModel(Minecraft minecraft) {
        var stack = new ItemStack(PufferfishItems.HEAVY.get());
        var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
        if (model == minecraft.getModelManager().getMissingModel()
                || model.getQuads(null, null, RandomSource.create()).isEmpty()
                || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/heavy_pufferfish"))
                || stack.getHoverName().getString().startsWith("item.")
                || !PufferfishTextures.heavyEntity().getNamespace().equals(KaleidoscopePufferfish.MODID)) {
            throw new IllegalStateException("Missing heavy pufferfish model, sprite, texture, or translation");
        }
        try {
            model.getParticleIcon().contents().getOriginalImage().writeToFile(java.nio.file.Path.of("heavy-pufferfish-item-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save heavy fish sprite preview", exception);
        }
    }

    /// Checks the actual generated sprites and entity UVs, including preserved mouths and small-fish eye sockets.
    private static void verifyWhiteEyes(Minecraft minecraft) {
        for (boolean heavy : new boolean[] {false, true}) {
            int color = heavy ? 0xFFE5F0FA : 0xFFE5DED1;
            int expected = (color & 0xFF00FF00) | (color & 255) << 16 | (color >>> 16 & 255);
            var stack = new ItemStack(heavy ? PufferfishItems.HEAVY.get() : PufferfishItems.NETHERITE.get());
            var item = minecraft.getItemRenderer().getModel(stack, null, null, 0).getParticleIcon().contents().getOriginalImage();
            if (item.getPixelRGBA(4, 6) != expected || item.getPixelRGBA(5, 6) != expected
                    || item.getPixelRGBA(9, 5) != expected || item.getPixelRGBA(10, 5) != expected
                    || item.getPixelRGBA(7, 8) != 0xFF262C1D) {
                throw new IllegalStateException("Metal pufferfish item eyes or mouth were incorrect");
            }
            var id = heavy ? PufferfishTextures.heavyEntity() : PufferfishTextures.netheriteEntity();
            var texture = (net.minecraft.client.renderer.texture.DynamicTexture) minecraft.getTextureManager().getTexture(id);
            var entity = texture.getPixels();
            for (int[] eye : new int[][] {{9, 9}, {10, 9}, {13, 9}, {14, 9}, {17, 28}, {21, 28}, {3, 29}, {5, 29}, {25, 7}, {29, 7}}) {
                if (entity.getPixelRGBA(eye[0], eye[1]) != expected) throw new IllegalStateException("Metal pufferfish entity retained a dark pupil");
            }
            if (entity.getPixelRGBA(24, 7) != 0xFF12140D) throw new IllegalStateException("Small pufferfish eye socket was overwritten");
        }
    }

    /// Verifies cutting models across resource reloads and saves an enlarged sashimi sprite for visual inspection.
    private static void verifyCuttingModels(Minecraft minecraft) {
        var stack = new ItemStack(PufferfishItems.SASHIMI.get());
        var model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
        if (model == minecraft.getModelManager().getMissingModel()
                || model.getQuads(null, null, RandomSource.create()).isEmpty()
                || !model.getParticleIcon().contents().name().equals(KaleidoscopePufferfish.id("item/pufferfish_sashimi"))
                || stack.getHoverName().getString().startsWith("item.")) {
            throw new IllegalStateException("Missing pufferfish sashimi model, sprite, or translation");
        }
        var sprite = model.getParticleIcon().contents().getOriginalImage();
        try (var preview = new com.mojang.blaze3d.platform.NativeImage(sprite.getWidth() * 16, sprite.getHeight() * 16, true)) {
            for (int y = 0; y < preview.getHeight(); y++) {
                for (int x = 0; x < preview.getWidth(); x++) {
                    preview.setPixelRGBA(x, y, sprite.getPixelRGBA(x / 16, y / 16));
                }
            }
            preview.writeToFile(java.nio.file.Path.of("pufferfish-sashimi-preview.png"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not save sashimi sprite preview", exception);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("kaleidoscope_cookery")) {
            var names = new java.util.ArrayList<String>();
            names.add("pufferfish_skin");
            for (var color : DyeColor.values()) names.add(PufferfishBlocks.skinName(color));
            names.addAll(java.util.List.of("golden_pufferfish", "netherite_pufferfish_skin", "radiant_pufferfish_skin", "heavy_pufferfish_skin"));
            for (var name : names) {
                for (int stage = 0; stage <= 3; stage++) {
                    var id = KaleidoscopePufferfish.id("chopping_board/" + name + "/" + stage);
                    var cuttingModel = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(id));
                    if (cuttingModel == minecraft.getModelManager().getMissingModel()
                            || cuttingModel.getQuads(null, null, RandomSource.create()).isEmpty()) {
                        throw new IllegalStateException("Missing chopping board model: " + id);
                    }
                    if (stage == 3) {
                        var textures = cuttingModel.getQuads(null, null, RandomSource.create()).stream()
                                .map(quad -> quad.getSprite().contents().name()).toList();
                        var skin = name.equals("golden_pufferfish") ? "golden_pufferfish_skin" : name;
                        if (!textures.contains(KaleidoscopePufferfish.id("item/pufferfish_sashimi"))
                                || !textures.contains(KaleidoscopePufferfish.id("block/" + skin))) {
                            throw new IllegalStateException("Chopping board outputs use incorrect textures: " + id);
                        }
                    }
                }
            }
        }
    }





    /// Checks that each skin's block and item models use the matching skin texture and a thin top face.
    private static void verifySkin(Minecraft minecraft, DyeColor color) {
        verifySkin(minecraft, PufferfishBlocks.skin(color), PufferfishBlocks.skinName(color));
    }

    private static void verifySkin(Minecraft minecraft, PufferfishSkinBlock block, String color) {
        var state = block.defaultBlockState();
        var stack = new ItemStack(block);
        var texture = KaleidoscopePufferfish.id("block/" + color);
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
