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

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.util.Property;

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
class PathProperty<T> extends Property<T, Float> {

    private final Property<T, PointF> mProperty;
    private final PathMeasure mPathMeasure;
    private final float mPathLength;
    private final float[] mPosition = new float[2];
    private final PointF mPointF = new PointF();
    private float mCurrentFraction;

    PathProperty(Property<T, PointF> property, Path path) {
        super(Float.class, property.getName());
        mProperty = property;
        mPathMeasure = new PathMeasure(path, false);
        mPathLength = mPathMeasure.getLength();
    }

    @Override
    public Float get(T object) {
        return mCurrentFraction;
    }

    @Override
    public void set(T target, Float fraction) {
        mCurrentFraction = fraction;
        mPathMeasure.getPosTan(mPathLength * fraction, mPosition, null);
        mPointF.x = mPosition[0];
        mPointF.y = mPosition[1];
        mProperty.set(target, mPointF);
    }

}
