/*
 * Copyright 2019 The Android Open Source Project
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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.R;
import androidx.car.util.DropShadowScrollListener;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.RadioButtonListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of {@link Dialog} that is tailored for the car environment. This dialog can display a
 * title, body text, a fixed list of single choice items and up to two buttons -- a positive and
 * negative button. Single choice items use a radio button to indicate selection.
 *
 * <p>Note that this dialog cannot be created with an empty list.
 */
public final class CarSingleChoiceDialog extends Dialog {
    private static final String TAG = "CarSingleChoiceDialog";

    private final CharSequence mTitle;
    private final CharSequence mBodyText;

    private final CharSequence mPositiveButtonText;
    private final CharSequence mNegativeButtonText;

    private ListItemAdapter mAdapter;
    private int mSelectedItem;

    private TextView mTitleView;
    private TextView mBodyTextView;

    private PagedListView mList;

    @Nullable
    private final OnClickListener mOnClickListener;

    /** Flag for if a touch on the scrim of the dialog will dismiss it. */
    private boolean mDismissOnTouchOutside;

    CarSingleChoiceDialog(Context context, Builder builder) {
        super(context, CarDialogUtil.getDialogTheme(context));

        mTitle = builder.mTitle;
        mBodyText = builder.mSubtitle;
        mSelectedItem = builder.mSelectedItem;
        mOnClickListener = builder.mOnClickListener;
        mPositiveButtonText = builder.mPositiveButtonText;
        mNegativeButtonText = builder.mNegativeButtonText;

        initializeWithItems(builder.mItems);
    }

    @Override
    public void setTitle(CharSequence title) {
        // Ideally this method should be private; the dialog should only be modifiable through the
        // Builder. Unfortunately, this method is defined with the Dialog itself and is public.
        // So, throw an error if this method is ever called.
        throw new UnsupportedOperationException("Title should only be set from the Builder");
    }

    /**
     * @see Dialog#setCanceledOnTouchOutside(boolean)
     */
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        // Need to override this method to save the value of cancel.
        mDismissOnTouchOutside = cancel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setContentView(R.layout.car_selection_dialog);

        // Ensure that the dialog takes up the entire window. This is needed because the scrollbar
        // needs to be drawn off the dialog.
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        // The container for this dialog takes up the entire screen. As a result, need to manually
        // listen for clicks and dismiss the dialog when necessary.
        window.findViewById(R.id.container).setOnClickListener(v -> handleTouchOutside());

        initializeTitle();
        initializeBodyText();
        initializeList();
        initializeButtons();

