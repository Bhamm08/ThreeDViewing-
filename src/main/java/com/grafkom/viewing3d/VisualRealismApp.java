package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Grafika Komputer - Minggu 11).
 *
 * Kontrol:
 *   B : Phong <-> Blinn-Phong        F : Flat <-> Smooth Shading
 *   Z : Z-Buffer on/off              C : Back-Face Culling on/off
 *   X : Texture on/off               T : Nearest <-> Bilinear + Mipmap
 *   S : Shadow Mapping on/off        M : Wireframe on/off
 *   L : Cahaya berputar on/off       R : Objek berputar on/off
 *   1/2/3 : Sphere / Torus / House   Mouse drag : orbit kamera, Scroll : zoom
 *   H : tampilkan bantuan            ESC : keluar
 */
public class VisualRealismApp {

    private static final float FLOOR_HALF = 9f;
    private static final float LIGHT_ORTHO = 13f;

    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    private final RenderSettings settings = new RenderSettings();
    private final Camera camera = new Camera();

    private ShaderProgram sceneShader, depthShader;
    private Texture2D objectTexture, floorTexture;
    private ShadowMapFBO shadowFBO;
    private Mesh[] objects;
    private Mesh floor;

    private int meshIndex = 0;
    private float objectAngle = 0f;
    private float lightAngle = 0.8f;
    private final Vector3f lightPos = new Vector3f();

    private boolean dragging = false, firstMouse = true;
    private double lastX, lastY;

    private int frames = 0;
    private double fpsTimer = 0;
    private int fps = 0;

    public void run() {
        init();
        loop();
        cleanup();
    }

    // ------------------------------------------------------------------ init
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // wajib di macOS
        glfwWindowHint(GLFW_SAMPLES, 4);                       // MSAA
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(1280, 720, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Gagal membuat window GLFW (butuh OpenGL 3.3)");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);

        setupCallbacks();
        glfwShowWindow(window);

        sceneShader = new ShaderProgram("shaders/phong.vert", "shaders/phong.frag");
        depthShader = new ShaderProgram("shaders/depth.vert", "shaders/depth.frag");
        objectTexture = new Texture2D("textures/brick.png");
        floorTexture = new Texture2D("textures/tiles.png");
        shadowFBO = new ShadowMapFBO();

        objects = new Mesh[]{MeshFactory.sphere(), MeshFactory.torus(), MeshFactory.house()};
        floor = MeshFactory.ground(FLOOR_HALF, 8f);

        sceneShader.use();
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);

