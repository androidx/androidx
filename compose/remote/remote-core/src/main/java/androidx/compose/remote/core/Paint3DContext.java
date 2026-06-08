/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.utilities.d3.MeshData;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Optional 3D extension surface for {@link PaintContext}.
 *
 * <p>A renderer signals 3D capability by having its concrete {@link PaintContext} also
 * implement this interface. 3D operations cast at runtime:
 *
 * <pre>{@code
 *   if (paintContext instanceof Paint3DContext p3d) {
 *       p3d.drawMesh3D(meshId, Paint3DContext.MODE_SOFTWARE_SMOOTH);
 *   }
 * }</pre>
 *
 * <p>A renderer without 3D support doesn't implement this interface, and the 3D operations then
 * draw nothing. The 3D operations are registered only in the experimental API level 7 profiles.
 * The normative description of the format is {@code remote-core/doc/PROTOCOL_3D.md}.
 *
 * <h3>Conventions (OpenGL floor)</h3>
 *
 * <ul>
 *   <li>Right-handed coordinates, +Y up, −Z forward.</li>
 *   <li>4×4 column-major {@code float[16]}. Final clip = {@code projection × view × model}.</li>
 *   <li>Matrix ops post-multiply the current modelview ({@code glTranslate} semantics).</li>
 *   <li>CCW winding = front-facing ({@link #MODE_FRONT_CW} makes clockwise front-facing).</li>
 *   <li>{@code perspective(fovYRadians, aspect, near, far)}, {@code ortho(l,r,b,t,n,f)},
 *       {@code lookAt(eye, center, up)}.</li>
 *   <li>NDC z ∈ [−1, +1]; window-z ∈ [0, 1], smaller = closer.</li>
 *   <li>Angles in radians.</li>
 *   <li>The 2D {@link PaintContext#matrixSave()} / {@link PaintContext#matrixRestore()} do not
 *       touch any 3D state. The model matrix is saved and restored with the sub-ops
 *       {@link #M3_PUSH} / {@link #M3_POP} of {@link #matrix3DOp(int, float[])}; projection and
 *       view are not stacked.</li>
 * </ul>
 *
 * <h3>Color and shading</h3>
 *
 * <p>The current {@link PaintContext} paint color is the mesh base color. Every engine uses one
 * lighting model (see {@link #setLights3D(int[], int[], float[])}): diffuse is the sum over lights
 * of {@code max(0, N'·L)} times the light's color and intensity, where {@code N'} is the surface
 * normal (negated on the back faces of a {@link #MODE_DOUBLE_SIDED} mesh) and {@code L} points
 * from the surface toward the light (a directional light's {@code dir} is the direction the
 * light travels, so {@code L = -dir}); a Blinn-Phong specular term is added only when the
 * material's specular strength is above 0 ({@link #setMaterial3D(float, float)}), and ambient
 * light is added on top.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Paint3DContext {

    /** Projection mode constants for {@link #setCamera3D(int, float[], float[])}. */
    int PROJECTION_PERSPECTIVE = 0;
    int PROJECTION_ORTHO = 1;

    /** Sub-op constants for {@link #matrix3DOp(int, float[])}. */
    int M3_IDENTITY = 0;
    int M3_TRANSLATE = 1;
    int M3_SCALE = 2;
    int M3_ROTATE_AXIS = 3;
    int M3_MULTIPLY = 4;

    /**
     * Sub-op: save a copy of the current model matrix. Takes no arguments. The stack holds
     * {@link #M3_STACK_DEPTH} entries: a push beyond that is a no-op (logged in debug builds) and
     * so is its matching {@link #M3_POP}. The stack is emptied by {@link #clearDepth3D()} and at
     * the end of each frame ({@link #endFrame3D()}). Projection and view are not stacked.
     */
    int M3_PUSH = 5;

    /**
     * Sub-op: restore the model matrix saved by the matching {@link #M3_PUSH}. Takes no
     * arguments. A pop with nothing to restore is a no-op (logged in debug builds).
     */
    int M3_POP = 6;

    /** Number of model matrices the {@link #M3_PUSH} stack holds. */
    int M3_STACK_DEPTH = 32;

    /** Light-type constants for {@link #setLights3D(int[], int[], float[])}. */
    int LIGHT_DIRECTIONAL = 0;
    int LIGHT_POINT = 1;

    /**
     * Light type: ambient light. Its color times its intensity (the 4th param; the first
     * three are unused and written as 0) is added to every surface. Ambient lights add up; a light
     * set with no ambient light gets the default ambient of 0.2 white.
     */
    int LIGHT_AMBIENT = 2;

    /**
     * Mask of the light kind ({@link #LIGHT_DIRECTIONAL}, {@link #LIGHT_POINT},
     * {@link #LIGHT_AMBIENT}) in a light's type word as written in a document (bits 0–7).
     */
    int LIGHT_KIND_MASK = 0xFF;

    /**
     * Position of the extra-parameter count in a light's type word (bits 8–15). A light with
     * {@code n} extra parameters has {@code 4 + n} params in the document; the extra ones are
     * reserved for extensions (spot cone, falloff, shadows) and a renderer that does not know them
     * ignores them.
     */
    int LIGHT_EXTRA_SHIFT = 8;

    /** Mask of the extra-parameter count after shifting by {@link #LIGHT_EXTRA_SHIFT}. */
    int LIGHT_EXTRA_MASK = 0xFF;

    /**
     * Render-mode constants for {@link #drawMesh3D(int, int)}. A mode chooses both the
     * <b>backend</b> (how triangles reach the screen) and the <b>shading</b> (flat per-face vs
     * smooth per-vertex), so the document author picks what fits the content — e.g. a faceted
     * low-poly look ({@code *_FLAT}) even on a mesh that carries normals, or smooth Gouraud
     * ({@code *_SMOOTH}) when it does.
     *
     * <p>Encoding: bit 0 is the shading bit ({@link #MODE_SMOOTH_MASK}); bits 1–3 are the engine
     * hint, decoded as {@code (mode >> MODE_ENGINE_SHIFT) & MODE_ENGINE_MASK} (one of the
     * {@code MODE_BACKEND_*} values); bits 4–7 are reserved and must be 0; bits 8–11 are the
     * wireframe style ({@link #MODE_WIREFRAME}, {@link #WIRE_EDGE_MASK}); bits 12 and up are
     * capability flags ({@link #MODE_TRANSPARENT}, {@link #MODE_GPU}, {@link #MODE_PBR},
     * {@link #MODE_DOUBLE_SIDED}, {@link #MODE_FRONT_CW}), which never affect the engine
     * choice. Bits 17–29 are reserved for future flags and ignored by players; bit 30 ({@link
     * #MODE_ADVANCED}) is refused by readers of this version; bit 31 is reserved.
     * Smooth requested on a mesh with no normals degrades to flat. The engine is a
     * hint: a renderer treats an engine it does not implement (e.g. the reserved shader/GL modes)
     * as software.
     */
    int MODE_SOFTWARE_FLAT = 0;

    int MODE_SOFTWARE_SMOOTH = 1;
    int MODE_CANVAS_FLAT = 2;
    int MODE_CANVAS_SMOOTH = 3;
    /** Painter-ordered GPU fill, flat per-face shading. */
    int MODE_DRAWMESH_FLAT = 4;
    /** Painter-ordered GPU fill, smooth per-vertex shading. */
    int MODE_DRAWMESH_SMOOTH = 5;
    /**
     * Global painter-sorted {@code Canvas.drawVertices}: collects triangles across all meshes in
     * the scene, sorts them back-to-front by average depth at {@link #endFrame3D()}, and draws them
     * in a single batch with flat (per-face) shading.
     */
    int MODE_DRAWMESH_ZBUF_FLAT = 8;
    /**
     * Global painter-sorted {@code Canvas.drawVertices}: collects triangles across all meshes in
     * the scene, sorts them back-to-front by average depth at {@link #endFrame3D()}, and draws them
     * in a single batch with smooth (Gouraud per-vertex) shading.
     */
    int MODE_DRAWMESH_ZBUF_SMOOTH = 9;

    /** {@code (mode & MODE_SMOOTH_MASK) != 0} selects smooth (Gouraud) shading. */
    int MODE_SMOOTH_MASK = 0x1;

    /**
     * Wireframe style flag, OR-ed into a mode. Draws the mesh as a hidden-line wireframe: each
     * front-facing triangle is rasterized into the depth buffer only (transparent faces), then its
     * three edges are drawn as depth-tested lines, so edges occluded by nearer surface are hidden.
     * Edge color is the current paint color. Depth-buffer based, so it is a <b>software</b>-only
     * style — setting it forces the software backend regardless of the backend bits (the canvas
     * backend has no depth buffer for hidden-line removal). Draw a shaded pass and then a wireframe
     * pass of the same mesh (the depth buffer is shared within a 3D pass) for a shaded+wireframe
     * look.
     */
    int MODE_WIREFRAME = 0x100;

    /**
     * Wireframe edge-selection bits, OR-ed into a wireframe mode to draw only a subset of each
     * triangle's three edges. A triangle {@code (v0,v1,v2)} has edge 0 = {@code v0→v1} (the
     * "first" line), edge 1 = {@code v1→v2} (the "second"), edge 2 = {@code v2→v0} (the "third").
     * When {@code MODE_WIREFRAME} is set but <b>no</b> edge bit is, all three edges are drawn
     * (the classic full wireframe — back-compatible with {@link #MODE_SOFTWARE_WIREFRAME}).
     *
     * <p>Selecting a subset is a way to visualize the mesh's triangulation order: if triangles are
     * emitted with a consistent edge order (e.g. all "first" edges run one way across a grid), the
     * selected edges form a regular pattern, which is a quick eyeball check that a generator's
     * winding/emit order is consistent.
     */
    int WIRE_EDGE0 = 0x200;

    int WIRE_EDGE1 = 0x400;
    int WIRE_EDGE2 = 0x800;

    /** Mask covering all three {@code WIRE_EDGE*} bits. */
    int WIRE_EDGE_MASK = WIRE_EDGE0 | WIRE_EDGE1 | WIRE_EDGE2;

    /** Convenience: hidden-line wireframe on the software backend ({@code MODE_WIREFRAME}). */
    int MODE_SOFTWARE_WIREFRAME = MODE_SOFTWARE_FLAT | MODE_WIREFRAME;

    /** Hidden-line wireframe drawing only each triangle's <b>first</b> edge ({@code v0→v1}). */
    int MODE_SOFTWARE_WIREFRAME1 = MODE_SOFTWARE_WIREFRAME | WIRE_EDGE0;

    /** Hidden-line wireframe drawing only each triangle's <b>second</b> edge ({@code v1→v2}). */
    int MODE_SOFTWARE_WIREFRAME2 = MODE_SOFTWARE_WIREFRAME | WIRE_EDGE1;

    /** Hidden-line wireframe drawing each triangle's <b>first and second</b> edges. */
    int MODE_SOFTWARE_WIREFRAME3 = MODE_SOFTWARE_WIREFRAME | WIRE_EDGE0 | WIRE_EDGE1;

    /** Backend selector values, i.e. {@code (mode >> MODE_ENGINE_SHIFT) & MODE_ENGINE_MASK}. */
    int MODE_BACKEND_SOFTWARE = 0;

    int MODE_BACKEND_CANVAS = 1;
    /** Backend selector for {@link #MODE_DRAWMESH_FLAT} / {@link #MODE_DRAWMESH_SMOOTH}. */
    int MODE_BACKEND_DRAWMESH = 2;
    int MODE_BACKEND_GL = 3;
    /**
     * Backend selector for {@link #MODE_DRAWMESH_ZBUF_FLAT} / {@link #MODE_DRAWMESH_ZBUF_SMOOTH}
     * (global painter-sorted {@code Canvas.drawVertices} across the scene's triangles).
     */
    int MODE_BACKEND_DRAWMESH_ZBUF = 4;
    /** Canvas engine with a depth buffer. A renderer without one uses software. */
    int MODE_BACKEND_CANVAS_ZBUF = 5;

    /** Position of the engine hint in a mode: {@code (mode >> MODE_ENGINE_SHIFT)}. */
    int MODE_ENGINE_SHIFT = 1;

    /** Mask of the engine hint after shifting: {@code (mode >> 1) & MODE_ENGINE_MASK}. */
    int MODE_ENGINE_MASK = 0x7;

    /**
     * Capability flag: blend the mesh src-over into the scene with the depth test on and
     * depth writes off. The document orders the draws: opaque first, then transparent from back
     * to front.
     */
    int MODE_TRANSPARENT = 1 << 12;

    /**
     * Capability flag: prefer a GPU engine if the player has one; the result may differ
     * from the software reference within normal GPU tolerances.
     */
    int MODE_GPU = 1 << 13;

    /**
     * Capability flag: the material is PBR. A renderer without PBR uses the base
     * material.
     */
    int MODE_PBR = 1 << 14;

    /**
     * Capability flag: turn off back-face culling. Back faces are lit with the reversed
     * normal.
     */
    int MODE_DOUBLE_SIDED = 1 << 15;

    /**
     * Flag: clockwise triangles (as the viewer sees them) are front-facing instead of
     * counter-clockwise ones, like {@code glFrontFace(GL_CW)}. It swaps which side of every
     * triangle is the front, so culling, the {@link #MODE_DOUBLE_SIDED} normal reversal and flat
     * face normals all follow. Use it for a mesh wound clockwise or drawn through a mirroring
     * model matrix. Infinite planes ignore it.
     */
    int MODE_FRONT_CW = 1 << 16;

    /**
     * Flag: the advanced form of the operation, defined by a later 3D format version. Readers of
     * this version refuse a mode with this bit set.
     */
    int MODE_ADVANCED = 1 << 30;

    /**
     * Upload (or replace) a cached mesh.
     *
     * @param id stable id used by {@link #drawMesh3D(int, int)}
     * @param indices triangle index list (length must be a multiple of 3). CCW winding =
     *     front-facing (clockwise when the draw sets {@link #MODE_FRONT_CW}).
     * @param verts vertex positions as {@code x,y,z} triples (length must be a multiple of
     *     3).
     * @param normals optional per-vertex normals as {@code nx,ny,nz} triples (same vertex
     *     count as {@code verts}). When {@code null} the renderer computes flat per-triangle
     *     normals.
     */
    void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals);

    /**
     * Upload (or replace) a cached mesh, with optional per-vertex UV texture coordinates.
     * Default implementation drops {@code uv} and forwards to {@link #defineMesh3D(int, int[],
     * float[], float[])} — UV is stored for future texturing and ignored by renderers that do
     * not texture yet.
     *
     * @param uv optional per-vertex {@code u,v} pairs (same vertex count as {@code verts}), or
     *     {@code null}
     */
    default void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals,
            float @Nullable [] uv) {
        defineMesh3D(id, indices, verts, normals);
    }

    /**
     * Define (or replace) mesh {@code id} as an infinite plane ({@code MeshPrimitive} type
     * {@link Primitive3D#INFINITE_PLANE}). It is drawn with {@link #drawMesh3D(int, int)} like any
     * mesh: the model matrix places it, and the paint color, material and lights shade it.
     *
     * <p>{@code params} ({@link Primitive3D#INFINITE_PLANE_PARAMS} floats): {@code cx, cy, cz} (a
     * point on the plane), {@code spacing} (grid cell size; 0 means no grid), {@code lineWidth}
     * (grid line width in pixels), and {@code lineA, lineR, lineG, lineB} (grid line color, each
     * in 0..1). The normal is +Y unless the axis field of {@code flags} ({@link
     * Primitive3D#axisMode}) selects +X or +Z. Grid lines blend over the plane and fade out as the
     * cells shrink below about 2 pixels, so there is no moiré.
     *
     * <p>The default implementation defines a large finite quad without a grid through {@link
     * #defineMesh3D(int, int[], float[], float[], float[])}, so a renderer without an
     * infinite-plane path still shows the plane around its center.
     */
    default void defineInfinitePlane3D(int id, int flags, float @NonNull [] params) {
        MeshData mesh = Primitive3D.build(Primitive3D.INFINITE_PLANE, 0f, flags, params);
        defineMesh3D(id, mesh.indices, mesh.verts, mesh.normals, mesh.uv);
    }

    /**
     * Set both projection and view in a single call.
     *
     * @param projection {@link #PROJECTION_PERSPECTIVE} or {@link #PROJECTION_ORTHO}
     * @param projParams perspective: 4 floats {@code fovYRadians, aspect, near, far}; ortho:
     *     6 floats {@code left, right, bottom, top, near, far}
     * @param viewParams 9 floats {@code eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX,
     *     upY, upZ} — {@code gluLookAt} semantics
     */
    void setCamera3D(
            int projection, float @NonNull [] projParams, float @NonNull [] viewParams);

    /**
     * Apply a 3D matrix op, post-multiplying the current modelview.
     *
     * @param sub one of {@link #M3_IDENTITY}, {@link #M3_TRANSLATE}, {@link #M3_SCALE},
     *     {@link #M3_ROTATE_AXIS}, {@link #M3_MULTIPLY}, or {@link #M3_PUSH} /
     *     {@link #M3_POP}. A renderer treats an unknown sub-op as a no-op.
     * @param args per sub: IDENTITY=0 floats, TRANSLATE/SCALE=3 floats {@code x,y,z},
     *     ROTATE_AXIS=4 floats {@code angleRadians, axisX, axisY, axisZ}, MULTIPLY=16 floats
     *     (column-major 4×4), PUSH/POP=0 floats
     */
    void matrix3DOp(int sub, float @NonNull [] args);

    /**
     * Draw a cached mesh with the current modelview, projection, and paint color.
     *
     * @param meshId mesh id from {@link #defineMesh3D} or {@link #defineInfinitePlane3D}
     * @param mode render mode: one of {@link #MODE_SOFTWARE_FLAT}, {@link #MODE_SOFTWARE_SMOOTH},
     *     {@link #MODE_CANVAS_FLAT}, {@link #MODE_CANVAS_SMOOTH}, …, optionally OR-ed with the
     *     wireframe bits and the capability flags ({@link #MODE_TRANSPARENT}, {@link #MODE_GPU},
     *     {@link #MODE_PBR}, {@link #MODE_DOUBLE_SIDED}, {@link #MODE_FRONT_CW}). Selects backend
     *     and shading; an unimplemented backend falls back to software, and unknown flag bits are
     *     ignored.
     */
    void drawMesh3D(int meshId, int mode);

    /** Clear the depth buffer for the current viewport. Required to start a 3D pass. */
    void clearDepth3D();

    /**
     * Define the entire 3D light set in one call. {@code types[i]} is {@link #LIGHT_DIRECTIONAL},
     * {@link #LIGHT_POINT} or {@link #LIGHT_AMBIENT}: the operation strips the extra
     * parameters of the document's type word ({@link #LIGHT_EXTRA_SHIFT}) and drops lights of a
     * kind it does not know, so a renderer only sees these kinds. {@code colors[i]} is the light
     * color (ARGB, RGB used); {@code params} holds 4 floats per light ({@code dirX,dirY,dirZ,
     * intensity} for directional, {@code posX,posY,posZ,intensity} for point, {@code 0,0,0,
     * intensity} for ambient), so {@code params.length == 4 * types.length}.
     *
     * <p>Lighting model, the same in every engine: with {@code N'} the surface normal, negated on
     * the back faces of a {@link #MODE_DOUBLE_SIDED} mesh, diffuse is the sum over directional
     * and point lights of {@code max(0, N'·L)} times the light's color and intensity (point
     * lights have no attenuation); a Blinn-Phong specular term is added only when the material's
     * specular strength is above 0; the ambient term is the sum of the ambient lights, or 0.2
     * white when the set has none. {@code L} is the unit vector from the surface toward the
     * light: a directional light's {@code dir} is the world-space direction the light travels, so
     * {@code L = -normalize(dir)} and the light strikes surfaces facing {@code -dir}; for a point
     * light {@code L = normalize(pos - P)}. Diffuse is one-sided, so a surface facing away from a
     * light gets none of it. An empty set means "no lights" (default ambient only). With this
     * never called, a renderer uses a headlight of intensity 0.8 that shines along the view
     * direction (so surfaces facing the camera are fully lit) plus the default ambient, so meshes
     * are never unlit.
     */
    void setLights3D(int @NonNull [] types, int @NonNull [] colors, float @NonNull [] params);

    /**
     * Set the active texture for subsequent {@link #drawMesh3D(int, int)} calls to a
     * previously uploaded bitmap, sampled through each mesh's UV. {@code bitmapId == 0} clears
     * the texture. A renderer without texture support, or a mesh without UV, ignores it.
     */
    void setTexture3D(int bitmapId);

    /**
     * Set the active surface material for subsequent {@link #drawMesh3D(int, int)} calls.
     * {@code specStrength} scales a Blinn-Phong specular highlight, in each light's color, added
     * on top of the diffuse shading; {@code shininess} is the specular exponent (higher = tighter,
     * glossier). {@code specStrength == 0} is the default matte material (no specular). A renderer
     * without specular support ignores it.
     */
    default void setMaterial3D(float specStrength, float shininess) {}

    /**
     * Set a polygon-offset depth bias (window-z units) for subsequent {@link #drawMesh3D(int,
     * int)} calls, to deterministically order near-coincident surfaces and avoid z-fighting.
     * The applied offset is {@code constant + slope * maxDepthSlope} (glPolygonOffset-style, where
     * {@code maxDepthSlope} is the triangle's max window-depth gradient). Positive pushes a surface
     * toward the far plane, negative pulls it nearer. {@code (0, 0)} is the default (no bias). A
     * renderer without depth-bias support ignores it.
     */
    default void setDepthBias3D(float constant, float slope) {}

    /**
     * Signal the end of a document frame: called once per frame, after the frame's last
     * operation, whether or not the frame drew any 3D. A renderer that defers 3D output (for
     * example to composite a run of 3D draws at once) must have drawn all of it to the canvas
     * when this returns. A renderer that draws immediately ignores it.
     */
    default void endFrame3D() {}
}
