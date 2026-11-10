package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/** Praktikum 3: memuat PNG/JPG (STBImage) dari classpath ke GPU sebagai tekstur 2D + mipmap. */
public class Texture2D {
    private final int textureID;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        ByteBuffer fileData = ResourceLoader.readBinary(resourcePath);
        try (MemoryStack stack = stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            stbi_set_flip_vertically_on_load(true); // origin UV (0,0) = kiri-bawah
            ByteBuffer pixels = stbi_load_from_memory(fileData, width, height, channels, 4);
            if (pixels == null) {
                throw new IllegalStateException("Failed to load texture: " + resourcePath
                        + " (" + stbi_failure_reason() + ")");
            }
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(pixels);
        } finally {
            MemoryUtil.memFree(fileData);
        }
        setMipmapped(true);
    }

    /** true = Bilinear + Mipmap (GL_LINEAR_MIPMAP_LINEAR), false = Nearest Neighbor. */
    public void setMipmapped(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void destroy() {
        glDeleteTextures(textureID);
    }
}
