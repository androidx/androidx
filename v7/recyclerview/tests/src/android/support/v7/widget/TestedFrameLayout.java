/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget;

import android.content.Context;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class TestedFrameLayout extends FrameLayout implements NestedScrollingParent {

    static final int TEST_NESTED_SCROLL_MODE_IGNORE = 0;
    static final int TEST_NESTED_SCROLL_MODE_CONSUME = 1;

    private int mNestedScrollMode;
    private int mNestedFlingMode;
    private boolean mNestedStopNestedScrollCalled;

    public TestedFrameLayout(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        RecyclerView recyclerView = getRvChild();
        if (recyclerView == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        FullControlLayoutParams lp = (FullControlLayoutParams) recyclerView.getLayoutParams();
        if (lp.wSpec == null && lp.hSpec == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        final int childWidthMeasureSpec;
        if (lp.wSpec != null) {
            childWidthMeasureSpec = lp.wSpec;
        } else if (lp.width == LayoutParams.MATCH_PARENT) {
            final int width = Math.max(0, getMeasuredWidth()
                    - lp.leftMargin - lp.rightMargin);
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    lp.leftMargin + lp.rightMargin, lp.width);
        }

        final int childHeightMeasureSpec;
        if (lp.hSpec != null) {
            childHeightMeasureSpec = lp.hSpec;
        } else if (lp.height == LayoutParams.MATCH_PARENT) {
            final int height = Math.max(0, getMeasuredHeight()
                    - lp.topMargin - lp.bottomMargin);
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        } else {
            childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                    lp.topMargin + lp.bottomMargin, lp.height);
        }
        recyclerView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY &&
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            setMeasuredDimension(
                    MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.getSize(heightMeasureSpec)
            );
        } else {
            setMeasuredDimension(
                    chooseSize(widthMeasureSpec,
                            recyclerView.getWidth() + getPaddingLeft() + getPaddingRight(),
                            getMinimumWidth()),
                    chooseSize(heightMeasureSpec,
                            recyclerView.getHeight() + getPaddingTop() + getPaddingBottom(),
                            getMinimumHeight()));
        }
    }

    public static int chooseSize(int spec, int desired, int min) {
        final int mode = View.MeasureSpec.getMode(spec);
        final int size = View.MeasureSpec.getSize(spec);
        switch (mode) {
            case View.MeasureSpec.EXACTLY:
                return size;
            case View.MeasureSpec.AT_MOST:
                return Math.min(size, desired);
            case View.MeasureSpec.UNSPECIFIED:
            default:
                return Math.max(desired, min);
        }
    }


    private RecyclerView getRvChild() {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) instanceof RecyclerView) {
                return (RecyclerView) getChildAt(i);
            }
        }
        return null;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof FullControlLayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new FullControlLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FullControlLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new FullControlLayoutParams(getWidth(), getHeight());
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        // Always start nested scroll
        return mNestedFlingMode == TEST_NESTED_SCROLL_MODE_CONSUME
                || mNestedScrollMode == TEST_NESTED_SCROLL_MODE_CONSUME;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.d("TestedFrameLayout", "onNestedPreFling: " + mNestedFlingMode);

        return mNestedFlingMode == TEST_NESTED_SCROLL_MODE_CONSUME;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        if (mNestedScrollMode == TEST_NESTED_SCROLL_MODE_CONSUME) {
            // We consume all scroll deltas
            consumed[0] = dx;
            consumed[1] = dy;
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed) {
        // ignore
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        // ignore
        return false;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // ignore
    }

    @Override
    public int getNestedScrollAxes() {
        // We can scroll in both direction
        return ViewCompat.SCROLL_AXIS_HORIZONTAL | ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedStopNestedScrollCalled = true;
    }

    public boolean stopNestedScrollCalled() {
        return mNestedStopNestedScrollCalled;
    }

    public void setNestedScrollMode(int mode) {
        mNestedScrollMode = mode;
    }

    public void setNestedFlingMode(int mode) {
        mNestedFlingMode = mode;
    }

    public static class FullControlLayoutParams extends FrameLayout.LayoutParams {

        Integer wSpec;
        Integer hSpec;

        public FullControlLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public FullControlLayoutParams(int width, int height) {
            super(width, height);
        }

        public FullControlLayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public FullControlLayoutParams(FrameLayout.LayoutParams source) {
            super(source);
        }

        public FullControlLayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
}
