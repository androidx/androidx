/*
 * Copyright 2018 The Android Open Source Project
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

import android.webkit.SafeBrowsingResponse;

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

/**
 * Compatibility version of {@link SafeBrowsingResponse}.
 */
public abstract class SafeBrowsingResponseCompat {
    /**
     * Display the default interstitial.
     *
     * @param allowReporting {@code true} if the interstitial should show a reporting checkbox.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL} feature is not supported.
     *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
     */
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void showInterstitial(boolean allowReporting);

    /**
     * Act as if the user clicked the "visit this unsafe site" button.
     *
     * @param report {@code true} to enable Safe Browsing reporting.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#SAFE_BROWSING_RESPONSE_PROCEED} feature is not supported.
     *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
     */
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void proceed(boolean report);

    /**
     * Act as if the user clicked the "back to safety" button.
     *
     * @param report {@code true} to enable Safe Browsing reporting.
     * @throws UnsupportedOperationException if the
     *     {@link WebViewFeature#SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY} feature is not supported.
     *     This should be checked before use with {@link WebViewFeature#isFeatureSupported}.
     */
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void backToSafety(boolean report);

    /**
     * This class cannot be created by applications.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SafeBrowsingResponseCompat() {
    }
}
