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

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_AVG;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_DEREF;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.creation.RemoteComposeWriter.map;
import static androidx.compose.remote.player.view.TestSerializeUtils.loadFileFromRaw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class DataTest {
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

        TestUtils.Callback cb =
                rdoc -> {
                    float fid = rdoc.addFloatConstant(200);
                    float arrayId = rdoc.addFloatArray(new float[] {1f, 2f, 3f, 4f, 5f, 6f, 7f});
                    System.out.println(
                            "id from add float array "
                                    + Integer.toHexString(
                                            Float.floatToRawIntBits(arrayId) & 0x7FFFFF));
                    float ans1 = rdoc.floatExpression(2, 2, MUL);
                    float ans2 = rdoc.floatExpression(arrayId, 2, 2, MUL, A_DEREF); // id[2*2]
                    int tid = rdoc.createTextFromFloat(ans2, 2, 3, 0);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTextAnchored(tid, tw / 2, th / 2, 0, 0, 0);
                    rdoc.drawOval(0, fid, tw, th);
                };

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_float_constant1);

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

        debugContext.clearResults();
        doc.paint(debugContext, Theme.UNSPECIFIED);
        String s = debugContext.getTestResults();
        System.out.println(45 + " = " + debugContext.getFloat(45) + "");
        System.out.println(46 + " = \"" + debugContext.getText(46) + "\"");
    }

    @Test
    public void testFloatConstant2() {

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
        float[] array = new float[] {1f, 2f, 3f, 4f, 5f, 6f, 7f};
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        sum /= array.length;
        TestUtils.Callback cb =
                rdoc -> {
                    float fid = rdoc.addFloatConstant(200);
                    float arrayId = rdoc.addFloatArray(array);
                    System.out.println(
                            "id from add float array "
                                    + Integer.toHexString(
                                            Float.floatToRawIntBits(arrayId) & 0x7FFFFF));
                    float ans1 = rdoc.floatExpression(2, 2, MUL);
                    float ans2 = rdoc.floatExpression(2, 2, MUL, arrayId, A_AVG); // id[2*2]
                    int tid = rdoc.createTextFromFloat(ans2, 2, 3, 0);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTextAnchored(tid, tw / 2, th / 2, 0, 0, 0);
                    rdoc.drawOval(0, fid, tw, th);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_float_constant2);

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        doc.initializeContext(debugContext);
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);

        debugContext.clearResults();
        doc.paint(debugContext, Theme.UNSPECIFIED);
        String s = debugContext.getTestResults();
        assertEquals("doc sum ", sum, debugContext.getFloat(45), 0.0001);
        System.out.println(45 + " = " + debugContext.getFloat(45) + "");
        System.out.println(46 + " = \"" + debugContext.getText(46) + "\"");
    }

    @Test
    public void testFloatConstant3() {

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
        float[] array = new float[] {1f, 2f, 3f, 4f, 5f, 6f, 7f};
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        sum /= array.length;
        int[] ansId = new int[1];
        TestUtils.Callback cb =
                rdoc -> {
                    float fid = rdoc.addFloatConstant(200);
                    float arrayId = rdoc.addFloatArray(array);
                    System.out.println(
                            "id from add float array "
                                    + Integer.toHexString(
                                            Float.floatToRawIntBits(arrayId) & 0x7FFFFF));
                    float ans1 = rdoc.floatExpression(2, 2, MUL);
                    float ans2 = rdoc.floatExpression(2, 2, MUL, arrayId, A_AVG); // id[2*2]
                    ansId[0] = Utils.idFromNan(ans2);
                    int tid = rdoc.createTextFromFloat(ans2, 2, 3, 0);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTextAnchored(tid, tw / 2, th / 2, 0, 0, 0);
                    rdoc.drawOval(0, fid, tw, th);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_float_constant3);

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);

        // RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        doc.initializeContext(debugContext);
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);

        debugContext.clearResults();

        doc.paint(debugContext, Theme.UNSPECIFIED);
        String s = debugContext.getTestResults();
        assertEquals(sum, debugContext.getFloat(ansId[0]), 0.0001);
        System.out.println(45 + " = " + debugContext.getFloat(45) + "");
        System.out.println(46 + " = \"" + debugContext.getText(46) + "\"");
    }

    @Test
    public void testFloatConstant4() {

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
        String[] strArray = "Quick brown fox jumps over lazy dog".split(" ");
        float[] array = new float[] {1f, 2f, 3f, 4f, 5f, 6f, 7f};
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        sum /= array.length;

        TestUtils.Callback cb =
                rdoc -> {
                    float fid = rdoc.addFloatConstant(200);
                    float arrayId = rdoc.addFloatMap(strArray, array);
                    System.out.println(
                            "id from add float array "
                                    + Integer.toHexString(
                                            Float.floatToRawIntBits(arrayId) & 0x7FFFFF));
                    float ans1 = rdoc.floatExpression(2, 2, MUL);

                    int tid = rdoc.createTextFromFloat(1234, 2, 3, 0);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTextAnchored(tid, tw / 2, th / 2, 0, 0, 0);
                    rdoc.drawOval(0, fid, tw, th);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_float_constant4);

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);

        debugContext.clearResults();
        doc.paint(debugContext, Theme.UNSPECIFIED);
        String s = debugContext.getTestResults();
        System.out.println(45 + " = " + debugContext.getFloat(45) + "");
        System.out.println(46 + " = \"" + debugContext.getText(46) + "\"");
    }

    @Test
    public void testDataContainers1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        Rect bounds = new Rect();
        String str = "hello world";
        String[] strArray = "Quick brown fox jumps over lazy dog".split(" ");

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        float centerX = tw / 2;
        float centerY = th / 2;
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x550000ff);
        paint.getTextBounds(strArray[4], 0, strArray[4].length(), bounds);
        canvas.drawText(
                strArray[4],
                centerX - tw / 2 - bounds.left,
                centerY + th / 2 - bounds.bottom,
                paint);

        int[] ansId = new int[1];
        TestUtils.Callback cb =
                rdoc -> {
                    float arrayId = rdoc.addStringList(strArray);
                    int id = rdoc.textLookup(arrayId, 4f);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTextAnchored(id, tw / 2, th / 2, 0, 0, 0);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_data_containers1);

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Test
    public void testDataContainers2a() {
        testDataContainers2(0);
    }

    @Test
    public void testDataContainers2b() {
        testDataContainers2(1);
    }

    private void testDataContainers2(int n) {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        Rect bounds = new Rect();

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String displayString = (n == 0) ? "--" : Integer.toString(n);
        float centerX = tw / 2;
        float centerY = th / 2;
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x550000ff);
        paint.getTextBounds(displayString, 0, displayString.length(), bounds);
        canvas.drawText(
                displayString,
                centerX - tw / 2 - bounds.left,
                centerY + th / 2 - bounds.bottom,
                paint);

        TestUtils.Callback cb =
                rdoc -> {
                    float variable = rdoc.addFloatConstant(n);
                    float exp = rdoc.floatExpression(0, variable, MAX, 1, MIN);
                    int text = rdoc.createTextFromFloat(variable, 2, 0, 0);
                    float arrayId = rdoc.addStringList(rdoc.textCreateId("--"), text);
                    int id = rdoc.textLookup(arrayId, exp);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    rdoc.drawTextAnchored(id, tw / 2, th / 2, 0, 0, 0);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_data_containers2);
        expected = expected.replaceAll("RVAR", Integer.toString(n));

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    @Test
    public void testDataContainers3() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();
        Rect bounds = new Rect();
        String str = "hello world";
        String[] strArray = "Quick brown fox jumps over lazy dog".split(" ");

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        float centerX = tw / 2;
        float centerY = th / 2;
        Bitmap localBitmap = TestUtils.blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0x550000ff);
        paint.getTextBounds(strArray[4], 0, strArray[4].length(), bounds);
        canvas.drawText(
                strArray[4],
                centerX - tw / 2 - bounds.left,
                centerY + th / 2 - bounds.bottom,
                paint);

        int[] ansId = new int[1];
        TestUtils.Callback cb =
                rdoc -> {
                    float arrayId = rdoc.addStringList(strArray);
                    int id = rdoc.textLookup(arrayId, 4f);
                    rdoc.getPainter().setColor(0x550000ff).commit();
                    int k = rdoc.addColor(0x273734);
                    int mapId =
                            rdoc.addDataMap(
                                    map("str", "test"),
                                    map("fl", 123.f),
                                    map("int", 123),
                                    map("long", 123L),
                                    map("bool", true));

                    int strId = rdoc.mapLookup(mapId, "str");

                    rdoc.drawTextAnchored(strId, tw / 2, th / 2, 0, 0, 0);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(cb);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_data_containers3);

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.printf("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.printf("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);
        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }
}
