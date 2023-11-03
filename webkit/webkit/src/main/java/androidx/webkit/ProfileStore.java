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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.UiThread;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.ProfileStoreImpl;
import androidx.webkit.internal.WebViewFeatureInternal;

import java.util.List;

/**
 * Manages any creation, deletion for {@link Profile}.
 *
 * <p>Example usage:
 * <pre class="prettyprint">
 *    ProfileStore profileStore = ProfileStore.getInstance();
 *
 *    // Use this store instance to manage Profiles.
 *    Profile createdProfile = profileStore.getOrCreateProfile("test_profile");
 *    createdProfile.getGeolocationPermissions().clear("example");
 *    //...
 *    profileStore.deleteProfile("profile_test");
 *
 * </pre>
 */
@UiThread
public interface ProfileStore {

    /**
     * Returns the production instance of ProfileStore.
     *
     * @return ProfileStore instance to use for managing profiles.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    static ProfileStore getInstance() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return ProfileStoreImpl.getInstance();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns a profile with the given name, creating if needed.
     * <p>
     * Returns the associated Profile with this name, if there's no match with this name it
     * will create a new Profile instance.
     *
     * @param name name of the profile to retrieve.
     * @return instance of {@link Profile} matching this name.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    Profile getOrCreateProfile(@NonNull String name);

    /**
     * Returns a profile with the given name, if it exists.
     * <p>
     * Returns the associated Profile with this name, if there's no Profile with this name or the
     * Profile was deleted by {@link ProfileStore#deleteProfile(String)} it will return null.
     *
     * @param name the name of the profile to retrieve.
     * @return instance of {@link Profile} matching this name, null otherwise if there's no match.
     */
    @Nullable
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    Profile getProfile(@NonNull String name);

    /**
     * Returns the names of all available profiles.
     * <p>
     * Default profile name will be included in this list.
     *
     * @return profile names as a list.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    List<String> getAllProfileNames();

    /**
     * Deletes the profile data associated with the name.
     * <p>
     * If this method returns true, the {@link Profile} object associated with the name will no
     * longer be usable by the application. Returning false means that this profile doesn't exist.
     * <p>
     * Some data may be deleted async and is not guaranteed to be cleared from disk by the time
     * this method returns.
     *
     * @param name the profile name to be deleted.
     * @return {@code true} if profile exists and its data is to be deleted, otherwise {@code
     * false}.
     * @throws IllegalStateException if there are living WebViews associated with that profile.
     * @throws IllegalStateException if you are trying to delete a Profile that was loaded in the
     * memory using {@link ProfileStore#getOrCreateProfile(String)}} or
     * {@link ProfileStore#getProfile(String)}.
     * @throws IllegalArgumentException if you are trying to delete the default Profile.
     */
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    boolean deleteProfile(@NonNull String name);
}
