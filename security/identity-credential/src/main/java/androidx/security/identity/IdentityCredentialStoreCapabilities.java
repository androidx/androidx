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

import android.icu.util.Calendar;

import androidx.annotation.NonNull;

import java.security.cert.X509Certificate;
import java.util.Set;

/* TODO: use this
 *
 *  {@link android.content.pm.PackageManager#FEATURE_IDENTITY_CREDENTIAL_HARDWARE}
 *  {@link android.content.pm.PackageManager#FEATURE_IDENTITY_CREDENTIAL_HARDWARE_DIRECT_ACCESS}
 *
 * when building against the Android 12 SDK.
 */

/**
 * A class that supports querying the capabilities of a {@link IdentityCredentialStore} as
 * implemented in secure hardware or in software (backed by Android Keystore).
 *
 * <p>Capabilities depend on the Android system features and can be queried using
 * {@link android.content.pm.PackageManager#getSystemAvailableFeatures()} and
 * {@link android.content.pm.PackageManager#hasSystemFeature(String, int)}.
 * The feature names in question are <em>android.hardware.identity_credential and</em>
 * <em>android.hardware.identity_credential_direct_access</em> for the direct access store.
 *
 * <p>Known feature versions include {@link #FEATURE_VERSION_202009} and
 * {@link #FEATURE_VERSION_202101}.
 */
public class IdentityCredentialStoreCapabilities {
    IdentityCredentialStoreCapabilities() {}

    /**
     * The feature version corresponding to features included in the Identity Credential API
     * shipped in Android 11.
     */
    public static final int FEATURE_VERSION_202009 = 202009;

    /**
     * The feature version corresponding to features included in the Identity Credential API
     * shipped in Android 12. This feature version adds support for
     * {@link IdentityCredential#delete(byte[])},
     * {@link IdentityCredential#update(PersonalizationData)},
     * {@link IdentityCredential#proveOwnership(byte[])}, and
     * {@link IdentityCredential#storeStaticAuthenticationData(X509Certificate, Calendar, byte[])}.
     */
    public static final int FEATURE_VERSION_202101 = 202101;

    /**
     * Returns the feature version of the {@link IdentityCredentialStore}.
     *
     * @return the feature version.
     */
    public int getFeatureVersion() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether the credential store is for direct access.
     *
     * <p>This always return {@code false} for the software-based store.
     *
     * @return {@code true} if credential store is for direct access, {@code false} if not.
     */
    public boolean isDirectAccess() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether the credential is backed by Secure Hardware.
     *
     * <p>This always return {@code false} for the software-based store.
     *
     * <p>Note that the software-based store is still using Android Keystore which
     * itself is backed by secure hardware.
     *
     * @return {@code true} if backed by secure hardware, {@code false} if not.
     */
    public boolean isHardwareBacked() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets a set of supported document types.
     *
     * <p>Only the direct-access store may restrict the kind of document types that can be used for
     * credentials. The default store always supports any document type.
     *
     * <p>This always return the empty set for the software-based store.
     *
     * @return The supported document types or the empty set if any document type is supported.
     */
    public @NonNull
    Set<String> getSupportedDocTypes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether {@link IdentityCredential#delete(byte[])} is supported
     * by the underlying hardware.
     *
     * <p>This is supported in feature version {@link #FEATURE_VERSION_202101} and later.
     *
     * <p>This is always supported by the software-based store.
     *
     * @return {@code true} if supported, {@code false} if not.
     */
    public boolean isDeleteSupported() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if {@link IdentityCredential#update(PersonalizationData)} is supported
     * by the underlying hardware.
     *
     * <p>This is supported in feature version {@link #FEATURE_VERSION_202101} and later.
     *
     * <p>This is always supported by the software-based store.
     *
     * @return {@code true} if supported, {@code false} if not.
     */
    public boolean isUpdateSupported() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if {@link IdentityCredential#proveOwnership(byte[])} is supported by the
     * underlying hardware.
     *
     * <p>This is supported in feature version {@link #FEATURE_VERSION_202101} and later.
     *
     * <p>This is always supported by the software-based store.
     *
     * @return {@code true} if supported, {@code false} if not.
     */
    public boolean isProveOwnershipSupported() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if
     * {@link IdentityCredential#storeStaticAuthenticationData(X509Certificate, Calendar, byte[])}
     * is supported by the underlying hardware.
     *
     * <p>This is supported in feature version {@link #FEATURE_VERSION_202101} and later.
     *
     * <p>This is always supported by the software-based store.
     *
     * @return {@code true} if supported, {@code false} if not.
     */
    public boolean isStaticAuthenticationDataExpirationSupported() {
        throw new UnsupportedOperationException();
    }

}
