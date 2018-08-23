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
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a list item with {@link RadioButton}.
 *
 * <p>A radio button list item visually composes of 3 parts.
 * <ul>
 *     <li>optional {@code Primary Action Icon}.
 *     <li>optional {@code Text}.
 *     <li>A {@link RadioButton}.
 * </ul>
 *
 * <p>Clicking the item always checks the radio button.
 */
public class RadioButtonListItem extends ListItem<RadioButtonListItem.ViewHolder> {

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_ICON_SIZE_SMALL, PRIMARY_ACTION_ICON_SIZE_MEDIUM,
            PRIMARY_ACTION_ICON_SIZE_LARGE})
    private @interface PrimaryActionIconSize {}
    /**
     * Small sized icon is the mostly commonly used size.
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

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();
    private final Context mContext;
    private boolean mIsEnabled = true;

    @Nullable private Icon mPrimaryActionIcon;
    @PrimaryActionIconSize private int mPrimaryActionIconSize = PRIMARY_ACTION_ICON_SIZE_SMALL;

    private int mTextStartMargin;
    private CharSequence mText;

    private boolean mIsChecked;
    private boolean mShowRadioButtonDivider;
    private CompoundButton.OnCheckedChangeListener mRadioButtonOnCheckedChangeListener;

    /**
     * Creates a {@link RadioButtonListItem.ViewHolder}.
     */
    @NonNull
    public static ViewHolder createViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    public RadioButtonListItem(@NonNull Context context) {
        mContext = context;
        markDirty();
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_RADIO;
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
     * Sets the state of radio button.
     *
     * @param isChecked {@code true} to check the button; {@code false} to uncheck it.
     */
    public void setChecked(boolean isChecked) {
        if (mIsChecked == isChecked) {
            return;
        }
        mIsChecked = isChecked;
        markDirty();
    }

    /**
     * Get whether the radio button is checked.
     *
     * <p>The return value is in sync with UI state.
     *
     * @return {@code true} if the widget is checked; {@code false} otherwise.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon. The size of icon automatically
     * adjusts the start of {@code Text}.
     *
     * @param icon the icon to set as primary action. Setting {@code null} clears the icon and
     *             aligns text to the start of list item; {@code size} will be ignored.
     * @param size constant that represents the size of icon. See
     *             {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *             {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM}, and
     *             {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     *             If {@code null} is passed in for icon, size will be ignored.
     */
    public void setPrimaryActionIcon(@NonNull Icon icon, @PrimaryActionIconSize int size) {
        mPrimaryActionIcon = icon;
        mPrimaryActionIconSize = size;
        markDirty();
    }

    /**
     * Sets text to be displayed next to icon.
     *
     * @param text Text to be displayed, or {@code null} to clear the content.
     */
    public void setText(@Nullable CharSequence text) {
        mText = text;
        markDirty();
    }

    /**
     * Sets the start margin of text.
     */
    public void setTextStartMargin(@DimenRes int dimenRes) {
        mTextStartMargin = mContext.getResources().getDimensionPixelSize(dimenRes);
        markDirty();
    }

    /**
     * Sets whether to display a vertical bar that separates {@code text} and radio button.
     */
    public void setShowRadioButtonDivider(boolean showDivider) {
        mShowRadioButtonDivider = showDivider;
        markDirty();
    }

    /**
     * Sets {@link android.widget.CompoundButton.OnCheckedChangeListener} of radio button.
     */
    public void setOnCheckedChangeListener(
            @NonNull CompoundButton.OnCheckedChangeListener listener) {
        mRadioButtonOnCheckedChangeListener = listener;
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
        setTextInternal();
        setRadioButton();
        setOnClickListenerToCheckRadioButton();
    }

    private void setPrimaryAction() {
        setPrimaryIconContent();
        setPrimaryIconLayout();
    }

    private void setTextInternal() {
        setTextContent();
        setTextStartMargin();
    }

    private void setRadioButton() {
        mBinders.add(vh -> {
            // Clear listener before setting checked to avoid listener is notified every time
            // we bind to view holder.
            vh.getRadioButton().setOnCheckedChangeListener(null);
            vh.getRadioButton().setChecked(mIsChecked);
            // Keep internal checked state in sync with UI by wrapping listener.
            vh.getRadioButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
                mIsChecked = isChecked;
                if (mRadioButtonOnCheckedChangeListener != null) {
                    mRadioButtonOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                }
            });

            vh.getRadioButtonDivider().setVisibility(
                    mShowRadioButtonDivider ? View.VISIBLE : View.GONE);
        });
    }

    private void setPrimaryIconContent() {
        mBinders.add(vh -> {
            if (mPrimaryActionIcon == null) {
                vh.getPrimaryIcon().setVisibility(View.GONE);
            } else {
                vh.getPrimaryIcon().setVisibility(View.VISIBLE);
                mPrimaryActionIcon.loadDrawableAsync(getContext(),
                        drawable -> vh.getPrimaryIcon().setImageDrawable(drawable),
                        new Handler(Looper.getMainLooper()));
            }
        });
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
        if (mPrimaryActionIcon == null) {
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

            vh.getPrimaryIcon().requestLayout();
        });
    }

    private void setTextContent() {
        if (!TextUtils.isEmpty(mText)) {
            mBinders.add(vh -> {
                vh.getText().setVisibility(View.VISIBLE);
                vh.getText().setText(mText);
            });
        }
    }

    /**
     * Sets start margin of text view depending on icon type.
     */
    private void setTextStartMargin() {
        int offset = 0;
        if (mPrimaryActionIcon != null) {
            // If there is an icon, offset text to accommodate it.
            @DimenRes int startMarginResId =
                    mPrimaryActionIconSize == PRIMARY_ACTION_ICON_SIZE_LARGE
                            ? R.dimen.car_keyline_4
                            : R.dimen.car_keyline_3;  // Small and medium sized icon.
            offset = mContext.getResources().getDimensionPixelSize(startMarginResId);
        }

        int startMargin = offset + mTextStartMargin;
        mBinders.add(vh -> {
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getText().getLayoutParams();
            layoutParams.setMarginStart(startMargin);
            vh.getText().requestLayout();
        });
    }

    // Clicking the item always checks radio button.
    private void setOnClickListenerToCheckRadioButton() {
        mBinders.add(vh -> {
            vh.itemView.setClickable(true);
            vh.itemView.setOnClickListener(v -> vh.getRadioButton().setChecked(true));
        });
    }

    /**
     * Hides all views in {@link ViewHolder} then applies ViewBinders to adjust view layout params.
     */
    @Override
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
        // Radio button is always visible.
        vh.getRadioButton().setVisibility(View.VISIBLE);
    }

    /**
     * Holds views of RadioButtonListItem.
     */
    public static final class ViewHolder extends ListItem.ViewHolder {

        private final View[] mWidgetViews;

        private ViewGroup mContainerLayout;

        private ImageView mPrimaryIcon;
        private TextView mText;

        private View mRadioButtonDivider;
        private RadioButton mRadioButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);
            mText = itemView.findViewById(R.id.text);

            mRadioButton = itemView.findViewById(R.id.radio_button);
            mRadioButtonDivider = itemView.findViewById(R.id.radio_button_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);

            MinTouchTargetHelper.ensureThat(mRadioButton)
                    .hasMinTouchSize(minTouchSize);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[]{
                    mPrimaryIcon, mText,
                    mRadioButton, mRadioButtonDivider};
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
        public RadioButton getRadioButton() {
            return mRadioButton;
        }

        @NonNull
        public View getRadioButtonDivider() {
            return mRadioButtonDivider;
        }

        @NonNull
        View[] getWidgetViews() {
            return mWidgetViews;
        }

        @Override
        public void onUxRestrictionsChanged(
                androidx.car.uxrestrictions.CarUxRestrictions restrictionInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionInfo, getText());
        }
    }
}
