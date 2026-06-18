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

import androidx.annotation.UiThread;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;

import org.chromium.support_lib_boundary.ProfileBoundaryInterface;
import org.chromium.support_lib_boundary.ProfileStoreBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal implementation of ProfileStore.
 */
public class ProfileStoreImpl implements ProfileStore {

    private final ProfileStoreBoundaryInterface mProfileStoreImpl;
    private static ProfileStoreImpl sInstance;

    // Note: We had originally considered using a WeakHashMap here, however that would break
    // equivalence for the objects that hang off Profile. For example:
    // 1. HttpCache cache1 = ProfileStore.getProfile(PROFILE_NAME).getHttpCache();
    // 2. Maybe run GC, possibly discarding the unreferenced Profile.
    // 3. HttpCache cache2 = ProfileStore.getProfile(PROFILE_NAME).getHttpCache();
    // 4. cache1 != cache2, as the Profile object was not the same
    private final Map<@NonNull String, @NonNull Profile> mProfileCache = new HashMap<>();

    private ProfileStoreImpl(ProfileStoreBoundaryInterface profileStoreImpl) {
        mProfileStoreImpl = profileStoreImpl;
    }

    /**
     * Returns the production instance of ProfileStore.
     *
     * @return ProfileStore instance to use for managing profiles.
     */
    @UiThread
    public static @NonNull ProfileStoreImpl getInstance() {
        if (sInstance == null) {
            sInstance = new ProfileStoreImpl(
                    WebViewGlueCommunicator.getFactory().getProfileStore());
        }
        return sInstance;
    }

    @Override
    public @NonNull Profile getOrCreateProfile(@NonNull String name) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }

        return mProfileCache.computeIfAbsent(name, key -> new ProfileImpl(
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        ProfileBoundaryInterface.class,
                        mProfileStoreImpl.getOrCreateProfile(name))));
    }

    @Override
    public @Nullable Profile getProfile(@NonNull String name) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }

        return mProfileCache.computeIfAbsent(name, key -> {
            InvocationHandler profileBoundaryInterface = mProfileStoreImpl.getProfile(name);
            if (profileBoundaryInterface == null) {
                return null;
            }
            return new ProfileImpl(
                    BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                            ProfileBoundaryInterface.class, profileBoundaryInterface));
        });
    }

    @Override
    public @NonNull List<String> getAllProfileNames() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileStoreImpl.getAllProfileNames();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @NonNull Profile getOrCreateProfileFromBoundaryInterface(
            @NonNull InvocationHandler profileInvocationHandler) {
        ProfileBoundaryInterface profile = BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                ProfileBoundaryInterface.class, profileInvocationHandler);

        if (profile == null) {
            throw new IllegalStateException("WebView returned the name of a non-existent Profile.");
        }

        return mProfileCache.computeIfAbsent(profile.getName(), key -> new ProfileImpl(profile));
    }

    @Override
    public boolean deleteProfile(@NonNull String name) throws IllegalStateException {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return mProfileStoreImpl.deleteProfile(name);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

}
