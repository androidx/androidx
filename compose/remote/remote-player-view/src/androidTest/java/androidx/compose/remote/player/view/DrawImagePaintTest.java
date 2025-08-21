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

import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;

@SdkSuppress(minSdkVersion = 34)
@RunWith(JUnit4.class)
public class DrawImagePaintTest {
    private final boolean mSaveImages = false;

    private final Platform mPlatform = new AndroidxPlatformServices();

    // ########################### TEST UTILS ######################################

    interface Callback {
        void run(RemoteComposeContextAndroid foo);
    }

    private RemoteComposeDocument createDocument(RemoteContext context, final Callback cb) {

        RemoteComposeContext doc =
                new RemoteComposeContextAndroid(
                        600,
                        600,
                        "Demo",
                        mPlatform,
                        doc1 -> {
                            if (cb != null) {
                                cb.run(doc1);
                            }

                            return null;
                        });

        byte[] buffer = doc.buffer();
        int bufferSize = doc.bufferSize();
        System.out.println("size of doc " + bufferSize / 1024 + "KB");
        RemoteComposeDocument recreatedDocument =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));
        System.out.println(
                "recreated doc 1 "
                        + recreatedDocument.getDocument().getBuffer().getBuffer().getSize());

        recreatedDocument.initializeContext(context);
        System.out.println(
                "recreated doc 2 "
                        + recreatedDocument.getDocument().getBuffer().getBuffer().getSize());
        return recreatedDocument;
    }

    String drawCommandTest(Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

    String drawCommandList(Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return doc.toString();
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
            Callback cb) {

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        Callback basic =
                rdoc -> {
                    int v = rdoc.addPathData(testPath);
                    rdoc.getPainter().setAntiAlias(false).commit();
                    rdoc.drawPath(v);
                };
        Callback use = cb == null ? basic : cb;
        RemoteComposeDocument doc = createDocument(debugContext, use);

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

    // ########################### END TEST UTILS ######################################

    @Test
    public void testPaintFilterBitmap1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = TestUtils.createImage(30, 30, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(false);
        canvas.drawBitmap(smallTestImage, 0, 0, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setFilterBitmap(false).commit();
                    rdoc.drawBitmap(smallTestImage, "small image");
                };
        String result = drawCommandList(cb);

        String expected =
                ""
                        + "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)=\"Demo\"\n"
                        + "loadImage(43)\n"
                        + "loadText(44)=\"small i...\"\n"
                        + "setTheme(-1)\n"
                        + "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)=\"Demo\"\n"
                        + "paintData(\n"
                        + "    FilterBitmap(false),\n"
                        + ")\n"
                        + "loadImage(43)\n"
                        + "loadText(44)=\"small i...\"\n"
                        + "drawBitmap <43>\n";

        if (TestUtils.diff(expected, result)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(expected, result);
        }
        // assertEquals("not eaquals",expected,result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("PaintFilterBitmap1", doc, appContext);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (mSaveImages) {
                String methodName = TestUtils.getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testPaintFilterBitmap2() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = TestUtils.createImage(30, 30, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(smallTestImage, null, new Rect(0, 0, tw, th), paint);

        Callback cb =
                rdoc -> {
                    rdoc.drawBitmap(smallTestImage, 0, 0, tw, th, "small image");
                };
        String result = drawCommandList(cb);

        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "BITMAP DATA 43\n"
                        + "TextData[44] = \"small i...\"\n"
                        + "DrawBitmap (desc=44)0.0 0.0 600.0 600.0;\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);
        System.out.println(
                "(DrawImagePaintTest.java:296). "
                        + doc.getDocument().getBuffer().getBuffer().getSize());
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        TestUtils.captureGold("PaintFilterBitmap2", doc, appContext);

        if (mSaveImages) {
            String fileName = "PaintFilterBitmap2.rcd";
            TestUtils.saveDoc(fileName, doc, appContext);
            RemoteComposeDocument fileDoc = TestUtils.getDoc(fileName, appContext);
            Bitmap fromFileBitmap = TestUtils.docToBitmap(tw, th, appContext, fileDoc);
            float diff = TestUtils.compareImages(remoteBitmap, fromFileBitmap);
            System.out.println(" diff from saved file = " + diff);
            TestUtils.saveBitmap(appContext, fromFileBitmap, "PaintFilterBitmap2.png");
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (mSaveImages) {
                String methodName = TestUtils.getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testPaintFilterBitmap3() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = TestUtils.createImage(30, 30, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(false);
        canvas.drawBitmap(smallTestImage, null, new Rect(0, 0, tw, th), paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setFilterBitmap(false).commit();
                    rdoc.drawBitmap(smallTestImage, 0, 0, tw, th, "small image");
                };
        String result = drawCommandList(cb);
        System.out.println(result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    FilterBitmap(false),\n"
                        + "\"\n"
                        + "BITMAP DATA 43\n"
                        + "TextData[44] = \"small i...\"\n"
                        + "DrawBitmap (desc=44)0.0 0.0 600.0 600.0;\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("PaintFilterBitmap3", doc, appContext);

        if (mSaveImages) {
            String fileName = "PaintFilterBitmap3.rcd";
            TestUtils.saveDoc(fileName, doc, appContext);
            RemoteComposeDocument fileDoc = TestUtils.getDoc(fileName, appContext);
            Bitmap fromFileBitmap = TestUtils.docToBitmap(tw, th, appContext, fileDoc);
            float diff = TestUtils.compareImages(remoteBitmap, fromFileBitmap);
            System.out.println(" diff from saved file = " + diff);
            TestUtils.saveBitmap(appContext, fromFileBitmap, "PaintFilterBitmap3.png");
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (mSaveImages) {
                String methodName = TestUtils.getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @SdkSuppress(minSdkVersion = 29)
    private void blendModeTestTool(BlendMode blendmode, String modeStr) {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";
        Bitmap smallTestImage = TestUtils.createImage(30, 30, false);

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setBlendMode(blendmode);
        paint.setFilterBitmap(false);
        canvas.drawBitmap(smallTestImage, null, new Rect(0, 0, tw, th), paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setFilterBitmap(false).setBlendMode(blendmode).commit();
                    rdoc.drawBitmap(smallTestImage, 0, 0, tw, th, "small image");
                };
        String result = drawCommandList(cb);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    FilterBitmap(false),\n"
                        + "    BlendMode("
                        + modeStr
                        + "),\n"
                        + "\"\n"
                        + "BITMAP DATA 43\n"
                        + "TextData[44] = \"small i...\"\n"
                        + "DrawBitmap (desc=44)0.0 0.0 600.0 600.0;\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("blend_" + modeStr, doc, appContext);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeCLEAR() {
        blendModeTestTool(BlendMode.CLEAR, "CLEAR");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSRC() {
        blendModeTestTool(BlendMode.SRC, "SRC");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDST() {
        blendModeTestTool(BlendMode.DST, "DST");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSRC_OVER() {
        blendModeTestTool(BlendMode.SRC_OVER, "SRC_OVER");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDST_OVER() {
        blendModeTestTool(BlendMode.DST_OVER, "DST_OVER");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSRC_IN() {
        blendModeTestTool(BlendMode.SRC_IN, "SRC_IN");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDST_IN() {
        blendModeTestTool(BlendMode.DST_IN, "DST_IN");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSRC_OUT() {
        blendModeTestTool(BlendMode.SRC_OUT, "SRC_OUT");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDST_OUT() {
        blendModeTestTool(BlendMode.DST_OUT, "DST_OUT");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSRC_ATOP() {
        blendModeTestTool(BlendMode.SRC_ATOP, "SRC_ATOP");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDST_ATOP() {
        blendModeTestTool(BlendMode.DST_ATOP, "DST_ATOP");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeXOR() {
        blendModeTestTool(BlendMode.XOR, "XOR");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModePLUS() {
        blendModeTestTool(BlendMode.PLUS, "PLUS");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeMODULATE() {
        blendModeTestTool(BlendMode.MODULATE, "MODULATE");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSCREEN() {
        blendModeTestTool(BlendMode.SCREEN, "SCREEN");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeOVERLAY() {
        blendModeTestTool(BlendMode.OVERLAY, "OVERLAY");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDARKEN() {
        blendModeTestTool(BlendMode.DARKEN, "DARKEN");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeLIGHTEN() {
        blendModeTestTool(BlendMode.LIGHTEN, "LIGHTEN");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeCOLOR_DODGE() {
        blendModeTestTool(BlendMode.COLOR_DODGE, "COLOR_DODGE");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeCOLOR_BURN() {
        blendModeTestTool(BlendMode.COLOR_BURN, "COLOR_BURN");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeHARD_LIGHT() {
        blendModeTestTool(BlendMode.HARD_LIGHT, "HARD_LIGHT");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSOFT_LIGHT() {
        blendModeTestTool(BlendMode.SOFT_LIGHT, "SOFT_LIGHT");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeDIFFERENCE() {
        blendModeTestTool(BlendMode.DIFFERENCE, "DIFFERENCE");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeEXCLUSION() {
        blendModeTestTool(BlendMode.EXCLUSION, "EXCLUSION");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeMULTIPLY() {
        blendModeTestTool(BlendMode.MULTIPLY, "MULTIPLY");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeHUE() {
        blendModeTestTool(BlendMode.HUE, "HUE");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeSATURATION() {
        blendModeTestTool(BlendMode.SATURATION, "SATURATION");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeCOLOR() {
        blendModeTestTool(BlendMode.COLOR, "COLOR");
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    public void testBlendModeLUMINOSITY() {
        blendModeTestTool(BlendMode.LUMINOSITY, "LUMINOSITY");
    }

    @Test
    public void testPaintTextures() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        Callback cb =
                rc -> {
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL);
                    float cy = rc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL);
                    float rad = rc.floatExpression(cx, cy, MIN);

                    Bitmap[] bitmap = new Bitmap[4];
                    int[] texture = new int[4];
                    int[] dimX = {32, 64, 32, 16};
                    int[] dimY = {64, 16, 64, 16};

                    for (int i = 0; i < bitmap.length; i++) {
                        bitmap[i] = Bitmap.createBitmap(dimX[i], dimY[i], Bitmap.Config.ARGB_8888);
                        int[] color = new int[dimX[i] * dimY[i]];
                        for (int k = 0; k < color.length; k++) {

                            color[k] =
                                    (k % dimX[i] >= k / dimX[i])
                                            ? (0xFF000000 | 0x10101 * i * 85)
                                            : (0xFF000000 | (0xFF << (8 * i)));
                        }
                        bitmap[i].eraseColor(0xFFFFFFFF);
                        bitmap[i].setPixels(color, 0, dimX[i], 0, 0, dimX[i], dimY[i]);
                        texture[i] = rc.addBitmap(bitmap[i]);
                    }

                    rc.getPainter()
                            .setTextureShader(
                                    texture[0],
                                    Rc.Texture.TILE_MIRROR,
                                    Rc.Texture.TILE_REPEAT,
                                    Rc.Texture.FILTER_DEFAULT,
                                    (short) 0)
                            .commit();
                    rc.drawCircle(cx, cy, rad);

                    float sec = rc.floatExpression(25, 60, MOD, 6, MUL);
                    float min = rc.floatExpression(49, 60, MOD, 6, MUL);
                    float hour = rc.floatExpression(2, 24, MOD, 360 / 24f, MUL);

                    float hrWidth = rc.floatExpression(rad, 30, DIV);
                    float hrLength = rc.floatExpression(rad, 2, DIV);
                    float hrL = rc.floatExpression(cx, hrWidth, SUB);
                    float hrR = rc.floatExpression(cx, hrWidth, ADD);
                    float hrT = rc.floatExpression(cy, hrLength, SUB);
                    float hrB = rc.floatExpression(cy, 2, ADD);
                    rc.getPainter()
                            .setTextureShader(
                                    texture[1],
                                    Rc.Texture.TILE_MIRROR,
                                    Rc.Texture.TILE_MIRROR,
                                    (short) 0,
                                    (short) 0)
                            .commit();

                    rc.save();
                    rc.rotate(hour, cx, cy);
                    rc.drawRoundRect(hrL, hrT, hrR, hrB, 30, 30);
                    rc.restore();

                    float minWidth = rc.floatExpression(rad, 30, DIV);
                    float minLength = rc.floatExpression(rad, 0.8f, MUL);
                    float minL = rc.floatExpression(cx, minWidth, SUB);
                    float minR = rc.floatExpression(cx, minWidth, ADD);
                    float minT = rc.floatExpression(cy, minLength, SUB);
                    float minB = rc.floatExpression(cy, 2, ADD);
                    rc.getPainter()
                            .setTextureShader(
                                    texture[2],
                                    Rc.Texture.TILE_REPEAT,
                                    Rc.Texture.TILE_REPEAT,
                                    (short) 0,
                                    (short) 0)
                            .commit();
                    rc.save();
                    rc.rotate(min, cx, cy);
                    rc.drawRoundRect(minL, minT, minR, minB, 30, 30);
                    rc.restore();
                    rc.getPainter().setShader(0).setColor(Color.GRAY).commit();
                    rc.drawCircle(cx, cy, 60);
                    rc.getPainter()
                            .setTextureShader(
                                    texture[3],
                                    (short) 1,
                                    (short) 1,
                                    Rc.Texture.FILTER_NEAREST,
                                    (short) 0)
                            .commit();
                    float secWidth = rc.floatExpression(rad, 10, DIV);
                    float secLength = rc.floatExpression(rad, 0.8f, MUL);
                    float secL = rc.floatExpression(cx, secWidth, SUB);
                    float secR = rc.floatExpression(cx, secWidth, ADD);
                    float secT = rc.floatExpression(cy, secLength, SUB);
                    float secB = rc.floatExpression(cy, 2, ADD);

                    rc.save();
                    rc.rotate(sec, cx, cy);
                    rc.drawRoundRect(secL, secT, secR, secB, 30, 30);
                    rc.restore();
                };
        String result = drawCommandList(cb);
        result = TestUtils.grep(result, ".*texture.*");
        System.out.println(result);
        String expected =
                "    texture( [48] 1, 2, 0, 0),\n"
                        + "    texture( [49] 1, 1, 0, 0),\n"
                        + "    texture( [50] 2, 2, 0, 0),\n"
                        + "    texture( [51] 1, 1, 1, 0),\n";

        if (TestUtils.diff(expected, result)) {
            Log.v("TEST", result);
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equal", expected, result);
    }
}
