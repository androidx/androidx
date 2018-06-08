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
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.car.R;
import androidx.car.widget.DayNightStyle;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.PagedScrollBarView;
import androidx.car.widget.SubheaderListItem;
import androidx.car.widget.TextListItem;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of {@link Dialog} that is tailored for the car environment. This dialog can display a
 * fixed list of items. There is no affordance for setting titles or any other text.
 *
 * <p>Its functionality is similar to if a list has been set on
 * {@link androidx.appcompat.app.AlertDialog}, but is styled so that it is more appropriate for
 * displaying in vehicles.
 *
 * <p>Note that this dialog cannot be created with an empty list.
 */
public class CarListDialog extends Dialog {
    private static final String TAG = "CarListDialog";
    private static final int ANIMATION_DURATION_MS = 100;

    @Nullable
    private final CharSequence mTitle;
    private TextView mTitleView;

    private ListItemAdapter mAdapter;
    private final int mInitialPosition;
    private PagedListView mList;
    private PagedScrollBarView mScrollBarView;

    private final float mTitleElevation;

    @Nullable
    private final DialogInterface.OnClickListener mOnClickListener;

    /** Flag for if a touch on the scrim of the dialog will dismiss it. */
    private boolean mDismissOnTouchOutside;

    private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    updateScrollbar();
                    // Remove this listener because the listener for the scroll state will be
                    // enough to keep the scrollbar in sync.
                    mList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            };

    private CarListDialog(Context context, Builder builder) {
        super(context, getDialogTheme(context));
        mInitialPosition = builder.mInitialPosition;
        mOnClickListener = builder.mOnClickListener;
        mTitle = builder.mTitle;
        mTitleElevation =
                context.getResources().getDimension(R.dimen.car_list_dialog_title_elevation);

        if (builder.mSections != null) {
            initializeWithSections(builder.mSections);
        } else {
            initializeWithItems(builder.mItems);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        // Ideally this method should be private; the dialog should only be modifiable through the
        // Builder. Unfortunately, this method is defined with the Dialog itself and is public.
        // So, throw an error if this method is ever called.
        throw new UnsupportedOperationException("Title should only be set from the Builder");
    }

    /**
     * @see super#setCanceledOnTouchOutside(boolean)
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
        window.setContentView(R.layout.car_list_dialog);

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
        initializeList();
        initializeScrollbar();

        // Need to set this elevation listener last because the title and list need to be
        // initialized first.
        initializeTitleElevationListener();
    }

    private void initializeTitle() {
        mTitleView = getWindow().findViewById(R.id.title);
        mTitleView.setText(mTitle);
        mTitleView.setVisibility(!TextUtils.isEmpty(mTitle) ? View.VISIBLE : View.GONE);
    }

    /**
     * Initializes a listener on the scrolling of the list in this dialog that will update
     * the elevation of the title text.
     *
     * <p>If the list is not at the top position, there will be elevation. Otherwise, the
     * elevation is zero.
     */
    private void initializeTitleElevationListener() {
        if (mTitleView.getVisibility() == View.GONE) {
            return;
        }

        mList.setOnScrollListener(new PagedListView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // The PagedListView is a vertically scrolling list, so it will be using a
                // LinearLayoutManager.
                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) recyclerView.getLayoutManager();

                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    // Need to remove elevation with animation so it is not jarring.
                    removeTitleElevationWithAnimation();
                } else {
                    // Note that elevation can be added without any elevation because the list
                    // scroll will hide the fact that it pops in.
                    mTitleView.setElevation(mTitleElevation);
                }
            }
        });
    }

    /** Animates the removal of elevation from the title view. */
    private void removeTitleElevationWithAnimation() {
        ValueAnimator elevationAnimator =
                ValueAnimator.ofFloat(mTitleView.getElevation(), 0f);
        elevationAnimator
                .setDuration(ANIMATION_DURATION_MS)
                .addUpdateListener(
                        animation -> mTitleView.setElevation((float) animation.getAnimatedValue()));
        elevationAnimator.start();
    }

    @Override
    protected void onStop() {
        // Cleanup to ensure that no stray view observers are still attached.
        if (mList != null) {
            mList.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
        }

        super.onStop();
    }

    private void initializeList() {
        mList = getWindow().findViewById(R.id.list);
        mList.setMaxPages(PagedListView.UNLIMITED_PAGES);
        mList.setAdapter(mAdapter);
        mList.setDividerVisibilityManager(mAdapter);

        // The list will start at the 0 position, so no need to scroll.
        if (mInitialPosition != 0) {
            mList.snapToPosition(mInitialPosition);
        }

        // Ensure that when the list is scrolled, the scrollbar updates to reflect the new position.
        mList.getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateScrollbar();
            }
        });

        // Update if the scrollbar should be visible after the PagedListView has finished
        // laying itself out. This is needed because the only way to the state of scrollbar is to
        // see the items after they have been laid out.
        mList.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
    }

    /**
     * Initializes the scrollbar that appears off the dialog. This scrollbar is not the one that
     * usually appears with the PagedListView, but mimics it in functionality.
     */
    private void initializeScrollbar() {
        mScrollBarView = getWindow().findViewById(R.id.scrollbar);
        mScrollBarView.setDayNightStyle(DayNightStyle.ALWAYS_LIGHT);

        mScrollBarView.setPaginationListener(new PagedScrollBarView.PaginationListener() {
            @Override
            public void onPaginate(int direction) {
                switch (direction) {
                    case PagedScrollBarView.PaginationListener.PAGE_UP:
                        mList.pageUp();
                        break;
                    case PagedScrollBarView.PaginationListener.PAGE_DOWN:
                        mList.pageDown();
                        break;
                    default:
                        Log.e(TAG, "Unknown pagination direction (" + direction + ")");
                }
            }

            @Override
            public void onAlphaJump() {
            }
        });
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
     * Initializes {@link #mAdapter} to display the items in the given array. It utilizes the
     * {@link TextListItem} but only populates the title field with the the values in the array.
     */
    private void initializeWithItems(String[] items) {
        Context context = getContext();
        List<ListItem> listItems = new ArrayList<>();

        for (int i = 0; i < items.length; i++) {
            listItems.add(createItem(/* text= */ items[i], /* position= */ i));
        }

        mAdapter = new ListItemAdapter(context, new ListItemProvider.ListProvider(listItems));

    }

    /**
     * Initializes the {@link #mAdapter} to display the sections in the given array. It utilizes
     * the {@link SubheaderListItem} to display the section title and {@link TextListItem} to
     * display the individual items of a section.
     */
    private void initializeWithSections(DialogSubSection[] sections) {
        Context context = getContext();
        List<ListItem> listItems = new ArrayList<>();

        for (DialogSubSection section : sections) {
            SubheaderListItem header = new SubheaderListItem(getContext(), section.getTitle());
            header.setHideDivider(true);

            listItems.add(header);

            String[] items = section.getItems();
            // Now initialize all the items associated with this subsection.
            for (int i = 0, length = items.length; i < length; i++) {
                listItems.add(createItem(/* text= */ items[i], /* position= */ i));
            }
        }

        mAdapter = new ListItemAdapter(context, new ListItemProvider.ListProvider(listItems));
    }

    /**
     * Creates the {@link TextListItem} that represents an item in the {@code CarListDialog}.
     *
     * @param text The text to display as the title in {@code TextListItem}.
     * @param position The position of the item in the list.
     */
    private TextListItem createItem(String text, int position) {
        TextListItem item = new TextListItem(getContext());
        item.setTitle(text);

        // Save the position to pass to onItemClick().
        item.setOnClickListener(v -> onItemClick(position));

        return item;
    }

    /**
     * Check if a click listener has been set on this dialog and notify that a click has happened
     * at the given item position, then dismisses this dialog. If no listener has been set, the
     * dialog just dismisses.
     */
    private void onItemClick(int position) {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(/* dialog= */ this, position);
        }
        dismiss();
    }

    /**
     * Determines if scrollbar should be visible or not and shows/hides it accordingly.
     *
     * <p>If this is being called as a result of adapter changes, it should be called after the new
     * layout has been calculated because the method of determining scrollbar visibility uses the
     * current layout.
     *
     * <p>If this is called after an adapter change but before the new layout, the visibility
     * determination may not be correct.
     */
    private void updateScrollbar() {
        RecyclerView recyclerView = mList.getRecyclerView();

        boolean isAtStart = mList.isAtStart();
        boolean isAtEnd = mList.isAtEnd();

        if ((isAtStart && isAtEnd)) {
            mScrollBarView.setVisibility(View.INVISIBLE);
            return;
        }

        mScrollBarView.setVisibility(View.VISIBLE);
        mScrollBarView.setUpEnabled(!isAtStart);
        mScrollBarView.setDownEnabled(!isAtEnd);

        // Assume the list scrolls vertically because we control the list and know the
        // LayoutManager cannot change.
        mScrollBarView.setParameters(
                recyclerView.computeVerticalScrollRange(),
                recyclerView.computeVerticalScrollOffset(),
                recyclerView.computeVerticalScrollExtent(),
                /* animate= */ false);

        getWindow().getDecorView().invalidate();
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
     * A struct that holds data for a section. A section is a combination of the section title and
     * the list of items associated with that section.
     */
    public static class DialogSubSection {
        private final String mTitle;
        private final String[] mItems;

        /**
         * Creates a subsection.
         *
         * @param title The title of the section. Must be non-empty.
         * @param items A list of items associated with this section. This list cannot be
         *              {@code null} or empty.
         */
        public DialogSubSection(@NonNull String title, @NonNull String[] items) {
            if (TextUtils.isEmpty(title)) {
                throw new IllegalArgumentException("Title cannot be empty.");
            }

            if (items == null || items.length == 0) {
                throw new IllegalArgumentException("Items cannot be empty.");
            }

            mTitle = title;
            mItems = items;
        }

        /** Returns the section title. */
        @NonNull
        public String getTitle() {
            return mTitle;
        }

        /** Returns the section items. */
        @NonNull
        public String[] getItems() {
            return mItems;
        }

        /**
         * Returns the total number of items related to this section. The length of the section is
         * defined as the number of items plus an entry for the title of the section.
         *
         * <p>This value will always be greater than 0 due to the fact that the title must always
         * be specified and the number of items passed to this {@code DialogSubSection} should
         * always be greater than 0.
         */
        public int getItemCount() {
            // Adding 1 to the length for account for the title.
            return mItems.length + 1;
        }
    }

    /**
     * Builder class that can be used to create a {@link CarListDialog} by configuring the
     * options for the list and behavior of the dialog.
     */
    public static final class Builder {
        private final Context mContext;

        private CharSequence mTitle;
        private int mInitialPosition;
        private String[] mItems;
        private DialogSubSection[] mSections;
        private DialogInterface.OnClickListener mOnClickListener;

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
         * Sets the title of the dialog to be the given string resource.
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
         * Sets the title of the dialog for be the given string.
         *
         * @param title The string to be used as the title.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the items that should appear in the list. The dialog will automatically dismiss
         * itself when an item in the list is clicked on.
         *
         * <p>If a {@link DialogInterface.OnClickListener} is given, then it will be notified
         * of the click. The dialog will still be dismissed afterwards. The {@code which}
         * parameter of the {@link DialogInterface.OnClickListener#onClick(DialogInterface, int)}
         * method will be the position of the item. This position maps to the index of the item in
         * the given list.
         *
         * <p>The provided list of items cannot be {@code null} or empty. Passing an empty list
         * to this method will throw can exception.
         *
         * <p>If both this method and {@link #setItems(DialogSubSection[], OnClickListener)} are
         * called, then the sections will take precedent, and the items set via this method will
         * be ignored.
         *
         * @param items The items that will appear in the list.
         * @param onClickListener The listener that will be notified of a click.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setItems(@NonNull String[] items,
                @Nullable OnClickListener onClickListener) {
            if (items == null || items.length == 0) {
                throw new IllegalArgumentException("Provided list of items cannot be empty.");
            }

            mItems = items;
            mOnClickListener = onClickListener;
            return this;
        }

        /**
         * Sets the items that should appear in the list, divided into sections. Each section has a
         * title and a list of items associated with it. The dialog will automatically dismiss
         * itself when an item in the list is clicked on; the title of the section is not
         * clickable.
         *
         * <p>If a {@link DialogInterface.OnClickListener} is given, then it will be notified
         * of the click. The dialog will still be dismissed afterwards. The {@code which}
         * parameter of the {@link DialogInterface.OnClickListener#onClick(DialogInterface, int)}
         * method will be the position of the item. This position maps to the index of the item in
         * the given list.
         *
         * <p>The provided list of sections cannot be {@code null} or empty. The list of items
         * within a section also cannot be empty. Passing an empty list to this method will
         * throw can exception.
         *
         * <p>If both this method and {@link #setItems(String[], OnClickListener)} are called, then
         * the sections will take precedent, and the items set via the other method will be
         * ignored.
         *
         * @param sections The sections that will appear in the list.
         * @param onClickListener The listener that will be notified of a click.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setItems(@NonNull DialogSubSection[] sections,
                @Nullable OnClickListener onClickListener) {
            if (sections == null || sections.length == 0) {
                throw new IllegalArgumentException("Provided list of sections cannot be empty.");
            }

            mSections = sections;
            mOnClickListener = onClickListener;
            return this;
        }

        /**
         * Sets the initial position in the list that the {@code CarListDialog} will start at. When
         * the dialog is created, the list will animate to the given position.
         *
         * <p>The position uses zero-based indexing. So, to scroll to the fifth item in the list,
         * a value of four should be passed.
         *
         * <p>If the items in this dialog was set by
         * {@link #setItems(DialogSubSection[], OnClickListener)}, then note that the title of the
         * section counts as an item in the list.
         *
         * @param initialPosition The initial position in the list to display.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setInitialPosition(int initialPosition) {
            if (initialPosition < 0) {
                throw new IllegalArgumentException("Initial position cannot be negative.");
            }
            mInitialPosition = initialPosition;
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
         *
         * @see #setCancelable(boolean)
         * @see #setOnDismissListener(OnDismissListener)
         */
        @NonNull
        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            mOnCancelListener = onCancelListener;
            return this;
        }

        /**
         * Sets the callback that will be called when the dialog is dismissed for any reason.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setOnDismissListener(OnDismissListener onDismissListener) {
            mOnDismissListener = onDismissListener;
            return this;
        }

        /**
         * Creates an {@link CarListDialog} with the arguments supplied to this {@code Builder}.
         *
         * <p>If {@link #setItems(String[],DialogInterface.OnClickListener)} is never called, then
         * calling this method will throw an exception.
         *
         * <p>Calling this method does not display the dialog. Utilize this dialog within a
         * {@link androidx.fragment.app.DialogFragment} to show the dialog.
         */
        public CarListDialog create() {
            // Check that the dialog was created with a list of either sections or items.
            if ((mSections == null || mSections.length == 0)
                    && (mItems == null || mItems.length == 0)) {
                throw new IllegalStateException(
                        "CarListDialog cannot be created with a non-empty list.");
            }

            int numOfItems = 0;

            // Subsections take precedent over items as both cannot be set at the same time.
            if (mSections != null) {
                mItems = null;

                // Calculate the total number of items by adding up all the sections.
                for (DialogSubSection section : mSections) {
                    numOfItems += section.getItemCount();
                }
            } else {
                numOfItems = mItems.length;
            }

            if (mInitialPosition >= numOfItems) {
                throw new IllegalStateException("Initial position is greater than the number of "
                        + "items in the list.");
            }

            CarListDialog dialog = new CarListDialog(mContext, /* builder= */ this);

            dialog.setCancelable(mCancelable);
            dialog.setCanceledOnTouchOutside(mCancelable);
            dialog.setOnCancelListener(mOnCancelListener);
            dialog.setOnDismissListener(mOnDismissListener);

            return dialog;
        }
    }
}
