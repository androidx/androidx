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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A class that contains biometric utilities. For authentication, see {@link BiometricPrompt}.
 * On devices running Q and above, this will query the framework's version of
 * {@link android.hardware.biometrics.BiometricManager}. On devices P and older, this will query
 * {@link androidx.core.hardware.fingerprint.FingerprintManagerCompat}.
 */
public class BiometricManager {

    private final Context mContext;
    // Only guaranteed to be non-null on SDK < 29
    @SuppressWarnings("deprecation")
    private final androidx.core.hardware.fingerprint.FingerprintManagerCompat mFingerprintManager;
    // Only guaranteed to be non-null on SDK >= 29 (Q)
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

    @RequiresApi(29)
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
     * Get a {@link BiometricManager} instance with the provided context.
     * @param context
     * @return
     */
    @NonNull
    public static BiometricManager from(@NonNull Context context) {
        return new BiometricManager(context);
    }

    @SuppressWarnings("deprecation")
    private BiometricManager(Context context) {
        mContext = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mBiometricManager = Api29Impl.create(context);
            mFingerprintManager = null;
        } else {
            mBiometricManager = null;
            mFingerprintManager = androidx.core.hardware.fingerprint.FingerprintManagerCompat
                    .from(mContext);
        }
    }

    @IntDef({android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS,
            android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE})
    @interface BiometricError {}

    /**
     * Determine if biometrics can be used. In other words, determine if {@link BiometricPrompt}
     * can be expected to be shown (hardware available, templates enrolled, user-enabled).
     *
     * @return Returns {@link #BIOMETRIC_ERROR_NONE_ENROLLED} if the user does not have any
     *     enrolled, or {@link #BIOMETRIC_ERROR_HW_UNAVAILABLE} if none are currently
     *     supported/enabled. Returns {@link #BIOMETRIC_SUCCESS} if a biometric can currently be
     *     used (enrolled and available).
     */
    public @BiometricError int canAuthenticate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Api29Impl.canAuthenticate(mBiometricManager);
        } else {
            if (!mFingerprintManager.isHardwareDetected()) {
                return BIOMETRIC_ERROR_HW_UNAVAILABLE;
            } else if (!mFingerprintManager.hasEnrolledFingerprints()) {
                return BIOMETRIC_ERROR_NONE_ENROLLED;
            } else {
                return BIOMETRIC_SUCCESS;
            }
        }
    }

}
