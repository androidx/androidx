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

package androidx.compose.remote.player.core.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.compose.remote.core.Paint3DContext;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;
import androidx.compose.remote.core.operations.paint.PaintBundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Tests how {@link AndroidPaintContext} composites 3D: one blit per run of 3D draws, flushed by
 * the next 2D call, {@code clearDepth3D} or the end of the frame; a bitmap per blit on a
 * recording canvas; the {@code mode} engine decode; and the viewport of the painted component.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AndroidPaintContext3DTest {
    private static final int SIZE = 200;
    private static final int MESH = 7;
    private static final int PLANE = 8;
    /** An infinite plane through (0, -1, 0) with normal +y (a floor), without a grid. */
    private static final float[] FLOOR = {0f, -1f, 0f, 0f, 1f, 1f, 0.5f, 0.5f, 0.5f};

    private AndroidRemoteContext mRemoteContext;
    private LoggingCanvas mCanvas;
    private AndroidPaintContext mPaintContext;

    /** One composited bitmap: where it went and a copy of its pixels at the time. */
    private static final class Blit {
        final Bitmap mBitmap;
        final int mLeft;
        final int mTop;
        final int mWidth;
        final int mHeight;
        final int[] mPixels;

        Blit(Bitmap bitmap, float left, float top) {
            mBitmap = bitmap;
            mLeft = (int) left;
            mTop = (int) top;
            mWidth = bitmap.getWidth();
            mHeight = bitmap.getHeight();
            mPixels = new int[mWidth * mHeight];
            bitmap.getPixels(mPixels, 0, mWidth, 0, 0, mWidth, mHeight);
        }

        /** Bounds {left, top, right, bottom} (exclusive) of the drawn pixels, in the canvas. */
        int[] drawnBounds() {
            int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (int y = 0; y < mHeight; y++) {
                for (int x = 0; x < mWidth; x++) {
                    if (mPixels[y * mWidth + x] != 0) {
                        b[0] = Math.min(b[0], mLeft + x);
                        b[1] = Math.min(b[1], mTop + y);
                        b[2] = Math.max(b[2], mLeft + x + 1);
                        b[3] = Math.max(b[3], mTop + y + 1);
                    }
                }
            }
            assertTrue("the blit has no drawn pixels", b[0] < b[2]);
            return b;
        }

        boolean drawnAt(int canvasX, int canvasY) {
            int x = canvasX - mLeft;
            int y = canvasY - mTop;
            return x >= 0 && y >= 0 && x < mWidth && y < mHeight && mPixels[y * mWidth + x] != 0;
        }

        /** Whether any pixel in canvas rows [y0, y1) and columns [x0, x1) is drawn. */
        boolean drawnIn(int x0, int y0, int x1, int y1) {
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    if (drawnAt(x, y)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** Records the calls the 3D code makes instead of drawing them. */
    private static final class LoggingCanvas extends Canvas {
        final List<String> mCalls = new ArrayList<>();
        final List<Blit> mBlits = new ArrayList<>();
        boolean mHardware;
        int mSize = SIZE;

        @Override
        public boolean isHardwareAccelerated() {
            return mHardware;
        }

        @Override
        public int getWidth() {
            return mSize;
        }

        @Override
        public int getHeight() {
            return mSize;
        }

        @Override
        public void drawBitmap(@NonNull Bitmap bitmap, float left, float top,
                @Nullable Paint paint) {
            mCalls.add("bitmap");
            mBlits.add(new Blit(bitmap, left, top));
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                @NonNull Paint paint) {
            mCalls.add("rect");
        }

        /** Mode, positions and colors of each drawVertices call, copied at the time. */
        final List<VertexMode> mVertexModes = new ArrayList<>();
        final List<float[]> mVerts = new ArrayList<>();
        final List<int @Nullable []> mVertexColors = new ArrayList<>();

        @Override
        public void drawVertices(@NonNull VertexMode mode, int vertexCount,
                float @NonNull [] verts, int vertOffset, float @Nullable [] texs, int texOffset,
                int @Nullable [] colors, int colorOffset, short @Nullable [] indices,
                int indexOffset, int indexCount, @NonNull Paint paint) {
            mCalls.add("vertices");
            mVertexModes.add(mode);
            mVerts.add(Arrays.copyOfRange(verts, vertOffset, vertOffset + vertexCount));
            mVertexColors.add(colors == null ? null
                    : Arrays.copyOfRange(colors, colorOffset, colorOffset + vertexCount / 2));
        }

        int mSaveCount = 1;
        final float[] mClip = new float[4];

        @Override
        public int save() {
            return mSaveCount++;
        }

        @Override
        public void restore() {
            mSaveCount--;
        }

        @Override
        public void restoreToCount(int saveCount) {
            mSaveCount = saveCount;
        }

        @Override
        public boolean clipRect(float left, float top, float right, float bottom) {
            mClip[0] = left;
            mClip[1] = top;
            mClip[2] = right;
            mClip[3] = bottom;
            return true;
        }
    }

    @Before
    public void setUp() {
        mRemoteContext = new AndroidRemoteContext();
        mCanvas = new LoggingCanvas();
        mPaintContext = new AndroidPaintContext(mRemoteContext, mCanvas);
        startScene(1f);
    }

    /** Camera at the origin looking down -z, and a 2 x 2 square at z = -5 facing it. */
    private void startScene(float aspect) {
        // An opaque color: a reset Paint may be transparent, which would draw nothing visible.
        setColor(0xFF3366CC);
        mPaintContext.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {(float) Math.toRadians(60), aspect, 0.1f, 100f},
                new float[] {0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f});
        mPaintContext.defineMesh3D(MESH, new int[] {0, 1, 2, 0, 2, 3},
                new float[] {-1f, -1f, -5f, 1f, -1f, -5f, 1f, 1f, -5f, -1f, 1f, -5f}, null);
        mPaintContext.clearDepth3D();
    }

    private void setColor(int argb) {
        PaintBundle paint = new PaintBundle();
        paint.setColor(argb);
        mPaintContext.applyPaint(paint);
    }

    /** Draws the square with its center moved to (x, y, z). */
    private void drawSquareAt(float x, float y, float z, int mode) {
        mPaintContext.matrix3DOp(Paint3DContext.M3_IDENTITY, new float[0]);
        mPaintContext.matrix3DOp(Paint3DContext.M3_TRANSLATE, new float[] {x, y, z + 5f});
        mPaintContext.drawMesh3D(MESH, mode);
    }

    private void newFrame() {
        mPaintContext.reset();
        mPaintContext.setCanvas(mCanvas);
        mCanvas.mCalls.clear();
        mCanvas.mBlits.clear();
        mCanvas.mVertexModes.clear();
        mCanvas.mVerts.clear();
        mCanvas.mVertexColors.clear();
    }

    @Test
    public void runOfDrawsIsCompositedOnceAtTheEndOfTheFrame() {
        drawSquareAt(-1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_SMOOTH);
        drawSquareAt(1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_WIREFRAME);
        assertEquals(0, mCanvas.mBlits.size());

        mPaintContext.endFrame3D();
        assertEquals(Arrays.asList("bitmap"), mCanvas.mCalls);
        Blit blit = mCanvas.mBlits.get(0);
        // Only the touched rectangle is copied: the squares span the middle third of the rows.
        assertTrue(blit.mHeight < SIZE / 2);
        assertTrue(blit.drawnAt(40, 100));
        assertTrue(blit.drawnAt(100, 100));
        // The wireframe square (x ~117..187) shows its edges but not its inside.
        assertTrue(blit.drawnIn(140, 60, 190, 140));
        assertTrue(!blit.drawnAt(175, 120));

        mPaintContext.endFrame3D();
        assertEquals(1, mCanvas.mBlits.size());
    }

    @Test
    public void canvasCallCompositesPendingOutputFirst() {
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.drawRect(0f, 0f, 10f, 10f);
        assertEquals(Arrays.asList("bitmap", "rect"), mCanvas.mCalls);
        mPaintContext.endFrame3D();
        assertEquals(1, mCanvas.mBlits.size());
    }

    @Test
    public void clearDepth3DCompositesPendingOutput() {
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.clearDepth3D();
        assertEquals(1, mCanvas.mBlits.size());
        mPaintContext.endFrame3D();
        assertEquals(1, mCanvas.mBlits.size());
    }

    @Test
    public void saveDoesNotCompositePendingOutput() {
        drawSquareAt(-1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.save();
        mPaintContext.matrixSave();
        drawSquareAt(1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        assertEquals(0, mCanvas.mBlits.size());
        // Restoring does: both squares in one blit.
        mPaintContext.matrixRestore();
        mPaintContext.restore();
        mPaintContext.endFrame3D();
        assertEquals(Arrays.asList("bitmap"), mCanvas.mCalls);
        Blit blit = mCanvas.mBlits.get(0);
        assertTrue(blit.drawnAt(40, 100));
        assertTrue(blit.drawnAt(160, 100));
    }

    @Test
    public void laterBlitsCarryOnlyNewPixels() {
        drawSquareAt(-1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.translate(0f, 0f);
        drawSquareAt(1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        assertEquals(2, mCanvas.mBlits.size());
        int[] first = mCanvas.mBlits.get(0).drawnBounds();
        int[] second = mCanvas.mBlits.get(1).drawnBounds();
        assertTrue(first[2] <= SIZE / 2);
        assertTrue(second[0] >= SIZE / 2);
    }

    @Test
    public void depthIsKeptAcrossBlits() {
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.translate(0f, 0f);
        // A larger square behind the first: only the part around it may show.
        drawSquareAt(0f, 0f, -6f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.matrix3DOp(Paint3DContext.M3_SCALE, new float[] {3f, 3f, 1f});
        mPaintContext.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        assertEquals(2, mCanvas.mBlits.size());
        Blit behind = mCanvas.mBlits.get(1);
        assertTrue(!behind.drawnAt(100, 100));
        assertTrue(behind.drawnAt(100, 40));
    }

    @Test
    public void recordingCanvasGetsABitmapPerBlitAndReusesThemNextFrame() {
        mCanvas.mHardware = true;
        Bitmap[] previous = null;
        for (int frame = 0; frame < 3; frame++) {
            startScene(1f);
            drawSquareAt(-1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
            mPaintContext.drawRect(0f, 0f, 1f, 1f);
            drawSquareAt(1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
            mPaintContext.endFrame3D();
            assertEquals(2, mCanvas.mBlits.size());
            Bitmap[] bitmaps = {mCanvas.mBlits.get(0).mBitmap, mCanvas.mBlits.get(1).mBitmap};
            assertNotSame(bitmaps[0], bitmaps[1]);
            if (previous != null) {
                assertSame(previous[0], bitmaps[0]);
                assertSame(previous[1], bitmaps[1]);
            }
            previous = bitmaps;
            newFrame();
        }
    }

    @Test
    public void immediateCanvasReusesOneBitmap() {
        drawSquareAt(-1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.drawRect(0f, 0f, 1f, 1f);
        drawSquareAt(1.5f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        assertEquals(2, mCanvas.mBlits.size());
        assertSame(mCanvas.mBlits.get(0).mBitmap, mCanvas.mBlits.get(1).mBitmap);
    }

    @Test
    public void recordingCanvasPoolGrowsToTheFrameAndThenAllocatesNothing() {
        mCanvas.mHardware = true;
        int blits = 12; // more than the 8 bitmaps the pool used to be limited to
        Bitmap[] previous = null;
        for (int frame = 0; frame < 3; frame++) {
            Bitmap[] bitmaps = drawFrameOfBlits(blits);
            for (int i = 0; i < blits; i++) {
                for (int j = 0; j < i; j++) {
                    assertNotSame(bitmaps[j], bitmaps[i]);
                }
                if (previous != null) {
                    assertSame(previous[i], bitmaps[i]);
                }
            }
            previous = bitmaps;
        }
    }

    @Test
    public void recordingCanvasPoolDropsTheBitmapsNoRecentFrameUsed() {
        mCanvas.mHardware = true;
        Bitmap[] burst = drawFrameOfBlits(12);
        // Two windows: the second has only frames of 2 blits.
        for (int frame = 0; frame < 2 * AndroidPaintContext.BLIT_POOL_WINDOW_FRAMES; frame++) {
            drawFrameOfBlits(2);
        }
        Bitmap[] after = drawFrameOfBlits(12);
        assertSame(burst[0], after[0]);
        assertSame(burst[1], after[1]);
        for (int i = 2; i < 12; i++) {
            assertNotSame(burst[i], after[i]);
        }
    }

    /**
     * Starts a frame and composites {@code count} squares one at a time, each nearer than the
     * last so that it passes the depth test.
     *
     * @return the bitmap of each blit, in order
     */
    private Bitmap[] drawFrameOfBlits(int count) {
        newFrame();
        startScene(1f);
        for (int i = 0; i < count; i++) {
            drawSquareAt(0f, 0f, -5f + 0.25f * i, Paint3DContext.MODE_SOFTWARE_FLAT);
            mPaintContext.drawRect(0f, 0f, 1f, 1f);
        }
        mPaintContext.endFrame3D();
        assertEquals(count, mCanvas.mBlits.size());
        Bitmap[] bitmaps = new Bitmap[count];
        for (int i = 0; i < count; i++) {
            bitmaps[i] = mCanvas.mBlits.get(i).mBitmap;
        }
        return bitmaps;
    }

    @Test
    public void resetDiscardsPendingOutput() {
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.reset();
        mPaintContext.setCanvas(mCanvas);
        mPaintContext.endFrame3D();
        assertEquals(0, mCanvas.mBlits.size());
    }

    @Test
    public void everyFrameStartsWithAClearDepthBuffer() {
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        int[] first = mCanvas.mBlits.get(0).drawnBounds();
        // Next frame, without reset() (which also empties the scene) and without clearDepth3D:
        // the square must still pass the depth test.
        mPaintContext.setCanvas(mCanvas);
        mCanvas.mBlits.clear();
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        assertEquals(1, mCanvas.mBlits.size());
        assertTrue(Arrays.equals(first, mCanvas.mBlits.get(0).drawnBounds()));
    }

    @Test
    public void modeBitsOneToThreeChooseTheEngine() {
        int canvas = Paint3DContext.MODE_CANVAS_FLAT;
        assertEngine(Paint3DContext.MODE_SOFTWARE_FLAT, "bitmap");
        assertEngine(Paint3DContext.MODE_SOFTWARE_SMOOTH, "bitmap");
        assertEngine(canvas, "vertices");
        assertEngine(Paint3DContext.MODE_CANVAS_SMOOTH, "vertices");
        // Bits 12 and up never change the engine.
        assertEngine(canvas | Paint3DContext.MODE_TRANSPARENT | Paint3DContext.MODE_GPU
                | Paint3DContext.MODE_PBR | Paint3DContext.MODE_DOUBLE_SIDED
                | Paint3DContext.MODE_FRONT_CW, "vertices");
        // Wireframe is software-only.
        assertEngine(canvas | Paint3DContext.MODE_WIREFRAME, "bitmap");
        assertEngine(canvas | Paint3DContext.MODE_WIREFRAME | Paint3DContext.WIRE_EDGE0,
                "bitmap");
        // GL, canvas-zbuf and the reserved engines fall back to software.
        for (int engine = 3; engine <= 7; engine += engine == 3 ? 2 : 1) {
            assertEngine(engine << Paint3DContext.MODE_ENGINE_SHIFT, "bitmap");
        }
        // The drawMesh engines need API 34; this test runs on API 28.
        assertEngine(Paint3DContext.MODE_DRAWMESH_FLAT, "bitmap");
        assertEngine(Paint3DContext.MODE_DRAWMESH_FLAT | Paint3DContext.MODE_TRANSPARENT,
                "bitmap");
        assertEngine(Paint3DContext.MODE_DRAWMESH_ZBUF_SMOOTH, "bitmap");
    }

    private void assertEngine(int mode, String call) {
        newFrame();
        startScene(1f);
        drawSquareAt(0f, 0f, -5f, mode);
        mPaintContext.endFrame3D();
        assertEquals("mode 0x" + Integer.toHexString(mode), Arrays.asList(call),
                mCanvas.mCalls);
    }

    @Test
    public void viewportIsThePaintedComponent() {
        // Without a component the viewport is the canvas.
        assertSquareCenteredAt(100, 100, 1f);

        // A plain component (e.g. a canvas's content): its size.
        mRemoteContext.mLastComponent = new Component(null, 10, -1, 30f, 20f, 120f, 80f);
        assertSquareCenteredAt(60, 40, 1.5f);

        // A layout component: its content box, inside the padding.
        mRemoteContext.mLastComponent =
                new LayoutComponent(null, 11, -1, 0f, 0f, 140f, 100f) {
                    {
                        mPaddingLeft = 10f;
                        mPaddingRight = 10f;
                        mPaddingTop = 4f;
                        mPaddingBottom = 16f;
                    }
                };
        assertSquareCenteredAt(60, 40, 1.5f);

        // A component larger than the canvas (e.g. in a scroller) falls back to the canvas.
        mRemoteContext.mLastComponent = new Component(null, 12, -1, 0f, 0f, 1000f, 900f);
        assertSquareCenteredAt(100, 100, 1f);
    }

    private void assertSquareCenteredAt(int x, int y, float aspect) {
        newFrame();
        startScene(aspect);
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        assertEquals(1, mCanvas.mBlits.size());
        int[] b = mCanvas.mBlits.get(0).drawnBounds();
        assertEquals((float) x, (b[0] + b[2]) / 2f, 1f);
        assertEquals((float) y, (b[1] + b[3]) / 2f, 1f);
    }

    @Test
    public void canvasEngineIsClippedToTheViewport() {
        mRemoteContext.mLastComponent = new Component(null, 10, -1, 30f, 20f, 120f, 80f);
        newFrame();
        startScene(1.5f);
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_CANVAS_SMOOTH);
        assertEquals(Arrays.asList("vertices"), mCanvas.mCalls);
        assertTrue(Arrays.equals(new float[] {0f, 0f, 120f, 80f}, mCanvas.mClip));
        assertEquals(1, mCanvas.mSaveCount);
    }

    @Test
    public void backFacesAreDrawnOnlyWhenDoubleSidedOrFrontWithFrontCw() {
        // The square wound clockwise: the camera sees its back, unless MODE_FRONT_CW makes
        // clockwise triangles front-facing.
        mPaintContext.defineMesh3D(MESH, new int[] {0, 2, 1, 0, 3, 2},
                new float[] {-1f, -1f, -5f, 1f, -1f, -5f, 1f, 1f, -5f, -1f, 1f, -5f}, null);
        int[] modes = {Paint3DContext.MODE_SOFTWARE_FLAT, Paint3DContext.MODE_CANVAS_FLAT};
        String[] calls = {"bitmap", "vertices"};
        int[] flags = {Paint3DContext.MODE_DOUBLE_SIDED, Paint3DContext.MODE_FRONT_CW};
        for (int i = 0; i < modes.length; i++) {
            mCanvas.mCalls.clear();
            drawSquareAt(0f, 0f, -5f, modes[i]);
            mPaintContext.endFrame3D();
            assertEquals(new ArrayList<String>(), mCanvas.mCalls);
            for (int flag : flags) {
                mCanvas.mCalls.clear();
                drawSquareAt(0f, 0f, -5f, modes[i] | flag);
                mPaintContext.endFrame3D();
                assertEquals(Arrays.asList(calls[i]), mCanvas.mCalls);
            }
        }
    }

    @Test
    public void infinitePlaneIsDrawnByEveryEngine() {
        // The drawMesh engines need API 34, so on API 28 the software engine draws.
        int[] modes = {Paint3DContext.MODE_SOFTWARE_FLAT, Paint3DContext.MODE_CANVAS_FLAT,
                Paint3DContext.MODE_DRAWMESH_FLAT, Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT};
        String[] calls = {"bitmap", "vertices", "bitmap", "bitmap"};
        for (int i = 0; i < modes.length; i++) {
            newFrame();
            startScene(1f);
            mPaintContext.defineInfinitePlane3D(PLANE, 0, FLOOR);
            mPaintContext.drawMesh3D(PLANE, modes[i]);
            mPaintContext.endFrame3D();
            assertEquals("mode " + modes[i], Arrays.asList(calls[i]), mCanvas.mCalls);
            if (calls[i].equals("bitmap")) {
                // Below the horizon (row 100) only.
                Blit blit = mCanvas.mBlits.get(0);
                assertTrue(blit.drawnAt(100, 150));
                assertTrue(!blit.drawnIn(0, 0, SIZE, 99));
            }
        }
    }

    @Test
    @Config(sdk = 34)
    public void drawmeshZbufSortsTheRunBackToFrontKeepingTiesInDrawOrder() {
        int zbuf = Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT;
        setColor(0xFFFF0000);
        drawSquareAt(0f, 0f, -5f, zbuf); // near, red
        setColor(0xFF00FF00);
        drawSquareAt(-1.5f, 0f, -8f, zbuf); // far, green, on the left
        setColor(0xFF0000FF);
        drawSquareAt(1.5f, 0f, -8f, zbuf); // just as far, blue, on the right
        assertEquals(new ArrayList<String>(), mCanvas.mCalls);

        mPaintContext.endFrame3D();
        assertEquals(Arrays.asList("vertices"), mCanvas.mCalls);
        assertEquals(Canvas.VertexMode.TRIANGLES, mCanvas.mVertexModes.get(0));
        float[] xy = mCanvas.mVerts.get(0);
        int[] colors = mCanvas.mVertexColors.get(0);
        assertNotNull(colors);
        assertEquals(18, colors.length);
        // Far to near, and the two equally far squares in draw order: green, blue, red.
        int[] channel = {1, 1, 2, 2, 0, 0};
        for (int v = 0; v < 18; v++) {
            int t = v / 3;
            assertEquals("vertex " + v, channel[t], dominantChannel(colors[v]));
            if (t < 4) {
                // Each vertex's position travels with its color.
                assertEquals("vertex " + v, t < 2, xy[2 * v] < SIZE / 2f);
            }
        }
    }

    @Test
    @Config(sdk = 34)
    public void drawmeshZbufDrawsATransparentPlaneAtOnceUnderThePendingRun() {
        int zbuf = Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT;
        drawSquareAt(0f, 0f, -5f, zbuf);
        setColor(0x80FFFFFF);
        mPaintContext.defineInfinitePlane3D(PLANE, 0, FLOOR);
        mPaintContext.drawMesh3D(PLANE, zbuf | Paint3DContext.MODE_TRANSPARENT);
        // The plane at once, as one polygon carrying the paint's alpha.
        assertEquals(Arrays.asList("vertices"), mCanvas.mCalls);
        assertEquals(Canvas.VertexMode.TRIANGLE_FAN, mCanvas.mVertexModes.get(0));
        assertAlphas(0x80, mCanvas.mVertexColors.get(0));
        // The opaque square at the flush, over it.
        mPaintContext.endFrame3D();
        assertEquals(Arrays.asList("vertices", "vertices"), mCanvas.mCalls);
        assertEquals(Canvas.VertexMode.TRIANGLES, mCanvas.mVertexModes.get(1));
        assertAlphas(0xFF, mCanvas.mVertexColors.get(1));
    }

    @Test
    @Config(sdk = 34)
    public void drawmeshBlendsATransparentDrawWithDrawVertices() {
        setColor(0x80FF0000);
        drawSquareAt(0f, 0f, -5f,
                Paint3DContext.MODE_DRAWMESH_FLAT | Paint3DContext.MODE_TRANSPARENT);
        assertEquals(Arrays.asList("vertices"), mCanvas.mCalls);
        assertEquals(Canvas.VertexMode.TRIANGLES, mCanvas.mVertexModes.get(0));
        int[] colors = mCanvas.mVertexColors.get(0);
        assertNotNull(colors);
        assertEquals(6, colors.length);
        assertAlphas(0x80, colors);
    }

    @Test
    @Config(sdk = 34)
    public void softwareDrawAfterAZbufRunLandsOverIt() {
        int zbuf = Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT;
        drawSquareAt(0f, 0f, -5f, zbuf);
        assertEquals(new ArrayList<String>(), mCanvas.mCalls);
        // A wireframe overlay is drawn by the software engine: the zbuf batch goes first.
        drawSquareAt(0f, 0f, -5f, zbuf | Paint3DContext.MODE_WIREFRAME);
        assertEquals(Arrays.asList("vertices"), mCanvas.mCalls);
        mPaintContext.endFrame3D();
        assertEquals(Arrays.asList("vertices", "bitmap"), mCanvas.mCalls);
    }

    @Test
    @Config(sdk = 34)
    public void zbufPlaneAfterSoftwareDrawsLandsOverThem() {
        drawSquareAt(0f, 0f, -5f, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.defineInfinitePlane3D(PLANE, 0, FLOOR);
        mPaintContext.drawMesh3D(PLANE, Paint3DContext.MODE_DRAWMESH_ZBUF_FLAT);
        // The software pixels first, then the plane, at once.
        assertEquals(Arrays.asList("bitmap", "vertices"), mCanvas.mCalls);
        assertEquals(Canvas.VertexMode.TRIANGLE_FAN, mCanvas.mVertexModes.get(0));
        mPaintContext.endFrame3D();
        assertEquals(Arrays.asList("bitmap", "vertices"), mCanvas.mCalls);
    }

    /** 0, 1 or 2: which of red, green and blue is the strongest in {@code argb}. */
    private static int dominantChannel(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r >= g && r >= b ? 0 : g >= b ? 1 : 2;
    }

    private static void assertAlphas(int alpha, int @Nullable [] colors) {
        assertNotNull(colors);
        assertTrue(colors.length > 0);
        for (int c : colors) {
            assertEquals(String.format("%08X", c), alpha, c >>> 24);
        }
    }

    @Test
    public void zbufSortMatchesAStableSortByDecreasingDepth() {
        Random random = new Random(42);
        float[] special = {0f, -0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.MIN_VALUE, -Float.MIN_VALUE, -1f};
        for (int n : new int[] {0, 1, 2, 7, 300, 5000}) {
            for (boolean nearlySorted : new boolean[] {false, true}) {
                float[] depth = new float[n];
                for (int i = 0; i < n; i++) {
                    // Few distinct values, so many ties.
                    depth[i] = nearlySorted ? (n - i) / 64 / 8f
                            : random.nextInt(8) == 0 ? special[random.nextInt(special.length)]
                            : random.nextInt(16) / 8f;
                }
                Integer[] expected = new Integer[n];
                for (int i = 0; i < n; i++) {
                    expected[i] = i;
                }
                Arrays.sort(expected, (a, b) -> Float.compare(depth[b], depth[a]));
                long[] order = new long[n];
                AndroidPaintContext.sortBackToFront(depth, n, order);
                for (int i = 0; i < n; i++) {
                    assertEquals("n " + n + ", entry " + i, (int) expected[i], (int) order[i]);
                }
            }
        }
    }

    @Test
    public void clearDepth3DEmptiesTheMatrixStackEvenWithoutAViewport() {
        mPaintContext.matrix3DOp(Paint3DContext.M3_IDENTITY, new float[0]);
        mPaintContext.matrix3DOp(Paint3DContext.M3_PUSH, new float[0]);
        mPaintContext.matrix3DOp(Paint3DContext.M3_TRANSLATE, new float[] {1.5f, 0f, 0f});
        mCanvas.mSize = 0;
        mPaintContext.clearDepth3D();
        mCanvas.mSize = SIZE;
        // The push went with the scene, so this pop has nothing to restore.
        mPaintContext.matrix3DOp(Paint3DContext.M3_POP, new float[0]);
        mPaintContext.drawMesh3D(MESH, Paint3DContext.MODE_SOFTWARE_FLAT);
        mPaintContext.endFrame3D();
        Blit blit = mCanvas.mBlits.get(0);
        assertTrue(blit.drawnAt(150, 100));
        assertTrue(!blit.drawnAt(100, 100));
    }
}
