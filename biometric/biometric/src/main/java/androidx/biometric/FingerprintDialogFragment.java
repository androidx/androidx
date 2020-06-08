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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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

    // States for the icon animation.
    static final int STATE_NONE = 0;
    static final int STATE_FINGERPRINT = 1;
    static final int STATE_FINGERPRINT_ERROR = 2;
    static final int STATE_FINGERPRINT_AUTHENTICATED = 3;

    @IntDef({
        STATE_NONE,
        STATE_FINGERPRINT,
        STATE_FINGERPRINT_ERROR,
        STATE_FINGERPRINT_AUTHENTICATED
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface State {}

    /**
     * Error/help message will show for this amount of time, unless
     * {@link DeviceConfig#shouldHideFingerprintDialog(Context, String)}} is true.
     *
     * <p>For error messages, the dialog will also be dismissed after this amount of time. Error
     * messages will be propagated back to the application via AuthenticationCallback
     * after this amount of time.
     */
    private static final int MESSAGE_DISPLAY_TIME_MS = 2000;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Handler mHandler = new Handler(Looper.getMainLooper());

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Runnable mResetDialogRunnable = new Runnable() {
        @Override
        public void run() {
            resetDialog();
        }
    };

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    BiometricViewModel mViewModel;

    private int mErrorTextColor;
    private int mNormalTextColor;

    private ImageView mFingerprintIcon;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    TextView mHelpMessageView;

    @NonNull
    static FingerprintDialogFragment newInstance() {
        return new FingerprintDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectViewModel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mErrorTextColor = getThemedColorFor(android.R.attr.colorError);
        } else {
            final Context context = getContext();
            mErrorTextColor = context != null
                    ? ContextCompat.getColor(context, R.color.biometric_error_color)
                    : 0;
        }
        mNormalTextColor = getThemedColorFor(android.R.attr.textColorSecondary);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(mViewModel.getTitle());

        // We have to use builder.getContext() instead of the usual getContext() in order to get
        // the appropriately themed context for this dialog.
        final View layout = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.fingerprint_dialog_layout, null);

        final TextView subtitleView = layout.findViewById(R.id.fingerprint_subtitle);
        final TextView descriptionView = layout.findViewById(R.id.fingerprint_description);

        final CharSequence subtitle = mViewModel.getSubtitle();
        if (TextUtils.isEmpty(subtitle)) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setVisibility(View.VISIBLE);
            subtitleView.setText(subtitle);
        }

        final CharSequence description = mViewModel.getDescription();
        if (TextUtils.isEmpty(description)) {
            descriptionView.setVisibility(View.GONE);
        } else {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionView.setText(description);
        }

        mFingerprintIcon = layout.findViewById(R.id.fingerprint_icon);
        mHelpMessageView = layout.findViewById(R.id.fingerprint_error);

        final CharSequence negativeButtonText =
                mViewModel.isDeviceCredentialAllowed()
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

    private void connectViewModel() {
        final FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        mViewModel = new ViewModelProvider(activity).get(BiometricViewModel.class);

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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateFingerprintIcon(@State int state) {
        // May be null if we're intentionally suppressing the dialog.
        if (mFingerprintIcon == null) {
            return;
        }

        // Devices older than this do not have FP support (and also do not support SVG), so it's
        // fine for this to be a no-op. An error is returned immediately and the dialog is not
        // shown.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @State final int previousState = mViewModel.getFingerprintDialogPreviousState();

            Drawable icon = getAnimationForTransition(previousState, state);
            if (icon == null) {
                return;
            }

            final AnimatedVectorDrawable animation = icon instanceof AnimatedVectorDrawable
                    ? (AnimatedVectorDrawable) icon
                    : null;

            mFingerprintIcon.setImageDrawable(icon);
            if (animation != null && shouldAnimateForTransition(previousState, state)) {
                animation.start();
            }

            mViewModel.setFingerprintDialogPreviousState(state);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateHelpMessageColor(@State int state) {
        if (mHelpMessageView != null) {
            final boolean isError = state == STATE_FINGERPRINT_ERROR;
            mHelpMessageView.setTextColor(isError ? mErrorTextColor : mNormalTextColor);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateHelpMessageText(@Nullable CharSequence helpMessage) {
        if (mHelpMessageView != null) {
            mHelpMessageView.setText(helpMessage);
        }
    }

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

    private int getThemedColorFor(int attr) {
        final Context context = getContext();
        final FragmentActivity activity = getActivity();
        if (context == null || activity == null) {
            Log.w(TAG, "Unable to get themed color. Context or activity is null.");
            return 0;
        }

        TypedValue tv = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, tv, true /* resolveRefs */);
        TypedArray arr = activity.obtainStyledAttributes(tv.data, new int[] {attr});

        final int color = arr.getColor(0 /* index */, 0 /* defValue */);
        arr.recycle();
        return color;
    }

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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private Drawable getAnimationForTransition(@State int previousState, @State int state) {
        final Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Unable to get animation. Context is null.");
            return null;
        }

        int iconRes;
        if (previousState == STATE_NONE && state == STATE_FINGERPRINT) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (previousState == STATE_FINGERPRINT && state == STATE_FINGERPRINT_ERROR) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (previousState == STATE_FINGERPRINT_ERROR && state == STATE_FINGERPRINT) {
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        } else if (previousState == STATE_FINGERPRINT
                && state == STATE_FINGERPRINT_AUTHENTICATED) {
            // TODO(b/77328470): add animation when fingerprint is authenticated
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        } else {
            return null;
        }

        return ContextCompat.getDrawable(context, iconRes);
    }
}
