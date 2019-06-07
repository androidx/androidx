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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.R)
class HardwareIdentityCredentialStore extends IdentityCredentialStore {

    private static final String TAG = "HardwareIdentityCredentialStore";

    private android.security.identity.IdentityCredentialStore mStore = null;

    private HardwareIdentityCredentialStore(
            @NonNull android.security.identity.IdentityCredentialStore store) {
        mStore = store;
    }

    static @Nullable IdentityCredentialStore getInstanceIfSupported(@NonNull Context context) {
        android.security.identity.IdentityCredentialStore store =
                android.security.identity.IdentityCredentialStore.getInstance(context);
        if (store != null) {
            return new HardwareIdentityCredentialStore(store);
        }
        return null;
    }

    public static @NonNull IdentityCredentialStore getInstance(@NonNull Context context) {
        IdentityCredentialStore instance = getInstanceIfSupported(context);
        if (instance != null) {
            return instance;
        }
        throw new RuntimeException("HW-backed IdentityCredential not supported");
    }

    static @Nullable IdentityCredentialStore getDirectAccessInstanceIfSupported(
            @NonNull Context context) {
        android.security.identity.IdentityCredentialStore store =
                android.security.identity.IdentityCredentialStore.getDirectAccessInstance(context);
        if (store != null) {
            return new HardwareIdentityCredentialStore(store);
        }
        return null;
    }

    public static @NonNull IdentityCredentialStore getDirectAccessInstance(
            @NonNull Context context) {
        IdentityCredentialStore instance = getDirectAccessInstanceIfSupported(context);
        if (instance != null) {
            return instance;
        }
        throw new RuntimeException("HW-backed direct-access IdentityCredential not supported");
    }

    public static boolean isDirectAccessSupported(@NonNull Context context) {
        IdentityCredentialStore directAccessStore = getDirectAccessInstanceIfSupported(context);
        return directAccessStore != null;
    }

    @Override
    public @NonNull String[] getSupportedDocTypes() {
        return mStore.getSupportedDocTypes();
    }

    @Override
    public @NonNull WritableIdentityCredential createCredential(
            @NonNull String credentialName,
            @NonNull String docType) throws AlreadyPersonalizedException,
            DocTypeNotSupportedException {
        try {
            android.security.identity.WritableIdentityCredential writableCredential =
                    mStore.createCredential(credentialName, docType);
            return new HardwareWritableIdentityCredential(writableCredential);
        } catch (android.security.identity.AlreadyPersonalizedException e) {
            throw new AlreadyPersonalizedException(e.getMessage(), e);
        } catch (android.security.identity.DocTypeNotSupportedException e) {
            throw new DocTypeNotSupportedException(e.getMessage(), e);
        }
    }

    @Override
    public @Nullable IdentityCredential getCredentialByName(
            @NonNull String credentialName,
            @Ciphersuite int cipherSuite) throws CipherSuiteNotSupportedException {
        try {
            android.security.identity.IdentityCredential credential =
                    mStore.getCredentialByName(credentialName, cipherSuite);
            if (credential == null) {
                return null;
            }
            return new HardwareIdentityCredential(credential);
        } catch (android.security.identity.CipherSuiteNotSupportedException e) {
            throw new CipherSuiteNotSupportedException(e.getMessage(), e);
        }
    }

    @Override
    public @Nullable byte[] deleteCredentialByName(@NonNull String credentialName) {
        return mStore.deleteCredentialByName(credentialName);
    }

}
