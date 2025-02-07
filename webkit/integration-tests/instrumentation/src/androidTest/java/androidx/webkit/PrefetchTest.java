/*
 * Copyright 2025 The Android Open Source Project
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
import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PrefetchTest {

    /**
     * Test setting valid values for
     * {@link SpeculativeLoadingConfig.Builder#setPrefetchTtlSeconds(int)}
     */
    @Test
    public void testTTLValidValues() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder = new SpeculativeLoadingConfig.Builder();
        // lower values
        builder.setPrefetchTtlSeconds(1);
        assertEquals(1, builder.build().getPrefetchTtlSeconds());

        builder.setPrefetchTtlSeconds(Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, builder.build().getPrefetchTtlSeconds());

        builder.setPrefetchTtlSeconds(5685);
        assertEquals(5685, builder.build().getPrefetchTtlSeconds());
    }

    /**
     * Test setting valid values for {@link SpeculativeLoadingConfig.Builder#setMaxPrefetches(int)}
     */
    @Test
    public void testMaxPrefetchesValidValues() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder = new SpeculativeLoadingConfig.Builder();
        builder.setMaxPrefetches(1);
        assertEquals(1, builder.build().getMaxPrefetches());

        builder.setMaxPrefetches(SpeculativeLoadingConfig.ABSOLUTE_MAX_PREFETCHES);
        assertEquals(SpeculativeLoadingConfig.ABSOLUTE_MAX_PREFETCHES,
                builder.build().getMaxPrefetches());
    }

    /**
     * Test setting out-of-range values for
     * {@link SpeculativeLoadingConfig.Builder#setPrefetchTtlSeconds(int)}
     */
    @Test
    public void testTTLLimit() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder = new SpeculativeLoadingConfig.Builder();

        IllegalArgumentException expectedException = assertThrows(IllegalArgumentException.class,
                () -> builder.setPrefetchTtlSeconds(0));
        assertEquals("Prefetch TTL must be greater than 0", expectedException.getMessage());
    }

    /**
     * Test setting out-of-range values for
     * {@link SpeculativeLoadingConfig.Builder#setMaxPrefetches(int)}
     */
    @Test
    public void testMaxPrefetchesLimit() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder = new SpeculativeLoadingConfig.Builder();

        // lower bound
        IllegalArgumentException expectedException = assertThrows(IllegalArgumentException.class,
                () -> builder.setMaxPrefetches(0));
        assertEquals("Max prefetches must be greater than 0", expectedException.getMessage());

        // upper bound
        expectedException = assertThrows(IllegalArgumentException.class,
                () -> builder.setMaxPrefetches(
                        SpeculativeLoadingConfig.ABSOLUTE_MAX_PREFETCHES + 1));
        assertEquals("Max prefetches cannot exceed"
                + SpeculativeLoadingConfig.ABSOLUTE_MAX_PREFETCHES, expectedException.getMessage());
    }

}
