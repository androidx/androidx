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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a list item that has up to two actions.
 *
 * <p>An item visually composes of 3 parts; each part may contain multiple views.
 * <ul>
 *     <li>{@code Primary Action}: represented by an icon of following types.
 *     <ul>
 *         <li>Primary Icon - icon size could be large or small.
 *         <li>No Icon - no icon is shown.
 *         <li>Empty Icon - {@code Text} offsets start space as if there was an icon.
 *     </ul>
 *     <li>{@code Text}: supports any combination of the following text views.
 *     <ul>
 *         <li>Title
 *         <li>Body
 *     </ul>
 *     <li>{@code Supplemental Action}: Up to two actions.
 * </ul>
 *
 * <p>{@code ActionListItem} binds data to {@link ViewHolder} based on components selected.
 *
 * <p>When conflicting setter methods are called (e.g. setting primary action to both primary icon
 * and no icon), the last called method wins.
 */
public final class ActionListItem extends ListItem<ActionListItem.ViewHolder> {
    @Retention(SOURCE)
    @IntDef({PRIMARY_ACTION_ICON_SIZE_SMALL, PRIMARY_ACTION_ICON_SIZE_MEDIUM,
            PRIMARY_ACTION_ICON_SIZE_LARGE})
    private @interface PrimaryActionIconSize {}

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
    private @interface PrimaryActionType {}

    private static final int PRIMARY_ACTION_TYPE_NO_ICON = 0;
    private static final int PRIMARY_ACTION_TYPE_EMPTY_ICON = 1;
    private static final int PRIMARY_ACTION_TYPE_ICON = 2;

    @PrimaryActionType private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
    private Drawable mPrimaryActionIconDrawable;
    @PrimaryActionIconSize private int mPrimaryActionIconSize = PRIMARY_ACTION_ICON_SIZE_SMALL;

    private final Context mContext;
    private boolean mIsEnabled = true;
    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    private CharSequence mTitle;
    private CharSequence mBody;

    @Dimension
    private final int mSupplementalGuidelineBegin;

    private boolean mIsActionBorderless = true;
    private String mPrimaryActionText;
    private View.OnClickListener mPrimaryActionOnClickListener;
    private boolean mShowPrimaryActionDivider;

    private String mSecondaryActionText;
    private View.OnClickListener mSecondaryActionOnClickListener;
    private boolean mShowSecondaryActionDivider;

    private View.OnClickListener mOnClickListener;

    /**
     * Creates a {@link ActionListItem.ViewHolder}.
     */
    @NonNull
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public ActionListItem(@NonNull Context context) {
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
        return ListItemAdapter.LIST_ITEM_TYPE_ACTION;
    }

    /**
     * Resets all views in {@link ActionListItem.ViewHolder} then applies ViewBinders to
     * adjust view layout params.
     */
    @Override
    public void onBind(ActionListItem.ViewHolder viewHolder) {
        for (View v : viewHolder.getWidgetViews()) {
            v.setEnabled(mIsEnabled);
            v.setVisibility(View.GONE);
        }

        // ActionListItem supports clicking on the item so we also update the entire itemView.
        viewHolder.itemView.setEnabled(mIsEnabled);

        for (ViewBinder<ViewHolder> binder : mBinders) {
            binder.bind(viewHolder);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * Calculates the layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();

        // Create binders that adjust layout params of each view.
        setPrimaryAction();
        setText();
        setSupplementalActions();
        setOnClickListener();
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

    private void setPrimaryAction() {
        setPrimaryIconContent();
        setPrimaryIconLayout();
    }

    private void setText() {
        setTextContent();
        setTextVerticalMargin();
        setTextStartMargin();
    }

    private void setOnClickListener() {
        mBinders.add(vh -> {
            vh.itemView.setOnClickListener(mOnClickListener);
            vh.itemView.setClickable(mOnClickListener != null);
        });
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
            case PRIMARY_ACTION_TYPE_NO_ICON:
                // Do nothing.
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

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
        int startMargin = mContext.getResources().getDimensionPixelSize(startMarginResId);
        mBinders.add(vh -> {
            ViewGroup.MarginLayoutParams titleLayoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getTitle().getLayoutParams();
            titleLayoutParams.setMarginStart(startMargin);
            vh.getTitle().requestLayout();

            ViewGroup.MarginLayoutParams bodyLayoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getBody().getLayoutParams();
            bodyLayoutParams.setMarginStart(startMargin);
            vh.getBody().requestLayout();
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
                ViewGroup.MarginLayoutParams layoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getTitle().getLayoutParams();
                layoutParams.topMargin = 0;
                vh.getTitle().requestLayout();
            });
        } else if (TextUtils.isEmpty(mTitle) && !TextUtils.isEmpty(mBody)) {
            mBinders.add(vh -> {
                // Body uses top and bottom margin.
                int margin = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_padding_3);
                ViewGroup.MarginLayoutParams layoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getBody().getLayoutParams();
                layoutParams.topMargin = margin;
                layoutParams.bottomMargin = margin;
                vh.getBody().requestLayout();
            });
        } else {
            mBinders.add(vh -> {
                Resources resources = mContext.getResources();
                int padding2 = resources.getDimensionPixelSize(R.dimen.car_padding_2);

                // Title has a top margin
                ViewGroup.MarginLayoutParams titleLayoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getTitle().getLayoutParams();
                titleLayoutParams.topMargin = padding2;
                vh.getTitle().requestLayout();

                // Body is below title with no margin and has bottom margin.
                ViewGroup.MarginLayoutParams bodyLayoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.topMargin = 0;
                bodyLayoutParams.bottomMargin = padding2;
                vh.getBody().requestLayout();
            });
        }
    }

