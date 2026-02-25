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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns true for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public long getTimeoutSeconds() {
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns true for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     *
     * @param timeoutSeconds The timeout in seconds.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setTimeoutSeconds(long timeoutSeconds) {
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns true for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public int getMaxPagesInCache() {
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)} returns true for
     * {@link WebViewFeature#BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3}.
     *
     * @param maxPagesInCache The maximum number of pages.
     */
    @RequiresFeature(name = WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public void setMaxPagesInCache(int maxPagesInCache) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3;
        if (feature.isSupportedByWebView()) {
            mAdapter.setBackForwardCacheMaxPagesInCache(maxPagesInCache);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

}
