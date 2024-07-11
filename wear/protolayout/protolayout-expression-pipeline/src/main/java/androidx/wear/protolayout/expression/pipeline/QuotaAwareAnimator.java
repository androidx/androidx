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

import static androidx.wear.protolayout.expression.pipeline.AnimationsHelper.applyAnimationSpecToAnimator;
import static androidx.wear.protolayout.expression.pipeline.AnimationsHelper.getRepeatDelays;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.IntEvaluator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.os.HandlerCompat;
import androidx.wear.protolayout.expression.pipeline.AnimationsHelper.RepeatDelays;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for Animator that is aware of quota. Animator's animations will be played only if given
 * quota manager allows. If not, non infinite animation will jump to an end. Any existing listeners
 * on wrapped {@link Animator} will be replaced.
 */
class QuotaAwareAnimator implements DynamicTypeAnimator {
    @NonNull protected final ValueAnimator mAnimator;
    @NonNull protected final QuotaManager mQuotaManager;
    @NonNull protected final QuotaReleasingAnimatorListener mListener;
    @NonNull protected final Handler mUiHandler;
    private final long mStartDelay;
    protected Runnable mAcquireQuotaAndAnimateRunnable = this::acquireQuotaAndAnimate;
    @NonNull protected final TypeEvaluator<?> mEvaluator;
    @Nullable protected Object mLastAnimatedValue;

    @Nullable private Object mStartValue = null; // To cache the start value
    @Nullable private Object mEndValue = null; // To cache the end value

    interface UpdateCallback {
        void onUpdate(@NonNull Object animatedValue);
    }

    /**
     * If an evaluator other than a float or int type shall be used when calculating the animated
     * values of this animation, use this constructor to set the preferred type evaluator.
     */
    QuotaAwareAnimator(
            @NonNull QuotaManager quotaManager,
            @NonNull AnimationSpec spec,
            @NonNull TypeEvaluator<?> evaluator) {
        this(quotaManager, spec, evaluator, false);
    }

    protected QuotaAwareAnimator(
            @NonNull QuotaManager quotaManager,
            @NonNull AnimationSpec spec,
            @NonNull TypeEvaluator<?> evaluator,
            boolean alwaysPauseWhenRepeatForward) {
        mQuotaManager = quotaManager;
        mAnimator = new ValueAnimator();
        mUiHandler = new Handler(Looper.getMainLooper());
        applyAnimationSpecToAnimator(mAnimator, spec);

        // The start delay would be handled outside ValueAnimator, to make sure that the quota was
        // not consumed during the delay.
        mStartDelay = mAnimator.getStartDelay();
        mAnimator.setStartDelay(0);

        RepeatDelays repeatDelays = getRepeatDelays(spec);
        mListener =
                new QuotaReleasingAnimatorListener(
                        quotaManager,
                        mAnimator.getRepeatMode(),
                        repeatDelays.mForwardRepeatDelay,
                        repeatDelays.mReverseRepeatDelay,
                        mAnimator::resume,
                        mUiHandler,
                        alwaysPauseWhenRepeatForward);
        mAnimator.addListener(mListener);

        mEvaluator = evaluator;
    }

    @NonNull
    @Override
    public TypeEvaluator<?> getTypeEvaluator() {
        return mEvaluator;
    }

    /**
     * Adds a listener that is sent update events through the life of the animation. This method is
     * called on every frame of the animation after the values of the animation have been
     * calculated.
     */
    void addUpdateCallback(@NonNull UpdateCallback updateCallback) {
        mAnimator.addUpdateListener(
                animation -> {
                    mLastAnimatedValue = animation.getAnimatedValue();
                    updateCallback.onUpdate(mLastAnimatedValue);
                });
    }

    /**
     * Sets float values that will be animated between.
     *
     * @param values A set of values that the animation will animate between over time.
     */
    @Override
    public void setFloatValues(@NonNull float... values) {
        setFloatValues(mAnimator, mEvaluator, values);
        mStartValue = values[0];
        mEndValue = values[values.length - 1];
    }

    protected static void setFloatValues(
            ValueAnimator animator, @NonNull TypeEvaluator<?> evaluator, float... values) {
        if (!(evaluator instanceof FloatEvaluator)) {
            throw new IllegalArgumentException("FloatEvaluator is needed for setting float values");
        }
        animator.cancel();
        // ValueAnimator#setEvaluator only valid after values are set, and only need to set once.
        boolean needToSetEvaluator = animator.getValues() == null;
        animator.setFloatValues(values);
        if (needToSetEvaluator) {
            animator.setEvaluator(evaluator);
        }
    }

