/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.security.identity;

import androidx.annotation.NonNull;

import java.util.Set;

class SimpleIdentityCredentialStoreCapabilities extends IdentityCredentialStoreCapabilities {

    boolean mIsDirectAccess;
    int mFeatureVersion;
    boolean mIsHardwareBacked;
    Set<String> mSupportedDocTypes;
    boolean mIsDeleteCredentialSupported;
    boolean mIsUpdateCredentialSupported;
    boolean mIsProveOwnershipSupported;
    boolean mIsStaticAuthenticationDataExpirationDateSupported;

    SimpleIdentityCredentialStoreCapabilities(boolean isDirectAccess,
            int featureVersion,
            boolean isHardwareBacked,
            Set<String> supportedDocTypes,
            boolean isDeleteCredentialSupported,
            boolean isUpdateCredentialSupported,
            boolean isProveOwnershipSupported,
            boolean isStaticAuthenticationDataExpirationDateSupported) {
        mIsDirectAccess = isDirectAccess;
        mFeatureVersion = featureVersion;
        mIsHardwareBacked = isHardwareBacked;
        mSupportedDocTypes = supportedDocTypes;
        mIsDeleteCredentialSupported = isDeleteCredentialSupported;
        mIsProveOwnershipSupported = isProveOwnershipSupported;
        mIsUpdateCredentialSupported = isUpdateCredentialSupported;
        mIsStaticAuthenticationDataExpirationDateSupported =
                isStaticAuthenticationDataExpirationDateSupported;
    }

    static SimpleIdentityCredentialStoreCapabilities getFeatureVersion202009(
            boolean isDirectAccess,
            boolean isHardwareBacked,
            Set<String> supportedDocTypesSet) {
        return new SimpleIdentityCredentialStoreCapabilities(
                isDirectAccess,
                IdentityCredentialStoreCapabilities.FEATURE_VERSION_202009,
                isHardwareBacked,
                supportedDocTypesSet,
                false,
                false,
                false,
                false);
    }

    static SimpleIdentityCredentialStoreCapabilities getFeatureVersion202101(
            boolean isDirectAccess,
            boolean isHardwareBacked,
            Set<String> supportedDocTypesSet) {
        return new SimpleIdentityCredentialStoreCapabilities(
                isDirectAccess,
                IdentityCredentialStoreCapabilities.FEATURE_VERSION_202101,
                isHardwareBacked,
                supportedDocTypesSet,
                true,
                true,
                true,
                true);
    }

    @Override
    public boolean isDirectAccess() {
        return mIsDirectAccess;
    }

    @Override
    public int getFeatureVersion() {
        return mFeatureVersion;
    }

    @Override
    public boolean isHardwareBacked() {
        return mIsHardwareBacked;
    }

    @Override
    public @NonNull Set<String> getSupportedDocTypes() {
        return mSupportedDocTypes;
    }

    @Override
    public boolean isDeleteSupported() {
        return mIsDeleteCredentialSupported;
    }

    @Override
    public boolean isUpdateSupported() {
        return mIsUpdateCredentialSupported;
    }

    @Override
    public boolean isProveOwnershipSupported() {
        return mIsProveOwnershipSupported;
    }

    @Override
    public boolean isStaticAuthenticationDataExpirationSupported() {
        return mIsStaticAuthenticationDataExpirationDateSupported;
    }

}
