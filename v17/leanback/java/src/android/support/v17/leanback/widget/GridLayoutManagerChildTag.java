/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.view.View;

/**
 * The Tag is associated with a child view and layout manager when child view is
 * attached to view hierarchy by layout manager.
 * The class currently has two jobs:
 * - Caches align view center to avoid calling findViewById().
 * - Manages child view layout animation.
 */
final class GridLayoutManagerChildTag implements TimeListener {

    private View mView;

    // For placement
    private int mLeftInset;
    private int mTopInset;
    private int mRighInset;
    private int mBottomInset;

    // For alignment
    private int mAlignX;
    private int mAlignY;

    // For animations
    private TimeAnimator mAnimator;
    private long mDuration;
    private boolean mFirstAttached;
    // current virtual view position (scrollOffset + left/top) in the GridLayoutManager
    private int mViewX, mViewY;
    // animation start value of translation x and y
    private float mAnimationStartTranslationX, mAnimationStartTranslationY;

    void attach(GridLayoutManager layout, View view) {
        endAnimate();
        mView = view;
        mFirstAttached = true;
    }

    boolean isAttached() {
        return mView != null;
    }

    void detach() {
        endAnimate();
        mView = null;
    }

    int getAlignX() {
        return mAlignX;
    }

    int getAlignY() {
        return mAlignY;
    }

    int getOpticalLeft() {
        return mView.getLeft() + mLeftInset;
    }

    int getOpticalTop() {
        return mView.getTop() + mTopInset;
    }

    int getOpticalRight() {
        return mView.getRight() - mRighInset;
    }

    int getOpticalBottom() {
        return mView.getBottom() - mBottomInset;
    }

    int getOpticalWidth() {
        return mView.getWidth() - mLeftInset - mRighInset;
    }

    int getOpticalHeight() {
        return mView.getHeight() - mTopInset - mBottomInset;
    }

    void setAlignX(int alignX) {
        mAlignX = alignX;
    }

    void setAlignY(int alignY) {
        mAlignY = alignY;
    }

    void setOpticalInsets(int leftInset, int topInset, int rightInset, int bottomInset) {
        mLeftInset = leftInset;
        mTopInset = topInset;
        mRighInset = rightInset;
        mBottomInset = bottomInset;
    }

    void startAnimate(GridLayoutManager layout, long startDelay) {
        if (mAnimator == null) {
            mAnimator = new TimeAnimator();
            mAnimator.setTimeListener(this);
        }
        if (mFirstAttached) {
            // first time record the initial location and return without animation
            // TODO do we need initial animation?
            mViewX = layout.getScrollOffsetX() + getOpticalLeft();
            mViewY = layout.getScrollOffsetY() + getOpticalTop();
            mFirstAttached = false;
            return;
        }
        if (!layout.isChildLayoutAnimated()) {
            return;
        }
        int newViewX = layout.getScrollOffsetX() + getOpticalLeft();
        int newViewY = layout.getScrollOffsetY() + getOpticalTop();
        if (newViewX != mViewX || newViewY != mViewY) {
            mAnimator.cancel();
            mAnimationStartTranslationX = mView.getTranslationX();
            mAnimationStartTranslationY = mView.getTranslationY();
            mAnimationStartTranslationX += mViewX - newViewX;
            mAnimationStartTranslationY += mViewY - newViewY;
            mDuration = layout.getChildLayoutAnimationDuration();
            mAnimator.setDuration(mDuration);
            mAnimator.setInterpolator(layout.getChildLayoutAnimationInterpolator());
            mAnimator.setStartDelay(startDelay);
            mAnimator.start();
            mViewX = newViewX;
            mViewY = newViewY;
        }
    }

    void endAnimate() {
        if (mAnimator != null) {
            mAnimator.end();
        }
        if (mView != null) {
            mView.setTranslationX(0);
            mView.setTranslationY(0);
        }
    }

    @Override
    public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
        if (totalTime >= mDuration) {
            mAnimator.end();
            mView.setTranslationX(0);
            mView.setTranslationY(0);
        } else {
            float fraction = (float) (totalTime / (double)mDuration);
            float fractionToEnd = 1 - mAnimator.getInterpolator().getInterpolation(fraction);
            mView.setTranslationX(fractionToEnd * mAnimationStartTranslationX);
            mView.setTranslationY(fractionToEnd * mAnimationStartTranslationY);
        }
    }
}
