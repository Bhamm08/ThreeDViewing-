package com.grafkom.viewing3d;

/** Status seluruh fitur Visual Realism yang bisa di-toggle secara real-time. */
public class RenderSettings {
    public boolean useBlinnPhong = true;   // Key B : Phong <-> Blinn-Phong
    public boolean flatShading = false;    // Key F : Flat <-> Smooth
    public boolean depthTest = true;       // Key Z : Z-Buffer
    public boolean backFaceCulling = true; // Key C : Back-Face Culling
    public boolean useTexture = true;      // Key X : Texture on/off
    public boolean mipmapFiltering = true; // Key T : Nearest <-> Bilinear + Mipmap
    public boolean shadows = true;         // Key S : Shadow Mapping
    public boolean wireframe = false;      // Key M : Wireframe
    public boolean animateLight = true;    // Key L : Cahaya berputar
    public boolean autoRotate = true;      // Key R : Objek berputar
}
