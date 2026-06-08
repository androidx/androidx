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
package androidx.compose.remote.player.core.platform.d3;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.Utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Pure-Java software implementation of {@link Paint3DContext}.
 *
 * <p>This class is a standalone <b>engine delegate</b>. It does not extend
 * {@link androidx.compose.remote.core.PaintContext}; the host {@code PaintContext} (e.g.
 * {@code AndroidPaintContext}) implements {@code Paint3DContext} and forwards each method
 * to an instance of this class. Future siblings (e.g. {@code AGSLPaint3DContext},
 * {@code GLPaint3DContext}) can replace this delegate without the host changing.
 *
 * <p>Pipeline per {@link #drawMesh3D(int, int)}: transform verts by {@code projection × view ×
 * model}, clip each triangle against the near plane ({@code z ≥ −w}) and a far-off-screen guard
 * band (triangles needing no clipping take a fast path), perspective divide, viewport transform
 * (Y-flipped for the top-down pixel buffer), backface cull by NDC winding (CCW front-facing,
 * CW with {@link Paint3DContext#MODE_FRONT_CW}; zero-area triangles are dropped;
 * {@link Paint3DContext#MODE_DOUBLE_SIDED} keeps back faces), shade (flat per-face or smooth
 * per-vertex per the {@code mode}) with the lighting model of
 * {@link Paint3DContext#setLights3D}, and call the {@link Rasterizer}. A
 * {@link Paint3DContext#MODE_TRANSPARENT} draw is depth-tested but writes no depth, and blends
 * src-over into the color buffer.
 *
 * <p>Lighting is one function for every engine (this one and the host's canvas engines, which
 * take their vertex colors from {@link #buildCanvasVertices}): the vertex normals of a
 * back-facing triangle (only drawn with {@code MODE_DOUBLE_SIDED}) are reversed, a flat face
 * normal faces the viewer, diffuse is the sum of {@code max(0, N·L)} over the directional and
 * point lights, a Blinn-Phong highlight is added when the material's specular strength is above
 * 0, and the ambient term is the sum of the ambient lights (0.2 white when the set has none).
 *
 * <p>Conventions: see {@link Paint3DContext}. Right-handed, +Y up, −Z forward; column-major
 * 4×4; matrix ops post-multiply modelview. {@link Paint3DContext#M3_PUSH} /
 * {@link Paint3DContext#M3_POP} save and restore the model matrix on a stack of
 * {@link Paint3DContext#M3_STACK_DEPTH} entries, emptied by {@link #clearDepth3D()} and
 * {@link #endFrame3D()}.
 *
 * <h3>Buffer ownership</h3>
 *
 * <p>The engine owns its color buffer ({@code int[]}, ARGB) and depth buffer
 * ({@code float[]}), both laid out with a row stride of {@link #getWidth()}. Call
 * {@link #setSize(int, int)} before any draw. The host reads pixels via
 * {@link #getColorBuffer()} and is responsible for compositing them onto its native canvas
 * (e.g. via an Android {@code Bitmap.setPixels} + {@code Canvas.drawBitmap}).
 *
 * <p>The engine tracks two rectangles so the host never has to copy or clear more than a scene
 * touched. The <i>pending</i> rectangle ({@link #getPendingLeft()} etc.) bounds every pixel
 * written since the host last composited: the host copies just that rectangle and then calls
 * {@link #clearPendingColor()}, which clears its color but keeps the depth, so later draws in the
 * same scene stay depth-tested against earlier ones. The <i>scene</i> rectangle bounds every pixel
 * written since the last {@link #clearDepth3D()}; outside it the color buffer is always 0 and the
 * depth buffer always {@code +∞}, so {@link #clearDepth3D()} only clears that rectangle.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class JavaPaint3DContext implements Paint3DContext {

    private final float[] mProj = new float[16];
    private final float[] mView = new float[16];
    private final float[] mModel = new float[16];
    private final float[] mPV = new float[16];
    private final float[] mMVP = new float[16];
    private final float[] mMV = new float[16];

    private final MeshTable mMeshes = new MeshTable();

    private int[] mColor;
    private float[] mZbuf;
    private int mWidth;
    private int mHeight;

    // Pixels written since the host last composited them (see clearPendingColor()); half-open
    // [left, right) x [top, bottom), empty when right <= left.
    private int mPendingLeft;
    private int mPendingTop;
    private int mPendingRight;
    private int mPendingBottom;

    // Pixels written since the last clearDepth3D() (same convention). Outside this rectangle the
    // color buffer is 0 and the depth buffer +inf: clears and blits only touch what a scene used.
    private int mSceneLeft;
    private int mSceneTop;
    private int mSceneRight;
    private int mSceneBottom;

    private int mBaseColorArgb = 0xFFFFFFFF;

    /**
     * Smallest clip-space {@code w} a vertex may have. The near plane ({@code z ≥ −w}) already
     * keeps {@code w ≥ near} for any valid perspective camera; this extra plane only matters for
     * degenerate projections (near ≤ 0) or projective model matrices, where it keeps the
     * perspective divide finite. {@link InfinitePlane} uses the same floor.
     */
    static final float NEAR_W_EPSILON = 0.0001f;

    /**
     * Guard band, in pixels beyond each viewport edge. Triangles reaching further are clipped
     * to it (in clip space) before rasterization, which bounds the rasterizer's coordinates (see
     * {@link Rasterizer}) and the length of wireframe lines. Anything closer takes the
     * unclipped fast path, so ordinary geometry is rasterized exactly as before.
     */
    private static final float GUARD_BAND_PX = 65536f;

    /**
     * Most triangles one input triangle can become: clipping a triangle by the near plane, the
     * {@code w} floor and the four guard-band planes leaves a convex polygon of at most 9
     * vertices, which fans into 7 triangles.
     */
    private static final int MAX_CLIPPED_TRIANGLES = 7;

    private static final int MAX_POLYGON_VERTICES = MAX_CLIPPED_TRIANGLES + 2;

    /** Floats per clip-polygon vertex: clip x, y, z, w, then barycentric weights b0, b1, b2. */
    private static final int CLIP_STRIDE = 7;

    /** Clip planes: near, {@code w} floor, and the guard band's four sides. */
    private static final int CLIP_PLANE_COUNT = 6;

    /** Ambient term of a light set without ambient lights (and of the default lighting). */
    private static final float AMBIENT = 0.2f;

    /** Cap on accumulated lights, guarding against a document that re-adds without clearing. */
    private static final int MAX_LIGHTS = 32;

    /**
     * Debug logging (engine diagnostics: stack overflow and underflow, unknown light kinds, draws
     * whose every triangle is back-facing, textured draws on meshes without UV, the GPU and PBR
     * flags). A compile-time constant, so the checks cost nothing when off.
     */
    private static final boolean DEBUG = false;

    // ----- Model matrix stack (M3_PUSH / M3_POP) -------------------------------

    /** Saved model matrices, 16 floats each; the first {@link #mModelStackSize} are in use. */
    private final float[] mModelStack = new float[M3_STACK_DEPTH * 16];

    private int mModelStackSize;

    /** Pushes beyond {@link #M3_STACK_DEPTH} not yet matched by a pop; each was a no-op. */
    private int mModelStackOverflow;

    // ----- Lighting state ---------------------------------------------------

    // The directional and point lights of the last setLights3D() in world space: kind, color in
    // [0,1], intensity, and travel direction (directional) or position (point). Preallocated, so
    // setting lights allocates nothing.
    private int mLightCount;
    private final int[] mLightKind = new int[MAX_LIGHTS];
    private final float[] mLightR = new float[MAX_LIGHTS];
    private final float[] mLightG = new float[MAX_LIGHTS];
    private final float[] mLightB = new float[MAX_LIGHTS];
    private final float[] mLightIntensity = new float[MAX_LIGHTS];
    private final float[] mLightX = new float[MAX_LIGHTS];
    private final float[] mLightY = new float[MAX_LIGHTS];
    private final float[] mLightZ = new float[MAX_LIGHTS];

    /** Once a light op runs we honor the light set verbatim; until then a default light. */
    private boolean mLightsTouched = false;

    // Ambient term added to every surface: the sum of the set's ambient lights (color ×
    // intensity), or AMBIENT white when it has none or no light op ran.
    private float mAmbientR = AMBIENT;
    private float mAmbientG = AMBIENT;
    private float mAmbientB = AMBIENT;

    // Per-draw eye-space light scratch (parallel arrays, filled by prepareLightsEyeSpace).
    private int mNumEyeLights;
    private final int[] mEType = new int[MAX_LIGHTS];
    private final float[] mElx = new float[MAX_LIGHTS];
    private final float[] mEly = new float[MAX_LIGHTS];
    private final float[] mElz = new float[MAX_LIGHTS];
    private final float[] mElr = new float[MAX_LIGHTS];
    private final float[] mElg = new float[MAX_LIGHTS];
    private final float[] mElb = new float[MAX_LIGHTS];

    private final float[] mScratch4 = new float[4];
    private final float[] mLightScratch3 = new float[3];

    /** Back-facing triangles {@link #projectTriangle} culled since the counter was reset. */
    private int mDebugBackFaces;

    /** Vertex normals the smooth shading used since the counter was reset (debug only). */
    private int mDebugNormals;

    /** Of {@link #mDebugNormals}, those pointing to their triangle's back side. */
    private int mDebugBackwardNormals;

    /** Bounds of the pixels the last infinite-plane draw may have written. */
    private final int[] mPlaneRect = new int[4];

    /**
     * Lights an infinite plane with the shared model ({@link #litColor}). The plane passes the
     * normal already turned toward the viewer ({@code N'}), so it is not reversed again.
     */
    private final InfinitePlane.Shader mPlaneShader =
            (px, py, pz, nx, ny, nz) -> litColor(mBaseColorArgb, nx, ny, nz, px, py, pz);

    // Software texture (host-decoded ARGB pixels) for the textured rasterizer path.
    private int @Nullable [] mTexPixels;
    private int mTexW;
    private int mTexH;

    // Specular material (Blinn-Phong). mSpecStrength == 0 → matte (default, no specular cost).
    private float mSpecStrength;
    private float mShininess = 32f;

    // Polygon-offset depth bias (window-z units). Both 0 → no bias (default, no per-triangle cost).
    private float mDepthBiasC;
    private float mDepthBiasS;

    /**
     * Screen-space output of {@link #projectTriangle}: {@code sx,sy,sz × 3} for each of the
     * triangles it returned (9 floats per triangle).
     */
    private final float[] mTriScreen = new float[9 * MAX_CLIPPED_TRIANGLES];

    /**
     * Per-vertex shaded ARGB of the triangles last returned by {@link #projectTriangle}, in
     * vertex order {@code v0,v1,v2} (3 per triangle). For meshes without normals all three are
     * equal (flat).
     */
    private final int[] mTriColor = new int[3 * MAX_CLIPPED_TRIANGLES];

    /** Scratch eye-space normal for per-vertex (smooth) shading. */
    private final float[] mNormal3 = new float[3];

    /** Per-vertex 1/clipW of the triangles last returned by {@link #projectTriangle} (v0,v1,v2),
     * for perspective-correct attribute interpolation. */
    private final float[] mTriInvW = new float[3 * MAX_CLIPPED_TRIANGLES];

    /** Per-vertex UV ({@code u,v × 3}, 6 per triangle) when the mesh has UV. */
    private final float[] mTriUv = new float[6 * MAX_CLIPPED_TRIANGLES];

    // Screen-space bounds of everything the last projectTriangle call returned (vertices of its
    // triangles, and of its outline). See addDrawnBounds() for the pixels they can touch.
    private float mTriMinX;
    private float mTriMinY;
    private float mTriMaxX;
    private float mTriMaxY;

    /**
     * Whether the last {@link #projectTriangle} call clipped its triangle. If not, the outline of
     * the triangle (for wireframe) is the returned triangle itself, {@link #mTriScreen}{@code
     * [0, 9)}, whose edges {@code i → i+1} lie on source edges {@code i}. If so, it is the clipped
     * polygon: {@link #mClipOutlineCount} vertices in {@link #mPolyScreen} ({@code sx,sy,sz}
     * each), with {@link #mClipOutlineEdge}{@code [i]} the source edge under edge {@code i → i+1}
     * (0 = {@code v0→v1}, 1 = {@code v1→v2}, 2 = {@code v2→v0}), or −1 if clipping introduced
     * it.
     */
    private boolean mTriClipped;

    // Shaded colors of the source triangle's three vertices, before clipping interpolates them.
    private final int[] mVertexColor = new int[3];

    // Clip polygon ping-pong buffers (CLIP_STRIDE floats per vertex) with the source edge each
    // polygon edge i -> i+1 lies on (-1 if introduced by clipping), per-vertex plane distances,
    // and the projected polygon's screen positions and attributes.
    private final float[] mPolyA = new float[CLIP_STRIDE * MAX_POLYGON_VERTICES];
    private final float[] mPolyB = new float[CLIP_STRIDE * MAX_POLYGON_VERTICES];
    private final int[] mPolyEdgeA = new int[MAX_POLYGON_VERTICES];
    private final int[] mPolyEdgeB = new int[MAX_POLYGON_VERTICES];
    private final float[] mPolyDist = new float[MAX_POLYGON_VERTICES];
    private final float[] mPolyScreen = new float[3 * MAX_POLYGON_VERTICES];
    private final int[] mPolyColor = new int[MAX_POLYGON_VERTICES];
    private final float[] mPolyInvW = new float[MAX_POLYGON_VERTICES];
    private final float[] mPolyUv = new float[2 * MAX_POLYGON_VERTICES];
    private int[] mClipOutlineEdge = mPolyEdgeA;
    private int mClipOutlineCount;

    public JavaPaint3DContext() {
        Matrix4.identity(mProj);
        Matrix4.identity(mView);
        Matrix4.identity(mModel);
    }

    /**
     * Set the viewport size in pixels. The buffers are only reallocated when they are too small;
     * a smaller or differently shaped viewport reuses them. On a size change the current scene
     * is cleared (so pixels not yet composited are dropped: composite them first).
     */
    public void setSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (mColor != null && mWidth == width && mHeight == height) {
            return;
        }
        long needed = (long) width * height;
        if (needed > Integer.MAX_VALUE) {
            return;
        }
        if (mColor == null || mColor.length < needed) {
            mColor = new int[(int) needed];
            mZbuf = new float[(int) needed];
            Rasterizer.clearDepth(mZbuf);
        } else {
            // Restore "color 0, depth +inf" everywhere before the row stride changes.
            clearScene();
        }
        mWidth = width;
        mHeight = height;
        mPendingRight = mPendingLeft;
        mSceneRight = mSceneLeft;
    }

    /** Viewport width in pixels; also the row stride of the color and depth buffers. */
    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    /**
     * Engine color buffer (ARGB, row stride {@link #getWidth()}). May be {@code null} before
     * setSize, and may be longer than {@code getWidth() * getHeight()}.
     */
    public int @Nullable [] getColorBuffer() {
        return mColor;
    }

    /** Whether pixels were written since the host last called {@link #clearPendingColor()}. */
    public boolean hasPendingPixels() {
        return mPendingRight > mPendingLeft && mPendingBottom > mPendingTop;
    }

    /** Left edge (inclusive) of the pending rectangle; see {@link #hasPendingPixels()}. */
    public int getPendingLeft() {
        return mPendingLeft;
    }

    /** Top edge (inclusive) of the pending rectangle; see {@link #hasPendingPixels()}. */
    public int getPendingTop() {
        return mPendingTop;
    }

    /** Right edge (exclusive) of the pending rectangle; see {@link #hasPendingPixels()}. */
    public int getPendingRight() {
        return mPendingRight;
    }

    /** Bottom edge (exclusive) of the pending rectangle; see {@link #hasPendingPixels()}. */
    public int getPendingBottom() {
        return mPendingBottom;
    }

    /**
     * Clear the color of the pending rectangle (keeping its depth) and empty it. The host calls
     * this after compositing those pixels, so the next composite of the same scene carries only
     * newer pixels and nothing is drawn twice; the kept depth still hides later geometry behind
     * earlier geometry. Every pixel outside the pending rectangle is 0 at all times.
     */
    public void clearPendingColor() {
        if (hasPendingPixels()) {
            int[] color = mColor;
            int stride = mWidth;
            int left = mPendingLeft;
            int right = mPendingRight;
            for (int y = mPendingTop; y < mPendingBottom; y++) {
                int row = y * stride;
                Arrays.fill(color, row + left, row + right, 0);
            }
        }
        mPendingRight = mPendingLeft;
    }

    /** Set the base ARGB color used by subsequent {@link #drawMesh3D(int, int)} calls. */
    public void setBaseColorArgb(int argb) {
        mBaseColorArgb = argb;
    }

    public int getBaseColorArgb() {
        return mBaseColorArgb;
    }

    // ----- Paint3DContext --------------------------------------------------

    @Override
    public void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals) {
        mMeshes.put(id, new Mesh(indices, verts, normals, null));
    }

    @Override
    public void defineMesh3D(
            int id,
            int @NonNull [] indices,
            float @NonNull [] verts,
            float @Nullable [] normals,
            float @Nullable [] uv) {
        mMeshes.put(id, new Mesh(indices, verts, normals, uv));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This engine draws the plane itself ({@link InfinitePlane}): to the horizon, with its
     * grid, in every engine. The params are copied; re-defining a plane reuses its storage.
     */
    @Override
    public void defineInfinitePlane3D(int id, int flags, float @NonNull [] params) {
        Mesh old = mMeshes.get(id);
        if (old != null && old.mPlane != null) {
            old.mPlane.set(flags, params);
            return;
        }
        mMeshes.put(id, new Mesh(new InfinitePlane(flags, params)));
    }

    /** The infinite plane defined as mesh {@code meshId}, or {@code null} if it is not one. */
    public @Nullable InfinitePlane getInfinitePlane(int meshId) {
        Mesh m = mMeshes.get(meshId);
        return m == null ? null : m.mPlane;
    }

    /**
     * Canvas engines: the visible polygon of the infinite plane {@code meshId} (the plane ∩
     * the view frustum, at most {@link InfinitePlane#MAX_POLYGON_VERTICES} vertices, not split)
     * with the current matrices, lights and base color; see
     * {@link InfinitePlane#visiblePolygon}.
     *
     * @return the vertex count (0 if nothing is visible or {@code meshId} is not a plane)
     */
    public int buildCanvasPlane(int meshId, int mode, float @NonNull [] outXY,
            float @Nullable [] outDepth, int @Nullable [] outColor) {
        InfinitePlane plane = getInfinitePlane(meshId);
        if (plane == null) {
            return 0;
        }
        if (DEBUG) {
            debugCheckDraw(meshId, mode, null);
        }
        Matrix4.multiply(mMV, mView, mModel);
        prepareLightsEyeSpace();
        return plane.visiblePolygon(mMV, mProj, mWidth, mHeight, mode, mDepthBiasC, mDepthBiasS,
                planeShadingUniform(), outXY, outDepth, outColor, mPlaneShader);
    }

    /**
     * Canvas engines: builds the grid lines of the infinite plane {@code meshId} with the
     * current matrices, to read from {@link #getInfinitePlane} (see
     * {@link InfinitePlane#buildGridLines}).
     *
     * @return the number of lines (0 without a grid, or if {@code meshId} is not a plane)
     */
    public int buildCanvasPlaneGrid(int meshId, int mode) {
        InfinitePlane plane = getInfinitePlane(meshId);
        if (plane == null) {
            return 0;
        }
        Matrix4.multiply(mMV, mView, mModel);
        return plane.buildGridLines(mMV, mProj, mWidth, mHeight, mode);
    }

    /**
     * Whether the lighting is the same at every point of a plane: no point lights and no
     * specular. Valid after {@link #prepareLightsEyeSpace}.
     */
    private boolean planeShadingUniform() {
        if (mSpecStrength > 0f) {
            return false;
        }
        for (int i = 0; i < mNumEyeLights; i++) {
            if (mEType[i] == LIGHT_POINT) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setCamera3D(
            int projection, float @NonNull [] projParams, float @NonNull [] viewParams) {
        // Extra trailing params are ignored. Too few projection params make the call a no-op;
        // too few view params (after a valid projection) leave the view and model as they were.
        if (projection == PROJECTION_PERSPECTIVE) {
            if (projParams.length < 4) {
                return;
            }
            Matrix4.perspective(
                    mProj, projParams[0], projParams[1], projParams[2], projParams[3]);
        } else {
            if (projParams.length < 6) {
                return;
            }
            Matrix4.ortho(
                    mProj,
                    projParams[0],
                    projParams[1],
                    projParams[2],
                    projParams[3],
                    projParams[4],
                    projParams[5]);
        }
        if (viewParams.length < 9) {
            return;
        }
        Matrix4.lookAt(
                mView,
                viewParams[0],
                viewParams[1],
                viewParams[2],
                viewParams[3],
                viewParams[4],
                viewParams[5],
                viewParams[6],
                viewParams[7],
                viewParams[8]);
        Matrix4.identity(mModel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extra trailing args are ignored. A sub-op with too few args, or one this engine does not
     * know, is a no-op. {@link #M3_PUSH} and {@link #M3_POP} save and restore the model matrix
     * only (projection and view are never stacked).
     */
    @Override
    public void matrix3DOp(int sub, float @NonNull [] args) {
        switch (sub) {
            case M3_IDENTITY:
                Matrix4.identity(mModel);
                break;
            case M3_TRANSLATE:
                if (args.length >= 3) {
                    Matrix4.translate(mModel, args[0], args[1], args[2]);
                } else if (DEBUG) {
                    Utils.log("3D matrix op " + sub + ": " + args.length + " args, need 3");
                }
                break;
            case M3_SCALE:
                if (args.length >= 3) {
                    Matrix4.scale(mModel, args[0], args[1], args[2]);
                } else if (DEBUG) {
                    Utils.log("3D matrix op " + sub + ": " + args.length + " args, need 3");
                }
                break;
            case M3_ROTATE_AXIS:
                if (args.length >= 4) {
                    Matrix4.rotateAxis(mModel, args[0], args[1], args[2], args[3]);
                } else if (DEBUG) {
                    Utils.log("3D matrix op " + sub + ": " + args.length + " args, need 4");
                }
                break;
            case M3_MULTIPLY:
                if (args.length >= 16) {
                    Matrix4.multiply(mModel, mModel, args);
                } else if (DEBUG) {
                    Utils.log("3D matrix op " + sub + ": " + args.length + " args, need 16");
                }
                break;
            case M3_PUSH:
                pushModelMatrix();
                break;
            case M3_POP:
                popModelMatrix();
                break;
            default:
                if (DEBUG) {
                    Utils.log("3D matrix op " + sub + ": unknown, ignored");
                }
                break;
        }
    }

    /**
     * Save a copy of the model matrix. Beyond {@link #M3_STACK_DEPTH} saved matrices the push is
     * counted but does nothing, so that its matching pop does nothing either.
     */
    private void pushModelMatrix() {
        if (mModelStackSize < M3_STACK_DEPTH) {
            System.arraycopy(mModel, 0, mModelStack, mModelStackSize * 16, 16);
            mModelStackSize++;
        } else {
            mModelStackOverflow++;
            if (DEBUG) {
                Utils.log("3D matrix push: stack full (" + M3_STACK_DEPTH + "), ignored");
            }
        }
    }

    /** Restore the model matrix of the matching push; nothing to restore is a no-op. */
    private void popModelMatrix() {
        if (mModelStackOverflow > 0) {
            mModelStackOverflow--; // the matching push was ignored
        } else if (mModelStackSize > 0) {
            mModelStackSize--;
            System.arraycopy(mModelStack, mModelStackSize * 16, mModel, 0, 16);
        } else if (DEBUG) {
            Utils.log("3D matrix pop: stack empty, ignored");
        }
    }

    /** Empty the model matrix stack, leaving the model matrix itself as it is. */
    private void resetModelStack() {
        mModelStackSize = 0;
        mModelStackOverflow = 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also clears the color buffer, and empties the model matrix stack ({@link #M3_PUSH}).
     */
    @Override
    public void clearDepth3D() {
        resetModelStack();
        clearScene();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This engine has no canvas, so the host composites its pixels; here the frame only ends
     * the model matrix stack ({@link #M3_PUSH}). The buffers are left as they are.
     */
    @Override
    public void endFrame3D() {
        resetModelStack();
    }

    /**
     * Restore "color 0, depth +∞" everywhere: clear the scene rectangle (outside it the buffers
     * already hold that) and empty the pending and scene rectangles.
     */
    private void clearScene() {
        if (mZbuf == null) {
            return;
        }
        // Also clear the color buffer to transparent so the host's existing canvas content
        // shows through where the 3D pass writes nothing. Only the scene rectangle can hold
        // anything else, so that is all that needs clearing.
        if (mSceneRight > mSceneLeft && mSceneBottom > mSceneTop) {
            float[] zbuf = mZbuf;
            int[] color = mColor;
            int stride = mWidth;
            int left = mSceneLeft;
            int right = mSceneRight;
            for (int y = mSceneTop; y < mSceneBottom; y++) {
                int row = y * stride;
                Arrays.fill(zbuf, row + left, row + right, Float.POSITIVE_INFINITY);
                Arrays.fill(color, row + left, row + right, 0);
            }
        }
        mSceneRight = mSceneLeft;
        mPendingRight = mPendingLeft;
    }

    /**
     * Add the pixels that primitives with screen-space bounds {@code [minX, maxX] × [minY, maxY]}
     * can touch to the pending and scene rectangles. The margins cover the rasterizer's 28.4
     * rounding and the line DDA's truncation, so the result is a superset (by at most two
     * pixels a side) of what was drawn. No-op when {@code minX > maxX} (nothing drawn).
     */
    private void addDrawnBounds(float minX, float minY, float maxX, float maxY, int w, int h) {
        if (!(minX <= maxX && minY <= maxY)) {
            return;
        }
        int left = (int) minX - 1;
        int top = (int) minY - 1;
        int right = (int) maxX + 2;
        int bottom = (int) maxY + 2;
        addDrawnRect(left < 0 ? 0 : left, top < 0 ? 0 : top, right > w ? w : right,
                bottom > h ? h : bottom);
    }

    /** Add a drawn pixel rectangle (half-open, viewport-clamped) to the pending and scene rects. */
    private void addDrawnRect(int left, int top, int right, int bottom) {
        if (right <= left || bottom <= top) {
            return;
        }
        if (mPendingRight <= mPendingLeft || mPendingBottom <= mPendingTop) {
            mPendingLeft = left;
            mPendingTop = top;
            mPendingRight = right;
            mPendingBottom = bottom;
        } else {
            mPendingLeft = Math.min(mPendingLeft, left);
            mPendingTop = Math.min(mPendingTop, top);
            mPendingRight = Math.max(mPendingRight, right);
            mPendingBottom = Math.max(mPendingBottom, bottom);
        }
        if (mSceneRight <= mSceneLeft || mSceneBottom <= mSceneTop) {
            mSceneLeft = left;
            mSceneTop = top;
            mSceneRight = right;
            mSceneBottom = bottom;
        } else {
            mSceneLeft = Math.min(mSceneLeft, left);
            mSceneTop = Math.min(mSceneTop, top);
            mSceneRight = Math.max(mSceneRight, right);
            mSceneBottom = Math.max(mSceneBottom, bottom);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Directional and point lights beyond the first 32 are dropped; ambient lights are summed,
     * however many there are. A light whose 4 params are missing ends the set, and a light of
     * unknown kind is ignored.
     */
    @Override
    public void setLights3D(
            int @NonNull [] types, int @NonNull [] colors, float @NonNull [] params) {
        int count = 0;
        boolean anyAmbient = false;
        float ambientR = 0f;
        float ambientG = 0f;
        float ambientB = 0f;
        int n = Math.min(types.length, colors.length);
        for (int i = 0; i < n; i++) {
            int p = i * 4;
            if (p + 3 >= params.length) {
                break;
            }
            int kind = types[i];
            int colorArgb = colors[i];
            float r = ((colorArgb >>> 16) & 0xFF) * (1f / 255f);
            float g = ((colorArgb >>> 8) & 0xFF) * (1f / 255f);
            float b = (colorArgb & 0xFF) * (1f / 255f);
            float intensity = params[p + 3];
            if (kind == LIGHT_AMBIENT) {
                anyAmbient = true;
                ambientR += r * intensity;
                ambientG += g * intensity;
                ambientB += b * intensity;
            } else if (kind == LIGHT_DIRECTIONAL || kind == LIGHT_POINT) {
                if (count == MAX_LIGHTS) {
                    if (DEBUG) {
                        Utils.log("3D lights: more than " + MAX_LIGHTS + ", light " + i
                                + " ignored");
                    }
                    continue;
                }
                mLightKind[count] = kind;
                mLightR[count] = r;
                mLightG[count] = g;
                mLightB[count] = b;
                mLightIntensity[count] = intensity;
                mLightX[count] = params[p];
                mLightY[count] = params[p + 1];
                mLightZ[count] = params[p + 2];
                count++;
            } else if (DEBUG) {
                Utils.log("3D lights: light " + i + " has unknown kind " + kind + ", ignored");
            }
        }
        mLightCount = count;
        if (anyAmbient) {
            mAmbientR = ambientR;
            mAmbientG = ambientG;
            mAmbientB = ambientB;
        } else {
            mAmbientR = AMBIENT;
            mAmbientG = AMBIENT;
            mAmbientB = AMBIENT;
        }
        mLightsTouched = true;
    }

    @Override
    public void setTexture3D(int bitmapId) {
        // The pure-Java engine has no bitmap store; the host (AndroidPaintContext) supplies the
        // decoded pixels for software sampling via setTextureData(). Nothing to do here.
    }

    @Override
    public void setMaterial3D(float specStrength, float shininess) {
        mSpecStrength = specStrength;
        mShininess = shininess > 0f ? shininess : 1f;
    }

    @Override
    public void setDepthBias3D(float constant, float slope) {
        mDepthBiasC = constant;
        mDepthBiasS = slope;
    }

    /**
     * Host-supplied texture pixels (ARGB, row {@code y} at {@code pixels[y * w]}, so at least
     * {@code w * h} of them) for the software textured rasterizer; {@code null} clears the
     * texture. Called by the host alongside {@link #setTexture3D}. The array is kept, not
     * copied, and read by the draws that follow, so a host may refill it with another texture
     * as long as it then calls this again.
     */
    public void setTextureData(int @Nullable [] pixels, int w, int h) {
        mTexPixels = pixels;
        mTexW = w;
        mTexH = h;
    }

    /**
     * Resolve the active lights into eye space for the current view. With no light op ever
     * issued ({@link #mLightsTouched} false) this installs a single default headlight (white,
     * along the view axis, {@code L = +z} toward the viewer) so surfaces facing the camera are
     * lit. Directional lights store {@code L}, the unit vector <b>toward</b> the light: the
     * document's {@code dir} is the direction the light travels, so {@code L =
     * -normalize(dir)}, negated here once per draw rather than per vertex. Point lights store
     * their eye-space position. Color is premultiplied by intensity. Ambient lights are not
     * here: they are summed into {@link #mAmbientR} etc.
     */
    private void prepareLightsEyeSpace() {
        if (!mLightsTouched) {
            mEType[0] = Paint3DContext.LIGHT_DIRECTIONAL;
            mElx[0] = 0f;
            mEly[0] = 0f;
            mElz[0] = 1f;
            mElr[0] = 1f - AMBIENT;
            mElg[0] = 1f - AMBIENT;
            mElb[0] = 1f - AMBIENT;
            mNumEyeLights = 1;
            return;
        }
        int n = mLightCount;
        for (int i = 0; i < n; i++) {
            int kind = mLightKind[i];
            mEType[i] = kind;
            if (kind == Paint3DContext.LIGHT_POINT) {
                Matrix4.transformPoint(mView, mLightX[i], mLightY[i], mLightZ[i], mScratch4);
                mElx[i] = mScratch4[0];
                mEly[i] = mScratch4[1];
                mElz[i] = mScratch4[2];
            } else {
                // Directional: world travel direction -> eye, store unit vector to the light.
                Matrix4.transformDirection(mView, mLightX[i], mLightY[i], mLightZ[i],
                        mLightScratch3);
                float x = -mLightScratch3[0], y = -mLightScratch3[1], z = -mLightScratch3[2];
                float len = (float) Math.sqrt((double) (x * x + y * y + z * z));
                if (len > 0f) {
                    x /= len;
                    y /= len;
                    z /= len;
                }
                mElx[i] = x;
                mEly[i] = y;
                mElz[i] = z;
            }
            mElr[i] = mLightR[i] * mLightIntensity[i];
            mElg[i] = mLightG[i] * mLightIntensity[i];
            mElb[i] = mLightB[i] * mLightIntensity[i];
        }
        mNumEyeLights = n;
    }

    /**
     * The lighting model, shared by every engine: {@code base} modulated by the ambient term
     * plus each directional and point light's {@code max(0, N'·L)} contribution, with {@code L}
     * the unit vector toward the light (no attenuation),
     * plus a Blinn-Phong highlight when the material's specular strength is above 0 (see
     * {@link #litColorSpecular}). Alpha is kept. {@code (nx,ny,nz)} is the eye-space normal
     * {@code N'} (need not be unit): the caller has already reversed it for a back-facing
     * triangle; it is never flipped here. {@code (px,py,pz)} is the eye-space position (point
     * lights and the view vector). Lights must have been resolved by
     * {@link #prepareLightsEyeSpace} for the current view.
     */
    private int litColor(int base, float nx, float ny, float nz, float px, float py, float pz) {
        float nlen = (float) Math.sqrt((double) (nx * nx + ny * ny + nz * nz));
        float inv = (nlen == 0f) ? 0f : 1f / nlen;
        if (mSpecStrength > 0f) {
            return litColorSpecular(base, nx * inv, ny * inv, nz * inv, px, py, pz);
        }
        float lr = mAmbientR, lg = mAmbientG, lb = mAmbientB;
        for (int i = 0; i < mNumEyeLights; i++) {
            float lx, ly, lz;
            if (mEType[i] == Paint3DContext.LIGHT_POINT) {
                lx = mElx[i] - px;
                ly = mEly[i] - py;
                lz = mElz[i] - pz;
                float ll = (float) Math.sqrt((double) (lx * lx + ly * ly + lz * lz));
                if (ll > 0f) {
                    lx /= ll;
                    ly /= ll;
                    lz /= ll;
                }
            } else {
                lx = mElx[i];
                ly = mEly[i];
                lz = mElz[i];
            }
            // One-sided: a surface facing away from the light gets none of it. Branchless, since
            // the sign of N'·L is unpredictable across a mesh.
            float d = Math.max(0f, (nx * lx + ny * ly + nz * lz) * inv);
            lr += mElr[i] * d;
            lg += mElg[i] * d;
            lb += mElb[i] * d;
        }
        int a = (base >>> 24) & 0xFF;
        int r = (int) (((base >>> 16) & 0xFF) * lr);
        int g = (int) (((base >>> 8) & 0xFF) * lg);
        int b = (int) ((base & 0xFF) * lb);
        return (a << 24) | (clampChannel(r) << 16) | (clampChannel(g) << 8) | clampChannel(b);
    }

    /**
     * The {@link #litColor} model when the specular strength is above 0: diffuse Lambert
     * modulating the base color, plus an additive Blinn-Phong highlight in the light's color,
     * only from lights the surface faces. The normal {@code (nfx,nfy,nfz)} is unit and already
     * oriented ({@code N'}). The view vector is {@code -normalize(p)} since the eye is at the
     * eye-space origin.
     */
    private int litColorSpecular(int base, float nfx, float nfy, float nfz, float px, float py,
            float pz) {
        float vlen = (float) Math.sqrt((double) (px * px + py * py + pz * pz));
        float vx = 0f, vy = 0f, vz = 1f;
        if (vlen > 0f) {
            vx = -px / vlen;
            vy = -py / vlen;
            vz = -pz / vlen;
        }
        float lr = mAmbientR, lg = mAmbientG, lb = mAmbientB; // diffuse + ambient (modulates base)
        float sr = 0f, sg = 0f, sb = 0f; // specular (added on top, light-colored)
        for (int i = 0; i < mNumEyeLights; i++) {
            float lx, ly, lz;
            if (mEType[i] == Paint3DContext.LIGHT_POINT) {
                lx = mElx[i] - px;
                ly = mEly[i] - py;
                lz = mElz[i] - pz;
                float ll = (float) Math.sqrt((double) (lx * lx + ly * ly + lz * lz));
                if (ll > 0f) {
                    lx /= ll;
                    ly /= ll;
                    lz /= ll;
                }
            } else {
                lx = mElx[i];
                ly = mEly[i];
                lz = mElz[i];
            }
            float ndl = nfx * lx + nfy * ly + nfz * lz;
            float d = Math.max(0f, ndl); // one-sided diffuse, branchless as in litColor
            lr += mElr[i] * d;
            lg += mElg[i] * d;
            lb += mElb[i] * d;
            if (ndl <= 0f) {
                continue; // facing away from this light: no highlight either
            }
            // Half vector between light and view; specular = (N·H)^shininess.
            float hx = lx + vx, hy = ly + vy, hz = lz + vz;
            float hlen = (float) Math.sqrt((double) (hx * hx + hy * hy + hz * hz));
            if (hlen <= 0f) {
                continue;
            }
            float ndh = (nfx * hx + nfy * hy + nfz * hz) / hlen;
            if (ndh <= 0f) {
                continue;
            }
            float spec = (float) Math.pow((double) ndh, (double) mShininess) * mSpecStrength;
            sr += mElr[i] * spec;
            sg += mElg[i] * spec;
            sb += mElb[i] * spec;
        }
        int a = (base >>> 24) & 0xFF;
        int r = (int) (((base >>> 16) & 0xFF) * lr + sr * 255f);
        int g = (int) (((base >>> 8) & 0xFF) * lg + sg * 255f);
        int b = (int) ((base & 0xFF) * lb + sb * 255f);
        return (a << 24) | (clampChannel(r) << 16) | (clampChannel(g) << 8) | clampChannel(b);
    }

    /** {@code v} clamped to a color channel's 0..255 (negative only with negative lights). */
    private static int clampChannel(int v) {
        return v > 255 ? 255 : (v < 0 ? 0 : v);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Software engine: the engine bits of {@code mode} don't apply here (the host routes the
     * canvas engines to {@link #buildCanvasVertices} instead); the shading bit, the wireframe
     * bits, {@link #MODE_TRANSPARENT}, {@link #MODE_DOUBLE_SIDED} and {@link #MODE_FRONT_CW}
     * (which an infinite plane ignores) do. Other flags are ignored.
     */
    @Override
    public void drawMesh3D(int meshId, int mode) {
        Mesh m = mMeshes.get(meshId);
        if (m == null || mColor == null) {
            return;
        }
        int w = mWidth;
        int h = mHeight;
        int[] pixels = mColor;

        Matrix4.multiply(mPV, mProj, mView);
        Matrix4.multiply(mMV, mView, mModel);
        Matrix4.multiply(mMVP, mPV, mModel);
        prepareLightsEyeSpace();
        if (DEBUG) {
            debugCheckDraw(meshId, mode, m.mUv);
        }

        InfinitePlane plane = m.mPlane;
        if (plane != null) {
            // The plane handles every mode bit itself (wireframe, transparent, double-sided).
            int[] r = mPlaneRect;
            if (plane.rasterize(pixels, mZbuf, w, h, mMV, mProj, mode, mDepthBiasC, mDepthBiasS,
                    planeShadingUniform(), r, mPlaneShader)) {
                addDrawnRect(Math.max(r[0], 0), Math.max(r[1], 0), Math.min(r[2], w),
                        Math.min(r[3], h));
            }
            return;
        }

        int baseColor = mBaseColorArgb;

        int[] idx = m.mIndices;
        float[] verts = m.mVerts;
        float[] normals = m.mNormals;
        float[] uv = m.mUv;
        boolean doubleSided = (mode & MODE_DOUBLE_SIDED) != 0;
        boolean frontCw = (mode & MODE_FRONT_CW) != 0;
        boolean transparent = (mode & MODE_TRANSPARENT) != 0;
        if (DEBUG) {
            mDebugBackFaces = 0;
            mDebugNormals = 0;
            mDebugBackwardNormals = 0;
        }

        // Wireframe (hidden-line) style: depth-only prepass + depth-tested edge lines.
        if ((mode & Paint3DContext.MODE_WIREFRAME) != 0) {
            int drawn = drawMeshWireframe(idx, verts, normals, baseColor, w, h, pixels, mode);
            if (DEBUG) {
                debugCheckCulled(meshId, drawn);
            }
            return;
        }
        if (transparent && (baseColor >>> 24) == 0) {
            return; // blending a fully transparent color changes nothing, and writes no depth
        }

        // Smooth requested by mode, but only possible when the mesh carries normals.
        boolean smooth = (mode & Paint3DContext.MODE_SMOOTH_MASK) != 0 && normals != null;
        int[] texPixels = mTexPixels;
        boolean textured = texPixels != null && uv != null;
        // Glossy flat faces carry a per-vertex specular gradient → interpolate via Gouraud.
        boolean interpolate = smooth || mSpecStrength > 0f;

        float[] zbuf = mZbuf;
        float[] s = mTriScreen;
        int[] c = mTriColor;
        float[] tuv = mTriUv;
        float[] invW = mTriInvW;
        // Screen-space bounds of everything this draw touches.
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        int drawn = 0;
        for (int t = 0; t + 2 < idx.length; t += 3) {
            int n = projectTriangle(idx, verts, normals, textured ? uv : null, smooth, true,
                    textured, doubleSided, frontCw, t, baseColor, w, h);
            if (n == 0) {
                continue;
            }
            if (DEBUG) {
                drawn++;
            }
            for (int k = 0; k < n; k++) {
                int p = 9 * k;
                int q = 3 * k;
                if (transparent) {
                    // Separate fillers, so the opaque ones below stay exactly as they were.
                    fillTransparent(k, textured ? texPixels : null, w, h);
                } else if (textured) {
                    int r = 6 * k;
                    Rasterizer.fillTriangleTextured(
                            zbuf,
                            pixels,
                            texPixels,
                            mTexW,
                            mTexH,
                            c[q + 2],
                            c[q + 1],
                            c[q],
                            tuv[r + 4],
                            tuv[r + 5],
                            tuv[r + 2],
                            tuv[r + 3],
                            tuv[r],
                            tuv[r + 1],
                            invW[q + 2],
                            invW[q + 1],
                            invW[q],
                            w,
                            h,
                            s[p + 6],
                            s[p + 7],
                            s[p + 8],
                            s[p + 3],
                            s[p + 4],
                            s[p + 5],
                            s[p],
                            s[p + 1],
                            s[p + 2]);
                } else if (interpolate) {
                    Rasterizer.fillTriangleGouraud(
                            zbuf,
                            pixels,
                            c[q + 2],
                            c[q + 1],
                            c[q],
                            w,
                            h,
                            s[p + 6],
                            s[p + 7],
                            s[p + 8],
                            s[p + 3],
                            s[p + 4],
                            s[p + 5],
                            s[p],
                            s[p + 1],
                            s[p + 2]);
                } else {
                    Rasterizer.fillTriangle(
                            zbuf,
                            pixels,
                            c[q],
                            w,
                            h,
                            s[p + 6],
                            s[p + 7],
                            s[p + 8],
                            s[p + 3],
                            s[p + 4],
                            s[p + 5],
                            s[p],
                            s[p + 1],
                            s[p + 2]);
                }
            }
            minX = Math.min(minX, mTriMinX);
            minY = Math.min(minY, mTriMinY);
            maxX = Math.max(maxX, mTriMaxX);
            maxY = Math.max(maxY, mTriMaxY);
        }
        addDrawnBounds(minX, minY, maxX, maxY, w, h);
        if (DEBUG) {
            debugCheckCulled(meshId, drawn);
        }
    }

    /**
     * Blend returned triangle {@code k} of {@link #projectTriangle} src-over into the color
     * buffer, depth-tested but writing no depth ({@link #MODE_TRANSPARENT}), textured with
     * {@code tex} when it is not {@code null}. Same vertex order as the opaque fillers.
     */
    private void fillTransparent(int k, int @Nullable [] tex, int w, int h) {
        float[] s = mTriScreen;
        int[] c = mTriColor;
        int p = 9 * k;
        int q = 3 * k;
        if (tex != null) {
            float[] tuv = mTriUv;
            float[] invW = mTriInvW;
            int r = 6 * k;
            Rasterizer.fillTriangleTexturedBlend(mZbuf, mColor, tex, mTexW, mTexH,
                    c[q + 2], c[q + 1], c[q],
                    tuv[r + 4], tuv[r + 5], tuv[r + 2], tuv[r + 3], tuv[r], tuv[r + 1],
                    invW[q + 2], invW[q + 1], invW[q],
                    w, h,
                    s[p + 6], s[p + 7], s[p + 8],
                    s[p + 3], s[p + 4], s[p + 5],
                    s[p], s[p + 1], s[p + 2]);
        } else {
            Rasterizer.fillTriangleBlend(mZbuf, mColor,
                    c[q + 2], c[q + 1], c[q],
                    w, h,
                    s[p + 6], s[p + 7], s[p + 8],
                    s[p + 3], s[p + 4], s[p + 5],
                    s[p], s[p + 1], s[p + 2]);
        }
    }

    /**
     * Debug diagnostics of a draw's mode and mesh: flags this engine has no use for, and a
     * texture the mesh cannot take. Only called when {@link #DEBUG} is on.
     */
    private void debugCheckDraw(int meshId, int mode, float @Nullable [] uv) {
        if ((mode & (MODE_GPU | MODE_PBR)) != 0) {
            Utils.log("3D draw of mesh " + meshId + ": MODE_GPU/MODE_PBR have no effect here"
                    + " (mode 0x" + Integer.toHexString(mode) + ")");
        }
        if (mTexPixels != null && uv == null) {
            Utils.log("3D draw of mesh " + meshId + ": a texture is set but the mesh has no UV,"
                    + " so it is drawn untextured");
        }
    }

    /**
     * Debug diagnostics at the end of a draw: it drew nothing because every triangle in view was
     * back-facing, or most of the vertex normals its smooth shading used point to the back side
     * of their triangles. Only called when {@link #DEBUG} is on.
     */
    private void debugCheckCulled(int meshId, int drawn) {
        if (drawn == 0 && mDebugBackFaces > 0) {
            Utils.log("3D draw of mesh " + meshId + ": all " + mDebugBackFaces + " triangles in"
                    + " view are back-facing and culled; wrong winding? (CCW is front-facing"
                    + " unless MODE_FRONT_CW) Use MODE_FRONT_CW for a clockwise mesh, or"
                    + " MODE_DOUBLE_SIDED");
        }
        if (2 * mDebugBackwardNormals > mDebugNormals) {
            Utils.log("3D draw of mesh " + meshId + ": " + mDebugBackwardNormals + " of "
                    + mDebugNormals + " vertex normals point to the back side of their"
                    + " triangles; normals disagree with winding: flip normals, or use"
                    + " MODE_FRONT_CW if the mesh is wound clockwise");
        }
    }

    /**
     * Debug statistic behind the normals-vs-winding hint of {@link #debugCheckCulled}: counts
     * the vertex normals of source triangle {@code i0, i1, i2} (offsets into {@code verts} and
     * {@code normals}) and those of them pointing to its back side, the side the winding (with
     * {@code frontCw}) makes the back. Only called when {@link #DEBUG} is on.
     */
    private void debugCountBackwardNormals(int i0, int i1, int i2, float[] verts,
            float[] normals, boolean frontCw) {
        Matrix4.transformPoint(mMV, verts[i0], verts[i0 + 1], verts[i0 + 2], mScratch4);
        float x0 = mScratch4[0], y0 = mScratch4[1], z0 = mScratch4[2];
        Matrix4.transformPoint(mMV, verts[i1], verts[i1 + 1], verts[i1 + 2], mScratch4);
        float ax = mScratch4[0] - x0, ay = mScratch4[1] - y0, az = mScratch4[2] - z0;
        Matrix4.transformPoint(mMV, verts[i2], verts[i2 + 1], verts[i2 + 2], mScratch4);
        float bx = mScratch4[0] - x0, by = mScratch4[1] - y0, bz = mScratch4[2] - z0;
        // Normal of the front side: toward a viewer who sees the triangle counter-clockwise
        // (clockwise with MODE_FRONT_CW).
        float sign = frontCw ? -1f : 1f;
        float fx = sign * (ay * bz - az * by);
        float fy = sign * (az * bx - ax * bz);
        float fz = sign * (ax * by - ay * bx);
        for (int k = 0; k < 3; k++) {
            int i = k == 0 ? i0 : (k == 1 ? i1 : i2);
            Matrix4.transformDirection(mMV, normals[i], normals[i + 1], normals[i + 2],
                    mNormal3);
            mDebugNormals++;
            if (mNormal3[0] * fx + mNormal3[1] * fy + mNormal3[2] * fz < 0f) {
                mDebugBackwardNormals++;
            }
        }
    }

    /**
     * Window-z (in {@code [0,1]}) bias for wireframe edge lines: an edge sits exactly on its own
     * triangle's plane, so without a small camera-ward bias it would fail the depth test against
     * the depth its own face wrote. Large enough to clear that self-occlusion and float-rounding,
     * small enough that a clearly-nearer surface still hides the edges behind it.
     */
    private static final float WIRE_DEPTH_BIAS = 6e-4f;

    /**
     * Hidden-line wireframe ({@link Paint3DContext#MODE_WIREFRAME}). Two passes over the
     * front-facing triangles: (1) rasterize each into the depth buffer only (transparent faces),
     * laying down the solid's depth; (2) draw each triangle's three edges as depth-tested lines
     * (camera-ward biased), so edges on visible surface show and edges behind nearer surface are
     * occluded. Edge color is the current paint color, unlit. Back faces (the counter-clockwise
     * ones under {@link #MODE_FRONT_CW}) are culled by {@link #projectTriangle}, which is exactly
     * the hidden-line result for the visible surface; with {@link #MODE_DOUBLE_SIDED} both passes
     * keep them. With {@link #MODE_TRANSPARENT} there is no prepass (the draw writes no depth):
     * the lines are only hidden by what the scene already holds, and blend src-over, so an edge
     * shared by two triangles blends twice.
     *
     * <p>The {@code WIRE_EDGE*} bits in {@code mode} select which of each triangle's three edges to
     * draw — edge 0 = {@code a→b} (first), edge 1 = {@code b→c} (second), edge 2 = {@code c→a}
     * (third). With no edge bit set, all three are drawn (the classic full wireframe). A clipped
     * triangle draws what is left of its selected edges, never the edges clipping introduced.
     *
     * @return how many source triangles were drawn, for the debug diagnostics (0 unless
     *     {@link #DEBUG} is on)
     */
    private int drawMeshWireframe(
            int[] idx, float[] verts, float @Nullable [] normals, int lineColor, int w, int h,
            int[] pixels, int mode) {
        int edges = mode & Paint3DContext.WIRE_EDGE_MASK;
        if (edges == 0) {
            edges = Paint3DContext.WIRE_EDGE_MASK; // no selection → all three edges
        }
        boolean e0 = (edges & Paint3DContext.WIRE_EDGE0) != 0;
        boolean e1 = (edges & Paint3DContext.WIRE_EDGE1) != 0;
        boolean e2 = (edges & Paint3DContext.WIRE_EDGE2) != 0;
        boolean blend = (mode & MODE_TRANSPARENT) != 0;
        boolean doubleSided = (mode & MODE_DOUBLE_SIDED) != 0;
        boolean frontCw = (mode & MODE_FRONT_CW) != 0;
        if (blend && (lineColor >>> 24) == 0) {
            return 0; // blending a fully transparent color changes nothing
        }
        float[] zbuf = mZbuf;
        float[] s = mTriScreen;
        // Screen-space bounds of everything this draw touches: the triangles pass 1 fills, whose
        // edges pass 2 draws.
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        int drawn = 0;
        // Pass 1: depth prepass — fill the faces into the z-buffer, no color.
        for (int t = 0; !blend && t + 2 < idx.length; t += 3) {
            int n = projectTriangle(idx, verts, normals, null, false, false, false, doubleSided,
                    frontCw, t, lineColor, w, h);
            if (n == 0) {
                continue;
            }
            for (int k = 0; k < n; k++) {
                int p = 9 * k;
                Rasterizer.fillTriangleDepthOnly(
                        zbuf, w, h,
                        s[p + 6], s[p + 7], s[p + 8],
                        s[p + 3], s[p + 4], s[p + 5],
                        s[p], s[p + 1], s[p + 2]);
            }
        }
        // Pass 2: draw the selected edges of each face, depth-tested against the prepass.
        if (DEBUG) {
            mDebugBackFaces = 0; // counted by pass 1 already
        }
        for (int t = 0; t + 2 < idx.length; t += 3) {
            if (projectTriangle(idx, verts, normals, null, false, false, false, doubleSided,
                    frontCw, t, lineColor, w, h) == 0) {
                continue;
            }
            if (DEBUG) {
                drawn++;
            }
            minX = Math.min(minX, mTriMinX);
            minY = Math.min(minY, mTriMinY);
            maxX = Math.max(maxX, mTriMaxX);
            maxY = Math.max(maxY, mTriMaxY);
            if (!mTriClipped) {
                float ax = s[0], ay = s[1], az = s[2];
                float bx = s[3], by = s[4], bz = s[5];
                float cx = s[6], cy = s[7], cz = s[8];
                if (e0) {
                    drawWireEdge(zbuf, pixels, lineColor, blend, w, h, ax, ay, az, bx, by, bz);
                }
                if (e1) {
                    drawWireEdge(zbuf, pixels, lineColor, blend, w, h, bx, by, bz, cx, cy, cz);
                }
                if (e2) {
                    drawWireEdge(zbuf, pixels, lineColor, blend, w, h, cx, cy, cz, ax, ay, az);
                }
                continue;
            }
            // Clipped: what is left of the selected source edges, never the edges clipping added.
            float[] outline = mPolyScreen;
            int[] outlineEdge = mClipOutlineEdge;
            int count = mClipOutlineCount;
            for (int i = 0; i < count; i++) {
                int edge = outlineEdge[i];
                if (edge < 0 || !(edge == 0 ? e0 : edge == 1 ? e1 : e2)) {
                    continue;
                }
                int a = 3 * i;
                int b = i + 1 == count ? 0 : a + 3;
                drawLineInViewport(zbuf, pixels, lineColor, blend, w, h,
                        outline[a], outline[a + 1], outline[a + 2],
                        outline[b], outline[b + 1], outline[b + 2]);
            }
        }
        addDrawnBounds(minX, minY, maxX, maxY, w, h);
        return drawn;
    }

    /**
     * One wireframe edge: {@link Rasterizer#drawLineDepthTested}, or its src-over blending
     * variant when {@code blend} ({@link #MODE_TRANSPARENT}).
     */
    private static void drawWireLine(float[] zbuf, int[] pixels, int color, boolean blend,
            int w, int h, float x0, float y0, float z0, float x1, float y1, float z1) {
        if (blend) {
            Rasterizer.drawLineDepthTestedBlend(zbuf, pixels, color, w, h, x0, y0, z0, x1, y1,
                    z1, WIRE_DEPTH_BIAS);
        } else {
            Rasterizer.drawLineDepthTested(zbuf, pixels, color, w, h, x0, y0, z0, x1, y1, z1,
                    WIRE_DEPTH_BIAS);
        }
    }

    /**
     * {@link #drawWireLine} for an edge of an unclipped triangle. Such a triangle lies inside
     * the guard band, which reaches far past the viewport, so an edge with an end outside the
     * viewport (plus a pixel) goes through {@link #drawLineInViewport}; any other edge is drawn
     * as is.
     */
    private static void drawWireEdge(float[] zbuf, int[] pixels, int color, boolean blend,
            int w, int h, float x0, float y0, float z0, float x1, float y1, float z1) {
        if (x0 >= -1f && x0 <= w + 1f && y0 >= -1f && y0 <= h + 1f
                && x1 >= -1f && x1 <= w + 1f && y1 >= -1f && y1 <= h + 1f) {
            drawWireLine(zbuf, pixels, color, blend, w, h, x0, y0, z0, x1, y1, z1);
        } else {
            drawLineInViewport(zbuf, pixels, color, blend, w, h, x0, y0, z0, x1, y1, z1);
        }
    }

    /**
     * {@link #drawWireLine} for an edge that can reach out to the guard band, far past the
     * viewport (an edge of a clipped triangle, or see {@link #drawWireEdge}): the edge is first
     * trimmed to the viewport (plus a pixel), keeping the cost of the line proportional to its
     * visible length.
     */
    private static void drawLineInViewport(float[] zbuf, int[] pixels, int color, boolean blend,
            int w, int h, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float t0 = 0f;
        float t1 = 1f;
        if (dx != 0f) {
            float ta = (-1f - x0) / dx;
            float tb = (w + 1f - x0) / dx;
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
        } else if (!(x0 >= -1f && x0 <= w + 1f)) {
            return;
        }
        if (dy != 0f) {
            float ta = (-1f - y0) / dy;
            float tb = (h + 1f - y0) / dy;
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
        } else if (!(y0 >= -1f && y0 <= h + 1f)) {
            return;
        }
        if (!(t0 <= t1)) {
            return; // misses the viewport
        }
        float dz = z1 - z0;
        drawWireLine(zbuf, pixels, color, blend, w, h,
                x0 + t0 * dx, y0 + t0 * dy, z0 + t0 * dz,
                x0 + t1 * dx, y0 + t1 * dy, z0 + t1 * dz);
    }

    /**
     * Project, clip, backface-cull and optionally shade the triangle {@code (idx[t], idx[t+1],
     * idx[t+2])}, writing the screen-space triangles it becomes into the {@code mTri*} scratch
     * arrays, and return how many there are.
     *
     * <p>A triangle in front of the near plane and inside the guard band (nearly every triangle)
     * takes the fast path: it is projected exactly as before clipping existed and yields one
     * triangle. Any other triangle is clipped by {@link #clipAndProject} and yields up to
     * {@link #MAX_CLIPPED_TRIANGLES}. Returns 0 when nothing is left to draw: indices out of
     * range, clipped away, back-facing (unless {@code doubleSided}; clockwise on screen, or
     * counter-clockwise with {@code frontCw}), zero area, or entirely outside the viewport.
     *
     * <p>For returned triangle {@code k}, {@link #mTriScreen}{@code [9k, 9k+9)} holds
     * {@code sx,sy,sz} for each of its vertices (Y already flipped for a top-down pixel buffer,
     * depth bias applied). With {@code computeColor}, {@link #mTriColor}{@code [3k, 3k+3)} holds
     * their shaded ARGB: when {@code smooth} is requested and {@code normals} are present each
     * vertex is shaded by its own eye-space normal (smooth/Gouraud), reversed on a back-facing
     * triangle, otherwise one flat per-face normal, facing the viewer, shades all three
     * identically. With {@code computeInvW}, {@link #mTriInvW}{@code [3k, 3k+3)} holds their
     * {@code 1/w}; with {@code uv}, {@link #mTriUv}{@code [6k, 6k+6)} their texture coordinates.
     * {@link #mTriMinX} etc. bound the screen positions of the triangles and of the outline of
     * what is left of the source triangle (see {@link #mTriClipped} for where that outline is).
     */
    private int projectTriangle(
            int[] idx, float[] verts, float @Nullable [] normals, float @Nullable [] uv,
            boolean smooth, boolean computeColor, boolean computeInvW, boolean doubleSided,
            boolean frontCw, int t, int baseColor, int w, int h) {
        int i0 = idx[t] * 3;
        int i1 = idx[t + 1] * 3;
        int i2 = idx[t + 2] * 3;
        if (i0 < 0 || i1 < 0 || i2 < 0
                || i0 + 2 >= verts.length
                || i1 + 2 >= verts.length
                || i2 + 2 >= verts.length) {
            return 0;
        }

        Matrix4.transformPoint(mMVP, verts[i0], verts[i0 + 1], verts[i0 + 2], mScratch4);
        float cx0 = mScratch4[0], cy0 = mScratch4[1], cz0 = mScratch4[2], cw0 = mScratch4[3];
        Matrix4.transformPoint(mMVP, verts[i1], verts[i1 + 1], verts[i1 + 2], mScratch4);
        float cx1 = mScratch4[0], cy1 = mScratch4[1], cz1 = mScratch4[2], cw1 = mScratch4[3];
        Matrix4.transformPoint(mMVP, verts[i2], verts[i2 + 1], verts[i2 + 2], mScratch4);
        float cx2 = mScratch4[0], cy2 = mScratch4[1], cz2 = mScratch4[2], cw2 = mScratch4[3];

        // Fast path: every vertex in front of the near plane (NaN fails these tests too).
        if (cw0 >= NEAR_W_EPSILON && cw1 >= NEAR_W_EPSILON && cw2 >= NEAR_W_EPSILON
                && cz0 + cw0 >= 0f && cz1 + cw1 >= 0f && cz2 + cw2 >= 0f) {
            float nx0 = cx0 / cw0, ny0 = cy0 / cw0, nz0 = cz0 / cw0;
            float nx1 = cx1 / cw1, ny1 = cy1 / cw1, nz1 = cz1 / cw1;
            float nx2 = cx2 / cw2, ny2 = cy2 / cw2, nz2 = cz2 / cw2;

            float sx0 = (nx0 * 0.5f + 0.5f) * w;
            float sy0 = (1f - (ny0 * 0.5f + 0.5f)) * h;
            float sz0 = nz0 * 0.5f + 0.5f;
            float sx1 = (nx1 * 0.5f + 0.5f) * w;
            float sy1 = (1f - (ny1 * 0.5f + 0.5f)) * h;
            float sz1 = nz1 * 0.5f + 0.5f;
            float sx2 = (nx2 * 0.5f + 0.5f) * w;
            float sy2 = (1f - (ny2 * 0.5f + 0.5f)) * h;
            float sz2 = nz2 * 0.5f + 0.5f;

            // Screen bounds, branch-free (Math.min/max also propagate NaN, failing the tests).
            float minX = Math.min(sx0, Math.min(sx1, sx2));
            float maxX = Math.max(sx0, Math.max(sx1, sx2));
            float minY = Math.min(sy0, Math.min(sy1, sy2));
            float maxY = Math.max(sy0, Math.max(sy1, sy2));
            if (minX >= -GUARD_BAND_PX && maxX <= w + GUARD_BAND_PX
                    && minY >= -GUARD_BAND_PX && maxY <= h + GUARD_BAND_PX) {
                // Backface cull in screen space (Y down). CCW-in-NDC → CW after Y-flip, so a
                // positive signed area means the triangle is back-facing: culled, unless
                // double-sided. MODE_FRONT_CW swaps the sides (one negation per triangle). A
                // zero-area triangle covers nothing and is dropped too.
                float signedArea = (sx1 - sx0) * (sy2 - sy0) - (sx2 - sx0) * (sy1 - sy0);
                float facing = frontCw ? -signedArea : signedArea;
                boolean backFacing = false;
                if (!(facing < 0f)) {
                    if (doubleSided && facing > 0f) {
                        backFacing = true;
                    } else {
                        if (DEBUG && facing > 0f
                                && !(maxX < -1f || minX > w || maxY < -1f || minY > h)) {
                            mDebugBackFaces++;
                        }
                        return 0;
                    }
                }
                if (maxX < -1f || minX > w || maxY < -1f || minY > h) {
                    return 0; // entirely outside the viewport
                }
                mTriMinX = minX;
                mTriMinY = minY;
                mTriMaxX = maxX;
                mTriMaxY = maxY;

                // Polygon-offset depth bias: shift all three vertices' depth by the same amount
                // (which is exactly a shift of the triangle's z-plane constant — no per-pixel
                // cost), so content can deterministically order near-coincident surfaces and
                // avoid z-fighting. Slope term (glPolygonOffset-style) scales with the
                // window-space depth gradient. Skipped entirely when no bias is set, so default
                // rendering is unchanged and un-slowed.
                if (mDepthBiasC != 0f || mDepthBiasS != 0f) {
                    float invA = 1f / signedArea;
                    float dzdx = ((sz1 - sz0) * (sy2 - sy0) - (sz2 - sz0) * (sy1 - sy0)) * invA;
                    float dzdy = ((sz2 - sz0) * (sx1 - sx0) - (sz1 - sz0) * (sx2 - sx0)) * invA;
                    float slope = Math.max(Math.abs(dzdx), Math.abs(dzdy));
                    float bias = mDepthBiasC + mDepthBiasS * slope;
                    sz0 += bias;
                    sz1 += bias;
                    sz2 += bias;
                }

                mTriScreen[0] = sx0;
                mTriScreen[1] = sy0;
                mTriScreen[2] = sz0;
                mTriScreen[3] = sx1;
                mTriScreen[4] = sy1;
                mTriScreen[5] = sz1;
                mTriScreen[6] = sx2;
                mTriScreen[7] = sy2;
                mTriScreen[8] = sz2;
                if (computeInvW) {
                    // Only the textured path needs 1/w; skip these divisions otherwise.
                    mTriInvW[0] = 1f / cw0;
                    mTriInvW[1] = 1f / cw1;
                    mTriInvW[2] = 1f / cw2;
                }
                if (uv != null) {
                    int q0 = idx[t] * 2, q1 = idx[t + 1] * 2, q2 = idx[t + 2] * 2;
                    mTriUv[0] = uvAt(uv, q0);
                    mTriUv[1] = uvAt(uv, q0 + 1);
                    mTriUv[2] = uvAt(uv, q1);
                    mTriUv[3] = uvAt(uv, q1 + 1);
                    mTriUv[4] = uvAt(uv, q2);
                    mTriUv[5] = uvAt(uv, q2 + 1);
                }
                if (computeColor) {
                    shadeVertices(i0, i1, i2, verts, normals, smooth, backFacing, frontCw,
                            baseColor, mTriColor);
                }
                mTriClipped = false;
                return 1;
            }
        }

        float[] p = mPolyA;
        p[0] = cx0;
        p[1] = cy0;
        p[2] = cz0;
        p[3] = cw0;
        p[4] = 1f;
        p[5] = 0f;
        p[6] = 0f;
        p[7] = cx1;
        p[8] = cy1;
        p[9] = cz1;
        p[10] = cw1;
        p[11] = 0f;
        p[12] = 1f;
        p[13] = 0f;
        p[14] = cx2;
        p[15] = cy2;
        p[16] = cz2;
        p[17] = cw2;
        p[18] = 0f;
        p[19] = 0f;
        p[20] = 1f;
        return clipAndProject(idx[t], idx[t + 1], idx[t + 2], verts, normals, uv, smooth,
                computeColor, computeInvW, doubleSided, frontCw, baseColor, w, h);
    }

    /**
     * The slow path of {@link #projectTriangle}, for a triangle crossing the near plane or
     * reaching past the guard band; its clip-space vertices are in {@link #mPolyA}.
     *
     * <p>Clips the triangle (Sutherland–Hodgman, in homogeneous clip space) against the near
     * plane {@code z ≥ −w}, the {@code w} floor and the guard band, carrying each vertex's
     * barycentric weights so colors, UVs and {@code 1/w} interpolate correctly; projects the
     * remaining convex polygon and fans it into triangles. An edge crossing a plane is always
     * split from its inside end, so triangles sharing that edge get bit-identical new vertices
     * and no cracks open between them.
     */
    private int clipAndProject(
            int v0, int v1, int v2, float[] verts, float @Nullable [] normals,
            float @Nullable [] uv, boolean smooth, boolean computeColor, boolean computeInvW,
            boolean doubleSided, boolean frontCw, int baseColor, int w, int h) {
        float[] src = mPolyA;
        for (int o = 0; o < 3 * CLIP_STRIDE; o += CLIP_STRIDE) {
            if (!isFinite(src[o]) || !isFinite(src[o + 1]) || !isFinite(src[o + 2])
                    || !isFinite(src[o + 3])) {
                return 0;
            }
        }
        float[] dst = mPolyB;
        int[] srcEdge = mPolyEdgeA;
        int[] dstEdge = mPolyEdgeB;
        srcEdge[0] = 0;
        srcEdge[1] = 1;
        srcEdge[2] = 2;
        float[] dist = mPolyDist;
        float gx = 1f + 2f * GUARD_BAND_PX / w;
        float gy = 1f + 2f * GUARD_BAND_PX / h;
        int n = 3;
        for (int plane = 0; plane < CLIP_PLANE_COUNT; plane++) {
            boolean anyIn = false;
            boolean anyOut = false;
            for (int i = 0, o = 0; i < n; i++, o += CLIP_STRIDE) {
                float d = clipDistance(plane, src[o], src[o + 1], src[o + 2], src[o + 3], gx, gy);
                if (d >= 0f) {
                    anyIn = true;
                } else if (d < 0f) {
                    anyOut = true;
                } else {
                    return 0; // NaN: overflow in the clip-space input
                }
                dist[i] = d;
            }
            if (!anyOut) {
                continue;
            }
            if (!anyIn) {
                return 0;
            }
            // Edge i runs from vertex i to vertex i + 1; its source edge (or -1) follows it
            // into the clipped polygon. A vertex exactly on the plane is kept as is.
            int m = 0;
            for (int i = 0; i < n; i++) {
                int j = i + 1 == n ? 0 : i + 1;
                float di = dist[i];
                float dj = dist[j];
                if (di >= 0f) {
                    if (m == MAX_POLYGON_VERTICES) {
                        return 0; // unreachable for a convex polygon
                    }
                    copyClipVertex(src, i, dst, m);
                    if (dj >= 0f) {
                        dstEdge[m++] = srcEdge[i];
                    } else if (di == 0f) {
                        dstEdge[m++] = -1; // leaves along the plane
                    } else {
                        dstEdge[m++] = srcEdge[i];
                        if (m == MAX_POLYGON_VERTICES) {
                            return 0;
                        }
                        clipVertex(src, i, j, di / (di - dj), dst, m);
                        dstEdge[m++] = -1;
                    }
                } else if (dj > 0f) {
                    if (m == MAX_POLYGON_VERTICES) {
                        return 0;
                    }
                    clipVertex(src, j, i, dj / (dj - di), dst, m);
                    dstEdge[m++] = srcEdge[i];
                }
            }
            if (m < 3) {
                return 0;
            }
            n = m;
            float[] swap = src;
            src = dst;
            dst = swap;
            int[] swapEdge = srcEdge;
            srcEdge = dstEdge;
            dstEdge = swapEdge;
        }

        // Project (with exactly the fast path's arithmetic, so an unclipped vertex lands on
        // the same pixel position as in a neighboring unclipped triangle).
        float[] ps = mPolyScreen;
        float limit = 2f * GUARD_BAND_PX;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0, o = 0, q = 0; i < n; i++, o += CLIP_STRIDE, q += 3) {
            float cw = src[o + 3];
            float sx = (src[o] / cw * 0.5f + 0.5f) * w;
            float sy = (1f - (src[o + 1] / cw * 0.5f + 0.5f)) * h;
            float sz = src[o + 2] / cw * 0.5f + 0.5f;
            if (!(sx >= -limit && sx <= w + limit && sy >= -limit && sy <= h + limit)
                    || !isFinite(sz)) {
                return 0; // only reachable through overflow in the clip-space input
            }
            ps[q] = sx;
            ps[q + 1] = sy;
            ps[q + 2] = sz;
            minX = Math.min(minX, sx);
            minY = Math.min(minY, sy);
            maxX = Math.max(maxX, sx);
            maxY = Math.max(maxY, sy);
        }
        int end = 3 * n;
        float sx0 = ps[0];
        float sy0 = ps[1];
        float area = 0f;
        for (int q = 3; q + 3 < end; q += 3) {
            area += (ps[q] - sx0) * (ps[q + 4] - sy0) - (ps[q + 3] - sx0) * (ps[q + 1] - sy0);
        }
        // Back-facing (same convention as the fast path, MODE_FRONT_CW included) and not
        // double-sided, zero area, or outside the viewport.
        float facing = frontCw ? -area : area;
        boolean backFacing = false;
        if (!(facing < 0f)) {
            if (doubleSided && facing > 0f) {
                backFacing = true;
            } else {
                if (DEBUG && facing > 0f
                        && !(maxX < -1f || minX > w || maxY < -1f || minY > h)) {
                    mDebugBackFaces++;
                }
                return 0;
            }
        }
        if (maxX < -1f || minX > w || maxY < -1f || minY > h) {
            return 0;
        }
        mTriMinX = minX;
        mTriMinY = minY;
        mTriMaxX = maxX;
        mTriMaxY = maxY;
        if (mDepthBiasC != 0f || mDepthBiasS != 0f) {
            // As in the fast path, with the depth gradient of the whole (planar) polygon.
            float sz0 = ps[2];
            float gzx = 0f;
            float gzy = 0f;
            for (int q = 3; q + 3 < end; q += 3) {
                float ax = ps[q] - sx0, ay = ps[q + 1] - sy0, az = ps[q + 2] - sz0;
                float bx = ps[q + 3] - sx0, by = ps[q + 4] - sy0, bz = ps[q + 5] - sz0;
                gzx += az * by - bz * ay;
                gzy += bz * ax - az * bx;
            }
            float invA = 1f / area;
            float slope = Math.max(Math.abs(gzx * invA), Math.abs(gzy * invA));
            float bias = mDepthBiasC + mDepthBiasS * slope;
            for (int q = 2; q < end; q += 3) {
                ps[q] += bias;
            }
        }

        // Per polygon vertex attributes, from the source vertices by barycentric weight.
        if (computeColor) {
            shadeVertices(v0 * 3, v1 * 3, v2 * 3, verts, normals, smooth, backFacing, frontCw,
                    baseColor, mVertexColor);
            int c0 = mVertexColor[0];
            int c1 = mVertexColor[1];
            int c2 = mVertexColor[2];
            boolean flat = c0 == c1 && c1 == c2;
            for (int i = 0, o = 0; i < n; i++, o += CLIP_STRIDE) {
                mPolyColor[i] = flat ? c0
                        : blendColor(src[o + 4], src[o + 5], src[o + 6], c0, c1, c2);
            }
        }
        if (computeInvW) {
            for (int i = 0, o = 0; i < n; i++, o += CLIP_STRIDE) {
                mPolyInvW[i] = 1f / src[o + 3];
            }
        }
        if (uv != null) {
            float ua = uvAt(uv, v0 * 2), va = uvAt(uv, v0 * 2 + 1);
            float ub = uvAt(uv, v1 * 2), vb = uvAt(uv, v1 * 2 + 1);
            float uc = uvAt(uv, v2 * 2), vc = uvAt(uv, v2 * 2 + 1);
            for (int i = 0, o = 0; i < n; i++, o += CLIP_STRIDE) {
                float b0 = src[o + 4], b1 = src[o + 5], b2 = src[o + 6];
                mPolyUv[2 * i] = b0 * ua + b1 * ub + b2 * uc;
                mPolyUv[2 * i + 1] = b0 * va + b1 * vb + b2 * vc;
            }
        }

        // Fan (v0, vk, vk+1), skipping slivers that clipping left with no area (or the other
        // on-screen winding than the polygon's).
        boolean screenCw = area > 0f;
        int count = 0;
        for (int k = 1; k + 1 < n; k++) {
            int qb = 3 * k;
            int qc = qb + 3;
            float subArea =
                    (ps[qb] - sx0) * (ps[qc + 1] - sy0) - (ps[qc] - sx0) * (ps[qb + 1] - sy0);
            if (screenCw ? !(subArea > 0f) : !(subArea < 0f)) {
                continue;
            }
            int so = 9 * count;
            mTriScreen[so] = ps[0];
            mTriScreen[so + 1] = ps[1];
            mTriScreen[so + 2] = ps[2];
            mTriScreen[so + 3] = ps[qb];
            mTriScreen[so + 4] = ps[qb + 1];
            mTriScreen[so + 5] = ps[qb + 2];
            mTriScreen[so + 6] = ps[qc];
            mTriScreen[so + 7] = ps[qc + 1];
            mTriScreen[so + 8] = ps[qc + 2];
            int co = 3 * count;
            if (computeColor) {
                mTriColor[co] = mPolyColor[0];
                mTriColor[co + 1] = mPolyColor[k];
                mTriColor[co + 2] = mPolyColor[k + 1];
            }
            if (computeInvW) {
                mTriInvW[co] = mPolyInvW[0];
                mTriInvW[co + 1] = mPolyInvW[k];
                mTriInvW[co + 2] = mPolyInvW[k + 1];
            }
            if (uv != null) {
                int uo = 6 * count;
                mTriUv[uo] = mPolyUv[0];
                mTriUv[uo + 1] = mPolyUv[1];
                mTriUv[uo + 2] = mPolyUv[2 * k];
                mTriUv[uo + 3] = mPolyUv[2 * k + 1];
                mTriUv[uo + 4] = mPolyUv[2 * k + 2];
                mTriUv[uo + 5] = mPolyUv[2 * k + 3];
            }
            count++;
        }
        mTriClipped = true;
        mClipOutlineEdge = srcEdge;
        mClipOutlineCount = n;
        return count;
    }

    /**
     * Signed distance of clip-space point {@code (x, y, z, w)} from clip plane {@code plane},
     * non-negative inside: the near plane, the {@code w} floor, then the guard band's right,
     * left, top and bottom planes ({@code |x| ≤ gx·w}, {@code |y| ≤ gy·w}).
     */
    private static float clipDistance(
            int plane, float x, float y, float z, float w, float gx, float gy) {
        switch (plane) {
            case 0:
                return z + w;
            case 1:
                return w - NEAR_W_EPSILON;
            case 2:
                return gx * w - x;
            case 3:
                return gx * w + x;
            case 4:
                return gy * w - y;
            default:
                return gy * w + y;
        }
    }

    private static void copyClipVertex(float[] src, int i, float[] dst, int m) {
        System.arraycopy(src, i * CLIP_STRIDE, dst, m * CLIP_STRIDE, CLIP_STRIDE);
    }

    /** Write {@code src[in] + t × (src[out] − src[in])} (every attribute) to {@code dst[m]}. */
    private static void clipVertex(float[] src, int in, int out, float t, float[] dst, int m) {
        int a = in * CLIP_STRIDE;
        int b = out * CLIP_STRIDE;
        int o = m * CLIP_STRIDE;
        for (int k = 0; k < CLIP_STRIDE; k++) {
            float va = src[a + k];
            dst[o + k] = va + t * (src[b + k] - va);
        }
    }

    /**
     * Shade source vertices {@code i0, i1, i2} (offsets into {@code verts}) into
     * {@code out[0..2]} with the lighting model's {@code N'}: each vertex by its own eye-space
     * normal when {@code smooth} and the normals cover all three (smooth/Gouraud), reversed on a
     * {@code backFacing} triangle (drawn only when double-sided); otherwise all three by the
     * flat face normal, which always faces the viewer, whichever winding is front-facing
     * ({@code frontCw}: {@link #MODE_FRONT_CW}).
     */
    private void shadeVertices(int i0, int i1, int i2, float[] verts,
            float @Nullable [] normals, boolean smooth, boolean backFacing, boolean frontCw,
            int baseColor, int[] out) {
        // Eye-space vertex positions: needed for point-light direction and the flat face
        // normal.
        Matrix4.transformPoint(mMV, verts[i0], verts[i0 + 1], verts[i0 + 2], mScratch4);
        float ev0x = mScratch4[0], ev0y = mScratch4[1], ev0z = mScratch4[2];
        Matrix4.transformPoint(mMV, verts[i1], verts[i1 + 1], verts[i1 + 2], mScratch4);
        float ev1x = mScratch4[0], ev1y = mScratch4[1], ev1z = mScratch4[2];
        Matrix4.transformPoint(mMV, verts[i2], verts[i2 + 1], verts[i2 + 2], mScratch4);
        float ev2x = mScratch4[0], ev2y = mScratch4[1], ev2z = mScratch4[2];

        if (smooth
                && normals != null
                && i0 + 2 < normals.length
                && i1 + 2 < normals.length
                && i2 + 2 < normals.length) {
            if (DEBUG) {
                debugCountBackwardNormals(i0, i1, i2, verts, normals, frontCw);
            }
            // Smooth (Gouraud): light each vertex by its own eye-space normal and position.
            Matrix4.transformDirection(mMV, normals[i0], normals[i0 + 1], normals[i0 + 2],
                    mNormal3);
            out[0] = litColorNormal3(baseColor, backFacing, ev0x, ev0y, ev0z);
            Matrix4.transformDirection(mMV, normals[i1], normals[i1 + 1], normals[i1 + 2],
                    mNormal3);
            out[1] = litColorNormal3(baseColor, backFacing, ev1x, ev1y, ev1z);
            Matrix4.transformDirection(mMV, normals[i2], normals[i2 + 1], normals[i2 + 2],
                    mNormal3);
            out[2] = litColorNormal3(baseColor, backFacing, ev2x, ev2y, ev2z);
        } else {
            // Flat: one eye-space face normal (cross product of two edges) and the centroid
            // position light all three vertices identically.
            float ax = ev1x - ev0x, ay = ev1y - ev0y, az = ev1z - ev0z;
            float bx = ev2x - ev0x, by = ev2y - ev0y, bz = ev2z - ev0z;
            float nxn = ay * bz - az * by;
            float nyn = az * bx - ax * bz;
            float nzn = ax * by - ay * bx;
            if (backFacing != frontCw) {
                // The cross product follows the winding: it points away from the viewer when
                // the triangle is clockwise on screen (a back face, or a front face under
                // MODE_FRONT_CW). Turn it to face the viewer.
                nxn = -nxn;
                nyn = -nyn;
                nzn = -nzn;
            }
            if (mSpecStrength > 0f) {
                // Glossy: keep the flat face normal but light each vertex at its own eye
                // position, so the view-dependent specular highlight gradates across the face
                // (the Gouraud filler interpolates the three colors).
                out[0] = litColor(baseColor, nxn, nyn, nzn, ev0x, ev0y, ev0z);
                out[1] = litColor(baseColor, nxn, nyn, nzn, ev1x, ev1y, ev1z);
                out[2] = litColor(baseColor, nxn, nyn, nzn, ev2x, ev2y, ev2z);
            } else {
                float cx = (ev0x + ev1x + ev2x) * (1f / 3f);
                float cy = (ev0y + ev1y + ev2y) * (1f / 3f);
                float cz = (ev0z + ev1z + ev2z) * (1f / 3f);
                int shaded = litColor(baseColor, nxn, nyn, nzn, cx, cy, cz);
                out[0] = shaded;
                out[1] = shaded;
                out[2] = shaded;
            }
        }
    }

    /** {@link #litColor} with the eye-space normal in {@link #mNormal3}, reversed if asked. */
    private int litColorNormal3(int base, boolean reverse, float px, float py, float pz) {
        float[] n = mNormal3;
        return reverse
                ? litColor(base, -n[0], -n[1], -n[2], px, py, pz)
                : litColor(base, n[0], n[1], n[2], px, py, pz);
    }

    /** {@code b0·c0 + b1·c1 + b2·c2} per ARGB channel, rounded and clamped to 0..255. */
    private static int blendColor(float b0, float b1, float b2, int c0, int c1, int c2) {
        int a = blendChannel(b0, b1, b2, c0 >>> 24, c1 >>> 24, c2 >>> 24);
        int r = blendChannel(b0, b1, b2, (c0 >>> 16) & 0xFF, (c1 >>> 16) & 0xFF,
                (c2 >>> 16) & 0xFF);
        int g = blendChannel(b0, b1, b2, (c0 >>> 8) & 0xFF, (c1 >>> 8) & 0xFF,
                (c2 >>> 8) & 0xFF);
        int b = blendChannel(b0, b1, b2, c0 & 0xFF, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendChannel(float b0, float b1, float b2, int v0, int v1, int v2) {
        int v = (int) (b0 * v0 + b1 * v1 + b2 * v2 + 0.5f);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** {@code uv[i]}, or 0 when the mesh's UV array is too short. */
    private static float uvAt(float[] uv, int i) {
        return i >= 0 && i < uv.length ? uv[i] : 0f;
    }

    private static boolean isFinite(float v) {
        return Math.abs(v) <= Float.MAX_VALUE;
    }

    /**
     * Project a mesh into a screen-space, flat-shaded triangle soup suitable for
     * {@code Canvas.drawVertices(TRIANGLES, …)} (the canvas-vertices path).
     *
     * <p>Triangles are projected, clipped, backface-culled and shaded exactly as in
     * {@link #drawMesh3D}, then sorted back-to-front (painter's algorithm) since the
     * canvas path has no depth buffer. Each kept triangle contributes three <b>unshared</b>
     * vertices carrying their own per-vertex colors, so {@code Canvas.drawVertices} reproduces
     * flat per-face shading (equal colors) or smooth Gouraud shading (distinct colors, from
     * per-vertex normals) by interpolating them. The result is written into {@code out};
     * positions are in the same canvas pixel space the software path blits into.
     *
     * @param mode the draw's mode: its shading bit, {@link #MODE_DOUBLE_SIDED} and
     *     {@link #MODE_FRONT_CW} apply (the caller has chosen the engine; the vertex colors carry
     *     the paint alpha, which the canvas blends, so {@link #MODE_TRANSPARENT} needs nothing
     *     here)
     * @return the number of vertices written ({@code out.vertexCount}, a multiple of 3)
     */
    public int buildCanvasVertices(int meshId, @NonNull CanvasMesh out, int mode) {
        out.mVertexCount = 0;
        Mesh m = mMeshes.get(meshId);
        if (m == null || m.mPlane != null) {
            return 0; // a plane is drawn with buildCanvasPlane() instead
        }
        int w = mWidth;
        int h = mHeight;

        Matrix4.multiply(mPV, mProj, mView);
        Matrix4.multiply(mMV, mView, mModel);
        Matrix4.multiply(mMVP, mPV, mModel);
        prepareLightsEyeSpace();

        int baseColor = mBaseColorArgb;
        int[] idx = m.mIndices;
        float[] verts = m.mVerts;
        float[] normals = m.mNormals;
        float[] uv = m.mUv;
        boolean useSmooth = (mode & MODE_SMOOTH_MASK) != 0 && normals != null;
        boolean doubleSided = (mode & MODE_DOUBLE_SIDED) != 0;
        boolean frontCw = (mode & MODE_FRONT_CW) != 0;
        out.mHasUv = uv != null;
        if (DEBUG) {
            debugCheckDraw(meshId, mode, uv);
            mDebugBackFaces = 0;
            mDebugNormals = 0;
            mDebugBackwardNormals = 0;
        }

        int triCount = idx.length / 3;
        out.ensureTriangleCapacity(triCount);

        int kept = 0;
        for (int t = 0; t + 2 < idx.length; t += 3) {
            int n = projectTriangle(idx, verts, normals, uv, useSmooth, true, false, doubleSided,
                    frontCw, t, baseColor, w, h);
            if (n == 0) {
                continue;
            }
            if (kept + n > out.mTriDepth.length) {
                // Clipping split a triangle: make room, for the rest of the mesh too.
                int needed = kept + n + (idx.length - t) / 3;
                out.ensureTriangleCapacity(Math.max(needed, 2 * out.mTriDepth.length));
            }
            for (int k = 0; k < n; k++) {
                int s = 9 * k;
                int p = kept * 6;
                out.mTriXY[p] = mTriScreen[s];
                out.mTriXY[p + 1] = mTriScreen[s + 1];
                out.mTriXY[p + 2] = mTriScreen[s + 3];
                out.mTriXY[p + 3] = mTriScreen[s + 4];
                out.mTriXY[p + 4] = mTriScreen[s + 6];
                out.mTriXY[p + 5] = mTriScreen[s + 7];
                int c = kept * 3;
                int q = 3 * k;
                out.mTriColor[c] = mTriColor[q];
                out.mTriColor[c + 1] = mTriColor[q + 1];
                out.mTriColor[c + 2] = mTriColor[q + 2];
                if (uv != null) {
                    int r = 6 * k;
                    out.mTriUv[p] = mTriUv[r];
                    out.mTriUv[p + 1] = mTriUv[r + 1];
                    out.mTriUv[p + 2] = mTriUv[r + 2];
                    out.mTriUv[p + 3] = mTriUv[r + 3];
                    out.mTriUv[p + 4] = mTriUv[r + 4];
                    out.mTriUv[p + 5] = mTriUv[r + 5];
                }
                // Painter's key: mean screen depth (larger = farther, drawn first).
                out.mTriDepth[kept] =
                        (mTriScreen[s + 2] + mTriScreen[s + 5] + mTriScreen[s + 8]) * (1f / 3f);
                int zi = kept * 3;
                out.mTriZ[zi]     = mTriScreen[s + 2]; // sz0 — window-z vertex 0
                out.mTriZ[zi + 1] = mTriScreen[s + 5]; // sz1 — window-z vertex 1
                out.mTriZ[zi + 2] = mTriScreen[s + 8]; // sz2 — window-z vertex 2
                kept++;
            }
        }
        if (DEBUG) {
            debugCheckCulled(meshId, kept);
        }

        sortTrianglesBackToFront(out, kept);

        out.ensureVertexCapacity(kept * 3);
        float[] pos = out.mPositions;
        int[] col = out.mColors;
        float[] uvs = out.mUvs;
        for (int k = 0; k < kept; k++) {
            int tri = out.mOrder[k];
            int src = tri * 6;
            int dst = k * 6;
            pos[dst] = out.mTriXY[src];
            pos[dst + 1] = out.mTriXY[src + 1];
            pos[dst + 2] = out.mTriXY[src + 2];
            pos[dst + 3] = out.mTriXY[src + 3];
            pos[dst + 4] = out.mTriXY[src + 4];
            pos[dst + 5] = out.mTriXY[src + 5];
            int srcC = tri * 3;
            int v = k * 3;
            col[v] = out.mTriColor[srcC];
            col[v + 1] = out.mTriColor[srcC + 1];
            col[v + 2] = out.mTriColor[srcC + 2];
            if (uv != null) {
                uvs[dst] = out.mTriUv[src];
                uvs[dst + 1] = out.mTriUv[src + 1];
                uvs[dst + 2] = out.mTriUv[src + 2];
                uvs[dst + 3] = out.mTriUv[src + 3];
                uvs[dst + 4] = out.mTriUv[src + 4];
                uvs[dst + 5] = out.mTriUv[src + 5];
            }
            int srcZ = tri * 3;
            out.mDepths[v]     = out.mTriZ[srcZ];
            out.mDepths[v + 1] = out.mTriZ[srcZ + 1];
            out.mDepths[v + 2] = out.mTriZ[srcZ + 2];
        }
        out.mVertexCount = kept * 3;
        return out.mVertexCount;
    }

    /**
     * Sort the first {@code count} triangles of {@code out} into back-to-front order by
     * {@link CanvasMesh#mTriDepth}, writing the permutation into {@link CanvasMesh#mOrder}.
     *
     * <p>Stability is the property that matters and is preserved: an unstable order lets
     * coplanar or equal-depth triangles swap between frames and flicker.
     *
     * <p>This was an insertion sort, on the reasoning that triangle counts are small and
     * meshes arrive nearly sorted. Neither holds for real content. A height field or any
     * rotated surface arrives in mesh order, which bears no relation to depth order, and the
     * cost is quadratic: measured on the C++ port, which runs the identical algorithm, a
     * 28k-triangle mesh spent 215 ms of a 222 ms frame here. Nearly-sorted input would have
     * cost about 1 ms, so the assumption is refuted by the measurement rather than merely
     * unproven.
     *
     * <p>Bottom-up merge sort instead: O(n log n), stable, and still allocation-free per
     * frame because the scratch permutation is reused from {@link CanvasMesh}. Same
     * comparator and same stability as before, so the permutation is unchanged and renders
     * are bit-identical.
     */
    private static void sortTrianglesBackToFront(CanvasMesh out, int count) {
        int[] order = out.mOrder;
        float[] depth = out.mTriDepth;
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        if (count < 2) {
            return;
        }
        int[] src = order;
        int[] dst = out.mOrderScratch;
        for (int width = 1; width < count; width <<= 1) {
            for (int lo = 0; lo < count; lo += width << 1) {
                int mid = Math.min(lo + width, count);
                int hi = Math.min(lo + (width << 1), count);
                int i = lo;
                int j = mid;
                int k = lo;
                // Farther triangles (larger depth) come first. Take from the right run only
                // when it is *strictly* farther, so equal depths keep their original order.
                while (i < mid && j < hi) {
                    dst[k++] = depth[src[j]] > depth[src[i]] ? src[j++] : src[i++];
                }
                while (i < mid) {
                    dst[k++] = src[i++];
                }
                while (j < hi) {
                    dst[k++] = src[j++];
                }
            }
            int[] swap = src;
            src = dst;
            dst = swap;
        }
        if (src != order) {
            System.arraycopy(src, 0, order, 0, count);
        }
    }

    /**
     * Reusable scratch buffers for the canvas-vertices path, filled by
     * {@link JavaPaint3DContext#buildCanvasVertices}. The host keeps one instance and reads
     * {@link #mPositions} / {@link #mColors} / {@link #mVertexCount} after each call to feed
     * {@code Canvas.drawVertices}. The {@code tri*} / {@code order} fields are internal
     * working storage (one entry per source triangle); callers should ignore them.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class CanvasMesh {
        /** Final per-vertex x,y pairs (length {@code >= vertexCount * 2}). */
        public float @NonNull [] mPositions = new float[0];

        /** Final per-vertex ARGB colors (length {@code >= vertexCount}). */
        public int @NonNull [] mColors = new int[0];

        /** Final per-vertex UV in [0,1] (u,v pairs); valid only when {@link #mHasUv}. */
        public float @NonNull [] mUvs = new float[0];

        /** Whether {@link #mUvs} were populated (the source mesh carried UV). */
        public boolean mHasUv;

        /** Number of emitted vertices (a multiple of 3). */
        public int mVertexCount;

        // Internal per-triangle working storage. triColor holds 3 entries per triangle
        // (one per vertex) to carry smooth/Gouraud shading; the rest are per-triangle.
        float @NonNull [] mTriXY = new float[0];
        int @NonNull [] mTriColor = new int[0];
        float @NonNull [] mTriUv = new float[0];
        float @NonNull [] mTriDepth = new float[0];
        int @NonNull [] mOrder = new int[0];
        /** Scratch permutation for the merge sort; reused, never read by callers. */
        int @NonNull [] mOrderScratch = new int[0];
        /** Per-triangle per-vertex window-space depth in [0,1] (3 per triangle). Internal. */
        float @NonNull [] mTriZ = new float[0];
        /** Per-vertex window-space depth in [0,1] (length >= vertexCount). */
        public float @NonNull [] mDepths = new float[0];

        /** Make room for {@code triCount} triangles, keeping the ones already stored. */
        void ensureTriangleCapacity(int triCount) {
            if (mTriXY.length < triCount * 6) {
                mTriXY = Arrays.copyOf(mTriXY, triCount * 6);
                mTriUv = Arrays.copyOf(mTriUv, triCount * 6);
            }
            if (mTriColor.length < triCount * 3) {
                mTriColor = Arrays.copyOf(mTriColor, triCount * 3);
            }
            if (mTriDepth.length < triCount) {
                mTriDepth = Arrays.copyOf(mTriDepth, triCount);
                mOrder = new int[triCount];
                mOrderScratch = new int[triCount];
            }
            if (mTriZ.length < triCount * 3) {
                mTriZ = Arrays.copyOf(mTriZ, triCount * 3);
            }
        }

        void ensureVertexCapacity(int vertexCount) {
            if (mPositions.length < vertexCount * 2) {
                mPositions = new float[vertexCount * 2];
                mUvs = new float[vertexCount * 2];
            }
            if (mColors.length < vertexCount) {
                mColors = new int[vertexCount];
            }
            if (mDepths.length < vertexCount) {
                mDepths = new float[vertexCount];
            }
        }
    }

    /** Cached mesh data. */
    private static final class Mesh {
        final int[] mIndices;
        final float[] mVerts;
        /** Per-vertex normals (nx,ny,nz triples) for smooth shading, or null = flat. */
        final float @Nullable [] mNormals;

        /** Per-vertex UV (u,v pairs) for texture sampling, or null. */
        final float @Nullable [] mUv;

        /** Non-null for an infinite plane, whose triangle arrays are then empty. */
        final @Nullable InfinitePlane mPlane;

        Mesh(int[] indices, float[] verts, float @Nullable [] normals, float @Nullable [] uv) {
            this.mIndices = indices;
            this.mVerts = verts;
            this.mNormals = normals;
            this.mUv = uv;
            this.mPlane = null;
        }

        Mesh(@NonNull InfinitePlane plane) {
            this.mIndices = new int[0];
            this.mVerts = new float[0];
            this.mNormals = null;
            this.mUv = null;
            this.mPlane = plane;
        }
    }

    /**
     * Mesh id → {@link Mesh} map keyed by primitive {@code int} (open addressing, linear
     * probing), so the lookup each draw makes allocates nothing.
     */
    private static final class MeshTable {
        private int[] mKeys = new int[16];
        private Mesh[] mValues = new Mesh[16];
        private int mSize;

        @Nullable Mesh get(int id) {
            int mask = mKeys.length - 1;
            for (int i = slot(id, mask); ; i = (i + 1) & mask) {
                Mesh mesh = mValues[i];
                if (mesh == null || mKeys[i] == id) {
                    return mesh;
                }
            }
        }

        void put(int id, Mesh mesh) {
            int mask = mKeys.length - 1;
            int i = slot(id, mask);
            while (mValues[i] != null) {
                if (mKeys[i] == id) {
                    mValues[i] = mesh;
                    return;
                }
                i = (i + 1) & mask;
            }
            mKeys[i] = id;
            mValues[i] = mesh;
            if (++mSize * 2 > mKeys.length) {
                rehash(mKeys.length * 2);
            }
        }

        private void rehash(int capacity) {
            int[] keys = mKeys;
            Mesh[] values = mValues;
            mKeys = new int[capacity];
            mValues = new Mesh[capacity];
            int mask = capacity - 1;
            for (int j = 0; j < keys.length; j++) {
                if (values[j] != null) {
                    int i = slot(keys[j], mask);
                    while (mValues[i] != null) {
                        i = (i + 1) & mask;
                    }
                    mKeys[i] = keys[j];
                    mValues[i] = values[j];
                }
            }
        }

        private static int slot(int id, int mask) {
            int hash = id * 0x9E3779B9; // spread sequential ids
            return (hash ^ (hash >>> 16)) & mask;
        }
    }
}
