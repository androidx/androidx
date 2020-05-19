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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

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
public class BiometricFragment extends Fragment {
    private static final String TAG = "BiometricFragment";

    private static final String FINGERPRINT_DIALOG_FRAGMENT_TAG = "FingerprintDialogFragment";

    private static final int HIDE_DIALOG_DELAY_MS = 2000;

    // In order to keep consistent behavior between versions, we need to send
    // FingerprintDialogFragment a message indicating whether or not to dismiss the UI instantly.
    private static final int DISMISS_INSTANTLY_DELAY_MS = 500;

    // For debugging fingerprint dialog only. Must never be checked in as `true`.
    private static final boolean DEBUG_FORCE_FINGERPRINT = false;

    // Request code used when launching the confirm device credential Settings activity.
    private static final int REQUEST_CONFIRM_CREDENTIAL = 1;

    // Where the dialog was canceled from.
    static final int USER_CANCELED_FROM_NONE = 0;
    static final int USER_CANCELED_FROM_USER = 1;
    static final int USER_CANCELED_FROM_NEGATIVE_BUTTON = 2;

    // Set whenever the support library's authenticate is called.
    private Bundle mBundle;

    // Re-set by the application, through BiometricPromptCompat upon orientation changes.
    private @Nullable FragmentActivity mClientActivity;
    private @Nullable Fragment mClientFragment;
    @VisibleForTesting @Nullable Executor mClientExecutor;
    @VisibleForTesting @Nullable BiometricPrompt.AuthenticationCallback mClientCallback;

    // Set once and retained.
    private BiometricPrompt.CryptoObject mCryptoObject;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    CharSequence mNegativeButtonText;

    private boolean mShowing;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mCanceledFrom = USER_CANCELED_FROM_NONE;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mStartRespectingCancel;

    // This flag is used to control the instant dismissal of the dialog fragment. In the case where
    // the user is already locked out this dialog will not appear. In the case where the user is
    // being locked out for the first time an error message will be displayed on the UI before
    // dismissing.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mDismissFingerprintDialogInstantly = true;

    // In Q, we must ignore the first onPause if setDeviceCredentialAllowed is true, since
    // the Q implementation launches ConfirmDeviceCredentialActivity which is an activity and
    // puts the client app onPause.
    private boolean mPausedOnce;

    private @Nullable android.hardware.biometrics.BiometricPrompt mBiometricPrompt;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable FingerprintDialogFragment mFingerprintDialog;

    // Do not rely on the application's executor when calling into the framework's code.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mPromptExecutor = new Executor() {
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

