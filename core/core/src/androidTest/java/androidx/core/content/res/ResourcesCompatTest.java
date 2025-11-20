/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.core.content.res;

import static android.os.Build.VERSION.SDK_INT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.text.PositionedGlyphs;
import android.graphics.text.TextRunShaper;
import android.os.Build;
import android.support.v4.testutils.TestUtils;
import android.util.DisplayMetrics;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.provider.FontsContractCompat;
import androidx.core.provider.MockFontProvider;
import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
public class ResourcesCompatTest {
    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mResources = mContext.getResources();
        MockFontProvider.prepareFontFiles(mContext);
    }

    @Test
    public void testGetColor() throws Throwable {
        assertEquals("Unthemed color load",
                ResourcesCompat.getColor(mResources, R.color.text_color, null),
                0xFFFF8090);

        final Resources.Theme yellowTheme = mResources.newTheme();
        yellowTheme.applyStyle(R.style.YellowTheme, true);
        assertEquals("Themed yellow color load", 0xFFF0B000,
                ResourcesCompat.getColor(mResources, R.color.simple_themed_selector,
                        yellowTheme));

        final Resources.Theme lilacTheme = mResources.newTheme();
        lilacTheme.applyStyle(R.style.LilacTheme, true);
        assertEquals("Themed lilac color load", 0xFFF080F0,
                ResourcesCompat.getColor(mResources, R.color.simple_themed_selector,
                        lilacTheme));
    }

    @Test
    public void testGetColorStateList() throws Throwable {
        final ColorStateList unthemedColorStateList =
                ResourcesCompat.getColorStateList(mResources, R.color.complex_unthemed_selector,
                        null);
        assertEquals("Unthemed color state list load: default", 0xFF70A0C0,
                unthemedColorStateList.getDefaultColor());
        assertEquals("Unthemed color state list load: focused", 0xFF70B0F0,
                unthemedColorStateList.getColorForState(
                        new int[]{android.R.attr.state_focused}, 0));
        assertEquals("Unthemed color state list load: pressed", 0xFF6080B0,
                unthemedColorStateList.getColorForState(
                        new int[]{android.R.attr.state_pressed}, 0));

        final Resources.Theme yellowTheme = mResources.newTheme();
        yellowTheme.applyStyle(R.style.YellowTheme, true);
        final ColorStateList themedYellowColorStateList =
                ResourcesCompat.getColorStateList(mResources, R.color.complex_themed_selector,
                        yellowTheme);
        assertEquals("Themed yellow color state list load: default", 0xFFF0B000,
                themedYellowColorStateList.getDefaultColor());
        assertEquals("Themed yellow color state list load: focused", 0xFFF0A020,
                themedYellowColorStateList.getColorForState(
                        new int[]{android.R.attr.state_focused}, 0));
        assertEquals("Themed yellow color state list load: pressed", 0xFFE0A040,
                themedYellowColorStateList.getColorForState(
                        new int[]{android.R.attr.state_pressed}, 0));

        // reloading the same resource with a different theme should not result in the cached CSL
        // being returned; it should inflate a new one
        final Resources.Theme lilacTheme = mResources.newTheme();
        lilacTheme.applyStyle(R.style.LilacTheme, true);
        final ColorStateList themedLilacColorStateList =
                ResourcesCompat.getColorStateList(mResources, R.color.complex_themed_selector,
                        lilacTheme);
        assertEquals("Themed lilac color state list load: default", 0xFFF080F0,
                themedLilacColorStateList.getDefaultColor());
        assertEquals("Themed lilac color state list load: focused", 0xFFF070D0,
                themedLilacColorStateList.getColorForState(
                        new int[]{android.R.attr.state_focused}, 0));
        assertEquals("Themed lilac color state list load: pressed", 0xFFE070A0,
                themedLilacColorStateList.getColorForState(
                        new int[]{android.R.attr.state_pressed}, 0));
    }

    @Test
    public void testGetDrawable() throws Throwable {
        final Drawable unthemedDrawable =
                ResourcesCompat.getDrawable(mResources, R.drawable.test_drawable_red, null);
        TestUtils.assertAllPixelsOfColor("Unthemed drawable load",
                unthemedDrawable, mResources.getColor(R.color.test_red));

        final Resources.Theme yellowTheme = mResources.newTheme();
        yellowTheme.applyStyle(R.style.YellowTheme, true);
        final Drawable themedYellowDrawable =
                ResourcesCompat.getDrawable(mResources, R.drawable.themed_drawable,
                        yellowTheme);
        TestUtils.assertAllPixelsOfColor("Themed yellow drawable load",
                themedYellowDrawable, 0xFFF0B000);

        final Resources.Theme lilacTheme = mResources.newTheme();
        lilacTheme.applyStyle(R.style.LilacTheme, true);
        final Drawable themedLilacDrawable =
                ResourcesCompat.getDrawable(mResources, R.drawable.themed_drawable, lilacTheme);
        TestUtils.assertAllPixelsOfColor("Themed lilac drawable load",
                themedLilacDrawable, 0xFFF080F0);
    }

    @Test
    public void testGetDrawableForDensityUnthemed() throws Throwable {
        // Density-aware drawable loading for now only works on raw bitmap drawables.

        // Different variants of density_aware_drawable are set up in the following way:
        //    mdpi - 12x12 px which is 12x12 dip
        //    hdpi - 21x21 px which is 14x14 dip
        //   xhdpi - 32x32 px which is 16x16 dip
        //  xxhdpi - 54x54 px which is 18x18 dip
        // The tests below (on v15+ devices) are checking that an unthemed density-aware
        // loading of raw bitmap drawables returns a drawable with matching intrinsic
        // dimensions.

        final Drawable unthemedDrawableForMediumDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_MEDIUM, null);
        // For pre-v15 devices we should get a drawable that corresponds to the density of the
        // current device. For v15+ devices we should get a drawable that corresponds to the
        // density requested in the API call.
        final int expectedSizeForMediumDensity = 12;
        assertEquals("Unthemed density-aware drawable load: medium width",
                expectedSizeForMediumDensity, unthemedDrawableForMediumDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: medium height",
                expectedSizeForMediumDensity,
                unthemedDrawableForMediumDensity.getIntrinsicHeight());

        final Drawable unthemedDrawableForHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_HIGH, null);

        final int expectedSizeForHighDensity = 21;
        assertEquals("Unthemed density-aware drawable load: high width",
                expectedSizeForHighDensity, unthemedDrawableForHighDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: high height",
                expectedSizeForHighDensity, unthemedDrawableForHighDensity.getIntrinsicHeight());

        final Drawable unthemedDrawableForXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_XHIGH, null);

        final int expectedSizeForXHighDensity = 32;
        assertEquals("Unthemed density-aware drawable load: xhigh width",
                expectedSizeForXHighDensity, unthemedDrawableForXHighDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: xhigh height",
                expectedSizeForXHighDensity, unthemedDrawableForXHighDensity.getIntrinsicHeight());

        final Drawable unthemedDrawableForXXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_XXHIGH, null);

        final int expectedSizeForXXHighDensity = 54;
        assertEquals("Unthemed density-aware drawable load: xxhigh width",
                expectedSizeForXXHighDensity, unthemedDrawableForXXHighDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: xxhigh height",
                expectedSizeForXXHighDensity,
                unthemedDrawableForXXHighDensity.getIntrinsicHeight());
    }

    @Test
    public void testGetDrawableForDensityThemed() throws Throwable {
        // Density- and theme-aware drawable loading for now only works partially. This test
        // checks only for theming of a tinted bitmap XML drawable, but not correct scaling.

        // Set up the two test themes, yellow and lilac.
        final Resources.Theme yellowTheme = mResources.newTheme();
        yellowTheme.applyStyle(R.style.YellowTheme, true);

        final Resources.Theme lilacTheme = mResources.newTheme();
        lilacTheme.applyStyle(R.style.LilacTheme, true);

        Drawable themedYellowDrawableForMediumDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_MEDIUM, yellowTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed yellow density-aware drawable load : medium color",
                themedYellowDrawableForMediumDensity, 0xFFF0B000);

        Drawable themedLilacDrawableForMediumDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_MEDIUM, lilacTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed lilac density-aware drawable load : medium color",
                themedLilacDrawableForMediumDensity, 0xFFF080F0);

        Drawable themedYellowDrawableForHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_HIGH, yellowTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed yellow density-aware drawable load : high color",
                themedYellowDrawableForHighDensity, 0xFFF0B000);

        Drawable themedLilacDrawableForHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_HIGH, lilacTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed lilac density-aware drawable load : high color",
                themedLilacDrawableForHighDensity, 0xFFF080F0);

        Drawable themedYellowDrawableForXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_XHIGH, yellowTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed yellow density-aware drawable load : xhigh color",
                themedYellowDrawableForXHighDensity, 0xFFF0B000);

        Drawable themedLilacDrawableForXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_XHIGH, lilacTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed lilac density-aware drawable load : xhigh color",
                themedLilacDrawableForXHighDensity, 0xFFF080F0);

        Drawable themedYellowDrawableForXXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_XXHIGH, yellowTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed yellow density-aware drawable load : xxhigh color",
                themedYellowDrawableForXXHighDensity, 0xFFF0B000);

        Drawable themedLilacDrawableForXXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.themed_bitmap,
                        DisplayMetrics.DENSITY_XXHIGH, lilacTheme);
        // We should get a drawable that corresponds to the theme requested in the API call.
        TestUtils.assertAllPixelsOfColor("Themed lilac density-aware drawable load : xxhigh color",
                themedLilacDrawableForXXHighDensity, 0xFFF080F0);
    }

    @Test(expected = Resources.NotFoundException.class)
    public void testGetFont_invalidResourceId() {
        ResourcesCompat.getFont(mContext, -1);
    }

    @Test
    public void testGetFont_fontFile_sync() {
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplefont);

        assertNotNull(font);
        assertNotSame(Typeface.DEFAULT, font);
    }

    @Test
    public void testGetFont_adjustDisplayStyle() {
        Typeface tf = ResourcesCompat.getFont(mContext, R.font.thin_italic);

        assertNotNull(tf);
        if (SDK_INT >= 28) {
            assertEquals(100, tf.getWeight());
        }
        assertEquals(Typeface.ITALIC, tf.getStyle());
    }

    @Test
    public void testGetFont_fontFile_cached() {
        TypefaceCompat.clearCache();
        assertNull(ResourcesCompat.getCachedFont(mContext, R.font.samplefont));
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplefont);
        Typeface cachedFont = ResourcesCompat.getCachedFont(mContext, R.font.samplefont);
        assertNotNull(cachedFont);
        assertSame(font, cachedFont);
    }

    private static final class FontCallback extends ResourcesCompat.FontCallback {
        private final CountDownLatch mLatch;
        Typeface mTypeface;

        FontCallback(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void onFontRetrieved(@NonNull Typeface typeface) {
            mTypeface = typeface;
            mLatch.countDown();
        }

        @Override
        public void onFontRetrievalFailed(int reason) {
            mLatch.countDown();
        }
    }

    @Test
    public void testGetFont_fontFile_async() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);
        FontsContractCompat.resetTypefaceCache();

        ResourcesCompat.getFont(mContext, R.font.samplefont, callback, null);

        assertTrue(latch.await(5L, TimeUnit.SECONDS));

        assertNotNull(callback.mTypeface);
        assertNotSame(Typeface.DEFAULT, callback.mTypeface);
    }

    @Test
    public void testGetFont_xmlFile_sync() {
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplexmlfont);

        assertNotNull(font);
        assertNotSame(Typeface.DEFAULT, font);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testGetFont_xmlFile_sync_29_hasRightWeight() {
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplexmlfont_medium);

        assertNotNull(font);
        assertEquals(500, font.getWeight());
    }

    @Test
    public void testGetFont_xmlFile_async() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        ResourcesCompat.getFont(mContext, R.font.samplexmlfont, callback, null);

        assertTrue(latch.await(5L, TimeUnit.SECONDS));

        assertNotNull(callback.mTypeface);
        assertNotSame(Typeface.DEFAULT, callback.mTypeface);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testGetFont_xmlFile_async_29_hasRightWeight() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        ResourcesCompat.getFont(mContext, R.font.samplexmlfont_medium, callback, null);

        assertTrue(latch.await(5L, TimeUnit.SECONDS));

        assertNotNull(callback.mTypeface);
        assertEquals(500, callback.mTypeface.getWeight());
    }

    @Test(expected = Resources.NotFoundException.class)
    public void testGetFont_brokenFont() {
        ResourcesCompat.getFont(mContext, R.font.invalid_font);
    }

    @Test
    public void testGetFont_xmlProviderFile_sync() {
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplexmldownloadedfont);

        assertNotNull(font);
        assertNotSame(Typeface.DEFAULT, font);
    }

    @Test
    public void testGetFont_xmlProviderFile_async() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        // Font provider non-blocking requests post on the calling thread so can't run on
        // the test thread as it doesn't have a Looper.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ResourcesCompat.getFont(mContext, R.font.samplexmldownloadedfont, callback, null);
            }
        });

        assertTrue(latch.await(5L, TimeUnit.SECONDS));

        assertNotNull(callback.mTypeface);
        assertNotSame(Typeface.DEFAULT, callback.mTypeface);
    }

    @Test
    public void testGetFont_invalidXmlFile() {
        try {
            assertNull(
                    ResourcesCompat.getFont(mContext, R.font.invalid_xmlfamily));
        } catch (Resources.NotFoundException e) {
            // pass
        }

        try {
            assertNull(ResourcesCompat.getFont(mContext, R.font.invalid_xmlempty));
        } catch (Resources.NotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetFont_fontFileIsCached() {
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplefont);
        Typeface font2 = ResourcesCompat.getFont(mContext, R.font.samplefont);

        assertSame(font, font2);
    }

    @Test
    public void testGetFont_xmlFileIsCached() {
        Typeface font = ResourcesCompat.getFont(mContext, R.font.samplexmlfont);
        Typeface font2 = ResourcesCompat.getFont(mContext, R.font.samplexmlfont);

        assertSame(font, font2);
    }

    @FlakyTest(bugId = 190534138)
    @Test
    public void testSystemFontFamilyReturnsSystemFont() {
        Typeface typeface = ResourcesCompat.getFont(mContext, R.font.samplexmldownloadedfont);
        assertNotNull(typeface);
        assertEquals(typeface,  Typeface.create("serif", Typeface.NORMAL));
    }

    @Test
    public void getFloatForFloat() {
        float value = ResourcesCompat.getFloat(mResources, R.dimen.twelve_point_five);
        assertEquals(12.5f, value, 0.01f);
    }

    @Test
    public void getFloatForNotAFloatThrows() {
        try {
            ResourcesCompat.getFloat(mResources, android.R.string.yes);
            fail();
        } catch (Resources.NotFoundException e) {
            assertEquals("Resource ID #0x1040013 type #0x3 is not valid", e.getMessage());
        }
    }

    @Test
    public void getFloatForMissingResourceThrows() {
        try {
            ResourcesCompat.getFloat(mResources, 42);
            fail();
        } catch (Resources.NotFoundException e) {
            assertEquals("Resource ID #0x2a", e.getMessage());
        }
    }

    @Test
    public void testMutateTransitionDrawable() {
        Drawable drawable = ResourcesCompat.getDrawable(mResources,
                R.drawable.test_transition_drawable, null);
        assertTrue(drawable instanceof TransitionDrawable);

        Drawable mutated = drawable.mutate();
        assertTrue(drawable instanceof TransitionDrawable);
        assertTrue(mutated instanceof TransitionDrawable);
    }
    @Test
    public void testClearCachesForTheme() {
        Resources.Theme theme = mResources.newTheme();
        ColorStateList csl = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list, theme);

        // Modify the contents of the theme.
        theme.applyStyle(android.R.style.Theme_Material, true);
        ColorStateList csl2 = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list, theme);

        if (Build.VERSION.SDK_INT < 33) {
            // Validate the failure case that's being worked around.
            assertEquals(csl, csl2);
        } else {
            // Theme.hashCode() was implemented in T, so the workaround is not needed.
            assertNotEquals(csl, csl2);
        }

        ResourcesCompat.clearCachesForTheme(theme);

        // Validate the workaround yields a correct result for all platforms.
        ColorStateList csl3 = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list, theme);
        assertNotEquals(csl, csl3);
    }

    @RequiresApi(31)
    private File getDrawingFont(String text, Typeface typeface) {
        Paint paint = new Paint();
        paint.setTypeface(typeface);
        paint.setTextSize(10f);

        PositionedGlyphs glyphs = TextRunShaper.shapeTextRun(
                text, 0, text.length(), 0, text.length(), 0f, 0f, false, paint);
        if (glyphs.glyphCount() < 1) {
            return null;
        } else {
            return glyphs.getFont(0).getFile();
        }
    }

    @Test
    public void testGetSystemFontFamilyWithFallback() {
        Typeface typeface = ResourcesCompat.getFont(mContext, R.font.system_fallback);
        assertNotNull(typeface);  // synchronously fetched
        // font/res/system_fallback.xml specify "serif" in the first fallback.
        assertEquals(typeface, Typeface.create("serif", Typeface.NORMAL));
    }

    @Test
    public void testGetSystemFontFamilyWithFallback_equivalentLegacy() {
        Typeface typeface = ResourcesCompat.getFont(mContext, R.font.system_fallback);
        Typeface legacy_typeface = ResourcesCompat.getFont(mContext, R.font.system_fallback_legacy);
        assertEquals(typeface, legacy_typeface);
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    public void testGetSystemFontFamilyWithFallback_monospace() {
        Typeface typeface = ResourcesCompat.getFont(mContext, R.font.system_fallback_multiple);
        assertNotNull(typeface);  // synchronously fetched

        // font/res/system_fallback_multiple.xml fallbacks from serif to monospace.
        // serif -> monospace fallback Typeface is different from serif and monospace Typeface.
        assertNotEquals(typeface, Typeface.create("serif", Typeface.NORMAL));
        assertNotEquals(typeface, Typeface.create("monospace", Typeface.NORMAL));

        // Verify serif is the top font.
        File topFont = getDrawingFont(" ", typeface);
        assertEquals(topFont, getDrawingFont(" ", Typeface.create("serif", Typeface.NORMAL)));
    }
}
