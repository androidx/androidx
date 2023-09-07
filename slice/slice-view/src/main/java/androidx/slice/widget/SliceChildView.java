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
 *
 * @deprecated Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
@RequiresApi(19)
@Deprecated
public abstract class SliceChildView extends FrameLayout {

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected SliceView.OnSliceActionListener mObserver;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected int mMode;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected int mTintColor = -1;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected boolean mShowLastUpdated;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected long mLastUpdated = -1;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected int mInsetStart;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected int mInsetTop;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected int mInsetEnd;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected int mInsetBottom;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected SliceActionView.SliceActionLoadingListener mLoadingListener;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected SliceStyle mSliceStyle;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected RowStyle mRowStyle;
    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceContent(ListContent content) {
        // Do nothing
    }

    /**
     * Sets the insets (padding) for the slice.
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
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceActions(List<SliceAction> actions) {
        // Do nothing
    }

    /**
     * @return the mode of the slice being presented.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SliceView.SliceMode
    public int getMode() {
        return mViewPolicy != null ? mViewPolicy.getMode() : MODE_LARGE;
    }

    /**
     * Sets a custom color to use for tinting elements like icons for this view.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setTint(@ColorInt int tintColor) {
        mTintColor = tintColor;
    }

    /**
     * Sets whether the last updated time should be displayed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setShowLastUpdated(boolean showLastUpdated) {
        mShowLastUpdated = showLastUpdated;
    }

    /**
     * Sets when the content of this view was last updated.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setLastUpdated(long lastUpdated) {
        mLastUpdated = lastUpdated;
    }

    /**
     * Sets the observer to notify when an interaction events occur on the view.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
    }

    /**
     * Sets the listener to notify whenever an action is being loaded.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceActionLoadingListener(SliceActionView.SliceActionLoadingListener listener) {
        mLoadingListener = listener;
    }

    /**
     * Indicates that a particular action is being loaded.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setActionLoading(SliceItem item) {
    }

    /**
     * Sets the actions that are being loaded.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setLoadingActions(Set<SliceItem> loadingActions) {
    }

    /**
     * Sets whether this slice can have 2 lines of subtitle text in the first row.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setAllowTwoLines(boolean allowTwoLines) {
    }

    /**
     * The set of currently loading actions.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Set<SliceItem> getLoadingActions() {
        return null;
    }

    /**
     * Sets the style information for this view.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setStyle(SliceStyle styles, @NonNull RowStyle rowStyle) {
        mSliceStyle = styles;
        mRowStyle = rowStyle;
    }

    /**
     * Sets the policy information for this view.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setPolicy(@Nullable SliceViewPolicy policy) {
        mViewPolicy = policy;
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public int getHiddenItemCount() {
        return 0;
    }
}
