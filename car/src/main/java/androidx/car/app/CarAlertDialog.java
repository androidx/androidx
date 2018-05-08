/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.app;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.R;

/**
 * A subclass of {@link Dialog} that is tailored for the car environment. This dialog can display a
 * title text, body text, and up to two buttons -- a positive and negative button. There is no
 * affordance for displaying a custom view or list of content, differentiating it from a regular
 * {@code AlertDialog}.
 */
public class CarAlertDialog extends Dialog {
    private final CharSequence mTitle;
    private final CharSequence mBody;
    private final CharSequence mPositiveButtonText;
    private final OnClickListener mPositiveButtonListener;
    private final CharSequence mNegativeButtonText;
    private final OnClickListener mNegativeButtonListener;

    private final int mTopPadding;
    private final int mButtonPanelTopMargin;
    private final int mBottomPadding;
    private final int mButtonMinWidth;
    private final int mButtonSpacing;

    private View mContentView;
    private TextView mTitleView;
    private TextView mBodyView;

    private View mButtonPanel;
    private Button mPositiveButton;
    private Button mNegativeButton;
    private ButtonPanelTouchDelegate mButtonPanelTouchDelegate;

    private CarAlertDialog(Context context, Builder builder) {
        super(context, getDialogTheme(context));

        mTitle = builder.mTitle;
        mBody = builder.mBody;
        mPositiveButtonText = builder.mPositiveButtonText;
        mPositiveButtonListener = builder.mPositiveButtonListener;
        mNegativeButtonText = builder.mNegativeButtonText;
        mNegativeButtonListener = builder.mNegativeButtonListener;

        Resources res = context.getResources();
        mTopPadding = res.getDimensionPixelSize(R.dimen.car_padding_4);
        mButtonPanelTopMargin = res.getDimensionPixelSize(R.dimen.car_padding_2);
        mBottomPadding = res.getDimensionPixelSize(R.dimen.car_padding_4);
        mButtonMinWidth = res.getDimensionPixelSize(R.dimen.car_button_min_width);
        mButtonSpacing = res.getDimensionPixelSize(R.dimen.car_padding_4);
    }

    @Override
    public void setTitle(CharSequence title) {
        // Ideally this method should be private; the dialog should only be modifiable through the
        // Builder. Unfortunately, this method is defined with the Dialog itself and is public.
        // So, throw an error if this method is ever called. setTitleInternal() should be used
        // to set the title within this class.
        throw new UnsupportedOperationException("Title should only be set from the Builder");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setContentView(R.layout.car_alert_dialog);

        initializeViews();

        setBody(mBody);
        setPositiveButton(mPositiveButtonText);
        setNegativeButton(mNegativeButtonText);
        // setTitleInternal() should be called last because we want to center title and adjust
        // padding depending on body/button configuration.
        setTitleInternal(mTitle);
    }

    private void setTitleInternal(CharSequence title) {
        boolean hasTitle = !TextUtils.isEmpty(title);
        boolean hasBody = mBodyView.getVisibility() == View.VISIBLE;
        boolean hasButton = mButtonPanel.getVisibility() == View.VISIBLE;

        mTitleView.setText(title);
        mTitleView.setVisibility(hasTitle ? View.VISIBLE : View.GONE);

        // If there's a title, then remove the padding at the top of the content view.
        int topPadding = hasTitle ? 0 : mTopPadding;

        // If there is only title, also remove the padding at the bottom so title is
        // vertically centered.
        int bottomPadding = !hasButton && !hasBody ? 0 : mContentView.getPaddingBottom();
        mContentView.setPaddingRelative(
                mContentView.getPaddingStart(),
                topPadding,
                mContentView.getPaddingEnd(),
                bottomPadding);
    }

    private void setBody(CharSequence body) {
        mBodyView.setText(body);
        mBodyView.setVisibility(TextUtils.isEmpty(body) ? View.GONE : View.VISIBLE);
        updateButtonPanelTopMargin();
    }

