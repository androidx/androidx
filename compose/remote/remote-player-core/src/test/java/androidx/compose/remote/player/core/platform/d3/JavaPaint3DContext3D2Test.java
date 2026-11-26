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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.Paint3DContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tests for the model matrix stack, the {@link Paint3DContext#MODE_TRANSPARENT},
 * {@link Paint3DContext#MODE_DOUBLE_SIDED} and {@link Paint3DContext#MODE_FRONT_CW} flags, the
 * lighting model shared by every engine, and the infinite plane.
 */
@RunWith(JUnit4.class)
public class JavaPaint3DContext3D2Test {
    private static final int SIZE = 200;
    private static final int MESH = 1;
    private static final int PLANE = 2;
    /** Index of pixel (100, 100), the middle of the viewport. */
    private static final int CENTER = SIZE / 2 * SIZE + SIZE / 2;
    private static final float FOV_60 = (float) Math.toRadians(60);
    private static final int GRAY = 0xFF808080;
    /** {@link #GRAY} lit by the default ambient term only (0.2). */
    private static final int GRAY_AMBIENT = 0xFF191919;
    private static final int TRANSLUCENT_RED = 0x80FF0000;

    private static final int FLAT = Paint3DContext.MODE_SOFTWARE_FLAT;
    private static final int SMOOTH = Paint3DContext.MODE_SOFTWARE_SMOOTH;
    private static final int WIREFRAME = Paint3DContext.MODE_SOFTWARE_WIREFRAME;
    private static final int TRANSPARENT = Paint3DContext.MODE_TRANSPARENT;
    private static final int DOUBLE_SIDED = Paint3DContext.MODE_DOUBLE_SIDED;
    private static final int FRONT_CW = Paint3DContext.MODE_FRONT_CW;
    private static final int CANVAS_FLAT = Paint3DContext.MODE_CANVAS_FLAT;
    private static final int CANVAS_SMOOTH = Paint3DContext.MODE_CANVAS_SMOOTH;

    private static final int PUSH = Paint3DContext.M3_PUSH;
    private static final int POP = Paint3DContext.M3_POP;
    private static final int TRANSLATE = Paint3DContext.M3_TRANSLATE;

    /** A 2 x 2 square at z = -5 on the view axis; it spans x, y in about [65, 135]. */
    private static final float[] SQUARE = {
            -1f, -1f, -5f, 1f, -1f, -5f, 1f, 1f, -5f, -1f, 1f, -5f};
    /** The square's triangles, counter-clockwise seen from the camera: front-facing. */
    private static final int[] FRONT = {0, 1, 2, 0, 2, 3};
    /** The same triangles wound the other way: the camera sees their back. */
    private static final int[] BACK = {0, 2, 1, 0, 3, 2};
    /** Vertex normals pointing at the camera. */
    private static final float[] TOWARD_CAMERA = {
            0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f};
    /** Vertex normals pointing away from the camera. */
    private static final float[] AWAY_FROM_CAMERA = {
            0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f};
    /**
     * A 2 x 2 square in the plane y = 0 around the origin: with {@link #FRONT} its normal is +y
     * (a floor), with {@link #BACK} it is -y (a ceiling).
     */
    private static final float[] HORIZONTAL = {
            -1f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, -1f, -1f, 0f, -1f};
    /** An infinite plane through (0, -1, 0) with normal +y (a floor), without a grid. */
    private static final float[] FLOOR = {0f, -1f, 0f, 0f, 1f, 1f, 0.5f, 0.5f, 0.5f};
    /** The same floor with a white grid of spacing 1. */
    private static final float[] FLOOR_WITH_GRID = {0f, -1f, 0f, 1f, 1f, 1f, 1f, 1f, 1f};

    /**
     * Engine with a 60 degree camera at the origin looking down -z, a gray base color, no
     * light ever set (so the default headlight), and the front-facing square as {@link #MESH}.
     */
    private static JavaPaint3DContext newEngine() {
        JavaPaint3DContext ctx = new JavaPaint3DContext();
        ctx.setSize(SIZE, SIZE);
        ctx.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {FOV_60, 1f, 0.1f, 100f},
                new float[] {0f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f});
        ctx.setBaseColorArgb(GRAY);
        ctx.defineMesh3D(MESH, FRONT, SQUARE, null);
        return ctx;
    }

    /** Draws {@link #MESH} scaled by {@code scale} in x and y, then moved by (0, 0, dz). */
    private static void drawSquare(JavaPaint3DContext ctx, float scale, float dz, int mode) {
        ctx.matrix3DOp(Paint3DContext.M3_IDENTITY, new float[0]);
        ctx.matrix3DOp(TRANSLATE, new float[] {0f, 0f, dz});
        ctx.matrix3DOp(Paint3DContext.M3_SCALE, new float[] {scale, scale, 1f});
        ctx.drawMesh3D(MESH, mode);
    }

    /** A copy of the color buffer after {@code ops} and a flat draw of the square. */
    private static int[] render(Consumer<JavaPaint3DContext> ops) {
        JavaPaint3DContext ctx = newEngine();
        ops.accept(ctx);
        ctx.drawMesh3D(MESH, FLAT);
        return pixels(ctx).clone();
    }

    private static void op(JavaPaint3DContext ctx, int sub, float... args) {
        ctx.matrix3DOp(sub, args);
    }

    private static void repeat(JavaPaint3DContext ctx, int sub, int times) {
        for (int i = 0; i < times; i++) {
            ctx.matrix3DOp(sub, new float[0]);
        }
    }

    // ---- Model matrix stack ----

    @Test
    public void popRestoresTheModelMatrixOfTheMatchingPush() {
        int[] plain = render(ctx -> { });
        int[] moved = render(ctx -> op(ctx, TRANSLATE, 1f, 0f, 0f));
        assertFalse(Arrays.equals(plain, moved));

        assertArrayEquals(plain, render(ctx -> {
            op(ctx, PUSH);
            op(ctx, TRANSLATE, 1f, 0f, 0f);
            op(ctx, POP);
        }));
        // Nested: each pop goes back one level.
        Consumer<JavaPaint3DContext> nested = ctx -> {
            op(ctx, PUSH);
            op(ctx, TRANSLATE, 1f, 0f, 0f);
            op(ctx, PUSH);
            op(ctx, Paint3DContext.M3_SCALE, 2f, 2f, 1f);
            op(ctx, POP);
        };
        assertArrayEquals(moved, render(nested));
        assertArrayEquals(plain, render(nested.andThen(ctx -> op(ctx, POP))));
    }

    @Test
    public void stackHoldsThirtyTwoMatricesAndIgnoresPushesBeyondAndExtraPops() {
        int depth = Paint3DContext.M3_STACK_DEPTH;
        float step = 1f / 16;
        // Saves the translations by 0, 1/16, ..., 31/16 and leaves the one by 2.
        Consumer<JavaPaint3DContext> fill = ctx -> {
            for (int i = 0; i < depth; i++) {
                op(ctx, PUSH);
                op(ctx, TRANSLATE, step, 0f, 0f);
            }
        };
        // A push past the depth saves nothing, so its pop restores nothing.
        Consumer<JavaPaint3DContext> overflow = fill.andThen(ctx -> {
            op(ctx, PUSH);
            op(ctx, TRANSLATE, -1f, 0f, 0f);
            op(ctx, POP);
        });
        assertArrayEquals(render(ctx -> op(ctx, TRANSLATE, 1f, 0f, 0f)), render(overflow));
        // The saved matrices come back in order: 31 pops later, the second push's.
        assertArrayEquals(render(ctx -> op(ctx, TRANSLATE, step, 0f, 0f)),
                render(overflow.andThen(ctx -> repeat(ctx, POP, depth - 1))));
        // Then the first one's; pops with nothing saved do nothing.
        assertArrayEquals(render(ctx -> { }),
                render(overflow.andThen(ctx -> repeat(ctx, POP, depth + 3))));
        assertArrayEquals(render(ctx -> op(ctx, TRANSLATE, 1f, 0f, 0f)), render(ctx -> {
            op(ctx, POP);
            op(ctx, TRANSLATE, 1f, 0f, 0f);
            op(ctx, POP);
        }));
    }

    @Test
    public void clearDepthAndEndFrameEmptyTheStack() {
        int[] plain = render(ctx -> { });
        int[] moved = render(ctx -> op(ctx, TRANSLATE, 1f, 0f, 0f));
        List<Consumer<JavaPaint3DContext>> ends = Arrays.asList(
                JavaPaint3DContext::clearDepth3D, JavaPaint3DContext::endFrame3D);
        for (Consumer<JavaPaint3DContext> end : ends) {
            assertArrayEquals(moved, render(ctx -> {
                op(ctx, PUSH);
                op(ctx, TRANSLATE, 1f, 0f, 0f);
                end.accept(ctx);
                op(ctx, POP); // nothing left to restore
            }));
            // Pushes past the depth are forgotten too: a new push is saved again.
            assertArrayEquals(plain, render(ctx -> {
                repeat(ctx, PUSH, Paint3DContext.M3_STACK_DEPTH + 1);
                end.accept(ctx);
                op(ctx, PUSH);
                op(ctx, TRANSLATE, 1f, 0f, 0f);
                op(ctx, POP);
            }));
        }
    }

    @Test
    public void cameraIsNotSavedByPush() {
        float[] projection = {FOV_60, 1f, 0.1f, 100f};
        float[] view = {1f, 0f, 0f, 1f, 0f, -1f, 0f, 1f, 0f}; // moved 1 to the right
        int perspective = Paint3DContext.PROJECTION_PERSPECTIVE;
        int[] expected = render(ctx -> ctx.setCamera3D(perspective, projection, view));
        assertFalse(Arrays.equals(render(ctx -> { }), expected));
        assertArrayEquals(expected, render(ctx -> {
            op(ctx, PUSH);
            ctx.setCamera3D(perspective, projection, view);
            op(ctx, POP);
        }));
    }

    // ---- MODE_TRANSPARENT ----

    @Test
    public void blendSrcOverRoundsStraightAlpha() {
        assertEquals(0xFF80007F, Rasterizer.blendSrcOver(0x80FF0000, 0xFF0000FF));
        assertEquals(0xC0AA0055, Rasterizer.blendSrcOver(0x80FF0000, 0x800000FF));
        // Exact cases: opaque or empty source, empty destination.
        assertEquals(0xFF123456, Rasterizer.blendSrcOver(0xFF123456, 0x80FFFFFF));
        assertEquals(0x80FFFFFF, Rasterizer.blendSrcOver(0x00123456, 0x80FFFFFF));
        assertEquals(0x80123456, Rasterizer.blendSrcOver(0x80123456, 0x00FFFFFF));
    }

    @Test
    public void transparentDrawBlendsOverTheSceneAndWritesNoDepth() {
        // An opaque gray square three times as large behind (z = -6), alone...
        JavaPaint3DContext behind = newEngine();
        drawSquare(behind, 3f, -1f, FLAT);
        int[] back = pixels(behind).clone();
        // ...and the translucent square in front (z = -5), alone: blending over nothing
        // leaves its own color.
        JavaPaint3DContext front = newEngine();
        front.setBaseColorArgb(TRANSLUCENT_RED);
        drawSquare(front, 1f, 0f, FLAT | TRANSPARENT);
        int[] src = pixels(front).clone();
        assertEquals(0x80, src[CENTER] >>> 24);

        JavaPaint3DContext both = newEngine();
        drawSquare(both, 3f, -1f, FLAT);
        both.setBaseColorArgb(TRANSLUCENT_RED);
        drawSquare(both, 1f, 0f, FLAT | TRANSPARENT);
        int[] out = pixels(both);
        for (int i = 0; i < SIZE * SIZE; i++) {
            assertEquals("pixel " + i, Rasterizer.blendSrcOver(src[i], back[i]), out[i]);
        }
        assertNotEquals(back[CENTER], out[CENTER]);
        assertPendingContainsDrawnPixels(both);

        // No depth written: the opaque square drawn after it, behind, still shows in full.
        JavaPaint3DContext later = newEngine();
        later.setBaseColorArgb(TRANSLUCENT_RED);
        drawSquare(later, 1f, 0f, FLAT | TRANSPARENT);
        later.setBaseColorArgb(GRAY);
        drawSquare(later, 3f, -1f, FLAT);
        assertArrayEquals(back, pixels(later));

        // Without the flag a translucent color replaces what is behind, as before.
        JavaPaint3DContext opaque = newEngine();
        drawSquare(opaque, 3f, -1f, FLAT);
        opaque.setBaseColorArgb(TRANSLUCENT_RED);
        drawSquare(opaque, 1f, 0f, FLAT);
        assertEquals(src[CENTER], pixels(opaque)[CENTER]);
    }

    @Test
    public void transparentDrawIsDepthTested() {
        JavaPaint3DContext ctx = newEngine();
        drawSquare(ctx, 3f, 0f, FLAT);
        int[] opaque = pixels(ctx).clone();
        ctx.setBaseColorArgb(TRANSLUCENT_RED);
        drawSquare(ctx, 1f, -1f, FLAT | TRANSPARENT); // behind the opaque square
        assertArrayEquals(opaque, pixels(ctx));
        // A fully transparent color draws nothing at all.
        JavaPaint3DContext clear = newEngine();
        clear.setBaseColorArgb(0x00FF0000);
        drawSquare(clear, 1f, 0f, FLAT | TRANSPARENT);
        drawSquare(clear, 1f, 0f, WIREFRAME | TRANSPARENT);
        assertFalse(clear.hasPendingPixels());
        assertEquals(0, countDrawn(pixels(clear)));
    }

    @Test
    public void transparentTextureBlendsWithTheTexelAlpha() {
        // 2 x 2 texels: half transparent, transparent (top row); opaque, quarter (bottom).
        int[] texture = {0x80FFFFFF, 0x00FFFFFF, 0xFFFFFFFF, 0x40FF00FF};
        float[] uv = {0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f};
        JavaPaint3DContext behind = newEngine();
        drawSquare(behind, 3f, -1f, FLAT);
        int[] back = pixels(behind).clone();

        JavaPaint3DContext front = newEngine();
        front.setTextureData(texture, 2, 2);
        front.defineMesh3D(MESH, FRONT, SQUARE, null, uv);
        drawSquare(front, 1f, 0f, FLAT | TRANSPARENT);
        int[] src = pixels(front).clone();
        // Lit alpha (255) times texel alpha, quadrant by quadrant; an empty texel is skipped.
        assertEquals(0x80, src[83 * SIZE + 83] >>> 24);
        assertEquals(0, src[83 * SIZE + 117]);
        assertEquals(0xFF, src[117 * SIZE + 83] >>> 24);
        assertEquals(0x40, src[117 * SIZE + 117] >>> 24);

        JavaPaint3DContext both = newEngine();
        drawSquare(both, 3f, -1f, FLAT);
        both.setTextureData(texture, 2, 2);
        both.defineMesh3D(MESH, FRONT, SQUARE, null, uv);
        drawSquare(both, 1f, 0f, FLAT | TRANSPARENT);
        int[] out = pixels(both);
        for (int i = 0; i < SIZE * SIZE; i++) {
            assertEquals("pixel " + i, Rasterizer.blendSrcOver(src[i], back[i]), out[i]);
        }
    }

    @Test
    public void transparentWireframeBlendsItsEdgesAndWritesNoDepth() {
        JavaPaint3DContext ctx = newEngine();
        drawSquare(ctx, 3f, -1f, FLAT);
        int[] gray = pixels(ctx).clone();
        ctx.setBaseColorArgb(TRANSLUCENT_RED);
        drawSquare(ctx, 1f, 0f, WIREFRAME | TRANSPARENT);
        int[] out = pixels(ctx).clone();
        int changed = 0;
        for (int i = 0; i < SIZE * SIZE; i++) {
            if (out[i] != gray[i]) {
                // Red blended over opaque gray: opaque and reddish.
                changed++;
                assertEquals(0xFF, out[i] >>> 24);
                assertTrue(((out[i] >> 16) & 0xFF) > ((out[i] >> 8) & 0xFF));
            }
        }
        assertTrue(changed > 0);
        // Only the edges are drawn: not the inside, here away from the diagonal.
        int inside = 90 * SIZE + 120;
        assertEquals(gray[inside], out[inside]);
        assertPendingContainsDrawnPixels(ctx);

        // No depth written: an opaque square between the two covers all of the edges.
        ctx.setBaseColorArgb(0xFF0000FF);
        drawSquare(ctx, 1.5f, -0.5f, FLAT);
        JavaPaint3DContext expected = newEngine();
        drawSquare(expected, 3f, -1f, FLAT);
        expected.setBaseColorArgb(0xFF0000FF);
        drawSquare(expected, 1.5f, -0.5f, FLAT);
        assertArrayEquals(pixels(expected), pixels(ctx));
    }

    // ---- MODE_DOUBLE_SIDED ----

    @Test
    public void backFacesAreDrawnOnlyWhenDoubleSidedAndLitLikeFrontFaces() {
        int[] front = render(ctx -> { });
        JavaPaint3DContext ctx = newEngine();
        ctx.defineMesh3D(MESH, BACK, SQUARE, null);
        ctx.drawMesh3D(MESH, FLAT);
        assertEquals(0, countDrawn(pixels(ctx)));
        // Lit with the reversed normal, which faces the camera: just like the front face.
        ctx.drawMesh3D(MESH, FLAT | DOUBLE_SIDED);
        assertArrayEquals(front, pixels(ctx));
        assertColorNear(GRAY, front[CENTER]);
    }

    @Test
    public void backFacesReverseTheirVertexNormals() {
        // Normals toward the camera on a back face point away from the viewer once reversed,
        // and so away from the headlight: only the ambient term is left.
        JavaPaint3DContext back = newEngine();
        back.defineMesh3D(MESH, BACK, SQUARE, TOWARD_CAMERA);
        back.drawMesh3D(MESH, SMOOTH | DOUBLE_SIDED);
        assertColorNear(GRAY_AMBIENT, pixels(back)[CENTER]);
        // The same normals on the front face are lit fully.
        JavaPaint3DContext front = newEngine();
        front.defineMesh3D(MESH, FRONT, SQUARE, TOWARD_CAMERA);
        front.drawMesh3D(MESH, SMOOTH);
        assertColorNear(GRAY, pixels(front)[CENTER]);
    }

    @Test
    public void canvasVerticesIncludeBackFacesOnlyWhenDoubleSided() {
        JavaPaint3DContext ctx = newEngine();
        ctx.defineMesh3D(MESH, BACK, SQUARE, null);
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        assertEquals(0, ctx.buildCanvasVertices(MESH, mesh, CANVAS_FLAT));
        assertEquals(6, ctx.buildCanvasVertices(MESH, mesh, CANVAS_FLAT | DOUBLE_SIDED));
        for (int i = 0; i < 6; i++) {
            assertColorNear(GRAY, mesh.mColors[i]);
        }
    }

    @Test
    public void clippedClockwiseFacesAreDrawnWhenDoubleSidedOrFrontCw() {
        // A ceiling at y = 1 facing up, from z = 5 (behind the eye) to z = -50: the camera
        // below sees its back (its triangles are clockwise on screen), and the near plane
        // clips it.
        int n = 7;
        float[] verts = new float[(n + 1) * (n + 1) * 3];
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                int v = (j * (n + 1) + i) * 3;
                verts[v] = -50f + 100f * i / n;
                verts[v + 1] = 1f;
                verts[v + 2] = 5f - 55f * j / n;
            }
        }
        int[] indices = new int[n * n * 6];
        int k = 0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int a = j * (n + 1) + i;
                int c = a + n + 1;
                indices[k++] = a;
                indices[k++] = a + 1;
                indices[k++] = c;
                indices[k++] = a + 1;
                indices[k++] = c + 1;
                indices[k++] = c;
            }
        }
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        int[] first = null;
        // Drawn as back faces, or as front faces: either way the flat normal faces the viewer.
        for (int flags : new int[] {DOUBLE_SIDED, FRONT_CW}) {
            JavaPaint3DContext ctx = newEngine();
            ctx.defineMesh3D(MESH, indices, verts, null);
            ctx.drawMesh3D(MESH, FLAT);
            assertEquals(0, countDrawn(pixels(ctx)));
            assertEquals(0, ctx.buildCanvasVertices(MESH, mesh, CANVAS_FLAT));
            ctx.drawMesh3D(MESH, FLAT | flags);
            int[] color = pixels(ctx);
            // The far edge (z = -50) projects just above the horizon, at row ~96.5.
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int c = color[y * SIZE + x];
                    if (y >= 99) {
                        assertEquals("leak at " + x + "," + y, 0, c);
                    } else if (y <= 93) {
                        assertNotEquals("hole at " + x + "," + y, 0, c);
                    }
                }
            }
            assertPendingContainsDrawnPixels(ctx);
            if (first == null) {
                first = color.clone();
            } else {
                assertArrayEquals(first, color);
            }
            assertTrue(ctx.buildCanvasVertices(MESH, mesh, CANVAS_FLAT | flags) > 0);
        }
    }

    // ---- MODE_FRONT_CW ----

    @Test
    public void clockwiseTrianglesAreFrontFacingWithFrontCw() {
        // BACK is clockwise as the camera sees it: culled by default, drawn with FRONT_CW,
        // where its flat normal faces the viewer, so it is lit just like the default front face.
        int[] front = render(ctx -> { });
        JavaPaint3DContext ctx = newEngine();
        ctx.defineMesh3D(MESH, BACK, SQUARE, null);
        ctx.drawMesh3D(MESH, FLAT);
        assertEquals(0, countDrawn(pixels(ctx)));
        ctx.drawMesh3D(MESH, FLAT | FRONT_CW);
        assertArrayEquals(front, pixels(ctx));
        assertColorNear(GRAY, front[CENTER]);
        // The counter-clockwise square is now the back: culled.
        JavaPaint3DContext ccw = newEngine();
        ccw.drawMesh3D(MESH, FLAT | FRONT_CW);
        assertEquals(0, countDrawn(pixels(ccw)));
    }

    @Test
    public void frontCwLightsClockwiseTrianglesByTheirGivenNormals() {
        // Smooth shading takes the normals as given: toward the camera they get all of the
        // headlight, away from it only the ambient term; the canvas engines agree.
        JavaPaint3DContext toward = newEngine();
        toward.defineMesh3D(MESH, BACK, SQUARE, TOWARD_CAMERA);
        toward.drawMesh3D(MESH, SMOOTH);
        assertEquals(0, countDrawn(pixels(toward)));
        assertColorNear(GRAY, drawSmoothAndCompareCanvas(toward, FRONT_CW));
        JavaPaint3DContext away = newEngine();
        away.defineMesh3D(MESH, BACK, SQUARE, AWAY_FROM_CAMERA);
        assertColorNear(GRAY_AMBIENT, drawSmoothAndCompareCanvas(away, FRONT_CW));
    }

    @Test
    public void frontCwDoubleSidedReversesTheNormalsOfCounterClockwiseFaces() {
        // With FRONT_CW the counter-clockwise square is the back face: DOUBLE_SIDED draws it
        // with its vertex normals reversed (toward the camera becomes away from it: ambient
        // only), and with its flat normal facing the viewer (fully lit), in every engine.
        JavaPaint3DContext smooth = newEngine();
        smooth.defineMesh3D(MESH, FRONT, SQUARE, TOWARD_CAMERA);
        smooth.drawMesh3D(MESH, SMOOTH | FRONT_CW);
        assertEquals(0, countDrawn(pixels(smooth)));
        assertColorNear(GRAY_AMBIENT, drawSmoothAndCompareCanvas(smooth, FRONT_CW | DOUBLE_SIDED));
        assertColorNear(GRAY, drawAndCompareCanvas(newEngine(), FRONT_CW | DOUBLE_SIDED));
        // The clockwise square is the front face: its normals are used as given.
        JavaPaint3DContext cw = newEngine();
        cw.defineMesh3D(MESH, BACK, SQUARE, TOWARD_CAMERA);
        assertColorNear(GRAY, drawSmoothAndCompareCanvas(cw, FRONT_CW | DOUBLE_SIDED));
    }

    @Test
    public void frontCwFlatNormalsFaceTheViewer() {
        // HORIZONTAL wound by BACK (normal -y by its winding) is clockwise seen from above.
        // With FRONT_CW it is front-facing from there, and its flat normal faces the viewer
        // (+y): a light from above lights it fully, one from below not at all.
        JavaPaint3DContext above = horizontalSquare(5f, BACK);
        above.drawMesh3D(MESH, FLAT);
        assertEquals(0, countDrawn(pixels(above)));
        directionalLight(above, 0f, -1f, 0f);
        assertColorNear(0xFF999999, drawAndCompareCanvas(above, FRONT_CW));
        JavaPaint3DContext litFromBelow = horizontalSquare(5f, BACK);
        directionalLight(litFromBelow, 0f, 1f, 0f);
        assertColorNear(GRAY_AMBIENT, drawAndCompareCanvas(litFromBelow, FRONT_CW));
        // Seen from below, the floor (FRONT) is clockwise: front-facing, its normal now -y.
        JavaPaint3DContext below = horizontalSquare(-5f, FRONT);
        directionalLight(below, 0f, 1f, 0f);
        assertColorNear(0xFF999999, drawAndCompareCanvas(below, FRONT_CW));
    }

    @Test
    public void wireframeFollowsFrontCw() {
        JavaPaint3DContext cw = newEngine();
        cw.defineMesh3D(MESH, BACK, SQUARE, null);
        cw.drawMesh3D(MESH, WIREFRAME);
        assertEquals(0, countDrawn(pixels(cw)));
        cw.drawMesh3D(MESH, WIREFRAME | FRONT_CW);
        assertTrue(countDrawn(pixels(cw)) > 0);
        assertPendingContainsDrawnPixels(cw);
        JavaPaint3DContext ccw = newEngine();
        ccw.drawMesh3D(MESH, WIREFRAME | FRONT_CW);
        assertEquals(0, countDrawn(pixels(ccw)));
    }

    @Test
    public void wireframeEdgeReachingFarPastTheViewportIsDrawnWhereItCrossesIt() {
        // Scaled 500 times, the square spans about 35000 px: inside the guard band, so it isn't
        // clipped, but only about 280 px of its 49000 px diagonal cross the viewport, along
        // x + y = SIZE. Its other edges lie far outside.
        JavaPaint3DContext ctx = newEngine();
        drawSquare(ctx, 500f, 0f, WIREFRAME);
        int[] color = pixels(ctx);
        int drawn = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (color[y * SIZE + x] != 0) {
                    drawn++;
                    assertEquals(GRAY, color[y * SIZE + x]);
                    assertTrue("pixel " + x + "," + y + " is off the diagonal",
                            Math.abs(x + y - (SIZE - 1)) <= 1);
                }
            }
        }
        assertTrue("only " + drawn + " pixels drawn", drawn >= SIZE - 2);
        assertPendingContainsDrawnPixels(ctx);
    }

    @Test
    public void infinitePlaneIgnoresFrontCw() {
        JavaPaint3DContext ctx = newEngine();
        ctx.defineInfinitePlane3D(PLANE, 0, FLOOR_WITH_GRID);
        ctx.drawMesh3D(PLANE, FLAT);
        JavaPaint3DContext cw = newEngine();
        cw.defineInfinitePlane3D(PLANE, 0, FLOOR_WITH_GRID);
        cw.drawMesh3D(PLANE, FLAT | FRONT_CW);
        assertTrue(countDrawn(pixels(ctx)) > 0);
        assertArrayEquals(pixels(ctx), pixels(cw));
        // The canvas engines' polygon and grid are the same too.
        int max = InfinitePlane.MAX_POLYGON_VERTICES;
        float[] xy = new float[2 * max];
        float[] xyCw = new float[2 * max];
        int[] colors = new int[max];
        int[] colorsCw = new int[max];
        int n = ctx.buildCanvasPlane(PLANE, CANVAS_FLAT, xy, null, colors);
        assertTrue(n >= 3);
        assertEquals(n, ctx.buildCanvasPlane(PLANE, CANVAS_FLAT | FRONT_CW, xyCw, null, colorsCw));
        assertArrayEquals(xy, xyCw, 0f);
        assertArrayEquals(colors, colorsCw);
        assertEquals(ctx.buildCanvasPlaneGrid(PLANE, CANVAS_FLAT),
                ctx.buildCanvasPlaneGrid(PLANE, CANVAS_FLAT | FRONT_CW));
        // Seen from below, the floor still shows its back: culled.
        JavaPaint3DContext below = newEngine();
        below.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {FOV_60, 1f, 0.1f, 100f},
                new float[] {0f, -2f, 0f, 0f, -2f, -1f, 0f, 1f, 0f});
        below.defineInfinitePlane3D(PLANE, 0, FLOOR);
        below.drawMesh3D(PLANE, FLAT | FRONT_CW);
        assertEquals(0, countDrawn(pixels(below)));
    }

    // ---- Lighting ----

    @Test
    public void defaultHeadlightLightsCameraFacingSquaresFromAnyViewpoint() {
        // The headlight is fixed to the camera (L = +z in eye space): whatever the view, a
        // square facing the camera gets ambient 0.2 plus 0.8, in every engine.
        assertColorNear(GRAY, drawFlatAndCompareCanvas(newEngine()));
        assertColorNear(GRAY, drawFlatAndCompareCanvas(horizontalSquare(5f, FRONT)));
        assertColorNear(GRAY, drawFlatAndCompareCanvas(horizontalSquare(-5f, BACK)));
    }

    @Test
    public void aLightTravellingDownLightsFloorsAndNotCeilings() {
        // dir is the direction the light travels, so (0, -1, 0) is a light from above and
        // L = -dir = +y. A floor (normal +y) seen from above gets ambient 0.2 plus all of it.
        JavaPaint3DContext floor = horizontalSquare(5f, FRONT);
        directionalLight(floor, 0f, -1f, 0f);
        assertColorNear(0xFF999999, drawFlatAndCompareCanvas(floor));
        // A ceiling (normal -y) seen from below faces away from it: ambient only.
        JavaPaint3DContext ceiling = horizontalSquare(-5f, BACK);
        directionalLight(ceiling, 0f, -1f, 0f);
        assertColorNear(GRAY_AMBIENT, drawFlatAndCompareCanvas(ceiling));
        // A light travelling up does the opposite.
        floor = horizontalSquare(5f, FRONT);
        directionalLight(floor, 0f, 1f, 0f);
        assertColorNear(GRAY_AMBIENT, drawFlatAndCompareCanvas(floor));
        ceiling = horizontalSquare(-5f, BACK);
        directionalLight(ceiling, 0f, 1f, 0f);
        assertColorNear(0xFF999999, drawFlatAndCompareCanvas(ceiling));
    }

    @Test
    public void lightsOnlyLightTheSideTheyShineOn() {
        int directional = Paint3DContext.LIGHT_DIRECTIONAL;
        // Travelling +z, from behind the square: none of it, only the default ambient.
        assertColorNear(GRAY_AMBIENT, render(ctx -> ctx.setLights3D(new int[] {directional},
                new int[] {0xFFFFFFFF}, new float[] {0f, 0f, 1f, 1f}))[CENTER]);
        // Travelling -z, onto its front: ambient 0.2 plus 1.
        assertColorNear(0xFF999999, render(ctx -> ctx.setLights3D(new int[] {directional},
                new int[] {0xFFFFFFFF}, new float[] {0f, 0f, -1f, 1f}))[CENTER]);
        // The same for the highlight of a glossy material.
        assertColorNear(GRAY_AMBIENT, render(ctx -> {
            ctx.setMaterial3D(1f, 4f);
            ctx.setLights3D(new int[] {directional}, new int[] {0xFFFFFFFF},
                    new float[] {0f, 0f, 1f, 1f});
        })[CENTER]);
    }

    @Test
    public void ambientLightsAreSummedAndReplaceTheDefault() {
        int ambient = Paint3DContext.LIGHT_AMBIENT;
        assertColorNear(0xFF402000, render(ctx -> ctx.setLights3D(
                new int[] {ambient, ambient}, new int[] {0xFFFF0000, 0xFF00FF00},
                new float[] {0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0.25f}))[CENTER]);
        assertColorNear(GRAY, render(ctx -> ctx.setLights3D(
                new int[] {ambient, Paint3DContext.LIGHT_DIRECTIONAL},
                new int[] {0xFFFFFFFF, 0xFFFFFFFF},
                new float[] {0f, 0f, 0f, 0.5f, 0f, 0f, -1f, 0.5f}))[CENTER]);
        // An empty set is no light at all: the default ambient only.
        assertColorNear(GRAY_AMBIENT, render(ctx ->
                ctx.setLights3D(new int[0], new int[0], new float[0]))[CENTER]);
    }

    @Test
    public void specularHighlightOnlyWithPositiveStrength() {
        assertColorNear(GRAY, render(ctx -> ctx.setMaterial3D(0f, 8f))[CENTER]);
        int glossy = render(ctx -> ctx.setMaterial3D(0.5f, 1f))[CENTER];
        assertTrue(Integer.toHexString(glossy), ((glossy >> 16) & 0xFF) > 0xC0);
    }

    @Test
    public void specularHighlightComesFromTheSideTheLightStrikes() {
        // A glossy floor seen from straight above, lit from above (L = -dir = +y, so the half
        // vector is about +y too): a highlight of about 0.25 * 255 on top of the diffuse 0x99,
        // in the software pixels and the canvas vertex colors alike. A glossy flat face is lit
        // at each vertex, and the software pixels interpolate those colors.
        JavaPaint3DContext lit = horizontalSquare(5f, FRONT);
        lit.setMaterial3D(0.25f, 8f);
        directionalLight(lit, 0f, -1f, 0f);
        lit.drawMesh3D(MESH, FLAT);
        int red = (pixels(lit)[CENTER] >> 16) & 0xFF;
        assertTrue(Integer.toHexString(red), red >= 0xD0 && red <= 0xDA);
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        assertEquals(6, lit.buildCanvasVertices(MESH, mesh, CANVAS_FLAT));
        int min = 0xFF;
        int max = 0;
        for (int i = 0; i < 6; i++) {
            int r = (mesh.mColors[i] >> 16) & 0xFF;
            assertTrue(Integer.toHexString(r), r >= 0xD0 && r <= 0xDA);
            min = Math.min(min, r);
            max = Math.max(max, r);
        }
        assertTrue(red >= min - 1 && red <= max);
        // Lit from below, the floor faces away from the light: no diffuse and no highlight.
        JavaPaint3DContext unlit = horizontalSquare(5f, FRONT);
        unlit.setMaterial3D(0.25f, 8f);
        directionalLight(unlit, 0f, 1f, 0f);
        assertColorNear(GRAY_AMBIENT, drawFlatAndCompareCanvas(unlit));
    }

    @Test
    public void canvasVerticesAreShadedLikeTheSoftwarePixels() {
        JavaPaint3DContext ctx = newEngine();
        ctx.setLights3D(
                new int[] {Paint3DContext.LIGHT_DIRECTIONAL, Paint3DContext.LIGHT_POINT,
                        Paint3DContext.LIGHT_AMBIENT},
                new int[] {0xFFFF8040, 0xFF4080FF, 0xFF202020},
                new float[] {0.3f, -0.5f, -1f, 0.7f, 2f, 2f, 0f, 0.6f, 0f, 0f, 0f, 1f});
        ctx.drawMesh3D(MESH, FLAT);
        int[] color = pixels(ctx);
        // Triangle 0 is the lower right half of the square, triangle 1 the upper left one.
        int lowerRight = color[120 * SIZE + 125];
        int upperLeft = color[80 * SIZE + 75];
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        assertEquals(6, ctx.buildCanvasVertices(MESH, mesh, CANVAS_FLAT));
        int[] c = mesh.mColors;
        assertEquals(c[0], c[1]);
        assertEquals(c[0], c[2]);
        assertEquals(c[3], c[4]);
        assertEquals(c[3], c[5]);
        assertTrue((c[0] == lowerRight && c[3] == upperLeft)
                || (c[0] == upperLeft && c[3] == lowerRight));
    }

    @Test
    public void shortOrUnknownArgumentsAreIgnored() {
        int[] lit = render(ctx -> { });
        assertArrayEquals(lit, render(ctx -> {
            op(ctx, TRANSLATE, 1f);
            op(ctx, Paint3DContext.M3_SCALE);
            op(ctx, Paint3DContext.M3_ROTATE_AXIS, 1f, 0f, 0f);
            op(ctx, Paint3DContext.M3_MULTIPLY, new float[15]);
            op(ctx, 42);
            ctx.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE, new float[3], new float[9]);
            ctx.setCamera3D(Paint3DContext.PROJECTION_ORTHO, new float[5], new float[9]);
        }));
        // Lights: those without colors or 4 params, and kinds this engine doesn't know, are
        // dropped; extra trailing values are ignored.
        int directional = Paint3DContext.LIGHT_DIRECTIONAL;
        assertColorNear(0xFF999999, render(ctx -> ctx.setLights3D(
                new int[] {directional, directional, 99}, new int[] {0xFFFFFFFF},
                new float[] {0f, 0f, -1f, 1f, 0f, 0f, -1f}))[CENTER]);
        assertColorNear(GRAY_AMBIENT, render(ctx -> ctx.setLights3D(
                new int[] {99}, new int[] {0xFFFFFFFF}, new float[] {0f, 0f, -1f, 1f, 7f}))
                [CENTER]);
    }

    // ---- Infinite plane ----

    @Test
    public void infinitePlaneIsDrawnToTheHorizonAndSharesTheDepthBuffer() {
        JavaPaint3DContext ctx = newEngine();
        ctx.defineInfinitePlane3D(PLANE, 0, FLOOR);
        ctx.drawMesh3D(PLANE, FLAT);
        int[] color = pixels(ctx);
        // The floor y = -1 seen from the origin: everything below the horizon (row 100).
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int c = color[y * SIZE + x];
                if (y < 99) {
                    assertEquals("above the horizon at " + x + "," + y, 0, c);
                } else if (y >= 104) {
                    assertNotEquals("hole at " + x + "," + y, 0, c);
                }
            }
        }
        assertPendingContainsDrawnPixels(ctx);
        int floor = color[130 * SIZE + 100];

        // The square moved below the floor is hidden by it...
        int[] before = color.clone();
        op(ctx, TRANSLATE, 0f, -3f, 0f);
        ctx.drawMesh3D(MESH, FLAT);
        assertArrayEquals(before, pixels(ctx));
        // ...and in its place, above the floor, covers it.
        op(ctx, Paint3DContext.M3_IDENTITY);
        ctx.drawMesh3D(MESH, FLAT);
        assertNotEquals(floor, pixels(ctx)[130 * SIZE + 100]);
        assertColorNear(GRAY, pixels(ctx)[130 * SIZE + 100]);
    }

    @Test
    public void infinitePlaneTakesTheModeFlags() {
        // Transparent: blends over the square below it.
        JavaPaint3DContext alone = newEngine();
        alone.setBaseColorArgb(0x80FFFFFF);
        alone.defineInfinitePlane3D(PLANE, 0, FLOOR);
        alone.drawMesh3D(PLANE, FLAT | TRANSPARENT);
        int[] src = pixels(alone).clone();
        JavaPaint3DContext under = newEngine();
        op(under, TRANSLATE, 0f, -3f, 0f);
        under.drawMesh3D(MESH, FLAT);
        int[] back = pixels(under).clone();
        op(under, Paint3DContext.M3_IDENTITY);
        under.setBaseColorArgb(0x80FFFFFF);
        under.defineInfinitePlane3D(PLANE, 0, FLOOR);
        under.drawMesh3D(PLANE, FLAT | TRANSPARENT);
        int[] out = pixels(under);
        int pixel = 185 * SIZE + 100; // the square, seen through the floor
        assertNotEquals(0, back[pixel]);
        assertEquals(Rasterizer.blendSrcOver(src[pixel], back[pixel]), out[pixel]);

        // Double-sided: from below, the floor shows its back.
        JavaPaint3DContext below = newEngine();
        below.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {FOV_60, 1f, 0.1f, 100f},
                new float[] {0f, -2f, 0f, 0f, -2f, -1f, 0f, 1f, 0f});
        below.defineInfinitePlane3D(PLANE, 0, FLOOR);
        below.drawMesh3D(PLANE, FLAT);
        assertEquals(0, countDrawn(pixels(below)));
        below.drawMesh3D(PLANE, FLAT | DOUBLE_SIDED);
        assertNotEquals(0, pixels(below)[40 * SIZE + 100]);
        assertEquals(0, pixels(below)[160 * SIZE + 100]);
        assertPendingContainsDrawnPixels(below);
    }

    @Test
    public void redefiningAPlaneReusesItAndAMeshReplacesIt() {
        JavaPaint3DContext ctx = newEngine();
        ctx.defineInfinitePlane3D(PLANE, 0, FLOOR);
        InfinitePlane plane = ctx.getInfinitePlane(PLANE);
        assertNotNull(plane);
        assertFalse(plane.hasGrid());
        ctx.defineInfinitePlane3D(PLANE, 0, FLOOR_WITH_GRID);
        assertSame(plane, ctx.getInfinitePlane(PLANE));
        assertTrue(plane.hasGrid());
        assertNull(ctx.getInfinitePlane(MESH));
        ctx.defineMesh3D(PLANE, FRONT, SQUARE, null);
        assertNull(ctx.getInfinitePlane(PLANE));
    }

    @Test
    public void canvasEnginesGetThePlaneAsOnePolygonAndItsGrid() {
        JavaPaint3DContext ctx = newEngine();
        ctx.defineInfinitePlane3D(PLANE, 0, FLOOR);
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        assertEquals(0, ctx.buildCanvasVertices(PLANE, mesh, CANVAS_FLAT));
        float[] xy = new float[2 * InfinitePlane.MAX_POLYGON_VERTICES];
        int[] colors = new int[InfinitePlane.MAX_POLYGON_VERTICES];
        int n = ctx.buildCanvasPlane(PLANE, CANVAS_FLAT, xy, null, colors);
        assertTrue("vertices: " + n, n >= 3 && n <= InfinitePlane.MAX_POLYGON_VERTICES);
        ctx.drawMesh3D(PLANE, FLAT);
        int software = pixels(ctx)[150 * SIZE + 100];
        for (int i = 0; i < n; i++) {
            assertTrue("y = " + xy[2 * i + 1], xy[2 * i + 1] >= SIZE / 2f - 1f);
            assertColorNear(software, colors[i]);
        }
        assertEquals(0, ctx.buildCanvasPlaneGrid(PLANE, CANVAS_FLAT));
        assertEquals(0, ctx.buildCanvasPlane(MESH, CANVAS_FLAT, xy, null, colors));
        assertEquals(0, ctx.buildCanvasPlaneGrid(MESH, CANVAS_FLAT));
        ctx.defineInfinitePlane3D(PLANE, 0, FLOOR_WITH_GRID);
        assertTrue(ctx.buildCanvasPlaneGrid(PLANE, CANVAS_FLAT) > 0);
    }

    // ---- Helpers ----

    /**
     * {@link #newEngine} looking at the origin from (0, eyeY, 0), with {@link #HORIZONTAL}
     * wound by {@code indices} as {@link #MESH}.
     */
    private static JavaPaint3DContext horizontalSquare(float eyeY, int[] indices) {
        JavaPaint3DContext ctx = newEngine();
        ctx.setCamera3D(Paint3DContext.PROJECTION_PERSPECTIVE,
                new float[] {FOV_60, 1f, 0.1f, 100f},
                new float[] {0f, eyeY, 0f, 0f, 0f, 0f, 0f, 0f, -1f});
        ctx.defineMesh3D(MESH, indices, HORIZONTAL, null);
        return ctx;
    }

    /** One white directional light of intensity 1 travelling along {@code (dx, dy, dz)}. */
    private static void directionalLight(JavaPaint3DContext ctx, float dx, float dy, float dz) {
        ctx.setLights3D(new int[] {Paint3DContext.LIGHT_DIRECTIONAL}, new int[] {0xFFFFFFFF},
                new float[] {dx, dy, dz, 1f});
    }

    /**
     * Draws {@link #MESH} flat and returns its center pixel, after asserting that the canvas
     * engines' vertex colors are that same color (for uniformly lit squares).
     */
    private static int drawFlatAndCompareCanvas(JavaPaint3DContext ctx) {
        return drawAndCompareCanvas(ctx, 0);
    }

    /** {@link #drawFlatAndCompareCanvas} with {@code flags} added to both modes. */
    private static int drawAndCompareCanvas(JavaPaint3DContext ctx, int flags) {
        ctx.drawMesh3D(MESH, FLAT | flags);
        int center = pixels(ctx)[CENTER];
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        assertEquals(6, ctx.buildCanvasVertices(MESH, mesh, CANVAS_FLAT | flags));
        for (int i = 0; i < 6; i++) {
            assertEquals(Integer.toHexString(center), center, mesh.mColors[i]);
        }
        return center;
    }

    /**
     * Draws {@link #MESH} smooth with {@code flags} and returns its center pixel, after
     * asserting that the canvas engines' vertex colors (smooth, same flags) are that color
     * (within 1, for uniformly lit squares).
     */
    private static int drawSmoothAndCompareCanvas(JavaPaint3DContext ctx, int flags) {
        ctx.drawMesh3D(MESH, SMOOTH | flags);
        int center = pixels(ctx)[CENTER];
        JavaPaint3DContext.CanvasMesh mesh = new JavaPaint3DContext.CanvasMesh();
        assertEquals(6, ctx.buildCanvasVertices(MESH, mesh, CANVAS_SMOOTH | flags));
        for (int i = 0; i < 6; i++) {
            assertColorNear(center, mesh.mColors[i]);
        }
        return center;
    }

    private static int[] pixels(JavaPaint3DContext ctx) {
        int[] color = ctx.getColorBuffer();
        assertNotNull(color);
        return color;
    }

    private static int countDrawn(int[] color) {
        int count = 0;
        for (int c : color) {
            if (c != 0) {
                count++;
            }
        }
        return count;
    }

    /** Asserts every channel of {@code actual} is within 1 of {@code expected}'s. */
    private static void assertColorNear(int expected, int actual) {
        for (int shift = 0; shift < 32; shift += 8) {
            int e = (expected >>> shift) & 0xFF;
            int a = (actual >>> shift) & 0xFF;
            assertTrue(String.format("expected %08X, got %08X", expected, actual),
                    Math.abs(e - a) <= 1);
        }
    }

    private static void assertPendingContainsDrawnPixels(JavaPaint3DContext ctx) {
        int[] color = pixels(ctx);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (color[y * SIZE + x] != 0) {
                    assertTrue("pixel " + x + "," + y + " outside the pending rectangle",
                            ctx.hasPendingPixels()
                                    && x >= ctx.getPendingLeft() && x < ctx.getPendingRight()
                                    && y >= ctx.getPendingTop() && y < ctx.getPendingBottom());
                }
            }
        }
    }
}
