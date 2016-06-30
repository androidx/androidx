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

import android.support.v17.leanback.widget.ParallaxSource.FloatVariable;
import android.support.v17.leanback.widget.ParallaxSource.FloatVariableKeyValue;
import android.support.v17.leanback.widget.ParallaxSource.IntVariable;
import android.support.v17.leanback.widget.ParallaxSource.IntVariableKeyValue;
import android.support.v17.leanback.widget.ParallaxSource.Variable;
import android.support.v17.leanback.widget.ParallaxSource.VariableKeyValue;

import android.animation.PropertyValuesHolder;

import java.util.List;
import java.util.ArrayList;

/**
 * ParallaxEffect class drives changes in {@link ParallaxTarget} in response to changes in
 * variables defined in {@link ParallaxSource}.
 * <p>
 * ParallaxEffect has a list of {@link VariableKeyValue}s which represents the range of values that
 * source variables can take. The main function is
 * {@link ParallaxEffect#performMapping(ParallaxSource)} which computes a fraction between 0 and 1
 * based on the current values of variables in {@link ParallaxSource}. As the parallax effect goes
 * on, the fraction increases from 0 at beginning to 1 at the end. Then the fraction is passed on
 * to {@link ParallaxTarget#update(float)}.
 * <p>
 * ParallaxEffect has two concrete subclasses, {@link IntEffect} and {@link FloatEffect}.
 */
