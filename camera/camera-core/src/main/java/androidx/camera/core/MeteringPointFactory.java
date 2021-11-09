/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.graphics.PointF;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A factory to create a {@link MeteringPoint}.
 *
 * <p>Users can call {@link #createPoint(float, float)} to
 * create a {@link MeteringPoint} with x, y, default size. There is also another
 * variant, {@link #createPoint(float, float, float)} for apps that want to also specify size.
 *
 * @see MeteringPoint
 * @see #createPoint(float, float)
 * @see #createPoint(float, float, float)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class MeteringPointFactory {

    /**
     * Surface aspect ratio used to created {@link MeteringPoint}s. Null for using Preview
     * aspect ratio.
     *
     * @see MeteringPoint#getSurfaceAspectRatio()
     */
    @Nullable
    private Rational mSurfaceAspectRatio;

    /**
     * Constructor that use Preview aspect ratio for {@link MeteringPoint}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public MeteringPointFactory() {
        this(null);
    }

    /**
     * Constructor that takes a custom surface aspect ratio for {@link MeteringPoint}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public MeteringPointFactory(@Nullable Rational surfaceAspectRatio) {
        mSurfaceAspectRatio = surfaceAspectRatio;
    }

    /**
     * Returns default point size. It is the default size of the MeteringPoint width and height
     * (ranging from 0 to 1) which is a (normalized) percentage of the sensor width/height (or crop
     * region width/height if crop region is set).
     *
     * @see MeteringPoint#getSize()
     */
    public static float getDefaultPointSize() {
        // width of MeteringPoint = 0.15 * cropRegion.width
        // height of MeteringPoint = 0.15 * cropRegion.height
        return 0.15f;
    }

    /**
     * Convert a (x, y) into the normalized surface (x, y) which can then be converted to sensor
     * coordinates by {@link CameraControl}.
     *
     * <p>The meaning of (x, y) is defined by {@link MeteringPointFactory} implementations. It is
     * tailored by specific needs. For example, when performing focus and metering on a point
     * in preview,  the (x, y) could be defined as (x, y) in a View. Each implementations is
     * responsible to convert this (x, y) into normalized surface coordinates.
     *
     * Implementation must implement this method for coordinates conversion.
     *
     * @param x x to be converted.
     * @param y y to be converted.
     * @return a {@link PointF} consisting of converted normalized surface coordinates.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    protected abstract PointF convertPoint(float x, float y);

    /**
     * Creates a {@link MeteringPoint} by x, y.
     *
     * <p>The (x, y) is a position from the area defined by the specific
     * {@link MeteringPointFactory} implementation, such as
     * {@link DisplayOrientedMeteringPointFactory} or {@link SurfaceOrientedMeteringPointFactory}.
     *
     * @param x x to be converted.
     * @param y y to be converted.
     * @return A {@link MeteringPoint} that is converted into normalized surface (x, y).
     * @see DisplayOrientedMeteringPointFactory
     * @see SurfaceOrientedMeteringPointFactory
     */
    @NonNull
    public final MeteringPoint createPoint(float x, float y) {
        return createPoint(x, y, getDefaultPointSize());
    }

    /**
     * Creates a {@link MeteringPoint} by x, y, size.
     *
     * <p>The (x, y) is a position from the area defined by the specific
     * {@link MeteringPointFactory} implementation, such as
     * {@link DisplayOrientedMeteringPointFactory} or {@link SurfaceOrientedMeteringPointFactory}.
     *
     * @param x    x to be converted.
     * @param y    y to be converted.
     * @param size size of the MeteringPoint width and height(ranging from 0 to 1). It is the
     *             (normalized) percentage of the sensor width/height (or crop region
     *             width/height if crop region is set).
     * @return A {@link MeteringPoint} that is converted into normalized surface (x, y).
     * @see DisplayOrientedMeteringPointFactory
     * @see SurfaceOrientedMeteringPointFactory
     */
    @NonNull
    public final MeteringPoint createPoint(float x, float y, float size) {
        PointF convertedPoint = convertPoint(x, y);
        return new MeteringPoint(convertedPoint.x, convertedPoint.y, size, mSurfaceAspectRatio);
    }
}
