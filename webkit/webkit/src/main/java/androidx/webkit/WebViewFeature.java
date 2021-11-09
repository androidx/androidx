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

import android.annotation.SuppressLint;
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
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(value = {
            VISUAL_STATE_CALLBACK,
            OFF_SCREEN_PRERASTER,
            SAFE_BROWSING_ENABLE,
            DISABLED_ACTION_MODE_MENU_ITEMS,
            START_SAFE_BROWSING,
            SAFE_BROWSING_ALLOWLIST,
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
            WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
            PROXY_OVERRIDE,
            SUPPRESS_ERROR_PAGE,
            MULTI_PROCESS,
            FORCE_DARK,
            FORCE_DARK_STRATEGY,
            WEB_MESSAGE_LISTENER,
            DOCUMENT_START_SCRIPT,
            PROXY_OVERRIDE_REVERSE_BYPASS,
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
    public static final String VISUAL_STATE_CALLBACK = "VISUAL_STATE_CALLBACK";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster(WebSettings, boolean)}.
     */
    public static final String OFF_SCREEN_PRERASTER = "OFF_SCREEN_PRERASTER";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled(WebSettings, boolean)}.
     */
    public static final String SAFE_BROWSING_ENABLE = "SAFE_BROWSING_ENABLE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems(WebSettings, int)}.
     */
    @SuppressLint("IntentName") // False positive: this constant is not to be used for Intents.
    public static final String DISABLED_ACTION_MODE_MENU_ITEMS =
            "DISABLED_ACTION_MODE_MENU_ITEMS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#startSafeBrowsing(Context, ValueCallback)}.
     */
    public static final String START_SAFE_BROWSING = "START_SAFE_BROWSING";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set, ValueCallback)}.
     */
    public static final String SAFE_BROWSING_ALLOWLIST = "SAFE_BROWSING_ALLOWLIST";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(List, ValueCallback)}.
     *
     * <p>This is functionally equivalent to {@link #SAFE_BROWSING_ALLOWLIST}: both constants
     * represent the same range of compatibility across Android OS versions and WebView versions.
     *
     * @deprecated Please use {@link #SAFE_BROWSING_ALLOWLIST} and {@link
     * androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set, ValueCallback)} instead.
     */
    @Deprecated
    public static final String SAFE_BROWSING_WHITELIST = "SAFE_BROWSING_WHITELIST";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#getSafeBrowsingPrivacyPolicyUrl()}.
     */
    public static final String SAFE_BROWSING_PRIVACY_POLICY_URL =
            "SAFE_BROWSING_PRIVACY_POLICY_URL";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerControllerCompat#getInstance()}.
     */
    public static final String SERVICE_WORKER_BASIC_USAGE = "SERVICE_WORKER_BASIC_USAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getCacheMode()}, and
     * {@link ServiceWorkerWebSettingsCompat#setCacheMode(int)}.
     */
    public static final String SERVICE_WORKER_CACHE_MODE = "SERVICE_WORKER_CACHE_MODE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getAllowContentAccess()}, and
     * {@link ServiceWorkerWebSettingsCompat#setAllowContentAccess(boolean)}.
     */
    public static final String SERVICE_WORKER_CONTENT_ACCESS =
            "SERVICE_WORKER_CONTENT_ACCESS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getAllowFileAccess()}, and
     * {@link ServiceWorkerWebSettingsCompat#setAllowFileAccess(boolean)}.
     */
    public static final String SERVICE_WORKER_FILE_ACCESS = "SERVICE_WORKER_FILE_ACCESS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerWebSettingsCompat#getBlockNetworkLoads()}, and
     * {@link ServiceWorkerWebSettingsCompat#setBlockNetworkLoads(boolean)}.
     */
    public static final String SERVICE_WORKER_BLOCK_NETWORK_LOADS =
            "SERVICE_WORKER_BLOCK_NETWORK_LOADS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ServiceWorkerClientCompat#shouldInterceptRequest(WebResourceRequest)}.
     */
    public static final String SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST =
            "SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#onReceivedError(android.webkit.WebView, WebResourceRequest,
     * WebResourceErrorCompat)}.
     */
    public static final String RECEIVE_WEB_RESOURCE_ERROR = "RECEIVE_WEB_RESOURCE_ERROR";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#onReceivedHttpError(android.webkit.WebView, WebResourceRequest,
     * WebResourceResponse)}.
     */
    public static final String RECEIVE_HTTP_ERROR = "RECEIVE_HTTP_ERROR";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#shouldOverrideUrlLoading(android.webkit.WebView,
     * WebResourceRequest)}.
     */
    public static final String SHOULD_OVERRIDE_WITH_REDIRECTS =
            "SHOULD_OVERRIDE_WITH_REDIRECTS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebViewClientCompat#onSafeBrowsingHit(android.webkit.WebView,
     * WebResourceRequest, int, SafeBrowsingResponseCompat)}.
     */
    public static final String SAFE_BROWSING_HIT = "SAFE_BROWSING_HIT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link TracingController#getInstance()},
     * {@link TracingController#isTracing()},
     * {@link TracingController#start(TracingConfig)},
     * {@link TracingController#stop(OutputStream, Executor)}.
     */
    public static final String TRACING_CONTROLLER_BASIC_USAGE =
            "TRACING_CONTROLLER_BASIC_USAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    public static final String WEB_RESOURCE_REQUEST_IS_REDIRECT =
            "WEB_RESOURCE_REQUEST_IS_REDIRECT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebResourceErrorCompat#getDescription()}.
     */
    public static final String WEB_RESOURCE_ERROR_GET_DESCRIPTION =
            "WEB_RESOURCE_ERROR_GET_DESCRIPTION";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebResourceErrorCompat#getErrorCode()}.
     */
    public static final String WEB_RESOURCE_ERROR_GET_CODE =
            "WEB_RESOURCE_ERROR_GET_CODE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link SafeBrowsingResponseCompat#backToSafety(boolean)}.
     */
    public static final String SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY =
            "SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link SafeBrowsingResponseCompat#proceed(boolean)}.
     */
    public static final String SAFE_BROWSING_RESPONSE_PROCEED =
            "SAFE_BROWSING_RESPONSE_PROCEED";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link SafeBrowsingResponseCompat#showInterstitial(boolean)}.
     */
    public static final String SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL =
            "SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#postMessage(WebMessageCompat)}.
     */
    public static final String WEB_MESSAGE_PORT_POST_MESSAGE =
            "WEB_MESSAGE_PORT_POST_MESSAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#close()}.
     */
    public static final String WEB_MESSAGE_PORT_CLOSE = "WEB_MESSAGE_PORT_CLOSE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#setWebMessageCallback(
     * WebMessagePortCompat.WebMessageCallbackCompat)}, and
     * {@link androidx.webkit.WebMessagePortCompat#setWebMessageCallback(Handler,
     * WebMessagePortCompat.WebMessageCallbackCompat)}.
     */
    public static final String WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK =
            "WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#createWebMessageChannel(WebView)}.
     */
    public static final String CREATE_WEB_MESSAGE_CHANNEL = "CREATE_WEB_MESSAGE_CHANNEL";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    public static final String POST_WEB_MESSAGE = "POST_WEB_MESSAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebMessagePortCompat.WebMessageCallbackCompat#onMessage(WebMessagePortCompat,
     * WebMessageCompat)}.
     */
    public static final String WEB_MESSAGE_CALLBACK_ON_MESSAGE =
            "WEB_MESSAGE_CALLBACK_ON_MESSAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link androidx.webkit.WebViewCompat#getWebViewClient(WebView)}
     */
    public static final String GET_WEB_VIEW_CLIENT = "GET_WEB_VIEW_CLIENT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link androidx.webkit.WebViewCompat#getWebChromeClient(WebView)}
     */
    public static final String GET_WEB_CHROME_CLIENT = "GET_WEB_CHROME_CLIENT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link androidx.webkit.WebViewCompat#getWebViewRenderProcess(WebView)}
     */
    public static final String GET_WEB_VIEW_RENDERER = "GET_WEB_VIEW_RENDERER";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewRenderProcess#terminate()}
     */
    public static final String WEB_VIEW_RENDERER_TERMINATE = "WEB_VIEW_RENDERER_TERMINATE";

    /**
     i* Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#getWebViewRenderProcessClient(WebView)},
     * {@link androidx.webkit.WebViewCompat#setWebViewRenderProcessClient(WebView, WebViewRenderProcessClient)},
     */
    public static final String WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE =
            "WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link ProxyController#setProxyOverride(ProxyConfig, Executor, Runnable)},
     * {@link ProxyController#clearProxyOverride(Executor, Runnable)}, and
     */
    public static final String PROXY_OVERRIDE = "PROXY_OVERRIDE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebSettingsCompat#willSuppressErrorPage(WebSettings)} and
     * {@link WebSettingsCompat#setWillSuppressErrorPage(WebSettings, boolean)}.
     *
     * TODO(cricke): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String SUPPRESS_ERROR_PAGE = "SUPPRESS_ERROR_PAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#isMultiProcessEnabled()}
     */
    public static final String MULTI_PROCESS = "MULTI_PROCESS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebSettingsCompat#setForceDark(WebSettings, int)} and
     * {@link WebSettingsCompat#getForceDark(WebSettings)}.
     */
    public static final String FORCE_DARK = "FORCE_DARK";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebSettingsCompat#setForceDarkStrategy(WebSettings, int)} and
     * {@link WebSettingsCompat#getForceDarkStrategy(WebSettings)}.
     */
    public static final String FORCE_DARK_STRATEGY = "FORCE_DARK_STRATEGY";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#addWebMessageListener(android.webkit.WebView,
     * String, Set, WebViewCompat.WebMessageListener)} and {@link
     * WebViewCompat#removeWebMessageListener(android.webkit.WebView, String)}.
     */
    public static final String WEB_MESSAGE_LISTENER = "WEB_MESSAGE_LISTENER";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#addDocumentStartJavaScript(android.webkit.WebView,
     * String, Set)}.
     */
    public static final String DOCUMENT_START_SCRIPT = "DOCUMENT_START_SCRIPT";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.ProxyConfig.Builder#setReverseBypassEnabled(boolean)}
     */
    public static final String PROXY_OVERRIDE_REVERSE_BYPASS = "PROXY_OVERRIDE_REVERSE_BYPASS";

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
        return WebViewFeatureInternal.isSupported(feature);
    }
}
