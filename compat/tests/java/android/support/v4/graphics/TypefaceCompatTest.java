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

import static android.content.res.AssetManager.ACCESS_BUFFER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.app.TestSupportActivity;
import android.support.v4.graphics.TypefaceCompat.FontRequestCallback;
import android.support.v4.graphics.fonts.FontRequest;
import android.support.v4.graphics.fonts.FontResult;
import android.support.v4.provider.FontsContractCompat;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link TypefaceCompatBaseImpl}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TypefaceCompatTest extends BaseInstrumentationTestCase<TestSupportActivity> {
    private static final String TEST_FONT_FILE = "samplefont.ttf";
    private static final String PROVIDER = "com.test.fontprovider.authority";
    private static final String QUERY_CACHED = "query_cached";
    private static final String QUERY = "query";
    private static final String PACKAGE = "com.test.fontprovider.package";
    private static final byte[] BYTE_ARRAY =
            Base64.decode("e04fd020ea3a6910a2d808002b30", Base64.DEFAULT);
    private static final List<List<byte[]>> CERTS = Arrays.asList(Arrays.asList(BYTE_ARRAY));

    private TypefaceCompatBaseImpl mCompat;

    public TypefaceCompatTest() {
        super(TestSupportActivity.class);
    }

    @Before
    public void setup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mCompat = new TypefaceCompatApi24Impl(mActivityTestRule.getActivity());
        } else {
            mCompat = new TypefaceCompatBaseImpl(mActivityTestRule.getActivity());
        }
        TypefaceCompatBaseImpl.putInCache(PROVIDER, QUERY_CACHED, Typeface.MONOSPACE);
    }

    @Test
    public void testReceiveResult_cachedResult() {
        FontRequestCallback callback = mock(FontRequestCallback.class);

        mCompat.receiveResult(new FontRequest(PROVIDER, PACKAGE, QUERY_CACHED, CERTS),
                callback, 0, null);

        verify(callback).onTypefaceRetrieved(Typeface.MONOSPACE);
    }

    @Test
    public void testReceiveResult_resultCodeProviderNotFound() {
        FontRequestCallback callback = mock(FontRequestCallback.class);

        mCompat.receiveResult(new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS), callback,
                FontsContractCompat.RESULT_CODE_PROVIDER_NOT_FOUND, null);

        verify(callback).onTypefaceRequestFailed(
                FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND);
    }

    @Test
    public void testReceiveResult_resultCodeFontNotFound() {
        FontRequestCallback callback = mock(FontRequestCallback.class);

        mCompat.receiveResult(new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS), callback,
                FontsContractCompat.Columns.RESULT_CODE_FONT_NOT_FOUND, null);

        verify(callback).onTypefaceRequestFailed(
                FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
    }

    @Test
    public void testReceiveResult_nullBundle() {
        FontRequestCallback callback = mock(FontRequestCallback.class);

        mCompat.receiveResult(new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS), callback,
                FontsContractCompat.Columns.RESULT_CODE_OK, null);

        verify(callback).onTypefaceRequestFailed(
                FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
    }

    @Test
    public void testReceiveResult_nullResult() {
        FontRequestCallback callback = mock(FontRequestCallback.class);

        mCompat.receiveResult(new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS), callback,
                FontsContractCompat.Columns.RESULT_CODE_OK, new Bundle());

        verify(callback).onTypefaceRequestFailed(
                FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
    }

    @Test
    public void testReceiveResult_emptyResult() {
        FontRequestCallback callback = mock(FontRequestCallback.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(
                FontsContractCompat.PARCEL_FONT_RESULTS, new ArrayList<FontResult>());

        mCompat.receiveResult(new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS), callback,
                FontsContractCompat.Columns.RESULT_CODE_OK, bundle);

        verify(callback).onTypefaceRequestFailed(
                FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
    }

    @Test
    public void testTypefaceRequestFailureConstantsAreInSync() {
        // Error codes from the provider are positive numbers and are in sync
        assertEquals(FontsContractCompat.Columns.RESULT_CODE_FONT_NOT_FOUND,
                TypefaceCompat.FontRequestCallback.FAIL_REASON_FONT_NOT_FOUND);
        assertEquals(FontsContractCompat.Columns.RESULT_CODE_FONT_UNAVAILABLE,
                TypefaceCompat.FontRequestCallback.FAIL_REASON_FONT_UNAVAILABLE);
        assertEquals(FontsContractCompat.Columns.RESULT_CODE_MALFORMED_QUERY,
                TypefaceCompat.FontRequestCallback.FAIL_REASON_MALFORMED_QUERY);

        // Internal errors are negative
        assertTrue(0 > TypefaceCompat.FontRequestCallback.FAIL_REASON_PROVIDER_NOT_FOUND);
        assertTrue(0 > TypefaceCompat.FontRequestCallback.FAIL_REASON_WRONG_CERTIFICATES);
        assertTrue(0 > TypefaceCompat.FontRequestCallback.FAIL_REASON_FONT_LOAD_ERROR);
    }

    private File loadFont() {
        File cacheFile = new File(mActivityTestRule.getActivity().getCacheDir(), TEST_FONT_FILE);
        try {
            copyToCacheFile(TEST_FONT_FILE, cacheFile);
            return cacheFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void copyToCacheFile(final String assetPath, final File cacheFile)
            throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = mActivityTestRule.getActivity().getAssets().open(assetPath, ACCESS_BUFFER);
            fos = new FileOutputStream(cacheFile, false);
            byte[] buffer = new byte[1024];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                fos.write(buffer, 0, readLen);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Test
    public void testCreateTypeface() throws IOException, InterruptedException {
        File file = loadFont();
        ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        try {
            FontResult result = new FontResult(pfd, 0, null, 400, false /* italic */);
            Typeface typeface = mCompat.createTypeface(Arrays.asList(result));

            assertNotNull(typeface);
        } finally {
            if (file != null) {
                file.delete();
            }
            if (pfd != null) {
                pfd.close();
            }
        }
    }
}