                    if (!isUsingFingerprintDialog()) {
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
                                sendErrorToClient(errorCode, errorString);
                            }
                            dismiss();
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
                    if (isUsingFingerprintDialog() && mFingerprintDialog != null) {
                        mFingerprintDialog.showHelp(helpMessage);
                    }
                }

                @Override
                public void onFailure() {
                    if (isUsingFingerprintDialog() && mFingerprintDialog != null) {
                        mFingerprintDialog.showHelp(
                                getString(R.string.fingerprint_not_recognized));
                    }
                    sendFailureToClient();
                }
            });

    // Also created once and retained.
    private final DialogInterface.OnClickListener mNegativeButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final CharSequence errorText = mNegativeButtonText != null
                            ? mNegativeButtonText
                            : "";
                    sendErrorToClient(ERROR_NEGATIVE_BUTTON, errorText);
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
                        launchConfirmCredentialActivity();
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
    public void onResume() {
        super.onResume();
        if (mFingerprintDialog != null && mFingerprintDialog.isDetached()) {
            final FragmentManager fragmentManager = getClientFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction().attach(mFingerprintDialog)
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isChangingConfigurations()) {
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && isDeviceCredentialAllowed()) {
            // Ignore the first onPause if isDeviceCredentialAllowed is true, since implementations
            // prior to R launch ConfirmDeviceCredentialActivity, putting the client app onPause.
            if (!mPausedOnce) {
                mPausedOnce = true;
            } else {
                cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
            }
        } else {
            cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            handleConfirmCredentialResult(resultCode);
        }
    }

    void setClientActivity(@Nullable FragmentActivity clientActivity) {
        mClientActivity = clientActivity;
    }

    void setClientFragment(@Nullable Fragment clientFragment) {
        mClientFragment = clientFragment;
    }

    /**
     * Sets the client's callback. This should be done whenever the lifecycle changes (orientation
     * changes).
     */
    void setClientCallback(Executor executor, BiometricPrompt.AuthenticationCallback callback) {
        mClientExecutor = executor;
        mClientCallback = callback;
    }

    void authenticate(
            @NonNull BiometricPrompt.PromptInfo info,
            @Nullable BiometricPrompt.CryptoObject crypto) {

        mBundle = info.getBundle();
        mCryptoObject = crypto;

        // Fall back to device credential immediately if no biometrics are enrolled.
        final FragmentActivity activity = getClientActivity();
        if (info.isDeviceCredentialAllowed()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && activity != null
                && BiometricManager.from(activity).canAuthenticate()
                        != BiometricManager.BIOMETRIC_SUCCESS) {
            launchConfirmCredentialActivity();
            return;
        }

        // Don't launch prompt if state has already been saved (potential for state loss).
        final FragmentManager fragmentManager = getClientFragmentManager();
        if (fragmentManager == null || fragmentManager.isStateSaved()) {
            Log.w(TAG, "Not launching prompt. authenticate() called after onSaveInstanceState()");
            return;
        }

        mPausedOnce = false;

        final Fragment addedFragment = fragmentManager.findFragmentByTag(
                BiometricPrompt.BIOMETRIC_FRAGMENT_TAG);
        if (addedFragment == null) {
            // If the fragment hasn't been added before, add it.
            fragmentManager.beginTransaction().add(this, BiometricPrompt.BIOMETRIC_FRAGMENT_TAG)
                    .commitAllowingStateLoss();
        } else if (isDetached()) {
            // If it's been added before, just re-attach it.
            fragmentManager.beginTransaction().attach(this).commitAllowingStateLoss();
        }

        // For the case when onResume() is being called right after authenticate,
        // we need to make sure that all fragment transactions have been committed.
        fragmentManager.executePendingTransactions();

        if (!mShowing) {
            if (!isUsingFingerprintDialog()) {
                showBiometricPromptForAuthentication();
            } else {
                showFingerprintDialogForAuthentication();
            }
            mShowing = true;
        }
    }

    void cancel(int canceledFrom) {
        if (!isUsingFingerprintDialog()) {
            cancelBiometricPrompt();
        } else {
            cancelFingerprintDialog(canceledFrom);
        }
    }

    void onNegativeButtonPressed(DialogInterface dialog, int which) {
        if (isDeviceCredentialAllowed()) {
            mDeviceCredentialButtonListener.onClick(dialog, which);
        } else {
            mNegativeButtonListener.onClick(dialog, which);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void launchConfirmCredentialActivity() {
        final FragmentActivity activity = getClientActivity();
        if (activity == null) {
            Log.e(TAG, "Failed to check device credential. Client FragmentActivity not found.");
            handleConfirmCredentialResult(Activity.RESULT_CANCELED);
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
        final CharSequence title;
        final CharSequence subtitle;
        final CharSequence description;
        if (mBundle != null) {
            title = mBundle.getCharSequence(BiometricPrompt.KEY_TITLE);
            subtitle = mBundle.getCharSequence(BiometricPrompt.KEY_SUBTITLE);
            description = mBundle.getCharSequence(BiometricPrompt.KEY_DESCRIPTION);
        } else {
            title = null;
            subtitle = null;
            description = null;
        }

        final CharSequence credentialDescription = subtitle != null ? subtitle : description;
        @SuppressWarnings("deprecation")
        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                title, credentialDescription);

        if (intent == null) {
            Log.e(TAG, "Failed to check device credential. Got null intent from Keyguard.");
            handleConfirmCredentialResult(Activity.RESULT_CANCELED);
            return;
        }

        // Dismiss the fingerprint dialog before launching the activity.
        if (isUsingFingerprintDialog()) {
            cancelFingerprintDialog(USER_CANCELED_FROM_NONE);
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
            mBiometricPrompt.authenticate(cancellationSignal, mPromptExecutor, callback);
        } else {
            android.hardware.biometrics.BiometricPrompt.CryptoObject wrappedCryptoObject =
                    Objects.requireNonNull(
                            CryptoObjectUtils.wrapForBiometricPrompt(mCryptoObject));
            mBiometricPrompt.authenticate(
                    wrappedCryptoObject, cancellationSignal, mPromptExecutor, callback);
        }
    }

    @SuppressWarnings("deprecation")
    private void showFingerprintDialogForAuthentication() {
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

        final FragmentManager fragmentManager = getClientFragmentManager();
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
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dismiss() {
        if (mFingerprintDialog != null) {
            mFingerprintDialog.dismissSafely();
            mFingerprintDialog = null;
        }

        mShowing = false;
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).commitAllowingStateLoss();
        }

        FragmentManager fragmentManager = getClientFragmentManager();
        if (fragmentManager != null) {
            final Fragment fingerprintFragment = fragmentManager.findFragmentByTag(
                    FINGERPRINT_DIALOG_FRAGMENT_TAG);
            if (fingerprintFragment != null) {
                fragmentManager.beginTransaction().remove(fingerprintFragment)
                        .commitAllowingStateLoss();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendSuccessAndDismiss(@NonNull BiometricPrompt.AuthenticationResult result) {
        sendSuccessToClient(result);
        dismiss();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendErrorAndDismiss(int errorCode, @NonNull CharSequence errorString) {
        sendErrorToClient(errorCode, errorString);
        dismiss();
    }

    private void sendSuccessToClient(@NonNull final BiometricPrompt.AuthenticationResult result) {
        if (mClientExecutor != null && mClientCallback != null) {
            mClientExecutor.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            mClientCallback.onAuthenticationSucceeded(result);
                        }
                    });
        } else {
            Log.e(TAG, "Unable to send success to client. Client executor or callback was null.");
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendErrorToClient(final int errorCode, @NonNull final CharSequence errorString) {
        if (mClientExecutor != null && mClientCallback != null) {
            mClientExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mClientCallback.onAuthenticationError(errorCode, errorString);
                }
            });
        } else {
            Log.e(TAG, "Unable to send error to client. Client executor or callback was null.");
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendFailureToClient() {
        if (mClientExecutor != null && mClientCallback != null) {
            mClientExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mClientCallback.onAuthenticationFailed();
                }
            });
        } else {
            Log.e(TAG, "Unable to send failure to client. Client executor or callback was null.");
        }
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
    boolean isUsingFingerprintDialog() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.P || shouldForceFingerprint();
    }

    /**
     * Force some devices to fall back to fingerprint in order to support strong (crypto) auth.
     */
    private boolean shouldForceFingerprint() {
        final FragmentActivity activity = getClientActivity();
        return DEBUG_FORCE_FINGERPRINT || (activity != null && mCryptoObject != null
                && DeviceConfig.shouldUseFingerprintForCrypto(
                        activity, Build.MANUFACTURER, Build.MODEL));
    }

    private boolean isDeviceCredentialAllowed() {
        return mBundle != null
                && mBundle.getBoolean(BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL, false);
    }

    /** Checks if the client is currently changing configurations (e.g., screen orientation). */
    private boolean isChangingConfigurations() {
        final FragmentActivity activity = getClientActivity();
        return activity != null && activity.isChangingConfigurations();
    }

    /** Gets the client activity that is hosting the biometric prompt. */
    private @Nullable FragmentActivity getClientActivity() {
        if (mClientActivity != null) {
            return mClientActivity;
        } else if (mClientFragment != null) {
            return mClientFragment.getActivity();
        } else {
            return null;
        }
    }

    /**
     * Gets the appropriate fragment manager for the client. This is either the support fragment
     * manager for a client activity or the child fragment manager for a client fragment.
     */
    private @Nullable FragmentManager getClientFragmentManager() {
        if (mClientActivity != null) {
            return mClientActivity.getSupportFragmentManager();
        } else if (mClientFragment != null) {
            return mClientFragment.getChildFragmentManager();
        } else {
            return null;
        }
    }

    /**
     * @return The effective millisecond delay to wait before hiding the dialog, while respecting
     * the result of {@link DeviceConfig#shouldHideFingerprintDialog(Context, String)}.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int getHideDialogDelay() {
        Context context = getContext();
        return context != null && DeviceConfig.shouldHideFingerprintDialog(context, Build.MODEL)
                ? 0
                : HIDE_DIALOG_DELAY_MS;
    }
}
