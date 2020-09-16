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

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

/**
 * A container for data associated with an ongoing authentication session, including intermediate
 * values needed to display the prompt UI.
 *
 * <p>This model and all of its data is persisted over the lifetime of the client activity that
 * hosts the {@link BiometricPrompt}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BiometricViewModel extends ViewModel {
    /**
     * The default executor provided when {@link #getClientExecutor()} is called before
     * {@link #setClientExecutor(Executor)}.
     */
    private static class DefaultExecutor implements Executor {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        DefaultExecutor() {}

        @Override
        public void execute(Runnable runnable) {
            mHandler.post(runnable);
        }
    }

    /**
     * The authentication callback listener passed to {@link AuthenticationCallbackProvider} when
     * {@link #getAuthenticationCallbackProvider()} is called.
     */
    private static final class CallbackListener extends AuthenticationCallbackProvider.Listener {
        @NonNull private final WeakReference<BiometricViewModel> mViewModelRef;

        /**
         * Creates a callback listener with a weak reference to the given view model.
         *
         * @param viewModel The view model instance to hold a weak reference to.
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        CallbackListener(@Nullable BiometricViewModel viewModel) {
            mViewModelRef = new WeakReference<>(viewModel);
        }

        @Override
        void onSuccess(@NonNull BiometricPrompt.AuthenticationResult result) {
            if (mViewModelRef.get() != null && mViewModelRef.get().isAwaitingResult()) {
                // Try to infer the authentication type if unknown.
                if (result.getAuthenticationType()
                        == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN) {
                    result = new BiometricPrompt.AuthenticationResult(
                            result.getCryptoObject(),
                            mViewModelRef.get().getInferredAuthenticationResultType());
                }

                mViewModelRef.get().setAuthenticationResult(result);
            }
        }

        @Override
        void onError(int errorCode, @Nullable CharSequence errorMessage) {
            if (mViewModelRef.get() != null
                    && !mViewModelRef.get().isConfirmingDeviceCredential()
                    && mViewModelRef.get().isAwaitingResult()) {
                mViewModelRef.get().setAuthenticationError(
                        new BiometricErrorData(errorCode, errorMessage));
            }
        }

        @Override
        void onHelp(@Nullable CharSequence helpMessage) {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get().setAuthenticationHelpMessage(helpMessage);
            }
        }

        @Override
        void onFailure() {
            if (mViewModelRef.get() != null && mViewModelRef.get().isAwaitingResult()) {
                mViewModelRef.get().setAuthenticationFailurePending(true);
            }
        }
    }

    /**
     * The dialog listener that is returned by {@link #getNegativeButtonListener()}.
     */
    private static class NegativeButtonListener implements DialogInterface.OnClickListener {
        @NonNull private final WeakReference<BiometricViewModel> mViewModelRef;

        /**
         * Creates a negative button listener with a weak reference to the given view model.
         *
         * @param viewModel The view model instance to hold a weak reference to.
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        NegativeButtonListener(@Nullable BiometricViewModel viewModel) {
            mViewModelRef = new WeakReference<>(viewModel);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get().setNegativeButtonPressPending(true);
            }
        }
    }

    /**
     * The executor that will run authentication callback methods.
     *
     * <p>If unset, callbacks are invoked on the main thread with {@link Looper#getMainLooper()}.
     */
    @Nullable private Executor mClientExecutor;

    /**
     * The callback object that will receive authentication events.
     */
    @Nullable private BiometricPrompt.AuthenticationCallback mClientCallback;

    /**
     * Info about the appearance and behavior of the prompt provided by the client application.
     */
    @Nullable private BiometricPrompt.PromptInfo mPromptInfo;

    /**
     * The crypto object associated with the current authentication session.
     */
    @Nullable private BiometricPrompt.CryptoObject mCryptoObject;

    /**
     * A provider for cross-platform compatible authentication callbacks.
     */
    @Nullable private AuthenticationCallbackProvider mAuthenticationCallbackProvider;

    /**
     * A provider for cross-platform compatible cancellation signal objects.
     */
    @Nullable private CancellationSignalProvider mCancellationSignalProvider;

    /**
     * A dialog listener for the negative button shown on the prompt.
     */
    @Nullable private DialogInterface.OnClickListener mNegativeButtonListener;

    /**
     * A label for the negative button shown on the prompt.
     *
     * <p>If set, this value overrides the one returned by
     * {@link BiometricPrompt.PromptInfo#getNegativeButtonText()}.
     */
    @Nullable private CharSequence mNegativeButtonTextOverride;

    /**
     * An integer indicating where the dialog was last canceled from.
     */
    @BiometricFragment.CanceledFrom
    private int mCanceledFrom = BiometricFragment.CANCELED_FROM_INTERNAL;

    /**
     * Whether the prompt is currently showing.
     */
    private boolean mIsPromptShowing;

    /**
     * Whether the client callback is awaiting an authentication result.
     */
    private boolean mIsAwaitingResult;

    /**
     * Whether the user is currently authenticating with their PIN, pattern, or password.
     */
    private boolean mIsConfirmingDeviceCredential;

    /**
     * Whether the prompt should delay showing the authentication UI.
     */
    private boolean mIsDelayingPrompt;

    /**
     * Whether the prompt should ignore cancel requests not initiated by the client.
     */
    private boolean mIsIgnoringCancel;

    /**
     * Information associated with a successful authentication attempt.
     */
    @Nullable private MutableLiveData<BiometricPrompt.AuthenticationResult> mAuthenticationResult;

    /**
     * Information associated with an unrecoverable authentication error.
     */
    @Nullable private MutableLiveData<BiometricErrorData> mAuthenticationError;

    /**
     * A human-readable message describing a recoverable authentication error or event.
     */
    @Nullable private MutableLiveData<CharSequence> mAuthenticationHelpMessage;

    /**
     * Whether an unrecognized biometric has been presented.
     */
    @Nullable private MutableLiveData<Boolean> mIsAuthenticationFailurePending;

    /**
     * Whether the user has pressed the negative button on the prompt.
     */
    @Nullable private MutableLiveData<Boolean> mIsNegativeButtonPressPending;

    /**
     * Whether the fingerprint dialog should always be dismissed instantly.
     */
    private boolean mIsFingerprintDialogDismissedInstantly = true;

    /**
     * Whether the user has manually canceled out of the fingerprint dialog.
     */
    @Nullable private MutableLiveData<Boolean> mIsFingerprintDialogCancelPending;

    /**
     * The previous state of the fingerprint dialog UI.
     */
    @FingerprintDialogFragment.State
    private int mFingerprintDialogPreviousState = FingerprintDialogFragment.STATE_NONE;

    /**
     * The current state of the fingerprint dialog UI.
     */
    @Nullable private MutableLiveData<Integer> mFingerprintDialogState;

    /**
     * A human-readable message to be displayed below the icon on the fingerprint dialog.
     */
    @Nullable private MutableLiveData<CharSequence> mFingerprintDialogHelpMessage;

    @NonNull
    Executor getClientExecutor() {
        return mClientExecutor != null ? mClientExecutor : new DefaultExecutor();
    }

    void setClientExecutor(@NonNull Executor clientExecutor) {
        mClientExecutor = clientExecutor;
    }

    @NonNull
    BiometricPrompt.AuthenticationCallback getClientCallback() {
        if (mClientCallback == null) {
            mClientCallback = new BiometricPrompt.AuthenticationCallback() {};
        }
        return mClientCallback;
    }

    void setClientCallback(@NonNull BiometricPrompt.AuthenticationCallback clientCallback) {
        mClientCallback = clientCallback;
    }

    void setPromptInfo(@Nullable BiometricPrompt.PromptInfo promptInfo) {
        mPromptInfo = promptInfo;
    }

    /**
     * Gets the title to be shown on the biometric prompt.
     *
     * <p>This method relies on the {@link BiometricPrompt.PromptInfo} set by
     * {@link #setPromptInfo(BiometricPrompt.PromptInfo)}.
     *
     * @return The title for the prompt, or {@code null} if not set.
     */
    @Nullable
    CharSequence getTitle() {
        return mPromptInfo != null ? mPromptInfo.getTitle() : null;
    }

    /**
     * Gets the subtitle to be shown on the biometric prompt.
     *
     * <p>This method relies on the {@link BiometricPrompt.PromptInfo} set by
     * {@link #setPromptInfo(BiometricPrompt.PromptInfo)}.
     *
     * @return The subtitle for the prompt, or {@code null} if not set.
     */
    @Nullable
    CharSequence getSubtitle() {
        return mPromptInfo != null ? mPromptInfo.getSubtitle() : null;
    }

    /**
     * Gets the description to be shown on the biometric prompt.
     *
     * <p>This method relies on the {@link BiometricPrompt.PromptInfo} set by
     * {@link #setPromptInfo(BiometricPrompt.PromptInfo)}.
     *
     * @return The description for the prompt, or {@code null} if not set.
     */
    @Nullable
    CharSequence getDescription() {
        return mPromptInfo != null ? mPromptInfo.getDescription() : null;
    }

    /**
     * Gets the text that should be shown for the negative button on the biometric prompt.
     *
     * <p>If non-null, the value set by {@link #setNegativeButtonTextOverride(CharSequence)} is
     * used. Otherwise, falls back to the value returned by
     * {@link BiometricPrompt.PromptInfo#getNegativeButtonText()}, or {@code null} if a non-null
     * {@link BiometricPrompt.PromptInfo} has not been set by
     * {@link #setPromptInfo(BiometricPrompt.PromptInfo)}.
     *
     * @return The negative button text for the prompt, or {@code null} if not set.
     */
    @Nullable
    CharSequence getNegativeButtonText() {
        if (mNegativeButtonTextOverride != null) {
            return mNegativeButtonTextOverride;
        } else if (mPromptInfo != null) {
            return mPromptInfo.getNegativeButtonText();
        } else {
            return null;
        }
    }

    /**
     * Checks if the confirmation required option is enabled for the biometric prompt.
     *
     * <p>This method relies on the {@link BiometricPrompt.PromptInfo} set by
     * {@link #setPromptInfo(BiometricPrompt.PromptInfo)}.
     *
     * @return Whether the confirmation required option is enabled.
     */
    boolean isConfirmationRequired() {
        return mPromptInfo == null || mPromptInfo.isConfirmationRequired();
    }

    /**
     * Gets the type(s) of authenticators that may be invoked by the biometric prompt.
     *
     * <p>If a non-null {@link BiometricPrompt.PromptInfo} has been set by
     * {@link #setPromptInfo(BiometricPrompt.PromptInfo)}, this is the single consolidated set of
     * authenticators allowed by the prompt, taking into account the values of
     * {@link BiometricPrompt.PromptInfo#getAllowedAuthenticators()},
     * {@link BiometricPrompt.PromptInfo#isDeviceCredentialAllowed()}, and
     * {@link #getCryptoObject()}.
     *
     * @return A bit field representing all valid authenticator types that may be invoked by
     * the prompt, or 0 if not set.
     */
    @SuppressWarnings("deprecation")
    @BiometricManager.AuthenticatorTypes
    int getAllowedAuthenticators() {
        return mPromptInfo != null
                ? AuthenticatorUtils.getConsolidatedAuthenticators(mPromptInfo, mCryptoObject)
                : 0;
    }

    @Nullable
    BiometricPrompt.CryptoObject getCryptoObject() {
        return mCryptoObject;
    }

    void setCryptoObject(@Nullable BiometricPrompt.CryptoObject cryptoObject) {
        mCryptoObject = cryptoObject;
    }

    @NonNull
    AuthenticationCallbackProvider getAuthenticationCallbackProvider() {
        if (mAuthenticationCallbackProvider == null) {
            mAuthenticationCallbackProvider =
                    new AuthenticationCallbackProvider(new CallbackListener(this));
        }
        return mAuthenticationCallbackProvider;
    }

    @NonNull
    CancellationSignalProvider getCancellationSignalProvider() {
        if (mCancellationSignalProvider == null) {
            mCancellationSignalProvider = new CancellationSignalProvider();
        }
        return mCancellationSignalProvider;
    }

    @NonNull
    DialogInterface.OnClickListener getNegativeButtonListener() {
        if (mNegativeButtonListener == null) {
            mNegativeButtonListener = new NegativeButtonListener(this);
        }
        return mNegativeButtonListener;
    }

    void setNegativeButtonTextOverride(@Nullable CharSequence negativeButtonTextOverride) {
        mNegativeButtonTextOverride = negativeButtonTextOverride;
    }

    int getCanceledFrom() {
        return mCanceledFrom;
    }

    void setCanceledFrom(int canceledFrom) {
        mCanceledFrom = canceledFrom;
    }

    boolean isPromptShowing() {
        return mIsPromptShowing;
    }

    void setPromptShowing(boolean promptShowing) {
        mIsPromptShowing = promptShowing;
    }

    boolean isAwaitingResult() {
        return mIsAwaitingResult;
    }

    void setAwaitingResult(boolean awaitingResult) {
        mIsAwaitingResult = awaitingResult;
    }

    boolean isConfirmingDeviceCredential() {
        return mIsConfirmingDeviceCredential;
    }

    void setConfirmingDeviceCredential(boolean confirmingDeviceCredential) {
        mIsConfirmingDeviceCredential = confirmingDeviceCredential;
    }

    boolean isDelayingPrompt() {
        return mIsDelayingPrompt;
    }

    void setDelayingPrompt(boolean delayingPrompt) {
        mIsDelayingPrompt = delayingPrompt;
    }

    boolean isIgnoringCancel() {
        return mIsIgnoringCancel;
    }

    void setIgnoringCancel(boolean ignoringCancel) {
        mIsIgnoringCancel = ignoringCancel;
    }

    @NonNull
    LiveData<BiometricPrompt.AuthenticationResult> getAuthenticationResult() {
        if (mAuthenticationResult == null) {
            mAuthenticationResult = new MutableLiveData<>();
        }
        return mAuthenticationResult;
    }

    void setAuthenticationResult(
            @Nullable BiometricPrompt.AuthenticationResult authenticationResult) {
        if (mAuthenticationResult == null) {
            mAuthenticationResult = new MutableLiveData<>();
        }
        updateValue(mAuthenticationResult, authenticationResult);
    }

    @NonNull
    MutableLiveData<BiometricErrorData> getAuthenticationError() {
        if (mAuthenticationError == null) {
            mAuthenticationError = new MutableLiveData<>();
        }
        return mAuthenticationError;
    }

    void setAuthenticationError(@Nullable BiometricErrorData authenticationError) {
        if (mAuthenticationError == null) {
            mAuthenticationError = new MutableLiveData<>();
        }
        updateValue(mAuthenticationError, authenticationError);
    }

    @NonNull
    LiveData<CharSequence> getAuthenticationHelpMessage() {
        if (mAuthenticationHelpMessage == null) {
            mAuthenticationHelpMessage = new MutableLiveData<>();
        }
        return mAuthenticationHelpMessage;
    }

    void setAuthenticationHelpMessage(
            @Nullable CharSequence authenticationHelpMessage) {
        if (mAuthenticationHelpMessage == null) {
            mAuthenticationHelpMessage = new MutableLiveData<>();
        }
        updateValue(mAuthenticationHelpMessage, authenticationHelpMessage);
    }

    @NonNull
    LiveData<Boolean> isAuthenticationFailurePending() {
        if (mIsAuthenticationFailurePending == null) {
            mIsAuthenticationFailurePending = new MutableLiveData<>();
        }
        return mIsAuthenticationFailurePending;
    }

    void setAuthenticationFailurePending(boolean authenticationFailurePending) {
        if (mIsAuthenticationFailurePending == null) {
            mIsAuthenticationFailurePending = new MutableLiveData<>();
        }
        updateValue(mIsAuthenticationFailurePending, authenticationFailurePending);
    }

    @NonNull
    LiveData<Boolean> isNegativeButtonPressPending() {
        if (mIsNegativeButtonPressPending == null) {
            mIsNegativeButtonPressPending = new MutableLiveData<>();
        }
        return mIsNegativeButtonPressPending;
    }

    void setNegativeButtonPressPending(boolean negativeButtonPressPending) {
        if (mIsNegativeButtonPressPending == null) {
            mIsNegativeButtonPressPending = new MutableLiveData<>();
        }
        updateValue(mIsNegativeButtonPressPending, negativeButtonPressPending);
    }

    boolean isFingerprintDialogDismissedInstantly() {
        return mIsFingerprintDialogDismissedInstantly;
    }

    void setFingerprintDialogDismissedInstantly(
            boolean fingerprintDialogDismissedInstantly) {
        mIsFingerprintDialogDismissedInstantly = fingerprintDialogDismissedInstantly;
    }

    @NonNull
    LiveData<Boolean> isFingerprintDialogCancelPending() {
        if (mIsFingerprintDialogCancelPending == null) {
            mIsFingerprintDialogCancelPending = new MutableLiveData<>();
        }
        return mIsFingerprintDialogCancelPending;
    }

    void setFingerprintDialogCancelPending(boolean fingerprintDialogCancelPending) {
        if (mIsFingerprintDialogCancelPending == null) {
            mIsFingerprintDialogCancelPending = new MutableLiveData<>();
        }
        updateValue(mIsFingerprintDialogCancelPending, fingerprintDialogCancelPending);
    }

    @FingerprintDialogFragment.State
    int getFingerprintDialogPreviousState() {
        return mFingerprintDialogPreviousState;
    }

    void setFingerprintDialogPreviousState(
            @FingerprintDialogFragment.State int fingerprintDialogPreviousState) {
        mFingerprintDialogPreviousState = fingerprintDialogPreviousState;
    }

    @NonNull
    LiveData<Integer> getFingerprintDialogState() {
        if (mFingerprintDialogState == null) {
            mFingerprintDialogState = new MutableLiveData<>();
        }
        return mFingerprintDialogState;
    }

    void setFingerprintDialogState(
            @FingerprintDialogFragment.State int fingerprintDialogState) {
        if (mFingerprintDialogState == null) {
            mFingerprintDialogState = new MutableLiveData<>();
        }
        updateValue(mFingerprintDialogState, fingerprintDialogState);
    }

    @NonNull
    LiveData<CharSequence> getFingerprintDialogHelpMessage() {
        if (mFingerprintDialogHelpMessage == null) {
            mFingerprintDialogHelpMessage = new MutableLiveData<>();
        }
        return mFingerprintDialogHelpMessage;
    }

    void setFingerprintDialogHelpMessage(
            @NonNull CharSequence fingerprintDialogHelpMessage) {
        if (mFingerprintDialogHelpMessage == null) {
            mFingerprintDialogHelpMessage = new MutableLiveData<>();
        }
        updateValue(mFingerprintDialogHelpMessage, fingerprintDialogHelpMessage);
    }

    /**
     * Attempts to infer the type of authenticator that was used to authenticate the user.
     *
     * @return The inferred authentication type, or
     * {@link BiometricPrompt#AUTHENTICATION_RESULT_TYPE_UNKNOWN} if unknown.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @BiometricPrompt.AuthenticationResultType
    int getInferredAuthenticationResultType() {
        @BiometricManager.AuthenticatorTypes final int authenticators = getAllowedAuthenticators();
        if (AuthenticatorUtils.isSomeBiometricAllowed(authenticators)
                && !AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            return BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC;
        }
        return BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN;
    }

    /**
     * Ensures the value of a given mutable live data object is updated on the main thread.
     *
     * @param liveData The mutable live data object whose value should be updated.
     * @param value    The new value to be set for the mutable live data object.
     */
    private static <T> void updateValue(MutableLiveData<T> liveData, T value) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
