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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.HandlerCompat;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for Animator that is aware of quota. Animator's animations will be played only if given
 * quota manager allows. If not, non infinite animation will jump to an end. Any existing listeners
 * on wrapped {@link Animator} will be replaced.
 */
class QuotaAwareAnimator {
    @NonNull private final ValueAnimator mAnimator;
    @NonNull private final QuotaManager mQuotaManager;
    @NonNull private final QuotaReleasingAnimatorListener mListener;
    @NonNull private final Handler mUiHandler;
    private long mStartDelay = 0;
    private final Runnable mAcquireQuotaAndAnimateRunnable = this::acquireQuotaAndAnimate;
    @Nullable private final TypeEvaluator<?> mEvaluator;

    interface UpdateCallback {
        abstract void onUpdate(@NonNull Object animatedValue);
    }

    QuotaAwareAnimator(@NonNull QuotaManager quotaManager, @NonNull AnimationSpec spec) {
        this(quotaManager, spec, null);
    }

    /**
     * If an evaluator other than a float or int type shall be used when calculating the animated
     * values of this animation, use this constructor to set the preferred type evaluator.
     */
    QuotaAwareAnimator(
            @NonNull QuotaManager quotaManager,
            @NonNull AnimationSpec spec,
            @Nullable TypeEvaluator<?> evaluator) {
        mQuotaManager = quotaManager;
        mAnimator = new ValueAnimator();
        mUiHandler = new Handler(Looper.getMainLooper());
        mListener = new QuotaReleasingAnimatorListener(quotaManager);
        mAnimator.addListener(mListener);
        mAnimator.addPauseListener(mListener);
        applyAnimationSpecToAnimator(mAnimator, spec);

        // The start delay would be handled outside ValueAnimator, to make sure that the quota was
        // not consumed during the delay.
        mStartDelay = mAnimator.getStartDelay();
        mAnimator.setStartDelay(0);
        mEvaluator = evaluator;
    }
    /**
     * Adds a listener that is sent update events through the life of the animation. This method is
     * called on every frame of the animation after the values of the animation have been
     * calculated.
     */
    void addUpdateCallback(@NonNull UpdateCallback updateCallback) {
        mAnimator.addUpdateListener(
                animation -> updateCallback.onUpdate(animation.getAnimatedValue()));
    }

    /**
     * Sets float values that will be animated between.
     *
     * @param values A set of values that the animation will animate between over time.
     */
    void setFloatValues(float... values) {
        mAnimator.cancel();
        // ValueAnimator#setEvaluator only valid after values are set, and only need to set once.
        boolean needToSetEvaluator = mAnimator.getValues() == null && mEvaluator != null;
        mAnimator.setFloatValues(values);
        if (needToSetEvaluator) {
            mAnimator.setEvaluator(mEvaluator);
        }
    }

    /**
     * Sets integer values that will be animated between.
     *
     * @param values A set of values that the animation will animate between over time.
     */
    void setIntValues(int... values) {
        mAnimator.cancel();

        // ValueAnimator#setEvaluator only valid after values are set, and only need to set once.
        boolean needToSetEvaluator = mAnimator.getValues() == null && mEvaluator != null;
        mAnimator.setIntValues(values);
        if (needToSetEvaluator) {
            mAnimator.setEvaluator(mEvaluator);
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

    private void acquireQuotaAndAnimate() {
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
        if (!isInfiniteAnimator() || isRunning() || mAnimator.getValues() == null) {
            return;
        }

        if (mAnimator.isPaused()) {
            if (mQuotaManager.tryAcquireQuota(1)) {
                mListener.mIsUsingQuota.set(true);
                mAnimator.resume();
            }
        } else {
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
            mAnimator.end();
        }
    }

    /** Returns whether the animator in this class has an infinite duration. */
    protected boolean isInfiniteAnimator() {
        return mAnimator.getTotalDuration() == Animator.DURATION_INFINITE;
    }

    /** Returns whether this node has a running animation. */
    boolean isRunning() {
        return mAnimator.isRunning();
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    boolean isPaused() {
        return mAnimator.isPaused();
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
        // and it has received quota, or it is skipped by calling {@link
        // android.animation.Animator#end()} because no quota is available.
        @NonNull final AtomicBoolean mIsUsingQuota = new AtomicBoolean(false);

        QuotaReleasingAnimatorListener(@NonNull QuotaManager quotaManager) {
            this.mQuotaManager = quotaManager;
        }

        @Override
        @UiThread
        public void onAnimationEnd(Animator animation) {
            if (mIsUsingQuota.compareAndSet(true, false)) {
                mQuotaManager.releaseQuota(1);
            }
        }
    }
}
