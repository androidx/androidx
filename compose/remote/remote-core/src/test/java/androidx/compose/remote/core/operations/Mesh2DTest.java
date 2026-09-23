/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.utilities.Mesh2DGenerator;
import androidx.compose.remote.core.testing.HeadlessRemoteContext;
import androidx.compose.remote.core.testing.SoftwareRasterPaintContext;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Tests for the 2D mesh operations, checked by rasterising them.
 *
 * <p>Mesh bugs are overwhelmingly geometric - a grid wired with the wrong winding, a polar layout
 * that fails to close the loop, a fan whose apex is not shared - and every one of those produces
 * vertices that pass a bounds assertion while drawing something visibly wrong. So these tests draw
 * the mesh and assert on pixels, and write a PNG on the way so a surprising failure can be looked
 * at rather than puzzled over.
 */
public class Mesh2DTest {

    private static final int SIZE = 128;
    private static final int BACKGROUND = 0xFF000000;

    /** Build a context with a rasteriser attached, ready to paint into. */
    private static SoftwareRasterPaintContext newRaster(HeadlessRemoteContext context) {
        SoftwareRasterPaintContext raster = new SoftwareRasterPaintContext(context, SIZE, SIZE);
        context.setPaintContext(raster);
        return raster;
    }

    /**
     * Register a mesh and then draw it, as a document does.
     *
     * <p>Defining a mesh and drawing it are separate operations - that separation is what lets one
     * mesh be drawn several times, and read for a matrix without being drawn at all - so a test
     * that only paints the {@link AddMesh2D} correctly renders nothing.
     */
    private static void addAndDraw(
            HeadlessRemoteContext context,
            SoftwareRasterPaintContext raster,
            AddMesh2D mesh,
            int meshId) {
        mesh.updateVariables(context);
        mesh.paint(raster);
        new DrawMesh2D(meshId, DrawMesh2D.BLEND_COLORS_ONLY, DrawMesh2D.NO_IMAGE).paint(raster);
    }

    /** A constant-valued RPN expression. */
    private static float[] constant(float value) {
        return new float[] {value};
    }

    /** The RPN for {@code var * scale}, where var is one of the domain parameters. */
    private static float[] scaled(float var, float scale) {
        return new float[] {var, scale, AnimatedFloatExpressionOps.MUL};
    }

    /** The multiply opcode, named so the expressions above read as arithmetic. */
    private static final class AnimatedFloatExpressionOps {
        static final float MUL =
                androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
    }

    private static final float VAR_U =
            androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1;
    private static final float VAR_V =
            androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR2;

    // ------------------------------------------------------------- topology

    @Test
    public void gridTopologyCoversEveryCell() {
        int uCount = 4;
        int vCount = 3;
        assertEquals(
                uCount * vCount,
                Mesh2DGenerator.vertexCount(Mesh2DGenerator.LAYOUT_GRID, uCount, vCount));
        // (uCount - 1) * (vCount - 1) cells, two triangles each, three indices per triangle.
        assertEquals(
                (uCount - 1) * (vCount - 1) * 6,
                Mesh2DGenerator.indexCount(Mesh2DGenerator.LAYOUT_GRID, uCount, vCount));
    }

    @Test
    public void wrappingLayoutsCloseTheLoop() {
        // A polar ring must join its last column back to its first, so u runs i/uCount and the
        // final column is a real column rather than a duplicate of the first.
        assertEquals(0f, Mesh2DGenerator.domainU(Mesh2DGenerator.LAYOUT_POLAR, 0, 4), 0f);
        assertEquals(0.75f, Mesh2DGenerator.domainU(Mesh2DGenerator.LAYOUT_POLAR, 3, 4), 1e-6f);
        // A grid does not wrap, so its last column sits exactly at u = 1.
        assertEquals(1f, Mesh2DGenerator.domainU(Mesh2DGenerator.LAYOUT_GRID, 3, 4), 1e-6f);
    }

    @Test
    public void fanSharesOneCentreVertex() {
        int uCount = 8;
        assertEquals(
                uCount + 1, Mesh2DGenerator.vertexCount(Mesh2DGenerator.LAYOUT_FAN, uCount, 2));
    }

    // ----------------------------------------------------------- half float

    @Test
    public void halfFloatRoundTripsScreenCoordinates() {
        // Half floats are exact for integers to 2048, which is the bound the spec states.
        for (float value : new float[] {0f, 1f, 0.5f, -1f, 255f, 2048f, -2048f}) {
            int half = Mesh2DGenerator.floatToHalf(value);
            assertEquals(value, Mesh2DGenerator.halfToFloat(half), 0f);
        }
        // And inexact beyond it, which is why the spec says 4096 is where it stops being free.
        assertNotEquals(4097f, Mesh2DGenerator.halfToFloat(Mesh2DGenerator.floatToHalf(4097f)), 0f);
    }

    // ----------------------------------------------------------- rasterised

    @Test
    public void expressionGridFillsTheRectangleItDescribes() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        // A flat grid spanning the middle of the framebuffer, in solid red.
        AddMesh2D mesh =
                new AddMesh2D(
                        1,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_GRID,
                        8,
                        8,
                        0,
                        0,
                        new float[][] {
                            scaled(VAR_U, 64f), // x: 0..64
                            scaled(VAR_V, 64f), // y: 0..64
                            null,
                            null,
                            constant(1f), // a
                            constant(1f), // r
                            constant(0f), // g
                            constant(0f), // b
                            null,
                        },
                        null,
                        null,
                        null,
                        null);

        addAndDraw(context, raster, mesh, 1);

        java.io.File png = raster.writePng("grid");

