/*
 * Copyright 2017 The Android Open Source Project
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
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

import androidx.car.R;

/**
 * Class to build a list item.
 *
 * <p>An item supports primary action and supplemental action(s).
 *
 * <p>An item visually composes of 3 parts; each part may contain multiple views.
 * <ul>
 *     <li>{@code Primary Action}: represented by an icon of following types.
 *     <ul>
 *         <li>Primary Icon - icon size could be large or small.
 *         <li>No Icon
 *         <li>Empty Icon - different from No Icon by how much margin {@code Text} offsets
 *     </ul>
 *     <li>{@code Text}: supports any combination of the follow text views.
 *     <ul>
 *         <li>Title
 *         <li>Body
 *     </ul>
 *     <li>{@code Supplemental Action(s)}: represented by one of the following types; aligned toward
 *     the end of item.
 *     <ul>
 *         <li>Supplemental Icon
 *         <li>One Action Button
 *         <li>Two Action Buttons
 *     </ul>
 * </ul>
 *
 * {@link ListItem} can be built through its {@link ListItem.Builder}. It binds data
 * to {@link ListItemAdapter.ViewHolder} based on components selected.
 */
public class ListItem {

    private Builder mBuilder;

    private ListItem(Builder builder) {
        mBuilder = builder;
    }

    /**
     * Applies all {@link ViewBinder} to {@code viewHolder}.
     */
    void bind(ListItemAdapter.ViewHolder viewHolder) {
        setAllSubViewsGone(viewHolder);
        for (ViewBinder binder : mBuilder.mBinders) {
            binder.bind(viewHolder);
        }
    }

