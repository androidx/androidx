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

import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.Theme;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeContextAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(AndroidJUnit4.class)
public class ImageErrorTest {

    private final Platform mPlatform = new AndroidxPlatformServices();

    // ########################### TEST UTILS ######################################
    private RemoteComposeDocument createDocument(
            RemoteContext context, Bitmap lightImage, Bitmap darkImage) {
        byte[] buffer = create(lightImage, darkImage);
        System.out.println("size of doc " + buffer.length / 1024 + "KB");
        return createDocument(buffer, buffer.length, context);
    }

    byte[] create(Bitmap lightImage, Bitmap darkImage) {
        int tw = lightImage.getWidth();
        int th = lightImage.getHeight();

        RemoteComposeContext doc =
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

        byte[] buffer = doc.buffer();
        int bufferSize = doc.bufferSize();
        return Arrays.copyOf(buffer, bufferSize);
    }

    private RemoteComposeDocument createDocument(
            byte[] buffer, int bufferSize, RemoteContext context) {
        RemoteComposeDocument recreatedDocument =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));
        recreatedDocument.initializeContext(context);
        return recreatedDocument;
    }

    ByteBuffer docGetBuffer(RemoteComposeDocument doc) {
        RemoteComposeBuffer buff = doc.getDocument().getBuffer();
        int size = buff.getBuffer().getSize();
        ByteBuffer b = ByteBuffer.allocate(size);
        b.put(buff.getBuffer().getBuffer(), 0, size);
        System.out.println("(ImageErrorTest.java:95). docGetBuffer " + size);
        return b;
    }

    // ########################### END TEST UTILS ######################################

    @Test
    public void testWriteDocument() {
        int tw = 600;
        int th = 600;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        DebugPlayerContext debugContext = new DebugPlayerContext();
        RemoteComposeDocument doc = createDocument(debugContext, lightImage, darkImage);
        doc.paint(debugContext, Theme.UNSPECIFIED);

        String result = TestUtils.removeTime(debugContext.getTestResults());
        String expectedResult =
                "header(1, 1, 0) 600 x 600, 0\n"
                        + "loadText(42)\n"
                        + "setTheme(-3)\n"
                        + "loadImage(43)\n"
                        + "loadText(44)\n"
                        + "setTheme(-2)\n"
                        + "loadImage(45)\n"
                        + "loadText(46)\n"
                        + "setTheme(-1)\n"
                        + "loadText(47)\n"
                        + "loadText(48)\n"
                        + "clickArea(1, 0.0, 0.0, 300.0, 300.0, 48)\n"
                        + "loadText(49)\n"
                        + "loadText(50)\n"
                        + "clickArea(2, 300.0, 0.0, 600.0, 300.0, 50)\n"
                        + "loadText(51)\n"
                        + "loadText(52)\n"
                        + "clickArea(3, 0.0, 300.0, 300.0, 600.0, 52)\n"
                        + "loadText(53)\n"
                        + "loadText(54)\n"
                        + "clickArea(4, 300.0, 300.0, 600.0, 600.0, 54)\n"
                        + "setTheme(-1)\n"
                        + "setTheme(-3)\n"
                        + "drawBitmap <43>\n"
                        + "setTheme(-2)\n"
                        + "drawBitmap <45>\n"
                        + "setTheme(-1)\n";

        TestUtils.diff(expectedResult, result);

        assertEquals("write doc <$result>", expectedResult, result);
    }

    ByteBuffer createDoc(int tw, int th) {
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        return createDoc(lightImage, darkImage);
    }

    ByteBuffer createDoc(Bitmap lightImage, Bitmap darkImage) {
        DebugPlayerContext debugContext = new DebugPlayerContext();
        RemoteComposeDocument doc = createDocument(debugContext, lightImage, darkImage);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        ByteBuffer b = docGetBuffer(doc);
        return b;
    }

    @Test
    public void testPlayerFromBuffer() {
        int tw = 600;
        int th = 600;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);

        byte[] b = create(lightImage, darkImage);
        InputStream is = new ByteArrayInputStream(b);
        RemoteComposeDocument rdoc = new RemoteComposeDocument(is);
        android.content.Context appContext =
                InstrumentationRegistry.getInstrumentation().getTargetContext();

        RemoteComposeView player = new RemoteComposeView(appContext);
        player.setDocument(rdoc);
        Bitmap result = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Bitmap blank = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        player.draw(canvas);
        float rms = TestUtils.compareImages(blank, result);
        System.out.println("relative to blank " + rms);
        float lightRms = TestUtils.compareImages(lightImage, result);
        System.out.println("relative to light " + lightRms);
        assertTrue("image not equivalent error=" + lightRms, lightRms < 32);
        player.setTheme(Theme.DARK);
        player.draw(canvas);
        float darkRms = TestUtils.compareImages(darkImage, result);
        System.out.println("relative to dark  error=" + darkRms);

        assertTrue("image not equivalent error=" + darkRms, darkRms < 32);
    }

    @Ignore("Flaky Test")
    @Test
    public void testPlayerFromCorruptBuffer() {
        int tw = 600;
        int th = 600;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);

        byte[] b = create(lightImage, darkImage);
        Random r = new Random(123456789);
        for (int k = 0; k < 100; k++) {

            long seed = r.nextLong() % 100000;
            System.out.println("seed = " + seed);
            r.setSeed(seed);
            for (int i = 7 * 4; i < b.length; i++) {
                b[i] = (byte) r.nextInt(256);
            }
            InputStream is = new ByteArrayInputStream(b);
            try {
                RemoteComposeDocument rdoc = new RemoteComposeDocument(is);
            } catch (Exception ex) {
                System.out.println(" caught " + seed + " " + ex.toString());
                continue;
            }
            assertTrue("Did not catch corrupt " + seed, false);
        }
        assertTrue(true);
    }

    @Test
    public void testImageTooLargeCreating1() {
        int tw = 400;
        int th = BitmapData.MAX_IMAGE_DIMENSION + 1;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        DebugPlayerContext debugContext = new DebugPlayerContext();

        RemoteComposeDocument doc = null;
        try {
            doc = createDocument(debugContext, lightImage, darkImage);
        } catch (Exception e) {
            System.out.println("successfully caught exception");
            return;
        }
        assertTrue("code should not reach here", false);
    }

    @Test
    public void testImageTooLargeCreating2() {
        int tw = BitmapData.MAX_IMAGE_DIMENSION + 1;
        int th = 60;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        DebugPlayerContext debugContext = new DebugPlayerContext();

        RemoteComposeDocument doc = null;
        try {
            doc = createDocument(debugContext, lightImage, darkImage);
        } catch (Exception e) {
            System.out.println("successfully caught exception");
            return;
        }
        assertTrue("code should not reach here", false);
    }

    @Test
    public void testNonSquareImageThroughPlayer() {
        int tw = 400;
        int th = BitmapData.MAX_IMAGE_DIMENSION + 1;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        DebugPlayerContext debugContext = new DebugPlayerContext();

        RemoteComposeDocument doc = null;
        try {
            doc = createDocument(debugContext, lightImage, darkImage);
        } catch (Exception e) {
            System.out.println("successfully caught exception");
            return;
        }
        assertTrue("code should not reach here", false);
    }
}
