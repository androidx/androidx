/*
 * Copyright 2018 The Android Open Source Project
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
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RestrictTo;
import androidx.core.os.CancellationSignal;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executor;

/**
 * A fragment that wraps the FingerprintManagerCompat and has the ability to continue authentication
 * across device configuration changes. This class is not meant to be preserved after process death;
 * for security reasons, the BiometricPromptCompat will automatically stop authentication when the
 * activity is no longer in the foreground.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FingerprintHelperFragment extends Fragment {

    private static final String TAG = "FingerprintHelperFragment";

    protected static final int USER_CANCELED_FROM_NONE = 0;
    protected static final int USER_CANCELED_FROM_USER = 1;
    protected static final int USER_CANCELED_FROM_NEGATIVE_BUTTON = 2;

    // Re-set by the application, through BiometricPromptCompat upon orientation changes.
    Executor mExecutor;
    BiometricPrompt.AuthenticationCallback mClientAuthenticationCallback;

    // Re-set by BiometricPromptCompat upon orientation changes. This handler is used to send
    // messages from the AuthenticationCallbacks to the UI.
    Handler mHandler;

    // Set once and retained.
    private boolean mShowing;
    private BiometricPrompt.CryptoObject mCryptoObject;

    // Created once and retained.
    Context mContext;
    int mCanceledFrom;
    private CancellationSignal mCancellationSignal;

    // Also created once and retained.
    @SuppressWarnings("deprecation")
    private final androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback
            mAuthenticationCallback = new androidx.core.hardware.fingerprint
            .FingerprintManagerCompat.AuthenticationCallback() {

                private void dismissAndForwardResult(final int errMsgId,
                        final CharSequence errString) {
                    mHandler.obtainMessage(FingerprintDialogFragment.MSG_DISMISS_DIALOG_ERROR)
                            .sendToTarget();
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback
                                    .onAuthenticationError(errMsgId, errString);
                        }
                    });
                }

                @Override
                public void onAuthenticationError(final int errMsgId,
                        final CharSequence errString) {
                    if (errMsgId == BiometricPrompt.ERROR_CANCELED) {
                        if (mCanceledFrom == USER_CANCELED_FROM_NONE) {
                            dismissAndForwardResult(errMsgId, errString);
                        }
                    } else if (errMsgId == BiometricPrompt.ERROR_LOCKOUT
                            || errMsgId == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                        dismissAndForwardResult(errMsgId, errString);
                    } else {
                        mHandler.obtainMessage(FingerprintDialogFragment.MSG_SHOW_ERROR, errMsgId,
                                0,
                                errString).sendToTarget();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        mClientAuthenticationCallback.onAuthenticationError(
                                                errMsgId,
                                                errString);
                                    }
                                });
                            }
                        }, FingerprintDialogFragment.HIDE_DIALOG_DELAY);
                    }
                    cleanup();
                }

                @Override
                public void onAuthenticationHelp(final int helpMsgId,
                        final CharSequence helpString) {
                    mHandler.obtainMessage(FingerprintDialogFragment.MSG_SHOW_HELP, helpString)
                            .sendToTarget();
                    // Don't forward the result to the client, since the dialog takes care of it.
                }

                @Override
                public void onAuthenticationSucceeded(final androidx.core.hardware.fingerprint
                        .FingerprintManagerCompat.AuthenticationResult result) {
                    mHandler.obtainMessage(
                            FingerprintDialogFragment.MSG_DISMISS_DIALOG_AUTHENTICATED)
                            .sendToTarget();
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationSucceeded(
                                    new BiometricPrompt.AuthenticationResult(
                                            unwrapCryptoObject(result.getCryptoObject())));
                        }
                    });
                    cleanup();
                }

                @Override
                public void onAuthenticationFailed() {
                    mHandler.obtainMessage(FingerprintDialogFragment.MSG_SHOW_HELP,
                            mContext.getResources().getString(R.string.fingerprint_not_recognized))
                            .sendToTarget();
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationFailed();
                        }
                    });
                }
            };

    /**
     * Creates a new instance of the {@link FingerprintHelperFragment}.
     */
    public static FingerprintHelperFragment newInstance() {
        return new FingerprintHelperFragment();
    }

    /**
     * Sets the crypto object to be associated with the authentication. Should be called before
     * adding the fragment to guarantee that it's ready in onCreate().
     */
    public void setCryptoObject(BiometricPrompt.CryptoObject crypto) {
        mCryptoObject = crypto;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mContext = getContext();

    }

    @Override
    @SuppressWarnings("deprecation")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (!mShowing) {
            mCancellationSignal = new CancellationSignal();
            mCanceledFrom = USER_CANCELED_FROM_NONE;
            androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManagerCompat =
                    androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(mContext);
            if (handlePreAuthenticationErrors(fingerprintManagerCompat)) {
                mHandler.obtainMessage(
                        FingerprintDialogFragment.MSG_DISMISS_DIALOG_ERROR).sendToTarget();
                cleanup();
            } else {
                fingerprintManagerCompat.authenticate(
                        wrapCryptoObject(mCryptoObject),
                        0 /* flags */,
                        mCancellationSignal,
                        mAuthenticationCallback,
                        null /* handler */);
                mShowing = true;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * Sets the client's callback. This should be done whenever the lifecycle changes (orientation
     * changes).
     */
    protected void setCallback(Executor executor,
            BiometricPrompt.AuthenticationCallback callback) {
        mExecutor = executor;
        mClientAuthenticationCallback = callback;
    }

    /**
     * Pass a reference to the handler used by FingerprintDialogFragment to update the UI.
     */
    protected void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Cancel the authentication.
     * @param canceledFrom one of the USER_CANCELED_FROM* constants
     */
    protected void cancel(int canceledFrom) {
        mCanceledFrom = canceledFrom;
        if (canceledFrom == USER_CANCELED_FROM_USER) {
            sendErrorToClient(BiometricPrompt.ERROR_USER_CANCELED);
        }

        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
        cleanup();
    }

    /**
     * Remove the fragment so that resources can be freed.
     */
    void cleanup() {
        mShowing = false;
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().detach(this)
                    .commitAllowingStateLoss();
        }
    }

    /**
     * Check before starting authentication for basic conditions, notifies client and returns true
     * if conditions are not met
     */
    @SuppressWarnings("deprecation")
    private boolean handlePreAuthenticationErrors(androidx.core.hardware.fingerprint
            .FingerprintManagerCompat fingerprintManager) {
        if (!fingerprintManager.isHardwareDetected()) {
            sendErrorToClient(BiometricPrompt.ERROR_HW_UNAVAILABLE);
            return true;
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            sendErrorToClient(BiometricPrompt.ERROR_NO_BIOMETRICS);
            return true;
        }
        return false;
    }

    /**
     * Bypasses the FingerprintManager authentication callback wrapper and sends it directly to the
     * client's callback, since the UI is not even showing yet.
     * @param error
     */
    private void sendErrorToClient(final int error) {
        mClientAuthenticationCallback.onAuthenticationError(error, getErrorString(mContext, error));
    }

    /**
     * Only needs to provide a subset of the fingerprint error strings since the rest are translated
     * in FingerprintManager
     */
    private String getErrorString(Context context, int errorCode) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                return context.getString(R.string.fingerprint_error_hw_not_present);
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                return context.getString(R.string.fingerprint_error_hw_not_available);
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
                return context.getString(R.string.fingerprint_error_no_fingerprints);
            case BiometricPrompt.ERROR_USER_CANCELED:
                return context.getString(R.string.fingerprint_error_user_canceled);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    static BiometricPrompt.CryptoObject unwrapCryptoObject(
            androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new BiometricPrompt.CryptoObject(cryptoObject.getMac());
        } else {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    static androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject
            wrapCryptoObject(BiometricPrompt.CryptoObject cryptoObject) {
        if (cryptoObject == null) {
            return null;
        } else if (cryptoObject.getCipher() != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cryptoObject.getCipher());
        } else if (cryptoObject.getSignature() != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cryptoObject.getSignature());
        } else if (cryptoObject.getMac() != null) {
            return new androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject(
                    cryptoObject.getMac());
        } else {
            return null;
        }
    }
}
