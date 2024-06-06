/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;

import java.nio.BufferUnderflowException;
import java.util.Map;

/**
 * A {@link CameraCaptureResult} backed by a Key/value map.
 */
class KeyValueMapCameraCaptureResult implements CameraCaptureResult {
    private static final String TAG = "KeyValueMapCameraCaptureResult";
    private final Map<CaptureResult.Key, Object> mKeyValues;
    private TagBundle mTagBundle;
    private final long mTimestamp;

    KeyValueMapCameraCaptureResult(long timestamp, @NonNull TagBundle tagBundle,
            @NonNull Map<CaptureResult.Key, Object> keyValues) {
        mKeyValues = keyValues;
        mTagBundle = tagBundle;
        mTimestamp = timestamp;
    }

    /**
     * Converts the camera2 {@link CaptureResult#CONTROL_AF_MODE} to
     * {@link CameraCaptureMetaData.AfMode}.
     *
     * @return the {@link CameraCaptureMetaData.AfMode}.
     */
    @NonNull
    @Override
    public CameraCaptureMetaData.AfMode getAfMode() {
        Integer mode = (Integer) mKeyValues.get(CaptureResult.CONTROL_AF_MODE);
        if (mode == null) {
            return CameraCaptureMetaData.AfMode.UNKNOWN;
        }
        switch (mode) {
            case CaptureResult.CONTROL_AF_MODE_OFF:
            case CaptureResult.CONTROL_AF_MODE_EDOF:
                return CameraCaptureMetaData.AfMode.OFF;
            case CaptureResult.CONTROL_AF_MODE_AUTO:
            case CaptureResult.CONTROL_AF_MODE_MACRO:
                return CameraCaptureMetaData.AfMode.ON_MANUAL_AUTO;
            case CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
            case CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
                return CameraCaptureMetaData.AfMode.ON_CONTINUOUS_AUTO;
            default: // fall out
        }
        Logger.e(TAG, "Undefined af mode: " + mode);
        return CameraCaptureMetaData.AfMode.UNKNOWN;
    }

    /**
     * Converts the camera2 {@link CaptureResult#CONTROL_AF_STATE} to
     * {@link CameraCaptureMetaData.AfState}.
     *
     * @return the {@link CameraCaptureMetaData.AfState}.
     */
    @NonNull
    @Override
    public CameraCaptureMetaData.AfState getAfState() {
        Integer state = (Integer) mKeyValues.get(CaptureResult.CONTROL_AF_STATE);
        if (state == null) {
            return CameraCaptureMetaData.AfState.UNKNOWN;
        }
        switch (state) {
            case CaptureResult.CONTROL_AF_STATE_INACTIVE:
                return CameraCaptureMetaData.AfState.INACTIVE;
            case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
            case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                return CameraCaptureMetaData.AfState.SCANNING;
            case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                return CameraCaptureMetaData.AfState.LOCKED_FOCUSED;
            case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                return CameraCaptureMetaData.AfState.LOCKED_NOT_FOCUSED;
            case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                return CameraCaptureMetaData.AfState.PASSIVE_NOT_FOCUSED;
            case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                return CameraCaptureMetaData.AfState.PASSIVE_FOCUSED;

            default: // fall out
        }
        Logger.e(TAG, "Undefined af state: " + state);
        return CameraCaptureMetaData.AfState.UNKNOWN;
    }

    /**
     * Converts the camera2 {@link CaptureResult#CONTROL_AE_STATE} to
     * {@link CameraCaptureMetaData.AeState}.
     *
     * @return the {@link CameraCaptureMetaData.AeState}.
     */
    @NonNull
    @Override
    public CameraCaptureMetaData.AeState getAeState() {
        Integer state = (Integer) mKeyValues.get(CaptureResult.CONTROL_AE_STATE);
        if (state == null) {
            return CameraCaptureMetaData.AeState.UNKNOWN;
        }
        switch (state) {
            case CaptureResult.CONTROL_AE_STATE_INACTIVE:
                return CameraCaptureMetaData.AeState.INACTIVE;
            case CaptureResult.CONTROL_AE_STATE_SEARCHING:
            case CaptureResult.CONTROL_AE_STATE_PRECAPTURE:
                return CameraCaptureMetaData.AeState.SEARCHING;
            case CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED:
                return CameraCaptureMetaData.AeState.FLASH_REQUIRED;
            case CaptureResult.CONTROL_AE_STATE_CONVERGED:
                return CameraCaptureMetaData.AeState.CONVERGED;
            case CaptureResult.CONTROL_AE_STATE_LOCKED:
                return CameraCaptureMetaData.AeState.LOCKED;
            default: // fall out
        }
        Logger.e(TAG, "Undefined ae state: " + state);
        return CameraCaptureMetaData.AeState.UNKNOWN;
    }

