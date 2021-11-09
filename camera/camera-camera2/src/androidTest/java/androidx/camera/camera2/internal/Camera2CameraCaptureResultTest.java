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

import static androidx.exifinterface.media.ExifInterface.FLAG_FLASH_FIRED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.graphics.Rect;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureResult;
import android.os.Build;

import androidx.camera.core.impl.CameraCaptureMetaData.AeState;
import androidx.camera.core.impl.CameraCaptureMetaData.AfMode;
import androidx.camera.core.impl.CameraCaptureMetaData.AfState;
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState;
import androidx.camera.core.impl.CameraCaptureMetaData.FlashState;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class Camera2CameraCaptureResultTest {

    private CaptureResult mCaptureResult;
    private Camera2CameraCaptureResult mCamera2CameraCaptureResult;
    private final TagBundle mTag = TagBundle.emptyBundle();

    @Before
    public void setUp() {
        mCaptureResult = Mockito.mock(CaptureResult.class);
        mCamera2CameraCaptureResult = new Camera2CameraCaptureResult(mTag, mCaptureResult);
    }

    @Test
    public void getAfMode_withNull() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE)).thenReturn(null);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.UNKNOWN);
    }

    @Test
    public void getAfMode_withAfModeOff() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_OFF);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.OFF);
    }

    @Test
    public void getAfMode_withAfModeEdof() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_EDOF);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.OFF);
    }

    @Test
    public void getAfMode_withAfModeAuto() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_AUTO);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_MANUAL_AUTO);
    }

    @Test
    public void getAfMode_withAfModeMacro() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_MACRO);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_MANUAL_AUTO);
    }

    @Test
    public void getAfMode_withAfModeContinuousPicture() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_CONTINUOUS_AUTO);
    }

    @Test
    public void getAfMode_withAfModeContinuousVideo() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        assertThat(mCamera2CameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_CONTINUOUS_AUTO);
    }

    @Test
    public void getAfState_withNull() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(null);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.UNKNOWN);
    }

    @Test
    public void getAfState_withAfStateInactive() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_INACTIVE);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.INACTIVE);
    }

    @Test
    public void getAfState_withAfStateActiveScan() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.SCANNING);
    }

    @Test
    public void getAfState_withAfStatePassiveScan() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.SCANNING);
    }

    @Test
    public void getAfState_withAfStatePassiveUnfocused() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.PASSIVE_NOT_FOCUSED);
    }

    @Test
    public void getAfState_withAfStatePassiveFocused() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.PASSIVE_FOCUSED);
    }

    @Test
    public void getAfState_withAfStateFocusedLocked() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.LOCKED_FOCUSED);
    }

    @Test
    public void getAfState_withAfStateNotFocusedLocked() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.LOCKED_NOT_FOCUSED);
    }

    @Test
    public void getAeState_withNull() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE)).thenReturn(null);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.UNKNOWN);
    }

    @Test
    public void getAeState_withAeStateInactive() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_INACTIVE);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.INACTIVE);
    }

    @Test
    public void getAeState_withAeStateSearching() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_SEARCHING);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.SEARCHING);
    }

    @Test
    public void getAeState_withAeStatePrecapture() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_PRECAPTURE);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.SEARCHING);
    }

    @Test
    public void getAeState_withAeStateFlashRequired() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.FLASH_REQUIRED);
    }

    @Test
    public void getAeState_withAeStateConverged() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_CONVERGED);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.CONVERGED);
    }

    @Test
    public void getAeState_withAeStateLocked() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_LOCKED);
        assertThat(mCamera2CameraCaptureResult.getAeState()).isEqualTo(AeState.LOCKED);
    }

    @Test
    public void getAwbState_withNull() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AWB_STATE)).thenReturn(null);
        assertThat(mCamera2CameraCaptureResult.getAwbState()).isEqualTo(AwbState.UNKNOWN);
    }

    @Test
    public void getAwbState_withAwbStateInactive() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_INACTIVE);
        assertThat(mCamera2CameraCaptureResult.getAwbState()).isEqualTo(AwbState.INACTIVE);
    }

    @Test
    public void getAwbState_withAwbStateSearching() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_SEARCHING);
        assertThat(mCamera2CameraCaptureResult.getAwbState()).isEqualTo(AwbState.METERING);
    }

    @Test
    public void getAwbState_withAwbStateConverged() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_CONVERGED);
        assertThat(mCamera2CameraCaptureResult.getAwbState()).isEqualTo(AwbState.CONVERGED);
    }

    @Test
    public void getAwbState_withAwbStateLocked() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_LOCKED);
        assertThat(mCamera2CameraCaptureResult.getAwbState()).isEqualTo(AwbState.LOCKED);
    }

    @Test
    public void getFlashState_withNull() {
        when(mCaptureResult.get(CaptureResult.FLASH_STATE)).thenReturn(null);
        assertThat(mCamera2CameraCaptureResult.getFlashState()).isEqualTo(FlashState.UNKNOWN);
    }

    @Test
    public void getFlashState_withFlashStateUnavailable() {
        when(mCaptureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_UNAVAILABLE);
        assertThat(mCamera2CameraCaptureResult.getFlashState()).isEqualTo(FlashState.NONE);
    }

    @Test
    public void getFlashState_withFlashStateCharging() {
        when(mCaptureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_CHARGING);
        assertThat(mCamera2CameraCaptureResult.getFlashState()).isEqualTo(FlashState.NONE);
    }

    @Test
    public void getFlashState_withFlashStateReady() {
        when(mCaptureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_READY);
        assertThat(mCamera2CameraCaptureResult.getFlashState()).isEqualTo(FlashState.READY);
    }

    @Test
    public void getFlashState_withFlashStateFired() {
        when(mCaptureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_FIRED);
        assertThat(mCamera2CameraCaptureResult.getFlashState()).isEqualTo(FlashState.FIRED);
    }

    @Test
    public void getFlashState_withFlashStatePartial() {
        when(mCaptureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_PARTIAL);
        assertThat(mCamera2CameraCaptureResult.getFlashState()).isEqualTo(FlashState.FIRED);
    }

    @Test
    public void canPopulateExif() {
        // Arrange
        when(mCaptureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_FIRED);

        Rect cropRegion = new Rect(0, 0, 640, 480);
        when(mCaptureResult.get(CaptureResult.SCALER_CROP_REGION)).thenReturn(cropRegion);

        when(mCaptureResult.get(CaptureResult.JPEG_ORIENTATION)).thenReturn(270);

        long exposureTime = TimeUnit.SECONDS.toNanos(5);
        when(mCaptureResult.get(CaptureResult.SENSOR_EXPOSURE_TIME)).thenReturn(exposureTime);

        float aperture = 1.8f;
        when(mCaptureResult.get(CaptureResult.LENS_APERTURE)).thenReturn(aperture);

        int iso = 200;
        int postRawSensitivityBoost = 100; // No boost for API < 24
        when(mCaptureResult.get(CaptureResult.SENSOR_SENSITIVITY)).thenReturn(iso);
        if (Build.VERSION.SDK_INT >= 24) {
            // Add boost for API >= 24
            postRawSensitivityBoost = 200;
            when(mCaptureResult.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST))
                    .thenReturn(postRawSensitivityBoost);
        }

        float focalLength = 4200f;
        when(mCaptureResult.get(CaptureResult.LENS_FOCAL_LENGTH)).thenReturn(focalLength);

        when(mCaptureResult.get(CaptureResult.CONTROL_AWB_MODE))
                .thenReturn(CameraMetadata.CONTROL_AWB_MODE_OFF);

        // Act
        ExifData.Builder exifBuilder = ExifData.builderForDevice();
        mCamera2CameraCaptureResult.populateExifData(exifBuilder);
        ExifData exifData = exifBuilder.build();

        // Assert
        assertThat(Short.parseShort(exifData.getAttribute(ExifInterface.TAG_FLASH)))
                .isEqualTo(FLAG_FLASH_FIRED);

        assertThat(exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH))
                .isEqualTo(String.valueOf(cropRegion.width()));

        assertThat(exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH))
                .isEqualTo(String.valueOf(cropRegion.height()));

        assertThat(exifData.getAttribute(ExifInterface.TAG_ORIENTATION))
                .isEqualTo(String.valueOf(ExifInterface.ORIENTATION_ROTATE_270));

        String exposureTimeString = exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        assertThat(exposureTimeString).isNotNull();
        assertThat(Float.parseFloat(exposureTimeString)).isWithin(0.1f)
                .of(TimeUnit.NANOSECONDS.toSeconds(exposureTime));

        assertThat(exifData.getAttribute(ExifInterface.TAG_F_NUMBER))
                .isEqualTo(String.valueOf(aperture));

        assertThat(
                Short.parseShort(exifData.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)))
                .isEqualTo((short) (iso * (int) (postRawSensitivityBoost / 100f)));

        String focalLengthString = exifData.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
        assertThat(focalLengthString).isNotNull();
        String[] fractionValues = focalLengthString.split("/");
        long numerator = Long.parseLong(fractionValues[0]);
        long denominator = Long.parseLong(fractionValues[1]);
        assertThat(numerator / (float) denominator).isWithin(0.1f).of(focalLength);

        assertThat(Short.parseShort(exifData.getAttribute(ExifInterface.TAG_WHITE_BALANCE)))
                .isEqualTo(ExifInterface.WHITE_BALANCE_MANUAL);
    }
}
