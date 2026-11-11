// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/// Supplies fish dishes and rice soup in brown bowls with food highlights and green garnish.
public final class PufferfishDishTexture {
    /// Pixel rows for overlapping fish slices in a wide, shallow serving bowl.
    private static final String[] DISH = {
            "................", "................", "......aaaa......", "....abdddda.....",
            "..abfhhfbdgga...", "..bfhhffbggcba..", ".abffddfhhffbba.", "obddbbfhhffdbbqp",
            "oqbbddffddbqqqrp", "omqqbdddqqrrqsp.", ".omrrqqqrrrsssp.", "..oommmsssspp...",
            "....oooopppp....", "................", "................", "................"
    };
    /// Pixel rows for a taller rice bowl with individual rice highlights beneath glazed fish slices.
    private static final String[] RICE = {
            "................", "......bb........", ".....bccaa......", "....bfhgcfa.....",
            "....bfhfhhfa....", "...bfhftffdda...", "...bddwttwfva...", "..obbttwtqqbbo..",
            "..ommbutbuuoop..", "..ommmmoooossp..", "...osmmssmmsp...", "....oosssspp....",
            "......pppp......", "................", "................", "................"
    };

    /// Pixel rows for pale fish broth, rice grains, sliced fish, and lettuce in a shallow bowl.
    private static final String[] ZOSUI = {
            "................", "................", "................", "................",
            "....oooooooo....", "..ootwkwtgcwoo..", ".ootwwhhtwctkso.", ".omkkhhwwtwwksp.",
            ".omsskwtwkkoosp.", "..ommskkooossp..", "..osmmssmmsssp..", "...oosssssppp...",
            ".....pppppp.....", "................", "................", "................"
    };

    /// Prevents construction of this sprite factory.
    private PufferfishDishTexture() {}

    /// Creates a 16-by-16 dish sprite, optionally with rice; the returned contents own the image.
    public static SpriteContents create(ResourceLocation id, boolean rice) {
        return create(id, rice ? RICE : DISH);
    }

    /// Creates a 16-by-16 rice soup sprite whose image is owned by the returned contents.
    public static SpriteContents createZosui(ResourceLocation id) {
        return create(id, ZOSUI);
    }

    /// Converts the supplied sixteen pixel rows to a sprite using the shared food and bowl palette.
    private static SpriteContents create(ResourceLocation id, String[] rows) {
        var image = new NativeImage(16, 16, true);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                int rgb = switch (rows[y].charAt(x)) {
                    case 'a' -> 0x632A1D;
                    case 'b' -> 0x782B1A;
                    case 'd' -> 0xA34D2F;
                    case 'f' -> 0xC26E47;
                    case 'h' -> 0xF3C49A;
                    case 'g' -> 0xB8CC6B;
                    case 'c' -> 0x778C37;
                    case 'o' -> 0x3E2E0F;
                    case 'p' -> 0x261D0C;
                    case 'q' -> 0xC8472A;
                    case 'r' -> 0xE5733A;
                    case 'm' -> 0x8F6B26;
                    case 's' -> 0x715218;
                    case 'w' -> 0xFFFEFD;
                    case 't' -> 0xE8E3D8;
                    case 'u' -> 0xD2C6B4;
                    case 'v' -> 0xB7A98F;
                    case 'k' -> 0xEBCD99;
                    default -> -1;
                };
                if (rgb >= 0) image.setPixelRGBA(x, y, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >>> 16);
            }
        }
        return new SpriteContents(id, new FrameSize(16, 16), image, ResourceMetadata.EMPTY);
    }
}
