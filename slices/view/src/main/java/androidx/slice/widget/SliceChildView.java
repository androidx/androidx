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

package androidx.slice.widget;

import static androidx.slice.widget.SliceView.MODE_LARGE;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;

import java.util.List;
import java.util.Set;

/**
 * Base class for children views of {@link SliceView}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public abstract class SliceChildView extends FrameLayout {

    protected SliceView.OnSliceActionListener mObserver;
    protected int mMode;
    protected int mTintColor = -1;
    protected boolean mShowLastUpdated;
    protected long mLastUpdated = -1;
    protected int mInsetStart;
    protected int mInsetTop;
    protected int mInsetEnd;
    protected int mInsetBottom;
    protected SliceActionView.SliceActionLoadingListener mLoadingListener;
    protected SliceStyle mSliceStyle;
    protected RowStyle mRowStyle;
    protected SliceViewPolicy mViewPolicy;

    public SliceChildView(@NonNull Context context) {
        super(context);
    }

    public SliceChildView(Context context, AttributeSet attributeSet) {
        this(context);
    }

    /**
     * Called when the view should be reset.
     */
    public abstract void resetView();

    /**
     * Sets the content to display in this slice.
     */
    public void setSliceContent(ListContent content) {
        // Do nothing
    }

    /**
     * Sets the insets (padding) for the slice.
     */
    public void setInsets(int l, int t, int r, int b) {
        mInsetStart = l;
        mInsetTop = t;
        mInsetEnd = r;
        mInsetBottom = b;
    }

    /**
     * Called when the slice being displayed in this view is an element of a larger list.
     */
    public void setSliceItem(SliceContent slice, boolean isHeader, int rowIndex,
            int rowCount, SliceView.OnSliceActionListener observer) {
        // Do nothing
    }

    /**
     * Sets the slice actions for this view.
     */
    public void setSliceActions(List<SliceAction> actions) {
        // Do nothing
    }

    /**
     * @return the mode of the slice being presented.
     */
    @SliceView.SliceMode
    public int getMode() {
        return mViewPolicy != null ? mViewPolicy.getMode() : MODE_LARGE;
    }

    /**
     * Sets a custom color to use for tinting elements like icons for this view.
     */
    public void setTint(@ColorInt int tintColor) {
        mTintColor = tintColor;
    }

    /**
     * Sets whether the last updated time should be displayed.
     */
    public void setShowLastUpdated(boolean showLastUpdated) {
        mShowLastUpdated = showLastUpdated;
    }

    /**
     * Sets when the content of this view was last updated.
     */
    public void setLastUpdated(long lastUpdated) {
        mLastUpdated = lastUpdated;
    }

    /**
     * Sets the observer to notify when an interaction events occur on the view.
     */
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
    }

    /**
     * Sets the listener to notify whenever an action is being loaded.
     */
    public void setSliceActionLoadingListener(SliceActionView.SliceActionLoadingListener listener) {
        mLoadingListener = listener;
    }

    /**
     * Indicates that a particular action is being loaded.
     */
    public void setActionLoading(SliceItem item) {
    }

    /**
     * Sets the actions that are being loaded.
     */
    public void setLoadingActions(Set<SliceItem> loadingActions) {
    }

    /**
     * Sets whether this slice can have 2 lines of subtitle text in the first row.
     */
    public void setAllowTwoLines(boolean allowTwoLines) {
    }

    /**
     * The set of currently loading actions.
     */
    public Set<SliceItem> getLoadingActions() {
        return null;
    }

    /**
     * Sets the style information for this view.
     */
    public void setStyle(SliceStyle styles, @NonNull RowStyle rowStyle) {
        mSliceStyle = styles;
        mRowStyle = rowStyle;
    }

    /**
     * Sets the policy information for this view.
     */
    public void setPolicy(@Nullable SliceViewPolicy policy) {
        mViewPolicy = policy;
    }

    public int getHiddenItemCount() {
        return 0;
    }
}
