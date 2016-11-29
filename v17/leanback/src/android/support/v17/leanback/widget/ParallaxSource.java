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

import android.util.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * ParallaxSource tracks a list of dynamic {@link Property}s typically representing foreground UI
 * element positions on screen. App should subclass either {@link ParallaxSource.IntSource} or
 * {@link ParallaxSource.FloatSource}. App may subclass {@link ParallaxSource.IntProperty} or
 * {@link ParallaxSource.FloatProperty} to supply additional information about how to retrieve
 * Property value.  For reference implementation, see {@link ParallaxRecyclerViewSource}.
 *
 * <p>
 * <ul>Restrictions
 * <li>Values must be in ascending order.</li>
 * <li>If the UI element is unknown above screen, use UNKNOWN_BEFORE.</li>
 * <li>if the UI element is unknown below screen, use UNKNOWN_AFTER.</li>
 * <li>UNKNOWN_BEFORE and USE_UNKNOWN_AFTER are not allowed to be next to each other.</li>
 * </ul>
 *
 * These rules can be verified by {@link #verifyProperties()}.
 * </p>
 * @hide
 */
public abstract class ParallaxSource<PropertyT extends Property> {

    /**
     * Listener for tracking Property value changes.
     */
    public static abstract class Listener {
        /**
         * Called when the value for any of the property in ParallaxSource changes.
         */
        public void onPropertiesChanged(ParallaxSource source) {
        }
    }

    /**
     * Class holding a fixed key value for a Property in {@link ParallaxSource}.
     * Base class for {@link IntPropertyKeyValue} and {@link FloatPropertyKeyValue}.
     */
    public static class PropertyKeyValue<PropertyT extends Property> {
        private final PropertyT mProperty;

        public PropertyKeyValue(PropertyT property) {
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
     * IntProperty provide access to an index based integer type property inside {@link IntSource}.
     * The IntProperty typically represents UI element position inside {@link IntSource}.
     */
    public static class IntProperty extends Property<IntSource, Integer> {

        /**
         * Property value is unknown and it's above screen.
         */
        public static final int UNKNOWN_BEFORE = Integer.MIN_VALUE;

        /**
         * Property value is unknown and it's bellow screen.
         */
        public static final int UNKNOWN_AFTER = Integer.MAX_VALUE;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param name Name of this Property.
         * @param index Index of this Property inside {@link IntSource}.
         */
        public IntProperty(String name, int index) {
            super(Integer.class, name);
            mIndex = index;
        }

        @Override
        public final Integer get(IntSource object) {
            return getIntValue(object);
        }

        @Override
        public final void set(IntSource object, Integer value) {
            setIntValue(object, value);
        }

        final int getIntValue(IntSource source) {
            return source.getPropertyValue(mIndex);
        }

        final void setIntValue(IntSource source, int value) {
            source.setPropertyValue(mIndex, value);
        }

        /**
         * @return Index of this Property in {@link IntSource}.
         */
        public final int getIndex() {
            return mIndex;
        }

        /**
         * Creates an {@link IntPropertyKeyValue} object for the absolute keyValue.
         *
         * @param keyValue The integer key value.
         * @return A new {@link IntPropertyKeyValue} object.
         */
        public final IntPropertyKeyValue atAbsolute(int keyValue) {
            return new IntPropertyKeyValue(this, keyValue, 0f);
        }

        /**
         * Creates an {@link IntPropertyKeyValue} object for a fraction of
         * {@link IntSource#getMaxParentVisibleSize()}.
         *
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link IntSource#getMaxParentVisibleSize()} for
         *                                       the key value.
         * @return A new {@link IntPropertyKeyValue} object.
         */
        public final IntPropertyKeyValue atFraction(float fractionOfMaxParentVisibleSize) {
            return new IntPropertyKeyValue(this, 0, fractionOfMaxParentVisibleSize);
        }

        /**
         * Create an {@link IntPropertyKeyValue} object by multiplying the fraction with
         * {@link IntSource#getMaxParentVisibleSize()} and adding offsetValue to it.
         *
         * @param offsetValue                    An offset integer value to be added to key value.
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link IntSource#getMaxParentVisibleSize()} for
         *                                       the key value.
         * @return A new {@link IntPropertyKeyValue} object.
         */
        public final IntPropertyKeyValue at(int offsetValue,
                float fractionOfMaxParentVisibleSize) {
            return new IntPropertyKeyValue(this, offsetValue, fractionOfMaxParentVisibleSize);
        }
    }

    /**
     * Manages a list of {@link IntProperty}. App should override this class with a specific
     * {@link IntProperty} subclass.
     *
     * @param <IntPropertyT> Type of {@link IntProperty} or subclass.
     */
    public abstract static class IntSource<IntPropertyT extends IntProperty>
            extends ParallaxSource<IntPropertyT> {

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
            mValues[index] = value;
        }

        /**
         * Return the size of parent visible area, e.g. parent view's height if we are tracking Y
         * position of a child. The size can be used to calculate key value using the provided
         * fraction.
         *
         * @return Size of parent visible area.
         * @see IntPropertyKeyValue
         */
        public abstract int getMaxParentVisibleSize();

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
                                    + " UNKNOW_BEFORE and Property[%d]\"%s\" is UNKNOWN_AFTER",
                            i - 1, mProperties.get(i - 1).getName(),
                            i, mProperties.get(i).getName()));
                }
                last = v;
            }
        }

    }

    /**
     * Implementation of {@link PropertyKeyValue} for {@link IntProperty}.
     */
    public static class IntPropertyKeyValue extends PropertyKeyValue<IntProperty> {
        private final int mValue;
        private final float mFactionOfMax;

        public IntPropertyKeyValue(IntProperty property, int value) {
            this(property, value, 0f);
        }

        public IntPropertyKeyValue(IntProperty property, int value, float fractionOfMax) {
            super(property);
            mValue = value;
            mFactionOfMax = fractionOfMax;
        }

        /**
         * @return The key value of integer type.
         */
        public final int getKeyValue(IntSource source) {
            return mFactionOfMax == 0 ? mValue : mValue + Math.round(source
                    .getMaxParentVisibleSize() * mFactionOfMax);
        }
    }

    /**
     * FloatProperty provide access to an index based integer type property inside
     * {@link FloatSource}. The FloatProperty typically represents UI element position inside
     * {@link FloatSource}.
     */
    public static class FloatProperty extends Property<FloatSource, Float> {

        /**
         * Property value is unknown and it's above screen.
         */
        public static final float UNKNOWN_BEFORE = -Float.MAX_VALUE;
        /**
         * Property value is unknown and it's bellow screen.
         */
        public static final float UNKNOWN_AFTER = Float.MAX_VALUE;

        private final int mIndex;

        /**
         * Constructor.
         *
         * @param name Name of this Property.
         * @param index Index of this Property inside {@link FloatSource}.
         */
        public FloatProperty(String name, int index) {
            super(Float.class, name);
            mIndex = index;
        }

        @Override
        public final Float get(FloatSource object) {
            return getFloatValue(object);
        }

        @Override
        public final void set(FloatSource object, Float value) {
            setFloatValue(object, value);
        }

        final float getFloatValue(FloatSource source) {
            return source.getPropertyValue(mIndex);
        }

        final void setFloatValue(FloatSource source, float value) {
            source.setPropertyValue(mIndex, value);
        }

        /**
         * @return Index of this Property in {@link FloatSource}.
         */
        public final int getIndex() {
            return mIndex;
        }

        /**
         * Creates an {@link FloatPropertyKeyValue} object for the absolute keyValue.
         *
         * @param keyValue The float key value.
         * @return A new {@link FloatPropertyKeyValue} object.
         */
        public final FloatPropertyKeyValue atAbsolute(float keyValue) {
            return new FloatPropertyKeyValue(this, keyValue, 0f);
        }

        /**
         * Creates an {@link FloatPropertyKeyValue} object for a fraction of
         * {@link FloatSource#getMaxParentVisibleSize()}.
         *
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link FloatSource#getMaxParentVisibleSize()} for
         *                                       the key value.
         * @return A new {@link FloatPropertyKeyValue} object.
         */
        public final FloatPropertyKeyValue atFraction(float fractionOfMaxParentVisibleSize) {
            return new FloatPropertyKeyValue(this, 0, fractionOfMaxParentVisibleSize);
        }

        /**
         * Create an {@link FloatPropertyKeyValue} object by multiplying the fraction with
         * {@link FloatSource#getMaxParentVisibleSize()} and adding offsetValue to it.
         *
         * @param offsetValue                    An offset float value to be added to key value.
         * @param fractionOfMaxParentVisibleSize 0 to 1 fraction to multiply with
         *                                       {@link FloatSource#getMaxParentVisibleSize()} for
         *                                       the key value.
         * @return A new {@link FloatPropertyKeyValue} object.
         */
        public final FloatPropertyKeyValue at(float offsetValue,
                float fractionOfMaxParentVisibleSize) {
            return new FloatPropertyKeyValue(this, offsetValue, fractionOfMaxParentVisibleSize);
        }
    }

    /**
     * Manages a list of {@link FloatProperty}. App should override this class with a specific
     * {@link FloatProperty} subclass.
     *
     * @param <FloatPropertyT> Type of {@link FloatProperty} or subclass.
     */
    public abstract static class FloatSource<FloatPropertyT extends FloatProperty> extends
            ParallaxSource<FloatPropertyT> {

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
            mValues[index] = value;
        }

        /**
         * Return the size of parent visible area, e.g. parent view's height if we are tracking Y
         * position of a child. The size can be used to calculate key value using the provided
         * fraction.
         *
         * @return Size of parent visible area.
         * @see FloatPropertyKeyValue
         */
        public abstract float getMaxParentVisibleSize();

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
                                    + " UNKNOW_BEFORE and Property[%d]\"%s\" is UNKNOWN_AFTER",
                            i - 1, mProperties.get(i - 1).getName(),
                            i, mProperties.get(i).getName()));
                }
                last = v;
            }
        }

    }

    /**
     * Implementation of {@link PropertyKeyValue} for {@link FloatProperty}.
     */
    public static class FloatPropertyKeyValue extends PropertyKeyValue<FloatProperty> {
        private final float mValue;
        private final float mFactionOfMax;

        public FloatPropertyKeyValue(FloatProperty property, float value) {
            this(property, value, 0f);
        }

        public FloatPropertyKeyValue(FloatProperty property, float value, float fractionOfMax) {
            super(property);
            mValue = value;
            mFactionOfMax = fractionOfMax;
        }

        /**
         * @return The key value.
         */
        public final float getKeyValue(FloatSource source) {
            return mFactionOfMax == 0 ? mValue : mValue + source.getMaxParentVisibleSize()
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
     * Add a new Property in the ParallaxSource.
     *
     * @return Newly created Property.
     */
    public abstract PropertyT addProperty(String name);

    /**
     * Create a new Property object. App does not directly call this method.  See
     * {@link #addProperty(String)}.
     *
     * @param index  Index of the property in this ParallaxSource object.
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
     * Sets listener to monitor property value changes.
     *
     * @param listener The listener to set on {@link ParallaxSource} object.
     */
    public abstract void setListener(Listener listener);

    /**
     * This is used when the source is unaware of the updates and requires caller to update the
     * values at the right time.
     */
    public void updateValues() {
    }
}
