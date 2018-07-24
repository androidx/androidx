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

package androidx.recyclerview.widget;

import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.ViewCompat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestedFrameLayout extends FrameLayout implements NestedScrollingParent2 {

    private NestedScrollingParent2 mNestedScrollingDelegate;
    private CountDownLatch mDrawLatch;
    private CountDownLatch mLayoutLatch;

    public TestedFrameLayout(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public void expectDraws(int count) {
        mDrawLatch = new CountDownLatch(count);
    }

    public void waitForDraw(int seconds) throws InterruptedException {
        assertTrue(mDrawLatch.await(seconds, TimeUnit.SECONDS));
    }

    public void expectLayouts(int count) {
        mLayoutLatch = new CountDownLatch(count);
    }

    public void waitForLayout(int seconds) throws InterruptedException {
        assertTrue(mLayoutLatch.await(seconds, TimeUnit.SECONDS));
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

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mLayoutLatch != null) {
            mLayoutLatch.countDown();
        }
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (mDrawLatch != null) {
            mDrawLatch.countDown();
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
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed) {
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                ViewCompat.TYPE_TOUCH);
    }

    @Override
    public void onStopNestedScroll(View target) {
        onStopNestedScroll(target, ViewCompat.TYPE_TOUCH);
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingDelegate != null
                ? mNestedScrollingDelegate.getNestedScrollAxes()
                : 0;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target,
            @ViewCompat.ScrollAxis int axes, @ViewCompat.NestedScrollType int type) {
        return mNestedScrollingDelegate != null
                && mNestedScrollingDelegate.onStartNestedScroll(child, target, axes, type);
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target,
            @ViewCompat.ScrollAxis int axes, @ViewCompat.NestedScrollType int type) {
        if (mNestedScrollingDelegate != null) {
            mNestedScrollingDelegate.onNestedScrollAccepted(child, target, axes, type);
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return mNestedScrollingDelegate != null
                && mNestedScrollingDelegate.onNestedPreFling(target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingDelegate != null
                && mNestedScrollingDelegate.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, @ViewCompat.NestedScrollType int type) {
        if (mNestedScrollingDelegate != null) {
            mNestedScrollingDelegate.onNestedScroll(target, dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, type);
        }
    }

    @Override
    public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
            @ViewCompat.NestedScrollType int type) {
        if (mNestedScrollingDelegate != null) {
            mNestedScrollingDelegate.onNestedPreScroll(target, dx, dy, consumed, type);
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, @ViewCompat.NestedScrollType int type) {
        if (mNestedScrollingDelegate != null) {
            mNestedScrollingDelegate.onStopNestedScroll(target, type);
        }
    }

    public void setNestedScrollingDelegate(NestedScrollingParent2 delegate) {
        mNestedScrollingDelegate = delegate;
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
