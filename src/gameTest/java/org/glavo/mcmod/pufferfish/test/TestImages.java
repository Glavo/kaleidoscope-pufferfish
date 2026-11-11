// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Screenshot;

/// Optional diagnostics; image contents are validated by the calling tests.
public final class TestImages {
    private TestImages() {}

    public static void capture(RenderTarget target, Path name) throws IOException {
        if (!Boolean.getBoolean("pufferfish_please.testImages")) return;
        try (var image = Screenshot.takeScreenshot(target)) { write(image, name); }
    }

    public static void write(NativeImage image, Path name) throws IOException {
        if (!Boolean.getBoolean("pufferfish_please.testImages")) return;
        var directory = Path.of("test-artifacts");
        Files.createDirectories(directory);
        image.writeToFile(directory.resolve(name.getFileName()));
    }
}