        // Need to set this elevation listener last because the text and list need to be
        // initialized first.
        initializeTextElevationListener();
    }

    private void initializeButtons() {
        Window window = getWindow();
        Resources res = getContext().getResources();

        Button positiveButtonView = window.findViewById(R.id.positive_button);
        positiveButtonView.setText(mPositiveButtonText);
        positiveButtonView.setOnClickListener(v -> {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(this, mSelectedItem);
            }
            dismiss();
        });

        int buttonOffset = res.getDimensionPixelSize(R.dimen.car_padding_4)
                - res.getDimensionPixelSize(R.dimen.car_padding_2);

        ViewGroup.MarginLayoutParams positiveButtonLayoutParams =
                (ViewGroup.MarginLayoutParams) positiveButtonView.getLayoutParams();
        Button negativeButtonView = window.findViewById(R.id.negative_button);

        if (!TextUtils.isEmpty(mNegativeButtonText)) {
            negativeButtonView.setText(mNegativeButtonText);
            negativeButtonView.setOnClickListener(v -> dismiss());

            ViewGroup.MarginLayoutParams negativeButtonLayoutParams =
                    (ViewGroup.MarginLayoutParams) negativeButtonView.getLayoutParams();

            int buttonSpacing = res.getDimensionPixelSize(R.dimen.car_padding_2);

            positiveButtonLayoutParams.setMarginStart(buttonSpacing);
            positiveButtonView.requestLayout();

            negativeButtonLayoutParams.setMarginStart(buttonOffset);
            negativeButtonLayoutParams.setMarginEnd(buttonSpacing);
            negativeButtonView.requestLayout();
        } else {
            negativeButtonView.setVisibility(View.GONE);

            positiveButtonLayoutParams.setMarginStart(buttonOffset);
            positiveButtonView.requestLayout();
        }
    }

    private void initializeTitle() {
        mTitleView = getWindow().findViewById(R.id.title);
        mTitleView.setText(mTitle);
        mTitleView.setVisibility(!TextUtils.isEmpty(mTitle) ? View.VISIBLE : View.GONE);
    }

    private void initializeBodyText() {
        mBodyTextView = getWindow().findViewById(R.id.bodyText);
        mBodyTextView.setText(mBodyText);
        mBodyTextView.setVisibility(!TextUtils.isEmpty(mBodyText) ? View.VISIBLE : View.GONE);
    }

    private void initializeTextElevationListener() {
        if (mTitleView.getVisibility() != View.GONE) {
            mList.addOnScrollListener(new DropShadowScrollListener(mTitleView));
        } else if (mBodyTextView.getVisibility() != View.GONE) {
            mList.addOnScrollListener(new DropShadowScrollListener(mBodyTextView));
        }
    }

    private void initializeList() {
        mList = getWindow().findViewById(R.id.list);
        mList.setMaxPages(PagedListView.UNLIMITED_PAGES);
        mList.setAdapter(mAdapter);
        mList.setDividerVisibilityManager(mAdapter);

        CarDialogUtil.setUpDialogList(mList, getWindow().findViewById(R.id.scrollbar));
    }

    /**
     * Handles if a touch has been detected outside of the dialog. If
     * {@link #mDismissOnTouchOutside} has been set, then the dialog will be dismissed.
     */
    private void handleTouchOutside() {
        if (mDismissOnTouchOutside) {
            dismiss();
        }
    }

    /**
     * Initializes {@link #mAdapter} to display the items in the given array by utilizing
     * {@link RadioButtonListItem}.
     */
    @SuppressWarnings("unchecked")
    private void initializeWithItems(List<Item> items) {
        List<ListItem> listItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            listItems.add(createItem(/* selectionItem= */ items.get(i), /* position= */ i));
        }

        mAdapter = new ListItemAdapter(getContext(), new ListItemProvider.ListProvider(listItems));
    }

    /**
     * Creates the {@link RadioButtonListItem} that represents an item in the {@code
     * CarSingleChoiceDialog}.
     *
     * @param {@link Item} to display as a {@code RadioButtonListItem}.
     * @param position The position of the item in the list.
     */
    private RadioButtonListItem createItem(Item selectionItem, int position) {
        RadioButtonListItem item = new RadioButtonListItem(getContext());
        item.setTitle(selectionItem.mTitle);
        item.setBody(selectionItem.mBody);
        item.setShowCompoundButtonDivider(false);
        item.addViewBinder(vh -> {
            vh.getCompoundButton().setChecked(mSelectedItem == position);
            vh.getCompoundButton().setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {
                        mSelectedItem = position;
                        // Refresh other radio button list items.
                        mAdapter.notifyDataSetChanged();
                    });
        });

        return item;
    }

    /**
     * A struct that holds data for a single choice item. A single choice item is a combination of
     * the item title and optional body text.
     */
    public static class Item {

        final CharSequence mTitle;
        final CharSequence mBody;

        /**
         * Creates a Item.
         *
         * @param title The title of the item. This value must be non-empty.
         */
        public Item(@NonNull CharSequence title) {
            this(title,  /* body= */ "");
        }


        /**
         * Creates a Item.
         *
         * @param title The title of the item. This value must be non-empty.
         * @param body  The secondary body text of the item.
         */
        public Item(@NonNull CharSequence title, @NonNull CharSequence body) {
            if (TextUtils.isEmpty(title)) {
                throw new IllegalArgumentException("Title cannot be empty.");
            }

            mTitle = title;
            mBody = body;
        }
    }

    /**
     * Builder class that can be used to create a {@link CarSingleChoiceDialog} by configuring
     * the options for the list and behavior of the dialog.
     */
    public static final class Builder {
        private final Context mContext;

        CharSequence mTitle;
        CharSequence mSubtitle;
        List<Item> mItems;
        int mSelectedItem;
        OnClickListener mOnClickListener;

        CharSequence mPositiveButtonText;
        CharSequence mNegativeButtonText;

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
         * Sets the title of the dialog to be the given string resource.
         *
         * @param titleId The resource id of the string to be used as the title.
         *                Text style will be retained.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setTitle(@StringRes int titleId) {
            mTitle = mContext.getText(titleId);
            return this;
        }

        /**
         * Sets the title of the dialog for be the given string.
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
         * @param bodyTextId The resource id of the string to be used as the body.
         *                   Text style will be retained.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setBody(@StringRes int bodyTextId) {
            mSubtitle = mContext.getText(bodyTextId);
            return this;
        }

        /**
         * Sets the bodyText of the dialog for be the given string.
         *
         * @param bodyText The string to be used as the body.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setBody(@Nullable CharSequence bodyText) {
            mSubtitle = bodyText;
            return this;
        }

        /**
         * Sets the items that should appear in the list.
         *
         * <p>If a {@link DialogInterface.OnClickListener} is given, then it will be notified
         * of the click.The {@code which} parameter of the
         * {@link DialogInterface.OnClickListener#onClick(DialogInterface, int)} method will be
         * the position of the item. This position maps to the index of the item in the given list.
         *
         * <p>The provided list of items cannot be {@code null} or empty. Passing an empty list
         * to this method will throw can exception.
         * *
         *
         * @param items        The items that will appear in the list.
         * @param selectedItem Specifies which item is checked initially. This value must map to
         *                     a valid index in the given list.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setItems(@NonNull List<Item> items,
                @IntRange(from = 0) int selectedItem) {
            if (items.size() == 0) {
                throw new IllegalArgumentException("Provided list of items cannot be empty.");
            }

            if (selectedItem >= items.size()) {
                throw new IllegalArgumentException("Selected item is not a valid index.");
            }

            mItems = items;
            mSelectedItem = selectedItem;
            return this;
        }

        /**
         * Configure the dialog to include a positive button.
         *
         * @param textId          The resource id of the text to display in the positive button.
         * @param onClickListener The listener that will be notified of a selection.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        @NonNull
        public Builder setPositiveButton(@StringRes int textId,
                @NonNull OnClickListener onClickListener) {
            mPositiveButtonText = mContext.getText(textId);
            mOnClickListener = onClickListener;
            return this;
        }

        /**
         * Configure the dialog to include a positive button.
         *
         * @param text            The text to display in the positive button.
         * @param onClickListener The listener that will be notified of a selection.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        @NonNull
        public Builder setPositiveButton(@NonNull CharSequence text,
                @NonNull OnClickListener onClickListener) {
            mPositiveButtonText = text;
            mOnClickListener = onClickListener;
            return this;
        }

        /**
         * Configure the dialog to include a negative button.
         *
         * @param textId The resource id of the text to display in the negative button.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        @NonNull
        public Builder setNegativeButton(@StringRes int textId) {
            mNegativeButtonText = mContext.getText(textId);
            return this;
        }

        /**
         * Configure the dialog to include a negative button.
         *
         * @param text The text to display in the negative button.
         * @return This Builder object to allow for chaining of calls to set methods.
         */
        @NonNull
        public Builder setNegativeButton(@NonNull CharSequence text) {
            mNegativeButtonText = text;
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
         * Creates a {@link CarSingleChoiceDialog}, which is returned as a {@link Dialog}, with the
         * arguments supplied to this {@code Builder}.
         *
         * <p>If {@link #setItems(List, int)} is never called, then calling this method will throw
         * an exception.
         *
         * <p>Calling this method does not display the dialog. Utilize this dialog within a
         * {@link androidx.fragment.app.DialogFragment} to show the dialog.
         */
        @NonNull
        public Dialog create() {
            // Check that the dialog was created with a list of items.
            if (mItems == null || mItems.size() == 0) {
                throw new IllegalStateException(
                        "CarSingleChoiceDialog cannot be created with a non-empty list.");
            }

            if (mSelectedItem < 0 || mSelectedItem >= mItems.size()) {
                throw new IllegalStateException(
                        "CarSingleChoiceDialog cannot be created with an invalid initial "
                                + "selected item.");
            }

            if (TextUtils.isEmpty(mPositiveButtonText)) {
                throw new IllegalStateException(
                        "CarSingleChoiceDialog cannot be created without a positive button.");
            }

            CarSingleChoiceDialog dialog = new CarSingleChoiceDialog(mContext,
                    /* builder= */this);

            dialog.setCancelable(mCancelable);
            dialog.setCanceledOnTouchOutside(mCancelable);
            dialog.setOnCancelListener(mOnCancelListener);
            dialog.setOnDismissListener(mOnDismissListener);

            return dialog;
        }
    }
}
