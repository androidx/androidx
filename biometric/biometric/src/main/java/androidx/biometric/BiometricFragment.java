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

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
public class BiometricFragment extends Fragment {
    private static final String TAG = "BiometricFragment";

    // Where the dialog was canceled from.
    static final int CANCELED_FROM_NONE = 0;
    static final int CANCELED_FROM_USER = 1;
    static final int CANCELED_FROM_NEGATIVE_BUTTON = 2;

    @IntDef({CANCELED_FROM_NONE, CANCELED_FROM_USER, CANCELED_FROM_NEGATIVE_BUTTON})
    @Retention(RetentionPolicy.SOURCE)
    @interface CanceledFrom {}

    private static final String FINGERPRINT_DIALOG_FRAGMENT_TAG =
            "androidx.biometric.FingerprintDialogFragment";

    private static final int HIDE_DIALOG_DELAY_MS = 2000;

    // In order to keep consistent behavior between versions, we need to send
    // FingerprintDialogFragment a message indicating whether or not to dismiss the UI instantly.
    private static final int DISMISS_INSTANTLY_DELAY_MS = 500;

    // For debugging fingerprint dialog only. Must never be checked in as `true`.
    private static final boolean DEBUG_FORCE_FINGERPRINT = false;

    // Request code used when launching the confirm device credential Settings activity.
    private static final int REQUEST_CONFIRM_CREDENTIAL = 1;

