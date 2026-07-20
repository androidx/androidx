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

import org.chromium.support_lib_boundary.ProfileStoreBoundaryInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.List;

/**
 * Internal implementation of ProfileStore.
 */
public class ProfileStoreImpl implements ProfileStore {

    private final ProfileStoreBoundaryInterface mProfileStoreImpl;
    private static ProfileStoreImpl sInstance;

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

        return ProfileImpl.forInvocationHandler(
                mProfileStoreImpl.getOrCreateProfile(name));
    }

    @Override
    public @Nullable Profile getProfile(@NonNull String name) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (!feature.isSupportedByWebView()) {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }

        InvocationHandler invocationHandler = mProfileStoreImpl.getProfile(name);
        if (invocationHandler == null) {
            return null;
        }
        return ProfileImpl.forInvocationHandler(invocationHandler);
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