    /**
     * Sets integer values that will be animated between.
     *
     * @param values A set of values that the animation will animate between over time.
     */
    @Override
    public void setIntValues(@NonNull int... values) {
        setIntValues(mAnimator, mEvaluator, values);
        mStartValue = values[0];
        mEndValue = values[values.length - 1];
    }

    /**
     * Gets the start value of the animation.
     *
     * @return The start value of the animation or null if value wasn't set.
     */
    @Override
    @Nullable
    public Object getStartValue() {
        return mStartValue;
    }

    /**
     * Gets the end value of the animation.
     *
     * @return The end value of the animation.
     */
    @Override
    @Nullable
    public Object getEndValue() {
        return mEndValue;
    }

    protected static void setIntValues(
            ValueAnimator animator, @NonNull TypeEvaluator<?> evaluator, int... values) {
        animator.cancel();
        if (!(evaluator instanceof IntEvaluator) && !(evaluator instanceof ArgbEvaluator)) {
            throw new IllegalArgumentException(
                    "IntEvaluator or ArgbEvaluator is needed for setting int values");
        }
        // ValueAnimator#setEvaluator only valid after values are set, and only need to set once.
        boolean needToSetEvaluator = animator.getValues() == null;
        animator.setIntValues(values);
        if (needToSetEvaluator) {
            animator.setEvaluator(evaluator);
        }
    }

    /**
     * Tries to start animation. This method first handles the start delay if any, then checks the
     * quota to start tha animation or skip and jump to the end directly.
     */
    @UiThread
    void tryStartAnimation() {
        if (isRunning()) {
            return;
        }

        if (mStartDelay > 0) {
            // Do nothing if we already has pending call to acquireQuotaAndAnimate
            if (!HandlerCompat.hasCallbacks(mUiHandler, mAcquireQuotaAndAnimateRunnable)) {
                mUiHandler.postDelayed(mAcquireQuotaAndAnimateRunnable, mStartDelay);
            }
        } else {
            acquireQuotaAndAnimate();
        }
    }

    protected void acquireQuotaAndAnimate() {
        // Only valid after setFloatValues/setIntValues has been called
        if (mAnimator.getValues() == null) {
            return;
        }

        if (mQuotaManager.tryAcquireQuota(1)) {
            mListener.mIsUsingQuota.set(true);
            mAnimator.start();
        } else {
            mListener.mIsUsingQuota.set(false);
            // No need to jump to an end of animation if it can't be played when they are infinite.
            if (!isInfiniteAnimator()) {
                mAnimator.end();
            }
        }
    }

    /**
     * Tries to start/resume infinite animation. This method will call start/resume on animation,
     * but when animation is due to start (i.e. after the given delay), listener will check the
     * quota and allow/disallow animation to be played.
     */
    @UiThread
    void tryStartOrResumeInfiniteAnimation() {
        // Early out for finite animation, already running animation or no valid values before any
        // setFloatValues or setIntValues call
        if (!isInfiniteAnimator() || mAnimator.getValues() == null) {
            return;
        }

        if (isPaused()) {
            if (mQuotaManager.tryAcquireQuota(1)) {
                mListener.mIsUsingQuota.set(true);
                mAnimator.resume();
            }
        } else if (!isRunning()) {
            // Infinite animators created when this node was invisible have not started yet.
            tryStartAnimation();
        }
    }

    /**
     * Stops or pauses the animator, depending on it's state. If stopped, it will assign the end
     * value.
     */
    @UiThread
    void stopOrPauseAnimator() {
        if (isInfiniteAnimator()) {
            // remove pending call to start the animation if any
            mUiHandler.removeCallbacks(mAcquireQuotaAndAnimateRunnable);
            // remove resume callback if the animation is during the repeat delay
            mUiHandler.removeCallbacks(mListener.mResumeRepeatRunnable);
            mAnimator.pause();
            if (mListener.mIsUsingQuota.compareAndSet(true, false)) {
                mQuotaManager.releaseQuota(1);
            }
        } else {
            // This causes the animation to assign the end value of the property being animated.
            // Quota will be released at onAnimationEnd()
            stopAnimator();
        }
    }

    /** Stops the animator, which will cause it to assign the end value. */
    @UiThread
    void stopAnimator() {
        // remove pending call to start the animation if any
        mUiHandler.removeCallbacks(mAcquireQuotaAndAnimateRunnable);
        if (mAnimator.getValues() != null) {
            endAnimator();
        }
    }

    protected void endAnimator() {
        mAnimator.end();
    }

