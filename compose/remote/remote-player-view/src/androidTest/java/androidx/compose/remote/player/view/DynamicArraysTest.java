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

import static androidx.compose.remote.player.view.TestUtils.blank;
import static androidx.compose.remote.player.view.TestUtils.compareImages;
import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.profile.PlatformProfile;
import androidx.compose.remote.player.core.RemoteComposeDocument;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;

@SdkSuppress(minSdkVersion = 26) // b/43795894
@RunWith(JUnit4.class)
public class DynamicArraysTest {
    private boolean mSaveImages = false;

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
        String paintStr = "paintData(\n" + "   AntiAlias(0),\n" + ")\n";
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

    static String getMethodName() {
        return new Throwable().getStackTrace()[1].getMethodName();
    }

    // ########################### END TEST UTILS ######################################

    @Test
    public void testDynamicArrays() {
        TestUtils.Callback cb =
                rdoc -> {
                    float id = rdoc.mRemoteWriter.addFloatArray(3f);
                    rdoc.mRemoteWriter.setArrayValue(Utils.idFromNan(id), 0f, 300f);
                    rdoc.mRemoteWriter.setArrayValue(Utils.idFromNan(id), 1f, 400f);
                    rdoc.mRemoteWriter.setArrayValue(Utils.idFromNan(id), 2f, 42f);
                    float x = rdoc.floatExpression(id, 0f, AnimatedFloatExpression.A_DEREF);
                    float y = rdoc.floatExpression(id, 1f, AnimatedFloatExpression.A_DEREF);
                    float z = rdoc.floatExpression(id, 2f, AnimatedFloatExpression.A_DEREF);
                    rdoc.drawCircle(x, y, z);
                };

        byte[] rawDoc = TestSerializeUtils.createDoc(PlatformProfile.ANDROIDX, cb);
        String result = TestSerializeUtils.toYamlFlatString(rawDoc);

        Log.v("TEST", result);

        System.out.println(result);

        String expected = "\n"
                + "CoreDocument width= 0 height= 0 \n"
                + "  DataDynamicListFloat   id= 2097194   values= [300.0, 400.0, 42.0]   arrayId="
                + " 2097194   index=      0.0        300.0   arrayId= 2097194   index=      1.0  "
                + "      400.0   arrayId= 2097194   index=      2.0        42.0 \n"
                + "  FloatExpression   id= 42   srcValues= \n"
                + "    Variable     id= 42          0.0 \n"
                + "    Instruction     instruction= A_DEREF   animation= null \n"
                + "  FloatExpression   id= 43   srcValues= \n"
                + "    Variable     id= 42          1.0 \n"
                + "    Instruction     instruction= A_DEREF   animation= null \n"
                + "  FloatExpression   id= 44   srcValues= \n"
                + "    Variable     id= 42          2.0 \n"
                + "    Instruction     instruction= A_DEREF   animation= null \n"
                + "  DrawCircle   cx= \n"
                + "  Variable   id= 42   300.0   cy= \n"
                + "  Variable   id= 43   400.0   radius= \n"
                + "  Variable   id= 44   42.0 ";
        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc =
                new RemoteComposeDocument(new ByteArrayInputStream(rawDoc, 0, rawDoc.length));

        DebugPlayerContext debugContext = new DebugPlayerContext();
        debugContext.mWidth = 1000;
        debugContext.mHeight = 1000;
        doc.initializeContext(debugContext);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        String resultPaint =
                debugContext.getTestResults(); // doc.getDocument().getRootLayoutComponent()
        // .displayHierarchy();

        String expectedPaint = "header(1, 1, 0) 600 x 600, 0\n"
                + "setTheme(-1)\n"
                + "drawCircle(300.0, 400.0, 42.0)\n";
        assertEquals("not equals", expectedPaint, resultPaint);

    }

}
