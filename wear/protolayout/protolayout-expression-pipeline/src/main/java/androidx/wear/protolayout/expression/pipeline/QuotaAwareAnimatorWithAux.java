/*
 * Copyright 2023 The Android Open Source Project
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

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;

import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat;
import androidx.wear.protolayout.expression.pipeline.AnimationsHelper.RepeatDelays;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;

/**
 * This class handles the animation with custom reverse duration. To have different duration for
 * forward and reverse animations, two animators are played alternately as follows:
 *
 * <p>1. After the start delay, start both animators. 2. Main animator plays forward part of the
 * animation, aux animator waits for its extra start delay of (forward duration + reverse delay). 3.
 * Main animator pauses before repeat and calls aux animator to resume after reverse delay. 4. Aux
 * animator plays reverse part of animation; main animator is paused. 5. Aux animator pauses before
 * repeat and calls main animator to resume after forward delay. 6. Main animator plays forward part
 * of the animation; aux animator is paused. 7. .....
 */
class QuotaAwareAnimatorWithAux extends QuotaAwareAnimator {

    @NonNull private final QuotaReleasingAnimatorListener mAuxListener;
    @NonNull private final ValueAnimator mAuxAnimator;
    private boolean mSuppressForwardUpdate = false;
    private boolean mSuppressReverseUpdate = false;
    private final boolean mEndsWithForward;

    QuotaAwareAnimatorWithAux(
            @NonNull QuotaManager quotaManager,
            @NonNull AnimationSpec spec,
            @NonNull AnimationSpec auxSpec,
            @NonNull TypeEvaluator<?> evaluator) {
        super(quotaManager, spec, evaluator, /* alwaysPauseWhenRepeatForward= */ true);

        mAuxAnimator = new ValueAnimator();
        applyAnimationSpecToAnimator(mAuxAnimator, auxSpec);
        RepeatDelays repeatDelays = getRepeatDelays(auxSpec);
        mAuxListener =
                new QuotaReleasingAnimatorListener(
                        quotaManager,
                        mAuxAnimator.getRepeatMode(),
                        repeatDelays.mForwardRepeatDelay,
                        repeatDelays.mReverseRepeatDelay,
                        mAnimator::resume,
                        mUiHandler,
                        /* alwaysPauseWhenRepeatForward= */ true);
        mAuxAnimator.addListener(mAuxListener);

        mAcquireQuotaAndAnimateRunnable = this::acquireQuotaAndAnimate;
        mListener.setResumeRunnable(mAuxAnimator::resume);
        mEndsWithForward = mAnimator.getRepeatCount() > mAuxAnimator.getRepeatCount();
    }

    @Override
    void addUpdateCallback(@NonNull UpdateCallback updateCallback) {
        // 1. Do not update the animated value when pausing for swap, or there is a jumping frame.
        // 2. Suppress the update temporarily to avoid assigning one of the end values depending
        // on the repeating count.
        mAnimator.addUpdateListener(
                animation -> {
                    if (!mSuppressForwardUpdate && !mAnimator.isPaused()) {
                        mLastAnimatedValue = animation.getAnimatedValue();
                        updateCallback.onUpdate(mLastAnimatedValue);
                    }
                });

        mAuxAnimator.addUpdateListener(
                animation -> {
                    if (!mSuppressReverseUpdate && !mAuxAnimator.isPaused()) {
                        mLastAnimatedValue = animation.getAnimatedValue();
                        updateCallback.onUpdate(mLastAnimatedValue);
                    }
                });
    }

    @Override
    public void setFloatValues(@NonNull float... values) {
        super.setFloatValues(values);

        // Create a copy of the values array before reversing it
        float[] reversedValues = values.clone();

        // reverse the copied array
        float temp;
        for (int i = 0; i < reversedValues.length / 2; i++) {
            temp = reversedValues[i];
            reversedValues[i] = reversedValues[reversedValues.length - 1 - i];
            reversedValues[reversedValues.length - 1 - i] = temp;
        }
        setFloatValues(mAuxAnimator, mEvaluator, reversedValues);
    }

    @Override
    public void setIntValues(@NonNull int... values) {
        super.setIntValues(values);

        // Create a copy of the values array before reversing it
        int[] reversedValues = values.clone();

        // reverse the copied array
        int temp;
        for (int i = 0; i < reversedValues.length / 2; i++) {
            temp = reversedValues[i];
            reversedValues[i] = reversedValues[reversedValues.length - 1 - i];
            reversedValues[reversedValues.length - 1 - i] = temp;
        }
        setIntValues(mAuxAnimator, mEvaluator, reversedValues);
    }

    @Override
    protected void acquireQuotaAndAnimate() {
        super.acquireQuotaAndAnimate();
        if (mAnimator.isStarted()) {
            mAuxAnimator.start();
        }
    }

    @Override
    void tryStartOrResumeInfiniteAnimation() {
        // Early out for finite animation, already running animation or no valid values before any
        // setFloatValues or setIntValues call
        if (!isInfiniteAnimator() || mAnimator.getValues() == null) {
            return;
        }

        if (isPaused()) {
            if (mQuotaManager.tryAcquireQuota(1)) {
                mListener.mIsUsingQuota.set(true);
                // to simplify the synchronization after pause, always resume the main animator
                // and set the aux animator to beginning to be ready to resume before repeating
                // the main one.
                mAnimator.resume();
                mAuxAnimator.setCurrentFraction(0);
            }
        } else if (!isRunning()) {
            // Infinite animators created when this node was invisible have not started yet.
            tryStartAnimation();
        }
    }

    @Override
    void stopOrPauseAnimator() {
        super.stopOrPauseAnimator();
        if (isInfiniteAnimator()) {
            mAuxAnimator.pause();
            mUiHandler.removeCallbacks(mAuxListener.mResumeRepeatRunnable);
        }
    }

    @Override
    protected void endAnimator() {
        mSuppressForwardUpdate = !mEndsWithForward;
        mSuppressReverseUpdate = mEndsWithForward;
        mAnimator.end();
        mAuxAnimator.end();
        mSuppressForwardUpdate = false;
        mSuppressReverseUpdate = false;
    }

    /** Returns whether this node has a running animation. */
    @Override
    protected boolean isRunning() {
        return super.isRunning() || mAuxAnimator.isRunning();
    }

    @Override
    protected boolean isPaused() {
        return super.isPaused()
                && mAuxAnimator.isPaused()
                && !HandlerCompat.hasCallbacks(mUiHandler, mAuxListener.mResumeRepeatRunnable);
    }

    @Override
    public void advanceToAnimationTime(long newTime) {
        if (newTime < mAuxAnimator.getStartDelay()) {
            super.advanceToAnimationTime(newTime);
        } else {
            // Adjust time for the auxiliary animator
            long adjustedTime = newTime - mAuxAnimator.getStartDelay();
            mAuxAnimator.setCurrentPlayTime(adjustedTime);
        }
    }

    @Override
    public long getDurationMs() {
        return mAnimator.getDuration() + mAuxAnimator.getDuration();
    }
}
