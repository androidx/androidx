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

import android.content.Context;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;

import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.WebResourceRequestCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebViewFeature.WebViewSupportFeature;

import java.util.List;

/**
 * Enum representing a WebView feature, this provides functionality for determining whether a
 * feature is supported by the current framework and/or WebView APK.
 */
public enum WebViewFeatureInternal {
    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postVisualStateCallback(android.webkit.WebView, long,
     * androidx.webkit.WebViewCompat.VisualStateCallback)}, and
     * {@link WebViewClientCompat#onPageCommitVisible(android.webkit.WebView, String)}.
     */
    VISUAL_STATE_CALLBACK_FEATURE(WebViewFeature.VISUAL_STATE_CALLBACK, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster(WebSettings, boolean)}.
     */
    OFF_SCREEN_PRERASTER(WebViewFeature.OFF_SCREEN_PRERASTER, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled(WebSettings, boolean)}.
     */
    SAFE_BROWSING_ENABLE(WebViewFeature.SAFE_BROWSING_ENABLE, Build.VERSION_CODES.O),

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems(WebSettings, int)}.
     */
    DISABLED_ACTION_MODE_MENU_ITEMS(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
                                    Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#startSafeBrowsing(Context, ValueCallback)}.
     */
    START_SAFE_BROWSING(WebViewFeature.START_SAFE_BROWSING, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(List, ValueCallback)}.
     */
    SAFE_BROWSING_WHITELIST(WebViewFeature.SAFE_BROWSING_WHITELIST, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link WebViewCompat#getSafeBrowsingPrivacyPolicyUrl()}.
     */
    SAFE_BROWSING_PRIVACY_POLICY_URL(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
            Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerControllerCompat#getInstance()}.
     */
    SERVICE_WORKER_BASIC_USAGE(WebViewFeature.SERVICE_WORKER_BASIC_USAGE, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getCacheMode()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setCacheMode(int)}.
     */
    SERVICE_WORKER_CACHE_MODE(WebViewFeature.SERVICE_WORKER_CACHE_MODE, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowContentAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowContentAccess(boolean)}.
     */
    SERVICE_WORKER_CONTENT_ACCESS(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
            Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowFileAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowFileAccess(boolean)}.
     */
    SERVICE_WORKER_FILE_ACCESS(WebViewFeature.SERVICE_WORKER_FILE_ACCESS, Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getBlockNetworkLoads()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setBlockNetworkLoads(boolean)}.
     */
    SERVICE_WORKER_BLOCK_NETWORK_LOADS(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
            Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link ServiceWorkerClientCompat#shouldInterceptRequest(WebResourceRequest)}.
     */
    SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST,
            Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedError(android.webkit.WebView, WebResourceRequest,
     * WebResourceErrorCompat)}.
     */
    RECEIVE_WEB_RESOURCE_ERROR(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedHttpError(android.webkit.WebView, WebResourceRequest,
     * WebResourceResponse)}.
     */
    RECEIVE_HTTP_ERROR(WebViewFeature.RECEIVE_HTTP_ERROR, Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebViewClientCompat#shouldOverrideUrlLoading(android.webkit.WebView,
     * WebResourceRequest)}.
     */
    SHOULD_OVERRIDE_WITH_REDIRECTS(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS,
            Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link WebViewClientCompat#onSafeBrowsingHit(android.webkit.WebView,
     * WebResourceRequest, int, SafeBrowsingResponseCompat)}.
     */
    SAFE_BROWSING_HIT(WebViewFeature.SAFE_BROWSING_HIT, Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    WEB_RESOURCE_REQUEST_IS_REDIRECT(WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT,
            Build.VERSION_CODES.N),

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getDescription()}.
     */
    WEB_RESOURCE_ERROR_GET_DESCRIPTION(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION,
            Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getErrorCode()}.
     */
    WEB_RESOURCE_ERROR_GET_CODE(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE,
            Build.VERSION_CODES.M),

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#backToSafety(boolean)}.
     */
    SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
            Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#proceed(boolean)}.
     */
    SAFE_BROWSING_RESPONSE_PROCEED(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED,
            Build.VERSION_CODES.O_MR1),

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#showInterstitial(boolean)}.
     */
    SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL(
            WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
            Build.VERSION_CODES.O_MR1);

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
        for (WebViewFeatureInternal internalFeature : WebViewFeatureInternal.values()) {
            if (internalFeature.mFeatureValue.equals(feature)) return internalFeature;
        }
        throw new RuntimeException("Unknown feature " + feature);
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


    public static String[] getWebViewApkFeaturesForTesting() {
        return LAZY_HOLDER.WEBVIEW_APK_FEATURES;
    }

    /**
     * Utility method for throwing an exception explaining that the feature the app trying to use
     * isn't supported.
     */
    public static UnsupportedOperationException getUnsupportedOperationException() {
        return new UnsupportedOperationException("This method is not supported by the current "
                + "version of the framework and the current WebView APK");
    }
}
