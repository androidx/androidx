/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * Wrapper for Animator that is aware of quota. Animator's animations will be played only if given
 * quota manager allows. If not, non infinite animation will jump to an end.
 */
class QuotaAwareAnimator {
    @Nullable private ValueAnimator mAnimator;

    @NonNull private final QuotaManager mQuotaManager;
    @NonNull private final QuotaReleasingAnimatorListener mListener;

    QuotaAwareAnimator(@Nullable ValueAnimator animator, @NonNull QuotaManager quotaManager) {
        this.mAnimator = animator;
        this.mQuotaManager = quotaManager;
        this.mListener = new QuotaReleasingAnimatorListener(quotaManager);

        if (this.mAnimator != null) {
            this.mAnimator.addListener(mListener);
        }
    }

    /**
     * Sets the new animator with {link @QuotaReleasingListener} added. Previous animator will be
     * canceled.
     */
    void updateAnimator(@NonNull ValueAnimator animator) {
        cancelAnimator();

        this.mAnimator = animator;
        this.mAnimator.addListener(mListener);
    }

    /** Resets the animator to null. Previous animator will be canceled. */
    void resetAnimator() {
        cancelAnimator();

        mAnimator = null;
    }

    /**
     * Tries to start animation. Returns true if quota allows the animator to start. Otherwise, it
     * returns false.
     */
    @UiThread
    boolean tryStartAnimation() {
        if (mAnimator == null) {
            return false;
        }
        ValueAnimator localAnimator = mAnimator;
        if (mQuotaManager.tryAcquireQuota(1)) {
            startAnimator(localAnimator);
            return true;
        } else {
            if (!isInfiniteAnimator()) {
                localAnimator.end();
            }
            return false;
        }
    }

    /**
     * Tries to start/resume infinite animation. Returns true if quota allows the animator to
     * start/resume. Otherwise, it returns false.
     */
    @UiThread
    boolean tryStartOrResumeAnimator() {
        if (mAnimator == null) {
            return false;
        }
        ValueAnimator localAnimator = mAnimator;
        if (localAnimator.isPaused() && mQuotaManager.tryAcquireQuota(1)) {
            resumeAnimator(localAnimator);
        } else if (isInfiniteAnimator() && mQuotaManager.tryAcquireQuota(1)) {
            // Infinite animators created when this node was invisible have not started yet.
            startAnimator(localAnimator);
        }
        // No need to jump to an end of animation if it can't be played as they are infinite.
        return false;
    }

    private void resumeAnimator(ValueAnimator localAnimator) {
        localAnimator.resume();
        mListener.mIsUsingQuota = true;
    }

    private void startAnimator(ValueAnimator localAnimator) {
        localAnimator.start();
        mListener.mIsUsingQuota = true;
    }

    /**
     * Stops or pauses the animator, depending on it's state. If stopped, it will assign the end
     * value.
     */
    @UiThread
    void stopOrPauseAnimator() {
        if (mAnimator == null) {
            return;
        }
        ValueAnimator localAnimator = mAnimator;
        if (isInfiniteAnimator()) {
            localAnimator.pause();
        } else {
            // This causes the animation to assign the end value of the property being animated.
            stopAnimator();
        }
    }

    /** Stops the animator, which will cause it to assign the end value. */
    @UiThread
    void stopAnimator() {
        if (mAnimator == null) {
            return;
        }
        mAnimator.end();
    }

    /** Cancels the animator, which will stop in its tracks. */
    @UiThread
    void cancelAnimator() {
        if (mAnimator == null) {
            return;
        }
        // This calls both onCancel and onEnd methods from listener.
        mAnimator.cancel();
        mAnimator.removeListener(mListener);
    }

    /** Returns whether the animator in this class has an infinite duration. */
    protected boolean isInfiniteAnimator() {
        return mAnimator != null && mAnimator.getTotalDuration() == Animator.DURATION_INFINITE;
    }

    /** Returns whether this node has a running animation. */
    boolean hasRunningAnimation() {
        return mAnimator != null && mAnimator.isRunning();
    }

    /**
     * The listener used for animatable nodes to release quota when the animation is finished or
     * paused.
     */
    private static final class QuotaReleasingAnimatorListener extends AnimatorListenerAdapter {
        @NonNull private final QuotaManager mQuotaManager;

        // We need to keep track of whether the animation has started because pipeline has initiated
        // and it has received quota, or onAnimationStart listener has been called because of the
        // inner ValueAnimator implementation (i.e., when calling end() on animator to assign it end
        // value, ValueAnimator will call start first if animation is not running to get it to the
        // end state.
        boolean mIsUsingQuota = false;

        QuotaReleasingAnimatorListener(@NonNull QuotaManager quotaManager) {
            this.mQuotaManager = quotaManager;
        }

        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationResume(Animator animation) {}

        @Override
        @UiThread
        public void onAnimationEnd(Animator animation) {
            if (mIsUsingQuota) {
                mQuotaManager.releaseQuota(1);
                mIsUsingQuota = false;
            }
        }

        @Override
        @UiThread
        public void onAnimationPause(Animator animation) {
            if (mIsUsingQuota) {
                mQuotaManager.releaseQuota(1);
                mIsUsingQuota = false;
            }
        }
    }
}
