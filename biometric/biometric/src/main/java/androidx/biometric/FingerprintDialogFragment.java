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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * This class implements a custom AlertDialog that prompts the user for fingerprint authentication.
 * This class is not meant to be preserved across process death; for security reasons, the
 * BiometricPromptCompat will automatically dismiss the dialog when the activity is no longer in the
 * foreground.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("SyntheticAccessor")
public class FingerprintDialogFragment extends DialogFragment {
    private static final String TAG = "FingerprintDialogFrag";
    private static final String KEY_DIALOG_BUNDLE = "SavedBundle";

    // Where the dialog was canceled from.
    static final int USER_CANCELED_FROM_NONE = 0;
    static final int USER_CANCELED_FROM_USER = 1;
    static final int USER_CANCELED_FROM_NEGATIVE_BUTTON = 2;

    /**
     * Error/help message will show for this amount of time, unless
     * {@link DeviceConfig#shouldHideFingerprintDialog(Context, String)}} is true.
     *
     * <p>For error messages, the dialog will also be dismissed after this amount of time. Error
     * messages will be propagated back to the application via AuthenticationCallback
     * after this amount of time.
     */
    private static final int MESSAGE_DISPLAY_TIME_MS = 2000;

    // In order to keep consistent behavior between versions, we need to send
    // FingerprintDialogFragment a message indicating whether or not to dismiss the UI instantly.
    private static final int DISMISS_INSTANTLY_DELAY_MS = 500;

    // States for icon animation
    private static final int STATE_NONE = 0;
    private static final int STATE_FINGERPRINT = 1;
    private static final int STATE_FINGERPRINT_ERROR = 2;
    private static final int STATE_FINGERPRINT_AUTHENTICATED = 3;

    private final Runnable mResetMessageRunnable = new Runnable() {
        @Override
        public void run() {
            resetMessage();
        }
    };

    private final CancellationSignalProvider mCancellationSignalProvider =
            new CancellationSignalProvider();

