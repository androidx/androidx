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

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DIV;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.player.view.TestSerializeUtils.loadFileFromRaw;
import static androidx.compose.remote.player.view.TestUtils.blank;
import static androidx.compose.remote.player.view.TestUtils.compareImages;
import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SweepGradient;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.compose.remote.player.view.test.R;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;

@SdkSuppress(minSdkVersion = 26) // b/43795894
@RunWith(JUnit4.class)
public class ColorTest {
    private boolean mSaveImages = false;

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
        String paintStr = "paintData(\n" + "   AntiAlias(0),\n" + ")\n";
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
        Bitmap localBitmap = blank(tw, th);
        Bitmap blankBitmap = blank(tw, th);

        Canvas canvas = new Canvas(localBitmap);
        if (paint == null) paint = new Paint();
        canvas.drawPath(testPath, paint);
        if (mSaveImages) {
            saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    static String getMethodName() {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    // ########################### END TEST UTILS ######################################

    @Test
    public void testSimpleColors() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        canvas.drawCircle(100, 200, 100, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(0x550000ff);
        canvas.drawCircle(200, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setColor(Color.GREEN).commit();
                    rdoc.drawCircle(100, 200, 100);
                    rdoc.getPainter().setColor(Color.RED).commit();
                    rdoc.drawCircle(300, 200, 100);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawCircle(200, 300, 100);
                };
        drawCommandTest(cb);

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);

        Log.v("TEST", result);

        System.out.println(result);

        String expected =
                "CoreDocument\n"
                        + "width: 0\n"
                        + "height: 0\n"
                        + "    TextData\n"
                        + "    textId: 42\n"
                        + "    text: Demo\n"
                        + "    RootContentDescription\n"
                        + "    contentDescriptionId: 42\n"
                        + "    PaintData\n"
                        + "    paintBundle:       PaintBundle\n"
                        + "                Color\n"
                        + "          color: 0xff00ff00\n"
                        + "    DrawCircle\n"
                        + "    cx:       Value\n"
                        + "      100.0\n"
                        + "    cy:       Value\n"
                        + "      200.0\n"
                        + "    radius:       Value\n"
                        + "      100.0\n"
                        + "    PaintData\n"
                        + "    paintBundle:       PaintBundle\n"
                        + "                Color\n"
                        + "          color: 0xffff0000\n"
                        + "    DrawCircle\n"
                        + "    cx:       Value\n"
                        + "      300.0\n"
                        + "    cy:       Value\n"
                        + "      200.0\n"
                        + "    radius:       Value\n"
                        + "      100.0\n"
                        + "    PaintData\n"
                        + "    paintBundle:       PaintBundle\n"
                        + "                Color\n"
                        + "          color: 0x550000ff\n"
                        + "    DrawCircle\n"
                        + "    cx:       Value\n"
                        + "      200.0\n"
                        + "    cy:       Value\n"
                        + "      300.0\n"
                        + "    radius:       Value\n"
                        + "      100.0\n";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

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
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testSimpleColors2() {

        int tw = 600;
        int th = 600;
        int red = Color.argb(128, 255, 0, 0);
        int green = Color.argb(128, 0, 255, 0);
        int blue = Color.argb(128, 0, 0, 255);

        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(red);
        canvas.drawCircle(150, 200, 100, paint);
        paint.setColor(green);
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(Utils.interpolateColor(red, green, 0.5f));
        canvas.drawCircle((150 + 300) / 2f, 300, 100, paint);
        paint.setColor(blue);
        canvas.drawCircle(450, 200, 100, paint);
        paint.setColor(Utils.interpolateColor(green, blue, 0.5f));
        canvas.drawCircle((300 + 450) / 2f, 300, 100, paint);
        int c1 = Utils.interpolateColor(green, blue, 0.5f);
        int c2 = Utils.interpolateColor(red, green, 0.5f);
        int c3 = Utils.interpolateColor(c1, c2, 0.5f);
        paint.setColor(c3);
        canvas.drawCircle((150 + 300 + 300 + 450) / 4f, 400, 100, paint);
        int c4 = Utils.interpolateColor(red, c2, 0.5f);
        paint.setColor(c4);
        canvas.drawCircle(100, 300, 100, paint);
        int c5 = Utils.interpolateColor(c1, blue, 0.5f);
        paint.setColor(c5);
        canvas.drawCircle(500, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setColor(red).commit();
                    rdoc.drawCircle(150, 200, 100);
                    rdoc.getPainter().setColor(green).commit();
                    rdoc.drawCircle(300, 200, 100);
                    short id1 = rdoc.addColorExpression(red, green, 0.5f);

                    rdoc.getPainter().setColorId(id1).commit();
                    rdoc.drawCircle((150 + 300) / 2f, 300, 100);
                    rdoc.getPainter().setColor(blue).commit();
                    rdoc.drawCircle(450, 200, 100);
                    short id2 = rdoc.addColorExpression(green, blue, 0.5f);
                    rdoc.getPainter().setColorId(id2).commit();
                    rdoc.drawCircle((300 + 450) / 2f, 300, 100);
                    short id3 = rdoc.addColorExpression(id1, id2, 0.5f);
                    rdoc.getPainter().setColorId(id3).commit();
                    rdoc.drawCircle((150 + 300 + 300 + 450) / 4f, 400, 100);
                    short id4 = rdoc.addColorExpression(id1, red, 0.5f);
                    rdoc.getPainter().setColorId(id4).commit();
                    rdoc.drawCircle(100f, 300, 100);
                    short id5 = rdoc.addColorExpression(blue, id2, 0.5f);
                    rdoc.getPainter().setColorId(id5).commit();
                    rdoc.drawCircle(500f, 300, 100);
                };
        drawCommandTest(cb);

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        result = result.replaceAll("\\d+E9", "");
        Log.v("TEST", result);

        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_simple_colors2);
        expected = expected.replaceAll("\\d+E9", "");

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);

        if (!mSaveImages) {

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

        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testSimpleColors3() {

        int tw = 600;
        int th = 600;
        int red = Color.argb(128, 255, 0, 0);
        int green = Color.argb(128, 0, 255, 0);
        int blue = Color.argb(128, 0, 0, 255);

        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        int[] colors = {
            Color.HSVToColor(0xFF, new float[] {0, 0.9f, 0.9f}),
            Color.HSVToColor(0xFF, new float[] {60, 0.9f, 0.9f}),
            Color.HSVToColor(0xFF, new float[] {120, 0.9f, 0.9f}),
            Color.HSVToColor(0xFF, new float[] {180, 0.9f, 0.9f}),
            Color.HSVToColor(0xFF, new float[] {240, 0.9f, 0.9f}),
            Color.HSVToColor(0xFF, new float[] {300, 0.9f, 0.9f}),
            Color.HSVToColor(0xFF, new float[] {360, 0.9f, 0.9f}),
        };

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new SweepGradient(300, 300, colors, null));
        canvas.drawCircle(300, 300, 200, paint);
        paint.setShader(null);
        for (int i = 0; i < 6; i++) {
            int color = Color.HSVToColor(0x8F, new float[] {i * 60, 0.9f, 0.9f});
            paint.setColor(color);
            double angle = i * Math.PI * 2f / 6;
            float x = 300 + (float) (Math.cos(angle) * 200);
            float y = 300 + (float) (Math.sin(angle) * 200);
            canvas.drawCircle(x, y, 100, paint);
        }

        paint.setColor(Utils.interpolateColor(red, green, 0.5f));

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setSweepGradient(300f, 300f, colors, null).commit();
                    rdoc.drawCircle(300, 300, 200);
                    rdoc.getPainter().setShader(0).commit();
                    for (int i = 0; i < 6; i++) {
                        short id2 = rdoc.addColorExpression(0x8F, i / 6f, 0.9f, 0.9f);
                        rdoc.getPainter().setColorId(id2).commit();
                        double angle = i * Math.PI * 2f / 6;
                        float x = 300 + (float) (Math.cos(angle) * 200);
                        float y = 300 + (float) (Math.sin(angle) * 200);
                        rdoc.drawCircle(x, y, 100);
                    }
                };
        drawCommandTest(cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);

        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_simple_colors3);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);

        if (!mSaveImages) {

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

        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testNamedColors1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        canvas.drawCircle(100, 200, 100, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(0x550000ff);
        canvas.drawCircle(200, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setColorId(rdoc.addNamedColor("GREEN", Color.GREEN)).commit();
                    rdoc.drawCircle(100, 200, 100);
                    rdoc.getPainter().setColor(Color.RED).commit();
                    rdoc.drawCircle(300, 200, 100);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawCircle(200, 300, 100);
                };
        drawCommandTest(cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_named_colors1);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (!mSaveImages) {

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
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testNamedColors2() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        canvas.drawCircle(100, 200, 100, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(0x550000ff);
        canvas.drawCircle(200, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setColorId(rdoc.addNamedColor("GREEN", Color.GREEN)).commit();
                    rdoc.drawCircle(100, 200, 100);
                    int redId = rdoc.addColor(Color.RED);
                    rdoc.setColorName(redId, "RED");

                    rdoc.getPainter().setColorId(redId).commit();
                    rdoc.drawCircle(300, 200, 100);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawCircle(200, 300, 100);
                };
        drawCommandTest(cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);

        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_named_colors2);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

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
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testNamedColors3() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.CYAN);
        canvas.drawCircle(100, 200, 100, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(0x550000ff);
        canvas.drawCircle(200, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.getPainter().setColorId(rdoc.addNamedColor("GREEN", Color.GREEN)).commit();
                    rdoc.drawCircle(100, 200, 100);
                    int redId = rdoc.addColor(Color.RED);
                    rdoc.setColorName(redId, "RED");

                    rdoc.getPainter().setColorId(redId).commit();
                    rdoc.drawCircle(300, 200, 100);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawCircle(200, 300, 100);
                };
        drawCommandTest(cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);

        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_named_colors3);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);
        final ArrayList<String> colors = new ArrayList<>();
        Bitmap remoteBitmap =
                TestUtils.docToBitmap(
                        tw,
                        th,
                        appContext,
                        doc,
                        (c -> {
                            c.setColor("GREEN", Color.CYAN);
                            colors.addAll(Arrays.asList(c.getNamedColors()));
                        }));
        String[] col = colors.toArray(new String[0]);
        System.out.println(">>>> " + Arrays.toString(col));
        Bitmap blankBitmap = blank(tw, th);

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
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testRGBColors1() {

        int tw = 600;
        int th = 600;
        int red = Color.argb(128, 255, 0, 0);
        int green = Color.argb(128, 0, 255, 0);
        int blue = Color.argb(128, 0, 0, 255);

        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(128, 0, 0, 0));
        canvas.drawCircle(200, 210, 100, paint);
        paint.setColor(Color.argb(128, 0, 0, 204));
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(Color.argb(128, 0, 204, 0));
        canvas.drawCircle(400, 200, 100, paint);
        paint.setColor(Color.argb(128, 0, 204, 204));
        canvas.drawCircle(500, 200, 100, paint);
        paint.setColor(Color.argb(128, 204, 0, 0));
        canvas.drawCircle(200, 300, 100, paint);
        paint.setColor(Color.argb(128, 204, 0, 204));
        canvas.drawCircle(300, 300, 100, paint);
        paint.setColor(Color.argb(128, 204, 204, 0));
        canvas.drawCircle(400, 300, 100, paint);
        paint.setColor(Color.argb(128, 204, 204, 204));
        canvas.drawCircle(500, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    short id1 = rdoc.addColorExpression(0.5f, 0.0f, 0.0f, 0.0f);
                    rdoc.getPainter().setColorId(id1).commit();
                    rdoc.drawCircle(200, 210, 100);

                    short id2 = rdoc.addColorExpression(0.5f, 0.0f, 0.0f, 0.8f);
                    rdoc.getPainter().setColorId(id2).commit();
                    rdoc.drawCircle(300, 200, 100);

                    short id3 = rdoc.addColorExpression(0.5f, 0.0f, 0.8f, 0.0f);
                    rdoc.getPainter().setColorId(id3).commit();
                    rdoc.drawCircle(400, 200, 100);

                    short id4 = rdoc.addColorExpression(0.5f, 0.0f, 0.8f, 0.8f);
                    rdoc.getPainter().setColorId(id4).commit();
                    rdoc.drawCircle(500, 200, 100);

                    short id5 = rdoc.addColorExpression(0.5f, 0.8f, 0.0f, 0.0f);
                    rdoc.getPainter().setColorId(id5).commit();
                    rdoc.drawCircle(200, 300, 100);

                    short id6 = rdoc.addColorExpression(0.5f, 0.8f, 0.0f, 0.8f);
                    rdoc.getPainter().setColorId(id6).commit();
                    rdoc.drawCircle(300, 300, 100);

                    short id7 = rdoc.addColorExpression(0.5f, 0.8f, 0.8f, 0.0f);
                    rdoc.getPainter().setColorId(id7).commit();
                    rdoc.drawCircle(400, 300, 100);

                    short id8 = rdoc.addColorExpression(0.5f, 0.8f, 0.8f, 0.8f);
                    rdoc.getPainter().setColorId(id8).commit();
                    rdoc.drawCircle(500, 300, 100);
                };
        drawCommandTest(cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_rgb_colors1);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);

        if (!mSaveImages) {

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

        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testRGBColors2() {

        int tw = 600;
        int th = 600;

        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.argb(128, 0, 0, 0));
        canvas.drawCircle(200, 200, 100, paint);
        paint.setColor(Color.argb(128, 0, 0, 204));
        canvas.drawCircle(300, 200, 100, paint);
        paint.setColor(Color.argb(128, 0, 204, 0));
        canvas.drawCircle(400, 200, 100, paint);
        paint.setColor(Color.argb(128, 0, 204, 204));
        canvas.drawCircle(500, 200, 100, paint);
        paint.setColor(Color.argb(128, 204, 0, 0));
        canvas.drawCircle(200, 300, 100, paint);
        paint.setColor(Color.argb(128, 204, 0, 204));
        canvas.drawCircle(300, 300, 100, paint);
        paint.setColor(Color.argb(128, 204, 204, 0));
        canvas.drawCircle(400, 300, 100, paint);
        paint.setColor(Color.argb(128, 204, 204, 204));
        canvas.drawCircle(500, 300, 100, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    float frac = rdoc.floatExpression(0.8f, 1f, MUL);
                    float half = rdoc.floatExpression(1f, 2f, DIV);
                    short id1 = rdoc.addColorExpression(half, 0.0f, 0.0f, 0.0f);
                    rdoc.getPainter().setColorId(id1).commit();
                    rdoc.drawCircle(200, 200, 100);

                    short id2 = rdoc.addColorExpression(0.5f, 0.0f, 0.0f, frac);
                    rdoc.getPainter().setColorId(id2).commit();
                    rdoc.drawCircle(300, 200, 100);

                    short id3 = rdoc.addColorExpression(half, 0.0f, frac, 0.0f);
                    rdoc.getPainter().setColorId(id3).commit();
                    rdoc.drawCircle(400, 200, 100);

                    short id4 = rdoc.addColorExpression(half, 0.0f, frac, frac);
                    rdoc.getPainter().setColorId(id4).commit();
                    rdoc.drawCircle(500, 200, 100);

                    short id5 = rdoc.addColorExpression(0.5f, frac, 0.0f, 0.0f);
                    rdoc.getPainter().setColorId(id5).commit();
                    rdoc.drawCircle(200, 300, 100);

                    short id6 = rdoc.addColorExpression(half, frac, 0.0f, frac);
                    rdoc.getPainter().setColorId(id6).commit();
                    rdoc.drawCircle(300, 300, 100);

                    short id7 = rdoc.addColorExpression(0.5f, frac, frac, 0.0f);
                    rdoc.getPainter().setColorId(id7).commit();
                    rdoc.drawCircle(400, 300, 100);

                    short id8 = rdoc.addColorExpression(half, frac, frac, frac);
                    rdoc.getPainter().setColorId(id8).commit();
                    rdoc.drawCircle(500, 300, 100);
                };
        drawCommandTest(cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_rgb_colors2);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("clipPath", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);

        if (!mSaveImages) {

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

        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
