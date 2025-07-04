/*
 * Copyright 2024 The Android Open Source Project
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

import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.webkit.UserAgentMetadata;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewMediaIntegrityApiStatusConfig;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Set;

/**
 * No-op adapter for WebSettingsCompat.
 *
 * <p>In a few rare cases, the OS will return a wrapped WebSettings object from
 * {@link WebView#getSettings()}, which cannot be used by the AndroidX library.
 * In that case, this adapter will be used to prevent an app crash.
 */
public class WebSettingsNoOpAdapter extends WebSettingsAdapter {

    public WebSettingsNoOpAdapter() {
        // Deliberately initialize super with null. This class is meant to override all
        // methods that call the passed-in boundary interface.
        // This is asserted by WebSettingsNoOpAdapterTest.

        //noinspection DataFlowIssue
        super(null);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster}.
     */
    @Override
    public void setOffscreenPreRaster(boolean enabled) {
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster}.
     */
    @Override
    public boolean getOffscreenPreRaster() {
        return false;
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled}.
     */
    @Override
    public void setSafeBrowsingEnabled(boolean enabled) {
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled}.
     */
    @Override
    public boolean getSafeBrowsingEnabled() {
        return true;
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems}.
     */
    @Override
    public void setDisabledActionModeMenuItems(int menuItems) {
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems}.
     */
    @Override
    public int getDisabledActionModeMenuItems() {
        return 0;
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setForceDark}.
     */
    @Override
    public void setForceDark(int forceDarkMode) {
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getForceDark}.
     */
    @SuppressWarnings("deprecation")
    @Override
    public int getForceDark() {
        return WebSettingsCompat.FORCE_DARK_AUTO;
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setForceDarkStrategy}.
     */
    @Override
    public void setForceDarkStrategy(int forceDarkStrategy) {
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getForceDarkStrategy}.
     */
    @SuppressWarnings("deprecation")
    @Override
    public int getForceDarkStrategy() {
        return WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING;
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setAlgorithmicDarkeningAllowed}.
     */
    @Override
    public void setAlgorithmicDarkeningAllowed(boolean allow) {
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#isAlgorithmicDarkeningAllowed}.
     */
    @Override
    public boolean isAlgorithmicDarkeningAllowed() {
        return false;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setEnterpriseAuthenticationAppLinkPolicyEnabled}.
     */
    @Override
    public void setEnterpriseAuthenticationAppLinkPolicyEnabled(boolean enabled) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getEnterpriseAuthenticationAppLinkPolicyEnabled}.
     */
    @Override
    public boolean getEnterpriseAuthenticationAppLinkPolicyEnabled() {
        return false;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getRequestedWithHeaderOriginAllowList(WebSettings)}.
     */
    @Override
    public @NonNull Set<String> getRequestedWithHeaderOriginAllowList() {
        return Collections.emptySet();
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setRequestedWithHeaderOriginAllowList(
     * WebSettings, Set)}.
     */
    @Override
    public void setRequestedWithHeaderOriginAllowList(@NonNull Set<String> allowList) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getUserAgentMetadata(WebSettings)}.
     */
    @Override
    public @NonNull UserAgentMetadata getUserAgentMetadata() {
        return UserAgentMetadataInternal.getUserAgentMetadataFromMap(
                Collections.emptyMap());
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setUserAgentMetadata(
     * WebSettings, UserAgentMetadata)}.
     */
    @Override
    public void setUserAgentMetadata(@NonNull UserAgentMetadata uaMetadata) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getAttributionRegistrationBehavior(WebSettings)}
     */
    @Override
    public int getAttributionRegistrationBehavior() {
        return WebSettingsCompat.ATTRIBUTION_BEHAVIOR_APP_SOURCE_AND_WEB_TRIGGER;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setAttributionRegistrationBehavior(WebSettings, int)}
     */
    @Override
    public void setAttributionRegistrationBehavior(int behavior) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setWebViewMediaIntegrityApiStatus(WebSettings, WebViewMediaIntegrityApiStatusConfig)}
     */
    @Override
    public void setWebViewMediaIntegrityApiStatus(
            @NonNull WebViewMediaIntegrityApiStatusConfig permissionConfig) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getWebViewMediaIntegrityApiStatus(WebSettings)}
     */
    @Override
    public @NonNull WebViewMediaIntegrityApiStatusConfig getWebViewMediaIntegrityApiStatus() {
        return new WebViewMediaIntegrityApiStatusConfig
                .Builder(WebViewMediaIntegrityApiStatusConfig.WEBVIEW_MEDIA_INTEGRITY_API_ENABLED)
                .build();
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setWebAuthenticationSupport(WebSettings, int)}
     */
    @Override
    public void setWebAuthenticationSupport(int support) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getWebAuthenticationSupport(WebSettings)}
     */
    @Override
    public int getWebAuthenticationSupport() {
        return WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_NONE;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setSpeculativeLoadingStatus(WebSettings, int)}
     */
    @Override
    public void setSpeculativeLoadingStatus(int speculativeLoadingStatus) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getSpeculativeLoadingStatus(WebSettings)}
     */
    @WebSettingsCompat.ExperimentalSpeculativeLoading
    @Override
    public int getSpeculativeLoadingStatus() {
        return WebSettingsCompat.SPECULATIVE_LOADING_DISABLED;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setBackForwardCacheEnabled(WebSettings, boolean)}
     */
    @Override
    public void setBackForwardCacheEnabled(boolean backForwardCacheEnabled) {
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getBackForwardCacheEnabled(WebSettings)}
     */
    @Override
    public boolean getBackForwardCacheEnabled() {
        return false;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setPaymentRequestEnabled(WebSettings, boolean)}
     */
    @Override
    public void setPaymentRequestEnabled(boolean enabled) {}

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getPaymentRequestEnabled(WebSettings)}
     */
    @Override
    public boolean getPaymentRequestEnabled() {
        return false;
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setHasEnrolledInstrumentEnabled(WebSettings, boolean)}
     */
    @Override
    public void setHasEnrolledInstrumentEnabled(boolean enabled) {}

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getHasEnrolledInstrumentEnabled(WebSettings)}
     */
    @Override
    public boolean getHasEnrolledInstrumentEnabled() {
        return false;
    }

    @Override
    public void setCookieAccessForShouldInterceptRequestEnabled(boolean enabled) {
    }

    @Override
    public boolean getCookieAccessForShouldInterceptRequestEnabled() {
        return false;
    }
}
