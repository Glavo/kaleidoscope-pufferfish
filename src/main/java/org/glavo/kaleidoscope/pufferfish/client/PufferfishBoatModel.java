// Copyright (c) 2026 Glavo
// SPDX-License-Identifier: MPL-2.0

package org.glavo.kaleidoscope.pufferfish.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.WaterPatchModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.vehicle.Boat;
import org.glavo.kaleidoscope.pufferfish.entity.PufferfishBoat;
import org.joml.Vector3f;

/// A decked tandem skin kayak with a faceted hull and a double-bladed paddle.
public final class PufferfishBoatModel extends ListModel<Boat> implements WaterPatchModel {
    private record Face(Vector3f[] points, Vector3f normal, int material) {}
    private final List<Face> faces = new ArrayList<>();
    private final ModelPart paddle;
    private final ModelPart water;

    public PufferfishBoatModel() {
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("paddle", CubeListBuilder.create().texOffs(80, 0)
                .addBox(-0.45F, -0.45F, -15, 0.9F, 0.9F, 30)
                .texOffs(80, 36).addBox(-1.8F, -0.4F, -22, 3.6F, 0.8F, 8)
                .addBox(-1.8F, -0.4F, 14, 3.6F, 0.8F, 8), PartPose.ZERO);
        root.addOrReplaceChild("water", CubeListBuilder.create().addBox(-15, -3, -5, 25, 0, 10), PartPose.ZERO);
        var baked = LayerDefinition.create(mesh, 160, 64).bakeRoot();
        paddle = baked.getChild("paddle");
        water = baked.getChild("water");
        float[][] sections = {{-26, 0.15F, -2, 0}, {-21, 2.5F, -2.5F, 2}, {-15, 6, -3, 4},
                {10, 6, -3, 4}, {16, 5.5F, -3, 3}, {22, 2, -2, 1}, {26, 0.15F, -1, 0}};
        for (int i = 0; i < sections.length - 1; i++) {
            var a = sections[i];
            var b = sections[i + 1];
            for (int side : new int[] {-1, 1}) {
                face(0, v(a[0], a[2], side * a[1]), v(b[0], b[2], side * b[1]),
                        v(b[0], b[3] - 1, side * b[1] * 0.85F), v(a[0], a[3] - 1, side * a[1] * 0.85F));
                face(1, v(a[0], a[3] - 1, side * a[1] * 0.85F), v(b[0], b[3] - 1, side * b[1] * 0.85F),
                        v(b[0], b[3], 0), v(a[0], a[3], 0));
                if (a[0] == -15 && b[0] == 10) {
                    face(0, v(a[0], a[2], side * a[1]), v(b[0], b[2], side * b[1]),
                            v(b[0], -4, side * 5), v(a[0], -4, side * 5));
                } else {
                    face(0, v(a[0], a[2], side * a[1]), v(b[0], b[2], side * b[1]),
                            v(b[0], b[2] - 0.8F, 0), v(a[0], a[2] - 0.8F, 0));
                }
            }
        }
        box(-15, 2, -5, 25, 0.6F, 10, 4);
        for (int side : new int[] {-1, 1}) {
            box(-15.4F, -4.4F, side * 5 - 0.35F, 25.8F, 0.8F, 0.7F, 3);
            face(2, v(-15, -4, side * 5), v(10, -4, side * 5), v(10, 2, side * 5), v(-15, 2, side * 5));
        }
        box(-15.4F, -4.4F, -5, 0.8F, 0.8F, 10, 3);
        box(9.6F, -4.6F, -5, 0.8F, 0.8F, 10, 3);
        for (float seat : new float[] {PufferfishBoat.REAR_SEAT * 16, PufferfishBoat.FRONT_SEAT * 16}) {
            box(seat - 2, 0.8F, -4.4F, 4, 1, 8.8F, 2);
            box(seat - 2.5F, -4.5F, -4.4F, 0.8F, 5.3F, 8.8F, 2);
        }
        // Short deck lashings stay readable without obscuring the skin pattern.
        for (float x : new float[] {-18, 17}) box(x, -3.5F, -2.7F, 0.35F, 0.35F, 5.4F, 3);
        restPaddle();
    }

    private static Vector3f v(float x, float y, float z) { return new Vector3f(x, y, z); }

    private void face(int material, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        var normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
        var center = new Vector3f(a).add(b).add(c).add(d).mul(0.25F);
        if (material <= 1 && normal.dot(new Vector3f(0, center.y, center.z)) < 0) normal.negate();
        faces.add(new Face(new Vector3f[] {a, b, c, d}, normal, material));
    }

    private void box(float x, float y, float z, float dx, float dy, float dz, int material) {
        face(material, v(x,y,z), v(x+dx,y,z), v(x+dx,y,z+dz), v(x,y,z+dz));
        face(material, v(x,y+dy,z+dz), v(x+dx,y+dy,z+dz), v(x+dx,y+dy,z), v(x,y+dy,z));
        face(material, v(x,y,z+dz), v(x+dx,y,z+dz), v(x+dx,y+dy,z+dz), v(x,y+dy,z+dz));
        face(material, v(x+dx,y,z), v(x,y,z), v(x,y+dy,z), v(x+dx,y+dy,z));
        face(material, v(x,y,z), v(x,y,z+dz), v(x,y+dy,z+dz), v(x,y+dy,z));
        face(material, v(x+dx,y,z+dz), v(x+dx,y,z), v(x+dx,y+dy,z), v(x+dx,y+dy,z+dz));
    }

    public void restPaddle() {
        paddle.setPos(0, -5.4F, 4.5F);
        paddle.xRot = 0;
        paddle.yRot = (float) Math.PI / 2;
        paddle.zRot = 0;
    }

    @Override
    public void setupAnim(Boat boat, float partialTick, float amount, float age, float yaw, float pitch) {
        restPaddle();
        if (boat.getPaddleState(0) || boat.getPaddleState(1)) {
            var motion = PufferfishRowingAnimation.paddle(boat, partialTick);
            paddle.setPos(motion.x(), motion.y(), motion.z());
            paddle.xRot = motion.pitch();
            paddle.yRot = motion.yaw();
        }
    }

    @Override
    public Iterable<ModelPart> parts() { return List.of(paddle); }

    @Override
    public ModelPart waterPatch() { return water; }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (var face : faces) {
            float u = (face.material == 4 ? 1 : face.material) * 16F;
            float t = face.material == 0 ? 0 : 16;
            for (int i = 0; i < 4; i++) {
                var p = face.points[i];
                buffer.addVertex(pose.last(), p.x / 16, p.y / 16, p.z / 16).setColor(color)
                        .setUv((u + (i == 1 || i == 2 ? 15 : 0)) / 160, (t + (i >= 2 ? 15 : 0)) / 64)
                        .setOverlay(overlay).setLight(light).setNormal(pose.last(), face.normal.x, face.normal.y, face.normal.z);
            }
        }
        paddle.render(pose, buffer, light, overlay, color);
    }
}
