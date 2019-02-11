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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.hardware.camera2.CaptureResult;

import androidx.camera.core.CameraCaptureMetaData.AeState;
import androidx.camera.core.CameraCaptureMetaData.AfMode;
import androidx.camera.core.CameraCaptureMetaData.AfState;
import androidx.camera.core.CameraCaptureMetaData.AwbState;
import androidx.camera.core.CameraCaptureMetaData.FlashState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public final class Camera2CameraCaptureResultAndroidTest {

    private CaptureResult captureResult;
    private Camera2CameraCaptureResult cameraCaptureResult;

    @Before
    public void setUp() {
        captureResult = Mockito.mock(CaptureResult.class);
        cameraCaptureResult = new Camera2CameraCaptureResult(captureResult);
    }

    @Test
    public void getAfMode_withNull() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE)).thenReturn(null);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.UNKNOWN);
    }

    @Test
    public void getAfMode_withAfModeOff() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_OFF);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.OFF);
    }

    @Test
    public void getAfMode_withAfModeEdof() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_EDOF);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.OFF);
    }

    @Test
    public void getAfMode_withAfModeAuto() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_AUTO);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_MANUAL_AUTO);
    }

    @Test
    public void getAfMode_withAfModeMacro() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_MACRO);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_MANUAL_AUTO);
    }

    @Test
    public void getAfMode_withAfModeContinuousPicture() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_CONTINUOUS_AUTO);
    }

    @Test
    public void getAfMode_withAfModeContinuousVideo() {
        when(captureResult.get(CaptureResult.CONTROL_AF_MODE))
                .thenReturn(CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        assertThat(cameraCaptureResult.getAfMode()).isEqualTo(AfMode.ON_CONTINUOUS_AUTO);
    }

    @Test
    public void getAfState_withNull() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(null);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.UNKNOWN);
    }

    @Test
    public void getAfState_withAfStateInactive() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_INACTIVE);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.INACTIVE);
    }

    @Test
    public void getAfState_withAfStateActiveScan() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.SCANNING);
    }

    @Test
    public void getAfState_withAfStatePassiveScan() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.SCANNING);
    }

    @Test
    public void getAfState_withAfStatePassiveUnfocused() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.SCANNING);
    }

    @Test
    public void getAfState_withAfStatePassiveFocused() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.FOCUSED);
    }

    @Test
    public void getAfState_withAfStateFocusedLocked() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.LOCKED_FOCUSED);
    }

    @Test
    public void getAfState_withAfStateNotFocusedLocked() {
        when(captureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED);
        assertThat(cameraCaptureResult.getAfState()).isEqualTo(AfState.LOCKED_NOT_FOCUSED);
    }

    @Test
    public void getAeState_withNull() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE)).thenReturn(null);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.UNKNOWN);
    }

    @Test
    public void getAeState_withAeStateInactive() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_INACTIVE);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.INACTIVE);
    }

    @Test
    public void getAeState_withAeStateSearching() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_SEARCHING);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.SEARCHING);
    }

    @Test
    public void getAeState_withAeStatePrecapture() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_PRECAPTURE);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.SEARCHING);
    }

    @Test
    public void getAeState_withAeStateFlashRequired() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.FLASH_REQUIRED);
    }

    @Test
    public void getAeState_withAeStateConverged() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_CONVERGED);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.CONVERGED);
    }

    @Test
    public void getAeState_withAeStateLocked() {
        when(captureResult.get(CaptureResult.CONTROL_AE_STATE))
                .thenReturn(CaptureResult.CONTROL_AE_STATE_LOCKED);
        assertThat(cameraCaptureResult.getAeState()).isEqualTo(AeState.LOCKED);
    }

    @Test
    public void getAwbState_withNull() {
        when(captureResult.get(CaptureResult.CONTROL_AWB_STATE)).thenReturn(null);
        assertThat(cameraCaptureResult.getAwbState()).isEqualTo(AwbState.UNKNOWN);
    }

    @Test
    public void getAwbState_withAwbStateInactive() {
        when(captureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_INACTIVE);
        assertThat(cameraCaptureResult.getAwbState()).isEqualTo(AwbState.INACTIVE);
    }

    @Test
    public void getAwbState_withAwbStateSearching() {
        when(captureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_SEARCHING);
        assertThat(cameraCaptureResult.getAwbState()).isEqualTo(AwbState.METERING);
    }

    @Test
    public void getAwbState_withAwbStateConverged() {
        when(captureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_CONVERGED);
        assertThat(cameraCaptureResult.getAwbState()).isEqualTo(AwbState.CONVERGED);
    }

    @Test
    public void getAwbState_withAwbStateLocked() {
        when(captureResult.get(CaptureResult.CONTROL_AWB_STATE))
                .thenReturn(CaptureResult.CONTROL_AWB_STATE_LOCKED);
        assertThat(cameraCaptureResult.getAwbState()).isEqualTo(AwbState.LOCKED);
    }

    @Test
    public void getFlashState_withNull() {
        when(captureResult.get(CaptureResult.FLASH_STATE)).thenReturn(null);
        assertThat(cameraCaptureResult.getFlashState()).isEqualTo(FlashState.UNKNOWN);
    }

    @Test
    public void getFlashState_withFlashStateUnavailable() {
        when(captureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_UNAVAILABLE);
        assertThat(cameraCaptureResult.getFlashState()).isEqualTo(FlashState.NONE);
    }

    @Test
    public void getFlashState_withFlashStateCharging() {
        when(captureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_CHARGING);
        assertThat(cameraCaptureResult.getFlashState()).isEqualTo(FlashState.NONE);
    }

    @Test
    public void getFlashState_withFlashStateReady() {
        when(captureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_READY);
        assertThat(cameraCaptureResult.getFlashState()).isEqualTo(FlashState.READY);
    }

    @Test
    public void getFlashState_withFlashStateFired() {
        when(captureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_FIRED);
        assertThat(cameraCaptureResult.getFlashState()).isEqualTo(FlashState.FIRED);
    }

    @Test
    public void getFlashState_withFlashStatePartial() {
        when(captureResult.get(CaptureResult.FLASH_STATE))
                .thenReturn(CaptureResult.FLASH_STATE_PARTIAL);
        assertThat(cameraCaptureResult.getFlashState()).isEqualTo(FlashState.FIRED);
    }
}
