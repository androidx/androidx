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

import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

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

/** This test the function call syntax */
@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class FunctionTest {
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
    public void testFunctionCall1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        for (int i = 0; i < 9; i++) {
            canvas.save();
            canvas.translate(12.4f + i * 30, 32.f + i * 12f + i * i);
            canvas.rotate(i * 5f);
            canvas.drawOval(-50f, -10f, 50f, 10f, paint);
            canvas.restore();
        }
        paint.setColor(Color.BLUE);
        for (int i = 9; i < 18; i++) {
            canvas.save();
            canvas.translate(12.4f + i * 30, 32.f + i * 12f + i * i);
            canvas.rotate(i * 5f);
            canvas.drawOval(-50f, -10f, 50f, 10f, paint);
            canvas.restore();
        }

        paint.setColor(0x550000ff);
        canvas.drawOval(0, 200, tw, th, paint);

        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    float[] args = new float[3];
                    int id = rdoc.createFloatFunction(args);
                    {
                        float x = args[0], y = args[1];
                        float angle = args[2];
                        rdoc.save();
                        rdoc.translate(x, y);
                        rdoc.rotate(angle);
                        rdoc.drawOval(-50f, -10f, 50f, 10f);
                        rdoc.restore();
                    }
                    rdoc.endFloatFunction();

                    rdoc.getPainter().setColor(Color.RED).commit();
                    for (int i = 0; i < 9; i++) {
                        rdoc.callFloatFunction(id, 12.4f + i * 30, 32.f + i * 12f + i * i, i * 5f);
                    }
                    rdoc.getPainter().setColor(Color.BLUE).commit();
                    for (int i = 9; i < 18; i++) {
                        rdoc.callFloatFunction(id, 12.4f + i * 30, 32.f + i * 12f + i * i, i * 5f);
                    }

                    rdoc.endCanvas();
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
                        + "FloatFunctionDefine[44] ([45] [46] [47] )\n"
                        + "  MatrixSave;\n"
                        + "  MatrixTranslate [45] [46]\n"
                        + "  MatrixRotate [47] NaN NaN\n"
                        + "  DrawOval -50.0 -10.0 50.0 10.0\n"
                        + "  MatrixRestore\n"
                        + "PaintData \"\n"
                        + "    Color(0xffff0000),\n"
                        + "\"\n"
                        + "callFunction[44] 12.4 ,32.0 ,0.0\n"
                        + "callFunction[44] 42.4 ,45.0 ,5.0\n"
                        + "callFunction[44] 72.4 ,60.0 ,10.0\n"
                        + "callFunction[44] 102.4 ,77.0 ,15.0\n"
                        + "callFunction[44] 132.4 ,96.0 ,20.0\n"
                        + "callFunction[44] 162.4 ,117.0 ,25.0\n"
                        + "callFunction[44] 192.4 ,140.0 ,30.0\n"
                        + "callFunction[44] 222.4 ,165.0 ,35.0\n"
                        + "callFunction[44] 252.4 ,192.0 ,40.0\n"
                        + "PaintData \"\n"
                        + "    Color(0xff0000ff),\n"
                        + "\"\n"
                        + "callFunction[44] 282.4 ,221.0 ,45.0\n"
                        + "callFunction[44] 312.4 ,252.0 ,50.0\n"
                        + "callFunction[44] 342.4 ,285.0 ,55.0\n"
                        + "callFunction[44] 372.4 ,320.0 ,60.0\n"
                        + "callFunction[44] 402.4 ,357.0 ,65.0\n"
                        + "callFunction[44] 432.4 ,396.0 ,70.0\n"
                        + "callFunction[44] 462.4 ,437.0 ,75.0\n"
                        + "callFunction[44] 492.4 ,480.0 ,80.0\n"
                        + "callFunction[44] 522.4 ,525.0 ,85.0\n"
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

        // assertFalse("not equals", TestUtils.diff(expected, result));

        // RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);
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
