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
import android.hardware.camera2.CaptureRequest.Key;
import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CaptureConfigTest {
    private DeferrableSurface mMockSurface0;

    @Before
    public void setup() {
        mMockSurface0 = Mockito.mock(DeferrableSurface.class);
    }

    @Test
    public void buildCaptureRequestWithNullCameraDevice() throws CameraAccessException {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraDevice cameraDevice = null;
        CaptureConfig captureConfig = builder.build();

        CaptureRequest.Builder captureRequestBuilder =
                captureConfig.buildCaptureRequest(cameraDevice);

        assertThat(captureRequestBuilder).isNull();
    }

    @Test
    public void builderSetTemplate() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        CaptureConfig captureConfig = builder.build();

        assertThat(captureConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void builderAddSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        CaptureConfig captureConfig = builder.build();

        List<DeferrableSurface> surfaces = captureConfig.getSurfaces();

        assertThat(surfaces).hasSize(1);
        assertThat(surfaces).contains(mMockSurface0);
    }

    @Test
    public void builderRemoveSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.removeSurface(mMockSurface0);
        CaptureConfig captureConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(captureConfig.getSurfaces());
        assertThat(surfaces).isEmpty();
    }

    @Test
    public void builderClearSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.clearSurfaces();
        CaptureConfig captureConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(captureConfig.getSurfaces());
        assertThat(surfaces.size()).isEqualTo(0);
    }

    @Test
    public void builderAddCharacteristic() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        CaptureConfig captureConfig = builder.build();

        Map<Key<?>, CaptureRequestParameter<?>> parameterMap =
                captureConfig.getCameraCharacteristics();

        assertThat(parameterMap.containsKey(CaptureRequest.CONTROL_AF_MODE)).isTrue();
        assertThat(parameterMap)
                .containsEntry(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequestParameter.create(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_AUTO));
    }

    @Test
    public void builderSetUseTargetedSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.setUseRepeatingSurface(true);
        CaptureConfig captureConfig = builder.build();

        assertThat(captureConfig.isUseRepeatingSurface()).isTrue();
    }
}
