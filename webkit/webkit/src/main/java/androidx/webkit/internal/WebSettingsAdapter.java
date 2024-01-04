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

import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.webkit.UserAgentMetadata;

import org.chromium.support_lib_boundary.WebSettingsBoundaryInterface;

import java.util.Set;

/**
 * Adapter between WebSettingsCompat and
 * {@link org.chromium.support_lib_boundary.WebSettingsBoundaryInterface} (the
 * corresponding interface shared with the support library glue in the WebView APK).
 */
public class WebSettingsAdapter {
    private final WebSettingsBoundaryInterface mBoundaryInterface;

    public WebSettingsAdapter(@NonNull WebSettingsBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setOffscreenPreRaster}.
     */
    public void setOffscreenPreRaster(boolean enabled) {
        mBoundaryInterface.setOffscreenPreRaster(enabled);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getOffscreenPreRaster}.
     */
    public boolean getOffscreenPreRaster() {
        return mBoundaryInterface.getOffscreenPreRaster();
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setSafeBrowsingEnabled}.
     */
    public void setSafeBrowsingEnabled(boolean enabled) {
        mBoundaryInterface.setSafeBrowsingEnabled(enabled);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getSafeBrowsingEnabled}.
     */
    public boolean getSafeBrowsingEnabled() {
        return mBoundaryInterface.getSafeBrowsingEnabled();
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setDisabledActionModeMenuItems}.
     */
    public void setDisabledActionModeMenuItems(int menuItems) {
        mBoundaryInterface.setDisabledActionModeMenuItems(menuItems);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getDisabledActionModeMenuItems}.
     */
    public int getDisabledActionModeMenuItems() {
        return mBoundaryInterface.getDisabledActionModeMenuItems();
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setForceDark}.
     */
    public void setForceDark(int forceDarkMode) {
        mBoundaryInterface.setForceDark(forceDarkMode);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getForceDark}.
     */
    public int getForceDark() {
        return mBoundaryInterface.getForceDark();
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setForceDarkStrategy}.
     */
    public void setForceDarkStrategy(int forceDarkStrategy) {
        mBoundaryInterface.setForceDarkBehavior(forceDarkStrategy);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#getForceDarkStrategy}.
     */
    public int getForceDarkStrategy() {
        return mBoundaryInterface.getForceDarkBehavior();
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#setAlgorithmicDarkeningAllowed}.
     */
    public void setAlgorithmicDarkeningAllowed(boolean allow) {
        mBoundaryInterface.setAlgorithmicDarkeningAllowed(allow);
    }

    /**
     * Adapter method for {@link androidx.webkit.WebSettingsCompat#isAlgorithmicDarkeningAllowed}.
     */
    public boolean isAlgorithmicDarkeningAllowed() {
        return mBoundaryInterface.isAlgorithmicDarkeningAllowed();
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setEnterpriseAuthenticationAppLinkPolicyEnabled}.
     */
    public void setEnterpriseAuthenticationAppLinkPolicyEnabled(boolean enabled) {
        mBoundaryInterface.setEnterpriseAuthenticationAppLinkPolicyEnabled(enabled);
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getEnterpriseAuthenticationAppLinkPolicyEnabled}.
     */
    public boolean getEnterpriseAuthenticationAppLinkPolicyEnabled() {
        return mBoundaryInterface.getEnterpriseAuthenticationAppLinkPolicyEnabled();
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getRequestedWithHeaderOriginAllowList(WebSettings)}.
     */
    @NonNull
    public Set<String> getRequestedWithHeaderOriginAllowList() {
        return mBoundaryInterface.getRequestedWithHeaderOriginAllowList();
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setRequestedWithHeaderOriginAllowList(
     * WebSettings, Set)}.
     */
    public void setRequestedWithHeaderOriginAllowList(@NonNull Set<String> allowList) {
        mBoundaryInterface.setRequestedWithHeaderOriginAllowList(allowList);
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#getUserAgentMetadata(WebSettings)}.
     */
    @NonNull
    public UserAgentMetadata getUserAgentMetadata() {
        return UserAgentMetadataInternal.getUserAgentMetadataFromMap(
                mBoundaryInterface.getUserAgentMetadataMap());
    }

    /**
     * Adapter method for
     * {@link androidx.webkit.WebSettingsCompat#setUserAgentMetadata(
     * WebSettings, UserAgentMetadata)}.
     */
    public void setUserAgentMetadata(@NonNull UserAgentMetadata uaMetadata) {
        mBoundaryInterface.setUserAgentMetadataFromMap(
                UserAgentMetadataInternal.convertUserAgentMetadataToMap(uaMetadata));
    }
}
