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
 * limitations under the License
 */

package android.support.v17.leanback.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * A ViewGroup which arranges its children in an overlapping stack with the most
 * recently added view displayed at the top. Adding and removing views is animated.
 *
 * @attr ref R.styleable#StackedLayout_cardWidth
 * @attr ref R.styleable#StackedLayout_stackShift
 * @attr ref R.styleable#StackedLayout_elevationIncrement
 * @attr ref R.styleable#StackedLayout_cardGravity
 *
 * @hide
 */
public class StackedLayout extends ViewGroup {

    private int mCardWidth;
    private int mStackShift;
    private float mElevationIncrement;
    private int mCardGravity;

    private int mAddedViews = 0;
    private int mRemovedViews = 0;
    private List<View> mQueuedRemovedViews;
    private Animator mCurrentAnimator;

    private OnHierarchyChangeListener mHierarchyChangeListener;
    private final OnHierarchyChangeListener mHierarchyChangeListenerInternal =
            new OnHierarchyChangeInternalListener();

    public StackedLayout(Context context) {
        this(context, null);
    }

    public StackedLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackedLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.StackedLayout, defStyleAttr, 0);
        mCardWidth = Math.round(a.getDimension(R.styleable.StackedLayout_cardWidth, 0));
        mStackShift = Math.round(a.getDimension(R.styleable.StackedLayout_stackShift, 0));
        mElevationIncrement = a.getDimension(R.styleable.StackedLayout_elevationIncrement, 0);
        mCardGravity = a.getInt(R.styleable.StackedLayout_cardGravity, GravityCompat.END);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        super.setOnHierarchyChangeListener(mHierarchyChangeListenerInternal);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        super.setOnHierarchyChangeListener(null);
    }

    @Override
    public void setOnHierarchyChangeListener(OnHierarchyChangeListener listener) {
        mHierarchyChangeListener = listener;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return false;
        }
        final View lastChild = getChildAt(childCount - 1);
        return lastChild.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        final int width;
        final int height;

        switch (widthMode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.AT_MOST:
                width = widthSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                width = ViewCompat.getMinimumWidth(this);
                break;
        }

        switch (heightMode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.AT_MOST:
                height = heightSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
                height = ViewCompat.getMinimumHeight(this);
                break;
        }

        setMeasuredDimension(width, height);

        final int childWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(mCardWidth, MeasureSpec.EXACTLY);
        final int childHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(height - getPaddingTop() - getPaddingBottom(),
                        MeasureSpec.AT_MOST);

        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final boolean rightEdge =
                (GravityCompat.getAbsoluteGravity(mCardGravity, ViewCompat.getLayoutDirection(this))
                & Gravity.RIGHT) == Gravity.RIGHT;

        layoutInternal(l, t, r, b, rightEdge);
        if (mAddedViews != 0 || mRemovedViews != 0) {
            if (mCurrentAnimator != null) {
                mCurrentAnimator.cancel();
                mCurrentAnimator = null;
            }

            final int initialPositions[] = getChildLeftPositions();

            mAddedViews = 0;
            mRemovedViews = 0;
            layoutInternal(l, t, r, b, rightEdge);

            final int finalPositions[] = getChildLeftPositions();


            final int childCount = getChildCount();
            final List<Animator> animators = new ArrayList<>(childCount);

            for (int childIndex = 0; childIndex < childCount; childIndex++) {
                final View child = getChildAt(childIndex);
                // Want: initialPos = finalPos + translationX
                // Thus: initialPos - finalPos = translationX
                // Plus any current translation in case we interrupted an animation in progress

                final int newTranslationX = initialPositions[childIndex]
                        - finalPositions[childIndex] + Math.round(child.getTranslationX());

                child.setTranslationX(newTranslationX);

                if (newTranslationX != 0) {
                    // Animate to zero
                    animators.add(ObjectAnimator.ofFloat(child, "translationX", 0));
                }
            }

            if (mQueuedRemovedViews != null) {
                for (final View child : mQueuedRemovedViews) {
                    // Want: initialPos = finalPos + translationX
                    // Thus: initialPos - finalPos = translationX
                    // Plus any current translation in case we interrupted an animation in progress

                    final int newTranslationX;

                    if (rightEdge) {
                        newTranslationX = child.getLeft() - getRight()
                                + Math.round(child.getTranslationX());
                        // Move the child to the new position and set the translation
                        child.offsetLeftAndRight(getRight() - child.getLeft());
                    } else {
                        newTranslationX = child.getLeft() - getLeft() + child.getWidth()
                                + Math.round(child.getTranslationX());
                        // Move the child to the new position and set the translation
                        child.offsetLeftAndRight(getLeft() - child.getWidth() - child.getLeft());
                    }

                    child.setTranslationX(newTranslationX);

                    // Animate to zero
                    final Animator animator = ObjectAnimator.ofFloat(child, "translationX", 0);
                    animator.addListener(new RemovalAnimationListener(child));
                    animators.add(animator);
                }
                mQueuedRemovedViews = null;
            }

            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            mCurrentAnimator = animatorSet;
            mCurrentAnimator.start();
        }

    }

    private int[] getChildLeftPositions() {
        final int childCount = getChildCount();
        final int positions[] = new int[childCount];
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            positions[childIndex] = getChildAt(childIndex).getLeft();
        }
        return positions;
    }

    private void layoutInternal(int l, int t, int r, int b, boolean rightEdge) {
        final int parentLeft = getPaddingLeft();
        final int parentRight = r - l - getPaddingRight();

        final int parentTop = getPaddingTop();
        final int parentBottom = b - t - getPaddingBottom();

        final float baseElevation = ViewCompat.getElevation(this);

        final int childCount = getChildCount();
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            final View child = getChildAt(childIndex);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = Math.round(mCardWidth);
                final int height = child.getMeasuredHeight();

                final int childLeft;
                if (childIndex >= childCount - mAddedViews) {
                    // This is a freshly added view, start it off the edge of the container.
                    if (rightEdge) {
                        childLeft = parentRight;
                    } else {
                        childLeft = parentLeft - width;
                    }
                } else {
                    // This is an existing view, just place it normally. If there are added/removed
                    // views, then place it as if those views have not yet been added/removed.
                    if (rightEdge) {
                        childLeft = parentRight - width - lp.rightMargin -
                                (childCount - childIndex - (1 + mAddedViews - mRemovedViews))
                                        * mStackShift;
                    } else {
                        childLeft = parentLeft + lp.leftMargin +
                                (childCount - childIndex - (1 + mAddedViews - mRemovedViews))
                                        * mStackShift;
                    }
                }
                final int childTop = parentTop + lp.topMargin;

                child.layout(childLeft, childTop, childLeft + width, childTop + height);

                ViewCompat.setElevation(child,
                        baseElevation + (childIndex + 1) * mElevationIncrement);
            }
        }
    }

    public class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    private class OnHierarchyChangeInternalListener implements OnHierarchyChangeListener {

        @Override
        public void onChildViewAdded(View parent, View child) {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                final View oldChild = getChildAt(i);
                oldChild.clearFocus();
            }
            mAddedViews++;
            if (mHierarchyChangeListener != null) {
                mHierarchyChangeListener.onChildViewAdded(parent, child);
            }
        }

        @Override
        public void onChildViewRemoved(View parent, final View child) {
            if (mHierarchyChangeListener != null) {
                mHierarchyChangeListener.onChildViewRemoved(parent, child);
            }
            mRemovedViews++;
            if (ViewGroupOverlayHelper.supportsOverlay()) {
                ViewGroupOverlayHelper.addChildToOverlay(StackedLayout.this, child);
                if (mQueuedRemovedViews == null) {
                    mQueuedRemovedViews = new ArrayList<>(1);
                }
                mQueuedRemovedViews.add(child);
            }
        }
    }

    private class RemovalAnimationListener implements Animator.AnimatorListener {
        private final View mView;

        public RemovalAnimationListener(View view) {
            mView = view;
        }

        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            ViewGroupOverlayHelper.removeChildFromOverlay(StackedLayout.this, mView);
            animation.removeListener(this);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            // Re-add the view to the queued list so that the next animation continues
            // animating it away
            if (mQueuedRemovedViews == null) {
                mQueuedRemovedViews = new ArrayList<>(1);
            }
            mQueuedRemovedViews.add(mView);
            animation.removeListener(this);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {}

        @Override
        public String toString() {
            return getClass().getName() + " [" + mView.toString() + "]";
        }
    }

}
