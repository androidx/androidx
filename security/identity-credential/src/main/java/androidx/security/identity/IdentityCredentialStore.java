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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.cert.X509Certificate;

/**
 * An interface to a secure store for user identity documents.
 *
 * <p>This interface is deliberately fairly general and abstract.  To the extent possible,
 * specification of the message formats and semantics of communication with credential
 * verification devices and issuing authorities (IAs) is out of scope. It provides the
 * interface with secure storage but a credential-specific Android application will be
 * required to implement the presentation and verification protocols and processes
 * appropriate for the specific credential type.
 *
 * <p>Multiple credentials can be created.  Each credential comprises:</p>
 * <ul>
 * <li>A document type, which is a string.</li>
 *
 * <li>A set of namespaces, which serve to disambiguate value names. It is recommended
 * that namespaces be structured as reverse domain names so that IANA effectively serves
 * as the namespace registrar.</li>
 *
 * <li>For each namespace, a set of name/value pairs, each with an associated set of
 * access control profile IDs.  Names are strings and values are typed and can be any
 * value supported by <a href="http://cbor.io/">CBOR</a>.</li>
 *
 * <li>A set of access control profiles, each with a profile ID and a specification
 * of the conditions which satisfy the profile's requirements.</li>
 *
 * <li>An asymmetric key pair which is used to authenticate the credential to the Issuing
 * Authority, called the <em>CredentialKey</em>.</li>
 *
 * <li>A set of zero or more named reader authentication public keys, which are used to
 * authenticate an authorized reader to the credential.</li>
 *
 * <li>A set of named signing keys, which are used to sign collections of values and session
 * transcripts.</li>
 * </ul>
 *
 * <p>Implementing support for user identity documents in secure storage requires dedicated
 * hardware-backed support and may not always be available. In addition to hardware-backed
 * Identity Credential support (which is only available in Android 11 and later and only
 * if the device has support for the <a href="https://android.googlesource.com/platform/hardware/interfaces/+/refs/heads/master/identity/aidl/android/hardware/identity/IIdentityCredentialStore.aidl">Identity Credential HAL</a>),
 * this Jetpack has an Android Keystore backed implementation (also known as the "software"
 * implementation) which works on any Android device with API level 24 or later.
 *
 * <p>The Identity Credential API is designed to be able to evolve and change over time
 * but still provide 100% backwards compatibility. This is complicated by the fact that
 * there may be a version skew between the API used by the application and the version
 * implemented in secure hardware. To solve this problem, the API provides for a way
 * for the application to query for hardware capabilities through
 * {@link IdentityCredentialStoreCapabilities}. The software-based store is designed
 * so it implements all capabilities that don't explicitly require hardware features. Each
 * of the methods in that class will state whether it's implemented in the software-based
 * implementation.
 *
 * <p>When provisioning a document, applications should use {@link #getInstance(Context)} to
 * obtain an {@link IdentityCredentialStore} instance. This method returns a hardware-backed
 * store if available and a software-based store if not and the application should use
 * {@link IdentityCredentialStoreCapabilities} to examine if the store supports the
 * capabilities required by the application. In the negative, the application can
 * obtain the software-based store by calling {@link #getSoftwareInstance(Context)}.
 *
 * <p>Since it's possible for an OS upgrade on a device to include an updated version of the
 * drivers used for secure hardware, it's possible that {@link #getInstance(Context)} returns the
 * software implementation at one point and the hardware implementation at a later point.
 * Therefore, applications should only use {@link #getInstance(Context)} only when creating a
 * credential, record whether it's hardware- or software-backed (using
 * {@link IdentityCredentialStoreCapabilities#isHardwareBacked()}, and then use this information
 * when retrieving the credential (using either {@link #getSoftwareInstance(Context)} or
 * {@link #getHardwareInstance(Context)}).
 *
 * <p>Apart from hardware- vs software-backed, two different flavors of credential stores exist -
 * the <em>default</em> store and the <em>direct access</em> store. Most often credentials will
 * be accessed through the default store but that requires that the Android device be powered up
 * and fully functional. It is desirable to allow identity credential usage when the Android
 * device's battery is too low to boot the Android operating system, so direct access to the
 * secure hardware via NFC may allow data retrieval, if the secure hardware chooses to implement it.
 *
 * <p>Credentials provisioned to the direct access store should <strong>always</strong> use reader
 * authentication to protect data elements. The reason for this is user authentication or user
 * approval of data release is not possible when the device is off.
 */
public abstract class IdentityCredentialStore {
    IdentityCredentialStore() {}

    /**
     * Specifies that the cipher suite that will be used to secure communications between the
     * reader and the prover is using the following primitives
     *
     * <ul>
     * <li>ECKA-DH (Elliptic Curve Key Agreement Algorithm - Diffie-Hellman, see BSI TR-03111).</li>
     * <li>HKDF-SHA-256 (see RFC 5869).</li>
     * <li>AES-256-GCM (see NIST SP 800-38D).</li>
     * <li>HMAC-SHA-256 (see RFC 2104).</li>
     * </ul>
     *
     * <p>The exact way these primitives are combined to derive the session key
     * is specified in section 9.2.1.4 of ISO/IEC 18013-5 (see description of cipher
     * suite '1').</p>
     *
     * <p>At present this is the only supported cipher suite.</p>
     */
    public static final int CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256 = 1;

