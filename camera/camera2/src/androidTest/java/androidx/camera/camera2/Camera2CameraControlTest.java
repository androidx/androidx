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
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.FlashMode;
import androidx.camera.core.SessionConfig;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2CameraControlTest {

    private Camera2CameraControl mCamera2CameraControl;
    private CameraControl.ControlUpdateListener mControlUpdateListener;
    private ArgumentCaptor<SessionConfig> mSessionConfigArgumentCaptor =
            ArgumentCaptor.forClass(SessionConfig.class);
    private ArgumentCaptor<List<CaptureConfig>> mCaptureConfigArgumentCaptor =
            ArgumentCaptor.forClass(List.class);

    @Before
    @UiThreadTest
    public void setUp() {
        mControlUpdateListener = mock(CameraControl.ControlUpdateListener.class);
        mCamera2CameraControl = new Camera2CameraControl(mControlUpdateListener, new Handler(
                Looper.getMainLooper()));
        // Reset the method call onCameraControlUpdateSessionConfig() in Camera2CameraControl
        // constructor.
        reset(mControlUpdateListener);
    }

    @Test
    @UiThreadTest
    public void setCropRegion_cropRectSetAndRepeatingRequestUpdated() {
        Rect rect = new Rect(0, 0, 10, 10);

        mCamera2CameraControl.setCropRegion(rect);
        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getValue();
        Camera2Config repeatingConfig = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(repeatingConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(rect);

        Camera2Config singleConfig = new Camera2Config(mCamera2CameraControl.getSharedOptions());
        assertThat(singleConfig.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null))
                .isEqualTo(rect);
    }

    @Test
    @UiThreadTest
    public void focus_focusRectSetAndRequestsExecuted() {
        Rect focusRect = new Rect(0, 0, 10, 10);
        Rect meteringRect = new Rect(20, 20, 30, 30);

        mCamera2CameraControl.focus(focusRect, meteringRect);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getValue();
        Camera2Config repeatingConfig = new Camera2Config(sessionConfig.getImplementationOptions());

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

        Camera2Config singleConfig = new Camera2Config(mCamera2CameraControl.getSharedOptions());
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

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());

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

        verify(mControlUpdateListener, times(2)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getAllValues().get(1);
        Camera2Config repeatingConfig = new Camera2Config(sessionConfig.getImplementationOptions());
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

        Camera2Config singleConfig = new Camera2Config(mCamera2CameraControl.getSharedOptions());
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

        verify(mControlUpdateListener, times(2)).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getAllValues().get(1).get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());

        assertThat(resultCaptureConfig.getCaptureRequestOption(CaptureRequest.CONTROL_AF_TRIGGER,
                null)).isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
    }

    @Test
    @UiThreadTest
    public void defaultAFAWBMode_ShouldBeCAFWhenNotFocusLocked() {
        Camera2Config singleConfig = new Camera2Config(mCamera2CameraControl.getSharedOptions());
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

        Camera2Config singleConfig = new Camera2Config(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO);

        mCamera2CameraControl.cancelFocus();

        Camera2Config singleConfig2 = new Camera2Config(mCamera2CameraControl.getSharedOptions());
        assertThat(
                singleConfig2.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    @Test
    @UiThreadTest
    public void setFlashModeAuto_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.AUTO);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getValue();
        Camera2Config camera2Config = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON_AUTO_FLASH);
        assertThat(mCamera2CameraControl.getFlashMode()).isEqualTo(FlashMode.AUTO);
    }

    @Test
    @UiThreadTest
    public void setFlashModeOff_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.OFF);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getValue();
        Camera2Config camera2Config = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
        assertThat(mCamera2CameraControl.getFlashMode()).isEqualTo(FlashMode.OFF);
    }

    @Test
    @UiThreadTest
    public void setFlashModeOn_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.ON);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getValue();
        Camera2Config camera2Config = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON_ALWAYS_FLASH);
        assertThat(mCamera2CameraControl.getFlashMode()).isEqualTo(FlashMode.ON);
    }

    @Test
    @UiThreadTest
    public void enableTorch_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.enableTorch(true);

        verify(mControlUpdateListener, times(1)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getValue();
        Camera2Config camera2Config = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON);
        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.FLASH_MODE, FLASH_MODE_OFF))
                .isEqualTo(FLASH_MODE_TORCH);
        assertThat(mCamera2CameraControl.isTorchOn()).isTrue();
    }

    @Test
    @UiThreadTest
    public void disableTorchFlashModeAuto_aeModeSetAndRequestUpdated() {
        mCamera2CameraControl.setFlashMode(FlashMode.AUTO);
        mCamera2CameraControl.enableTorch(false);

        verify(mControlUpdateListener, times(2)).onCameraControlUpdateSessionConfig(
                mSessionConfigArgumentCaptor.capture());
        SessionConfig sessionConfig = mSessionConfigArgumentCaptor.getAllValues().get(0);
        Camera2Config camera2Config = new Camera2Config(sessionConfig.getImplementationOptions());
        assertThat(
                camera2Config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, CONTROL_AE_MODE_OFF))
                .isEqualTo(CONTROL_AE_MODE_ON_AUTO_FLASH);
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.FLASH_MODE, -1))
                .isEqualTo(-1);
        assertThat(mCamera2CameraControl.isTorchOn()).isFalse();

        verify(mControlUpdateListener, times(1)).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, null))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    @UiThreadTest
    public void triggerAf_captureRequestSent() {
        mCamera2CameraControl.triggerAf();

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AF_TRIGGER_START);
    }

    @Test
    @UiThreadTest
    public void triggerAePrecapture_captureRequestSent() {
        mCamera2CameraControl.triggerAePrecapture();

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(
                resultCaptureConfig.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, null))
                .isEqualTo(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
    }

    @Test
    @UiThreadTest
    public void cancelAfAeTrigger_captureRequestSent() {
        mCamera2CameraControl.cancelAfAeTrigger(true, true);

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());
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
    public void cancelAfTrigger_captureRequestSent() {
        mCamera2CameraControl.cancelAfAeTrigger(true, false);

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());
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
    public void cancelAeTrigger_captureRequestSent() {
        mCamera2CameraControl.cancelAfAeTrigger(false, true);

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());

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
    public void submitCaptureRequest_overrideBySharedOptions() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        configBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_MACRO);
        builder.setImplementationOptions(configBuilder.build());
        mCamera2CameraControl.submitCaptureRequests(Collections.singletonList(builder.build()));

        verify(mControlUpdateListener).onCameraControlCaptureRequests(
                mCaptureConfigArgumentCaptor.capture());
        CaptureConfig captureConfig = mCaptureConfigArgumentCaptor.getValue().get(0);
        Camera2Config resultCaptureConfig =
                new Camera2Config(captureConfig.getImplementationOptions());

        Camera2Config sharedOptions =
                new Camera2Config(mCamera2CameraControl.getSharedOptions());

        assertThat(resultCaptureConfig.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE,
                null)).isEqualTo(
                sharedOptions.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, null));
    }
}
