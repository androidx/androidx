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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.car.widget.ListItemAdapter.ListItemType;
import androidx.constraintlayout.widget.Guideline;

/**
 * Class to build a list item with {@link Switch}.
 *
 * <p>A switch list item is visually composed of 5 parts.
 * <ul>
 * <li>A {@link Switch}.
 * <li>optional {@code Divider}.
 * <li>optional {@code Primary Action Icon}.
 * <li>optional {@code Title}.
 * <li>optional {@code Body}.
 * </ul>
 */
public final class SwitchListItem extends CompoundButtonListItem<SwitchListItem.ViewHolder> {

    /**
     * Creates a {@link ViewHolder}.
     *
     * @return a {@link ViewHolder} for this {@link SwitchListItem}.
     */
    @NonNull
    public static ViewHolder createViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     *
     * @return Type of this {@link CompoundButtonListItem}.
     */
    @ListItemType
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_SWITCH;
    }

    /**
     * Creates a {@link SwitchListItem} that will be used to display a list item with a
     * {@link Switch}.
     *
     * @param context The context to be used by this {@link SwitchListItem}.
     */
    public SwitchListItem(@NonNull Context context) {
        super(context);
    }

    /**
     * Returns whether the compound button will be placed at the end of the list item layout. This
     * value is used to determine start margins for the {@code Title} and {@code Body}.
     *
     * @return Whether compound button is placed at the end of the list item layout.
     */
    @Override
    public boolean isCompoundButtonPositionEnd() {
        return true;
    }

    /**
     * Sets the state of {@code Switch}.
     *
     * @param isChecked sets the "checked/unchecked, namely on/off" state of switch.
     * @deprecated Use {@link #setChecked(boolean)} instead.
     */
    @Deprecated
    public void setSwitchState(boolean isChecked) {
        setChecked(isChecked);
    }

    /**
     * Registers a callback to be invoked when the checked state of switch changes.
     *
     * @param listener callback to be invoked when the checked state shown in the UI changes.
     * @deprecated Use {@link #setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener)}
     * instead.
     */
    @Deprecated
    public void setSwitchOnCheckedChangeListener(
            @Nullable CompoundButton.OnCheckedChangeListener listener) {
        setOnCheckedChangeListener(listener);
    }

    /**
     * Sets whether to display a vertical bar between switch and text.
     *
     * @deprecated Use {@link #setShowCompoundButtonDivider(boolean)} instead.
     */
    @Deprecated
    public void setShowSwitchDivider(boolean showCompoundButtonDivider) {
        setShowCompoundButtonDivider(showCompoundButtonDivider);
    }

    /**
     * ViewHolder that contains necessary widgets for {@link SwitchListItem}.
     */
    public static final class ViewHolder extends CompoundButtonListItem.ViewHolder {

        private View[] mWidgetViews;

        private ViewGroup mContainerLayout;

        private ImageView mPrimaryIcon;

        private TextView mTitle;
        private TextView mBody;

        private Guideline mSupplementalGuideline;

        private CompoundButton mCompoundButton;
        private View mCompoundButtonDivider;

        /**
         * Creates a {@link ViewHolder} for a {@link SwitchListItem}.
         *
         * @param itemView The view to be used to display a {@link SwitchListItem}.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);

            mSupplementalGuideline = itemView.findViewById(R.id.supplemental_actions_guideline);

            mCompoundButton = itemView.findViewById(R.id.switch_widget);
            mCompoundButtonDivider = itemView.findViewById(R.id.switch_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);
            MinTouchTargetHelper.ensureThat(mCompoundButton).hasMinTouchSize(minTouchSize);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[]{
                    mPrimaryIcon,
                    mTitle, mBody,
                    mCompoundButton, mCompoundButtonDivider,
            };
        }

        /**
         * Updates child views with current car UX restrictions.
         *
         * <p>{@code Text} might be truncated to meet length limit required by regulation.
         *
         * @param restrictionsInfo current car UX restrictions.
         */
        @Override
        public void onUxRestrictionsChanged(@NonNull CarUxRestrictions restrictionsInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionsInfo, getBody());
        }

        /**
         * Returns the primary icon view within this view holder's view.
         *
         * @return Icon view within this view holder's view.
         */
        @NonNull
        @Override
        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        /**
         * Returns the title view within this view holder's view.
         *
         * @return Title view within this view holder's view.
         */
        @NonNull
        @Override
        public TextView getTitle() {
            return mTitle;
        }

        /**
         * Returns the body view within this view holder's view.
         *
         * @return Body view within this view holder's view.
         */
        @NonNull
        @Override
        public TextView getBody() {
            return mBody;
        }

        /**
         * Returns the compound button divider view within this view holder's view.
         *
         * @return Compound button divider view within this view holder's view.
         */
        @NonNull
        @Override
        public View getCompoundButtonDivider() {
            return mCompoundButtonDivider;
        }

        /**
         * Returns the compound button within this view holder's view.
         *
         * @return Compound button within this view holder's view.
         */
        @NonNull
        @Override
        public CompoundButton getCompoundButton() {
            return mCompoundButton;
        }

        /**
         * Returns the switch within this view holder's view.
         *
         * @return Switch within this view holder's view.
         * @deprecated Use {@link #getCompoundButton()} instead.
         */
        @Deprecated
        @NonNull
        public Switch getSwitch() {
            return (Switch) getCompoundButton();
        }

        @NonNull
        @Override
        Guideline getSupplementalGuideline() {
            return mSupplementalGuideline;
        }

        @NonNull
        @Override
        View[] getWidgetViews() {
            return mWidgetViews;
        }

        /**
         * Returns the container layout of this view holder.
         *
         * @return Container layout of this view holder.
         */
        @NonNull
        @Override
        public ViewGroup getContainerLayout() {
            return mContainerLayout;
        }
    }
}