    @Override
    public void advanceToAnimationTime(long newTime) {
        long adjustedTime = newTime - mStartDelay;
        mAnimator.setCurrentPlayTime(adjustedTime);
    }

    @Nullable
    @Override
    public Object getCurrentValue() {
        return mLastAnimatedValue;
    }

    @Override
    public long getDurationMs() {
        return mAnimator.getDuration();
    }

    @Override
    public long getStartDelayMs() {
        return mStartDelay;
    }

    /** Returns whether the animator in this class has an infinite duration. */
    protected boolean isInfiniteAnimator() {
        return mAnimator.getTotalDuration() == Animator.DURATION_INFINITE;
    }

    /** Returns whether this node has a running animation. */
    boolean isRunning() {
        return mAnimator.isRunning();
    }

    /** Returns whether this node has a paused animation. */
    boolean isPaused() {
        return mAnimator.isPaused()
                // Not during repeat delay
                && !HandlerCompat.hasCallbacks(mUiHandler, mListener.mResumeRepeatRunnable);
    }

    /**
     * The listener used for animatable nodes to release quota when the animation is finished or
     * paused. Additionally, when {@link
     * android.animation.Animator.AnimatorListener#onAnimationStart(Animator)} is called, this
     * listener will check quota, and if there isn't any available, it will jump to an end of
     * animation.
     */
    protected static final class QuotaReleasingAnimatorListener extends AnimatorListenerAdapter {
        @NonNull private final QuotaManager mQuotaManager;

        // We need to keep track of whether the animation has started because pipeline has initiated
        // and it has received quota, or it is skipped by calling {@link android.animation
        // .Animator#end()} because no quota is available.
        @NonNull final AtomicBoolean mIsUsingQuota = new AtomicBoolean(false);

        private final int mRepeatMode;
        private final long mForwardRepeatDelay;
        private final long mReverseRepeatDelay;
        @NonNull private final Handler mHandler;
        @NonNull Runnable mResumeRepeatRunnable;
        private boolean mIsReverse;

        /**
         * Only intended to be true with {@link QuotaAwareAnimatorWithAux} to play main and aux
         * animators alternately, the pause and resume is still required to swap animators even
         * without repeat delay.
         */
        private final boolean mAlwaysPauseWhenRepeatForward;

        QuotaReleasingAnimatorListener(
                @NonNull QuotaManager quotaManager,
                int repeatMode,
                long forwardRepeatDelay,
                long reverseRepeatDelay,
                @NonNull Runnable resumeRepeatRunnable,
                @NonNull Handler uiHandler,
                boolean alwaysPauseWhenRepeatForward) {
            this.mQuotaManager = quotaManager;
            this.mRepeatMode = repeatMode;
            this.mForwardRepeatDelay = forwardRepeatDelay;
            this.mReverseRepeatDelay = reverseRepeatDelay;
            this.mResumeRepeatRunnable = resumeRepeatRunnable;
            this.mHandler = uiHandler;
            mIsReverse = false;
            mAlwaysPauseWhenRepeatForward = alwaysPauseWhenRepeatForward;
        }

        /**
         * Only intended to be called from QuotaAwareAnimatorWithAux To play main and aux animators
         * alternately, resume aux animator after pausing main animator, and resume main animator
         * after pause aux animator.
         */
        void setResumeRunnable(@NonNull Runnable runnable) {
            mResumeRepeatRunnable = runnable;
        }

        @Override
        @UiThread
        public void onAnimationStart(@NonNull Animator animation, boolean isReverse) {
            super.onAnimationStart(animation, isReverse);
            mIsReverse = isReverse;
        }

        @Override
        @UiThread
        public void onAnimationEnd(Animator animation) {
            if (mIsUsingQuota.compareAndSet(true, false)) {
                mQuotaManager.releaseQuota(1);
            }
            mHandler.removeCallbacks(mResumeRepeatRunnable);
        }

        @Override
        @UiThread
        public void onAnimationRepeat(Animator animation) {
            if (mRepeatMode == ValueAnimator.REVERSE) {
                mIsReverse = !mIsReverse;
            } else {
                mIsReverse = false;
            }

            if ((mAlwaysPauseWhenRepeatForward || mForwardRepeatDelay > 0) && !mIsReverse) {
                animation.pause();
                mHandler.postDelayed(mResumeRepeatRunnable, mForwardRepeatDelay);
            } else if (mReverseRepeatDelay > 0 && mIsReverse) {
                animation.pause();
                mHandler.postDelayed(mResumeRepeatRunnable, mReverseRepeatDelay);
            }
        }
    }
}
