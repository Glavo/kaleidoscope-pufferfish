// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.DyeColor;
import org.glavo.mcmod.pufferfish.client.PufferfishTextures;

/// Exercises explicit textures, generated fallbacks, and switching between them on reload.
final class PufferfishDyedTextureClientTest {
    private static final String PACK_ID = "pufferfish_dyed_texture_test";
    private static final int PIXEL = 0xFF2A7FCB;

    private PufferfishDyedTextureClientTest() {}

    static void enablePack(Minecraft minecraft) {
        try {
            Path root = Files.createTempDirectory(Path.of("../build"), "dyed-texture-test-");
            Files.writeString(root.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":34,\"description\":\"Dyed pufferfish texture test\"}}");
            writeTexture(root, "item/red_pufferfish.png", 32);
            writeTexture(root, "item/red_pufferfish_bucket.png", 32);
            writeTexture(root, "entity/red_pufferfish.png", 64);
            var repository = minecraft.getResourcePackRepository();
            repository.addPackFinder(output -> {
                var info = new PackLocationInfo(PACK_ID, Component.literal(PACK_ID), PackSource.DEFAULT, Optional.empty());
                var pack = Pack.readMetaAndCreate(info, new PathPackResources.PathResourcesSupplier(root),
                        PackType.CLIENT_RESOURCES, new PackSelectionConfig(false, Pack.Position.TOP, false));
                if (pack == null) throw new IllegalStateException("Could not read dyed texture test pack");
                output.accept(pack);
            });
            repository.reload();
            if (!repository.addPack(PACK_ID)) throw new IllegalStateException("Could not enable dyed texture test pack");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create dyed texture test pack", exception);
        }
    }

    static void disablePack(Minecraft minecraft) {
        if (!minecraft.getResourcePackRepository().removePack(PACK_ID)) {
            throw new IllegalStateException("Could not disable dyed texture test pack");
        }
    }

    static void verify(Minecraft minecraft, boolean replacementExpected) {
        var entityId = PufferfishTextures.entity(DyeColor.RED);
        boolean replacementPresent = minecraft.getResourceManager().getResource(entityId).isPresent();
        if (replacementPresent != replacementExpected) {
            throw new IllegalStateException("Unexpected dyed texture test pack state");
        }
        var entityTexture = minecraft.getTextureManager().getTexture(entityId);
        if ((entityTexture instanceof DynamicTexture) == replacementExpected) {
            throw new IllegalStateException("Dyed entity texture did not switch between PNG and generated fallback");
        }
        if (!(minecraft.getTextureManager().getTexture(PufferfishTextures.entity(DyeColor.BLUE)) instanceof DynamicTexture)) {
            throw new IllegalStateException("Missing generated fallback for an unmodified dye");
        }
        if (replacementExpected) {
            try (var pixels = new NativeImage(64, 64, true)) {
                entityTexture.bind();
                pixels.downloadTexture(0, false);
                verifyPixels(pixels, "entity");
            }
        }
        for (boolean bucket : new boolean[] {false, true}) {
            var id = PufferfishTextures.item(DyeColor.RED, bucket);
            var model = minecraft.getModelManager().getModel(ModelResourceLocation.standalone(id));
            var pixels = model.getParticleIcon().contents().getOriginalImage();
            if (replacementExpected) {
                if (pixels.getWidth() != 32 || pixels.getHeight() != 32) {
                    throw new IllegalStateException("Dyed item replacement resolution was not preserved: " + id);
                }
                verifyPixels(pixels, id.toString());
            } else if (pixels.getPixelRGBA(0, 0) == PIXEL) {
                throw new IllegalStateException("Dyed item replacement survived pack removal: " + id);
            }
        }
    }

    private static void writeTexture(Path root, String name, int size) throws IOException {
        Path path = root.resolve("assets/pufferfish_please/textures/" + name);
        Files.createDirectories(path.getParent());
        try (var image = new NativeImage(size, size, true)) {
            image.fillRect(0, 0, size, size, PIXEL);
            image.writeToFile(path);
        }
    }

    private static void verifyPixels(NativeImage image, String name) {
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            if (image.getPixelRGBA(x, y) != PIXEL) {
                throw new IllegalStateException("Explicit texture was recolored or replaced: " + name);
            }
        }
    }
}
