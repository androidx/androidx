/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static androidx.compose.remote.core.operations.ImageAttribute.IMAGE_HEIGHT;
import static androidx.compose.remote.core.operations.ImageAttribute.IMAGE_WIDTH;
import static androidx.compose.remote.core.operations.TextAttribute.MEASURE_HEIGHT;
import static androidx.compose.remote.core.operations.TextAttribute.MEASURE_WIDTH;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.player.view.TestUtils.drawTextAnchored;
import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** This test the function call syntax */
@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class AttributeTest {
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

    /** Test the image attribute */
    @Ignore("Flaky Test")
    @Test
    public void testImageAttribute() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Bitmap testImage = TestUtils.createImage(123, 321, false); // randmom size
        canvas.drawBitmap(testImage, 0, 0, paint);

        paint.setColor(Color.BLUE);
        canvas.drawOval(0, 200, tw, th, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    int bitmapId = rdoc.addBitmap(testImage);

                    float width = rdoc.bitmapAttribute(bitmapId, IMAGE_WIDTH);
                    float height = rdoc.bitmapAttribute(bitmapId, IMAGE_HEIGHT);
                    rdoc.drawBitmap(bitmapId, 0, 0, width, height, null);
                    //                    rdoc.drawScaledBitmap(bitmapId,
                    //                            0,0,123,321,
                    //                            0,0,123,321,0,1,null);

                    rdoc.getPainter().setColor(0xFF0000FF).commit();
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
                        + "BITMAP DATA 44\n"
                        + "ImageAttribute[45] = 44 0\n"
                        + "ImageAttribute[46] = 44 1\n"
                        + "DrawBitmap (desc=0)0.0 0.0 NaN NaN;\n"
                        + "PaintData \"\n"
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 [43]200.0 600.0 600.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }

        assertFalse("not equals", TestUtils.diff(expected, result));

        RemoteComposeDocument doc2 = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc2);
        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            saveBitmap(appContext, remoteBitmap, remoteImageName);
            saveBitmap(appContext, localBitmap, localImageName);
        }
        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    /** Test the text attribute */
    @Ignore("Flaky Test")
    @Test
    public void testTextAttribute() {

        int tw = 600;
        int th = 600;
        String str = "XO";
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        {
            paint.setColor(Color.BLUE);
            canvas.drawOval(0, 0, tw, th, paint);
            paint.setTextSize(30f);
            Rect bounds = new Rect();
            paint.getTextBounds(str, 0, str.length(), bounds);
            float width = bounds.width();
            float height = bounds.height();
            float scale = 30 * Math.min(tw / width, th / height);
            paint.setTextSize(scale);
            paint.setColor(Color.YELLOW);
            drawTextAnchored(str, tw / 2, th / 2, 0, 0, canvas, paint);
        }
        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    rdoc.getPainter().setColor(0xFF0000FF).commit();
                    rdoc.drawOval(0, 0, tw, th);
                    int tId = rdoc.textCreateId(str);
                    rdoc.getPainter().setTextSize(30f).setColor(Color.YELLOW).commit();

                    float width = rdoc.textAttribute(tId, MEASURE_WIDTH);
                    float height = rdoc.textAttribute(tId, MEASURE_HEIGHT);
                    float scale =
                            rdoc.floatExpression(tw, width, DIV, th, height, DIV, MIN, 30, MUL);
                    rdoc.getPainter().setTextSize(scale).setColor(Color.YELLOW).commit();

                    rdoc.drawTextAnchored(tId, tw / 2, th / 2, 0, 0, 0);
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
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 0.0 600.0 600.0\n"
                        + "TextData[44] = \"XO\"\n"
                        + "PaintData \"\n"
                        + "    TextSize(30.0),\n"
                        + "    Color(0xffffff00),\n"
                        + "\"\n"
                        + "FloatConstant[45] = 44 0\n"
                        + "FloatConstant[46] = 44 1\n"
                        + "FloatExpression[47] = (600.0 [45]0.0 / 600.0 [46]0.0 / min 30.0 * )\n"
                        + "PaintData \"\n"
                        + "    TextSize([47]),\n"
                        + "    Color(0xffffff00),\n"
                        + "\"\n"
                        + "DrawTextAnchored [44] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }

        assertFalse("not equals", TestUtils.diff(expected, result));

        RemoteComposeDocument doc2 = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc2);
        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            saveBitmap(appContext, remoteBitmap, remoteImageName);
            saveBitmap(appContext, localBitmap, localImageName);
        }
        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    /** Test the text attribute */
    @Ignore("Flaky Test")
    @Test
    public void testTextAttribute2() {

        int tw = 600;
        int th = 600;
        String str = "X";
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        {
            paint.setColor(Color.BLUE);
            canvas.drawOval(0, 0, tw, th, paint);
            paint.setTextSize(30f);
            Rect bounds = new Rect();
            paint.getTextBounds(str, 0, str.length(), bounds);
            float width = bounds.width();
            float height = bounds.height();
            float scale = 30 * Math.min(tw / width, th / height);
            paint.setTextSize(scale);
            paint.setColor(Color.YELLOW);
            drawTextAnchored(str, tw / 2, th / 2, 0, 0, canvas, paint);
        }
        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    rdoc.getPainter().setColor(0xFF0000FF).commit();
                    rdoc.drawOval(0, 0, tw, th);
                    int tId = rdoc.textCreateId(str);
                    rdoc.getPainter().setTextSize(30f).setColor(Color.YELLOW).commit();

                    float width = rdoc.textAttribute(tId, MEASURE_WIDTH);
                    float height = rdoc.textAttribute(tId, MEASURE_HEIGHT);
                    float scale =
                            rdoc.floatExpression(tw, width, DIV, th, height, DIV, MIN, 30, MUL);
                    rdoc.getPainter().setTextSize(scale).setColor(Color.YELLOW).commit();

                    rdoc.drawTextAnchored(tId, tw / 2, th / 2, 0, 0, 0);
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
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 0.0 600.0 600.0\n"
                        + "TextData[44] = \"X\"\n"
                        + "PaintData \"\n"
                        + "    TextSize(30.0),\n"
                        + "    Color(0xffffff00),\n"
                        + "\"\n"
                        + "FloatConstant[45] = 44 0\n"
                        + "FloatConstant[46] = 44 1\n"
                        + "FloatExpression[47] = (600.0 [45]0.0 / 600.0 [46]0.0 / min 30.0 * )\n"
                        + "PaintData \"\n"
                        + "    TextSize([47]),\n"
                        + "    Color(0xffffff00),\n"
                        + "\"\n"
                        + "DrawTextAnchored [44] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }

        assertFalse("not equals", TestUtils.diff(expected, result));

        RemoteComposeDocument doc2 = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc2);
        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            saveBitmap(appContext, remoteBitmap, remoteImageName);
            saveBitmap(appContext, localBitmap, localImageName);
        }
        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    // ########################### END TEST UTILS ######################################

    /** Test the time attribute */
    @Test
    public void testTimeAttribute() {

        int tw = 600;
        int th = 600;
        String str = "X";
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        {
            paint.setColor(Color.BLUE);
            canvas.drawOval(0, 0, tw, th, paint);

            paint.setTextSize(100f);
            paint.setColor(Color.BLUE);
            drawTextAnchored("44", tw / 2, th / 2, 0, 0, canvas, paint);
        }
        long time = System.currentTimeMillis();
        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    rdoc.getPainter().setColor(0xFF0000FF).commit();
                    rdoc.drawOval(0, 0, tw, th);

                    int creation_time = rdoc.addTimeLong(time);
                    float timeSinceCreate =
                            rdoc.timeAttribute(creation_time, TimeAttribute.TIME_FROM_NOW_SEC);

                    int tId = rdoc.createTextFromFloat(timeSinceCreate, 0, 4, 0);
                    // Change this to WHITE to see the difference
                    rdoc.getPainter().setTextSize(100f).setColor(0xFF0000FF).commit();
                    rdoc.drawTextAnchored(tId, tw / 2, th / 2, 0, 0, 0);
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
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 0.0 600.0 600.0\n"
                        + "LongConstant[44] = 1738377887933\n"
                        + "TimeAttribute[45] = 44 0\n"
                        + "TextFromFloat[46] = [45] 0.4 0\n"
                        + "PaintData \"\n"
                        + "    TextSize(100.0),\n"
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawTextAnchored [46] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }
        expected = expected.replace("1738377887933", Long.toString(time));

        assertFalse("not equals", TestUtils.diff(expected, result));

        RemoteComposeDocument doc2 = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc2);
        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            saveBitmap(appContext, remoteBitmap, remoteImageName);
            saveBitmap(appContext, localBitmap, localImageName);
        }
        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    /** Test the time attribute */
    @Test
    public void testTimeSet() {

        int tw = 600;
        int th = 600;
        String str = "X";
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        {
            paint.setColor(Color.BLUE);
            canvas.drawOval(0, 0, tw, th, paint);

            paint.setTextSize(100f);
            paint.setColor(Color.BLUE);
            drawTextAnchored("44", tw / 2, th / 2, 0, 0, canvas, paint);
        }
        long time = System.currentTimeMillis();
        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    rdoc.getPainter().setColor(0xFF0000FF).commit();
                    rdoc.drawOval(0, 0, tw, th);

                    int creation_time = rdoc.addNamedLong("now", time);
                    rdoc.setStringName(3, "foo");
                    float timeSinceCreate =
                            rdoc.timeAttribute(creation_time, TimeAttribute.TIME_FROM_NOW_SEC);

                    int tId = rdoc.createTextFromFloat(timeSinceCreate, 0, 4, 0);
                    // Change this to WHITE to see the difference
                    rdoc.getPainter().setTextSize(100f).setColor(0xFF0000FF).commit();
                    rdoc.drawTextAnchored(tId, tw / 2, th / 2, 0, 0, 0);
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
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 0.0 600.0 600.0\n"
                        + "LongConstant[44] = 1740461458193\n"
                        + "VariableName[44] = \"now\" type=4\n"
                        + "VariableName[3] = \"foo\" type=0\n"
                        + "TimeAttribute[45] = 44 0\n"
                        + "TextFromFloat[46] = [45] 0.4 0\n"
                        + "PaintData \"\n"
                        + "    TextSize(100.0),\n"
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "DrawTextAnchored [46] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        Log.v("TEST", result);
        expected = expected.replace("1738377887933", Long.toString(time));

        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }
        debugContext.setNamedLong("now", System.currentTimeMillis());

        RemoteComposeDocument doc2 = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc2);
        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            saveBitmap(appContext, remoteBitmap, remoteImageName);
            saveBitmap(appContext, localBitmap, localImageName);
        }
        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }
}
