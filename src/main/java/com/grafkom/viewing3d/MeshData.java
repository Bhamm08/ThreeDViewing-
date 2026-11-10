package com.grafkom.viewing3d;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Data mentah mesh: posisi, UV, dan indeks segitiga (CCW = sisi depan). */
public class MeshData {
    final List<Vector3f> positions = new ArrayList<>();
    final List<Vector2f> uvs = new ArrayList<>();
    final List<Integer> indices = new ArrayList<>();

    public int addVertex(float x, float y, float z, float u, float v) {
        positions.add(new Vector3f(x, y, z));
        uvs.add(new Vector2f(u, v));
        return positions.size() - 1;
    }

    /** Segitiga degenerate (luas ~ 0, misal di kutub bola) dibuang. */
    public void addTriangle(int a, int b, int c) {
        Vector3f pa = positions.get(a), pb = positions.get(b), pc = positions.get(c);
        Vector3f n = new Vector3f(pb).sub(pa).cross(new Vector3f(pc).sub(pa));
        if (n.lengthSquared() < 1e-12f) return;
        indices.add(a);
        indices.add(b);
        indices.add(c);
    }

    /** Quad dengan 4 titik berurutan CCW (dilihat dari luar). UV: (0,0) (1,0) (1,1) (0,1). */
    public void addQuad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float uMax, float vMax) {
        int a = addVertex(p0.x, p0.y, p0.z, 0f, 0f);
        int b = addVertex(p1.x, p1.y, p1.z, uMax, 0f);
        int c = addVertex(p2.x, p2.y, p2.z, uMax, vMax);
        int d = addVertex(p3.x, p3.y, p3.z, 0f, vMax);
        addTriangle(a, b, c);
        addTriangle(a, c, d);
    }

    public int triangleCount() {
        return indices.size() / 3;
    }
}
