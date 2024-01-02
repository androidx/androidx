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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;

import org.chromium.support_lib_boundary.ProfileBoundaryInterface;
import org.chromium.support_lib_boundary.ProfileStoreBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.util.List;

/**
 * Internal implementation of ProfileStore.
 */
public class ProfileStoreImpl implements ProfileStore {

    private final ProfileStoreBoundaryInterface mProfileStoreImpl;
    private static ProfileStore sInstance;

    private ProfileStoreImpl(ProfileStoreBoundaryInterface profileStoreImpl) {
        mProfileStoreImpl = profileStoreImpl;
    }

    private ProfileStoreImpl() {
        mProfileStoreImpl = null;
    }

    /**
     * Returns the production instance of ProfileStore.
     *
     * @return ProfileStore instance to use for managing profiles.
     */
    @NonNull
    public static ProfileStore getInstance() {
        if (sInstance == null) {
            sInstance = new ProfileStoreImpl(
                    WebViewGlueCommunicator.getFactory().getProfileStore());
        }
        return sInstance;
    }

    @Override
    @NonNull
    public Profile getOrCreateProfile(@NonNull String name) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            return new ProfileImpl(BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                    ProfileBoundaryInterface.class, mProfileStoreImpl.getOrCreateProfile(name)));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @Nullable
    public Profile getProfile(@NonNull String name) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.MULTI_PROFILE;
        if (feature.isSupportedByWebView()) {
            InvocationHandler profileBoundaryInterface = mProfileStoreImpl.getProfile(name);
            if (profileBoundaryInterface != null) {
                return new ProfileImpl(BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        ProfileBoundaryInterface.class, profileBoundaryInterface));
            } else {
                return null;
            }
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    @Override
    @NonNull
    public List<String> getAllProfileNames() {
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
