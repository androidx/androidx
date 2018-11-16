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
import android.net.Uri;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.util.Features;

import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.Executor;

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
            TRACING_CONTROLLER_BASIC_USAGE,
            WEB_RESOURCE_REQUEST_IS_REDIRECT,
            WEB_RESOURCE_ERROR_GET_DESCRIPTION,
            WEB_RESOURCE_ERROR_GET_CODE,
            SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
            SAFE_BROWSING_RESPONSE_PROCEED,
            SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
            WEB_MESSAGE_PORT_POST_MESSAGE,
            WEB_MESSAGE_PORT_CLOSE,
            WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK,
            CREATE_WEB_MESSAGE_CHANNEL,
            POST_WEB_MESSAGE,
            WEB_MESSAGE_CALLBACK_ON_MESSAGE,
            GET_WEB_VIEW_CLIENT,
            GET_WEB_CHROME_CLIENT,
            GET_WEB_VIEW_RENDERER,
            WEB_VIEW_RENDERER_TERMINATE,
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
     * {@link TracingController#getInstance()},
     * {@link TracingController#isTracing()},
     * {@link TracingController#start(TracingConfig)},
     * {@link TracingController#stop(OutputStream, Executor)}.
     */
    public static final String TRACING_CONTROLLER_BASIC_USAGE =
            Features.TRACING_CONTROLLER_BASIC_USAGE;

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
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#postMessage(WebMessageCompat)}.
     */
    public static final String WEB_MESSAGE_PORT_POST_MESSAGE =
            Features.WEB_MESSAGE_PORT_POST_MESSAGE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#close()}.
     */
    public static final String WEB_MESSAGE_PORT_CLOSE = Features.WEB_MESSAGE_PORT_CLOSE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#setWebMessageCallback(
     * WebMessagePortCompat.WebMessageCallbackCompat)}, and
     * {@link androidx.webkit.WebMessagePortCompat#setWebMessageCallback(Handler,
     * WebMessagePortCompat.WebMessageCallbackCompat)}.
     */
    public static final String WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK =
            Features.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewCompat#createWebMessageChannel(WebView)}.
     */
    public static final String CREATE_WEB_MESSAGE_CHANNEL = Features.CREATE_WEB_MESSAGE_CHANNEL;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    public static final String POST_WEB_MESSAGE = Features.POST_WEB_MESSAGE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebMessagePortCompat.WebMessageCallbackCompat#onMessage(WebMessagePortCompat,
     * WebMessageCompat)}.
     */
    public static final String WEB_MESSAGE_CALLBACK_ON_MESSAGE =
            Features.WEB_MESSAGE_CALLBACK_ON_MESSAGE;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#getWebViewClient(WebView)}
     */
    public static final String GET_WEB_VIEW_CLIENT = Features.GET_WEB_VIEW_CLIENT;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#getWebChromeClient(WebView)}
     */
    public static final String GET_WEB_CHROME_CLIENT = Features.GET_WEB_CHROME_CLIENT;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#getWebViewRenderer(WebView)}
     */
    public static final String GET_WEB_VIEW_RENDERER = Features.GET_WEB_VIEW_RENDERER;

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewRenderer#terminate()}
     */
    public static final String WEB_VIEW_RENDERER_TERMINATE = Features.WEB_VIEW_RENDERER_TERMINATE;

    /**
     * Return whether a feature is supported at run-time. On devices running Android version {@link
     * android.os.Build.VERSION_CODES#LOLLIPOP} and higher, this will check whether a feature is
     * supported, depending on the combination of the desired feature, the Android version of
     * device, and the WebView APK on the device. If running on a device with a lower API level,
     * this will always return {@code false}.
     *
     * <p class="note"><b>Note:</b> If this method returns {@code false}, it is not safe to invoke
     * the methods requiring the desired feature. Furthermore, if this method returns {@code false}
     * for a particular feature, any callback guarded by that feature will not be invoked.
     *
     * @param feature the feature to be checked
     * @return whether the feature is supported given the current platform SDK and webview version
     */
    public static boolean isFeatureSupported(@NonNull @WebViewSupportFeature String feature) {
        WebViewFeatureInternal webviewFeature = WebViewFeatureInternal.getFeature(feature);
        return webviewFeature.isSupportedByFramework() || webviewFeature.isSupportedByWebView();
    }
}
