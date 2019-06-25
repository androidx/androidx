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

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.MovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.car.R;

/**
 * A subclass of {@link Dialog} that is tailored for the car environment. This dialog can display a
 * title text, body text, and up to two buttons -- a positive and negative button. There is no
 * affordance for displaying a custom view or list of content, differentiating it from a regular
 * {@code AlertDialog}.
 */
public class CarAlertDialog extends Dialog {
    private final CharSequence mTitle;
    private final Drawable mIconDrawable;
    private final CharSequence mBody;
    private final CharSequence mPositiveButtonText;
    private final OnClickListener mPositiveButtonListener;
    private final CharSequence mNegativeButtonText;
    private final OnClickListener mNegativeButtonListener;

    private final int mTopPadding;
    private final int mButtonPanelTopMargin;
    private final int mBottomPadding;
    private final int mButtonSpacing;

    private View mContentView;
    private View mHeaderView;
    private TextView mTitleView;
    private ImageView mIconView;
    private TextView mBodyView;
    private MovementMethod mBodyMovementMethod;

    private View mButtonPanel;
    private Button mPositiveButton;
    private Button mNegativeButton;

    CarAlertDialog(Context context, Builder builder) {
        super(context, getDialogTheme(context));

        mTitle = builder.mTitle;
        mIconDrawable = builder.mIconDrawable;
        mBody = builder.mBody;
        mBodyMovementMethod = builder.mBodyMovementMethod;
        mPositiveButtonText = builder.mPositiveButtonText;
        mPositiveButtonListener = builder.mPositiveButtonListener;
        mNegativeButtonText = builder.mNegativeButtonText;
        mNegativeButtonListener = builder.mNegativeButtonListener;

        Resources res = context.getResources();
        mTopPadding = res.getDimensionPixelSize(R.dimen.car_padding_4);
        mButtonPanelTopMargin = res.getDimensionPixelSize(R.dimen.car_padding_2);
        mBottomPadding = res.getDimensionPixelSize(R.dimen.car_padding_4);
        mButtonSpacing = res.getDimensionPixelSize(R.dimen.car_padding_2);
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
        setHeaderIcon(mIconDrawable);
        setTitleInternal(mTitle);
        // setupHeader() should be called last because we want to center title and adjust
        // padding depending on icon/body/button configuration.
        setupHeader();

        Window window = getWindow();

        // Background dim animation.
        Resources res = getContext().getResources();
        TypedValue outValue = new TypedValue();
        res.getValue(R.dimen.car_dialog_background_dim, outValue, true);
        float dimAmount = outValue.getFloat();

        ValueAnimator backgroundDimAnimator = ValueAnimator.ofFloat(0f, dimAmount);
        backgroundDimAnimator.setDuration(res.getInteger(R.integer.car_dialog_enter_duration_ms));

        backgroundDimAnimator.addUpdateListener(
                animation -> window.setDimAmount((float) animation.getAnimatedValue()));
        backgroundDimAnimator.start();
    }

    private void setHeaderIcon(@Nullable Drawable iconDrawable) {
        if (iconDrawable != null) {
            mIconView.setImageDrawable(iconDrawable);
            mIconView.setVisibility(View.VISIBLE);
        } else {
            mIconView.setVisibility(View.GONE);
        }
    }

    private void setTitleInternal(CharSequence title) {
        boolean hasTitle = !TextUtils.isEmpty(title);

        mTitleView.setText(title);
        mTitleView.setVisibility(hasTitle ? View.VISIBLE : View.GONE);
    }

    private void setupHeader() {
        boolean hasTitle = mTitleView.getVisibility() == View.VISIBLE;
        boolean hasIcon = mIconView.getVisibility() == View.VISIBLE;
        boolean hasBody = mBodyView.getVisibility() == View.VISIBLE;
        boolean hasButton = mButtonPanel.getVisibility() == View.VISIBLE;
        boolean onlyTitle = !hasIcon && !hasButton && !hasBody;

        // If there's a title, then remove the padding at the top of the content view.
        int topPadding = (hasTitle || hasIcon) ? 0 : mTopPadding;

        // If there is only title, also remove the padding at the bottom so title is
        // vertically centered.
        int bottomPadding = onlyTitle ? 0 : mContentView.getPaddingBottom();
        mContentView.setPaddingRelative(
                mContentView.getPaddingStart(),
                topPadding,
                mContentView.getPaddingEnd(),
                bottomPadding);

        // Remove the Header padding if there's an icon.
        int headerTopPadding = hasIcon ? mHeaderView.getPaddingTop() : 0;
        int headerBottomPadding = hasIcon ? mHeaderView.getPaddingBottom() : 0;

        mHeaderView.setPaddingRelative(
                mHeaderView.getPaddingStart(),
                headerTopPadding,
                mHeaderView.getPaddingEnd(),
                headerBottomPadding);
    }

    private void setBody(CharSequence body) {
        mBodyView.setText(body);
        mBodyView.setMovementMethod(mBodyMovementMethod);
        mBodyView.setVisibility(TextUtils.isEmpty(body) ? View.GONE : View.VISIBLE);

        updateButtonPanelTopMargin();
    }

    private void setPositiveButton(CharSequence text) {
        boolean showButton = !TextUtils.isEmpty(text);

        mPositiveButton.setText(text);
        mPositiveButton.setVisibility(showButton ? View.VISIBLE : View.GONE);

        updateButtonPanelVisibility();
        updateButtonSpacing();
    }

