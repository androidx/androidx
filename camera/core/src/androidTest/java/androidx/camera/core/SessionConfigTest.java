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
public class SessionConfigTest {
    private DeferrableSurface mMockSurface0;
    private DeferrableSurface mMockSurface1;

    @Before
    public void setup() {
        mMockSurface0 = new ImmediateSurface(Mockito.mock(Surface.class));
        mMockSurface1 = new ImmediateSurface(Mockito.mock(Surface.class));
    }

    @Test
    public void builderSetTemplate() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        SessionConfig sessionConfig = builder.build();

        assertThat(sessionConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void builderAddSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addSurface(mMockSurface0);
        SessionConfig sessionConfig = builder.build();

        List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();

        assertThat(surfaces).hasSize(1);
        assertThat(surfaces).contains(mMockSurface0);
    }

    @Test
    public void builderAddNonRepeatingSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addNonRepeatingSurface(mMockSurface0);
        SessionConfig sessionConfig = builder.build();

        List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();
        List<DeferrableSurface> repeatingSurfaces = sessionConfig.getCaptureConfig().getSurfaces();

        assertThat(surfaces).hasSize(1);
        assertThat(surfaces).contains(mMockSurface0);
        assertThat(repeatingSurfaces).isEmpty();
        assertThat(repeatingSurfaces).doesNotContain(mMockSurface0);
    }

    @Test
    public void builderAddSurfaceContainsRepeatingSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.addNonRepeatingSurface(mMockSurface1);
        SessionConfig sessionConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(sessionConfig.getSurfaces());
        List<Surface> repeatingSurfaces =
                DeferrableSurfaces.surfaceList(sessionConfig.getCaptureConfig().getSurfaces());

        assertThat(surfaces.size()).isAtLeast(repeatingSurfaces.size());
        assertThat(surfaces).containsAllIn(repeatingSurfaces);
    }

    @Test
    public void builderRemoveSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.removeSurface(mMockSurface0);
        SessionConfig sessionConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces).isEmpty();
    }

    @Test
    public void builderClearSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.clearSurfaces();
        SessionConfig sessionConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces.size()).isEqualTo(0);
    }

    @Test
    public void builderAddCharacteristic() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        SessionConfig sessionConfig = builder.build();

        Map<Key<?>, CaptureRequestParameter<?>> parameterMap =
                sessionConfig.getCameraCharacteristics();

        assertThat(parameterMap.containsKey(CaptureRequest.CONTROL_AF_MODE)).isTrue();
        assertThat(parameterMap)
                .containsEntry(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequestParameter.create(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_AUTO));
    }

    @Test
    public void conflictingTemplate() {
        SessionConfig.Builder builderPreview = new SessionConfig.Builder();
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        SessionConfig sessionConfigPreview = builderPreview.build();
        SessionConfig.Builder builderZsl = new SessionConfig.Builder();
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
        SessionConfig sessionConfigZsl = builderZsl.build();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        validatingBuilder.add(sessionConfigPreview);
        validatingBuilder.add(sessionConfigZsl);

        assertThat(validatingBuilder.isValid()).isFalse();
    }

    @Test
    public void conflictingCharacteristics() {
        SessionConfig.Builder builderAfAuto = new SessionConfig.Builder();
        builderAfAuto.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        SessionConfig sessionConfigAfAuto = builderAfAuto.build();
        SessionConfig.Builder builderAfOff = new SessionConfig.Builder();
        builderAfOff.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        SessionConfig sessionConfigAfOff = builderAfOff.build();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        validatingBuilder.add(sessionConfigAfAuto);
        validatingBuilder.add(sessionConfigAfOff);

        assertThat(validatingBuilder.isValid()).isFalse();
    }

    @Test
    public void combineTwoSessionsValid() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        assertThat(validatingBuilder.isValid()).isTrue();
    }

    @Test
    public void combineTwoSessionsTemplate() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfig sessionConfig = validatingBuilder.build();

        assertThat(sessionConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void combineTwoSessionsSurfaces() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfig sessionConfig = validatingBuilder.build();

        List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();
        assertThat(surfaces).containsExactly(mMockSurface0, mMockSurface1);
    }

    @Test
    public void combineTwoSessionsCharacteristics() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfig sessionConfig = validatingBuilder.build();

        Map<Key<?>, CaptureRequestParameter<?>> parameterMap =
                sessionConfig.getCameraCharacteristics();
        assertThat(parameterMap)
                .containsExactly(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequestParameter.create(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_AUTO),
                        CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                        CaptureRequestParameter.create(
                                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO));
    }
}
