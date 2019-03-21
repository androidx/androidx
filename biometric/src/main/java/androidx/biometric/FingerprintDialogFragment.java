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

import android.app.AlertDialog;
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
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

/**
 * This class implements a custom AlertDialog that prompts the user for fingerprint authentication.
 * This class is not meant to be preserved across process death; for security reasons, the
 * BiometricPromptCompat will automatically dismiss the dialog when the activity is no longer in the
 * foreground.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FingerprintDialogFragment extends DialogFragment {

    private static final String TAG = "FingerprintDialogFragment";
    private static final String KEY_DIALOG_BUNDLE = "SavedBundle";

    /**
     * Error/help message will show for this amount of time.
     * For error messages, the dialog will also be dismissed after this amount of time.
     * Error messages will be propagated back to the application via AuthenticationCallback
     * after this amount of time.
     */
    protected static final int HIDE_DIALOG_DELAY = 2000; // ms

    // Shows a temporary message in the help area
    protected static final int MSG_SHOW_HELP = 1;
    // Show an error in the help area, and dismiss the dialog afterwards
    protected static final int MSG_SHOW_ERROR = 2;
    // Dismisses the authentication dialog
    protected static final int MSG_DISMISS_DIALOG_ERROR = 3;
    // Resets the help message
    protected static final int MSG_RESET_MESSAGE = 4;
    // Dismisses the authentication dialog after success.
    protected static final int MSG_DISMISS_DIALOG_AUTHENTICATED = 5;
    // The amount of time required that this fragment be displayed for in order that
    // we show an error message on top of the UI.
    protected static final int DISPLAYED_FOR_500_MS = 6;

    // States for icon animation
    private static final int STATE_NONE = 0;
    private static final int STATE_FINGERPRINT = 1;
    private static final int STATE_FINGERPRINT_ERROR = 2;
    private static final int STATE_FINGERPRINT_AUTHENTICATED = 3;

    /**
     * Creates a dialog requesting for Fingerprint authentication.
     */
    public static FingerprintDialogFragment newInstance() {
        FingerprintDialogFragment fragment = new FingerprintDialogFragment();
        return fragment;
    }

    final class H extends Handler {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_SHOW_HELP:
                    handleShowHelp((CharSequence) msg.obj);
                    break;
                case MSG_SHOW_ERROR:
                    handleShowError(msg.arg1, (CharSequence) msg.obj);
                    break;
                case MSG_DISMISS_DIALOG_ERROR:
                    handleDismissDialogError();
                    break;
                case MSG_DISMISS_DIALOG_AUTHENTICATED:
                    dismiss();
                    break;
                case MSG_RESET_MESSAGE:
                    handleResetMessage();
                    break;
                case DISPLAYED_FOR_500_MS:
                    mDismissInstantly = false;
                    break;
            }
        }
    }

    private H mHandler = new H();
    private Bundle mBundle;
    private int mErrorColor;
    private int mTextColor;
    private int mLastState;
    private ImageView mFingerprintIcon;
    private TextView mErrorText;

    private Context mContext;
    private Dialog mDialog;
    /**
     * This flag is used to control the instant dismissal of the dialog fragment. In the case where
     * the user is already locked out this dialog will not appear. In the case where the user is
     * being locked out for the first time an error message will be displayed on the UI before
     * dismissing.
     */
    protected boolean mDismissInstantly = true;

    // This should be re-set by the BiometricPromptCompat each time the lifecycle changes.
    DialogInterface.OnClickListener mNegativeButtonListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null && mBundle == null) {
            mBundle = savedInstanceState.getBundle(KEY_DIALOG_BUNDLE);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(mBundle.getCharSequence(BiometricPrompt.KEY_TITLE));

        final View layout = LayoutInflater.from(getContext())
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
                mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mNegativeButtonListener != null) {
                    mNegativeButtonListener.onClick(dialog, which);
                }
            }
        });

        builder.setView(layout);
        mDialog = builder.create();
        return mDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(KEY_DIALOG_BUNDLE, mBundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mErrorColor = getThemedColorFor(android.R.attr.colorError);
        } else {
            mErrorColor = ContextCompat.getColor(mContext, R.color.biometric_error_color);
        }
        mTextColor = getThemedColorFor(android.R.attr.textColorSecondary);
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
        // Remove everything since the fragment is going away.
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        FingerprintHelperFragment fingerprintHelperFragment = (FingerprintHelperFragment)
                getFragmentManager()
                        .findFragmentByTag(BiometricPrompt.FINGERPRINT_HELPER_FRAGMENT_TAG);
        if (fingerprintHelperFragment != null) {
            fingerprintHelperFragment.cancel(FingerprintHelperFragment.USER_CANCELED_FROM_USER);
        }
    }

    public void setBundle(Bundle bundle) {
        mBundle = bundle;
    }

    private int getThemedColorFor(int attr) {
        TypedValue tv = new TypedValue();
        Resources.Theme theme = mContext.getTheme();
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
    protected CharSequence getNegativeButtonText() {
        return mBundle.getCharSequence(BiometricPrompt.KEY_NEGATIVE_TEXT);
    }

    /**
     * Sets the negative button listener.
     * @param listener
     */
    protected void setNegativeButtonListener(DialogInterface.OnClickListener listener) {
        mNegativeButtonListener = listener;
    }

    /**
     * Returns the handler; the handler is used by FingerprintHelperFragment to notify the UI of
     * changes from Fingerprint callbacks.
     * @return
     */
    protected Handler getHandler() {
        return mHandler;
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

    @RequiresApi(21)
    private Drawable getAnimationForTransition(int oldState, int newState) {
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
        return mContext.getDrawable(iconRes);
    }

    private void updateFingerprintIcon(int newState) {
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

    void handleShowHelp(CharSequence msg) {
        updateFingerprintIcon(STATE_FINGERPRINT_ERROR);
        mHandler.removeMessages(MSG_RESET_MESSAGE);
        mErrorText.setTextColor(mErrorColor);
        mErrorText.setText(msg);

        // Reset the text after a delay
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_RESET_MESSAGE), HIDE_DIALOG_DELAY);
    }

    void handleShowError(int errMsgId, CharSequence msg) {
        updateFingerprintIcon(STATE_FINGERPRINT_ERROR);
        mHandler.removeMessages(MSG_RESET_MESSAGE);
        mErrorText.setTextColor(mErrorColor);
        mErrorText.setText(msg);

        // Dismiss the dialog after a delay
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DISMISS_DIALOG_ERROR),
                HIDE_DIALOG_DELAY);
    }

    void dismissAfterDelay() {
        mErrorText.setTextColor(mErrorColor);
        mErrorText.setText(
                R.string.fingerprint_error_lockout);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, HIDE_DIALOG_DELAY);
    }

    void handleDismissDialogError() {
        if (mDismissInstantly) {
            dismiss();
        } else {
            dismissAfterDelay();
        }
        // Always set this to true. In case the user tries to authenticate again the UI will not be
        // shown.
        mDismissInstantly = true;
    }

    void handleResetMessage() {
        updateFingerprintIcon(STATE_FINGERPRINT);
        mErrorText.setTextColor(mTextColor);
        mErrorText.setText(mContext.getString(R.string.fingerprint_dialog_touch_sensor));
    }
}
