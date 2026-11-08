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
        for (DyeColor color : DyeColor.values()) {
            addBlock(manager, output, color);
        }
        var netheriteItem = KaleidoscopePufferfish.id("item/netherite_pufferfish");
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(netheriteItem));
        if (replacement.isPresent()) {
            output.add(netheriteItem, replacement.get());
        } else {
            manager.getResource(TEXTURE_ID_CONVERTER.idToFile(ResourceLocation.withDefaultNamespace("item/pufferfish")))
                    .ifPresent(resource -> output.add(netheriteItem, loader -> loader.loadSprite(netheriteItem, resource, (name, size, image, metadata) -> {
                        PufferfishTextures.recolor(image, PufferfishTextures.NETHERITE_COLOR);
                        return new SpriteContents(name, size, image, metadata);
                    })));
        }
        addBlock(manager, output, "netherite_pufferfish_block", PufferfishTextures.NETHERITE_COLOR);
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

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
