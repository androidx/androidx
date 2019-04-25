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

package androidx.camera.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraOrientationUtil;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageOutputConfig.RotationValue;
import androidx.core.util.Preconditions;

/** Implementation of the {@link CameraInfo} interface that exposes parameters through camera2. */
final class Camera2CameraInfo implements CameraInfo {

    private final CameraCharacteristics mCameraCharacteristics;
    private static final String TAG = "Camera2CameraInfo";

    Camera2CameraInfo(CameraManager cameraManager, String cameraId)
            throws CameraInfoUnavailableException {
        try {
            mCameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new CameraInfoUnavailableException(
                    "Unable to retrieve info for camera " + cameraId, e);
        }

        checkCharacteristicAvailable(
                CameraCharacteristics.SENSOR_ORIENTATION, "Sensor orientation");
        checkCharacteristicAvailable(CameraCharacteristics.LENS_FACING, "Lens facing direction");
        checkCharacteristicAvailable(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, "Supported hardware level");
        logDeviceInfo();
    }

    @Nullable
    @Override
    public LensFacing getLensFacing() {
        Integer lensFacing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        Preconditions.checkNotNull(lensFacing);
        switch (lensFacing) {
            case CameraCharacteristics.LENS_FACING_FRONT:
                return LensFacing.FRONT;
            case CameraCharacteristics.LENS_FACING_BACK:
                return LensFacing.BACK;
            default:
                return null;
        }
    }

    @Override
    public int getSensorRotationDegrees(@RotationValue int relativeRotation) {
        Integer sensorOrientation =
                mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Preconditions.checkNotNull(sensorOrientation);
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation);
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        boolean isOppositeFacingScreen = LensFacing.BACK.equals(getLensFacing());
        return CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                sensorOrientation,
                isOppositeFacingScreen);
    }

    int getSupportedHardwareLevel() {
        Integer deviceLevel =
                mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        Preconditions.checkNotNull(deviceLevel);
        return deviceLevel;
    }

    private void checkCharacteristicAvailable(CameraCharacteristics.Key<?> key, String readableName)
            throws CameraInfoUnavailableException {
        if (mCameraCharacteristics.get(key) == null) {
            throw new CameraInfoUnavailableException(
                    "Camera characteristics map is missing value for characteristic: "
                            + readableName);
        }
    }

    @Override
    public int getSensorRotationDegrees() {
        return getSensorRotationDegrees(Surface.ROTATION_0);
    }

    private void logDeviceInfo() {
        // Extend by adding logging here as needed.
        logDeviceLevel();
    }

    private void logDeviceLevel() {
        String levelString;

        int deviceLevel = getSupportedHardwareLevel();
        switch (deviceLevel) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_FULL";
                break;
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                levelString = "INFO_SUPPORTED_HARDWARE_LEVEL_3";
                break;
            default:
                levelString = "Unknown value: " + deviceLevel;
                break;
        }
        Log.i(TAG, "Device Level: " + levelString);
    }
}
