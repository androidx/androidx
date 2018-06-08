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

import static android.app.slice.Slice.HINT_HORIZONTAL;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.Slice;
import androidx.slice.SliceItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LargeTemplateView extends SliceChildView {

    private SliceView mParent;
    private final View mForeground;
    private final LargeSliceAdapter mAdapter;
    private final RecyclerView mRecyclerView;
    private Slice mSlice;
    private boolean mIsScrollable;
    private ListContent mListContent;
    private List<SliceItem> mDisplayedItems = new ArrayList<>();
    private int mDisplayedItemsHeight = 0;
    private int[] mLoc = new int[2];

    public LargeTemplateView(Context context) {
        super(context);
        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new LargeSliceAdapter(context);
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

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mParent = (SliceView) getParent();
        mAdapter.setParents(mParent, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (mDisplayedItems.size() > 0 && mDisplayedItemsHeight > height) {
            // Need to resize
            updateDisplayedItems(height);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Called when the foreground view handling touch feedback should be activated.
     * @param event the event to handle.
     */
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

    @Override
    public void setMode(int newMode) {
        super.setMode(newMode);
        updateDisplayedItems(getMeasuredHeight());
    }

    @Override
    public int getActualHeight() {
        return mDisplayedItemsHeight;
    }

    @Override
    public int getSmallHeight() {
        if (mListContent == null || mListContent.getHeaderItem() == null) {
            return 0;
        }
        SliceItem headerItem = mListContent.getHeaderItem();
        if (headerItem.hasHint(HINT_HORIZONTAL)) {
            GridContent gc = new GridContent(getContext(), headerItem);
            return gc.getSmallHeight();
        } else {
            RowContent rc = new RowContent(getContext(), headerItem, mListContent.hasHeader());
            return rc.getSmallHeight();
        }
    }

    @Override
    public void setTint(int tint) {
        super.setTint(tint);
        populate();
    }

    @Override
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
        if (mAdapter != null) {
            mAdapter.setSliceObserver(mObserver);
        }
    }

    @Override
    public void setSliceActions(List<SliceItem> actions) {
        mAdapter.setSliceActions(actions);
    }

    @Override
    public void setSlice(Slice slice) {
        mSlice = slice;
        populate();
    }

    @Override
    public void setStyle(AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super.setStyle(attrs, defStyleAttrs, defStyleRes);
        mAdapter.setStyle(attrs, defStyleAttrs, defStyleRes);
    }

    @Override
    public void setShowLastUpdated(boolean showLastUpdated) {
        super.setShowLastUpdated(showLastUpdated);
        mAdapter.setShowLastUpdated(showLastUpdated);
    }

    @Override
    public void setLastUpdated(long lastUpdated) {
        super.setLastUpdated(lastUpdated);
        mAdapter.setLastUpdated(lastUpdated);
    }

    private void populate() {
        if (mSlice == null) {
            resetView();
            return;
        }
        mListContent = new ListContent(getContext(), mSlice);
        updateDisplayedItems(getMeasuredHeight());
    }

    /**
     * Whether or not the content in this template should be scrollable.
     */
    public void setScrollable(boolean isScrollable) {
        mIsScrollable = isScrollable;
        updateDisplayedItems(getMeasuredHeight());
    }

    private void updateDisplayedItems(int height) {
        if (mListContent == null) {
            return;
        }
        if (!mIsScrollable) {
            // If we're not scrollable we must cap the number of items we're displaying such
            // that they fit in the available space
            if (height == 0) {
                // Not measured, use default
                mDisplayedItems = mListContent.getItemsForNonScrollingList(-1);
            } else {
                mDisplayedItems = mListContent.getItemsForNonScrollingList(height);
            }
        } else {
            mDisplayedItems = mListContent.getRowItems();
        }
        mDisplayedItemsHeight = ListContent.getListHeight(getContext(), mDisplayedItems);
        if (getMode() == SliceView.MODE_LARGE) {
            mAdapter.setSliceItems(mDisplayedItems, mTintColor);
        } else if (getMode() == SliceView.MODE_SMALL) {
            mAdapter.setSliceItems(
                    Collections.singletonList(mDisplayedItems.get(0)), mTintColor);
        }
    }

    @Override
    public void resetView() {
        mSlice = null;
        mDisplayedItemsHeight = 0;
        mDisplayedItems.clear();
        mAdapter.setSliceItems(null, -1);
        mListContent = null;
    }
}
