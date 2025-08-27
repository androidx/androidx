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

import static androidx.compose.remote.player.view.TestSerializeUtils.loadFileFromRaw;
import static androidx.compose.remote.player.view.TestUtils.blank;
import static androidx.compose.remote.player.view.TestUtils.compareImages;
import static androidx.compose.remote.player.view.TestUtils.saveBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.compose.remote.player.view.test.R;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;

/** This test the function call syntax */
@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(JUnit4.class)
public class ConditionTest {
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

    // ########################### END TEST UTILS ######################################

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
    public void testConditional1() {

        int tw = 600;
        int th = 600;
        DebugPlayerContext debugContext = new DebugPlayerContext();

        String str = "hello world";

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeView rc_player = new RemoteComposeView(appContext);
        Bitmap localBitmap = blank(tw, th);
        Canvas canvas = new Canvas(localBitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        {
            float x1 = 0, y1 = 0, x2 = 20, y2 = 20;
            paint.setColor(Color.RED);
            canvas.drawOval(x1, y1, x2, y2, paint);
            x1 = x2;
            x2 += 20;
            y1 = y2;
            y2 += 20;
            canvas.drawOval(x1, y1, x2, y2, paint);
            x1 = x2;
            x2 += 20;
            y1 = y2;
            y2 += 20;
            canvas.drawOval(x1, y1, x2, y2, paint);
            x1 = x2;
            x2 += 20;
            y1 = y2;
            y2 += 20;
            canvas.drawOval(x1, y1, x2, y2, paint);
        }

        TestUtils.Callback cb2 =
                rc -> {
                    float x1 = 0, y1 = 0, x2 = 20, y2 = 20;
                    Float fid = rc.addFloatConstant(200);
                    int tid1 = rc.textCreateId("This is a test");
                    float widthV1 = rc.textMeasure(tid1, 0);
                    int tid2 = rc.textCreateId("This is a longer test");
                    float widthV2 = rc.textMeasure(tid2, 0);
                    rc.getPainter().setColor(Color.RED).commit();
                    rc.conditionalOperations(ConditionalOperations.TYPE_EQ, widthV1, widthV1);
                    rc.drawOval(x1, y1, x2, y2); // 1
                    rc.endConditionalOperations();
                    x1 = x2;
                    x2 += 20;
                    y1 = y2;
                    y2 += 20;
                    rc.conditionalOperations(ConditionalOperations.TYPE_NEQ, widthV1, 2f);
                    rc.drawOval(x1, y1, x2, y2); // 2
                    rc.endConditionalOperations();

                    x1 = x2;
                    x2 += 20;
                    y1 = y2;
                    y2 += 20;
                    rc.conditionalOperations(ConditionalOperations.TYPE_LTE, widthV1, widthV2);
                    rc.drawOval(x1, y1, x2, y2); // 3
                    rc.endConditionalOperations();

                    x1 = x2;
                    x2 += 20;
                    y1 = y2;
                    y2 += 20;
                    rc.conditionalOperations(ConditionalOperations.TYPE_GTE, widthV1, widthV1);
                    rc.drawOval(x1, y1, x2, y2); // 4
                    rc.endConditionalOperations();
                };
        drawCommandTest(cb2);
        // String result = TestUtils.createDocument(debugContext, cb2).toString();
        byte[] rawDoc = TestSerializeUtils.createDoc(cb2);
        String result = TestSerializeUtils.toYamlString(rawDoc);
        Log.v("TEST", result);

        System.out.println(result);
        String expected = loadFileFromRaw(CtsTest.sAppContext, R.raw.test_conditional1);

        if (TestUtils.diff(expected, result)) {
            TestUtils.dumpDifference(expected, result);
        }
        assertEquals("not equals", expected, result);

        RemoteComposeDocument doc = TestUtils.createDocument(debugContext, cb2);

        TestUtils.captureGold("clipPath", doc, appContext);
        final ArrayList<String> colors = new ArrayList<>();
        Bitmap remoteBitmap = TestUtils.docToBitmap(tw, th, appContext, doc, null);
        String[] col = colors.toArray(new String[0]);
        System.out.println(">>>> " + Arrays.toString(col));
        Bitmap blankBitmap = blank(tw, th);

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
        System.out.println("relative to blank " + compareImages(blankBitmap, remoteBitmap));
        float rms = compareImages(localBitmap, remoteBitmap);
        System.out.println("relative to local " + rms);
        assertTrue("image not equivalent error = " + rms, rms < 4);
    }
}
