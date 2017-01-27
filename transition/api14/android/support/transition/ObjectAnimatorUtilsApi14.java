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

package android.support.transition;

import android.animation.ObjectAnimator;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.annotation.RequiresApi;
import android.util.Property;

@RequiresApi(14)
class ObjectAnimatorUtilsApi14 implements ObjectAnimatorUtilsImpl {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectAnimator ofInt(T target, String xPropertyName, String yPropertyName,
            Path path) {
        Class<?> hostType = target.getClass();
        Property<T, Integer> px = (Property<T, Integer>) Property.of(hostType, Integer.class,
                xPropertyName);
        Property<T, Integer> py = (Property<T, Integer>) Property.of(hostType, Integer.class,
                yPropertyName);
        return ObjectAnimator.ofFloat(target,
                new PathProperty<>(
                        new CastIntegerProperty<>(px),
                        new CastIntegerProperty<>(py),
                        path),
                0f, 1f);
    }

    @Override
    public <T> ObjectAnimator ofInt(T target, Property<T, Integer> xProperty,
            Property<T, Integer> yProperty, Path path) {
        return ObjectAnimator.ofFloat(target,
                new PathProperty<>(
                        new CastIntegerProperty<>(xProperty),
                        new CastIntegerProperty<>(yProperty),
                        path),
                0f, 1f);
    }

    @Override
    public <T> ObjectAnimator ofFloat(T target, Property<T, Float> xProperty,
            Property<T, Float> yProperty, Path path) {
        return ObjectAnimator.ofFloat(target,
                new PathProperty<>(xProperty, yProperty, path));
    }

    /**
     * Converts a {@link Property} with Integer value type to a {@link Property} with a Float
     * value type.
     *
     * @param <T> The target type
     */
    private static class CastIntegerProperty<T> extends Property<T, Float> {

        private final Property<T, Integer> mProperty;

        CastIntegerProperty(Property<T, Integer> property) {
            super(Float.class, property.getName());
            mProperty = property;
        }

        @Override
        public Float get(T object) {
            return (float) mProperty.get(object);
        }

        @Override
        public void set(T object, Float value) {
            mProperty.set(object,
                    // This cannot be a simple cast to make animations pixel perfect.
                    Math.round(value));
        }

    }

    /**
     * A special {@link Property} that can animate a pair of properties bi-dimensionally along the
     * specified path.
     * <p>
     * This property should always be used with Animator that sets float fractions between
     * {@code 0.f} and {@code 1.f}. For example, setting {@code 0.5f} to this property sets the
     * values right in the middle of the specified path to the underlying properties.
     * <p>
     * Unlike many of the platform built-in properties, instances of this class cannot be reused
     * for later animations.
     */
    private static class PathProperty<T> extends Property<T, Float> {

        private final PathMeasure mPathMeasure;
        private final float mPathLength;
        private final float[] mPosition = new float[2];
        private final Property<T, Float> mXProperty;
        private final Property<T, Float> mYProperty;
        private float mCurrentFraction;

        PathProperty(Property<T, Float> xProperty, Property<T, Float> yProperty, Path path) {
            super(Float.class, xProperty.getName() + "/" + yProperty.getName());
            mXProperty = xProperty;
            mYProperty = yProperty;
            mPathMeasure = new PathMeasure(path, false);
            mPathLength = mPathMeasure.getLength();
        }

        @Override
        public Float get(T t) {
            return mCurrentFraction;
        }

        @Override
        public void set(T view, Float fraction) {
            mCurrentFraction = fraction;
            mPathMeasure.getPosTan(mPathLength * fraction, mPosition, null);
            mXProperty.set(view, mPosition[0]);
            mYProperty.set(view, mPosition[1]);
        }

    }

}
