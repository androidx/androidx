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

package androidx.biometric.internal;

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

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.R;
import androidx.biometric.utils.AuthenticatorUtils;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A fragment that provides a standard prompt UI for fingerprint authentication on versions prior
 * to Android 9.0 (API 28).
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FingerprintDialogFragment extends DialogFragment {
    private static final String TAG = "FingerprintFragment";

    private static final String ARG_HOST_ACTIVITY = "host_activity";

    /**
     * The dialog has not been initialized.
     */
    static final int STATE_NONE = 0;

    /**
     * Waiting for the user to authenticate with fingerprint.
     */
    static final int STATE_FINGERPRINT = 1;

    /**
     * An error or failure occurred during fingerprint authentication.
     */
    static final int STATE_FINGERPRINT_ERROR = 2;

    /**
     * The user has successfully authenticated with fingerprint.
     */
    static final int STATE_FINGERPRINT_AUTHENTICATED = 3;

    /**
     * A possible state for the fingerprint dialog.
     */
    @IntDef({
        STATE_NONE,
        STATE_FINGERPRINT,
        STATE_FINGERPRINT_ERROR,
        STATE_FINGERPRINT_AUTHENTICATED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface State {}

    /**
     * Transient errors and help messages will be displayed on the dialog for this amount of time.
     */
    private static final int MESSAGE_DISPLAY_TIME_MS = 2000;

    /**
     * A handler used to post delayed events.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * A runnable that resets the dialog to its default state and appearance.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Runnable mResetDialogRunnable = new Runnable() {
        @Override
        public void run() {
            resetDialog();
        }
    };

    /**
     * The view model for the ongoing authentication session.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    BiometricViewModel mViewModel;

    /**
     * The text color used for displaying error messages.
     */
    private int mErrorTextColor;

    /**
     * The text color used for displaying help messages.
     */
    private int mNormalTextColor;

    /**
     * An icon shown on the dialog during authentication.
     */
    private @Nullable ImageView mFingerprintIcon;

    /**
     * Help text shown below the fingerprint icon on the dialog.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable TextView mHelpMessageView;

    /**
     * Creates a new instance of {@link FingerprintDialogFragment}.
     *
     * @return A {@link FingerprintDialogFragment}.
     */
    static @NonNull FingerprintDialogFragment newInstance(boolean hostedInActivity) {
        final FingerprintDialogFragment fragment = new FingerprintDialogFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_HOST_ACTIVITY, hostedInActivity);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean isHostedInActivity() {
        return getArguments().getBoolean(ARG_HOST_ACTIVITY, true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectViewModel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mErrorTextColor = getThemedColorFor(Api26Impl.getColorErrorAttr());
        } else {
            final Context context = getContext();
            mErrorTextColor = context != null
                    ? ContextCompat.getColor(context, R.color.biometric_error_color)
                    : 0;
        }
        mNormalTextColor = getThemedColorFor(android.R.attr.textColorSecondary);
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(mViewModel.getTitle());

        // We have to use builder.getContext() instead of the usual getContext() in order to get
        // the appropriately themed context for this dialog.
        final View layout = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.fingerprint_dialog_layout, null);

        final TextView subtitleView = layout.findViewById(R.id.fingerprint_subtitle);
        if (subtitleView != null) {
            final CharSequence subtitle = mViewModel.getSubtitle();
            if (TextUtils.isEmpty(subtitle)) {
                subtitleView.setVisibility(View.GONE);
            } else {
                subtitleView.setVisibility(View.VISIBLE);
                subtitleView.setText(subtitle);
            }
        }

        final TextView descriptionView = layout.findViewById(R.id.fingerprint_description);
        if (descriptionView != null) {
            final CharSequence description = mViewModel.getDescription();
            if (TextUtils.isEmpty(description)) {
                descriptionView.setVisibility(View.GONE);
            } else {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(description);
            }
        }

        mFingerprintIcon = layout.findViewById(R.id.fingerprint_icon);
        mHelpMessageView = layout.findViewById(R.id.fingerprint_error);

        final CharSequence negativeButtonText =
                AuthenticatorUtils.isDeviceCredentialAllowed(mViewModel.getAllowedAuthenticators())
                        ? getString(R.string.confirm_device_credential_password)
                        : mViewModel.getNegativeButtonText();
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mViewModel.setNegativeButtonPressPending(true);
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
        mViewModel.setFingerprintDialogPreviousState(STATE_NONE);
        mViewModel.setFingerprintDialogState(STATE_FINGERPRINT);
        mViewModel.setFingerprintDialogHelpMessage(
                getString(R.string.fingerprint_dialog_touch_sensor));
    }

    // TODO(b/178855209): Move to AuthenticationInternalHelper
    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        mViewModel.setFingerprintDialogCancelPending(true);
    }

    /**
     * Connects the {@link BiometricViewModel} for the ongoing authentication session to this
     * fragment.
     */
    private void connectViewModel() {
        mViewModel = BiometricPrompt.getViewModel(this, isHostedInActivity());

        mViewModel.getFingerprintDialogState().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@State Integer state) {
                mHandler.removeCallbacks(mResetDialogRunnable);
                updateFingerprintIcon(state);
                updateHelpMessageColor(state);
                mHandler.postDelayed(mResetDialogRunnable, MESSAGE_DISPLAY_TIME_MS);
            }
        });

        mViewModel.getFingerprintDialogHelpMessage().observe(this, new Observer<CharSequence>() {
            @Override
            public void onChanged(CharSequence helpMessage) {
                mHandler.removeCallbacks(mResetDialogRunnable);
                updateHelpMessageText(helpMessage);
                mHandler.postDelayed(mResetDialogRunnable, MESSAGE_DISPLAY_TIME_MS);
            }
        });
    }

    /**
     * Updates the fingerprint icon to match the new dialog state, including animating between
     * states if necessary.
     *
     * @param state The new state for the fingerprint dialog.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateFingerprintIcon(@State int state) {
        // May be null if we're intentionally suppressing the dialog.
        if (mFingerprintIcon == null) {
            return;
        }

        @State final int previousState = mViewModel.getFingerprintDialogPreviousState();

        Drawable icon = getAssetForTransition(previousState, state);
        if (icon == null) {
            return;
        }

        mFingerprintIcon.setImageDrawable(icon);
        if (shouldAnimateForTransition(previousState, state)) {
            if (icon instanceof AnimatedVectorDrawable) {
                ((AnimatedVectorDrawable) icon).start();
            }
        }

        mViewModel.setFingerprintDialogPreviousState(state);
    }

    /**
     * Updates the color of the help message text to match the new dialog state.
     *
     * @param state The new state for the fingerprint dialog.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateHelpMessageColor(@State int state) {
        if (mHelpMessageView != null) {
            final boolean isError = state == STATE_FINGERPRINT_ERROR;
            mHelpMessageView.setTextColor(isError ? mErrorTextColor : mNormalTextColor);
        }
    }

    /**
     * Changes the help message text shown on the dialog.
     *
     * @param helpMessage The new help message text for the dialog.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateHelpMessageText(@Nullable CharSequence helpMessage) {
        if (mHelpMessageView != null) {
            mHelpMessageView.setText(helpMessage);
        }
    }

    /**
     * Resets the appearance of the dialog to its initial state (i.e. waiting for authentication).
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void resetDialog() {
        final Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Not resetting the dialog. Context is null.");
            return;
        }

        mViewModel.setFingerprintDialogState(STATE_FINGERPRINT);
        mViewModel.setFingerprintDialogHelpMessage(
                context.getString(R.string.fingerprint_dialog_touch_sensor));
    }

    /**
     * Gets the theme color corresponding to a given style attribute.
     *
     * @param attr The desired attribute.
     * @return The theme color for that attribute.
     */
    private int getThemedColorFor(int attr) {
        final Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Unable to get themed color. Context or activity is null.");
            return 0;
        }

        TypedValue tv = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, tv, true /* resolveRefs */);
        TypedArray arr = context.obtainStyledAttributes(tv.data, new int[] {attr});

        final int color = arr.getColor(0 /* index */, 0 /* defValue */);
        arr.recycle();
        return color;
    }

    /**
     * Checks if the fingerprint icon should animate when transitioning between dialog states.
     *
     * @param previousState The previous state for the fingerprint dialog.
     * @param state The new state for the fingerprint dialog.
     * @return Whether the fingerprint icon should animate.
     */
    private boolean shouldAnimateForTransition(@State int previousState, @State int state) {
        if (previousState == STATE_NONE && state == STATE_FINGERPRINT) {
            return false;
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_ERROR) {
            return true;
        } else if (previousState == STATE_FINGERPRINT_ERROR && state == STATE_FINGERPRINT) {
            return true;
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            return false;
        }
        return false;
    }

    /**
     * Gets the icon or animation asset that should appear when transitioning between dialog states.
     *
     * @param previousState The previous state for the fingerprint dialog.
     * @param state The new state for the fingerprint dialog.
     * @return A drawable asset to be used for the fingerprint icon.
     */
    private Drawable getAssetForTransition(@State int previousState, @State int state) {
        final Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Unable to get asset. Context is null.");
            return null;
        }

        int iconRes;
        if (previousState == STATE_NONE && state == STATE_FINGERPRINT) {
            iconRes = R.drawable.fingerprint_dialog_fp_icon;
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_ERROR) {
            iconRes = R.drawable.fingerprint_dialog_error;
        } else if (previousState == STATE_FINGERPRINT_ERROR && state == STATE_FINGERPRINT) {
            iconRes = R.drawable.fingerprint_dialog_fp_icon;
        } else if (previousState == STATE_FINGERPRINT
                && state == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            iconRes = R.drawable.fingerprint_dialog_fp_icon;
        } else {
            return null;
        }

        return ContextCompat.getDrawable(context, iconRes);
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 8.0 (API 26).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private static class Api26Impl {
        // Prevent instantiation.
        private Api26Impl() {}

        /**
         * Gets the resource ID of the {@code colorError} style attribute.
         */
        static int getColorErrorAttr() {
            return androidx.appcompat.R.attr.colorError;
        }
    }
}
