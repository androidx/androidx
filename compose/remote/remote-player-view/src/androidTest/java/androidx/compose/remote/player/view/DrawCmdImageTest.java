/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.player.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SdkSuppress(minSdkVersion = 34)
@RunWith(JUnit4.class)
public class DrawCmdImageTest {
    private boolean mSaveImages = true;

    // ########################### TEST UTILS ######################################

    String drawCommandTest(TestUtils.Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

    // ########################### END TEST UTILS ######################################
    void pathTest(int tw, int th, Path testPath, String str, String testName) {
        pathTest(tw, th, testPath, str, null, null, null, testName);
    }

    void pathTest(
            int tw,
            int th,
            Path testPath,
            String pathString,
            Paint paint,
            String paintString,
            TestUtils.Callback cb,
            String testName) {

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        TestUtils.Callback basic =
                rdoc -> {
                    int v = rdoc.addPathData(testPath);
                    rdoc.getPainter().setAntiAlias(false).commit();
                    rdoc.drawPath(v);
                };
        TestUtils.Callback use = cb == null ? basic : cb;
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, use);

        doc.paint(debugContext, Theme.UNSPECIFIED);
        doc = TestUtils.createDocument(debugContext, use);
        String result = debugContext.getTestResults();
        String paintStr = "PaintData \"\n" + "    AntiAlias(0),\n";
        if (paintString != null) {
            paintStr = paintString;
        }
        result = doc.toString();
        Log.v("TEST", result);

        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PathData[43] = \""
                        + pathString
                        + "\"\n"
                        + paintStr
                        + "\"\n"
                        + "DrawPath [43], 0.0, 1.0\n"
                        + "}";

        if (TestUtils.diff(result, expectedResult)) { // they are different expect issues
            System.out.println("----------------------------------");
            TestUtils.dumpDifference(expectedResult, result);
            System.out.println("----------------------------------");
        }

        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        doc = TestUtils.createDocument(debugContext, use);
        TestUtils.captureGold(testName, doc, appContext);
        doc = TestUtils.createDocument(debugContext, use);
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Bitmap blankBitmap = TestUtils.blank(tw, th);

        Canvas canvas = new Canvas(localBitmap);
        if (paint == null) paint = new Paint();
        canvas.drawPath(testPath, paint);

        if (mSaveImages) {
            String name = mSaveImages ? TestUtils.getMethodName(3) : "bitmap";
            TestUtils.saveBoth(name, remoteBitmap, localBitmap, appContext);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    // ########################### END TEST UTILS ######################################

    @Test
    public void testPaintCubicTo() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.cubicTo(0, tw / 2, th / 2, 0, tw, th);
        path.close();
        String str = "M 0.0 0.0 C 0.0 0.0 0.0 300.0 300.0 0.0 600.0 600.0 Z";
        pathTest(tw, th, path, str, "PathCubicTo");
    }

    @Test
    public void testLineTo() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String str = "M 300.0 10.0 L 300.0 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        pathTest(tw, th, path, str, "PathLineTo");
    }

