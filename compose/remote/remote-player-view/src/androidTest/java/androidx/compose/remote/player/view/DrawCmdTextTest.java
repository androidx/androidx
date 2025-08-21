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
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.Painter;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.File;

@RunWith(JUnit4.class)
public class DrawCmdTextTest {

    private final Platform mPlatform = new AndroidxPlatformServices();
    private final boolean mSaveImages = false;

    // ########################### TEST UTILS ######################################

    interface Callback {
        void run(RemoteComposeContextAndroid foo);
    }

    private RemoteComposeDocument createDocument(RemoteContext context, final Callback cb) {

        RemoteComposeContextAndroid doc =
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
        recreatedDocument.initializeContext(context);
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
        String paintStr = "paintData(\n" + "    AntiAlias(0),\n";
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

    @SdkSuppress(minSdkVersion = 34)
    @Test
    public void testDrawText() {

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
        canvas.drawText(str, 200, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(100f).commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 200, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(100.0),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 200.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testSetTextFont() {

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
        paint.setTypeface(null);
        canvas.drawText(str, 200, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(100f).setTypeface((Typeface) null).commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 200, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(100.0),\n"
                        + "    TypeFace(0, 400, false),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 200.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
        TestUtils.captureGold("TextFont", doc, appContext);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testSetTextFontSerif() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tf = Typeface.SERIF;
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(50f).setTypeface(Typeface.SERIF).commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 20, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(2, 400, false),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("TextFontSerif", doc, appContext);

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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testSetTextFontSanSerif() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tf = Typeface.SANS_SERIF;
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(50f).setTypeface(tf).commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 20, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 400, false),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("TextFontSanSerif", doc, appContext);

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

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testSetTextFontMonospace() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tf = Typeface.MONOSPACE;
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(50f).setTypeface(tf).commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 20, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(3, 400, false),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("TextFontMonospace", doc, appContext);

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

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testSetTextFontSanSerif800() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 20, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("TextFontSanSerif800", doc, appContext);

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

    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void testSetTextFontCustom() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.create("Roboto Flex", Typeface.NORMAL);

        String path = "/system/fonts";
        File file = new File(path);
        File[] ff = file.listFiles();
        if (ff != null) {
            for (int i = 0; i < ff.length; i++) {
                File file1 = ff[i];
                Utils.log(i + "  " + file1.getName());
            }
        }

        Typeface tf = Typeface.create(tmp, 800, true);

        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    rdoc.drawTextRun(str, 0, str.length(), 0, 0, 20, th / 2, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not eaquals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        TestUtils.captureGold("TextFontSanSerif800", doc, appContext);

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
}
