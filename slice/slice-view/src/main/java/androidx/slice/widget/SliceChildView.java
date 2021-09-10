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
 */
@RequiresApi(19)
public abstract class SliceChildView extends FrameLayout {

    /**
     * @hide
     */
    protected SliceView.OnSliceActionListener mObserver;
    /**
     * @hide
     */
    protected int mMode;
    /**
     * @hide
     */
    protected int mTintColor = -1;
    /**
     * @hide
     */
    protected boolean mShowLastUpdated;
    /**
     * @hide
     */
    protected long mLastUpdated = -1;
    /**
     * @hide
     */
    protected int mInsetStart;
    /**
     * @hide
     */
    protected int mInsetTop;
    /**
     * @hide
     */
    protected int mInsetEnd;
    /**
     * @hide
     */
    protected int mInsetBottom;
    /**
     * @hide
     */
    protected SliceActionView.SliceActionLoadingListener mLoadingListener;
    /**
     * @hide
     */
    protected SliceStyle mSliceStyle;
    /**
     * @hide
     */
    protected RowStyle mRowStyle;
    /**
     * @hide
     */
    protected SliceViewPolicy mViewPolicy;

    public SliceChildView(@NonNull Context context) {
        super(context);
    }

    public SliceChildView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context);
    }

    /**
     * Called when the view should be reset.
     */
    public abstract void resetView();

    /**
     * Sets the content to display in this slice.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceContent(ListContent content) {
        // Do nothing
    }

    /**
     * Sets the insets (padding) for the slice.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setInsets(int l, int t, int r, int b) {
        mInsetStart = l;
        mInsetTop = t;
        mInsetEnd = r;
        mInsetBottom = b;
    }

    /**
     * Called when the slice being displayed in this view is an element of a larger list.
     */
    public void setSliceItem(@Nullable SliceContent slice, boolean isHeader, int rowIndex,
            int rowCount, @Nullable SliceView.OnSliceActionListener observer) {
        // Do nothing
    }

    /**
     * Sets the slice actions for this view.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceActions(List<SliceAction> actions) {
        // Do nothing
    }

    /**
     * @return the mode of the slice being presented.
     * @hide
     */
    @SliceView.SliceMode
    public int getMode() {
        return mViewPolicy != null ? mViewPolicy.getMode() : MODE_LARGE;
    }

    /**
     * Sets a custom color to use for tinting elements like icons for this view.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setTint(@ColorInt int tintColor) {
        mTintColor = tintColor;
    }

    /**
     * Sets whether the last updated time should be displayed.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setShowLastUpdated(boolean showLastUpdated) {
        mShowLastUpdated = showLastUpdated;
    }

    /**
     * Sets when the content of this view was last updated.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setLastUpdated(long lastUpdated) {
        mLastUpdated = lastUpdated;
    }

    /**
     * Sets the observer to notify when an interaction events occur on the view.
     * @hide
     */
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
    }

    /**
     * Sets the listener to notify whenever an action is being loaded.
     * @hide
     */
    public void setSliceActionLoadingListener(SliceActionView.SliceActionLoadingListener listener) {
        mLoadingListener = listener;
    }

    /**
     * Indicates that a particular action is being loaded.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setActionLoading(SliceItem item) {
    }

    /**
     * Sets the actions that are being loaded.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setLoadingActions(Set<SliceItem> loadingActions) {
    }

    /**
     * Sets whether this slice can have 2 lines of subtitle text in the first row.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setAllowTwoLines(boolean allowTwoLines) {
    }

    /**
     * The set of currently loading actions.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Set<SliceItem> getLoadingActions() {
        return null;
    }

    /**
     * Sets the style information for this view.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setStyle(SliceStyle styles, @NonNull RowStyle rowStyle) {
        mSliceStyle = styles;
        mRowStyle = rowStyle;
    }

    /**
     * Sets the policy information for this view.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setPolicy(@Nullable SliceViewPolicy policy) {
        mViewPolicy = policy;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public int getHiddenItemCount() {
        return 0;
    }
}
