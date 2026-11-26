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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Paint3DContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Random;

/**
 * Tests for the software 3D engine: near-plane clipping, zero-area rejection, the pending and
 * scene rectangles the host composites from, and buffer state across clears and resizes.
 */
@RunWith(JUnit4.class)
public class JavaPaint3DContextTest {
    private static final int SIZE = 200;
    private static final int MESH = 1;
    private static final float FOV_60 = (float) Math.toRadians(60);

    /** Engine with a 60 degree camera at the origin looking down -z. */
    private static JavaPaint3DContext newEngine(int w, int h, float near) {
        JavaPaint3DContext ctx = new JavaPaint3DContext();
        ctx.setSize(w, h);
        ctx.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {FOV_60, (float) w / h, near, 100f},
                new float[] {0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f});
        ctx.setBaseColorArgb(0xFF808080);
        return ctx;
    }

    /** A floor at y = -1 from z = +5 (behind the eye) to z = -50, split into n x n quads. */
    private static void defineFloor(JavaPaint3DContext ctx, int n) {
        float x0 = -50f;
        float x1 = 50f;
        float z0 = 5f;
        float z1 = -50f;
        float[] verts = new float[(n + 1) * (n + 1) * 3];
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                int v = (j * (n + 1) + i) * 3;
                verts[v] = x0 + (x1 - x0) * i / n;
                verts[v + 1] = -1f;
                verts[v + 2] = z0 + (z1 - z0) * j / n;
            }
        }
        int[] indices = new int[n * n * 6];
        int k = 0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int a = j * (n + 1) + i;
                int b = a + 1;
                int c = a + n + 1;
                int d = c + 1;
                // Counter-clockwise seen from above, so the floor faces the camera.
                indices[k++] = a;
                indices[k++] = b;
                indices[k++] = c;
                indices[k++] = b;
                indices[k++] = d;
                indices[k++] = c;
            }
        }
        ctx.defineMesh3D(MESH, indices, verts, null);
    }

    @Test
    public void floorCrossingTheNearPlaneIsDrawnWithoutLeaksOrCracks() {
        for (int n : new int[] {1, 7}) {
            JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 0.1f);
            defineFloor(ctx, n);
            ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
            int[] color = ctx.getColorBuffer();
            // The floor's far edge (z = -50) projects just below the horizon (row ~103.5), and
            // the floor is wide enough to cover every column below it.
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int c = color[y * SIZE + x];
                    if (y <= SIZE / 2) {
                        assertEquals("leak at " + x + "," + y + " n=" + n, 0, c);
                    } else if (y >= 106) {
                        assertNotEquals("hole at " + x + "," + y + " n=" + n, 0, c);
                    }
                }
            }
            assertPendingContainsDrawnPixels(ctx);
        }
    }

    @Test
    public void clippedWireframeDrawsNoEdgeAlongTheNearPlane() {
        // near = 2: the triangle's third vertex is behind the camera, so clipping cuts it along
        // the near plane, which projects to row ~186.6 between x ~39.4 and ~160.6.
        JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 2f);
        ctx.defineMesh3D(MESH, new int[] {0, 1, 2},
                new float[] {-1f, -1f, -5f, 0f, -1f, 5f, 1f, -1f, -5f}, null);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_WIREFRAME);
        int[] color = ctx.getColorBuffer();
        for (int y = 182; y < SIZE; y++) {
            for (int x = 50; x <= 150; x++) {
                assertEquals("stray edge at " + x + "," + y, 0, color[y * SIZE + x]);
            }
        }
        // The far edge (z = -5, row ~134.6, x ~65.4..134.6) and both clipped side edges show.
        assertTrue(anyDrawn(color, 132, 137, 80, 120));
        assertTrue(anyDrawn(color, 158, 162, 45, 60));
        assertTrue(anyDrawn(color, 158, 162, 140, 155));
        assertPendingContainsDrawnPixels(ctx);
    }

    @Test
    public void zeroAreaTrianglesDrawNothing() {
        JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 0.1f);
        ctx.defineMesh3D(MESH, new int[] {0, 1, 2, 3, 3, 4},
                new float[] {
                        -1f, -1f, -5f, 0f, 0f, -5f, 1f, 1f, -5f, // collinear
                        -1f, 1f, -5f, 1f, -1f, -5f, // two identical vertices
                }, null);
        for (int mode : new int[] {
                Paint3DContext.MODE_SOFTWARE_FLAT,
                Paint3DContext.MODE_SOFTWARE_SMOOTH,
                Paint3DContext.MODE_SOFTWARE_WIREFRAME}) {
            ctx.drawMesh3D(MESH, mode);
            assertFalse(ctx.hasPendingPixels());
            assertAllZero(ctx.getColorBuffer(), SIZE * SIZE);
        }
    }

    @Test
    public void pendingRectangleHoldsEveryDrawnPixel() {
        JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 0.1f);
        Random random = new Random(42);
        for (int round = 0; round < 50; round++) {
            float[] verts = new float[9];
            float cx = random.nextFloat() * 8f - 4f;
            float cy = random.nextFloat() * 8f - 4f;
            float z = -2f - random.nextFloat() * 10f;
            float size = 0.05f + random.nextFloat() * 4f;
            for (int i = 0; i < 9; i += 3) {
                verts[i] = cx + (random.nextFloat() - 0.5f) * size;
                verts[i + 1] = cy + (random.nextFloat() - 0.5f) * size;
                verts[i + 2] = z + (random.nextFloat() - 0.5f);
            }
            ctx.defineMesh3D(MESH, new int[] {0, 1, 2, 0, 2, 1}, verts, null);
            int mode = round % 3 == 0 ? Paint3DContext.MODE_SOFTWARE_WIREFRAME
                    : Paint3DContext.MODE_SOFTWARE_FLAT;
            ctx.drawMesh3D(MESH, mode);
            assertPendingContainsDrawnPixels(ctx);
            if (round % 5 == 4) {
                // What a host does after compositing: the color goes, nothing else is drawn.
                ctx.clearPendingColor();
                assertFalse(ctx.hasPendingPixels());
                assertAllZero(ctx.getColorBuffer(), SIZE * SIZE);
            }
        }
    }

    @Test
    public void clearPendingColorKeepsDepth() {
        JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 0.1f);
        ctx.defineMesh3D(MESH, new int[] {0, 1, 2},
                new float[] {-1f, -1f, -3f, 1f, -1f, -3f, 0f, 1f, -3f}, null);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertTrue(ctx.hasPendingPixels());
        ctx.clearPendingColor();
        assertAllZero(ctx.getColorBuffer(), SIZE * SIZE);

        // The same triangle again is hidden by the depth it left behind...
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertAllZero(ctx.getColorBuffer(), SIZE * SIZE);

        // ...until the scene is cleared.
        ctx.clearDepth3D();
        assertFalse(ctx.hasPendingPixels());
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertTrue(countDrawn(ctx.getColorBuffer(), SIZE * SIZE) > 0);
    }

    @Test
    public void clearDepth3DAndSetSizeLeaveCleanBuffers() {
        JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 0.1f);
        defineFloor(ctx, 3);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        int drawn = countDrawn(ctx.getColorBuffer(), SIZE * SIZE);
        assertTrue(drawn > 0);

        ctx.clearDepth3D();
        assertAllZero(ctx.getColorBuffer(), SIZE * SIZE);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertEquals(drawn, countDrawn(ctx.getColorBuffer(), SIZE * SIZE));

        // A smaller viewport reuses the buffers: they must read as cleared at the new stride,
        // with no depth left over from the larger one.
        ctx.setSize(150, 100);
        assertEquals(150, ctx.getWidth());
        assertEquals(100, ctx.getHeight());
        assertFalse(ctx.hasPendingPixels());
        assertAllZero(ctx.getColorBuffer(), 150 * 100);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        int smaller = countDrawn(ctx.getColorBuffer(), 150 * 100);
        assertTrue(smaller > 0);
        assertPendingContainsDrawnPixels(ctx);

        ctx.setSize(SIZE, SIZE);
        assertAllZero(ctx.getColorBuffer(), SIZE * SIZE);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertEquals(drawn, countDrawn(ctx.getColorBuffer(), SIZE * SIZE));
    }

    @Test
    public void malformedMeshesDoNotThrow() {
        JavaPaint3DContext ctx = newEngine(SIZE, SIZE, 0.1f);
        float[] verts = {-1f, -1f, -3f, 1f, -1f, -3f, 0f, 1f, -3f};
        // Index count not a multiple of 3, out-of-range and negative indices, short normals
        // and UVs.
        ctx.defineMesh3D(MESH, new int[] {0, 1, 2, 0}, verts, new float[] {0f, 0f}, null);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_SMOOTH);
        ctx.defineMesh3D(MESH, new int[] {0, 1, 7, -1, 0, 1}, verts, null, new float[] {0f});
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_WIREFRAME);
        ctx.setTextureData(new int[] {0xFFFFFFFF}, 1, 1);
        ctx.defineMesh3D(MESH, new int[] {0, 1, 2}, verts, null, new float[] {0.5f});
        ctx.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertTrue(ctx.hasPendingPixels());
        assertPendingContainsDrawnPixels(ctx);
    }

    private static void assertPendingContainsDrawnPixels(JavaPaint3DContext ctx) {
        int w = ctx.getWidth();
        int h = ctx.getHeight();
        int[] color = ctx.getColorBuffer();
        boolean pending = ctx.hasPendingPixels();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (color[y * w + x] == 0) {
                    continue;
                }
                assertTrue("pixel " + x + "," + y + " drawn without a pending rectangle",
                        pending);
                assertTrue("pixel " + x + "," + y + " outside the pending rectangle",
                        x >= ctx.getPendingLeft() && x < ctx.getPendingRight()
                                && y >= ctx.getPendingTop() && y < ctx.getPendingBottom());
            }
        }
    }

    private static boolean anyDrawn(int[] color, int y0, int y1, int x0, int x1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (color[y * SIZE + x] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countDrawn(int[] color, int n) {
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (color[i] != 0) {
                count++;
            }
        }
        return count;
    }

    private static void assertAllZero(int[] color, int n) {
        assertEquals(0, countDrawn(color, n));
    }
}
