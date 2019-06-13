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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;

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
    @NonNull
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    private final Context mContext;
    private boolean mIsEnabled = true;

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    private CharSequence mText;

    public SubheaderListItem(@NonNull Context context, @NonNull CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Text for subheader cannot be empty.");
        }
        mContext = context;
        mText = text;
        mTextStartMarginType = TEXT_START_MARGIN_TYPE_NONE;
        markDirty();
    }

    @IntDef({
            TEXT_START_MARGIN_TYPE_NONE, TEXT_START_MARGIN_TYPE_LARGE,
            TEXT_START_MARGIN_TYPE_SMALL})
    @Retention(SOURCE)
    public @interface TextStartMarginType {}

    /**
     * No starting margin before the subheader text.
     */
    public static final int TEXT_START_MARGIN_TYPE_NONE = 0;
    /**
     * A small starting margin before the subheader text.
     */
    public static final int TEXT_START_MARGIN_TYPE_SMALL = 1;
    /**
     * A large starting margin before the subheader text.
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
     * Sets the text to be displayed; cannot be null or empty string.
     */
    public void setText(@NonNull CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Text for subheader cannot be empty.");
        }
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

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * Calculates layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();
        setText();
    }

    /**
     * Applies ViewBinders to adjust view layout params.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onBind(ViewHolder viewHolder) {
        for (ViewBinder binder : mBinders) {
            binder.bind(viewHolder);
        }

        viewHolder.getText().setEnabled(mIsEnabled);
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

    private void setText() {
        @DimenRes int textStartPadding;
        switch (mTextStartMarginType) {
            case TEXT_START_MARGIN_TYPE_NONE:
                textStartPadding = R.dimen.car_keyline_1;
                break;
            case TEXT_START_MARGIN_TYPE_LARGE:
                textStartPadding = R.dimen.car_keyline_4;
                break;
            case TEXT_START_MARGIN_TYPE_SMALL:
                textStartPadding = R.dimen.car_keyline_3;
                break;
            default:
                throw new IllegalStateException("Unknown text start margin type.");
        }

        int startMargin = mContext.getResources().getDimensionPixelSize(textStartPadding);

        mBinders.add(vh -> {
            vh.getText().setText(mText);
            vh.getText().setPaddingRelative(startMargin, 0, 0, 0);
            vh.getText().requestLayout();
        });
    }

    /**
     * Holds views of SubHeaderListItem.
     */
    public static final class ViewHolder extends ListItem.ViewHolder {
        private TextView mText;

        public ViewHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.sub_header_text);
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
        public TextView getText() {
            return mText;
        }
    }
}
