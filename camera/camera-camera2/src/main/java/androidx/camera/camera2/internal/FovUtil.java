/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;
import android.util.SizeF;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.impl.utils.TransformUtils;
import androidx.core.util.Preconditions;

/**
 * Contains utility methods related to view angle transformation.
 */
public class FovUtil {

    private static final String TAG = "FovUtil";

    // Do not allow instantiation.
    private FovUtil() {
    }

    /**
     * Calculates view angle by focal length and sensor length.
     *
     * <p>The returned view angle is inexact and might not be hundred percent accurate comparing
     * to the output image.
     *
     * <p>The returned view angle should between 0 and 360.
     */
    @IntRange(from = 0, to = 360)
    public static int focalLengthToViewAngleDegrees(float focalLength, float sensorLength) {
        Preconditions.checkArgument(focalLength > 0, "Focal length should be positive.");
        Preconditions.checkArgument(sensorLength > 0, "Sensor length should be positive.");

        int viewAngleDegrees = (int) Math.toDegrees(
                2 * Math.atan(sensorLength / (2 * focalLength)));
        Preconditions.checkArgumentInRange(viewAngleDegrees, 0, 360, "The provided focal length "
                + "and sensor length result in an invalid view angle degrees.");

        return viewAngleDegrees;
    }

    /**
     * Gets the angle of view of the default camera on the device.
     *
     * <p>The default cameras is the camera selected by
     * {@link CameraSelector#DEFAULT_FRONT_CAMERA} or {@link CameraSelector#DEFAULT_BACK_CAMERA}
     * depending on the specified lens facing.
     */
    public static int getDeviceDefaultViewAngleDegrees(@NonNull CameraManagerCompat cameraManager,
            @CameraSelector.LensFacing int lensFacing) {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristicsCompat cameraCharacteristics =
                        cameraManager.getCameraCharacteristicsCompat(cameraId);
                Integer cameraCharacteristicsLensFacing =
                        cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                Preconditions.checkNotNull(cameraCharacteristicsLensFacing,
                        "Lens facing can not be null");
                if (cameraCharacteristicsLensFacing == LensFacingUtil.getLensFacingInt(
                        lensFacing)) {
                    return focalLengthToViewAngleDegrees(
                            getDefaultFocalLength(cameraCharacteristics),
                            getSensorHorizontalLength(cameraCharacteristics));
                }
            }
        } catch (CameraAccessExceptionCompat e) {
            throw new IllegalArgumentException("Unable to get the default focal length.");
        }

        throw new IllegalArgumentException("Unable to get the default focal length with the "
                + "specified lens facing.");
    }

    /**
     * Gets the length of the horizontal side of the sensor.
     *
     * <p>The horizontal side is the width of the sensor size after rotated by the sensor
     * orientation.
     */
    public static float getSensorHorizontalLength(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        SizeF sensorSize =
                cameraCharacteristics.get(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        final Rect activeArrayRect =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Size pixelArraySize = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        final Integer sensorOrientation =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Preconditions.checkNotNull(sensorSize, "The sensor size can't be null.");
        Preconditions.checkNotNull(sensorOrientation, "The sensor orientation can't be "
                + "null.");
        Preconditions.checkNotNull(activeArrayRect, "The active array size can't be null.");
        Preconditions.checkNotNull(pixelArraySize, "The pixel array size can't be null.");

        Size activeArraySize = TransformUtils.rectToSize(activeArrayRect);
        if (TransformUtils.is90or270(sensorOrientation)) {
            sensorSize = TransformUtils.reverseSizeF(sensorSize);
            activeArraySize = TransformUtils.reverseSize(activeArraySize);
            pixelArraySize = TransformUtils.reverseSize(pixelArraySize);
        }

        return sensorSize.getWidth() * activeArraySize.getWidth() / pixelArraySize.getWidth();
    }

    /**
     * Gets the default focal length from a {@link CameraCharacteristics}.
     *
     * <p>If the camera is a logical camera that consists of multiple physical cameras, the
     * default focal length is the focal length of the physical camera that produces image at
     * zoom ratio {@code 1.0}.
     */
    public static float getDefaultFocalLength(
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        final float[] focalLengths =
                cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        Preconditions.checkNotNull(focalLengths, "The focal lengths can not be empty.");

        // Assume the first focal length is the default focal length. This will not be true if the
        // camera is a logical camera consist of multiple physical cameras and reports multiple
        // focal lengths. However for this kind of cameras, it's suggested to use zoom ratio to
        // do optical zoom.
        return focalLengths[0];
    }
}
