/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.layout.managers.CoreText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AndroidPaintContextTest {

    private AndroidRemoteContext mRemoteContext;
    private Canvas mCanvas;
    private AndroidPaintContext mPaintContext;

    @Before
    public void setUp() {
        mRemoteContext = new AndroidRemoteContext();
        Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(bitmap);
        mPaintContext = new AndroidPaintContext(mRemoteContext, mCanvas);
    }

    @Test
    public void testAndroidComputedTextLayout_properties() {
        TextPaint paint = new TextPaint();
        StaticLayout staticLayout =
                StaticLayout.Builder.obtain("Test", 0, 4, paint, 500)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build();

        AndroidComputedTextLayout layout =
                new AndroidComputedTextLayout(staticLayout, 25f, 100f, 50f, 1, false);

        assertEquals(staticLayout, layout.get());
        assertEquals(25f, layout.getLeft(), 0.001f);
        assertEquals(100f, layout.getWidth(), 0.001f);
        assertEquals(50f, layout.getHeight(), 0.001f);
        assertEquals(1, layout.getVisibleLineCount());
        assertFalse(layout.isHyphenatedText());
    }

    @Test
    public void testLayoutComplexText_centeredSingleLine_hasValidLeftAndPositiveWidth() {
        String text = "label";
        int textId = 1;
        mRemoteContext.loadText(textId, text);

        float maxWidth = 500f;
        RcPlatformServices.ComputedTextLayout layout =
                mPaintContext.layoutComplexText(
                        textId,
                        0,
                        text.length(),
                        CoreText.TEXT_ALIGN_CENTER,
                        CoreText.OVERFLOW_ELLIPSIS,
                        1,
                        maxWidth,
                        500f,
                        0f,
                        0f,
                        1f,
                        0,
                        0,
                        0,
                        false,
                        false,
                        0);

        assertNotNull(layout);
        AndroidComputedTextLayout androidLayout = (AndroidComputedTextLayout) layout;

        // Centered text inside a 500px container must have a positive left offset.
        assertTrue(
                "Centered text should have left offset > 0",
                androidLayout.getLeft() > 0f);

        // Width must be positive (maxRight - minLeft) and tightly bounded.
        assertTrue(
                "Tight bounding width must be positive",
                androidLayout.getWidth() > 0f);
        assertTrue(
                "Tight bounding width should be significantly smaller than maxWidth",
                androidLayout.getWidth() < maxWidth);

        assertEquals(1, androidLayout.getVisibleLineCount());
    }

    @Test
    public void testLayoutComplexText_multilineCentered_calculatesTightBounds() {
        String text = "First Line\nSecond Line";
        int textId = 2;
        mRemoteContext.loadText(textId, text);

        float maxWidth = 500f;
        RcPlatformServices.ComputedTextLayout layout =
                mPaintContext.layoutComplexText(
                        textId,
                        0,
                        text.length(),
                        CoreText.TEXT_ALIGN_CENTER,
                        CoreText.OVERFLOW_ELLIPSIS,
                        2,
                        maxWidth,
                        500f,
                        0f,
                        0f,
                        1f,
                        0,
                        0,
                        0,
                        false,
                        false,
                        0);

        assertNotNull(layout);
        AndroidComputedTextLayout androidLayout = (AndroidComputedTextLayout) layout;

        assertTrue(
                "Multiline centered text should have left offset > 0",
                androidLayout.getLeft() > 0f);
        assertTrue(
                "Multiline tight bounding width must be positive",
                androidLayout.getWidth() > 0f);
        assertEquals(2, androidLayout.getVisibleLineCount());
    }

    @Test
    public void testLayoutComplexText_startAligned_hasZeroLeftOffset() {
        String text = "Start Aligned";
        int textId = 3;
        mRemoteContext.loadText(textId, text);

        float maxWidth = 500f;
        RcPlatformServices.ComputedTextLayout layout =
                mPaintContext.layoutComplexText(
                        textId,
                        0,
                        text.length(),
                        CoreText.TEXT_ALIGN_START,
                        CoreText.OVERFLOW_ELLIPSIS,
                        1,
                        maxWidth,
                        500f,
                        0f,
                        0f,
                        1f,
                        0,
                        0,
                        0,
                        false,
                        false,
                        0);

        assertNotNull(layout);
        AndroidComputedTextLayout androidLayout = (AndroidComputedTextLayout) layout;

        // LTR start-aligned text starts at x = 0.
        assertEquals(0f, androidLayout.getLeft(), 0.001f);
        assertTrue(
                "Tight bounding width must be positive",
                androidLayout.getWidth() > 0f);
    }

    @Test
    public void testDrawComplexText_rendersWithoutError() {
        String text = "Centered Render";
        int textId = 4;
        mRemoteContext.loadText(textId, text);

        RcPlatformServices.ComputedTextLayout layout =
                mPaintContext.layoutComplexText(
                        textId,
                        0,
                        text.length(),
                        CoreText.TEXT_ALIGN_CENTER,
                        CoreText.OVERFLOW_ELLIPSIS,
                        1,
                        500f,
                        500f,
                        0f,
                        0f,
                        1f,
                        0,
                        0,
                        0,
                        false,
                        false,
                        0);

        assertNotNull(layout);
        // Ensure drawComplexText executes and translates canvas cleanly without exceptions
        mPaintContext.drawComplexText(layout);
    }
}
