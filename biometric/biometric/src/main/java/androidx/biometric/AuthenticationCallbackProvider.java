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

package androidx.biometric;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Uses a common listener interface provided by the client to create and cache authentication
 * callback objects that are compatible with {@link android.hardware.biometrics.BiometricPrompt} or
 * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 */
@SuppressWarnings("deprecation")
class AuthenticationCallbackProvider {
    static class Listener {
        void onSuccess(@NonNull BiometricPrompt.AuthenticationResult result) {}

        void onError(int errorCode, @Nullable CharSequence errorMessage) {}

        void onHelp(@Nullable CharSequence helpMessage) {}

        void onFailure() {}
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Nullable
    private android.hardware.biometrics.BiometricPrompt.AuthenticationCallback mBiometricCallback;

    @Nullable
    private androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback
            mFingerprintCallback;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull
    final Listener mListener;

    AuthenticationCallbackProvider(@NonNull Listener listener) {
        mListener = listener;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @NonNull
    android.hardware.biometrics.BiometricPrompt.AuthenticationCallback getBiometricCallback() {
        if (mBiometricCallback == null) {
            mBiometricCallback = new android.hardware.biometrics.BiometricPrompt
                    .AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    mListener.onError(errorCode, errString);
                }

                @Override
                public void onAuthenticationHelp(
                        final int helpCode, final CharSequence helpString) {
                    // Don't forward the result to the client, since the dialog takes care of it.
                }

                @Override
                public void onAuthenticationSucceeded(
                        android.hardware.biometrics.BiometricPrompt.AuthenticationResult result) {
                    final BiometricPrompt.AuthenticationResult unwrappedResult =
                            new BiometricPrompt.AuthenticationResult(result != null
                                    ? CryptoObjectUtils.unwrapFromBiometricPrompt(
                                            result.getCryptoObject())
                                    : null);
                    mListener.onSuccess(unwrappedResult);
                }

                @Override
                public void onAuthenticationFailed() {
                    mListener.onFailure();
                }
            };
        }
        return mBiometricCallback;
    }

    @NonNull
    androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback
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
                    final BiometricPrompt.AuthenticationResult unwrappedResult =
                            result != null
                                    ? new BiometricPrompt.AuthenticationResult(
                                            CryptoObjectUtils.unwrapFromFingerprintManager(
                                                    result.getCryptoObject()))
                                    : new BiometricPrompt.AuthenticationResult(null /* crypto */);
                    mListener.onSuccess(unwrappedResult);
                }

                @Override
                public void onAuthenticationFailed() {
                    mListener.onFailure();
                }
            };
        }
        return mFingerprintCallback;
    }
}