    private void setNegativeButton(CharSequence text) {
        mNegativeButton.setText(text);
        mNegativeButton.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);

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
     * Updates the start and end margins for the positive and negative buttons.
     */
    private void updateButtonSpacing() {
        // If both buttons are visible, then there needs to be spacing between them.

        Resources res = getContext().getResources();

        int buttonOffset = mBottomPadding - mButtonSpacing;

        ViewGroup.MarginLayoutParams positiveButtonLayoutParams =
                (ViewGroup.MarginLayoutParams) mPositiveButton.getLayoutParams();
        ViewGroup.MarginLayoutParams negativeButtonLayoutParams =
                (ViewGroup.MarginLayoutParams) mNegativeButton.getLayoutParams();


        if ((mPositiveButton.getVisibility() == View.VISIBLE
                && mNegativeButton.getVisibility() == View.VISIBLE)) {

            positiveButtonLayoutParams.setMarginStart(buttonOffset);
            positiveButtonLayoutParams.setMarginEnd(mButtonSpacing);
            mPositiveButton.requestLayout();

            negativeButtonLayoutParams.setMarginStart(mButtonSpacing);
            mNegativeButton.requestLayout();
        } else if (mPositiveButton.getVisibility() == View.VISIBLE) {
            positiveButtonLayoutParams.setMarginStart(buttonOffset);
            mPositiveButton.requestLayout();
        } else if (mNegativeButton.getVisibility() == View.VISIBLE) {
            negativeButtonLayoutParams.setMarginStart(buttonOffset);
            mNegativeButton.requestLayout();
        }
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
     * set on it.
     */
    private void initializeViews() {
        Window window = getWindow();

        mContentView = window.findViewById(R.id.content_view);
        mHeaderView = window.findViewById(R.id.header_view);
        mIconView = window.findViewById(R.id.icon_view);
        mTitleView = window.findViewById(R.id.title);
        mBodyView = window.findViewById(R.id.body);

        mButtonPanel = window.findViewById(R.id.button_panel);
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
     * Returns the style that has been assigned to {@code carDialogTheme} in the
     * current theme that is inflating this dialog. If a style has not been defined, a default
     * style will be returned.
     */
    @StyleRes
    private static int getDialogTheme(Context context) {
        TypedValue outValue = new TypedValue();
        boolean hasStyle =
                context.getTheme().resolveAttribute(R.attr.carDialogTheme, outValue, true);
        return hasStyle ? outValue.resourceId : R.style.Theme_Car_Dark_Dialog;
    }

    /**
     * Builder class that can be used to create a {@link CarAlertDialog} by configuring the options
     * for what shows up in the resulting dialog.
     */
    public static final class Builder {
        private final Context mContext;

        Drawable mIconDrawable;
        CharSequence mTitle;
        CharSequence mBody;
        MovementMethod mBodyMovementMethod;

        CharSequence mPositiveButtonText;
        OnClickListener mPositiveButtonListener;
        CharSequence mNegativeButtonText;
        OnClickListener mNegativeButtonListener;

        private boolean mCancelable = true;
        private OnCancelListener mOnCancelListener;
        private OnDismissListener mOnDismissListener;

        /**
         * Creates a new instance of the {@code Builder}.
         *
         * @param context The {@code Context} that the dialog is to be created in.
         */
        public Builder(@NonNull Context context) {
            mContext = context;
        }

        /**
         * Sets the header icon of the dialog to be the given int resource id.
         * Passing-in an invalid id will throw a NotFoundException.
         *
         * @param iconId The resource id of the Icon to be used as the header icon.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setHeaderIcon(@DrawableRes int iconId) {
            mIconDrawable = mContext.getDrawable(iconId);
            return this;
        }

        /**
         * Sets the main title of the dialog to be the given string resource.
         *
         * @param titleId The resource id of the string to be used as the title.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
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
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the body text of the dialog to be the given string resource.
         *
         * @param bodyId The resource id of the string to be used as the body text.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
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
        @NonNull
        public Builder setBody(@Nullable CharSequence body) {
            mBody = body;
            return this;
        }

        /**
         * Sets the {@link MovementMethod} to be applied on the body text of this alert dialog.
         *
         * @param movementMethod The {@code MovementMethod} to apply or {@code null}.
         * @return This {@code Builder} object to allow for chaining of calls.
         * @see TextView#setMovementMethod(MovementMethod)
         */
        @NonNull
        public Builder setBodyMovementMethod(@Nullable MovementMethod movementMethod) {
            mBodyMovementMethod = movementMethod;
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
         * @param textId   The resource id of the string to be used for the positive button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
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
         * @param text     The string to be used for the positive button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setPositiveButton(@NonNull CharSequence text,
                @Nullable OnClickListener listener) {
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
         * @param textId   The resource id of the string to be used for the negative button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
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
         * @param text     The string to be used for the negative button text.
         * @param listener A {@link android.content.DialogInterface.OnClickListener} to be invoked
         *                 when the button is clicked. Can be {@code null} to represent no listener.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setNegativeButton(@NonNull CharSequence text,
                @Nullable OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        /**
         * Sets whether the dialog is cancelable or not. Default is {@code true}.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
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
         * @see #setCancelable(boolean)
         * @see #setOnDismissListener(OnDismissListener)
         */
        @NonNull
        public Builder setOnCancelListener(@NonNull OnCancelListener onCancelListener) {
            mOnCancelListener = onCancelListener;
            return this;
        }

        /**
         * Sets the callback that will be called when the dialog is dismissed for any reason.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setOnDismissListener(@NonNull OnDismissListener onDismissListener) {
            mOnDismissListener = onDismissListener;
            return this;
        }

        /**
         * Creates an {@link CarAlertDialog} with the arguments supplied to this {@code Builder}.
         *
         * <p>Calling this method does not display the dialog. Utilize this dialog within a
         * {@link androidx.fragment.app.DialogFragment} to show the dialog.
         */
        @NonNull
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
