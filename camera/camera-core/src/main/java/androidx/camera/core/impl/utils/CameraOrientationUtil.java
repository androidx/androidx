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

package androidx.camera.core.impl.utils;

import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;

/**
 * Contains utility methods related to camera orientation.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraOrientationUtil {
    private static final String TAG = "CameraOrientationUtil";

    // Do not allow instantiation
    private CameraOrientationUtil() {
    }

    /**
     * Calculates the delta between a source rotation and destination rotation.
     *
     * <p>A typical use of this method would be calculating the angular difference between the
     * display orientation (destRotationDegrees) and camera sensor orientation
     * (sourceRotationDegrees).
     *
     * @param destRotationDegrees   The destination rotation relative to the device's natural
     *                              rotation.
     * @param sourceRotationDegrees The source rotation relative to the device's natural rotation.
     * @param isOppositeFacing      Whether the source and destination planes are facing opposite
     *                              directions.
     */
    public static int getRelativeImageRotation(
            int destRotationDegrees, int sourceRotationDegrees, boolean isOppositeFacing) {
        int result;
        if (isOppositeFacing) {
            result = (sourceRotationDegrees - destRotationDegrees + 360) % 360;
        } else {
            result = (sourceRotationDegrees + destRotationDegrees) % 360;
        }
        if (Logger.isDebugEnabled(TAG)) {
            Logger.d(
                    TAG,
                    String.format(
                            "getRelativeImageRotation: destRotationDegrees=%s, "
                                    + "sourceRotationDegrees=%s, isOppositeFacing=%s, "
                                    + "result=%s",
                            destRotationDegrees, sourceRotationDegrees, isOppositeFacing, result));
        }
        return result;
    }

    /**
     * Converts rotation constant values defined in {@link Surface} to their equivalent in
     * degrees.
     *
     * <p>Valid values for the relative rotation are {@link Surface#ROTATION_0}, {@link
     * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     *
     * @param rotation One of the rotation constant values from {@link Surface}.
     * @return The equivalent rotation value in degrees.
     * @throws IllegalArgumentException If the provided rotation value is not one of those
     * defined in {@link Surface}.
     */
    public static int surfaceRotationToDegrees(@RotationValue int rotation) {
        int rotationDegrees;
        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDegrees = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = 90;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = 180;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = 270;
                break;
            default:
                throw new IllegalArgumentException("Unsupported surface rotation: " + rotation);
        }

        return rotationDegrees;
    }

    /**
     * Converts rotation degrees to their equivalent in values defined in {@link Surface}.
     *
     * <p>Valid values for the relative rotation are {@link Surface#ROTATION_0}, {@link
     * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     *
     * @param degrees The rotation value in degrees.
     * @return One of the constant rotation values defined in {@link Surface}.
     * @throws IllegalArgumentException If the provided rotation degrees doesn't fall into any
     *                                  one of those defined in {@link Surface}.
     */
    @RotationValue
    public static int degreesToSurfaceRotation(int degrees) {
        int surfaceRotation = Surface.ROTATION_0;
        switch (degrees) {
            case 0:
                surfaceRotation = Surface.ROTATION_0;
                break;
            case 90:
                surfaceRotation = Surface.ROTATION_90;
                break;
            case 180:
                surfaceRotation = Surface.ROTATION_180;
                break;
            case 270:
                surfaceRotation = Surface.ROTATION_270;
                break;
            default:
                throw new IllegalStateException("Invalid sensor rotation: " + degrees);
        }

        return surfaceRotation;
    }
}
