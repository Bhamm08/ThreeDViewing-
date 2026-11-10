package com.grafkom.viewing3d;

import org.joml.Vector3f;

/** Pembuat mesh prosedural: Sphere, Torus, Rumah 3D, dan bidang lantai (ground plane). */
public final class MeshFactory {

    private MeshFactory() {}

    /** UV Sphere radius 1. Resolusi sengaja sedang agar efek Flat Shading terlihat. */
    public static Mesh sphere() {
        int stacks = 20, slices = 32;
        MeshData d = new MeshData();
        int[][] grid = new int[stacks + 1][slices + 1];
        for (int i = 0; i <= stacks; i++) {
            double theta = Math.PI * i / stacks;          // 0 (kutub atas) .. PI (kutub bawah)
            for (int j = 0; j <= slices; j++) {
                double phi = 2.0 * Math.PI * j / slices;
                float x = (float) (Math.sin(theta) * Math.cos(phi));
                float y = (float) Math.cos(theta);
                float z = (float) (Math.sin(theta) * Math.sin(phi));
                grid[i][j] = d.addVertex(x, y, z, (float) j / slices * 2f, 1f - (float) i / stacks);
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int a = grid[i][j], b = grid[i + 1][j], c = grid[i + 1][j + 1], e = grid[i][j + 1];
                d.addTriangle(a, e, c);   // CCW dilihat dari luar
                d.addTriangle(a, c, b);
            }
        }
        return new Mesh("Sphere", d, 1.2f, 0f, 1.0f);
    }

    /** Torus: R = 1.0 (lingkar besar), r = 0.4 (tabung). */
    public static Mesh torus() {
        int rings = 32, sides = 20;
        float R = 1.0f, r = 0.4f;
        MeshData d = new MeshData();
        int[][] grid = new int[rings + 1][sides + 1];
        for (int i = 0; i <= rings; i++) {
            double u = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double v = 2.0 * Math.PI * j / sides;
                float x = (float) ((R + r * Math.cos(v)) * Math.cos(u));
                float y = (float) (r * Math.sin(v));
                float z = (float) ((R + r * Math.cos(v)) * Math.sin(u));
                grid[i][j] = d.addVertex(x, y, z, (float) i / rings * 4f, (float) j / sides);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = grid[i][j], b = grid[i + 1][j], c = grid[i + 1][j + 1], e = grid[i][j + 1];
                d.addTriangle(a, e, c);
                d.addTriangle(a, c, b);
            }
        }
        return new Mesh("Torus", d, 1.55f, 65f, 1.0f);
    }

    /** Rumah sederhana (badan kotak + atap pelana). Sudut tajam -> kontras Flat vs Smooth sangat jelas. */
    public static Mesh house() {
        MeshData d = new MeshData();
        float w = 1f, h = 1.2f, z0 = 0.8f;   // setengah lebar, tinggi dinding, setengah kedalaman
        float rz = 1.0f, ridge = 2.0f;       // setengah lebar atap (overhang), tinggi puncak

        // Dinding (CCW dari luar)
        d.addQuad(new Vector3f(-w, 0, z0), new Vector3f(w, 0, z0), new Vector3f(w, h, z0), new Vector3f(-w, h, z0), 2f, 1f);       // depan +Z
        d.addQuad(new Vector3f(w, 0, -z0), new Vector3f(-w, 0, -z0), new Vector3f(-w, h, -z0), new Vector3f(w, h, -z0), 2f, 1f);   // belakang -Z
        d.addQuad(new Vector3f(w, 0, z0), new Vector3f(w, 0, -z0), new Vector3f(w, h, -z0), new Vector3f(w, h, z0), 2f, 1f);       // kanan +X
        d.addQuad(new Vector3f(-w, 0, -z0), new Vector3f(-w, 0, z0), new Vector3f(-w, h, z0), new Vector3f(-w, h, -z0), 2f, 1f);   // kiri -X

        // Atap
        d.addQuad(new Vector3f(-w, h, rz), new Vector3f(w, h, rz), new Vector3f(w, ridge, 0), new Vector3f(-w, ridge, 0), 2f, 1f);     // lereng depan
        d.addQuad(new Vector3f(w, h, -rz), new Vector3f(-w, h, -rz), new Vector3f(-w, ridge, 0), new Vector3f(w, ridge, 0), 2f, 1f);   // lereng belakang

        // Segitiga gable kiri/kanan
        int a = d.addVertex(w, h, rz, 0f, 0f), b = d.addVertex(w, h, -rz, 2f, 0f), c = d.addVertex(w, ridge, 0, 1f, 1f);
        d.addTriangle(a, b, c);
        a = d.addVertex(-w, h, -rz, 0f, 0f);
        b = d.addVertex(-w, h, rz, 2f, 0f);
        c = d.addVertex(-w, ridge, 0, 1f, 1f);
        d.addTriangle(a, b, c);

        return new Mesh("House", d, 0f, 0f, 1.5f);
    }

    /** Bidang lantai (y = 0), normal +Y, UV diulang repeat x repeat kali. */
    public static Mesh ground(float half, float repeat) {
        MeshData d = new MeshData();
        d.addQuad(new Vector3f(-half, 0, half), new Vector3f(half, 0, half),
                new Vector3f(half, 0, -half), new Vector3f(-half, 0, -half), repeat, repeat);
        return new Mesh("Ground", d, 0f, 0f, 1f);
    }
}
