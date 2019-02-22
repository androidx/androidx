/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_OFF;
import static android.hardware.camera2.CameraMetadata.FLASH_MODE_TORCH;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Handler;
import android.os.Looper;

import androidx.camera.core.CameraControl;
import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.FlashMode;
import androidx.camera.core.SessionConfiguration;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2CameraControlTest {

    private Camera2CameraControl mCamera2CameraControl;
    private CameraControl.ControlUpdateListener mControlUpdateListener;
    private ArgumentCaptor<SessionConfiguration> mSessionConfigurationArgumentCaptor =
            ArgumentCaptor.forClass(SessionConfiguration.class);
    private ArgumentCaptor<CaptureRequestConfiguration> mCaptureRequestConfigurationArgumentCaptor =
            ArgumentCaptor.forClass(CaptureRequestConfiguration.class);

    @Before
    @UiThreadTest
    public void setUp() {
        mControlUpdateListener = mock(CameraControl.ControlUpdateListener.class);
        mCamera2CameraControl = new Camera2CameraControl(mControlUpdateListener, new Handler(
                Looper.getMainLooper()));
        // Reset the method call onCameraControlUpdateSessionConfiguration() in
        // Camera2CameraControl constructor.
        reset(mControlUpdateListener);
    }

    @Test
    @UiThreadTest
    public void setCropRegion_cropRectSetAndRepeatingRequestUpdated() {
        Rect rect = new Rect(0, 0, 10, 10);

        mCamera2CameraControl.setCropRegion(rect);
        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration = mSessionConfigurationArgumentCaptor.getValue();
        Camera2Configuration repeatingConfig =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        assertThat(repeatingConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(rect);

        Camera2Configuration singleConfig =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());
        assertThat(singleConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(rect);
    }

    @Test
    @UiThreadTest
    public void focus_focusRectSetAndRequestsExecuted() {
        Rect focusRect = new Rect(0, 0, 10, 10);
        Rect meteringRect = new Rect(20, 20, 30, 30);

        mCamera2CameraControl.focus(focusRect, meteringRect);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration = mSessionConfigurationArgumentCaptor.getValue();
        Camera2Configuration repeatingConfig =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());

        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_REGIONS, null))
                .isEqualTo(
                        new MeteringRectangle[]{
                                new MeteringRectangle(focusRect,
                                        MeteringRectangle.METERING_WEIGHT_MAX)
                        });

        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_REGIONS, null))
                .isEqualTo(
                        new MeteringRectangle[]{
                                new MeteringRectangle(
                                        meteringRect, MeteringRectangle.METERING_WEIGHT_MAX)
                        });

        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_REGIONS, null))
                .isEqualTo(
                        new MeteringRectangle[]{
                                new MeteringRectangle(
                                        meteringRect, MeteringRectangle.METERING_WEIGHT_MAX)
                        });

        Camera2Configuration singleConfig =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_REGIONS, null))
                .isEqualTo(
                        new MeteringRectangle[]{
                                new MeteringRectangle(focusRect,
                                        MeteringRectangle.METERING_WEIGHT_MAX)
                        });

        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_REGIONS, null))
                .isEqualTo(
                        new MeteringRectangle[]{
                                new MeteringRectangle(
                                        meteringRect, MeteringRectangle.METERING_WEIGHT_MAX)
                        });

        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_REGIONS, null))
                .isEqualTo(
                        new MeteringRectangle[]{
                                new MeteringRectangle(
                                        meteringRect, MeteringRectangle.METERING_WEIGHT_MAX)
                        });

        assertThat(mCamera2CameraControl.isFocusLocked()).isTrue();

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());

        assertThat(resultCaptureConfig.getCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                null)).isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START);
    }

    @Test
    @UiThreadTest
    public void cancelFocus_regionRestored() {
        Rect focusRect = new Rect(0, 0, 10, 10);
        Rect meteringRect = new Rect(20, 20, 30, 30);

        mCamera2CameraControl.focus(focusRect, meteringRect);
        mCamera2CameraControl.cancelFocus();

        verify(mControlUpdateListener, times(2)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration =
                mSessionConfigurationArgumentCaptor.getAllValues().get(1);
        Camera2Configuration repeatingConfig =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        MeteringRectangle zeroRegion =
                new MeteringRectangle(new Rect(), MeteringRectangle.METERING_WEIGHT_DONT_CARE);

        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_REGIONS, null))
                .isEqualTo(new MeteringRectangle[]{zeroRegion});
        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_REGIONS, null))
                .isEqualTo(new MeteringRectangle[]{zeroRegion});
        assertThat(
                repeatingConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_REGIONS, null))
                .isEqualTo(new MeteringRectangle[]{zeroRegion});

        Camera2Configuration singleConfig =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_REGIONS, null))
                .isEqualTo(new MeteringRectangle[]{zeroRegion});
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_REGIONS, null))
                .isEqualTo(new MeteringRectangle[]{zeroRegion});
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_REGIONS, null))
                .isEqualTo(new MeteringRectangle[]{zeroRegion});

        assertThat(mCamera2CameraControl.isFocusLocked()).isFalse();

        verify(mControlUpdateListener, times(2)).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getAllValues().get(1);
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());

        assertThat(resultCaptureConfig.getCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                null)).isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
    }

    @Test
    @UiThreadTest
    public void defaultAFAWBMode_ShouldBeCAFWhenNotFocusLocked() {
        Camera2Configuration singleConfig =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_MODE_AUTO);
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AWB_MODE_AUTO);
    }

    @Test
    @UiThreadTest
    public void focus_afModeSetToAuto() {
        Rect focusRect = new Rect(0, 0, 10, 10);
        mCamera2CameraControl.focus(focusRect, focusRect);

        Camera2Configuration singleConfig =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO);

        mCamera2CameraControl.cancelFocus();

        Camera2Configuration singleConfig2 =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig2.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    @Test
    @UiThreadTest
    public void setFlashModeAuto_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.AUTO);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration = mSessionConfigurationArgumentCaptor.getValue();
        Camera2Configuration camera2Configuration =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        assertThat(
                camera2Configuration.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON_AUTO_FLASH);
        assertThat(mCamera2CameraControl.getFlashMode()).isEqualTo(FlashMode.AUTO);
    }

    @Test
    @UiThreadTest
    public void setFlashModeOff_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.OFF);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration = mSessionConfigurationArgumentCaptor.getValue();
        Camera2Configuration camera2Configuration =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        assertThat(
                camera2Configuration.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
        assertThat(mCamera2CameraControl.getFlashMode()).isEqualTo(FlashMode.OFF);
    }

    @Test
    @UiThreadTest
    public void setFlashModeOn_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.ON);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration = mSessionConfigurationArgumentCaptor.getValue();
        Camera2Configuration camera2Configuration =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        assertThat(
                camera2Configuration.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        assertThat(mCamera2CameraControl.getFlashMode()).isEqualTo(FlashMode.ON);
    }

    @Test
    @UiThreadTest
    public void enableTorch_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.enableTorch(true);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration = mSessionConfigurationArgumentCaptor.getValue();
        Camera2Configuration camera2Configuration =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        assertThat(
                camera2Configuration.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON);
        assertThat(
                camera2Configuration.getCaptureRequestOption(
                        CaptureRequest.FLASH_MODE, FLASH_MODE_OFF))
                .isEqualTo(FLASH_MODE_TORCH);
        assertThat(mCamera2CameraControl.isTorchOn()).isTrue();
    }

    @Test
    @UiThreadTest
    public void disableTorchFlashModeAuto_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.AUTO);
        mCamera2CameraControl.enableTorch(false);

        verify(mControlUpdateListener, times(2)).onCameraControlUpdateSessionConfiguration(
                mSessionConfigurationArgumentCaptor.capture());
        SessionConfiguration sessionConfiguration =
                mSessionConfigurationArgumentCaptor.getAllValues().get(0);
        Camera2Configuration camera2Configuration =
                new Camera2Configuration(sessionConfiguration.getImplementationOptions());
        assertThat(
                camera2Configuration.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON_AUTO_FLASH);
        assertThat(camera2Configuration.getCaptureRequestOption(CaptureRequest.FLASH_MODE, -1))
                .isEqualTo(-1);
        assertThat(mCamera2CameraControl.isTorchOn()).isFalse();

        verify(mControlUpdateListener, times(1)).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, null))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    @UiThreadTest
    public void triggerAf_singleRequestSent() {
        mCamera2CameraControl.triggerAf();

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START);
    }

    @Test
    @UiThreadTest
    public void triggerAePrecapture_singleRequestSent() {
        mCamera2CameraControl.triggerAePrecapture();

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
    }

    @Test
    @UiThreadTest
    public void cancelAfAeTrigger_singleRequestSent() {
        mCamera2CameraControl.cancelAfAeTrigger(true, true);

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
    }

    @Test
    @UiThreadTest
    public void cancelAfTrigger_singleRequestSent() {
        mCamera2CameraControl.cancelAfAeTrigger(true, false);

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null))
                .isNull();
    }

    @Test
    @UiThreadTest
    public void cancelAeTrigger_singleRequestSent() {
        mCamera2CameraControl.cancelAfAeTrigger(false, true);

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());

        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isNull();
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
    }

    @Test
    @UiThreadTest
    public void submitSingleRequest_overrideBySharedOptions() {
        CaptureRequestConfiguration.Builder builder = new CaptureRequestConfiguration.Builder();
        Camera2Configuration.Builder configBuilder = new Camera2Configuration.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_MACRO);
        builder.setImplementationOptions(configBuilder.build());
        mCamera2CameraControl.submitSingleRequest(builder.build());

        verify(mControlUpdateListener).onCameraControlSingleRequest(
                mCaptureRequestConfigurationArgumentCaptor.capture());
        CaptureRequestConfiguration captureRequestConfiguration =
                mCaptureRequestConfigurationArgumentCaptor.getValue();
        Camera2Configuration resultCaptureConfig =
                new Camera2Configuration(captureRequestConfiguration.getImplementationOptions());

        Camera2Configuration sharedOptions =
                new Camera2Configuration(mCamera2CameraControl.getSharedOptions());

        assertThat(resultCaptureConfig.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
                null)).isEqualTo(
                sharedOptions.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, null));
    }
}
