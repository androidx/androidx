/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.animation;

import android.support.annotation.FloatRange;

/**
 * Fling animation is an animation that continues an initial momentum (most often from gesture
 * velocity) and gradually slows down. The fling animation will come to a stop when the velocity of
 * the animation is below the velocity threshold defined with {@link #setVelocityThreshold(float)},
 * or when the value of the animation has gone beyond the min or max value defined via
 * {@link DynamicAnimation#setMinValue(float)} or {@link DynamicAnimation#setMaxValue(float)}.
 * It is recommended to restrict the fling animation with min and/or max value, such that the
 * animation can end when it goes beyond screen bounds, thus preserving CPU cycles and resources.
 */
public final class FlingAnimation extends DynamicAnimation<FlingAnimation> {

    private final DragForce mFlingForce;

    /**
     * <p>This creates a FlingAnimation that animates a float value that is not associated with an
     * object. During the animation, the value will be updated via
     * {@link FloatPropertyCompat#setValue(Object, float)} each frame. The caller can obtain the
     * up-to-date animation value via {@link FloatPropertyCompat#getValue(Object)}. These setter
     * and getter will be called with a <code>null</code> object.
     *
     * <p><strong>Note:</strong> changing the property value via
     * {@link FloatPropertyCompat#setValue(Object, float)} outside of the animation during an
     * animation run will not have any effect on the on-going animation.
     *
     * @param property the property to be animated
     * @param <K> the class on which the Property is declared
     */
    public <K> FlingAnimation(FloatPropertyCompat<K> property) {
        super(null, property);
        mFlingForce = new DragForce();
    }

    /**
     * This creates a FlingAnimation that animates the property of the given object.
     *
     * @param object the Object whose property will be animated
     * @param property the property to be animated
     * @param <K> the class on which the property is declared
     */
    public <K> FlingAnimation(K object, FloatPropertyCompat<K> property) {
        super(object, property);
        mFlingForce = new DragForce();
    }

    /**
     * Sets the friction for the fling animation. The greater the friction is, the sooner the
     * animation will slow down. When not set, the friction defaults to 1.
     *
     * @param friction the friction used in the animation
     * @return the animation whose friction will be scaled
     * @throws IllegalArgumentException if the input friction is not positive
     */
    public FlingAnimation setFriction(
            @FloatRange(from = 0.0, fromInclusive = false) float friction) {
        if (friction <= 0) {
            throw new IllegalArgumentException("Friction must be positive");
        }
        mFlingForce.setFrictionScalar(friction);
        return this;
    }

    /**
     * Returns the friction being set on the animation via {@link #setFriction(float)}. If the
     * friction has not been set, the default friction of 1 will be returned.
     *
     * @return friction being used in the animation
     */
    public float getFriction() {
        return mFlingForce.getFrictionScalar();
    }

    /**
     * Sets the min value of the animation. When a fling animation reaches the min value, the
     * animation will end immediately. Animations will not animate beyond the min value.
     *
     * @param minValue minimum value of the property to be animated
     * @return the Animation whose min value is being set
     */
    @Override
    public FlingAnimation setMinValue(float minValue) {
        super.setMinValue(minValue);
        return this;
    }

    /**
     * Sets the max value of the animation. When a fling animation reaches the max value, the
     * animation will end immediately. Animations will not animate beyond the max value.
     *
     * @param maxValue maximum value of the property to be animated
     * @return the Animation whose max value is being set
     */
    @Override
    public FlingAnimation setMaxValue(float maxValue) {
        super.setMaxValue(maxValue);
        return this;
    }

    /**
     * Sets the velocity threshold, which determines when the velocity of the fling animation is
     * slow enough to be considered finished. By default, this value is tuned based on different
     * properties to be animated. For animations that don't animate a particular property, the
     * default velocity threshold is the same as if the animation property was pixel based.
     *
     * @param threshold the velocity threshold to consider equilibrium, unit: pixel/second
     * @return the animation that the velocity threshold is being set on
     * @throws IllegalArgumentException if the given threshold is not positive
     */
    public FlingAnimation setVelocityThreshold(
            @FloatRange(from = 0.0, fromInclusive = false) float threshold) {
        mFlingForce.setVelocityThreshold(threshold);
        return this;
    }

