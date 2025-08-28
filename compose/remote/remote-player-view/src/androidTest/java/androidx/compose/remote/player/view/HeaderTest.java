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

import static androidx.compose.remote.creation.RemoteComposeWriter.hTag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class HeaderTest {
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
    private static final Platform sPlatform = new AndroidxPlatformServices();

    @Test
    public void testHeader1() {

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

        RemoteComposeWriterAndroid writer =
                new RemoteComposeWriterAndroid(
                        sPlatform,
                        hTag(Header.DOC_WIDTH, 600),
                        hTag(Header.DOC_HEIGHT, 600),
                        hTag(Header.DOC_CONTENT_DESCRIPTION, "DEMO"));
        Float fid = writer.addFloatConstant(200);
        writer.getPainter().setColor(0x550000ff).commit();
        writer.drawOval(0, fid, tw, th);

        byte[] buffer = writer.buffer();
        int bufferSize = writer.bufferSize();
        RemoteComposeDocument doc =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));

        String result = doc.toString();
        String expected =
                "Document{\n"
                        + "HEADER v1.1.0\n"
                        + "  DOC_WIDTH 600\n"
                        + "  DOC_HEIGHT 600\n"
                        + "  DOC_CONTENT_DESCRIPTION DEMO\n"
                        + "FloatConstant[42] = 200.0\n"
                        + "PaintData \"\n"
                        + "    Color(0x550000ff),\n"
                        + "\"\n"
                        + "DrawOval 0.0 [42][42] 600.0 600.0\n"
                        + "}";

        Log.v("TEST", result);
        if (TestUtils.diff(expected, result)) {
            System.out.println("---------- DIFF -------------");
            TestUtils.dumpDifference(expected, result);
            System.out.println("---------- DIFF -------------");
        }
        assertEquals("not equals", expected, result);

        // RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb);

        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc);

        Bitmap blankBitmap = TestUtils.blank(tw, th);
        checkResults(appContext, blankBitmap, remoteBitmap, localBitmap);
    }

    /**
     * test that we can read the header from a file Header h = Header.readDirect(new
     * FileInputStream(f)); This can be used to directly scan many files.
     */
    @Test
    public void testHeader2() {

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

        RemoteComposeWriterAndroid writer =
                new RemoteComposeWriterAndroid(
                        sPlatform,
                        hTag(Header.DOC_WIDTH, 600),
                        hTag(Header.DOC_HEIGHT, 500),
                        hTag(Header.DOC_CONTENT_DESCRIPTION, "DEMO"),
                        hTag(Header.DOC_DESIRED_FPS, 120));
        Float fid = writer.addFloatConstant(200);
        writer.getPainter().setColor(0x550000ff).commit();
        writer.drawOval(0, fid, tw, th);

        byte[] buffer = writer.buffer();
        int bufferSize = writer.bufferSize();

        // Write the document to a file
        // read the header directly from the file
        // compare the header values to the expected values
        try {
            File f = File.createTempFile("headerTest", "rc");

            f.deleteOnExit();
            OutputStream os = new java.io.FileOutputStream(f);
            os.write(buffer, 0, bufferSize);
            os.close();
            Header h = Header.readDirect(new FileInputStream(f));
            System.out.println(h.toString());

            assertEquals(600, h.get(Header.DOC_WIDTH));
            assertEquals(500, h.get(Header.DOC_HEIGHT));
            assertEquals("DEMO", h.get(Header.DOC_CONTENT_DESCRIPTION));
            assertEquals(120, h.get(Header.DOC_DESIRED_FPS));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("=============================");
    }
}
