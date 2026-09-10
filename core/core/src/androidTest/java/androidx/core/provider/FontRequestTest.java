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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link FontRequest}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class FontRequestTest {
    private static final String PROVIDER = "com.test.fontprovider.authority";
    private static final String QUERY = "query";
    private static final String PACKAGE = "com.test.fontprovider.package";
    private static final byte[] BYTE_ARRAY =
            Base64.decode("e04fd020ea3a6910a2d808002b30", Base64.DEFAULT);
    private static final List<List<byte[]>> CERTS = Arrays.asList(Arrays.asList(BYTE_ARRAY));

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullAuthority() {
        new FontRequest(null, PACKAGE, QUERY, CERTS);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullPackage() {
        new FontRequest(PROVIDER, null, QUERY, CERTS);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullQuery() {
        new FontRequest(PROVIDER, PACKAGE, null, CERTS);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_nullCerts() {
        new FontRequest(PROVIDER, PACKAGE, QUERY, null);
    }

    @Test
    public void testCreateIdentifier_default() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS);
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-null", request.getId());
    }

    @Test
    public void testCreateIdentifier_withVariationSettings() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS, "wght 400");
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-null-VF", request.getId());
    }

    @Test
    public void testCreateIdentifier_withSystemFont() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS, "systemFont",
                "wght 400");
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-systemFont-VF", request.getId());
    }

    @Test
    public void testCreateIdentifier_blankVariationSettings() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS, "systemFont", "   ");
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-systemFont", request.getId());
    }

    @Test
    public void testCreateIdentifier_withCertificateInRes_default() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.mock_provider_certs);
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-null", request.getId());
    }

    @Test
    public void testCreateIdentifier_withCertificateInRes_withVariationSettings() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.mock_provider_certs, "wght 400");
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-null-VF", request.getId());
    }

    @Test
    public void testCreateIdentifier_withCertificateInRes_blankVariationSettings() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.mock_provider_certs, "   ");
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-null", request.getId());
    }

    @Test
    public void testCreateIdentifier_fromXmlParserFallback_withSystemFont() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS,
                Collections.emptyList(), "serif", "wght 400");
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-serif-VF", request.getId());
        assertEquals("serif", request.getSystemFont());
        assertEquals("wght 400", request.getVariationSettings());
    }

    @Test
    public void testBuilder_systemCertificates() {
        FontRequest request = new FontRequest.Builder(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.certs1).build();
        assertEquals(PROVIDER, request.getProviderAuthority());
        assertEquals(PACKAGE, request.getProviderPackage());
        assertEquals(QUERY, request.getQuery());
        assertEquals(androidx.core.test.R.array.certs1, request.getSystemCertificatesArrayResId());
        assertEquals(0, request.getDebugFallbackCertificatesArrayResId());
        assertEquals(0, request.getCertificatesArrayResId());
        assertNull(request.getSystemCertificates());
        assertNull(request.getCertificates());
        assertNull(request.getVariationSettings());
    }

    @Test
    public void testBuilder_withDebugFallbackCertificates() {
        FontRequest request = new FontRequest.Builder(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.certs1)
                .setDebugFallbackCertificates(androidx.core.test.R.array.mock_provider_certs)
                .build();
        assertEquals(androidx.core.test.R.array.certs1, request.getSystemCertificatesArrayResId());
        assertEquals(androidx.core.test.R.array.mock_provider_certs,
                request.getDebugFallbackCertificatesArrayResId());
        assertEquals(androidx.core.test.R.array.mock_provider_certs,
                request.getCertificatesArrayResId());
    }

    @Test
    public void testBuilder_withVariationSettings() {
        FontRequest request = new FontRequest.Builder(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.certs1)
                .setVariationSettings("wght 400")
                .build();
        assertEquals("wght 400", request.getVariationSettings());
        assertEquals(PROVIDER + "-" + PACKAGE + "-" + QUERY + "-null-VF", request.getId());
    }

    @Test
    public void testBuilder_copyConstructor() {
        FontRequest original = new FontRequest.Builder(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.certs1)
                .setDebugFallbackCertificates(androidx.core.test.R.array.mock_provider_certs)
                .setVariationSettings("wght 400")
                .build();

        FontRequest copy = new FontRequest.Builder(original).build();
        assertEquals(original.getProviderAuthority(), copy.getProviderAuthority());
        assertEquals(original.getProviderPackage(), copy.getProviderPackage());
        assertEquals(original.getQuery(), copy.getQuery());
        assertEquals(original.getSystemCertificatesArrayResId(),
                copy.getSystemCertificatesArrayResId());
        assertEquals(original.getDebugFallbackCertificatesArrayResId(),
                copy.getDebugFallbackCertificatesArrayResId());
        assertEquals(original.getVariationSettings(), copy.getVariationSettings());
        assertEquals(original.getId(), copy.getId());
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_nullAuthority() {
        new FontRequest.Builder(null, PACKAGE, QUERY, androidx.core.test.R.array.certs1);
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_nullPackage() {
        new FontRequest.Builder(PROVIDER, null, QUERY, androidx.core.test.R.array.certs1);
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_nullQuery() {
        new FontRequest.Builder(PROVIDER, PACKAGE, null, androidx.core.test.R.array.certs1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_zeroSystemCertificates() {
        new FontRequest.Builder(PROVIDER, PACKAGE, QUERY, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_zeroDebugFallbackCertificates() {
        new FontRequest.Builder(PROVIDER, PACKAGE, QUERY, androidx.core.test.R.array.certs1)
                .setDebugFallbackCertificates(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_copyLegacyRequestWithoutSystemCerts_throws() {
        FontRequest legacyRequest = new FontRequest(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.mock_provider_certs);
        new FontRequest.Builder(legacyRequest);
    }

    @Test
    public void testToString() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, CERTS);
        String str = request.toString();
        assertTrue(str.startsWith("FontRequest {"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("mProviderAuthority: " + PROVIDER));
        assertTrue(str.contains("mProviderPackage: " + PACKAGE));
        assertTrue(str.contains("mQuery: " + QUERY));
        assertTrue(str.contains("mCertificates:"));
        assertTrue(str.contains("mCertificatesArray: 0"));
        assertTrue(str.contains("mSystemCertificates:"));
        assertTrue(str.contains("mSystemCertificatesArray: 0"));

        // Verify there is only one opening brace and one closing brace in the entire string
        int openBraces = 0;
        int closeBraces = 0;
        for (char c : str.toCharArray()) {
            if (c == '{') openBraces++;
            if (c == '}') closeBraces++;
        }
        assertEquals(1, openBraces);
        assertEquals(1, closeBraces);
    }

    @Test
    public void testToString_builder() {
        FontRequest request = new FontRequest.Builder(PROVIDER, PACKAGE, QUERY,
                androidx.core.test.R.array.certs1)
                .setDebugFallbackCertificates(androidx.core.test.R.array.mock_provider_certs)
                .setVariationSettings("wght 400")
                .build();
        String str = request.toString();
        assertTrue(str.startsWith("FontRequest {"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("mProviderAuthority: " + PROVIDER));
        assertTrue(str.contains("mProviderPackage: " + PACKAGE));
        assertTrue(str.contains("mQuery: " + QUERY));
        assertTrue(str.contains("mVariationSettings: wght 400"));
        assertTrue(str.contains("mCertificatesArray: "
                + androidx.core.test.R.array.mock_provider_certs));
        assertTrue(str.contains("mSystemCertificatesArray: " + androidx.core.test.R.array.certs1));
    }
}
