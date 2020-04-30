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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.ImageOutputConfig.RotationValue;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;

import java.util.concurrent.Executor;

/**
 * Implementation of the {@link CameraInfoInternal} interface that exposes parameters through
 * camera2.
 */
public final class Camera2CameraInfoImpl implements CameraInfoInternal {

    private static final String TAG = "Camera2CameraInfo";
    private final String mCameraId;
    private final CameraCharacteristics mCameraCharacteristics;
    private final Camera2CameraControl mCamera2CameraControl;
    private final ZoomControl mZoomControl;
    private final TorchControl mTorchControl;

    Camera2CameraInfoImpl(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Camera2CameraControl camera2CameraControl) {
        Preconditions.checkNotNull(cameraCharacteristics, "Camera characteristics map is missing");
        mCameraId = Preconditions.checkNotNull(cameraId);
        mCameraCharacteristics = cameraCharacteristics;
        mCamera2CameraControl = camera2CameraControl;
        mZoomControl = camera2CameraControl.getZoomControl();
        mTorchControl = camera2CameraControl.getTorchControl();
        logDeviceInfo();
    }

    @NonNull
    @Override
    public String getCameraId() {
        return mCameraId;
    }

    @Nullable
    @Override
    public Integer getLensFacing() {
        Integer lensFacing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
        Preconditions.checkNotNull(lensFacing);
        switch (lensFacing) {
            case CameraCharacteristics.LENS_FACING_FRONT:
                return CameraSelector.LENS_FACING_FRONT;
            case CameraCharacteristics.LENS_FACING_BACK:
                return CameraSelector.LENS_FACING_BACK;
            default:
                return null;
        }
    }

    @Override
    public int getSensorRotationDegrees(@RotationValue int relativeRotation) {
        Integer sensorOrientation = getSensorOrientation();
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation);
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        final Integer lensFacing = getLensFacing();
        boolean isOppositeFacingScreen =
                (lensFacing != null && CameraSelector.LENS_FACING_BACK == lensFacing);
        return CameraOrientationUtil.getRelativeImageRotation(
                relativeRotationDegrees,
                sensorOrientation,
                isOppositeFacingScreen);
    }

    int getSensorOrientation() {
        Integer sensorOrientation =
                mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        Preconditions.checkNotNull(sensorOrientation);
        return sensorOrientation;
    }

    int getSupportedHardwareLevel() {
        Integer deviceLevel =
                mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        Preconditions.checkNotNull(deviceLevel);
        return deviceLevel;
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

    @Override
    public boolean hasFlashUnit() {
        Boolean hasFlashUnit = mCameraCharacteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE);
        Preconditions.checkNotNull(hasFlashUnit);
        return hasFlashUnit;
    }

    @NonNull
    @Override
    public LiveData<Integer> getTorchState() {
        return mTorchControl.getTorchState();
    }

    @NonNull
    @Override
    public LiveData<ZoomState> getZoomState() {
        return mZoomControl.getZoomState();
    }

    /**
     * {@inheritDoc}
     *
     * <p>When the CameraX configuration is {@link androidx.camera.camera2.Camera2Config}, the
     * return value depends on whether the device is legacy
     * ({@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL} {@code ==
     * }{@link CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}).
     *
     * @return {@link #IMPLEMENTATION_TYPE_CAMERA2_LEGACY} if the device is legacy, otherwise
     * {@link #IMPLEMENTATION_TYPE_CAMERA2}.
     */
    @NonNull
    @Override
    public String getImplementationType() {
        final int hardwareLevel = getSupportedHardwareLevel();
        return hardwareLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                ? IMPLEMENTATION_TYPE_CAMERA2_LEGACY : IMPLEMENTATION_TYPE_CAMERA2;
    }

    @Override
    public void addSessionCaptureCallback(@NonNull Executor executor,
            @NonNull CameraCaptureCallback callback) {
        mCamera2CameraControl.addSessionCameraCaptureCallback(executor, callback);
    }

    @Override
    public void removeSessionCaptureCallback(@NonNull CameraCaptureCallback callback) {
        mCamera2CameraControl.removeSessionCameraCaptureCallback(callback);
    }
}
