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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2CameraCaptureResultTest {

    private CaptureResult mCaptureResult;
    private Camera2CameraCaptureResult mCamera2CameraCaptureResult;
    private Object mTag = null;

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
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.SCANNING);
    }

    @Test
    public void getAfState_withAfStatePassiveFocused() {
        when(mCaptureResult.get(CaptureResult.CONTROL_AF_STATE))
                .thenReturn(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED);
        assertThat(mCamera2CameraCaptureResult.getAfState()).isEqualTo(AfState.FOCUSED);
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
}
