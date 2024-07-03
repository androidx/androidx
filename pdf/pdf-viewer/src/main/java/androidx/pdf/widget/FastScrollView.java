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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.pdf.R;
import androidx.pdf.data.Range;
import androidx.pdf.util.MathUtils;
import androidx.pdf.util.ObservableValue.ValueObserver;
import androidx.pdf.viewer.PaginationModel;
import androidx.pdf.viewer.PaginationModelObserver;

/**
 * A {@link FrameLayout} that draws a draggable scrollbar over its child views. It is tightly
 * integrated with {@link ZoomView} as its scrolling content view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FastScrollView extends FrameLayout implements PaginationModelObserver {

    private enum State {
        NONE,
        VISIBLE,
        DRAG
    }

    private static final long FADE_DELAY_MS = 1300;
    private static final float MIN_SCREENS_TO_SHOW = 1.5f;

    private final View mDragHandle;
    private final float mOriginalTranslateX;

    private int mThumbY = 0;
    private float mCurrentPosition;
    private State mState = State.NONE;

    // The track's top and bottom margin include space for the scroll-thumb, but
    // this isn't included in the scrollBar margin as specified by callers.
    private int mTrackTopMargin;
    private int mTrackRightMargin;
    private int mTrackBottomMargin;

    /** Has the thumb been dragged during the display of the scrollbar */
    private boolean mDragged;

    private ZoomView mZoomView;
    private Rect mZoomViewBasePadding;

    private final PageIndicator mPageIndicator;
    private PaginationModel mPaginationModel;

    private final ValueObserver<ZoomView.ZoomScroll> mZoomScrollObserver =
            new ValueObserver<ZoomView.ZoomScroll>() {
                @Override
                public void onChange(@Nullable ZoomView.ZoomScroll oldValue,
                        @Nullable ZoomView.ZoomScroll newValue) {
                    if (mPaginationModel == null || !mPaginationModel.isInitialized()
                            || newValue == null) {
                        return;
                    }
                    if (mPageIndicator.setRangeAndZoom(
                            computeImportantRange(newValue), newValue.zoom, newValue.stable)) {
                        setVisible();
                    }
                    updateFastScrollbar(newValue.scrollY / newValue.zoom);
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
        int scrollbarMarginDefault = res.getDimensionPixelOffset(
                R.dimen.viewer_fastscroll_edge_offset);

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.FastScrollView, 0,
                0);
        setScrollbarMarginTop(ta.getDimensionPixelOffset(
                R.styleable.FastScrollView_scrollbarMarginTop, scrollbarMarginDefault));
        setScrollbarMarginBottom(ta.getDimensionPixelOffset(
                R.styleable.FastScrollView_scrollbarMarginBottom, scrollbarMarginDefault));
        ta.recycle();

        mPageIndicator = new PageIndicator(getContext(), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addView(mDragHandle, getChildCount()); // Add to end so that we draw on top.
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof ZoomView && mZoomView != child) {
            mZoomView = (ZoomView) child;
            configureZoomView();
        }
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        // Prevent leaks if ZoomView is removed from this ViewGroup.
        if (child instanceof ZoomView && child == mZoomView) {
            mZoomView.zoomScroll().removeObserver(mZoomScrollObserver);
            mZoomView = null;
        }
    }

    private void configureZoomView() {
        mZoomView
                .setFitMode(ZoomView.FitMode.FIT_TO_WIDTH)
                .setInitialZoomMode(ZoomView.InitialZoomMode.ZOOM_TO_FIT)
                .setRotateMode(ZoomView.RotateMode.KEEP_SAME_VIEWPORT_WIDTH)
                .setContentResizedModeX(ZoomView.ContentResizedMode.KEEP_SAME_RELATIVE);
        mZoomView.setStraightenVerticalScroll(true);
        mZoomView.zoomScroll().addObserver(mZoomScrollObserver);
        mZoomViewBasePadding =
                new Rect(
                        mZoomView.getPaddingLeft(),
                        mZoomView.getPaddingTop()
                                + getResources().getDimensionPixelSize(
                                R.dimen.viewer_doc_additional_top_offset),
                        mZoomView.getPaddingRight(),
                        mZoomView.getPaddingBottom());
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        Insets insetsCompat =
                WindowInsetsCompat.toWindowInsetsCompat(insets)
                        .getInsetsIgnoringVisibility(
                                WindowInsetsCompat.Type.systemBars()
                                        | WindowInsetsCompat.Type.displayCutout());
        if (mZoomView != null) {
            mZoomView.setPadding(
                    mZoomViewBasePadding.left + insetsCompat.left,
                    mZoomViewBasePadding.top + insetsCompat.top,
                    mZoomViewBasePadding.right + insetsCompat.top,
                    mZoomViewBasePadding.bottom + insetsCompat.bottom);
            setScrollbarMarginTop(mZoomView.getPaddingTop());
            // Ignore ZoomView's intrinsic padding on the right side as we want it to be
            // right-anchored
            setScrollbarMarginRight(insetsCompat.right);
            setScrollbarMarginBottom(mZoomView.getPaddingBottom());
        }
        mPageIndicator.getView().setTranslationX(-insetsCompat.right);
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mZoomView != null) {
            mZoomView.zoomScroll().removeObserver(mZoomScrollObserver);
        }
        if (mPaginationModel != null) {
            mPaginationModel.removeObserver(this);
        }
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
        requireZoomViewAndPaginationModel();
        int top = mTrackTopMargin;
        int bottom = getHeight() - mTrackBottomMargin;
        newThumbY = MathUtils.clamp(newThumbY, top, bottom);
        if (!stable && Math.abs(mThumbY - newThumbY) < 2) {
            return false;
        }
        mThumbY = newThumbY;
        updateDragHandleAndIndicator(newThumbY);
        int scrollbarLength = bottom - top;
        float fraction = (mThumbY - mTrackTopMargin) / (float) scrollbarLength;
        float scrollRange =
                mPaginationModel.getEstimatedFullHeight()
                        - mZoomView.getViewportHeight() / mZoomView.getZoom();
        mZoomView.scrollTo(
                mZoomView.getScrollX(), (int) (scrollRange * fraction * mZoomView.getZoom()),
                stable);
        return true;
    }

    boolean isPointInside(float x, float y) {
        return x > mDragHandle.getX()
                // Deliberately ignore (x < getWidth() - scrollbarMarginRight) to make it easier
                // to grab it.
                && y >= mThumbY - (float) mDragHandle.getMeasuredHeight() / 2
                && y <= mThumbY + (float) mDragHandle.getMeasuredHeight() / 2;
    }

    private void updateFastScrollbar(float position) {
        if (position == mCurrentPosition) {
            return;
        }
        requireZoomViewAndPaginationModel();
        mCurrentPosition = position;

        boolean showScrollThumb = mPaginationModel.getEstimatedFullHeight()
                > mZoomView.getViewportHeight() / mZoomView.getZoom() * MIN_SCREENS_TO_SHOW;
        if (!showScrollThumb) {
            if (mState != State.NONE) {
                setState(State.NONE);
            }
            return;
        }

        if (mState != State.DRAG) {
            int scrollbarBottom = getHeight() - mTrackBottomMargin;
            int scrollbarLength = scrollbarBottom - mTrackTopMargin;
            float scrollRange =
                    mPaginationModel.getEstimatedFullHeight()
                            - mZoomView.getViewportHeight() / mZoomView.getZoom();
            int tempThumbY = mTrackTopMargin + (int) (scrollbarLength * position / scrollRange);
            mThumbY = MathUtils.clamp(tempThumbY, mTrackTopMargin,
                    getHeight() - mTrackBottomMargin);
            updateDragHandleAndIndicator(mThumbY);
            if (mState != State.VISIBLE) {
                setState(State.VISIBLE);
            }
        }
    }

    private void updateDragHandleAndIndicator(int newPosition) {
        if (mDragHandle == null) {
            return;
        }
        View view = mDragHandle;
        int transY = newPosition - (view.getMeasuredHeight() / 2);
        view.setTranslationY(transY);
        View indicatorView = mPageIndicator.getView();
        indicatorView.setY(newPosition - ((float) indicatorView.getHeight() / 2));
        mPageIndicator.show();
        setVisible();
    }

    /**
     * Sets the {@link PaginationModel} to inform this view about relationships between the viewport
     * and document.
     */
    public void setPaginationModel(@NonNull PaginationModel paginationModel) {
        mPaginationModel = paginationModel;
        mPageIndicator.setNumPages(mPaginationModel.getNumPages());
        mPaginationModel.addObserver(this);
    }

    /**
     * Computes the range of pages that are entirely visible, or if no page is entirely visible,
     * returns the most visible page.
     */
    private Range computeImportantRange(ZoomView.ZoomScroll position) {
        requireZoomViewAndPaginationModel();
        int top = Math.round(position.scrollY / position.zoom);
        int bottom = Math.round((position.scrollY + mZoomView.getHeight()) / position.zoom);
        Range window = new Range(top, bottom);
        return mPaginationModel.getPagesInWindow(window, false);
    }

    @Override
    public void onPageAdded() {
        // Update PageIndicator as page dimensions become known
        requireZoomViewAndPaginationModel();
        ZoomView.ZoomScroll position = mZoomView.zoomScroll().get();
        mPageIndicator.setRangeAndZoom(computeImportantRange(position), position.zoom,
                position.stable);
    }

    private void requireZoomViewAndPaginationModel() {
        if (mZoomView == null) {
            throw new IllegalStateException("ZoomView must be a direct child of FastScrollView");
        }
        if (mPaginationModel == null || !mPaginationModel.isInitialized()) {
            throw new IllegalStateException("PaginationModel not initialized!");
        }
    }
}
