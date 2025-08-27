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
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class DrawPathTweenTest {
    private final boolean mSaveImages = false;

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
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, use);

        doc.paint(debugContext, Theme.UNSPECIFIED);
        String result = debugContext.getTestResults();
        String paintStr = "paintData \"\n" + "    AntiAlias(0),\n" + "\"\n";
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

    // ########################### END TEST UTILS ######################################

    @Test
    public void testTweenTest1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Path path1 = new Path();
        path1.reset();
        float shift = 0;
        path1.moveTo(shift + 10, th / 2);
        path1.lineTo(shift + 30, 10);
        path1.lineTo(shift + 30, th / 2);
        path1.close();
        Path path2 = new Path();
        path2.reset();
        shift = 500;
        path2.moveTo(shift + 10, th / 2);
        path2.lineTo(shift + 30, 10);
        path2.lineTo(shift + 30, th / 2);
        path2.close();
        Path path3 = new Path();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        canvas.drawPath(path1, paint);
        paint.setColor(Color.RED);
        canvas.drawPath(path2, paint);
        paint.setColor(0x550000ff);
        for (float i = 0.2f; i < 1.0f; i += 0.2f) {
            path3.reset();
            shift = 500 * i;
            path3.moveTo(shift + 10, th / 2);
            path3.lineTo(shift + 30, 10);
            path3.lineTo(shift + 30, th / 2);
            path3.close();
            canvas.drawPath(path3, paint);
        }

        TestUtils.Callback cb =
                rdoc -> {
                    int id1 = rdoc.addPathData(path1);
                    int id2 = rdoc.addPathData(path2);
                    rdoc.getPainter().setColor(Color.GREEN).commit();
                    rdoc.drawPath(id1);
                    rdoc.getPainter().setColor(Color.RED).commit();
                    rdoc.drawPath(id2);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    for (float i = 0.2f; i < 1.0f; i += 0.2f) {
                        rdoc.drawTweenPath(id1, id2, i, 0f, 1f);
                    }
                };
        String result = drawCommandTest(cb);
        System.out.println(result);
        String expected =
                "header(1, 1, 0) 600 x 600, 0\n"
                    + "loadText(42)=\"Demo\"\n"
                    + "loadPathData(43)=M 10.0 300.0 L 10.0 300.0 30.0 10.0 L 30.0 10.0 30.0 300.0"
                    + " Z\n"
                    + "loadPathData(44)=M 510.0 300.0 L 510.0 300.0 530.0 10.0 L 530.0 10.0 530.0"
                    + " 300.0 Z\n"
                    + "setTheme(-1)\n"
                    + "header(1, 1, 0) 600 x 600, 0\n"
                    + "loadText(42)=\"Demo\"\n"
                    + "loadPathData(43)=M 10.0 300.0 L 10.0 300.0 30.0 10.0 L 30.0 10.0 30.0 300.0"
                    + " Z\n"
                    + "loadPathData(44)=M 510.0 300.0 L 510.0 300.0 530.0 10.0 L 530.0 10.0 530.0"
                    + " 300.0 Z\n"
                    + "paintData(\n"
                    + "    Color(0xff00ff00),\n"
                    + ")\n"
                    + "drawPath(43, 0.0, 1.0)\n"
                    + "paintData(\n"
                    + "    Color(0xffff0000),\n"
                    + ")\n"
                    + "drawPath(44, 0.0, 1.0)\n"
                    + "paintData(\n"
                    + "    Color(0x550000ff),\n"
                    + ")\n"
                    + "drawTweenPath(43, 44, 0.2, 0.0, 1.0)\n"
                    + "drawTweenPath(43, 44, 0.4, 0.0, 1.0)\n"
                    + "drawTweenPath(43, 44, 0.6, 0.0, 1.0)\n"
                    + "drawTweenPath(43, 44, 0.8, 0.0, 1.0)\n";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        // assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        if (mSaveImages) {

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
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void testTweenTest2() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Path path1 = new Path();
        path1.reset();
        path1.moveTo(10, th / 2);
        path1.quadTo(10, 10, tw - 10, th / 2);
        path1.close();
        Path path2 = new Path();
        path2.reset();
        path2.moveTo(10, th / 2);
        path2.quadTo(tw - 10, 10, tw - 10, th / 2);
        path2.close();
        Path path3 = new Path();
        path3.reset();
        path3.moveTo(10, th / 2);
        path3.quadTo(tw / 2, 10, tw - 10, th / 2);
        path3.close();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        canvas.drawPath(path1, paint);
        paint.setColor(Color.RED);
        canvas.drawPath(path2, paint);
        paint.setColor(0x550000ff);
        canvas.drawPath(path3, paint);
        TestUtils.Callback cb =
                rdoc -> {
                    int id1 = rdoc.addPathData(path1);
                    int id2 = rdoc.addPathData(path2);
                    rdoc.getPainter().setColor(Color.GREEN).commit();
                    rdoc.drawPath(id1);
                    rdoc.getPainter().setColor(Color.RED).commit();
                    rdoc.drawPath(id2);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTweenPath(id1, id2, 0.5f, 0f, 1f);
                };
        String result = drawCommandList(cb);

        Log.v("TEST", result);
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "PathData[43] = \"M 10.0 300.0 Q 10.0 300.0 10.0 10.0 590.0 300.0 Z\"\n"
                        + "PathData[44] = \"M 10.0 300.0 Q 10.0 300.0 590.0 10.0 590.0 300.0 Z\"\n"
                        + "PaintData \"\n"
                        + "    Color(0xff00ff00),\n"
                        + "\"\n"
                        + "DrawPath [43], 0.0, 1.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xffff0000),\n"
                        + "\"\n"
                        + "DrawPath [44], 0.0, 1.0\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "DrawTweenPath 43 44 0.5 0.0 - 1.0\n"
                        + "}";

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        TestUtils.captureGold("pathTweenTest2", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        if (mSaveImages) {

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
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
