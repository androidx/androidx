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

import static androidx.biometric.BiometricConstants.ERROR_NEGATIVE_BUTTON;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * A fragment that wraps the BiometricPrompt and has the ability to continue authentication across
 * device configuration changes. This class is not meant to be preserved after process death; for
 * security reasons, the BiometricPromptCompat will automatically stop authentication when the
 * activity is no longer in the foreground.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("SyntheticAccessor")
public class BiometricFragment extends Fragment {
    private static final String TAG = "BiometricFragment";

    private static final String FINGERPRINT_DIALOG_FRAGMENT_TAG = "FingerprintDialogFragment";

    private static final int HIDE_DIALOG_DELAY_MS = 2000;

    // In order to keep consistent behavior between versions, we need to send
    // FingerprintDialogFragment a message indicating whether or not to dismiss the UI instantly.
    private static final int DISMISS_INSTANTLY_DELAY_MS = 500;

    // For debugging fingerprint dialog only. Must never be checked in as `true`.
    private static final boolean DEBUG_FORCE_FINGERPRINT = false;

    // Where the dialog was canceled from.
    static final int USER_CANCELED_FROM_NONE = 0;
    static final int USER_CANCELED_FROM_USER = 1;
    static final int USER_CANCELED_FROM_NEGATIVE_BUTTON = 2;

    // Set whenever the support library's authenticate is called.
    private Bundle mBundle;

    // Re-set by the application, through BiometricPromptCompat upon orientation changes.
    @VisibleForTesting
    Executor mClientExecutor;
    @VisibleForTesting
    BiometricPrompt.AuthenticationCallback mClientAuthenticationCallback;

    // Set once and retained.
    private BiometricPrompt.CryptoObject mCryptoObject;
    private CharSequence mNegativeButtonText;

    private boolean mShowing;
    private int mCanceledFrom = USER_CANCELED_FROM_NONE;
    private boolean mStartRespectingCancel;

    /**
     * This flag is used to control the instant dismissal of the dialog fragment. In the case where
     * the user is already locked out this dialog will not appear. In the case where the user is
     * being locked out for the first time an error message will be displayed on the UI before
     * dismissing.
     */
    private boolean mDismissFingerprintDialogInstantly = true;

    @Nullable
    private android.hardware.biometrics.BiometricPrompt mBiometricPrompt;
    @Nullable
    private FingerprintDialogFragment mFingerprintDialog;