    private void setPositiveButton(CharSequence text) {
        boolean showButton = !TextUtils.isEmpty(text);

        mPositiveButton.setText(text);
        mPositiveButton.setVisibility(showButton ? View.VISIBLE : View.GONE);

        updateTargetTargetForButton(mPositiveButton);
        updateButtonPanelVisibility();
        updateButtonSpacing();
    }

    private void setNegativeButton(CharSequence text) {
        mNegativeButton.setText(text);
        mNegativeButton.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);

        updateTargetTargetForButton(mNegativeButton);
        updateButtonPanelVisibility();
        updateButtonSpacing();
    }

    /**
     * Updates the top margin of the button panel depending on if there's body text. If there is,
     * then separate the body text from the button panel with margin specified by
     * {@link #mButtonPanelTopMargin}. Otherwise, no margin.
     */
    private void updateButtonPanelTopMargin() {
        boolean hasBody = mBodyView.getVisibility() == View.VISIBLE;

        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) mButtonPanel.getLayoutParams();

        // Separate the action panel with a top margin if there's body text. Otherwise, do not have
        // any margin.
        layoutParams.topMargin = hasBody ? mButtonPanelTopMargin : 0;

        mButtonPanel.requestLayout();
    }

    /**
     * Checks if the given view that represents a positive or negative button currently meets the
     * minimum touch target size that is dictated by {@link #mButtonMinWidth}. If it does not, then
     * this method will utilize a {@link TouchDelegate} to expand the touch target size of that
     * button.
     *
     * @param button One of {@link #mPositiveButton} or {@link #mNegativeButton}.
     */
    private void updateTargetTargetForButton(View button) {
        if (button != mPositiveButton && button != mNegativeButton) {
            throw new IllegalArgumentException("Method must be passed one of mPositiveButton or "
                    + "mNegativeButton");
        }

        if (button.getVisibility() != View.VISIBLE) {
            return;
        }

        // The TouchDelegate needs to be set after the panel has been laid out in order to get the
        // hit Rect.
        mButtonPanel.post(() -> {
            Rect rect = new Rect();
            button.getHitRect(rect);

            int hitWidth = Math.abs(rect.right - rect.left);

            TouchDelegate touchDelegate = null;

            // If the button does not meet the minimum requirements for touch target size, then
            // expand its hit area with a TouchDelegate.
            if (hitWidth < mButtonMinWidth) {
                int amountToIncrease = (mButtonMinWidth - hitWidth) / 2;
                rect.left -= amountToIncrease;
                rect.right += amountToIncrease;

                touchDelegate = new TouchDelegate(rect, button);
            }

            if (button == mPositiveButton) {
                mButtonPanelTouchDelegate.setPositiveButtonDelegate(touchDelegate);
            } else {
                mButtonPanelTouchDelegate.setNegativeButtonDelegate(touchDelegate);
            }
        });
    }

    /**
     * Checks if spacing should be added between the positive and negative button. The spacing is
     * only needed if both buttons are visible.
     */
    private void updateButtonSpacing() {
        int marginEnd;

        // If both buttons are visible, then there needs to be spacing between them.
        if ((mPositiveButton.getVisibility() == View.VISIBLE
                && mNegativeButton.getVisibility() == View.VISIBLE)) {
            marginEnd = mButtonSpacing;
        } else {
            marginEnd = 0;
        }

        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) mPositiveButton.getLayoutParams();
        layoutParams.setMarginEnd(marginEnd);
        mPositiveButton.requestLayout();
    }

    /**
     * Toggles whether or not the panel containing the action buttons are visible depending on if
     * a button should be shown.
     */
    private void updateButtonPanelVisibility() {
        boolean hasButtons = mPositiveButton.getVisibility() == View.VISIBLE
                || mNegativeButton.getVisibility() == View.VISIBLE;

        int visibility = hasButtons ? View.VISIBLE : View.GONE;

        // Visibility is already correct, so nothing further needs to be done.
        if (mButtonPanel.getVisibility() == visibility) {
            return;
        }

        mButtonPanel.setVisibility(visibility);

        // If there are buttons, then remove the padding at the bottom of the content view.
        int buttonPadding = hasButtons ? 0 : mBottomPadding;
        mContentView.setPaddingRelative(
                mContentView.getPaddingStart(),
                mContentView.getPaddingTop(),
                mContentView.getPaddingEnd(),
                buttonPadding);
    }

    /**
     * Initializes the views within the dialog that are modifiable based on the data that has been
     * set on it. Also responsible for hooking up listeners for button clicks.
     */
    private void initializeViews() {
        Window window = getWindow();

        mContentView = window.findViewById(R.id.content_view);
        mTitleView = window.findViewById(R.id.title);
        mBodyView = window.findViewById(R.id.body);

        mButtonPanel = window.findViewById(R.id.button_panel);
        mButtonPanelTouchDelegate = new ButtonPanelTouchDelegate(mButtonPanel);
        mButtonPanel.setTouchDelegate(mButtonPanelTouchDelegate);

        mPositiveButton = window.findViewById(R.id.positive_button);
        mNegativeButton = window.findViewById(R.id.negative_button);

        mPositiveButton.setOnClickListener(v -> onPositiveButtonClick());
        mNegativeButton.setOnClickListener(v -> onNegativeButtonClick());
    }

    /** Delegates to a listener on the positive button if it exists or dismisses the dialog. */
    private void onPositiveButtonClick() {
        if (mPositiveButtonListener != null) {
            mPositiveButtonListener.onClick(/* dialog= */ this, BUTTON_POSITIVE);
        } else {
            dismiss();
        }
    }

    /** Delegates to a listener on the negative button if it exists or dismisses the dialog. */
    private void onNegativeButtonClick() {
        if (mNegativeButtonListener != null) {
            mNegativeButtonListener.onClick(/* dialog= */ this, BUTTON_NEGATIVE);
        } else {
            dismiss();
        }
    }

    /**
     * A composite {@link TouchDelegate} for a button panel that has two buttons. It can hold
     * multiple {@code TouchDelegate}s and will delegate out touch events to each.
     */
    private static final class ButtonPanelTouchDelegate extends TouchDelegate {
        @Nullable private TouchDelegate mPositiveButtonDelegate;
        @Nullable private TouchDelegate mNegativeButtonDelegate;

        ButtonPanelTouchDelegate(View view) {
            super(new Rect(), view);
        }

        void setPositiveButtonDelegate(@Nullable TouchDelegate delegate) {
            mPositiveButtonDelegate = delegate;
        }

        void setNegativeButtonDelegate(@Nullable TouchDelegate delegate) {
            mNegativeButtonDelegate = delegate;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            boolean result = false;
            float x = event.getX();
            float y = event.getY();
            event.setLocation(x, y);

            if (mPositiveButtonDelegate != null) {
                result = mPositiveButtonDelegate.onTouchEvent(event);
            }

            if (mNegativeButtonDelegate != null) {
                result |= mNegativeButtonDelegate.onTouchEvent(event);
            }

            return result;
        }
    }

    /**
     * Returns the style that has been assigned to {@code carDialogTheme} in the
     * current theme that is inflating this dialog.
     */
    private static int getDialogTheme(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.carDialogTheme, outValue, true);
        return outValue.resourceId;
    }

    /**
     * Builder class that can be used to create a {@link CarAlertDialog} by configuring the options
     * for what shows up in the resulting dialog.
     */
    public static final class Builder {
        private final Context mContext;

        private CharSequence mTitle;
        private CharSequence mBody;
        private CharSequence mPositiveButtonText;
        private OnClickListener mPositiveButtonListener;
        private CharSequence mNegativeButtonText;
        private OnClickListener mNegativeButtonListener;

        private boolean mCancelable = true;
        private OnCancelListener mOnCancelListener;
        private OnDismissListener mOnDismissListener;

        /**
         * Creates a new instance of the {@code Builder}.
         *
         * @param context The {@code Context} that the dialog is to be created in.
         */
        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Sets the main title of the dialog to be the given string resource.
         *
         * @param titleId The resource id of the string to be used as the title.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setTitle(@StringRes int titleId) {
            mTitle = mContext.getString(titleId);
            return this;
        }

        /**
         * Sets the main title of the dialog for be the given string.
         *
         * @param title The string to be used as the title.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the body text of the dialog to be the given string resource.
         *
         * @param bodyId The resource id of the string to be used as the body text.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setBody(@StringRes int bodyId) {
            mBody = mContext.getString(bodyId);
            return this;
        }

        /**
         * Sets the body text of the dialog to be the given string.
         *
         * @param body The string to be used as the body text.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setBody(CharSequence body) {
            mBody = body;
            return this;
        }

        /**
         * Sets the text of the positive button and the listener that will be invoked when the
         * button is pressed. If a listener is not provided, then the dialog will dismiss itself
         * when the positive button is clicked.
         *
         * <p>The positive button should be used to accept and continue with the action (e.g.
         * an "OK" action).
         *
         * @param textId The resource id of the string to be used for the positive button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setPositiveButton(@StringRes int textId,
                @Nullable OnClickListener listener) {
            mPositiveButtonText = mContext.getString(textId);
            mPositiveButtonListener = listener;
            return this;
        }

        /**
         * Sets the text of the positive button and the listener that will be invoked when the
         * button is pressed. If a listener is not provided, then the dialog will dismiss itself
         * when the positive button is clicked.
         *
         * <p>The positive button should be used to accept and continue with the action (e.g.
         * an "OK" action).
         *
         * @param text The string to be used for the positive button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setPositiveButton(CharSequence text, @Nullable OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonListener = listener;
            return this;
        }

        /**
         * Sets the text of the negative button and the listener that will be invoked when the
         * button is pressed. If a listener is not provided, then the dialog will dismiss itself
         * when the negative button is clicked.
         *
         * <p>The negative button should be used to cancel any actions the dialog represents.
         *
         * @param textId The resource id of the string to be used for the negative button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setNegativeButton(@StringRes int textId,
                @Nullable OnClickListener listener) {
            mNegativeButtonText = mContext.getString(textId);
            mNegativeButtonListener = listener;
            return this;
        }

        /**
         * Sets the text of the negative button and the listener that will be invoked when the
         * button is pressed. If a listener is not provided, then the dialog will dismiss itself
         * when the negative button is clicked.
         *
         * <p>The negative button should be used to cancel any actions the dialog represents.
         *
         * @param text The string to be used for the negative button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setNegativeButton(CharSequence text, @Nullable OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        /**
         * Sets whether the dialog is cancelable or not. Default is {@code true}.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        /**
         * Sets the callback that will be called if the dialog is canceled.
         *
         * <p>Even in a cancelable dialog, the dialog may be dismissed for reasons other than
         * being canceled or one of the supplied choices being selected.
         * If you are interested in listening for all cases where the dialog is dismissed
         * and not just when it is canceled, see {@link #setOnDismissListener(OnDismissListener)}.
         *
         * @param onCancelListener The listener to be invoked when this dialog is canceled.
         * @return This {@code Builder} object to allow for chaining of calls.
         *
         * @see #setCancelable(boolean)
         * @see #setOnDismissListener(OnDismissListener)
         */
        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            mOnCancelListener = onCancelListener;
            return this;
        }

        /**
         * Sets the callback that will be called when the dialog is dismissed for any reason.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        public Builder setOnDismissListener(OnDismissListener onDismissListener) {
            mOnDismissListener = onDismissListener;
            return this;
        }

        /**
         * Creates an {@link CarAlertDialog} with the arguments supplied to this {@code Builder}.
         *
         * <p>Calling this method does not display the dialog. Utilize this dialog within a
         * {@link androidx.fragment.app.DialogFragment} to show the dialog.
         */
        public CarAlertDialog create() {
            CarAlertDialog dialog = new CarAlertDialog(mContext, /* builder= */ this);

            dialog.setCancelable(mCancelable);
            dialog.setCanceledOnTouchOutside(mCancelable);
            dialog.setOnCancelListener(mOnCancelListener);
            dialog.setOnDismissListener(mOnDismissListener);

            return dialog;
        }
    }
}
