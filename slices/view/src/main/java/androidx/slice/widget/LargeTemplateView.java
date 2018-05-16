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

import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;

import java.util.ArrayList;
import java.util.Arrays;
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
    private boolean mScrollingEnabled;
    private ListContent mListContent;
    private ArrayList<SliceItem> mDisplayedItems = new ArrayList<>();
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
        if (!mScrollingEnabled && mDisplayedItems.size() > 0 && mDisplayedItemsHeight != height) {
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
        if (mMode != newMode) {
            mMode = newMode;
            if (mListContent != null && mListContent.isValid()) {
                int sliceHeight = mListContent.getLargeHeight(-1, mScrollingEnabled);
                updateDisplayedItems(sliceHeight);
            }
        }
    }

    @Override
    public int getActualHeight() {
        return mDisplayedItemsHeight;
    }

    @Override
    public int getSmallHeight() {
        if (mListContent == null || !mListContent.isValid()) {
            return 0;
        }
        return mListContent.getSmallHeight();
    }

    @Override
    public void setTint(int tint) {
        super.setTint(tint);
        updateDisplayedItems(getMeasuredHeight());
    }

    @Override
    public void setSliceActionListener(SliceView.OnSliceActionListener observer) {
        mObserver = observer;
        if (mAdapter != null) {
            mAdapter.setSliceObserver(mObserver);
        }
    }

    @Override
    public void setSliceActions(List<SliceAction> actions) {
        mAdapter.setSliceActions(actions);
    }

    @Override
    public void setSliceContent(ListContent sliceContent) {
        mListContent = sliceContent;
        int sliceHeight = mListContent.getLargeHeight(-1, mScrollingEnabled);
        updateDisplayedItems(sliceHeight);
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

    /**
     * Whether or not the content in this template should be scrollable.
     */
    public void setScrollable(boolean scrollingEnabled) {
        if (mScrollingEnabled != scrollingEnabled) {
            mScrollingEnabled = scrollingEnabled;
            if (mListContent != null && mListContent.isValid()) {
                int sliceHeight = mListContent.getLargeHeight(-1, mScrollingEnabled);
                updateDisplayedItems(sliceHeight);
            }
        }
    }

    private void updateDisplayedItems(int height) {
        if (mListContent == null || !mListContent.isValid()) {
            resetView();
            return;
        }
        int mode = getMode();
        if (mode == MODE_SMALL) {
            mDisplayedItems = new ArrayList(Arrays.asList(mListContent.getRowItems().get(0)));
        } else if (!mScrollingEnabled && height != 0) {
            mDisplayedItems = mListContent.getItemsForNonScrollingList(height);
        } else {
            mDisplayedItems = mListContent.getRowItems();
        }
        mDisplayedItemsHeight = mListContent.getListHeight(mDisplayedItems);
        mAdapter.setSliceItems(mDisplayedItems, mTintColor, mode);
        updateOverscroll();
    }

    private void updateOverscroll() {
        boolean scrollable = mDisplayedItemsHeight > getMeasuredHeight();
        mRecyclerView.setOverScrollMode(mScrollingEnabled && scrollable
                ? View.OVER_SCROLL_IF_CONTENT_SCROLLS
                : View.OVER_SCROLL_NEVER);
    }

    @Override
    public void resetView() {
        mDisplayedItemsHeight = 0;
        mDisplayedItems.clear();
        mAdapter.setSliceItems(null, -1, getMode());
        mListContent = null;
    }
}