    void setAllSubViewsGone(ListItemAdapter.ViewHolder vh) {
        View[] subviews = new View[] {
                vh.getPrimaryIcon(),
                vh.getTitle(), vh.getBody(),
                vh.getSupplementalIcon(), vh.getSupplementalIconDivider(),
                vh.getAction1(), vh.getAction1Divider(), vh.getAction2(), vh.getAction2Divider()};
        for (View v : subviews) {
            v.setVisibility(View.GONE);
        }
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     * New view type needs support in {@link ListItemAdapter}.
     */
    protected int getViewType() {
        return mBuilder.mIsCard
                ? ListItemAdapter.CAR_PAGED_LIST_CARD
                : ListItemAdapter.CAR_PAGED_LIST_ITEM;
    }

    /**
     * Functional interface to provide a way to interact with views in
     * {@link ListItemAdapter.ViewHolder}. {@code ViewBinder}s added to a
     * {@code ListItem} will be called when {@code ListItem} {@code bind}s to
     * {@link ListItemAdapter.ViewHolder}.
     */
    public interface ViewBinder {
        /**
         * Provides a way to interact with views in view holder.
         */
        void bind(ListItemAdapter.ViewHolder viewHolder);
    }

    /**
     * Builds a {@link ListItem}.
     *
     * <p>With conflicting methods are called, e.g. setting primary action to both primary icon and
     * no icon, the last called method wins.
     */
    public static class Builder {

        @Retention(SOURCE)
        @IntDef({
                PRIMARY_ACTION_TYPE_NO_ICON, PRIMARY_ACTION_TYPE_EMPTY_ICON,
                PRIMARY_ACTION_TYPE_LARGE_ICON, PRIMARY_ACTION_TYPE_SMALL_ICON})
        private @interface PrimaryActionType {}

        private static final int PRIMARY_ACTION_TYPE_NO_ICON = 0;
        private static final int PRIMARY_ACTION_TYPE_EMPTY_ICON = 1;
        private static final int PRIMARY_ACTION_TYPE_LARGE_ICON = 2;
        private static final int PRIMARY_ACTION_TYPE_SMALL_ICON = 3;

        @Retention(SOURCE)
        @IntDef({SUPPLEMENTAL_ACTION_NO_ACTION, SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON,
                SUPPLEMENTAL_ACTION_ONE_ACTION, SUPPLEMENTAL_ACTION_TWO_ACTIONS})
        private @interface SupplementalActionType {}

        private static final int SUPPLEMENTAL_ACTION_NO_ACTION = 0;
        private static final int SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON = 1;
        private static final int SUPPLEMENTAL_ACTION_ONE_ACTION = 2;
        private static final int SUPPLEMENTAL_ACTION_TWO_ACTIONS = 3;

        private final Context mContext;
        private final List<ViewBinder> mBinders = new ArrayList<>();
        // Store custom binders separately so they will bind after binders are created in build().
        private final List<ViewBinder> mCustomBinders = new ArrayList<>();

        private boolean mIsCard;

        private View.OnClickListener mOnClickListener;

        @PrimaryActionType private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
        private int mPrimaryActionIconResId;
        private Drawable mPrimaryActionIconDrawable;

        private String mTitle;
        private String mBody;
        private boolean mIsBodyPrimary;

        @SupplementalActionType private int mSupplementalActionType = SUPPLEMENTAL_ACTION_NO_ACTION;
        private int mSupplementalIconResId;
        private View.OnClickListener mSupplementalIconOnClickListener;
        private boolean mShowSupplementalIconDivider;

        private String mAction1Text;
        private View.OnClickListener mAction1OnClickListener;
        private boolean mShowAction1Divider;
        private String mAction2Text;
        private View.OnClickListener mAction2OnClickListener;
        private boolean mShowAction2Divider;

        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Builds a {@link ListItem}. Adds {@link ViewBinder}s that will adjust layout in
         * {@link ListItemAdapter.ViewHolder} depending on sub-views used.
         */
        public ListItem build() {
            setItemLayoutHeight();
            setPrimaryAction();
            setText();
            setSupplementalActions();
            setOnClickListener();

            mBinders.addAll(mCustomBinders);

            return new ListItem(this);
        }

        /**
         * Sets the height of item depending on which text field is set.
         */
        private void setItemLayoutHeight() {
            if (TextUtils.isEmpty(mBody)) {
                // If the item only has title or no text, it uses fixed-height as single line.
                int height = (int) mContext.getResources().getDimension(
                        R.dimen.car_single_line_list_item_height);
                mBinders.add((vh) -> {
                    RecyclerView.LayoutParams layoutParams =
                            (RecyclerView.LayoutParams) vh.itemView.getLayoutParams();
                    layoutParams.height = height;
                    vh.itemView.setLayoutParams(layoutParams);
                });
            } else {
                // If body is present, the item should be at least as tall as min height, and wraps
                // content.
                int minHeight = (int) mContext.getResources().getDimension(
                        R.dimen.car_double_line_list_item_height);
                mBinders.add((vh) -> {
                    vh.itemView.setMinimumHeight(minHeight);
                    vh.getContainerLayout().setMinimumHeight(minHeight);

                    RecyclerView.LayoutParams layoutParams =
                            (RecyclerView.LayoutParams) vh.itemView.getLayoutParams();
                    layoutParams.height = RecyclerView.LayoutParams.WRAP_CONTENT;
                    vh.itemView.setLayoutParams(layoutParams);
                });
            }
        }

        private void setPrimaryAction() {
            setPrimaryIconContent();
            setPrimaryIconLayout();
        }

        private void setText() {
            setTextContent();
            setTextVerticalMargin();
            // Only setting start margin because text end is relative to the start of supplemental
            // actions.
            setTextStartMargin();
        }

        private void setOnClickListener() {
            if (mOnClickListener != null) {
                mBinders.add(vh -> vh.itemView.setOnClickListener(mOnClickListener));
            }
        }

        private void setPrimaryIconContent() {
            switch (mPrimaryActionType) {
                case PRIMARY_ACTION_TYPE_SMALL_ICON:
                case PRIMARY_ACTION_TYPE_LARGE_ICON:
                    mBinders.add((vh) -> {
                        vh.getPrimaryIcon().setVisibility(View.VISIBLE);

                        if (mPrimaryActionIconDrawable != null) {
                            vh.getPrimaryIcon().setImageDrawable(mPrimaryActionIconDrawable);
                        } else if (mPrimaryActionIconResId != 0) {
                            vh.getPrimaryIcon().setImageResource(mPrimaryActionIconResId);
                        }
                    });
                    break;
                case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                case PRIMARY_ACTION_TYPE_NO_ICON:
                    // Do nothing.
                    break;
                default:
                    throw new IllegalStateException("Unrecognizable primary action type.");
            }
        }

        /**
         * Sets layout params of primary icon.
         *
         * <p>Large icon will have no start margin, and always align center vertically.
         *
         * <p>Small icon will have start margin. When body text is present small icon uses a top
         * margin otherwise align center vertically.
         */
        private void setPrimaryIconLayout() {
            // Set all relevant fields in layout params to avoid carried over params when the item
            // gets bound to a recycled view holder.
            switch (mPrimaryActionType) {
                case PRIMARY_ACTION_TYPE_SMALL_ICON:
                    mBinders.add(vh -> {
                        int iconSize = mContext.getResources().getDimensionPixelSize(
                                R.dimen.car_primary_icon_size);
                        // Icon size.
                        RelativeLayout.LayoutParams layoutParams =
                                (RelativeLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();
                        layoutParams.height = iconSize;
                        layoutParams.width = iconSize;

                        // Start margin.
                        layoutParams.setMarginStart(mContext.getResources().getDimensionPixelSize(
                                R.dimen.car_keyline_1));

                        if (!TextUtils.isEmpty(mBody)) {
                            // Set top margin.
                            layoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
                            layoutParams.topMargin = mContext.getResources().getDimensionPixelSize(
                                    R.dimen.car_padding_4);
                        } else {
                            // Centered vertically.
                            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                            layoutParams.topMargin = 0;
                        }
                        vh.getPrimaryIcon().setLayoutParams(layoutParams);
                    });
                    break;
                case PRIMARY_ACTION_TYPE_LARGE_ICON:
                    mBinders.add(vh -> {
                        int iconSize = mContext.getResources().getDimensionPixelSize(
                                R.dimen.car_single_line_list_item_height);
                        // Icon size.
                        RelativeLayout.LayoutParams layoutParams =
                                (RelativeLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();
                        layoutParams.height = iconSize;
                        layoutParams.width = iconSize;

                        // No start margin.
                        layoutParams.setMarginStart(0);

                        // Always centered vertically.
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        layoutParams.topMargin = 0;

                        vh.getPrimaryIcon().setLayoutParams(layoutParams);
                    });
                    break;
                case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                case PRIMARY_ACTION_TYPE_NO_ICON:
                    // Do nothing.
                    break;
                default:
                    throw new IllegalStateException("Unrecognizable primary action type.");
            }
        }

        private void setTextContent() {
            if (!TextUtils.isEmpty(mTitle)) {
                mBinders.add(vh -> {
                    vh.getTitle().setVisibility(View.VISIBLE);
                    vh.getTitle().setText(mTitle);
                });
            }
            if (!TextUtils.isEmpty(mBody)) {
                mBinders.add(vh -> {
                    vh.getBody().setVisibility(View.VISIBLE);
                    vh.getBody().setText(mBody);
                });
            }

            if (mIsBodyPrimary) {
                mBinders.add((vh) -> {
                    vh.getTitle().setTextAppearance(R.style.CarBody2);
                    vh.getBody().setTextAppearance(R.style.CarBody1);
                });
            } else {
                mBinders.add((vh) -> {
                    vh.getTitle().setTextAppearance(R.style.CarBody1);
                    vh.getBody().setTextAppearance(R.style.CarBody2);
                });
            }
        }

        /**
         * Sets start margin of text view depending on icon type.
         */
        private void setTextStartMargin() {
            final int startMarginResId;
            switch (mPrimaryActionType) {
                case PRIMARY_ACTION_TYPE_NO_ICON:
                    startMarginResId = R.dimen.car_keyline_1;
                    break;
                case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                    startMarginResId = R.dimen.car_keyline_3;
                    break;
                case PRIMARY_ACTION_TYPE_SMALL_ICON:
                    startMarginResId = R.dimen.car_keyline_3;
                    break;
                case PRIMARY_ACTION_TYPE_LARGE_ICON:
                    startMarginResId = R.dimen.car_keyline_4;
                    break;
                default:
                    throw new IllegalStateException("Unrecognizable primary action type.");
            }
            int startMargin = mContext.getResources().getDimensionPixelSize(startMarginResId);
            mBinders.add(vh -> {
                RelativeLayout.LayoutParams titleLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
                titleLayoutParams.setMarginStart(startMargin);
                vh.getTitle().setLayoutParams(titleLayoutParams);

                RelativeLayout.LayoutParams bodyLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.setMarginStart(startMargin);
                vh.getBody().setLayoutParams(bodyLayoutParams);
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
                    RelativeLayout.LayoutParams layoutParams =
                            (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    layoutParams.topMargin = 0;
                    vh.getTitle().setLayoutParams(layoutParams);
                });
            } else if (TextUtils.isEmpty(mTitle) && !TextUtils.isEmpty(mBody)) {
                mBinders.add(vh -> {
                    // Body uses top and bottom margin.
                    int margin = mContext.getResources().getDimensionPixelSize(
                            R.dimen.car_padding_3);
                    RelativeLayout.LayoutParams layoutParams =
                            (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    layoutParams.removeRule(RelativeLayout.BELOW);
                    layoutParams.topMargin = margin;
                    layoutParams.bottomMargin = margin;
                    vh.getBody().setLayoutParams(layoutParams);
                });
            } else {
                mBinders.add(vh -> {
                    // Title has a top margin
                    Resources resources = mContext.getResources();
                    int padding1 = resources.getDimensionPixelSize(R.dimen.car_padding_1);
                    int padding3 = resources.getDimensionPixelSize(R.dimen.car_padding_3);

                    RelativeLayout.LayoutParams titleLayoutParams =
                            (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
                    titleLayoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
                    titleLayoutParams.topMargin = padding3;
                    vh.getTitle().setLayoutParams(titleLayoutParams);
                    // Body is below title with a margin, and has bottom margin.
                    RelativeLayout.LayoutParams bodyLayoutParams =
                            (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
                    bodyLayoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
                    bodyLayoutParams.addRule(RelativeLayout.BELOW, R.id.title);
                    bodyLayoutParams.topMargin = padding1;
                    bodyLayoutParams.bottomMargin = padding3;
                    vh.getBody().setLayoutParams(bodyLayoutParams);
                });
            }
        }

        /**
         * Sets up view(s) for supplemental action.
         */
        private void setSupplementalActions() {
            switch (mSupplementalActionType) {
                case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON:
                    mBinders.add((vh) -> {
                        vh.getSupplementalIcon().setVisibility(View.VISIBLE);
                        if (mShowSupplementalIconDivider) {
                            vh.getSupplementalIconDivider().setVisibility(View.VISIBLE);
                        }

                        vh.getSupplementalIcon().setImageResource(mSupplementalIconResId);
                        vh.getSupplementalIcon().setOnClickListener(
                                mSupplementalIconOnClickListener);
                    });
                    break;
                case SUPPLEMENTAL_ACTION_TWO_ACTIONS:
                    mBinders.add((vh) -> {
                        vh.getAction2().setVisibility(View.VISIBLE);
                        if (mShowAction2Divider) {
                            vh.getAction2Divider().setVisibility(View.VISIBLE);
                        }

                        vh.getAction2().setText(mAction2Text);
                        vh.getAction2().setOnClickListener(mAction2OnClickListener);
                    });
                    // Fall through
                case SUPPLEMENTAL_ACTION_ONE_ACTION:
                    mBinders.add((vh) -> {
                        vh.getAction1().setVisibility(View.VISIBLE);
                        if (mShowAction1Divider) {
                            vh.getAction1Divider().setVisibility(View.VISIBLE);
                        }

                        vh.getAction1().setText(mAction1Text);
                        vh.getAction1().setOnClickListener(mAction1OnClickListener);
                    });
                    break;
                case SUPPLEMENTAL_ACTION_NO_ACTION:
                    // Do nothing
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized supplemental action type.");
            }
        }

        /**
         * Builds the item in a {@link android.support.v7.widget.CardView}.
         *
         * <p>Each item will have rounded corner, margin between items, and elevation.
         *
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withCardLook() {
            mIsCard = true;
            return this;
        }

        /**
         * Sets {@link View.OnClickListener} of {@code ListItem}.
         *
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withOnClickListener(View.OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        /**
         * Sets {@code Primary Action} to be represented by an icon.
         *
         * @param iconResId the resource identifier of the drawable.
         * @param useLargeIcon the size of primary icon. Large Icon is a square as tall as an item
         *                     with only title set; useful for album cover art.
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withPrimaryActionIcon(@DrawableRes int iconResId, boolean useLargeIcon) {
            return withPrimaryActionIcon(null, iconResId, useLargeIcon);
        }

        /**
         * Sets {@code Primary Action} to be represented by an icon.
         *
         * @param drawable the Drawable to set, or null to clear the content.
         * @param useLargeIcon the size of primary icon. Large Icon is a square as tall as an item
         *                     with only title set; useful for album cover art.
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withPrimaryActionIcon(Drawable drawable, boolean useLargeIcon) {
            return withPrimaryActionIcon(drawable, 0, useLargeIcon);
        }

        private Builder withPrimaryActionIcon(Drawable drawable, @DrawableRes int iconResId,
                boolean useLargeIcon) {
            mPrimaryActionType = useLargeIcon
                    ? PRIMARY_ACTION_TYPE_LARGE_ICON
                    : PRIMARY_ACTION_TYPE_SMALL_ICON;
            mPrimaryActionIconResId = iconResId;
            mPrimaryActionIconDrawable = drawable;
            return this;
        }

        /**
         * Sets {@code Primary Action} to be empty icon.
         *
         * {@code Text} would have a start margin as if {@code Primary Action} were set to
         * primary icon.
         *
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withPrimaryActionEmptyIcon() {
            mPrimaryActionType = PRIMARY_ACTION_TYPE_EMPTY_ICON;
            return this;
        }

        /**
         * Sets {@code Primary Action} to have no icon. Text would align to the start of item.
         *
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withPrimaryActionNoIcon() {
            mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
            return this;
        }

        /**
         * Sets the title of item.
         *
         * <p>Primary text is {@code title} by default. It can be set by
         * {@link #withBody(String, boolean)}
         *
         * @param title text to display as title.
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withTitle(String title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the body text of item.
         *
         * <p>Text beyond length required by regulation will be truncated. Defaults {@code Title}
         * text as the primary.
         *
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withBody(String body) {
            return withBody(body, false);
        }

        /**
         * Sets the body text of item.
         *
         * <p>Text beyond length required by regulation will be truncated.
         *
         * @param asPrimary sets {@code Body Text} as primary text of item.
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withBody(String body, boolean asPrimary) {
            int limit = mContext.getResources().getInteger(
                    R.integer.car_list_item_text_length_limit);
            if (body.length() < limit) {
                mBody = body;
            } else {
                mBody = body.substring(0, limit) + mContext.getString(R.string.ellipsis);
            }
            mIsBodyPrimary = asPrimary;
            return this;
        }

        /**
         * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
         *
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withSupplementalIcon(int iconResId, boolean showDivider) {
            return withSupplementalIcon(iconResId, showDivider, null);
        }

        /**
         * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
         *
         * @param iconResId drawable resource id.
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withSupplementalIcon(int iconResId, boolean showDivider,
                View.OnClickListener listener) {
            mSupplementalActionType = SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON;

            mSupplementalIconResId = iconResId;
            mSupplementalIconOnClickListener = listener;
            mShowSupplementalIconDivider = showDivider;
            return this;
        }

        /**
         * Sets {@code Supplemental Action} to be represented by an {@code Action Button}.
         *
         * @param text button text to display.
         * @return This Builder object to allow for chaining calls to set methods.
         */
        public Builder withAction(String text, boolean showDivider, View.OnClickListener listener) {
            if (TextUtils.isEmpty(text)) {
                throw new IllegalArgumentException("Action text cannot be empty.");
            }
            mSupplementalActionType = SUPPLEMENTAL_ACTION_ONE_ACTION;

            mAction1Text = text;
            mAction1OnClickListener = listener;
            mShowAction1Divider = showDivider;
            return this;
        }

        /**
         * Sets {@code Supplemental Action} to be represented by two {@code Action Button}s.
         *
         * <p>These two action buttons will be aligned towards item end.
         *
         * @param action1Text button text to display - this button will be closer to item end.
         * @param action2Text button text to display.
         */
        public Builder withActions(String action1Text, boolean showAction1Divider,
                View.OnClickListener action1OnClickListener,
                String action2Text, boolean showAction2Divider,
                View.OnClickListener action2OnClickListener) {
            if (TextUtils.isEmpty(action1Text)) {
                throw new IllegalArgumentException("Action1 text cannot be empty.");
            }
            if (TextUtils.isEmpty(action2Text)) {
                throw new IllegalArgumentException("Action2 text cannot be empty.");
            }
            mSupplementalActionType = SUPPLEMENTAL_ACTION_TWO_ACTIONS;

            mAction1Text = action1Text;
            mAction1OnClickListener = action1OnClickListener;
            mShowAction1Divider = showAction1Divider;
            mAction2Text = action2Text;
            mAction2OnClickListener = action2OnClickListener;
            mShowAction2Divider = showAction2Divider;
            return this;
        }

        /**
         * Adds {@link ViewBinder} to interact with sub-views in
         * {@link ListItemAdapter.ViewHolder}. These ViewBinders will always bind after
         * other {@link Builder} methods have bond.
         *
         * <p>Make sure to call with...() method on the intended sub-view first.
         *
         * <p>Example:
         * <pre>
         * {@code
         * new Builder()
         *     .withTitle("title")
         *     .withViewBinder((viewHolder) -> {
         *         viewHolder.getTitle().doMoreStuff();
         *     })
         *     .build();
         * }
         * </pre>
         */
        public Builder withViewBinder(ViewBinder binder) {
            mCustomBinders.add(binder);
            return this;
        }
    }
}
