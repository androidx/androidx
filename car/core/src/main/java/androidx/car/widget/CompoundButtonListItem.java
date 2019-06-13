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

package androidx.car.widget;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.R;
import androidx.car.widget.ListItemAdapter.ListItemType;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Class to build a list item with {@link CompoundButton}.
 *
 * <p>A compound button list item is visually composed of 5 parts.
 * <ul>
 * <li>optional {@code Primary Action Icon}.
 * <li>optional {@code Title}.
 * <li>optional {@code Body}.
 * <li>optional {@code Divider}.
 * <li>A {@link CompoundButton}.
 * </ul>
 *
 * @param <VH> ViewHolder that extends {@link CompoundButtonListItem.ViewHolder}.
 */
public abstract class CompoundButtonListItem<VH extends CompoundButtonListItem.ViewHolder> extends
        ListItem<VH> {

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_ICON_SIZE_SMALL, PRIMARY_ACTION_ICON_SIZE_MEDIUM,
            PRIMARY_ACTION_ICON_SIZE_LARGE})
    private @interface PrimaryActionIconSize {
    }

    /**
     * Small sized icon is the mostly commonly used size. It's the same as supplemental action icon.
     */
    public static final int PRIMARY_ACTION_ICON_SIZE_SMALL = 0;
    /**
     * Medium sized icon is slightly bigger than {@code SMALL} ones. It is intended for profile
     * pictures (avatar), in which case caller is responsible for passing in a circular image.
     */
    public static final int PRIMARY_ACTION_ICON_SIZE_MEDIUM = 1;
    /**
     * Large sized icon is as tall as a list item with only {@code title} text. It is intended for
     * album art.
     */
    public static final int PRIMARY_ACTION_ICON_SIZE_LARGE = 2;

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_TYPE_NO_ICON, PRIMARY_ACTION_TYPE_EMPTY_ICON,
            PRIMARY_ACTION_TYPE_ICON})
    private @interface PrimaryActionType {
    }

    private static final int PRIMARY_ACTION_TYPE_NO_ICON = 0;
    private static final int PRIMARY_ACTION_TYPE_EMPTY_ICON = 1;
    private static final int PRIMARY_ACTION_TYPE_ICON = 2;

    private final Context mContext;
    private boolean mIsEnabled = true;
    private boolean mIsClickable;

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    @PrimaryActionType
    private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
    private Drawable mPrimaryActionIconDrawable;
    @PrimaryActionIconSize
    private int mPrimaryActionIconSize = PRIMARY_ACTION_ICON_SIZE_SMALL;

    private CharSequence mTitle;
    private CharSequence mBody;

    @Dimension
    private final int mSupplementalGuidelineBegin;

    private boolean mIsChecked;
    /**
     * {@code true} if the checked state of the item has changed programmatically and
     * {@link #mOnCheckedChangeListener} needs to be notified.
     */
    private boolean mShouldNotifyChecked;
    private boolean mShowCompoundButtonDivider;
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

    /**
     * Creates a {@link CompoundButtonListItem} that will be used to display a list item with a
     * {@link CompoundButton}.
     *
     * @param context The context to be used by this {@link CompoundButtonListItem}.
     */
    public CompoundButtonListItem(@NonNull Context context) {
        mContext = context;
        Resources res = mContext.getResources();
        mSupplementalGuidelineBegin = res.getDimensionPixelSize(
                R.dimen.car_list_item_supplemental_guideline_top);
        markDirty();
    }

    /**
     * Classes that extend {@link CompoundButtonListItem} should register its view type in
     * {@link ListItemAdapter#registerListItemViewType(int, int, Function)}.
     *
     * @return Type of this {@link CompoundButtonListItem}.
     */
    @ListItemType
    @Override
    public abstract int getViewType();

    /**
     * Calculates the layout params for views in {@link ViewHolder}.
     */
    @Override
    @CallSuper
    protected void resolveDirtyState() {
        mBinders.clear();

        // Create binders that adjust layout params of each view.
        setPrimaryAction();
        setText();
        setCompoundButton();
        setItemClickable();
    }

    /**
     * Hides all views in {@link ViewHolder} then applies ViewBinders to adjust view layout params.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onBind(@NonNull VH viewHolder) {
        hideSubViews(viewHolder);
        for (ViewBinder binder : mBinders) {
            binder.bind(viewHolder);
        }

        for (View v : viewHolder.getWidgetViews()) {
            v.setEnabled(mIsEnabled);
        }
        // SwitchListItem supports clicking on the item so we also update the entire itemView.
        viewHolder.itemView.setEnabled(mIsEnabled);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    /**
     * Sets whether a click anywhere on the list toggles the state of compound button. The
     * default state of {@code false} requires an explicit click on the compound button to toggle
     * its state.
     *
     * @param isClickable {@code true} for a click anywhere on the list to toggle the state of
     *                    the compound button.
     * @deprecated Use {@link #setEntireItemClickable(boolean)} instead.
     */
    @Deprecated
    public void setClickable(boolean isClickable) {
        mIsClickable = isClickable;
        markDirty();
    }

    /**
     * Sets whether a click anywhere on the list toggles the state of compound button. The
     * default state of {@code false} requires an explicit click on the compound button to toggle
     * its state.
     *
     * @param isClickable {@code true} for a click anywhere on the list to toggle the state of
     *                    the compound button.
     */
    public void setEntireItemClickable(boolean isClickable) {
        mIsClickable = isClickable;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param drawable the {@link Drawable} to set.
     * @param size     small/medium/large. Available as {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *                 {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM},
     *                 {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     */
    public void setPrimaryActionIcon(@NonNull Drawable drawable, @PrimaryActionIconSize int size) {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_ICON;
        mPrimaryActionIconDrawable = drawable;
        mPrimaryActionIconSize = size;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param iconResId the resource identifier of the drawable.
     * @param size      small/medium/large. Available as {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *                  {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM},
     *                  {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     */
    public void setPrimaryActionIcon(@DrawableRes int iconResId, @PrimaryActionIconSize int size) {
        setPrimaryActionIcon(mContext.getDrawable(iconResId), size);
    }

    /**
     * Sets {@code Primary Action} to be empty icon.
     *
     * <p>{@code Text} would have a start margin as if {@code Primary Action} were set to primary
     * icon.
     */
    public void setPrimaryActionEmptyIcon() {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_EMPTY_ICON;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to have no icon. Text would align to the start of item.
     */
    public void setPrimaryActionNoIcon() {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
        markDirty();
    }

    /**
     * Sets the title of item.
     *
     * <p>{@code Title} text is limited to one line, and ellipses at the end.
     *
     * @param title text to display as title.
     */
    public void setTitle(@Nullable CharSequence title) {
        mTitle = title;
        markDirty();
    }

    /**
     * Sets the body text of item.
     *
     * <p>Text beyond length required by regulation will be truncated.
     *
     * @param body text to be displayed.
     */
    public void setBody(@Nullable CharSequence body) {
        mBody = body;
        markDirty();
    }

    /**
     * Sets the state of {@link CompoundButton}.
     *
     * @param isChecked sets the "checked/unchecked, namely on/off" state of compound button.
     */
    public void setChecked(boolean isChecked) {
        if (mIsChecked == isChecked) {
            return;
        }
        mIsChecked = isChecked;
        mShouldNotifyChecked = true;
        markDirty();
    }

    /**
     * Registers a callback to be invoked when the checked state of compound button changes.
     *
     * @param listener callback to be invoked when the checked state shown in the UI changes.
     */
    public void setOnCheckedChangeListener(
            @Nullable CompoundButton.OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
        // This method invalidates previous listener. Reset so that we *only*
        // notify when the checked state changes and not on the initial bind.
        mShouldNotifyChecked = false;
        markDirty();
    }

    /**
     * Sets whether to display a vertical bar between compound button and text.
     */
    public void setShowCompoundButtonDivider(boolean showCompoundButtonDivider) {
        mShowCompoundButtonDivider = showCompoundButtonDivider;
        markDirty();
    }

    private void hideSubViews(ViewHolder vh) {
        for (View v : vh.getWidgetViews()) {
            v.setVisibility(View.GONE);
        }
    }

    private void setPrimaryAction() {
        setPrimaryIconContent();
        setPrimaryIconLayout();
    }

    private void setText() {
        setTextContent();
        setTextVerticalMargin();
        setTextStartMargin();
        setTextEndMargin();
    }

    private void setPrimaryIconContent() {
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_ICON:
                mBinders.add(vh -> {
                    vh.getPrimaryIcon().setVisibility(View.VISIBLE);
                    vh.getPrimaryIcon().setImageDrawable(mPrimaryActionIconDrawable);
                });
                break;
            case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                // Do nothing.
                break;
            case PRIMARY_ACTION_TYPE_NO_ICON:
                mBinders.add(vh -> {
                    vh.getPrimaryIcon().setVisibility(View.GONE);
                });
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    /**
     * Returns whether the compound button will be placed at the end of the list item layout. This
     * value is used to determine start margins for the {@code Title} and {@code Body}.
     *
     * @return Whether compound button is placed at the end of the list item layout.
     */
    public abstract boolean isCompoundButtonPositionEnd();

    /**
     * Sets the size, start margin, and vertical position of primary icon.
     *
     * <p>Large icon will have no start margin, and always align center vertically.
     *
     * <p>Small/medium icon will have start margin, and uses a top margin such that it is "pinned"
     * at the same position in list item regardless of item height.
     */
    private void setPrimaryIconLayout() {
        if (mPrimaryActionType == PRIMARY_ACTION_TYPE_EMPTY_ICON
                || mPrimaryActionType == PRIMARY_ACTION_TYPE_NO_ICON) {
            return;
        }

        // Size of icon.
        @DimenRes int sizeResId;
        switch (mPrimaryActionIconSize) {
            case PRIMARY_ACTION_ICON_SIZE_SMALL:
                sizeResId = R.dimen.car_primary_icon_size;
                break;
            case PRIMARY_ACTION_ICON_SIZE_MEDIUM:
                sizeResId = R.dimen.car_avatar_icon_size;
                break;
            case PRIMARY_ACTION_ICON_SIZE_LARGE:
                sizeResId = R.dimen.car_single_line_list_item_height;
                break;
            default:
                throw new IllegalStateException("Unknown primary action icon size.");
        }

        int iconSize = mContext.getResources().getDimensionPixelSize(sizeResId);

        // Start margin of icon.
        int startMargin;
        switch (mPrimaryActionIconSize) {
            case PRIMARY_ACTION_ICON_SIZE_SMALL:
            case PRIMARY_ACTION_ICON_SIZE_MEDIUM:
                startMargin = mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
                break;
            case PRIMARY_ACTION_ICON_SIZE_LARGE:
                startMargin = 0;
                break;
            default:
                throw new IllegalStateException("Unknown primary action icon size.");
        }

        mBinders.add(vh -> {
            ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();
            layoutParams.height = layoutParams.width = iconSize;
            layoutParams.setMarginStart(startMargin);

            if (mPrimaryActionIconSize == PRIMARY_ACTION_ICON_SIZE_LARGE) {
                // A large icon is always vertically centered.
                layoutParams.verticalBias = 0.5f;
                layoutParams.topMargin = 0;
            } else {
                // Align the icon to the top of the parent. This allows the topMargin to shift it
                // down relative to the top.
                layoutParams.verticalBias = 0f;

                // For all other icon sizes, the icon should be centered within the height of
                // car_double_line_list_item_height. Note: the actual height of the item can be
                // larger than this.
                int itemHeight = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_double_line_list_item_height);
                layoutParams.topMargin = (itemHeight - iconSize) / 2;
            }

            vh.getPrimaryIcon().requestLayout();
        });
    }

    private void setTextContent() {
        boolean hasTitle = !TextUtils.isEmpty(mTitle);
        boolean hasBody = !TextUtils.isEmpty(mBody);

        if (!hasTitle && !hasBody) {
            return;
        }

        mBinders.add(vh -> {
            if (hasTitle) {
                vh.getTitle().setVisibility(View.VISIBLE);
                vh.getTitle().setText(mTitle);
            }

            if (hasBody) {
                vh.getBody().setVisibility(View.VISIBLE);
                vh.getBody().setText(mBody);
            }

            if (hasTitle && !hasBody) {
                // If only title, then center the supplemental actions.
                vh.getSupplementalGuideline().setGuidelineBegin(
                        ConstraintLayout.LayoutParams.UNSET);
                vh.getSupplementalGuideline().setGuidelinePercent(0.5f);
            } else {
                // Otherwise, position it a fixed distance from the top.
                vh.getSupplementalGuideline().setGuidelinePercent(
                        ConstraintLayout.LayoutParams.UNSET);
                vh.getSupplementalGuideline().setGuidelineBegin(
                        mSupplementalGuidelineBegin);
            }
        });
    }

    /**
     * Sets start margin of text view depending on icon type.
     */
    private void setTextStartMargin() {
        @DimenRes int startMarginResId;
        if (!isCompoundButtonPositionEnd()) {
            startMarginResId = R.dimen.car_keyline_3;
        } else {
            switch (mPrimaryActionType) {
                case PRIMARY_ACTION_TYPE_NO_ICON:
                    startMarginResId = R.dimen.car_keyline_1;
                    break;
                case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                    startMarginResId = R.dimen.car_keyline_3;
                    break;
                case PRIMARY_ACTION_TYPE_ICON:
                    startMarginResId = mPrimaryActionIconSize == PRIMARY_ACTION_ICON_SIZE_LARGE
                            ? R.dimen.car_keyline_4
                            : R.dimen.car_keyline_3;  // Small and medium sized icon.
                    break;
                default:
                    throw new IllegalStateException("Unknown primary action type.");
            }
        }

        int startMargin = mContext.getResources().getDimensionPixelSize(startMarginResId);
        mBinders.add(vh -> {
            MarginLayoutParams titleLayoutParams =
                    (MarginLayoutParams) vh.getTitle().getLayoutParams();
            titleLayoutParams.setMarginStart(startMargin);
            vh.getTitle().requestLayout();

            MarginLayoutParams bodyLayoutParams =
                    (MarginLayoutParams) vh.getBody().getLayoutParams();
            bodyLayoutParams.setMarginStart(startMargin);
            vh.getBody().requestLayout();
        });
    }

    private void setTextEndMargin() {
        int endMargin = mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4);

        mBinders.add(vh -> {
            MarginLayoutParams titleLayoutParams =
                    (MarginLayoutParams) vh.getTitle().getLayoutParams();
            titleLayoutParams.setMarginEnd(endMargin);

            MarginLayoutParams bodyLayoutParams =
                    (MarginLayoutParams) vh.getBody().getLayoutParams();
            bodyLayoutParams.setMarginEnd(endMargin);
        });
    }

    /**
     * Sets top/bottom margins of {@code Title} and {@code Body}.
     */
    private void setTextVerticalMargin() {
        // Set all relevant fields in layout params to avoid carried over params when the item
        // gets bound to a recycled view holder.
        if (!TextUtils.isEmpty(mTitle) && TextUtils.isEmpty(mBody)) {
            // Title only - view is aligned center vertically by itself.
            mBinders.add(vh -> {
                MarginLayoutParams layoutParams =
                        (MarginLayoutParams) vh.getTitle().getLayoutParams();
                layoutParams.topMargin = 0;
                vh.getTitle().requestLayout();
            });
        } else if (TextUtils.isEmpty(mTitle) && !TextUtils.isEmpty(mBody)) {
            mBinders.add(vh -> {
                // Body uses top and bottom margin.
                int margin = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_padding_3);
                MarginLayoutParams layoutParams =
                        (MarginLayoutParams) vh.getBody().getLayoutParams();
                layoutParams.topMargin = margin;
                layoutParams.bottomMargin = margin;
                vh.getBody().requestLayout();
            });
        } else {
            mBinders.add(vh -> {
                Resources resources = mContext.getResources();
                int padding2 = resources.getDimensionPixelSize(R.dimen.car_padding_2);

                // Title has a top margin
                MarginLayoutParams titleLayoutParams =
                        (MarginLayoutParams) vh.getTitle().getLayoutParams();
                titleLayoutParams.topMargin = padding2;
                vh.getTitle().requestLayout();

                // Body is below title with no margin and has bottom margin.
                MarginLayoutParams bodyLayoutParams =
                        (MarginLayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.topMargin = 0;
                bodyLayoutParams.bottomMargin = padding2;
                vh.getBody().requestLayout();
            });
        }
    }

    /**
     * Sets up view(s) for supplemental action.
     */
    private void setCompoundButton() {
        mBinders.add(vh -> {
            vh.getCompoundButton().setVisibility(View.VISIBLE);
            vh.getCompoundButton().setOnCheckedChangeListener(null);
            vh.getCompoundButton().setChecked(mIsChecked);
            vh.getCompoundButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mOnCheckedChangeListener != null) {
                    // The checked state changed via user interaction with the compound button.
                    mOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                }
                mIsChecked = isChecked;
            });
            if (mShouldNotifyChecked && mOnCheckedChangeListener != null) {
                // The checked state was changed programmatically.
                mOnCheckedChangeListener.onCheckedChanged(vh.getCompoundButton(),
                        mIsChecked);
                mShouldNotifyChecked = false;
            }

            if (mShowCompoundButtonDivider) {
                vh.getCompoundButtonDivider().setVisibility(View.VISIBLE);
            }
        });
    }

    private void setItemClickable() {
        mBinders.add(vh -> {
            // If applicable (namely item is clickable), clicking item always toggles the
            // compound button.
            vh.itemView.setOnClickListener(v -> vh.getCompoundButton().toggle());
            vh.itemView.setClickable(mIsClickable);
        });
    }

    /**
     * Holds views of CompoundButtonListItem.
     */
    public abstract static class ViewHolder extends ListItem.ViewHolder {

        /**
         * Creates a {@link ViewHolder} for a {@link CompoundButtonListItem}.
         *
         * @param itemView The view to be used to display a {@link CompoundButtonListItem}.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        /**
         * Returns the primary icon view within this view holder's view.
         *
         * @return Icon view within this view holder's view.
         */
        @NonNull
        public abstract ImageView getPrimaryIcon();

        /**
         * Returns the title view within this view holder's view.
         *
         * @return Title view within this view holder's view.
         */
        @NonNull
        public abstract TextView getTitle();

        /**
         * Returns the body view within this view holder's view.
         *
         * @return Body view within this view holder's view.
         */
        @NonNull
        public abstract TextView getBody();

        /**
         * Returns the compound button divider view within this view holder's view.
         *
         * @return Compound button divider view within this view holder's view.
         */
        @NonNull
        public abstract View getCompoundButtonDivider();

        /**
         * Returns the compound button within this view holder's view.
         *
         * @return Compound button within this view holder's view.
         */
        @NonNull
        public abstract CompoundButton getCompoundButton();

        @NonNull
        abstract Guideline getSupplementalGuideline();

        @NonNull
        abstract ViewGroup getContainerLayout();

        /**
         * Returns the container layout of this view holder.
         *
         * @return Container layout of this view holder.
         */
        @NonNull
        abstract View[] getWidgetViews();
    }
}