    // Do not rely on the application's executor when calling into the framework's code.
    @VisibleForTesting Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mPromptExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            mHandler.post(runnable);
        }
    };

    @VisibleForTesting BiometricViewModel mViewModel;

    /**
     * Creates a new instance of the {@link BiometricFragment}.
     */
    static BiometricFragment newInstance() {
        return new BiometricFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectViewModel();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            cancelAuthentication(BiometricFragment.CANCELED_FROM_NONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            mViewModel.setConfirmingDeviceCredential(false);
            handleConfirmCredentialResult(resultCode);
        }
    }

    private void connectViewModel() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mViewModel = new ViewModelProvider(getActivity()).get(BiometricViewModel.class);

        mViewModel.getAuthenticationResult().observe(
                this,
                new Observer<BiometricPrompt.AuthenticationResult>() {
                    @Override
                    public void onChanged(
                            BiometricPrompt.AuthenticationResult authenticationResult) {
                        if (authenticationResult != null) {
                            onAuthenticationSucceeded(authenticationResult);
                            mViewModel.setAuthenticationResult(null);
                        }
                    }
                });

        mViewModel.getAuthenticationError().observe(
                this,
                new Observer<BiometricErrorData>() {
                    @Override
                    public void onChanged(BiometricErrorData authenticationError) {
                        if (authenticationError != null) {
                            onAuthenticationError(
                                    authenticationError.getErrorCode(),
                                    authenticationError.getErrorMessage());
                            mViewModel.setAuthenticationError(null);
                        }
                    }
                });

        mViewModel.getAuthenticationHelpMessage().observe(
                this,
                new Observer<CharSequence>() {
                    @Override
                    public void onChanged(CharSequence authenticationHelpMessage) {
                        if (authenticationHelpMessage != null) {
                            onAuthenticationHelp(authenticationHelpMessage);
                            mViewModel.setAuthenticationError(null);
                        }
                    }
                });

        mViewModel.isAuthenticationFailurePending().observe(
                this,
                new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean authenticationFailurePending) {
                        if (authenticationFailurePending) {
                            onAuthenticationFailed();
                            mViewModel.setAuthenticationFailurePending(false);
                        }
                    }
                });

        mViewModel.isNegativeButtonPressPending().observe(
                this,
                new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean negativeButtonPressPending) {
                        if (negativeButtonPressPending) {
                            if (isManagingDeviceCredentialButton()) {
                                onDeviceCredentialButtonPressed();
                            } else {
                                onNegativeButtonPressed();
                            }
                            mViewModel.setNegativeButtonPressPending(false);
                        }
                    }
                });

        mViewModel.isFingerprintDialogCancelPending().observe(
                this,
                new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean fingerprintDialogCancelPending) {
                        if (fingerprintDialogCancelPending) {
                            cancelAuthentication(BiometricFragment.CANCELED_FROM_USER);
                            dismiss();
                            mViewModel.setFingerprintDialogCancelPending(false);
                        }
                    }
                });
    }

    void authenticate(
            @NonNull BiometricPrompt.PromptInfo info,
            @Nullable BiometricPrompt.CryptoObject crypto) {

        final FragmentActivity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Not launching prompt. Client activity was null.");
            return;
        }

        mViewModel.setPromptInfo(info);
        mViewModel.setCryptoObject(crypto);
        if (isManagingDeviceCredentialButton()) {
            mViewModel.setNegativeButtonText(
                    getString(R.string.confirm_device_credential_password));
        } else {
            // Don't override the negative button text from the client.
            mViewModel.setNegativeButtonText(null);
        }

        // Fall back to device credential immediately if no biometrics are enrolled.
        if (mViewModel.isDeviceCredentialAllowed()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && BiometricManager.from(activity).canAuthenticate()
                        != BiometricManager.BIOMETRIC_SUCCESS) {
            mViewModel.setAwaitingResult(true);
            launchConfirmCredentialActivity();
            return;
        }

        if (!mViewModel.isPromptShowing()) {
            if (getContext() == null) {
                Log.w(TAG, "Not showing biometric prompt. Context is null.");
                return;
            }

            mViewModel.setPromptShowing(true);
            mViewModel.setAwaitingResult(true);
            if (isUsingFingerprintDialog()) {
                showFingerprintDialogForAuthentication();
            } else {
                showBiometricPromptForAuthentication();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void showFingerprintDialogForAuthentication() {
        final Context context = requireContext();
        androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManagerCompat =
                androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(context);
        final int errorCode = checkForFingerprintPreAuthenticationErrors(fingerprintManagerCompat);
        if (errorCode != 0) {
            sendErrorAndDismiss(
                    errorCode, Utils.getFingerprintErrorString(getContext(), errorCode));
            return;
        }

        if (isAdded()) {
            final boolean shouldHideFingerprintDialog =
                    DeviceConfig.shouldHideFingerprintDialog(context, Build.MODEL);
            if (mViewModel.isFingerprintDialogDismissedInstantly() != shouldHideFingerprintDialog) {
                mHandler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                mViewModel.setFingerprintDialogDismissedInstantly(
                                        shouldHideFingerprintDialog);
                            }
                        },
                        DISMISS_INSTANTLY_DELAY_MS);
            }

            final FingerprintDialogFragment dialog = FingerprintDialogFragment.newInstance();
            dialog.show(getParentFragmentManager(), FINGERPRINT_DIALOG_FRAGMENT_TAG);

            mViewModel.setCanceledFrom(CANCELED_FROM_NONE);
            fingerprintManagerCompat.authenticate(
                    CryptoObjectUtils.wrapForFingerprintManager(mViewModel.getCryptoObject()),
                    0 /* flags */,
                    mViewModel.getCancellationSignalProvider().getFingerprintCancellationSignal(),
                    mViewModel.getAuthenticationCallbackProvider().getFingerprintCallback(),
                    null /* handler */);
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressWarnings("deprecation")
    private void showBiometricPromptForAuthentication() {
        final Context context = requireContext();
        final android.hardware.biometrics.BiometricPrompt.Builder builder =
                new android.hardware.biometrics.BiometricPrompt.Builder(context);

        final CharSequence title = mViewModel.getTitle();
        final CharSequence subtitle = mViewModel.getSubtitle();
        final CharSequence description = mViewModel.getDescription();
        if (title != null) {
            builder.setTitle(title);
        }
        if (subtitle != null) {
            builder.setSubtitle(subtitle);
        }
        if (description != null) {
            builder.setDescription(description);
        }

        final CharSequence negativeButtonText = mViewModel.getNegativeButtonText();
        if (!TextUtils.isEmpty(negativeButtonText)) {
            builder.setNegativeButton(
                    negativeButtonText,
                    mViewModel.getClientExecutor(),
                    mViewModel.getNegativeButtonListener());
        }

        // Set builder flags introduced in Q.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setConfirmationRequired(mViewModel.isConfirmationRequired());
            builder.setDeviceCredentialAllowed(mViewModel.isDeviceCredentialAllowed());
        }

        final android.hardware.biometrics.BiometricPrompt biometricPrompt = builder.build();
        final android.os.CancellationSignal cancellationSignal =
                mViewModel.getCancellationSignalProvider().getBiometricCancellationSignal();
        final android.hardware.biometrics.BiometricPrompt.AuthenticationCallback callback =
                mViewModel.getAuthenticationCallbackProvider().getBiometricCallback();
        final BiometricPrompt.CryptoObject cryptoObject = mViewModel.getCryptoObject();
        if (mViewModel.getCryptoObject() == null) {
            biometricPrompt.authenticate(cancellationSignal, mPromptExecutor, callback);
        } else {
            android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCryptoObject =
                    Objects.requireNonNull(
                            CryptoObjectUtils.wrapForBiometricPrompt(cryptoObject));
            biometricPrompt.authenticate(
                    wrappedCryptoObject, cancellationSignal, mPromptExecutor, callback);
        }
    }

    void cancelAuthentication(@CanceledFrom int canceledFrom) {
        if (isUsingFingerprintDialog()) {
            mViewModel.setCanceledFrom(canceledFrom);
            if (canceledFrom == CANCELED_FROM_USER) {
                final int errorCode = BiometricPrompt.ERROR_USER_CANCELED;
                sendErrorToClient(
                        errorCode, Utils.getFingerprintErrorString(getContext(), errorCode));
            }
        }
        mViewModel.getCancellationSignalProvider().cancel();
    }

    /**
     * Remove the fragment so that resources can be freed.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dismiss() {
        mViewModel.setPromptShowing(false);
        dismissFingerprintDialog();
        if (!mViewModel.isConfirmingDeviceCredential() && isAdded()) {
            getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

    private void dismissFingerprintDialog() {
        mViewModel.setPromptShowing(false);
        if (isAdded()) {
            final FragmentManager fragmentManager = getParentFragmentManager();
            final FingerprintDialogFragment fingerprintDialog =
                    (FingerprintDialogFragment) fragmentManager.findFragmentByTag(
                            FINGERPRINT_DIALOG_FRAGMENT_TAG);
            if (fingerprintDialog != null) {
                if (fingerprintDialog.isAdded()) {
                    fingerprintDialog.dismissAllowingStateLoss();
                } else {
                    fragmentManager.beginTransaction().remove(fingerprintDialog)
                            .commitAllowingStateLoss();
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @VisibleForTesting
    void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        sendSuccessAndDismiss(result);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @VisibleForTesting
    void onAuthenticationError(final int errorCode, @Nullable CharSequence errorMessage) {
        // Ensure we're only sending publicly defined errors.
        final int knownErrorCode = Utils.isUnknownError(errorCode)
                ? BiometricPrompt.ERROR_VENDOR
                : errorCode;

        if (isUsingFingerprintDialog()) {
            // Avoid passing a null error string to the client callback.
            final CharSequence errorString = errorMessage != null
                    ? errorMessage
                    : Utils.getFingerprintErrorString(getContext(), errorCode);

            if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                // User-initiated cancellation errors should already be handled.
                if (mViewModel.getCanceledFrom() == CANCELED_FROM_NONE) {
                    sendErrorToClient(errorCode, errorString);
                }
                dismiss();
            } else {
                if (mViewModel.isFingerprintDialogDismissedInstantly()) {
                    sendErrorAndDismiss(knownErrorCode, errorString);
                } else {
                    showFingerprintErrorMessage(errorString);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendErrorAndDismiss(errorCode, errorString);
                        }
                    }, getHideDialogDelay());
                }

                // Always set this to true. In case the user tries to authenticate again
                // the UI will not be shown.
                mViewModel.setFingerprintDialogDismissedInstantly(true);
            }
        } else {
            final CharSequence errorString = errorMessage != null
                    ? errorMessage
                    : getString(R.string.default_error_msg)
                            + " "
                            + errorCode;
            sendErrorAndDismiss(knownErrorCode, errorString);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAuthenticationHelp(@Nullable CharSequence helpMessage) {
        if (isUsingFingerprintDialog()) {
            showFingerprintErrorMessage(helpMessage);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAuthenticationFailed() {
        if (isUsingFingerprintDialog()) {
            showFingerprintErrorMessage(getString(R.string.fingerprint_not_recognized));
        }
        sendFailureToClient();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onDeviceCredentialButtonPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "Failed to check device credential. Not supported prior to API 21.");
            return;
        }
        launchConfirmCredentialActivity();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onNegativeButtonPressed() {
        final BiometricPrompt.PromptInfo info = mViewModel.getPromptInfo();
        final CharSequence errorText = info != null
                ? info.getNegativeButtonText()
                : null;
        sendErrorAndDismiss(
                ERROR_NEGATIVE_BUTTON,
                errorText != null
                        ? errorText
                        : getString(R.string.default_error_msg));
        cancelAuthentication(BiometricFragment.CANCELED_FROM_NEGATIVE_BUTTON);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void launchConfirmCredentialActivity() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            Log.e(TAG, "Failed to check device credential. Client FragmentActivity not found.");
            return;
        }

        // Get the KeyguardManager service in whichever way the platform supports.
        final KeyguardManager keyguardManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = activity.getSystemService(KeyguardManager.class);
        } else {
            final Object service = activity.getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager = service instanceof KeyguardManager ? (KeyguardManager) service : null;
        }

        if (keyguardManager == null) {
            Log.e(TAG, "Failed to check device credential. KeyguardManager not found.");
            handleConfirmCredentialResult(Activity.RESULT_CANCELED);
            return;
        }

        // Pass along the title and subtitle/description from the biometric prompt.
        final CharSequence title = mViewModel.getTitle();
        final CharSequence subtitle = mViewModel.getSubtitle();
        final CharSequence description = mViewModel.getDescription();
        final CharSequence credentialDescription = subtitle != null ? subtitle : description;

        @SuppressWarnings("deprecation")
        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                title, credentialDescription);

        if (intent == null) {
            Log.e(TAG, "Failed to check device credential. Got null intent from Keyguard.");
            handleConfirmCredentialResult(Activity.RESULT_CANCELED);
            return;
        }

        mViewModel.setConfirmingDeviceCredential(true);

        // Dismiss the fingerprint dialog before launching the activity.
        if (isUsingFingerprintDialog()) {
            dismissFingerprintDialog();
        }

        // Launch a new instance of the confirm device credential Settings activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivityForResult(intent, REQUEST_CONFIRM_CREDENTIAL);
    }

    private void handleConfirmCredentialResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            // Device credential auth succeeded. This is incompatible with crypto.
            sendSuccessAndDismiss(
                    new BiometricPrompt.AuthenticationResult(null /* crypto */));
        } else {
            // Device credential auth failed. Assume this is due to the user canceling.
            sendErrorAndDismiss(
                    BiometricConstants.ERROR_USER_CANCELED,
                    getString(R.string.generic_error_user_canceled));
        }
    }

    private void showFingerprintErrorMessage(@Nullable CharSequence errorMessage) {
        final CharSequence helpMessage = errorMessage != null
                ? errorMessage
                : getString(R.string.default_error_msg);
        mViewModel.setFingerprintDialogState(FingerprintDialogFragment.STATE_FINGERPRINT_ERROR);
        mViewModel.setFingerprintDialogHelpMessage(helpMessage);
    }

    private void sendSuccessAndDismiss(@NonNull BiometricPrompt.AuthenticationResult result) {
        sendSuccessToClient(result);
        dismiss();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendErrorAndDismiss(int errorCode, @NonNull CharSequence errorString) {
        sendErrorToClient(errorCode, errorString);
        dismiss();
    }

    private void sendSuccessToClient(@NonNull final BiometricPrompt.AuthenticationResult result) {
        if (!mViewModel.isAwaitingResult()) {
            Log.w(TAG, "Success not sent to client. Client is not awaiting a result.");
            return;
        }

        mViewModel.setAwaitingResult(false);
        mViewModel.getClientExecutor().execute(
                new Runnable() {
                    @Override
                    public void run() {
                        mViewModel.getClientCallback().onAuthenticationSucceeded(result);
                    }
                });
    }

    private void sendErrorToClient(final int errorCode, @NonNull final CharSequence errorString) {
        if (mViewModel.isConfirmingDeviceCredential()) {
            Log.v(TAG, "Error not sent to client. User is confirming their device credential.");
            return;
        }

        if (!mViewModel.isAwaitingResult()) {
            Log.w(TAG, "Error not sent to client. Client is not awaiting a result.");
            return;
        }

        mViewModel.setAwaitingResult(false);
        mViewModel.getClientExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mViewModel.getClientCallback().onAuthenticationError(errorCode, errorString);
            }
        });
    }

    private void sendFailureToClient() {
        if (!mViewModel.isAwaitingResult()) {
            Log.w(TAG, "Failure not sent to client. Client is not awaiting a result.");
            return;
        }

        mViewModel.getClientExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mViewModel.getClientCallback().onAuthenticationFailed();
            }
        });
    }

    /**
     * Check for possible error conditions prior to starting authentication.
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isManagingDeviceCredentialButton() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && mViewModel.isDeviceCredentialAllowed();
    }

    private boolean isUsingFingerprintDialog() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.P || shouldForceFingerprint();
    }

    /**
     * Force some devices to fall back to fingerprint in order to support strong (crypto) auth.
     */
    private boolean shouldForceFingerprint() {
        final FragmentActivity activity = getActivity();
        return DEBUG_FORCE_FINGERPRINT || (activity != null && mViewModel.getCryptoObject() != null
                && DeviceConfig.shouldUseFingerprintForCrypto(
                        activity, Build.MANUFACTURER, Build.MODEL));
    }

    /** Checks if the client is currently changing configurations (e.g., screen orientation). */
    private boolean isChangingConfigurations() {
        final FragmentActivity activity = getActivity();
        return activity != null && activity.isChangingConfigurations();
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
}