public abstract class ParallaxEffect<ParallaxEffectT extends ParallaxEffect,
        VariableKeyValueT extends ParallaxSource.VariableKeyValue> {

    final List<VariableKeyValueT> mKeyValues = new ArrayList<VariableKeyValueT>(2);
    final List<Float> mWeights = new ArrayList<Float>(2);
    final List<Float> mTotalWeights = new ArrayList<Float>(2);
    final List<ParallaxTarget> mTargets = new ArrayList<ParallaxTarget>(4);

    /**
     * Returns the list of {@link VariableKeyValue}s, which represents the range of values that
     * source variables can take.
     *
     * @return A list of {@link VariableKeyValue}s.
     * @see #performMapping(ParallaxSource)
     */
    public final List<VariableKeyValueT> getVariableRanges() {
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
     * Sets the list of {@link VariableKeyValue}s, which represents the range of values that
     * source variables can take.
     *
     * @param keyValues A list of {@link VariableKeyValue}s.
     * @see #performMapping(ParallaxSource)
     */
    public final void setVariableRanges(VariableKeyValueT... keyValues) {
        mKeyValues.clear();
        for (VariableKeyValueT keyValue : keyValues) {
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
     * Perform mapping from {@link ParallaxSource} to list of {@link ParallaxTarget}.
     */
    public final void performMapping(ParallaxSource source) {
        if (mKeyValues.size() < 2) {
            return;
        }
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

    private static int getVariableIndex(Variable variable) {
        List<Variable> variables = variable.getSource().getVariables();
        return variables == null ? -1 : variables.indexOf(variable);
    }

    /**
     * Implementation of {@link ParallaxEffect} for integer type.
     */
    public static final class IntEffect extends ParallaxEffect<IntEffect, IntVariableKeyValue> {

        @Override
        protected float calculateFraction(ParallaxSource s) {
            ParallaxSource<IntVariable> source = (ParallaxSource<IntVariable>) s;
            ParallaxSource.verifyIntVariables(source.getVariables());
            int lastIndex = 0;
            int lastValue = 0;
            int lastKeyValue = 0;
            // go through all KeyValues, find first KeyValue that current value is less than.
            for (int i = 0; i < mKeyValues.size(); i++) {
                IntVariableKeyValue k = mKeyValues.get(i);
                int index = getVariableIndex(k.getVariable());
                int keyValue = k.getIntValue();
                int currentValue = k.getVariable().getIntValue();

                if (currentValue < keyValue && currentValue != IntVariable.UNKNOWN_BEFORE) {
                    if (i == 0) {
                        return 0f;
                    }
                    float fraction;
                    if (lastIndex == index) {
                        // same variable index,  same UI element at two different KeyValues, e.g.
                        // UI element moves from lastkeyValue=500 to keyValue=0,
                        // fraction moves from 0 to 1.
                        fraction = (float) (lastKeyValue - currentValue)
                                / (lastKeyValue - keyValue);
                    } else if (lastValue != IntVariable.UNKNOWN_BEFORE) {
                        // e.g. UIElement_1 at keyValue=300 scroll to UIElement_2 at keyValue=400
                        // Figure out when UIElement_1 is at keyValue=300,  UIElement_2 should be
                        // at the position by adding delta of values to keyValue of UIElement_2.
                        lastKeyValue = lastKeyValue + (currentValue - lastValue);
                        fraction = (float) (lastKeyValue - currentValue)
                                / (lastKeyValue - keyValue);
                    } else {
                        // now we have to estimate
                        lastKeyValue = keyValue + source.getMaxParentVisibleSize().getIntValue();
                        fraction = 1f - (float) (currentValue - keyValue)
                                / (lastKeyValue - keyValue);
                    }
                    // when there are three or more KeyValues, take weight into consideration.
                    if (mTotalWeights.size() >= 2) {
                        float totalWeight = mTotalWeights.size() > 0 ?
                                mTotalWeights.get(mTotalWeights.size() - 1) : 0f;
                        fraction = mTotalWeights.get(index - 1) / totalWeight * fraction;
                        if (index - 2 >= 0) {
                            fraction = mTotalWeights.get(index - 2) / totalWeight + fraction;
                        }
                    }
                    return fraction;
                }
                lastValue = currentValue;
                lastIndex = index;
                lastKeyValue = keyValue;
            }
            if (lastValue == IntVariable.UNKNOWN_BEFORE) {
                return 0f;
            } else {
                return 1f;
            }
        }
    }

    /**
     * Implementation of {@link ParallaxEffect} for float type.
     */
    public static final class FloatEffect extends ParallaxEffect<FloatEffect,
            FloatVariableKeyValue> {

        @Override
        protected float calculateFraction(ParallaxSource s) {
            ParallaxSource<FloatVariable> source = (ParallaxSource<FloatVariable>) s;
            ParallaxSource.verifyFloatVariables(source.getVariables());
            int lastIndex = 0;
            float lastValue = 0;
            float lastKeyValue = 0;
            // go through all KeyValues, find first KeyValue that current value is less than.
            for (int i = 0; i < mKeyValues.size(); i++) {
                FloatVariableKeyValue k = mKeyValues.get(i);
                int index = getVariableIndex(k.getVariable());
                float keyValue = k.getFloatValue();
                float currentValue = k.getVariable().getFloatValue();

                if (currentValue < keyValue && currentValue != IntVariable.UNKNOWN_BEFORE) {
                    if (i == 0) {
                        return 0f;
                    }
                    float fraction;
                    if (lastIndex == index) {
                        // same variable index,  same UI element at two different KeyValues, e.g.
                        // UI element moves from lastkeyValue=500 to keyValue=0,
                        // fraction moves from 0 to 1.
                        fraction = (float) (lastKeyValue - currentValue)
                                / (lastKeyValue - keyValue);
                    } else if (lastValue != IntVariable.UNKNOWN_BEFORE) {
                        // e.g. UIElement_1 at keyValue=300 scroll to UIElement_2 at keyValue=400
                        // Figure out when UIElement_1 is at keyValue=300,  UIElement_2 should be
                        // at the position by adding delta of values to keyValue of UIElement_2.
                        lastKeyValue = lastKeyValue + (currentValue - lastValue);
                        fraction = (float) (lastKeyValue - currentValue)
                                / (lastKeyValue - keyValue);
                    } else {
                        // now we have to estimate
                        lastKeyValue = keyValue + source.getMaxParentVisibleSize().getFloatValue();
                        fraction = 1f - (float) (currentValue - keyValue)
                                / (lastKeyValue - keyValue);
                    }
                    // when there are three or more KeyValues, calculate the weight.
                    if (mTotalWeights.size() >= 2) {
                        float totalWeight = mTotalWeights.size() > 0 ?
                                mTotalWeights.get(mTotalWeights.size() - 1) : 0f;
                        fraction = mTotalWeights.get(index - 1) / totalWeight * fraction;
                        if (index - 2 >= 0) {
                            fraction = mTotalWeights.get(index - 2) / totalWeight + fraction;
                        }
                    }
                    return fraction;
                }
                lastValue = currentValue;
                lastIndex = index;
                lastKeyValue = keyValue;
            }
            if (lastValue == IntVariable.UNKNOWN_BEFORE) {
                return 0f;
            } else {
                return 1f;
            }
        }
    }

}

