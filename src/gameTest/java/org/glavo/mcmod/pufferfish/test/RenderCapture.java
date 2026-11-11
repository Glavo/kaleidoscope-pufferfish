// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.mcmod.pufferfish.test;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/// Counts emitted geometry without allocating a framebuffer or depending on artwork pixels.
public final class RenderCapture implements MultiBufferSource {
    private final Map<RenderType, Vertices> buffers = new HashMap<>();

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return buffers.computeIfAbsent(type, ignored -> new Vertices());
    }

    public int count() { return buffers.values().stream().mapToInt(vertices -> vertices.count).sum(); }

    public int count(RenderType type) {
        var vertices = buffers.get(type);
        return vertices == null ? 0 : vertices.count;
    }

    private static final class Vertices implements VertexConsumer {
        private int count;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalStateException("Renderer emitted a non-finite vertex");
            }
            count++;
            return this;
        }

        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
