/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit.test;

import static java.util.Map.entry;

import androidx.webkit.WebViewFeature;

import java.util.Map;

/**
 * Mapping from public feature constant in {@link WebViewFeature} to the Chromium CL that made it
 * available - typically by removing the DEV_SUFFIX, but sometimes by directly introducing the
 * feature.
 *
 * <p>New features should be added at the bottom, and the entries should be kept in ascending CL
 * order.
 */
@SuppressWarnings("deprecation")
public final class PublicFeatureAvailability {

    private PublicFeatureAvailability() {}

    public static final Map<String, String> PUBLIC_FEATURE_UNHIDE_CLS =
            Map.ofEntries(
                    entry(WebViewFeature.VISUAL_STATE_CALLBACK, "https://crrev.com/c/941805"),
                    entry(WebViewFeature.OFF_SCREEN_PRERASTER, "https://crrev.com/c/995933"),
                    entry(WebViewFeature.SAFE_BROWSING_ENABLE, "https://crrev.com/c/995933"),
                    entry(
                            WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
                            "https://crrev.com/c/995933"),
                    entry(WebViewFeature.START_SAFE_BROWSING, "https://crrev.com/c/995933"),
                    entry(WebViewFeature.SAFE_BROWSING_WHITELIST, "https://crrev.com/c/995933"),
                    entry(
                            WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
                            "https://crrev.com/c/995933"),
                    entry(WebViewFeature.SERVICE_WORKER_BASIC_USAGE, "https://crrev.com/c/995933"),
                    entry(WebViewFeature.SERVICE_WORKER_CACHE_MODE, "https://crrev.com/c/995933"),
                    entry(
                            WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS,
                            "https://crrev.com/c/995933"),
                    entry(WebViewFeature.SERVICE_WORKER_FILE_ACCESS, "https://crrev.com/c/995933"),
                    entry(
                            WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS,
                            "https://crrev.com/c/995933"),
                    entry(
                            WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST,
                            "https://crrev.com/c/998164"),
                    entry(
                            WebViewFeature.WEB_RESOURCE_REQUEST_IS_REDIRECT,
                            "https://crrev.com/c/1005755"),
                    entry(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR, "https://crrev.com/c/1006051"),
                    entry(WebViewFeature.RECEIVE_HTTP_ERROR, "https://crrev.com/c/1006051"),
                    entry(WebViewFeature.SAFE_BROWSING_HIT, "https://crrev.com/c/1006051"),
                    entry(
                            WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS,
                            "https://crrev.com/c/1006051"),
                    entry(
                            WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION,
                            "https://crrev.com/c/1010717"),
                    entry(
                            WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE,
                            "https://crrev.com/c/1010717"),
                    entry(
                            WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY,
                            "https://crrev.com/c/1010717"),
                    entry(
                            WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED,
                            "https://crrev.com/c/1010717"),
                    entry(
                            WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL,
                            "https://crrev.com/c/1010717"),
                    entry(
                            WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE,
                            "https://crrev.com/c/1041931"),
                    entry(WebViewFeature.WEB_MESSAGE_PORT_CLOSE, "https://crrev.com/c/1041931"),
                    entry(
                            WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK,
                            "https://crrev.com/c/1041931"),
                    entry(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL, "https://crrev.com/c/1041931"),
                    entry(WebViewFeature.POST_WEB_MESSAGE, "https://crrev.com/c/1041931"),
                    entry(
                            WebViewFeature.WEB_MESSAGE_CALLBACK_ON_MESSAGE,
                            "https://crrev.com/c/1041931"),
                    entry(WebViewFeature.GET_WEB_VIEW_CLIENT, "https://crrev.com/c/1101680"),
                    entry(WebViewFeature.GET_WEB_CHROME_CLIENT, "https://crrev.com/c/1107273"),
                    entry(WebViewFeature.GET_WEB_VIEW_RENDERER, "https://crrev.com/c/1185595"),
                    entry(
                            WebViewFeature.WEB_VIEW_RENDERER_TERMINATE,
                            "https://crrev.com/c/1185595"),
                    entry(
                            WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE,
                            "https://crrev.com/c/1225874"),
                    entry(
                            WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
                            "https://crrev.com/c/1410244"),
                    entry(WebViewFeature.PROXY_OVERRIDE, "https://crrev.com/c/1442719"),
                    entry(WebViewFeature.FORCE_DARK, "https://crrev.com/c/1730917"),
                    entry(WebViewFeature.MULTI_PROCESS, "https://crrev.com/c/1738371"),
                    entry(WebViewFeature.WEB_MESSAGE_LISTENER, "https://crrev.com/c/2099233"),
                    entry(WebViewFeature.FORCE_DARK_STRATEGY, "https://crrev.com/c/2132331"),
                    entry(WebViewFeature.SAFE_BROWSING_ALLOWLIST, "https://crrev.com/c/2354994"),
                    entry(
                            WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS,
                            "https://crrev.com/c/2653051"),
                    entry(WebViewFeature.DOCUMENT_START_SCRIPT, "https://crrev.com/c/2761958"),
                    entry(WebViewFeature.GET_VARIATIONS_HEADER, "https://crrev.com/c/3514478"),
                    entry(WebViewFeature.ALGORITHMIC_DARKENING, "https://crrev.com/c/3591014"),
                    entry(
                            WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
                            "https://crrev.com/c/3804885"),
                    entry(WebViewFeature.GET_COOKIE_INFO, "https://crrev.com/c/3910131"),
                    entry(
                            WebViewFeature.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX,
                            "https://crrev.com/c/3977910"),
                    entry(
                            WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS,
                            "https://crrev.com/c/4300277"),
                    entry(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER, "https://crrev.com/c/4568044"),
                    entry(WebViewFeature.USER_AGENT_METADATA, "https://crrev.com/c/4894976"),
                    entry(
                            WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS,
                            "https://crrev.com/c/4894976"),
                    entry(WebViewFeature.MULTI_PROFILE, "https://crrev.com/c/4895669"),
                    entry(
                            WebViewFeature.ATTRIBUTION_REGISTRATION_BEHAVIOR,
                            "https://crrev.com/c/4898539"),
                    entry(
                            WebViewFeature.WEBVIEW_MEDIA_INTEGRITY_API_STATUS,
                            "https://crrev.com/c/5066170"),
                    entry(WebViewFeature.MUTE_AUDIO, "https://crrev.com/c/5291076"),
                    entry(WebViewFeature.SPECULATIVE_LOADING, "https://crrev.com/c/5587721"),
                    entry(WebViewFeature.BACK_FORWARD_CACHE, "https://crrev.com/c/5587721"),
                    entry(
                            WebViewFeature.STARTUP_FEATURE_CONFIGURE_PARTITIONED_COOKIES,
                            "https://crrev.com/c/5850034"),
                    entry(WebViewFeature.WEB_AUTHENTICATION, "https://crrev.com/c/5903910"),
                    entry(
                            WebViewFeature.DEFAULT_TRAFFICSTATS_TAGGING,
                            "https://crrev.com/c/6054142"),
                    entry(WebViewFeature.DELETE_BROWSING_DATA, "https://crrev.com/c/6098412"),
                    entry(WebViewFeature.PROFILE_URL_PREFETCH, "https://crrev.com/c/6110487"),
                    entry(WebViewFeature.SPECULATIVE_LOADING_CONFIG, "https://crrev.com/c/6270257"),
                    entry(WebViewFeature.PRERENDER_WITH_URL, "https://crrev.com/c/6276197"),
                    entry(
                            WebViewFeature.PROVIDER_WEAKLY_REF_WEBVIEW,
                            "https://crrev.com/c/6355010"),
                    entry(WebViewFeature.PAYMENT_REQUEST, "https://crrev.com/c/6375563"),
                    entry(WebViewFeature.SAVE_STATE, "https://crrev.com/c/6375603"),
                    entry(WebViewFeature.COOKIE_INTERCEPT, "https://crrev.com/c/6574075"),
                    entry(WebViewFeature.WARM_UP_RENDERER_PROCESS, "https://crrev.com/c/6633693"),
                    entry(
                            WebViewFeature.BACK_FORWARD_CACHE_SETTINGS,
                            "https://crrev.com/c/6716021"),
                    entry(
                            WebViewFeature.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE,
                            "https://crrev.com/c/6771138"),
                    entry(
                            WebViewFeature.STARTUP_FEATURE_SET_PROFILES_TO_LOAD,
                            "https://crrev.com/c/6802221"),
                    entry(WebViewFeature.PRECONNECT, "https://crrev.com/c/6829839"),
                    entry(WebViewFeature.CUSTOM_REQUEST_HEADERS, "https://crrev.com/c/6983153"),
                    entry(
                            WebViewFeature.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE_V2,
                            "https://crrev.com/c/7079050"),
                    entry(WebViewFeature.ADD_QUIC_HINTS_V1, "https://crrev.com/c/7079589"),
                    entry(WebViewFeature.PAGE_GET_URL, "https://crrev.com/c/7317696"),
                    entry(WebViewFeature.NAVIGATION_LISTENER, "https://crrev.com/c/7317696"),
                    entry(
                            WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3,
                            "https://crrev.com/c/7483433"),
                    entry(
                            WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD,
                            "https://crrev.com/c/7571230"),
                    entry(
                            WebViewFeature.NAVIGATION_GET_WEB_RESOURCE_ERROR,
                            "https://crrev.com/c/7575118"),
                    entry(
                            WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V1,
                            "https://crrev.com/c/7657727"),
                    entry(
                            WebViewFeature.WEBVIEW_BUILDER_EXPERIMENTAL_V2,
                            "https://crrev.com/c/7657727"),
                    entry(
                            WebViewFeature.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4,
                            "https://crrev.com/c/7669238"),
                    entry(
                            WebViewFeature.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1,
                            "https://crrev.com/c/7829939"),
                    entry(WebViewFeature.HTTP_CACHE_MANAGER, "https://crrev.com/c/7958057"),
                    entry(WebViewFeature.ENQUEUE_PRECONNECT, "https://crrev.com/c/7979079"),
                    entry(WebViewFeature.DOWNLOAD_FAVICONS_ENABLED, "https://crrev.com/c/7984627"),
                    entry(
                            WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST,
                            "https://crrev.com/c/8233544"),
                    entry(
                            WebViewFeature.WEBVIEW_NAVIGATE_DRAIN_PREFETCH,
                            "https://crrev.com/c/8256949"));
}