    /**
     * Gets the default {@link IdentityCredentialStore}.
     *
     * @param context the application context.
     * @return the {@link IdentityCredentialStore}.
     */
    public static @NonNull IdentityCredentialStore getInstance(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            IdentityCredentialStore store =
                    HardwareIdentityCredentialStore.getInstanceIfSupported(appContext);
            if (store != null) {
                return store;
            }
        }
        return SoftwareIdentityCredentialStore.getInstance(appContext);
    }

    /**
     * Gets the {@link IdentityCredentialStore} for direct access.
     *
     * This should only be called if {@link #isDirectAccessSupported(Context)} returns {@code true}.
     *
     * @param context the application context.
     * @return the {@link IdentityCredentialStore} or {@code null} if direct access is not
     *     supported on this device.
     */
    public static @NonNull IdentityCredentialStore getDirectAccessInstance(@NonNull
            Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Context appContext = context.getApplicationContext();
            IdentityCredentialStore store = HardwareIdentityCredentialStore.getDirectAccessInstance(
                    appContext);
            if (store != null) {
                return store;
            }
        }
        throw new RuntimeException("Direct-access IdentityCredential is not supported");
    }

    /**
     * Checks if direct-access is supported.
     *
     * <p>Direct access requires specialized NFC hardware and may not be supported on all
     * devices even if default store is available.</p>
     *
     * <p>Because Android is not running when direct-access credentials are presented, there is
     * no way for the user to consent to release of credential data. Therefore, credentials
     * provisioned to the direct access store should <strong>always</strong> use reader
     * authentication to protect data elements such that only readers authorized by the issuer
     * can access them. The
     * {@link AccessControlProfile.Builder#setReaderCertificate(X509Certificate)}
     * method can be used at provisioning time to set which reader (or group of readers) are
     * authorized to access data elements.</p>
     *
     * @param context the application context.
     * @return {@code true} if direct-access is supported.
     */
    public static boolean isDirectAccessSupported(@NonNull Context context) {
        // SoftwareIdentityCredentialStore will never support direct-access.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Context appContext = context.getApplicationContext();
            return HardwareIdentityCredentialStore.isDirectAccessSupported(appContext);
        }
        return false;
    }

    /**
     * Gets a list of supported document types.
     *
     * <p>Only the direct-access store may restrict the kind of document types that can be used for
     * credentials. The default store always supports any document type.
     *
     * @return The supported document types or the empty array if any document type is supported.
     * @deprecated Use {@link IdentityCredentialStoreCapabilities#getSupportedDocTypes()} instead.
     */
    @Deprecated
    public abstract @NonNull String[] getSupportedDocTypes();

    /**
     * Creates a new credential.
     *
     * @param credentialName The name used to identify the credential.
     * @param docType        The document type for the credential.
     * @return A @{link WritableIdentityCredential} that can be used to create a new credential.
     * @throws AlreadyPersonalizedException if a credential with the given name already exists.
     * @throws DocTypeNotSupportedException if the given document type isn't supported by the store.
     */
    public abstract @NonNull WritableIdentityCredential createCredential(
            @NonNull String credentialName, @NonNull String docType)
            throws AlreadyPersonalizedException, DocTypeNotSupportedException;

    /**
     * Retrieve a named credential.
     *
     * @param credentialName the name of the credential to retrieve.
     * @param cipherSuite    the cipher suite to use for communicating with the verifier.
     * @return The named credential, or null if not found.
     */
    public abstract @Nullable IdentityCredential getCredentialByName(@NonNull String credentialName,
            @Ciphersuite int cipherSuite)
            throws CipherSuiteNotSupportedException;

    /**
     * Delete a named credential.
     *
     * <p>This method returns a COSE_Sign1 data structure signed by the CredentialKey
     * with payload set to {@code ProofOfDeletion} as defined below:
     *
     * <pre>
     *     ProofOfDeletion = [
     *          "ProofOfDeletion",            ; tstr
     *          tstr,                         ; DocType
     *          bool                          ; true if this is a test credential, should
     *                                        ; always be false.
     *      ]
     * </pre>
     *
     * @param credentialName the name of the credential to delete.
     * @return {@code null} if the credential was not found, the COSE_Sign1 data structure above
     *     if the credential was found and deleted.
     * @deprecated Use {@link IdentityCredential#delete(byte[])} instead.
     */
    @Deprecated
    public abstract @Nullable byte[] deleteCredentialByName(@NonNull String credentialName);

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256})
    public @interface Ciphersuite {
    }

    /**
     * Returns the capabilities of the store.
     *
     * @return the capabilities of the store
     */
    @NonNull
    public IdentityCredentialStoreCapabilities getCapabilities() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets a {@link IdentityCredentialStore} implemented via Android Keystore. This
     * is also known as the software implementation.
     *
     * @return an implementation of {@link IdentityCredentialStore} implemented on top
     * of Android Keystore.
     */
    @SuppressWarnings("deprecation")
    public static @NonNull IdentityCredentialStore getSoftwareInstance(@NonNull
            Context context) {
        return SoftwareIdentityCredentialStore.getInstance(context);
    }

    /**
     * Gets a {@link IdentityCredentialStore} implemented via secure hardware using
     * the
     * <a href="https://android.googlesource.com/platform/hardware/interfaces/+/refs/heads/master/identity/aidl/android/hardware/identity/IIdentityCredentialStore.aidl">Identity Credential HAL</a>.
     *
     * <p>This only works on devices running Android 11 or later and only if the device has
     * support for the Identity Credential HAL.
     *
     * @return an implementation of {@link IdentityCredentialStore} implemented in
     * secure hardware or {@code null} if the device doesn't support the Android Identity
     * Credential HAL.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable IdentityCredentialStore getHardwareInstance(@NonNull
            Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return HardwareIdentityCredentialStore.getInstance(context);
        }
        return null;
    }
}
