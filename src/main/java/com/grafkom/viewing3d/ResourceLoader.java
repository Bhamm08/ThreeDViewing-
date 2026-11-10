package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Utilitas membaca file dari classpath (src/main/resources). */
public final class ResourceLoader {

    private ResourceLoader() {}

    private static InputStream open(String path) {
        InputStream in = ResourceLoader.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalArgumentException("Resource tidak ditemukan: " + path);
        }
        return in;
    }

    public static String readText(String path) {
        try (InputStream in = open(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Hasil harus dibebaskan dengan MemoryUtil.memFree(). */
    public static ByteBuffer readBinary(String path) {
        try (InputStream in = open(path)) {
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
