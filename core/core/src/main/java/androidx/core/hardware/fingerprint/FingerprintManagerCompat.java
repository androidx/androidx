/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package androidx.core.hardware.fingerprint;

import android.Manifest;
import android.content.Context;
import android.os.CancellationSignal;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;

import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * A class that coordinates access to the fingerprint hardware.
 * <p>
 * This class has been deprecated and should no longer be used. On all platform versions, it behaves
 * as though no fingerprint hardware is available.
 *
 * @deprecated {@code FingerprintManager} was removed from the platform SDK in Android V, use
 * {@code androidx.biometrics.BiometricPrompt} instead.
 */
@SuppressWarnings({"deprecation", "unused"})
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FingerprintManagerCompat {

    /** Get a {@link FingerprintManagerCompat} instance for a provided context. */
    @NonNull
    public static FingerprintManagerCompat from(@NonNull Context context) {
        return new FingerprintManagerCompat();
    }

    private FingerprintManagerCompat() {
    }

    /**
     * Prior to deprecation, this method would determine if there is at least one fingerprint
     * enrolled.
     *
     * @return false
     */
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public boolean hasEnrolledFingerprints() {
        return false;
    }

    /**
     * Prior to deprecation, this method would determine if fingerprint hardware is present and
     * functional.
     *
     * @return false
     */
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public boolean isHardwareDetected() {
        return false;
    }

    /**
     * Prior to deprecation, this method would request authentication of a crypto object.
     * <p>
     * This call warms up the fingerprint hardware and starts scanning for a fingerprint. It
     * terminates when {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)} or
     * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult)} is called, at
     * which point the object is no longer valid. The operation can be canceled by using the
     * provided cancel object.
     *
     * @param crypto object associated with the call or null if none required.
     * @param flags optional flags; should be 0
     * @param cancel an object that can be used to cancel authentication
     * @param callback an object to receive authentication events
     * @param handler an optional handler for events
     * @deprecated Use
     * {@link #authenticate(CryptoObject, int, CancellationSignal, AuthenticationCallback, Handler)}
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public void authenticate(@Nullable CryptoObject crypto, int flags,
            @Nullable androidx.core.os.CancellationSignal cancel,
            @NonNull AuthenticationCallback callback,
            @Nullable Handler handler) {
        // No-op.
    }

    /**
     * Prior to deprecation, this method would request authentication of a crypto object.
     * <p>
     * This call warms up the fingerprint hardware and starts scanning for a fingerprint.
     * It terminates when {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)} or
     * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult)} is called, at
     * which point the object is no longer valid. The operation can be canceled by using the
     * provided cancel object.
     *
     * @param crypto object associated with the call or null if none required.
     * @param flags optional flags; should be 0
     * @param cancel an object that can be used to cancel authentication
     * @param callback an object to receive authentication events
     * @param handler an optional handler for events
     */
    @RequiresPermission(Manifest.permission.USE_FINGERPRINT)
    public void authenticate(@Nullable CryptoObject crypto, int flags,
            @Nullable CancellationSignal cancel, @NonNull AuthenticationCallback callback,
            @Nullable Handler handler) {
        // No-op.
    }

    /**
     * A wrapper class for the crypto objects supported by FingerprintManager. Currently the
     * framework supports {@link Signature} and {@link Cipher} objects.
     */
    public static class CryptoObject {

        private final Signature mSignature;
        private final Cipher mCipher;
        private final Mac mMac;

        public CryptoObject(@NonNull Signature signature) {
            mSignature = signature;
            mCipher = null;
            mMac = null;

        }

        public CryptoObject(@NonNull Cipher cipher) {
            mCipher = cipher;
            mSignature = null;
            mMac = null;
        }

        public CryptoObject(@NonNull Mac mac) {
            mMac = mac;
            mCipher = null;
            mSignature = null;
        }

        /**
         * Get {@link Signature} object.
         * @return {@link Signature} object or null if this doesn't contain one.
         */
        @Nullable
        public Signature getSignature() { return mSignature; }

        /**
         * Get {@link Cipher} object.
         * @return {@link Cipher} object or null if this doesn't contain one.
         */
        @Nullable
        public Cipher getCipher() { return mCipher; }

        /**
         * Get {@link Mac} object.
         * @return {@link Mac} object or null if this doesn't contain one.
         */
        @Nullable
        public Mac getMac() { return mMac; }
    }

    /**
     * Container for callback data from {@link FingerprintManagerCompat#authenticate(CryptoObject,
     *     int, CancellationSignal, AuthenticationCallback, Handler)}.
     */
    public static final class AuthenticationResult {
        private final CryptoObject mCryptoObject;

        public AuthenticationResult(@NonNull CryptoObject crypto) {
            mCryptoObject = crypto;
        }

        /**
         * Obtain the crypto object associated with this transaction
         * @return crypto object provided to {@link FingerprintManagerCompat#authenticate(
         *         CryptoObject, int, CancellationSignal, AuthenticationCallback, Handler)}.
         */
        @NonNull
        public CryptoObject getCryptoObject() { return mCryptoObject; }
    }

    /**
     * Callback structure provided to {@link FingerprintManagerCompat#authenticate(CryptoObject,
     * int, CancellationSignal, AuthenticationCallback, Handler)}. Users of {@link
     * FingerprintManagerCompat#authenticate(CryptoObject, int, CancellationSignal,
     * AuthenticationCallback, Handler) } must provide an implementation of this for listening to
     * fingerprint events.
     */
    public static abstract class AuthenticationCallback {
        /**
         * Called when an unrecoverable error has been encountered and the operation is complete.
         * No further callbacks will be made on this object.
         * @param errMsgId An integer identifying the error message
         * @param errString A human-readable error string that can be shown in UI
         */
        public void onAuthenticationError(int errMsgId, @NonNull CharSequence errString) { }

        /**
         * Called when a recoverable error has been encountered during authentication. The help
         * string is provided to give the user guidance for what went wrong, such as
         * "Sensor dirty, please clean it."
         * @param helpMsgId An integer identifying the error message
         * @param helpString A human-readable string that can be shown in UI
         */
        public void onAuthenticationHelp(int helpMsgId, @NonNull CharSequence helpString) { }

        /**
         * Called when a fingerprint is recognized.
         * @param result An object containing authentication-related data
         */
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) { }

        /**
         * Called when a fingerprint is valid but not recognized.
         */
        public void onAuthenticationFailed() { }
    }
}
