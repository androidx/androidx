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
import androidx.annotation.RestrictTo;

import java.util.Set;

/**
 * A {@link MeteringPointFactory} that can create {@link MeteringPoint} by surface oriented x, y
 * on a area defined by (0, 0) - (width, height). {@link MeteringPoint} can then be used to
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
public class SurfaceOrientedMeteringPointFactory extends MeteringPointFactory {
    /** the width of the area in surface orientation */
    private final float mWidth;
    /** he height of the area in surface orientation */
    private final float mHeight;

    /**
     * Creates the {@link SurfaceOrientedMeteringPointFactory} by width and height
     *
     * <p>The width/height is the width/height in surface orientation which defines a
     * area (0, 0) - (width, height) where apps can pick a point (x, y) to
     * {@link #createPoint(float, float)}. User can set the width and height to 1.0 to make the
     * (x, y) be normalized coordinates [0..1].
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
     * Creates the {@link SurfaceOrientedMeteringPointFactory} by width, height and
     * useCaseForAspectRatio.
     *
     * <p>The width/height is the width/height in surface orientation which defines a
     * area (0, 0) - (width, height) where apps can pick a point (x, y) to
     * {@link #createPoint(float, float)}. User can set the width and height to 1.0 to make the
     * (x, y) be normalized coordinates [0..1].
     *
     * <p>useCaseForSurface is used to determine the surface aspect ratio for for final
     * coordinates conversion. This useCaseForSurface needs to be bound via {@code CameraX
     * #bindToLifecycle(LifecycleOwner, CameraSelector, UseCase...)} first. Otherwise it will
     * throw a{@link IllegalStateException}.
     *
     * @param width the width of the area in surface orientation.
     * @param height the height of the area in surface orientation.
     * @param useCaseForSurface the {@link UseCase} to get the surface aspect ratio.
     */
    public SurfaceOrientedMeteringPointFactory(float width, float height,
            @NonNull UseCase useCaseForSurface) {
        super(getUseCaseAspectRatio(useCaseForSurface));
        mWidth = width;
        mHeight = height;
    }

    @Nullable
    private static Rational getUseCaseAspectRatio(@Nullable UseCase useCase) {
        if (useCase == null) {
            return null;
        }

        Set<String> cameraIds = useCase.getAttachedCameraIds();
        if (cameraIds.isEmpty()) {
            throw new IllegalStateException("UseCase " + useCase + " is not bound.");
        }

        for (String id : cameraIds) {
            Size resolution = useCase.getAttachedSurfaceResolution(id);
            // Returns an aspect ratio of first found attachedSurfaceResolution.
            return new Rational(resolution.getWidth(), resolution.getHeight());
        }

        return null;
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
