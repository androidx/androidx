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

package androidx.webkit.test

// Import as alias to make the map lines shorter.
import androidx.webkit.WebViewFeature as WF

/**
 * Mapping from public feature constant in [WF] to the Chromium CL that made it available -
 * typically by removing the DEV_SUFFIX, but sometimes by directly introducing the feature.
 *
 * New features should be added at the bottom, and the entries should be kept in ascending CL order.
 */
@Suppress("DEPRECATION")
internal val PUBLIC_FEATURE_UNHIDE_CLS =
    mapOf(
        WF.VISUAL_STATE_CALLBACK to "https://crrev.com/c/941805",
        WF.OFF_SCREEN_PRERASTER to "https://crrev.com/c/995933",
        WF.SAFE_BROWSING_ENABLE to "https://crrev.com/c/995933",
        WF.DISABLED_ACTION_MODE_MENU_ITEMS to "https://crrev.com/c/995933",
        WF.START_SAFE_BROWSING to "https://crrev.com/c/995933",
        WF.SAFE_BROWSING_WHITELIST to "https://crrev.com/c/995933",
        WF.SAFE_BROWSING_PRIVACY_POLICY_URL to "https://crrev.com/c/995933",
        WF.SERVICE_WORKER_BASIC_USAGE to "https://crrev.com/c/995933",
        WF.SERVICE_WORKER_CACHE_MODE to "https://crrev.com/c/995933",
        WF.SERVICE_WORKER_CONTENT_ACCESS to "https://crrev.com/c/995933",
        WF.SERVICE_WORKER_FILE_ACCESS to "https://crrev.com/c/995933",
        WF.SERVICE_WORKER_BLOCK_NETWORK_LOADS to "https://crrev.com/c/995933",
        WF.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST to "https://crrev.com/c/998164",
        WF.WEB_RESOURCE_REQUEST_IS_REDIRECT to "https://crrev.com/c/1005755",
        WF.RECEIVE_WEB_RESOURCE_ERROR to "https://crrev.com/c/1006051",
        WF.RECEIVE_HTTP_ERROR to "https://crrev.com/c/1006051",
        WF.SAFE_BROWSING_HIT to "https://crrev.com/c/1006051",
        WF.SHOULD_OVERRIDE_WITH_REDIRECTS to "https://crrev.com/c/1006051",
        WF.WEB_RESOURCE_ERROR_GET_DESCRIPTION to "https://crrev.com/c/1010717",
        WF.WEB_RESOURCE_ERROR_GET_CODE to "https://crrev.com/c/1010717",
        WF.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY to "https://crrev.com/c/1010717",
        WF.SAFE_BROWSING_RESPONSE_PROCEED to "https://crrev.com/c/1010717",
        WF.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL to "https://crrev.com/c/1010717",
        WF.WEB_MESSAGE_PORT_POST_MESSAGE to "https://crrev.com/c/1041931",
        WF.WEB_MESSAGE_PORT_CLOSE to "https://crrev.com/c/1041931",
        WF.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK to "https://crrev.com/c/1041931",
        WF.CREATE_WEB_MESSAGE_CHANNEL to "https://crrev.com/c/1041931",
        WF.POST_WEB_MESSAGE to "https://crrev.com/c/1041931",
        WF.WEB_MESSAGE_CALLBACK_ON_MESSAGE to "https://crrev.com/c/1041931",
        WF.GET_WEB_VIEW_CLIENT to "https://crrev.com/c/1101680",
        WF.GET_WEB_CHROME_CLIENT to "https://crrev.com/c/1107273",
        WF.GET_WEB_VIEW_RENDERER to "https://crrev.com/c/1185595",
        WF.WEB_VIEW_RENDERER_TERMINATE to "https://crrev.com/c/1185595",
        WF.TRACING_CONTROLLER_BASIC_USAGE to "https://crrev.com/c/1225874",
        WF.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE to "https://crrev.com/c/1410244",
        WF.PROXY_OVERRIDE to "https://crrev.com/c/1442719",
        WF.FORCE_DARK to "https://crrev.com/c/1730917",
        WF.MULTI_PROCESS to "https://crrev.com/c/1738371",
        WF.WEB_MESSAGE_LISTENER to "https://crrev.com/c/2099233",
        WF.FORCE_DARK_STRATEGY to "https://crrev.com/c/2132331",
        WF.SAFE_BROWSING_ALLOWLIST to "https://crrev.com/c/2354994",
        WF.PROXY_OVERRIDE_REVERSE_BYPASS to "https://crrev.com/c/2653051",
        WF.DOCUMENT_START_SCRIPT to "https://crrev.com/c/2761958",
        WF.GET_VARIATIONS_HEADER to "https://crrev.com/c/3514478",
        WF.ALGORITHMIC_DARKENING to "https://crrev.com/c/3591014",
        WF.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY to "https://crrev.com/c/3804885",
        WF.GET_COOKIE_INFO to "https://crrev.com/c/3910131",
        WF.STARTUP_FEATURE_SET_DATA_DIRECTORY_SUFFIX to "https://crrev.com/c/3977910",
        WF.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS to "https://crrev.com/c/4300277",
        WF.WEB_MESSAGE_ARRAY_BUFFER to "https://crrev.com/c/4568044",
        WF.USER_AGENT_METADATA to "https://crrev.com/c/4894976",
        WF.USER_AGENT_METADATA_FORM_FACTORS to "https://crrev.com/c/4894976",
        WF.MULTI_PROFILE to "https://crrev.com/c/4895669",
        WF.ATTRIBUTION_REGISTRATION_BEHAVIOR to "https://crrev.com/c/4898539",
        WF.WEBVIEW_MEDIA_INTEGRITY_API_STATUS to "https://crrev.com/c/5066170",
        WF.MUTE_AUDIO to "https://crrev.com/c/5291076",
        WF.SPECULATIVE_LOADING to "https://crrev.com/c/5587721",
        WF.BACK_FORWARD_CACHE to "https://crrev.com/c/5587721",
        WF.STARTUP_FEATURE_CONFIGURE_PARTITIONED_COOKIES to "https://crrev.com/c/5850034",
        WF.WEB_AUTHENTICATION to "https://crrev.com/c/5903910",
        WF.DEFAULT_TRAFFICSTATS_TAGGING to "https://crrev.com/c/6054142",
        WF.DELETE_BROWSING_DATA to "https://crrev.com/c/6098412",
        WF.PROFILE_URL_PREFETCH to "https://crrev.com/c/6110487",
        WF.SPECULATIVE_LOADING_CONFIG to "https://crrev.com/c/6270257",
        WF.PRERENDER_WITH_URL to "https://crrev.com/c/6276197",
        WF.PROVIDER_WEAKLY_REF_WEBVIEW to "https://crrev.com/c/6355010",
        WF.PAYMENT_REQUEST to "https://crrev.com/c/6375563",
        WF.SAVE_STATE to "https://crrev.com/c/6375603",
        WF.COOKIE_INTERCEPT to "https://crrev.com/c/6574075",
        WF.WARM_UP_RENDERER_PROCESS to "https://crrev.com/c/6633693",
        WF.BACK_FORWARD_CACHE_SETTINGS to "https://crrev.com/c/6716021",
        WF.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE to "https://crrev.com/c/6771138",
        WF.STARTUP_FEATURE_SET_PROFILES_TO_LOAD to "https://crrev.com/c/6802221",
        WF.PRECONNECT to "https://crrev.com/c/6829839",
        WF.CUSTOM_REQUEST_HEADERS to "https://crrev.com/c/6983153",
        WF.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE_V2 to "https://crrev.com/c/7079050",
        WF.ADD_QUIC_HINTS_V1 to "https://crrev.com/c/7079589",
        WF.PAGE_GET_URL to "https://crrev.com/c/7317696",
        WF.NAVIGATION_LISTENER to "https://crrev.com/c/7317696",
        WF.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V3 to "https://crrev.com/c/7483433",
        WF.JS_INJECTION_IN_FRAME_AND_WORLD to "https://crrev.com/c/7571230",
        WF.NAVIGATION_GET_WEB_RESOURCE_ERROR to "https://crrev.com/c/7575118",
        WF.WEBVIEW_BUILDER_EXPERIMENTAL_V1 to "https://crrev.com/c/7657727",
        WF.WEBVIEW_BUILDER_EXPERIMENTAL_V2 to "https://crrev.com/c/7657727",
        WF.BACK_FORWARD_CACHE_SETTINGS_EXPERIMENTAL_V4 to "https://crrev.com/c/7669238",
        WF.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1 to "https://crrev.com/c/7829939",
        WF.HTTP_CACHE_MANAGER to "https://crrev.com/c/7958057",
        WF.ENQUEUE_PRECONNECT to "https://crrev.com/c/7979079",
        WF.DOWNLOAD_FAVICONS_ENABLED to "https://crrev.com/c/7984627",
    )
