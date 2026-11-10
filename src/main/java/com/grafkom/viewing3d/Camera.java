package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit sederhana (drag mouse = rotasi, scroll = zoom). */
public class Camera {
    private float yaw = 35f;       // derajat
    private float pitch = 25f;     // derajat
    private float distance = 13f;
    private final Vector3f target = new Vector3f(0f, 1.0f, 0f);

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(2f, Math.min(85f, pitch + dPitch));
    }

    public void zoom(float amount) {
        distance = Math.max(4f, Math.min(30f, distance - amount * 0.8f));
    }

    public Vector3f getPosition() {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vector3f(
                target.x + distance * (float) (Math.cos(p) * Math.sin(y)),
                target.y + distance * (float) Math.sin(p),
                target.z + distance * (float) (Math.cos(p) * Math.cos(y)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }
}
