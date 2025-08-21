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

import static androidx.compose.remote.player.view.TestUtils.createImage;
import static androidx.compose.remote.player.view.TestUtils.dumpDifference;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

@RunWith(AndroidJUnit4.class)
public class ContentBehaviorTest {

    private final Platform mPlatform = new AndroidxPlatformServices();

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // TEST UTILS
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private RemoteComposeDocument createDoc(RemoteComposeContext buffer) {
        return new RemoteComposeDocument(
                new ByteArrayInputStream(buffer.buffer(), 0, buffer.bufferSize()));
    }

    private RemoteComposeContext remoteComposeWriter(
            int tw, int th, int scrolling, int alignment, int sizing, int mode) {
        Bitmap lightImage = createImage(tw, th, false);
        RemoteComposeContextAndroid doc =
                new RemoteComposeContextAndroid(
                        tw,
                        th,
                        "demo",
                        6,
                        0,
                        mPlatform,
                        doc1 -> {
                            doc1.setRootContentBehavior(scrolling, alignment, sizing, mode);
                            doc1.drawBitmap(lightImage, "Light Mode");
                            return null;
                        });
        return doc;
    }

    private RemoteComposeDocument createDocument(
            RemoteContext context, Bitmap lightImage, Bitmap darkImage) {
        int tw = lightImage.getWidth();
        int th = lightImage.getHeight();

        RemoteComposeContextAndroid doc =
                new RemoteComposeContextAndroid(
                        tw,
                        th,
                        "demo",
                        mPlatform,
                        doc1 -> {
                            doc1.setTheme(Theme.LIGHT);
                            doc1.drawBitmap(lightImage, "Light Mode");
                            doc1.setTheme(Theme.DARK);
                            doc1.drawBitmap(darkImage, "Dark Mode");
                            doc1.setTheme(Theme.UNSPECIFIED);
                            doc1.addClickArea(1, "Area A", 0f, 0f, 300f, 300f, "A");
                            doc1.addClickArea(2, "Area B", 300f, 0f, 600f, 300f, "B");
                            doc1.addClickArea(3, "Area C", 0f, 300f, 300f, 600f, "C");
                            doc1.addClickArea(4, "Area D", 300f, 300f, 600f, 600f, "D");
                            return null;
                        });

        RemoteComposeDocument recreatedDocument = createDoc(doc);
        recreatedDocument.initializeContext(context);
        return recreatedDocument;
    }

    void diff(String a, String b) {
        String[] as = a.split("\n");
        String[] bs = b.split("\n");

        if (as.length != bs.length) {
            System.out.println("diff " + as.length + " lines vs. " + bs.length + " lines");
        } else {
            System.out.println("diff ---------" + as.length + " lines");
        }
        int max = Math.max(as.length, bs.length);
        String mark = new String(new char[50]).replace('0', '-');
        for (int i = 0; i < max; i++) {

            if (i >= as.length) {
                System.out.println(
                        i + ": \"" + mark.substring(0, bs[i].length()) + "\"!=\"" + bs[i] + "\"");
                continue;
            }
            if (i >= bs.length) {
                System.out.println(
                        i + ": \"" + as[i] + "\"!=\"" + mark.substring(0, as[i].length()) + "\"");
                continue;
            }
            if (!bs[i].equals(as[i])) {
                System.out.println(i + ": \"" + as[i] + "\"!=\"" + bs[i] + "\"");
                break;
            }
            System.out.println(i + ": " + as[i]);
        }
    }

    private void checkScale(
            int tw,
            int th,
            String expectedResult,
            int scrolling,
            int alignment,
            int sizing,
            int mode) {
        RemoteComposeDocument doc1 =
                createDoc(remoteComposeWriter(tw, th, scrolling, alignment, sizing, mode));

        doc1.getDocument().setWidth(tw);
        doc1.getDocument().setHeight(th);
        DebugPlayerContext debugContext = new DebugPlayerContext();

        debugContext.mWidth = 600;
        debugContext.mHeight = 600;

        doc1.initializeContext(debugContext);
        doc1.paint(debugContext, Theme.UNSPECIFIED);

        String result = TestUtils.removeTime(debugContext.getTestResults());
        if (TestUtils.diff(expectedResult, result)) {
            Log.v("TEST", result);
            dumpDifference(expectedResult, result);
        }
        diff(expectedResult, result);
        assertEquals("write doc \n" + result + "\n", expectedResult, result);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // END TEST UTILS
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testScaleInside() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (75.0, 200.0)\n"
                        + "scale (1.0, 1.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (200.0, 75.0)\n"
                        + "scale (1.0, 1.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 166.66666)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (166.66666, 0.0)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);
    }