    // Do not rely on the application's executor when calling into the framework's code.
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            mHandler.post(runnable);
        }
    };

    private final CancellationSignalProvider mCancellationSignalProvider =
            new CancellationSignalProvider();

    // Created once and retained.
    @VisibleForTesting
    final AuthenticationCallbackProvider mCallbackProvider = new AuthenticationCallbackProvider(
            new AuthenticationCallbackProvider.Listener() {
                @Override
                public void onSuccess(@NonNull final BiometricPrompt.AuthenticationResult result) {
                    sendSuccessAndDismiss(result);
                }

                @Override
                public void onError(
                        final int errorCode, @Nullable final CharSequence errorMessage) {

                    // Ensure we're only sending publicly defined errors.
                    final int knownErrorCode = Utils.isUnknownError(errorCode)
                            ? BiometricPrompt.ERROR_VENDOR
                            : errorCode;

                    if (!shouldForceFingerprint()
                            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        final CharSequence errorString = errorMessage != null
                                ? errorMessage
                                : getString(R.string.default_error_msg)
                                        + " "
                                        + errorCode;
                        sendErrorAndDismiss(knownErrorCode, errorString);
                    } else {
                        final CharSequence errorString = errorMessage != null
                                ? errorMessage
                                : Utils.getFingerprintErrorString(getContext(), errorCode);
                        if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                            if (mCanceledFrom == USER_CANCELED_FROM_NONE) {
                                sendErrorAndDismiss(errorCode, errorString);
                            } else if (!Utils.isConfirmingDeviceCredential()) {
                                dismiss();
                            }
                        } else {
                            // Avoid passing a null error string to the client callback.
                            if (mDismissFingerprintDialogInstantly) {
                                sendErrorAndDismiss(knownErrorCode, errorString);
                            } else {
                                mFingerprintDialog.showHelp(errorString);
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendErrorAndDismiss(errorCode, errorMessage);
                                    }
                                }, getHideDialogDelay());
                            }

                            // Always set this to true. In case the user tries to authenticate again
                            // the UI will not be shown.
                            mDismissFingerprintDialogInstantly = true;
                        }
                    }
                }

                @Override
                void onHelp(@Nullable CharSequence helpMessage) {
                    if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.P || shouldForceFingerprint())
                            && mFingerprintDialog != null) {
                        mFingerprintDialog.showHelp(helpMessage);
                    }
                }

                @Override
                public void onFailure() {
                    if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.P || shouldForceFingerprint())
                            && mFingerprintDialog != null) {
                        mFingerprintDialog.showHelp(
                                getString(R.string.fingerprint_not_recognized));
                    }
                    mClientExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationFailed();
                        }
                    });
                }
            });

    // Also created once and retained.
    private final DialogInterface.OnClickListener mNegativeButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final CharSequence errorText = mNegativeButtonText;
                    mClientAuthenticationCallback.onAuthenticationError(
                            ERROR_NEGATIVE_BUTTON, errorText != null ? errorText : "");
                    cancel(BiometricFragment.USER_CANCELED_FROM_NEGATIVE_BUTTON);
                }
            };

    // Also created once and retained.
    private final DialogInterface.OnClickListener mDeviceCredentialButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            Log.e(TAG, "Failed to check device credential. Not supported prior to"
                                    + " API 21.");
                            return;
                        }

                        final Runnable onLaunch;
                        if (!shouldForceFingerprint()
                                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            onLaunch = null;
                        } else {
                            onLaunch = new Runnable() {
                                @Override
                                public void run() {
                                    cancelFingerprintDialog(USER_CANCELED_FROM_NONE);
                                }
                            };
                        }

                        DeviceCredentialLauncher.launchConfirmation(
                                TAG, getActivity(), mBundle, onLaunch);
                    }
                }
            };

    /**
     * Creates a new instance of the {@link BiometricFragment}.
     */
    static BiometricFragment newInstance() {
        return new BiometricFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    @Nullable
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        if (!mShowing && mBundle != null) {
            if (!shouldForceFingerprint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                showBiometricPromptForAuthentication();
            } else {
                showFingerprintDialogForAuthentication();
            }
        }

        mShowing = true;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    void setBundle(@Nullable Bundle bundle) {
        mBundle = bundle;
    }

    /**
     * Sets the client's callback. This should be done whenever the lifecycle changes (orientation
     * changes).
     */
    void setCallback(Executor executor,
            BiometricPrompt.AuthenticationCallback authenticationCallback) {
        mClientExecutor = executor;
        mClientAuthenticationCallback = authenticationCallback;
    }

    /**
     * Sets the crypto object to be associated with the authentication. Should be called before
     * adding the fragment to guarantee that it's ready in onCreate().
     */
    void setCryptoObject(BiometricPrompt.CryptoObject crypto) {
        mCryptoObject = crypto;
    }

    boolean isDeviceCredentialAllowed() {
        return mBundle != null
                && mBundle.getBoolean(BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL, false);
    }

    void cancel(int canceledFrom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cancelBiometricPrompt();
        } else {
            cancelFingerprintDialog(canceledFrom);
        }
    }

    /**
     * @return The effective millisecond delay to wait before hiding the dialog, while respecting
     * the result of {@link DeviceConfig#shouldHideFingerprintDialog(Context, String)}.
     */
    private int getHideDialogDelay() {
        Context context = getContext();
        return context != null && DeviceConfig.shouldHideFingerprintDialog(context, Build.MODEL)
                ? 0
                : HIDE_DIALOG_DELAY_MS;
    }

    /**
     * Cancel the authentication.
     */
    private void cancelBiometricPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isDeviceCredentialAllowed()) {
            if (!mStartRespectingCancel) {
                Log.w(TAG, "Ignoring fast cancel signal");
                return;
            }
        }
        mCancellationSignalProvider.cancel();
        dismiss();
    }

    private void cancelFingerprintDialog(int canceledFrom) {
        mCanceledFrom = canceledFrom;
        if (canceledFrom == USER_CANCELED_FROM_USER) {
            final int errorCode = BiometricPrompt.ERROR_USER_CANCELED;
            sendErrorToClient(errorCode, Utils.getFingerprintErrorString(getContext(), errorCode));
        }
        mCancellationSignalProvider.cancel();
        dismiss();
    }

    /**
     * Remove the fragment so that resources can be freed.
     */
    void dismiss() {
        if (mFingerprintDialog != null) {
            mFingerprintDialog.dismissSafely();
            mFingerprintDialog = null;
        }

        mShowing = false;
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).commitAllowingStateLoss();
        }

        if (!Utils.isConfirmingDeviceCredential()) {
            Utils.maybeFinishHandler(getActivity());
        }
    }

    private void sendSuccessAndDismiss(@NonNull final BiometricPrompt.AuthenticationResult result) {
        mClientExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        mClientAuthenticationCallback.onAuthenticationSucceeded(result);
                    }
                });
        dismiss();
    }

    private void sendErrorAndDismiss(final int errorCode, @NonNull final CharSequence errorString) {
        sendErrorToClient(errorCode, errorString);
        if (!Utils.isConfirmingDeviceCredential()) {
            dismiss();
        }
    }

    private void sendErrorToClient(final int errorCode, @NonNull final CharSequence errorString) {
        if (!Utils.isConfirmingDeviceCredential()) {
            mClientExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mClientAuthenticationCallback.onAuthenticationError(errorCode, errorString);
                }
            });
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressWarnings("deprecation")
    private void showBiometricPromptForAuthentication() {
        final android.hardware.biometrics.BiometricPrompt.Builder builder =
                new android.hardware.biometrics.BiometricPrompt.Builder(getContext());

        final CharSequence title = mBundle.getCharSequence(BiometricPrompt.KEY_TITLE);
        final CharSequence subtitle = mBundle.getCharSequence(BiometricPrompt.KEY_SUBTITLE);
        final CharSequence description = mBundle.getCharSequence(BiometricPrompt.KEY_DESCRIPTION);
        if (title != null) {
            builder.setTitle(title);
        }
        if (subtitle != null) {
            builder.setSubtitle(subtitle);
        }
        if (description != null) {
            builder.setDescription(description);
        }

        final boolean allowDeviceCredential =
                mBundle.getBoolean(BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL);
        final DialogInterface.OnClickListener negativeButtonListener;
        if (allowDeviceCredential && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            mNegativeButtonText = getString(R.string.confirm_device_credential_password);
            negativeButtonListener = mDeviceCredentialButtonListener;
        } else {
            mNegativeButtonText = mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
            negativeButtonListener = mNegativeButtonListener;
        }

        if (!TextUtils.isEmpty(mNegativeButtonText)) {
            builder.setNegativeButton(
                    mNegativeButtonText, mClientExecutor, negativeButtonListener);
        }

        // Set builder flags introduced in Q.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setConfirmationRequired(
                    mBundle.getBoolean(BiometricPrompt.KEY_REQUIRE_CONFIRMATION, true));
            builder.setDeviceCredentialAllowed(allowDeviceCredential);
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && allowDeviceCredential) {
            mStartRespectingCancel = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Ignore cancel signal if it's within the first quarter second.
                    mStartRespectingCancel = true;
                }
            }, 250 /* ms */);
        }

        mBiometricPrompt = builder.build();
        final android.os.CancellationSignal cancellationSignal =
                mCancellationSignalProvider.getBiometricCancellationSignal();
        final android.hardware.biometrics.BiometricPrompt.AuthenticationCallback callback =
                mCallbackProvider.getBiometricCallback();
        if (mCryptoObject == null) {
            mBiometricPrompt.authenticate(cancellationSignal, mExecutor, callback);
        } else {
            android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCryptoObject =
                    Objects.requireNonNull(
                            CryptoObjectUtils.wrapForBiometricPrompt(mCryptoObject));
            mBiometricPrompt.authenticate(
                    wrappedCryptoObject, cancellationSignal, mExecutor, callback);
        }
    }

    @SuppressWarnings("deprecation")
    void showFingerprintDialogForAuthentication() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManagerCompat =
                androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(context);
        final int errorCode = checkForFingerprintPreAuthenticationErrors(fingerprintManagerCompat);
        if (errorCode != 0) {
            sendErrorToClient(errorCode, Utils.getFingerprintErrorString(getContext(), errorCode));
        }

        final FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            final boolean shouldHideFingerprintDialog =
                    DeviceConfig.shouldHideFingerprintDialog(context, Build.MODEL);
            if (mDismissFingerprintDialogInstantly != shouldHideFingerprintDialog) {
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                mDismissFingerprintDialogInstantly = shouldHideFingerprintDialog;
                            }
                        },
                        DISMISS_INSTANTLY_DELAY_MS);
            }

            mFingerprintDialog = FingerprintDialogFragment.newInstance();
            mFingerprintDialog.setBundle(mBundle);
            mFingerprintDialog.show(fragmentManager, FINGERPRINT_DIALOG_FRAGMENT_TAG);

            mCanceledFrom = USER_CANCELED_FROM_NONE;
            fingerprintManagerCompat.authenticate(
                    CryptoObjectUtils.wrapForFingerprintManager(mCryptoObject),
                    0 /* flags */,
                    mCancellationSignalProvider.getFingerprintCancellationSignal(),
                    mCallbackProvider.getFingerprintCallback(),
                    null /* handler */);
        }
    }

    void onNegativeButtonPressed(DialogInterface dialog, int which) {
        final boolean allowDeviceCredential =
                mBundle.getBoolean(BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL);
        if (allowDeviceCredential) {
            mDeviceCredentialButtonListener.onClick(dialog, which);
        } else {
            mNegativeButtonListener.onClick(dialog, which);
        }
    }

    /**
     * Force some devices to fall back to fingerprint in order to support strong (crypto) auth.
     */
    private boolean shouldForceFingerprint() {
        final FragmentActivity activity = getActivity();
        return DEBUG_FORCE_FINGERPRINT || (activity != null && mCryptoObject != null
                && DeviceConfig.shouldUseFingerprintForCrypto(
                        activity, Build.MANUFACTURER, Build.MODEL));
    }

    /**
     * Check before starting authentication for basic conditions, notifies client and returns true
     * if conditions are not met
     */
    @SuppressWarnings("deprecation")
    private static int checkForFingerprintPreAuthenticationErrors(
            androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager) {
        if (!fingerprintManager.isHardwareDetected()) {
            return BiometricPrompt.ERROR_HW_NOT_PRESENT;
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            return BiometricPrompt.ERROR_NO_BIOMETRICS;
        }
        return 0;
    }
}
