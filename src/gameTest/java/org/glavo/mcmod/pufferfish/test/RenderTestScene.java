// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/// Restores the shared render state even when a rendering assertion fails.
public final class RenderTestScene implements AutoCloseable {
    private final Minecraft minecraft;
    private final TextureTarget target;
    private final float[] color;

    public RenderTestScene(Minecraft minecraft, int width, int height, float red, float green, float blue) {
        this.minecraft = minecraft;
        color = RenderSystem.getShaderColor().clone();
        target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        target.setClearColor(red, green, blue, 1);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.getModelViewStack().pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public TextureTarget target() { return target; }

    @Override
    public void close() {
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        target.destroyBuffers();
        minecraft.getMainRenderTarget().bindWrite(true);
    }
}