    /**
     * Returns the velocity threshold set via {@link #setVelocityThreshold(float)}.
     * {@see #setVelocityThreshold(float)}
     *
     * @return velocity threshold in unit: pixel/second
     */
    public float getVelocityThreshold() {
        return mFlingForce.getVelocityThreshold();
    }

    @Override
    boolean updateValueAndVelocity(long deltaT) {

        MassState state = mFlingForce.updateValueAndVelocity(mValue, mVelocity, deltaT);
        mValue = state.mValue;
        mVelocity = state.mVelocity;

        // When the animation hits the max/min value, consider animation done.
        if (mValue < mMinValue) {
            mValue = mMinValue;
            return true;
        }
        if (mValue > mMaxValue) {
            mValue = mMaxValue;
            return true;
        }

        if (isAtEquilibrium(mValue, mVelocity)) {
            return true;
        }
        return false;
    }

    @Override
    float getAcceleration(float value, float velocity) {
        return mFlingForce.getAcceleration(value, velocity);
    }

    @Override
    boolean isAtEquilibrium(float value, float velocity) {
        return value >= mMaxValue
                || value <= mMinValue
                || mFlingForce.isAtEquilibrium(value, velocity);
    }

    @Override
    void setDefaultThreshold(float threshold) {
        mFlingForce.setDefaultThreshold(threshold);
    }

    private static final class DragForce implements Force {

        private static final float DEFAULT_FRICTION = -4.2f;

        // This multiplier is used to calculate the velocity threshold given a certain value
        // threshold. The idea is that if it takes >= 1 frame to move the value threshold amount,
        // then the velocity is a reasonable threshold.
        private static final float VELOCITY_THRESHOLD_MULTIPLIER = 1000f / 16f;
        private float mFriction = DEFAULT_FRICTION;
        private float mVelocityThreshold = VALUE_THRESHOLD_IN_PIXEL * VELOCITY_THRESHOLD_MULTIPLIER;
        private boolean mCustomThresholdSet = false;

        // Internal state to hold a value/velocity pair.
        private final DynamicAnimation.MassState mMassState = new DynamicAnimation.MassState();

        void setFrictionScalar(float frictionScalar) {
            mFriction = frictionScalar * DEFAULT_FRICTION;
        }

        float getFrictionScalar() {
            return mFriction / DEFAULT_FRICTION;
        }

        void setVelocityThreshold(float threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Velocity threshold must be positive.");
            }
            mVelocityThreshold = threshold;
            mCustomThresholdSet = true;
        }

        float getVelocityThreshold() {
            return mVelocityThreshold;
        }

        MassState updateValueAndVelocity(float value, float velocity, long deltaT) {
            mMassState.mVelocity = (float) (velocity * Math.exp((deltaT / 1000f) * mFriction));
            mMassState.mValue = (float) (value - velocity / mFriction
                    + velocity / mFriction * Math.exp(mFriction * deltaT / 1000f));
            if (isAtEquilibrium(mMassState.mValue, mMassState.mVelocity)) {
                mMassState.mVelocity = 0f;
            }
            return mMassState;
        }

        @Override
        public float getAcceleration(float position, float velocity) {
            return velocity * mFriction;
        }

        @Override
        public boolean isAtEquilibrium(float value, float velocity) {
            return Math.abs(velocity) < mVelocityThreshold;
        }

        void setDefaultThreshold(float defaultThreshold) {
            if (!mCustomThresholdSet) {
                mVelocityThreshold = defaultThreshold * VELOCITY_THRESHOLD_MULTIPLIER;
            }
        }
    }

}
