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

package androidx.webkit.internal;

import android.os.Build;

import androidx.webkit.WebViewFeature;
import androidx.webkit.WebViewFeature.WebViewSupportFeature;

/**
 * Enum representing a WebView feature, this provides functionality for determining whether a
 * feature is supported by the current framework and/or WebView APK.
 */
public enum WebViewFeatureInternal {
    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postVisualStateCallback(android.webkit.WebView, long,
     * androidx.webkit.WebViewCompat.VisualStateCallback)}.
     */
    VISUAL_STATE_CALLBACK_FEATURE(WebViewFeature.VISUAL_STATE_CALLBACK, Build.VERSION_CODES.M);

    private final String mFeatureValue;
    private final int mOsVersion;

    WebViewFeatureInternal(@WebViewSupportFeature String featureValue, int osVersion) {
        mFeatureValue = featureValue;
        mOsVersion = osVersion;
    }

    /**
     * Return the {@link WebViewFeatureInternal} corresponding to {@param feature}.
     */
    public static WebViewFeatureInternal getFeature(@WebViewSupportFeature String feature) {
        switch (feature) {
            case WebViewFeature.VISUAL_STATE_CALLBACK:
                return VISUAL_STATE_CALLBACK_FEATURE;
            default:
                throw new RuntimeException("Unknown feature " + feature);
        }
    }

    /**
     * Return whether this {@link WebViewFeature} is supported by the framework of the current
     * device.
     */
    public boolean isSupportedByFramework() {
        return Build.VERSION.SDK_INT >= mOsVersion;
    }

    /**
     * Return whether this {@link WebViewFeature} is supported by the current WebView APK.
     */
    public boolean isSupportedByWebView() {
        String[] webviewFeatures = LAZY_HOLDER.WEBVIEW_APK_FEATURES;
        for (String webviewFeature : webviewFeatures) {
            if (webviewFeature.equals(mFeatureValue)) return true;
        }
        return false;
    }

    private static class LAZY_HOLDER {
        static final String[] WEBVIEW_APK_FEATURES =
                WebViewGlueCommunicator.getFactory().getWebViewFeatures();
    }

    /**
     * Utility method for throwing an exception explaining that the feature the app trying to use
     * isn't supported.
     */
    public static void throwUnsupportedOperationException(String feature) {
        throw new UnsupportedOperationException("Feature " + feature
                + " is not supported by the current version of the framework and WebView APK");
    }
}
