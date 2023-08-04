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

package androidx.webkit;

import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerController;
import android.webkit.WebStorage;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

/**
 * A Profile represents one browsing session for WebView.
 * <p> You can have multiple profiles and each profile holds its own set of data. The creation
 * and deletion of the Profile is being managed by {@link ProfileStore}.
 *
 * @hide
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Profile {

    /**
     * Represents the name of the default profile which can't be deleted.
     */
    String DEFAULT_PROFILE_NAME = "Default";

    /**
     * @return the name of this Profile which was used to create the Profile from
     * ProfileStore create methods.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    String getName();

    /**
     * Returns the profile's cookie manager.
     * <p>
     * Can be called from any thread. It is recommended to not hold onto references of this.
     *
     * @throws IllegalStateException if the profile has been deleted by
     * {@link ProfileStore#deleteProfile(String)}}.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    CookieManager getCookieManager() throws IllegalStateException;

    /**
     * Returns the profile's web storage.
     * <p>
     * Can be called from any thread. It is recommended to not hold onto references of this.
     *
     * @throws IllegalStateException if the profile has been deleted by
     * {@link ProfileStore#deleteProfile(String)}}.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    WebStorage getWebStorage() throws IllegalStateException;

    /**
     * Returns the geolocation permissions of the profile.
     * <p>
     * Can be called from any thread. It is recommended to not hold onto references of this.
     *
     * @throws IllegalStateException if the profile has been deleted by
     * {@link ProfileStore#deleteProfile(String)}}.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    GeolocationPermissions getGeolocationPermissions() throws IllegalStateException;

    /**
     * Returns the service worker controller of the profile.
     * <p>
     * Can be called from any thread. It is recommended to not hold onto references of this.
     *
     * @throws IllegalStateException if the profile has been deleted by
     * {@link ProfileStore#deleteProfile(String)}}.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    ServiceWorkerController getServiceWorkerController() throws IllegalStateException;

}
