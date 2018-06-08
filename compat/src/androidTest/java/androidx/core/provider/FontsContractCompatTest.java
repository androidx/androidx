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
package androidx.core.provider;

import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_FONT_NOT_FOUND;
import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_FONT_UNAVAILABLE;
import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_MALFORMED_QUERY;
import static androidx.core.provider.FontsContractCompat.Columns.RESULT_CODE_OK;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.graphics.Typeface;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.provider.FontsContractCompat.FontInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link FontsContractCompat}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FontsContractCompatTest {
    private static final String AUTHORITY = "androidx.core.provider.fonts.font";
    private static final String PACKAGE = "androidx.core.test";

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

    private static final byte[] BYTE_ARRAY =
            Base64.decode("e04fd020ea3a6910a2d808002b30", Base64.DEFAULT);
    // Use a different instance to test byte array comparison
    private static final byte[] BYTE_ARRAY_COPY =
            Base64.decode("e04fd020ea3a6910a2d808002b30", Base64.DEFAULT);
    private static final byte[] BYTE_ARRAY_2 =
            Base64.decode("e04fd020ea3a6910a2d808002b32", Base64.DEFAULT);
    private Instrumentation mInstrumentation;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        MockFontProvider.prepareFontFiles(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @After
    public void tearDown() {
        MockFontProvider.cleanUpFontFiles(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void typefaceNotCacheTest() throws NameNotFoundException {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.SINGLE_FONT_FAMILY_QUERY, SIGNATURE);
        FontFamilyResult result = FontsContractCompat.fetchFonts(
                mContext, null /* cancellation signal */, request);
        assertEquals(FontFamilyResult.STATUS_OK, result.getStatusCode());
        Typeface typeface = FontsContractCompat.buildTypeface(
                mContext, null /* cancellation signal */, result.getFonts());

        FontFamilyResult result2 = FontsContractCompat.fetchFonts(
                mContext, null /* cancellation signal */, request);
        assertEquals(FontFamilyResult.STATUS_OK, result2.getStatusCode());
        Typeface typeface2 = FontsContractCompat.buildTypeface(
                mContext, null /* cancellation signal */, result2.getFonts());

        // Neither fetchFonts nor buildTypeface should cache the Typeface.
        assertNotSame(typeface, typeface2);
    }

    @Test
    public void testGetFontFromProvider_resultOK() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.SINGLE_FONT_FAMILY2_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);
        assertNotNull(fonts);
        assertEquals(1, fonts.length);
        FontInfo font = fonts[0];
        assertEquals(0, font.getTtcIndex());
        assertEquals(700, font.getWeight());
        assertTrue(font.isItalic());
        assertNotNull(font.getUri());
        assertEquals(RESULT_CODE_OK, font.getResultCode());
    }

    @Test
    public void testGetFontFromProvider_providerDoesntReturnAllFields() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.MANDATORY_FIELDS_ONLY_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);
        assertNotNull(fonts);
        assertEquals(1, fonts.length);
        FontInfo font = fonts[0];
        assertEquals(0, font.getTtcIndex());
        assertEquals(RESULT_CODE_OK, font.getResultCode());
    }

    @Test
    public void testGetFontFromProvider_resultFontNotFound() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NOT_FOUND_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);
        assertNotNull(fonts);
        assertEquals(1, fonts.length);
        FontInfo font = fonts[0];
        assertEquals(RESULT_CODE_FONT_NOT_FOUND, font.getResultCode());
    }

    @Test
    public void testGetFontFromProvider_resultFontUnavailable() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.UNAVAILABLE_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);

        assertNotNull(fonts);
        assertEquals(1, fonts.length);
        FontInfo font = fonts[0];
        assertEquals(RESULT_CODE_FONT_UNAVAILABLE, font.getResultCode());
    }

    @Test
    public void testGetFontFromProvider_resultMalformedQuery() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.MALFORMED_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);

        assertNotNull(fonts);
        assertEquals(1, fonts.length);
        FontInfo font = fonts[0];
        assertEquals(RESULT_CODE_MALFORMED_QUERY, font.getResultCode());
    }

    @Test
    public void testGetFontFromProvider_resultFontNotFoundSecondRow() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NOT_FOUND_SECOND_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);

        assertNotNull(fonts);
        assertEquals(2, fonts.length);

        FontInfo font = fonts[0];
        assertEquals(RESULT_CODE_OK, font.getResultCode());

        font = fonts[1];
        assertEquals(RESULT_CODE_FONT_NOT_FOUND, font.getResultCode());
    }

    @Test
    public void testGetFontFromProvider_resultFontNotFoundOtherRow() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NOT_FOUND_THIRD_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);

        assertNotNull(fonts);
        assertEquals(3, fonts.length);

        FontInfo font = fonts[0];
        assertEquals(RESULT_CODE_OK, font.getResultCode());

        font = fonts[1];
        assertEquals(RESULT_CODE_FONT_NOT_FOUND, font.getResultCode());

        font = fonts[2];
        assertEquals(RESULT_CODE_OK, font.getResultCode());
    }

    public void testGetFontFromProvider_resultCodeIsNegativeNumber() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NEGATIVE_ERROR_CODE_QUERY, SIGNATURE);
        FontInfo[] fonts = FontsContractCompat.getFontFromProvider(
                mContext, request, AUTHORITY, null);


        assertNotNull(fonts);
        assertEquals(1, fonts.length);
        FontInfo font = fonts[0];
        assertEquals(RESULT_CODE_FONT_NOT_FOUND, font.getResultCode());
    }

    @Test
    public void testGetProvider_providerNotFound() {
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.resolveContentProvider(anyString(), anyInt())).thenReturn(null);

        FontRequest request = new FontRequest(AUTHORITY, PACKAGE, "query", SIGNATURE);
        try {
            FontsContractCompat.getProvider(packageManager, request, null);
            fail();
        } catch (NameNotFoundException e) {
            // pass
        }
    }

    @Test
    public void testGetProvider_noCerts()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mContext.getPackageManager();

        List<List<byte[]>> emptyList = Collections.emptyList();

        FontRequest request = new FontRequest(AUTHORITY, PACKAGE, "query", emptyList);
        assertNull(FontsContractCompat.getProvider(packageManager, request, null));
    }

    @Test
    public void testGetProvider_wrongCerts()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        setupPackageManager(packageManager);

        byte[] wrongCert = Base64.decode("this is a wrong cert", Base64.DEFAULT);
        List<byte[]> certList = Arrays.asList(wrongCert);
        FontRequest requestWrongCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));

        assertNull(FontsContractCompat.getProvider(packageManager, requestWrongCerts, null));
    }

    @Test
    public void testGetProvider_correctCerts()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        ProviderInfo info = setupPackageManager(packageManager);

        List<byte[]> certList = Arrays.asList(BYTE_ARRAY);
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        ProviderInfo result =
                FontsContractCompat.getProvider(packageManager, requestRightCerts, null);

        assertEquals(info, result);
    }

    @Test
    public void testGetProvider_moreCerts()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        setupPackageManager(packageManager);

        byte[] wrongCert = Base64.decode("this is a wrong cert", Base64.DEFAULT);
        List<byte[]> certList = Arrays.asList(wrongCert, BYTE_ARRAY);
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        assertNull(FontsContractCompat.getProvider(packageManager, requestRightCerts, null));
    }

    @Test
    public void testGetProvider_duplicateCerts()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        setupPackageManager(packageManager);
        PackageInfo packageInfo = new PackageInfo();
        Signature signature = mock(Signature.class);
        when(signature.toByteArray()).thenReturn(BYTE_ARRAY_COPY);
        Signature signature2 = mock(Signature.class);
        when(signature2.toByteArray()).thenReturn(BYTE_ARRAY_COPY);
        packageInfo.packageName = PACKAGE;
        packageInfo.signatures = new Signature[] { signature, signature2 };
        when(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);

        // The provider has {BYTE_ARRAY_COPY, BYTE_ARRAY_COPY}, the request has
        // {BYTE_ARRAY_2, BYTE_ARRAY_COPY}.
        List<byte[]> certList = Arrays.asList(BYTE_ARRAY_2, BYTE_ARRAY_COPY);
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        assertNull(FontsContractCompat.getProvider(packageManager, requestRightCerts, null));
    }

    @Test
    public void testGetProvider_correctCertsSeveralSets()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mock(PackageManager.class);
        ProviderInfo info = setupPackageManager(packageManager);

        List<List<byte[]>> certList = new ArrayList<>();
        byte[] wrongCert = Base64.decode("this is a wrong cert", Base64.DEFAULT);
        certList.add(Arrays.asList(wrongCert));
        certList.add(Arrays.asList(BYTE_ARRAY));
        FontRequest requestRightCerts = new FontRequest(AUTHORITY, PACKAGE, "query", certList);
        ProviderInfo result =
                FontsContractCompat.getProvider(packageManager, requestRightCerts, null);

        assertEquals(info, result);
    }

    @Test
    public void testGetProvider_wrongPackage()
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mContext.getPackageManager();

        List<List<byte[]>> certList = new ArrayList<>();
        certList.add(Arrays.asList(BYTE_ARRAY));
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, "com.wrong.package.name", "query", certList);
        try {
            FontsContractCompat.getProvider(packageManager, requestRightCerts, null);
            fail();
        } catch (NameNotFoundException e) {
            // pass
        }
    }

    private ProviderInfo setupPackageManager(PackageManager packageManager)
            throws PackageManager.NameNotFoundException {
        ProviderInfo info = new ProviderInfo();
        info.packageName = PACKAGE;
        info.applicationInfo = new ApplicationInfo();
        when(packageManager.resolveContentProvider(anyString(), anyInt())).thenReturn(info);
        PackageInfo packageInfo = new PackageInfo();
        Signature signature = mock(Signature.class);
        when(signature.toByteArray()).thenReturn(BYTE_ARRAY_COPY);
        packageInfo.packageName = PACKAGE;
        packageInfo.signatures = new Signature[] { signature };
        when(packageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
        return info;
    }

    @Test
    public void testGetFontSync_invalidUri() throws InterruptedException {
        final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        final FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.INVALID_URI, SIGNATURE);
        final CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                FontsContractCompat.getFontSync(mContext, request, callback, null,
                        false /* isBlockingFetch */, 300 /* timeout */, Typeface.NORMAL);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNull(callback.mTypeface);
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
}