    @Test
    public void testQuadTo() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(tw / 2, 10);
        path.quadTo(tw / 2, th / 2, tw - 10, th - 10);
        path.close();
        String str = "M 300.0 10.0 Q 300.0 10.0 300.0 300.0 590.0 590.0 Z";
        pathTest(tw, th, path, str, "PathQuadTo");
    }

    @Test
    public void testMultiPath() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String str =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        pathTest(tw, th, path, str, "MultiPath");
    }

    @Test
    public void testPaintSetColor() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter().setColor(Color.RED).commit();
                    rdoc.drawPath(v);
                };
        String paintString = "PaintData \"\n" + "    Color(0xffff0000),\n";
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        pathTest(tw, th, path, pathString, paint, paintString, cb, "PaintSetColor");
    }

    @Test
    public void testPaintSetStroke() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.BEVEL)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(2),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.BEVEL);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        pathTest(tw, th, path, pathString, paint, paintString, cb, "PaintSetStroke");
    }

    @Test
    public void testPaintSetStrokeMiter() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setAlpha(0.42f)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    Alpha(0.42),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        paint.setAlpha((int) (0.42 * 255));
        pathTest(tw, th, path, pathString, paint, paintString, cb, "PaintStrokeMiter");
    }

    public void testPaintSetColorFilter() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 \n"
                        + "L 300.0 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setPorterDuffColorFilter(0xFFB97531, PorterDuff.Mode.DST_OVER)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    ColorFilter(color=0xffb97531, mode=5),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        paint.setColorFilter(new PorterDuffColorFilter(0xFFB97531, PorterDuff.Mode.DST_OVER));

        pathTest(tw, th, path, pathString, paint, paintString, cb, "PaintSetColorFilter");
    }

    public void testModes(PorterDuff.Mode pDMode) {
        System.out.println("========== " + pDMode.name() + " ============");
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setPorterDuffColorFilter(0xFFB97531, pDMode)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    ColorFilter(color=0xffb97531, mode="
                        + pDMode.name()
                        + "),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        paint.setColorFilter(new PorterDuffColorFilter(0xFFB97531, pDMode));
        pathTest(tw, th, path, pathString, paint, paintString, cb, "Porter_" + pDMode.name());
    }

    @Test
    public void testPorterDuffModesSRC_OVER() {
        testModes(PorterDuff.Mode.SRC_OVER);
    }

    @Test
    public void testPorterDuffModesDST_OVER() {
        testModes(PorterDuff.Mode.DST_OVER);
    }

    @Test
    public void testPorterDuffModesCLEAR() {
        testModes(PorterDuff.Mode.CLEAR);
    }

    @Test
    public void testPorterDuffModesSRC() {
        testModes(PorterDuff.Mode.SRC);
    }

    @Test
    public void testPorterDuffModesDST() {
        testModes(PorterDuff.Mode.DST);
    }

    @Test
    public void testPorterDuffModesSRC_IN() {
        testModes(PorterDuff.Mode.SRC_IN);
    }

    @Test
    public void testPorterDuffModesDST_IN() {
        testModes(PorterDuff.Mode.DST_IN);
    }

    @Test
    public void testPorterDuffModesSRC_OUT() {
        testModes(PorterDuff.Mode.SRC_OUT);
    }

    @Test
    public void testPorterDuffModesDST_OUT() {
        testModes(PorterDuff.Mode.DST_OUT);
    }

    @Test
    public void testPorterDuffModesSRC_ATOP() {
        testModes(PorterDuff.Mode.SRC_ATOP);
    }

    @Test
    public void testPorterDuffModesDST_ATOP() {
        testModes(PorterDuff.Mode.DST_ATOP);
    }

    @Test
    public void testPorterDuffModesXOR() {
        testModes(PorterDuff.Mode.XOR);
    }

    @Test
    public void testPorterDuffModesDARKEN() {
        testModes(PorterDuff.Mode.DARKEN);
    }

    @Test
    public void testPorterDuffModesLIGHTEN() {
        testModes(PorterDuff.Mode.LIGHTEN);
    }

    @Test
    public void testPorterDuffModesMULTIPLY() {
        testModes(PorterDuff.Mode.MULTIPLY);
    }

    @Test
    public void testPorterDuffModesSCREEN() {
        testModes(PorterDuff.Mode.SCREEN);
    }

    @Test
    public void testPorterDuffModesADD() {
        testModes(PorterDuff.Mode.ADD);
    }

    @Test
    public void testPorterDuffModesOVERLAY() {
        testModes(PorterDuff.Mode.OVERLAY);
    }

    @Test
    public void testSetLinearGradient1() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setLinearGradient(
                                    0,
                                    0,
                                    tw,
                                    th,
                                    new int[] {Color.WHITE, Color.BLACK},
                                    null,
                                    Shader.TileMode.CLAMP)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    LinearGradient(\n"
                        + "      colors = [0xffffffff, 0xff000000],\n"
                        + "      stops = null,\n"
                        + "      start = [0.0, 0.0],\n"
                        + "      end = [600.0, 600.0],\n"
                        + "      tileMode = 0\n"
                        + "    ),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        Shader shader =
                new LinearGradient(0, 0, tw, th, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        mSaveImages = true;
        pathTest(tw, th, path, pathString, paint, paintString, cb, "LinearGradient1");
    }

    @Test
    public void testSetLinearGradient2() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setLinearGradient(
                                    tw,
                                    0,
                                    0,
                                    th,
                                    new int[] {
                                        Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK
                                    },
                                    new float[] {0, 0.2f, 0.5f, 0.8f, 1},
                                    Shader.TileMode.CLAMP)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    LinearGradient(\n"
                        + "      colors = [0xffffffff, 0xffff0000, 0xff00ff00, 0xff0000ff,"
                        + " 0xff000000],\n"
                        + "      stops = [0.0, 0.2, 0.5, 0.8, 1.0],\n"
                        + "      start = [600.0, 0.0],\n"
                        + "      end = [0.0, 600.0],\n"
                        + "      tileMode = 0\n"
                        + "    ),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        Shader shader =
                new LinearGradient(
                        tw,
                        0,
                        0,
                        th,
                        new int[] {Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK},
                        new float[] {0, 0.2f, 0.5f, 0.8f, 1},
                        Shader.TileMode.CLAMP);
        paint.setShader(shader);
        mSaveImages = true;
        pathTest(tw, th, path, pathString, paint, paintString, cb, "LinearGradient2");
    }

    @Test
    public void testSetRadialGradient1() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setRadialGradient(
                                    tw / 2,
                                    th / 2,
                                    tw / 2,
                                    new int[] {Color.WHITE, Color.RED, Color.BLACK},
                                    null,
                                    Shader.TileMode.CLAMP)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    RadialGradient(\n"
                        + "      colors = [0xffffffff, 0xffff0000, 0xff000000],\n"
                        + "      stops = null,\n"
                        + "      center = [300.0, 300.0],\n"
                        + "      radius = 300.0,\n"
                        + "      tileMode = 0\n"
                        + "    ),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        Shader shader =
                new RadialGradient(
                        tw / 2,
                        th / 2,
                        tw / 2,
                        new int[] {Color.WHITE, Color.RED, Color.BLACK},
                        null,
                        Shader.TileMode.CLAMP);
        paint.setShader(shader);
        mSaveImages = true;
        pathTest(tw, th, path, pathString, paint, paintString, cb, "RadialGradient1");
    }

    @Test
    public void testSetRadialGradient2() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setRadialGradient(
                                    tw / 2,
                                    th / 2,
                                    tw / 2,
                                    new int[] {
                                        Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK
                                    },
                                    new float[] {0, 0.2f, 0.5f, 0.8f, 1},
                                    Shader.TileMode.CLAMP)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    RadialGradient(\n"
                        + "      colors = [0xffffffff, 0xffff0000, 0xff00ff00, 0xff0000ff,"
                        + " 0xff000000],\n"
                        + "      stops = [0.0, 0.2, 0.5, 0.8, 1.0],\n"
                        + "      center = [300.0, 300.0],\n"
                        + "      radius = 300.0,\n"
                        + "      tileMode = 0\n"
                        + "    ),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        Shader shader =
                new RadialGradient(
                        tw / 2,
                        th / 2,
                        tw / 2,
                        new int[] {Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK},
                        new float[] {0, 0.2f, 0.5f, 0.8f, 1},
                        Shader.TileMode.CLAMP);
        paint.setShader(shader);
        // save_images = true;
        pathTest(tw, th, path, pathString, paint, paintString, cb, "RadialGradient2");
    }

    @Test
    public void testSetSweepGradient1() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setSweepGradient(
                                    tw / 2,
                                    th / 2,
                                    new int[] {Color.WHITE, Color.RED, Color.BLACK},
                                    null)
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    SweepGradient(\n"
                        + "      colors = [0xffffffff, 0xffff0000, 0xff000000],\n"
                        + "      stops = null,\n"
                        + "      center = [300.0, 300.0],\n"
                        + "    ),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        Shader shader =
                new SweepGradient(
                        tw / 2, th / 2, new int[] {Color.WHITE, Color.RED, Color.BLACK}, null);
        paint.setShader(shader);
        // save_images = true;
        pathTest(tw, th, path, pathString, paint, paintString, cb, "SweepGradient1");
    }

    @Test
    public void testSetSweepGradient2() {
        int tw = 600, th = 600;
        Path path = new Path();
        path.reset();
        path.moveTo(10, th / 2);
        path.cubicTo(tw / 2, 10, tw / 2, th - 10, tw - 10, th / 2);
        path.moveTo(tw / 2, 10);
        path.lineTo(tw - 10, th - 10);
        path.lineTo(10, th - 10);
        path.close();
        String pathString =
                "M 10.0 300.0 C 10.0 300.0 300.0 10.0 300.0 590.0 590.0 300.0 M 300.0 10.0 L 300.0"
                        + " 10.0 590.0 590.0 L 590.0 590.0 10.0 590.0 Z";
        TestUtils.Callback cb =
                rdoc -> {
                    int v = rdoc.addPathData(path);
                    rdoc.getPainter()
                            .setStrokeJoin(Paint.Join.MITER)
                            .setStrokeWidth(14.2f)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .setStrokeMiter(0.3f)
                            .setSweepGradient(
                                    tw / 2,
                                    th / 2,
                                    new int[] {
                                        Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK
                                    },
                                    new float[] {0, 0.2f, 0.5f, 0.8f, 1})
                            .commit();
                    rdoc.drawPath(v);
                };
        String paintString =
                "PaintData \"\n"
                        + "    StrokeJoin(0),\n"
                        + "    StrokeWidth(14.2),\n"
                        + "    Style(1),\n"
                        + "    StrokeCap(1),\n"
                        + "    StrokeMiter(0.3),\n"
                        + "    SweepGradient(\n"
                        + "      colors = [0xffffffff, 0xffff0000, 0xff00ff00, 0xff0000ff,"
                        + " 0xff000000],\n"
                        + "      stops = [0.0, 0.2, 0.5, 0.8, 1.0],\n"
                        + "      center = [300.0, 300.0],\n"
                        + "    ),\n";
        Paint paint = new Paint();
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(14.2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeMiter(0.3f);
        Shader shader =
                new SweepGradient(
                        tw / 2,
                        th / 2,
                        new int[] {Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.BLACK},
                        new float[] {0, 0.2f, 0.5f, 0.8f, 1});
        paint.setShader(shader);
        mSaveImages = true;
        pathTest(tw, th, path, pathString, paint, paintString, cb, "SweepGradient2");
    }

    @Test
    public void testMultiDraw() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint();
        canvas.drawCircle(100.0f, 200.0f, 50.0f, paint);
        canvas.drawArc(10, 20, 30, 400, 1, 160, true, paint);
        canvas.drawLine(0, 0, tw, th, paint);
        canvas.drawRect(tw / 2, th / 2, tw - 10, th / 2 + 30, paint);
        canvas.drawRoundRect(tw / 2, th / 2 + 30, tw - 10, th / 2 + 70, 20, 20, paint);
        paint.setColor(Color.RED);
        canvas.drawOval(tw / 3, th / 2, 2 * tw / 3, th - 10, paint);
        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.drawCircle(100.0f, 200.0f, 50.0f);
                    rdoc.drawSector(10, 20, 30, 400, 1, 160);
                    rdoc.drawLine(0, 0, tw, th);
                    rdoc.drawRect(tw / 2, th / 2, tw - 10, th / 2 + 30);
                    rdoc.drawRoundRect(tw / 2, th / 2 + 30, tw - 10, th / 2 + 70, 20, 20);
                    rdoc.getPainter().setColor(Color.RED).commit();
                    rdoc.drawOval(tw / 3, th / 2, 2 * tw / 3, th - 10);
                };
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("MultipleDrawCommands", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        if (mSaveImages || rms > 4) {
            TestUtils.saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            TestUtils.saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
