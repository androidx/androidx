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

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.view.View;

/**
 * Uses framework Animators to provide ValueAnimatorCompat interface.
 * <p>
 * This is not a fully implemented API which is why it is not public.
 *
 * @hide
 */
class HoneycombMr1AnimatorCompatProvider implements AnimatorProvider {

    private TimeInterpolator mDefaultInterpolator;

    @Override
    public ValueAnimatorCompat emptyValueAnimator() {
        return new HoneycombValueAnimatorCompat(ValueAnimator.ofFloat(0f, 1f));
    }

    static class HoneycombValueAnimatorCompat implements ValueAnimatorCompat {

        final Animator mWrapped;

        public HoneycombValueAnimatorCompat(Animator wrapped) {
            mWrapped = wrapped;
        }

        @Override
        public void setTarget(View view) {
            mWrapped.setTarget(view);
        }

        @Override
        public void addListener(AnimatorListenerCompat listener) {
            mWrapped.addListener(new AnimatorListenerCompatWrapper(listener, this));
        }

        @Override
        public void setDuration(long duration) {
            mWrapped.setDuration(duration);
        }

        @Override
        public void start() {
            mWrapped.start();
        }

        @Override
        public void cancel() {
            mWrapped.cancel();
        }

        @Override
        public void addUpdateListener(final AnimatorUpdateListenerCompat animatorUpdateListener) {
            if (mWrapped instanceof ValueAnimator) {
                ((ValueAnimator) mWrapped).addUpdateListener(
                        new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                animatorUpdateListener
                                        .onAnimationUpdate(HoneycombValueAnimatorCompat.this);
                            }
                        });
            }
        }

        @Override
        public float getAnimatedFraction() {
            return ((ValueAnimator) mWrapped).getAnimatedFraction();
        }
    }

    static class AnimatorListenerCompatWrapper implements Animator.AnimatorListener {

        final AnimatorListenerCompat mWrapped;

        final ValueAnimatorCompat mValueAnimatorCompat;

        public AnimatorListenerCompatWrapper(
                AnimatorListenerCompat wrapped, ValueAnimatorCompat valueAnimatorCompat) {
            mWrapped = wrapped;
            mValueAnimatorCompat = valueAnimatorCompat;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            mWrapped.onAnimationStart(mValueAnimatorCompat);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mWrapped.onAnimationEnd(mValueAnimatorCompat);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mWrapped.onAnimationCancel(mValueAnimatorCompat);
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            mWrapped.onAnimationRepeat(mValueAnimatorCompat);
        }
    }

    @Override
    public void clearInterpolator(View view) {
        if (mDefaultInterpolator == null) {
            mDefaultInterpolator = new ValueAnimator().getInterpolator();
        }
        view.animate().setInterpolator(mDefaultInterpolator);
    }
}
