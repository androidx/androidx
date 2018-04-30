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

import android.content.Context;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.util.Features;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Utility class for checking which WebView Support Library features are supported on the device.
 */
public class WebViewFeature {

    private WebViewFeature() {}

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @StringDef(value = {
            VISUAL_STATE_CALLBACK,
            OFF_SCREEN_PRERASTER,
            SAFE_BROWSING_ENABLE,
            DISABLED_ACTION_MODE_MENU_ITEMS,
            START_SAFE_BROWSING,
            SAFE_BROWSING_WHITELIST,
            SAFE_BROWSING_PRIVACY_POLICY_URL,
            SERVICE_WORKER_BASIC_USAGE,
            SERVICE_WORKER_CACHE_MODE,
            SERVICE_WORKER_CONTENT_ACCESS,
            SERVICE_WORKER_FILE_ACCESS,
            SERVICE_WORKER_BLOCK_NETWORK_LOADS,
            SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST,
            RECEIVE_WEB_RESOURCE_ERROR,
            RECEIVE_HTTP_ERROR,
            SHOULD_OVERRIDE_WITH_REDIRECTS,
            SAFE_BROWSING_HIT,
            WEB_RESOURCE_REQUEST_IS_REDIRECT,
            WEB_RESOURCE_ERROR_GET_DESCRIPTION,
            WEB_RESOURCE_ERROR_GET_CODE,
            SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
            SAFE_BROWSING_RESPONSE_PROCEED,
            SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface WebViewSupportFeature {}

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postVisualStateCallback(android.webkit.WebView, long,
     * WebViewCompat.VisualStateCallback)}, and {@link
     * WebViewClientCompat#onPageCommitVisible(
     * android.webkit.WebView, String)}.
     */
    public static final String VISUAL_STATE_CALLBACK = Features.VISUAL_STATE_CALLBACK;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster(WebSettings, boolean)}.
     */
    public static final String OFF_SCREEN_PRERASTER = Features.OFF_SCREEN_PRERASTER;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled(WebSettings, boolean)}.
     */
    public static final String SAFE_BROWSING_ENABLE = Features.SAFE_BROWSING_ENABLE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems(WebSettings, int)}.
     */
    public static final String DISABLED_ACTION_MODE_MENU_ITEMS =
            Features.DISABLED_ACTION_MODE_MENU_ITEMS;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#startSafeBrowsing(Context, ValueCallback)}.
     */
    public static final String START_SAFE_BROWSING = Features.START_SAFE_BROWSING;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(List, ValueCallback)}.
     */
    public static final String SAFE_BROWSING_WHITELIST = Features.SAFE_BROWSING_WHITELIST;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewCompat#getSafeBrowsingPrivacyPolicyUrl()}.
     */
    public static final String SAFE_BROWSING_PRIVACY_POLICY_URL =
            Features.SAFE_BROWSING_PRIVACY_POLICY_URL;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerControllerCompat#getInstance()}.
     */
    public static final String SERVICE_WORKER_BASIC_USAGE = Features.SERVICE_WORKER_BASIC_USAGE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getCacheMode()}, and
     * {@link ServiceWorkerWebSettingsCompat#setCacheMode(int)}.
     */
    public static final String SERVICE_WORKER_CACHE_MODE = Features.SERVICE_WORKER_CACHE_MODE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getAllowContentAccess()}, and
     * {@link ServiceWorkerWebSettingsCompat#setAllowContentAccess(boolean)}.
     */
    public static final String SERVICE_WORKER_CONTENT_ACCESS =
            Features.SERVICE_WORKER_CONTENT_ACCESS;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getAllowFileAccess()}, and
     * {@link ServiceWorkerWebSettingsCompat#setAllowFileAccess(boolean)}.
     */
    public static final String SERVICE_WORKER_FILE_ACCESS = Features.SERVICE_WORKER_FILE_ACCESS;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getBlockNetworkLoads()}, and
     * {@link ServiceWorkerWebSettingsCompat#setBlockNetworkLoads(boolean)}.
     */
    public static final String SERVICE_WORKER_BLOCK_NETWORK_LOADS =
            Features.SERVICE_WORKER_BLOCK_NETWORK_LOADS;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerClientCompat#shouldInterceptRequest(WebResourceRequest)}.
     */
    public static final String SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST =
            Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#onReceivedError(android.webkit.WebView, WebResourceRequest,
     * WebResourceErrorCompat)}.
     */
    public static final String RECEIVE_WEB_RESOURCE_ERROR = Features.RECEIVE_WEB_RESOURCE_ERROR;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#onReceivedHttpError(android.webkit.WebView, WebResourceRequest,
     * WebResourceResponse)}.
     */
    public static final String RECEIVE_HTTP_ERROR = Features.RECEIVE_HTTP_ERROR;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#shouldOverrideUrlLoading(android.webkit.WebView,
     * WebResourceRequest)}.
     */
    public static final String SHOULD_OVERRIDE_WITH_REDIRECTS =
            Features.SHOULD_OVERRIDE_WITH_REDIRECTS;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#onSafeBrowsingHit(android.webkit.WebView,
     * WebResourceRequest, int, SafeBrowsingResponseCompat)}.
     */
    public static final String SAFE_BROWSING_HIT = Features.SAFE_BROWSING_HIT;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    public static final String WEB_RESOURCE_REQUEST_IS_REDIRECT =
            Features.WEB_RESOURCE_REQUEST_IS_REDIRECT;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebResourceErrorCompat#getDescription()}.
     */
    public static final String WEB_RESOURCE_ERROR_GET_DESCRIPTION =
            Features.WEB_RESOURCE_ERROR_GET_DESCRIPTION;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebResourceErrorCompat#getErrorCode()}.
     */
    public static final String WEB_RESOURCE_ERROR_GET_CODE =
            Features.WEB_RESOURCE_ERROR_GET_CODE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link SafeBrowsingResponseCompat#backToSafety(boolean)}.
     */
    public static final String SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY =
            Features.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link SafeBrowsingResponseCompat#proceed(boolean)}.
     */
    public static final String SAFE_BROWSING_RESPONSE_PROCEED =
            Features.SAFE_BROWSING_RESPONSE_PROCEED;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link SafeBrowsingResponseCompat#showInterstitial(boolean)}.
     */
    public static final String SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL =
            Features.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL;

    /**
     * Return whether a feature is supported at run-time. This depends on the Android version of the
     * device and the WebView APK on the device.
     */
    public static boolean isFeatureSupported(@NonNull @WebViewSupportFeature String feature) {
        WebViewFeatureInternal webviewFeature = WebViewFeatureInternal.getFeature(feature);
        return webviewFeature.isSupportedByFramework() || webviewFeature.isSupportedByWebView();
    }
}
