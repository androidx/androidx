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
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private @Nullable FragmentActivity mClientActivity;
    private @Nullable Fragment mClientFragment;
    @VisibleForTesting @Nullable Executor mClientExecutor;
    @VisibleForTesting @Nullable BiometricPrompt.AuthenticationCallback mClientCallback;

    // Set once and retained.
    private BiometricPrompt.CryptoObject mCryptoObject;
    private CharSequence mNegativeButtonText;

    private boolean mShowing;
    private int mCanceledFrom = USER_CANCELED_FROM_NONE;
    private boolean mStartRespectingCancel;

    // This flag is used to control the instant dismissal of the dialog fragment. In the case where
    // the user is already locked out this dialog will not appear. In the case where the user is
    // being locked out for the first time an error message will be displayed on the UI before
    // dismissing.
    private boolean mDismissFingerprintDialogInstantly = true;

    // In Q, we must ignore the first onPause if setDeviceCredentialAllowed is true, since
    // the Q implementation launches ConfirmDeviceCredentialActivity which is an activity and
    // puts the client app onPause.
    private boolean mPausedOnce;

    // Whether this fragment is being hosted in DeviceCredentialHandlerActivity.
    private boolean mIsHandlingDeviceCredential;

    private @Nullable android.hardware.biometrics.BiometricPrompt mBiometricPrompt;
    private @Nullable FingerprintDialogFragment mFingerprintDialog;

    // Do not rely on the application's executor when calling into the framework's code.
    private final Handler mHandler = new Handler(Looper.getMainLooper());
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
                    sendFailureToClient();
                }
            });

    // Also created once and retained.
    private final DialogInterface.OnClickListener mNegativeButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final CharSequence errorText = mNegativeButtonText;
                    mClientCallback.onAuthenticationError(
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
                                TAG, getClientActivity(), mBundle, onLaunch);
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
        maybeInitHandlerBridge(false /* startIgnoringReset */);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isChangingConfigurations()) {
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && isDeviceCredentialAllowed()) {
            // Ignore the first onPause if isDeviceCredentialAllowed is true, since
            // the Q implementation launches ConfirmDeviceCredentialActivity, which puts
            // the client app onPause. Implementations prior to Q instead launch
            // DeviceCredentialHandlerActivity, resulting in the same problem.
            if (!mPausedOnce) {
                mPausedOnce = true;
            } else {
                cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
            }
        } else {
            cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
        }

        maybeHandleDeviceCredentialResult();
        maybeResetHandlerBridge();
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

        mIsHandlingDeviceCredential = info.isHandlingDeviceCredentialResult();
        final FragmentActivity activity = getClientActivity();
        if (info.isDeviceCredentialAllowed() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Launch handler activity to support device credential on older versions.
            if (!mIsHandlingDeviceCredential) {
                launchDeviceCredentialHandler(info);
                return;
            }

            // Fall back to device credential immediately if no biometrics are enrolled.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (activity == null) {
                    Log.e(TAG, "Failed to authenticate with device credential. Activity was null.");
                    return;
                }

                final DeviceCredentialHandlerBridge bridge =
                        DeviceCredentialHandlerBridge.getInstanceIfNotNull();
                if (bridge == null) {
                    Log.e(TAG, "Failed to authenticate with device credential. Bridge was null.");
                    return;
                }

                if (!bridge.isConfirmingDeviceCredential()) {
                    final BiometricManager biometricManager = BiometricManager.from(activity);
                    if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
                        DeviceCredentialLauncher.launchConfirmation(
                                TAG, activity, info.getBundle(), null /* onLaunch */);
                        return;
                    }
                }
            }
        }

        // Don't launch prompt if state has already been saved (potential for state loss).
        final FragmentManager fragmentManager = getClientFragmentManager();
        if (fragmentManager == null || fragmentManager.isStateSaved()) {
            Log.w(TAG, "Not launching prompt. authenticate() called after onSaveInstanceState()");
            return;
        }

        mPausedOnce = false;
        mBundle = info.getBundle();
        mCryptoObject = crypto;

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
            if (!shouldForceFingerprint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                showBiometricPromptForAuthentication();
            } else {
                showFingerprintDialogForAuthentication();
            }
            mShowing = true;
        }
    }

    void cancelAuthentication() {
        // If we launched a device credential handler activity, clean up its fragment.
        if (!mIsHandlingDeviceCredential) {
            final DeviceCredentialHandlerBridge bridge =
                    DeviceCredentialHandlerBridge.getInstanceIfNotNull();
            if (bridge != null && bridge.getBiometricFragment() != null) {
                bridge.getBiometricFragment().cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
            }
        }
        cancel(BiometricFragment.USER_CANCELED_FROM_NONE);
    }

    void cancel(int canceledFrom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
    private void dismiss() {
        if (mFingerprintDialog != null) {
            mFingerprintDialog.dismissSafely();
            mFingerprintDialog = null;
        }

        mShowing = false;
        if (isAdded()) {
            getParentFragmentManager().beginTransaction().detach(this).commitAllowingStateLoss();
        }

        if (!Utils.isConfirmingDeviceCredential()) {
            Utils.maybeFinishHandler(getClientActivity());
        }
    }

    private void sendSuccessAndDismiss(@NonNull final BiometricPrompt.AuthenticationResult result) {
        sendSuccessToClient(result);
        dismiss();
    }

    private void sendSuccessToClient(@NonNull final BiometricPrompt.AuthenticationResult result) {
        mClientExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        mClientCallback.onAuthenticationSucceeded(result);
                    }
                });
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
                    mClientCallback.onAuthenticationError(errorCode, errorString);
                }
            });
        }
    }

    private void sendFailureToClient() {
        mClientExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mClientCallback.onAuthenticationFailed();
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

    /**
     * Launches a copy of this prompt in a transparent {@link DeviceCredentialHandlerActivity}.
     * This allows that activity to intercept and handle activity results from {@link
     * android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence, CharSequence)}.
     */
    private void launchDeviceCredentialHandler(BiometricPrompt.PromptInfo info) {
        final FragmentActivity activity = getClientActivity();
        if (activity == null || activity.isFinishing()) {
            Log.w(TAG, "Failed to start handler activity. Parent activity was null or finishing.");
            return;
        }

        maybeInitHandlerBridge(true /* startIgnoringReset */);

        // Set the handling device credential flag so the new prompt knows not to launch another
        // instance of the handler activity.
        final Bundle infoBundle = info.getBundle();
        infoBundle.putBoolean(BiometricPrompt.KEY_HANDLING_DEVICE_CREDENTIAL_RESULT, true);

        final Intent intent = new Intent(activity, DeviceCredentialHandlerActivity.class);
        intent.putExtra(DeviceCredentialHandlerActivity.EXTRA_PROMPT_INFO_BUNDLE, infoBundle);
        activity.startActivity(intent);
    }

    /**
     * Creates (if necessary) the singleton bridge used for communication between the client-hosted
     * prompt and one hosted by {@link DeviceCredentialHandlerActivity}, and initializes all of the
     * relevant data for the bridge.
     *
     * @param startIgnoringReset Whether the bridge should start ignoring calls to
     *                           {@link DeviceCredentialHandlerBridge#reset()} once initialized.
     */
    private void maybeInitHandlerBridge(boolean startIgnoringReset) {
        // Don't create bridge if DeviceCredentialHandlerActivity isn't needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return;
        }

        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        if (mIsHandlingDeviceCredential) {
            bridge.setBiometricFragment(this);
        } else {
            // If hosted by the client, register the current activity theme to the bridge.
            final FragmentActivity activity = getClientActivity();
            if (activity != null) {
                try {
                    bridge.setClientThemeResId(activity.getPackageManager().getActivityInfo(
                            activity.getComponentName(), 0).getThemeResource());
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Failed to register client theme to bridge", e);
                }
            }
        }
        bridge.setCallback(mClientExecutor, mClientCallback);

        if (startIgnoringReset) {
            bridge.startIgnoringReset();
        }
    }

    /**
     * Checks the handler bridge to see if we've received a result from the confirm device
     * credential Settings activity. If so, handles that result by calling the appropriate
     * authentication callback.
     */
    private void maybeHandleDeviceCredentialResult() {
        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge != null) {
            switch (bridge.getDeviceCredentialResult()) {
                case DeviceCredentialHandlerBridge.RESULT_SUCCESS:
                    // Device credential auth succeeded. This is incompatible with crypto.
                    sendSuccessToClient(
                            new BiometricPrompt.AuthenticationResult(null /* crypto */));
                    bridge.stopIgnoringReset();
                    bridge.reset();
                    dismiss();
                    break;

                case DeviceCredentialHandlerBridge.RESULT_ERROR:
                    // Device credential auth failed. Assume this is due to the user canceling.
                    sendErrorToClient(
                            BiometricConstants.ERROR_USER_CANCELED,
                            getString(R.string.generic_error_user_canceled));
                    bridge.stopIgnoringReset();
                    bridge.reset();
                    dismiss();
                    break;
            }
        }
    }

    /** Cleans up the device credential handler bridge (if it exists) to avoid leaking memory. */
    private void maybeResetHandlerBridge() {
        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge != null) {
            bridge.reset();
        }
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
    private int getHideDialogDelay() {
        Context context = getContext();
        return context != null && DeviceConfig.shouldHideFingerprintDialog(context, Build.MODEL)
                ? 0
                : HIDE_DIALOG_DELAY_MS;
    }
}
