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

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

/** Data animatable source node within a dynamic data pipeline. */
abstract class AnimatableNode {
    private boolean mIsVisible = false;
    @NonNull final QuotaAwareAnimator mQuotaAwareAnimator;

    protected AnimatableNode(@NonNull QuotaManager quotaManager) {
        mQuotaAwareAnimator = new QuotaAwareAnimator(null, quotaManager);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    AnimatableNode(@NonNull QuotaAwareAnimator quotaAwareAnimator) {
        mQuotaAwareAnimator = quotaAwareAnimator;
    }

    /**
     * Starts the animator (if present) if the node is visible and there is a quota, otherwise, skip
     * it.
     */
    @UiThread
    protected void startOrSkipAnimator() {
        if (mIsVisible) {
            mQuotaAwareAnimator.tryStartAnimation();
        } else {
            stopOrPauseAnimator();
        }
    }

    /**
     * Sets the node's visibility and resumes or stops the corresponding animator (if present).
     *
     * <p>If it's becoming visible, paused animations are resumed, other infinite animations that
     * haven't started yet will start.
     *
     * <p>If it's becoming invisible, all animations should skip to end or pause.
     */
    @UiThread
    void setVisibility(boolean visible) {
        if (mIsVisible == visible) {
            return;
        }
        mIsVisible = visible;
        if (mIsVisible) {
            startOrResumeAnimator();
        } else if (mQuotaAwareAnimator.hasRunningOrStartedAnimation()) {
            stopOrPauseAnimator();
        }
    }

    /**
     * Starts or resumes the animator if there is a quota, depending on whether the animation was
     * paused.
     */
    private void startOrResumeAnimator() {
        mQuotaAwareAnimator.tryStartOrResumeInfiniteAnimation();
    }

    /** Returns whether this node has a running animation. */
    boolean hasRunningOrStartedAnimation() {
        return mQuotaAwareAnimator.hasRunningOrStartedAnimation();
    }

    /** Returns whether the animator in this node has an infinite duration. */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    protected boolean isInfiniteAnimator() {
        return mQuotaAwareAnimator.isInfiniteAnimator();
    }

    /**
     * Pauses the animator in this node if it has infinite duration, stop it otherwise. Note that
     * this method has no effect on infinite animators that are not running since Animator#pause()
     * will be a no-op in that case.
     */
    private void stopOrPauseAnimator() {
        mQuotaAwareAnimator.stopOrPauseAnimator();
    }
}
