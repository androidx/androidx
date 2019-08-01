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

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

/**
 * Singleton class to facilitate communication between the {@link BiometricPrompt} for the client
 * activity and the one attached to {@link DeviceCredentialHandlerActivity} when allowing device
 * credential authentication prior to Q.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class DeviceCredentialHandlerBridge {
    private static DeviceCredentialHandlerBridge sInstance;

    @Nullable
    private BiometricFragment mBiometricFragment;

    @Nullable
    private Executor mExecutor;

    @Nullable
    private DialogInterface.OnClickListener mOnClickListener;

    @Nullable
    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback;

    // Private constructor to enforce singleton pattern.
    private DeviceCredentialHandlerBridge() {
    }

    /** @return Instance of the singleton bridge, creating it if necessary. */
    @NonNull
    static DeviceCredentialHandlerBridge getInstance() {
        if (sInstance == null) {
            sInstance = new DeviceCredentialHandlerBridge();
        }
        return sInstance;
    }

    /** @return Instance of the singleton bridge if already created, or null otherwise. */
    @Nullable
    static DeviceCredentialHandlerBridge getInstanceIfNotNull() {
        return sInstance;
    }

    /**
     * Registers a {@link BiometricFragment} to the bridge. Will automatically receive new callbacks
     * set by {@link #setCallbacks(Executor, DialogInterface.OnClickListener,
     * BiometricPrompt.AuthenticationCallback)}.
     */
    void setBiometricFragment(@Nullable BiometricFragment biometricFragment) {
        mBiometricFragment = biometricFragment;
    }

    /** @return See {@link #setBiometricFragment(BiometricFragment)} */
    @Nullable
    BiometricFragment getBiometricFragment() {
        return mBiometricFragment;
    }

    /**
     * Registers dialog and authentication callbacks to the bridge, along with an executor that can
     * be used to run them. If a {@link BiometricFragment} has been registered via
     * {@link #setBiometricFragment(BiometricFragment)}, then it will receive the updated executor
     * and callbacks as well.
     *
     * @param executor               An executor that can be used to run callbacks.
     * @param onClickListener        A dialog button listener for a biometric prompt.
     * @param authenticationCallback A handler for various biometric prompt authentication events.
     */
    @SuppressLint("LambdaLast")
    void setCallbacks(@NonNull Executor executor,
            @NonNull DialogInterface.OnClickListener onClickListener,
            @NonNull BiometricPrompt.AuthenticationCallback authenticationCallback) {
        mExecutor = executor;
        mOnClickListener = onClickListener;
        mAuthenticationCallback = authenticationCallback;
        if (mBiometricFragment != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mBiometricFragment.setCallbacks(executor, onClickListener, authenticationCallback);
        }
    }

    /**
     * @return The latest {@link Executor} set via {@link #setCallbacks(Executor,
     * DialogInterface.OnClickListener, BiometricPrompt.AuthenticationCallback)}.
     */
    @Nullable
    Executor getExecutor() {
        return mExecutor;
    }

    /**
     * @return The latest {@link DialogInterface.OnClickListener} set via {@link #setCallbacks(
     * Executor, DialogInterface.OnClickListener, BiometricPrompt.AuthenticationCallback)}.
     */
    @Nullable
    DialogInterface.OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    /**
     * @return The latest {@link BiometricPrompt.AuthenticationCallback} set via
     * {@link #setCallbacks(Executor, DialogInterface.OnClickListener,
     * BiometricPrompt.AuthenticationCallback)}.
     */
    @Nullable
    BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        return mAuthenticationCallback;
    }

    /** Clears all data associated with the bridge, returning it to its default state. */
    void reset() {
        mBiometricFragment = null;
        mExecutor = null;
        mOnClickListener = null;
        mAuthenticationCallback = null;
        sInstance = null;
    }
}