    /**
     * Converts the camera2 {@link CaptureResult#CONTROL_AWB_STATE} to
     * {@link CameraCaptureMetaData.AwbState}.
     *
     * @return the {@link CameraCaptureMetaData.AwbState}.
     */
    @NonNull
    @Override
    public CameraCaptureMetaData.AwbState getAwbState() {
        Integer state = (Integer) mKeyValues.get(CaptureResult.CONTROL_AWB_STATE);
        if (state == null) {
            return CameraCaptureMetaData.AwbState.UNKNOWN;
        }
        switch (state) {
            case CaptureResult.CONTROL_AWB_STATE_INACTIVE:
                return CameraCaptureMetaData.AwbState.INACTIVE;
            case CaptureResult.CONTROL_AWB_STATE_SEARCHING:
                return CameraCaptureMetaData.AwbState.METERING;
            case CaptureResult.CONTROL_AWB_STATE_CONVERGED:
                return CameraCaptureMetaData.AwbState.CONVERGED;
            case CaptureResult.CONTROL_AWB_STATE_LOCKED:
                return CameraCaptureMetaData.AwbState.LOCKED;
            default: // fall out
        }
        Logger.e(TAG, "Undefined awb state: " + state);
        return CameraCaptureMetaData.AwbState.UNKNOWN;
    }

