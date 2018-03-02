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

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;

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

    public LargeTemplateView(Context context) {
        super(context);
        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new LargeSliceAdapter(context);
        mRecyclerView.setAdapter(mAdapter);
        addView(mRecyclerView);
    }

    @Override
    public int getActualHeight() {
        return mListContent != null ? mListContent.getListHeight() : 0;
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
            return;
        }
        mListContent = new ListContent(getContext(), mSlice);
        mAdapter.setSliceItems(mListContent.getRowItems(), mTintColor);
    }

    /**
     * Whether or not the content in this template should be scrollable.
     */
    public void setScrollable(boolean isScrollable) {
        // TODO -- restrict / enable how much this view can show
        mIsScrollable = isScrollable;
    }

    @Override
    public void resetView() {
        mSlice = null;
        mAdapter.setSliceItems(null, -1);
        mListContent = null;
    }
}