    /**
     * Sets up view(s) for supplemental action.
     */
    private void setSupplementalActions() {
        boolean hasPrimaryAction = !TextUtils.isEmpty(mPrimaryActionText);
        boolean hasSecondaryAction = !TextUtils.isEmpty(mSecondaryActionText);

        if (!hasPrimaryAction && !hasSecondaryAction) {
            return;
        }

        mBinders.add(vh -> {
            vh.setActionBorderless(mIsActionBorderless);

            if (hasSecondaryAction) {
                Button secondaryAction = vh.getSecondaryAction();

                secondaryAction.setVisibility(View.VISIBLE);
                if (mShowSecondaryActionDivider) {
                    vh.getSecondaryActionDivider().setVisibility(View.VISIBLE);
                }

                secondaryAction.setText(mSecondaryActionText);
                secondaryAction.setOnClickListener(mSecondaryActionOnClickListener);

                // Add spacing between the buttons if there is a primary action.
                int endMargin = hasPrimaryAction
                        ? mContext.getResources().getDimensionPixelSize(R.dimen.car_padding_4)
                        : 0;

                ViewGroup.MarginLayoutParams layoutParams =
                        (ViewGroup.MarginLayoutParams) secondaryAction.getLayoutParams();
                layoutParams.setMarginEnd(endMargin);
                secondaryAction.requestLayout();
            }

            if (hasPrimaryAction) {
                Button primaryAction = vh.getPrimaryAction();

                primaryAction.setVisibility(View.VISIBLE);
                if (mShowPrimaryActionDivider) {
                    vh.getPrimaryActionDivider().setVisibility(View.VISIBLE);
                }

                primaryAction.setText(mPrimaryActionText);
                primaryAction.setOnClickListener(mPrimaryActionOnClickListener);
            }
        });
    }

