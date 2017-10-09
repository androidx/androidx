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

package android.support.v4.content.res;

import static android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import static android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import static android.support.v4.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import static android.support.v4.content.res.FontResourcesParserCompat.ProviderResourceEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.support.compat.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.provider.FontRequest;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * Tests for {@link FontResourcesParserCompat}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class FontResourcesParserCompatTest {

    private Instrumentation mInstrumentation;
    private Resources mResources;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mResources = mInstrumentation.getContext().getResources();
    }

    @Test
    public void testParse() throws XmlPullParserException, IOException {
        @SuppressLint("ResourceType")
        XmlResourceParser parser = mResources.getXml(R.font.samplexmlfont);

        FamilyResourceEntry result = FontResourcesParserCompat.parse(parser, mResources);

        assertNotNull(result);
        FontFamilyFilesResourceEntry filesEntry = (FontFamilyFilesResourceEntry) result;
        FontFileResourceEntry[] fileEntries = filesEntry.getEntries();
        assertEquals(4, fileEntries.length);
        FontFileResourceEntry font1 = fileEntries[0];
        assertEquals(400, font1.getWeight());
        assertEquals(false, font1.isItalic());
        assertEquals(R.font.samplefont, font1.getResourceId());
        FontFileResourceEntry font2 = fileEntries[1];
        assertEquals(400, font2.getWeight());
        assertEquals(true, font2.isItalic());
        assertEquals(R.font.samplefont2, font2.getResourceId());
        FontFileResourceEntry font3 = fileEntries[2];
        assertEquals(700, font3.getWeight());
        assertEquals(false, font3.isItalic());
        assertEquals(R.font.samplefont3, font3.getResourceId());
        FontFileResourceEntry font4 = fileEntries[3];
        assertEquals(700, font4.getWeight());
        assertEquals(true, font4.isItalic());
        assertEquals(R.font.samplefont4, font4.getResourceId());
    }

    @Test
    public void testParseAndroidAttrs() throws XmlPullParserException, IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            // The following tests are only expected to pass on v22+ devices. The android
            // resources are stripped in older versions and hence won't be parsed.
            return;
        }

        @SuppressLint("ResourceType")
        XmlResourceParser parser = mResources.getXml(R.font.samplexmlfont2);

        FamilyResourceEntry result = FontResourcesParserCompat.parse(parser, mResources);

        assertNotNull(result);
        FontFamilyFilesResourceEntry filesEntry = (FontFamilyFilesResourceEntry) result;
        FontFileResourceEntry[] fileEntries = filesEntry.getEntries();
        assertEquals(4, fileEntries.length);
        FontFileResourceEntry font1 = fileEntries[0];
        assertEquals(400, font1.getWeight());
        assertEquals(false, font1.isItalic());
        assertEquals(R.font.samplefont, font1.getResourceId());
        FontFileResourceEntry font2 = fileEntries[1];
        assertEquals(400, font2.getWeight());
        assertEquals(true, font2.isItalic());
        assertEquals(R.font.samplefont2, font2.getResourceId());
        FontFileResourceEntry font3 = fileEntries[2];
        assertEquals(700, font3.getWeight());
        assertEquals(false, font3.isItalic());
        assertEquals(R.font.samplefont3, font3.getResourceId());
        FontFileResourceEntry font4 = fileEntries[3];
        assertEquals(700, font4.getWeight());
        assertEquals(true, font4.isItalic());
        assertEquals(R.font.samplefont4, font4.getResourceId());
    }

    @Test
    public void testParseDownloadableFont() throws IOException, XmlPullParserException {
        @SuppressLint("ResourceType")
        XmlResourceParser parser = mResources.getXml(R.font.samplexmldownloadedfont);

        FamilyResourceEntry result = FontResourcesParserCompat.parse(parser, mResources);

        assertNotNull(result);
        ProviderResourceEntry providerEntry = (ProviderResourceEntry) result;
        FontRequest request = providerEntry.getRequest();
        assertEquals("android.support.provider.fonts.font",
                request.getProviderAuthority());
        assertEquals("android.support.compat.test", request.getProviderPackage());
        assertEquals("singleFontFamily", request.getQuery());
    }

    @Test
    public void testReadCertsSingleArray() {
        List<List<byte[]>> result = FontResourcesParserCompat.readCerts(mResources, R.array.certs1);

        assertEquals(1, result.size());
        List<byte[]> firstSet = result.get(0);
        assertEquals(2, firstSet.size());
        String firstValue = Base64.encodeToString(firstSet.get(0), Base64.DEFAULT).trim();
        assertEquals("MIIEqDCCA5CgAwIBAgIJANWFuGx9", firstValue);
        String secondValue = Base64.encodeToString(firstSet.get(1), Base64.DEFAULT).trim();
        assertEquals("UEChMHQW5kcm9pZDEQMA4GA=", secondValue);
    }

    @Test
    public void testReadCertsMultiArray() {
        List<List<byte[]>> result =
                FontResourcesParserCompat.readCerts(mResources, R.array.certarray);

        assertEquals(2, result.size());
        List<byte[]> firstSet = result.get(0);
        assertEquals(2, firstSet.size());
        String firstValue = Base64.encodeToString(firstSet.get(0), Base64.DEFAULT).trim();
        assertEquals("MIIEqDCCA5CgAwIBAgIJANWFuGx9", firstValue);
        String secondValue = Base64.encodeToString(firstSet.get(1), Base64.DEFAULT).trim();
        assertEquals("UEChMHQW5kcm9pZDEQMA4GA=", secondValue);
        List<byte[]> secondSet = result.get(1);
        assertEquals(2, secondSet.size());
        String thirdValue = Base64.encodeToString(secondSet.get(0), Base64.DEFAULT).trim();
        assertEquals("MDEyMzM2NTZaMIGUMQswCQYD", thirdValue);
        String fourthValue = Base64.encodeToString(secondSet.get(1), Base64.DEFAULT).trim();
        assertEquals("DHThvbbR24kT9ixcOd9W+EY=", fourthValue);
    }
}
