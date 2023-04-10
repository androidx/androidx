/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.helper.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.motion.widget.MotionHelper;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.motion.widget.MotionScene;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.R;

import java.util.ArrayList;

/**
 * Carousel works within a MotionLayout to provide a simple recycler like pattern.
 * Based on a series of Transitions and callback to give you the ability to swap views.
 */
public class Carousel extends MotionHelper {
    private static final boolean DEBUG = false;
    private static final String TAG = "Carousel";
    private Adapter mAdapter = null;
    private final ArrayList<View> mList = new ArrayList<>();
    private int mPreviousIndex = 0;
    private int mIndex = 0;
    private MotionLayout mMotionLayout;
    private int mFirstViewReference = -1;
    private boolean mInfiniteCarousel = false;
    private int mBackwardTransition = -1;
    private int mForwardTransition = -1;
    private int mPreviousState = -1;
    private int mNextState = -1;
    private float mDampening = 0.9f;
    private int mStartIndex = 0;
    private int mEmptyViewBehavior = INVISIBLE;

    public static final int TOUCH_UP_IMMEDIATE_STOP = 1;
    public static final int TOUCH_UP_CARRY_ON = 2;

    private int mTouchUpMode = TOUCH_UP_IMMEDIATE_STOP;
    private float mVelocityThreshold = 2f;
    private int mTargetIndex = -1;
    private int mAnimateTargetDelay = 200;

    /**
     * Adapter for a Carousel
     */
    public interface Adapter {
        /**
         * Number of items you want to display in the Carousel
         * @return number of items
         */
        int count();

        /**
         * Callback to populate the view for the given index
         *
         * @param view
         * @param index
         */
        void populate(View view, int index);

        /**
         * Callback when we reach a new index
         * @param index
         */
        void onNewItem(int index);
    }

    public Carousel(Context context) {
        super(context);
    }

