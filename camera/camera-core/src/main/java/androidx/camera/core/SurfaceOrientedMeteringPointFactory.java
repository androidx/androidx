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
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A {@link MeteringPointFactory} that can create {@link MeteringPoint} by surface oriented x, y
 * on an area defined by (0, 0) - (width, height). {@link MeteringPoint} can then be used to
 * construct a {@link FocusMeteringAction} to start a focus and metering action.
 *
 * <p>The {@link MeteringPoint} defines a normalized coordinate system whose left-top is (0, 0)
 * and right-bottom is (1.0, 1.0). This coordinate system is the normalized coordinate
 * system of a {@link Surface} of certain aspect ratio.
 * {@link SurfaceOrientedMeteringPointFactory} is the simplest factory to create this normalized
 * (x, y) by dividing the (x, y) with (width, height).
 *
 * <p>This factory is suitable for apps that already have coordinates converted into surface
 * oriented coordinates. It is also useful for apps that want to focus on something detected in
 * {@link ImageAnalysis}. Apps can pass the {@link ImageAnalysis} instance for useCaseForSurface
 * argument and CameraX will then adjust the final sensor coordinates by aspect ratio of
 * ImageAnalysis.
 *
 * @see MeteringPoint
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class SurfaceOrientedMeteringPointFactory extends MeteringPointFactory {
    /** the width of the area in surface orientation */
    private final float mWidth;
    /** The height of the area in surface orientation */
    private final float mHeight;

    /**
     * Creates the {@link SurfaceOrientedMeteringPointFactory} by width and height
     *
     * <p>The width/height is the width/height in surface orientation which defines an area (0, 0)
     * - (width, height) within which apps can specify metering points by
     * {@link #createPoint(float, float)}. Setting width and height to 1.0 will allow points to
     * be created by specifying normalized coordinates.
     *
     * <p>By default, it will use active {@link Preview} to get the surface aspect ratio for final
     * coordinates conversion.
     *
     * @param width the width of the area in surface orientation.
     * @param height the height of the area in surface orientation.
     */
    public SurfaceOrientedMeteringPointFactory(float width, float height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Creates the {@link SurfaceOrientedMeteringPointFactory} by width, height and the surface
     * aspect ratio. The surface aspect ratio is retrieved from the {@link UseCase}.
     *
     * <p>The width/height is the width/height in surface orientation which defines an
     * area (0, 0) - (width, height) within which apps can specify metering points by
     * {@link #createPoint(float, float)}. Setting width and height to 1.0 will allow points to
     * be created by specifying normalized coordinates.
     *
     * <p>A {@link UseCase} is passed in order to determine the surface aspect ratio for final
     * coordinates conversion. This use case needs to be bound at the time this method is called,
     * otherwise an {@link IllegalStateException} will be thrown.
     *
     * @param width the width of the area in surface orientation.
     * @param height the height of the area in surface orientation.
     * @param useCaseForAspectRatio the {@link UseCase} to get the surface aspect ratio.
     */
    public SurfaceOrientedMeteringPointFactory(float width, float height,
            @NonNull UseCase useCaseForAspectRatio) {
        super(getUseCaseAspectRatio(useCaseForAspectRatio));
        mWidth = width;
        mHeight = height;
    }

    @Nullable
    private static Rational getUseCaseAspectRatio(@Nullable UseCase useCase) {
        if (useCase == null) {
            return null;
        }

        Size resolution = useCase.getAttachedSurfaceResolution();
        if (resolution == null) {
            throw new IllegalStateException("UseCase " + useCase + " is not bound.");
        }

        // Returns an aspect ratio of first found attachedSurfaceResolution.
        return new Rational(resolution.getWidth(), resolution.getHeight());
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    protected PointF convertPoint(float x, float y) {
        PointF pt = new PointF(x / mWidth, y / mHeight);
        return pt;
    }
}
