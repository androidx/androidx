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

package androidx.biometric.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.biometric.BiometricPrompt;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Uses a common listener interface provided by the client to create and cache authentication
 * callback objects that are compatible with {@link android.hardware.biometrics.BiometricPrompt} or
 * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 */
@SuppressWarnings("deprecation")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthenticationCallbackProvider {
    /**
     * A listener object that can receive events from either
     * {@link android.hardware.biometrics.BiometricPrompt.AuthenticationCallback} or
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback}.
     */
    public static class Listener {
        /**
         * See {@link BiometricPrompt.AuthenticationCallback#onAuthenticationSucceeded(
         *BiometricPrompt.AuthenticationResult)}.
         *
         * @param result An object containing authentication-related data.
         */
        public void onSuccess(BiometricPrompt.@NonNull AuthenticationResult result) {
        }

        /**
         * See {@link BiometricPrompt.AuthenticationCallback#onAuthenticationError(int,
         * CharSequence)}.
         *
         * @param errorCode    An integer ID associated with the error.
         * @param errorMessage A human-readable message that describes the error.
         */
        public void onError(int errorCode, @Nullable CharSequence errorMessage) {
        }

        /**
         * Called when a recoverable error/event has been encountered during authentication.
         *
         * @param helpMessage A human-readable message that describes the event.
         */
        public void onHelp(@Nullable CharSequence helpMessage) {
        }

        /**
         * See {@link BiometricPrompt.AuthenticationCallback#onAuthenticationFailed()}.
         */
        public void onFailure() {
        }
    }

    /**
     * An authentication callback object that is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
     */
    private android.hardware.biometrics.BiometricPrompt.@Nullable AuthenticationCallback
            mBiometricCallback;

    /**
     * An authentication callback object that is compatible with
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    private androidx.core.hardware.fingerprint.FingerprintManagerCompat.@Nullable
            AuthenticationCallback
            mFingerprintCallback;

    /**
     * A common listener object that will receive all authentication events.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @NonNull Listener mListener;

    /**
     * Constructs a callback provider that delegates events to the given listener.
     *
     * @param listener A listener that will receive authentication events.
     */
    public AuthenticationCallbackProvider(@NonNull Listener listener) {
        mListener = listener;
    }

    /**
     * Provides a callback object that wraps the given listener and is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * <p>Subsequent calls to this method for the same provider instance will return the same
     * callback object.
     *
     * @return A callback object that can be passed to
     * {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    public android.hardware.biometrics.BiometricPrompt.@NonNull AuthenticationCallback
            getBiometricCallback() {
        if (mBiometricCallback == null) {
            mBiometricCallback = Api28Impl.createCallback(mListener);
        }
        return mBiometricCallback;
    }

    /**
     * Provides a callback object that wraps the given listener and is compatible with
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     *
     * <p>Subsequent calls to this method for the same provider instance will return the same
     * callback object.
     *
     * @return A callback object that can be passed to
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    public androidx.core.hardware.fingerprint.FingerprintManagerCompat.@NonNull AuthenticationCallback
            getFingerprintCallback() {
        if (mFingerprintCallback == null) {
            mFingerprintCallback = new androidx.core.hardware.fingerprint.FingerprintManagerCompat
                    .AuthenticationCallback() {
                @Override
                public void onAuthenticationError(final int errMsgId, CharSequence errString) {
                    mListener.onError(errMsgId, errString);
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, final CharSequence helpString) {
                    mListener.onHelp(helpString);
                }

                @Override
                public void onAuthenticationSucceeded(final androidx.core.hardware.fingerprint
                        .FingerprintManagerCompat.AuthenticationResult result) {

                    final BiometricPrompt.CryptoObject crypto = result != null
                            ? CryptoObjectUtils.unwrapFromFingerprintManager(
                            result.getCryptoObject())
                            : null;

                    final BiometricPrompt.AuthenticationResult resultCompat =
                            new BiometricPrompt.AuthenticationResult(
                                    crypto, BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC);

                    mListener.onSuccess(resultCompat);
                }

                @Override
                public void onAuthenticationFailed() {
                    mListener.onFailure();
                }
            };
        }
        return mFingerprintCallback;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        // Prevent instantiation.
        private Api30Impl() {
        }

        /**
         * Gets the authentication type from the given framework authentication result.
         *
         * @param result An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.AuthenticationResult}.
         * @return The value returned by calling {@link
         * android.hardware.biometrics.BiometricPrompt.AuthenticationResult#getAuthenticationType()}
         * for the given result object.
         */
        @BiometricPrompt.AuthenticationResultType
        // This is expected because AndroidX and framework annotation are not identical
        @SuppressWarnings("WrongConstant")
        static int getAuthenticationType(
                android.hardware.biometrics.BiometricPrompt.@NonNull AuthenticationResult result) {
            return result.getAuthenticationType();
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private static class Api28Impl {
        // Prevent instantiation.
        private Api28Impl() {
        }

        /**
         * Creates a {@link android.hardware.biometrics.BiometricPrompt.AuthenticationCallback} that
         * delegates events to the given listener.
         *
         * @param listener A listener object that will receive authentication events.
         * @return A new instance of
         * {@link android.hardware.biometrics.BiometricPrompt.AuthenticationCallback}.
         */
        static android.hardware.biometrics.BiometricPrompt.@NonNull AuthenticationCallback
                createCallback(
                final @NonNull Listener listener) {
            return new android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    listener.onError(errorCode, errString);
                }

                @Override
                public void onAuthenticationHelp(
                        final int helpCode, final CharSequence helpString) {
                    // Don't forward the result to the client, since the dialog takes care of it.
                }

                @Override
                public void onAuthenticationSucceeded(
                        android.hardware.biometrics.BiometricPrompt.AuthenticationResult result) {

                    final BiometricPrompt.CryptoObject crypto = result != null
                            ? CryptoObjectUtils.unwrapFromBiometricPrompt(getCryptoObject(result))
                            : null;

                    @BiometricPrompt.AuthenticationResultType final int authenticationType;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        authenticationType = result != null
                                ? Api30Impl.getAuthenticationType(result)
                                : BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN;
                    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN;
                    } else {
                        authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC;
                    }

                    final BiometricPrompt.AuthenticationResult resultCompat =
                            new BiometricPrompt.AuthenticationResult(crypto, authenticationType);

                    listener.onSuccess(resultCompat);
                }

                @Override
                public void onAuthenticationFailed() {
                    listener.onFailure();
                }
            };
        }

        /**
         * Gets the crypto object from the given framework authentication result.
         *
         * @param result An instance of
         *               {@link android.hardware.biometrics.BiometricPrompt.AuthenticationResult}.
         * @return The value returned by calling {@link
         * android.hardware.biometrics.BiometricPrompt.AuthenticationResult#getCryptoObject()} for
         * the given result object.
         */
        static android.hardware.biometrics.BiometricPrompt.@Nullable CryptoObject getCryptoObject(
                android.hardware.biometrics.BiometricPrompt.@NonNull AuthenticationResult result) {
            return result.getCryptoObject();
        }
    }
}
