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

package android.support.v4.graphics;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.compat.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.testutils.PollingCheck;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.MockFontProvider;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SdkSuppress(maxSdkVersion = 25)  // on API 26, use platform implementation.
@SmallTest
public class TypefaceCompatTest {
    private static final String AUTHORITY = "android.provider.fonts.font";
    private static final String PACKAGE = "android.support.compat.test";

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
        return new ProviderResourceEntry(request, entry.getFetchStrategy(), entry.getTimeout());
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont_syncloading() throws Exception {
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.styletest_sync_providerfont), mResources,
                R.font.styletest_sync_providerfont, Typeface.NORMAL, null /* TextView */);
        typeface = Typeface.create(typeface, Typeface.NORMAL);
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.styletest_sync_providerfont), mResources,
                R.font.styletest_sync_providerfont, Typeface.ITALIC, null /* TextView */);
        typeface = Typeface.create(typeface, Typeface.ITALIC);
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.styletest_sync_providerfont), mResources,
                R.font.styletest_sync_providerfont, Typeface.BOLD, null /* TextView */);
        typeface = Typeface.create(typeface, Typeface.BOLD);
        assertEquals(R.font.large_c, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.styletest_sync_providerfont), mResources,
                R.font.styletest_sync_providerfont, Typeface.BOLD_ITALIC, null /* TextView */);
        typeface = Typeface.create(typeface, Typeface.BOLD_ITALIC);
        assertEquals(R.font.large_d, getSelectedFontResourceId(typeface));
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont_asyncloading() throws Exception {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        final TextView textView = new TextView(mContext);
        PollingCheck.PollingCheckCondition condition = new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return textView.getTypeface() != null;
            }
        };

        textView.setTypeface(null);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.NORMAL, textView);
            }
        });
        PollingCheck.waitFor(condition);
        assertEquals(R.font.large_a, getSelectedFontResourceId(textView.getTypeface()));

        textView.setTypeface(null);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.ITALIC, textView);
            }
        });
        PollingCheck.waitFor(condition);
        assertEquals(R.font.large_b, getSelectedFontResourceId(textView.getTypeface()));

        textView.setTypeface(null);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.BOLD, textView);
            }
        });
        PollingCheck.waitFor(condition);
        assertEquals(R.font.large_c, getSelectedFontResourceId(textView.getTypeface()));

        textView.setTypeface(null);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.BOLD_ITALIC, textView);
            }
        });
        PollingCheck.waitFor(condition);
        assertEquals(R.font.large_d, getSelectedFontResourceId(textView.getTypeface()));
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont() throws Exception {
        final FamilyResourceEntry entry = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.styletestfont), mResources);
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.NORMAL, null /* text view */);
        assertEquals(typeface, TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.NORMAL));
        typeface = Typeface.create(typeface, Typeface.NORMAL);
        // styletestfont has a node of fontStyle="normal" fontWeight="400" font="@font/large_a".
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.ITALIC, null);
        assertEquals(typeface, TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.ITALIC));
        typeface = Typeface.create(typeface, Typeface.ITALIC);
        // styletestfont has a node of fontStyle="italic" fontWeight="400" font="@font/large_b".
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.BOLD, null);
        assertEquals(typeface, TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.BOLD));
        typeface = Typeface.create(typeface, Typeface.BOLD);
        // styletestfont has a node of fontStyle="normal" fontWeight="700" font="@font/large_c".
        assertEquals(R.font.large_c, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.BOLD_ITALIC, null);
        assertEquals(typeface, TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.BOLD_ITALIC));
        typeface = Typeface.create(typeface, Typeface.BOLD_ITALIC);
        // styletestfont has a node of fontStyle="italic" fontWeight="700" font="@font/large_d".
        assertEquals(R.font.large_d, getSelectedFontResourceId(typeface));
    }

    @Test
    public void testCreateFromResourcesFontFile() {
        Typeface typeface = TypefaceCompat.createFromResourcesFontFile(
                mContext, mResources, R.font.large_a, Typeface.NORMAL);
        assertEquals(typeface, TypefaceCompat.findFromCache(
                mResources, R.font.large_a, Typeface.NORMAL));
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFontFile(
                mContext, mResources, R.font.large_b, Typeface.NORMAL);
        assertEquals(typeface, TypefaceCompat.findFromCache(
                mResources, R.font.large_b, Typeface.NORMAL));
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));
    }
}
