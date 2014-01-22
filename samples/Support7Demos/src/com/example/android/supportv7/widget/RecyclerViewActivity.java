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

import android.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.TextView;
import com.example.android.supportv7.Cheeses;

import java.util.ArrayList;
import java.util.Collections;

public class RecyclerViewActivity extends Activity {
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new MyLayoutManager(this));
        rv.setHasFixedSize(true);
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setAdapter(new MyAdapter(Cheeses.sCheeseStrings));
        setContentView(rv);

        mRecyclerView = rv;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItemCompat.setShowAsAction(menu.add("Layout"), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
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
        public void layoutChildren(RecyclerView.Adapter adapter, RecyclerView.Recycler recycler) {
            final RecyclerView parent = getRecyclerView();
            final int parentBottom = parent.getHeight() - parent.getPaddingBottom();

            final View oldTopView = parent.getChildCount() > 0 ? parent.getChildAt(0) : null;
            int oldTop = parent.getPaddingTop();
            if (oldTopView != null) {
                oldTop = oldTopView.getTop();
            }

            recycler.scrapAllViewsAttached();

            int top = oldTop;
            int bottom;
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int count = adapter.getItemCount();
            for (int i = 0; mFirstPosition + i < count && top < parentBottom; i++, top = bottom) {
                View v = recycler.getViewForPosition(mFirstPosition + i);
                addView(v, i);
                v.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                bottom = top + v.getMeasuredHeight();
                v.layout(left, top, right, bottom);
            }

            recycler.detachDirtyScrapViews();
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
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler) {
            final RecyclerView parent = getRecyclerView();
            if (parent.getChildCount() == 0) {
                return 0;
            }

            int scrolled = 0;
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();
            if (dy < 0) {
                while (scrolled > dy) {
                    final View topView = parent.getChildAt(0);
                    final int hangingTop = Math.max(-topView.getTop(), 0);
                    final int scrollBy = Math.min(scrolled - dy, hangingTop);
                    scrolled -= scrollBy;
                    parent.offsetChildrenVertical(scrollBy);
                    if (mFirstPosition > 0 && scrolled > dy) {
                        mFirstPosition--;
                        View v = recycler.getViewForPosition(mFirstPosition);
                        addView(v, 0);
                        v.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                        final int bottom = topView.getTop(); // TODO decorated top?
                        final int top = bottom - v.getMeasuredHeight();
                        v.layout(left, top, right, bottom);
                    } else {
                        break;
                    }
                }
            } else if (dy > 0) {
                final int parentHeight = parent.getHeight();
                while (scrolled < dy) {
                    final View bottomView = parent.getChildAt(parent.getChildCount() - 1);
                    final int hangingBottom = Math.max(bottomView.getBottom() - parentHeight, 0);
                    final int scrollBy = -Math.min(dy - scrolled, hangingBottom);
                    scrolled -= scrollBy;
                    parent.offsetChildrenVertical(scrollBy);
                    if (scrolled < dy &&
                            parent.getAdapter().getItemCount() > mFirstPosition + parent.getChildCount()) {
                        View v = recycler.getViewForPosition(
                                mFirstPosition + parent.getChildCount());
                        final int top = parent.getChildAt(parent.getChildCount() - 1).getBottom();
                        addView(v);
                        v.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                        final int bottom = top + v.getMeasuredHeight();
                        v.layout(left, top, right, bottom);
                    } else {
                        break;
                    }
                }
            }
            detachAndScrapViewsOutOfBounds(recycler);
            return scrolled;
        }

        @Override
        public View onFocusSearchFailed(View focused, int direction,
                RecyclerView.Recycler recycler) {
            final RecyclerView rv = getRecyclerView();
            final int oldFirstPosition = mFirstPosition;
            final int oldCount = rv.getChildCount();

            if (oldCount == 0) {
                return null;
            }

            final int left = rv.getPaddingLeft();
            final int right = rv.getWidth() - rv.getPaddingRight();

            View toFocus = null;
            int newViewsHeight = 0;
            if (direction == View.FOCUS_UP || direction == View.FOCUS_BACKWARD) {
                while (mFirstPosition > 0 && newViewsHeight < mScrollDistance) {
                    mFirstPosition--;
                    View v = recycler.getViewForPosition(mFirstPosition);
                    final int bottom = rv.getChildAt(0).getTop(); // TODO decorated top?
                    addView(v, 0);
                    v.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                    final int top = bottom - v.getMeasuredHeight();
                    v.layout(left, top, right, bottom);
                    if (v.isFocusable()) {
                        toFocus = v;
                        break;
                    }
                }
            }
            if (direction == View.FOCUS_DOWN || direction == View.FOCUS_FORWARD) {
                while (mFirstPosition + rv.getChildCount() < rv.getAdapter().getItemCount() &&
                        newViewsHeight < mScrollDistance) {
                    View v = recycler.getViewForPosition(mFirstPosition + rv.getChildCount());
                    final int top = rv.getChildAt(rv.getChildCount() - 1).getBottom();
                    addView(v);
                    v.measure(MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                    final int bottom = top + v.getMeasuredHeight();
                    v.layout(left, top, right, bottom);
                    if (v.isFocusable()) {
                        toFocus = v;
                        break;
                    }
                }
            }

            return toFocus;
        }

        public void detachAndScrapViewsOutOfBounds(RecyclerView.Recycler recycler) {
            final RecyclerView parent = getRecyclerView();
            final int childCount = parent.getChildCount();
            final int parentWidth = parent.getWidth();
            final int parentHeight = parent.getHeight();
            boolean foundFirst = false;
            int first = 0;
            int last = 0;
            for (int i = 0; i < childCount; i++) {
                final View v = parent.getChildAt(i);
                if (v.hasFocus() || (v.getRight() >= 0 && v.getLeft() <= parentWidth &&
                        v.getBottom() >= 0 && v.getTop() <= parentHeight)) {
                    if (!foundFirst) {
                        first = i;
                        foundFirst = true;
                    }
                    last = i;
                }
            }
            for (int i = childCount - 1; i > last; i--) {
                recycler.detachAndScrapView(parent.getChildAt(i));
            }
            for (int i = 0; i < first; i++) {
                recycler.detachAndScrapView(parent.getChildAt(i));
            }
            if (parent.getChildCount() == 0) {
                mFirstPosition = 0;
            } else {
                mFirstPosition += first;
            }
        }
    }

    class MyAdapter extends RecyclerView.Adapter {
        private int mBackground;
        private ArrayList<String> mValues;

        public MyAdapter(String[] strings) {
            TypedValue val = new TypedValue();
            RecyclerViewActivity.this.getTheme().resolveAttribute(
                    R.attr.selectableItemBackground, val, true);
            mBackground = val.resourceId;
            mValues = new ArrayList();
            Collections.addAll(mValues, strings);
        }

        @Override
        public RecyclerView.ViewHolder createViewHolder(ViewGroup parent, int viewType) {
            final ViewHolder h = new ViewHolder(new TextView(RecyclerViewActivity.this));
            h.textView.setMinimumHeight(128);
            h.textView.setFocusable(true);
            h.textView.setBackgroundResource(mBackground);
            h.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int pos = h.getPosition();
                    if (mValues.size() > pos + 1) {
                        final String t = mValues.get(pos);
                        mValues.set(pos, mValues.get(pos + 1));
                        mValues.set(pos + 1, t);
                        notifyDataSetChanged();
                    }
                }
            });
            return h;
        }

        @Override
        public void bindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((ViewHolder) holder).textView.setText(mValues.get(position));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(TextView v) {
            super(v);
            textView = v;
        }
    }
}
