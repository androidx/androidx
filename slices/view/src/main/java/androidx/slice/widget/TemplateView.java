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

package androidx.slice.widget;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Slice template containing all view components.
 */
@RequiresApi(19)
public class TemplateView extends SliceChildView implements
        SliceViewPolicy.PolicyChangeListener {

    private SliceView mParent;
    private final View mForeground;
    private SliceAdapter mAdapter;
    private final RecyclerView mRecyclerView;
    private ListContent mListContent;
    private List<SliceContent> mDisplayedItems = new ArrayList<>();
    private int mDisplayedItemsHeight = 0;
    private int[] mLoc = new int[2];
    private int mHiddenItemCount;

    public TemplateView(@NonNull Context context) {
        super(context);
        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setAdapter(new SliceAdapter(context));
        mAdapter = new SliceAdapter(context);
        mRecyclerView.setAdapter(mAdapter);
        addView(mRecyclerView);

        mForeground = new View(getContext());
        mForeground.setBackground(SliceViewUtil.getDrawable(getContext(),
                android.R.attr.selectableItemBackground));
        addView(mForeground);

        FrameLayout.LayoutParams lp = (LayoutParams) mForeground.getLayoutParams();
        lp.width = LayoutParams.MATCH_PARENT;
        lp.height = LayoutParams.MATCH_PARENT;
        mForeground.setLayoutParams(lp);
    }

    /**
     * Allows subclasses to set a custom adapter.
     */
    public void setAdapter(@NonNull SliceAdapter adapter) {
        mAdapter = adapter;
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mParent = (SliceView) getParent();
        mAdapter.setParents(mParent, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!mViewPolicy.isScrollable() && mDisplayedItems.size() > 0
                && mDisplayedItemsHeight != height) {
            updateDisplayedItems(height);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setInsets(int l, int t, int r, int b) {
        super.setInsets(l, t, r, b);
        mAdapter.setInsets(l, t, r, b);
    }

    /**
     * Called when the foreground view handling touch feedback should be activated.
     * @param event the event to handle.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void onForegroundActivated(MotionEvent event) {
        if (mParent != null && !mParent.isSliceViewClickable()) {
            // Only show highlight if clickable
            mForeground.setPressed(false);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mForeground.getLocationOnScreen(mLoc);
            final int x = (int) (event.getRawX() - mLoc[0]);
            final int y = (int) (event.getRawY() - mLoc[1]);
            mForeground.getBackground().setHotspot(x, y);
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mForeground.setPressed(true);
        } else if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_MOVE) {
            mForeground.setPressed(false);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setPolicy(SliceViewPolicy policy) {
        super.setPolicy(policy);
        mAdapter.setPolicy(policy);
        policy.setListener(this);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setActionLoading(SliceItem item) {
        mAdapter.onSliceActionLoading(item, 0 /* header position */);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setLoadingActions(Set<SliceItem> loadingActions) {
        mAdapter.setLoadingActions(loadingActions);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public Set<SliceItem> getLoadingActions() {
        return mAdapter.getLoadingActions();
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setTint(int tint) {
        super.setTint(tint);
        updateDisplayedItems(getMeasuredHeight());
    }

    /**
     * @hide
     */
    @Override
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
        if (mAdapter != null) {
            mAdapter.setSliceObserver(mObserver);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setSliceActions(List<SliceAction> actions) {
        mAdapter.setSliceActions(actions);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setSliceContent(ListContent sliceContent) {
        mListContent = sliceContent;
        int sliceHeight = mListContent.getHeight(mSliceStyle, mViewPolicy);
        updateDisplayedItems(sliceHeight);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setStyle(SliceStyle style, @NonNull RowStyle rowStyle) {
        super.setStyle(style, rowStyle);
        mAdapter.setStyle(style);
        applyRowStyle(rowStyle);
    }

    private void applyRowStyle(RowStyle rowStyle) {
        if (rowStyle.getDisableRecyclerViewItemAnimator()) {
            mRecyclerView.setItemAnimator(null);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setShowLastUpdated(boolean showLastUpdated) {
        super.setShowLastUpdated(showLastUpdated);
        mAdapter.setShowLastUpdated(showLastUpdated);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setLastUpdated(long lastUpdated) {
        super.setLastUpdated(lastUpdated);
        mAdapter.setLastUpdated(lastUpdated);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setAllowTwoLines(boolean allowTwoLines) {
        mAdapter.setAllowTwoLines(allowTwoLines);
    }

    private void updateDisplayedItems(int height) {
        if (mListContent == null || !mListContent.isValid()) {
            resetView();
            return;
        }
        DisplayedListItems response = mListContent.getRowItems(
                height, mSliceStyle, mViewPolicy);
        mDisplayedItems = response.getDisplayedItems();
        mHiddenItemCount = response.getHiddenItemCount();
        mDisplayedItemsHeight = mListContent.getListHeight(mDisplayedItems, mSliceStyle,
                mViewPolicy);
        mAdapter.setSliceItems(mDisplayedItems, mTintColor, mViewPolicy.getMode());
        updateOverscroll();
    }

    private void updateOverscroll() {
        boolean scrollable = mDisplayedItemsHeight > getMeasuredHeight();
        mRecyclerView.setOverScrollMode(mViewPolicy.isScrollable() && scrollable
                ? View.OVER_SCROLL_IF_CONTENT_SCROLLS
                : View.OVER_SCROLL_NEVER);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void resetView() {
        mDisplayedItemsHeight = 0;
        mDisplayedItems.clear();
        mAdapter.setSliceItems(null, -1, getMode());
        mListContent = null;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onScrollingChanged(boolean newScrolling) {
        // Disable nested scrolling if the slice isn't scrollable. This allows inertial
        // scrolling if the slice is inside a ScrollView.
        mRecyclerView.setNestedScrollingEnabled(newScrolling);

        if (mListContent != null) {
            updateDisplayedItems(mListContent.getHeight(mSliceStyle, mViewPolicy));
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onMaxHeightChanged(int newNewHeight) {
        if (mListContent != null) {
            updateDisplayedItems(mListContent.getHeight(mSliceStyle, mViewPolicy));
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onMaxSmallChanged(int newMaxSmallHeight) {
        if (mAdapter != null) {
            mAdapter.notifyHeaderChanged();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void onModeChanged(int newMode) {
        if (mListContent != null) {
            updateDisplayedItems(mListContent.getHeight(mSliceStyle, mViewPolicy));
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public int getHiddenItemCount() {
        return mHiddenItemCount;
    }
}
