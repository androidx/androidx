/*
 * Copyright 2019 The Android Open Source Project
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


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class SoftwareIdentityCredentialStore extends IdentityCredentialStore {

    private static final String TAG = "SoftwareIdentityCredentialStore";

    private Context mContext = null;

    private SoftwareIdentityCredentialStore(@NonNull Context context) {
        mContext = context;
    }

    public static @NonNull IdentityCredentialStore getInstance(@NonNull Context context) {
        return new SoftwareIdentityCredentialStore(context);
    }

    public static @NonNull IdentityCredentialStore getDirectAccessInstance(@NonNull
            Context context) {
        throw new RuntimeException("Direct-access IdentityCredential is not supported");
    }

    public static boolean isDirectAccessSupported(@NonNull Context context) {
        return false;
    }

    @Override
    public @NonNull String[] getSupportedDocTypes() {
        // Any document type is supported.
        return new String[0];
    }

    @Override
    public @NonNull WritableIdentityCredential createCredential(
            @NonNull String credentialName,
            @NonNull String docType) throws AlreadyPersonalizedException,
            DocTypeNotSupportedException {
        return new SoftwareWritableIdentityCredential(mContext, credentialName, docType);
    }

    @Override
    public @Nullable IdentityCredential getCredentialByName(
            @NonNull String credentialName,
            @Ciphersuite int cipherSuite) throws CipherSuiteNotSupportedException {
        SoftwareIdentityCredential credential =
                new SoftwareIdentityCredential(mContext, credentialName, cipherSuite);
        if (credential.loadData()) {
            return credential;
        }
        return null;
    }

    @Override
    public @Nullable byte[] deleteCredentialByName(@NonNull String credentialName) {
        return SoftwareIdentityCredential.delete(mContext, credentialName);
    }

}
