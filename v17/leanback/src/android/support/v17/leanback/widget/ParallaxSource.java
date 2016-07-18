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

import java.util.List;

/**
 * ParallaxSource tracks a list of dynamic {@link Variable}s typically representing foreground UI
 * element positions on screen.
 *
 * <p>
 * <ul>Restrictions
 * <li>Values must be in ascending order.</li>
 * <li>If the UI element is unknown above screen, use UNKNOWN_BEFORE.</li>
 * <li>if the UI element is unknown below screen, use UNKNOWN_AFTER.</li>
 * <li>UNKNOWN_BEFORE and USE_UNKNOWN_AFTER are not allowed to be next to each other.</li>
 * </ul>
 *
 * These rules can be verified by {@link #verifyFloatVariables(List)} or
 * {@link #verifyIntVariables(List)}.
 * </p>
 */
public abstract class ParallaxSource<VariableT extends ParallaxSource.Variable> {

    /**
     * Listener for tracking Variable value changes.
     */
    public static abstract class Listener {
        /**
         * Called when the value for any of the variable in ParallaxSource changes.
         */
        public void onVariableChanged(ParallaxSource source) {
        }
    }

    /**
     * This class typically represents UI element position inside {@link ParallaxSource}.
     */
    public abstract static class Variable<VariableT extends Variable> {
        private final ParallaxSource<VariableT> mSource;
        private String mName;

        public Variable(ParallaxSource<VariableT> source) {
            mSource = source;
        }

        /**
         * Returns the {@link ParallaxSource} this variable belongs to.
         *
         * @return The {@link ParallaxSource} this variable belongs to.
         */
        public ParallaxSource<VariableT> getSource() {
            return mSource;
        }

        /**
         * Sets name of the variable, used for debugging.
         *
         * @param name Name of the variable.
         */
        public final void setName(String name) {
            mName = name;
        }

        /**
         * Returns name of the variable, used for debugging.
         *
         * @return Name of the variable.
         */
        public final String getName() {
            return mName;
        }

        /**
         * Returns current value of this variable.
         *
         * @return Current value of this variable.
         */
        public abstract Object getValue();
    }

    /**
     * Implementation of (@link Variable} class for integer type.
     */
    public static class IntVariable extends Variable<IntVariable> {

        /**
         * Variable value is unknown and it's above screen.
         */
        public static final int UNKNOWN_BEFORE = Integer.MIN_VALUE;
        /**
         * Variable value is unknown and it's bellow screen.
         */
        public static final int UNKNOWN_AFTER = Integer.MAX_VALUE;

        private int mValue = UNKNOWN_AFTER;

        public IntVariable(ParallaxSource<IntVariable> source) {
            super(source);
        }

        /**
         * Returns the current int value.
         *
         * @return The current int value.
         */
        public final int getIntValue() {
            return mValue;
        }

        /**
         * Sets the current int value.
         *
         * @param value The current value to set.
         */
        public final void setIntValue(int value) {
            mValue = value;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        /**
         * Creates an {@link IntVariableKeyValue} object for the given keyValue.
         *
         * @param keyValue The integer key value.
         * @return A new {@link IntVariableKeyValue} object.
         */
        public IntVariableKeyValue at(int keyValue) {
            return new IntVariableKeyValue(this, keyValue, 0f);
        }

        /**
         * Create an {@link IntVariableKeyValue} object by multiplying the fraction with
         * {@link #getMaxParentVisibleSize()} and adding offsetValue to it.
         *
         * @param offsetValue                    An offset integer value to be added to key value.
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link #getMaxParentVisibleSize()} for the key
         *                                       value.
         * @return A new {@link IntVariableKeyValue} object.
         */
        public IntVariableKeyValue at(int offsetValue, float fractionOfMaxParentVisibleSize) {
            return new IntVariableKeyValue(this, offsetValue, fractionOfMaxParentVisibleSize);
        }
    }

    /**
     * Implementation of (@link Variable} class for float type.
     */
    public static class FloatVariable extends Variable<FloatVariable> {

        /**
         * Variable value is unknown and it's considered inside area before screen start.
         */
        public static final float UNKNOWN_BEFORE = -Float.MAX_VALUE;
        /**
         * Variable value is unknown and it's considered inside area after screen upper limit.
         */
        public static final float UNKNOWN_AFTER = Float.MAX_VALUE;

        private float mValue = UNKNOWN_AFTER;

        public FloatVariable(ParallaxSource<FloatVariable> source) {
            super(source);
        }

        /**
         * Gets current float value.
         *
         * @return Current float value.
         */
        public final float getFloatValue() {
            return mValue;
        }

        /**
         * Sets current float value.
         *
         * @param value Current value to set.
         */
        public final void setFloatValue(float value) {
            mValue = value;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        /**
         * Creates an {@link IntVariableKeyValue} object for the given keyValue.
         *
         * @param keyValue The integer key value.
         * @return A new {@link IntVariableKeyValue} object.
         */
        public FloatVariableKeyValue at(float keyValue) {
            return new FloatVariableKeyValue(this, keyValue, 0f);
        }

        /**
         * Create an {@link FloatVariableKeyValue} object by multiplying the fraction with
         * {@link #getMaxParentVisibleSize()} and adding offsetValue to it.
         *
         * @param offsetValue                    An offset integer value to be added to key value.
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link #getMaxParentVisibleSize()} for the key
         *                                       value.
         * @return A new {@link FloatVariableKeyValue} object.
         */
        public FloatVariableKeyValue at(float offsetValue, float fractionOfMaxParentVisibleSize) {
            return new FloatVariableKeyValue(this, offsetValue, fractionOfMaxParentVisibleSize);
        }
    }

