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
import static androidx.compose.remote.player.view.TestUtils.diff;
import static androidx.compose.remote.player.view.TestUtils.dumpDifference;
import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;

@RunWith(JUnit4.class)
public class DrawTextAnchoredTest {

    private final Platform mPlatform = new AndroidxPlatformServices();
    private boolean mSaveImages = false;

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

    @Ignore("Flaky Test")
    @Test
    public void testDrawTextAnchored01() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        float textSize = 50f;
        float recLeft = 300 - textSize * 5;
        float recTop = 300 - textSize / 2;
        float recRight = 300 + textSize * 5;
        float recBottom = 300 + textSize / 2;
        float centerX = (recLeft + recRight) / 2;
        float centerY = (recTop + recBottom) / 2;

        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(str, 0, str.length(), bounds);
        float width = bounds.width();
        float height = bounds.height();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(recLeft, recTop, recRight, recBottom, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText(
                str,
                centerX - width / 2 - bounds.left,
                centerY + height / 2 - bounds.bottom,
                paint);

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(50f).commit();
                    rdoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.FILL).commit();
                    rdoc.drawRect(recLeft, recTop, recRight, recBottom);
                    rdoc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.FILL).commit();
                    rdoc.drawTextAnchored(str, centerX, centerY, 0f, 0f, 0);
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
                        + "\"\n"
                        + "PaintData \"\n"
                        + "    Color(0xffff0000),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "DrawRect 50.0 275.0 550.0 325.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffffffff),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextAnchored [43] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (!mSaveImages) {
            saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        TestUtils.captureGold("DrawText", doc, appContext);

        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Ignore("Flaky Test")
    @Test
    public void testDrawTextAnchored02() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        float textSize = 50f;
        float recLeft = 300 - textSize * 5;
        float recTop = 300 - textSize / 2;
        float recRight = 300 + textSize * 5;
        float recBottom = 300 + textSize / 2;
        float centerX = (recLeft + recRight) / 2;
        float centerY = (recTop + recBottom) / 2;
        float[] dx = {-1, 0, 1, -1, 0, 1, -1, 0, 1};
        float[] dy = {-1, -1, -1, 0, 0, 0, 1, 1, 1};
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(str, 0, str.length(), bounds);
        float width = bounds.width();
        float height = bounds.height();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(recLeft, recTop, recRight, recBottom, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText(
                str,
                centerX - width / 2 - bounds.left,
                centerY + height / 2 - bounds.bottom,
                paint);
        {
            float[] hsv = {0.5f, 1f, 0f};
            float x = centerX;
            float y = 60;
            for (int i = 0; i < dx.length; i++) {
                float deltaX = (dx[i] + 1) * width / 2;
                float deltaY = (dy[i] + 1) * height / 2;
                hsv[2] += 0.1f;
                paint.setColor(Color.HSVToColor(hsv));

                canvas.drawText(str, x - deltaX - bounds.left, y + deltaY - bounds.bottom, paint);

                paint.setColor(Color.GREEN);
                canvas.drawRect(x - 2, y - 2, x + 2, y + 2, paint);
                y += 60;
            }
        }

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(50f).commit();
                    rdoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.FILL).commit();
                    rdoc.drawRect(recLeft, recTop, recRight, recBottom);
                    rdoc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.FILL).commit();
                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextAnchored(strId, centerX, centerY, 0f, 0f, 0);
                    float[] hsv = {0.5f, 1f, 0f};
                    float x = centerX;
                    float y = 60;
                    for (int i = 0; i < dx.length; i++) {
                        hsv[2] += 0.1;
                        rdoc.getPainter().setColor(Color.HSVToColor(hsv)).commit();
                        rdoc.drawTextAnchored(strId, x, y, dx[i], dy[i], 0);
                        rdoc.getPainter().setColor(Color.GREEN).commit();
                        rdoc.drawRect(x - 2, y - 2, x + 2, y + 2);
                        y += 60;
                    }
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
                        + "\"\n"
                        + "PaintData \"\n"
                        + "    Color(0xffff0000),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "DrawRect 50.0 275.0 550.0 325.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffffffff),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "TextData[43] = \"hello w...\"\n"
                        + "DrawTextAnchored [43] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff1a0000),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 60.0, -1.0, -1.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 58.0 302.0 62.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff330000),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 120.0, 0.0, -1.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 118.0 302.0 122.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff4d0100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 180.0, 1.0, -1.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 178.0 302.0 182.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff660100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 240.0, -1.0, 0.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 238.0 302.0 242.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff800100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 298.0 302.0 302.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff990100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 360.0, 1.0, 0.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 358.0 302.0 362.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffb30100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 420.0, -1.0, 1.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 418.0 302.0 422.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffcc0200),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 480.0, 0.0, 1.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 478.0 302.0 482.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffe60200),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 540.0, 1.0, 1.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 298.0 538.0 302.0 542.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {
            saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        TestUtils.captureGold("DrawText", doc, appContext);

        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Ignore("Flaky Test")
    @Test
    public void testDrawTextAnchored03() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "quality Doggy";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        float textSize = 50f;
        float recLeft = 300 - textSize * 5;
        float recTop = 300 - textSize / 2;
        float recRight = 300 + textSize * 5;
        float recBottom = 300 + textSize / 2;
        float centerX = (recLeft + recRight) / 2;
        float centerY = (recTop + recBottom) / 2;
        float[] dx = {-1, 0, 1, -1, 0, 1, -1, 0, 1};
        float[] dy = {-1, -1, -1, 0, 0, 0, 1, 1, 1};
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(str, 0, str.length(), bounds);
        float width = bounds.width();
        float height = bounds.height();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(recLeft, recTop, recRight, recBottom, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText(
                str,
                centerX - width / 2 - bounds.left,
                centerY + height / 2 - bounds.bottom,
                paint);
        {
            float[] hsv = {0.5f, 1f, 0f};
            float x = centerX;
            float y = 60;
            float offY = height / 2 - bounds.bottom;
            for (int i = 0; i < dx.length; i++) {
                float deltaX = (dx[i] + 1) * width / 2;
                // float deltaY = (dy[i]+1) * height / 2;
                hsv[2] += 0.1f;
                paint.setColor(Color.HSVToColor(hsv));

                canvas.drawText(str, x - deltaX - bounds.left, y + offY, paint);

                paint.setColor(Color.GREEN);
                canvas.drawRect(x - 20, y - 1, x + 20, y + 1, paint);
                y += 60;
            }
        }

        Callback cb =
                rdoc -> {
                    rdoc.getPainter().setTextSize(50f).commit();
                    rdoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.FILL).commit();
                    rdoc.drawRect(recLeft, recTop, recRight, recBottom);
                    rdoc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.FILL).commit();
                    int strId = rdoc.textCreateId(str);
                    rdoc.drawTextAnchored(strId, centerX, centerY, 0f, 0f, 0);
                    float[] hsv = {0.5f, 1f, 0f};
                    float x = centerX;
                    float y = 60;
                    for (int i = 0; i < dx.length; i++) {
                        hsv[2] += 0.1;
                        rdoc.getPainter().setColor(Color.HSVToColor(hsv)).commit();
                        rdoc.drawTextAnchored(strId, x, y, dx[i], Float.NaN, 0);
                        rdoc.getPainter().setColor(Color.GREEN).commit();
                        rdoc.drawRect(x - 20, y - 1, x + 20, y + 1);
                        y += 60;
                    }
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
                        + "\"\n"
                        + "PaintData \"\n"
                        + "    Color(0xffff0000),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "DrawRect 50.0 275.0 550.0 325.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffffffff),\n"
                        + "    Style(0),\n"
                        + "\"\n"
                        + "TextData[43] = \"quality...\"\n"
                        + "DrawTextAnchored [43] 300.0, 300.0, 0.0, 0.0, 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff1a0000),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 60.0, -1.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 59.0 320.0 61.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff330000),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 120.0, 0.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 119.0 320.0 121.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff4d0100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 180.0, 1.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 179.0 320.0 181.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff660100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 240.0, -1.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 239.0 320.0 241.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff800100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 300.0, 0.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 299.0 320.0 301.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff990100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 360.0, 1.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 359.0 320.0 361.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffb30100),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 420.0, -1.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 419.0 320.0 421.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffcc0200),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 480.0, 0.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 479.0 320.0 481.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffe60200),\n"
                        + "\"\n"
                        + "DrawTextAnchored [43] 300.0, 540.0, 1.0, [0], 0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawRect 280.0 539.0 320.0 541.0\n"
                        + "}";

        if (diff(expected, result)) {
            dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = createDocument(debugContext, cb);

        rc_player.setDocument(doc);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = blank(tw, th);

        if (mSaveImages) {
            saveBitmap(appContext, remoteBitmap, "remoteBitmap.png");
            saveBitmap(appContext, localBitmap, "localBitmap.png");
        }
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        TestUtils.captureGold("DrawText", doc, appContext);

        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
