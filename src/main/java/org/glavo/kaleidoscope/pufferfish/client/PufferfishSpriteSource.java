// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;
import org.glavo.kaleidoscope.pufferfish.KaleidoscopePufferfish;
import org.glavo.kaleidoscope.pufferfish.PufferfishBlocks;

public final class PufferfishSpriteSource implements SpriteSource {
    public static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(PufferfishSpriteSource::new));

    @Override
    public void run(ResourceManager manager, Output output) {
        for (boolean bucket : new boolean[] {false, true}) {
            ResourceLocation original = ResourceLocation.withDefaultNamespace(bucket ? "item/pufferfish_bucket" : "item/pufferfish");
            manager.getResource(TEXTURE_ID_CONVERTER.idToFile(original)).ifPresent(resource -> {
                for (DyeColor color : DyeColor.values()) {
                    ResourceLocation id = PufferfishTextures.item(color, bucket);
                    output.add(id, loader -> loader.loadSprite(id, resource, (name, size, image, metadata) -> {
                        PufferfishTextures.recolor(image, color);
                        return new SpriteContents(name, size, image, metadata);
                    }));
                }
            });
        }
        addBlock(manager, output, null);
        addCarpet(manager, output, null);
        for (DyeColor color : DyeColor.values()) {
            addBlock(manager, output, color);
            addCarpet(manager, output, color);
        }
        addItem(manager, output, "netherite_pufferfish", PufferfishTextures.NETHERITE_COLOR);
        addItem(manager, output, "golden_pufferfish", PufferfishTextures.GOLDEN_COLOR);
        addBlock(manager, output, "netherite_pufferfish_block", PufferfishTextures.NETHERITE_COLOR);
    }

    /// Adds an item sprite from a resource-pack replacement or a recolored vanilla pufferfish.
    private static void addItem(ResourceManager manager, Output output, String itemName, int color) {
        var id = KaleidoscopePufferfish.id("item/" + itemName);
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) {
            output.add(id, replacement.get());
        } else {
            manager.getResource(TEXTURE_ID_CONVERTER.idToFile(ResourceLocation.withDefaultNamespace("item/pufferfish")))
                    .ifPresent(resource -> output.add(id, loader -> loader.loadSprite(id, resource, (name, size, image, metadata) -> {
                        PufferfishTextures.recolor(image, color);
                        return new SpriteContents(name, size, image, metadata);
                    })));
        }
    }

    private static void addBlock(ResourceManager manager, Output output, DyeColor color) {
        addBlock(manager, output, PufferfishBlocks.name(color), color == null ? null : color.getTextureDiffuseColor());
    }

    private static void addBlock(ResourceManager manager, Output output, String blockName, Integer color) {
        var id = KaleidoscopePufferfish.id("block/" + blockName);
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) {
            output.add(id, replacement.get());
            return;
        }
        var source = ResourceLocation.withDefaultNamespace("item/pufferfish");
        manager.getResource(TEXTURE_ID_CONVERTER.idToFile(source)).ifPresent(resource ->
                output.add(id, loader -> loader.loadSprite(id, resource, (name, size, image, metadata) ->
                        PufferfishBlockTexture.create(name, size, image, metadata, color))));
    }

    /// Adds a carpet sprite from a resource-pack replacement or the entity's unfolded skin.
    private static void addCarpet(ResourceManager manager, Output output, DyeColor color) {
        var id = KaleidoscopePufferfish.id("block/" + PufferfishBlocks.carpetName(color));
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) {
            output.add(id, replacement.get());
            return;
        }
        var source = ResourceLocation.withDefaultNamespace("entity/fish/pufferfish");
        manager.getResource(TEXTURE_ID_CONVERTER.idToFile(source)).ifPresent(resource ->
                output.add(id, loader -> loader.loadSprite(id, resource, (name, size, image, metadata) ->
                        PufferfishCarpetTexture.create(name, size, image, metadata, color == null ? null : color.getTextureDiffuseColor()))));
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
