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

import static androidx.compose.remote.creation.RemoteComposeWriter.L_ADD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class TestOVariable {
    private static final boolean SAVE_IMAGES = false;
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

    static String getMethodName() {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    private void checkResults(
            Context appContext, Bitmap blankBitmap, Bitmap remoteBitmap, Bitmap localBitmap) {
        if (!SAVE_IMAGES) {
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

    String drawCommandTest(TestUtils.Callback run) {
        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.setHideString(false);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, run);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        return debugContext.getTestResults();
    }

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

        // ================= SETUP ===================
        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    rdoc.addInteger(32);
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
                        + "IntegerConstant[44] = 32\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 [43]200.0 600.0 600.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);

        // RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Test
    public void testIntegerConstant1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);

        // ================= SETUP ===================
        TestUtils.Callback cb =
                rdoc -> {
                    Float fid = rdoc.addFloatConstant(200);
                    long id = rdoc.addInteger(100);
                    long id2 = rdoc.integerExpression(4, 5, 7, IntegerExpressionEvaluator.I_ADD);
                    long id3 = rdoc.integerExpression(5, 7, L_ADD, id, L_ADD);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    int txtId = rdoc.createTextFromFloat(rdoc.asFloatId(id2), 2, 0, 0);
                    rdoc.drawTextRun(txtId, 0, -1, 300, 300, 0, 1, false);
                    rdoc.drawOval(0, fid, rdoc.asFloatId(id), th);
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
                        + "IntegerConstant[44] = 100\n"
                        + "IntegerExpression[45] = (5 7 +)\n"
                        + "IntegerExpression[46] = (5 7 + [44] +)\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "TextFromFloat[47] = [45] 2.0 0\n"
                        + "DrawTextRun [47] 0, -1, 0.0, 1.0\n"
                        + "DrawOval 0.0 [43]200.0 [44]100.0 600.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);
        debugContext.clearResults();
        debugContext.setHideString(false);
        doc.paint(debugContext, Theme.LIGHT);
        System.out.println(debugContext.getTestResults());
    }
}
