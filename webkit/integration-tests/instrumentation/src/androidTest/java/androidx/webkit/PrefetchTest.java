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

import org.junit.Assert;
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

        builder.setMaxPrefetches(Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, builder.build().getMaxPrefetches());
    }

    /**
     * Test setting valid values for
     * {@link SpeculativeLoadingConfig.Builder#setMaxPrerenders(int)} (int)}
     */
    @Test
    public void testMaxPrerendersValidValues() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder = new SpeculativeLoadingConfig.Builder();
        builder.setMaxPrerenders(1);
        assertEquals(1, builder.build().getMaxPrerenders());

        builder.setMaxPrerenders(Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE - 1, builder.build().getMaxPrerenders());
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
    }

    /**
     * Test setting out-of-range values for
     * {@link SpeculativeLoadingConfig.Builder#setMaxPrerenders(int)}
     */
    @Test
    public void testMaxPrerendersLimit() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder = new SpeculativeLoadingConfig.Builder();

        // lower bound
        IllegalArgumentException expectedException = assertThrows(IllegalArgumentException.class,
                () -> builder.setMaxPrerenders(0));
        assertEquals("Max prerenders must be greater than 0", expectedException.getMessage());
    }

    /**
     * Test to make sure that calling the API won't cause any obvious errors.
     */
    @Test
    @SuppressWarnings("removal")
    public void testSettingCacheConfig() {
        WebkitUtils.checkFeature(WebViewFeature.SPECULATIVE_LOADING_CONFIG);
        SpeculativeLoadingConfig.Builder builder =
                new SpeculativeLoadingConfig.Builder().setMaxPrefetches(1).setMaxPrerenders(
                        1).setPrefetchTtlSeconds(60);
        WebkitUtils.onMainThreadSync(() -> {
            Profile testProfile = ProfileStore.getInstance().getProfile(
                    Profile.DEFAULT_PROFILE_NAME);
            try {
                testProfile.setSpeculativeLoadingConfig(builder.build());
            } catch (Exception exception) {
                Assert.fail(exception.getMessage());
            }
        });
    }

    /**
     * Test to make sure that calling the setMaxPrefetches API won't cause any obvious errors.
     */
    @Test
    public void testSetMaxPrefetches() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PREFETCH_CACHE_V1);
        WebkitUtils.onMainThreadSync(() -> {
            PrefetchCache prefetchCache = ProfileStore.getInstance().getProfile(
                    Profile.DEFAULT_PROFILE_NAME).getPrefetchCache();
            prefetchCache.setMaxPrefetches(5);
            prefetchCache.setMaxPrefetches(null);
            prefetchCache.setMaxPrefetches(1);
        });
    }

    /**
     * Test to make sure that calling the setPrefetchTtlSeconds API won't cause any obvious errors.
     */
    @Test
    public void testSetPrefetchTtlSeconds() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PREFETCH_CACHE_V1);
        WebkitUtils.onMainThreadSync(() -> {
            PrefetchCache prefetchCache = ProfileStore.getInstance().getProfile(
                    Profile.DEFAULT_PROFILE_NAME).getPrefetchCache();
            prefetchCache.setPrefetchTtlSeconds(60);
            prefetchCache.setPrefetchTtlSeconds(1);
            prefetchCache.setPrefetchTtlSeconds(null);
        });
    }

    /**
     * Test to make sure that calling the setMaxPrerenders API won't cause any obvious errors.
     */
    @Test
    public void testSetMaxPrerenders() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SET_MAX_PRERENDERS_V1);
        WebkitUtils.onMainThreadSync(() -> {
            Profile testProfile = ProfileStore.getInstance().getProfile(
                    Profile.DEFAULT_PROFILE_NAME);
            testProfile.setMaxPrerenders(5);
            testProfile.setMaxPrerenders(1);
            testProfile.setMaxPrerenders(null);
        });
    }

    /**
     * Test to make sure that IntRange returns the right exception
     */
    @Test
    public void testIntRangeThrowsException() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.SET_MAX_PRERENDERS_V1);
        WebkitUtils.onMainThreadSync(() -> {
            Profile testProfile = ProfileStore.getInstance().getProfile(
                    Profile.DEFAULT_PROFILE_NAME);
            assertThrows(IllegalArgumentException.class, () -> testProfile.setMaxPrerenders(0));
            assertThrows(IllegalArgumentException.class, () -> testProfile.setMaxPrerenders(-1));
            assertThrows(IllegalArgumentException.class,
                    () -> testProfile.getPrefetchCache().setMaxPrefetches(0));
            assertThrows(IllegalArgumentException.class,
                    () -> testProfile.getPrefetchCache().setMaxPrefetches(-1));
            assertThrows(IllegalArgumentException.class,
                    () -> testProfile.getPrefetchCache().setPrefetchTtlSeconds(0));
            assertThrows(IllegalArgumentException.class,
                    () -> testProfile.getPrefetchCache().setPrefetchTtlSeconds(-1));
        });
    }
}
