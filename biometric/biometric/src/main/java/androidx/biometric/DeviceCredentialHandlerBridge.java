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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Singleton class to facilitate communication between the {@link BiometricPrompt} for the client
 * activity and the one attached to {@link DeviceCredentialHandlerActivity} when allowing device
 * credential authentication prior to Q.
 */
class DeviceCredentialHandlerBridge {
    @Nullable
    private static DeviceCredentialHandlerBridge sInstance;

    private int mClientThemeResId;

    @Nullable
    private BiometricFragment mBiometricFragment;

    @Nullable
    private FingerprintDialogFragment mFingerprintDialogFragment;

    @Nullable
    private FingerprintHelperFragment mFingerprintHelperFragment;

    @Nullable
    private Executor mExecutor;

    @Nullable
    private DialogInterface.OnClickListener mOnClickListener;

    @Nullable
    private BiometricPrompt.AuthenticationCallback mAuthenticationCallback;

    private boolean mConfirmingDeviceCredential;

    // Possible results from launching the confirm device credential Settings activity.
    static final int RESULT_NONE = 0;
    static final int RESULT_SUCCESS = 1;
    static final int RESULT_ERROR = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_NONE, RESULT_SUCCESS, RESULT_ERROR})
    @interface DeviceCredentialResult {}

    private @DeviceCredentialResult int mDeviceCredentialResult = RESULT_NONE;

    // States indicating whether and for how long to ignore calls to reset().
    private static final int NOT_IGNORING_RESET = 0;
    private static final int IGNORING_NEXT_RESET = 1;
    private static final int IGNORING_RESET = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOT_IGNORING_RESET, IGNORING_NEXT_RESET, IGNORING_RESET})
    private @interface IgnoreResetState {}

    private @IgnoreResetState int mIgnoreResetState = NOT_IGNORING_RESET;

    // Private constructor to enforce singleton pattern.
    private DeviceCredentialHandlerBridge() {
    }

    /** @return The singleton bridge, creating it if necessary. */
    @NonNull
    static DeviceCredentialHandlerBridge getInstance() {
        if (sInstance == null) {
            sInstance = new DeviceCredentialHandlerBridge();
        }
        return sInstance;
    }

    /** @return The singleton bridge if already created, or null otherwise. */
    @Nullable
    static DeviceCredentialHandlerBridge getInstanceIfNotNull() {
        return sInstance;
    }

    /**
     * Register the resource ID for the client activity's theme to the bridge. This will be used
     * for styling dialogs and other views in the handler activity.
     */
    void setClientThemeResId(int clientThemeResId) {
        mClientThemeResId = clientThemeResId;
    }

    /** @return See {@link #setClientThemeResId(int)}. */
    int getClientThemeResId() {
        return mClientThemeResId;
    }

    /**
     * Registers a {@link BiometricFragment} to the bridge. This will automatically receive new
     * callbacks set by {@link #setCallbacks(Executor, DialogInterface.OnClickListener,
     * BiometricPrompt.AuthenticationCallback)}.
     */
    void setBiometricFragment(@Nullable BiometricFragment biometricFragment) {
        mBiometricFragment = biometricFragment;
    }

    /** @return See {@link #setBiometricFragment(BiometricFragment)}. */
    @Nullable
    BiometricFragment getBiometricFragment() {
        return mBiometricFragment;
    }

    /**
     * Registers a {@link FingerprintDialogFragment} and {@link FingerprintHelperFragment} to the
     * bridge. These will automatically receive new callbacks set by {@link #setCallbacks(Executor,
     * DialogInterface.OnClickListener, BiometricPrompt.AuthenticationCallback)}.
     */
    void setFingerprintFragments(@Nullable FingerprintDialogFragment fingerprintDialogFragment,
            @Nullable FingerprintHelperFragment fingerprintHelperFragment) {
        mFingerprintDialogFragment = fingerprintDialogFragment;
        mFingerprintHelperFragment = fingerprintHelperFragment;
    }

    /**
     * @return The latest {@link FingerprintDialogFragment} set via
     * {@link #setFingerprintFragments(FingerprintDialogFragment, FingerprintHelperFragment)}.
     */
    @Nullable
    public FingerprintDialogFragment getFingerprintDialogFragment() {
        return mFingerprintDialogFragment;
    }

    /**
     * @return The latest {@link FingerprintHelperFragment} set via
     * {@link #setFingerprintFragments(FingerprintDialogFragment, FingerprintHelperFragment)}.
     */
    @Nullable
    public FingerprintHelperFragment getFingerprintHelperFragment() {
        return mFingerprintHelperFragment;
    }

    /**
     * Registers dialog and authentication callbacks to the bridge, along with an executor that can
     * be used to run them.
     *
     * <p>If a {@link BiometricFragment} has been registered via
     * {@link #setBiometricFragment(BiometricFragment)}, or if a {@link FingerprintDialogFragment}
     * and {@link FingerprintHelperFragment} have been registered via
     * {@link #setFingerprintFragments(FingerprintDialogFragment, FingerprintHelperFragment)}, then
     * these fragments will receive the updated executor and callbacks as well.
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
        } else if (mFingerprintDialogFragment != null && mFingerprintHelperFragment != null) {
            mFingerprintDialogFragment.setNegativeButtonListener(onClickListener);
            mFingerprintHelperFragment.setCallback(executor, authenticationCallback);
            mFingerprintHelperFragment.setHandler(mFingerprintDialogFragment.getHandler());
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

    /**
     * Stores the authentication result from launching the confirm device credential Settings
     * activity. This is intended for the client's {@link BiometricPrompt} instance to read this
     * result and invoke the appropriate authentication callback method.
     */
    void setDeviceCredentialResult(int deviceCredentialResult) {
        mDeviceCredentialResult = deviceCredentialResult;
    }

    /** @return See {@link #setDeviceCredentialResult(int)}. */
    int getDeviceCredentialResult() {
        return mDeviceCredentialResult;
    }

    /**
     * Sets a flag indicating whether the confirm device credential Settings activity is currently
     * being shown.
     */
    void setConfirmingDeviceCredential(boolean confirmingDeviceCredential) {
        mConfirmingDeviceCredential = confirmingDeviceCredential;
    }

    /** @return See {@link #setConfirmingDeviceCredential(boolean)}. */
    boolean isConfirmingDeviceCredential() {
        return mConfirmingDeviceCredential;
    }

    /**
     * Indicates that the bridge should ignore the next call to {@link #reset}. Calling this method
     * after {@link #startIgnoringReset()} but before {@link #stopIgnoringReset()} has no effect.
     */
    void ignoreNextReset() {
        if (mIgnoreResetState == NOT_IGNORING_RESET) {
            mIgnoreResetState = IGNORING_NEXT_RESET;
        }
    }

    /**
     * Indicates that the bridge should ignore all subsequent calls to {@link #reset} until
     * {@link #stopIgnoringReset()} is called.
     */
    void startIgnoringReset() {
        mIgnoreResetState = IGNORING_RESET;
    }

    /**
     * When called after {@link #ignoreNextReset()} or {@link #startIgnoringReset()}, allows
     * subsequent calls to {@link #reset} to go through as normal, until either is called again.
     */
    void stopIgnoringReset() {
        mIgnoreResetState = NOT_IGNORING_RESET;
    }

    /**
     * Clears all data associated with the bridge, returning it to its default state.
     *
     * <p>Note that calls to this method may be ignored if {@link #ignoreNextReset()} or
     * {@link #startIgnoringReset()} has been called without a corresponding call to
     * {@link #stopIgnoringReset()}.
     */
    void reset() {
        if (mIgnoreResetState == IGNORING_RESET) {
            return;
        }

        if (mIgnoreResetState == IGNORING_NEXT_RESET) {
            stopIgnoringReset();
            return;
        }

        mClientThemeResId = 0;
        mBiometricFragment = null;
        mFingerprintDialogFragment = null;
        mFingerprintHelperFragment = null;
        mExecutor = null;
        mOnClickListener = null;
        mAuthenticationCallback = null;
        mDeviceCredentialResult = RESULT_NONE;
        mConfirmingDeviceCredential = false;

        sInstance = null;
    }
}
