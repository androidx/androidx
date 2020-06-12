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

package androidx.biometric;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class that provides system information related to biometrics (e.g. fingerprint, face, etc.).
 *
 * <p>On devices running Android 10 (API 29) and above, this will query the framework's version of
 * {@link android.hardware.biometrics.BiometricManager}. On Android 9.0 (API 28) and prior
 * versions, this will query {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 *
 * @see BiometricPrompt To prompt the user to authenticate with their biometric.
 */
@SuppressWarnings("deprecation")
public class BiometricManager {
    private static final String TAG = "BiometricManager";

    // Only guaranteed to be non-null on API <29.
    @Nullable
    private final androidx.core.hardware.fingerprint.FingerprintManagerCompat mFingerprintManager;

    // Only guaranteed to be non-null on API 29+.
    @Nullable
    private final android.hardware.biometrics.BiometricManager mBiometricManager;

    /**
     * No error detected.
     */
    public static final int BIOMETRIC_SUCCESS =
            android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS;

    /**
     * The hardware is unavailable. Try again later.
     */
    public static final int BIOMETRIC_ERROR_HW_UNAVAILABLE =
            android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE;

    /**
     * The user does not have any biometrics enrolled.
     */
    public static final int BIOMETRIC_ERROR_NONE_ENROLLED =
            android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;

    /**
     * There is no biometric hardware.
     */
    public static final int BIOMETRIC_ERROR_NO_HARDWARE =
            android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;

    /**
     * An error code that may be returned when checking for biometric authentication.
     */
    @IntDef({
        BIOMETRIC_SUCCESS,
        BIOMETRIC_ERROR_HW_UNAVAILABLE,
        BIOMETRIC_ERROR_NONE_ENROLLED,
        BIOMETRIC_ERROR_NO_HARDWARE
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface BiometricError {}

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29Impl {
        @NonNull
        static android.hardware.biometrics.BiometricManager create(Context context) {
            return context.getSystemService(android.hardware.biometrics.BiometricManager.class);
        }

        static @BiometricError int canAuthenticate(
                android.hardware.biometrics.BiometricManager biometricManager) {
            return biometricManager.canAuthenticate();
        }
    }

    /**
     * Creates a {@link BiometricManager} instance from the given context.
     *
     * @return An instance of {@link BiometricManager}.
     */
    @NonNull
    public static BiometricManager from(@NonNull Context context) {
        return new BiometricManager(context);
    }

    // Prevent direct instantiation.
    private BiometricManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mBiometricManager = Api29Impl.create(context);
            mFingerprintManager = null;
        } else {
            mBiometricManager = null;
            mFingerprintManager =
                    androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(context);
        }
    }

    /**
     * Constructs a {@link BiometricManager} instance from the given framework implementation.
     *
     * @param frameworkManager An instance of {@link android.hardware.biometrics.BiometricManager}.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @VisibleForTesting
    BiometricManager(android.hardware.biometrics.BiometricManager frameworkManager) {
        mBiometricManager = frameworkManager;
        mFingerprintManager = null;
    }

    /**
     * Checks if the user can authenticate with biometrics. This requires at least one biometric
     * sensor to be present, enrolled, and available on the device.
     *
     * @return {@link #BIOMETRIC_SUCCESS} if the user can authenticate with biometrics. Otherwise,
     * returns an error code indicating why the user cannot authenticate.
     */
    @BiometricError
    public int canAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mBiometricManager == null) {
                Log.e(TAG, "Failure in canAuthenticate(). BiometricManager was null.");
                return BIOMETRIC_ERROR_HW_UNAVAILABLE;
            } else {
                return Api29Impl.canAuthenticate(mBiometricManager);
            }
        } else {
            if (mFingerprintManager == null) {
                Log.e(TAG, "Failure in canAuthenticate(). FingerprintManager was null.");
                return BIOMETRIC_ERROR_HW_UNAVAILABLE;
            } else if (!mFingerprintManager.isHardwareDetected()) {
                return BIOMETRIC_ERROR_NO_HARDWARE;
            } else if (!mFingerprintManager.hasEnrolledFingerprints()) {
                return BIOMETRIC_ERROR_NONE_ENROLLED;
            } else {
                return BIOMETRIC_SUCCESS;
            }
        }
    }
}