    public Carousel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Carousel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Carousel);
            final int n = a.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.Carousel_carousel_firstView) {
                    mFirstViewReference = a.getResourceId(attr, mFirstViewReference);
                } else if (attr == R.styleable.Carousel_carousel_backwardTransition) {
                    mBackwardTransition = a.getResourceId(attr, mBackwardTransition);
                } else if (attr == R.styleable.Carousel_carousel_forwardTransition) {
                    mForwardTransition = a.getResourceId(attr, mForwardTransition);
                } else if (attr == R.styleable.Carousel_carousel_emptyViewsBehavior) {
                    mEmptyViewBehavior = a.getInt(attr, mEmptyViewBehavior);
                } else if (attr == R.styleable.Carousel_carousel_previousState) {
                    mPreviousState = a.getResourceId(attr, mPreviousState);
                } else if (attr == R.styleable.Carousel_carousel_nextState) {
                    mNextState = a.getResourceId(attr, mNextState);
                } else if (attr == R.styleable.Carousel_carousel_touchUp_dampeningFactor) {
                    mDampening = a.getFloat(attr, mDampening);
                } else if (attr == R.styleable.Carousel_carousel_touchUpMode) {
                    mTouchUpMode = a.getInt(attr, mTouchUpMode);
                } else if (attr == R.styleable.Carousel_carousel_touchUp_velocityThreshold) {
                    mVelocityThreshold = a.getFloat(attr, mVelocityThreshold);
                } else if (attr == R.styleable.Carousel_carousel_infinite) {
                    mInfiniteCarousel = a.getBoolean(attr, mInfiniteCarousel);
                }
            }
            a.recycle();
        }
    }

    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Returns the number of elements in the Carousel
     *
     * @return number of elements
     */
    public int getCount() {
        if (mAdapter != null) {
            return mAdapter.count();
        }
        return 0;
    }

    /**
     * Returns the current index
     *
     * @return current index
     */
    public int getCurrentIndex() {
        return mIndex;
    }

    /**
     * Transition the carousel to the given index, animating until we reach it.
     *
     * @param index index of the element we want to reach
     * @param delay animation duration for each individual transition to the next item, in ms
     */
    public void transitionToIndex(int index, int delay) {
        mTargetIndex = Math.max(0, Math.min(getCount() - 1, index));
        mAnimateTargetDelay = Math.max(0, delay);
        mMotionLayout.setTransitionDuration(mAnimateTargetDelay);
        if (index < mIndex) {
            mMotionLayout.transitionToState(mPreviousState, mAnimateTargetDelay);
        } else {
            mMotionLayout.transitionToState(mNextState, mAnimateTargetDelay);
        }
    }

    /**
     * Jump to the given index without any animation
     *
     * @param index index of the element we want to reach
     */
    public void jumpToIndex(int index) {
        mIndex = Math.max(0, Math.min(getCount() - 1, index));
        refresh();
    }

    /**
     * Rebuilds the scene
     */
    public void refresh() {
        final int count = mList.size();
        for (int i = 0; i < count; i++) {
            View view = mList.get(i);
            if (mAdapter.count() == 0) {
                updateViewVisibility(view, mEmptyViewBehavior);
            } else {
                updateViewVisibility(view, VISIBLE);
            }
        }
        mMotionLayout.rebuildScene();
        updateItems();
    }

    @Override
    public void onTransitionChange(MotionLayout motionLayout,
                                   int startId,
                                   int endId,
                                   float progress) {
        if (DEBUG) {
            System.out.println("onTransitionChange from " + startId
                    + " to " + endId + " progress " + progress);
        }
        mLastStartId = startId;
    }

    int mLastStartId = -1;

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
        mPreviousIndex = mIndex;
        if (currentId == mNextState) {
            mIndex++;
        } else if (currentId == mPreviousState) {
            mIndex--;
        }
        if (mInfiniteCarousel) {
            if (mIndex >= mAdapter.count()) {
                mIndex = 0;
            }
            if (mIndex < 0) {
                mIndex = mAdapter.count() - 1;
            }
        } else {
            if (mIndex >= mAdapter.count()) {
                mIndex = mAdapter.count() - 1;
            }
            if (mIndex < 0) {
                mIndex = 0;
            }
        }

        if (mPreviousIndex != mIndex) {
            mMotionLayout.post(mUpdateRunnable);
        }
    }

    @SuppressWarnings("unused")
    private void enableAllTransitions(boolean enable) {
        ArrayList<MotionScene.Transition> transitions = mMotionLayout.getDefinedTransitions();
        for (MotionScene.Transition transition : transitions) {
            transition.setEnabled(enable);
        }
    }

    private boolean enableTransition(int transitionID, boolean enable) {
        if (transitionID == -1) {
            return false;
        }
        if (mMotionLayout == null) {
            return false;
        }
        MotionScene.Transition transition = mMotionLayout.getTransition(transitionID);
        if (transition == null) {
            return false;
        }
        if (enable == transition.isEnabled()) {
            return false;
        }
        transition.setEnabled(enable);
        return true;
    }

    Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            mMotionLayout.setProgress(0);
            updateItems();
            mAdapter.onNewItem(mIndex);
            float velocity = mMotionLayout.getVelocity();
            if (mTouchUpMode == TOUCH_UP_CARRY_ON && velocity > mVelocityThreshold
                    && mIndex < mAdapter.count() - 1) {
                final float v = velocity * mDampening;
                if (mIndex == 0 && mPreviousIndex > mIndex) {
                    // don't touch animate when reaching the first item
                    return;
                }
                if (mIndex == mAdapter.count() - 1 && mPreviousIndex < mIndex) {
                    // don't touch animate when reaching the last item
                    return;
                }
                mMotionLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mMotionLayout.touchAnimateTo(MotionLayout.TOUCH_UP_DECELERATE_AND_COMPLETE,
                                1, v);
                    }
                });
            }
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mList.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MotionLayout container = null;
        if (getParent() instanceof MotionLayout) {
            container = (MotionLayout) getParent();
        } else {
            return;
        }
        mList.clear();
        for (int i = 0; i < mCount; i++) {
            int id = mIds[i];
            View view = container.getViewById(id);
            if (mFirstViewReference == id) {
                mStartIndex = i;
            }
            mList.add(view);
        }
        mMotionLayout = container;
        // set up transitions if needed
        if (mTouchUpMode == TOUCH_UP_CARRY_ON) {
            MotionScene.Transition forward = mMotionLayout.getTransition(mForwardTransition);
            if (forward != null) {
                forward.setOnTouchUp(MotionLayout.TOUCH_UP_DECELERATE_AND_COMPLETE);
            }
            MotionScene.Transition backward = mMotionLayout.getTransition(mBackwardTransition);
            if (backward != null) {
                backward.setOnTouchUp(MotionLayout.TOUCH_UP_DECELERATE_AND_COMPLETE);
            }
        }
        updateItems();
    }

    /**
     * Update the view visibility on the different ConstraintSets
     *
     * @param view
     * @param visibility
     * @return
     */
    private boolean updateViewVisibility(View view, int visibility) {
        if (mMotionLayout == null) {
            return false;
        }
        boolean needsMotionSceneRebuild = false;
        int[] constraintSets = mMotionLayout.getConstraintSetIds();
        for (int i = 0; i < constraintSets.length; i++) {
            needsMotionSceneRebuild |= updateViewVisibility(constraintSets[i], view, visibility);
        }
        return needsMotionSceneRebuild;
    }

    private boolean updateViewVisibility(int constraintSetId, View view, int visibility) {
        ConstraintSet constraintSet = mMotionLayout.getConstraintSet(constraintSetId);
        if (constraintSet == null) {
            return false;
        }
        ConstraintSet.Constraint constraint = constraintSet.getConstraint(view.getId());
        if (constraint == null) {
            return false;
        }
        constraint.propertySet.mVisibilityMode = ConstraintSet.VISIBILITY_MODE_IGNORE;
//        if (constraint.propertySet.visibility == visibility) {
//            return false;
//        }
//        constraint.propertySet.visibility = visibility;
        view.setVisibility(visibility);
        return true;
    }

    private void updateItems() {
        if (mAdapter == null) {
            return;
        }
        if (mMotionLayout == null) {
            return;
        }
        if (mAdapter.count() == 0) {
            return;
        }
        if (DEBUG) {
            System.out.println("Update items, index: " + mIndex);
        }
        final int viewCount = mList.size();
        for (int i = 0; i < viewCount; i++) {
            // mIndex should map to i == startIndex
            View view = mList.get(i);
            int index = mIndex + i - mStartIndex;
            if (mInfiniteCarousel) {
                if (index < 0) {
                    if (mEmptyViewBehavior != View.INVISIBLE) {
                        updateViewVisibility(view, mEmptyViewBehavior);
                    } else {
                        updateViewVisibility(view, VISIBLE);
                    }
                    if (index % mAdapter.count() == 0) {
                        mAdapter.populate(view, 0);
                    } else {
                        mAdapter.populate(view, mAdapter.count() + (index % mAdapter.count()));
                    }
                } else if (index >= mAdapter.count()) {
                    if (index == mAdapter.count()) {
                        index = 0;
                    } else if (index > mAdapter.count()) {
                        index = index % mAdapter.count();
                    }
                    if (mEmptyViewBehavior != View.INVISIBLE) {
                        updateViewVisibility(view, mEmptyViewBehavior);
                    } else {
                        updateViewVisibility(view, VISIBLE);
                    }
                    mAdapter.populate(view, index);
                } else {
                    updateViewVisibility(view, VISIBLE);
                    mAdapter.populate(view, index);
                }
            } else {
                if (index < 0) {
                    updateViewVisibility(view, mEmptyViewBehavior);
                } else if (index >= mAdapter.count()) {
                    updateViewVisibility(view, mEmptyViewBehavior);
                } else {
                    updateViewVisibility(view, VISIBLE);
                    mAdapter.populate(view, index);
                }
            }
        }

        if (mTargetIndex != -1 && mTargetIndex != mIndex) {
            mMotionLayout.post(() -> {
                mMotionLayout.setTransitionDuration(mAnimateTargetDelay);
                if (mTargetIndex < mIndex) {
                    mMotionLayout.transitionToState(mPreviousState, mAnimateTargetDelay);
                } else {
                    mMotionLayout.transitionToState(mNextState, mAnimateTargetDelay);
                }
            });
        } else if (mTargetIndex == mIndex) {
            mTargetIndex = -1;
        }

        if (mBackwardTransition == -1 || mForwardTransition == -1) {
            Log.w(TAG, "No backward or forward transitions defined for Carousel!");
            return;
        }

        if (mInfiniteCarousel) {
            return;
        }

        final int count = mAdapter.count();
        if (mIndex == 0) {
            enableTransition(mBackwardTransition, false);
        } else {
            enableTransition(mBackwardTransition, true);
            mMotionLayout.setTransition(mBackwardTransition);
        }
        if (mIndex == count - 1) {
            enableTransition(mForwardTransition, false);
        } else {
            enableTransition(mForwardTransition, true);
            mMotionLayout.setTransition(mForwardTransition);
        }
    }

}
