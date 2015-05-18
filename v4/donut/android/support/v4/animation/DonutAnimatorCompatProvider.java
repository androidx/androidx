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

package android.support.v4.animation;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides similar functionality to Animators on platforms prior to Honeycomb.
 * <p>
 * This is not a fully implemented API which is why it is not public.
 *
 * @hide
 */
class DonutAnimatorCompatProvider implements AnimatorProvider {

    @Override
    public ValueAnimatorCompat emptyValueAnimator() {
        return new DonutFloatValueAnimator();
    }

    private static class DonutFloatValueAnimator implements ValueAnimatorCompat {

        List<AnimatorListenerCompat> mListeners = new ArrayList<AnimatorListenerCompat>();
        List<AnimatorUpdateListenerCompat> mUpdateListeners
                = new ArrayList<AnimatorUpdateListenerCompat>();
        View mTarget;
        private long mStartTime;
        private long mDuration = 200;
        private float mFraction = 0f;

        private boolean mStarted = false;
        private boolean mEnded = false;

        public DonutFloatValueAnimator() {
        }

        private Runnable mLoopRunnable = new Runnable() {
            @Override
            public void run() {
                long dt = getTime() - mStartTime;
                float fraction = dt * 1f / mDuration;
                if (fraction > 1f || mTarget.getParent() == null) {
                    fraction = 1f;
                }
                mFraction = fraction;
                notifyUpdateListeners();
                if (mFraction >= 1f) {
                    dispatchEnd();
                } else {
                    mTarget.postDelayed(mLoopRunnable, 16);
                }
            }
        };

        private void notifyUpdateListeners() {
            for (int i = mUpdateListeners.size() - 1; i >= 0; i--) {
                mUpdateListeners.get(i).onAnimationUpdate(this);
            }
        }

        @Override
        public void setTarget(View view) {
            mTarget = view;
        }

        @Override
        public void addListener(AnimatorListenerCompat listener) {
            mListeners.add(listener);
        }

        @Override
        public void setDuration(long duration) {
            if (!mStarted) {
                mDuration = duration;
            }
        }

        @Override
        public void start() {
            if (mStarted) {
                return;
            }
            mStarted = true;
            dispatchStart();
            mFraction = 0f;
            mStartTime = getTime();
            mTarget.postDelayed(mLoopRunnable, 16);
        }

        private long getTime() {
            return mTarget.getDrawingTime();
        }

        private void dispatchStart() {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onAnimationStart(this);
            }
        }

        private void dispatchEnd() {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onAnimationEnd(this);
            }
        }

        private void dispatchCancel() {
            for (int i = mListeners.size() - 1; i >= 0; i--) {
                mListeners.get(i).onAnimationCancel(this);
            }
        }

        @Override
        public void cancel() {
            if (mEnded) {
                return;
            }
            mEnded = true;
            if (mStarted) {
                dispatchCancel();
            }
            dispatchEnd();
        }

        @Override
        public void addUpdateListener(AnimatorUpdateListenerCompat animatorUpdateListener) {
            mUpdateListeners.add(animatorUpdateListener);
        }

        @Override
        public float getAnimatedFraction() {
            return mFraction;
        }
    }

    @Override
    public void clearInterpolator(View view) {
    }
}
