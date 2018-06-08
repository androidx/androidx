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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.StyleRes;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsUtils;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a list item of text.
 *
 * <p>An item supports primary action and supplemental action(s).
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
 *     <li>{@code Supplemental Action}: represented by one of the following types; aligned toward
 *     the end of item.
 *     <ul>
 *         <li>Supplemental Icon
 *         <li>One Action Button
 *         <li>Two Action Buttons
 *         <li>Switch
 *     </ul>
 * </ul>
 *
 * <p>{@code TextListItem} binds data to {@link ViewHolder} based on components selected.
 *
 * <p>When conflicting setter methods are called (e.g. setting primary action to both primary icon
 * and no icon), the last called method wins.
 */
public class TextListItem extends ListItem<TextListItem.ViewHolder> {

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
            SUPPLEMENTAL_ACTION_ONE_ACTION, SUPPLEMENTAL_ACTION_TWO_ACTIONS,
            SUPPLEMENTAL_ACTION_SWITCH})
    private @interface SupplementalActionType {}

    private static final int SUPPLEMENTAL_ACTION_NO_ACTION = 0;
    private static final int SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON = 1;
    private static final int SUPPLEMENTAL_ACTION_ONE_ACTION = 2;
    private static final int SUPPLEMENTAL_ACTION_TWO_ACTIONS = 3;
    private static final int SUPPLEMENTAL_ACTION_SWITCH = 4;

    private final Context mContext;

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    private View.OnClickListener mOnClickListener;

    @PrimaryActionType private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
    private Drawable mPrimaryActionIconDrawable;

    private String mTitle;
    private String mBody;
    private boolean mIsBodyPrimary;

    @SupplementalActionType private int mSupplementalActionType = SUPPLEMENTAL_ACTION_NO_ACTION;
    private Drawable mSupplementalIconDrawable;
    private View.OnClickListener mSupplementalIconOnClickListener;
    private boolean mShowSupplementalIconDivider;

    private boolean mSwitchChecked;
    private boolean mShowSwitchDivider;
    private CompoundButton.OnCheckedChangeListener mSwitchOnCheckedChangeListener;

    private String mAction1Text;
    private View.OnClickListener mAction1OnClickListener;
    private boolean mShowAction1Divider;
    private String mAction2Text;
    private View.OnClickListener mAction2OnClickListener;
    private boolean mShowAction2Divider;

    /**
     * Creates a {@link TextListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public TextListItem(Context context) {
        mContext = context;
        markDirty();
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_TEXT;
    }

    /**
     * Calculates the layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();

        // Create binders that adjust layout params of each view.
        setItemLayoutHeight();
        setPrimaryAction();
        setText();
        setSupplementalActions();
        setOnClickListener();
    }

    /**
     * Hides all views in {@link ViewHolder} then applies ViewBinders to adjust view layout params.
     */
    @Override
    public void onBind(ViewHolder viewHolder) {
        hideSubViews(viewHolder);
        for (ViewBinder binder : mBinders) {
            binder.bind(viewHolder);
        }
    }

    /** Sets the title text appearance from the specified style resource. */
    @Override
    void setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        super.setTitleTextAppearance(titleTextAppearance);
        setTextContent();
    }

    /** Sets the body text appearance from the specified style resource. */
    @Override
    void setBodyTextAppearance(@StyleRes int bodyTextAppearance) {
        super.setBodyTextAppearance(bodyTextAppearance);
        setTextContent();
    }

    private void hideSubViews(ViewHolder vh) {
        View[] subviews = new View[] {
                vh.getPrimaryIcon(),
                vh.getTitle(), vh.getBody(),
                vh.getSupplementalIcon(), vh.getSupplementalIconDivider(),
                vh.getSwitch(), vh.getSwitchDivider(),
                vh.getAction1(), vh.getAction1Divider(), vh.getAction2(), vh.getAction2Divider()};
        for (View v : subviews) {
            v.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the height of item depending on which text field is set.
     */
    private void setItemLayoutHeight() {
        if (TextUtils.isEmpty(mBody)) {
            // If the item only has title or no text, it uses fixed-height as single line.
            int height = mContext.getResources().getDimensionPixelSize(
                     R.dimen.car_single_line_list_item_height);
            mBinders.add(vh -> {
                ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
                layoutParams.height = height;
                vh.itemView.requestLayout();
            });
        } else {
            // If body is present, the item should be at least as tall as min height, and wraps
            // content.
            int minHeight = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_double_line_list_item_height);
            mBinders.add(vh -> {
                vh.itemView.setMinimumHeight(minHeight);
                vh.getContainerLayout().setMinimumHeight(minHeight);

                ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
                layoutParams.height = RecyclerView.LayoutParams.WRAP_CONTENT;
                vh.itemView.requestLayout();
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
        // Only set start margin because text end is relative to the start of supplemental actions.
        setTextStartMargin();
        setTextEndLayout();
    }

    private void setOnClickListener() {
        mBinders.add(vh -> vh.itemView.setOnClickListener(mOnClickListener));
    }

    private void setPrimaryIconContent() {
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_SMALL_ICON:
            case PRIMARY_ACTION_TYPE_LARGE_ICON:
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
                    layoutParams.height = layoutParams.width = iconSize;

                    // Start margin.
                    layoutParams.setMarginStart(mContext.getResources().getDimensionPixelSize(
                                            R.dimen.car_keyline_1));

                    if (!TextUtils.isEmpty(mBody)) {
                        // Set icon top margin so that the icon remains in the same position it
                        // would've been in for non-long-text item, namely so that the center
                        // line of icon matches that of line item.
                        layoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
                        int itemHeight = mContext.getResources().getDimensionPixelSize(
                                     R.dimen.car_double_line_list_item_height);
                        layoutParams.topMargin = (itemHeight - iconSize) / 2;
                    } else {
                        // If the icon can be centered vertically, leave the work for framework.
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                        layoutParams.topMargin = 0;
                    }
                    vh.getPrimaryIcon().requestLayout();
                });
                break;
            case PRIMARY_ACTION_TYPE_LARGE_ICON:
                mBinders.add(vh -> {
                    int iconSize = mContext.getResources().getDimensionPixelSize(
                               R.dimen.car_single_line_list_item_height);
                    // Icon size.
                    RelativeLayout.LayoutParams layoutParams =
                            (RelativeLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();
                    layoutParams.height = layoutParams.width = iconSize;

                    // No start margin.
                    layoutParams.setMarginStart(0);

                    // Always centered vertically.
                    layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                    layoutParams.topMargin = 0;

                    vh.getPrimaryIcon().requestLayout();
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
            mBinders.add(vh -> {
                vh.getTitle().setTextAppearance(getBodyTextAppearance());
                vh.getBody().setTextAppearance(getTitleTextAppearance());
            });
        } else {
            mBinders.add(vh -> {
                vh.getTitle().setTextAppearance(getTitleTextAppearance());
                vh.getBody().setTextAppearance(getBodyTextAppearance());
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
                throw new IllegalStateException("Unknown primary action type.");
        }
        int startMargin = mContext.getResources().getDimensionPixelSize(startMarginResId);
        mBinders.add(vh -> {
            RelativeLayout.LayoutParams titleLayoutParams =
                    (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
            titleLayoutParams.setMarginStart(startMargin);
            vh.getTitle().requestLayout();

            RelativeLayout.LayoutParams bodyLayoutParams =
                    (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
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
                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                layoutParams.topMargin = 0;
                vh.getTitle().requestLayout();
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
                vh.getBody().requestLayout();
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
                vh.getTitle().requestLayout();
                // Body is below title with a margin, and has bottom margin.
                RelativeLayout.LayoutParams bodyLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
                bodyLayoutParams.addRule(RelativeLayout.BELOW, R.id.title);
                bodyLayoutParams.topMargin = padding1;
                bodyLayoutParams.bottomMargin = padding3;
                vh.getBody().requestLayout();
            });
        }
    }

    /**
     * Returns the id of the leading (left most in LTR) view of supplemental actions.
     * The view could be one of the supplemental actions (icon, button, switch), or their divider.
     * Returns 0 if none is enabled.
     */
    @IdRes
    private int getSupplementalActionLeadingView() {
        int leadingViewId;
        switch (mSupplementalActionType) {
            case SUPPLEMENTAL_ACTION_NO_ACTION:
                leadingViewId = 0;
                break;
            case SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON:
                leadingViewId = mShowSupplementalIconDivider
                        ? R.id.supplemental_icon_divider : R.id.supplemental_icon;
                break;
            case SUPPLEMENTAL_ACTION_ONE_ACTION:
                leadingViewId = mShowAction1Divider ? R.id.action1_divider : R.id.action1;
                break;
            case SUPPLEMENTAL_ACTION_TWO_ACTIONS:
                leadingViewId = mShowAction2Divider ? R.id.action2_divider : R.id.action2;
                break;
            case SUPPLEMENTAL_ACTION_SWITCH:
                leadingViewId = mShowSwitchDivider ? R.id.switch_divider : R.id.switch_widget;
                break;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
        return leadingViewId;
    }

    private void setTextEndLayout() {
        // Figure out which view the text should align to.
        @IdRes int leadingViewId = getSupplementalActionLeadingView();

        if (leadingViewId == 0) {
            // There is no supplemental action. Text should align to parent end with KL1 padding.
            mBinders.add(vh -> {
                Resources resources = mContext.getResources();
                int padding = resources.getDimensionPixelSize(R.dimen.car_keyline_1);

                RelativeLayout.LayoutParams titleLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
                titleLayoutParams.setMarginEnd(padding);
                titleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                titleLayoutParams.removeRule(RelativeLayout.START_OF);

                RelativeLayout.LayoutParams bodyLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.setMarginEnd(padding);
                bodyLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
                bodyLayoutParams.removeRule(RelativeLayout.START_OF);
            });
        } else {
            // Text align to start of leading supplemental view with padding.
            mBinders.add(vh -> {
                Resources resources = mContext.getResources();
                int padding = resources.getDimensionPixelSize(R.dimen.car_padding_4);

                RelativeLayout.LayoutParams titleLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getTitle().getLayoutParams();
                titleLayoutParams.setMarginEnd(padding);
                titleLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END);
                titleLayoutParams.addRule(RelativeLayout.START_OF, leadingViewId);
                vh.getTitle().requestLayout();

                RelativeLayout.LayoutParams bodyLayoutParams =
                        (RelativeLayout.LayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.setMarginEnd(padding);
                bodyLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_END);
                bodyLayoutParams.addRule(RelativeLayout.START_OF, leadingViewId);
                vh.getBody().requestLayout();
            });
        }
    }

    /**
     * Sets up view(s) for supplemental action.
     */
    private void setSupplementalActions() {
        switch (mSupplementalActionType) {
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
            case SUPPLEMENTAL_ACTION_TWO_ACTIONS:
                mBinders.add(vh -> {
                    vh.getAction2().setVisibility(View.VISIBLE);
                    if (mShowAction2Divider) {
                        vh.getAction2Divider().setVisibility(View.VISIBLE);
                    }

                    vh.getAction2().setText(mAction2Text);
                    vh.getAction2().setOnClickListener(mAction2OnClickListener);
                });
                // Fall through
            case SUPPLEMENTAL_ACTION_ONE_ACTION:
                mBinders.add(vh -> {
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
            case SUPPLEMENTAL_ACTION_SWITCH:
                mBinders.add(vh -> {
                    vh.getSwitch().setVisibility(View.VISIBLE);
                    vh.getSwitch().setChecked(mSwitchChecked);
                    vh.getSwitch().setOnCheckedChangeListener(mSwitchOnCheckedChangeListener);
                    if (mShowSwitchDivider) {
                        vh.getSwitchDivider().setVisibility(View.VISIBLE);
                    }
                });
                break;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
    }

    /**
     * Sets {@link View.OnClickListener} of {@code TextListItem}.
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param iconResId the resource identifier of the drawable.
     * @param useLargeIcon the size of primary icon. Large Icon is a square as tall as an item.
     */
    public void setPrimaryActionIcon(@DrawableRes int iconResId, boolean useLargeIcon) {
        setPrimaryActionIcon(mContext.getDrawable(iconResId), useLargeIcon);
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param drawable the Drawable to set, or null to clear the content.
     * @param useLargeIcon the size of primary icon. Large Icon is a square as tall as an item.
     */
    public void setPrimaryActionIcon(Drawable drawable, boolean useLargeIcon) {
        mPrimaryActionType = useLargeIcon
                ? PRIMARY_ACTION_TYPE_LARGE_ICON
                : PRIMARY_ACTION_TYPE_SMALL_ICON;
        mPrimaryActionIconDrawable = drawable;

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
     * <p>Primary text is {@code Title} by default. It can be set by
     * {@link #setBody(String, boolean)}
     *
     * <p>{@code Title} text is limited to one line, and ellipses at the end.
     *
     * @param title text to display as title.
     */
    public void setTitle(String title) {
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
    public void setBody(String body) {
        setBody(body, false);
    }

    /**
     * Sets the body text of item.
     *
     * @param body text to be displayed.
     * @param asPrimary sets {@code Body Text} as primary text of item.
     */
    public void setBody(String body, boolean asPrimary) {
        mBody = body;
        mIsBodyPrimary = asPrimary;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
     *
     * @param iconResId drawable resource id.
     * @param showDivider whether to display a vertical bar that separates {@code text} and
     *                    {@code Supplemental Icon}.
     */
    public void setSupplementalIcon(int iconResId, boolean showDivider) {
        setSupplementalIcon(mContext.getDrawable(iconResId), showDivider, null);
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
     *
     * @param drawable the Drawable to set, or null to clear the content.
     * @param showDivider whether to display a vertical bar that separates {@code text} and
     *                    {@code Supplemental Icon}.
     */
    public void setSupplementalIcon(Drawable drawable, boolean showDivider) {
        setSupplementalIcon(drawable, showDivider, null);
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
     *
     * @param iconResId drawable resource id.
     * @param showDivider whether to display a vertical bar that separates {@code text} and
     *                    {@code Supplemental Icon}.
     * @param listener the callback that will run when icon is clicked.
     */
    public void setSupplementalIcon(int iconResId, boolean showDivider,
            View.OnClickListener listener) {
        setSupplementalIcon(mContext.getDrawable(iconResId), showDivider, listener);
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Supplemental Icon}.
     *
     * @param drawable the Drawable to set, or null to clear the content.
     * @param showDivider whether to display a vertical bar that separates {@code text} and
     *                    {@code Supplemental Icon}.
     * @param listener the callback that will run when icon is clicked.
     */
    public void setSupplementalIcon(Drawable drawable, boolean showDivider,
            View.OnClickListener listener) {
        mSupplementalActionType = SUPPLEMENTAL_ACTION_SUPPLEMENTAL_ICON;

        mSupplementalIconDrawable = drawable;
        mSupplementalIconOnClickListener = listener;
        mShowSupplementalIconDivider = showDivider;
        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Action Button}.
     *
     * @param text button text to display.
     * @param showDivider whether to display a vertical bar that separates {@code Text} and
     *                    {@code Action Button}.
     * @param listener the callback that will run when action button is clicked.
     */
    public void setAction(String text, boolean showDivider, View.OnClickListener listener) {
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Action text cannot be empty.");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Action OnClickListener cannot be null.");
        }
        mSupplementalActionType = SUPPLEMENTAL_ACTION_ONE_ACTION;

        mAction1Text = text;
        mAction1OnClickListener = listener;
        mShowAction1Divider = showDivider;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by two {@code Action Button}s.
     *
     * <p>These two action buttons will be aligned towards item end.
     *
     * @param action1Text button text to display - this button will be closer to item end.
     * @param action2Text button text to display.
     */
    public void setActions(String action1Text, boolean showAction1Divider,
            View.OnClickListener action1OnClickListener,
            String action2Text, boolean showAction2Divider,
            View.OnClickListener action2OnClickListener) {
        if (TextUtils.isEmpty(action1Text) || TextUtils.isEmpty(action2Text)) {
            throw new IllegalArgumentException("Action text cannot be empty.");
        }
        if (action1OnClickListener == null || action2OnClickListener == null) {
            throw new IllegalArgumentException("Action OnClickListener cannot be null.");
        }
        mSupplementalActionType = SUPPLEMENTAL_ACTION_TWO_ACTIONS;

        mAction1Text = action1Text;
        mAction1OnClickListener = action1OnClickListener;
        mShowAction1Divider = showAction1Divider;
        mAction2Text = action2Text;
        mAction2OnClickListener = action2OnClickListener;
        mShowAction2Divider = showAction2Divider;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by a {@link android.widget.Switch}.
     *
     * @param checked initial value for switched.
     * @param showDivider whether to display a vertical bar between switch and text.
     * @param listener callback to be invoked when the checked state is markDirty.
     */
    public void setSwitch(boolean checked, boolean showDivider,
            CompoundButton.OnCheckedChangeListener listener) {
        mSupplementalActionType = SUPPLEMENTAL_ACTION_SWITCH;

        mSwitchChecked = checked;
        mShowSwitchDivider = showDivider;
        mSwitchOnCheckedChangeListener = listener;

        markDirty();
    }

    /**
     * Sets the state of {@code Switch}. For this method to take effect,
     * {@link #setSwitch(boolean, boolean, CompoundButton.OnCheckedChangeListener)} must be called
     * first to set {@code Supplemental Action} as a {@code Switch}.
     *
     * @param isChecked sets the "checked/unchecked, namely on/off" state of switch.
     */
    public void setSwitchState(boolean isChecked) {
        mSwitchChecked = isChecked;
        markDirty();
    }

    /**
     * Holds views of TextListItem.
     */
    public static class ViewHolder extends ListItem.ViewHolder {

        private RelativeLayout mContainerLayout;

        private ImageView mPrimaryIcon;

        private TextView mTitle;
        private TextView mBody;

        private View mSupplementalIconDivider;
        private ImageView mSupplementalIcon;

        private Button mAction1;
        private View mAction1Divider;

        private Button mAction2;
        private View mAction2Divider;

        private Switch mSwitch;
        private View mSwitchDivider;

        public ViewHolder(View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);

            mSupplementalIcon = itemView.findViewById(R.id.supplemental_icon);
            mSupplementalIconDivider = itemView.findViewById(R.id.supplemental_icon_divider);

            mSwitch = itemView.findViewById(R.id.switch_widget);
            mSwitchDivider = itemView.findViewById(R.id.switch_divider);

            mAction1 = itemView.findViewById(R.id.action1);
            mAction1Divider = itemView.findViewById(R.id.action1_divider);
            mAction2 = itemView.findViewById(R.id.action2);
            mAction2Divider = itemView.findViewById(R.id.action2_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);

            MinTouchTargetHelper.ensureThat(mSupplementalIcon)
                    .hasMinTouchSize(minTouchSize);
        }

        /**
         * Update children views to comply with car UX restrictions.
         *
         * <p>{@code Body} text might be truncated to meet length limit required by regulation.
         *
         * @param restrictions current car UX restrictions.
         */
        @Override
        protected void complyWithUxRestrictions(CarUxRestrictions restrictions) {
            CarUxRestrictionsUtils.comply(itemView.getContext(), restrictions, getBody());
        }

        public RelativeLayout getContainerLayout() {
            return mContainerLayout;
        }

        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        public TextView getTitle() {
            return mTitle;
        }

        public TextView getBody() {
            return mBody;
        }

        public ImageView getSupplementalIcon() {
            return mSupplementalIcon;
        }

        public View getSupplementalIconDivider() {
            return mSupplementalIconDivider;
        }

        public View getSwitchDivider() {
            return mSwitchDivider;
        }

        public Switch getSwitch() {
            return mSwitch;
        }

        public Button getAction1() {
            return mAction1;
        }

        public View getAction1Divider() {
            return mAction1Divider;
        }

        public Button getAction2() {
            return mAction2;
        }

        public View getAction2Divider() {
            return mAction2Divider;
        }
    }
}
