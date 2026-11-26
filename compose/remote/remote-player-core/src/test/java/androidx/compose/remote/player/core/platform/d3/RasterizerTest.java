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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Coverage tests for the {@link Rasterizer} fillers. Each filler's coverage is compared with a
 * direct 64-bit evaluation of the same 28.4 edge functions, for triangles that fit the 32-bit
 * path and for triangles far too large for it.
 */
@RunWith(JUnit4.class)
public class RasterizerTest {
    private static final int W = 256;
    private static final int H = 192;
    private static final int COLOR = 0xFF336699;

    private static final int FLAT = 0;
    private static final int GOURAUD = 1;
    private static final int DEPTH_ONLY = 2;

    /** Triangles as {x1, y1, x2, y2, x3, y3}: small, larger than the int path, and huge. */
    private static final float[][] TRIANGLES = {
            {10.3f, 20.7f, 200.1f, 40.2f, 60.5f, 180.9f},
            {200.1f, 40.2f, 10.3f, 20.7f, 60.5f, 180.9f},
            {-0.5f, -0.5f, 255.5f, 0.25f, 128f, 191.75f},
            {-3000.25f, 100.5f, 3000.75f, 140.25f, -3000.5f, 5000.5f},
            {3000.75f, 140.25f, -3000.25f, 100.5f, -3000.5f, 5000.5f},
            {-50000f, 96.25f, 300.5f, 60.5f, 300.5f, 136.75f},
            {-20000f, -20000f, 40000f, 30f, -20000f, 40000f},
            {-30000f, -30000f, 60000f, -30000f, -30000f, 60000f},
    };

    @Test
    public void fillersCoverExactlyTheEdgeFunctionInterior() {
        for (float[] t : TRIANGLES) {
            boolean[] expected = referenceCoverage(t);
            for (int filler = FLAT; filler <= DEPTH_ONLY; filler++) {
                boolean[] actual = coverage(filler, t);
                int mismatches = 0;
                for (int i = 0; i < expected.length; i++) {
                    if (expected[i] != actual[i]) {
                        mismatches++;
                    }
                }
                assertEquals("filler " + filler + " triangle " + describe(t), 0, mismatches);
            }
        }
    }

    @Test
    public void opaqueGouraudAndTexturedPixelsStayOpaque() {
        // Interpolating a constant alpha of 255 with float weights that sum to just under 1
        // used to truncate it to 254 on many pixels. TRIANGLES covers the int and long paths.
        int[] tex = {0xFF102030, 0xFF405060, 0xFF708090, 0xFFA0B0C0};
        for (float[] t : TRIANGLES) {
            for (boolean textured : new boolean[] {false, true}) {
                float[] zbuf = new float[W * H];
                int[] img = new int[W * H];
                Rasterizer.clearDepth(zbuf);
                if (textured) {
                    Rasterizer.fillTriangleTextured(zbuf, img, tex, 2, 2,
                            COLOR, 0xFF00FF00, 0xFFFF0000, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0.5f,
                            0.25f, W, H, t[4], t[5], 0.25f, t[2], t[3], 0.5f, t[0], t[1], 0.75f);
                } else {
                    Rasterizer.fillTriangleGouraud(zbuf, img, COLOR, 0xFF00FF00, 0xFFFF0000, W,
                            H, t[4], t[5], 0.25f, t[2], t[3], 0.5f, t[0], t[1], 0.75f);
                }
                int covered = 0;
                for (int i = 0; i < img.length; i++) {
                    if (zbuf[i] != Float.POSITIVE_INFINITY) {
                        covered++;
                        assertEquals("textured " + textured + " triangle " + describe(t)
                                + " pixel " + i, 0xFF, img[i] >>> 24);
                    }
                }
                assertTrue("triangle " + describe(t), covered > 0);
            }
        }
    }

    @Test
    public void hugeTriangleCoversEveryPixel() {
        // Spans 90000 px: its edge functions overflow 32 bits by a wide margin.
        float[] t = {-30000f, -30000f, 60000f, -30000f, -30000f, 60000f};
        for (int filler = FLAT; filler <= DEPTH_ONLY; filler++) {
            boolean[] actual = coverage(filler, t);
            for (int i = 0; i < actual.length; i++) {
                assertTrue("filler " + filler + " missed pixel " + i, actual[i]);
            }
        }
    }

