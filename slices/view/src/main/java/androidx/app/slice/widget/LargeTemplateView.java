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

package androidx.app.slice.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(24)
public class LargeTemplateView extends SliceChildView {

    private final LargeSliceAdapter mAdapter;
    private final RecyclerView mRecyclerView;
    private Slice mSlice;
    private boolean mIsScrollable;
    private ListContent mListContent;
    private List<SliceItem> mDisplayedItems = new ArrayList<>();
    private int mDisplayedItemsHeight = 0;

    public LargeTemplateView(Context context) {
        super(context);
        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new LargeSliceAdapter(context);
        mRecyclerView.setAdapter(mAdapter);
        addView(mRecyclerView);
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

    @Override
    public int getActualHeight() {
        return mDisplayedItemsHeight;
    }

    @Override
    public void setTint(int tint) {
        super.setTint(tint);
        populate();
    }

    @Override
    public @SliceView.SliceMode int getMode() {
        return SliceView.MODE_LARGE;
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
    public void setStyle(AttributeSet attrs) {
        super.setStyle(attrs);
        mAdapter.setStyle(attrs);
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
        mAdapter.setSliceItems(mDisplayedItems, mTintColor);
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
