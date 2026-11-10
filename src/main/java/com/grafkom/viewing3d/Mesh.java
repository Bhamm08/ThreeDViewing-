package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Mesh 3D dengan DUA representasi buffer di GPU:
 *  - SMOOTH : indexed, normal tiap vertex = rata-rata normal semua face yang berbagi vertex
 *             (Vertex Normal Averaging) -> Smooth Shading.
 *  - FLAT   : non-indexed, normal tiap segitiga = normal face -> Flat Shading.
 * Layout vertex (stride 8 float): pos(3) | normal(3) | uv(2).
 */
public class Mesh {
    public final String name;
    public final float yOffset;   // tinggi pusat objek dari lantai
    public final float tiltDeg;   // kemiringan awal (derajat, sumbu X)
    public final float scale;

    private final int smoothVao, smoothVbo, smoothEbo;
    private final int flatVao, flatVbo;
    private final int smoothIndexCount;
    private final int flatVertexCount;
    public final int triangleCount;

    public Mesh(String name, MeshData data, float yOffset, float tiltDeg, float scale) {
        this.name = name;
        this.yOffset = yOffset;
        this.tiltDeg = tiltDeg;
        this.scale = scale;
        this.triangleCount = data.triangleCount();

        // ---------- SMOOTH: vertex normal averaging ----------
        int vCount = data.positions.size();
        Map<String, Vector3f> accum = new HashMap<>();
        for (int i = 0; i < data.indices.size(); i += 3) {
            int ia = data.indices.get(i), ib = data.indices.get(i + 1), ic = data.indices.get(i + 2);
            Vector3f pa = data.positions.get(ia), pb = data.positions.get(ib), pc = data.positions.get(ic);
            // cross product tak ternormalisasi => bobot proporsional luas face
            Vector3f fn = new Vector3f(pb).sub(pa).cross(new Vector3f(pc).sub(pa));
            accum.computeIfAbsent(key(pa), k -> new Vector3f()).add(fn);
            accum.computeIfAbsent(key(pb), k -> new Vector3f()).add(fn);
            accum.computeIfAbsent(key(pc), k -> new Vector3f()).add(fn);
        }
        accum.values().forEach(v -> {
            if (v.lengthSquared() > 1e-12f) v.normalize();
        });

        FloatBuffer smoothBuf = memAllocFloat(vCount * 8);
        for (int i = 0; i < vCount; i++) {
            Vector3f p = data.positions.get(i);
            Vector3f n = accum.getOrDefault(key(p), new Vector3f(0, 1, 0));
            smoothBuf.put(p.x).put(p.y).put(p.z).put(n.x).put(n.y).put(n.z)
                    .put(data.uvs.get(i).x).put(data.uvs.get(i).y);
        }
        smoothBuf.flip();
        IntBuffer idxBuf = memAllocInt(data.indices.size());
        for (int idx : data.indices) idxBuf.put(idx);
        idxBuf.flip();
        smoothIndexCount = data.indices.size();

        smoothVao = glGenVertexArrays();
        smoothVbo = glGenBuffers();
        smoothEbo = glGenBuffers();
        glBindVertexArray(smoothVao);
        glBindBuffer(GL_ARRAY_BUFFER, smoothVbo);
        glBufferData(GL_ARRAY_BUFFER, smoothBuf, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, smoothEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf, GL_STATIC_DRAW);
        setupAttributes();
        glBindVertexArray(0);
        memFree(smoothBuf);
        memFree(idxBuf);

        // ---------- FLAT: satu normal per face ----------
        flatVertexCount = data.indices.size();
        FloatBuffer flatBuf = memAllocFloat(flatVertexCount * 8);
        for (int i = 0; i < data.indices.size(); i += 3) {
            int[] ids = {data.indices.get(i), data.indices.get(i + 1), data.indices.get(i + 2)};
            Vector3f pa = data.positions.get(ids[0]), pb = data.positions.get(ids[1]), pc = data.positions.get(ids[2]);
            Vector3f fn = new Vector3f(pb).sub(pa).cross(new Vector3f(pc).sub(pa)).normalize();
            for (int id : ids) {
                Vector3f p = data.positions.get(id);
                flatBuf.put(p.x).put(p.y).put(p.z).put(fn.x).put(fn.y).put(fn.z)
                        .put(data.uvs.get(id).x).put(data.uvs.get(id).y);
            }
        }
        flatBuf.flip();
        flatVao = glGenVertexArrays();
        flatVbo = glGenBuffers();
        glBindVertexArray(flatVao);
        glBindBuffer(GL_ARRAY_BUFFER, flatVbo);
        glBufferData(GL_ARRAY_BUFFER, flatBuf, GL_STATIC_DRAW);
        setupAttributes();
        glBindVertexArray(0);
        memFree(flatBuf);
    }

    /** Kunci posisi (dibulatkan) agar vertex kembar (seam UV / sisi kubus) ikut dirata-ratakan. */
    private static String key(Vector3f p) {
        return Math.round(p.x * 10000f) + "," + Math.round(p.y * 10000f) + "," + Math.round(p.z * 10000f);
    }

    private static void setupAttributes() {
        int stride = 8 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
    }

    public Matrix4f modelMatrix(float angleRad) {
        return new Matrix4f()
                .translate(0f, yOffset, 0f)
                .rotateY(angleRad)
                .rotateX((float) Math.toRadians(tiltDeg))
                .scale(scale);
    }

    public void draw(boolean flat) {
        if (flat) {
            glBindVertexArray(flatVao);
            glDrawArrays(GL_TRIANGLES, 0, flatVertexCount);
        } else {
            glBindVertexArray(smoothVao);
            glDrawElements(GL_TRIANGLES, smoothIndexCount, GL_UNSIGNED_INT, 0L);
        }
        glBindVertexArray(0);
    }

    public void destroy() {
        glDeleteVertexArrays(smoothVao);
        glDeleteVertexArrays(flatVao);
        glDeleteBuffers(smoothVbo);
        glDeleteBuffers(smoothEbo);
        glDeleteBuffers(flatVbo);
    }
}