        printHelp();
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(w, true);
                case GLFW_KEY_B -> settings.useBlinnPhong = !settings.useBlinnPhong;
                case GLFW_KEY_F -> settings.flatShading = !settings.flatShading;
                case GLFW_KEY_Z -> settings.depthTest = !settings.depthTest;
                case GLFW_KEY_C -> settings.backFaceCulling = !settings.backFaceCulling;
                case GLFW_KEY_X -> settings.useTexture = !settings.useTexture;
                case GLFW_KEY_T -> {
                    settings.mipmapFiltering = !settings.mipmapFiltering;
                    objectTexture.setMipmapped(settings.mipmapFiltering);
                    floorTexture.setMipmapped(settings.mipmapFiltering);
                }
                case GLFW_KEY_S -> settings.shadows = !settings.shadows;
                case GLFW_KEY_M -> settings.wireframe = !settings.wireframe;
                case GLFW_KEY_L -> settings.animateLight = !settings.animateLight;
                case GLFW_KEY_R -> settings.autoRotate = !settings.autoRotate;
                case GLFW_KEY_1 -> meshIndex = 0;
                case GLFW_KEY_2 -> meshIndex = 1;
                case GLFW_KEY_3 -> meshIndex = 2;
                case GLFW_KEY_H -> printHelp();
                default -> { }
            }
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = (action == GLFW_PRESS);
                firstMouse = true;
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (!dragging) return;
            if (firstMouse) {
                lastX = x;
                lastY = y;
                firstMouse = false;
            }
            camera.rotate(-(float) (x - lastX) * 0.3f, (float) (y - lastY) * 0.3f);
            lastX = x;
            lastY = y;
        });

        glfwSetScrollCallback(window, (w, xoff, yoff) -> camera.zoom((float) yoff));
    }

    // ------------------------------------------------------------------ loop
    private void loop() {
        double previous = glfwGetTime();
        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - previous);
            previous = now;

            update(dt);
            render();

            glfwSwapBuffers(window);
            glfwPollEvents();
            updateTitle(now, dt);
        }
    }

    private void update(float dt) {
        if (settings.autoRotate) objectAngle += dt * 0.6f;
        if (settings.animateLight) lightAngle += dt * 0.5f;
        lightPos.set(6f * (float) Math.cos(lightAngle), 8f, 6f * (float) Math.sin(lightAngle));
    }

    // ---------------------------------------------------------------- render
    private void render() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }
        if (fbWidth == 0 || fbHeight == 0) return; // window di-minimize

        Mesh object = objects[meshIndex];
        Matrix4f objectModel = object.modelMatrix(objectAngle);

        // Matriks ruang cahaya (Directional Light => Ortografik)
        Matrix4f lightProjection = new Matrix4f().ortho(-LIGHT_ORTHO, LIGHT_ORTHO, -LIGHT_ORTHO, LIGHT_ORTHO, 1f, 30f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f lightSpace = new Matrix4f(lightProjection).mul(lightView);

        // ===== PASS 1: Depth Pass dari sudut pandang cahaya =====
        if (settings.shadows) {
            shadowFBO.bindForWriting();
            HSRManager.setupDepthTest(true);          // pass bayangan selalu butuh depth test
            HSRManager.setupBackFaceCulling(false);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            depthShader.setMat4("model", objectModel);
            object.draw(false);
            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ===== PASS 2: Lighting Pass dari sudut pandang kamera =====
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.10f, 0.12f, 0.16f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        HSRManager.setupDepthTest(settings.depthTest);
        HSRManager.setupBackFaceCulling(settings.backFaceCulling);
        glPolygonMode(GL_FRONT_AND_BACK, settings.wireframe ? GL_LINE : GL_FILL);

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0),
                (float) fbWidth / fbHeight, 0.1f, 100f);

        sceneShader.use();
        sceneShader.setMat4("view", camera.getViewMatrix());
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("lightColor", 1.0f, 0.97f, 0.90f);
        sceneShader.setBool("useBlinnPhong", settings.useBlinnPhong);
        sceneShader.setBool("useShadows", settings.shadows);
        sceneShader.setBool("unlit", false);
        shadowFBO.bindDepthTexture(1);

        // Lantai
        sceneShader.setMat4("model", new Matrix4f());
        sceneShader.setBool("useTexture", settings.useTexture);
        sceneShader.setVec3("objectColor", 0.55f, 0.60f, 0.68f);
        sceneShader.setFloat("specularStrength", 0.15f);
        floorTexture.bind(0);
        floor.draw(false);

        // Objek utama
        sceneShader.setMat4("model", objectModel);
        sceneShader.setVec3("objectColor", 0.85f, 0.45f, 0.30f);
        sceneShader.setFloat("specularStrength", 0.8f);
        objectTexture.bind(0);
        object.draw(settings.flatShading);

        // Penanda posisi cahaya (bola kecil tanpa pencahayaan)
        sceneShader.setBool("unlit", true);
        sceneShader.setVec3("objectColor", 1.0f, 0.95f, 0.55f);
        sceneShader.setMat4("model", new Matrix4f().translate(lightPos).scale(0.3f));
        objects[0].draw(false);
        sceneShader.setBool("unlit", false);

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    // ------------------------------------------------------------------- HUD
    private static String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    private void updateTitle(double now, float dt) {
        frames++;
        if (now - fpsTimer >= 0.5) {
            fps = (int) Math.round(frames / (now - fpsTimer));
            frames = 0;
            fpsTimer = now;
            String title = "3D Visual Realism Suite | " + objects[meshIndex].name
                    + " | [B] " + (settings.useBlinnPhong ? "Blinn-Phong" : "Phong")
                    + " | [F] " + (settings.flatShading ? "Flat" : "Smooth")
                    + " | [Z] ZBuf " + onOff(settings.depthTest)
                    + " | [C] Cull " + onOff(settings.backFaceCulling)
                    + " | [X] Tex " + onOff(settings.useTexture)
                    + " | [T] " + (settings.mipmapFiltering ? "Mipmap" : "Nearest")
                    + " | [S] Shadow " + onOff(settings.shadows)
                    + " | [M] Wire " + onOff(settings.wireframe)
                    + " | FPS " + fps;
            glfwSetWindowTitle(window, title);
        }
    }

    private static void printHelp() {
        System.out.println("""
                ===== 3D Visual Realism Suite - Kontrol =====
                 B : Phong <-> Blinn-Phong        F : Flat <-> Smooth Shading
                 Z : Z-Buffer on/off              C : Back-Face Culling on/off
                 X : Texture on/off               T : Nearest <-> Bilinear+Mipmap
                 S : Shadow Mapping on/off        M : Wireframe on/off
                 L : Cahaya berputar on/off       R : Objek berputar on/off
                 1/2/3 : Sphere / Torus / House
                 Mouse drag : orbit kamera        Scroll : zoom
                 H : bantuan                      ESC : keluar
                =============================================""");
    }

    // --------------------------------------------------------------- cleanup
    private void cleanup() {
        for (Mesh m : objects) m.destroy();
        floor.destroy();
        objectTexture.destroy();
        floorTexture.destroy();
        shadowFBO.destroy();
        sceneShader.destroy();
        depthShader.destroy();

        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
