/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import android.view.View;

/**
 * A base class for {@link Preference} objects that are
 * dialog-based. These preferences will, when clicked, open a dialog showing the
 * actual preference controls.
 *
 * @attr name android:dialogTitle
 * @attr name android:dialogMessage
 * @attr name android:dialogIcon
 * @attr name android:dialogLayout
 * @attr name android:positiveButtonText
 * @attr name android:negativeButtonText
 */
public abstract class DialogPreference extends Preference {

    public interface TargetFragment {
        Preference findPreference(CharSequence key);
    }

    private CharSequence mDialogTitle;
    private CharSequence mDialogMessage;
    private Drawable mDialogIcon;
    private CharSequence mPositiveButtonText;
    private CharSequence mNegativeButtonText;
    private int mDialogLayoutResId;

    public DialogPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.DialogPreference, defStyleAttr, defStyleRes);

        mDialogTitle = TypedArrayUtils.getString(a, R.styleable.DialogPreference_dialogTitle,
                R.styleable.DialogPreference_android_dialogTitle);
        if (mDialogTitle == null) {
            // Fall back on the regular title of the preference
            // (the one that is seen in the list)
            mDialogTitle = getTitle();
        }

        mDialogMessage = TypedArrayUtils.getString(a, R.styleable.DialogPreference_dialogMessage,
                R.styleable.DialogPreference_android_dialogMessage);

        mDialogIcon = TypedArrayUtils.getDrawable(a, R.styleable.DialogPreference_dialogIcon,
                R.styleable.DialogPreference_android_dialogIcon);

        mPositiveButtonText = TypedArrayUtils.getString(a,
                R.styleable.DialogPreference_positiveButtonText,
                R.styleable.DialogPreference_android_positiveButtonText);

        mNegativeButtonText = TypedArrayUtils.getString(a,
                R.styleable.DialogPreference_negativeButtonText,
                R.styleable.DialogPreference_android_negativeButtonText);

        mDialogLayoutResId = TypedArrayUtils.getResourceId(a,
                R.styleable.DialogPreference_dialogLayout,
                R.styleable.DialogPreference_android_dialogLayout, 0);

        a.recycle();
    }

    public DialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DialogPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public DialogPreference(Context context) {
        this(context, null);
    }

    /**
     * Sets the title of the dialog. This will be shown on subsequent dialogs.
     *
     * @param dialogTitle The title.
     */
    public void setDialogTitle(CharSequence dialogTitle) {
        mDialogTitle = dialogTitle;
    }

    /**
     * @see #setDialogTitle(CharSequence)
     * @param dialogTitleResId The dialog title as a resource.
     */
    public void setDialogTitle(int dialogTitleResId) {
        setDialogTitle(getContext().getString(dialogTitleResId));
    }

    /**
     * Returns the title to be shown on subsequent dialogs.
     * @return The title.
     */
    public CharSequence getDialogTitle() {
        return mDialogTitle;
    }

    /**
     * Sets the message of the dialog. This will be shown on subsequent dialogs.
     * <p>
     * This message forms the content View of the dialog and conflicts with
     * list-based dialogs, for example. If setting a custom View on a dialog via
     * {@link #setDialogLayoutResource(int)}, include a text View with ID
     * {@link android.R.id#message} and it will be populated with this message.
     *
     * @param dialogMessage The message.
     */
    public void setDialogMessage(CharSequence dialogMessage) {
        mDialogMessage = dialogMessage;
    }

    /**
     * @see #setDialogMessage(CharSequence)
     * @param dialogMessageResId The dialog message as a resource.
     */
    public void setDialogMessage(int dialogMessageResId) {
        setDialogMessage(getContext().getString(dialogMessageResId));
    }

    /**
     * Returns the message to be shown on subsequent dialogs.
     * @return The message.
     */
    public CharSequence getDialogMessage() {
        return mDialogMessage;
    }

    /**
     * Sets the icon of the dialog. This will be shown on subsequent dialogs.
     *
     * @param dialogIcon The icon, as a {@link Drawable}.
     */
    public void setDialogIcon(Drawable dialogIcon) {
        mDialogIcon = dialogIcon;
    }

    /**
     * Sets the icon (resource ID) of the dialog. This will be shown on
     * subsequent dialogs.
     *
     * @param dialogIconRes The icon, as a resource ID.
     */
    public void setDialogIcon(int dialogIconRes) {
        mDialogIcon = ContextCompat.getDrawable(getContext(), dialogIconRes);
    }

    /**
     * Returns the icon to be shown on subsequent dialogs.
     * @return The icon, as a {@link Drawable}.
     */
    public Drawable getDialogIcon() {
        return mDialogIcon;
    }

    /**
     * Sets the text of the positive button of the dialog. This will be shown on
     * subsequent dialogs.
     *
     * @param positiveButtonText The text of the positive button.
     */
    public void setPositiveButtonText(CharSequence positiveButtonText) {
        mPositiveButtonText = positiveButtonText;
    }

    /**
     * @see #setPositiveButtonText(CharSequence)
     * @param positiveButtonTextResId The positive button text as a resource.
     */
    public void setPositiveButtonText(int positiveButtonTextResId) {
        setPositiveButtonText(getContext().getString(positiveButtonTextResId));
    }

    /**
     * Returns the text of the positive button to be shown on subsequent
     * dialogs.
     *
     * @return The text of the positive button.
     */
    public CharSequence getPositiveButtonText() {
        return mPositiveButtonText;
    }

    /**
     * Sets the text of the negative button of the dialog. This will be shown on
     * subsequent dialogs.
     *
     * @param negativeButtonText The text of the negative button.
     */
    public void setNegativeButtonText(CharSequence negativeButtonText) {
        mNegativeButtonText = negativeButtonText;
    }

    /**
     * @see #setNegativeButtonText(CharSequence)
     * @param negativeButtonTextResId The negative button text as a resource.
     */
    public void setNegativeButtonText(int negativeButtonTextResId) {
        setNegativeButtonText(getContext().getString(negativeButtonTextResId));
    }

    /**
     * Returns the text of the negative button to be shown on subsequent
     * dialogs.
     *
     * @return The text of the negative button.
     */
    public CharSequence getNegativeButtonText() {
        return mNegativeButtonText;
    }

    /**
     * Sets the layout resource that is inflated as the {@link View} to be shown
     * as the content View of subsequent dialogs.
     *
     * @param dialogLayoutResId The layout resource ID to be inflated.
     * @see #setDialogMessage(CharSequence)
     */
    public void setDialogLayoutResource(int dialogLayoutResId) {
        mDialogLayoutResId = dialogLayoutResId;
    }

    /**
     * Returns the layout resource that is used as the content View for
     * subsequent dialogs.
     *
     * @return The layout resource.
     */
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }

    @Override
    protected void onClick() {
        getPreferenceManager().showDialog(this);
    }

}
