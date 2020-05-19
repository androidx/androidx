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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

/**
 * This class implements a custom AlertDialog that prompts the user for fingerprint authentication.
 * This class is not meant to be preserved across process death; for security reasons, the
 * BiometricPromptCompat will automatically dismiss the dialog when the activity is no longer in the
 * foreground.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FingerprintDialogFragment extends DialogFragment {
    private static final String TAG = "FingerprintDialogFrag";
    private static final String KEY_DIALOG_BUNDLE = "SavedBundle";

    /**
     * Error/help message will show for this amount of time, unless
     * {@link DeviceConfig#shouldHideFingerprintDialog(Context, String)}} is true.
     *
     * <p>For error messages, the dialog will also be dismissed after this amount of time. Error
     * messages will be propagated back to the application via AuthenticationCallback
     * after this amount of time.
     */
    private static final int MESSAGE_DISPLAY_TIME_MS = 2000;

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

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Bundle mBundle;
    private int mErrorColor;
    private int mTextColor;
    private int mLastState;
    private ImageView mFingerprintIcon;
    private TextView mErrorText;

    @NonNull
    static FingerprintDialogFragment newInstance() {
        return new FingerprintDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                final BiometricFragment biometricFragment = getBiometricFragment();
                if (biometricFragment != null) {
                    biometricFragment.onNegativeButtonPressed(dialog, which);
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
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        final BiometricFragment biometricFragment = getBiometricFragment();
        if (biometricFragment != null) {
            biometricFragment.cancel(BiometricFragment.USER_CANCELED_FROM_USER);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_DIALOG_BUNDLE, mBundle);
    }

    /** Attempts to dismiss this fragment while avoiding potential crashes. */
    void dismissSafely() {
        if (isAdded()) {
            dismissAllowingStateLoss();
        } else {
            Log.w(TAG, "Failed to dismiss fingerprint dialog fragment.");
        }
    }

    void setBundle(@NonNull Bundle bundle) {
        mBundle = bundle;
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

    void showHelp(CharSequence helpMessage) {
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void resetMessage() {
        updateFingerprintIcon(STATE_FINGERPRINT);

        // May be null if we're intentionally suppressing the dialog.
        if (mErrorText != null) {
            mErrorText.setTextColor(mTextColor);
            mErrorText.setText(getString(R.string.fingerprint_dialog_touch_sensor));
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable BiometricFragment getBiometricFragment() {
        return isAdded()
                ? (BiometricFragment) getParentFragmentManager().findFragmentByTag(
                        BiometricPrompt.BIOMETRIC_FRAGMENT_TAG)
                : null;
    }
}
