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

import static androidx.compose.remote.core.RemoteContext.FLOAT_WINDOW_WIDTH;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ACOS;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;

import androidx.compose.remote.core.operations.TextFromFloat;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Painter;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.DecimalFormat;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class FloatTest {
    private final boolean mSaveImages = false;

    static String sColorShaderSrc =
            "        uniform float2 iResolution;\n"
                + "        uniform float iTime;\n"
                + "        half4 main(vec2 fragcoord) { \n"
                + "            vec2 uv = (.53 - fragcoord.xy / iResolution.y) * 16;\n"
                + "            vec3 color = vec3(0.0, 0.0, 0.4);\n"
                + "\n"
                + "            float size =  0.3 * sin(length(floor(uv) * 0.5) - iTime);\n"
                + "            color.rgb += smoothstep(0.1, 0.0, length(fract(uv) - 0.5) - size);\n"
                + "\t        color.g *= 0.4;\n"
                + "            color.rg *= 0.4;\n"
                + "            color.b *= 0.8;\n"
                + "            return vec4(color, 1.0);\n"
                + "        }";

    // ########################### TEST UTILS ######################################

    String drawCommandTest(TestUtils.Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

    // ########################### END TEST UTILS ######################################
    void pathTest(int tw, int th, Path testPath, String str) {
        pathTest(tw, th, testPath, str, null, null, null);
    }

    void pathTest(
            int tw,
            int th,
            Path testPath,
            String pathString,
            Paint paint,
            String paintString,
            TestUtils.Callback cb) {

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
        String result = debugContext.getTestResults();
        String paintStr = "paintData(\n" + "    AntiAlias(0),\n" + ")\n";
        if (paintString != null) {
            paintStr = paintString;
        }
        String expectedResult =
                "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)=\"Demo\"\n"
                        + pathString
                        + "setTheme(-1)\n"
                        + "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)=\"Demo\"\n"
                        + pathString
                        + paintStr
                        + "drawPath(43, 0.0, 1.0)\n";
        if (TestUtils.diff(result, expectedResult)) { // they are different expect issues
            System.out.println("---------actual Result-----------");
            System.out.println(result);
            System.out.println("---------expectedResult-----------");
            System.out.println(expectedResult);
            System.out.println("----------------------------------");
        }
        assertEquals("write doc \n\n\n" + result + "\n\n\n", expectedResult, result);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Bitmap blankBitmap = TestUtils.blank(tw, th);

        Canvas canvas = new Canvas(localBitmap);
        if (paint == null) paint = new Paint();
        canvas.drawPath(testPath, paint);
        if (mSaveImages) {
            TestUtils.saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            TestUtils.saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    static String getMethodName() {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    private void checkResults(
            Context appContext, Bitmap blankBitmap, Bitmap remoteBitmap, Bitmap localBitmap) {
        if (!mSaveImages) {
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        Log.v("TEST", "vs. blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        Log.v("TEST", "vs. local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    // ########################### END TEST UTILS ######################################

    @Test
    public void testFloatConstant1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x550000ff);
        canvas.drawOval(0, 200, tw, th, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawOval(0, fid, tw, th);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        doc.initializeContext(debugContext);
        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatConstant[43] = 200.0\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 [43]200.0 600.0 600.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);

        // RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Test
    public void testFloatExpression2() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x550000ff);
        canvas.drawOval(0, 200, tw, th, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.floatExpression(100, 2, MUL);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawOval(0, fid, tw, th);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        doc.initializeContext(debugContext);
        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (100.0 2.0 * )\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 [43]200.0 600.0 600.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);

        // RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Test
    public void testFloatEval1() {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            float halfWidth = tw * 0.5f;
            paint.setColor(0x550000ff);
            canvas.drawOval(0, top, halfWidth, th, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.floatExpression(200, 0.5f, MUL);
                    Float halfWidth = rdoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawOval(0, fid, halfWidth, th);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        doc.initializeContext(debugContext);
        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (200.0 0.5 * )\n"
                        + "FloatExpression[44] = ([5]600.0 0.5 * )\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 [43]100.0 [44]300.0 600.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testFloatEval2() {
        assumeTrue(
                "PathIterator and NaN not support on API 34+",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            Float halfWidth = tw * 0.5f;
            paint.setColor(0x550000ff);
            Path p = new Path();
            p.reset();
            p.quadTo(10, 20, 600, 300);
            p.moveTo(2, 3);
            p.close();
            canvas.drawPath(p, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.floatExpression(200, 0.5f, MUL);
                    Float halfWidth = rdoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    Path p = new Path();
                    p.reset();
                    p.quadTo(10, 20, FLOAT_WINDOW_WIDTH, 300);
                    p.moveTo(2, 3);
                    p.close();
                    int pid = rdoc.addPathData(p);
                    rdoc.drawPath(pid);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (200.0 0.5 * )\n"
                        + "FloatExpression[44] = ([5]600.0 0.5 * )\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "PathData[45] = \"M 0.0 0.0 Q 0.0 0.0 10.0 20.0 [5] 300.0 M 2.0 3.0 Z\"\n"
                        + "DrawPath [45], 0.0, 1.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testFloatEvalPaint() {
        assumeTrue(
                "PathIterator and NaN not support on API 34+",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            float halfWidth = tw * 0.5f;
            paint.setColor(0x550000ff);
            paint.setStrokeWidth(10);
            paint.setStyle(Paint.Style.STROKE);
            Path p = new Path();
            p.reset();
            p.moveTo(100, 200);
            p.quadTo(10, 300, 600, 300);
            p.lineTo(2, 600);
            p.close();
            canvas.drawPath(p, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    float fid = rdoc.floatExpression(200, 0.05f, MUL);
                    float halfWidth = rdoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
                    float strokeWidth = rdoc.addFloatConstant(32f);
                    rdoc.getPainter()
                            .setColor(0x550000ff)
                            .setStrokeWidth(fid)
                            .setStyle(Paint.Style.STROKE)
                            .commit();
                    Path p = new Path();
                    p.reset();
                    p.moveTo(100, 200);
                    p.quadTo(10, 300, FLOAT_WINDOW_WIDTH, 300);
                    p.lineTo(2, 600);
                    p.close();
                    int pid = rdoc.addPathData(p);
                    rdoc.drawPath(pid);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (200.0 0.05 * )\n"
                        + "FloatExpression[44] = ([5]600.0 0.5 * )\n"
                        + "FloatConstant[45] = 32.0\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "    StrokeWidth([43]),\n"
                        + "    Style(1),\n"
                        + "\"\n"
                        + "PathData[46] = \"M 100.0 200.0 Q 100.0 200.0 10.0 300.0 [5] 300.0 L [5]"
                        + " 300.0 2.0 600.0 Z\"\n"
                        + "DrawPath [46], 0.0, 1.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testFloatEvalGradient1() {
        assumeTrue(
                "PathIterator and NaN not support on API 34+",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        float[] stopsGradient = new float[] {0, 0.5f, 1}; // the 3 points correspond to the 3 colors
        int[] colorsGradient = new int[] {Color.RED, Color.WHITE, Color.BLUE};
        float rgX = tw / 2f; // x for the gradient center
        float rgY = th / 2f; // y for the gradient center
        float rgRadius = 200;

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            float halfWidth = tw * 0.5f;

            RadialGradient radialGradient =
                    new RadialGradient(
                            200.0f,
                            rgY,
                            rgRadius,
                            colorsGradient,
                            stopsGradient,
                            Shader.TileMode.CLAMP);
            paint.setShader(radialGradient);
            paint.setStrokeWidth(10);
            paint.setStyle(Paint.Style.FILL);
            Path p = new Path();
            p.reset();
            p.moveTo(100, 200);
            p.quadTo(10, 300, 600, 300);
            p.lineTo(2, 600);
            p.close();
            canvas.drawPath(p, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    float yPos = rdoc.addFloatConstant(400);
                    float fid = rdoc.floatExpression(200, 0.05f, MUL);
                    float halfWidth = rdoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
                    float strokeWidth = rdoc.addFloatConstant(32f);
                    rdoc.getPainter()
                            .setStrokeWidth(fid)
                            .setRadialGradient(
                                    200.f,
                                    halfWidth,
                                    rgRadius,
                                    colorsGradient,
                                    stopsGradient,
                                    Shader.TileMode.CLAMP)
                            .setStyle(Paint.Style.FILL)
                            .commit();
                    Path p = new Path();
                    p.reset();
                    p.moveTo(100, 200);
                    p.quadTo(10, 300, FLOAT_WINDOW_WIDTH, 300);
                    p.lineTo(2, 600);
                    p.close();
                    int pid = rdoc.addPathData(p);
                    rdoc.drawPath(pid);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatConstant[43] = 400.0\n"
                        + "FloatExpression[44] = (200.0 0.05 * )\n"
                        + "FloatExpression[45] = ([5]600.0 0.5 * )\n"
                        + "FloatConstant[46] = 32.0\n"
                        + "PaintData \"\n"
                        + "    StrokeWidth([44]),\n"
                        + "    RadialGradient(\n"
                        + "      colors = [0xffff0000, 0xffffffff, 0xff0000ff],\n"
                        + "      stops = [0.0, 0.5, 1.0],\n"
                        + "      center = [200.0, [45]],\n"
                        + "      radius = 200.0,\n"
                        + "      tileMode = 0\n"
                        + "    ),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "PathData[47] = \"M 100.0 200.0 Q 100.0 200.0 10.0 300.0 [5] 300.0 L [5]"
                        + " 300.0 2.0 600.0 Z\"\n"
                        + "DrawPath [47], 0.0, 1.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testFloatEvalGradient2() {
        assumeTrue(
                "PathIterator and NaN not support on API 34+",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        float[] stopsGradient = new float[] {0, 0.5f, 1}; // the 3 points correspond to the 3 colors
        int[] colorsGradient = new int[] {Color.RED, Color.WHITE, Color.BLUE};
        float rgX = tw / 2f; // x for the gradient center
        float rgY = th / 2f; // y for the gradient center
        float rgRadius = 200;

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            float halfWidth = tw * 0.5f;
            stopsGradient[1] = 0.3f;
            RadialGradient radialGradient =
                    new RadialGradient(
                            200.0f,
                            rgY,
                            rgRadius,
                            colorsGradient,
                            stopsGradient,
                            Shader.TileMode.CLAMP);
            paint.setShader(radialGradient);
            paint.setStrokeWidth(10);
            paint.setStyle(Paint.Style.FILL);
            Path p = new Path();
            p.reset();
            p.moveTo(100, 200);
            p.quadTo(10, 300, 600, 300);
            p.lineTo(2, 600);
            p.close();
            canvas.drawPath(p, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    float fid = rdoc.floatExpression(200, 0.05f, MUL);
                    float halfWidth = rdoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
                    float strokeWidth = rdoc.addFloatConstant(32f);
                    stopsGradient[1] = rdoc.addFloatConstant(0.3f);
                    rdoc.getPainter()
                            .setStrokeWidth(fid)
                            .setRadialGradient(
                                    200.f,
                                    halfWidth,
                                    rgRadius,
                                    colorsGradient,
                                    stopsGradient,
                                    Shader.TileMode.CLAMP)
                            .setStyle(Paint.Style.FILL)
                            .commit();
                    Path p = new Path();
                    p.reset();
                    p.moveTo(100, 200);
                    p.quadTo(10, 300, FLOAT_WINDOW_WIDTH, 300);
                    p.lineTo(2, 600);
                    p.close();
                    int pid = rdoc.addPathData(p);
                    rdoc.drawPath(pid);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (200.0 0.05 * )\n"
                        + "FloatExpression[44] = ([5]600.0 0.5 * )\n"
                        + "FloatConstant[45] = 32.0\n"
                        + "FloatConstant[46] = 0.3\n"
                        + "PaintData \"\n"
                        + "    StrokeWidth([43]),\n"
                        + "    RadialGradient(\n"
                        + "      colors = [0xffff0000, 0xffffffff, 0xff0000ff],\n"
                        + "      stops = [0.0, [46], 1.0],\n"
                        + "      center = [200.0, [44]],\n"
                        + "      radius = 200.0,\n"
                        + "      tileMode = 0\n"
                        + "    ),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "PathData[47] = \"M 100.0 200.0 Q 100.0 200.0 10.0 300.0 [5] 300.0 L [5]"
                        + " 300.0 2.0 600.0 Z\"\n"
                        + "DrawPath [47], 0.0, 1.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testTextFromFloat1() {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            Float halfWidth = tw * 0.5f;
            paint.setColor(0xff000077);
            paint.setTextSize(100f);
            DecimalFormat df = new DecimalFormat("#.0000");
            String s = df.format(Math.PI);
            float m = paint.measureText(s);
            Rect rect = new Rect();
            paint.getTextBounds(s, 0, s.length(), rect);
            canvas.drawText(
                    s,
                    tw / 2.f - rect.width() / 2f - rect.left,
                    th / 2.f + rect.height() / 2f - rect.bottom,
                    paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    float pi = rdoc.floatExpression(0f, ACOS, 2f, MUL);
                    rdoc.getPainter().setColor(0xff000077).setTextSize(100f).commit();

                    int strId = rdoc.createTextFromFloat(pi, 2, 4, 0);
                    rdoc.drawTextAnchored(strId, tw / 2f, th / 2.f, 0f, 0f, 0);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (0.0 acos 2.0 * )\n"
                        + "PaintData \"\n"
                        + "    Color(0xff000077),\n"
                        + "    TextSize(100.0),\n"
                        + "\"\n"
                        + "TextFromFloat[44] = [43] 2.4 0\n"
                        + "DrawTextAnchored [44] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testTextFromFloat2() {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            Float halfWidth = tw * 0.5f;
            paint.setColor(0xff000077);
            paint.setTextSize(100f);
            String s = 32.2 + "";
            float m = paint.measureText(s);
            Rect rect = new Rect();
            paint.getTextBounds(s, 0, s.length(), rect);
            canvas.drawText(
                    s,
                    tw / 2.f - rect.width() / 2f - rect.left,
                    th / 2.f + rect.height() / 2f - rect.bottom,
                    paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.floatExpression(200, 0.5f, MUL);
                    Float halfWidth = rdoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
                    rdoc.getPainter().setColor(0xff000077).setTextSize(100f).commit();

                    int strId = rdoc.createTextFromFloat(32.2321f, 2, 1, 0);
                    rdoc.drawTextAnchored(strId, tw / 2f, th / 2.f, 0f, 0f, 0);
                    // rdoc.drawTextRun(nstr,0,nstr.length(),0,1,10f,th/2.f,false);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        doc.initializeContext(debugContext);
        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (200.0 0.5 * )\n"
                        + "FloatExpression[44] = ([5]600.0 0.5 * )\n"
                        + "PaintData \"\n"
                        + "    Color(0xff000077),\n"
                        + "    TextSize(100.0),\n"
                        + "\"\n"
                        + "TextFromFloat[45] = 32.2321 2.1 0\n"
                        + "DrawTextAnchored [45] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testTextFromFloat3() {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            Float halfWidth = tw * 0.5f;
            paint.setColor(0xff000077);
            paint.setTextSize(50f);
            DecimalFormat df = new DecimalFormat("#.0000");
            String s = df.format(Math.PI);
            float m = paint.measureText(s);
            Rect rect = new Rect();
            float k = 0.05f;
            paint.setTypeface(Typeface.MONOSPACE);
            canvas.drawText(" 3.1416", 50, th * (k += 0.1f), paint);
            canvas.drawText("23.", 50, th * (k += 0.1f), paint);
            canvas.drawText(" 0", 50, th * (k += 0.1f), paint);
            canvas.drawText("2.1200", 50, th * (k += 0.1f), paint);
            canvas.drawText("2.12", 50, th * (k += 0.1f), paint);
            canvas.drawText("2.12", 50, th * (k += 0.1f), paint);
            canvas.drawText("  12.12", 50, th * (k += 0.1f), paint);
            canvas.drawText("12.12", 50, th * (k += 0.1f), paint);
            canvas.drawText("0012.12", 50, th * (k += 0.1f), paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    float pi = rdoc.floatExpression(0f, ACOS, 2f, MUL);
                    float num1 = rdoc.addFloatConstant(123.0f);
                    float num2 = rdoc.addFloatConstant(0.123f);
                    float num3 = rdoc.addFloatConstant(12.12f);
                    rdoc.getPainter()
                            .setColor(0xff000077)
                            .setTypeface(Painter.FONT_TYPE_MONOSPACE, 400, false)
                            .setTextSize(50f)
                            .commit();
                    float k = 0.05f;
                    int strId1 = rdoc.createTextFromFloat(pi, 2, 4, 0);
                    rdoc.drawTextRun(strId1, 0, -1, 0, 1, 50, th * (k += 0.1f), false);
                    int strId2 = rdoc.createTextFromFloat(num1, 2, 4, 0);
                    rdoc.drawTextRun(strId2, 0, -1, 0, 1, 50, th * (k += 0.1f), false);

                    int strId3 = rdoc.createTextFromFloat(num2, 2, 0, 0);
                    rdoc.drawTextRun(strId3, 0, -1, 0, 1, 50, th * (k += 0.1f), false);

                    int strId4 = rdoc.createTextFromFloat(num3, 1, 4, TextFromFloat.PAD_AFTER_ZERO);
                    rdoc.drawTextRun(strId4, 0, -1, 0, 1, 50, th * (k += 0.1f), false);
                    int id;
                    id = rdoc.createTextFromFloat(num3, 1, 4, TextFromFloat.PAD_AFTER_SPACE);
                    rdoc.drawTextRun(id, 0, -1, 0, 1, 50, th * (k += 0.1f), false);

                    id = rdoc.createTextFromFloat(num3, 1, 4, TextFromFloat.PAD_AFTER_NONE);
                    rdoc.drawTextRun(id, 0, -1, 0, 1, 50, th * (k += 0.1f), false);

                    id = rdoc.createTextFromFloat(num3, 4, 4, TextFromFloat.PAD_AFTER_NONE);
                    rdoc.drawTextRun(id, 0, -1, 0, 1, 50, th * (k += 0.1f), false);

                    id =
                            rdoc.createTextFromFloat(
                                    num3,
                                    4,
                                    4,
                                    TextFromFloat.PAD_AFTER_NONE | TextFromFloat.PAD_PRE_NONE);
                    rdoc.drawTextRun(id, 0, -1, 0, 1, 50, th * (k += 0.1f), false);

                    id =
                            rdoc.createTextFromFloat(
                                    num3,
                                    4,
                                    4,
                                    TextFromFloat.PAD_AFTER_NONE | TextFromFloat.PAD_PRE_ZERO);
                    rdoc.drawTextRun(id, 0, -1, 0, 1, 50, th * (k += 0.1f), false);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (0.0 acos 2.0 * )\n"
                        + "FloatConstant[44] = 123.0\n"
                        + "FloatConstant[45] = 0.123\n"
                        + "FloatConstant[46] = 12.12\n"
                        + "PaintData \"\n"
                        + "    Color(0xff000077),\n"
                        + "    TypeFace(3, 400, false),\n"
                        + "    TextSize(50.0),\n"
                        + "\"\n"
                        + "TextFromFloat[47] = [43] 2.4 0\n"
                        + "DrawTextRun [47] 0, -1, 50.0, 90.0\n"
                        + "TextFromFloat[48] = [44] 2.4 0\n"
                        + "DrawTextRun [48] 0, -1, 50.0, 150.0\n"
                        + "TextFromFloat[49] = [45] 2.0 0\n"
                        + "DrawTextRun [49] 0, -1, 50.0, 210.0\n"
                        + "TextFromFloat[50] = [46] 1.4 3\n"
                        + "DrawTextRun [50] 0, -1, 50.0, 270.0\n"
                        + "TextFromFloat[51] = [46] 1.4 0\n"
                        + "DrawTextRun [51] 0, -1, 50.0, 330.0\n"
                        + "TextFromFloat[52] = [46] 1.4 1\n"
                        + "DrawTextRun [52] 0, -1, 50.0, 390.00003\n"
                        + "TextFromFloat[53] = [46] 4.4 1\n"
                        + "DrawTextRun [53] 0, -1, 50.0, 450.00003\n"
                        + "TextFromFloat[54] = [46] 4.4 5\n"
                        + "DrawTextRun [54] 0, -1, 50.0, 510.00006\n"
                        + "TextFromFloat[55] = [46] 4.4 13\n"
                        + "DrawTextRun [55] 0, -1, 50.0, 570.00006\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testTextMerge() {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            float top = 200f * 0.5f;
            Float halfWidth = tw * 0.5f;
            paint.setColor(0xff000077);
            paint.setTextSize(100f);
            DecimalFormat df = new DecimalFormat("#.0000");
            String s = "foo:" + df.format(Math.PI);
            float m = paint.measureText(s);
            Rect rect = new Rect();
            paint.getTextBounds(s, 0, s.length(), rect);
            canvas.drawText(
                    s,
                    tw / 2.f - rect.width() / 2f - rect.left,
                    th / 2.f + rect.height() / 2f - rect.bottom,
                    paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    float pi = rdoc.floatExpression(0f, ACOS, 2f, MUL);
                    rdoc.getPainter().setColor(0xff000077).setTextSize(100f).commit();

                    int floatString = rdoc.createTextFromFloat(pi, 1, 4, 0);
                    int prompt = rdoc.textCreateId("foo:");
                    int ans = rdoc.textMerge(prompt, floatString);
                    rdoc.drawTextAnchored(ans, tw / 2f, th / 2.f, 0f, 0f, 0);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "FloatExpression[43] = (0.0 acos 2.0 * )\n"
                        + "PaintData \"\n"
                        + "    Color(0xff000077),\n"
                        + "    TextSize(100.0),\n"
                        + "\"\n"
                        + "TextFromFloat[44] = [43] 1.4 0\n"
                        + "TextData[45] = \"foo:\"\n"
                        + "TextMerge[46] = [45 ] + [ 44]\n"
                        + "DrawTextAnchored [46] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        System.out.println();
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Ignore("Flaky Test")
    @Test
    public void testDrawFloatVariables() {
        assumeTrue(
                "PathIterator and NaN not support on API 34+",
                Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE);

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(100f);
        //            paint.setTypeface()
        Path p1 = new Path();
        Path p2 = new Path();
        Path p3 = new Path();
        p1.moveTo(100, 100);
        p2.moveTo(110, 110);
        p3.moveTo(105, 105);
        p1.lineTo(200, 100);
        p2.lineTo(210, 110);
        p3.lineTo(205, 105);
        p1.lineTo(200, 200);
        p2.lineTo(210, 210);
        p3.lineTo(205, 205);
        p1.close();
        p2.close();
        p3.close();

        canvas.drawText(str, 200, th / 2f, paint);
        {
            float y = th / 2f;
            float x = 200f;
            canvas.drawCircle(x, y, 11f, paint);
            canvas.drawLine(x, y, 412f, 413f, paint);
            canvas.drawOval(x, y, 414f, 415f, paint);
            canvas.drawRoundRect(x, y, 416f, 417f, 18f, 19f, paint);
            Path p = new Path();
            p.moveTo(x, y);
            p.lineTo(20f, 21f);
            p.cubicTo(300f, 400f, 20f, 20f, 500f, 500f);
            canvas.drawPath(p, paint);
            canvas.drawPath(p3, paint);

            canvas.drawText(str, x, y, paint);
            float xOff = 10;
            canvas.drawTextOnPath(str, p, xOff, 10f, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(100f).commit();
                    float y = rdoc.floatExpression(th, 2f, AnimatedFloatExpression.DIV);
                    float x = rdoc.addFloatConstant(200);
                    rdoc.drawCircle(x, y, 11f);
                    rdoc.drawLine(x, y, 412f, 413f);
                    rdoc.drawOval(x, y, 414f, 415f);
                    rdoc.drawRoundRect(x, y, 416f, 417f, 18f, 19f);
                    Path p = new Path();
                    p.moveTo(x, y);
                    p.lineTo(20f, 21f);
                    p.cubicTo(300f, 400f, 20f, 20f, 500f, 500f);

                    int id1 = rdoc.addPathData(p1);
                    int id2 = rdoc.addPathData(p2);
                    rdoc.drawPath(p);
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, x, y, false);
                    float xOff = rdoc.addFloatConstant(10);
                    rdoc.drawTextOnPath(str, p, xOff, 10f);
                    float start = rdoc.addFloatConstant(0.01f);
                    float end = rdoc.addFloatConstant(0.99f);
                    rdoc.drawTweenPath(id1, id2, 0.5f, start, end);
                };
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        String result = doc.toString();
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                    + "HEADER v1.1.0, 600 x 600 [0]\n"
                    + "TextData[42] = \"Demo\"\n"
                    + "RootContentDescription 42\n"
                    + "PaintData \"\n"
                    + "    TextSize(100.0),\n"
                    + "\"\n"
                    + "FloatExpression[43] = (600.0 2.0 / )\n"
                    + "FloatConstant[44] = 200.0\n"
                    + "DrawCircle 200.0 300.0 11.0\n"
                    + "DrawLine [44]200.0 [43]300.0 412.0 413.0\n"
                    + "DrawOval [44]200.0 [43]300.0 414.0 415.0\n"
                    + "DrawRoundRect 200.0 300.0 416.0 417.0\n"
                    + "PathData[45] = \"M 100.0 100.0 L 100.0 100.0 200.0 100.0 L 200.0 100.0 200.0"
                    + " 200.0 Z\"\n"
                    + "PathData[46] = \"M 110.0 110.0 L 110.0 110.0 210.0 110.0 L 210.0 110.0 210.0"
                    + " 210.0 Z\"\n"
                    + "PathData[47] = \"M (44) (43) L (44) (43) 20.0 21.0 C 20.0 21.0 300.0 400.0"
                    + " 20.0 20.0 500.0 500.0\"\n"
                    + "DrawPath [47], 0.0, 1.0\n"
                    + "TextData[48] = \"hello w...\"\n"
                    + "DrawTextRun [48] 0, 11, [44]200.0, [43]300.0\n"
                    + "FloatConstant[49] = 10.0\n"
                    + "DrawTextOnPath [48] [47] [49]10.0, 10.0\n"
                    + "FloatConstant[50] = 0.01\n"
                    + "FloatConstant[51] = 0.99\n"
                    + "DrawTweenPath 45 46 0.5 [50]0.01 - [51]0.99\n"
                    + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }

        assertEquals("not equals", expected, result);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        if (mSaveImages) {
            TestUtils.saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            TestUtils.saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        TestUtils.captureGold("DrawText", doc, appContext);

        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
