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

package androidx.car.widget;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.car.uxrestrictions.OnUxRestrictionsChangedListener;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a list item with {@link SeekBar}.
 *
 * <p>An item supports primary action and supplemental action(s).
 *
 * <p>An item visually composes of 3 parts; each part may contain multiple views.
 * <ul>
 *     <li>{@code Primary Action}: represented by an icon of following types.
 *     <ul>
 *         <li>Primary Icon - icon size could be large or small.
 *         <li>No Icon - no icon is shown.
 *         <li>Empty Icon - {@code Seekbar} offsets start space as if there was an icon.
 *     </ul>
 *     <li>{@code Seekbar}: with optional {@code Text}.
 *     <li>{@code Supplemental Action}: presented by an icon of following types; aligned to
 *     the end of item.
 *     <ul>
 *         <li>Supplemental Icon.
 *         <li>Supplemental Empty Icon - {@code Seekbar} offsets end space as if there was an icon.
 *     </ul>
 * </ul>
 *
 * {@code SeekbarListItem} binds data to {@link ViewHolder} based on components selected.
 *
 * <p>When conflicting methods are called (e.g. setting primary action to both primary icon and
 * no icon), the last called method wins.
 *
 * {@code minimum value} is set to 0.
 */
public class SeekbarListItem extends ListItem<SeekbarListItem.ViewHolder> {

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_TYPE_NO_ICON, PRIMARY_ACTION_TYPE_EMPTY_ICON,
            PRIMARY_ACTION_TYPE_SMALL_ICON})
    private @interface PrimaryActionType {}

    private static final int PRIMARY_ACTION_TYPE_NO_ICON = 0;
    private static final int PRIMARY_ACTION_TYPE_EMPTY_ICON = 1;
    private static final int PRIMARY_ACTION_TYPE_SMALL_ICON = 2;

    @Retention(SOURCE)
    @IntDef({SUPPLEMENTAL_ACTION_NO_ACTION, SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON,
            SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON,
            SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON_WITH_DIVIDER})
    private @interface SupplementalActionType {}

    private static final int SUPPLEMENTAL_ACTION_NO_ACTION = 0;
    private static final int SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON = 1;
    private static final int SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON = 2;
    private static final int SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON_WITH_DIVIDER = 3;

    private final Context mContext;
    private boolean mIsEnabled = true;

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    @PrimaryActionType private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
    private Drawable mPrimaryActionIconDrawable;
    private View.OnClickListener mPrimaryActionIconOnClickListener;

    @Nullable private CharSequence mText;

    private int mMax;
    private int mProgress;
    private int mSecondaryProgress;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;

    @Dimension
    private final int mSupplementalGuidelineBegin;

    @SupplementalActionType private int mSupplementalActionType = SUPPLEMENTAL_ACTION_NO_ACTION;
    private Drawable mSupplementalIconDrawable;
    private View.OnClickListener mSupplementalIconOnClickListener;
    private boolean mShowSupplementalIconDivider;

    /**
     * Creates a {@link SeekbarListItem.ViewHolder}.
     */
    @NonNull
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public SeekbarListItem(@NonNull Context context) {
        mContext = context;
        mSupplementalGuidelineBegin = mContext.getResources().getDimensionPixelSize(
                R.dimen.car_list_item_supplemental_guideline_top);
        markDirty();
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_SEEKBAR;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

    /**
     * Sets max value of seekbar.
     */
    public void setMax(int max) {
        mMax = max;
        markDirty();
    }

    /**
     * Sets progress of seekbar.
     */
    public void setProgress(int progress) {
        mProgress = progress;
        markDirty();
    }

    /**
     * Sets secondary progress of seekbar.
     */
    public void setSecondaryProgress(int secondaryProgress) {
        mSecondaryProgress = secondaryProgress;
        markDirty();
    }

    /**
     * Sets {@link SeekBar.OnSeekBarChangeListener}.
     */
    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener listener) {
        mOnSeekBarChangeListener = listener;
        markDirty();
    }

    /**
     * Sets text that sits on top of seekbar.
     *
     * @param text text to be displayed, or {@code null} to clear the content.
     */
    public void setText(@Nullable CharSequence text) {
        mText = text;
        markDirty();
    }

    /**
     * Calculates the layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();

        // Create binders that adjust layout params of each view.
        setPrimaryAction();
        setSeekBarAndText();
        setSupplementalAction();
    }

    /**
     * Hides all views in {@link ViewHolder} then applies ViewBinders to adjust view layout params.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(ViewHolder viewHolder) {
        // Hide all subviews then apply view binders to adjust subviews.
        hideSubViews(viewHolder);
        for (ViewBinder binder : mBinders) {
            binder.bind(viewHolder);
        }

        for (View v : viewHolder.getWidgetViews()) {
            v.setEnabled(mIsEnabled);
        }
    }

    private void hideSubViews(ViewHolder vh) {
        for (View v : vh.getWidgetViews()) {
            v.setVisibility(View.GONE);
        }
        // SeekBar is always visible.
        vh.getSeekBar().setVisibility(View.VISIBLE);
    }

    private void setPrimaryAction() {
        setPrimaryActionLayout();
        setPrimaryActionContent();
    }

    private void setSeekBarAndText() {
        setSeekBarAndTextContent();
        setSeekBarAndTextLayout();
    }

    private void setPrimaryActionLayout() {
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_NO_ICON:
            case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                // Do nothing.
                break;
            case PRIMARY_ACTION_TYPE_SMALL_ICON:
                mBinders.add(vh -> {
                    ConstraintLayout.LayoutParams layoutParams =
                            (ConstraintLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();

                    if (TextUtils.isEmpty(mText)) {
                        // If there is no text, then the icon should be vertically centered.
                        layoutParams.verticalBias = 0.5f;
                        layoutParams.topMargin = 0;
                    } else {
                        // Align the icon to  the opt of the parent. This allows the topMargin to
                        // shift it down relative to the top.
                        layoutParams.verticalBias = 0f;

                        Resources res = mContext.getResources();

                        // Set icon top margin so that the icon remains in the same position it
                        // would've been in for non-long-text item, namely so that the center
                        // line of icon matches that of line item.
                        int itemHeight = res.getDimensionPixelSize(
                                R.dimen.car_double_line_list_item_height);
                        int iconSize = res.getDimensionPixelSize(R.dimen.car_primary_icon_size);
                        layoutParams.topMargin = (itemHeight - iconSize) / 2;
                    }

                    vh.getPrimaryIcon().requestLayout();
                });
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    private void setPrimaryActionContent() {
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_NO_ICON:
            case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                // Do nothing.
                break;
            case PRIMARY_ACTION_TYPE_SMALL_ICON:
                mBinders.add(vh -> {
                    vh.getPrimaryIcon().setVisibility(View.VISIBLE);

                    vh.getPrimaryIcon().setImageDrawable(mPrimaryActionIconDrawable);
                    vh.getPrimaryIcon().setOnClickListener(
                            mPrimaryActionIconOnClickListener);
                    vh.getPrimaryIcon().setClickable(
                            mPrimaryActionIconOnClickListener != null);
                });
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    private void setSeekBarAndTextContent() {
        mBinders.add(vh -> {
            vh.getSeekBar().setMax(mMax);
            vh.getSeekBar().setProgress(mProgress);
            vh.getSeekBar().setSecondaryProgress(mSecondaryProgress);
            vh.getSeekBar().setOnSeekBarChangeListener(mOnSeekBarChangeListener);

            if (!TextUtils.isEmpty(mText)) {
                vh.getText().setVisibility(View.VISIBLE);
                vh.getText().setText(mText);

                // If there is a title, the ensure the guideline is a fixed
                vh.getSupplementalGuideline().setGuidelineBegin(
                        mSupplementalGuidelineBegin);
                vh.getSupplementalGuideline().setGuidelinePercent(
                        ConstraintLayout.LayoutParams.UNSET);
            } else {
                // Otherwise, the guideline should center the supplemental icon.
                vh.getSupplementalGuideline().setGuidelineBegin(
                        ConstraintLayout.LayoutParams.UNSET);
                vh.getSupplementalGuideline().setGuidelinePercent(0.5f);
            }
        });
    }

    private void setSeekBarAndTextLayout() {
        mBinders.add(vh -> {
            // Set start and end margin of text and seek bar.
            int marginStart = getSeekBarAndTextMarginStart();
            int marginEnd = getSeekBarAndTextMarginEnd();

            ViewGroup.MarginLayoutParams textLayoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getText().getLayoutParams();
            textLayoutParams.setMarginStart(marginStart);
            textLayoutParams.setMarginEnd(marginEnd);
            vh.getText().requestLayout();

            ConstraintLayout.LayoutParams seekBarLayoutParams =
                    (ConstraintLayout.LayoutParams) vh.getSeekBar().getLayoutParams();
            seekBarLayoutParams.setMarginStart(marginStart);
            seekBarLayoutParams.setMarginEnd(marginEnd);

            if (TextUtils.isEmpty(mText)) {
                // If there is no text, set the vertical bias to 0.5 so that the seekbar is
                // vertically centered.
                seekBarLayoutParams.verticalBias = 0.5f;
                seekBarLayoutParams.bottomMargin = 0;
            } else {
                // If there is text, set the vertical bias to 1 so that it is aligned to the bottom
                // of the parent view, allowing the bottom margin to take effect.
                seekBarLayoutParams.verticalBias = 1f;
                seekBarLayoutParams.bottomMargin =
                        mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_1);
            }

            vh.getSeekBar().requestLayout();
        });
    }

    /**
     * Returns the starting margin that should be used for the text and seekbar views. This value
     * will depend on whether or not a primary icon is currently visible.
     */
    private int getSeekBarAndTextMarginStart() {
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_NO_ICON:
                return mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
            case PRIMARY_ACTION_TYPE_EMPTY_ICON:
            case PRIMARY_ACTION_TYPE_SMALL_ICON:
                return mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_3);
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    /**
     * Returns the end margin that should be used for the text and seekbar views. This value
     * will depend on whether or not supplemental icons are visible.
     */
    private int getSeekBarAndTextMarginEnd() {
        int endMargin = 0;
        switch (mSupplementalActionType) {
            case SUPPLEMENTAL_ACTION_NO_ACTION:
                // Aligned to parent end.
                return 0;
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON:
                // Add padding to account for the supplemental icon.
                return mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4);
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON_WITH_DIVIDER:
                // Align to parent end with a margin as if the icon and an optional divider were
                // present. We do this by adding the divider padding to icon, and width of divider.
                endMargin += mContext.getResources().getDimensionPixelSize(
                         R.dimen.car_padding_4);
                endMargin += mContext.getResources().getDimensionPixelSize(
                         R.dimen.car_vertical_line_divider_width);
                // Fall through.
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON:
                // Add view padding, width of icon, and icon end margin.
                endMargin += mContext.getResources().getDimensionPixelSize(
                         R.dimen.car_padding_4);
                endMargin += mContext.getResources().getDimensionPixelSize(
                         R.dimen.car_primary_icon_size);

                return endMargin;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
    }

    private void setSupplementalAction() {
        switch (mSupplementalActionType) {
            case SUPPLEMENTAL_ACTION_NO_ACTION:
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON_WITH_DIVIDER:
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON:
                break;
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON:
                mBinders.add(vh -> {
                    vh.getSupplementalIcon().setVisibility(View.VISIBLE);
                    if (mShowSupplementalIconDivider) {
                        vh.getSupplementalIconDivider().setVisibility(View.VISIBLE);
                    }
                    vh.getSupplementalIcon().setImageDrawable(mSupplementalIconDrawable);

                    vh.getSupplementalIcon().setOnClickListener(
                            mSupplementalIconOnClickListener);
                    vh.getSupplementalIcon().setClickable(
                            mSupplementalIconOnClickListener != null);
                });
                break;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param iconResId the resource identifier of the drawable.
     */
    public void setPrimaryActionIcon(@DrawableRes int iconResId) {
        setPrimaryActionIcon(getContext().getDrawable(iconResId));
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param drawable the Drawable to set.
     */
    public void setPrimaryActionIcon(@NonNull Drawable drawable) {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_SMALL_ICON;
        mPrimaryActionIconDrawable = drawable;
        markDirty();
    }

    /**
     * Sets an {@code OnClickListener} for the icon representing the {@code Primary Action}.
     *
     * @param onClickListener the listener to be set for the primary action icon.
     */
    public void setPrimaryActionIconListener(View.OnClickListener onClickListener) {
        mPrimaryActionIconOnClickListener = onClickListener;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be empty icon.
     *
     * {@code Seekbar} would have a start margin as if {@code Primary Action} were set as icon.
     */
    public void setPrimaryActionEmptyIcon() {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_EMPTY_ICON;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
     */
    public void setSupplementalIcon(@DrawableRes int iconResId,
            boolean showSupplementalIconDivider) {
        setSupplementalIcon(getContext().getDrawable(iconResId),
                showSupplementalIconDivider);
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
     */
    public void setSupplementalIcon(@NonNull Drawable drawable,
            boolean showSupplementalIconDivider) {
        mSupplementalActionType = SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON;
        mSupplementalIconDrawable = drawable;
        mShowSupplementalIconDivider = showSupplementalIconDivider;
        markDirty();
    }

    /**
     * Sets {@code OnClickListener} for a {@code Supplemental Icon}.
     */
    public void setSupplementalIconListener(View.OnClickListener listener) {
        mSupplementalIconOnClickListener = listener;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be empty icon.
     *
     * {@code Seekbar} would have an end margin as if {@code Supplemental Action} were set.
     */
    public void setSupplementalEmptyIcon(boolean seekbarOffsetDividerWidth) {
        mSupplementalActionType = seekbarOffsetDividerWidth
                ? SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON_WITH_DIVIDER
                : SUPPLEMENTAL_ACTION_SUPPLEMENTAL_EMPTY_ICON;
        markDirty();
    }

    /**
     * Holds views of SeekbarListItem.
     */
    public static final class ViewHolder extends ListItem.ViewHolder implements
            OnUxRestrictionsChangedListener {

        private final View[] mWidgetViews;

        private ViewGroup mContainerLayout;

        private ImageView mPrimaryIcon;

        private TextView mText;
        private SeekBar mSeekBar;

        private Guideline mSupplementalGuideline;
        private View mSupplementalIconDivider;
        private ImageView mSupplementalIcon;

        public ViewHolder(View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mText = itemView.findViewById(R.id.seek_bar_text);
            mSeekBar = itemView.findViewById(R.id.seek_bar);

            mSupplementalGuideline = itemView.findViewById(R.id.supplemental_icon_guideline);
            mSupplementalIcon = itemView.findViewById(R.id.supplemental_icon);
            mSupplementalIconDivider = itemView.findViewById(R.id.supplemental_icon_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);

            MinTouchTargetHelper.ensureThat(mSupplementalIcon)
                    .hasMinTouchSize(minTouchSize);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[]{
                    mPrimaryIcon,
                    mSeekBar, mText,
                    mSupplementalIcon, mSupplementalIconDivider};
        }

        /**
         * Updates child views with current car UX restrictions.
         *
         * <p>{@code Text} might be truncated to meet length limit required by regulation.
         *
         * @param restrictionsInfo current car UX restrictions.
         */
        @Override
        public void onUxRestrictionsChanged(CarUxRestrictions restrictionsInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionsInfo, getText());
        }

        @NonNull
        public ViewGroup getContainerLayout() {
            return mContainerLayout;
        }

        @NonNull
        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        @NonNull
        public TextView getText() {
            return mText;
        }

        @NonNull
        public SeekBar getSeekBar() {
            return mSeekBar;
        }

        @NonNull
        public ImageView getSupplementalIcon() {
            return mSupplementalIcon;
        }

        @NonNull
        public View getSupplementalIconDivider() {
            return mSupplementalIconDivider;
        }

        /** Returns the guideline that the supplemental icon is centered upon. */
        @NonNull
        Guideline getSupplementalGuideline() {
            return mSupplementalGuideline;
        }

        @NonNull
        View[] getWidgetViews() {
            return mWidgetViews;
        }
    }
}
