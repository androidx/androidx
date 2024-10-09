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
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.webkit.internal.WebViewFeatureInternal;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Utility class for checking which WebView Support Library features are supported on the device.
 */
public class WebViewFeature {

    private WebViewFeature() {}

    /**
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
            WEB_MESSAGE_ARRAY_BUFFER,
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
            MULTI_PROCESS,
            FORCE_DARK,
            FORCE_DARK_STRATEGY,
            WEB_MESSAGE_LISTENER,
            DOCUMENT_START_SCRIPT,
            PROXY_OVERRIDE_REVERSE_BYPASS,
            GET_VARIATIONS_HEADER,
            ALGORITHMIC_DARKENING,
            ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
            GET_COOKIE_INFO,
            REQUESTED_WITH_HEADER_ALLOW_LIST,
            USER_AGENT_METADATA,
            MULTI_PROFILE,
            ATTRIBUTION_REGISTRATION_BEHAVIOR,
            WEBVIEW_MEDIA_INTEGRITY_API_STATUS,
            MUTE_AUDIO,
            PROFILE_URL_PREFETCH,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface WebViewSupportFeature {}

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(value = {
            STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
            STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface WebViewStartupFeature {}

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
     * {@link WebMessagePortCompat#postMessage(WebMessageCompat)} with ArrayBuffer type,
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)} with ArrayBuffer type,
     * and {@link JavaScriptReplyProxy#postMessage(byte[])}.
     */
    public static final String WEB_MESSAGE_ARRAY_BUFFER = "WEB_MESSAGE_ARRAY_BUFFER";

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
     * This feature covers
     * {@link WebSettingsCompat#setAlgorithmicDarkeningAllowed(WebSettings, boolean)} and
     * {@link WebSettingsCompat#isAlgorithmicDarkeningAllowed(WebSettings)}.
     */
    public static final String ALGORITHMIC_DARKENING = "ALGORITHMIC_DARKENING";

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
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link WebViewCompat#getVariationsHeader()}.
     */
    public static final String GET_VARIATIONS_HEADER = "GET_VARIATIONS_HEADER";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link WebSettingsCompat#setEnterpriseAuthenticationAppLinkPolicyEnabled(WebSettings, boolean)}and
     * {@link WebSettingsCompat#getEnterpriseAuthenticationAppLinkPolicyEnabled(WebSettings)}.
     *
     */
    public static final String ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY =
            "ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link CookieManagerCompat#getCookieInfo(CookieManager, String)}.
     */
    public static final String GET_COOKIE_INFO = "GET_COOKIE_INFO";

    /**
     * Feature for {@link #isStartupFeatureSupported(Context, String)}.
     * This feature covers
     * {@link androidx.webkit.ProcessGlobalConfig#setDataDirectorySuffix(Context, String)}.
     */
    public static final String STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX =
            "STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX";

    /**
     * Feature for {@link #isStartupFeatureSupported(Context, String)}.
     * This feature covers
     * {@link androidx.webkit.ProcessGlobalConfig#setDirectoryBasePaths(Context, File, File)}
     */
    public static final String STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS =
            "STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getRequestedWithHeaderOriginAllowList(WebSettings)],
     * {@link androidx.webkit.WebSettingsCompat#setRequestedWithHeaderAllowList(WebSettings, Set)},
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getRequestedWithHeaderAllowList(WebSettings)}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setRequestedWithHeaderAllowList(WebSettings, Set)}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String REQUESTED_WITH_HEADER_ALLOW_LIST =
            "REQUESTED_WITH_HEADER_ALLOW_LIST";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getUserAgentMetadata(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setUserAgentMetadata(WebSettings, UserAgentMetadata)}.
     */
    public static final String USER_AGENT_METADATA = "USER_AGENT_METADATA";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link Profile#getName()}.
     * {@link Profile#getWebStorage()}.
     * {@link Profile#getCookieManager()}.
     * {@link Profile#getGeolocationPermissions()}.
     * {@link Profile#getServiceWorkerController()}.
     * {@link ProfileStore#getProfile(String)}.
     * {@link ProfileStore#getOrCreateProfile(String)}.
     * {@link ProfileStore#getAllProfileNames()}.
     * {@link ProfileStore#deleteProfile(String)}.
     * {@link ProfileStore#getInstance()}.
     */
    public static final String MULTI_PROFILE = "MULTI_PROFILE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setAttributionRegistrationBehavior(WebSettings, int)}
     * {@link androidx.webkit.WebSettingsCompat#getAttributionRegistrationBehavior(WebSettings)}
     */
    public static final String ATTRIBUTION_REGISTRATION_BEHAVIOR =
            "ATTRIBUTION_REGISTRATION_BEHAVIOR";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setWebViewMediaIntegrityApiStatus(WebSettings, WebViewMediaIntegrityApiStatusConfig)}
     * {@link androidx.webkit.WebSettingsCompat#getWebViewMediaIntegrityApiStatus(WebSettings)}
     */
    public static final String WEBVIEW_MEDIA_INTEGRITY_API_STATUS =
            "WEBVIEW_MEDIA_INTEGRITY_API_STATUS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#isAudioMuted(WebView)}
     * {@link androidx.webkit.WebViewCompat#setAudioMuted(WebView, boolean)}
     */
    public static final String MUTE_AUDIO = "MUTE_AUDIO";

    /**
     * Feature for {@link #isFeatureSupported(String)}
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setWebAuthenticationSupport(WebSettings, int)}
     * {@link androidx.webkit.WebSettingsCompat#getWebAuthenticationSupport(WebSettings)}
     */
    public static final String WEB_AUTHENTICATION = "WEB_AUTHENTICATION";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setSpeculativeLoadingStatus(WebSettings, int)}
     * {@link androidx.webkit.WebSettingsCompat#getSpeculativeLoadingStatus(WebSettings)}}
     */
    public static final String SPECULATIVE_LOADING =
            "SPECULATIVE_LOADING_STATUS";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setBackForwardCacheEnabled(WebSettings, boolean)}
     * {@link androidx.webkit.WebSettingsCompat#getBackForwardCacheEnabled(WebSettings)}
     */
    public static final String BACK_FORWARD_CACHE = "BACK_FORWARD_CACHE";

    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.Profile#prefetchUrlAsync(String, PrefetchParameters)}
     * {@link androidx.webkit.Profile#clearPrefetchAsync(String)}
     */
    @Profile.ExperimentalUrlPrefetch
    public static final String PROFILE_URL_PREFETCH = "PROFILE_URL_PREFETCH";

    /**
     * Return whether a feature is supported at run-time. This will check whether a feature is
     * supported, depending on the combination of the desired feature, the Android version of
     * device, and the WebView APK on the device.
     *
     * <p class="note"><b>Note:</b> This method is different from
     * {@link WebViewFeature#isStartupFeatureSupported(Context, String)} and this method only accepts
     * certain features. Please verify that the correct feature checking method is used for a
     * particular feature.
     *
     * <p class="note"><b>Note:</b> If this method returns {@code false}, it is not safe to invoke
     * the methods requiring the desired feature. Furthermore, if this method returns {@code false}
     * for a particular feature, any callback guarded by that feature will not be invoked.
     *
     * @param feature the feature to be checked
     * @return whether the feature is supported given the current platform SDK and WebView version
     */
    public static boolean isFeatureSupported(@NonNull @WebViewSupportFeature String feature) {
        return WebViewFeatureInternal.isSupported(feature);
    }

    /**
     * Return whether a startup feature is supported at run-time. This will check whether a startup
     * feature is
     * supported, depending on the combination of the desired feature, the Android version of
     * device, and the WebView APK on the device.
     *
     * <p class="note"><b>Note:</b> This method is different from
     * {@link #isFeatureSupported(String)} and this method only accepts startup features. Please
     * verify that the correct feature checking method is used for a particular feature.
     *
     * <p class="note"><b>Note:</b> If this method returns {@code false}, it is not safe to invoke
     * the methods requiring the desired feature. Furthermore, if this method returns {@code false}
     * for a particular feature, any callback guarded by that feature will not be invoked.
     *
     * @param context a Context to access application assets This value cannot be null.
     * @param startupFeature the startup feature to be checked
     * @return whether the feature is supported given the current platform SDK and WebView version
     */
    public static boolean isStartupFeatureSupported(@NonNull Context context,
            @NonNull @WebViewStartupFeature String startupFeature) {
        return WebViewFeatureInternal.isStartupFeatureSupported(startupFeature, context);
    }
}
