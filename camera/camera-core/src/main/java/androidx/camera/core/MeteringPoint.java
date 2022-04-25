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

import android.util.Rational;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * A {@link MeteringPoint} is used to specify a region which can then be converted to sensor
 * coordinate system for focus and metering purpose.
 *
 * <p>Conceptually, a {@link MeteringPoint} is a opaque handle to a metering point created by a
 * {@link MeteringPointFactory}, for use when building a {@link FocusMeteringAction}. The
 * coordinates of the point are specified by the application when creating points from the
 * factory, and then the coordinates are converted into an internal representation stored by this
 * class. Because of the nature of internal representation, the X and Y of the
 * {@link MeteringPoint} is not publicly visible. These are for internal use only.
 *
 * <p>When a {@link FocusMeteringAction} is submitted via
 * {@link CameraControl#startFocusAndMetering(FocusMeteringAction)}, the {@link MeteringPoint} is
 * converted to a point in the sensor coordinate system where it defines the center of a metering
 * rectangle. If zoom is applied via {@link CameraControl} , it will set a crop region calculated
 * by the zoom and the final coordinates will be mapped into the crop region. If not set, it is
 * mapped to the sensor active array.
 *
 * <p>Besides defining the center point of the metering rectangle, there is also the size of the
 * {@link MeteringPoint}. The size of the {@link MeteringPoint} ranges from 0 to 1.0.
 * The size is the percentage of sensor width and height (or crop region width/height if
 * crop region is set). See formula below:
 * <p><pre>Metering rectangle width = size * sensorSizeOrCropRegion.width
 * Metering rectangle height = size * sensorSizeOrCropRegion.height
 * </pre>
 * The metering rectangle defined by the {@link MeteringPoint} has the same shape as the sensor
 * array.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MeteringPoint {
    private float mNormalizedX;
    private float mNormalizedY;
    private float mSize;
    @Nullable
    private Rational mSurfaceAspectRatio; // null for preview aspect ratio.

    /**
     * Constructor is restricted for use within library.
     *
     * @param normalizedX        center X of the region in current normalized coordinate
     *                           system. (ranging from 0 to 1).
     * @param normalizedY        center Y of the region in current normalized coordinate
     *                           system. (ranging from 0 to 1).
     * @param size               size of the MeteringPoint width and height (ranging from 0 to
     *                           1). It is the percentage of the sensor width/height (or crop
     *                           region width/height if crop region is set).
     * @param surfaceAspectRatio If not null, use this as the Surface aspect ratio. Otherwise
     *                           use Preview's aspect ratio.
     */
    MeteringPoint(float normalizedX, float normalizedY, float size,
            @Nullable Rational surfaceAspectRatio) {
        mNormalizedX = normalizedX;
        mNormalizedY = normalizedY;
        mSize = size;
        mSurfaceAspectRatio = surfaceAspectRatio;
    }

    /**
     * center X of the region in current normalized surface coordinate system. (ranging from 0 to
     * 1).
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public float getX() {
        return mNormalizedX;
    }

    /**
     * center Y of the region in current normalized surface coordinate system. (ranging from 0 to
     * 1).
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public float getY() {
        return mNormalizedY;
    }

    /**
     * Size of the MeteringPoint width and height (ranging from 0 to 1). It is the percentage of
     * the sensor width/height (or crop region width/height if crop region is set).
     *
     * <p>Crop region is set when zoom is set in {@link CameraControl} and it is the region
     * inside the sensor active array and it defines the output region of the sensor.  See
     * formula below:
     *
     * <p><pre>Metering rectangle width = size * sensorSizeOrCropRegion.width
     * Metering rectangle height = size * sensorSizeOrCropRegion.height
     * </pre>
     */
    public float getSize() {
        return mSize;
    }

    /**
     * Get aspect ratio of the output {@link android.view.Surface} to be adjusted for final sensor
     * coordinates.
     *
     * <p>SurfaceAspectRatio is used when the output {@link android.view.Surface} is not preview.
     * Because not every output {@link android.view.Surface} has the same aspect ratio as the
     * sensor array, the output image could be cropped by the
     * <a href="https://source.android.com/devices/camera/camera3_crop_reprocess">rule</a>
     * . Therefore in order to correctly convert a normalized (x, y) of a certain
     * {@link android.view.Surface} to the sensor pixel array, we need to know the aspect ratio
     * of the {@link android.view.Surface}. For most cases Preview Surface is used because
     * normally user will focus and metering on a area from preview. Having surfaceAspectRatio
     * allows us to have the flexibility to support different aspect ratios of other
     * {@link UseCase}. For example, apps might want to focus on some area when analyzing images
     * in {@link ImageAnalysis}. If surfaceAspectRatio is null, then Preview aspect ratio will be
     * used. Otherwise, use the specified surfaceAspectRatio.
     *
     * @hide
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Rational getSurfaceAspectRatio() {
        return mSurfaceAspectRatio;
    }
}