    @Test
    public void largeTriangleCoversItsInteriorAndNothingOutside() {
        // A 6000 px triangle whose long edge crosses the buffer: pixels clearly on either side
        // of that edge must be inside or outside regardless of the filler's arithmetic.
        float[] t = {-3000f, 100f, 3000f, 140f, -3000f, 5000f};
        double ex = t[2] - t[0];
        double ey = t[3] - t[1];
        for (int filler = FLAT; filler <= DEPTH_ONLY; filler++) {
            boolean[] actual = coverage(filler, t);
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    // Signed distance of the sample point (x, y) from the long edge.
                    double side = (ex * (y - t[1]) - ey * (x - t[0])) / Math.hypot(ex, ey);
                    if (side > 0.5) {
                        assertTrue("filler " + filler + " missed " + x + "," + y,
                                actual[y * W + x]);
                    } else if (side < -0.5) {
                        assertTrue("filler " + filler + " leaked " + x + "," + y,
                                !actual[y * W + x]);
                    }
                }
            }
        }
    }

    private static boolean[] coverage(int filler, float[] t) {
        float[] zbuf = new float[W * H];
        int[] img = new int[W * H];
        Rasterizer.clearDepth(zbuf);
        if (filler == FLAT) {
            Rasterizer.fillTriangle(zbuf, img, COLOR, W, H,
                    t[4], t[5], 0.25f, t[2], t[3], 0.5f, t[0], t[1], 0.75f);
        } else if (filler == GOURAUD) {
            Rasterizer.fillTriangleGouraud(zbuf, img, COLOR, 0xFF00FF00, 0xFFFF0000, W, H,
                    t[4], t[5], 0.25f, t[2], t[3], 0.5f, t[0], t[1], 0.75f);
        } else {
            Rasterizer.fillTriangleDepthOnly(zbuf, W, H,
                    t[4], t[5], 0.25f, t[2], t[3], 0.5f, t[0], t[1], 0.75f);
        }
        boolean[] covered = new boolean[W * H];
        for (int i = 0; i < covered.length; i++) {
            covered[i] = filler == DEPTH_ONLY ? zbuf[i] != Float.POSITIVE_INFINITY : img[i] != 0;
        }
        return covered;
    }

    /**
     * Direct (non-incremental) 64-bit evaluation of the rasterizer's coverage rule: vertices
     * snapped to 28.4, counter-clockwise order, samples at integer pixel coordinates, and the
     * top-left tie-break.
     */
    private static boolean[] referenceCoverage(float[] t) {
        // Fillers take (v3, v2, v1); with v1 = (t[0], t[1]) this mirrors their winding swap.
        float fx1 = t[0];
        float fy1 = t[1];
        float fx2 = t[2];
        float fy2 = t[3];
        float fx3 = t[4];
        float fy3 = t[5];
        if (((fx1 - fx2) * (fy3 - fy2) - (fy1 - fy2) * (fx3 - fx2)) < 0) {
            float tx = fx1;
            float ty = fy1;
            fx1 = fx2;
            fy1 = fy2;
            fx2 = tx;
            fy2 = ty;
        }
        long x1 = (int) (16.0f * fx1 + .5f);
        long y1 = (int) (16.0f * fy1 + .5f);
        long x2 = (int) (16.0f * fx2 + .5f);
        long y2 = (int) (16.0f * fy2 + .5f);
        long x3 = (int) (16.0f * fx3 + .5f);
        long y3 = (int) (16.0f * fy3 + .5f);
        long c1 = edgeConstant(x1, y1, x2, y2);
        long c2 = edgeConstant(x2, y2, x3, y3);
        long c3 = edgeConstant(x3, y3, x1, y1);
        boolean[] covered = new boolean[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                long px = (long) x << 4;
                long py = (long) y << 4;
                covered[y * W + x] = c1 + (x1 - x2) * py - (y1 - y2) * px > 0
                        && c2 + (x2 - x3) * py - (y2 - y3) * px > 0
                        && c3 + (x3 - x1) * py - (y3 - y1) * px > 0;
            }
        }
        return covered;
    }

    private static long edgeConstant(long xa, long ya, long xb, long yb) {
        long dx = xa - xb;
        long dy = ya - yb;
        long c = dy * xa - dx * ya;
        if (dy < 0 || (dy == 0 && dx > 0)) {
            c++;
        }
        return c;
    }

    private static String describe(float[] t) {
        StringBuilder sb = new StringBuilder();
        for (float v : t) {
            sb.append(v).append(' ');
        }
        return sb.toString().trim();
    }
}
