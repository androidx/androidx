/*
 * Copyright 2026 The Android Open Source Project
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

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

@RunWith(JUnit4.class)
public class FontProviderTest {

    private static final String PROVIDER = "com.test.fontprovider.authority";
    private static final String PACKAGE = "com.test.fontprovider.package";
    private static final String QUERY = "query";

    @Test
    public void getSelectionArgs_noVariationSettings_returnsQueryOnly() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, Collections.emptyList());
        String[] result = FontProvider.getSelectionArgs(request);
        assertArrayEquals(new String[]{QUERY}, result);
    }

    @Test
    public void getSelectionArgs_emptyVariationSettings_returnsQueryOnly() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, Collections.emptyList(),
                null, "");
        String[] result = FontProvider.getSelectionArgs(request);
        assertArrayEquals(new String[]{QUERY}, result);
    }

    @Test
    public void getSelectionArgs_blankVariationSettings_returnsQueryOnly() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, Collections.emptyList(),
                null, "   ");
        String[] result = FontProvider.getSelectionArgs(request);
        assertArrayEquals(new String[]{QUERY}, result);
    }

    @Test
    public void getSelectionArgs_withVariationSettings_returnsQueryAndVFParam() {
        FontRequest request = new FontRequest(PROVIDER, PACKAGE, QUERY, Collections.emptyList(),
                null, "'wght' 100");
        String[] result = FontProvider.getSelectionArgs(request);
        assertArrayEquals(new String[]{QUERY, "VF"}, result);
    }
}
