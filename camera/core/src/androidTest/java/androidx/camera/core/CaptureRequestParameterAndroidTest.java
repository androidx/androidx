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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CaptureRequestParameterAndroidTest {
    private CameraDevice mCameraDevice;

    @Before
    public void setup() throws CameraAccessException, InterruptedException {
        mCameraDevice = CameraUtil.getCameraDevice();
    }

    @After
    public void teardown() {
        CameraUtil.releaseCameraDevice(mCameraDevice);
    }

    @Test
    public void instanceCreation() {
        CaptureRequestParameter<?> captureRequestParameter =
                CaptureRequestParameter.create(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        assertThat(captureRequestParameter.getKey()).isEqualTo(CaptureRequest.CONTROL_AF_MODE);
        assertThat(captureRequestParameter.getValue())
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO);
    }

    @Test
    public void applyParameter() throws CameraAccessException {
        CaptureRequest.Builder builder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        assertThat(builder).isNotNull();

        CaptureRequestParameter<?> captureRequestParameter =
                CaptureRequestParameter.create(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        captureRequestParameter.apply(builder);

        assertThat(builder.get(CaptureRequest.CONTROL_AF_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO);
    }
}