        // Inside the described rectangle is red; outside is untouched.
        assertEquals("see " + png, 0xFFFF0000, raster.pixelAt(32, 32));
        assertEquals("see " + png, BACKGROUND, raster.pixelAt(100, 100));
        // A 64x64 patch, allowing a pixel of slack on each edge from the scanline rule.
        int covered = raster.coveredPixels(BACKGROUND);
        assertTrue("covered " + covered + ", see " + png, covered > 63 * 63 && covered <= 65 * 65);
    }

    @Test
    public void vertexColoursInterpolateAcrossTheSurface() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        // Red ramps with u across a 100-wide grid: this is the gradient the paint layer cannot
        // express, and the whole argument for vertex colour.
        AddMesh2D mesh =
                new AddMesh2D(
                        1,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_GRID,
                        16,
                        2,
                        0,
                        0,
                        new float[][] {
                            scaled(VAR_U, 100f),
                            scaled(VAR_V, 100f),
                            null,
                            null,
                            constant(1f),
                            new float[] {VAR_U},
                            constant(0f),
                            constant(0f),
                            null,
                        },
                        null,
                        null,
                        null,
                        null);

        addAndDraw(context, raster, mesh, 1);

        java.io.File png = raster.writePng("gradient");

        int left = raster.pixelAt(5, 50) >> 16 & 0xFF;
        int middle = raster.pixelAt(50, 50) >> 16 & 0xFF;
        int right = raster.pixelAt(95, 50) >> 16 & 0xFF;
        assertTrue(
                "expected a red ramp, got " + left + "/" + middle + "/" + right + ", see " + png,
                left < middle && middle < right);
    }

    @Test
    public void polarLayoutDrawsAClosedDisc() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        // No position expressions at all: the layout's default geometry is the unit disc, and the
        // canvas matrix is what places and sizes it. That is the point of the layout defaults.
        AddMesh2D mesh =
                new AddMesh2D(
                        1,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_POLAR,
                        24,
                        2,
                        0,
                        0,
                        new float[][] {
                            null,
                            null,
                            null,
                            null,
                            constant(1f),
                            constant(0f),
                            constant(1f),
                            constant(0f),
                            null,
                        },
                        null,
                        null,
                        null,
                        null);

        raster.translate(64f, 64f);
        raster.scale(50f, 50f);

        addAndDraw(context, raster, mesh, 1);

        java.io.File png = raster.writePng("polar");

        // Green at the centre and near the rim in all four directions - the loop is closed.
        assertEquals("see " + png, 0xFF00FF00, raster.pixelAt(64, 64));
        assertEquals("centre-right, see " + png, 0xFF00FF00, raster.pixelAt(100, 64));
        assertEquals("centre-left, see " + png, 0xFF00FF00, raster.pixelAt(28, 64));
        assertEquals("above centre, see " + png, 0xFF00FF00, raster.pixelAt(64, 28));
        assertEquals("below centre, see " + png, 0xFF00FF00, raster.pixelAt(64, 100));
        // And nothing outside the disc.
        assertEquals("corner, see " + png, BACKGROUND, raster.pixelAt(120, 120));
    }

    @Test
    public void literalValuesDrawTheTriangleTheyDescribe() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        AddMesh2D mesh =
                new AddMesh2D(
                        1,
                        AddMesh2D.TYPE_VALUES,
                        Mesh2DGenerator.LAYOUT_GRID,
                        0,
                        0,
                        0,
                        0,
                        null,
                        new int[] {0, 1, 2},
                        new float[] {10f, 10f, 110f, 10f, 10f, 110f},
                        null,
                        new int[] {0xFF0000FF, 0xFF0000FF, 0xFF0000FF});

        addAndDraw(context, raster, mesh, 1);

        java.io.File png = raster.writePng("literal-triangle");

        // Inside the lower-left half of the described square.
        assertEquals("see " + png, 0xFF0000FF, raster.pixelAt(20, 20));
        // The opposite corner is outside the triangle.
        assertEquals("see " + png, BACKGROUND, raster.pixelAt(100, 100));
    }

    // -------------------------------------------------------- matrix from mesh

    @Test
    public void matrixFromMeshPlacesContentOnTheSurface() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        AddMesh2D mesh =
                new AddMesh2D(
                        7,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_GRID,
                        8,
                        8,
                        0,
                        0,
                        new float[][] {
                            scaled(VAR_U, 80f),
                            scaled(VAR_V, 40f),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                        },
                        null,
                        null,
                        null,
                        null);
        mesh.updateVariables(context);
        mesh.paint(raster);

        // Sampling the centre of a plain axis-aligned grid should give the centre point and an
        // unrotated frame, scaled by the patch size.
        MatrixFromMesh2D matrix = new MatrixFromMesh2D(7, 0.5f, 0.5f, MatrixFromMesh2D.FLAG_ORIGIN);
        matrix.updateVariables(context);
        matrix.paint(raster);

        float[] result = raster.currentMatrix();
        assertEquals("translate x", 40f, result[4], 0.5f);
        assertEquals("translate y", 20f, result[5], 0.5f);
    }

    @Test
    public void degeneratePatchFallsBackInsteadOfCollapsing() {
        // At a fan's apex du and dv are parallel, so the full affine is singular. The contract is
        // to fall back to a lower flag rather than emit a matrix that scales content to nothing;
        // every implementation would otherwise pick its own answer.
        // The frame is [duX, duY, dvX, dvY, originX, originY]: both axes collapse to zero here,
        // which is exactly the apex case.
        float[] frame = {0f, 0f, 0f, 0f, 10f, 20f};
        float[] out = new float[6];
        Mesh2DGenerator.buildMatrix(frame, MatrixFromMesh2D.FLAG_FULL, out);

        assertEquals("origin x survives", 10f, out[4], 0f);
        assertEquals("origin y survives", 20f, out[5], 0f);
        // The linear part must stay invertible - identity, not zero.
        assertEquals(1f, out[0], 0f);
        assertEquals(0f, out[1], 0f);
        assertEquals(0f, out[2], 0f);
        assertEquals(1f, out[3], 0f);
    }

    // ------------------------------------------------------------ wire format

    @Test
    public void expressionMeshSurvivesTheWire() {
        float[][] expressions =
                new float[][] {
                    scaled(VAR_U, 3f),
                    scaled(VAR_V, 5f),
                    null,
                    null,
                    constant(1f),
                    constant(0.25f),
                    constant(0.5f),
                    constant(0.75f),
                    constant(12f),
                };
        AddMesh2D original =
                new AddMesh2D(
                        3,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_PATH_STRIP,
                        16,
                        2,
                        0,
                        9,
                        expressions,
                        null,
                        null,
                        null,
                        null);

        WireBuffer buffer = new WireBuffer();
        original.write(buffer);
        buffer.setIndex(0);
        buffer.readByte(); // the opcode, consumed by the dispatcher in the real reader

        ArrayList<Operation> operations = new ArrayList<>();
        AddMesh2D.read(buffer, operations);

        assertEquals(1, operations.size());
        assertEquals(original.toString(), operations.get(0).toString());
    }

    @Test
    public void halfFloatMeshWidensOnRead() {
        AddMesh2D original =
                new AddMesh2D(
                        4,
                        AddMesh2D.TYPE_F16_VALUES,
                        Mesh2DGenerator.LAYOUT_GRID,
                        2,
                        2,
                        0,
                        0,
                        null,
                        new int[] {0, 1, 2},
                        new float[] {0f, 0f, 16f, 0f, 0f, 16f},
                        new float[] {0f, 0f, 1f, 0f, 0f, 1f},
                        null);

        WireBuffer buffer = new WireBuffer();
        original.write(buffer);
        buffer.setIndex(0);
        buffer.readByte();

        ArrayList<Operation> operations = new ArrayList<>();
        AddMesh2D.read(buffer, operations);

        // Nothing downstream should be able to tell the document used half floats: the values it
        // carried are exactly representable, so they come back bit for bit.
        assertEquals(1, operations.size());
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);
        AddMesh2D restored = (AddMesh2D) operations.get(0);
        addAndDraw(context, raster, restored, 4);
        // The triangle's coordinates were all exactly representable as halves, so it lands in
        // exactly the pixels the 32-bit form would have covered.
        assertTrue(raster.coveredPixels(BACKGROUND) > 0);
        assertNotEquals(BACKGROUND, raster.pixelAt(3, 3));
        assertEquals(BACKGROUND, raster.pixelAt(60, 60));
    }

    @Test
    public void ringLayoutLeavesCenterHollowAndScalesLocalFrame() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        // Default LAYOUT_RING spans radii 0.5..1.0 and wraps u seamlessly around the full circle.
        AddMesh2D mesh =
                new AddMesh2D(
                        10,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_RING,
                        32,
                        4,
                        0,
                        0,
                        new float[][] {
                            null,
                            null,
                            null,
                            null,
                            constant(1f),
                            constant(1f),
                            constant(0.8f),
                            constant(0.2f),
                            null,
                        },
                        null,
                        null,
                        null,
                        null);

        raster.translate(64f, 64f);
        raster.scale(50f, 50f);
        addAndDraw(context, raster, mesh, 10);

        java.io.File png = raster.writePng("ring");

        // The centre of a Ring (r < 0.5 * 50 = 25) is hollow, while the annulus (r ~ 38) is filled.
        assertEquals("ring centre is hollow, see " + png, BACKGROUND, raster.pixelAt(64, 64));
        assertNotEquals("ring right band filled, see " + png, BACKGROUND, raster.pixelAt(102, 64));
        assertNotEquals("ring left band filled, see " + png, BACKGROUND, raster.pixelAt(26, 64));

        // Check FLAG_SCALE on the ring: the radial frame has non-zero scale magnitudes.
        MatrixFromMesh2D scaleMatrix =
                new MatrixFromMesh2D(10, 0f, 0.5f, MatrixFromMesh2D.FLAG_SCALE);
        scaleMatrix.updateVariables(context);
        raster.matrixSave();
        scaleMatrix.paint(raster);
        float[] m = raster.currentMatrix();
        raster.matrixRestore();
        float scaleU = (float) Math.hypot(m[0], m[1]);
        float scaleV = (float) Math.hypot(m[2], m[3]);
        assertTrue(
                "expected non-zero scale factors, got " + scaleU + ", " + scaleV,
                scaleU > 1f && scaleV > 1f);
    }

    @Test
    public void fanLayoutRadiatesFromSingleSharedApex() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        AddMesh2D mesh =
                new AddMesh2D(
                        11,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_FAN,
                        36,
                        2,
                        0,
                        0,
                        new float[][] {
                            null,
                            null,
                            null,
                            null,
                            constant(1f),
                            constant(0.2f),
                            constant(1f),
                            constant(0.6f),
                            null,
                        },
                        null,
                        null,
                        null,
                        null);

        raster.translate(64f, 64f);
        raster.scale(48f, 48f);
        addAndDraw(context, raster, mesh, 11);

        java.io.File png = raster.writePng("fan");
        assertEquals("fan apex is filled, see " + png, 0xFF33FF99, raster.pixelAt(64, 64));
        assertEquals("fan rim is filled, see " + png, 0xFF33FF99, raster.pixelAt(100, 64));
        assertEquals("outside fan is empty, see " + png, BACKGROUND, raster.pixelAt(120, 120));
    }

    @Test
    public void texturedMeshModulateAndColorsOnlyModes() throws IOException {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        // Register a 2x2 checkerboard texture: left column pure cyan (0xFF00FFFF), right column
        // pure yellow (0xFFFFFF00).
        HeadlessRemoteContext.BitmapBuffer tex = new HeadlessRemoteContext.BitmapBuffer(2, 2);
        tex.pixels[0] = 0xFF00FFFF;
        tex.pixels[1] = 0xFFFFFF00;
        tex.pixels[2] = 0xFF00FFFF;
        tex.pixels[3] = 0xFFFFFF00;
        context.putBitmap(42, tex);

        // A grid spanning (0,0)..(100,100) with vertex colour = pure red (1, 0.5, 0.5) and custom
        // texU = u, texV = v.
        AddMesh2D mesh =
                new AddMesh2D(
                        12,
                        AddMesh2D.TYPE_EXPRESSION,
                        Mesh2DGenerator.LAYOUT_GRID,
                        4,
                        4,
                        0,
                        0,
                        new float[][] {
                            scaled(VAR_U, 100f),
                            scaled(VAR_V, 100f),
                            new float[] {VAR_U},
                            new float[] {VAR_V},
                            constant(1f),
                            constant(1f),
                            constant(0.5f),
                            constant(0.5f),
                            null,
                        },
                        null,
                        null,
                        null,
                        null);
        mesh.updateVariables(context);
        mesh.paint(raster);

        // 1. Draw with BLEND_MODULATE: left half multiplies vertex (255, 128, 128) by cyan
        // (0, 255, 255) -> red channel becomes 0! Right half multiplies by yellow (255, 255, 0) ->
        // blue channel becomes 0!
        new DrawMesh2D(12, DrawMesh2D.BLEND_MODULATE, 42).paint(raster);
        int modLeft = raster.pixelAt(15, 50);
        int modRight = raster.pixelAt(85, 50);
        assertEquals("modulate left red channel should be 0", 0, (modLeft >>> 16) & 0xFF);
        assertTrue("modulate left green channel > 100", ((modLeft >>> 8) & 0xFF) > 100);
        assertTrue("modulate right red channel > 200", ((modRight >>> 16) & 0xFF) > 200);
        assertEquals("modulate right blue channel should be 0", 0, modRight & 0xFF);

        // 2. Clear and draw the same mesh and imageId with BLEND_COLORS_ONLY: the bound texture is
        // ignored and both sides draw the pure vertex colour (255, 128, 128).
        raster.clear(BACKGROUND);
        new DrawMesh2D(12, DrawMesh2D.BLEND_COLORS_ONLY, 42).paint(raster);
        int colLeft = raster.pixelAt(15, 50);
        int colRight = raster.pixelAt(85, 50);
        assertEquals("ColorsOnly ignores texture across surface", colLeft, colRight);
        assertEquals(255, (colLeft >>> 16) & 0xFF);
    }

    @Test
    public void matrixFromLiteralMeshExtractsFullShearedFrame() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        SoftwareRasterPaintContext raster = newRaster(context);

        // A 2x2 literal grid (TYPE_VALUES) with horizontal shear along v:
        // (u=0,v=0)->(10,10), (u=1,v=0)->(90,10), (u=0,v=1)->(40,70), (u=1,v=1)->(120,70)
        // Here du = (80, 0) and dv = (30, 60) - non-orthogonal!
        AddMesh2D mesh =
                new AddMesh2D(
                        13,
                        AddMesh2D.TYPE_VALUES,
                        Mesh2DGenerator.LAYOUT_GRID,
                        2,
                        2,
                        0,
                        0,
                        null,
                        new int[] {0, 1, 2, 1, 3, 2},
                        new float[] {10f, 10f, 90f, 10f, 40f, 70f, 120f, 70f},
                        null,
                        null);
        mesh.updateVariables(context);
        mesh.paint(raster);

        MatrixFromMesh2D full = new MatrixFromMesh2D(13, 0.5f, 0.5f, MatrixFromMesh2D.FLAG_FULL);
        full.updateVariables(context);
        full.paint(raster);

        float[] m = raster.currentMatrix();
        // Origin at (0.5, 0.5) is (65, 40); du = (80, 0), dv = (30, 60) -> m[2] (shear c) is 30!
        assertEquals("duX", 80f, m[0], 0.5f);
        assertEquals("duY", 0f, m[1], 0.5f);
        assertEquals("dvX (shear)", 30f, m[2], 0.5f);
        assertEquals("dvY", 60f, m[3], 0.5f);
        assertEquals("originX", 65f, m[4], 0.5f);
        assertEquals("originY", 40f, m[5], 0.5f);
    }

    // --------------------------------------------------- utilities: Mesh2DGenerator

    private static float nanOp(int op) {
        return Float.intBitsToFloat(0x7FC00000 | op);
    }

    @Test
    public void flattenPathAndSamplePolylineMeasureArclengthAndTangent() {
        // Null path returns 0 points.
        float[] out = new float[64];
        assertEquals(0, Mesh2DGenerator.flattenPath(null, out));

        // Build a path with MOVE(0, 0) -> LINE(30, 0) -> LINE(30, 40) -> DONE (total length = 70).
        float[] pathData =
                new float[] {
                    nanOp(10), 0f, 0f, // MOVE (0, 0)
                    nanOp(11), 30f, 0f, // LINE (30, 0)
                    nanOp(11), 30f, 40f, // LINE (30, 40)
                    nanOp(16), // DONE
                };
        int count = Mesh2DGenerator.flattenPath(pathData, out);
        assertEquals(3, count);
        assertEquals(0f, out[0], 1e-5f);
        assertEquals(0f, out[1], 1e-5f);
        assertEquals(30f, out[2], 1e-5f);
        assertEquals(0f, out[3], 1e-5f);
        assertEquals(30f, out[4], 1e-5f);
        assertEquals(40f, out[5], 1e-5f);

        float[] sample = new float[4];
        // At fraction = 15/70 (halfway along the horizontal segment): pos = (15, 0), tangent = (1,
        // 0)
        Mesh2DGenerator.samplePolyline(out, count, 15f / 70f, sample);
        assertEquals(15f, sample[0], 1e-4f);
        assertEquals(0f, sample[1], 1e-4f);
        assertEquals(1f, sample[2], 1e-4f);
        assertEquals(0f, sample[3], 1e-4f);

        // At fraction = 50/70 (20 units up the vertical segment): pos = (30, 20), tangent = (0, 1)
        Mesh2DGenerator.samplePolyline(out, count, 50f / 70f, sample);
        assertEquals(30f, sample[0], 1e-4f);
        assertEquals(20f, sample[1], 1e-4f);
        assertEquals(0f, sample[2], 1e-4f);
        assertEquals(1f, sample[3], 1e-4f);

        // Clamping past the ends: fraction < 0 clamps to start, fraction > 1 clamps to end.
        Mesh2DGenerator.samplePolyline(out, count, -0.5f, sample);
        assertEquals(0f, sample[0], 1e-4f);
        assertEquals(0f, sample[1], 1e-4f);

        Mesh2DGenerator.samplePolyline(out, count, 1.5f, sample);
        assertEquals(30f, sample[0], 1e-4f);
        assertEquals(40f, sample[1], 1e-4f);

        // Empty and single-point polylines degrade cleanly without NaN.
        Mesh2DGenerator.samplePolyline(out, 0, 0.5f, sample);
        assertEquals(0f, sample[0], 0f);
        assertEquals(1f, sample[2], 0f);

        Mesh2DGenerator.samplePolyline(new float[] {7f, 9f}, 1, 0.5f, sample);
        assertEquals(7f, sample[0], 0f);
        assertEquals(9f, sample[1], 0f);
        assertEquals(1f, sample[2], 0f);
    }

    @Test
    public void flattenPathSubdividesQuadraticAndCubicCurves() {
        // Quadratic from (0,0) via (50, 100) to (100, 0), followed by Cubic to (200, 0).
        float[] pathData =
                new float[] {
                    nanOp(10), 0f, 0f, nanOp(12), 50f, 100f, 100f, 0f, nanOp(14), 125f, -50f, 175f,
                    -50f, 200f, 0f, nanOp(15), // CLOSE
                    nanOp(16), // DONE
                };
        float[] out = new float[64];
        int count = Mesh2DGenerator.flattenPath(pathData, out);
        // 1 MOVE + 8 QUADRATIC subdivisions + 8 CUBIC subdivisions = 17 points
        assertEquals(17, count);
        // Midpoint of quadratic (step 4 of 8, index 4) has x = 50, y = 50
        assertEquals(50f, out[4 * 2], 1e-4f);
        assertEquals(50f, out[4 * 2 + 1], 1e-4f);
        // Final point of cubic (index 16) is (200, 0)
        assertEquals(200f, out[16 * 2], 1e-4f);
        assertEquals(0f, out[16 * 2 + 1], 1e-4f);
    }

    @Test
    public void halfFloatHandlesSubnormalsInfinityAndNaN() {
        // Positive and negative infinity
        int posInf = Mesh2DGenerator.floatToHalf(Float.POSITIVE_INFINITY);
        assertEquals(Float.POSITIVE_INFINITY, Mesh2DGenerator.halfToFloat(posInf), 0f);
        int negInf = Mesh2DGenerator.floatToHalf(Float.NEGATIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, Mesh2DGenerator.halfToFloat(negInf), 0f);

        // Overflow of huge finite float to half infinity
        int overflow = Mesh2DGenerator.floatToHalf(100000f);
        assertEquals(Float.POSITIVE_INFINITY, Mesh2DGenerator.halfToFloat(overflow), 0f);

        // NaN round-trips as NaN
        int nanHalf = Mesh2DGenerator.floatToHalf(Float.NaN);
        assertTrue(Float.isNaN(Mesh2DGenerator.halfToFloat(nanHalf)));

        // Subnormal half-float (smallest positive half subnormal = 2^-24 ≈ 5.9604645e-8f)
        float subnormal = Math.scalb(1f, -20);
        int subHalf = Mesh2DGenerator.floatToHalf(subnormal);
        assertEquals(subnormal, Mesh2DGenerator.halfToFloat(subHalf), 1e-7f);

        // Signed negative zero preserves sign bit
        int negZeroHalf = Mesh2DGenerator.floatToHalf(-0.0f);
        assertEquals(0x8000, negZeroHalf);
        assertEquals(
                Float.floatToRawIntBits(-0.0f),
                Float.floatToRawIntBits(Mesh2DGenerator.halfToFloat(negZeroHalf)));
    }

    @Test
    public void defaultPositionRingAndBuildMatrixIntermediateFallbacks() {
        float[] pos = new float[2];
        Mesh2DGenerator.defaultPosition(Mesh2DGenerator.LAYOUT_RING, 0f, 0f, pos);
        assertEquals(Mesh2DGenerator.DEFAULT_RING_INNER_RADIUS, pos[0], 1e-5f);
        assertEquals(0f, pos[1], 1e-5f);

        Mesh2DGenerator.defaultPosition(Mesh2DGenerator.LAYOUT_RING, 0f, 1f, pos);
        assertEquals(1f, pos[0], 1e-5f);
        assertEquals(0f, pos[1], 1e-5f);

        // 1. Collinear non-zero du=(4, 0) and dv=(2, 0): cross == 0, so FLAG_FULL degrades to
        // FLAG_SCALE with orthogonal axes of lengths 4 and 2.
        float[] out = new float[6];
        Mesh2DGenerator.buildMatrix(
                new float[] {4f, 0f, 2f, 0f, 5f, 6f}, Mesh2DGenerator.FLAG_FULL, out);
        assertEquals(4f, out[0], 1e-5f);
        assertEquals(0f, out[1], 1e-5f);
        assertEquals(0f, out[2], 1e-5f);
        assertEquals(2f, out[3], 1e-5f);
        assertEquals(5f, out[4], 1e-5f);
        assertEquals(6f, out[5], 1e-5f);

        // 2. dv collapsed to 0 while du=(0, 3) is valid: FLAG_SCALE degrades to FLAG_ROTATION
        // (unit orthogonal basis aligned with du=(0, 1)).
        Mesh2DGenerator.buildMatrix(
                new float[] {0f, 3f, 0f, 0f, 7f, 8f}, Mesh2DGenerator.FLAG_SCALE, out);
        assertEquals(0f, out[0], 1e-5f);
        assertEquals(1f, out[1], 1e-5f);
        assertEquals(-1f, out[2], 1e-5f);
        assertEquals(0f, out[3], 1e-5f);

        // 3. Left-handed patch (cross < 0, e.g. du=(2, 0), dv=(0, -3)): FLAG_ROTATION preserves
        // handedness (sign = -1).
        Mesh2DGenerator.buildMatrix(
                new float[] {2f, 0f, 0f, -3f, 0f, 0f}, Mesh2DGenerator.FLAG_ROTATION, out);
        assertEquals(1f, out[0], 1e-5f);
        assertEquals(0f, out[1], 1e-5f);
        assertEquals(0f, out[2], 1e-5f);
        assertEquals(-1f, out[3], 1e-5f);
    }

    @Test
    public void computeMatrixFromMeshSupportsFanAndWrappingSeamAndUnstructuredFallback() {
        // 1. LAYOUT_FAN with uCount = 4: vertex 0 = (50, 50), rim = (100,50), (50,100), (0,50),
        // (50,0)
        float[] fanVerts =
                new float[] {
                    50f, 50f, // apex (v = 0)
                    100f, 50f, // u = 0.0
                    50f, 100f, // u = 0.25
                    0f, 50f, // u = 0.5
                    50f, 0f, // u = 0.75
                };
        float[] out = new float[6];
        // At u = 0, v = 0.5: midpoint between apex (50,50) and rim (100,50) is (75, 50).
        assertTrue(
                Mesh2DGenerator.computeMatrixFromMesh(
                        Mesh2DGenerator.LAYOUT_FAN,
                        4,
                        2,
                        fanVerts,
                        0f,
                        0.5f,
                        Mesh2DGenerator.FLAG_ORIGIN,
                        out));
        assertEquals(75f, out[4], 1e-4f);
        assertEquals(50f, out[5], 1e-4f);

        // At u = 0.875 (halfway across the wrapping seam between u=0.75 (50,0) and u=0.0 (100,50))
        // and v = 1.0: rim midpoint is (75, 25).
        assertTrue(
                Mesh2DGenerator.computeMatrixFromMesh(
                        Mesh2DGenerator.LAYOUT_FAN,
                        4,
                        2,
                        fanVerts,
                        0.875f,
                        1f,
                        Mesh2DGenerator.FLAG_ORIGIN,
                        out));
        assertEquals(75f, out[4], 1e-4f);
        assertEquals(25f, out[5], 1e-4f);

        // 2. Unstructured TYPE_VALUES mesh (uCount = 0, vCount = 0): falls back to nearest vertex.
        float[] soupVerts = new float[] {10f, 20f, 30f, 40f, 50f, 60f};
        assertTrue(
                Mesh2DGenerator.computeMatrixFromMesh(
                        Mesh2DGenerator.LAYOUT_GRID,
                        0,
                        0,
                        soupVerts,
                        0.5f,
                        0.5f,
                        Mesh2DGenerator.FLAG_ORIGIN,
                        out));
        assertEquals(30f, out[4], 1e-4f);
        assertEquals(40f, out[5], 1e-4f);

        // 3. Empty vertex array returns false.
        assertEquals(
                false,
                Mesh2DGenerator.computeMatrixFromMesh(
                        Mesh2DGenerator.LAYOUT_GRID,
                        4,
                        4,
                        new float[0],
                        0.5f,
                        0.5f,
                        Mesh2DGenerator.FLAG_FULL,
                        out));
    }

    // ------------------------------------------------------- spline path strip

    /** A straight path from (0, 0) to (100, 0), so the normal is (0, 1) everywhere. */
    private static float[] straightPath() {
        return new float[] {
            nanOp(10), 0f, 0f, // MOVE (0, 0)
            nanOp(11), 100f, 0f, // LINE (100, 0)
            nanOp(16), // DONE
        };
    }

    /** The cross width of the ribbon at column {@code i}, i.e. the gap between its two rows. */
    private static float crossWidthAt(AddMesh2D mesh, int uCount, int i) {
        float[] verts = mesh.getVerts();
        float dx = verts[(uCount + i) * 2] - verts[i * 2];
        float dy = verts[(uCount + i) * 2 + 1] - verts[i * 2 + 1];
        return (float) Math.hypot(dx, dy);
    }

    private static AddMesh2D splineStrip(int segments, float[] widths, float[] positions) {
        return new AddMesh2D(
                7,
                AddMesh2D.TYPE_PATH_SPLINE_STRIP,
                Mesh2DGenerator.LAYOUT_PATH_STRIP,
                segments + 1,
                2,
                0,
                21,
                null,
                null,
                null,
                null,
                null,
                widths,
                positions);
    }

    @Test
    public void splineStripInterpolatesWidthBetweenControlPoints() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        int segments = 8;
        int uCount = segments + 1;
        // Evenly spaced with no explicit positions: 0 at the start, 20 in the middle, 0 at the end.
        AddMesh2D mesh = splineStrip(segments, new float[] {0f, 20f, 0f}, null);
        mesh.updateVariables(context);
        mesh.expand(context);

        assertEquals(uCount * 2 * 2, mesh.getVerts().length);
        // The control points land exactly on their knots.
        assertEquals(0f, crossWidthAt(mesh, uCount, 0), 1e-4f);
        assertEquals(20f, crossWidthAt(mesh, uCount, segments / 2), 1e-4f);
        assertEquals(0f, crossWidthAt(mesh, uCount, segments), 1e-4f);
        // Monotone between them: no overshoot below zero on the way up, and it really does swell.
        for (int i = 0; i <= segments / 2; i++) {
            float width = crossWidthAt(mesh, uCount, i);
            assertTrue(width >= -1e-4f);
            assertTrue(width <= 20f + 1e-4f);
        }
        assertTrue(crossWidthAt(mesh, uCount, 2) > 0f);

        // The ribbon still follows the path: the spine advances along x and is centred on y = 0.
        float[] verts = mesh.getVerts();
        assertEquals(0f, verts[0], 1e-4f);
        assertEquals(100f, verts[segments * 2], 1e-4f);
        assertEquals(
                0f,
                verts[(segments / 2) * 2 + 1] + verts[(uCount + segments / 2) * 2 + 1],
                1e-4f);
    }

    @Test
    public void splineStripWithOneWidthIsAConstantRibbon() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        // A single control point must not reach the spline at all: MonotonicSpline cannot fit one
        // knot, so this is the case that would throw if it were not short circuited.
        int segments = 4;
        AddMesh2D mesh = splineStrip(segments, new float[] {12f}, null);
        mesh.updateVariables(context);
        mesh.expand(context);

        for (int i = 0; i <= segments; i++) {
            assertEquals(12f, crossWidthAt(mesh, segments + 1, i), 1e-4f);
        }
    }

    @Test
    public void splineStripHonoursExplicitWidthPositions() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        // The fat point is pushed to three quarters along rather than the middle.
        int segments = 8;
        AddMesh2D mesh =
                splineStrip(segments, new float[] {2f, 30f, 2f}, new float[] {0f, 0.75f, 1f});
        mesh.updateVariables(context);
        mesh.expand(context);

        int uCount = segments + 1;
        assertEquals(2f, crossWidthAt(mesh, uCount, 0), 1e-4f);
        assertEquals(30f, crossWidthAt(mesh, uCount, 6), 1e-4f);
        assertEquals(2f, crossWidthAt(mesh, uCount, segments), 1e-4f);
        // Off-centre, so the midpoint is nowhere near the peak.
        assertTrue(crossWidthAt(mesh, uCount, 4) < 30f);
    }

    @Test
    public void splineStripSurvivesTheWire() {
        AddMesh2D original = splineStrip(12, new float[] {1f, 9f, 3f}, new float[] {0f, 0.4f, 1f});

        WireBuffer buffer = new WireBuffer();
        original.write(buffer);
        buffer.setIndex(0);
        buffer.readByte(); // the opcode, consumed by the dispatcher in the real reader

        ArrayList<Operation> operations = new ArrayList<>();
        AddMesh2D.read(buffer, operations);

        assertEquals(1, operations.size());
        assertEquals(original.toString(), operations.get(0).toString());

        // toString only covers the header, so check the payload survived by generating from it.
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());
        AddMesh2D restored = (AddMesh2D) operations.get(0);
        restored.updateVariables(context);
        restored.expand(context);
        assertEquals(1f, crossWidthAt(restored, 13, 0), 1e-4f);
        assertEquals(3f, crossWidthAt(restored, 13, 12), 1e-4f);
    }

    @Test
    public void widthSplineRefusesToProduceNaNFromDegenerateKnots() {
        // Fewer than two control points is a constant, not a fit.
        assertNull(Mesh2DGenerator.widthSpline(null, null));
        assertNull(Mesh2DGenerator.widthSpline(new float[0], null));
        assertNull(Mesh2DGenerator.widthSpline(new float[] {4f}, null));
        assertEquals(1f, Mesh2DGenerator.widthAt(null, new float[0], 0.5f), 0f);
        assertEquals(4f, Mesh2DGenerator.widthAt(null, new float[] {4f}, 0.5f), 0f);

        // Positions are animatable, so at playback they can collide or go backwards. That divides
        // by zero inside the fit and would otherwise poison every vertex with NaN.
        float[] widths = new float[] {1f, 5f, 9f};
        float[] collapsed = new float[] {0.5f, 0.5f, 0.5f};
        for (float t = 0f; t <= 1f; t += 0.25f) {
            float width =
                    Mesh2DGenerator.widthAt(
                            Mesh2DGenerator.widthSpline(widths, collapsed), widths, t);
            assertTrue(!Float.isNaN(width));
        }
        float[] reversed = new float[] {0.9f, 0.5f, 0.1f};
        for (float t = 0f; t <= 1f; t += 0.25f) {
            float width =
                    Mesh2DGenerator.widthAt(
                            Mesh2DGenerator.widthSpline(widths, reversed), widths, t);
            assertTrue(!Float.isNaN(width));
        }

        // A mismatched position array is ignored rather than fatal: the widths spread evenly.
        float[] shortPositions = new float[] {0f, 1f};
        assertEquals(
                1f,
                Mesh2DGenerator.widthAt(
                        Mesh2DGenerator.widthSpline(widths, shortPositions), widths, 0f),
                1e-4f);

        // Extrapolation past the ends can dip below zero; a negative width folds the ribbon inside
        // out, so it is clamped away.
        float[] falling = new float[] {10f, 0f};
        assertEquals(
                0f,
                Mesh2DGenerator.widthAt(Mesh2DGenerator.widthSpline(falling, null), falling, 2f),
                0f);
    }

    @Test
    public void splineStripRejectsProfilesItCannotFit() {
        WireBuffer buffer = new WireBuffer();
        assertThrows(
                RuntimeException.class,
                () -> AddMesh2D.applyPathSplineStrip(buffer, 1, 8, 2, new float[0], null));
        assertThrows(
                RuntimeException.class,
                () -> AddMesh2D.applyPathSplineStrip(buffer, 1, 0, 2, new float[] {4f}, null));
        // Positions, when given at all, must name every width.
        assertThrows(
                RuntimeException.class,
                () ->
                        AddMesh2D.applyPathSplineStrip(
                                buffer, 1, 8, 2, new float[] {1f, 2f, 3f}, new float[] {0f, 1f}));
        // Literal positions must be ordered; only variables get the benefit of the doubt.
        assertThrows(
                RuntimeException.class,
                () ->
                        AddMesh2D.applyPathSplineStrip(
                                buffer,
                                1,
                                8,
                                2,
                                new float[] {1f, 2f, 3f},
                                new float[] {0f, 0.8f, 0.4f}));
        assertThrows(
                RuntimeException.class,
                () ->
                        AddMesh2D.applyPathSplineStrip(
                                buffer,
                                1,
                                8,
                                2,
                                new float[Limits.MAX_MESH_2D_WIDTH_SAMPLES + 1],
                                null));
    }

    // ------------------------------------------------------- round capped spline strip

    /** Build a round capped strip the way a document does, so the header derivation is covered. */
    private static AddMesh2D roundStrip(int segments, float[] widths, float[] positions) {
        WireBuffer buffer = new WireBuffer();
        AddMesh2D.applySplineRoundStrip(buffer, 7, segments, 21, widths, positions);
        buffer.setIndex(0);
        buffer.readByte(); // the opcode, consumed by the dispatcher in the real reader
        ArrayList<Operation> operations = new ArrayList<>();
        AddMesh2D.read(buffer, operations);
        return (AddMesh2D) operations.get(0);
    }

    /** The two vertices of column {@code i}, as x0, y0, x1, y1. */
    private static float[] columnAt(AddMesh2D mesh, int uCount, int i) {
        float[] verts = mesh.getVerts();
        return new float[] {
            verts[i * 2], verts[i * 2 + 1], verts[(uCount + i) * 2], verts[(uCount + i) * 2 + 1]
        };
    }

    @Test
    public void roundCapSegmentCountIsClampedAtBothEnds() {
        // Too coarse and the cap reads as a chamfer, so there is a floor.
        assertEquals(3, Mesh2DGenerator.roundCapSegments(1));
        assertEquals(3, Mesh2DGenerator.roundCapSegments(8));
        assertEquals(3, Mesh2DGenerator.roundCapSegments(12));
        // Between the clamps it tracks the body, so a finely sampled strip gets a fine cap.
        assertEquals(4, Mesh2DGenerator.roundCapSegments(16));
        assertEquals(10, Mesh2DGenerator.roundCapSegments(40));
        // Past the ceiling the arc is already smooth and more vertices buy nothing.
        assertEquals(16, Mesh2DGenerator.roundCapSegments(64));
        assertEquals(16, Mesh2DGenerator.roundCapSegments(100000));
    }

    @Test
    public void roundStripAddsItsCapsOutsideTheSegmentBudget() {
        int segments = 8;
        int cap = Mesh2DGenerator.roundCapSegments(segments);
        AddMesh2D mesh = roundStrip(segments, new float[] {20f}, null);

        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());
        mesh.updateVariables(context);
        mesh.expand(context);

        // The caps are extra columns, not a slice taken out of the body.
        int uCount = segments + 1 + 2 * cap;
        assertEquals(uCount * 2 * 2, mesh.getVerts().length);
        // Topology is untouched: still a two row strip, still one quad per column.
        assertEquals((uCount - 1) * 6, mesh.getIndices().length);
    }

    @Test
    public void roundStripFollowsItsPathExactlyAsTheFlatOneDoes() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        int segments = 8;
        int cap = Mesh2DGenerator.roundCapSegments(segments);
        float[] widths = new float[] {6f, 24f, 10f};

        AddMesh2D flat = splineStrip(segments, widths, null);
        flat.updateVariables(context);
        flat.expand(context);

        AddMesh2D round = roundStrip(segments, widths, null);
        round.updateVariables(context);
        round.expand(context);

        // This is the whole point of spending extra columns on the caps rather than borrowing
        // them: the body of the rounded strip is the flat strip, vertex for vertex.
        int flatUCount = segments + 1;
        int roundUCount = segments + 1 + 2 * cap;
        for (int k = 0; k <= segments; k++) {
            float[] a = columnAt(flat, flatUCount, k);
            float[] b = columnAt(round, roundUCount, cap + k);
            for (int c = 0; c < 4; c++) {
                assertEquals(a[c], b[c], 1e-3f);
            }
        }
    }

    @Test
    public void roundStripCapsSweepTheHalfDisc() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        int segments = 8;
        int cap = Mesh2DGenerator.roundCapSegments(segments);
        int uCount = segments + 1 + 2 * cap;
        float radius = 10f;
        AddMesh2D mesh = roundStrip(segments, new float[] {radius * 2f}, null);
        mesh.updateVariables(context);
        mesh.expand(context);

        // The tips are degenerate, sitting one radius beyond each end of the path along it.
        float[] startTip = columnAt(mesh, uCount, 0);
        assertEquals(-radius, startTip[0], 1e-3f);
        assertEquals(0f, startTip[1], 1e-3f);
        assertEquals(startTip[0], startTip[2], 1e-3f);
        assertEquals(startTip[1], startTip[3], 1e-3f);

        float[] endTip = columnAt(mesh, uCount, uCount - 1);
        assertEquals(100f + radius, endTip[0], 1e-3f);
        assertEquals(0f, endTip[1], 1e-3f);
        assertEquals(endTip[0], endTip[2], 1e-3f);
        assertEquals(endTip[1], endTip[3], 1e-3f);

        // Every cap vertex lies on the circle about the endpoint, which is what makes the fan of
        // cross segments between them sweep the half disc rather than some cheaper polygon.
        for (int i = 0; i <= cap; i++) {
            float[] column = columnAt(mesh, uCount, i);
            assertEquals(radius, (float) Math.hypot(column[0], column[1]), 1e-3f);
            assertEquals(radius, (float) Math.hypot(column[2], column[3]), 1e-3f);
            // Behind the start, never in front of it: the cap must not eat into the body.
            assertTrue(column[0] <= 1e-3f);
        }
        for (int i = 0; i <= cap; i++) {
            float[] column = columnAt(mesh, uCount, uCount - 1 - i);
            assertEquals(radius, (float) Math.hypot(column[0] - 100f, column[1]), 1e-3f);
            assertEquals(radius, (float) Math.hypot(column[2] - 100f, column[3]), 1e-3f);
            assertTrue(column[0] >= 100f - 1e-3f);
        }
    }

    @Test
    public void roundStripTaperingToNothingEndsInAPoint() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        int segments = 8;
        int cap = Mesh2DGenerator.roundCapSegments(segments);
        int uCount = segments + 1 + 2 * cap;
        // The cap radius is half the width at that end, so a profile that reaches zero has no cap
        // to draw and must collapse onto the endpoint rather than bulge or invert.
        AddMesh2D mesh = roundStrip(segments, new float[] {0f, 20f, 0f}, null);
        mesh.updateVariables(context);
        mesh.expand(context);

        for (int i = 0; i <= cap; i++) {
            float[] start = columnAt(mesh, uCount, i);
            assertEquals(0f, (float) Math.hypot(start[0], start[1]), 1e-3f);
            assertEquals(0f, (float) Math.hypot(start[2], start[3]), 1e-3f);

            float[] end = columnAt(mesh, uCount, uCount - 1 - i);
            assertEquals(0f, (float) Math.hypot(end[0] - 100f, end[1]), 1e-3f);
            assertEquals(0f, (float) Math.hypot(end[2] - 100f, end[3]), 1e-3f);
        }
    }

    @Test
    public void roundStripSurvivesTheWire() {
        WireBuffer buffer = new WireBuffer();
        AddMesh2D.applySplineRoundStrip(buffer, 7, 12, 21, new float[] {1f, 9f, 3f}, null);
        buffer.setIndex(0);
        buffer.readByte();

        ArrayList<Operation> operations = new ArrayList<>();
        AddMesh2D.read(buffer, operations);
        assertEquals(1, operations.size());
        AddMesh2D restored = (AddMesh2D) operations.get(0);

        // Rewriting must reproduce the same bytes, which means write() recovered the original
        // segment count from a header that had the caps folded into it.
        WireBuffer rewritten = new WireBuffer();
        restored.write(rewritten);
        assertEquals(buffer.getSize(), rewritten.getSize());

        buffer.setIndex(0);
        rewritten.setIndex(0);
        for (int i = 0; i < rewritten.getSize(); i++) {
            assertEquals(buffer.readByte(), rewritten.readByte());
        }
    }

    @Test
    public void roundStripIgnoresACapCountThatWouldSwallowTheStrip() {
        HeadlessRemoteContext context = new HeadlessRemoteContext();
        context.loadPathData(21, 0, straightPath());

        // flags is read straight off the wire, and a corrupt document can put anything there. A
        // cap wider than the strip would leave no body at all and divide by zero in the remap.
        int uCount = 6;
        AddMesh2D mesh =
                new AddMesh2D(
                        7,
                        AddMesh2D.TYPE_SPLINE_ROUND_STRIP,
                        Mesh2DGenerator.LAYOUT_PATH_STRIP,
                        uCount,
                        2,
                        9999,
                        21,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new float[] {8f},
                        null);
        mesh.updateVariables(context);
        mesh.expand(context);

        for (float vert : mesh.getVerts()) {
            assertTrue(!Float.isNaN(vert) && !Float.isInfinite(vert));
        }
    }

    @Test
    public void roundStripRejectsProfilesItCannotFit() {
        WireBuffer buffer = new WireBuffer();
        assertThrows(
                RuntimeException.class,
                () -> AddMesh2D.applySplineRoundStrip(buffer, 1, 8, 2, new float[0], null));
        assertThrows(
                RuntimeException.class,
                () -> AddMesh2D.applySplineRoundStrip(buffer, 1, 0, 2, new float[] {4f}, null));
        assertThrows(
                RuntimeException.class,
                () ->
                        AddMesh2D.applySplineRoundStrip(
                                buffer, 1, 8, 2, new float[] {1f, 2f, 3f}, new float[] {0f, 1f}));
    }
}
