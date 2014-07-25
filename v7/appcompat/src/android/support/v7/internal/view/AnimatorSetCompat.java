/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.internal.view;

import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;

/**
 * A very naive implementation of AnimatorSet for ViewPropertyAnimatorCompat. This should be
 * improved and moved to support-v4.
 *
 * @hide
 */
public class AnimatorSetCompat {

    private final ArrayList<ViewPropertyAnimatorCompat> mAnimators;

    private long mDuration = -1;
    private Interpolator mInterpolator;
    private ViewPropertyAnimatorListener mListener;

    private boolean mIsStarted;

    public AnimatorSetCompat() {
        mAnimators = new ArrayList<ViewPropertyAnimatorCompat>();
    }

    public void play(ViewPropertyAnimatorCompat animator) {
        if (mIsStarted) return;
        mAnimators.add(animator);
    }

    public void start() {
        if (mIsStarted) return;

        for (ViewPropertyAnimatorCompat animator : mAnimators) {
            if (mDuration >= 0) {
                animator.setDuration(mDuration);
            }
            if (mInterpolator != null) {
                animator.setInterpolator(mInterpolator);
            }
            if (mListener != null) {
                animator.setListener(mProxyListener);
            }
            animator.start();
        }

        mIsStarted = true;
    }

    public void cancel() {
        if (mIsStarted) return;
        for (ViewPropertyAnimatorCompat animator : mAnimators) {
            animator.cancel();
        }
    }

    public void setDuration(long duration) {
        if (mIsStarted) return;
        mDuration = duration;
    }

    public void setInterpolator(Interpolator interpolator) {
        if (mIsStarted) return;
        mInterpolator = interpolator;
    }

    public void setListener(ViewPropertyAnimatorListener listener) {
        if (mIsStarted) return;
        mListener = listener;
    }

    private final ViewPropertyAnimatorListenerAdapter mProxyListener
            = new ViewPropertyAnimatorListenerAdapter() {
        private int mStarted = 0;
        private int mEnded = 0;

        @Override
        public void onAnimationStart(View view) {
            if (mListener != null && ++mStarted == mAnimators.size()) {
                mListener.onAnimationStart(null);
            }
        }

        @Override
        public void onAnimationEnd(View view) {
            if (mListener != null && ++mEnded == mAnimators.size()) {
                mListener.onAnimationEnd(null);
            }
        }
    };
}
