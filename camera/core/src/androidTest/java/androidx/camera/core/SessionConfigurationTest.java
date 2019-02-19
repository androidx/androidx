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
public class SessionConfigurationTest {
    private DeferrableSurface mMockSurface0;
    private DeferrableSurface mMockSurface1;

    @Before
    public void setup() {
        mMockSurface0 = new ImmediateSurface(Mockito.mock(Surface.class));
        mMockSurface1 = new ImmediateSurface(Mockito.mock(Surface.class));
    }

    @Test
    public void builderSetTemplate() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        SessionConfiguration sessionConfiguration = builder.build();

        assertThat(sessionConfiguration.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void builderAddSurface() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.addSurface(mMockSurface0);
        SessionConfiguration sessionConfiguration = builder.build();

        List<DeferrableSurface> surfaces = sessionConfiguration.getSurfaces();

        assertThat(surfaces).hasSize(1);
        assertThat(surfaces).contains(mMockSurface0);
    }

    @Test
    public void builderAddNonRepeatingSurface() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.addNonRepeatingSurface(mMockSurface0);
        SessionConfiguration sessionConfiguration = builder.build();

        List<DeferrableSurface> surfaces = sessionConfiguration.getSurfaces();
        List<DeferrableSurface> repeatingSurfaces =
                sessionConfiguration.getCaptureRequestConfiguration().getSurfaces();

        assertThat(surfaces).hasSize(1);
        assertThat(surfaces).contains(mMockSurface0);
        assertThat(repeatingSurfaces).isEmpty();
        assertThat(repeatingSurfaces).doesNotContain(mMockSurface0);
    }

    @Test
    public void builderAddSurfaceContainsRepeatingSurface() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.addSurface(mMockSurface0);
        builder.addNonRepeatingSurface(mMockSurface1);
        SessionConfiguration sessionConfiguration = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces());
        List<Surface> repeatingSurfaces =
                DeferrableSurfaces.surfaceList(
                        sessionConfiguration.getCaptureRequestConfiguration().getSurfaces());

        assertThat(surfaces.size()).isAtLeast(repeatingSurfaces.size());
        assertThat(surfaces).containsAllIn(repeatingSurfaces);
    }

    @Test
    public void builderRemoveSurface() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.addSurface(mMockSurface0);
        builder.removeSurface(mMockSurface0);
        SessionConfiguration sessionConfiguration = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces());
        assertThat(surfaces).isEmpty();
    }

    @Test
    public void builderClearSurface() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.addSurface(mMockSurface0);
        builder.clearSurfaces();
        SessionConfiguration sessionConfiguration = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces());
        assertThat(surfaces.size()).isEqualTo(0);
    }

    @Test
    public void builderAddCharacteristic() {
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();

        builder.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        SessionConfiguration sessionConfiguration = builder.build();

        Map<Key<?>, CaptureRequestParameter<?>> parameterMap =
                sessionConfiguration.getCameraCharacteristics();

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
        SessionConfiguration.Builder builderPreview = new SessionConfiguration.Builder();
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        SessionConfiguration sessionConfigurationPreview = builderPreview.build();
        SessionConfiguration.Builder builderZsl = new SessionConfiguration.Builder();
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
        SessionConfiguration sessionConfigurationZsl = builderZsl.build();

        SessionConfiguration.ValidatingBuilder validatingBuilder =
                new SessionConfiguration.ValidatingBuilder();

        validatingBuilder.add(sessionConfigurationPreview);
        validatingBuilder.add(sessionConfigurationZsl);

        assertThat(validatingBuilder.isValid()).isFalse();
    }

    @Test
    public void conflictingCharacteristics() {
        SessionConfiguration.Builder builderAfAuto = new SessionConfiguration.Builder();
        builderAfAuto.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        SessionConfiguration sessionConfigurationAfAuto = builderAfAuto.build();
        SessionConfiguration.Builder builderAfOff = new SessionConfiguration.Builder();
        builderAfOff.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        SessionConfiguration sessionConfigurationAfOff = builderAfOff.build();

        SessionConfiguration.ValidatingBuilder validatingBuilder =
                new SessionConfiguration.ValidatingBuilder();

        validatingBuilder.add(sessionConfigurationAfAuto);
        validatingBuilder.add(sessionConfigurationAfOff);

        assertThat(validatingBuilder.isValid()).isFalse();
    }

    @Test
    public void combineTwoSessionsValid() {
        SessionConfiguration.Builder builder0 = new SessionConfiguration.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfiguration.Builder builder1 = new SessionConfiguration.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfiguration.ValidatingBuilder validatingBuilder =
                new SessionConfiguration.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        assertThat(validatingBuilder.isValid()).isTrue();
    }

    @Test
    public void combineTwoSessionsTemplate() {
        SessionConfiguration.Builder builder0 = new SessionConfiguration.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfiguration.Builder builder1 = new SessionConfiguration.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfiguration.ValidatingBuilder validatingBuilder =
                new SessionConfiguration.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfiguration sessionConfiguration = validatingBuilder.build();

        assertThat(sessionConfiguration.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void combineTwoSessionsSurfaces() {
        SessionConfiguration.Builder builder0 = new SessionConfiguration.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfiguration.Builder builder1 = new SessionConfiguration.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfiguration.ValidatingBuilder validatingBuilder =
                new SessionConfiguration.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfiguration sessionConfiguration = validatingBuilder.build();

        List<DeferrableSurface> surfaces = sessionConfiguration.getSurfaces();
        assertThat(surfaces).containsExactly(mMockSurface0, mMockSurface1);
    }

    @Test
    public void combineTwoSessionsCharacteristics() {
        SessionConfiguration.Builder builder0 = new SessionConfiguration.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder0.addCharacteristic(
                CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        SessionConfiguration.Builder builder1 = new SessionConfiguration.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addCharacteristic(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);

        SessionConfiguration.ValidatingBuilder validatingBuilder =
                new SessionConfiguration.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfiguration sessionConfiguration = validatingBuilder.build();

        Map<Key<?>, CaptureRequestParameter<?>> parameterMap =
                sessionConfiguration.getCameraCharacteristics();
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
