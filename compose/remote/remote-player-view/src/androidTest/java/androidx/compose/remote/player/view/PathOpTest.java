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

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class PathOpTest {
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
    public void pathOpTestIntersection() {

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
        float shift = 200;
        path1.moveTo(shift, 100);
        path1.lineTo(shift + 200, th - 100);
        path1.lineTo(shift - 200, th - 100);
        path1.close();
        Path path2 = new Path();
        path2.reset();
        shift = 400;
        path2.moveTo(shift + 100, 100);
        path2.lineTo(shift + 200, th - 50);
        path2.lineTo(shift - 200, th - 50);
        path2.close();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x8800ff00);
        canvas.drawPath(path1, paint);
        paint.setColor(0x88ff0000);
        canvas.drawPath(path2, paint);
        paint.setColor(0x550000ff);

        Path path3 = new Path(path1);
        path3.op(path2, Path.Op.INTERSECT);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawPath(path3, paint);

        paint.setColor(0x55ffff00);
        Path path4 = new Path(path1);
        path4.op(path2, Path.Op.XOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawPath(path4, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    int id1 = rdoc.addPathData(path1);
                    int id2 = rdoc.addPathData(path2);
                    rdoc.getPainter().setColor(0x8800ff00).commit();
                    rdoc.drawPath(id1);
                    rdoc.getPainter().setColor(0x88ff0000).commit();
                    rdoc.drawPath(id2);
                    rdoc.getPainter()
                            .setColor(0x550000ff)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(5f)
                            .commit();
                    int path = rdoc.pathCombine(id1, id2, RemoteComposeWriter.COMBINE_INTERSECT);
                    rdoc.drawPath(path);
                    rdoc.getPainter().setColor(0x55ffff00).commit();
                    rdoc.drawPath(rdoc.pathCombine(id1, id2, RemoteComposeWriter.COMBINE_XOR));
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        String result = doc.toString();
        String expected =
                "Document{\n"
                    + "HEADER v1.1.0, 600 x 600 [0]\n"
                    + "TextData[42] = \"Demo\"\n"
                    + "RootContentDescription 42\n"
                    + "PathData[43] = \"M 200.0 100.0 L 200.0 100.0 400.0 500.0 L 400.0 500.0 0.0"
                    + " 500.0 Z\"\n"
                    + "PathData[44] = \"M 500.0 100.0 L 500.0 100.0 600.0 550.0 L 600.0 550.0 200.0"
                    + " 550.0 Z\"\n"
                    + "PaintData \"\n"
                    + "    Color(0x8800ff00),\n"
                    + "\"\n"
                    + "DrawPath [43], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x88ff0000),\n"
                    + "\"\n"
                    + "DrawPath [44], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x550000ff),\n"
                    + "    Style(1),\n"
                    + "    StrokeWidth(5.0),\n"
                    + "\"\n"
                    + "PathCombine[45] = [43 ] + [ 44], 1\n"
                    + "DrawPath [45], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x55ffff00),\n"
                    + "\"\n"
                    + "PathCombine[46] = [43 ] + [ 44], 4\n"
                    + "DrawPath [46], 0.0, 1.0\n"
                    + "}";

        System.err.println(result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        } else {
            System.out.println("not diff");
        }
        assertEquals("not equals", expected, result);

        doc = TestUtils.createDocument(debugContext, cb);

        // TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        if (!mSaveImages) {
            System.out.println(
                    "####################### SAVING IMAGES #################################");
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
            System.out.println(
                    "####################### DONE SAVING IMAGES #################################");
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void pathOpTestDifference() {

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
        float shift = 200;
        path1.moveTo(shift, 100);
        path1.lineTo(shift + 200, th - 100);
        path1.lineTo(shift - 200, th - 100);
        path1.close();
        Path path2 = new Path();
        path2.reset();
        shift = 400;
        path2.moveTo(shift + 100, 100);
        path2.lineTo(shift + 200, th - 50);
        path2.lineTo(shift - 200, th - 50);
        path2.close();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x8800ff00);
        canvas.drawPath(path1, paint);
        paint.setColor(0x88ff0000);
        canvas.drawPath(path2, paint);
        paint.setColor(0x550000ff);

        Path path3 = new Path(path1);
        path3.op(path2, Path.Op.DIFFERENCE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawPath(path3, paint);

        paint.setColor(0x55ffff00);
        Path path4 = new Path(path1);
        path4.op(path2, Path.Op.REVERSE_DIFFERENCE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawPath(path4, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    int id1 = rdoc.addPathData(path1);
                    int id2 = rdoc.addPathData(path2);
                    rdoc.getPainter().setColor(0x8800ff00).commit();
                    rdoc.drawPath(id1);
                    rdoc.getPainter().setColor(0x88ff0000).commit();
                    rdoc.drawPath(id2);
                    rdoc.getPainter()
                            .setColor(0x550000ff)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(5f)
                            .commit();
                    int path = rdoc.pathCombine(id1, id2, RemoteComposeWriter.COMBINE_DIFFERENCE);
                    rdoc.drawPath(path);
                    rdoc.getPainter().setColor(0x55ffff00).commit();
                    rdoc.drawPath(
                            rdoc.pathCombine(
                                    id1, id2, RemoteComposeWriter.COMBINE_REVERSE_DIFFERENCE));
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        String result = doc.toString();
        String expected =
                "Document{\n"
                    + "HEADER v1.1.0, 600 x 600 [0]\n"
                    + "TextData[42] = \"Demo\"\n"
                    + "RootContentDescription 42\n"
                    + "PathData[43] = \"M 200.0 100.0 L 200.0 100.0 400.0 500.0 L 400.0 500.0 0.0"
                    + " 500.0 Z\"\n"
                    + "PathData[44] = \"M 500.0 100.0 L 500.0 100.0 600.0 550.0 L 600.0 550.0 200.0"
                    + " 550.0 Z\"\n"
                    + "PaintData \"\n"
                    + "    Color(0x8800ff00),\n"
                    + "\"\n"
                    + "DrawPath [43], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x88ff0000),\n"
                    + "\"\n"
                    + "DrawPath [44], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x550000ff),\n"
                    + "    Style(1),\n"
                    + "    StrokeWidth(5.0),\n"
                    + "\"\n"
                    + "PathCombine[45] = [43 ] + [ 44], 0\n"
                    + "DrawPath [45], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x55ffff00),\n"
                    + "\"\n"
                    + "PathCombine[46] = [43 ] + [ 44], 2\n"
                    + "DrawPath [46], 0.0, 1.0\n"
                    + "}";

        System.err.println(result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        } else {
            System.out.println("not diff");
        }
        assertEquals("not equals", expected, result);

        doc = TestUtils.createDocument(debugContext, cb);

        // TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        if (!mSaveImages) {
            System.out.println(
                    "####################### SAVING IMAGES #################################");
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
            System.out.println(
                    "####################### DONE SAVING IMAGES #################################");
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }

    @Test
    public void pathOpTestUnion() {

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
        float shift = 200;
        path1.moveTo(shift, 100);
        path1.lineTo(shift + 200, th - 100);
        path1.lineTo(shift - 200, th - 100);
        path1.close();
        Path path2 = new Path();
        path2.reset();
        shift = 400;
        path2.moveTo(shift + 100, 100);
        path2.lineTo(shift + 200, th - 50);
        path2.lineTo(shift - 200, th - 50);
        path2.close();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x8800ff00);
        canvas.drawPath(path1, paint);
        paint.setColor(0x88ff0000);
        canvas.drawPath(path2, paint);
        paint.setColor(0x550000ff);

        Path path3 = new Path(path1);
        path3.op(path2, Path.Op.UNION);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawPath(path3, paint);

        paint.setColor(0x55ffff00);
        Path path4 = new Path(path1);
        path4.op(path2, Path.Op.XOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawPath(path4, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    int id1 = rdoc.addPathData(path1);
                    int id2 = rdoc.addPathData(path2);
                    rdoc.getPainter().setColor(0x8800ff00).commit();
                    rdoc.drawPath(id1);
                    rdoc.getPainter().setColor(0x88ff0000).commit();
                    rdoc.drawPath(id2);
                    rdoc.getPainter()
                            .setColor(0x550000ff)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(5f)
                            .commit();
                    int path = rdoc.pathCombine(id1, id2, RemoteComposeWriter.COMBINE_UNION);
                    rdoc.drawPath(path);
                    rdoc.getPainter().setColor(0x55ffff00).commit();
                    rdoc.drawPath(rdoc.pathCombine(id1, id2, RemoteComposeWriter.COMBINE_XOR));
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        String result = doc.toString();
        String expected =
                "Document{\n"
                    + "HEADER v1.1.0, 600 x 600 [0]\n"
                    + "TextData[42] = \"Demo\"\n"
                    + "RootContentDescription 42\n"
                    + "PathData[43] = \"M 200.0 100.0 L 200.0 100.0 400.0 500.0 L 400.0 500.0 0.0"
                    + " 500.0 Z\"\n"
                    + "PathData[44] = \"M 500.0 100.0 L 500.0 100.0 600.0 550.0 L 600.0 550.0 200.0"
                    + " 550.0 Z\"\n"
                    + "PaintData \"\n"
                    + "    Color(0x8800ff00),\n"
                    + "\"\n"
                    + "DrawPath [43], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x88ff0000),\n"
                    + "\"\n"
                    + "DrawPath [44], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x550000ff),\n"
                    + "    Style(1),\n"
                    + "    StrokeWidth(5.0),\n"
                    + "\"\n"
                    + "PathCombine[45] = [43 ] + [ 44], 3\n"
                    + "DrawPath [45], 0.0, 1.0\n"
                    + "PaintData \"\n"
                    + "    Color(0x55ffff00),\n"
                    + "\"\n"
                    + "PathCombine[46] = [43 ] + [ 44], 4\n"
                    + "DrawPath [46], 0.0, 1.0\n"
                    + "}";

        System.err.println(result);
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        } else {
            System.out.println("not diff");
        }
        assertEquals("not equals", expected, result);

        doc = TestUtils.createDocument(debugContext, cb);

        // TestUtils.captureGold("pathTweenTest1", doc, appContext);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);

        if (!mSaveImages) {
            System.out.println(
                    "####################### SAVING IMAGES #################################");
            String remoteImageName = "remoteBitmap.png";
            String localImageName = "localBitmap.png";
            if (false) {
                String methodName = getMethodName();
                remoteImageName = methodName + "Remote.png";
                localImageName = methodName + "Local.png";
            }
            TestUtils.saveBitmap(appContext, remoteBitmap, remoteImageName);
            TestUtils.saveBitmap(appContext, localBitmap, localImageName);
            System.out.println(
                    "####################### DONE SAVING IMAGES #################################");
        }
        System.out.println(
                "relative to blank " + TestUtils.compareImages(blankBitmap, remoteBitmap));
        float rms = TestUtils.compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
