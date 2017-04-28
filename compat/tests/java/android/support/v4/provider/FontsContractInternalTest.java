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

package android.support.v4.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.graphics.fonts.FontRequest;
import android.support.v4.graphics.fonts.FontResult;
import android.support.v4.os.ResultReceiver;
import android.support.v4.provider.FontsContractCompat.Columns;
import android.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link FontsContractInternal}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FontsContractInternalTest {
    private static final String AUTHORITY = "android.provider.fonts.font";
    private static final String PACKAGE = "android.support.compat.test";

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

    private FontsContractInternal mContract;
    private ResultReceiver mResultReceiver;
    private PackageManager mPackageManager;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        MockFontProvider.prepareFontFiles(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        mPackageManager = mock(PackageManager.class);
        mContract = new FontsContractInternal(mContext, mPackageManager);
        mResultReceiver = mock(ResultReceiver.class);
    }

    @After
    public void tearDown() {
        MockFontProvider.cleanUpFontFiles(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test
    public void testGetFontFromProvider_resultOK() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.SINGLE_FONT_FAMILY2_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        final ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(mResultReceiver).send(eq(
                Columns.RESULT_CODE_OK), bundleCaptor.capture());

        Bundle bundle = bundleCaptor.getValue();
        assertNotNull(bundle);
        List<FontResult> resultList =
                bundle.getParcelableArrayList(FontsContractInternal.PARCEL_FONT_RESULTS);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        FontResult fontResult = resultList.get(0);
        assertEquals(0, fontResult.getTtcIndex());
        assertEquals("'wght' 100", fontResult.getFontVariationSettings());
        assertEquals(700, fontResult.getWeight());
        assertTrue(fontResult.getItalic());
        assertNotNull(fontResult.getFileDescriptor());
    }

    @Test
    public void testGetFontFromProvider_providerDoesntReturnAllFields() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.MANDATORY_FIELDS_ONLY_QUERY, SIGNATURE);
        final ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);
        verify(mResultReceiver).send(eq(
                Columns.RESULT_CODE_OK), bundleCaptor.capture());

        Bundle bundle = bundleCaptor.getValue();
        assertNotNull(bundle);
        List<FontResult> resultList =
                bundle.getParcelableArrayList(FontsContractInternal.PARCEL_FONT_RESULTS);
        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        FontResult fontResult = resultList.get(0);
        assertEquals(0, fontResult.getTtcIndex());
        assertNull(fontResult.getFontVariationSettings());
        assertEquals(400, fontResult.getWeight());
        assertFalse(fontResult.getItalic());
        assertNotNull(fontResult.getFileDescriptor());
    }

    @Test
    public void testGetFontFromProvider_resultFontNotFound() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NOT_FOUND_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        verify(mResultReceiver).send(Columns.RESULT_CODE_FONT_NOT_FOUND, null);
    }

    @Test
    public void testGetFontFromProvider_resultFontUnavailable() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.UNAVAILABLE_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        verify(mResultReceiver).send(Columns.RESULT_CODE_FONT_UNAVAILABLE, null);
    }

    @Test
    public void testGetFontFromProvider_resultMalformedQuery() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.MALFORMED_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        verify(mResultReceiver).send(Columns.RESULT_CODE_MALFORMED_QUERY, null);
    }

    @Test
    public void testGetFontFromProvider_resultFontNotFoundSecondRow() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NOT_FOUND_SECOND_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        verify(mResultReceiver).send(Columns.RESULT_CODE_FONT_NOT_FOUND, null);
    }

    @Test
    public void testGetFontFromProvider_resultFontNotFoundOtherRow() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NOT_FOUND_THIRD_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        verify(mResultReceiver).send(Columns.RESULT_CODE_FONT_NOT_FOUND, null);
    }

    @Test
    public void testGetFontFromProvider_resultCodeIsNegativeNumber() {
        FontRequest request = new FontRequest(
                AUTHORITY, PACKAGE, MockFontProvider.NEGATIVE_ERROR_CODE_QUERY, SIGNATURE);
        mContract.getFontFromProvider(request, mResultReceiver, AUTHORITY);

        verify(mResultReceiver).send(Columns.RESULT_CODE_FONT_NOT_FOUND, null);
    }

    @Test
    public void testGetProvider_providerNotFound() {
        when(mPackageManager.resolveContentProvider(anyString(), anyInt())).thenReturn(null);

        FontRequest request = new FontRequest(AUTHORITY, PACKAGE, "query", SIGNATURE);
        ProviderInfo result = mContract.getProvider(request, mResultReceiver);

        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_PROVIDER_NOT_FOUND, null);
        assertNull(result);
    }

    @Test
    public void testGetProvider_noCertSets()
            throws PackageManager.NameNotFoundException {
        setupPackageManager();

        FontRequest request = new FontRequest(AUTHORITY, PACKAGE,
                "query", new ArrayList<List<byte[]>>());
        ProviderInfo result = mContract.getProvider(request, mResultReceiver);

        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_WRONG_CERTIFICATES, null);
        assertNull(result);
    }

    @Test
    public void testGetProvider_noCerts()
            throws PackageManager.NameNotFoundException {
        setupPackageManager();

        FontRequest request = new FontRequest(AUTHORITY, PACKAGE,
                "query", Arrays.<List<byte[]>>asList(new ArrayList<byte[]>()));
        ProviderInfo result = mContract.getProvider(request, mResultReceiver);

        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_WRONG_CERTIFICATES, null);
        assertNull(result);
    }

    @Test
    public void testGetProvider_wrongCerts()
            throws PackageManager.NameNotFoundException {
        setupPackageManager();

        byte[] wrongCert = Base64.decode("this is a wrong cert", Base64.DEFAULT);
        List<byte[]> certList = Arrays.asList(wrongCert);
        FontRequest requestWrongCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        ProviderInfo result = mContract.getProvider(requestWrongCerts, mResultReceiver);

        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_WRONG_CERTIFICATES, null);
        assertNull(result);
    }

    @Test
    public void testGetProvider_correctCerts()
            throws PackageManager.NameNotFoundException {
        ProviderInfo info = setupPackageManager();

        List<byte[]> certList = Arrays.asList(BYTE_ARRAY);
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        ProviderInfo result = mContract.getProvider(requestRightCerts, mResultReceiver);

        verifyZeroInteractions(mResultReceiver);
        assertEquals(info, result);
    }

    @Test
    public void testGetProvider_moreCerts()
            throws PackageManager.NameNotFoundException {
        setupPackageManager();

        byte[] wrongCert = Base64.decode("this is a wrong cert", Base64.DEFAULT);
        List<byte[]> certList = Arrays.asList(wrongCert, BYTE_ARRAY);
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        ProviderInfo result = mContract.getProvider(requestRightCerts, mResultReceiver);

        // There is one too many certs, should fail as the set doesn't match.
        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_WRONG_CERTIFICATES, null);
        // There is one too many certs, should fail as the set doesn't match.
        assertNull(result);
    }

    @Test
    public void testGetProvider_duplicateCerts()
            throws PackageManager.NameNotFoundException {
        ProviderInfo info = new ProviderInfo();
        info.packageName = PACKAGE;
        info.applicationInfo = new ApplicationInfo();
        when(mPackageManager.resolveContentProvider(anyString(), anyInt())).thenReturn(info);
        PackageInfo packageInfo = new PackageInfo();
        Signature signature = mock(Signature.class);
        when(signature.toByteArray()).thenReturn(BYTE_ARRAY_COPY);
        Signature signature2 = mock(Signature.class);
        when(signature2.toByteArray()).thenReturn(BYTE_ARRAY_COPY);
        packageInfo.packageName = PACKAGE;
        packageInfo.signatures = new Signature[] { signature, signature2 };
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);

        // The provider has {BYTE_ARRAY_COPY, BYTE_ARRAY_COPY}, the request has
        // {BYTE_ARRAY_2, BYTE_ARRAY_COPY}.
        List<byte[]> certList = Arrays.asList(BYTE_ARRAY_2, BYTE_ARRAY_COPY);
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", Arrays.asList(certList));
        ProviderInfo result = mContract.getProvider(requestRightCerts, mResultReceiver);

        // The given list includes an extra cert and doesn't have a second copy of the cert like
        // the provider does, so it should have failed.
        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_WRONG_CERTIFICATES, null);
        assertNull(result);
    }

    @Test
    public void testGetProvider_correctCertsSeveralSets()
            throws PackageManager.NameNotFoundException {
        ProviderInfo info = setupPackageManager();

        List<List<byte[]>> certList = new ArrayList<>();
        byte[] wrongCert = Base64.decode("this is a wrong cert", Base64.DEFAULT);
        certList.add(Arrays.asList(wrongCert));
        certList.add(Arrays.asList(BYTE_ARRAY));
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, PACKAGE, "query", certList);
        ProviderInfo result = mContract.getProvider(requestRightCerts, mResultReceiver);

        verifyZeroInteractions(mResultReceiver);
        assertEquals(info, result);
    }

    @Test
    public void testGetProvider_wrongPackage()
            throws PackageManager.NameNotFoundException {
        setupPackageManager();

        List<List<byte[]>> certList = new ArrayList<>();
        certList.add(Arrays.asList(BYTE_ARRAY));
        FontRequest requestRightCerts = new FontRequest(
                AUTHORITY, "com.wrong.package.name", "query", certList);
        ProviderInfo result = mContract.getProvider(requestRightCerts, mResultReceiver);

        verify(mResultReceiver).send(FontsContractInternal.RESULT_CODE_PROVIDER_NOT_FOUND, null);
        assertNull(result);
    }

    private ProviderInfo setupPackageManager()
            throws PackageManager.NameNotFoundException {
        ProviderInfo info = new ProviderInfo();
        info.packageName = PACKAGE;
        info.applicationInfo = new ApplicationInfo();
        when(mPackageManager.resolveContentProvider(anyString(), anyInt())).thenReturn(info);
        PackageInfo packageInfo = new PackageInfo();
        Signature signature = mock(Signature.class);
        when(signature.toByteArray()).thenReturn(BYTE_ARRAY_COPY);
        packageInfo.packageName = PACKAGE;
        packageInfo.signatures = new Signature[] { signature };
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
        return info;
    }
}
