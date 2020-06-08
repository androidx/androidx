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

import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BiometricViewModel extends ViewModel {
    @Nullable private Executor mClientExecutor;

    @Nullable private BiometricPrompt.AuthenticationCallback mClientCallback;

    @Nullable private BiometricPrompt.PromptInfo mPromptInfo;

    @Nullable private BiometricPrompt.CryptoObject mCryptoObject;

    @Nullable private AuthenticationCallbackProvider mAuthenticationCallbackProvider;

    @Nullable private CancellationSignalProvider mCancellationSignalProvider;

    @Nullable private DialogInterface.OnClickListener mNegativeButtonListener;

    @Nullable private CharSequence mNegativeButtonText;

    @BiometricFragment.CanceledFrom
    private int mCanceledFrom = BiometricFragment.CANCELED_FROM_NONE;

    private boolean mIsPromptShowing;

    private boolean mIsAwaitingResult;

    private boolean mIsConfirmingDeviceCredential;

    @Nullable private MutableLiveData<BiometricPrompt.AuthenticationResult> mAuthenticationResult;

    @Nullable private MutableLiveData<BiometricErrorData> mAuthenticationError;

    @Nullable private MutableLiveData<CharSequence> mAuthenticationHelpMessage;

    @Nullable private MutableLiveData<Boolean> mIsAuthenticationFailurePending;

    @Nullable private MutableLiveData<Boolean> mIsNegativeButtonPressPending;

    private boolean mIsFingerprintDialogDismissedInstantly = true;

    @Nullable private MutableLiveData<Boolean> mIsFingerprintDialogCancelPending;

    @FingerprintDialogFragment.State
    private int mFingerprintDialogPreviousState = FingerprintDialogFragment.STATE_NONE;

    @Nullable private MutableLiveData<Integer> mFingerprintDialogState;

    @Nullable private MutableLiveData<CharSequence> mFingerprintDialogHelpMessage;

    @NonNull
    Executor getClientExecutor() {
        if (mClientExecutor == null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            mClientExecutor = new Executor() {
                @Override
                public void execute(@NonNull Runnable command) {
                    handler.post(command);
                }
            };
        }
        return mClientExecutor;
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

    @Nullable
    BiometricPrompt.PromptInfo getPromptInfo() {
        return mPromptInfo;
    }

    void setPromptInfo(@Nullable BiometricPrompt.PromptInfo promptInfo) {
        mPromptInfo = promptInfo;
    }

    @Nullable
    CharSequence getTitle() {
        return mPromptInfo != null ? mPromptInfo.getTitle() : null;
    }

    @Nullable
    CharSequence getSubtitle() {
        return mPromptInfo != null ? mPromptInfo.getSubtitle() : null;
    }

    @Nullable
    CharSequence getDescription() {
        return mPromptInfo != null ? mPromptInfo.getDescription() : null;
    }

    boolean isDeviceCredentialAllowed() {
        return mPromptInfo != null && mPromptInfo.isDeviceCredentialAllowed();
    }

    boolean isConfirmationRequired() {
        return mPromptInfo == null || mPromptInfo.isConfirmationRequired();
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
            mAuthenticationCallbackProvider = new AuthenticationCallbackProvider(
                    new AuthenticationCallbackProvider.Listener() {
                        @Override
                        void onSuccess(@NonNull BiometricPrompt.AuthenticationResult result) {
                            if (isAwaitingResult()) {
                                setAuthenticationResult(result);
                            }
                        }

                        @Override
                        void onError(int errorCode, @Nullable CharSequence errorMessage) {
                            if (!isConfirmingDeviceCredential() && isAwaitingResult()) {
                                setAuthenticationError(
                                        new BiometricErrorData(errorCode, errorMessage));
                            }
                        }

                        @Override
                        void onHelp(@Nullable CharSequence helpMessage) {
                            setAuthenticationHelpMessage(helpMessage);
                        }

                        @Override
                        void onFailure() {
                            if (isAwaitingResult()) {
                                setAuthenticationFailurePending(true);
                            }
                        }
                    }
            );
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
            mNegativeButtonListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setNegativeButtonPressPending(true);
                }
            };
        }
        return mNegativeButtonListener;
    }

    @Nullable
    CharSequence getNegativeButtonText() {
        if (mNegativeButtonText != null) {
            return mNegativeButtonText;
        } else if (mPromptInfo != null) {
            return mPromptInfo.getNegativeButtonText();
        } else {
            return null;
        }
    }

    void setNegativeButtonText(@Nullable CharSequence negativeButtonText) {
        mNegativeButtonText = negativeButtonText;
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
        mAuthenticationResult.setValue(authenticationResult);
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
        mAuthenticationError.setValue(authenticationError);
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
        mAuthenticationHelpMessage.setValue(authenticationHelpMessage);
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
        mIsAuthenticationFailurePending.setValue(authenticationFailurePending);
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
        mIsNegativeButtonPressPending.setValue(negativeButtonPressPending);
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
        mIsFingerprintDialogCancelPending.setValue(fingerprintDialogCancelPending);
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
        mFingerprintDialogState.setValue(fingerprintDialogState);
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
        mFingerprintDialogHelpMessage.setValue(fingerprintDialogHelpMessage);
    }
}
