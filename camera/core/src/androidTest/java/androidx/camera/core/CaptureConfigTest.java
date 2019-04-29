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

import static org.mockito.Mockito.mock;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Key;
import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class CaptureConfigTest {
    private DeferrableSurface mMockSurface0;

    @Before
    public void setup() {
        mMockSurface0 = mock(DeferrableSurface.class);
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

    @Test
    public void builderAddMultipleCameraCaptureCallbacks() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback1);
        CaptureConfig configuration = builder.build();

        assertThat(configuration.getCameraCaptureCallbacks()).containsExactly(callback0, callback1);
    }

    @Test
    public void builderAddAllCameraCaptureCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> callbacks = Lists.newArrayList(callback0, callback1);

        builder.addAllRepeatingCameraCaptureCallbacks(callbacks);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getRepeatingCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderAddDuplicateCameraCaptureCallback_throwsException() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback0);
    }

    @Test
    public void builderFromPrevious_containsCameraCaptureCallbacks() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback1);
        builder = CaptureConfig.Builder.from(builder.build());
        CameraCaptureCallback callback2 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback2);
        CaptureConfig configuration = builder.build();

        assertThat(configuration.getCameraCaptureCallbacks())
                .containsExactly(callback0, callback1, callback2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void cameraCaptureCallbacks_areImmutable() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CaptureConfig configuration = builder.build();

        configuration.getCameraCaptureCallbacks().add(mock(CameraCaptureCallback.class));
    }

}
