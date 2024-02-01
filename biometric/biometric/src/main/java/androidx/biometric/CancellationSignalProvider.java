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

import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Creates and caches cancellation signal objects that are compatible with
 * {@link android.hardware.biometrics.BiometricPrompt} or
 * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 */
@SuppressWarnings("deprecation")
class CancellationSignalProvider {
    private static final String TAG = "CancelSignalProvider";

    /**
     * An injector for various class dependencies. Used for testing.
     */
    @VisibleForTesting
    interface Injector {
        /**
         * Returns a cancellation signal object that is compatible with
         * {@link android.hardware.biometrics.BiometricPrompt}.
         *
         * @return An instance of {@link android.os.CancellationSignal}.
         */
        @NonNull
        android.os.CancellationSignal getBiometricCancellationSignal();

        /**
         * Returns a cancellation signal object that is compatible with
         * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
         *
         * @return An instance of {@link androidx.core.os.CancellationSignal}.
         */
        @NonNull
        androidx.core.os.CancellationSignal getFingerprintCancellationSignal();
    }

    /**
     * The injector for class dependencies used by this provider.
     */
    private final Injector mInjector;

    /**
     * A cancellation signal object that is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @Nullable private android.os.CancellationSignal mBiometricCancellationSignal;

    /**
     * A cancellation signal object that is compatible with
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    @Nullable private androidx.core.os.CancellationSignal mFingerprintCancellationSignal;

    /**
     * Creates a new cancellation signal provider instance.
     */
    CancellationSignalProvider() {
        mInjector = new Injector() {
            @Override
            @NonNull
            public CancellationSignal getBiometricCancellationSignal() {
                return new CancellationSignal();
            }

            @Override
            @NonNull
            public androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
                return new androidx.core.os.CancellationSignal();
            }
        };
    }

    /**
     * Creates a new cancellation signal provider instance with the given injector.
     *
     * @param injector An injector for class and method dependencies.
     */
    @VisibleForTesting
    CancellationSignalProvider(Injector injector) {
        mInjector = injector;
    }

    /**
     * Provides a cancellation signal object that is compatible with
     * {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * <p>Subsequent calls to this method for the same provider instance will return the same
     * cancellation signal, until {@link #cancel()} is invoked.
     *
     * @return A cancellation signal that can be passed to
     *  {@link android.hardware.biometrics.BiometricPrompt}.
     */
    @NonNull
    android.os.CancellationSignal getBiometricCancellationSignal() {
        if (mBiometricCancellationSignal == null) {
            mBiometricCancellationSignal = mInjector.getBiometricCancellationSignal();
        }
        return mBiometricCancellationSignal;
    }

    /**
     * Provides a cancellation signal object that is compatible with
     * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     *
     * <p>Subsequent calls to this method for the same provider instance will return the same
     * cancellation signal, until {@link #cancel()} is invoked.
     *
     * @return A cancellation signal that can be passed to
     *  {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
     */
    @NonNull
    androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
        if (mFingerprintCancellationSignal == null) {
            mFingerprintCancellationSignal = mInjector.getFingerprintCancellationSignal();
        }
        return mFingerprintCancellationSignal;
    }

    /**
     * Invokes cancel for all cached cancellation signal objects and clears any references to them.
     */
    void cancel() {
        if (mBiometricCancellationSignal != null) {
            try {
                mBiometricCancellationSignal.cancel();
            } catch (NullPointerException e) {
                // Catch and handle NPE if thrown by framework call to cancel() (b/151316421).
                Log.e(TAG, "Got NPE while canceling biometric authentication.", e);
            }
            mBiometricCancellationSignal = null;
        }
        if (mFingerprintCancellationSignal != null) {
            try {
                mFingerprintCancellationSignal.cancel();
            } catch (NullPointerException e) {
                // Catch and handle NPE if thrown by framework call to cancel() (b/151316421).
                Log.e(TAG, "Got NPE while canceling fingerprint authentication.", e);
            }
            mFingerprintCancellationSignal = null;
        }
    }
}
