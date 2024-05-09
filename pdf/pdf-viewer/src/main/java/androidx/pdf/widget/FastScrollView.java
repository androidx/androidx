/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.R;
import androidx.pdf.util.MathUtils;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.ObservableValue.ValueObserver;
import androidx.pdf.util.Observables;
import androidx.pdf.widget.FastScrollContentModel.FastScrollListener;

/**
 * A {@link FrameLayout} that draws a draggable scrollbar over its child views. It uses a
 * {@link FastScrollContentModel} as its model to listen for scroll events on the content view to
 * control the scrollbar and conversely to scroll the content view when the scrollbar thumb is
 * dragged.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FastScrollView extends FrameLayout implements FastScrollListener {

    private enum State {
        NONE,
        VISIBLE,
        DRAG
    }

    private static final long FADE_DELAY_MS = 1300;
    private static final float MIN_SCREENS_TO_SHOW = 1.5f;

    private final View mDragHandle;
    private final float mOriginalTranslateX;

    private FastScrollContentModel mScrollable;
    private Observables.ExposedValue<Integer> mThumbY = Observables.newExposedValueWithInitialValue(
            0);
    private float mCurrentPosition;
    private State mState = State.NONE;
    private boolean mShowScrollThumb = false;
    private final int mScrollbarMarginDefault;

    // The track's top and bottom margin include space for the scroll-thumb, but
    // this isn't included in the scrollBar margin as specified by callers.
    private int mTrackTopMargin;
    private int mTrackRightMargin;
    private int mTrackBottomMargin;

    /** Has the thumb been dragged during the display of the scrollbar */
    private boolean mDragged;

    private final ValueObserver<Integer> mYObserver =
            new ValueObserver<Integer>() {
                @Override
                public void onChange(@Nullable Integer oldValue, @Nullable Integer newValue) {
                    View view = mDragHandle;
                    if (view != null && newValue != null) {
                        int transY = newValue - (view.getMeasuredHeight() / 2);
                        view.setTranslationY(transY);
                    }
                }
            };

    public FastScrollView(@NonNull Context context, @NonNull AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(false);

        mDragHandle = LayoutInflater.from(context).inflate(R.layout.fastscroll_handle, this, false);
        mDragHandle.setAlpha(0F);
        mOriginalTranslateX = mDragHandle.getTranslationX();

        Resources res = getContext().getResources();
        mScrollbarMarginDefault = res.getDimensionPixelOffset(
                R.dimen.viewer_fastscroll_edge_offset);

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.FastScrollView, 0,
                0);
        setScrollbarMarginTop(ta.getDimensionPixelOffset(
                R.styleable.FastScrollView_scrollbarMarginTop, mScrollbarMarginDefault));
        setScrollbarMarginBottom(ta.getDimensionPixelOffset(
                R.styleable.FastScrollView_scrollbarMarginBottom, mScrollbarMarginDefault));
        ta.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addView(mDragHandle, getChildCount()); // Add to end so that we draw on top.
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Integer y = mThumbY.get();
        if (y != null) {
            mYObserver.onChange(null, y);
        }
        mThumbY.addObserver(mYObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mThumbY.removeObserver(mYObserver);
    }

    public void setScrollbarMarginTop(int scrollbarMarginTop) {
        this.mTrackTopMargin = scrollbarMarginTop + mDragHandle.getMeasuredHeight() / 2;
    }

    public void setScrollbarMarginRight(int scrollbarMarginRight) {
        // This view does not support RTL so there is no reason to expose left
        this.mTrackRightMargin = scrollbarMarginRight;
    }

    public void setScrollbarMarginBottom(int scrollbarMarginBottom) {
        this.mTrackBottomMargin = scrollbarMarginBottom + mDragHandle.getMeasuredHeight() / 2;
    }

    private void updateDragHandleX() {
        // This has to be calculated because the amount of reserve space on the right can change
        // mattering on display configuration. The FastScrollView also holds other views, as it is a
        // ViewGroup, and we want the other views to ignore the reserve space.
        mDragHandle.setX(
                (getMeasuredWidth() + mOriginalTranslateX)
                        - (mDragHandle.getMeasuredWidth() + mTrackRightMargin));
    }

    /** Set listener on scrollable. */
    public void setScrollable(@NonNull FastScrollContentModel scrollable) {
        this.mScrollable = scrollable;
        scrollable.setFastScrollListener(this);
    }

    /** Return the Y coordinate of center of the Scroller thumb, in pixels. */
    @NonNull
    public ObservableValue<Integer> getScrollerPositionY() {
        return mThumbY;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mState != State.NONE && ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPointInside(ev.getX(), ev.getY())) {
                setState(State.DRAG);
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    /** Set view as visible. */
    public void setVisible() {
        mDragHandle.setAlpha(1);
        mDragHandle.animate().setStartDelay(FADE_DELAY_MS).alpha(0F).start();
    }

    private void setState(State state) {
        switch (state) {
            case NONE:
                mDragHandle.setAlpha(0F);
                if (mDragged) {
                    // TODO: Tracker fast scroll.
                    mDragged = false;
                }
                break;
            case VISIBLE:
                setVisible();
                break;
            case DRAG:
                mDragHandle.animate().alpha(1).start();
                mDragged = true;
                break;
        }
        this.mState = state;
        refreshDrawableState();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // This is due to the different events ordering when keyboard is opened or
        // retracted vs rotate. Hence to avoid corner cases we just disable the
        // scroller when size changed, and wait until the scroll position is recomputed
        // before showing it back.
        setState(State.NONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateDragHandleX();
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (mState == State.NONE) {
            return false;
        }

        int action = me.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            if (isPointInside(me.getX(), me.getY())) {
                // This fixes a bug where an ancestor would capture events like horizontal
                // fling/scroll, despite the user not having lifted their finger while/after
                // dragging the scroll bar.
                this.requestDisallowInterceptTouchEvent(true);
                setState(State.DRAG);
                scrollTo((int) me.getY(), true);

                return true;
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (mState == State.DRAG) {
                setState(State.VISIBLE);
                scrollTo((int) me.getY(), true);

                this.requestDisallowInterceptTouchEvent(false);

                return true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mState == State.DRAG) {
                scrollTo((int) me.getY(), false);
                return true;
            }
        }
        return false;
    }

    private boolean scrollTo(int newThumbY, boolean stable) {
        int top = mTrackTopMargin;
        int bottom = getHeight() - mTrackBottomMargin;
        newThumbY = MathUtils.clamp(newThumbY, top, bottom);
        if (!stable && Math.abs(mThumbY.get() - newThumbY) < 2) {
            return false;
        }
        mThumbY.set(newThumbY);
        int scrollbarLength = bottom - top;
        float fraction = (mThumbY.get() - mTrackTopMargin) / (float) scrollbarLength;
        float scrollRange = mScrollable.estimateFullContentHeight() - mScrollable.visibleHeight();
        mScrollable.fastScrollTo(scrollRange * fraction, stable);
        return true;
    }

    boolean isPointInside(float x, float y) {
        return x > mDragHandle.getX()
                // Deliberately ignore (x < getWidth() - scrollbarMarginRight) to make it easier
                // to grab it.
                && y >= mThumbY.get() - mDragHandle.getMeasuredHeight() / 2
                && y <= mThumbY.get() + mDragHandle.getMeasuredHeight() / 2;
    }


    @Override
    public void updateFastScrollbar(float position) {
        if (position == mCurrentPosition) {
            return;
        }
        mCurrentPosition = position;

        mShowScrollThumb =
                mScrollable.estimateFullContentHeight()
                        > mScrollable.visibleHeight() * MIN_SCREENS_TO_SHOW;
        if (!mShowScrollThumb) {
            if (mState != State.NONE) {
                setState(State.NONE);
            }
            return;
        }

        if (mState != State.DRAG) {
            int scrollbarBottom = getHeight() - mTrackBottomMargin;
            int scrollbarLength = scrollbarBottom - mTrackTopMargin;
            float scrollRange =
                    mScrollable.estimateFullContentHeight() - mScrollable.visibleHeight();
            int tempThumbY = mTrackTopMargin + (int) (scrollbarLength * position / scrollRange);
            mThumbY.set(
                    MathUtils.clamp(tempThumbY, mTrackTopMargin, getHeight() - mTrackBottomMargin));
            if (mState != State.VISIBLE) {
                setState(State.VISIBLE);
            }
        }
    }
}
