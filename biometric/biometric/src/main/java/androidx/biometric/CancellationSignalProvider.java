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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Creates and caches cancellation signal objects that are compatible with
 * {@link android.hardware.biometrics.BiometricPrompt} or
 * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 */
@SuppressWarnings("deprecation")
class CancellationSignalProvider {
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private @Nullable android.os.CancellationSignal mBiometricCancellationSignal;

    private @Nullable androidx.core.os.CancellationSignal mFingerprintCancellationSignal;

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    android.os.CancellationSignal getBiometricCancellationSignal() {
        if (mBiometricCancellationSignal == null) {
            mBiometricCancellationSignal = new android.os.CancellationSignal();
        }
        return mBiometricCancellationSignal;
    }

    androidx.core.os.CancellationSignal getFingerprintCancellationSignal() {
        if (mFingerprintCancellationSignal == null) {
            mFingerprintCancellationSignal = new androidx.core.os.CancellationSignal();
        }
        return mFingerprintCancellationSignal;
    }

    void cancel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && mBiometricCancellationSignal != null) {
            mBiometricCancellationSignal.cancel();
            mBiometricCancellationSignal = null;
        }
        if (mFingerprintCancellationSignal != null) {
            mFingerprintCancellationSignal.cancel();
            mFingerprintCancellationSignal = null;
        }
    }
}