    /**
     * Converts the camera2 {@link CaptureResult#FLASH_STATE} to
     * {@link CameraCaptureMetaData.FlashState}.
     *
     * @return the {@link CameraCaptureMetaData.FlashState}.
     */
    @NonNull
    @Override
    public CameraCaptureMetaData.FlashState getFlashState() {
        Integer state = (Integer) mKeyValues.get(CaptureResult.FLASH_STATE);
        if (state == null) {
            return CameraCaptureMetaData.FlashState.UNKNOWN;
        }
        switch (state) {
            case CaptureResult.FLASH_STATE_UNAVAILABLE:
            case CaptureResult.FLASH_STATE_CHARGING:
                return CameraCaptureMetaData.FlashState.NONE;
            case CaptureResult.FLASH_STATE_READY:
                return CameraCaptureMetaData.FlashState.READY;
            case CaptureResult.FLASH_STATE_FIRED:
            case CaptureResult.FLASH_STATE_PARTIAL:
                return CameraCaptureMetaData.FlashState.FIRED;
            default: // fall out
        }
        Logger.e(TAG, "Undefined flash state: " + state);
        return CameraCaptureMetaData.FlashState.UNKNOWN;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AeMode getAeMode() {
        Integer aeMode = (Integer) mKeyValues.get(CaptureResult.CONTROL_AE_MODE);
        if (aeMode == null) {
            return CameraCaptureMetaData.AeMode.UNKNOWN;
        }
        switch (aeMode) {
            case CaptureResult.CONTROL_AE_MODE_OFF:
                return CameraCaptureMetaData.AeMode.OFF;
            case CaptureResult.CONTROL_AE_MODE_ON:
                return CameraCaptureMetaData.AeMode.ON;
            case CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH:
                return CameraCaptureMetaData.AeMode.ON_AUTO_FLASH;
            case CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH:
                return CameraCaptureMetaData.AeMode.ON_ALWAYS_FLASH;
            case CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE:
                return CameraCaptureMetaData.AeMode.ON_AUTO_FLASH_REDEYE;
            case CaptureResult.CONTROL_AE_MODE_ON_EXTERNAL_FLASH:
                return CameraCaptureMetaData.AeMode.ON_EXTERNAL_FLASH;
            default:
                return CameraCaptureMetaData.AeMode.UNKNOWN;
        }
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AwbMode getAwbMode() {
        Integer awbMode = (Integer) mKeyValues.get(CaptureResult.CONTROL_AWB_MODE);
        if (awbMode == null) {
            return CameraCaptureMetaData.AwbMode.UNKNOWN;
        }
        switch (awbMode) {
            case CaptureResult.CONTROL_AWB_MODE_OFF:
                return CameraCaptureMetaData.AwbMode.OFF;
            case CaptureResult.CONTROL_AWB_MODE_AUTO:
                return CameraCaptureMetaData.AwbMode.AUTO;
            case CaptureResult.CONTROL_AWB_MODE_INCANDESCENT:
                return CameraCaptureMetaData.AwbMode.INCANDESCENT;
            case CaptureResult.CONTROL_AWB_MODE_FLUORESCENT:
                return CameraCaptureMetaData.AwbMode.FLUORESCENT;
            case CaptureResult.CONTROL_AWB_MODE_WARM_FLUORESCENT:
                return CameraCaptureMetaData.AwbMode.WARM_FLUORESCENT;
            case CaptureResult.CONTROL_AWB_MODE_DAYLIGHT:
                return CameraCaptureMetaData.AwbMode.DAYLIGHT;
            case CaptureResult.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
                return CameraCaptureMetaData.AwbMode.CLOUDY_DAYLIGHT;
            case CaptureResult.CONTROL_AWB_MODE_TWILIGHT:
                return CameraCaptureMetaData.AwbMode.TWILIGHT;
            case CaptureResult.CONTROL_AWB_MODE_SHADE:
                return CameraCaptureMetaData.AwbMode.SHADE;
            default:
                return CameraCaptureMetaData.AwbMode.UNKNOWN;
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @NonNull
    @Override
    public TagBundle getTagBundle() {
        return mTagBundle;
    }

    @Override
    public void populateExifData(@NonNull ExifData.Builder exifData) {
        // Call interface default to set flash mode
        CameraCaptureResult.super.populateExifData(exifData);

        // Set orientation
        try {
            Integer jpegOrientation = (Integer) mKeyValues.get(CaptureResult.JPEG_ORIENTATION);
            if (jpegOrientation != null) {
                exifData.setOrientationDegrees(jpegOrientation);
            }
        } catch (BufferUnderflowException exception) {
            // On certain devices, e.g. Pixel 3 XL API 31, getting JPEG orientation on YUV stream
            // throws BufferUnderflowException. The value will be overridden in post-processing
            // anyway, so it's safe to ignore.
            Logger.w(TAG, "Failed to get JPEG orientation.");
        }

        // Set exposure time
        Long exposureTimeNs = (Long) mKeyValues.get(CaptureResult.SENSOR_EXPOSURE_TIME);
        if (exposureTimeNs != null) {
            exifData.setExposureTimeNanos(exposureTimeNs);
        }

        // Set the aperture
        Float aperture = (Float) mKeyValues.get(CaptureResult.LENS_APERTURE);
        if (aperture != null) {
            exifData.setLensFNumber(aperture);
        }

        // Set the ISO
        Integer iso = (Integer) mKeyValues.get(CaptureResult.SENSOR_SENSITIVITY);
        if (iso != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                Integer postRawSensitivityBoost =
                        (Integer) mKeyValues
                                .get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST);
                if (postRawSensitivityBoost != null) {
                    iso *= (int) (postRawSensitivityBoost / 100f);
                }
            }
            exifData.setIso(iso);
        }

        // Set the focal length
        Float focalLength = (Float) mKeyValues.get(CaptureResult.LENS_FOCAL_LENGTH);
        if (focalLength != null) {
            exifData.setFocalLength(focalLength);
        }

        // Set white balance MANUAL/AUTO
        Integer whiteBalanceMode = (Integer) mKeyValues.get(CaptureResult.CONTROL_AWB_MODE);
        if (whiteBalanceMode != null) {
            ExifData.WhiteBalanceMode wbMode = ExifData.WhiteBalanceMode.AUTO;
            if (whiteBalanceMode == CameraMetadata.CONTROL_AWB_MODE_OFF) {
                wbMode = ExifData.WhiteBalanceMode.MANUAL;
            }
            exifData.setWhiteBalanceMode(wbMode);
        }
    }

    @Nullable
    @Override
    public CaptureResult getCaptureResult() {
        return null;
    }
}
