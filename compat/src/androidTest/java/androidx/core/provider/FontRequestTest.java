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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
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
}
