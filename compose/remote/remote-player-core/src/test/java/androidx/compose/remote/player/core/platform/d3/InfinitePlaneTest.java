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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.utilities.d3.MeshData;
import androidx.compose.remote.core.operations.utilities.d3.Primitive3D;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link InfinitePlane}: coverage up to the horizon and the near plane, depth against
 * the coplanar finite quad drawn like a mesh with {@link Rasterizer}, the grid and its fade, the
 * mode flags, the axis flag, and the canvas engines' polygon and grid lines.
 */
@RunWith(JUnit4.class)
public class InfinitePlaneTest {
    private static final int BACKGROUND = 0x00000000;
    private static final int BASE = 0xFF336699;
    private static final int MESH_COLOR = 0xFFCC2211;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final InfinitePlane.Shader BASE_SHADER = (px, py, pz, nx, ny, nz) -> BASE;
    private static final InfinitePlane.Shader BLACK_SHADER = (px, py, pz, nx, ny, nz) -> BLACK;
    private static final float[] IDENTITY = identity();
    private static final int LAST_LEVEL = InfinitePlane.GRID_LEVELS - 1;

    // ---- Horizon and near plane ---------------------------------------------------------------

    @Test
    public void levelCamera_drawsEveryRowBelowTheHorizonAndNothingAbove() {
        int size = 64;
        // Eye 1 above the floor looking level: the horizon is the middle row (y = 32).
        Camera cam = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, 1f, 0f, 0f, 1f, -1f, 0f, 1f, 0f);
        Target t = new Target(size, size);
        InfinitePlane plane = new InfinitePlane(0, new float[0]);

