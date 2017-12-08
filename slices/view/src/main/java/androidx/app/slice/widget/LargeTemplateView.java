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

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.SliceItem.FORMAT_COLOR;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(24)
public class LargeTemplateView extends FrameLayout implements SliceView.SliceModeView {

    private final LargeSliceAdapter mAdapter;
    private final RecyclerView mRecyclerView;
    private final int mDefaultHeight;
    private Slice mSlice;
    private boolean mIsScrollable;

    public LargeTemplateView(Context context) {
        super(context);

        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new LargeSliceAdapter(context);
        mRecyclerView.setAdapter(mAdapter);
        addView(mRecyclerView);
        mDefaultHeight = getResources().getDimensionPixelSize(R.dimen.abc_slice_large_height);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public @SliceView.SliceMode int getMode() {
        return SliceView.MODE_LARGE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mRecyclerView.getLayoutParams().height = WRAP_CONTENT;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (mRecyclerView.getMeasuredHeight() > width
                || (mSlice != null && SliceQuery.hasHints(mSlice, HINT_PARTIAL))) {
            mRecyclerView.getLayoutParams().height = width;
        } else {
            mRecyclerView.getLayoutParams().height = mRecyclerView.getMeasuredHeight();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setSlice(Slice slice) {
        SliceItem color = SliceQuery.find(slice, FORMAT_COLOR);
        mSlice = slice;
        final List<SliceItem> items = new ArrayList<>();
        final boolean[] hasHeader = new boolean[1];
        if (SliceQuery.hasHints(slice, HINT_LIST)) {
            addList(slice, items);
        } else {
            slice.getItems().forEach(new Consumer<SliceItem>() {
                @Override
                public void accept(SliceItem item) {
                    if (item.hasHint(HINT_ACTIONS)) {
                        return;
                    } else if (FORMAT_COLOR.equals(item.getFormat())) {
                        return;
                    } else if (FORMAT_SLICE.equals(item.getFormat())
                            && item.hasHint(HINT_LIST)) {
                        addList(item.getSlice(), items);
                    } else if (item.hasHint(HINT_LIST_ITEM)) {
                        items.add(item);
                    } else if (!hasHeader[0]) {
                        hasHeader[0] = true;
                        items.add(0, item);
                    } else {
                        items.add(item);
                    }
                }
            });
        }
        mAdapter.setSliceItems(items, color);
    }

    private void addList(Slice slice, List<SliceItem> items) {
        List<SliceItem> sliceItems = slice.getItems();
        items.addAll(sliceItems);
    }

    /**
     * Whether or not the content in this template should be scrollable.
     */
    public void setScrollable(boolean isScrollable) {
        // TODO -- restrict / enable how much this view can show
        mIsScrollable = isScrollable;
    }
}
