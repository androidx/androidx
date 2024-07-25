/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.graphics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import androidx.core.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.provider.FontRequest;
import androidx.core.provider.FontsContractCompat;
import androidx.core.provider.MockFontProvider;
import androidx.core.test.R;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.WeightStyleFont;

import com.google.common.truth.Truth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@SuppressWarnings("deprecation")
public class TypefaceCompatTest {

    public Context mContext;
    public Resources mResources;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mResources = mContext.getResources();
        MockFontProvider.prepareFontFiles(mContext);
    }

    @After
    public void tearDown() {
        MockFontProvider.cleanUpFontFiles(mContext);
    }

    // Signature to be used for authentication to access content provider.
    // In this test case, the content provider and consumer live in the same package, self package's
    // signature works.
    private static final List<List<byte[]>> SIGNATURE;
    static {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            ArrayList<byte[]> out = new ArrayList<>();
            for (Signature sig : info.signatures) {
                out.add(sig.toByteArray());
            }
            SIGNATURE = new ArrayList<>();
            SIGNATURE.add(out);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to get the used font resource id by typeface.
     *
     * If the typeface is created from one of the R.font.large_a, R.font.large_b, R.font.large_c or
     * R.font.large_d resource, this method returns the resource id used by the typeface.
     */
    private static int getSelectedFontResourceId(Typeface typeface) {
        // The glyph for "a" in R.font.large_a font has a 3em width and glyph for "b", "c" and "d"
        // have 1em width. Similarly, The glyph for "b" in R.font.large_b font, the glyph for "c"
        // in R.font.large_c font, the glyph for "d" in R.font.large_d font has 3em width and the
        // glyph for the rest characters have 1em. Thus we can get the resource id of the source
        // font file by comparing width of "a", "b", "c" and "d".
        Paint p = new Paint();
        p.setTypeface(typeface);
        final int[] ids = { R.font.large_a, R.font.large_b, R.font.large_c, R.font.large_d };
        final float[] widths = {
            p.measureText("a"), p.measureText("b"), p.measureText("c"), p.measureText("d")
        };

        int maxIndex = Integer.MIN_VALUE;
        float maxValue = Float.MIN_VALUE;
        for (int i = 0; i < widths.length; ++i) {
            if (maxValue < widths[i]) {
                maxIndex = i;
                maxValue = widths[i];
            }
        }
        return ids[maxIndex];
    }

    /**
     * Helper method to obtain ProviderResourceEntry with overwriting correct signatures.
     */
    private ProviderResourceEntry getProviderResourceEntry(int id) {
        final ProviderResourceEntry entry;
        try {
            entry = (ProviderResourceEntry) FontResourcesParserCompat.parse(
                    mResources.getXml(id), mResources);
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
        final FontRequest parsedRequest = entry.getRequest();
        final FontRequest request = new FontRequest(parsedRequest.getProviderAuthority(),
                parsedRequest.getProviderPackage(), parsedRequest.getQuery(), SIGNATURE);
        return new ProviderResourceEntry(request, null /* fallbackRequest */,
                entry.getFetchStrategy(), entry.getTimeout(), entry.getSystemFontFamilyName());
    }

    public static class FontCallback extends ResourcesCompat.FontCallback {
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
    public void testCreateFromResourcesFamilyXml_resourceFont_NORMAL_asyncloading()
            throws Exception {
        final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont,
                        "res/font/styletest_async_providerfont", 0,
                        Typeface.NORMAL, callback, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_a, getSelectedFontResourceId(callback.mTypeface));
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont_ITALIC_asyncloading()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback2 = new FontCallback(latch);
        final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont,
                        "res/font/styletest_async_providerfont", 0, Typeface.ITALIC,
                        callback2, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_b, getSelectedFontResourceId(callback2.mTypeface));
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont_BOLD_asyncloading()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback3 = new FontCallback(latch);
        final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont,
                        "res/font/styletest_async_providerfont", 0, Typeface.BOLD,
                        callback3, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_c, getSelectedFontResourceId(callback3.mTypeface));
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont_BOLD_ITALIC_asyncloading()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback4 = new FontCallback(latch);
        final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont,
                        "res/font/styletest_async_providerfont", 0,
                        Typeface.BOLD_ITALIC, callback4, null /* handler */,
                        false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_d, getSelectedFontResourceId(callback4.mTypeface));
    }

    @Test
    public void testProviderFont_xmlRequest() {
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.samplexmldownloadedfontblocking), mResources,
                R.font.samplexmldownloadedfontblocking,
                "res/font/samplexmldownloadedfontblocking", 0, Typeface.NORMAL, null,
                null /* handler */, true /* isXmlRequest */);

        assertNotNull(typeface);
        assertNotEquals(Typeface.DEFAULT, typeface);
    }

    @Test
    public void testProviderFont_nonXmlRequest_noCallback() {
        // If we don't give a callback, the request should be blocking.
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.samplexmldownloadedfontblocking), mResources,
                R.font.samplexmldownloadedfontblocking,
                "res/font/samplexmldownloadedfontblocking", 0, Typeface.NORMAL, null,
                null /* handler */, false /* isXmlRequest */);

        assertNotNull(typeface);
        assertNotEquals(Typeface.DEFAULT, typeface);
    }

    @Test
    public void testProviderFont_nonXmlRequest_withCallback() throws InterruptedException {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);
        FontsContractCompat.resetTypefaceCache();

        final Typeface[] result = new Typeface[1];
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[0] = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.samplexmldownloadedfontblocking),
                        mResources, R.font.samplexmldownloadedfontblocking,
                        "res/font/samplexmldownloadedfontblocking", 0, Typeface.NORMAL,
                        callback, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNotNull(callback.mTypeface);
        assertNull(result[0]);
    }

    @Test
    public void testProviderFont_nonXmlRequest_withCallback_cached() throws InterruptedException {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);
        FontsContractCompat.resetTypefaceCache();

        final Typeface[] result = new Typeface[2];
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[0] = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.samplexmldownloadedfontblocking),
                        mResources, R.font.samplexmldownloadedfontblocking,
                        "res/font/samplexmldownloadedfontblocking", 0, Typeface.NORMAL,
                        callback, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNotNull(callback.mTypeface);
        assertNull(result[0]);

        latch = new CountDownLatch(1);
        final FontCallback callback2 = new FontCallback(latch);

        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[1] = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.samplexmldownloadedfontblocking),
                        mResources, R.font.samplexmldownloadedfontblocking,
                        "res/font/samplexmldownloadedfontblocking", 0, Typeface.NORMAL,
                        callback2, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNotNull(callback2.mTypeface);
        assertNotNull(result[1]);
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont() throws Exception {
        @SuppressLint("ResourceType")
        // We are retrieving the XML font as an XML resource for testing purposes.
        final FamilyResourceEntry entry = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.styletestfont), mResources);
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, "res/font/styletestfont", 0, Typeface.NORMAL, null /* callback
                 */,
                null /* handler */,
                false /* isXmlRequest */);
        Typeface cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, "res/font/styletestfont", 0, Typeface.NORMAL);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.NORMAL);
        // styletestfont has a node of fontStyle="normal" fontWeight="400" font="@font/large_a".
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, "res/font/styletestfont", 0, Typeface.ITALIC,
                null /* callback */, null /* handler */, false /* isXmlRequest */);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, "res/font/styletestfont", 0, Typeface.ITALIC);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.ITALIC);
        // styletestfont has a node of fontStyle="italic" fontWeight="400" font="@font/large_b".
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, "res/font/styletestfont", 0, Typeface.BOLD,
                null /* callback */, null /* handler */, false /* isXmlRequest */);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, "res/font/styletestfont", 0, Typeface.BOLD);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.BOLD);
        // styletestfont has a node of fontStyle="normal" fontWeight="700" font="@font/large_c".
        assertEquals(R.font.large_c, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, "res/font/styletestfont", 0, Typeface.BOLD_ITALIC,
                null /* callback */, null /* handler */, false /* isXmlRequest */);
        cachedTypeface = TypefaceCompat.findFromCache(mResources, R.font.styletestfont,
                "res/font/styletestfont", 0, Typeface.BOLD_ITALIC);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.BOLD_ITALIC);
        // styletestfont has a node of fontStyle="italic" fontWeight="700" font="@font/large_d".
        assertEquals(R.font.large_d, getSelectedFontResourceId(typeface));
    }

    private Typeface getLargerTypeface(String text, Typeface typeface1, Typeface typeface2) {
        Paint p1 = new Paint();
        p1.setTypeface(typeface1);
        float width1 = p1.measureText(text);
        Paint p2 = new Paint();
        p2.setTypeface(typeface2);
        float width2 = p2.measureText(text);

        if (width1 > width2) {
            return typeface1;
        } else if (width1 < width2) {
            return typeface2;
        } else {
            assertTrue(false);
            return null;
        }
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceTtcFont() throws Exception {
        // Here we test that building typefaces by indexing in font collections works correctly.
        // We want to ensure that the built typefaces correspond to the fonts with the right index.
        // sample_font_collection.ttc contains two fonts (with indices 0 and 1). The first one has
        // glyph "a" of 3em width, and all the other glyphs 1em. The second one has glyph "b" of
        // 3em width, and all the other glyphs 1em. Hence, we can compare the width of these
        // glyphs to assert that ttc indexing works.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // Creating typefaces with ttc index was only supported in the API starting with N.
            return;
        }
        final FamilyResourceEntry entry1 = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.ttctestfont1), mResources);
        Typeface typeface1 = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry1,
                mResources, R.font.ttctestfont1, "res/font/ttctestfont1", 0,
                Typeface.NORMAL, null /* callback */, null /*handler */, false /* isXmlRequest */);
        assertNotNull(typeface1);
        final FamilyResourceEntry entry2 = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.ttctestfont2), mResources);
        Typeface typeface2 = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry2,
                mResources, R.font.ttctestfont2, "res/font/ttctestfont2", 0, Typeface.NORMAL,
                null /* callback */, null /*handler */, false /* isXmlRequest */);
        assertNotNull(typeface2);

        assertEquals(getLargerTypeface("a", typeface1, typeface2), typeface1);
        assertEquals(getLargerTypeface("b", typeface1, typeface2), typeface2);
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFontWithVariationSettings()
            throws Exception {
        // Here we test that specifying variation settings for fonts in XMLs works correctly.
        // We build typefaces from two families containing one font each, using the same font
        // resource, but having different values for the 'wdth' tag. Then we measure the painted
        // text to ensure that the tag affects the text width. The font resource used supports
        // the 'wdth' axis for the dash (-) character.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Variation settings are only supported on O and newer.
            return;
        }
        final FamilyResourceEntry entry1 = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.variationsettingstestfont1), mResources);
        Typeface typeface1 = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry1,
                mResources, R.font.variationsettingstestfont1,
                "res/font/variationsettingstestfont1", 0, Typeface.NORMAL, null /* callback */,
                null /*handler */, false /* isXmlRequest */);
        assertNotNull(typeface1);
        final FamilyResourceEntry entry2 = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.variationsettingstestfont2), mResources);
        Typeface typeface2 = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry2,
                mResources, R.font.variationsettingstestfont2,
                "res/font/variationsettingstestfont2", 0, Typeface.NORMAL, null /* callback */,
                null /*handler */, false /* isXmlRequest */);
        assertNotNull(typeface2);

        assertEquals(getLargerTypeface("-", typeface1, typeface2), typeface2);
    }

    @Test
    public void testCreateFromResourcesFontFile() {
        Typeface typeface = TypefaceCompat.createFromResourcesFontFile(mContext, mResources,
                R.font.large_a, "res/font/large_a.ttf", 0, Typeface.NORMAL);
        assertNotNull(typeface);
        Typeface cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.large_a, "res/font/large_a.ttf", 0, Typeface.NORMAL);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFontFile(mContext, mResources, R.font.large_b,
                "res/font/large_b.ttf", 0, Typeface.NORMAL);
        assertNotNull(typeface);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.large_b, "res/font/large_b.ttf", 0, Typeface.NORMAL);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));
    }

    @Test
    public void testApplyFontFromFamilyUsingStyle() {
        // Test we get the correct font from the family when specifying a style instead of
        // applying the style to the "normal" font. The fonts used have a specific property.
        // The first font has glyph "a" of 3em width, and all the other glyphs 1em
        // and the second one has glyph "c" of 3em width, and all the other glyphs 1em.
        final Typeface family = ResourcesCompat.getFont(mContext, R.font.styletestfont);
        assertNotNull(family);

        final AppCompatTextView appCompatTextView = new AppCompatTextView(
                new ContextThemeWrapper(mContext, R.style.Theme_AppCompat_Light));
        assertNotNull(appCompatTextView);

        appCompatTextView.setTypeface(family, Typeface.NORMAL);
        final Typeface large_a = appCompatTextView.getTypeface();
        assertEquals(R.font.large_a, getSelectedFontResourceId(large_a));

        appCompatTextView.setTypeface(family, Typeface.BOLD);
        final Typeface large_c = appCompatTextView.getTypeface();
        assertEquals(R.font.large_c, getSelectedFontResourceId(large_c));

        appCompatTextView.setTypeface(family, Typeface.NORMAL);
        final Typeface large_a2 = appCompatTextView.getTypeface();
        assertEquals(R.font.large_a, getSelectedFontResourceId(large_a2));
    }

    @Test
    public void testTypeFaceCompatCreate() {
        final Typeface family = ResourcesCompat.getFont(mContext, R.font.styletestfont);
        assertNotNull(family);

        final Typeface large_a = TypefaceCompat.create(mContext, family, Typeface.NORMAL);
        assertEquals(R.font.large_a, getSelectedFontResourceId(large_a));

        final Typeface large_c = TypefaceCompat.create(mContext, family, Typeface.BOLD);
        assertEquals(R.font.large_c, getSelectedFontResourceId(large_c));

        final Typeface large_a2 = TypefaceCompat.create(mContext, family, Typeface.NORMAL);
        assertEquals(R.font.large_a, getSelectedFontResourceId(large_a2));

        final Typeface defaultTypeface = TypefaceCompat.create(mContext, null, Typeface.NORMAL);
        assertNotNull(defaultTypeface);
    }

    @Test
    public void testTypeFaceCompatCreateWithExactStyle_upright() {
        doTypefaceCreate(false);
    }

    private void doTypefaceCreate(boolean italic) {
        final Typeface family = ResourcesCompat.getFont(mContext, R.font.weighttestfont);
        assertNotNull(family);

        final int[] weights = new int[]{100, 400, 900};
        final float[] widths = new float[weights.length];

        Paint p = new Paint();
        p.setTextSize(120);

        // Normal font style
        for (int i = 0, size = weights.length; i < size; i++) {
            final int weight = weights[i];
            final Typeface t = TypefaceCompat.create(mContext, family, weight, false);
            char wideChar = new WeightStyleFont().getWideCharacter(weight, italic);
            p.setTypeface(t);
            widths[i] = p.measureText("" + wideChar);
        }

        float expectedWeight = italic ? 120.0f : 360.0f;
        Truth.assertThat(widths).usingTolerance(0.1)
                .containsExactly(expectedWeight, expectedWeight, expectedWeight);
        // check the test validity by matching a never-matching char
        Truth.assertThat(p.measureText("" + WeightStyleFont.SkinnyChar)).isNotWithin(0.1f)
                .of(expectedWeight);
    }

    @Test
    public void testTypeFaceCompatCreateWithExactStyle_italic() {
        doTypefaceCreate(true);
    }

    @Test
    public void testTypeFaceCompatCreateWithExactStyle_italic_api14_to_16() {
        doTypefaceCreate(true);
    }

    @Test
    public void testTypeFaceCompatCreateWithExactStyle_fallbackFamily() {
        // Fallback family
        final Typeface defaultTypeface = TypefaceCompat.create(mContext, null, 400, false);
        assertNotNull(defaultTypeface);
    }

    @Test
    public void testTypeFaceCompatCreateWithExactStyle_preconditions() {
        // Preconditions
        try {
            TypefaceCompat.create(null, null, 400, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Truth.assertThat(expected).hasMessageThat()
                    .isEqualTo("Context cannot be null");
        }

        try {
            TypefaceCompat.create(mContext, null, -1, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Truth.assertThat(expected).hasMessageThat()
                    .isEqualTo("weight is out of range of [1, 1000] (too low)");
        }

        try {
            TypefaceCompat.create(mContext, null, 1001, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            Truth.assertThat(expected).hasMessageThat()
                    .isEqualTo("weight is out of range of [1, 1000] (too high)");
        }
    }
}
