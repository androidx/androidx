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

import android.webkit.WebView;

import androidx.annotation.IntRange;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebSettingsAdapter;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.jspecify.annotations.NonNull;

/**
 * A class for developers to configure the back-forward cache on a {@link WebView}.
 *
 * <p>The back-forward cache is a browser feature that improves the user experience by keeping pages
 * alive for a limited time after the user navigates away from them. If the user navigates back
 * or forward, the page is reused for a fast back navigation.
 *
 * <p>Example:
 *
 * <pre class="prettyprint">
 * BackForwardCacheSettings settings =
 *         WebSettingsCompat.getBackForwardCacheSettings(webView.getSettings());
 * settings.setTimeoutSeconds(600);
 * settings.setMaxPagesInCache(10);
 * </pre>
 */
@WebSettingsCompat.ExperimentalBackForwardCacheSettings
public class BackForwardCacheSettings {

    private final WebSettingsAdapter mAdapter;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    BackForwardCacheSettings(@NonNull WebSettingsAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Returns the timeout for pages in the back-forward cache, in seconds.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     *
     * @throws UnsupportedOperationException if the
     *                            {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public @IntRange(from = 0) long getTimeoutSeconds() {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3;
        if (feature.isSupportedByWebView()) {
            return mAdapter.getBackForwardCacheTimeoutSeconds();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets the timeout for pages in the back-forward cache.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     *
     * @param timeoutSeconds The timeout in seconds.
     * @throws UnsupportedOperationException if the
     *                            {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setTimeoutSeconds(@IntRange(from = 0) long timeoutSeconds) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3;
        if (feature.isSupportedByWebView()) {
            mAdapter.setBackForwardCacheTimeoutSeconds(timeoutSeconds);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns the maximum number of pages that can be stored in the back-forward cache.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     *
     * @throws UnsupportedOperationException if the
     *                            {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public @IntRange(from = 0) int getMaxPagesInCache() {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3;
        if (feature.isSupportedByWebView()) {
            return mAdapter.getBackForwardCacheMaxPagesInCache();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets the maximum number of pages that can be stored in the back-forward cache.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     *
     * @param maxPagesInCache The maximum number of pages.
     * @throws UnsupportedOperationException if the
     *                            {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setMaxPagesInCache(@IntRange(from = 0) int maxPagesInCache) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3;
        if (feature.isSupportedByWebView()) {
            mAdapter.setBackForwardCacheMaxPagesInCache(maxPagesInCache);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns whether to keep forward cache entries when the back-forward cache is enabled.
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4}.
     *
     * @throws UnsupportedOperationException if the
     *                            {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @WebSettingsCompat.ExperimentalBackForwardCacheSettings
    public boolean isKeepForwardEntriesEnabled() {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4;
        if (feature.isSupportedByWebView()) {
            return mAdapter.getBackForwardCacheKeepForwardEntries();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets whether to keep forward cache entries when the back-forward cache is enabled.
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns {@code true} for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4}.
     *
     * @param keepForwardEntries Whether to keep forward cache entries.
     * @throws UnsupportedOperationException if the
     *                            {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4}
     *                                       feature is not supported.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @WebSettingsCompat.ExperimentalBackForwardCacheSettings
    public void setKeepForwardEntriesEnabled(boolean keepForwardEntries) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4;
        if (feature.isSupportedByWebView()) {
            mAdapter.setBackForwardCacheKeepForwardEntries(keepForwardEntries);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

}
