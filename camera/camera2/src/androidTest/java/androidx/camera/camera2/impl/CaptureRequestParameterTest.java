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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;

import androidx.camera.core.CaptureRequestParameter;
import androidx.camera.testing.CameraUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CaptureRequestParameterTest {
    private CameraDevice mCameraDevice;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setup() throws CameraAccessException, InterruptedException {
        assumeTrue(CameraUtil.deviceHasCamera());
        mCameraDevice = CameraUtil.getCameraDevice();
    }

    @After
    public void teardown() {
        if (mCameraDevice != null) {
            CameraUtil.releaseCameraDevice(mCameraDevice);
        }
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
