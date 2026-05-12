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

package androidx.webkit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PrefetchParametersTest {

    @Test
    public void testDefaultValues() {
        WebkitUtils.checkFeature(WebViewFeature.PROFILE_URL_PREFETCH);
        PrefetchParameters.Builder builder = new PrefetchParameters.Builder();
        PrefetchParameters parameters = builder.build();
        assertTrue(parameters.getAdditionalHeaders().isEmpty());
        assertNull(parameters.getExpectedNoVarySearchData());
        assertFalse(parameters.isJavaScriptEnabled());
        assertNull(parameters.getVariationsId());
    }

    @Test
    public void testBuilderValues() {
        WebkitUtils.checkFeature(WebViewFeature.PROFILE_URL_PREFETCH);
        PrefetchParameters.Builder builder = new PrefetchParameters.Builder();

        builder.addAdditionalHeader("key1", "value1");
        builder.setJavaScriptEnabled(true);
        builder.setVariationsId(123);

        NoVarySearchHeader noVarySearchHeader = NoVarySearchHeader.neverVaryHeader();
        builder.setExpectedNoVarySearchData(noVarySearchHeader);

        PrefetchParameters parameters = builder.build();

        assertEquals(1, parameters.getAdditionalHeaders().size());
        assertEquals("value1", parameters.getAdditionalHeaders().get("key1"));
        assertTrue(parameters.isJavaScriptEnabled());
        assertEquals(Integer.valueOf(123), parameters.getVariationsId());
        assertEquals(noVarySearchHeader, parameters.getExpectedNoVarySearchData());
    }

    @Test
    public void testHeaderMerging() {
        WebkitUtils.checkFeature(WebViewFeature.PROFILE_URL_PREFETCH);
        PrefetchParameters.Builder builder = new PrefetchParameters.Builder();
        builder.addAdditionalHeader("key1", "value1");

        Map<String, String> headers = Map.of(
                "key1", "value2",
                "key2", "value3"
        );

        builder.addAdditionalHeaders(headers);

        PrefetchParameters parameters = builder.build();
        assertEquals(headers, parameters.getAdditionalHeaders());
    }
}
