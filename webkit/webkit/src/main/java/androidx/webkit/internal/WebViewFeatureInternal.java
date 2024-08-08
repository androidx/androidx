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
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.PrefetchParameters;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.SafeBrowsingResponseCompat;
import androidx.webkit.ServiceWorkerClientCompat;
import androidx.webkit.TracingConfig;
import androidx.webkit.TracingController;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebResourceErrorCompat;
import androidx.webkit.WebResourceRequestCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.chromium.support_lib_boundary.util.Features;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enum representing a WebView feature, this provides functionality for determining whether a
 * feature is supported by the current framework and/or WebView APK.
 */
public class WebViewFeatureInternal {
    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#postVisualStateCallback(android.webkit.WebView, long,
     * androidx.webkit.WebViewCompat.VisualStateCallback)}, and
     * {@link WebViewClientCompat#onPageCommitVisible(android.webkit.WebView, String)}.
     */
    public static final ApiFeature.M VISUAL_STATE_CALLBACK = new ApiFeature.M(
            WebViewFeature.VISUAL_STATE_CALLBACK, Features.VISUAL_STATE_CALLBACK);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster(WebSettings, boolean)}.
     */
    public static final ApiFeature.M OFF_SCREEN_PRERASTER = new ApiFeature.M(
            WebViewFeature.OFF_SCREEN_PRERASTER, Features.OFF_SCREEN_PRERASTER);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled(WebSettings, boolean)}.
     */
    public static final ApiFeature.O SAFE_BROWSING_ENABLE = new ApiFeature.O(
            WebViewFeature.SAFE_BROWSING_ENABLE, Features.SAFE_BROWSING_ENABLE);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems(WebSettings)}, and
     * {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems(WebSettings, int)}.
     */
    public static final ApiFeature.N DISABLED_ACTION_MODE_MENU_ITEMS = new ApiFeature.N(
            WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            Features.DISABLED_ACTION_MODE_MENU_ITEMS);

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#startSafeBrowsing(Context, ValueCallback)}.
     */
    public static final ApiFeature.O_MR1 START_SAFE_BROWSING = new ApiFeature.O_MR1(
            WebViewFeature.START_SAFE_BROWSING,
            Features.START_SAFE_BROWSING);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(
     * java.util.List, ValueCallback)}, plumbing through the deprecated boundary interface.
     *
     * <p>Don't use this value directly. This exists only so
     * {@link WebViewFeatureInternal#isSupported(String)} supports the <b>deprecated</b> public
     * feature when running against <b>old</b> WebView versions.
     *
     * @deprecated use {@link #SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED} to test for the
     * <b>old</b> boundary interface
     */
    @Deprecated
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_DEPRECATED_TO_DEPRECATED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_WHITELIST,
                    Features.SAFE_BROWSING_WHITELIST);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingWhitelist(
     * java.util.List, ValueCallback)}, plumbing through the new boundary interface.
     *
     * <p>Don't use this value directly. This exists only so
     * {@link WebViewFeatureInternal#isSupported(String)} supports the <b>deprecated</b> public
     * feature when running against <b>new</b> WebView versions.
     *
     * @deprecated use {@link #SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED} to test for the
     * <b>new</b> boundary interface.
     */
    @Deprecated
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_DEPRECATED_TO_PREFERRED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_WHITELIST,
                    Features.SAFE_BROWSING_ALLOWLIST);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set,
     * ValueCallback)}, plumbing through the deprecated boundary interface.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_ALLOWLIST,
                    Features.SAFE_BROWSING_WHITELIST);

    /**
     * This feature covers {@link androidx.webkit.WebViewCompat#setSafeBrowsingAllowlist(Set,
     * ValueCallback)}, plumbing through the new boundary interface.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_ALLOWLIST,
                    Features.SAFE_BROWSING_ALLOWLIST);

    /**
     * This feature covers
     * {@link WebViewCompat#getSafeBrowsingPrivacyPolicyUrl()}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_PRIVACY_POLICY_URL =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
                    Features.SAFE_BROWSING_PRIVACY_POLICY_URL);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerControllerCompat#getInstance()}.
     */
    public static final ApiFeature.N SERVICE_WORKER_BASIC_USAGE =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_BASIC_USAGE,
                    Features.SERVICE_WORKER_BASIC_USAGE);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getCacheMode()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setCacheMode(int)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_CACHE_MODE =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_CACHE_MODE,
                    Features.SERVICE_WORKER_CACHE_MODE);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowContentAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowContentAccess(boolean)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_CONTENT_ACCESS =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
                    Features.SERVICE_WORKER_CONTENT_ACCESS);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getAllowFileAccess()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setAllowFileAccess(boolean)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_FILE_ACCESS =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_FILE_ACCESS,
                    Features.SERVICE_WORKER_FILE_ACCESS);

    /**
     * This feature covers
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getBlockNetworkLoads()}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setBlockNetworkLoads(boolean)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_BLOCK_NETWORK_LOADS =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
                    Features.SERVICE_WORKER_BLOCK_NETWORK_LOADS);

    /**
     * This feature covers
     * {@link ServiceWorkerClientCompat#shouldInterceptRequest(WebResourceRequest)}.
     */
    public static final ApiFeature.N SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST =
            new ApiFeature.N(WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST,
                    Features.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST);

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedError(android.webkit.WebView, WebResourceRequest,
     * WebResourceErrorCompat)}.
     */
    public static final ApiFeature.M RECEIVE_WEB_RESOURCE_ERROR =
            new ApiFeature.M(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR,
                    Features.RECEIVE_WEB_RESOURCE_ERROR);

    /**
     * This feature covers
     * {@link WebViewClientCompat#onReceivedHttpError(android.webkit.WebView, WebResourceRequest,
     * WebResourceResponse)}.
     */
    public static final ApiFeature.M RECEIVE_HTTP_ERROR = new ApiFeature.M(
            WebViewFeature.RECEIVE_HTTP_ERROR, Features.RECEIVE_HTTP_ERROR);

    /**
     * This feature covers
     * {@link WebViewClientCompat#shouldOverrideUrlLoading(android.webkit.WebView,
     * WebResourceRequest)}.
     */
    public static final ApiFeature.N SHOULD_OVERRIDE_WITH_REDIRECTS =
            new ApiFeature.N(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS,
                    Features.SHOULD_OVERRIDE_WITH_REDIRECTS);

    /**
     * This feature covers
     * {@link WebViewClientCompat#onSafeBrowsingHit(android.webkit.WebView,
     * WebResourceRequest, int, SafeBrowsingResponseCompat)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_HIT = new ApiFeature.O_MR1(
            WebViewFeature.SAFE_BROWSING_HIT, Features.SAFE_BROWSING_HIT);

    /**
     * This feature covers
     * {@link WebResourceRequestCompat#isRedirect(WebResourceRequest)}.
     */
    public static final ApiFeature.N WEB_RESOURCE_REQUEST_IS_REDIRECT =
            new ApiFeature.N(WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT,
                    Features.WEB_RESOURCE_REQUEST_IS_REDIRECT);

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getDescription()}.
     */
    public static final ApiFeature.M WEB_RESOURCE_ERROR_GET_DESCRIPTION =
            new ApiFeature.M(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION,
                    Features.WEB_RESOURCE_ERROR_GET_DESCRIPTION);

    /**
     * This feature covers
     * {@link WebResourceErrorCompat#getErrorCode()}.
     */
    public static final ApiFeature.M WEB_RESOURCE_ERROR_GET_CODE =
            new ApiFeature.M(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE,
                    Features.WEB_RESOURCE_ERROR_GET_CODE);

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#backToSafety(boolean)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
                    Features.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY);

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#proceed(boolean)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_RESPONSE_PROCEED =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED,
                    Features.SAFE_BROWSING_RESPONSE_PROCEED);

    /**
     * This feature covers
     * {@link SafeBrowsingResponseCompat#showInterstitial(boolean)}.
     */
    public static final ApiFeature.O_MR1 SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL =
            new ApiFeature.O_MR1(WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
                    Features.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL);

    /**
     * This feature covers
     * {@link WebMessagePortCompat#postMessage(WebMessageCompat)}.
     */
    public static final ApiFeature.M WEB_MESSAGE_PORT_POST_MESSAGE =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE,
                    Features.WEB_MESSAGE_PORT_POST_MESSAGE);

    /**
     * * This feature covers
     * {@link androidx.webkit.WebMessagePortCompat#close()}.
     */
    public static final ApiFeature.M WEB_MESSAGE_PORT_CLOSE =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_PORT_CLOSE,
                    Features.WEB_MESSAGE_PORT_CLOSE);

    /**
     * This feature covers
     * {@link WebMessagePortCompat#postMessage(WebMessageCompat)} with ArrayBuffer type, and
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)} with ArrayBuffer type.
     */
    public static final ApiFeature.NoFramework WEB_MESSAGE_ARRAY_BUFFER =
            new ApiFeature.NoFramework(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER,
                    Features.WEB_MESSAGE_ARRAY_BUFFER);

    /**
     * This feature covers
     * {@link WebMessagePortCompat#setWebMessageCallback(
     *WebMessagePortCompat.WebMessageCallbackCompat)}, and
     * {@link WebMessagePortCompat#setWebMessageCallback(Handler,
     * WebMessagePortCompat.WebMessageCallbackCompat)}.
     */
    public static final ApiFeature.M WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK,
                    Features.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK);

    /**
     * This feature covers
     * {@link WebViewCompat#createWebMessageChannel(WebView)}.
     */
    public static final ApiFeature.M CREATE_WEB_MESSAGE_CHANNEL =
            new ApiFeature.M(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL,
                    Features.CREATE_WEB_MESSAGE_CHANNEL);

    /**
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    public static final ApiFeature.M POST_WEB_MESSAGE = new ApiFeature.M(
            WebViewFeature.POST_WEB_MESSAGE, Features.POST_WEB_MESSAGE);

    /**
     * This feature covers
     * {@link WebViewCompat#postWebMessage(WebView, WebMessageCompat, Uri)}.
     */
    public static final ApiFeature.M WEB_MESSAGE_CALLBACK_ON_MESSAGE =
            new ApiFeature.M(WebViewFeature.WEB_MESSAGE_CALLBACK_ON_MESSAGE,
                    Features.WEB_MESSAGE_CALLBACK_ON_MESSAGE);

    /**
     * This feature covers {@link WebViewCompat#getWebViewClient(WebView)}.
     */
    public static final ApiFeature.O GET_WEB_VIEW_CLIENT = new ApiFeature.O(
            WebViewFeature.GET_WEB_VIEW_CLIENT, Features.GET_WEB_VIEW_CLIENT);

    /**
     * This feature covers {@link WebViewCompat#getWebChromeClient(WebView)}.
     */
    public static final ApiFeature.O GET_WEB_CHROME_CLIENT =
            new ApiFeature.O(WebViewFeature.GET_WEB_CHROME_CLIENT, Features.GET_WEB_CHROME_CLIENT);

    public static final ApiFeature.Q GET_WEB_VIEW_RENDERER =
            new ApiFeature.Q(WebViewFeature.GET_WEB_VIEW_RENDERER, Features.GET_WEB_VIEW_RENDERER);
    public static final ApiFeature.Q WEB_VIEW_RENDERER_TERMINATE =
            new ApiFeature.Q(WebViewFeature.WEB_VIEW_RENDERER_TERMINATE,
                    Features.WEB_VIEW_RENDERER_TERMINATE);

    /**
     * This feature covers
     * {@link TracingController#getInstance()},
     * {@link TracingController#isTracing()},
     * {@link TracingController#start(TracingConfig)},
     * {@link TracingController#stop(OutputStream, Executor)}.
     */
    public static final ApiFeature.P TRACING_CONTROLLER_BASIC_USAGE =
            new ApiFeature.P(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
                    Features.TRACING_CONTROLLER_BASIC_USAGE);

    /**
     * This feature covers
     * {@link androidx.webkit.ProcessGlobalConfig#setDataDirectorySuffix(Context, String)}
     */
    public static final StartupApiFeature.P STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX =
            new StartupApiFeature.P(WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
                    StartupFeatures.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX);

    /**
     * This feature covers
     * {@link androidx.webkit.ProcessGlobalConfig#setDirectoryBasePaths(Context, File, File)}.
     */
    public static final StartupApiFeature.NoFramework STARTUP_FEATURE_SET_DIRECTORY_BASE_PATH =
            new StartupApiFeature.NoFramework(
                    WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
                    StartupFeatures.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATH);

    /**
     * This feature covers
     * {@link WebViewCompat#getWebViewRenderProcessClient(android.webkit.WebView)},
     * {@link WebViewCompat#setWebViewRenderProcessClient(WebView, androidx.webkit.WebViewRenderProcessClient)},
     * {@link android.webkit.WebViewRenderProcessClient#onRenderProcessUnresponsive(WebView, android.webkit.WebViewRenderProcess)},
     * {@link android.webkit.WebViewRenderProcessClient#onRenderProcessResponsive(WebView, android.webkit.WebViewRenderProcess)}
     */
    public static final ApiFeature.Q WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE =
            new ApiFeature.Q(WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
                    Features.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setAlgorithmicDarkeningAllowed(WebSettings, boolean)} and
     * {@link androidx.webkit.WebSettingsCompat#isAlgorithmicDarkeningAllowed(WebSettings)}.
     */
    public static final ApiFeature.T ALGORITHMIC_DARKENING =
            new ApiFeature.T(WebViewFeature.ALGORITHMIC_DARKENING, Features.ALGORITHMIC_DARKENING) {
                private final Pattern mVersionPattern = Pattern.compile("\\A\\d+");
                @Override
                public boolean isSupportedByWebView() {
                    boolean supported = super.isSupportedByWebView();
                    if (!supported || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        return supported;
                    }
                    //Since version 105, WebView has supported the algorithmic darkening for
                    // pre-Q Android platform.
                    // WebView should have been loaded.
                    PackageInfo info = WebViewCompat.getCurrentLoadedWebViewPackage();
                    if (info == null) return false;
                    Matcher m = mVersionPattern.matcher(info.versionName);
                    return m.find() && Integer.parseInt(info.versionName.substring(m.start(),
                                m.end())) >= 105;
                }
            };

    /**
     * This feature covers
     * {@link ProxyController#setProxyOverride(ProxyConfig, Executor, Runnable)}, and
     * {@link ProxyController#clearProxyOverride(Executor, Runnable)}.
     */
    public static final ApiFeature.NoFramework PROXY_OVERRIDE = new ApiFeature.NoFramework(
            WebViewFeature.PROXY_OVERRIDE, Features.PROXY_OVERRIDE);


    /**
     * This feature covers {@link WebViewCompat#isMultiProcessEnabled()}.
     */
    public static final ApiFeature.NoFramework MULTI_PROCESS = new ApiFeature.NoFramework(
            WebViewFeature.MULTI_PROCESS, Features.MULTI_PROCESS_QUERY);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setForceDark(WebSettings, int)} and
     * {@link androidx.webkit.WebSettingsCompat#getForceDark(WebSettings)}.
     */
    public static final ApiFeature.Q FORCE_DARK = new ApiFeature.Q(
            WebViewFeature.FORCE_DARK, Features.FORCE_DARK);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setForceDarkStrategy(WebSettings, int)} and
     * {@link androidx.webkit.WebSettingsCompat#getForceDarkStrategy(WebSettings)}.
     */
    public static final ApiFeature.NoFramework FORCE_DARK_STRATEGY =
            new ApiFeature.NoFramework(WebViewFeature.FORCE_DARK_STRATEGY,
                    Features.FORCE_DARK_BEHAVIOR);

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#addWebMessageListener(WebView, String, Set, WebViewCompat.WebMessageListener)} and
     * {@link androidx.webkit.WebViewCompat#removeWebMessageListener(WebView, String)}
     */
    public static final ApiFeature.NoFramework WEB_MESSAGE_LISTENER =
            new ApiFeature.NoFramework(WebViewFeature.WEB_MESSAGE_LISTENER,
                    Features.WEB_MESSAGE_LISTENER);

    /**
     * This feature covers
     * {@link
     * androidx.webkit.WebViewCompat#addDocumentStartJavaScript(android.webkit.WebView, String,
     * Set)}
     */
    public static final ApiFeature.NoFramework DOCUMENT_START_SCRIPT =
            new ApiFeature.NoFramework(WebViewFeature.DOCUMENT_START_SCRIPT,
                    Features.DOCUMENT_START_SCRIPT);

    /**
     * This feature covers {@link
     * androidx.webkit.ProxyConfig.Builder#setReverseBypassEnabled(boolean)}
     */
    public static final ApiFeature.NoFramework PROXY_OVERRIDE_REVERSE_BYPASS =
            new ApiFeature.NoFramework(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS,
                    Features.PROXY_OVERRIDE_REVERSE_BYPASS);

    /**
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#getVariationsHeader()}
     */
    public static final ApiFeature.NoFramework GET_VARIATIONS_HEADER =
            new ApiFeature.NoFramework(WebViewFeature.GET_VARIATIONS_HEADER,
                    Features.GET_VARIATIONS_HEADER);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setEnterpriseAuthenticationAppLinkPolicyEnabled(WebSettings, boolean)} and
     * {@link androidx.webkit.WebSettingsCompat#getEnterpriseAuthenticationAppLinkPolicyEnabled(WebSettings)}.
     */
    public static final ApiFeature.NoFramework ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY =
            new ApiFeature.NoFramework(WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
                    Features.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY);

    /**
     * This feature covers
     * {@link androidx.webkit.CookieManagerCompat#getCookieInfo(android.webkit.CookieManager, String)}.
     */
    public static final ApiFeature.NoFramework GET_COOKIE_INFO =
            new ApiFeature.NoFramework(WebViewFeature.GET_COOKIE_INFO, Features.GET_COOKIE_INFO);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#getRequestedWithHeaderOriginAllowList(WebSettings)],
     * {@link androidx.webkit.WebSettingsCompat#setRequestedWithHeaderAllowList(WebSettings, Set)},
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#getRequestedWithHeaderAllowList(WebSettings)}, and
     * {@link androidx.webkit.ServiceWorkerWebSettingsCompat#setRequestedWithHeaderAllowList(WebSettings, Set)}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final ApiFeature.NoFramework REQUESTED_WITH_HEADER_ALLOW_LIST =
            new ApiFeature.NoFramework(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST,
                    Features.REQUESTED_WITH_HEADER_ALLOW_LIST);

    /**
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setUserAgentMetadata(WebSettings, androidx.webkit.UserAgentMetadata)} and
     * {@link androidx.webkit.WebSettingsCompat#getUserAgentMetadata(WebSettings)}.
     *
     */
    public static final ApiFeature.NoFramework USER_AGENT_METADATA =
            new ApiFeature.NoFramework(WebViewFeature.USER_AGENT_METADATA,
                    Features.USER_AGENT_METADATA);

    /**
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
    public static final ApiFeature.NoFramework MULTI_PROFILE =
            new ApiFeature.NoFramework(WebViewFeature.MULTI_PROFILE, Features.MULTI_PROFILE) {
                @Override
                public boolean isSupportedByWebView() {
                    // Multi-process mode is a requirement for Multi-Profile feature.
                    if (!super.isSupportedByWebView()) {
                        return false;
                    }
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)) {
                        return WebViewCompat.isMultiProcessEnabled();
                    }
                    return false;
                }
            };

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setAttributionRegistrationBehavior(WebSettings, int)}
     * {@link androidx.webkit.WebSettingsCompat#getAttributionRegistrationBehavior(WebSettings)}
     */
    public static final ApiFeature.NoFramework ATTRIBUTION_REGISTRATION_BEHAVIOR =
            new ApiFeature.NoFramework(WebViewFeature.ATTRIBUTION_REGISTRATION_BEHAVIOR,
                    Features.ATTRIBUTION_BEHAVIOR);

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setWebViewMediaIntegrityApiStatus(WebSettings, androidx.webkit.WebViewMediaIntegrityApiStatusConfig)}
     * {@link androidx.webkit.WebSettingsCompat#getWebViewMediaIntegrityApiStatus(WebSettings)}
     */
    public static final ApiFeature.NoFramework WEBVIEW_MEDIA_INTEGRITY_API_STATUS =
            new ApiFeature.NoFramework(WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS,
                    Features.WEBVIEW_MEDIA_INTEGRITY_API_STATUS);

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebViewCompat#isAudioMuted(WebView)}
     * {@link androidx.webkit.WebViewCompat#setAudioMuted(WebView, boolean)}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final ApiFeature.NoFramework MUTE_AUDIO =
            new ApiFeature.NoFramework(WebViewFeature.MUTE_AUDIO,
                    Features.MUTE_AUDIO);

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setWebAuthenticationSupport(WebSettings, int)}
     * {@link androidx.webkit.WebSettingsCompat#getWebAuthenticationSupport(WebSettings)}
     */
    public static final ApiFeature.NoFramework WEB_AUTHENTICATION = new ApiFeature.NoFramework(
            WebViewFeature.WEB_AUTHENTICATION, Features.WEB_AUTHENTICATION);

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setSpeculativeLoadingStatus(WebSettings, int)}
     * {@link androidx.webkit.WebSettingsCompat#getSpeculativeLoadingStatus(WebSettings)}
     */
    public static final ApiFeature.NoFramework SPECULATIVE_LOADING =
            new ApiFeature.NoFramework(WebViewFeature.SPECULATIVE_LOADING,
                    Features.SPECULATIVE_LOADING);

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.WebSettingsCompat#setBackForwardCacheEnabled(WebSettings, boolean)}
     * {@link androidx.webkit.WebSettingsCompat#getBackForwardCacheEnabled(WebSettings)}
     */
    public static final ApiFeature.NoFramework BACK_FORWARD_CACHE =
            new ApiFeature.NoFramework(WebViewFeature.BACK_FORWARD_CACHE,
                    Features.BACK_FORWARD_CACHE);

    /**
     * Feature for {@link WebViewFeature#isFeatureSupported(String)}.
     * This feature covers
     * {@link androidx.webkit.Profile#prefetchUrlAsync(String, PrefetchParameters)}
     * {@link androidx.webkit.Profile#clearPrefetchAsync(String)}
     */
    public static final ApiFeature.NoFramework PROFILE_URL_PREFETCH =
            new ApiFeature.NoFramework(WebViewFeature.PROFILE_URL_PREFETCH,
                    Features.PREFETCH_WITH_URL) {
                @Override
                public boolean isSupportedByWebView() {
                    // Multi-profile is a requirement for this feature.
                    if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                        return false;
                    }
                    return super.isSupportedByWebView();
                }
            };

    // --- Add new feature constants above this line ---

    private WebViewFeatureInternal() {
        // Class should not be instantiated
    }

    /**
     * Return whether a public feature is supported by any internal features defined in this class.
     */
    public static boolean isSupported(
            @NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue) {
        return isSupported(publicFeatureValue, ApiFeature.values());
    }

    /**
     * Return whether a public startup feature is supported by any internal features defined in
     * this class.
     */
    public static boolean isStartupFeatureSupported(
            @NonNull @WebViewFeature.WebViewStartupFeature String publicFeatureValue,
            @NonNull Context context) {
        return isStartupFeatureSupported(publicFeatureValue, StartupApiFeature.values(), context);
    }

    /**
     * Return whether a public feature is supported by any {@link ConditionallySupportedFeature}s
     * defined in {@code internalFeatures}.
     *
     * @throws RuntimeException if {@code publicFeatureValue} is not matched in
     *      {@code internalFeatures}
     */
    @VisibleForTesting
    public static <T extends ConditionallySupportedFeature> boolean isSupported(
            @NonNull @WebViewFeature.WebViewSupportFeature String publicFeatureValue,
            @NonNull Collection<T> internalFeatures) {
        Set<ConditionallySupportedFeature> matchingFeatures = new HashSet<>();
        for (ConditionallySupportedFeature feature : internalFeatures) {
            if (feature.getPublicFeatureName().equals(publicFeatureValue)) {
                matchingFeatures.add(feature);
            }
        }
        if (matchingFeatures.isEmpty()) {
            throw new RuntimeException("Unknown feature " + publicFeatureValue);
        }
        for (ConditionallySupportedFeature feature : matchingFeatures) {
            if (feature.isSupported()) return true;
        }
        return false;
    }

    /**
     * Return whether a public startup feature is supported by any {@link StartupApiFeature}s
     * defined in {@code internalFeatures}.
     *
     * @throws RuntimeException if {@code publicFeatureValue} is not matched in
     *      {@code internalFeatures}
     */
    @VisibleForTesting
    public static boolean isStartupFeatureSupported(
            @NonNull @WebViewFeature.WebViewStartupFeature String publicFeatureValue,
            @NonNull Collection<StartupApiFeature> internalFeatures, @NonNull Context context) {
        Set<StartupApiFeature> matchingFeatures = new HashSet<>();
        for (StartupApiFeature feature : internalFeatures) {
            if (feature.getPublicFeatureName().equals(publicFeatureValue)) {
                matchingFeatures.add(feature);
            }
        }
        if (matchingFeatures.isEmpty()) {
            throw new RuntimeException("Unknown feature " + publicFeatureValue);
        }
        for (StartupApiFeature feature : matchingFeatures) {
            if (feature.isSupported(context)) return true;
        }
        return false;
    }

    /**
     * Utility method for throwing an exception explaining that the feature the app trying to use
     * isn't supported.
     */
    @NonNull
    public static UnsupportedOperationException getUnsupportedOperationException() {
        return new UnsupportedOperationException("This method is not supported by the current "
                + "version of the framework and the current WebView APK");
    }
}
