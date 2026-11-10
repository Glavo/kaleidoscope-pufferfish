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
        for (var name : org.glavo.kaleidoscope.pufferfish.PufferfishArmor.VARIANTS.keySet()) {
            var id = KaleidoscopePufferfish.id("item/" + name);
            var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
            if (replacement.isPresent()) output.add(id, replacement.get());
            else manager.getResource(ResourceLocation.withDefaultNamespace("textures/item/leather_" + name.substring(name.lastIndexOf('_') + 1) + ".png"))
                    .ifPresent(resource -> output.add(id, loader -> loader.loadSprite(id, resource, (sprite, size, image, metadata) -> {
                        PufferfishArmorTextures.skin(manager, image, name, false);
                        return new SpriteContents(sprite, size, image, metadata);
                    })));
        }
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
        addSashimi(manager, output);
        addDish(manager, output, false);
        addDish(manager, output, true);
        addZosui(manager, output);
        var aspicTexture = KaleidoscopePufferfish.id("block/pufferfish_aspic");
        var aspicReplacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(aspicTexture));
        if (aspicReplacement.isPresent()) output.add(aspicTexture, aspicReplacement.get());
        else output.add(aspicTexture, loader -> PufferfishAspicTexture.create(aspicTexture));
        var pieTexture = KaleidoscopePufferfish.id("block/pufferfish_stargazy_pie");
        var pieReplacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(pieTexture));
        if (pieReplacement.isPresent()) output.add(pieTexture, pieReplacement.get());
        else output.add(pieTexture, loader -> PufferfishPieTexture.create(pieTexture, manager));
        for (boolean raw : new boolean[] {true, false}) {
            var id = KaleidoscopePufferfish.id("item/" + (raw ? "raw_" : "") + "pufferfish_baozi");
            var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
            if (replacement.isPresent()) output.add(id, replacement.get());
            else output.add(id, loader -> PufferfishBaoziTexture.create(id, raw));
        }
        addSkin(manager, output, null);
        for (var skin : PufferfishBlocks.COPPER_SKINS) {
            addSkin(manager, output, skin.getId().getPath(), null);
        }
        addSkin(manager, output, "golden_pufferfish_skin", null);
        addSkin(manager, output, "netherite_pufferfish_skin", null);
        addSkin(manager, output, "heavy_pufferfish_skin", null);
        for (var material : new String[] {"iron", "diamond"}) {
            addSkin(manager, output, material + "_pufferfish_skin", null);
        }
        for (DyeColor color : DyeColor.values()) {
            addSkin(manager, output, color);
        }
        for (var material : new String[] {"exposed_copper", "weathered_copper", "oxidized_copper"}) {
            addItem(manager, output, material + "_pufferfish", 0);
        }
        addItem(manager, output, "copper_pufferfish", 0);
        addItem(manager, output, "netherite_pufferfish", PufferfishTextures.NETHERITE_COLOR);
        addItem(manager, output, "golden_pufferfish", 0);
        addItem(manager, output, "heavy_pufferfish", 0);
        addItem(manager, output, "radiant_pufferfish", 0);
        for (var material : new String[] {"iron", "diamond"}) {
            addItem(manager, output, material + "_pufferfish", 0);
        }
    }

    /// Adds a resource-pack cooked dish sprite or the built-in bowl sprite.
    private static void addDish(ResourceManager manager, Output output, boolean rice) {
        var id = KaleidoscopePufferfish.id("item/" + (rice ? "braised_pufferfish_rice_bowl" : "braised_pufferfish"));
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) output.add(id, replacement.get());
        else output.add(id, loader -> PufferfishDishTexture.create(id, rice));
    }

    /// Adds a resource-pack rice soup sprite or the built-in fish and rice soup sprite.
    private static void addZosui(ResourceManager manager, Output output) {
        var id = KaleidoscopePufferfish.id("item/pufferfish_zosui");
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) output.add(id, replacement.get());
        else output.add(id, loader -> PufferfishDishTexture.createZosui(id));
    }

    /// Adds a resource-pack sashimi sprite or the built-in sliced-fish sprite.
    private static void addSashimi(ResourceManager manager, Output output) {
        var id = KaleidoscopePufferfish.id("item/pufferfish_sashimi");
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) {
            output.add(id, replacement.get());
        } else {
            output.add(id, loader -> PufferfishSashimiTexture.create(id));
        }
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
                        if (itemName.equals("heavy_pufferfish")) HeavyPufferfishVisuals.recolor(image, false);
                        else if (itemName.equals("golden_pufferfish")) GoldenPufferfishTexture.recolor(image);
                        else if (itemName.endsWith("copper_pufferfish") || itemName.equals("iron_pufferfish") || itemName.equals("diamond_pufferfish") || itemName.equals("radiant_pufferfish")) {
                            MaterialPufferfishTexture.recolor(image, itemName.substring(0, itemName.length() - "_pufferfish".length()), false);
                        }
                        else PufferfishTextures.recolor(image, color);
                        if (itemName.equals("netherite_pufferfish")) PufferfishTextures.whiteEyes(image, false, PufferfishTextures.NETHERITE_EYE_COLOR);
                        return new SpriteContents(name, size, image, metadata);
                    })));
        }
    }

    /// Adds a skin sprite from a resource-pack replacement or the entity's unfolded skin.
    private static void addSkin(ResourceManager manager, Output output, DyeColor color) {
        addSkin(manager, output, PufferfishBlocks.skinName(color), color);
    }

    private static void addSkin(ResourceManager manager, Output output, String skinName, DyeColor color) {
        var id = KaleidoscopePufferfish.id("block/" + skinName);
        var replacement = manager.getResource(TEXTURE_ID_CONVERTER.idToFile(id));
        if (replacement.isPresent()) {
            output.add(id, replacement.get());
            return;
        }
        var source = ResourceLocation.withDefaultNamespace("entity/fish/pufferfish");
        manager.getResource(TEXTURE_ID_CONVERTER.idToFile(source)).ifPresent(resource ->
                output.add(id, loader -> loader.loadSprite(id, resource, (name, size, image, metadata) ->
                        PufferfishSkinTexture.create(name, size, image, metadata, color == null ? null : color.getTextureDiffuseColor()))));
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
