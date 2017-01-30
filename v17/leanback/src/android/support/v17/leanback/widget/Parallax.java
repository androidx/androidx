/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v17.leanback.widget;

import android.support.annotation.CallSuper;
import android.support.v17.leanback.widget.ParallaxEffect.FloatEffect;
import android.support.v17.leanback.widget.ParallaxEffect.IntEffect;
import android.util.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parallax tracks a list of dynamic {@link Property}s typically representing foreground UI
 * element positions on screen. Parallax keeps a list of {@link ParallaxEffect} objects which define
 * rules to mapping property values to {@link ParallaxTarget}.
 *
 * <p>
 * There are two types of Parallax, int or float. App should subclass either
 * {@link Parallax.IntParallax} or {@link Parallax.FloatParallax}. App may subclass
 * {@link Parallax.IntProperty} or {@link Parallax.FloatProperty} to supply additional information
 * about how to retrieve Property value.  {@link RecyclerViewParallax} is a great example of
 * Parallax implementation tracking child view positions on screen.
 * </p>
 * <p>
 * <ul>Restrictions of properties
 * <li>Values must be in ascending order.</li>
 * <li>If the UI element is unknown above screen, use UNKNOWN_BEFORE.</li>
 * <li>if the UI element is unknown below screen, use UNKNOWN_AFTER.</li>
 * <li>UNKNOWN_BEFORE and UNKNOWN_AFTER are not allowed to be next to each other.</li>
 * </ul>
 * These rules can be verified by {@link #verifyProperties()}.
 * </p>
 * Subclass should override {@link #updateValues()} to update property values and perform
 * {@link ParallaxEffect}s. Subclass may call {@link #updateValues()} automatically e.g.
 * {@link RecyclerViewParallax} calls {@link #updateValues()} in RecyclerView scrolling. App might
 * call {@link #updateValues()} manually when Parallax is unaware of the value change. For example,
 * when a slide transition is running, {@link RecyclerViewParallax} is unaware of translation value
 * changes; it's the app's responsibility to call {@link #updateValues()} in every frame of
 * animation.
 * </p>
 * @param <PropertyT> Class of the property, e.g. {@link IntProperty} or {@link FloatProperty}.
 */
public abstract class Parallax<PropertyT extends Property> {

    private final List<ParallaxEffect> mEffects = new ArrayList<ParallaxEffect>(4);

    /**
     * Class holding a fixed value for a Property in {@link Parallax}.
     * Base class for {@link IntPropertyMarkerValue} and {@link FloatPropertyMarkerValue}.
     * @param <PropertyT> Class of the property, e.g. {@link IntProperty} or {@link FloatProperty}.
     */
    public static class PropertyMarkerValue<PropertyT> {
        private final PropertyT mProperty;

        public PropertyMarkerValue(PropertyT property) {
            mProperty = property;
        }

        /**
         * @return Associated property.
         */
        public PropertyT getProperty() {
            return mProperty;
        }
    }

    /**
     * IntProperty provide access to an index based integer type property inside
     * {@link IntParallax}. The IntProperty typically represents UI element position inside
     * {@link IntParallax}.
     */
    public static class IntProperty extends Property<IntParallax, Integer> {

        /**
         * Property value is unknown and it's smaller than minimal value of Parallax. For
         * example if a child is not created and before the first visible child of RecyclerView.
         */
        public static final int UNKNOWN_BEFORE = Integer.MIN_VALUE;

        /**
         * Property value is unknown and it's larger than {@link IntParallax#getMaxValue()}. For
         * example if a child is not created and after the last visible child of RecyclerView.
         */
        public static final int UNKNOWN_AFTER = Integer.MAX_VALUE;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param name Name of this Property.
         * @param index Index of this Property inside {@link IntParallax}.
         */
        public IntProperty(String name, int index) {
            super(Integer.class, name);
            mIndex = index;
        }

        @Override
        public final Integer get(IntParallax object) {
            return getIntValue(object);
        }

        @Override
        public final void set(IntParallax object, Integer value) {
            setIntValue(object, value);
        }

        final int getIntValue(IntParallax source) {
            return source.getPropertyValue(mIndex);
        }

        final void setIntValue(IntParallax source, int value) {
            source.setPropertyValue(mIndex, value);
        }

        /**
         * @return Index of this Property in {@link IntParallax}.
         */
        public final int getIndex() {
            return mIndex;
        }

        /**
         * Creates an {@link IntPropertyMarkerValue} object for the absolute marker value.
         *
         * @param absoluteValue The integer marker value.
         * @return A new {@link IntPropertyMarkerValue} object.
         */
        public final IntPropertyMarkerValue atAbsolute(int absoluteValue) {
            return new IntPropertyMarkerValue(this, absoluteValue, 0f);
        }

        /**
         * Creates an {@link IntPropertyMarkerValue} object for a fraction of
         * {@link IntParallax#getMaxValue()}.
         *
         * @param fractionOfMaxValue 0 to 1 fraction to multiply with
         *                                       {@link IntParallax#getMaxValue()} for
         *                                       the marker value.
         * @return A new {@link IntPropertyMarkerValue} object.
         */
        public final IntPropertyMarkerValue atFraction(float fractionOfMaxValue) {
            return new IntPropertyMarkerValue(this, 0, fractionOfMaxValue);
        }

        /**
         * Create an {@link IntPropertyMarkerValue} object by multiplying the fraction with
         * {@link IntParallax#getMaxValue()} and adding offsetValue to it.
         *
         * @param offsetValue                    An offset integer value to be added to marker
         *                                       value.
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link IntParallax#getMaxValue()} for
         *                                       the marker value.
         * @return A new {@link IntPropertyMarkerValue} object.
         */
        public final IntPropertyMarkerValue at(int offsetValue,
                                               float fractionOfMaxParentVisibleSize) {
            return new IntPropertyMarkerValue(this, offsetValue, fractionOfMaxParentVisibleSize);
        }
    }

    /**
     * Parallax that manages a list of {@link IntProperty}. App may override this class with a
     * specific {@link IntProperty} subclass.
     *
     * @param <IntPropertyT> Type of {@link IntProperty} or subclass.
     */
    public abstract static class IntParallax<IntPropertyT extends IntProperty>
            extends Parallax<IntPropertyT> {

        private int[] mValues = new int[4];

        /**
         * Get index based property value.
         *
         * @param index Index of the property.
         * @return Value of the property.
         */
        public final int getPropertyValue(int index) {
            return mValues[index];
        }

        /**
         * Set index based property value.
         *
         * @param index Index of the property.
         * @param value Value of the property.
         */
        public final void setPropertyValue(int index, int value) {
            if (index >= mProperties.size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            mValues[index] = value;
        }

        /**
         * Return the max value, which is typically parent visible area, e.g. RecyclerView's height
         * if we are tracking Y position of a child. The size can be used to calculate marker value
         * using the provided fraction of IntPropertyMarkerValue.
         *
         * @return Max value of parallax.
         * @see IntPropertyMarkerValue#IntPropertyMarkerValue(IntProperty, int, float)
         */
        public abstract int getMaxValue();

        @Override
        public final IntPropertyT addProperty(String name) {
            int newPropertyIndex = mProperties.size();
            IntPropertyT property = createProperty(name, newPropertyIndex);
            mProperties.add(property);
            int size = mValues.length;
            if (size == newPropertyIndex) {
                int[] newValues = new int[size * 2];
                for (int i = 0; i < size; i++) {
                    newValues[i] = mValues[i];
                }
                mValues = newValues;
            }
            mValues[newPropertyIndex] = IntProperty.UNKNOWN_AFTER;
            return property;
        }

        @Override
        public final void verifyProperties() throws IllegalStateException {
            if (mProperties.size() < 2) {
                return;
            }
            int last = mProperties.get(0).getIntValue(this);
            for (int i = 1; i < mProperties.size(); i++) {
                int v = mProperties.get(i).getIntValue(this);
                if (v < last) {
                    throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is"
                                    + " smaller than Property[%d]\"%s\"",
                            i, mProperties.get(i).getName(),
                            i - 1, mProperties.get(i - 1).getName()));
                } else if (last == IntProperty.UNKNOWN_BEFORE && v == IntProperty.UNKNOWN_AFTER) {
                    throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is"
                                    + " UNKNOWN_BEFORE and Property[%d]\"%s\" is UNKNOWN_AFTER",
                            i - 1, mProperties.get(i - 1).getName(),
                            i, mProperties.get(i).getName()));
                }
                last = v;
            }
        }

    }

    /**
     * Implementation of {@link PropertyMarkerValue} for {@link IntProperty}.
     */
    public static class IntPropertyMarkerValue extends PropertyMarkerValue<IntProperty> {
        private final int mValue;
        private final float mFactionOfMax;

        public IntPropertyMarkerValue(IntProperty property, int value) {
            this(property, value, 0f);
        }

        public IntPropertyMarkerValue(IntProperty property, int value, float fractionOfMax) {
            super(property);
            mValue = value;
            mFactionOfMax = fractionOfMax;
        }

        /**
         * @return The marker value of integer type.
         */
        public final int getMarkerValue(IntParallax source) {
            return mFactionOfMax == 0 ? mValue : mValue + Math.round(source
                    .getMaxValue() * mFactionOfMax);
        }
    }

    /**
     * FloatProperty provide access to an index based integer type property inside
     * {@link FloatParallax}. The FloatProperty typically represents UI element position inside
     * {@link FloatParallax}.
     */
    public static class FloatProperty extends Property<FloatParallax, Float> {

        /**
         * Property value is unknown and it's smaller than minimal value of Parallax. For
         * example if a child is not created and before the first visible child of RecyclerView.
         */
        public static final float UNKNOWN_BEFORE = -Float.MAX_VALUE;

        /**
         * Property value is unknown and it's larger than {@link FloatParallax#getMaxValue()}. For
         * example if a child is not created and after the last visible child of RecyclerView.
         */
        public static final float UNKNOWN_AFTER = Float.MAX_VALUE;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param name Name of this Property.
         * @param index Index of this Property inside {@link FloatParallax}.
         */
        public FloatProperty(String name, int index) {
            super(Float.class, name);
            mIndex = index;
        }

        @Override
        public final Float get(FloatParallax object) {
            return getFloatValue(object);
        }

        @Override
        public final void set(FloatParallax object, Float value) {
            setFloatValue(object, value);
        }

        final float getFloatValue(FloatParallax source) {
            return source.getPropertyValue(mIndex);
        }

        final void setFloatValue(FloatParallax source, float value) {
            source.setPropertyValue(mIndex, value);
        }

        /**
         * @return Index of this Property in {@link FloatParallax}.
         */
        public final int getIndex() {
            return mIndex;
        }

        /**
         * Creates an {@link FloatPropertyMarkerValue} object for the absolute marker value.
         *
         * @param markerValue The float marker value.
         * @return A new {@link FloatPropertyMarkerValue} object.
         */
        public final FloatPropertyMarkerValue atAbsolute(float markerValue) {
            return new FloatPropertyMarkerValue(this, markerValue, 0f);
        }

        /**
         * Creates an {@link FloatPropertyMarkerValue} object for a fraction of
         * {@link FloatParallax#getMaxValue()}.
         *
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link FloatParallax#getMaxValue()} for
         *                                       the marker value.
         * @return A new {@link FloatPropertyMarkerValue} object.
         */
        public final FloatPropertyMarkerValue atFraction(float fractionOfMaxParentVisibleSize) {
            return new FloatPropertyMarkerValue(this, 0, fractionOfMaxParentVisibleSize);
        }

        /**
         * Create an {@link FloatPropertyMarkerValue} object by multiplying the fraction with
         * {@link FloatParallax#getMaxValue()} and adding offsetValue to it.
         *
         * @param offsetValue                    An offset float value to be added to marker value.
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link FloatParallax#getMaxValue()} for
         *                                       the marker value.
         * @return A new {@link FloatPropertyMarkerValue} object.
         */
        public final FloatPropertyMarkerValue at(float offsetValue,
                                                 float fractionOfMaxParentVisibleSize) {
            return new FloatPropertyMarkerValue(this, offsetValue, fractionOfMaxParentVisibleSize);
        }
    }

    /**
     * Parallax that manages a list of {@link FloatProperty}. App may override this class with a
     * specific {@link FloatProperty} subclass.
     *
     * @param <FloatPropertyT> Type of {@link FloatProperty} or subclass.
     */
    public abstract static class FloatParallax<FloatPropertyT extends FloatProperty> extends
            Parallax<FloatPropertyT> {

        private float[] mValues = new float[4];

        /**
         * Get index based property value.
         *
         * @param index Index of the property.
         * @return Value of the property.
         */
        public final float getPropertyValue(int index) {
            return mValues[index];
        }

        /**
         * Set index based property value.
         *
         * @param index Index of the property.
         * @param value Value of the property.
         */
        public final void setPropertyValue(int index, float value) {
            if (index >= mProperties.size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            mValues[index] = value;
        }

        /**
         * Return the max value which is typically size of parent visible area, e.g. RecyclerView's
         * height if we are tracking Y position of a child. The size can be used to calculate marker
         * value using the provided fraction of FloatPropertyMarkerValue.
         *
         * @return Size of parent visible area.
         * @see FloatPropertyMarkerValue#FloatPropertyMarkerValue(FloatProperty, float, float)
         */
        public abstract float getMaxValue();

        @Override
        public final FloatPropertyT addProperty(String name) {
            int newPropertyIndex = mProperties.size();
            FloatPropertyT property = createProperty(name, newPropertyIndex);
            mProperties.add(property);
            int size = mValues.length;
            if (size == newPropertyIndex) {
                float[] newValues = new float[size * 2];
                for (int i = 0; i < size; i++) {
                    newValues[i] = mValues[i];
                }
                mValues = newValues;
            }
            mValues[newPropertyIndex] = FloatProperty.UNKNOWN_AFTER;
            return property;
        }

        @Override
        public final void verifyProperties() throws IllegalStateException {
            if (mProperties.size() < 2) {
                return;
            }
            float last = mProperties.get(0).getFloatValue(this);
            for (int i = 1; i < mProperties.size(); i++) {
                float v = mProperties.get(i).getFloatValue(this);
                if (v < last) {
                    throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is"
                                    + " smaller than Property[%d]\"%s\"",
                            i, mProperties.get(i).getName(),
                            i - 1, mProperties.get(i - 1).getName()));
                } else if (last == FloatProperty.UNKNOWN_BEFORE && v
                        == FloatProperty.UNKNOWN_AFTER) {
                    throw new IllegalStateException(String.format("Parallax Property[%d]\"%s\" is"
                                    + " UNKNOWN_BEFORE and Property[%d]\"%s\" is UNKNOWN_AFTER",
                            i - 1, mProperties.get(i - 1).getName(),
                            i, mProperties.get(i).getName()));
                }
                last = v;
            }
        }

    }

    /**
     * Implementation of {@link PropertyMarkerValue} for {@link FloatProperty}.
     */
    public static class FloatPropertyMarkerValue extends PropertyMarkerValue<FloatProperty> {
        private final float mValue;
        private final float mFactionOfMax;

        public FloatPropertyMarkerValue(FloatProperty property, float value) {
            this(property, value, 0f);
        }

        public FloatPropertyMarkerValue(FloatProperty property, float value, float fractionOfMax) {
            super(property);
            mValue = value;
            mFactionOfMax = fractionOfMax;
        }

        /**
         * @return The marker value.
         */
        public final float getMarkerValue(FloatParallax source) {
            return mFactionOfMax == 0 ? mValue : mValue + source.getMaxValue()
                    * mFactionOfMax;
        }
    }

    final List<PropertyT> mProperties = new ArrayList<PropertyT>();
    final List<PropertyT> mPropertiesReadOnly = Collections.unmodifiableList(mProperties);

    /**
     * @return A unmodifiable list of properties.
     */
    public final List<PropertyT> getProperties() {
        return mPropertiesReadOnly;
    }

    /**
     * Add a new Property in the Parallax object.
     *
     * @param name Name of the property.
     * @return Newly created Property.
     */
    public abstract PropertyT addProperty(String name);

    /**
     * Create a new Property object. App does not directly call this method.  See
     * {@link #addProperty(String)}.
     *
     * @param index  Index of the property in this Parallax object.
     * @return Newly created Property object.
     */
    public abstract PropertyT createProperty(String name, int index);

    /**
     * Verify sanity of property values, throws RuntimeException if fails. The property values
     * must be in ascending order. UNKNOW_BEFORE and UNKNOWN_AFTER are not allowed to be next to
     * each other.
     */
    public abstract void verifyProperties() throws IllegalStateException;

    /**
     * Update property values and perform {@link ParallaxEffect}s. Subclass may override and call
     * super.updateValues() after updated properties values.
     */
    @CallSuper
    public void updateValues() {
        for (int i = 0; i < mEffects.size(); i++) {
            mEffects.get(i).performMapping(this);
        }
    }

    /**
     * Adds a {@link ParallaxEffect} object which defines rules to perform mapping to multiple
     * {@link ParallaxTarget}s.
     *
     * @param effect A {@link ParallaxEffect} object.
     */
    public void addEffect(ParallaxEffect effect) {
        mEffects.add(effect);
    }

    /**
     * Returns a list of {@link ParallaxEffect} object which defines rules to perform mapping to
     * multiple {@link ParallaxTarget}s.
     *
     * @return A list of {@link ParallaxEffect} object.
     */
    public List<ParallaxEffect> getEffects() {
        return mEffects;
    }

    /**
     * Remove the {@link ParallaxEffect} object.
     *
     * @param effect The {@link ParallaxEffect} object to remove.
     */
    public void removeEffect(ParallaxEffect effect) {
        mEffects.remove(effect);
    }

    /**
     * Remove all {@link ParallaxEffect} objects.
     */
    public void removeAllEffects() {
        mEffects.clear();
    }

    /**
     * Create a {@link ParallaxEffect} object that will track source variable changes within a
     * provided set of ranges.
     *
     * @param ranges A list of marker values that defines the ranges.
     * @return Newly created ParallaxEffect object.
     */
    public ParallaxEffect addEffect(IntPropertyMarkerValue... ranges) {
        IntEffect effect = new IntEffect();
        effect.setPropertyRanges(ranges);
        addEffect(effect);
        return effect;
    }

    /**
     * Create a {@link ParallaxEffect} object that will track source variable changes within a
     * provided set of ranges.
     *
     * @param ranges A list of marker values that defines the ranges.
     * @return Newly created ParallaxEffect object.
     */
    public ParallaxEffect addEffect(FloatPropertyMarkerValue... ranges) {
        FloatEffect effect = new FloatEffect();
        effect.setPropertyRanges(ranges);
        addEffect(effect);
        return effect;
    }
}