    @Test
    public void testScaleFillWidth() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 2\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 166.66666)\n"
                        + "scale (1.3333334, 1.3333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_WIDTH);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 2\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, -375.0)\n"
                        + "scale (3.0, 3.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_WIDTH);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 2\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 166.66666)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_WIDTH);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 2\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, -375.0)\n"
                        + "scale (0.75, 0.75)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_WIDTH);
    }

    @Test
    public void testScaleFillHeight() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 3\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (-375.0, 0.0)\n"
                        + "scale (3.0, 3.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_HEIGHT);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 3\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (166.66666, 0.0)\n"
                        + "scale (1.3333334, 1.3333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_HEIGHT);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 3\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (-375.0, 0.0)\n"
                        + "scale (0.75, 0.75)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_HEIGHT);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 3\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (166.66666, 0.0)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_HEIGHT);
    }

    @Test
    public void testScaleFit() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 4\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 166.66666)\n"
                        + "scale (1.3333334, 1.3333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 4\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (166.66666, 0.0)\n"
                        + "scale (1.3333334, 1.3333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 4\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 166.66666)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 4\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (166.66666, 0.0)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT);
    }

    @Test
    public void testScaleCrop() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 5\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (-375.0, 0.0)\n"
                        + "scale (3.0, 3.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_CROP);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 5\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, -375.0)\n"
                        + "scale (3.0, 3.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_CROP);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 5\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (-375.0, 0.0)\n"
                        + "scale (0.75, 0.75)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_CROP);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 5\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, -375.0)\n"
                        + "scale (0.75, 0.75)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_CROP);
    }

    @Test
    public void testScaleFillBounds() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 6\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 0.0)\n"
                        + "scale (1.3333334, 3.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_BOUNDS);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 6\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 0.0)\n"
                        + "scale (3.0, 1.3333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_BOUNDS);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 6\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 0.0)\n"
                        + "scale (0.33333334, 0.75)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_BOUNDS);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 34, 2, 6\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 0.0)\n"
                        + "scale (0.75, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_BOUNDS);
    }

    @Test
    public void testScaleAlignment() {
        String expectedResult1 =
                "header(1, 0, 0) 450 x 200, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 68, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (150.0, 400.0)\n"
                        + "scale (1.0, 1.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450,
                200,
                expectedResult1,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_END + RootContentBehavior.ALIGNMENT_BOTTOM,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);

        String expectedResult2 =
                "header(1, 0, 0) 200 x 450, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 17, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 0.0)\n"
                        + "scale (1.0, 1.0)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200,
                450,
                expectedResult2,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_START + RootContentBehavior.ALIGNMENT_TOP,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);

        String expectedResult3 =
                "header(1, 0, 0) 1800 x 800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 36, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 333.3333)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                450 * 4,
                200 * 4,
                expectedResult3,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_HORIZONTAL_CENTER
                        + RootContentBehavior.ALIGNMENT_BOTTOM,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);

        String expectedResult4 =
                "header(1, 0, 0) 800 x 1800, 0\n"
                        + "loadText(42)\n"
                        + "rootContentBehavior 0, 18, 2, 1\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-1)\n"
                        + "translate (0.0, 0.0)\n"
                        + "scale (0.33333334, 0.33333334)\n"
                        + "drawBitmap <43>\n";

        checkScale(
                200 * 4,
                450 * 4,
                expectedResult4,
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_START + RootContentBehavior.ALIGNMENT_VERTICAL_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_INSIDE);
    }
}
