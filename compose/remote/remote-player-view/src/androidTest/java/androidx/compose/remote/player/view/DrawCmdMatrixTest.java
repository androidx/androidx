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

import static androidx.compose.remote.player.view.TestUtils.blank;
import static androidx.compose.remote.player.view.TestUtils.compareImages;
import static androidx.compose.remote.player.view.TestUtils.createDocument;
import static androidx.compose.remote.player.view.TestUtils.diff;
import static androidx.compose.remote.player.view.TestUtils.dumpDifference;
import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.creation.Painter;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DrawCmdMatrixTest {
    private final boolean mSaveImages = false;

    // ########################### TEST UTILS ######################################

    String drawCommandTest(TestUtils.Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);
        RemoteComposeDocument doc = createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

    String drawCommandList(TestUtils.Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, run);
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
        RemoteComposeDocument doc = createDocument(debugContext, use);

        doc.paint(debugContext, Theme.UNSPECIFIED);
        String result = debugContext.getTestResults();
        String paintStr = "paintData(\n" + "    AntiAlias(0),\n" + "\"\n";
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
        if (diff(result, expectedResult)) { // they are different expect issues
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

    // ########################### END TEST UTILS ######################################

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testSkew() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        canvas.save();
        canvas.skew(.5f, 0f);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);
        canvas.restore();
        canvas.drawText(str, 20, th / 2 + 20, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.save();
                    rdoc.skew(.5f, 0f);
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    int id = rdoc.textCreateId(str);
                    rdoc.drawTextRun(id, 0, str.length(), 0, 0, 20, th / 2, false);

                    rdoc.restore();
                    rdoc.drawTextRun(id, 0, str.length(), 0, 0, 20, th / 2 + 20, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "MatrixSave;\n"
                        + "MatrixSkew 0.5 0.0\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace("
                        + PaintBundle.FONT_TYPE_SANS_SERIF
                        + ", 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "MatrixRestore\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 320.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        TestUtils.captureGold("MatrixSkew", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testScale1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        canvas.save();
        canvas.scale(1.5f, 1f);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);
        canvas.restore();
        canvas.drawText(str, 20, th / 2 + 20, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.save();
                    rdoc.scale(1.5f, 1f);
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2, false);

                    rdoc.restore();
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2 + 20, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "MatrixSave;\n"
                        + "MatrixScale 1.5 1.0 NaN NaN\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "MatrixRestore\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 320.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);
        RemoteComposeDocument doc = createDocument(debugContext, cb);
        TestUtils.captureGold("MatrixScale1", doc, appContext);
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (mSaveImages) {
                String methodName = TestUtils.getMethodName();
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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testScale2() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        canvas.save();
        canvas.scale(0.5f, 1.5f, tw / 2.0f, tw / 3.0f);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);
        canvas.restore();
        canvas.drawText(str, 20, th / 2 + 20, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.save();
                    rdoc.scale(0.5f, 1.5f, tw / 2.0f, tw / 3.0f);
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();

                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2, false);

                    rdoc.restore();
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2 + 20, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "MatrixSave;\n"
                        + "MatrixScale 0.5 1.5 300.0 200.0\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "MatrixRestore\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 320.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);
        RemoteComposeDocument doc = createDocument(debugContext, cb);
        TestUtils.captureGold("MatrixScale2", doc, appContext);
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testTranslate() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        canvas.save();
        canvas.translate(200.5f, -100.5f);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);
        canvas.restore();
        canvas.drawText(str, 20, th / 2 + 20, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.save();
                    rdoc.translate(200.5f, -100.5f);
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2, false);

                    rdoc.restore();
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2 + 20, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "MatrixSave;\n"
                        + "MatrixTranslate 200.5 -100.5\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "MatrixRestore\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 320.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        TestUtils.captureGold("MatrixTranslate", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testRotate1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        canvas.save();
        canvas.rotate(20f, tw / 2.0f, th / 2.0f);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);
        canvas.restore();
        canvas.drawText(str, 20, th / 2 + 20, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.save();
                    rdoc.rotate(20f, tw / 2.0f, th / 2.0f);
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2, false);

                    rdoc.restore();
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2 + 20, false);
                };
        String result = drawCommandList(cb);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "MatrixSave;\n"
                        + "MatrixRotate 20.0 300.0 300.0\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "MatrixRestore\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 320.0\n"
                        + "}";

        if (diff(expected, result)) {
            Log.v("TEST", result);
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        TestUtils.captureGold("MatrixRotate1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
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

    @SdkSuppress(minSdkVersion = 28)
    @Ignore("Flaky Test")
    @Test
    public void testRotate2() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface tmp = Typeface.SANS_SERIF;
        Typeface tf = Typeface.create(tmp, 800, true);
        canvas.save();
        canvas.rotate(20f);
        paint.setTextSize(50f);
        paint.setTypeface(tf);
        canvas.drawText(str, 20, th / 2, paint);
        canvas.restore();
        canvas.drawText(str, 20, th / 2 + 20, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    rdoc.save();
                    rdoc.rotate(20f);
                    rdoc.getPainter()
                            .setTextSize(50f)
                            .setTypeface(Painter.FONT_TYPE_SANS_SERIF, 800, true)
                            .commit();
                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2, false);

                    rdoc.restore();
                    rdoc.drawTextRun(strId, 0, str.length(), 0, 0, 20, th / 2 + 20, false);
                };
        String result = drawCommandList(cb);
        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "MatrixSave;\n"
                        + "MatrixRotate 20.0 NaN NaN\n"
                        + "PaintData \"\n"
                        + "    TextSize(50.0),\n"
                        + "    TypeFace(1, 800, true),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 300.0\n"
                        + "MatrixRestore\n"
                        + "DrawTextRun [43] 0, 11, 20.0, 320.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        TestUtils.captureGold("MatrixRotate2", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {

            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = TestUtils.getMethodName();
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
}