    // Created once and retained.
    @VisibleForTesting
    final AuthenticationCallbackProvider mCallbackProvider = new AuthenticationCallbackProvider(
            new AuthenticationCallbackProvider.Listener() {
                @Override
                void onSuccess(@NonNull final BiometricPrompt.AuthenticationResult result) {
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationSucceeded(result);
                        }
                    });
                    dismissSafely();
                }

                @Override
                void onError(int errorCode, @Nullable CharSequence errorMessage) {
                    if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                        if (mCanceledFrom == USER_CANCELED_FROM_NONE) {
                            sendErrorToClient(errorCode, errorMessage);
                        }
                        dismissSafely();
                    } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT
                            || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                        dismissAndForwardError(errorCode, errorMessage);
                    } else {
                        // Ensure we're only sending publicly defined errors.
                        final int dialogErrMsgId = Utils.isUnknownError(errorCode)
                                ? BiometricPrompt.ERROR_VENDOR : errorCode;
                        dismissAndForwardError(dialogErrMsgId, errorMessage);
                    }
                }

                @Override
                void onHelp(@Nullable CharSequence helpMessage) {
                    showHelp(helpMessage);
                }

                @Override
                void onFailure() {
                    showHelp(getString(R.string.fingerprint_not_recognized));
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            mClientAuthenticationCallback.onAuthenticationFailed();
                        }
                    });
                }
            });

    // Created once and retained.
    private final DialogInterface.OnClickListener mDeviceCredentialButtonListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            Log.e(TAG, "Failed to check device credential."
                                    + " Not supported prior to L.");
                            return;
                        }

                        DeviceCredentialLauncher.launchConfirmation(
                                TAG, FingerprintDialogFragment.this.getActivity(), mBundle,
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        // Dismiss the fingerprint dialog without forwarding errors.
                                        FingerprintDialogFragment.this.onCancel(dialog);
                                    }
                                });
                    }
                }
            };

    // Re-set by the application, through BiometricPromptCompat upon orientation changes.
    @VisibleForTesting
    Executor mExecutor;

    @VisibleForTesting
    BiometricPrompt.AuthenticationCallback mClientAuthenticationCallback;

    // This should be re-set by the BiometricPromptCompat each time the lifecycle changes.
    @VisibleForTesting
    DialogInterface.OnClickListener mNegativeButtonListener;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    // Set once and retained.
    private boolean mShowing;
    private BiometricPrompt.CryptoObject mCryptoObject;

    // Created once and retained.
    private int mCanceledFrom;

    private Bundle mBundle;
    private int mErrorColor;
    private int mTextColor;
    private int mLastState;
    private ImageView mFingerprintIcon;
    private TextView mErrorText;

    /**
     * This flag is used to control the instant dismissal of the dialog fragment. In the case where
     * the user is already locked out this dialog will not appear. In the case where the user is
     * being locked out for the first time an error message will be displayed on the UI before
     * dismissing.
     */
    private boolean mDismissInstantly = true;

    @NonNull
    static FingerprintDialogFragment newInstance() {
        return new FingerprintDialogFragment();
    }

    @Override
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
        mHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        mDismissInstantly = DeviceConfig.shouldHideFingerprintDialog(
                                context, Build.MODEL);
                    }
                },
                DISMISS_INSTANTLY_DELAY_MS);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        final Context context = getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mErrorColor = getThemedColorFor(android.R.attr.colorError);
        } else {
            mErrorColor = context != null
                    ? ContextCompat.getColor(context, R.color.biometric_error_color)
                    : 0;
        }
        mTextColor = getThemedColorFor(android.R.attr.textColorSecondary);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && mBundle == null) {
            mBundle = savedInstanceState.getBundle(KEY_DIALOG_BUNDLE);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(mBundle.getCharSequence(BiometricPrompt.KEY_TITLE));

        // We have to use builder.getContext() instead of the usual getContext() in order to get
        // the appropriately themed context for this dialog.
        final View layout = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.fingerprint_dialog_layout, null);

        final TextView subtitleView = layout.findViewById(R.id.fingerprint_subtitle);
        final TextView descriptionView = layout.findViewById(R.id.fingerprint_description);

        final CharSequence subtitle = mBundle.getCharSequence(
                BiometricPrompt.KEY_SUBTITLE);
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setVisibility(View.VISIBLE);
            subtitleView.setText(subtitle);
        }

        final CharSequence description = mBundle.getCharSequence(
                BiometricPrompt.KEY_DESCRIPTION);
        if (TextUtils.isEmpty(description)) {
            descriptionView.setVisibility(View.GONE);
        } else {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionView.setText(description);
        }

        mFingerprintIcon = layout.findViewById(R.id.fingerprint_icon);
        mErrorText = layout.findViewById(R.id.fingerprint_error);

        final CharSequence negativeButtonText =
                isDeviceCredentialAllowed()
                        ? getString(R.string.confirm_device_credential_password)
                        : mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (FingerprintDialogFragment.this.isDeviceCredentialAllowed()) {
                    mDeviceCredentialButtonListener.onClick(dialog, which);
                } else if (mNegativeButtonListener != null) {
                    mNegativeButtonListener.onClick(dialog, which);
                } else {
                    Log.w(TAG, "No suitable negative button listener.");
                }
            }
        });

        builder.setView(layout);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    @SuppressWarnings("deprecation")
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        if (!mShowing && context != null) {
            mCanceledFrom = USER_CANCELED_FROM_NONE;
            androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManagerCompat =
                    androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(context);
            final int errorCode = checkForPreAuthenticationErrors(fingerprintManagerCompat);
            if (errorCode != 0) {
                dismissAndForwardError(errorCode, getErrorString(errorCode));
            } else {
                fingerprintManagerCompat.authenticate(
                        CryptoObjectUtils.wrapForFingerprintManager(mCryptoObject),
                        0 /* flags */,
                        mCancellationSignalProvider.getFingerprintCancellationSignal(),
                        mCallbackProvider.getFingerprintCallback(),
                        null /* handler */);
                mShowing = true;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLastState = STATE_NONE;
        updateFingerprintIcon(STATE_FINGERPRINT);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_DIALOG_BUNDLE, mBundle);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        reportCancellation(USER_CANCELED_FROM_USER);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        FragmentActivity activity = getActivity();
        if (activity != null && !activity.isChangingConfigurations()) {
            mShowing = false;
            if (!Utils.isConfirmingDeviceCredential()) {
                Utils.maybeFinishHandler(activity);
            }
        }
    }

    /**
     * Cancel the authentication.
     *
     * @param canceledFrom one of the USER_CANCELED_FROM* constants
     */
    void cancel(int canceledFrom) {
        reportCancellation(canceledFrom);
        dismissSafely();
    }

    /** Attempts to dismiss this fragment while avoiding potential crashes. */
    private void dismissSafely() {
        if (isAdded()) {
            dismissAllowingStateLoss();
        } else {
            Log.e(TAG, "Failed to dismiss fingerprint dialog fragment. Fragment manager was null.");
        }
    }

    private void showErrorAndDismissAfterDelay(final int errorCode,
            final @Nullable CharSequence errorMessage) {

        mHandler.removeCallbacks(mResetMessageRunnable);
        updateFingerprintIcon(STATE_FINGERPRINT_ERROR);

        // May be null if we're intentionally suppressing the dialog.
        if (mErrorText != null) {
            mErrorText.setTextColor(mErrorColor);
            mErrorText.setText(errorMessage);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendErrorToClient(errorCode, errorMessage);
                FingerprintDialogFragment.this.dismissSafely();
            }
        }, getHideDialogDelay(getContext()));
    }

    void setBundle(@NonNull Bundle bundle) {
        mBundle = bundle;
    }

    void setNegativeButtonListener(DialogInterface.OnClickListener listener) {
        mNegativeButtonListener = listener;
    }

    /**
     * Sets the client's callback. This should be done whenever the lifecycle changes (orientation
     * changes).
     */
    void setCallback(Executor executor, BiometricPrompt.AuthenticationCallback callback) {
        mExecutor = executor;
        mClientAuthenticationCallback = callback;
    }

    /**
     * Sets the crypto object to be associated with the authentication. Should be called before
     * adding the fragment to guarantee that it's ready in onCreate().
     */
    void setCryptoObject(BiometricPrompt.CryptoObject crypto) {
        mCryptoObject = crypto;
    }

    @VisibleForTesting
    void setHandler(Handler handler) {
        mHandler = handler;
    }

    @VisibleForTesting
    boolean isShowing() {
        return mShowing;
    }

    /**
     * Check before starting authentication for basic conditions, notifies client and returns true
     * if conditions are not met
     */
    @SuppressWarnings("deprecation")
    private int checkForPreAuthenticationErrors(
            androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager) {
        if (!fingerprintManager.isHardwareDetected()) {
            return BiometricPrompt.ERROR_HW_NOT_PRESENT;
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            return BiometricPrompt.ERROR_NO_BIOMETRICS;
        }
        return 0;
    }

    private void reportCancellation(int canceledFrom) {
        mCanceledFrom = canceledFrom;
        if (canceledFrom == USER_CANCELED_FROM_USER) {
            final int errorCode = BiometricPrompt.ERROR_USER_CANCELED;
            sendErrorToClient(errorCode, getErrorString(errorCode));
        }
        mCancellationSignalProvider.cancel();
    }

    private void updateFingerprintIcon(int newState) {
        // May be null if we're intentionally suppressing the dialog.
        if (mFingerprintIcon == null) {
            return;
        }

        // Devices older than this do not have FP support (and also do not support SVG), so it's
        // fine for this to be a no-op. An error is returned immediately and the dialog is not
        // shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Drawable icon = getAnimationForTransition(mLastState, newState);
            if (icon == null) {
                return;
            }

            final AnimatedVectorDrawable animation = icon instanceof AnimatedVectorDrawable
                    ? (AnimatedVectorDrawable) icon
                    : null;

            mFingerprintIcon.setImageDrawable(icon);
            if (animation != null && shouldAnimateForTransition(mLastState, newState)) {
                animation.start();
            }

            mLastState = newState;
        }
    }

    private void showHelp(CharSequence helpMessage) {
        updateFingerprintIcon(STATE_FINGERPRINT_ERROR);
        mHandler.removeCallbacks(mResetMessageRunnable);

        // May be null if we're intentionally suppressing the dialog.
        if (mErrorText != null) {
            mErrorText.setTextColor(mErrorColor);
            mErrorText.setText(helpMessage);
        }

        // Reset the text after a delay
        mHandler.postDelayed(mResetMessageRunnable, MESSAGE_DISPLAY_TIME_MS);
    }

    private void dismissAndForwardError(int errorCode, @Nullable CharSequence errorMessage) {
        // Avoid passing a null error string to the client callback.
        if (mDismissInstantly) {
            sendErrorToClient(errorCode, errorMessage);
            dismissSafely();
        } else {
            showErrorAndDismissAfterDelay(errorCode, errorMessage);
        }

        // Always set this to true. In case the user tries to authenticate again the UI will not be
        // shown.
        mDismissInstantly = true;
    }

    private void resetMessage() {
        updateFingerprintIcon(STATE_FINGERPRINT);

        // May be null if we're intentionally suppressing the dialog.
        if (mErrorText != null) {
            mErrorText.setTextColor(mTextColor);
            mErrorText.setText(getString(R.string.fingerprint_dialog_touch_sensor));
        }
    }

    /**
     * Bypasses the FingerprintManager authentication callback wrapper and sends it directly to the
     * client's callback, since the UI is not even showing yet.
     *
     * @param errorCode The error code that will be sent to the client.
     */
    private void sendErrorToClient(final int errorCode, @Nullable CharSequence errorMessage) {
        if (!Utils.isConfirmingDeviceCredential()) {
            final CharSequence errorString = errorMessage != null
                    ? errorMessage
                    : getErrorString(errorCode);
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mClientAuthenticationCallback.onAuthenticationError(errorCode, errorString);
                }
            });
        }
    }

    private int getThemedColorFor(int attr) {
        final Context context = getContext();
        final FragmentActivity activity = getActivity();
        if (context == null || activity == null) {
            return 0;
        }

        TypedValue tv = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, tv, true /* resolveRefs */);
        TypedArray arr = getActivity().obtainStyledAttributes(tv.data, new int[] {attr});

        final int color = arr.getColor(0 /* index */, 0 /* defValue */);
        arr.recycle();
        return color;
    }

    /**
     * The negative button text is persisted in the fragment, not in BiometricPromptCompat. Since
     * the dialog persists through rotation, this allows us to return this as the error text for
     * ERROR_NEGATIVE_BUTTON.
     */
    @Nullable
    protected CharSequence getNegativeButtonText() {
        return mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
    }

    /**
     * Only needs to provide a subset of the fingerprint error strings since the rest are translated
     * in FingerprintManager
     */
    @NonNull
    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                return getString(R.string.fingerprint_error_hw_not_present);
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                return getString(R.string.fingerprint_error_hw_not_available);
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
                return getString(R.string.fingerprint_error_no_fingerprints);
            case BiometricPrompt.ERROR_USER_CANCELED:
                return getString(R.string.fingerprint_error_user_canceled);
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                return getString(R.string.fingerprint_error_lockout);
            default:
                Log.e(TAG, "Unknown error code: " + errorCode);
                return getString(R.string.default_error_msg);
        }
    }

    private boolean isDeviceCredentialAllowed() {
        return mBundle.getBoolean(BiometricPrompt.KEY_ALLOW_DEVICE_CREDENTIAL);
    }

    private boolean shouldAnimateForTransition(int oldState, int newState) {
        if (oldState == STATE_NONE && newState == STATE_FINGERPRINT) {
            return false;
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_ERROR) {
            return true;
        } else if (oldState == STATE_FINGERPRINT_ERROR && newState == STATE_FINGERPRINT) {
            return true;
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            return false;
        }
        return false;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private Drawable getAnimationForTransition(int oldState, int newState) {
        final Context context = getContext();
        if (context == null) {
            return null;
        }

        int iconRes;
        if (oldState == STATE_NONE && newState == STATE_FINGERPRINT) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (oldState == STATE_FINGERPRINT && newState == STATE_FINGERPRINT_ERROR) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (oldState == STATE_FINGERPRINT_ERROR && newState == STATE_FINGERPRINT) {
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        } else if (oldState == STATE_FINGERPRINT
                && newState == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        } else {
            return null;
        }

        return ContextCompat.getDrawable(context, iconRes);
    }

    /**
     * @return The effective millisecond delay to wait before hiding the dialog, while respecting
     * the result of {@link DeviceConfig#shouldHideFingerprintDialog(Context, String)}.
     */
    private static int getHideDialogDelay(Context context) {
        return context != null && DeviceConfig.shouldHideFingerprintDialog(context, Build.MODEL)
                ? 0
                : MESSAGE_DISPLAY_TIME_MS;
    }
}