    /**
     * Class holding a fixed key value for a variable in {@link ParallaxSource}.
     * Base class for {@link IntVariableKeyValue} and {@link FloatVariableKeyValue}.
     */
    public static class VariableKeyValue<VariableT extends Variable> {
        private final VariableT mVariable;

        public VariableKeyValue(VariableT variable) {
            mVariable = variable;
        }

        /**
         * Returns associated variable.
         *
         * @return Associated variable.
         */
        public final VariableT getVariable() {
            return mVariable;
        }
    }

    /**
     * Implementation of {@link VariableKeyValue} for integer type.
     */
    public static class IntVariableKeyValue extends VariableKeyValue<IntVariable> {
        private final int mValue;
        private final float mFactionOfMax;

        public IntVariableKeyValue(IntVariable variable, int value) {
            this(variable, value, 0f);
        }

        public IntVariableKeyValue(IntVariable variable, int value, float fractionOfMax) {
            super(variable);
            mValue = value;
            mFactionOfMax = fractionOfMax;
        }

        /**
         * Returns the key value of integer type.
         *
         * @return The key value.
         */
        public final int getIntValue() {
            return mFactionOfMax == 0 ? mValue : mValue + Math.round(getVariable().getSource()
                    .getMaxParentVisibleSize().getIntValue() * mFactionOfMax);
        }
    }

    /**
     * Implementation of {@link VariableKeyValue} for float type.
     */
    public static class FloatVariableKeyValue extends VariableKeyValue<FloatVariable> {
        private final float mValue;
        private final float mFactionOfMax;

        public FloatVariableKeyValue(FloatVariable variable, float value) {
            this(variable, value, 0f);
        }

        public FloatVariableKeyValue(FloatVariable variable, float value, float fractionOfMax) {
            super(variable);
            mValue = value;
            mFactionOfMax = fractionOfMax;
        }

        /**
         * Returns the key value of float type.
         *
         * @return The key value.
         */
        public final float getFloatValue() {
            return mFactionOfMax == 0 ? mValue : mValue + getVariable().getSource()
                    .getMaxParentVisibleSize().getFloatValue() * mFactionOfMax;
        }
    }

    /**
     * Returns a list of variables. Caller should not modify returned list.
     *
     * @return A list of variables.
     */
    public abstract List<VariableT> getVariables();

    /**
     * Sets listener to monitor variable value changes.
     *
     * @param listener The listener to set on {@link ParallaxSource} object.
     */
    public abstract void setListener(Listener listener);

    /**
     * Return the size of parent visible area, e.g. parent view's height if we are tracking Y
     * position of a child. The size can be used to calculate key value using the provided
     * fraction.
     *
     * @return Size of parent visible area.
     * @see IntVariableKeyValue
     * @see FloatVariableKeyValue
     */
    public abstract VariableT getMaxParentVisibleSize();

    /**
     * Verify sanity of variable values, throws RuntimeException if fails. The variables values
     * must be in ascending order. UNKNOW_BEFORE and UNKNOWN_AFTER are not allowed to be next to
     * each other.
     *
     * @param variables Variables to verify.
     */
    public static void verifyIntVariables(List<IntVariable> variables)
            throws IllegalStateException {
        if (variables.size() < 2) {
            return;
        }
        int last = variables.get(0).getIntValue();
        for (int i = 1; i < variables.size(); i++) {
            int v = variables.get(i).getIntValue();
            if (v < last) {
                throw new IllegalStateException(String.format("Parallax Variable[%d]\"%s\" is" +
                                " smaller than Varaible[%d]\"%s\"",
                        i, variables.get(i).getName(),
                        i - 1, variables.get(i - 1).getName()));
            } else if (last == IntVariable.UNKNOWN_BEFORE && v == IntVariable.UNKNOWN_AFTER) {
                throw new IllegalStateException(String.format("Parallax Variable[%d]\"%s\" is " +
                                "UNKNOW_BEFORE and Varaible[%d]\"%s\" is UNKNOWN_AFTER",
                        i - 1, variables.get(i - 1).getName(),
                        i, variables.get(i).getName()));
            }
            last = v;
        }
    }

    /**
     * Verify sanity of Variable values, throws RuntimeException if fails. The variables values
     * must be in ascending order. UNKNOW_BEFORE and UNKNOWN_AFTER are not allowed to be next to
     * each other.
     *
     * @param variables Variables to verify.
     */
    public static void verifyFloatVariables(List<FloatVariable> variables)
            throws IllegalStateException {
        if (variables.size() < 2) {
            return;
        }
        float last = variables.get(0).getFloatValue();
        for (int i = 1; i < variables.size(); i++) {
            float v = variables.get(i).getFloatValue();
            if (v < last) {
                throw new IllegalStateException(String.format("Parallax Variable[%d]\"%s\" is" +
                                " smaller than Varaible[%d]\"%s\"",
                        i, variables.get(i).getName(),
                        i - 1, variables.get(i - 1).getName()));
            } else if (last == FloatVariable.UNKNOWN_BEFORE && v == FloatVariable.UNKNOWN_AFTER) {
                throw new IllegalStateException(String.format("Parallax Variable[%d]\"%s\" is " +
                                "UNKNOW_BEFORE and Varaible[%d]\"%s\" is UNKNOWN_AFTER",
                        i - 1, variables.get(i - 1).getName(),
                        i, variables.get(i).getName()));
            }
            last = v;
        }
    }
}
