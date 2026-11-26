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

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * 28.4 fixed-point edge-function triangle rasterizer with linear-Z interpolation and a
 * depth buffer. The hot path of the pure-Java software 3D backend.
 *
 * <p>Convention: smaller {@code z} = closer. {@link #clearDepth(float[])} fills with
 * {@code +∞}.
 *
 * <p>Edge functions are exact integers. The flat, Gouraud and depth-only fillers evaluate them
 * in 32-bit {@code int} when the triangle's extent guarantees that no evaluated value can
 * overflow (a vertex bounding box up to roughly 2048 × 2048 px), and in 64-bit {@code long}
 * otherwise; the textured filler and the blending ({@code *Blend}, for transparent draws)
 * fillers always use {@code long}. Both paths compute the same values, so every filler covers
 * exactly the same pixels for the same triangle. Vertex coordinates must stay within
 * ±2<sup>26</sup> px of the origin (the 28.4 conversion saturates beyond that); callers clip
 * larger triangles to a guard band first.
 *
 * <p>The color buffer holds straight (not premultiplied) ARGB; the blending fillers composite
 * src-over in that form ({@link #blendSrcOver}).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Rasterizer {
    private Rasterizer() {
    }

    /** Clear the depth buffer to {@code +∞} (so any rasterized z wins on first write). */
    public static void clearDepth(float @NonNull [] zbuf) {
        Arrays.fill(zbuf, Float.POSITIVE_INFINITY);
    }

    /**
     * Rasterize a flat-shaded triangle into {@code img} with a depth test against
     * {@code zbuff}. Vertex order is reversed if the screen-space winding is clockwise so
     * the caller does not need to track winding per-triangle.
     *
     * @param zbuff depth buffer of length {@code w * h}
     * @param img   pixel buffer of length {@code w * h} (ARGB)
     * @param color packed ARGB to write to passing pixels
     */
    public static void fillTriangle(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int color,
            int w,
            int h,
            float fx3,
            float fy3,
            float fz3,
            float fx2,
            float fy2,
            float fz2,
            float fx1,
            float fy1,
            float fz1) {

        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tmpx = fx1;
            float tmpy = fy1;
            float tmpz = fz1;
            fx1 = fx2;
            fy1 = fy2;
            fz1 = fz2;
            fx2 = tmpx;
            fy2 = tmpy;
            fz2 = tmpz;
        }
        final double dfx1 = fx1, dfx2 = fx2, dfx3 = fx3;
        final double dfy1 = fy1, dfy2 = fy2, dfy3 = fy3;
        final double dfz1 = fz1, dfz2 = fz2, dfz3 = fz3;
        // NOTE: For environments that must stay in 32-bit float (like microcontrollers or GPUs),
        // catastrophic cancellation here can be avoided by translating the triangle to the origin
        // first (Edge-Vector formulation):
        // float dx2 = fx2-fx1, dy2 = fy2-fy1, dx3 = fx3-fx1,
        // dy3 = fy3-fy1; float d = dx2*dy3 - dx3*dy2;
        // Or by using Fused Multiply-Add (Math.fma) to prevent intermediate precision loss.
        double d = (dfx1 * (dfy3 - dfy2) - dfx2 * dfy3 + dfx3 * dfy2 + (dfx2 - dfx3) * dfy1);

        if (d == 0) {
            return;
        }
        float dx =
                (float)
                        (-(dfy1 * (dfz3 - dfz2) - dfy2 * dfz3 + dfy3 * dfz2 + (dfy2 - dfy3) * dfz1)
                                / d);
        float dy =
                (float)
                        ((dfx1 * (dfz3 - dfz2) - dfx2 * dfz3 + dfx3 * dfz2 + (dfx2 - dfx3) * dfz1)
                                / d);
        float zoff =
                (float)
                        ((dfx1 * (dfy3 * dfz2 - dfy2 * dfz3)
                                + dfy1 * (dfx2 * dfz3 - dfx3 * dfz2)
                                + (dfx3 * dfy2 - dfx2 * dfy3) * dfz1)
                                / d);

        // 28.4 fixed-point coordinates
        int y1 = (int) (16.0f * fy1 + .5f);
        int y2 = (int) (16.0f * fy2 + .5f);
        int y3 = (int) (16.0f * fy3 + .5f);

        int x1 = (int) (16.0f * fx1 + .5f);
        int x2 = (int) (16.0f * fx2 + .5f);
        int x3 = (int) (16.0f * fx3 + .5f);

        int dx12 = x1 - x2;
        int dx23 = x2 - x3;
        int dx31 = x3 - x1;

        int dy12 = y1 - y2;
        int dy23 = y2 - y3;
        int dy31 = y3 - y1;

        int fdx12 = dx12 << 4;
        int fdx23 = dx23 << 4;
        int fdx31 = dx31 << 4;

        int fdy12 = dy12 << 4;
        int fdy23 = dy23 << 4;
        int fdy31 = dy31 << 4;

        int vminx = min(x1, x2, x3);
        int vmaxx = max(x1, x2, x3);
        int vminy = min(y1, y2, y3);
        int vmaxy = max(y1, y2, y3);
        int minx = (vminx + 0xF) >> 4;
        int maxx = (vmaxx + 0xF) >> 4;
        int miny = (vminy + 0xF) >> 4;
        int maxy = (vmaxy + 0xF) >> 4;

        if (miny < 0) {
            miny = 0;
        }
        if (minx < 0) {
            minx = 0;
        }
        if (maxx > w) {
            maxx = w;
        }
        if (maxy > h) {
            maxy = h;
        }
        if (minx >= maxx || miny >= maxy) {
            return; // no pixel centre of the buffer can be covered
        }
        if (!edgesFitInt((long) vmaxx - vminx, (long) vmaxy - vminy)) {
            fillTriangleLong(zbuff, img, color, w, minx, maxx, miny, maxy,
                    x1, y1, x2, y2, x3, y3, dx, dy, zoff);
            return;
        }
        int off = miny * w;

        int c1 = dy12 * x1 - dx12 * y1;
        int c2 = dy23 * x2 - dx23 * y2;
        int c3 = dy31 * x3 - dx31 * y3;

        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) {
            c1++;
        }
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) {
            c2++;
        }
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) {
            c3++;
        }
        int cy1 = c1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        int cy2 = c2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        int cy3 = c3 + dx31 * (miny << 4) - dy31 * (minx << 4);

        for (int y = miny; y < maxy; y++) {
            int cx1 = cy1;
            int cx2 = cy2;
            int cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        zbuff[point] = zval;
                        img[point] = color;
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * Rasterize a Gouraud-shaded (per-vertex color) triangle into {@code img} with a depth
     * test against {@code zbuff}. Identical coverage/winding/Z handling to
     * {@link #fillTriangle}; the only difference is each covered pixel's ARGB is
     * barycentrically interpolated from the three vertex colors instead of a single flat
     * color. {@code c3/c2/c1} pair with {@code (fx3,fy3,fz3) / (fx2,fy2,fz2) / (fx1,fy1,fz1)}.
     */
    public static void fillTriangleGouraud(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int c3,
            int c2,
            int c1,
            int w,
            int h,
            float fx3,
            float fy3,
            float fz3,
            float fx2,
            float fy2,
            float fz2,
            float fx1,
            float fy1,
            float fz1) {

        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tmpx = fx1;
            float tmpy = fy1;
            float tmpz = fz1;
            fx1 = fx2;
            fy1 = fy2;
            fz1 = fz2;
            fx2 = tmpx;
            fy2 = tmpy;
            fz2 = tmpz;
            int tmpc = c1;
            c1 = c2;
            c2 = tmpc;
        }
        final double dfx1 = fx1, dfx2 = fx2, dfx3 = fx3;
        final double dfy1 = fy1, dfy2 = fy2, dfy3 = fy3;
        final double dfz1 = fz1, dfz2 = fz2, dfz3 = fz3;
        // NOTE: For environments that must stay in 32-bit float (like microcontrollers or GPUs),
        // catastrophic cancellation here can be avoided by translating the triangle to the origin
        // first (Edge-Vector formulation):
        // float dx2 = fx2-fx1, dy2 = fy2-fy1, dx3 = fx3-fx1,
        // dy3 = fy3-fy1; float d = dx2*dy3 - dx3*dy2;
        // Or by using Fused Multiply-Add (Math.fma) to prevent intermediate precision loss.
        double d = (dfx1 * (dfy3 - dfy2) - dfx2 * dfy3 + dfx3 * dfy2 + (dfx2 - dfx3) * dfy1);

        if (d == 0) {
            return;
        }
        float dx =
                (float)
                        (-(dfy1 * (dfz3 - dfz2) - dfy2 * dfz3 + dfy3 * dfz2 + (dfy2 - dfy3) * dfz1)
                                / d);
        float dy =
                (float)
                        ((dfx1 * (dfz3 - dfz2) - dfx2 * dfz3 + dfx3 * dfz2 + (dfx2 - dfx3) * dfz1)
                                / d);
        float zoff =
                (float)
                        ((dfx1 * (dfy3 * dfz2 - dfy2 * dfz3)
                                + dfy1 * (dfx2 * dfz3 - dfx3 * dfz2)
                                + (dfx3 * dfy2 - dfx2 * dfy3) * dfz1)
                                / d);

        int y1 = (int) (16.0f * fy1 + .5f);
        int y2 = (int) (16.0f * fy2 + .5f);
        int y3 = (int) (16.0f * fy3 + .5f);

        int x1 = (int) (16.0f * fx1 + .5f);
        int x2 = (int) (16.0f * fx2 + .5f);
        int x3 = (int) (16.0f * fx3 + .5f);

        int dx12 = x1 - x2;
        int dx23 = x2 - x3;
        int dx31 = x3 - x1;

        int dy12 = y1 - y2;
        int dy23 = y2 - y3;
        int dy31 = y3 - y1;

        int fdx12 = dx12 << 4;
        int fdx23 = dx23 << 4;
        int fdx31 = dx31 << 4;

        int fdy12 = dy12 << 4;
        int fdy23 = dy23 << 4;
        int fdy31 = dy31 << 4;

        int vminx = min(x1, x2, x3);
        int vmaxx = max(x1, x2, x3);
        int vminy = min(y1, y2, y3);
        int vmaxy = max(y1, y2, y3);
        int minx = (vminx + 0xF) >> 4;
        int maxx = (vmaxx + 0xF) >> 4;
        int miny = (vminy + 0xF) >> 4;
        int maxy = (vmaxy + 0xF) >> 4;

        if (miny < 0) {
            miny = 0;
        }
        if (minx < 0) {
            minx = 0;
        }
        if (maxx > w) {
            maxx = w;
        }
        if (maxy > h) {
            maxy = h;
        }
        if (minx >= maxx || miny >= maxy) {
            return; // no pixel centre of the buffer can be covered
        }
        if (!edgesFitInt((long) vmaxx - vminx, (long) vmaxy - vminy)) {
            fillTriangleGouraudLong(zbuff, img, c3, c2, c1, w, minx, maxx, miny, maxy,
                    x1, y1, x2, y2, x3, y3, dx, dy, zoff);
            return;
        }
        int off = miny * w;

        int cc1 = dy12 * x1 - dx12 * y1;
        int cc2 = dy23 * x2 - dx23 * y2;
        int cc3 = dy31 * x3 - dx31 * y3;

        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) {
            cc1++;
        }
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) {
            cc2++;
        }
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) {
            cc3++;
        }
        int cy1 = cc1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        int cy2 = cc2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        int cy3 = cc3 + dx31 * (miny << 4) - dy31 * (minx << 4);

        // Edge sum is constant across the triangle and proportional to twice its area; use
        // it to normalize the edge functions into barycentric weights. CX1 ↔ vertex 3,
        // CX2 ↔ vertex 1, CX3 ↔ vertex 2.
        long s = (long) cy1 + (long) cy2 + (long) cy3;
        if (s == 0) {
            return;
        }
        float inv = 1.0f / s;

        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a3 = (c3 >>> 24) & 0xFF, r3 = (c3 >>> 16) & 0xFF, g3 = (c3 >>> 8) & 0xFF;
        int b3 = c3 & 0xFF;
        // A single vertex alpha (e.g. opaque) is copied exactly: interpolating it with float
        // weights that sum to just under 1 would truncate 255 to 254.
        boolean oneAlpha = a1 == a2 && a2 == a3;

        for (int y = miny; y < maxy; y++) {
            int cx1 = cy1;
            int cx2 = cy2;
            int cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        zbuff[point] = zval;
                        float w1 = cx2 * inv;
                        float w2 = cx3 * inv;
                        float w3 = cx1 * inv;
                        int a = oneAlpha ? a1 : lerpAlpha(w1, w2, w3, a1, a2, a3);
                        int r = (int) (w1 * r1 + w2 * r2 + w3 * r3);
                        int g = (int) (w1 * g1 + w2 * g2 + w3 * g3);
                        int b = (int) (w1 * b1 + w2 * b2 + w3 * b3);
                        img[point] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * Rasterize a textured triangle with <b>perspective-correct</b> UV: {@code u/w}, {@code
     * v/w} and {@code 1/w} are interpolated linearly in screen space and divided per pixel
     * (where {@code iwK = 1/clipW} of vertex K), so the texture stays attached to the surface
     * under foreshortening. The texture ({@code tex}, ARGB {@code int[texW*texH]}) is sampled
     * nearest with v flipped (top-down bitmap) and CLAMP, then modulated by the interpolated
     * (affine) per-vertex lighting color {@code c1/c2/c3}. Same coverage/winding/Z handling as
     * {@link #fillTriangle}; {@code c3/u3,v3/iw3} pair with {@code (fx3,…)}, etc.
     */
    public static void fillTriangleTextured(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int @NonNull [] tex,
            int texW,
            int texH,
            int c3,
            int c2,
            int c1,
            float u3,
            float v3,
            float u2,
            float v2,
            float u1,
            float v1,
            float iw3,
            float iw2,
            float iw1,
            int w,
            int h,
            float fx3,
            float fy3,
            float fz3,
            float fx2,
            float fy2,
            float fz2,
            float fx1,
            float fy1,
            float fz1) {

        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tmpx = fx1, tmpy = fy1, tmpz = fz1;
            fx1 = fx2;
            fy1 = fy2;
            fz1 = fz2;
            fx2 = tmpx;
            fy2 = tmpy;
            fz2 = tmpz;
            int tc = c1;
            c1 = c2;
            c2 = tc;
            float tu = u1, tv = v1;
            u1 = u2;
            v1 = v2;
            u2 = tu;
            v2 = tv;
            float tiw = iw1;
            iw1 = iw2;
            iw2 = tiw;
        }
        final double dfx1 = fx1, dfx2 = fx2, dfx3 = fx3;
        final double dfy1 = fy1, dfy2 = fy2, dfy3 = fy3;
        final double dfz1 = fz1, dfz2 = fz2, dfz3 = fz3;
        // NOTE: For environments that must stay in 32-bit float (like microcontrollers or GPUs),
        // catastrophic cancellation here can be avoided by translating the triangle to the origin
        // first (Edge-Vector formulation):
        // float dx2 = fx2-fx1, dy2 = fy2-fy1, dx3 = fx3-fx1,
        // dy3 = fy3-fy1; float d = dx2*dy3 - dx3*dy2;
        // Or by using Fused Multiply-Add (Math.fma) to prevent intermediate precision loss.
        double d = (dfx1 * (dfy3 - dfy2) - dfx2 * dfy3 + dfx3 * dfy2 + (dfx2 - dfx3) * dfy1);

        if (d == 0) {
            return;
        }
        float dx =
                (float)
                        (-(dfy1 * (dfz3 - dfz2) - dfy2 * dfz3 + dfy3 * dfz2 + (dfy2 - dfy3) * dfz1)
                                / d);
        float dy =
                (float)
                        ((dfx1 * (dfz3 - dfz2) - dfx2 * dfz3 + dfx3 * dfz2 + (dfx2 - dfx3) * dfz1)
                                / d);
        float zoff =
                (float)
                        ((dfx1 * (dfy3 * dfz2 - dfy2 * dfz3)
                                + dfy1 * (dfx2 * dfz3 - dfx3 * dfz2)
                                + (dfx3 * dfy2 - dfx2 * dfy3) * dfz1)
                                / d);

        int y1 = (int) (16.0f * fy1 + .5f), y2 = (int) (16.0f * fy2 + .5f),
                y3 = (int) (16.0f * fy3 + .5f);
        int x1 = (int) (16.0f * fx1 + .5f), x2 = (int) (16.0f * fx2 + .5f),
                x3 = (int) (16.0f * fx3 + .5f);

        int minx = (min(x1, x2, x3) + 0xF) >> 4;
        int maxx = (max(x1, x2, x3) + 0xF) >> 4;
        int miny = (min(y1, y2, y3) + 0xF) >> 4;
        int maxy = (max(y1, y2, y3) + 0xF) >> 4;
        if (miny < 0) miny = 0;
        if (minx < 0) minx = 0;
        if (maxx > w) maxx = w;
        if (maxy > h) maxy = h;
        if (minx >= maxx || miny >= maxy) {
            return; // no pixel centre of the buffer can be covered
        }
        int off = miny * w;

        // Edge functions use long: this textured path handles large/grazing triangles whose
        // off-screen vertices project to coordinates that would overflow 28.4 int math. The
        // deltas and steps are long as well, so nothing wraps anywhere in the documented range.
        long dx12 = (long) x1 - x2, dx23 = (long) x2 - x3, dx31 = (long) x3 - x1;
        long dy12 = (long) y1 - y2, dy23 = (long) y2 - y3, dy31 = (long) y3 - y1;
        long fdx12 = dx12 << 4, fdx23 = dx23 << 4, fdx31 = dx31 << 4;
        long fdy12 = dy12 << 4, fdy23 = dy23 << 4, fdy31 = dy31 << 4;
        long cc1 = dy12 * x1 - dx12 * y1;
        long cc2 = dy23 * x2 - dx23 * y2;
        long cc3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) cc1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) cc2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) cc3++;
        long cy1 = cc1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        long cy2 = cc2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        long cy3 = cc3 + dx31 * (miny << 4) - dy31 * (minx << 4);

        long s = cy1 + cy2 + cy3;
        if (s == 0) {
            return;
        }
        float inv = 1.0f / s;

        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 =
                c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 =
                c2 & 0xFF;
        int a3 = (c3 >>> 24) & 0xFF, r3 = (c3 >>> 16) & 0xFF, g3 = (c3 >>> 8) & 0xFF, b3 =
                c3 & 0xFF;
        boolean oneAlpha = a1 == a2 && a2 == a3;

        // Perspective-correct: interpolate u/w, v/w and 1/w, then divide per pixel.
        float uiw1 = u1 * iw1, uiw2 = u2 * iw2, uiw3 = u3 * iw3;
        float viw1 = v1 * iw1, viw2 = v2 * iw2, viw3 = v3 * iw3;

        for (int y = miny; y < maxy; y++) {
            long cx1 = cy1, cx2 = cy2, cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        float w1 = cx2 * inv, w2 = cx3 * inv, w3 = cx1 * inv;
                        float invW = 1f / (w1 * iw1 + w2 * iw2 + w3 * iw3);
                        float u = (w1 * uiw1 + w2 * uiw2 + w3 * uiw3) * invW;
                        float v = (w1 * viw1 + w2 * viw2 + w3 * viw3) * invW;
                        int tx = (int) (u * texW);
                        int ty = (int) ((1f - v) * texH);
                        if (tx < 0) {
                            tx = 0;
                        } else if (tx >= texW) {
                            tx = texW - 1;
                        }
                        if (ty < 0) {
                            ty = 0;
                        } else if (ty >= texH) {
                            ty = texH - 1;
                        }
                        int t = tex[ty * texW + tx];
                        int ta = (t >>> 24) & 0xFF;
                        // Treat fully-transparent texels as cutouts: write neither color nor
                        // depth, so a textured cutout (e.g. a grid wall) never occludes what is
                        // behind its holes. The texel's own alpha is kept and composited by the
                        // host blit (drawBitmap source-over) onto the 2D background.
                        if (ta != 0) {
                            zbuff[point] = zval;
                            int la = oneAlpha ? a1 : lerpAlpha(w1, w2, w3, a1, a2, a3);
                            int lr = (int) (w1 * r1 + w2 * r2 + w3 * r3);
                            int lg = (int) (w1 * g1 + w2 * g2 + w3 * g3);
                            int lb = (int) (w1 * b1 + w2 * b2 + w3 * b3);
                            int a = (ta * la) / 255;
                            int r = (((t >>> 16) & 0xFF) * lr) / 255;
                            int g = (((t >>> 8) & 0xFF) * lg) / 255;
                            int b = ((t & 0xFF) * lb) / 255;
                            img[point] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * Rasterize a triangle's coverage into {@code zbuff} only — depth test + depth write, but
     * <b>no</b> color write. The depth-buffer prepass of the wireframe path
     * ({@link androidx.compose.remote.core.Paint3DContext#MODE_WIREFRAME}): it lays down the
     * solid's depth (transparent faces) so {@link #drawLineDepthTested} can hide edges behind it.
     * Same coverage/winding/Z handling as {@link #fillTriangle}.
     */
    public static void fillTriangleDepthOnly(
            float @NonNull [] zbuff,
            int w,
            int h,
            float fx3,
            float fy3,
            float fz3,
            float fx2,
            float fy2,
            float fz2,
            float fx1,
            float fy1,
            float fz1) {

        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tmpx = fx1;
            float tmpy = fy1;
            float tmpz = fz1;
            fx1 = fx2;
            fy1 = fy2;
            fz1 = fz2;
            fx2 = tmpx;
            fy2 = tmpy;
            fz2 = tmpz;
        }
        final double dfx1 = fx1, dfx2 = fx2, dfx3 = fx3;
        final double dfy1 = fy1, dfy2 = fy2, dfy3 = fy3;
        final double dfz1 = fz1, dfz2 = fz2, dfz3 = fz3;
        // NOTE: For environments that must stay in 32-bit float (like microcontrollers or GPUs),
        // catastrophic cancellation here can be avoided by translating the triangle to the origin
        // first (Edge-Vector formulation):
        // float dx2 = fx2-fx1, dy2 = fy2-fy1, dx3 = fx3-fx1,
        // dy3 = fy3-fy1; float d = dx2*dy3 - dx3*dy2;
        // Or by using Fused Multiply-Add (Math.fma) to prevent intermediate precision loss.
        double d = (dfx1 * (dfy3 - dfy2) - dfx2 * dfy3 + dfx3 * dfy2 + (dfx2 - dfx3) * dfy1);

        if (d == 0) {
            return;
        }
        float dx =
                (float)
                        (-(dfy1 * (dfz3 - dfz2) - dfy2 * dfz3 + dfy3 * dfz2 + (dfy2 - dfy3) * dfz1)
                                / d);
        float dy =
                (float)
                        ((dfx1 * (dfz3 - dfz2) - dfx2 * dfz3 + dfx3 * dfz2 + (dfx2 - dfx3) * dfz1)
                                / d);
        float zoff =
                (float)
                        ((dfx1 * (dfy3 * dfz2 - dfy2 * dfz3)
                                + dfy1 * (dfx2 * dfz3 - dfx3 * dfz2)
                                + (dfx3 * dfy2 - dfx2 * dfy3) * dfz1)
                                / d);

        int y1 = (int) (16.0f * fy1 + .5f);
        int y2 = (int) (16.0f * fy2 + .5f);
        int y3 = (int) (16.0f * fy3 + .5f);
        int x1 = (int) (16.0f * fx1 + .5f);
        int x2 = (int) (16.0f * fx2 + .5f);
        int x3 = (int) (16.0f * fx3 + .5f);

        int dx12 = x1 - x2;
        int dx23 = x2 - x3;
        int dx31 = x3 - x1;
        int dy12 = y1 - y2;
        int dy23 = y2 - y3;
        int dy31 = y3 - y1;
        int fdy12 = dy12 << 4;
        int fdy23 = dy23 << 4;
        int fdy31 = dy31 << 4;
        int fdx12 = dx12 << 4;
        int fdx23 = dx23 << 4;
        int fdx31 = dx31 << 4;

        int vminx = min(x1, x2, x3);
        int vmaxx = max(x1, x2, x3);
        int vminy = min(y1, y2, y3);
        int vmaxy = max(y1, y2, y3);
        int minx = (vminx + 0xF) >> 4;
        int maxx = (vmaxx + 0xF) >> 4;
        int miny = (vminy + 0xF) >> 4;
        int maxy = (vmaxy + 0xF) >> 4;
        if (miny < 0) miny = 0;
        if (minx < 0) minx = 0;
        if (maxx > w) maxx = w;
        if (maxy > h) maxy = h;
        if (minx >= maxx || miny >= maxy) {
            return; // no pixel centre of the buffer can be covered
        }
        if (!edgesFitInt((long) vmaxx - vminx, (long) vmaxy - vminy)) {
            fillTriangleDepthOnlyLong(zbuff, w, minx, maxx, miny, maxy,
                    x1, y1, x2, y2, x3, y3, dx, dy, zoff);
            return;
        }
        int off = miny * w;

        int c1 = dy12 * x1 - dx12 * y1;
        int c2 = dy23 * x2 - dx23 * y2;
        int c3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) c1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) c2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) c3++;
        int cy1 = c1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        int cy2 = c2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        int cy3 = c3 + dx31 * (miny << 4) - dy31 * (minx << 4);

        for (int y = miny; y < maxy; y++) {
            int cx1 = cy1;
            int cx2 = cy2;
            int cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        zbuff[point] = zval;
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * Draw a depth-tested 3D line into {@code img} (color only — does <b>not</b> write the depth
     * buffer). Screen-space DDA between the two endpoints with linearly interpolated z; a covered
     * pixel is written when its (camera-ward biased) z passes the test against {@code zbuff}.
     *
     * <p>The {@code bias} (in window-z units, smaller = closer) is subtracted from the line's z
     * before the test so an edge passes against <i>its own</i> face — which wrote the same z in
     * the depth prepass — while still being occluded by clearly nearer surface. This is the
     * hidden-line removal used by
     * {@link androidx.compose.remote.core.Paint3DContext#MODE_WIREFRAME}.
     */
    public static void drawLineDepthTested(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int color,
            int w,
            int h,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float bias) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float adx = dx < 0 ? -dx : dx;
        float ady = dy < 0 ? -dy : dy;
        int steps = (int) (adx > ady ? adx : ady);
        if (steps < 1) steps = 1;
        float inv = 1f / steps;
        float sx = dx * inv;
        float sy = dy * inv;
        float sz = (z1 - z0) * inv;
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 0; i <= steps; i++) {
            int ix = (int) px;
            int iy = (int) py;
            if (ix >= 0 && ix < w && iy >= 0 && iy < h) {
                int point = ix + iy * w;
                if (zbuff[point] >= pz - bias) {
                    img[point] = color;
                }
            }
            px += sx;
            py += sy;
            pz += sz;
        }
    }

    /**
     * {@link #drawLineDepthTested}, blending {@code color} src-over ({@link #blendSrcOver}) into
     * the covered pixels instead of replacing them: the wireframe of a
     * {@link androidx.compose.remote.core.Paint3DContext#MODE_TRANSPARENT} draw.
     */
    public static void drawLineDepthTestedBlend(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int color,
            int w,
            int h,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float bias) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float adx = dx < 0 ? -dx : dx;
        float ady = dy < 0 ? -dy : dy;
        int steps = (int) (adx > ady ? adx : ady);
        if (steps < 1) steps = 1;
        float inv = 1f / steps;
        float sx = dx * inv;
        float sy = dy * inv;
        float sz = (z1 - z0) * inv;
        float px = x0;
        float py = y0;
        float pz = z0;
        for (int i = 0; i <= steps; i++) {
            int ix = (int) px;
            int iy = (int) py;
            if (ix >= 0 && ix < w && iy >= 0 && iy < h) {
                int point = ix + iy * w;
                if (zbuff[point] >= pz - bias) {
                    img[point] = blendSrcOver(color, img[point]);
                }
            }
            px += sx;
            py += sy;
            pz += sz;
        }
    }

    /**
     * Blend a triangle src-over into {@code img}, for
     * {@link androidx.compose.remote.core.Paint3DContext#MODE_TRANSPARENT}: each covered pixel
     * whose depth passes the test against {@code zbuff} is blended with {@link #blendSrcOver},
     * and the depth buffer is not written. Covers the same pixels, with the same depth, as
     * {@link #fillTriangleGouraud} (edge functions are always 64-bit here, which evaluate the
     * same values). The colors are interpolated as there, except that equal colors are blended
     * as given and equal alphas stay exact. {@code c3/c2/c1} pair with
     * {@code (fx3,fy3,fz3) / (fx2,fy2,fz2) / (fx1,fy1,fz1)}.
     */
    public static void fillTriangleBlend(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int c3,
            int c2,
            int c1,
            int w,
            int h,
            float fx3,
            float fy3,
            float fz3,
            float fx2,
            float fy2,
            float fz2,
            float fx1,
            float fy1,
            float fz1) {

        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tmpx = fx1, tmpy = fy1, tmpz = fz1;
            fx1 = fx2;
            fy1 = fy2;
            fz1 = fz2;
            fx2 = tmpx;
            fy2 = tmpy;
            fz2 = tmpz;
            int tc = c1;
            c1 = c2;
            c2 = tc;
        }
        final double dfx1 = fx1, dfx2 = fx2, dfx3 = fx3;
        final double dfy1 = fy1, dfy2 = fy2, dfy3 = fy3;
        final double dfz1 = fz1, dfz2 = fz2, dfz3 = fz3;
        // The depth plane exactly as the opaque fillers compute it, so a transparent surface
        // and an opaque one through the same points get the same depths.
        double d = (dfx1 * (dfy3 - dfy2) - dfx2 * dfy3 + dfx3 * dfy2 + (dfx2 - dfx3) * dfy1);

        if (d == 0) {
            return;
        }
        float dx =
                (float)
                        (-(dfy1 * (dfz3 - dfz2) - dfy2 * dfz3 + dfy3 * dfz2 + (dfy2 - dfy3) * dfz1)
                                / d);
        float dy =
                (float)
                        ((dfx1 * (dfz3 - dfz2) - dfx2 * dfz3 + dfx3 * dfz2 + (dfx2 - dfx3) * dfz1)
                                / d);
        float zoff =
                (float)
                        ((dfx1 * (dfy3 * dfz2 - dfy2 * dfz3)
                                + dfy1 * (dfx2 * dfz3 - dfx3 * dfz2)
                                + (dfx3 * dfy2 - dfx2 * dfy3) * dfz1)
                                / d);

        int y1 = (int) (16.0f * fy1 + .5f), y2 = (int) (16.0f * fy2 + .5f),
                y3 = (int) (16.0f * fy3 + .5f);
        int x1 = (int) (16.0f * fx1 + .5f), x2 = (int) (16.0f * fx2 + .5f),
                x3 = (int) (16.0f * fx3 + .5f);

        int minx = (min(x1, x2, x3) + 0xF) >> 4;
        int maxx = (max(x1, x2, x3) + 0xF) >> 4;
        int miny = (min(y1, y2, y3) + 0xF) >> 4;
        int maxy = (max(y1, y2, y3) + 0xF) >> 4;
        if (miny < 0) miny = 0;
        if (minx < 0) minx = 0;
        if (maxx > w) maxx = w;
        if (maxy > h) maxy = h;
        if (minx >= maxx || miny >= maxy) {
            return; // no pixel centre of the buffer can be covered
        }
        int off = miny * w;

        long dx12 = (long) x1 - x2, dx23 = (long) x2 - x3, dx31 = (long) x3 - x1;
        long dy12 = (long) y1 - y2, dy23 = (long) y2 - y3, dy31 = (long) y3 - y1;
        long fdx12 = dx12 << 4, fdx23 = dx23 << 4, fdx31 = dx31 << 4;
        long fdy12 = dy12 << 4, fdy23 = dy23 << 4, fdy31 = dy31 << 4;
        long cc1 = dy12 * x1 - dx12 * y1;
        long cc2 = dy23 * x2 - dx23 * y2;
        long cc3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) cc1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) cc2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) cc3++;
        long cy1 = cc1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        long cy2 = cc2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        long cy3 = cc3 + dx31 * (miny << 4) - dy31 * (minx << 4);

        long s = cy1 + cy2 + cy3;
        if (s == 0) {
            return;
        }

        if (c1 == c2 && c2 == c3) {
            for (int y = miny; y < maxy; y++) {
                long cx1 = cy1, cx2 = cy2, cx3 = cy3;
                float p = zoff + dy * y;
                for (int x = minx; x < maxx; x++) {
                    if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                        int point = x + off;
                        float zval = p + dx * x;
                        if (zbuff[point] > zval) {
                            img[point] = blendSrcOver(c1, img[point]);
                        }
                    }
                    cx1 -= fdy12;
                    cx2 -= fdy23;
                    cx3 -= fdy31;
                }
                cy1 += fdx12;
                cy2 += fdx23;
                cy3 += fdx31;
                off += w;
            }
            return;
        }

        float inv = 1.0f / s;
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a3 = (c3 >>> 24) & 0xFF, r3 = (c3 >>> 16) & 0xFF, g3 = (c3 >>> 8) & 0xFF;
        int b3 = c3 & 0xFF;
        boolean oneAlpha = a1 == a2 && a2 == a3;

        for (int y = miny; y < maxy; y++) {
            long cx1 = cy1, cx2 = cy2, cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        float w1 = cx2 * inv, w2 = cx3 * inv, w3 = cx1 * inv;
                        int a = oneAlpha ? a1 : lerpAlpha(w1, w2, w3, a1, a2, a3);
                        int r = (int) (w1 * r1 + w2 * r2 + w3 * r3);
                        int g = (int) (w1 * g1 + w2 * g2 + w3 * g3);
                        int b = (int) (w1 * b1 + w2 * b2 + w3 * b3);
                        img[point] =
                                blendSrcOver((a << 24) | (r << 16) | (g << 8) | b, img[point]);
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * {@link #fillTriangleTextured} for
     * {@link androidx.compose.remote.core.Paint3DContext#MODE_TRANSPARENT}: same coverage,
     * depth, texture sampling and lighting, but each passing pixel is blended src-over
     * ({@link #blendSrcOver}) with alpha = lit alpha × texel alpha, and the depth buffer is not
     * written. Fully transparent texels are skipped.
     */
    public static void fillTriangleTexturedBlend(
            float @NonNull [] zbuff,
            int @NonNull [] img,
            int @NonNull [] tex,
            int texW,
            int texH,
            int c3,
            int c2,
            int c1,
            float u3,
            float v3,
            float u2,
            float v2,
            float u1,
            float v1,
            float iw3,
            float iw2,
            float iw1,
            int w,
            int h,
            float fx3,
            float fy3,
            float fz3,
            float fx2,
            float fy2,
            float fz2,
            float fx1,
            float fy1,
            float fz1) {

        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tmpx = fx1, tmpy = fy1, tmpz = fz1;
            fx1 = fx2;
            fy1 = fy2;
            fz1 = fz2;
            fx2 = tmpx;
            fy2 = tmpy;
            fz2 = tmpz;
            int tc = c1;
            c1 = c2;
            c2 = tc;
            float tu = u1, tv = v1;
            u1 = u2;
            v1 = v2;
            u2 = tu;
            v2 = tv;
            float tiw = iw1;
            iw1 = iw2;
            iw2 = tiw;
        }
        final double dfx1 = fx1, dfx2 = fx2, dfx3 = fx3;
        final double dfy1 = fy1, dfy2 = fy2, dfy3 = fy3;
        final double dfz1 = fz1, dfz2 = fz2, dfz3 = fz3;
        double d = (dfx1 * (dfy3 - dfy2) - dfx2 * dfy3 + dfx3 * dfy2 + (dfx2 - dfx3) * dfy1);

        if (d == 0) {
            return;
        }
        float dx =
                (float)
                        (-(dfy1 * (dfz3 - dfz2) - dfy2 * dfz3 + dfy3 * dfz2 + (dfy2 - dfy3) * dfz1)
                                / d);
        float dy =
                (float)
                        ((dfx1 * (dfz3 - dfz2) - dfx2 * dfz3 + dfx3 * dfz2 + (dfx2 - dfx3) * dfz1)
                                / d);
        float zoff =
                (float)
                        ((dfx1 * (dfy3 * dfz2 - dfy2 * dfz3)
                                + dfy1 * (dfx2 * dfz3 - dfx3 * dfz2)
                                + (dfx3 * dfy2 - dfx2 * dfy3) * dfz1)
                                / d);

        int y1 = (int) (16.0f * fy1 + .5f), y2 = (int) (16.0f * fy2 + .5f),
                y3 = (int) (16.0f * fy3 + .5f);
        int x1 = (int) (16.0f * fx1 + .5f), x2 = (int) (16.0f * fx2 + .5f),
                x3 = (int) (16.0f * fx3 + .5f);

        int minx = (min(x1, x2, x3) + 0xF) >> 4;
        int maxx = (max(x1, x2, x3) + 0xF) >> 4;
        int miny = (min(y1, y2, y3) + 0xF) >> 4;
        int maxy = (max(y1, y2, y3) + 0xF) >> 4;
        if (miny < 0) miny = 0;
        if (minx < 0) minx = 0;
        if (maxx > w) maxx = w;
        if (maxy > h) maxy = h;
        if (minx >= maxx || miny >= maxy) {
            return; // no pixel centre of the buffer can be covered
        }
        int off = miny * w;

        long dx12 = (long) x1 - x2, dx23 = (long) x2 - x3, dx31 = (long) x3 - x1;
        long dy12 = (long) y1 - y2, dy23 = (long) y2 - y3, dy31 = (long) y3 - y1;
        long fdx12 = dx12 << 4, fdx23 = dx23 << 4, fdx31 = dx31 << 4;
        long fdy12 = dy12 << 4, fdy23 = dy23 << 4, fdy31 = dy31 << 4;
        long cc1 = dy12 * x1 - dx12 * y1;
        long cc2 = dy23 * x2 - dx23 * y2;
        long cc3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) cc1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) cc2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) cc3++;
        long cy1 = cc1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        long cy2 = cc2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        long cy3 = cc3 + dx31 * (miny << 4) - dy31 * (minx << 4);

        long s = cy1 + cy2 + cy3;
        if (s == 0) {
            return;
        }
        float inv = 1.0f / s;

        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 =
                c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 =
                c2 & 0xFF;
        int a3 = (c3 >>> 24) & 0xFF, r3 = (c3 >>> 16) & 0xFF, g3 = (c3 >>> 8) & 0xFF, b3 =
                c3 & 0xFF;
        boolean oneAlpha = a1 == a2 && a2 == a3;

        float uiw1 = u1 * iw1, uiw2 = u2 * iw2, uiw3 = u3 * iw3;
        float viw1 = v1 * iw1, viw2 = v2 * iw2, viw3 = v3 * iw3;

        for (int y = miny; y < maxy; y++) {
            long cx1 = cy1, cx2 = cy2, cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        float w1 = cx2 * inv, w2 = cx3 * inv, w3 = cx1 * inv;
                        float invW = 1f / (w1 * iw1 + w2 * iw2 + w3 * iw3);
                        float u = (w1 * uiw1 + w2 * uiw2 + w3 * uiw3) * invW;
                        float v = (w1 * viw1 + w2 * viw2 + w3 * viw3) * invW;
                        int tx = (int) (u * texW);
                        int ty = (int) ((1f - v) * texH);
                        if (tx < 0) {
                            tx = 0;
                        } else if (tx >= texW) {
                            tx = texW - 1;
                        }
                        if (ty < 0) {
                            ty = 0;
                        } else if (ty >= texH) {
                            ty = texH - 1;
                        }
                        int t = tex[ty * texW + tx];
                        int ta = (t >>> 24) & 0xFF;
                        if (ta != 0) {
                            int la = oneAlpha ? a1 : lerpAlpha(w1, w2, w3, a1, a2, a3);
                            int lr = (int) (w1 * r1 + w2 * r2 + w3 * r3);
                            int lg = (int) (w1 * g1 + w2 * g2 + w3 * g3);
                            int lb = (int) (w1 * b1 + w2 * b2 + w3 * b3);
                            int a = (ta * la) / 255;
                            int r = (((t >>> 16) & 0xFF) * lr) / 255;
                            int g = (((t >>> 8) & 0xFF) * lg) / 255;
                            int b = ((t & 0xFF) * lb) / 255;
                            img[point] = blendSrcOver((a << 24) | (r << 16) | (g << 8) | b,
                                    img[point]);
                        }
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * {@code src} over {@code dst}, both straight (not premultiplied) ARGB, which is how the
     * color buffer holds them (the host composites it with {@code Bitmap.setPixels}). Exact when
     * the source is opaque or fully transparent or the destination is empty; otherwise the output
     * alpha and each channel are rounded to the nearest 8-bit value. Associative up to that
     * rounding, so blending into a cleared pending rectangle and compositing later gives the same
     * result as blending onto the composited pixels. The one blend of the software engine (the
     * transparent fillers and the infinite plane).
     */
    static int blendSrcOver(int src, int dst) {
        int sa = src >>> 24;
        if (sa == 255) {
            return src;
        }
        if (sa == 0) {
            return dst;
        }
        int da = dst >>> 24;
        if (da == 0) {
            return src;
        }
        int ia = 255 - sa;
        if (da == 255) {
            int r = (((src >>> 16) & 0xFF) * sa + ((dst >>> 16) & 0xFF) * ia + 127) / 255;
            int g = (((src >>> 8) & 0xFF) * sa + ((dst >>> 8) & 0xFF) * ia + 127) / 255;
            int b = ((src & 0xFF) * sa + (dst & 0xFF) * ia + 127) / 255;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        // Output alpha × 255², and each color weighted by its alpha (× 255²).
        int ws = sa * 255;
        int wd = da * ia;
        int wo = ws + wd;
        int half = wo >> 1;
        int r = (((src >>> 16) & 0xFF) * ws + ((dst >>> 16) & 0xFF) * wd + half) / wo;
        int g = (((src >>> 8) & 0xFF) * ws + ((dst >>> 8) & 0xFF) * wd + half) / wo;
        int b = ((src & 0xFF) * ws + (dst & 0xFF) * wd + half) / wo;
        return (((wo + 127) / 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * The alpha at barycentric weights {@code w1..w3} of a triangle whose vertex alphas differ,
     * rounded to the nearest value and clamped to 0..255. Rounding (rather than truncating, as
     * the color channels do) keeps weights that sum to just under 1 from lowering an alpha by
     * one step. Fillers copy a single vertex alpha directly instead of calling this.
     */
    private static int lerpAlpha(float w1, float w2, float w3, int a1, int a2, int a3) {
        int a = (int) (w1 * a1 + w2 * a2 + w3 * a3 + 0.5f);
        return a < 0 ? 0 : (a > 255 ? 255 : a);
    }

    /**
     * Whether every edge-function value an {@code int} filler evaluates is guaranteed to fit in
     * 32 bits, for a triangle whose 28.4 vertex extents are {@code spanX × spanY}.
     *
     * <p>An edge function is {@code E = dy·(xv − X) − dx·(yv − Y) (+1)}, with {@code |dx| ≤ spanX},
     * {@code |dy| ≤ spanY}, and is only evaluated at sample points {@code (X, Y)} inside the
     * vertex bounding box rounded out to whole pixels, so {@code |xv − X| ≤ spanX + 15} and
     * {@code |yv − Y| ≤ spanY + 15}. Hence {@code |E| ≤ 2·spanX·spanY + 15·(spanX + spanY) + 1}.
     * (The int arithmetic is exact modulo 2<sup>32</sup>, so only the final values matter, not
     * the intermediate products.) This admits bounding boxes up to about 2048 × 2048 px.
     */
    private static boolean edgesFitInt(long spanX, long spanY) {
        if (spanX >= (1L << 30) || spanY >= (1L << 30)) {
            return false; // also keeps the products below from overflowing long
        }
        return 2 * spanX * spanY + 15 * (spanX + spanY) + 1 <= Integer.MAX_VALUE;
    }

    /**
     * The {@link #fillTriangle} edge walk in 64-bit arithmetic, for triangles too large for the
     * int edge functions (see {@link #edgesFitInt}). Evaluates the same exact edge values, so it
     * covers the same pixels the int walk would if it could not overflow.
     */
    private static void fillTriangleLong(float[] zbuff, int[] img, int color, int w,
            int minx, int maxx, int miny, int maxy,
            int x1, int y1, int x2, int y2, int x3, int y3, float dx, float dy, float zoff) {
        long dx12 = (long) x1 - x2, dx23 = (long) x2 - x3, dx31 = (long) x3 - x1;
        long dy12 = (long) y1 - y2, dy23 = (long) y2 - y3, dy31 = (long) y3 - y1;
        long fdx12 = dx12 << 4, fdx23 = dx23 << 4, fdx31 = dx31 << 4;
        long fdy12 = dy12 << 4, fdy23 = dy23 << 4, fdy31 = dy31 << 4;
        long c1 = dy12 * x1 - dx12 * y1;
        long c2 = dy23 * x2 - dx23 * y2;
        long c3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) c1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) c2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) c3++;
        long cy1 = c1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        long cy2 = c2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        long cy3 = c3 + dx31 * (miny << 4) - dy31 * (minx << 4);
        int off = miny * w;
        for (int y = miny; y < maxy; y++) {
            long cx1 = cy1, cx2 = cy2, cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        zbuff[point] = zval;
                        img[point] = color;
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * The {@link #fillTriangleGouraud} edge walk in 64-bit arithmetic; see
     * {@link #edgesFitInt}.
     */
    private static void fillTriangleGouraudLong(float[] zbuff, int[] img, int c3, int c2, int c1,
            int w, int minx, int maxx, int miny, int maxy,
            int x1, int y1, int x2, int y2, int x3, int y3, float dx, float dy, float zoff) {
        long dx12 = (long) x1 - x2, dx23 = (long) x2 - x3, dx31 = (long) x3 - x1;
        long dy12 = (long) y1 - y2, dy23 = (long) y2 - y3, dy31 = (long) y3 - y1;
        long fdx12 = dx12 << 4, fdx23 = dx23 << 4, fdx31 = dx31 << 4;
        long fdy12 = dy12 << 4, fdy23 = dy23 << 4, fdy31 = dy31 << 4;
        long cc1 = dy12 * x1 - dx12 * y1;
        long cc2 = dy23 * x2 - dx23 * y2;
        long cc3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) cc1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) cc2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) cc3++;
        long cy1 = cc1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        long cy2 = cc2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        long cy3 = cc3 + dx31 * (miny << 4) - dy31 * (minx << 4);
        long s = cy1 + cy2 + cy3;
        if (s == 0) {
            return;
        }
        float inv = 1.0f / s;
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a3 = (c3 >>> 24) & 0xFF, r3 = (c3 >>> 16) & 0xFF, g3 = (c3 >>> 8) & 0xFF;
        int b3 = c3 & 0xFF;
        boolean oneAlpha = a1 == a2 && a2 == a3;
        int off = miny * w;
        for (int y = miny; y < maxy; y++) {
            long cx1 = cy1, cx2 = cy2, cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        zbuff[point] = zval;
                        float w1 = cx2 * inv;
                        float w2 = cx3 * inv;
                        float w3 = cx1 * inv;
                        int a = oneAlpha ? a1 : lerpAlpha(w1, w2, w3, a1, a2, a3);
                        int r = (int) (w1 * r1 + w2 * r2 + w3 * r3);
                        int g = (int) (w1 * g1 + w2 * g2 + w3 * g3);
                        int b = (int) (w1 * b1 + w2 * b2 + w3 * b3);
                        img[point] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    /**
     * The {@link #fillTriangleDepthOnly} edge walk in 64-bit arithmetic; see
     * {@link #edgesFitInt}.
     */
    private static void fillTriangleDepthOnlyLong(float[] zbuff, int w,
            int minx, int maxx, int miny, int maxy,
            int x1, int y1, int x2, int y2, int x3, int y3, float dx, float dy, float zoff) {
        long dx12 = (long) x1 - x2, dx23 = (long) x2 - x3, dx31 = (long) x3 - x1;
        long dy12 = (long) y1 - y2, dy23 = (long) y2 - y3, dy31 = (long) y3 - y1;
        long fdx12 = dx12 << 4, fdx23 = dx23 << 4, fdx31 = dx31 << 4;
        long fdy12 = dy12 << 4, fdy23 = dy23 << 4, fdy31 = dy31 << 4;
        long c1 = dy12 * x1 - dx12 * y1;
        long c2 = dy23 * x2 - dx23 * y2;
        long c3 = dy31 * x3 - dx31 * y3;
        if (dy12 < 0 || (dy12 == 0 && dx12 > 0)) c1++;
        if (dy23 < 0 || (dy23 == 0 && dx23 > 0)) c2++;
        if (dy31 < 0 || (dy31 == 0 && dx31 > 0)) c3++;
        long cy1 = c1 + dx12 * (miny << 4) - dy12 * (minx << 4);
        long cy2 = c2 + dx23 * (miny << 4) - dy23 * (minx << 4);
        long cy3 = c3 + dx31 * (miny << 4) - dy31 * (minx << 4);
        int off = miny * w;
        for (int y = miny; y < maxy; y++) {
            long cx1 = cy1, cx2 = cy2, cx3 = cy3;
            float p = zoff + dy * y;
            for (int x = minx; x < maxx; x++) {
                if (cx1 > 0 && cx2 > 0 && cx3 > 0) {
                    int point = x + off;
                    float zval = p + dx * x;
                    if (zbuff[point] > zval) {
                        zbuff[point] = zval;
                    }
                }
                cx1 -= fdy12;
                cx2 -= fdy23;
                cx3 -= fdy31;
            }
            cy1 += fdx12;
            cy2 += fdx23;
            cy3 += fdx31;
            off += w;
        }
    }

    private static int min(int x1, int x2, int x3) {
        return (x1 > x2) ? ((x2 > x3) ? x3 : x2) : ((x1 > x3) ? x3 : x1);
    }

    private static int max(int x1, int x2, int x3) {
        return (x1 < x2) ? ((x2 < x3) ? x3 : x2) : ((x1 < x3) ? x3 : x1);
    }
}
