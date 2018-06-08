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
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.IntDef;
import androidx.annotation.StyleRes;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsUtils;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a sub-header list item.
 *
 * <p>A sub-header list item consists of a one-line text. Its margin can be adjusted to match the
 * rest of {@link ListItem} through {@link #setTextStartMarginType(int)}.
 */
public class SubheaderListItem extends ListItem<SubheaderListItem.ViewHolder> {

    /**
     * Creates a {@link SubheaderListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    private final Context mContext;

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    @StyleRes private int mListItemSubheaderTextAppearance;
    private String mText;

    public SubheaderListItem(Context context, String text) {

        mContext = context;
        mText = text;
        mTextStartMarginType = TEXT_START_MARGIN_TYPE_NONE;

        TypedArray a = context.getTheme().obtainStyledAttributes(R.styleable.ListItem);
        mListItemSubheaderTextAppearance = a.getResourceId(
                R.styleable.ListItem_listItemSubheaderTextAppearance,
                R.style.TextAppearance_Car_Subheader);
        a.recycle();

        markDirty();
    }

    @IntDef({
            TEXT_START_MARGIN_TYPE_NONE, TEXT_START_MARGIN_TYPE_LARGE,
            TEXT_START_MARGIN_TYPE_SMALL})
    @Retention(SOURCE)
    public @interface TextStartMarginType {}

    /**
     * Sets start margin of text the same as {@link TextListItem#setPrimaryActionNoIcon()}.
     */
    public static final int TEXT_START_MARGIN_TYPE_NONE = 0;
    /**
     * Sets start margin of text the same as {@link TextListItem#setPrimaryActionIcon(int, boolean)}
     * with {@code useLargeIcon} set to {@code false}.
     */
    public static final int TEXT_START_MARGIN_TYPE_SMALL = 1;
    /**
     * Sets start margin of text the same as {@link TextListItem#setPrimaryActionIcon(int, boolean)}
     * with {@code useLargeIcon} set to {@code true}.
     */
    public static final int TEXT_START_MARGIN_TYPE_LARGE = 2;
    @TextStartMarginType private int mTextStartMarginType;

    /**
     * Sets the start margin of text. Defaults to {@link #TEXT_START_MARGIN_TYPE_NONE}.
     */
    public void setTextStartMarginType(@TextStartMarginType int type) {
        mTextStartMarginType = type;
        markDirty();
    }

    /**
     * Sets the text to be displayed.
     */
    public void setText(String text) {
        mText = text;
        markDirty();
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_SUBHEADER;
    }

    /**
     * Calculates layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();

        setItemLayoutHeight();
        setText();
    }

    /**
     * Applies ViewBinders to adjust view layout params.
     */
    @Override
    protected void onBind(ViewHolder viewHolder) {
        for (ViewBinder binder : mBinders) {
            binder.bind(viewHolder);
        }
    }

    private void setItemLayoutHeight() {
        int height = mContext.getResources().getDimensionPixelSize(R.dimen.car_sub_header_height);
        mBinders.add(vh -> {
            vh.itemView.getLayoutParams().height = height;
            vh.itemView.requestLayout();
        });
    }

    private void setText() {
        @DimenRes int textStartMarginDimen;
        switch (mTextStartMarginType) {
            case TEXT_START_MARGIN_TYPE_NONE:
                textStartMarginDimen = R.dimen.car_keyline_1;
                break;
            case TEXT_START_MARGIN_TYPE_LARGE:
                textStartMarginDimen = R.dimen.car_keyline_4;
                break;
            case TEXT_START_MARGIN_TYPE_SMALL:
                textStartMarginDimen = R.dimen.car_keyline_3;
                break;
            default:
                throw new IllegalStateException("Unknown text start margin type.");
        }

        mBinders.add(vh -> {
            vh.getText().setText(mText);
            vh.getText().setTextAppearance(mListItemSubheaderTextAppearance);

            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getText().getLayoutParams();
            layoutParams.setMarginStart(
                    mContext.getResources().getDimensionPixelSize(textStartMarginDimen));
            vh.getText().requestLayout();
        });
    }

    /**
     * Holds views of SubHeaderListItem.
     */
    public static class ViewHolder extends ListItem.ViewHolder {

        private TextView mText;

        public ViewHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.text);
        }

        /**
         * Update children views to comply with car UX restrictions.
         *
         * <p>{@code Text} might be truncated to meet length limit required by regulation.
         *
         * @param restrictions current car UX restrictions.
         */
        @Override
        protected void complyWithUxRestrictions(CarUxRestrictions restrictions) {
            CarUxRestrictionsUtils.comply(itemView.getContext(), restrictions, getText());
        }

        public TextView getText() {
            return mText;
        }
    }
}
