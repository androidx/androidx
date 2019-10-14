/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.animation;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This class holds a time/value pair for an animation. The Keyframe class is used
 * by {@link ValueAnimator} to define the values that the animation target will have over the course
 * of the animation. As the time proceeds from one keyframe to the other, the value of the
 * target object will animate between the value at the previous keyframe and the value at the
 * next keyframe. Each keyframe also holds an optional {@link Interpolator}
 * object, which defines the time interpolation over the intervalue preceding the keyframe.
 *
 * <p>The Keyframe class itself is abstract. The type-specific factory methods will return
 * a subclass of Keyframe specific to the type of value being stored. This is done to improve
 * performance when dealing with the most common cases (e.g., <code>float</code> and
 * <code>int</code> values). Other types will fall into a more general Keyframe class that
 * treats its values as Objects. Unless your animation requires dealing with a custom type
 * or a data structure that needs to be animated directly (and evaluated using an implementation
 * of {@link TypeEvaluator}), you should stick to using float and int as animations using those
 * types have lower runtime overhead than other types.</p>
 *
 * @param <T> type of the data value stored in a key frame.
 */
public abstract class Keyframe<T> implements Cloneable {
    /**
     * Flag to indicate whether this keyframe has a valid value. This flag is used when an
     * animation first starts, to populate placeholder keyframes with real values derived
     * from the target object.
     */
    boolean mHasValue;

    /**
     * Flag to indicate whether the value in the keyframe was read from the target object or not.
     * If so, its value will be recalculated if target changes.
     */
    boolean mValueWasSetOnStart;


    /**
     * The time at which mValue will hold true.
     */
    float mFraction;

    /**
     * The type of the value in this Keyframe. This type is determined at construction time,
     * based on the type of the <code>value</code> object passed into the constructor.
     */
    Class<?> mValueType;

