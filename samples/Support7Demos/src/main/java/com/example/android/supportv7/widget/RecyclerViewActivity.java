/*
 * Copyright (C) 2013 The Android Open Source Project
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


package com.example.android.supportv7.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;

public class RecyclerViewActivity extends Activity {

    private static final String TAG = "RecyclerViewActivity";

    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new MyLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setAdapter(new SimpleStringAdapter(this, Cheeses.sCheeseStrings) {
            @Override
            public SimpleStringAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final SimpleStringAdapter.ViewHolder vh = super
                        .onCreateViewHolder(parent, viewType);
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int pos = vh.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) {
                            return;
                        }
                        if (pos + 1 < getItemCount()) {
                            swap(pos, pos + 1);
                        }
                    }
                });
                return vh;
            }
        });
        rv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        setContentView(rv);
        mRecyclerView = rv;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("Layout").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mRecyclerView.requestLayout();
        return super.onOptionsItemSelected(item);
    }

    private static final int SCROLL_DISTANCE = 80; // dp

    /**
     * A basic ListView-style LayoutManager.
     */
    class MyLayoutManager extends RecyclerView.LayoutManager {

        private static final String TAG = "MyLayoutManager";

        private int mFirstPosition;

        private final int mScrollDistance;

        public MyLayoutManager(Context c) {
            final DisplayMetrics dm = c.getResources().getDisplayMetrics();
            mScrollDistance = (int) (SCROLL_DISTANCE * dm.density + 0.5f);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            final int parentBottom = getHeight() - getPaddingBottom();
            final View oldTopView = getChildCount() > 0 ? getChildAt(0) : null;
            int oldTop = getPaddingTop();
            if (oldTopView != null) {
                oldTop = oldTopView.getTop();
            }

            detachAndScrapAttachedViews(recycler);

            int top = oldTop;
            int bottom;
            final int left = getPaddingLeft();
            final int right = getWidth() - getPaddingRight();

            final int count = state.getItemCount();
            for (int i = 0; mFirstPosition + i < count && top < parentBottom; i++, top = bottom) {
                View v = recycler.getViewForPosition(mFirstPosition + i);
                addView(v, i);
                measureChildWithMargins(v, 0, 0);
                bottom = top + getDecoratedMeasuredHeight(v);
                layoutDecorated(v, left, top, right, bottom);
            }
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            if (getChildCount() == 0) {
                return 0;
            }

            int scrolled = 0;
            final int left = getPaddingLeft();
            final int right = getWidth() - getPaddingRight();
            if (dy < 0) {
                while (scrolled > dy) {
                    final View topView = getChildAt(0);
                    final int hangingTop = Math.max(-getDecoratedTop(topView), 0);
                    final int scrollBy = Math.min(scrolled - dy, hangingTop);
                    scrolled -= scrollBy;
                    offsetChildrenVertical(scrollBy);
                    if (mFirstPosition > 0 && scrolled > dy) {
                        mFirstPosition--;
                        View v = recycler.getViewForPosition(mFirstPosition);
                        addView(v, 0);
                        measureChildWithMargins(v, 0, 0);
                        final int bottom = getDecoratedTop(topView);
                        final int top = bottom - getDecoratedMeasuredHeight(v);
                        layoutDecorated(v, left, top, right, bottom);
                    } else {
                        break;
                    }
                }
            } else if (dy > 0) {
                final int parentHeight = getHeight();
                while (scrolled < dy) {
                    final View bottomView = getChildAt(getChildCount() - 1);
                    final int hangingBottom =
                            Math.max(getDecoratedBottom(bottomView) - parentHeight, 0);
                    final int scrollBy = -Math.min(dy - scrolled, hangingBottom);
                    scrolled -= scrollBy;
                    offsetChildrenVertical(scrollBy);
                    if (scrolled < dy && state.getItemCount() > mFirstPosition + getChildCount()) {
                        View v = recycler.getViewForPosition(mFirstPosition + getChildCount());
                        final int top = getDecoratedBottom(getChildAt(getChildCount() - 1));
                        addView(v);
                        measureChildWithMargins(v, 0, 0);
                        final int bottom = top + getDecoratedMeasuredHeight(v);
                        layoutDecorated(v, left, top, right, bottom);
                    } else {
                        break;
                    }
                }
            }
            recycleViewsOutOfBounds(recycler);
            return scrolled;
        }

        @Override
        public View onFocusSearchFailed(View focused, int direction,
                RecyclerView.Recycler recycler, RecyclerView.State state) {
            final int oldCount = getChildCount();

            if (oldCount == 0) {
                return null;
            }

            final int left = getPaddingLeft();
            final int right = getWidth() - getPaddingRight();

            View toFocus = null;
            int newViewsHeight = 0;
            if (direction == View.FOCUS_UP || direction == View.FOCUS_BACKWARD) {
                while (mFirstPosition > 0 && newViewsHeight < mScrollDistance) {
                    mFirstPosition--;
                    View v = recycler.getViewForPosition(mFirstPosition);
                    final int bottom = getDecoratedTop(getChildAt(0));
                    addView(v, 0);
                    measureChildWithMargins(v, 0, 0);
                    final int top = bottom - getDecoratedMeasuredHeight(v);
                    layoutDecorated(v, left, top, right, bottom);
                    if (v.isFocusable()) {
                        toFocus = v;
                        break;
                    }
                }
            }
            if (direction == View.FOCUS_DOWN || direction == View.FOCUS_FORWARD) {
                while (mFirstPosition + getChildCount() < state.getItemCount() &&
                        newViewsHeight < mScrollDistance) {
                    View v = recycler.getViewForPosition(mFirstPosition + getChildCount());
                    final int top = getDecoratedBottom(getChildAt(getChildCount() - 1));
                    addView(v);
                    measureChildWithMargins(v, 0, 0);
                    final int bottom = top + getDecoratedMeasuredHeight(v);
                    layoutDecorated(v, left, top, right, bottom);
                    if (v.isFocusable()) {
                        toFocus = v;
                        break;
                    }
                }
            }

            return toFocus;
        }

        public void recycleViewsOutOfBounds(RecyclerView.Recycler recycler) {
            final int childCount = getChildCount();
            final int parentWidth = getWidth();
            final int parentHeight = getHeight();
            boolean foundFirst = false;
            int first = 0;
            int last = 0;
            for (int i = 0; i < childCount; i++) {
                final View v = getChildAt(i);
                if (v.hasFocus() || (getDecoratedRight(v) >= 0 &&
                        getDecoratedLeft(v) <= parentWidth &&
                        getDecoratedBottom(v) >= 0 &&
                        getDecoratedTop(v) <= parentHeight)) {
                    if (!foundFirst) {
                        first = i;
                        foundFirst = true;
                    }
                    last = i;
                }
            }
            for (int i = childCount - 1; i > last; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
            for (int i = first - 1; i >= 0; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
            if (getChildCount() == 0) {
                mFirstPosition = 0;
            } else {
                mFirstPosition += first;
            }
        }
    }
}
