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

import android.animation.PropertyValuesHolder;
import android.support.v17.leanback.widget.ParallaxSource.FloatProperty;
import android.support.v17.leanback.widget.ParallaxSource.FloatPropertyKeyValue;
import android.support.v17.leanback.widget.ParallaxSource.IntProperty;
import android.support.v17.leanback.widget.ParallaxSource.IntPropertyKeyValue;
import android.support.v17.leanback.widget.ParallaxSource.PropertyKeyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * ParallaxEffect class drives changes in {@link ParallaxTarget} in response to changes in
 * variables defined in {@link ParallaxSource}.
 * <p>
 * ParallaxEffect has a list of {@link PropertyKeyValue}s which represents the range of values that
 * source variables can take. The main function is
 * {@link ParallaxEffect#performMapping(ParallaxSource)} which computes a fraction between 0 and 1
 * based on the current values of variables in {@link ParallaxSource}. As the parallax effect goes
 * on, the fraction increases from 0 at beginning to 1 at the end. Then the fraction is passed on
 * to {@link ParallaxTarget#update(float)}.
 * <p>
 * ParallaxEffect has two concrete subclasses, {@link IntEffect} and {@link FloatEffect}.
 * @hide
 */
public abstract class ParallaxEffect<ParallaxEffectT extends ParallaxEffect,
        PropertyKeyValueT extends ParallaxSource.PropertyKeyValue> {

    final List<PropertyKeyValueT> mKeyValues = new ArrayList<PropertyKeyValueT>(2);
    final List<Float> mWeights = new ArrayList<Float>(2);
    final List<Float> mTotalWeights = new ArrayList<Float>(2);
    final List<ParallaxTarget> mTargets = new ArrayList<ParallaxTarget>(4);

    /**
     * Returns the list of {@link PropertyKeyValue}s, which represents the range of values that
     * source variables can take.
     *
     * @return A list of {@link PropertyKeyValue}s.
     * @see #performMapping(ParallaxSource)
     */
    public final List<PropertyKeyValueT> getPropertyRanges() {
        return mKeyValues;
    }

    /**
     * Returns a list of Float objects that represents weight associated with each variable range.
     * Weights are used when there are three or more key values.
     *
     * @return A list of Float objects that represents weight associated with each variable range.
     * @hide
     */
    public final List<Float> getWeights() {
        return mWeights;
    }

    /**
     * Sets the list of {@link PropertyKeyValue}s, which represents the range of values that
     * source variables can take.
     *
     * @param keyValues A list of {@link PropertyKeyValue}s.
     * @see #performMapping(ParallaxSource)
     */
    public final void setPropertyRanges(PropertyKeyValueT... keyValues) {
        mKeyValues.clear();
        for (PropertyKeyValueT keyValue : keyValues) {
            mKeyValues.add(keyValue);
        }
    }

    /**
     * Sets a list of Float objects that represents weight associated with each variable range.
     * Weights are used when there are three or more key values.
     *
     * @param weights A list of Float objects that represents weight associated with each variable
     *                range.
     * @hide
     */
    public final void setWeights(float... weights) {
        for (float weight : weights) {
            if (weight <= 0) {
                throw new IllegalArgumentException();
            }
        }
        mWeights.clear();
        mTotalWeights.clear();
        float totalWeight = 0f;
        for (float weight : weights) {
            mWeights.add(weight);
            totalWeight += weight;
            mTotalWeights.add(totalWeight);
        }
    }

    /**
     * Sets a list of Float objects that represents weight associated with each variable range.
     * Weights are used when there are three or more key values.
     *
     * @param weights A list of Float objects that represents weight associated with each variable
     *                range.
     * @return This ParallaxEffect object, allowing calls to methods in this class to be chained.
     * @hide
     */
    public final ParallaxEffect weights(float... weights) {
        setWeights(weights);
        return this;
    }

    /**
     * Add a ParallaxTarget to run parallax effect.
     *
     * @param target ParallaxTarget to add.
     */
    public final void addTarget(ParallaxTarget target) {
        mTargets.add(target);
    }

    /**
     * Add a ParallaxTarget to run parallax effect.
     *
     * @param target ParallaxTarget to add.
     * @return This ParallaxEffect object, allowing calls to methods in this class to be chained.
     */
    public final ParallaxEffect target(ParallaxTarget target) {
        mTargets.add(target);
        return this;
    }

    /**
     * Creates a {@link ParallaxTarget} from {@link PropertyValuesHolder} and adds it to the list
     * of targets.
     *
     * @param targetObject Target object for PropertyValuesHolderTarget.
     * @param values       PropertyValuesHolder for PropertyValuesHolderTarget.
     * @return This ParallaxEffect object, allowing calls to methods in this class to be chained.
     */
    public final ParallaxEffect target(Object targetObject, PropertyValuesHolder values) {
        mTargets.add(new ParallaxTarget.PropertyValuesHolderTarget(targetObject, values));
        return this;
    }

    /**
     * Returns the list of {@link ParallaxTarget} objects.
     *
     * @return The list of {@link ParallaxTarget} objects.
     */
    public final List<ParallaxTarget> getTargets() {
        return mTargets;
    }

    /**
     * Remove a {@link ParallaxTarget} object from the list.
     * @param target The {@link ParallaxTarget} object to be removed.
     */
    public final void removeTarget(ParallaxTarget target) {
        mTargets.remove(target);
    }

    /**
     * Perform mapping from {@link ParallaxSource} to list of {@link ParallaxTarget}.
     */
    public final void performMapping(ParallaxSource source) {
        if (mKeyValues.size() < 2) {
            return;
        }
        source.verifyProperties();
        float fraction = calculateFraction(source);
        for (int i = 0; i < mTargets.size(); i++) {
            mTargets.get(i).update(fraction);
        }
    }

    /**
     * This method is expected to compute a fraction between 0 and 1 based on the current values of
     * variables in {@link ParallaxSource}. As the parallax effect goes on, the fraction increases
     * from 0 at beginning to 1 at the end.
     *
     * @return Float value between 0 and 1.
     */
    protected abstract float calculateFraction(ParallaxSource source);

    /**
     * When there are multiple ranges (aka three or more keyvalues),  this method adjust the
     * fraction inside a range to fraction of whole range.
     * e.g. four key values, three weight values: 6, 2, 2.  totalWeights are 6, 8, 10
     * When keyValueIndex is 3, the fraction is inside last range.
     * adjusted_fraction = 8 / 10 + 2 / 10 * fraction.
     */
    final float getFractionWithWeightAdjusted(float fraction, int keyValueIndex) {
        // when there are three or more KeyValues, take weight into consideration.
        if (mKeyValues.size() >= 3) {
            final boolean hasWeightsDefined = mWeights.size() == mKeyValues.size() - 1;
            if (hasWeightsDefined) {
                // use weights user defined
                final float allWeights = mTotalWeights.get(mTotalWeights.size() - 1);
                fraction = fraction * mWeights.get(keyValueIndex - 1) / allWeights;
                if (keyValueIndex >= 2) {
                    fraction += mTotalWeights.get(keyValueIndex - 2) / allWeights;
                }
            } else {
                // assume each range has same weight.
                final float allWeights = mKeyValues.size() - 1;
                fraction = fraction / allWeights;
                if (keyValueIndex >= 2) {
                    fraction += (float)(keyValueIndex - 1) / allWeights;
                }
            }
        }
        return fraction;
    }

    /**
     * Implementation of {@link ParallaxEffect} for integer type.
     */
    public static final class IntEffect extends ParallaxEffect<IntEffect, IntPropertyKeyValue> {

        @Override
        protected float calculateFraction(ParallaxSource s) {
            ParallaxSource.IntSource source = (ParallaxSource.IntSource) s;
            int lastIndex = 0;
            int lastValue = 0;
            int lastKeyValue = 0;
            // go through all KeyValues, find first KeyValue that current value is less than.
            for (int i = 0; i < mKeyValues.size(); i++) {
                IntPropertyKeyValue k = mKeyValues.get(i);
                int index = k.getProperty().getIndex();
                int keyValue = k.getKeyValue(source);
                int currentValue = source.getPropertyValue(index);

                float fraction;
                if (i == 0) {
                    if (currentValue >= keyValue) {
                        return 0f;
                    }
                } else {
                    if (lastIndex == index && lastKeyValue < keyValue) {
                        throw new IllegalStateException("KeyValue of same variable must be "
                                + "descendant order");
                    }
                    if (currentValue == IntProperty.UNKNOWN_AFTER) {
                        // Implies lastValue is less than lastKeyValue and lastValue is not
                        // UNKNWON_AFTER.  Estimates based on distance of two variables is screen
                        // size.
                        fraction = (float) (lastKeyValue - lastValue)
                                / source.getMaxParentVisibleSize();
                        return getFractionWithWeightAdjusted(fraction, i);
                    } else if (currentValue >= keyValue) {
                        if (lastIndex == index) {
                            // same variable index,  same UI element at two different KeyValues,
                            // e.g. UI element moves from lastkeyValue=500 to keyValue=0,
                            // fraction moves from 0 to 1.
                            fraction = (float) (lastKeyValue - currentValue)
                                    / (lastKeyValue - keyValue);
                        } else if (lastValue != IntProperty.UNKNOWN_BEFORE) {
                            // e.g. UIElement_1 at 300 scroll to UIElement_2 at 400, figure out when
                            // UIElement_1 is at keyValue=300,  keyValue of UIElement_2 by adding
                            // delta of values to keyValue of UIElement_2.
                            lastKeyValue = lastKeyValue + (currentValue - lastValue);
                            fraction = (float) (lastKeyValue - currentValue)
                                    / (lastKeyValue - keyValue);
                        } else {
                            // Last variable is UNKNOWN_BEFORE.  Estimates based on assumption total
                            // travel distance from last variable to this variable is screen visible
                            // size.
                            fraction = 1f - (float) (currentValue - keyValue)
                                    / source.getMaxParentVisibleSize();
                        }
                        return getFractionWithWeightAdjusted(fraction, i);
                    }
                }
                lastValue = currentValue;
                lastIndex = index;
                lastKeyValue = keyValue;
            }
            return 1f;
        }
    }

    /**
     * Implementation of {@link ParallaxEffect} for float type.
     */
    public static final class FloatEffect extends ParallaxEffect<FloatEffect,
            FloatPropertyKeyValue> {

        @Override
        protected float calculateFraction(ParallaxSource s) {
            ParallaxSource.FloatSource source = (ParallaxSource.FloatSource) s;
            int lastIndex = 0;
            float lastValue = 0;
            float lastKeyValue = 0;
            // go through all KeyValues, find first KeyValue that current value is less than.
            for (int i = 0; i < mKeyValues.size(); i++) {
                FloatPropertyKeyValue k = mKeyValues.get(i);
                int index = k.getProperty().getIndex();
                float keyValue = k.getKeyValue(source);
                float currentValue = source.getPropertyValue(index);

                float fraction;
                if (i == 0) {
                    if (currentValue >= keyValue) {
                        return 0f;
                    }
                } else {
                    if (lastIndex == index && lastKeyValue < keyValue) {
                        throw new IllegalStateException("KeyValue of same variable must be "
                                + "descendant order");
                    }
                    if (currentValue == FloatProperty.UNKNOWN_AFTER) {
                        // Implies lastValue is less than lastKeyValue and lastValue is not
                        // UNKNOWN_AFTER.  Estimates based on distance of two variables is screen
                        // size.
                        fraction = (float) (lastKeyValue - lastValue)
                                / source.getMaxParentVisibleSize();
                        return getFractionWithWeightAdjusted(fraction, i);
                    } else if (currentValue >= keyValue) {
                        if (lastIndex == index) {
                            // same variable index,  same UI element at two different KeyValues,
                            // e.g. UI element moves from lastkeyValue=500 to keyValue=0,
                            // fraction moves from 0 to 1.
                            fraction = (float) (lastKeyValue - currentValue)
                                    / (lastKeyValue - keyValue);
                        } else if (lastValue != FloatProperty.UNKNOWN_BEFORE) {
                            // e.g. UIElement_1 at 300 scroll to UIElement_2 at 400, figure out when
                            // UIElement_1 is at keyValue=300,  keyValue of UIElement_2 by adding
                            // delta of values to keyValue of UIElement_2.
                            lastKeyValue = lastKeyValue + (currentValue - lastValue);
                            fraction = (float) (lastKeyValue - currentValue)
                                    / (lastKeyValue - keyValue);
                        } else {
                            // Last variable is UNKNOWN_BEFORE.  Estimates based on assumption total
                            // travel distance from last variable to this variable is screen visible
                            // size.
                            fraction = 1f - (float) (currentValue - keyValue)
                                    / source.getMaxParentVisibleSize();
                        }
                        return getFractionWithWeightAdjusted(fraction, i);
                    }
                }
                lastValue = currentValue;
                lastIndex = index;
                lastKeyValue = keyValue;
            }
            return 1f;
        }
    }

}