    /**
     * The optional interpolator for the interval preceding this keyframe. A null interpolator
     * (the default) results in linear interpolation over the interval.
     */
    private Interpolator mInterpolator = null;

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     * @param value The value that the object will animate to as the animation time approaches
     * the time in this keyframe, and the the value animated from as the time passes the time in
     * this keyframe.
     */
    @NonNull
    public static IntKeyframe ofInt(@FloatRange(from = 0, to = 1) float fraction, int value) {
        return new IntKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     */
    @NonNull
    public static IntKeyframe ofInt(@FloatRange(from = 0, to = 1) float fraction) {
        return new IntKeyframe(fraction);
    }

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     * @param value The value that the object will animate to as the animation time approaches
     * the time in this keyframe, and the the value animated from as the time passes the time in
     * this keyframe.
     */
    @NonNull
    public static FloatKeyframe ofFloat(@FloatRange(from = 0, to = 1) float fraction, float value) {
        return new FloatKeyframe(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     */
    @NonNull
    public static FloatKeyframe ofFloat(@FloatRange(from = 0, to = 1) float fraction) {
        return new FloatKeyframe(fraction);
    }

    /**
     * Constructs a Keyframe object with the given time and value. The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     * @param value The value that the object will animate to as the animation time approaches
     * the time in this keyframe, and the the value animated from as the time passes the time in
     * this keyframe.
     */
    @NonNull
    public static <T> ObjectKeyframe<T> ofObject(@FloatRange(from = 0, to = 1) float fraction,
                @Nullable T value) {
        return new ObjectKeyframe<T>(fraction, value);
    }

    /**
     * Constructs a Keyframe object with the given time. The value at this time will be derived
     * from the target object when the animation first starts (note that this implies that keyframes
     * with no initial value must be used as part of an {@link ObjectAnimator}).
     * The time defines the
     * time, as a proportion of an overall animation's duration, at which the value will hold true
     * for the animation. The value for the animation between keyframes will be calculated as
     * an interpolation between the values at those keyframes.
     *
     * @param fraction The time, expressed as a value between 0 and 1, representing the fraction
     * of time elapsed of the overall animation duration.
     */
    @NonNull
    public static <T> ObjectKeyframe<T> ofObject(@FloatRange(from = 0, to = 1) float fraction) {
        return new ObjectKeyframe<>(fraction, null);
    }

    /**
     * Indicates whether this keyframe has a valid value. This method is called internally when
     * an {@link ObjectAnimator} first starts; keyframes without values are assigned values at
     * that time by deriving the value for the property from the target object.
     *
     * @return boolean Whether this object has a value assigned.
     */
    public boolean hasValue() {
        return mHasValue;
    }

    /**
     * If the Keyframe's value was acquired from the target object, this flag should be set so that,
     * if target changes, value will be reset.
     *
     * @return boolean Whether this Keyframe's value was retieved from the target object or not.
     */
    boolean valueWasSetOnStart() {
        return mValueWasSetOnStart;
    }

    void setValueWasSetOnStart(boolean valueWasSetOnStart) {
        mValueWasSetOnStart = valueWasSetOnStart;
    }

    /**
     * Gets the value for this Keyframe.
     *
     * @return The value for this Keyframe.
     */
    //TODO: Consider removing hasValue() and making keyframe always contain nonNull value, and
    // using a different signal for when the animation should read the property value as its start
    // value.
    @Nullable
    public abstract T getValue();

    /**
     * Sets the value for this Keyframe.
     *
     * @param value value for this Keyframe.
     */
    public abstract void setValue(@Nullable T value);

    /**
     * Gets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @return The time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public @FloatRange(from = 0, to = 1) float getFraction() {
        return mFraction;
    }

    /**
     * Sets the time for this keyframe, as a fraction of the overall animation duration.
     *
     * @param fraction time associated with this keyframe, as a fraction of the overall animation
     * duration. This should be a value between 0 and 1.
     */
    public void setFraction(@FloatRange(from = 0, to = 1) float fraction) {
        mFraction = fraction;
    }

    /**
     * Gets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     *
     * @return The optional interpolator for this Keyframe.
     */
    @Nullable
    public Interpolator getInterpolator() {
        return mInterpolator;
    }

    /**
     * Sets the optional interpolator for this Keyframe. A value of <code>null</code> indicates
     * that there is no interpolation, which is the same as linear interpolation.
     */
    public void setInterpolator(@Nullable Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    /**
     * Gets the type of keyframe. This information is used by ValueAnimator to determine the type of
     * {@link TypeEvaluator} to use when calculating values between keyframes. The type is based
     * on the type of Keyframe created.
     *
     * @return The type of the value stored in the Keyframe.
     */
    @NonNull
    public Class getType() {
        return mValueType;
    }

    @Override
    public abstract Keyframe clone();

    /**
     * This internal subclass is used for all types which are not int or float.
     */
    static class ObjectKeyframe<T> extends Keyframe<T> {

        /**
         * The value of the animation at the time mFraction.
         */
        T mValue;

        ObjectKeyframe(float fraction, T value) {
            mFraction = fraction;
            mValue = value;
            mHasValue = (value != null);
            mValueType = mHasValue ? value.getClass() : Object.class;
        }

        @Override
        public T getValue() {
            return mValue;
        }

        @Override
        public void setValue(T value) {
            mValue = value;
            mHasValue = (value != null);
        }

        @Override
        public ObjectKeyframe<T> clone() {
            ObjectKeyframe<T> kfClone = new ObjectKeyframe<>(getFraction(),
                    hasValue() ? mValue : null);
            kfClone.mValueWasSetOnStart = mValueWasSetOnStart;
            kfClone.setInterpolator(getInterpolator());
            return kfClone;
        }
    }

    /**
     * Internal subclass used when the keyframe value is of type int.
     */
    static class IntKeyframe extends Keyframe<Integer> {

        /**
         * The value of the animation at the time mFraction.
         */
        int mValue;

        IntKeyframe(float fraction, int value) {
            mFraction = fraction;
            mValue = value;
            mValueType = int.class;
            mHasValue = true;
        }

        IntKeyframe(float fraction) {
            mFraction = fraction;
            mValueType = int.class;
        }

        public int getIntValue() {
            return mValue;
        }

        @Override
        public Integer getValue() {
            return mValue;
        }

        @Override
        public void setValue(Integer value) {
            if (value != null && value.getClass() == Integer.class) {
                mValue = value.intValue();
                mHasValue = true;
            }
        }

        @Override
        public IntKeyframe clone() {
            IntKeyframe kfClone = mHasValue ? new IntKeyframe(getFraction(), mValue) :
                    new IntKeyframe(getFraction());
            kfClone.setInterpolator(getInterpolator());
            kfClone.mValueWasSetOnStart = mValueWasSetOnStart;
            return kfClone;
        }
    }

    /**
     * Internal subclass used when the keyframe value is of type float.
     */
    static class FloatKeyframe extends Keyframe<Float> {
        /**
         * The value of the animation at the time mFraction.
         */
        float mValue;

        FloatKeyframe(float fraction, float value) {
            mFraction = fraction;
            mValue = value;
            mValueType = float.class;
            mHasValue = true;
        }

        FloatKeyframe(float fraction) {
            mFraction = fraction;
            mValueType = float.class;
        }

        public float getFloatValue() {
            return mValue;
        }

        @Override
        public Float getValue() {
            return mValue;
        }

        @Override
        public void setValue(Float value) {
            if (value != null && value.getClass() == Float.class) {
                mValue =  value.floatValue();
                mHasValue = true;
            }
        }

        @Override
        public FloatKeyframe clone() {
            FloatKeyframe kfClone = mHasValue ? new FloatKeyframe(getFraction(), mValue) :
                    new FloatKeyframe(getFraction());
            kfClone.setInterpolator(getInterpolator());
            kfClone.mValueWasSetOnStart = mValueWasSetOnStart;
            return kfClone;
        }
    }
}
