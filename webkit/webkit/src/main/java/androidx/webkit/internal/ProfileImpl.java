/*
 * Copyright 2023 The Android Open Source Project
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

import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerController;
import android.webkit.WebStorage;

import androidx.annotation.NonNull;
import androidx.webkit.Profile;

import org.chromium.support_lib_boundary.ProfileBoundaryInterface;

/**
 * Internal implementation of Profile.
 */
public class ProfileImpl implements Profile {

    private final ProfileBoundaryInterface mProfileImpl;

    ProfileImpl(ProfileBoundaryInterface profileImpl) {
        mProfileImpl = profileImpl;
    }

    // Use ProfileStore to create a Profile instance.
    private ProfileImpl() {
        mProfileImpl = null;
    }

    @Override
    @NonNull
    public String getName() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getName();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @NonNull
    public CookieManager getCookieManager() throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getCookieManager();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @NonNull
    public WebStorage getWebStorage() throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getWebStorage();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @NonNull
    @Override
    public GeolocationPermissions getGeolocationPermissions() throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getGeoLocationPermissions();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @NonNull
    @Override
    public ServiceWorkerController getServiceWorkerController() throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileImpl.getServiceWorkerController();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

}
