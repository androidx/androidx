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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.v4.testutils.TestUtils;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.core.provider.FontsContractCompat;
import androidx.core.provider.MockFontProvider;
import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
public class ResourcesCompatTest {
    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mResources = mContext.getResources();
        MockFontProvider.prepareFontFiles(mContext);
    }

    @Test
    public void testGetColor() throws Throwable {
        assertEquals("Unthemed color load",
                ResourcesCompat.getColor(mResources, R.color.text_color, null),
                0xFFFF8090);

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getColor() in pre-v23 is undefined.
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

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getColorStateList() in pre-v23 is undefined.
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
    }

    @Test
    public void testGetDrawable() throws Throwable {
        final Drawable unthemedDrawable =
                ResourcesCompat.getDrawable(mResources, R.drawable.test_drawable_red, null);
        TestUtils.assertAllPixelsOfColor("Unthemed drawable load",
                unthemedDrawable, mResources.getColor(R.color.test_red));

        if (Build.VERSION.SDK_INT >= 23) {
            // The following tests are only expected to pass on v23+ devices. The result of
            // calling theme-aware getDrawable() in pre-v23 is undefined.
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
        final int expectedSizeForMediumDensity = (Build.VERSION.SDK_INT < 15) ?
                mResources.getDimensionPixelSize(R.dimen.density_aware_size) : 12;
        assertEquals("Unthemed density-aware drawable load: medium width",
                expectedSizeForMediumDensity, unthemedDrawableForMediumDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: medium height",
                expectedSizeForMediumDensity,
                unthemedDrawableForMediumDensity.getIntrinsicHeight());

        final Drawable unthemedDrawableForHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_HIGH, null);
        // For pre-v15 devices we should get a drawable that corresponds to the density of the
        // current device. For v15+ devices we should get a drawable that corresponds to the
        // density requested in the API call.
        final int expectedSizeForHighDensity = (Build.VERSION.SDK_INT < 15) ?
                mResources.getDimensionPixelSize(R.dimen.density_aware_size) : 21;
        assertEquals("Unthemed density-aware drawable load: high width",
                expectedSizeForHighDensity, unthemedDrawableForHighDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: high height",
                expectedSizeForHighDensity, unthemedDrawableForHighDensity.getIntrinsicHeight());

        final Drawable unthemedDrawableForXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_XHIGH, null);
        // For pre-v15 devices we should get a drawable that corresponds to the density of the
        // current device. For v15+ devices we should get a drawable that corresponds to the
        // density requested in the API call.
        final int expectedSizeForXHighDensity = (Build.VERSION.SDK_INT < 15) ?
                mResources.getDimensionPixelSize(R.dimen.density_aware_size) : 32;
        assertEquals("Unthemed density-aware drawable load: xhigh width",
                expectedSizeForXHighDensity, unthemedDrawableForXHighDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: xhigh height",
                expectedSizeForXHighDensity, unthemedDrawableForXHighDensity.getIntrinsicHeight());

        final Drawable unthemedDrawableForXXHighDensity =
                ResourcesCompat.getDrawableForDensity(mResources, R.drawable.density_aware_drawable,
                        DisplayMetrics.DENSITY_XXHIGH, null);
        // For pre-v15 devices we should get a drawable that corresponds to the density of the
        // current device. For v15+ devices we should get a drawable that corresponds to the
        // density requested in the API call.
        final int expectedSizeForXXHighDensity = (Build.VERSION.SDK_INT < 15) ?
                mResources.getDimensionPixelSize(R.dimen.density_aware_size) : 54;
        assertEquals("Unthemed density-aware drawable load: xxhigh width",
                expectedSizeForXXHighDensity, unthemedDrawableForXXHighDensity.getIntrinsicWidth());
        assertEquals("Unthemed density-aware drawable load: xxhigh height",
                expectedSizeForXXHighDensity,
                unthemedDrawableForXXHighDensity.getIntrinsicHeight());
    }

    @Test
    public void testGetDrawableForDensityThemed() throws Throwable {
        if (Build.VERSION.SDK_INT < 21) {
            // The following tests are only expected to pass on v21+ devices. The result of
            // calling theme-aware getDrawableForDensity() in pre-v21 is undefined.
            return;
        }

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
        FontsContractCompat.resetCache();

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
    public void testGetFont_xmlFile_async() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        ResourcesCompat.getFont(mContext, R.font.samplexmlfont, callback, null);

        assertTrue(latch.await(5L, TimeUnit.SECONDS));

        assertNotNull(callback.mTypeface);
        assertNotSame(Typeface.DEFAULT, callback.mTypeface);
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
}
