# ThreeDViewing — 3D Visual Realism Suite (Grafika Komputer, Minggu 11)

Java 17+ • LWJGL 3.3.3 • JOML • OpenGL 3.3 core (GLSL 330) • Maven (NetBeans)

## Cara membuka di NetBeans
1. `File > Open Project...` → pilih folder `ThreeDViewing` (ikon Maven).
2. Tunggu Maven mengunduh dependency (butuh internet, pertama kali saja).
3. Klik kanan project → **Run** (main class: `com.grafkom.viewing3d.Main`).
   - Pastikan JDK project = 17 atau lebih baru (`Properties > Build > Compile`).
   - macOS: tambahkan VM option `-XstartOnFirstThread` (Properties > Run > VM Options).

## Kontrol
| Tombol | Fungsi (Indikator) |
|---|---|
| B | Phong <-> Blinn-Phong (1) |
| F | Flat <-> Smooth Shading (2) |
| Z | Z-Buffer Depth Test on/off (3) |
| C | Back-Face Culling on/off (3) |
| X | Texture on/off (4) |
| T | Nearest <-> Bilinear + Mipmap (4) |
| S | Shadow Mapping on/off (5) |
| M | Wireframe on/off |
| L / R | Cahaya / objek berputar on/off |
| 1 / 2 / 3 | Sphere / Torus / House |
| Mouse drag, scroll | Orbit & zoom kamera |
| H / ESC | Bantuan / keluar |

Status semua toggle + FPS tampil di judul window.

## Struktur
- `Main`, `VisualRealismApp` — window, input, render loop 2-pass
- `ShaderProgram`, `ResourceLoader` — loader GLSL & resource
- `Mesh`, `MeshData`, `MeshFactory` — mesh Sphere/Torus/House + lantai; buffer FLAT (normal per-face) & SMOOTH (vertex normal averaging)
- `HSRManager` — Z-Buffer & Back-Face Culling (Praktikum 2)
- `Texture2D` — STBImage + mipmap (Praktikum 3)
- `ShadowMapFBO` — Depth FBO 2048x2048 (Praktikum 4)
- `resources/shaders` — `phong.vert/frag` (Blinn-Phong per-pixel + PCF shadow), `depth.vert/frag`
- `resources/textures` — `brick.png` (objek), `tiles.png` (lantai)
