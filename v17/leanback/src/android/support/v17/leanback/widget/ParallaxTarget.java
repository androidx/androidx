/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.LinearInterpolator;

import java.util.List;

/**
 * ParallaxTarget is reponsible for updating the target through the {@link #update(float)} method.
 * {@link ParallaxEffect} transforms the values of {@link ParallaxSource}, which represents the
 * current state of UI, into a float value between 0 and 1. That float value is passed into
 * {@link #update(float)} method.
 * @hide
 */
public abstract class ParallaxTarget {

    /**
     * Implementation class is supposed to update target with the provided fraction
     * (between 0 and 1). The fraction represents percentage of completed change (e.g. scroll) on
     * target.
     *
     * @param fraction Fraction between 0 to 1.
     */
    public abstract void update(float fraction);

    /**
     * Returns the current fraction (between 0 and 1). The fraction represents percentage of
     * completed change (e.g. scroll) on target.
     *
     * @return Current fraction value.
     */
    public abstract float getFraction();

    /**
     * PropertyValuesHolderTarget is an implementation of {@link ParallaxTarget} that uses
     * {@link PropertyValuesHolder} to update the target object.
     */
    public static final class PropertyValuesHolderTarget extends ParallaxTarget {

        /**
         * We simulate a parallax effect on target object using an ObjectAnimator. PSEUDO_DURATION
         * is used on the ObjectAnimator.
         */
        private static final long PSEUDO_DURATION = 1000000;

        private final ObjectAnimator mAnimator;
        private float mFraction;

        public PropertyValuesHolderTarget(Object targetObject, PropertyValuesHolder values) {
            mAnimator = ObjectAnimator.ofPropertyValuesHolder(targetObject, values);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.setDuration(PSEUDO_DURATION);
        }

        @Override
        public void update(float fraction) {
            mFraction = fraction;
            mAnimator.setCurrentPlayTime((long) (PSEUDO_DURATION * fraction));
        }

        @Override
        public float getFraction() {
            return mFraction;
        }
    }
}