    /**
     * Sets {@link View.OnClickListener} of {@code ActionListItem}.
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param iconResId the resource identifier of the drawable.
     * @param size The size of the icon. Must be one of {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *             {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM}, or
     *             {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     */
    public void setPrimaryActionIcon(@DrawableRes int iconResId, @PrimaryActionIconSize int size) {
        setPrimaryActionIcon(mContext.getDrawable(iconResId), size);
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param drawable the Drawable to set, or null to clear the content.
     * @param size The size of the icon. Must be one of {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *             {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM}, or
     *             {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     */
    public void setPrimaryActionIcon(@Nullable Drawable drawable, @PrimaryActionIconSize int size) {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_ICON;
        mPrimaryActionIconDrawable = drawable;
        mPrimaryActionIconSize = size;
        markDirty();
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
     * <p>{@code Title} text is limited to one line, and ellipsizes at the end.
     *
     * @param title text to display as title.
     */
    public void setTitle(@NonNull CharSequence title) {
        mTitle = title;
        markDirty();
    }

    /**
     * Sets the body text of item.
     *
     * <p>Text beyond length required by regulation will be truncated. Defaults {@code Title}
     * text as the primary.
     * @param body text to be displayed.
     */
    public void setBody(@NonNull CharSequence body) {
        mBody = body;
        markDirty();
    }

    /**
     * Sets the primary action of this {@code ListItem}.
     *
     * @param text button text to display.
     * @param showDivider whether to display a vertical bar that separates {@code Text} and
     *                    {@code Action Button}.
     * @param listener the callback that will run when action button is clicked.
     * @deprecated Use {@link #setPrimaryAction(String, boolean, View.OnClickListener)} or
     * {@link #setSecondaryAction(String, boolean, View.OnClickListener)} instead to individually
     * set the actions.
     */
    @Deprecated
    public void setAction(@NonNull String text, boolean showDivider,
            @NonNull View.OnClickListener listener) {
        setPrimaryAction(text, showDivider, listener);
    }

    /**
     * Sets the primary and secondary actions for this {@code ListItem}.
     *
     * @param primaryActionText The primary action text.
     * @param showPrimaryActionDivider Whether or not to show a divider before the primary action.
     * @param primaryActionOnClickListener The listener to be invoked when the primary action is
     *                                     triggered.
     * @param secondaryActionText The secondary action text.
     * @param showSecondaryActionDivider Whether or not to show a divider before the secondary
     *                                   action.
     * @param secondaryActionOnClickListener The listener to be invoked when the secondary action is
     *                                       triggered.
     * @deprecated Use {@link #setPrimaryAction(String, boolean, View.OnClickListener)} and
     * {@link #setSecondaryAction(String, boolean, View.OnClickListener)} to set both actions.
     */
    @Deprecated
    public void setActions(@NonNull String primaryActionText, boolean showPrimaryActionDivider,
            @NonNull View.OnClickListener primaryActionOnClickListener,
            @NonNull String secondaryActionText, boolean showSecondaryActionDivider,
            @NonNull View.OnClickListener secondaryActionOnClickListener) {
        setPrimaryAction(primaryActionText, showPrimaryActionDivider, primaryActionOnClickListener);
        setSecondaryAction(secondaryActionText, showSecondaryActionDivider,
                secondaryActionOnClickListener);
    }

    /**
     * Sets the primary action of this {@code ListItem}.
     *
     * @param primaryActionText Action text to display.
     * @param showPrimaryActionDivider Whether or not to display a vertical bar before the primary
     *                                 action.
     * @param primaryActionOnClickListener The callback that will run when the action is clicked.
     *
     * @throws IllegalArgumentException If {@code primaryActionText} is {@code null} or empty.
     * @throws IllegalArgumentException If {@code primaryActionOnClickListener} is {@code null}.
     */
    public void setPrimaryAction(@NonNull String primaryActionText,
            boolean showPrimaryActionDivider,
            @NonNull View.OnClickListener primaryActionOnClickListener) {
        if (TextUtils.isEmpty(primaryActionText)) {
            throw new IllegalArgumentException("Action text cannot be empty.");
        }
        if (primaryActionOnClickListener == null) {
            throw new IllegalArgumentException("Action OnClickListener cannot be null.");
        }

        mPrimaryActionText = primaryActionText;
        mPrimaryActionOnClickListener = primaryActionOnClickListener;
        mShowPrimaryActionDivider = showPrimaryActionDivider;

        markDirty();
    }

    /**
     * Sets the secondary action of this {@code ListItem}.
     *
     * <p>The secondary action will appear before the primary action if both are set.
     *
     * @param secondaryActionText Action text to display.
     * @param showSecondaryActionDivider Whether or not to display a vertical bar before the
     *                                   secondary action.
     * @param secondaryActionOnClickListener The callback that will run when the action is clicked.
     *
     * @throws IllegalArgumentException If {@code secondaryActionText} is {@code null} or empty.
     * @throws IllegalArgumentException If {@code secondaryActionOnClickListener} is {@code null}.
     */
    public void setSecondaryAction(@NonNull String secondaryActionText,
            boolean showSecondaryActionDivider,
            @NonNull View.OnClickListener secondaryActionOnClickListener) {
        if (TextUtils.isEmpty(secondaryActionText)) {
            throw new IllegalArgumentException("Action text cannot be empty.");
        }
        if (secondaryActionOnClickListener == null) {
            throw new IllegalArgumentException("Action OnClickListener cannot be null.");
        }

        mSecondaryActionText = secondaryActionText;
        mSecondaryActionOnClickListener = secondaryActionOnClickListener;
        mShowSecondaryActionDivider = showSecondaryActionDivider;

        markDirty();
    }

    /**
     * Sets whether or not the actions should be styled as borderless.
     *
     * <p>By default, this value is {@code true}.
     *
     * @param isActionBorderless {@code true} if the actions should be borderless. {@code false}
     *                           otherwise.
     */
    public void setActionBorderless(boolean isActionBorderless) {
        mIsActionBorderless = isActionBorderless;
    }

    /**
     * Holds the children views of {@link ActionListItem}.
     */
    public static final class ViewHolder extends ListItem.ViewHolder {
        private final View[] mWidgetViews;

        private ImageView mPrimaryIcon;

        private TextView mTitle;
        private TextView mBody;

        private boolean mIsActionBorderless = true;

        private Guideline mSupplementalGuideline;

        private Button mPrimaryActionBorderless;
        private Button mPrimaryAction;
        private View mPrimaryActionDivider;

        private Button mSecondaryActionBorderless;
        private Button mSecondaryAction;
        private View mSecondaryActionDivider;

        private View mClickInterceptor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);

            mSupplementalGuideline = itemView.findViewById(R.id.actions_guideline);

            mPrimaryAction = itemView.findViewById(R.id.primary_action);
            mPrimaryActionBorderless = itemView.findViewById(R.id.primary_action_borderless);
            mPrimaryActionDivider = itemView.findViewById(R.id.primary_action_divider);
            mSecondaryAction = itemView.findViewById(R.id.secondary_action);
            mSecondaryActionBorderless = itemView.findViewById(R.id.secondary_action_borderless);
            mSecondaryActionDivider = itemView.findViewById(R.id.secondary_action_divider);

            mClickInterceptor = itemView.findViewById(R.id.click_interceptor);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[] {
                    // Primary action.
                    mPrimaryIcon,
                    // Text.
                    mTitle, mBody,
                    // Supplemental actions
                    mPrimaryAction,
                    mPrimaryActionBorderless,
                    mPrimaryActionDivider,
                    mSecondaryAction,
                    mSecondaryActionBorderless,
                    mSecondaryActionDivider
            };
        }

        @Override
        public void onUxRestrictionsChanged(@NonNull CarUxRestrictions restrictions) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictions, getBody());
        }

        /**
         * Sets if the action returned is styled as borderless or non-borderless.
         *
         * <p>By default, this value is {@code true}.
         *
         * @param isBorderless Whether or not the action is borderless.
         */
        public void setActionBorderless(boolean isBorderless) {
            mIsActionBorderless = isBorderless;
        }

        @NonNull
        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        @NonNull
        public TextView getTitle() {
            return mTitle;
        }

        @NonNull
        public TextView getBody() {
            return mBody;
        }

        @NonNull
        public Button getPrimaryAction() {
            return mIsActionBorderless ? mPrimaryActionBorderless : mPrimaryAction;
        }

        @NonNull
        @VisibleForTesting
        Button getBorderlessPrimaryAction() {
            return mPrimaryActionBorderless;
        }

        @NonNull
        @VisibleForTesting
        Button getBorderedPrimaryAction() {
            return mPrimaryAction;
        }

        @NonNull
        public View getPrimaryActionDivider() {
            return mPrimaryActionDivider;
        }

        @NonNull
        public Button getSecondaryAction() {
            return mIsActionBorderless ? mSecondaryActionBorderless : mSecondaryAction;
        }

        @NonNull
        @VisibleForTesting
        Button getBorderlessSecondaryAction() {
            return mSecondaryActionBorderless;
        }

        @NonNull
        @VisibleForTesting
        Button getBorderedSecondaryAction() {
            return mSecondaryAction;
        }

        @NonNull
        public View getSecondaryActionDivider() {
            return mSecondaryActionDivider;
        }

        @NonNull
        View[] getWidgetViews() {
            return mWidgetViews;
        }

        /** Returns the Guideline that the actions should be centered upon. */
        @NonNull
        Guideline getSupplementalGuideline() {
            return mSupplementalGuideline;
        }

        /**
         * Returns the view that will intercept clicks beneath the supplemental icon and action
         * views.
         */
        @NonNull
        View getClickInterceptView() {
            return mClickInterceptor;
        }
    }
}
