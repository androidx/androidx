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
import android.util.Log;

import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteComposeState;
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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

@SdkSuppress(minSdkVersion = 26) // b/437958945
@RunWith(AndroidJUnit4.class)
public class ImageDrawTest {

    private final Platform mPlatform = new AndroidxPlatformServices();

    // ########################### TEST UTILS ######################################
    private RemoteComposeDocument createDocument(
            RemoteContext context, Bitmap lightImage, Bitmap darkImage) {
        int tw = lightImage.getWidth();
        int th = lightImage.getHeight();

        RemoteComposeContext doc =
                new RemoteComposeContextAndroid(
                        tw,
                        th,
                        "Demo",
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
        System.out.println("size of doc " + bufferSize / 1024 + "KB");
        RemoteComposeDocument recreatedDocument =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));
        recreatedDocument.initializeContext(context);
        return recreatedDocument;
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

        String result = doc.toString();
        Log.v("TEST", result);
        String expectedResult =
                "Document{\n"
                        + "HEADER v1.1.0, 600 x 600 [0]\n"
                        + "TextData[42] = \"Demo\"\n"
                        + "RootContentDescription 42\n"
                        + "SET_THEME -3\n"
                        + "BITMAP DATA 43\n"
                        + "TextData[44] = \"Light Mode\"\n"
                        + "DRAW_BITMAP_INT 43 on 0 0 600 600 - 0 0 600 600;\n"
                        + "SET_THEME -2\n"
                        + "BITMAP DATA 45\n"
                        + "TextData[46] = \"Dark Mode\"\n"
                        + "DRAW_BITMAP_INT 45 on 0 0 600 600 - 0 0 600 600;\n"
                        + "SET_THEME -1\n"
                        + "TextData[47] = \"Area A\"\n"
                        + "TextData[48] = \"A\"\n"
                        + "CLICK_AREA <1 <47> <48>+0.0 0.0 300.0 300.0+ (300.0 x 300.0 }\n"
                        + "TextData[49] = \"Area B\"\n"
                        + "TextData[50] = \"B\"\n"
                        + "CLICK_AREA <2 <49> <50>+300.0 0.0 600.0 300.0+ (300.0 x 300.0 }\n"
                        + "TextData[51] = \"Area C\"\n"
                        + "TextData[52] = \"C\"\n"
                        + "CLICK_AREA <3 <51> <52>+0.0 300.0 300.0 600.0+ (300.0 x 300.0 }\n"
                        + "TextData[53] = \"Area D\"\n"
                        + "TextData[54] = \"D\"\n"
                        + "CLICK_AREA <4 <53> <54>+300.0 300.0 600.0 600.0+ (300.0 x 300.0 }\n"
                        + "}";

        assertEquals("write doc <$result>", expectedResult, result);
    }

    @Test
    public void testImageThroughPlayer() {
        int tw = 600;
        int th = 600;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        android.content.Context appContext =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        DebugPlayerContext debugContext = new DebugPlayerContext();

        RemoteComposeDocument doc = createDocument(debugContext, lightImage, darkImage);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        RemoteComposeView player = new RemoteComposeView(appContext);
        player.setDocument(doc);
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

    @Test
    public void testNonSquareImageThroughPlayer() {
        int tw = 400;
        int th = 600;
        Bitmap lightImage = TestUtils.createImage(tw, th, false);
        Bitmap darkImage = TestUtils.createImage(tw, th, true);
        android.content.Context appContext =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        DebugPlayerContext debugContext = new DebugPlayerContext();

        RemoteComposeDocument doc = createDocument(debugContext, lightImage, darkImage);
        doc.paint(debugContext, Theme.UNSPECIFIED);
        RemoteComposeView player = new RemoteComposeView(appContext);
        player.setDocument(doc);
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

    @Test
    public void testRgba888AndAlpha8ImagesRecreatedWithCorrectConfig() {
        int tw = 400;
        int th = 600;
        Bitmap rgbaImage = TestUtils.createImage(tw, th, false);
        Bitmap alpha8Image = TestUtils.createAlpha8Image(tw, th);
        DebugPlayerContext debugContext = new DebugPlayerContext();
        android.content.Context appContext =
                InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteComposeContext doc =
                new RemoteComposeContextAndroid(
                        tw,
                        th,
                        "Demo",
                        mPlatform,
                        doc1 -> {
                            doc1.drawBitmap(rgbaImage, "rgbaImage");
                            doc1.drawBitmap(alpha8Image, "alpha8Image");
                            return null;
                        });
        int rgbaImageId = doc.addBitmap(rgbaImage);
        int alpha8ImageId = doc.addBitmap(alpha8Image);

        byte[] buffer = doc.buffer();
        int bufferSize = doc.bufferSize();
        RemoteComposeDocument recreatedDocument =
                new RemoteComposeDocument(new ByteArrayInputStream(buffer, 0, bufferSize));
        recreatedDocument.initializeContext(debugContext);
        RemoteComposeView player = new RemoteComposeView(appContext);
        player.setDocument(recreatedDocument);
        RemoteComposeState remoteComposeState =
                recreatedDocument.getDocument().getRemoteComposeState();

        BitmapData recreatedRgbaImageData = (BitmapData) remoteComposeState.getObject(rgbaImageId);
        BitmapData recreatedAlpha8ImageData =
                (BitmapData) remoteComposeState.getObject(alpha8ImageId);
        assertEquals(recreatedRgbaImageData.getType(), BitmapData.TYPE_PNG_8888);
        assertEquals(recreatedAlpha8ImageData.getType(), BitmapData.TYPE_PNG_ALPHA_8);
        Bitmap recreatedRgbaImage = (Bitmap) remoteComposeState.getFromId(rgbaImageId);
        Bitmap recreatedAlpha8Image = (Bitmap) remoteComposeState.getFromId(alpha8ImageId);
        assertEquals(recreatedRgbaImage.getConfig(), Bitmap.Config.ARGB_8888);
        assertEquals(recreatedAlpha8Image.getConfig(), Bitmap.Config.ALPHA_8);
    }
}
