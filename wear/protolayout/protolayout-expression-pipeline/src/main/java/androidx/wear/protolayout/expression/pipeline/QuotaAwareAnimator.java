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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for Animator that is aware of quota. Animator's animations will be played only if given
 * quota manager allows. If not, non infinite animation will jump to an end. Any existing listeners
 * on wrapped {@link Animator} will be replaced.
 */
class QuotaAwareAnimator {
    @Nullable private ValueAnimator mAnimator;

    @NonNull private final QuotaReleasingAnimatorListener mListener;

    QuotaAwareAnimator(@Nullable ValueAnimator animator, @NonNull QuotaManager quotaManager) {
        this.mAnimator = animator;
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
        this.mAnimator.addPauseListener(mListener);
    }

    /** Resets the animator to null. Previous animator will be canceled. */
    void resetAnimator() {
        cancelAnimator();

        mAnimator = null;
    }

    /**
     * Tries to start animation. This method will call start on animation, but when animation is due
     * to start (i.e. after the given delay), listener will check the quota and allow/disallow
     * animation to be played.
     */
    @UiThread
    void tryStartAnimation() {
        if (mAnimator == null) {
            return;
        }

        mAnimator.start();
    }

    /**
     * Tries to start/resume infinite animation. This method will call start/resume on animation,
     * but when animation is due to start (i.e. after the given delay), listener will check the
     * quota and allow/disallow animation to be played.
     */
    @UiThread
    void tryStartOrResumeInfiniteAnimation() {
        if (mAnimator == null) {
            return;
        }
        ValueAnimator localAnimator = mAnimator;
        if (localAnimator.isPaused()) {
            localAnimator.resume();
        } else if (isInfiniteAnimator()) {
            // Infinite animators created when this node was invisible have not started yet.
            localAnimator.start();
        }
        // No need to jump to an end of animation if it can't be played as they are infinite.
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
        mAnimator.removePauseListener(mListener);
    }

    /** Returns whether the animator in this class has an infinite duration. */
    protected boolean isInfiniteAnimator() {
        return mAnimator != null && mAnimator.getTotalDuration() == Animator.DURATION_INFINITE;
    }

    /**
     * Returns whether this node has a running or started animation. Started means that animation is
     * scheduled to run, but it has set time delay.
     */
    boolean hasRunningOrStartedAnimation() {
        return mAnimator != null
                && (mAnimator.isRunning() || /* delayed animation */ mAnimator.isStarted());
    }

    /**
     * The listener used for animatable nodes to release quota when the animation is finished or
     * paused. Additionally, when {@link
     * android.animation.Animator.AnimatorListener#onAnimationStart(Animator)} is called, this
     * listener will check quota, and if there isn't any available, it will jump to an end of
     * animation.
     */
    private static final class QuotaReleasingAnimatorListener extends AnimatorListenerAdapter {
        @NonNull private final QuotaManager mQuotaManager;

        // We need to keep track of whether the animation has started because pipeline has initiated
        // and it has received quota, or onAnimationStart listener has been called because of the
        // inner ValueAnimator implementation (i.e., when calling end() on animator to assign it end
        // value, ValueAnimator will call start first if animation is not running to get it to the
        // end state.
        @NonNull final AtomicBoolean mIsUsingQuota = new AtomicBoolean(false);

        QuotaReleasingAnimatorListener(@NonNull QuotaManager quotaManager) {
            this.mQuotaManager = quotaManager;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            acquireQuota(animation);
        }

        @Override
        public void onAnimationResume(Animator animation) {
            acquireQuota(animation);
        }

        @Override
        @UiThread
        public void onAnimationEnd(Animator animation) {
            releaseQuota();
        }

        @Override
        @UiThread
        public void onAnimationPause(Animator animation) {
            releaseQuota();
        }

        /**
         * This method will block the given Animator from running animation if there is no enough
         * quota. In that case, animation will jump to an end.
         */
        private void acquireQuota(Animator animation) {
            if (!mQuotaManager.tryAcquireQuota(1)) {
                mIsUsingQuota.set(false);
                animation.end();
                // End will fire end value via UpdateListener. We don't want any new updates to be
                // pushed to the callback.
                if (animation instanceof ValueAnimator) {
                    ((ValueAnimator) animation).removeAllUpdateListeners();
                }
            } else {
                mIsUsingQuota.set(true);
            }
        }

        private void releaseQuota() {
            if (mIsUsingQuota.compareAndSet(true, false)) {
                mQuotaManager.releaseQuota(1);
            }
        }
    }
}
