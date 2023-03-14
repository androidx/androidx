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

package androidx.wear.protolayout.renderer.dynamicdata;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.os.HandlerCompat;
import androidx.wear.protolayout.expression.pipeline.QuotaManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper for AnimationSet that is aware of quota. Animation will be played only if given quota
 * manager allows. Any existing listeners on wrapped {@link AnimationSet} will be replaced.
 */
final class QuotaAwareAnimationSet {
    @NonNull private final AnimationSet mAnimationSet;
    @NonNull private final QuotaManager mQuotaManager;

    @NonNull private final View mAssociatedView;
    @NonNull private final QuotaReleasingAnimationListener mListener;

    @Nullable private final Runnable mOnAnimationEnd;
    private final long mCommonDelay;
    @NonNull private final Handler mUiHandler;

    // Suppress initialization warnings here. These are only used inside of methods, and this class
    // is final, so these cannot actually be referenced while the class is under initialization.
    @SuppressWarnings("methodref.receiver.bound")
    @NonNull
    private final Runnable mTryAcquireQuotaAndStartAnimation =
            this::tryAcquireQuotaAndStartAnimation;

    QuotaAwareAnimationSet(
            @NonNull AnimationSet animation,
            @NonNull QuotaManager quotaManager,
            @NonNull View associatedView) {
        this(animation, quotaManager, associatedView, /* onAnimationEnd= */ null);
    }

    QuotaAwareAnimationSet(
            @NonNull AnimationSet animation,
            @NonNull QuotaManager quotaManager,
            @NonNull View associatedView,
            @Nullable Runnable onAnimationEnd) {
        this.mAnimationSet = animation;
        this.mQuotaManager = quotaManager;
        this.mAssociatedView = associatedView;
        this.mOnAnimationEnd = onAnimationEnd;
        this.mUiHandler = new Handler(Looper.getMainLooper());

        // AnimationSet contains multiple animation, of which each of them can have set start delay,
        // that is offset. To prevent consuming quota before animation is due to be played, we're
        // going to get the minimum starting offset among animations in the set and implement
        // delaying starting animation set for that period of time. This way, quota will be consumed
        // when the earliest animation in the set should be played. In order to preserve set delay
        // in animations, each animation in the set will have their delay updated relatively to the
        // minimum delay.

        // Getting minimum offset
        this.mCommonDelay =
                mAnimationSet.getAnimations().stream()
                        .mapToLong(Animation::getStartOffset)
                        .min()
                        .orElse(0L);

        // Updating children offsets.
        mAnimationSet
                .getAnimations()
                .forEach(anim -> anim.setStartOffset(anim.getStartOffset() - this.mCommonDelay));

        mListener =
                new QuotaReleasingAnimationListener(
                        mQuotaManager, animation.getAnimations().size(), onAnimationEnd);
        this.mAnimationSet.setAnimationListener(mListener);
    }

    /**
     * Tries to start animations in the given set. Animation will try to start after the delay it
     * has set.
     *
     * <p>The Runnables {@code beforeAnimationStart} and {@code onAnimationEnd} will still be run
     * even if animation could not start due to quota being unavailable.
     */
    @UiThread
    void tryStartAnimation(@NonNull Runnable beforeAnimationStart) {
        // Don't start new animation if there are already running ones.
        if (getRunningAnimationCount() > 0) {
            return;
        }

        beforeAnimationStart.run();
        // We are implementing start offset ourselves, because we don't want quota to be consumed
        // before animation is running.
        if (mCommonDelay > 0) {
            if (!HandlerCompat.hasCallbacks(mUiHandler, mTryAcquireQuotaAndStartAnimation)) {
                mUiHandler.postDelayed(mTryAcquireQuotaAndStartAnimation, mCommonDelay);
            }
        } else {
            tryAcquireQuotaAndStartAnimation();
        }
    }

    @UiThread
    private void tryAcquireQuotaAndStartAnimation() {
        if (mQuotaManager.tryAcquireQuota(mAnimationSet.getAnimations().size())) {
            mListener.mIsUsingQuota.set(true);
            mAssociatedView.startAnimation(mAnimationSet);
        } else if (mOnAnimationEnd != null) {
            mOnAnimationEnd.run();
        }
        // No need to jump to an end of animation, because if animation is not played, the changed
        // node will be replaced in its place, the same way as if it'd be when content transition is
        // not set.
    }

    /** Cancels all animation in this set and notifies the listener on the same thread. */
    @UiThread
    void cancelAnimations() {
        mAnimationSet.cancel();
        mListener.onAnimationEnd(mAnimationSet);
        mUiHandler.removeCallbacks(mTryAcquireQuotaAndStartAnimation);
    }

    /** Returns the number of currently running animations. */
    int getRunningAnimationCount() {
        return (mAnimationSet.hasStarted() && !mAnimationSet.hasEnded())
                ? mAnimationSet.getAnimations().size()
                : 0;
    }

    private static final class QuotaReleasingAnimationListener implements AnimationListener {

        @Nullable private final Runnable mOnAnimationEnd;
        @NonNull private final QuotaManager mQuotaManager;
        private final int mAnimationNum;
        @NonNull final AtomicBoolean mIsUsingQuota = new AtomicBoolean(false);

        QuotaReleasingAnimationListener(
                @NonNull QuotaManager mQuotaManager,
                int animationNum,
                @Nullable Runnable mOnAnimationEnd) {
            this.mOnAnimationEnd = mOnAnimationEnd;
            this.mQuotaManager = mQuotaManager;
            this.mAnimationNum = animationNum;
        }

        @Override
        @UiThread
        public void onAnimationStart(@NonNull Animation animation) {}

        @Override
        @UiThread
        public void onAnimationEnd(@NonNull Animation animation) {
            if (mIsUsingQuota.compareAndSet(true, false)) {
                mQuotaManager.releaseQuota(mAnimationNum);

                if (mOnAnimationEnd != null) {
                    mOnAnimationEnd.run();
                }
            }
        }

        @Override
        @UiThread
        public void onAnimationRepeat(@NonNull Animation animation) {}
    }
}
