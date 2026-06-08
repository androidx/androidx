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

import static androidx.compose.remote.player.core.platform.d3.JavaPaint3DContext.NEAR_W_EPSILON;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * The infinite plane of {@link Primitive3D#INFINITE_PLANE}, with its optional grid, drawn
 * without tessellation: by the software engine as one span loop over the rows the plane covers
 * ({@link #rasterize}), and by the canvas engines as one screen-space polygon ({@link
 * #visiblePolygon}) plus grid lines ({@link #buildGridLines}).
 *
 * <h3>Definition</h3>
 *
 * <p>In model space the plane passes through the center {@code C} with normal +Y, or +X / +Z when
 * the axis field of the flags ({@link Primitive3D#axisMode}) says so. Its in-plane axes {@code U},
 * {@code V} (with {@code U × V} = the normal, so the plane is front-facing from the normal's side)
 * are {@code U = +X, V = −Z} for +Y, rotated by the same quarter turns {@link Primitive3D} applies
 * to Y-native meshes: {@code U = −Y, V = −Z} for +X and {@code U = +X, V = +Y} for +Z. Grid lines
 * are the lines {@code u = k·spacing} and {@code v = k·spacing} through {@code C}. The current
 * model and view matrices place the plane like any mesh.
 *
 * <h3>Screen-space math</h3>
 *
 * <p>With {@code M = projection × modelView}, the plane point {@code C + u·U + v·V} has clip
 * coordinates {@code c0 + u·cu + v·cv}, so its homogeneous screen position {@code (sx·w, sy·w,
 * w)} is a 3×3 matrix {@code H} times {@code (u, v, 1)} (the viewport transform is the engine's:
 * {@code sx = (x/w + 1)·width/2}, {@code sy = (1 − y/w)·height/2}, and pixel {@code (x, y)} is
 * sampled at {@code sx = x, sy = y}, as in {@link Rasterizer}). Inverting {@code H} gives three
 * functions that are <i>affine</i> in the pixel position: {@code Q = 1/w}, {@code Qu = u/w} and
 * {@code Qv = v/w}. Window depth {@code z/(2w) + 1/2} is affine as well. Per pixel, depth costs
 * one multiply-add, exactly like a triangle's, and so matches a coplanar mesh. The plane
 * coordinates {@code u = Qu/Q}, {@code v = Qv/Q} need one division, made at the ends of short span
 * segments; in between they are stepped linearly in 16.16 fixed point. A segment is at most 64 px
 * (32 px with non-uniform shading) and is halved until the interpolation error, at most
 * {@code n²·|dQ/dx| / (4·Q)} px for {@code n} pixels, is under 1/8 px; with a level horizon
 * {@code dQ/dx = 0} the interpolation is exact. Only the pixels that a line can reach, bounded
 * per segment from the exact range of the coordinates' screen gradients, compute line coverage.
 *
 * <p>The visible part of the plane is where {@code Q > 0} (in front of the eye), window depth
 * {@code ≥ 0} (the near plane {@code z ≥ −w}, so near-plane pixels are not drawn, as with the
 * engine's near clipping of meshes) and {@code Q ≤ 1/NEAR_W_EPSILON} (the engine's smallest
 * clip-space {@code w}): three half-planes of the screen, which cut the viewport into one convex
 * polygon. A row's span is solved from them directly, so no pixel outside the plane is visited.
 *
 * <p><b>Horizon and far plane.</b> The plane is drawn up to its horizon, the line {@code Q = 0}
 * where it meets the plane at infinity; it is not cut at the far plane. The engine does not clip
 * meshes at the far plane either, so depth past it (window z above 1, tending to {@code f/(f−n)}
 * at the horizon of a perspective camera) is still finite, monotonic and depth-tested as usual.
 * Pixels closer than 10<sup>−4</sup> px to the horizon are skipped, since their plane coordinates
 * are unbounded. An orthographic camera has no horizon: the plane covers the whole viewport on the
 * visible side of the near plane. A plane seen exactly edge-on draws nothing.
 *
 * <h3>Look</h3>
 *
 * <p>The base color comes from the {@link Shader}, which the host wires to its lighting model. A
 * shader that does not depend on the position ({@code uniformShading}: only directional and
 * ambient light, no specular) is called once per draw, so directional lighting is exact. Otherwise
 * (point lights, specular) the color is shaded at the ends of each span segment (at most 32 px
 * apart, and exact in y) and interpolated linearly along the row; shading points beyond 10<sup>5
 * </sup> times the distance of the plane's nearest visible point are pulled in to that distance
 * along their view ray, which leaves the lighting at the horizon at its limit. The flat and smooth
 * shading modes look the same on a plane.
 *
 * <p>Grid lines blend over the base color with the line color's alpha. Their coverage is the exact
 * box filter of a {@code lineWidth}-px line over a 1-px pixel, at the screen-space distance to the
 * nearest line: {@code d = |u/spacing − round(u/spacing)| · c}, where the cell size in pixels,
 * {@code c = 1/|∇(u/spacing)|}, comes from the screen-space derivatives of the plane coordinates
 * ({@code ∇u = (∇Qu − u·∇Q)/Q}) at that pixel. Each family of lines fades linearly with the cell
 * size, from full strength at {@code 2·m} px to nothing at {@code m = max(2, lineWidth + 1)} px
 * (4 to 2 px for lines up to 1 px wide), so dense cells near the horizon show the base color and
 * never alias into moiré.
 *
 * <h3>Mode flags</h3>
 *
 * <ul>
 *   <li>Depth test as for meshes (a pixel is drawn when its depth is smaller than the buffer's);
 *       the depth bias ({@code setDepthBias3D}) is applied as for a triangle, with the plane's
 *       depth slope.
 *   <li>{@link Paint3DContext#MODE_TRANSPARENT}: the plane's color (base plus grid) is blended
 *       src-over into the color buffer and depth is not written.
 *   <li>{@link Paint3DContext#MODE_DOUBLE_SIDED}: the back side is drawn, lit with the normal
 *       turned toward the viewer; without it a plane seen from behind draws nothing.
 *   <li>{@link Paint3DContext#MODE_WIREFRAME}: only the grid is drawn, blended src-over, and the
 *       whole plane writes depth like the mesh wireframe's hidden-line prepass (no depth with
 *       {@code TRANSPARENT}). Without a grid such a draw only writes depth.
 *   <li>Textures are not applied: the plane has no UV and is drawn untextured.
 * </ul>
 *
 * <p>The color buffer holds straight (not premultiplied) ARGB, as the engine's does. Drawing
 * allocates nothing, apart from growing the canvas grid buffers ({@link #buildGridLines}) when a
 * frame needs more lines than any earlier frame.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InfinitePlane {

    /** The host's lighting model, which turns the plane's base color into its lit color. */
    public interface Shader {
        /**
         * Returns the lit ARGB color (straight alpha) of the plane at the eye-space point
         * {@code (px, py, pz)}, where its unit eye-space normal is {@code (nx, ny, nz)}. The
         * normal is already turned toward the viewer: it is the plane's normal on the front side
         * and its negation on the back side of a double-sided plane.
         */
        int shade(float px, float py, float pz, float nx, float ny, float nz);
    }

    /** Most vertices {@link #visiblePolygon} returns. */
    public static final int MAX_POLYGON_VERTICES = 8;

    /** Fade levels the grid lines of the canvas engines are grouped by (see buildGridLines). */
    private static final int RAMP_LEVELS = 4;

    /**
     * Number of groups {@link #buildGridLines} sorts the grid lines into: levels {@code 0} to
     * {@code GRID_LEVELS − 2} hold the fading parts of the lines, by increasing alpha, and level
     * {@code GRID_LEVELS − 1} the parts at full strength.
     */
    public static final int GRID_LEVELS = RAMP_LEVELS + 1;

    /** Pixels closer than this (in px) to the horizon are not drawn. */
    private static final double HORIZON_EPSILON = 1e-4;

    /** Longest span segment, in pixels, between two divisions (the width of a pixel mask). */
    private static final int SEGMENT_MAX = 64;

    /** Longest span segment, in pixels, between two shading points (non-uniform shading). */
    private static final int SHADE_SEGMENT_MAX = 32;

    /**
     * Four times the largest plane-coordinate interpolation error allowed along a segment, in
     * pixels (the error bound is {@code n²·|dQ/dx| / (4·Q)}).
     */
    private static final double SEGMENT_ERROR_X4 = 0.5;

    /** Shading points are kept within this multiple of the nearest visible point's distance. */
    private static final double SHADE_DISTANCE_LIMIT = 1e5;

    /** Most grid lines per family that {@link #buildGridLines} considers. */
    private static final int MAX_GRID_LINES = 4096;

    /** {@code |det H|} below this fraction of the product of its row norms counts as edge-on. */
    private static final double SINGULAR_EPSILON = 1e-10;

    // ---- Definition (model space) ----------------------------------------------------------

    private int mFlags;
    private float mCx;
    private float mCy;
    private float mCz;
    private float mUx;
    private float mUy;
    private float mUz;
    private float mVx;
    private float mVy;
    private float mVz;
    private float mSpacing;
    private float mLineWidth;
    private int mLineColor;
    private boolean mHasGrid;
    /** Unit of the plane coordinates used per draw: the spacing, or 1 without a grid. */
    private float mCell;
    private float mHalfWidth;
    private float mMaxCoverage;
    private float mFadeLo;
    private float mFadeHi;

    // ---- Per-draw state, set by setup() -----------------------------------------------------
    // Affine functions of the pixel position (x, y): a·x + b·y + c.

    private int mWidth;
    private int mHeight;
    /** Q = 1/w. */
    private double mQa;
    private double mQb;
    private double mQc;
    /** Q·u, with u in cells (units of mCell) from the re-centered origin. */
    private double mUa;
    private double mUb;
    private double mUc;
    /** Q·v, likewise. */
    private double mVa;
    private double mVb;
    private double mVc;
    /** Window z, without depth bias. */
    private double mZa;
    private double mZb;
    private double mZc;
    private double mBias;
    private double mQEps;
    /** Grid lines need Q above this somewhere in a segment (the smaller of the two families). */
    private double mQFade;
    private double mDminU;
    private double mDminV;
    /** Eye-space plane point at cell coordinates (0, 0), and the eye-space cell axes. */
    private double mEx;
    private double mEy;
    private double mEz;
    private double mEux;
    private double mEuy;
    private double mEuz;
    private double mEvx;
    private double mEvy;
    private double mEvz;
    private double mShadeLimit;
    /** Unit eye-space normal, turned toward the viewer. */
    private float mNx;
    private float mNy;
    private float mNz;
    /** Screen point whose plane point is nearest the eye (largest Q). */
    private double mAnchorX;
    private double mAnchorY;

    private final double[] mMvp = new double[16];
    // Visible polygon (x, y pairs) and clipping scratch.
    private final double[] mPoly = new double[2 * MAX_POLYGON_VERTICES];
    private final double[] mPolyScratch = new double[2 * MAX_POLYGON_VERTICES];
    private int mPolyCount;

    // Span of the current row (inclusive), set by computeSpan().
    private int mSpanLeft;
    private int mSpanRight;

    // Segment end values: plane coordinates u, v (in cells) and shaded colors.
    private final float[] mEnd0 = new float[2];
    private final float[] mEnd1 = new float[2];
    private int mEndColor0;
    private int mEndColor1;

    // Float copies of the affine coefficients, for the per-pixel grid coverage.
    private float mQaF;
    private float mQbF;
    private float mUaF;
    private float mUbF;
    private float mVaF;
    private float mVbF;

    // Grid state of the current segment (see fillSegment): its first pixel, the plane
    // coordinates there and per pixel, Q there, and each family's reach in 1/65536 cells.
    private int mSegX0;
    private float mSegU0;
    private float mSegDu;
    private float mSegV0;
    private float mSegDv;
    private float mSegQ0;
    private int mSegReachU;
    private int mSegReachV;

    // Canvas grid lines, by fade level: x0, y0, x1, y1 per line.
    private final float[][] mGridLines = new float[GRID_LEVELS][];
    private final int[] mGridCounts = new int[GRID_LEVELS];

    /** Creates the plane of a {@link Primitive3D#INFINITE_PLANE} with these flags and params. */
    public InfinitePlane(int flags, float @NonNull [] params) {
        for (int i = 0; i < GRID_LEVELS; i++) {
            mGridLines[i] = new float[0];
        }
        set(flags, params);
    }

    /**
     * Redefines the plane in place. {@code params} is {@code [cx, cy, cz, spacing, lineWidth,
     * lineA, lineR, lineG, lineB]} ({@link Primitive3D#INFINITE_PLANE_PARAMS}); a missing or
     * non-finite entry takes its default ({@code 0, 0, 0, 0, 1, 1, 0.5, 0.5, 0.5}) and extra
     * entries are ignored. A spacing or line width that is not positive, or a transparent line
     * color, means no grid.
     */
    public void set(int flags, float @NonNull [] params) {
        mFlags = flags;
        switch (Primitive3D.axisMode(flags)) {
            case Primitive3D.AXIS_X: // normal +X
                setAxes(0f, -1f, 0f, 0f, 0f, -1f);
                break;
            case Primitive3D.AXIS_Z: // normal +Z
                setAxes(1f, 0f, 0f, 0f, 1f, 0f);
                break;
            default: // native or AXIS_Y: normal +Y
                setAxes(1f, 0f, 0f, 0f, 0f, -1f);
                break;
        }
        mCx = param(params, 0, 0f);
        mCy = param(params, 1, 0f);
        mCz = param(params, 2, 0f);
        float spacing = param(params, 3, 0f);
        float lineWidth = param(params, 4, 1f);
        int a = channel(param(params, 5, 1f));
        int r = channel(param(params, 6, 0.5f));
        int g = channel(param(params, 7, 0.5f));
        int b = channel(param(params, 8, 0.5f));
        mSpacing = spacing > 0f ? spacing : 0f;
        mLineWidth = lineWidth > 0f ? lineWidth : 0f;
        mLineColor = (a << 24) | (r << 16) | (g << 8) | b;
        mHasGrid = mSpacing > 0f && mLineWidth > 0f && a > 0;
        mCell = mHasGrid ? mSpacing : 1f;
        mHalfWidth = 0.5f * mLineWidth + 0.5f;
        mMaxCoverage = Math.min(mLineWidth, 1f);
        mFadeLo = Math.max(2f, mLineWidth + 1f);
        mFadeHi = 2f * mFadeLo;
    }

    private void setAxes(float ux, float uy, float uz, float vx, float vy, float vz) {
        mUx = ux;
        mUy = uy;
        mUz = uz;
        mVx = vx;
        mVy = vy;
        mVz = vz;
    }

    private static float param(float[] params, int i, float fallback) {
        if (i >= params.length) {
            return fallback;
        }
        float v = params[i];
        return Math.abs(v) <= Float.MAX_VALUE ? v : fallback;
    }

    private static int channel(float v) {
        return v <= 0f ? 0 : v >= 1f ? 255 : (int) (v * 255f + 0.5f);
    }

    /** The flags the plane was defined with. */
    public int getFlags() {
        return mFlags;
    }

    /** Whether the plane draws a grid. */
    public boolean hasGrid() {
        return mHasGrid;
    }

    /** Grid cell size in model units, 0 without a grid. */
    public float getSpacing() {
        return mSpacing;
    }

    /** Grid line width in pixels. */
    public float getLineWidth() {
        return mLineWidth;
    }

    /** Grid line color, ARGB. */
    public int getLineColor() {
        return mLineColor;
    }

    // ---- Software engine ---------------------------------------------------------------------

    /**
     * Draws the plane into the software engine's buffers: the visible rows, each as one span,
     * depth-tested against (and, unless {@link Paint3DContext#MODE_TRANSPARENT}, written to)
     * {@code depth}. See the class documentation for the mode flags.
     *
     * @param color ARGB color buffer (straight alpha), row stride {@code width}
     * @param depth window-z buffer, row stride {@code width}, {@code +∞} where empty
     * @param modelView column-major view × model matrix
     * @param projection column-major projection matrix
     * @param mode the draw's mode ({@link Paint3DContext#drawMesh3D})
     * @param depthBiasConstant constant depth bias, in window-z units
     * @param depthBiasSlope depth bias per unit of the plane's depth slope
     * @param uniformShading whether {@code shader} returns the same color at every point (no
     *     point lights and no specular), so it is called once per draw
     * @param outDirtyRect receives the bounds of every pixel the draw may have written,
     *     {@code [left, top, right, bottom)}, when it returns true
     * @param shader lights the plane's base color
     * @return whether the plane covered any pixel
     */
    public boolean rasterize(int @NonNull [] color, float @NonNull [] depth, int width,
            int height, float @NonNull [] modelView, float @NonNull [] projection, int mode,
            float depthBiasConstant, float depthBiasSlope, boolean uniformShading,
            int @NonNull [] outDirtyRect, @NonNull Shader shader) {
        long size = (long) width * height;
        if (size > color.length || size > depth.length) {
            return false;
        }
        if (!setup(modelView, projection, width, height, mode, depthBiasConstant,
                depthBiasSlope)) {
            return false;
        }
        boolean transparent = (mode & Paint3DContext.MODE_TRANSPARENT) != 0;
        boolean lit = (mode & Paint3DContext.MODE_WIREFRAME) == 0;
        boolean grid = mHasGrid;
        if (!lit && !grid && transparent) {
            return false; // a wireframe plane without a grid only writes depth
        }
        boolean blend = transparent || !lit;
        boolean writeDepth = !transparent;
        boolean uniform = uniformShading || !lit;
        int base = lit && uniform ? shadeAt(shader, mAnchorX, mAnchorY) : 0;

        // Rows the visible polygon spans.
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 1; i < 2 * mPolyCount; i += 2) {
            minY = Math.min(minY, mPoly[i]);
            maxY = Math.max(maxY, mPoly[i]);
        }
        int yStart = Math.max(0, (int) Math.ceil(minY));
        int yEnd = Math.min(height - 1, (int) Math.floor(maxY));

        float zA = (float) mZa;
        double zB = mZb;
        double zC = mZc + mBias;
        int left = width;
        int right = 0;
        int top = height;
        int bottom = 0;
        for (int y = yStart; y <= yEnd; y++) {
            if (!computeSpan(y)) {
                continue;
            }
            int xl = mSpanLeft;
            int xr = mSpanRight;
            left = Math.min(left, xl);
            right = Math.max(right, xr + 1);
            top = Math.min(top, y);
            bottom = y + 1;
            int row = y * width;
            float zRow = (float) (zB * y + zC);
            boolean gridRow = grid && Math.max(q(xl, y), q(xr, y)) > mQFade;
            if (!gridRow && uniform) {
                if (!lit) {
                    if (writeDepth) {
                        fillDepth(depth, row, xl, xr + 1, zRow, zA);
                    }
                } else if (blend) {
                    fillBlend(color, depth, row, xl, xr + 1, zRow, zA, base);
                } else {
                    fillOpaque(color, depth, row, xl, xr + 1, zRow, zA, base);
                }
            } else {
                drawSegments(color, depth, y, row, xl, xr, zRow, zA, gridRow, shader,
                        uniform, base, lit, blend, writeDepth);
            }
        }
        if (bottom == 0) {
            return false;
        }
        outDirtyRect[0] = left;
        outDirtyRect[1] = top;
        outDirtyRect[2] = right;
        outDirtyRect[3] = bottom;
        return true;
    }

    /** Q at pixel (x, y). */
    private double q(double x, double y) {
        return mQa * x + mQb * y + mQc;
    }

    /**
     * Sets {@link #mSpanLeft} and {@link #mSpanRight} to the pixels of row {@code y} inside the
     * visible polygon and returns whether there are any.
     */
    private boolean computeSpan(int y) {
        double lo = 0.0;
        double hi = mWidth - 1;
        // Q ≥ ε (in front of the eye, off the horizon).
        double r = mQb * y + mQc - mQEps;
        double a = mQa;
        if (a > 0.0) {
            lo = Math.max(lo, -r / a);
        } else if (a < 0.0) {
            hi = Math.min(hi, -r / a);
        } else if (!(r >= 0.0)) {
            return false;
        }
        // Window z ≥ 0 (the near plane).
        r = mZb * y + mZc;
        a = mZa;
        if (a > 0.0) {
            lo = Math.max(lo, -r / a);
        } else if (a < 0.0) {
            hi = Math.min(hi, -r / a);
        } else if (!(r >= 0.0)) {
            return false;
        }
        // Q ≤ 1/NEAR_W_EPSILON (the w floor).
        r = 1.0 / NEAR_W_EPSILON - mQb * y - mQc;
        a = -mQa;
        if (a > 0.0) {
            lo = Math.max(lo, -r / a);
        } else if (a < 0.0) {
            hi = Math.min(hi, -r / a);
        } else if (!(r >= 0.0)) {
            return false;
        }
        if (!(lo <= hi)) {
            return false;
        }
        mSpanLeft = (int) Math.ceil(lo);
        mSpanRight = (int) Math.floor(hi);
        return mSpanLeft <= mSpanRight;
    }

    /** Opaque fill with one color: depth test, depth and color write. */
    private static void fillOpaque(int[] color, float[] depth, int row, int x0, int x1,
            float zRow, float zA, int c) {
        for (int x = x0; x < x1; x++) {
            float z = zRow + zA * x;
            int p = row + x;
            if (z < depth[p]) {
                depth[p] = z;
                color[p] = c;
            }
        }
    }

    /** Depth-only fill (wireframe without grid lines here). */
    private static void fillDepth(float[] depth, int row, int x0, int x1, float zRow, float zA) {
        for (int x = x0; x < x1; x++) {
            float z = zRow + zA * x;
            int p = row + x;
            if (z < depth[p]) {
                depth[p] = z;
            }
        }
    }

    /** Transparent fill with one color: depth test, src-over blend, no depth write. */
    private static void fillBlend(int[] color, float[] depth, int row, int x0, int x1,
            float zRow, float zA, int c) {
        if ((c >>> 24) == 0) {
            return;
        }
        for (int x = x0; x < x1; x++) {
            float z = zRow + zA * x;
            int p = row + x;
            if (z < depth[p]) {
                color[p] = Rasterizer.blendSrcOver(c, color[p]);
            }
        }
    }

    /**
     * Draws the span {@code [xl, xr]} of row {@code y} in segments, with the plane coordinates
     * (grid) and/or the shaded color (non-uniform shading) evaluated at each segment's ends.
     */
    private void drawSegments(int[] color, float[] depth, int y, int row, int xl, int xr,
            float zRow, float zA, boolean grid, Shader shader, boolean uniform, int base,
            boolean lit, boolean blend, boolean writeDepth) {
        boolean shade = lit && !uniform;
        float[] e0 = mEnd0;
        float[] e1 = mEnd1;
        evalEnd(xl, y, grid, shade, shader, e0, true);
        int x = xl;
        while (x <= xr) {
            int n = segmentLength(x, y, shade ? SHADE_SEGMENT_MAX : SEGMENT_MAX);
            int xe = x + n;
            int steps;
            if (xe > xr) {
                xe = xr + 1;
                steps = xr - x;
                evalEnd(xr, y, grid, shade, shader, e1, false);
            } else {
                steps = n;
                evalEnd(xe, y, grid, shade, shader, e1, false);
            }
            fillSegment(color, depth, row, y, x, xe, steps, zRow, zA, e0, e1,
                    shade ? mEndColor0 : base, shade ? mEndColor1 : base, grid, lit, blend,
                    writeDepth);
            float[] swap = e0;
            e0 = e1;
            e1 = swap;
            mEndColor0 = mEndColor1;
            x = xe;
        }
    }

    /**
     * Pixels of the segment starting at {@code x}: {@code max}, halved until linear
     * interpolation of the plane coordinates is within 1/8 px.
     */
    private int segmentLength(int x, int y, int max) {
        int n = max;
        double qa = Math.abs(mQa);
        if (qa > 0.0) {
            double q0 = q(x, y);
            while (n > 1) {
                double qMin = Math.min(q0, q0 + mQa * n);
                if (n * n * qa <= SEGMENT_ERROR_X4 * qMin) {
                    break;
                }
                n >>= 1;
            }
        }
        return n;
    }

    /**
     * Evaluates the segment end values at pixel {@code (x, y)}: the plane coordinates in cells
     * into {@code out} (grid), and the shaded color into {@link #mEndColor0} ({@code first}) or
     * {@link #mEndColor1} (shade).
     */
    private void evalEnd(int x, int y, boolean grid, boolean shade, Shader shader, float[] out,
            boolean first) {
        if (grid) {
            double iq = 1.0 / q(x, y);
            out[0] = (float) ((mUa * x + mUb * y + mUc) * iq);
            out[1] = (float) ((mVa * x + mVb * y + mVc) * iq);
        }
        if (shade) {
            int c = shadeAt(shader, x, y);
            if (first) {
                mEndColor0 = c;
            } else {
                mEndColor1 = c;
            }
        }
    }

    /**
     * Draws the segment {@code [x0, x1)} of row {@code y} (at most 64 pixels) with grid lines
     * and/or a color interpolated from {@code c0} to {@code c1}. Each grid family's plane
     * coordinate is stepped in 16.16 fixed point, so finding the pixels near a line costs a few
     * integer operations per pixel; the line coverage is computed at those pixels only.
     */
    private void fillSegment(int[] color, float[] depth, int row, int y, int x0, int x1,
            int steps, float zRow, float zA, float[] e0, float[] e1, int c0, int c1,
            boolean grid, boolean lit, boolean blend, boolean writeDepth) {
        float inv = steps > 0 ? 1f / steps : 0f;
        int reachU = 0;
        int reachV = 0;
        int uf = 0;
        int duf = 0;
        int vf = 0;
        int dvf = 0;
        if (grid) {
            float u0 = e0[0];
            float v0 = e0[1];
            float du = (e1[0] - u0) * inv;
            float dv = (e1[1] - v0) * inv;
            double qStart = q(x0, y);
            double qEnd = q(x1 - 1, y);
            double qMin = Math.min(qStart, qEnd);
            double qMax = Math.max(qStart, qEnd);
            int n = x1 - x0;
            reachU = reach(mUa, mUb, u0, du, n, qMin, qMax);
            reachV = reach(mVa, mVb, v0, dv, n, qMin, qMax);
            uf = fixedFraction(u0);
            duf = (int) Math.rint(du * 65536.0);
            vf = fixedFraction(v0);
            dvf = (int) Math.rint(dv * 65536.0);
            mSegX0 = x0;
            mSegU0 = u0;
            mSegDu = du;
            mSegV0 = v0;
            mSegDv = dv;
            mSegQ0 = (float) qStart;
            mSegReachU = reachU;
            mSegReachV = reachV;
        }
        boolean lines = reachU > 0 || reachV > 0;
        if (lit && !blend && c0 == c1) {
            if (lines) {
                fillGridOpaque(color, depth, row, x0, x1, zRow, zA, c0, uf, duf, reachU, vf,
                        dvf, reachV);
            } else {
                fillOpaque(color, depth, row, x0, x1, zRow, zA, c0);
            }
            return;
        }
        // The general loop: color interpolated in 16.16 fixed point per channel.
        int ca = ((c0 >>> 24) << 16) + 0x8000;
        int cr = (((c0 >>> 16) & 0xFF) << 16) + 0x8000;
        int cg = (((c0 >>> 8) & 0xFF) << 16) + 0x8000;
        int cb = ((c0 & 0xFF) << 16) + 0x8000;
        int dca = (int) ((((c1 >>> 24) - (c0 >>> 24)) << 16) * inv);
        int dcr = (int) (((((c1 >>> 16) & 0xFF) - ((c0 >>> 16) & 0xFF)) << 16) * inv);
        int dcg = (int) (((((c1 >>> 8) & 0xFF) - ((c0 >>> 8) & 0xFF)) << 16) * inv);
        int dcb = (int) ((((c1 & 0xFF) - (c0 & 0xFF)) << 16) * inv);
        for (int x = x0; x < x1; x++) {
            float z = zRow + zA * x;
            int p = row + x;
            if (z < depth[p]) {
                if (writeDepth) {
                    depth[p] = z;
                }
                int out = color[p];
                if (lit) {
                    int src = ((ca >>> 16) << 24) | ((cr >>> 16) << 16) | ((cg >>> 16) << 8)
                            | (cb >>> 16);
                    out = blend ? Rasterizer.blendSrcOver(src, out) : src;
                }
                if (lines) {
                    int tu = Math.abs((short) uf);
                    int tv = Math.abs((short) vf);
                    if (tu < reachU || tv < reachV) {
                        out = gridPixel(out, x, tu, tv);
                    }
                }
                color[p] = out;
            }
            uf += duf;
            vf += dvf;
            ca += dca;
            cr += dcr;
            cg += dcg;
            cb += dcb;
        }
    }

    /** {@link #fillOpaque} with grid lines (see {@link #fillSegment}). */
    private void fillGridOpaque(int[] color, float[] depth, int row, int x0, int x1, float zRow,
            float zA, int c, int uf, int duf, int reachU, int vf, int dvf, int reachV) {
        for (int x = x0; x < x1; x++) {
            float z = zRow + zA * x;
            int p = row + x;
            if (z < depth[p]) {
                depth[p] = z;
                // Distances to the nearest line of each family, in 1/65536 cells.
                int tu = Math.abs((short) uf);
                int tv = Math.abs((short) vf);
                color[p] = tu < reachU || tv < reachV ? gridPixel(c, x, tu, tv) : c;
            }
            uf += duf;
            vf += dvf;
        }
    }

    /**
     * {@code dst} under the grid lines at pixel {@code x} of the current segment, where the
     * fixed-point distances to the nearest lines are {@code tu} and {@code tv}. Each family's
     * coverage is the box filter of the {@code lineWidth}-px line at the pixel's distance to
     * it, times the family's fade, both from the cell size at the pixel; the families combine
     * by maximum.
     */
    private int gridPixel(int dst, int x, int tu, int tv) {
        int i = x - mSegX0;
        float qa = mQaF;
        float qb = mQbF;
        float q = mSegQ0 + i * qa;
        float cu = 0f;
        float cv = 0f;
        if (tu < mSegReachU) {
            float u = mSegU0 + i * mSegDu;
            float gx = mUaF - u * qa;
            float gy = mUbF - u * qb;
            cu = lineCoverage(tu * (1f / 65536f), q / (float) Math.sqrt(gx * gx + gy * gy));
        }
        if (tv < mSegReachV) {
            float v = mSegV0 + i * mSegDv;
            float gx = mVaF - v * qa;
            float gy = mVbF - v * qb;
            cv = lineCoverage(tv * (1f / 65536f), q / (float) Math.sqrt(gx * gx + gy * gy));
        }
        float a = (cu > cv ? cu : cv) * ((mLineColor >>> 24) * (1f / 255f));
        return a > 0f ? lineOver(dst, mLineColor & 0xFFFFFF, a) : dst;
    }

    /**
     * Coverage (0..1) of a pixel by a line {@code t} cells away, with cells of {@code cell}
     * px: the box filter of the {@code lineWidth}-px line, times the fade.
     */
    private float lineCoverage(float t, float cell) {
        float c = mHalfWidth - t * cell;
        float f = (cell - mFadeLo) / (mFadeHi - mFadeLo);
        if (!(c > 0f) || !(f > 0f)) {
            return 0f;
        }
        return (c < mMaxCoverage ? c : mMaxCoverage) * (f < 1f ? f : 1f);
    }

    /**
     * How far from a line, in 1/65536 cells, a pixel of the segment can be covered by a line of
     * the family {@code G = Q·u} ({@code ga}, {@code gb}: the x and y coefficients of G), where
     * {@code u = u0 + i·du} at pixel {@code i} and Q lies in {@code [qMin, qMax]}; 0 when the
     * family is faded out over the whole segment, and above 32768 (every pixel) when lines are
     * dense. The cell size {@code Q / |g(u)|}, {@code g(u) = ∇G − u·∇Q}, is bounded through the
     * exact range of {@code |g(u)|} over the segment, which is convex in u.
     */
    private int reach(double ga, double gb, float u0, float du, int n, double qMin,
            double qMax) {
        double uLo = u0;
        double uHi = u0 + (double) (n - 1) * du;
        if (uHi < uLo) {
            double swap = uLo;
            uLo = uHi;
            uHi = swap;
        }
        double qa = mQa;
        double qb = mQb;
        double qq = qa * qa + qb * qb;
        double uMin = qq > 0.0 ? (ga * qa + gb * qb) / qq : uLo; // where |g| is smallest
        uMin = uMin < uLo ? uLo : (uMin > uHi ? uHi : uMin);
        double gMin2 = norm2(ga - uMin * qa, gb - uMin * qb);
        double fadeLo = mFadeLo;
        if (!(qMax * qMax > fadeLo * fadeLo * gMin2)) {
            return 0; // cells under fadeLo px everywhere on the segment
        }
        double gMax2 = Math.max(norm2(ga - uLo * qa, gb - uLo * qb),
                norm2(ga - uHi * qa, gb - uHi * qb));
        double cellMin = Math.max(qMin / Math.sqrt(gMax2), fadeLo);
        // Reach of a line, plus 1/8 px for the interpolated coordinates and 2⁻¹⁰ cells for
        // the fixed-point stepping.
        double r = (mHalfWidth + 0.125) / cellMin + 1.0 / 1024;
        if (!(r < 0.5) || !(Math.abs(du) < 0.25f)) {
            return 0x8001;
        }
        return (int) Math.ceil(r * 65536.0);
    }

    /** The fractional part of {@code u}, in 16.16 fixed point. */
    private static int fixedFraction(float u) {
        return (int) Math.rint((u - Math.floor(u)) * 65536.0);
    }

    private static double norm2(double x, double y) {
        return x * x + y * y;
    }

    /**
     * {@code src} under a grid line of color {@code lineRgb} and effective alpha {@code a}
     * (coverage × fade × line alpha, in (0, 1]), in straight alpha.
     */
    private static int lineOver(int src, int lineRgb, float a) {
        if ((src >>> 24) == 255) {
            float ia = 1f - a;
            int r = (int) (((lineRgb >>> 16) & 0xFF) * a + ((src >>> 16) & 0xFF) * ia + 0.5f);
            int g = (int) (((lineRgb >>> 8) & 0xFF) * a + ((src >>> 8) & 0xFF) * ia + 0.5f);
            int b = (int) ((lineRgb & 0xFF) * a + (src & 0xFF) * ia + 0.5f);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return Rasterizer.blendSrcOver(((int) (a * 255f + 0.5f) << 24) | lineRgb, src);
    }

    // ---- Canvas engines ----------------------------------------------------------------------

    /**
     * Computes the visible part of the plane for the canvas engines: the plane ∩ the view
     * frustum (without its far plane, see the class documentation) as one convex polygon, in the
     * pixel space the software engine draws into. It is the polygon the software engine fills,
     * and it is not split for depth sorting.
     *
     * @param outXY receives {@code x, y} per vertex, in order around the polygon (at least
     *     {@code 2 ×} {@link #MAX_POLYGON_VERTICES} floats)
     * @param outDepth if not null, receives each vertex's window z, with the depth bias
     * @param outColor if not null, receives each vertex's lit color (vertices on the horizon are
     *     shaded at the limited distance; with uniform shading all are equal)
     * @param shader lights the plane's base color
     * @return the vertex count: 0 when nothing is visible (also when the back side faces the
     *     viewer without {@link Paint3DContext#MODE_DOUBLE_SIDED}), otherwise at least 3
     */
    public int visiblePolygon(float @NonNull [] modelView, float @NonNull [] projection,
            int width, int height, int mode, float depthBiasConstant, float depthBiasSlope,
            boolean uniformShading, float @NonNull [] outXY, float @Nullable [] outDepth,
            int @Nullable [] outColor, @NonNull Shader shader) {
        if (!setup(modelView, projection, width, height, mode, depthBiasConstant,
                depthBiasSlope)) {
            return 0;
        }
        int n = mPolyCount;
        int uniformColor = 0;
        if (outColor != null && uniformShading) {
            uniformColor = shadeAt(shader, mAnchorX, mAnchorY);
        }
        for (int i = 0; i < n; i++) {
            double x = mPoly[2 * i];
            double y = mPoly[2 * i + 1];
            outXY[2 * i] = (float) x;
            outXY[2 * i + 1] = (float) y;
            if (outDepth != null) {
                outDepth[i] = (float) (mZa * x + mZb * y + mZc + mBias);
            }
            if (outColor != null) {
                outColor[i] = uniformShading ? uniformColor : shadeAt(shader, x, y);
            }
        }
        return n;
    }

    /**
     * Computes the grid lines for the canvas engines: the visible part of every line whose
     * cells are large enough to show, clipped to {@link #visiblePolygon}, with the same fade as
     * the software engine. A line fades in along its length, so it is cut into pieces, grouped
     * by fade level: draw each level with {@link #getGridLineColor(int)} (the line color with the
     * level's alpha) as lines of width {@link #getLineWidth()} px, e.g. with
     * {@code Canvas.drawLines(getGridLines(level), 0, 4 * getGridLineCount(level), paint)}.
     *
     * @return the total number of lines over all levels (0 without a grid)
     */
    public int buildGridLines(float @NonNull [] modelView, float @NonNull [] projection,
            int width, int height, int mode) {
        Arrays.fill(mGridCounts, 0);
        if (!mHasGrid || !setup(modelView, projection, width, height, mode, 0f, 0f)) {
            return 0;
        }
        int total = buildFamily(mUa, mUb, mUc, mDminU);
        total += buildFamily(mVa, mVb, mVc, mDminV);
        return total;
    }

    /** Lines of {@code level} ({@code 0 ≤ level <} {@link #GRID_LEVELS}): x0, y0, x1, y1 each. */
    public float @NonNull [] getGridLines(int level) {
        return mGridLines[level];
    }

    /** Number of lines in {@link #getGridLines(int)} for {@code level}. */
    public int getGridLineCount(int level) {
        return mGridCounts[level];
    }

    /** ARGB color to draw the lines of {@code level} with: the line color, faded. */
    public int getGridLineColor(int level) {
        float f = level >= RAMP_LEVELS ? 1f : (level + 0.5f) / RAMP_LEVELS;
        int a = (int) ((mLineColor >>> 24) * f + 0.5f);
        return (a << 24) | (mLineColor & 0xFFFFFF);
    }

    /**
     * Adds the lines {@code G = k·Q} (G being {@code Q·u} or {@code Q·v}) for every integer
     * {@code k} with visible cells. On line k the cell size is {@code Q/|g_k|} px with {@code g_k
     * = ∇G − k·∇Q} (so the fade is linear along it), and a line can only show where {@code Q ≥
     * fadeLo·|g_k|}; as Q is at most its value at the anchor, this bounds k.
     */
    private int buildFamily(double ga, double gb, double gc, double dmin) {
        double qa = mQa;
        double qb = mQb;
        double qc = mQc;
        double qq = Math.sqrt(qa * qa + qb * qb);
        double qMax = q(mAnchorX, mAnchorY);
        double reach = qMax / mFadeLo; // |g_k| must not exceed this
        if (!(dmin <= reach)) {
            return 0;
        }
        double kMin;
        double kMax;
        if (qq > 0.0) {
            double kc = (ga * qa + gb * qb) / (qq * qq);
            double r = Math.sqrt(reach * reach - dmin * dmin) / qq;
            kMin = Math.ceil(kc - r);
            kMax = Math.floor(kc + r);
        } else {
            // Affine (orthographic): every line has the same cells; take the polygon's k range.
            kMin = Double.POSITIVE_INFINITY;
            kMax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < mPolyCount; i++) {
                double x = mPoly[2 * i];
                double y = mPoly[2 * i + 1];
                double k = (ga * x + gb * y + gc) / q(x, y);
                kMin = Math.min(kMin, k);
                kMax = Math.max(kMax, k);
            }
            kMin = Math.ceil(kMin);
            kMax = Math.floor(kMax);
        }
        if (!(kMin <= kMax)) {
            return 0;
        }
        // Lines near the anchor (k ≈ 0 after re-centering) first, if there are too many.
        kMin = Math.max(kMin, -MAX_GRID_LINES / 2);
        kMax = Math.min(kMax, MAX_GRID_LINES / 2);
        int added = 0;
        double lo = mFadeLo;
        double hi = mFadeHi;
        for (double k = kMin; k <= kMax; k++) {
            double nx = ga - k * qa;
            double ny = gb - k * qb;
            double c = gc - k * qc;
            double nn = Math.sqrt(nx * nx + ny * ny);
            if (!(nn > 0.0)) {
                continue;
            }
            // Line through p0, direction d (unit).
            double dx = -ny / nn;
            double dy = nx / nn;
            double px = -c * nx / (nn * nn);
            double py = -c * ny / (nn * nn);
            // Clip to the viewport, the visible polygon's half-planes and the fade limit.
            mClipS0 = Double.NEGATIVE_INFINITY;
            mClipS1 = Double.POSITIVE_INFINITY;
            if (!clipLineHalf(1.0, 0.0, 0.0, px, py, dx, dy)
                    || !clipLineHalf(-1.0, 0.0, mWidth, px, py, dx, dy)
                    || !clipLineHalf(0.0, 1.0, 0.0, px, py, dx, dy)
                    || !clipLineHalf(0.0, -1.0, mHeight, px, py, dx, dy)
                    || !clipLineHalf(qa, qb, qc - mQEps, px, py, dx, dy)
                    || !clipLineHalf(mZa, mZb, mZc, px, py, dx, dy)
                    || !clipLineHalf(-qa, -qb, 1.0 / NEAR_W_EPSILON - qc, px, py, dx, dy)
                    || !clipLineHalf(qa, qb, qc - lo * nn, px, py, dx, dy)) {
                continue;
            }
            // Fade along the line: f(s) = (Q(s)/nn − lo)/(hi − lo), linear in s.
            double f0 = (q(px, py) / nn - lo) / (hi - lo);
            double fd = (qa * dx + qb * dy) / nn / (hi - lo);
            added += emitPieces(px, py, dx, dy, mClipS0, mClipS1, f0, fd);
        }
        return added;
    }

    // Parameter range of the line being clipped (see clipLineHalf).
    private double mClipS0;
    private double mClipS1;

    /**
     * Clips the line {@code p + s·d}, {@code s ∈ [mClipS0, mClipS1]}, to the half-plane
     * {@code a·x + b·y + c ≥ 0}; returns whether anything is left.
     */
    private boolean clipLineHalf(double a, double b, double c, double px, double py, double dx,
            double dy) {
        double f0 = a * px + b * py + c;
        double fd = a * dx + b * dy;
        if (fd > 0.0) {
            mClipS0 = Math.max(mClipS0, -f0 / fd);
        } else if (fd < 0.0) {
            mClipS1 = Math.min(mClipS1, -f0 / fd);
        } else if (!(f0 >= 0.0)) {
            return false;
        }
        return mClipS0 < mClipS1;
    }

    /**
     * Cuts the visible line {@code p + s·d}, {@code s ∈ [s0, s1]}, whose fade is {@code f0 +
     * s·fd}, into pieces by fade level and adds them; returns how many.
     */
    private int emitPieces(double px, double py, double dx, double dy, double s0, double s1,
            double f0, double fd) {
        int added = 0;
        double sa = s0;
        // Cut where the fade crosses i / RAMP_LEVELS, in increasing s; each piece takes the
        // level of the fade at its middle.
        for (int j = 1; j <= RAMP_LEVELS && fd != 0.0; j++) {
            int i = fd > 0.0 ? j : RAMP_LEVELS + 1 - j;
            double s = ((double) i / RAMP_LEVELS - f0) / fd;
            if (s > sa && s < s1) {
                added += emit(levelOf(f0 + fd * 0.5 * (sa + s)), px, py, dx, dy, sa, s);
                sa = s;
            }
        }
        return added + emit(levelOf(f0 + fd * 0.5 * (sa + s1)), px, py, dx, dy, sa, s1);
    }

    /** Fade level of fade {@code f} (≥ 0). */
    private static int levelOf(double f) {
        if (f >= 1.0) {
            return RAMP_LEVELS;
        }
        int level = (int) (f * RAMP_LEVELS);
        return level < 0 ? 0 : level;
    }

    /** Adds the piece {@code s ∈ [sa, sb]} of the line to {@code level}. */
    private int emit(int level, double px, double py, double dx, double dy, double sa,
            double sb) {
        if (!(sb > sa)) {
            return 0;
        }
        int n = mGridCounts[level];
        float[] lines = mGridLines[level];
        if (4 * n + 4 > lines.length) {
            lines = Arrays.copyOf(lines, Math.max(64, 2 * lines.length));
            mGridLines[level] = lines;
        }
        lines[4 * n] = (float) (px + sa * dx);
        lines[4 * n + 1] = (float) (py + sa * dy);
        lines[4 * n + 2] = (float) (px + sb * dx);
        lines[4 * n + 3] = (float) (py + sb * dy);
        mGridCounts[level] = n + 1;
        return 1;
    }

    // ---- Setup -------------------------------------------------------------------------------

    /**
     * Shades the plane point under pixel {@code (x, y)}: its eye-space position, pulled in to
     * {@link #mShadeLimit} along its view ray when farther.
     */
    private int shadeAt(Shader shader, double x, double y) {
        double q = Math.max(q(x, y), mQEps);
        double u = (mUa * x + mUb * y + mUc) / q;
        double v = (mVa * x + mVb * y + mVc) / q;
        double px = mEx + u * mEux + v * mEvx;
        double py = mEy + u * mEuy + v * mEvy;
        double pz = mEz + u * mEuz + v * mEvz;
        double d2 = px * px + py * py + pz * pz;
        double limit = mShadeLimit;
        if (d2 > limit * limit) {
            double s = limit / Math.sqrt(d2);
            px *= s;
            py *= s;
            pz *= s;
        }
        return shader.shade((float) px, (float) py, (float) pz, mNx, mNy, mNz);
    }

    /**
     * Computes the per-draw state for this camera and viewport. Returns false when nothing is
     * visible: the plane is edge-on, behind the eye or the near plane, out of the viewport, or
     * seen from behind without {@link Paint3DContext#MODE_DOUBLE_SIDED}.
     */
    private boolean setup(float[] mv, float[] proj, int width, int height, int mode,
            float biasConstant, float biasSlope) {
        mPolyCount = 0;
        if (width <= 0 || height <= 0 || mv.length < 16 || proj.length < 16) {
            return false;
        }
        mWidth = width;
        mHeight = height;
        double[] m = mMvp;
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                m[4 * c + r] = (double) proj[r] * mv[4 * c]
                        + (double) proj[4 + r] * mv[4 * c + 1]
                        + (double) proj[8 + r] * mv[4 * c + 2]
                        + (double) proj[12 + r] * mv[4 * c + 3];
            }
        }
        double cell = mCell;
        double cx = mCx;
        double cy = mCy;
        double cz = mCz;
        double ux = mUx * cell;
        double uy = mUy * cell;
        double uz = mUz * cell;
        double vx = mVx * cell;
        double vy = mVy * cell;
        double vz = mVz * cell;
        // Clip-space images of the center and of the two cell axes.
        double pX = m[0] * cx + m[4] * cy + m[8] * cz + m[12];
        double pY = m[1] * cx + m[5] * cy + m[9] * cz + m[13];
        double pZ = m[2] * cx + m[6] * cy + m[10] * cz + m[14];
        double pW = m[3] * cx + m[7] * cy + m[11] * cz + m[15];
        double uX = m[0] * ux + m[4] * uy + m[8] * uz;
        double uY = m[1] * ux + m[5] * uy + m[9] * uz;
        double uZ = m[2] * ux + m[6] * uy + m[10] * uz;
        double uW = m[3] * ux + m[7] * uy + m[11] * uz;
        double vX = m[0] * vx + m[4] * vy + m[8] * vz;
        double vY = m[1] * vx + m[5] * vy + m[9] * vz;
        double vZ = m[2] * vx + m[6] * vy + m[10] * vz;
        double vW = m[3] * vx + m[7] * vy + m[11] * vz;
        // H: (u, v, 1) → (sx·w, sy·w, w).
        double hw = 0.5 * width;
        double hh = 0.5 * height;
        double a00 = hw * (uX + uW);
        double a01 = hw * (vX + vW);
        double a02 = hw * (pX + pW);
        double a10 = hh * (uW - uY);
        double a11 = hh * (vW - vY);
        double a12 = hh * (pW - pY);
        double a20 = uW;
        double a21 = vW;
        double a22 = pW;
        double i00 = a11 * a22 - a12 * a21;
        double i01 = a02 * a21 - a01 * a22;
        double i02 = a01 * a12 - a02 * a11;
        double i10 = a12 * a20 - a10 * a22;
        double i11 = a00 * a22 - a02 * a20;
        double i12 = a02 * a10 - a00 * a12;
        double i20 = a10 * a21 - a11 * a20;
        double i21 = a01 * a20 - a00 * a21;
        double i22 = a00 * a11 - a01 * a10;
        double det = a00 * i00 + a01 * i10 + a02 * i20;
        double norms = Math.sqrt((a00 * a00 + a01 * a01 + a02 * a02)
                * (a10 * a10 + a11 * a11 + a12 * a12) * (a20 * a20 + a21 * a21 + a22 * a22));
        if (!(Math.abs(det) > SINGULAR_EPSILON * norms) || !isFinite(det)) {
            return false; // edge-on (or degenerate matrices)
        }
        // The plane's (u, v) → screen map keeps orientation iff det < 0: then U, V wind CCW in
        // NDC, i.e. the front side (as for a CCW triangle) faces the viewer.
        boolean front = det < 0.0;
        if (!front && (mode & Paint3DContext.MODE_DOUBLE_SIDED) == 0) {
            return false;
        }
        double inv = 1.0 / det;
        mUa = i00 * inv;
        mUb = i01 * inv;
        mUc = i02 * inv;
        mVa = i10 * inv;
        mVb = i11 * inv;
        mVc = i12 * inv;
        mQa = i20 * inv;
        mQb = i21 * inv;
        mQc = i22 * inv;
        // z_ndc = (uZ, vZ, pZ) · H⁻¹ · (x, y, 1); window z = z_ndc/2 + 1/2.
        mZa = 0.5 * (uZ * mUa + vZ * mVa + pZ * mQa);
        mZb = 0.5 * (uZ * mUb + vZ * mVb + pZ * mQb);
        mZc = 0.5 * (uZ * mUc + vZ * mVc + pZ * mQc) + 0.5;
        mBias = biasConstant != 0f || biasSlope != 0f
                ? biasConstant + biasSlope * Math.max(Math.abs(mZa), Math.abs(mZb)) : 0.0;
        mQEps = HORIZON_EPSILON * (Math.abs(mQa) + Math.abs(mQb));

        if (!computePolygon()) {
            return false;
        }

        // Anchor: the polygon vertex nearest the eye (largest Q), or its centroid when Q is
        // constant (orthographic). Re-center the cell coordinates on the anchor's cell, so they
        // stay small (precise in float) where the grid is visible.
        double bestQ = Double.NEGATIVE_INFINITY;
        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < mPolyCount; i++) {
            double x = mPoly[2 * i];
            double y = mPoly[2 * i + 1];
            sumX += x;
            sumY += y;
            double qv = q(x, y);
            if (qv > bestQ) {
                bestQ = qv;
                mAnchorX = x;
                mAnchorY = y;
            }
        }
        if (Math.abs(mQa) + Math.abs(mQb) == 0.0) {
            mAnchorX = sumX / mPolyCount;
            mAnchorY = sumY / mPolyCount;
        }
        double qAnchor = q(mAnchorX, mAnchorY);
        double ku = Math.floor((mUa * mAnchorX + mUb * mAnchorY + mUc) / qAnchor + 0.5);
        double kv = Math.floor((mVa * mAnchorX + mVb * mAnchorY + mVc) / qAnchor + 0.5);
        if (!isFinite(ku) || !isFinite(kv)) {
            return false;
        }
        mUa -= ku * mQa;
        mUb -= ku * mQb;
        mUc -= ku * mQc;
        mVa -= kv * mQa;
        mVb -= kv * mQb;
        mVc -= kv * mQc;
        mQaF = (float) mQa;
        mQbF = (float) mQb;
        mUaF = (float) mUa;
        mUbF = (float) mUb;
        mVaF = (float) mVa;
        mVbF = (float) mVb;

        // Grid fade bound: on any line of a family, cells are at most Q/dmin px.
        if (mHasGrid) {
            double qq = Math.sqrt(mQa * mQa + mQb * mQb);
            mDminU = qq > 0.0 ? Math.abs(mUa * mQb - mUb * mQa) / qq
                    : Math.sqrt(mUa * mUa + mUb * mUb);
            mDminV = qq > 0.0 ? Math.abs(mVa * mQb - mVb * mQa) / qq
                    : Math.sqrt(mVa * mVa + mVb * mVb);
            mQFade = mFadeLo * Math.min(mDminU, mDminV);
        }

        // Eye space, for shading (the model-view is affine).
        double ex = mv[0] * cx + mv[4] * cy + mv[8] * cz + mv[12];
        double ey = mv[1] * cx + mv[5] * cy + mv[9] * cz + mv[13];
        double ez = mv[2] * cx + mv[6] * cy + mv[10] * cz + mv[14];
        mEux = mv[0] * ux + mv[4] * uy + mv[8] * uz;
        mEuy = mv[1] * ux + mv[5] * uy + mv[9] * uz;
        mEuz = mv[2] * ux + mv[6] * uy + mv[10] * uz;
        mEvx = mv[0] * vx + mv[4] * vy + mv[8] * vz;
        mEvy = mv[1] * vx + mv[5] * vy + mv[9] * vz;
        mEvz = mv[2] * vx + mv[6] * vy + mv[10] * vz;
        mEx = ex + ku * mEux + kv * mEvx;
        mEy = ey + ku * mEuy + kv * mEvy;
        mEz = ez + ku * mEuz + kv * mEvz;
        double nx = mEuy * mEvz - mEuz * mEvy;
        double ny = mEuz * mEvx - mEux * mEvz;
        double nz = mEux * mEvy - mEuy * mEvx;
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl > 0.0) {
            double s = front ? 1.0 / nl : -1.0 / nl;
            nx *= s;
            ny *= s;
            nz *= s;
        } else {
            nx = 0.0;
            ny = 0.0;
            nz = 1.0;
        }
        mNx = (float) nx;
        mNy = (float) ny;
        mNz = (float) nz;
        double au = (mUa * mAnchorX + mUb * mAnchorY + mUc) / qAnchor;
        double av = (mVa * mAnchorX + mVb * mAnchorY + mVc) / qAnchor;
        double ax = mEx + au * mEux + av * mEvx;
        double ay = mEy + au * mEuy + av * mEvy;
        double az = mEz + au * mEuz + av * mEvz;
        double nearest = Math.sqrt(ax * ax + ay * ay + az * az);
        mShadeLimit = SHADE_DISTANCE_LIMIT * (nearest > 0.0 ? nearest : 1.0);
        return true;
    }

    /**
     * Clips the viewport {@code [0, width] × [0, height]} to the visible half-planes into
     * {@link #mPoly}; returns whether at least a triangle is left.
     */
    private boolean computePolygon() {
        double[] p = mPoly;
        p[0] = 0.0;
        p[1] = 0.0;
        p[2] = mWidth;
        p[3] = 0.0;
        p[4] = mWidth;
        p[5] = mHeight;
        p[6] = 0.0;
        p[7] = mHeight;
        int n = 4;
        n = clipPolygon(n, mQa, mQb, mQc - mQEps);
        n = clipPolygon(n, mZa, mZb, mZc);
        n = clipPolygon(n, -mQa, -mQb, 1.0 / NEAR_W_EPSILON - mQc);
        mPolyCount = n >= 3 ? n : 0;
        return n >= 3;
    }

    /** Clips the {@code n}-vertex polygon in {@link #mPoly} to {@code a·x + b·y + c ≥ 0}. */
    private int clipPolygon(int n, double a, double b, double c) {
        if (n < 3) {
            return 0;
        }
        double[] src = mPoly;
        double[] dst = mPolyScratch;
        int m = 0;
        for (int i = 0; i < n; i++) {
            int j = i + 1 == n ? 0 : i + 1;
            double xi = src[2 * i];
            double yi = src[2 * i + 1];
            double xj = src[2 * j];
            double yj = src[2 * j + 1];
            double di = a * xi + b * yi + c;
            double dj = a * xj + b * yj + c;
            if (di >= 0.0) {
                if (m < MAX_POLYGON_VERTICES) {
                    dst[2 * m] = xi;
                    dst[2 * m + 1] = yi;
                    m++;
                }
            }
            if ((di >= 0.0) != (dj >= 0.0) && m < MAX_POLYGON_VERTICES) {
                double t = di / (di - dj);
                dst[2 * m] = xi + t * (xj - xi);
                dst[2 * m + 1] = yi + t * (yj - yi);
                m++;
            }
        }
        System.arraycopy(dst, 0, src, 0, 2 * m);
        return m;
    }

    private static boolean isFinite(double v) {
        return Math.abs(v) <= Double.MAX_VALUE;
    }
}