        assertTrue(drawPlane(plane, t, cam, IDENTITY, 0, BASE_SHADER, true));

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                assertEquals("pixel " + x + "," + y, y > size / 2 ? BASE : BACKGROUND,
                        t.color(x, y));
            }
        }
        // Row y sees the floor at distance -1 / ndcY (eye height 1, focal length 1).
        for (int y = size / 2 + 1; y < size; y++) {
            double ndcY = 1.0 - 2.0 * y / size;
            assertEquals(cam.windowZ(1.0 / ndcY), t.depth(7, y), 1e-6);
        }
        assertArrayEquals(new int[] {0, size / 2 + 1, size, size}, t.mRect);
    }

    @Test
    public void tiltedAndRolledCameras_coverExactlyTheVisiblePlane() {
        int w = 96;
        int h = 64;
        Camera[] cameras = {
                // Pitched down slightly and rolled: a slanted horizon.
                Camera.perspective(w, h, 60f, 0.5f, 50f)
                        .lookAt(1f, 2f, 3f, 4f, 1f, -6f, 0.3f, 1f, 0f),
                // Low eye and a far near plane: the near plane cuts the floor inside the view.
                Camera.perspective(w, h, 70f, 0.3f, 50f)
                        .lookAt(0f, 0.2f, 0f, 0.5f, -0.445f, -1f, 0.2f, 1f, 0f),
                // Looking up from below a double-sided plane.
                Camera.perspective(w, h, 60f, 0.1f, 50f)
                        .lookAt(0f, -2f, 0f, 1f, -1f, -3f, 0f, 1f, 0f),
        };
        for (Camera cam : cameras) {
            Target t = new Target(w, h);
            InfinitePlane plane = new InfinitePlane(0, new float[0]);
            assertTrue(drawPlane(plane, t, cam, IDENTITY, Paint3DContext.MODE_DOUBLE_SIDED,
                    BASE_SHADER, true));
            boolean[] expected = new boolean[w * h];
            int inside = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double d = cam.floorHit(x, y)[3];
                    expected[y * w + x] = d >= cam.mNear;
                    inside += expected[y * w + x] ? 1 : 0;
                }
            }
            assertTrue("inside " + inside, inside > w * h / 5 && inside < w * h * 4 / 5);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (!nearEdge(expected, w, h, x, y)) {
                        assertEquals("pixel " + x + "," + y,
                                expected[y * w + x] ? BASE : BACKGROUND, t.color(x, y));
                    }
                }
            }
        }
    }

    @Test
    public void edgeOnPlane_drawsNothing() {
        int size = 32;
        Camera cam = Camera.perspective(size, size, 60f, 0.1f, 100f)
                .lookAt(0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f);
        Target t = new Target(size, size);
        InfinitePlane plane = new InfinitePlane(0, new float[0]);
        int mode = Paint3DContext.MODE_DOUBLE_SIDED;
        assertFalse(drawPlane(plane, t, cam, IDENTITY, mode, BASE_SHADER, true));
        assertEquals(0, plane.visiblePolygon(IDENTITY, cam.mProj, size, size, mode, 0f, 0f,
                true, new float[16], null, null, BASE_SHADER));
        assertUntouched(t);
    }

    // ---- Depth and coverage against the coplanar quad ----------------------------------------

    @Test
    public void depthAndCoverage_matchTheCoplanarQuadDrawnAsAMesh() {
        for (Scene s : scenes()) {
            Target planeTarget = new Target(s.mCamera.mWidth, s.mCamera.mHeight);
            Target quadTarget = new Target(s.mCamera.mWidth, s.mCamera.mHeight);
            boolean doubleSided = (s.mMode & Paint3DContext.MODE_DOUBLE_SIDED) != 0;
            boolean drew = drawPlane(s.mPlane, planeTarget, s.mCamera, s.mModel, s.mMode,
                    BASE_SHADER, true);
            // The engine's finite stand-in for the plane, shrunk about its center from a half
            // size of 1000 to 20 so that its float projection stays precise.
            MeshData quad = Primitive3D.build(Primitive3D.INFINITE_PLANE, 0f, s.mFlags, s.mParams);
            for (int i = 0; i < quad.verts.length; i++) {
                float c = i % 3 < s.mParams.length ? s.mParams[i % 3] : 0f;
                quad.verts[i] = c + (quad.verts[i] - c) * 0.02f;
            }
            drawMesh(quad, quadTarget, s.mCamera, s.mModel, MESH_COLOR, doubleSided);

            int w = planeTarget.mWidth;
            int h = planeTarget.mHeight;
            boolean[] planeMask = planeTarget.drawnMask();
            float[] mv = new float[16];
            Matrix4.multiply(mv, s.mCamera.mView, s.mModel);
            int both = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean inPlane = planeMask[y * w + x];
                    boolean inQuad = quadTarget.depth(x, y) != Float.POSITIVE_INFINITY;
                    if (inPlane) {
                        assertEquals(s.mName + " exact depth at " + x + "," + y,
                                analyticDepth(s, mv, x, y), planeTarget.depth(x, y), 2e-6);
                    }
                    if (inPlane && inQuad) {
                        both++;
                        assertEquals(s.mName + " depth at " + x + "," + y,
                                quadTarget.depth(x, y), planeTarget.depth(x, y), 1e-5f);
                    } else if (inQuad) {
                        assertTrue(s.mName + " quad pixel " + x + "," + y + " not in the plane",
                                nearEdge(planeMask, w, h, x, y));
                    }
                }
            }
            if (s.mVisible) {
                assertTrue(s.mName, drew);
                assertTrue(s.mName + " both " + both, both > w * h / 10);
            } else {
                assertFalse(s.mName, drew);
                assertEquals(s.mName, 0, both);
                assertUntouched(quadTarget);
            }
        }
    }

    @Test
    public void cubeCrossingThePlane_isCutByTheDepthTestInBothDrawOrders() {
        int w = 80;
        int h = 60;
        Camera cam = Camera.perspective(w, h, 50f, 0.1f, 100f)
                .lookAt(2f, 1.5f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);
        InfinitePlane plane = new InfinitePlane(0, new float[0]);
        MeshData cube = Primitive3D.cube(1f, 1f, 1f, 0f, 0.1f, 0f, Primitive3D.UV_NONE);

        Target planeOnly = new Target(w, h);
        drawPlane(plane, planeOnly, cam, IDENTITY, 0, BASE_SHADER, true);
        Target cubeOnly = new Target(w, h);
        drawMesh(cube, cubeOnly, cam, IDENTITY, MESH_COLOR, false);
        Target planeFirst = new Target(w, h);
        drawPlane(plane, planeFirst, cam, IDENTITY, 0, BASE_SHADER, true);
        drawMesh(cube, planeFirst, cam, IDENTITY, MESH_COLOR, false);
        Target cubeFirst = new Target(w, h);
        drawMesh(cube, cubeFirst, cam, IDENTITY, MESH_COLOR, false);
        drawPlane(plane, cubeFirst, cam, IDENTITY, 0, BASE_SHADER, true);

        int hidden = 0;
        int shown = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float zp = planeOnly.depth(x, y);
                float zc = cubeOnly.depth(x, y);
                if (Math.abs(zp - zc) < 1e-6f) {
                    continue; // on the intersection line
                }
                int expected = zc < zp ? MESH_COLOR : planeOnly.color(x, y);
                assertEquals("plane first " + x + "," + y, expected, planeFirst.color(x, y));
                assertEquals("cube first " + x + "," + y, expected, cubeFirst.color(x, y));
                assertEquals(Math.min(zp, zc), planeFirst.depth(x, y), 0f);
                assertEquals(Math.min(zp, zc), cubeFirst.depth(x, y), 0f);
                if (zc != Float.POSITIVE_INFINITY) {
                    if (zc < zp) {
                        shown++;
                    } else {
                        hidden++;
                    }
                }
            }
        }
        // The camera looks down on the cube, whose lower part is under the floor.
        assertTrue("shown " + shown, shown > 200);
        assertTrue("hidden " + hidden, hidden > 50);
    }

    @Test
    public void depthBias_isAppliedWithThePlaneSlope() {
        int size = 48;
        Camera cam = Camera.perspective(size, size, 60f, 0.1f, 100f)
                .lookAt(0f, 2f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);
        InfinitePlane plane = new InfinitePlane(0, new float[0]);
        Target plain = new Target(size, size);
        drawPlane(plane, plain, cam, IDENTITY, 0, BASE_SHADER, true);
        Target biased = new Target(size, size);
        float[] mv = new float[16];
        Matrix4.multiply(mv, cam.mView, IDENTITY);
        assertTrue(plane.rasterize(biased.mColor, biased.mDepth, size, size, mv, cam.mProj, 0,
                0.001f, 2f, true, biased.mRect, BASE_SHADER));
        // Window z is affine in the pixel position; its slope gives the bias.
        int c = size / 2;
        float dzdx = plain.depth(c + 1, c) - plain.depth(c, c);
        float dzdy = plain.depth(c, c + 1) - plain.depth(c, c);
        float bias = 0.001f + 2f * Math.max(Math.abs(dzdx), Math.abs(dzdy));
        assertEquals(plain.depth(c, c) + bias, biased.depth(c, c), 1e-6f);
    }

    // ---- Grid ----------------------------------------------------------------------------------

    @Test
    public void grid_onePixelLinesFallOnTheLinePixels() {
        int size = 64;
        Target t = new Target(size, size);
        Camera cam = topDown(size);
        InfinitePlane plane = new InfinitePlane(0, gridParams(0f, 0f, 8f, 1f));

        assertTrue(drawPlane(plane, t, cam, IDENTITY, 0, BLACK_SHADER, true));

        double z = cam.windowZ(-10.0);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int expected = x % 8 == 0 || y % 8 == 0 ? WHITE : BLACK;
                assertEquals("pixel " + x + "," + y, expected, t.color(x, y));
                assertEquals(z, t.depth(x, y), 1e-6);
            }
        }
    }

    @Test
    public void grid_wideLinesCoverLineWidthPixels() {
        int size = 64;
        Target t = new Target(size, size);
        InfinitePlane plane = new InfinitePlane(0, gridParams(0f, 0f, 8f, 3f));

        assertTrue(drawPlane(plane, t, topDown(size), IDENTITY, 0, BLACK_SHADER, true));

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean line = (x + 1) % 8 <= 2 || (y + 1) % 8 <= 2;
                assertEquals("pixel " + x + "," + y, line ? WHITE : BLACK, t.color(x, y));
            }
        }
    }

    @Test
    public void grid_lineBetweenPixelsIsAntialiased() {
        int size = 64;
        Target t = new Target(size, size);
        // Lines x = 0.5 + 8k fall between pixel columns 8k and 8k + 1: half coverage each.
        InfinitePlane plane = new InfinitePlane(0, gridParams(0.5f, 0f, 8f, 1f));

        assertTrue(drawPlane(plane, t, topDown(size), IDENTITY, 0, BLACK_SHADER, true));

        for (int y = 1; y < size; y += 8) {
            for (int x = 0; x < size; x++) {
                int c = t.color(x, y);
                if (x % 8 <= 1) {
                    // Half coverage: gray 128 on every channel, give or take rounding.
                    assertEquals(0xFF, c >>> 24);
                    assertEquals("pixel " + x + "," + y, 128, (c >> 16) & 0xFF, 1);
                    assertEquals(c & 0xFF, (c >> 16) & 0xFF);
                    assertEquals(c & 0xFF, (c >> 8) & 0xFF);
                } else {
                    assertEquals("pixel " + x + "," + y, BLACK, c);
                }
            }
        }
    }

    @Test
    public void grid_cellsSmallerThanTwoPixelsShowOnlyTheBaseColor() {
        int w = 128;
        int h = 96;
        Camera cam = Camera.perspective(w, h, 60f, 0.1f, 100f)
                .lookAt(0f, 1f, 0f, 0f, 0.6f, -1f, 0f, 1f, 0f);
        InfinitePlane plane = new InfinitePlane(0, gridParams(0f, 0f, 0.25f, 1f));
        Target t = new Target(w, h);

        assertTrue(drawPlane(plane, t, cam, IDENTITY, 0, BASE_SHADER, true));

        int faded = 0;
        int lines = 0;
        int onLine = 0;
        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w - 1; x++) {
                double[] p = cam.floorHit(x, y);
                if (!(p[3] >= cam.mNear)) {
                    continue;
                }
                double[] px = cam.floorHit(x + 1, y);
                double[] py = cam.floorHit(x, y + 1);
                // Cell sizes in pixels of the lines x = 0.25·k and z = 0.25·k: 0.25 / |grad|.
                double cellX = 0.25 / Math.hypot(px[0] - p[0], py[0] - p[0]);
                double cellZ = 0.25 / Math.hypot(px[2] - p[2], py[2] - p[2]);
                if (Math.max(cellX, cellZ) < 1.8) {
                    faded++;
                    assertEquals("pixel " + x + "," + y, BASE, t.color(x, y));
                } else if (t.color(x, y) != BASE) {
                    lines++;
                }
                // A pixel on a line of a family drawn at full strength shows the line.
                double offX = Math.abs(p[0] / 0.25 - Math.rint(p[0] / 0.25)) * cellX;
                double offZ = Math.abs(p[2] / 0.25 - Math.rint(p[2] / 0.25)) * cellZ;
                if ((cellX > 4.5 && offX < 0.2) || (cellZ > 4.5 && offZ < 0.2)) {
                    onLine++;
                    assertTrue("pixel " + x + "," + y, t.color(x, y) != BASE);
                }
            }
        }
        assertTrue("faded " + faded, faded > w * 3);
        assertTrue("lines " + lines, lines > w * 3);
        assertTrue("on line " + onLine, onLine > w);
    }

    // ---- Modes ---------------------------------------------------------------------------------

    @Test
    public void transparent_blendsAndKeepsTheDepthBuffer() {
        int size = 64;
        Camera cam = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, 1f, 0f, 0f, 1f, -1f, 0f, 1f, 0f);
        Target t = new Target(size, size);
        Arrays.fill(t.mColor, 0xFF0000FF);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size / 2; x++) {
                t.mDepth[y * size + x] = 0f; // nearer than the plane: hides it
            }
        }
        float[] depthBefore = t.mDepth.clone();
        InfinitePlane plane = new InfinitePlane(0, new float[0]);

        assertTrue(drawPlane(plane, t, cam, IDENTITY, Paint3DContext.MODE_TRANSPARENT,
                (px, py, pz, nx, ny, nz) -> 0x80FF0000, true));

        assertArrayEquals(depthBefore, t.mDepth, 0f);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int expected = y > size / 2 && x >= size / 2 ? 0xFF80007F : 0xFF0000FF;
                assertEquals("pixel " + x + "," + y, expected, t.color(x, y));
            }
        }
    }

    @Test
    public void backSide_isCulledUnlessDoubleSided_andThenLitFacingTheViewer() {
        int size = 64;
        Camera below = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, -1f, 0f, 0f, -1f, -1f, 0f, 1f, 0f);
        InfinitePlane plane = new InfinitePlane(0, new float[0]);
        Target t = new Target(size, size);

        assertFalse(drawPlane(plane, t, below, IDENTITY, 0, BASE_SHADER, true));
        assertUntouched(t);
        assertEquals(0, plane.visiblePolygon(below.mView, below.mProj, size, size, 0, 0f, 0f,
                true, new float[16], null, null, BASE_SHADER));

        RecordingShader shader = new RecordingShader();
        assertTrue(drawPlane(plane, t, below, IDENTITY, Paint3DContext.MODE_DOUBLE_SIDED, shader,
                true));
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                assertEquals("pixel " + x + "," + y, y < size / 2 ? BASE : BACKGROUND,
                        t.color(x, y));
            }
        }
        // Seen from below, the normal handed to the shader is the eye-space -Y.
        assertEquals(1, shader.mCalls);
        assertArrayEquals(new float[] {0f, -1f, 0f}, shader.mNormal, 1e-6f);

        Camera above = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, 1f, 0f, 0f, 1f, -1f, 0f, 1f, 0f);
        assertTrue(drawPlane(plane, new Target(size, size), above, IDENTITY, 0, shader, true));
        assertArrayEquals(new float[] {0f, 1f, 0f}, shader.mNormal, 1e-6f);
    }

    @Test
    public void nonUniformShading_shadesPointsOnThePlane() {
        int size = 64;
        Camera cam = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, 1f, 0f, 0f, 1f, -1f, 0f, 1f, 0f);
        RecordingShader shader = new RecordingShader();
        Target t = new Target(size, size);

        assertTrue(drawPlane(new InfinitePlane(0, gridParams(0f, 0f, 1f, 1f)), t, cam,
                IDENTITY, 0, shader, false));

        // At least the two ends of every row, all on the floor (eye-space y = -1), ahead.
        assertTrue("calls " + shader.mCalls, shader.mCalls >= 2 * (size / 2 - 1));
        for (int i = 0; i < shader.mCalls; i++) {
            float[] p = shader.mPoints.get(i);
            assertEquals(-1f, p[1], 1e-4f * (1f + Math.abs(p[2])));
            assertTrue(p[2] < 0f);
        }
        for (int y = size / 2 + 1; y < size; y++) {
            for (int x = 0; x < size; x++) {
                assertTrue(t.color(x, y) != BACKGROUND);
            }
        }
    }

    @Test
    public void wireframe_drawsOnlyTheGridButWritesDepthEverywhere() {
        int size = 64;
        Camera cam = topDown(size);
        InfinitePlane plane = new InfinitePlane(0, gridParams(0f, 0f, 8f, 1f));
        Target t = new Target(size, size);
        Arrays.fill(t.mColor, 0xFF0000FF);

        assertTrue(drawPlane(plane, t, cam, IDENTITY, Paint3DContext.MODE_WIREFRAME, BASE_SHADER,
                true));

        double z = cam.windowZ(-10.0);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int expected = x % 8 == 0 || y % 8 == 0 ? WHITE : 0xFF0000FF;
                assertEquals("pixel " + x + "," + y, expected, t.color(x, y));
                assertEquals(z, t.depth(x, y), 1e-6);
            }
        }

        // Without a grid: depth only. Transparent as well: nothing at all.
        InfinitePlane bare = new InfinitePlane(0, new float[0]);
        Target depthOnly = new Target(size, size);
        assertTrue(drawPlane(bare, depthOnly, cam, IDENTITY, Paint3DContext.MODE_WIREFRAME,
                BASE_SHADER, true));
        assertEquals(z, depthOnly.depth(5, 9), 1e-6);
        assertEquals(BACKGROUND, depthOnly.color(5, 9));
        Target nothing = new Target(size, size);
        assertFalse(drawPlane(bare, nothing, cam, IDENTITY,
                Paint3DContext.MODE_WIREFRAME | Paint3DContext.MODE_TRANSPARENT,
                BASE_SHADER, true));
        assertUntouched(nothing);
    }

    @Test
    public void dirtyRect_boundsEveryWrittenPixel() {
        for (Scene s : scenes()) {
            Target t = new Target(s.mCamera.mWidth, s.mCamera.mHeight);
            if (!drawPlane(s.mPlane, t, s.mCamera, s.mModel, s.mMode, BASE_SHADER, true)) {
                assertUntouched(t);
                continue;
            }
            int[] r = t.mRect;
            assertTrue(s.mName, 0 <= r[0] && r[0] < r[2] && r[2] <= t.mWidth);
            assertTrue(s.mName, 0 <= r[1] && r[1] < r[3] && r[3] <= t.mHeight);
            for (int y = 0; y < t.mHeight; y++) {
                for (int x = 0; x < t.mWidth; x++) {
                    if (t.depth(x, y) != Float.POSITIVE_INFINITY) {
                        assertTrue(s.mName + " " + x + "," + y,
                                x >= r[0] && x < r[2] && y >= r[1] && y < r[3]);
                    }
                }
            }
        }
    }

    // ---- Axis ----------------------------------------------------------------------------------

    @Test
    public void axisZ_planeFacingTheCamera_fillsTheViewWithTheGridThroughTheCenter() {
        int size = 64;
        Camera cam = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f);
        float[] params = {0f, 0f, -5f, 1f, 1f, 1f, 1f, 1f, 1f};
        InfinitePlane plane = new InfinitePlane(axis(Primitive3D.AXIS_Z), params);
        Target t = new Target(size, size);

        assertTrue(drawPlane(plane, t, cam, IDENTITY, 0, BLACK_SHADER, true));

        double z = cam.windowZ(-5.0);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                assertEquals(z, t.depth(x, y), 1e-6);
            }
        }
        // Lines x = k and y = k, 32/5 = 6.4 px apart, cross at the center pixel.
        for (int i = 0; i < size; i++) {
            assertEquals(WHITE, t.color(size / 2, i));
            assertEquals(WHITE, t.color(i, size / 2));
        }
        assertEquals(BLACK, t.color(size / 2 + 3, size / 2 + 3));
    }

    @Test
    public void axisX_planeIsSeenFromPlusXAndCulledFromMinusX() {
        int size = 48;
        float[] params = {-5f, 0f, 0f};
        InfinitePlane plane = new InfinitePlane(axis(Primitive3D.AXIS_X), params);
        Camera front = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(0f, 0f, 0f, -1f, 0f, 0f, 0f, 1f, 0f);
        Target t = new Target(size, size);
        assertTrue(drawPlane(plane, t, front, IDENTITY, 0, BASE_SHADER, true));
        double z = front.windowZ(-5.0);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                assertEquals(BASE, t.color(x, y));
                assertEquals(z, t.depth(x, y), 1e-6);
            }
        }
        Camera back = Camera.perspective(size, size, 90f, 0.1f, 100f)
                .lookAt(-10f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f);
        Target culled = new Target(size, size);
        assertFalse(drawPlane(plane, culled, back, IDENTITY, 0, BASE_SHADER, true));
        assertUntouched(culled);
    }

    // ---- Canvas engines ------------------------------------------------------------------------

    @Test
    public void visiblePolygon_coversTheSamePixelsAsTheSoftwareFill() {
        float[] xy = new float[2 * InfinitePlane.MAX_POLYGON_VERTICES];
        float[] z = new float[InfinitePlane.MAX_POLYGON_VERTICES];
        int[] colors = new int[InfinitePlane.MAX_POLYGON_VERTICES];
        for (Scene s : scenes()) {
            int w = s.mCamera.mWidth;
            int h = s.mCamera.mHeight;
            Target t = new Target(w, h);
            boolean drew = drawPlane(s.mPlane, t, s.mCamera, s.mModel, s.mMode, BASE_SHADER, true);
            float[] mv = new float[16];
            Matrix4.multiply(mv, s.mCamera.mView, s.mModel);
            int n = s.mPlane.visiblePolygon(mv, s.mCamera.mProj, w, h, s.mMode, 0f, 0f,
                    true, xy, z, colors, BASE_SHADER);
            if (!drew) {
                assertEquals(s.mName, 0, n);
                continue;
            }
            assertTrue(s.mName, n >= 3 && n <= InfinitePlane.MAX_POLYGON_VERTICES);
            for (int i = 0; i < n; i++) {
                assertEquals(BASE, colors[i]);
                assertTrue(s.mName, z[i] >= -1e-6f && !Float.isInfinite(z[i]));
            }
            int compared = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double d = signedDistanceInside(xy, n, x, y);
                    if (Math.abs(d) <= 1.0) {
                        continue;
                    }
                    compared++;
                    assertEquals(s.mName + " pixel " + x + "," + y, d > 0.0,
                            t.depth(x, y) != Float.POSITIVE_INFINITY);
                }
            }
            assertTrue(s.mName, compared > w * h / 2);
        }
    }

    @Test
    public void gridLines_topDown_areTheLinePixelColumnsAndRows() {
        int size = 64;
        Camera cam = topDown(size);
        InfinitePlane plane = new InfinitePlane(0, gridParams(0f, 0f, 8f, 1f));

        int total = plane.buildGridLines(cam.mView, cam.mProj, size, size, 0);

        for (int level = 0; level < LAST_LEVEL; level++) {
            assertEquals(0, plane.getGridLineCount(level));
        }
        int count = plane.getGridLineCount(LAST_LEVEL);
        assertEquals(total, count);
        assertTrue("count " + count, count >= 16 && count <= 18);
        assertEquals(WHITE, plane.getGridLineColor(LAST_LEVEL));
        float[] lines = plane.getGridLines(LAST_LEVEL);
        for (int i = 0; i < count; i++) {
            float x0 = lines[4 * i];
            float y0 = lines[4 * i + 1];
            float x1 = lines[4 * i + 2];
            float y1 = lines[4 * i + 3];
            if (Math.abs(x0 - x1) < 1e-3f) {
                assertEquals(0f, x0 - 8f * Math.round(x0 / 8f), 1e-3f);
                assertEquals(size, Math.abs(y1 - y0), 1e-3f);
            } else {
                assertEquals(y0, y1, 1e-3f);
                assertEquals(0f, y0 - 8f * Math.round(y0 / 8f), 1e-3f);
                assertEquals(size, Math.abs(x1 - x0), 1e-3f);
            }
        }
    }

    @Test
    public void gridLines_perspective_lieOnTheSoftwareGridAndFadeByLevel() {
        int w = 120;
        int h = 90;
        Camera cam = Camera.perspective(w, h, 60f, 0.1f, 100f)
                .lookAt(0.3f, 1.5f, 0.2f, 1f, 0f, -3f, 0f, 1f, 0f);
        InfinitePlane plane = new InfinitePlane(0, gridParams(0f, 0f, 1f, 1f));
        Target t = new Target(w, h);
        assertTrue(drawPlane(plane, t, cam, IDENTITY, 0, BASE_SHADER, true));
        float[] xy = new float[2 * InfinitePlane.MAX_POLYGON_VERTICES];
        int n = plane.visiblePolygon(cam.mView, cam.mProj, w, h, 0, 0f, 0f, true,
                xy, null, null, BASE_SHADER);

        int total = plane.buildGridLines(cam.mView, cam.mProj, w, h, 0);

        assertTrue("total " + total, total > 10);
        int sum = 0;
        int previousAlpha = 0;
        for (int level = 0; level < InfinitePlane.GRID_LEVELS; level++) {
            sum += plane.getGridLineCount(level);
            int alpha = plane.getGridLineColor(level) >>> 24;
            assertTrue(alpha > previousAlpha);
            previousAlpha = alpha;
        }
        assertEquals(total, sum);
        assertEquals(255, previousAlpha);
        // Points along the full-strength lines land on pixels the software grid touched.
        float[] lines = plane.getGridLines(LAST_LEVEL);
        int samples = 0;
        int hits = 0;
        for (int i = 0; i < plane.getGridLineCount(LAST_LEVEL); i++) {
            float x0 = lines[4 * i];
            float y0 = lines[4 * i + 1];
            float x1 = lines[4 * i + 2];
            float y1 = lines[4 * i + 3];
            int steps = (int) Math.ceil(Math.hypot(x1 - x0, y1 - y0) / 2.0);
            for (int k = 0; k <= steps; k++) {
                float f = steps == 0 ? 0f : (float) k / steps;
                int px = Math.round(x0 + f * (x1 - x0));
                int py = Math.round(y0 + f * (y1 - y0));
                if (px < 0 || py < 0 || px >= w || py >= h
                        || signedDistanceInside(xy, n, px, py) < 1.5) {
                    continue;
                }
                samples++;
                hits += t.color(px, py) != BASE ? 1 : 0;
            }
        }
        assertTrue("samples " + samples, samples > 100);
        assertTrue("hits " + hits + " of " + samples, hits >= samples * 0.98);
    }

    // ---- Definition ----------------------------------------------------------------------------

    @Test
    public void params_takeDefaultsAndDecideTheGrid() {
        InfinitePlane plane = new InfinitePlane(0, new float[0]);
        assertFalse(plane.hasGrid());
        assertEquals(0f, plane.getSpacing(), 0f);
        assertEquals(1f, plane.getLineWidth(), 0f);
        assertEquals(0xFF808080, plane.getLineColor());

        plane.set(axis(Primitive3D.AXIS_Z), new float[] {1f, 2f, 3f, 0.5f, Float.NaN});
        assertEquals(axis(Primitive3D.AXIS_Z), plane.getFlags());
        assertTrue(plane.hasGrid());
        assertEquals(0.5f, plane.getSpacing(), 0f);
        assertEquals(1f, plane.getLineWidth(), 0f);

        plane.set(0, new float[] {0f, 0f, 0f, 1f, 2f, 0.5f, 1f, 0f, 0f, 99f});
        assertTrue(plane.hasGrid());
        assertEquals(0x80FF0000, plane.getLineColor());
        assertEquals(2f, plane.getLineWidth(), 0f);

        plane.set(0, new float[] {0f, 0f, 0f, 1f, 1f, 0f}); // transparent lines
        assertFalse(plane.hasGrid());
        plane.set(0, new float[] {0f, 0f, 0f, -1f});
        assertFalse(plane.hasGrid());
        plane.set(0, new float[] {0f, 0f, 0f, 1f, 0f});
        assertFalse(plane.hasGrid());
    }

    // ---- Scenes and helpers --------------------------------------------------------------------

    /** A plane, a camera and a model matrix. */
    private static final class Scene {
        final String mName;
        final int mFlags;
        final float[] mParams;
        final InfinitePlane mPlane;
        final Camera mCamera;
        final float[] mModel;
        final int mMode;
        final boolean mVisible;

        Scene(String name, int flags, float[] params, Camera camera, float[] model, int mode,
                boolean visible) {
            mName = name;
            mFlags = flags;
            mParams = params;
            mPlane = new InfinitePlane(flags, params);
            mCamera = camera;
            mModel = model;
            mMode = mode;
            mVisible = visible;
        }
    }

    private static List<Scene> scenes() {
        int w = 96;
        int h = 72;
        float[] plain = {0f, 0f, 0f};
        float[] grid = {0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f};
        int sided = Paint3DContext.MODE_DOUBLE_SIDED;
        List<Scene> list = new ArrayList<>();
        list.add(new Scene("pitched", 0, plain,
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(0f, 3f, 6f, 0f, 0f, 0f, 0f, 1f, 0f),
                IDENTITY, 0, true));
        list.add(new Scene("grid", 0, grid,
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(0f, 3f, 6f, 0f, 0f, 0f, 0f, 1f, 0f),
                IDENTITY, 0, true));
        list.add(new Scene("moved", 0, new float[] {2f, -1f, 3f, 0.5f},
                Camera.perspective(w, h, 50f, 0.1f, 100f)
                        .lookAt(1f, 4f, 9f, 2f, -1f, 0f, 0f, 1f, 0f),
                model(30f, 0.5f, 0.25f, -1f), 0, true));
        list.add(new Scene("nearCut", 0, plain,
                Camera.perspective(w, h, 70f, 0.3f, 100f)
                        .lookAt(0f, 0.2f, 0f, 0.5f, -0.445f, -1f, 0.2f, 1f, 0f),
                IDENTITY, 0, true));
        list.add(new Scene("axisX", axis(Primitive3D.AXIS_X), new float[] {-3f, 0f, 0f},
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(4f, 1f, 2f, -3f, 0f, -1f, 0f, 1f, 0f),
                IDENTITY, 0, true));
        list.add(new Scene("axisZ", axis(Primitive3D.AXIS_Z), new float[] {0f, 0f, -4f},
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(1f, 2f, 3f, 0f, 0f, -4f, 0f, 1f, 0f),
                IDENTITY, 0, true));
        list.add(new Scene("ortho", 0, plain,
                Camera.ortho(w, h, 5f, 0.1f, 30f).lookAt(0f, 6f, 8f, 0f, 0f, 0f, 0f, 1f, 0f),
                IDENTITY, 0, true));
        list.add(new Scene("belowDoubleSided", 0, plain,
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(0f, -2f, 4f, 0f, 0f, 0f, 0f, 1f, 0f),
                IDENTITY, sided, true));
        list.add(new Scene("belowCulled", 0, plain,
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(0f, -2f, 4f, 0f, 0f, 0f, 0f, 1f, 0f),
                IDENTITY, 0, false));
        list.add(new Scene("lookingAway", 0, plain,
                Camera.perspective(w, h, 60f, 0.1f, 100f)
                        .lookAt(0f, 2f, 0f, 0f, 3f, -1f, 0f, 1f, 0f),
                IDENTITY, 0, false));
        return list;
    }

    /** Straight-ARGB color and window-z buffers. */
    private static final class Target {
        final int mWidth;
        final int mHeight;
        final int[] mColor;
        final float[] mDepth;
        final int[] mRect = new int[4];

        Target(int width, int height) {
            mWidth = width;
            mHeight = height;
            mColor = new int[width * height];
            mDepth = new float[width * height];
            Arrays.fill(mDepth, Float.POSITIVE_INFINITY);
        }

        int color(int x, int y) {
            return mColor[y * mWidth + x];
        }

        float depth(int x, int y) {
            return mDepth[y * mWidth + x];
        }

        boolean[] drawnMask() {
            boolean[] mask = new boolean[mWidth * mHeight];
            for (int i = 0; i < mask.length; i++) {
                mask[i] = mDepth[i] != Float.POSITIVE_INFINITY;
            }
            return mask;
        }
    }

    /** View and projection matrices, in the engine's conventions. */
    private static final class Camera {
        final int mWidth;
        final int mHeight;
        final float mNear;
        final float[] mView = new float[16];
        final float[] mProj = new float[16];

        private Camera(int width, int height, float near) {
            mWidth = width;
            mHeight = height;
            mNear = near;
        }

        static Camera perspective(int width, int height, float fovYDegrees, float near,
                float far) {
            Camera c = new Camera(width, height, near);
            Matrix4.perspective(c.mProj, (float) Math.toRadians(fovYDegrees),
                    (float) width / height, near, far);
            return c;
        }

        static Camera ortho(int width, int height, float halfHeight, float near, float far) {
            Camera c = new Camera(width, height, near);
            float halfWidth = halfHeight * width / height;
            Matrix4.ortho(c.mProj, -halfWidth, halfWidth, -halfHeight, halfHeight, near, far);
            return c;
        }

        Camera lookAt(float ex, float ey, float ez, float cx, float cy, float cz, float ux,
                float uy, float uz) {
            Matrix4.lookAt(mView, ex, ey, ez, cx, cy, cz, ux, uy, uz);
            return this;
        }

        /** Window z of the eye-space depth {@code zEye}. */
        double windowZ(double zEye) {
            double clipZ = (double) mProj[10] * zEye + mProj[14];
            double clipW = (double) mProj[11] * zEye + mProj[15];
            return 0.5 * clipZ / clipW + 0.5;
        }

        /**
         * Where the eye ray through the pixel position {@code (x, y)} meets the world floor
         * {@code y = 0} (perspective only): {@code x, y, z} and the eye-space distance along
         * the view axis (negative or infinite when the ray misses).
         */
        double[] floorHit(double x, double y) {
            float[] m = mView;
            double dx = (2.0 * x / mWidth - 1.0) / mProj[0];
            double dy = (1.0 - 2.0 * y / mHeight) / mProj[5];
            double dz = -1.0;
            // World direction and eye position (the view is rigid).
            double wx = m[0] * dx + m[1] * dy + m[2] * dz;
            double wy = m[4] * dx + m[5] * dy + m[6] * dz;
            double wz = m[8] * dx + m[9] * dy + m[10] * dz;
            double ox = -(m[0] * m[12] + m[1] * m[13] + m[2] * m[14]);
            double oy = -(m[4] * m[12] + m[5] * m[13] + m[6] * m[14]);
            double oz = -(m[8] * m[12] + m[9] * m[13] + m[10] * m[14]);
            double t = -oy / wy;
            return new double[] {ox + t * wx, oy + t * wy, oz + t * wz, t};
        }
    }

    /** Records the shader's calls. */
    private static final class RecordingShader implements InfinitePlane.Shader {
        int mCalls;
        final float[] mNormal = new float[3];
        final List<float[]> mPoints = new ArrayList<>();

        @Override
        public int shade(float px, float py, float pz, float nx, float ny, float nz) {
            mCalls++;
            mNormal[0] = nx;
            mNormal[1] = ny;
            mNormal[2] = nz;
            mPoints.add(new float[] {px, py, pz});
            return BASE;
        }
    }

    private static boolean drawPlane(InfinitePlane plane, Target t, Camera cam, float[] model,
            int mode, InfinitePlane.Shader shader, boolean uniform) {
        float[] mv = new float[16];
        Matrix4.multiply(mv, cam.mView, model);
        return plane.rasterize(t.mColor, t.mDepth, t.mWidth, t.mHeight, mv, cam.mProj, mode, 0f,
                0f, uniform, t.mRect, shader);
    }

    /**
     * Draws {@code mesh} flat in one color as the engine draws meshes: near clipping in clip
     * space ({@code z ≥ −w}, {@code w ≥ 10⁻⁴}), back faces (positive screen-space signed area)
     * culled unless {@code doubleSided}, and {@link Rasterizer#fillTriangle}.
     */
    private static void drawMesh(MeshData mesh, Target t, Camera cam, float[] model, int color,
            boolean doubleSided) {
        float[] mv = new float[16];
        float[] mvp = new float[16];
        Matrix4.multiply(mv, cam.mView, model);
        Matrix4.multiply(mvp, cam.mProj, mv);
        float[] poly = new float[4 * 8];
        float[] scratch = new float[4 * 8];
        float[] s = new float[3 * 8];
        int[] idx = mesh.indices;
        float[] v = mesh.verts;
        for (int i = 0; i + 2 < idx.length; i += 3) {
            for (int k = 0; k < 3; k++) {
                int vi = 3 * idx[i + k];
                for (int r = 0; r < 4; r++) {
                    poly[4 * k + r] = mvp[r] * v[vi] + mvp[4 + r] * v[vi + 1]
                            + mvp[8 + r] * v[vi + 2] + mvp[12 + r];
                }
            }
            int n = clip(poly, scratch, 3, 1f, 1f, 0f);
            n = clip(poly, scratch, n, 0f, 1f, -1e-4f);
            if (n < 3) {
                continue;
            }
            float area = 0f;
            for (int k = 0; k < n; k++) {
                float iw = 1f / poly[4 * k + 3];
                s[3 * k] = (poly[4 * k] * iw * 0.5f + 0.5f) * t.mWidth;
                s[3 * k + 1] = (1f - (poly[4 * k + 1] * iw * 0.5f + 0.5f)) * t.mHeight;
                s[3 * k + 2] = poly[4 * k + 2] * iw * 0.5f + 0.5f;
            }
            for (int k = 0; k < n; k++) {
                int j = (k + 1) % n;
                area += s[3 * k] * s[3 * j + 1] - s[3 * j] * s[3 * k + 1];
            }
            if (!(area < 0f) && !(doubleSided && area > 0f)) {
                continue;
            }
            for (int k = 1; k + 1 < n; k++) {
                Rasterizer.fillTriangle(t.mDepth, t.mColor, color, t.mWidth, t.mHeight,
                        s[0], s[1], s[2], s[3 * k], s[3 * k + 1], s[3 * k + 2],
                        s[3 * k + 3], s[3 * k + 4], s[3 * k + 5]);
            }
        }
    }

    /** Window z of the scene's plane at the pixel position {@code (x, y)}, ray cast in double. */
    private static double analyticDepth(Scene s, float[] mv, double x, double y) {
        Camera c = s.mCamera;
        int axis = Primitive3D.axisMode(s.mFlags);
        double[] n = axis == Primitive3D.AXIS_X ? new double[] {1, 0, 0}
                : axis == Primitive3D.AXIS_Z ? new double[] {0, 0, 1} : new double[] {0, 1, 0};
        double[] p = new double[3];
        for (int i = 0; i < 3; i++) {
            p[i] = i < s.mParams.length ? s.mParams[i] : 0.0;
        }
        double cx = mv[0] * p[0] + mv[4] * p[1] + mv[8] * p[2] + mv[12];
        double cy = mv[1] * p[0] + mv[5] * p[1] + mv[9] * p[2] + mv[13];
        double cz = mv[2] * p[0] + mv[6] * p[1] + mv[10] * p[2] + mv[14];
        double nx = mv[0] * n[0] + mv[4] * n[1] + mv[8] * n[2];
        double ny = mv[1] * n[0] + mv[5] * n[1] + mv[9] * n[2];
        double nz = mv[2] * n[0] + mv[6] * n[1] + mv[10] * n[2];
        double ndcX = 2.0 * x / c.mWidth - 1.0;
        double ndcY = 1.0 - 2.0 * y / c.mHeight;
        // Eye ray o + t·d with d.z = -1, so the eye-space depth is -t.
        double ox = 0.0;
        double oy = 0.0;
        double dx = 0.0;
        double dy = 0.0;
        if (c.mProj[15] == 0f) {
            dx = ndcX / c.mProj[0];
            dy = ndcY / c.mProj[5];
        } else {
            ox = (ndcX - c.mProj[12]) / c.mProj[0];
            oy = (ndcY - c.mProj[13]) / c.mProj[5];
        }
        double t = ((cx - ox) * nx + (cy - oy) * ny + cz * nz) / (dx * nx + dy * ny - nz);
        return c.windowZ(-t);
    }

    /** Clips the clip-space polygon to {@code a·z + b·w + c ≥ 0}; returns its vertex count. */
    private static int clip(float[] poly, float[] scratch, int n, float a, float b, float c) {
        int m = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float di = a * poly[4 * i + 2] + b * poly[4 * i + 3] + c;
            float dj = a * poly[4 * j + 2] + b * poly[4 * j + 3] + c;
            if (di >= 0f) {
                System.arraycopy(poly, 4 * i, scratch, 4 * m++, 4);
            }
            if ((di >= 0f) != (dj >= 0f)) {
                float f = di / (di - dj);
                for (int r = 0; r < 4; r++) {
                    scratch[4 * m + r] = poly[4 * i + r] + f * (poly[4 * j + r] - poly[4 * i + r]);
                }
                m++;
            }
        }
        System.arraycopy(scratch, 0, poly, 0, 4 * m);
        return m;
    }

    /**
     * Distance from {@code (x, y)} to the boundary of the convex polygon, positive inside and
     * negative outside.
     */
    private static double signedDistanceInside(float[] xy, int n, double x, double y) {
        double area = 0.0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += (double) xy[2 * i] * xy[2 * j + 1] - (double) xy[2 * j] * xy[2 * i + 1];
        }
        double sign = area > 0.0 ? 1.0 : -1.0;
        double inside = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ex = xy[2 * j] - xy[2 * i];
            double ey = xy[2 * j + 1] - xy[2 * i + 1];
            double len = Math.hypot(ex, ey);
            if (len == 0.0) {
                continue;
            }
            double cross = (ex * (y - xy[2 * i + 1]) - ey * (x - xy[2 * i])) / len;
            inside = Math.min(inside, sign * cross);
        }
        return inside;
    }

    /** Whether the mask differs anywhere in the 3×3 neighborhood of {@code (x, y)}. */
    private static boolean nearEdge(boolean[] mask, int w, int h, int x, int y) {
        boolean center = mask[y * w + x];
        for (int j = Math.max(0, y - 1); j <= Math.min(h - 1, y + 1); j++) {
            for (int i = Math.max(0, x - 1); i <= Math.min(w - 1, x + 1); i++) {
                if (mask[j * w + i] != center) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertUntouched(Target t) {
        for (int i = 0; i < t.mColor.length; i++) {
            assertEquals(BACKGROUND, t.mColor[i]);
            assertEquals(Float.POSITIVE_INFINITY, t.mDepth[i], 0f);
        }
    }

    /**
     * A top-down orthographic view, 1 px per unit: floor point (x, 0, z) at pixel
     * (x + s/2, z + s/2).
     */
    private static Camera topDown(int size) {
        return Camera.ortho(size, size, size / 2f, 0.1f, 100f)
                .lookAt(0f, 10f, 0f, 0f, 0f, 0f, 0f, 0f, -1f);
    }

    /** Params for a floor through (cx, 0, cz) with opaque white grid lines. */
    private static float[] gridParams(float cx, float cz, float spacing, float lineWidth) {
        return new float[] {cx, 0f, cz, spacing, lineWidth, 1f, 1f, 1f, 1f};
    }

    private static int axis(int axis) {
        return axis << Primitive3D.FLAG_AXIS_SHIFT;
    }

    private static float[] identity() {
        float[] m = new float[16];
        Matrix4.identity(m);
        return m;
    }

    /** A rotation by {@code degrees} about Y followed by a translation. */
    private static float[] model(float degrees, float tx, float ty, float tz) {
        double a = Math.toRadians(degrees);
        float c = (float) Math.cos(a);
        float s = (float) Math.sin(a);
        float[] m = identity();
        m[0] = c;
        m[2] = -s;
        m[8] = s;
        m[10] = c;
        m[12] = tx;
        m[13] = ty;
        m[14] = tz;
        return m;
    }
}
