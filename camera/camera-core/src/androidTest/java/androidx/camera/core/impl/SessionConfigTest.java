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

package androidx.camera.core.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;

import androidx.camera.core.impl.Config.Option;
import androidx.camera.testing.DeferrableSurfacesUtil;
import androidx.camera.testing.fakes.FakeMultiValueSet;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SessionConfigTest {
    private static final Option<Integer> OPTION = Option.create(
            "camerax.test.option_0", Integer.class);
    private static final Option<String> OPTION_1 = Option.create(
            "camerax.test.option_1", String.class);

    private DeferrableSurface mMockSurface0;
    private DeferrableSurface mMockSurface1;

    @Before
    public void setup() {
        mMockSurface0 = new ImmediateSurface(mock(Surface.class));
        mMockSurface1 = new ImmediateSurface(mock(Surface.class));
    }

    @After
    public void tearDown() {
        mMockSurface0.close();
        mMockSurface1.close();
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
        List<DeferrableSurface> repeatingSurfaces =
                sessionConfig.getRepeatingCaptureConfig().getSurfaces();

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

        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.getSurfaces());
        List<Surface> repeatingSurfaces = DeferrableSurfacesUtil.surfaceList(
                sessionConfig.getRepeatingCaptureConfig().getSurfaces());

        assertThat(surfaces.size()).isAtLeast(repeatingSurfaces.size());
        assertThat(surfaces).containsAtLeastElementsIn(repeatingSurfaces);
    }

    @Test
    public void builderRemoveSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.removeSurface(mMockSurface0);
        SessionConfig sessionConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces).isEmpty();
    }

    @Test
    public void builderClearSurface() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.clearSurfaces();
        SessionConfig sessionConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces.size()).isEqualTo(0);
    }

    @Test
    public void builderAddOption() {
        SessionConfig.Builder builder = new SessionConfig.Builder();

        MutableOptionsBundle options = MutableOptionsBundle.create();
        options.insertOption(OPTION, 1);
        builder.addImplementationOptions(options);
        SessionConfig sessionConfig = builder.build();

        Config config = sessionConfig.getImplementationOptions();

        assertThat(config.containsOption(OPTION)).isTrue();
        assertThat(config.retrieveOption(OPTION)).isEqualTo(1);
    }

    @Test
    public void prioritizeTemplateType_previewHigherThanUnsupportedType() {
        SessionConfig.Builder builderPreview = new SessionConfig.Builder();
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        SessionConfig sessionConfigPreview = builderPreview.build();
        SessionConfig.Builder builderZsl = new SessionConfig.Builder();
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
        SessionConfig sessionConfigZsl = builderZsl.build();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        validatingBuilder.add(sessionConfigPreview);
        validatingBuilder.add(sessionConfigZsl);

        assertThat(validatingBuilder.isValid()).isTrue();

        assertThat(validatingBuilder.build().getTemplateType()).isEqualTo(
                CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void prioritizeTemplateType_recordHigherThanPreview() {
        SessionConfig.Builder builderPreview = new SessionConfig.Builder();
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        SessionConfig sessionConfigPreview = builderPreview.build();
        SessionConfig.Builder builderRecord = new SessionConfig.Builder();
        builderRecord.setTemplateType(CameraDevice.TEMPLATE_RECORD);
        SessionConfig sessionConfigRecord = builderRecord.build();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        validatingBuilder.add(sessionConfigPreview);
        validatingBuilder.add(sessionConfigRecord);

        assertThat(validatingBuilder.isValid()).isTrue();

        assertThat(validatingBuilder.build().getTemplateType()).isEqualTo(
                CameraDevice.TEMPLATE_RECORD);
    }

    @Test
    public void conflictingOptions() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        MutableOptionsBundle options0 = MutableOptionsBundle.create();
        options0.insertOption(OPTION, 1);
        builder0.addImplementationOptions(options0);
        SessionConfig config0 = builder0.build();

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        MutableOptionsBundle options1 = MutableOptionsBundle.create();
        options1.insertOption(OPTION, 2);
        builder1.addImplementationOptions(options1);
        SessionConfig config1 = builder1.build();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();

        validatingBuilder.add(config0);
        validatingBuilder.add(config1);

        assertThat(validatingBuilder.isValid()).isFalse();
    }

    @Test
    public void combineTwoSessionsValid() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options0 = MutableOptionsBundle.create();
        options0.insertOption(OPTION, 1);
        builder0.addImplementationOptions(options0);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options1 = MutableOptionsBundle.create();
        options1.insertOption(OPTION_1, "test");
        builder1.addImplementationOptions(options1);

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
        MutableOptionsBundle options0 = MutableOptionsBundle.create();
        options0.insertOption(OPTION, 1);
        builder0.addImplementationOptions(options0);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options1 = MutableOptionsBundle.create();
        options1.insertOption(OPTION_1, "test");
        builder1.addImplementationOptions(options1);

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
        MutableOptionsBundle options0 = MutableOptionsBundle.create();
        options0.insertOption(OPTION, 1);
        builder0.addImplementationOptions(options0);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options1 = MutableOptionsBundle.create();
        options1.insertOption(OPTION_1, "test");
        builder1.addImplementationOptions(options1);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfig sessionConfig = validatingBuilder.build();

        List<DeferrableSurface> surfaces = sessionConfig.getSurfaces();
        assertThat(surfaces).containsExactly(mMockSurface0, mMockSurface1);
    }

    @Test
    public void combineTwoSessionsOptions() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.addSurface(mMockSurface0);
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options0 = MutableOptionsBundle.create();
        options0.insertOption(OPTION, 1);
        builder0.addImplementationOptions(options0);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options1 = MutableOptionsBundle.create();
        options1.insertOption(OPTION_1, "test");
        builder1.addImplementationOptions(options1);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfig sessionConfig = validatingBuilder.build();

        Config config = sessionConfig.getImplementationOptions();

        assertThat(config.retrieveOption(OPTION)).isEqualTo(1);
        assertThat(config.retrieveOption(OPTION_1)).isEqualTo("test");
    }

    @Test
    public void combineTwoSessionsMultiValueSetValid() {
        Option<FakeMultiValueSet> option = Option.create("multiValueSet", FakeMultiValueSet.class);

        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        MutableOptionsBundle options0 = MutableOptionsBundle.create();
        FakeMultiValueSet multiValueSet0 = new FakeMultiValueSet();
        options0.insertOption(option, multiValueSet0);
        builder0.addImplementationOptions(options0);
        SessionConfig config0 = builder0.build();

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        MutableOptionsBundle options1 = MutableOptionsBundle.create();
        FakeMultiValueSet multiValueSet1 = new FakeMultiValueSet();
        options1.insertOption(option, multiValueSet1);
        builder1.addImplementationOptions(options1);
        SessionConfig config1 = builder1.build();

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(config0);
        validatingBuilder.add(config1);

        assertThat(validatingBuilder.isValid()).isTrue();
    }

    @Test
    public void builderAddMultipleRepeatingCameraCaptureCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);

        builder.addRepeatingCameraCaptureCallback(callback0);
        builder.addRepeatingCameraCaptureCallback(callback1);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getRepeatingCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
        assertThat(configuration.getSingleCameraCaptureCallbacks())
                .containsNoneOf(callback0, callback1);
    }

    @Test
    public void builderAddAllRepeatingCameraCaptureCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> callbacks = Lists.newArrayList(callback0, callback1);

        builder.addAllRepeatingCameraCaptureCallbacks(callbacks);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getRepeatingCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
        assertThat(configuration.getSingleCameraCaptureCallbacks())
                .containsNoneOf(callback0, callback1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void repeatingCameraCaptureCallbacks_areImmutable() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        SessionConfig configuration = builder.build();

        configuration.getRepeatingCameraCaptureCallbacks().add(mock(CameraCaptureCallback.class));
    }

    @Test
    public void builderAddMultipleDeviceStateCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraDevice.StateCallback callback0 = mock(CameraDevice.StateCallback.class);
        CameraDevice.StateCallback callback1 = mock(CameraDevice.StateCallback.class);

        builder.addDeviceStateCallback(callback0);
        builder.addDeviceStateCallback(callback1);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getDeviceStateCallbacks()).containsExactly(callback0, callback1);
    }

    @Test
    public void builderAddAllDeviceStateCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraDevice.StateCallback callback0 = mock(CameraDevice.StateCallback.class);
        CameraDevice.StateCallback callback1 = mock(CameraDevice.StateCallback.class);
        List<CameraDevice.StateCallback> callbacks = Lists.newArrayList(callback0, callback1);

        builder.addAllDeviceStateCallbacks(callbacks);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getDeviceStateCallbacks()).containsExactly(callback0, callback1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void deviceStateCallbacks_areImmutable() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        SessionConfig configuration = builder.build();

        configuration.getDeviceStateCallbacks().add(mock(CameraDevice.StateCallback.class));
    }

    @Test
    public void builderAddMultipleSessionStateCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureSession.StateCallback callback0 =
                mock(CameraCaptureSession.StateCallback.class);
        CameraCaptureSession.StateCallback callback1 =
                mock(CameraCaptureSession.StateCallback.class);

        builder.addSessionStateCallback(callback0);
        builder.addSessionStateCallback(callback1);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getSessionStateCallbacks()).containsExactly(callback0, callback1);
    }

    @Test
    public void builderAddAllSessionStateCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureSession.StateCallback callback0 =
                mock(CameraCaptureSession.StateCallback.class);
        CameraCaptureSession.StateCallback callback1 =
                mock(CameraCaptureSession.StateCallback.class);
        List<CameraCaptureSession.StateCallback> callbacks =
                Lists.newArrayList(callback0, callback1);

        builder.addAllSessionStateCallbacks(callbacks);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getSessionStateCallbacks()).containsExactly(callback0, callback1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sessionStateCallbacks_areImmutable() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        SessionConfig configuration = builder.build();

        configuration.getSessionStateCallbacks()
                .add(mock(CameraCaptureSession.StateCallback.class));
    }

    @Test
    public void builderAddMultipleCameraCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback1);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getSingleCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
        assertThat(configuration.getRepeatingCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
    }

    @Test
    public void builderAddAllCameraCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> callbacks = Lists.newArrayList(callback0, callback1);

        builder.addAllCameraCaptureCallbacks(callbacks);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getSingleCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
        assertThat(configuration.getRepeatingCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void singleCameraCaptureCallbacks_areImmutable() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        SessionConfig configuration = builder.build();

        configuration.getSingleCameraCaptureCallbacks().add(mock(CameraCaptureCallback.class));
    }

    @Test
    public void builderAddErrorListener() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        SessionConfig.ErrorListener callback = mock(SessionConfig.ErrorListener.class);

        builder.addErrorListener(callback);

        SessionConfig config = builder.build();

        assertThat(config.getErrorListeners()).contains(callback);
    }

    @Test
    public void combineTwoSessionsCallbacks() {
        SessionConfig.Builder builder0 = new SessionConfig.Builder();
        CameraCaptureSession.StateCallback sessionCallback0 =
                mock(CameraCaptureSession.StateCallback.class);
        CameraDevice.StateCallback deviceCallback0 = mock(CameraDevice.StateCallback.class);
        CameraCaptureCallback repeatingCallback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback cameraCallback0 = mock(CameraCaptureCallback.class);
        SessionConfig.ErrorListener errorListener0 = mock(SessionConfig.ErrorListener.class);
        builder0.addSessionStateCallback(sessionCallback0);
        builder0.addDeviceStateCallback(deviceCallback0);
        builder0.addRepeatingCameraCaptureCallback(repeatingCallback0);
        builder0.addCameraCaptureCallback(cameraCallback0);
        builder0.addErrorListener(errorListener0);

        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        CameraCaptureSession.StateCallback sessionCallback1 =
                mock(CameraCaptureSession.StateCallback.class);
        CameraDevice.StateCallback deviceCallback1 = mock(CameraDevice.StateCallback.class);
        CameraCaptureCallback repeatingCallback1 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback cameraCallback1 = mock(CameraCaptureCallback.class);
        SessionConfig.ErrorListener errorListener1 = mock(SessionConfig.ErrorListener.class);
        builder1.addSessionStateCallback(sessionCallback1);
        builder1.addDeviceStateCallback(deviceCallback1);
        builder1.addRepeatingCameraCaptureCallback(repeatingCallback1);
        builder1.addCameraCaptureCallback(cameraCallback1);
        builder1.addErrorListener(errorListener1);

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(builder0.build());
        validatingBuilder.add(builder1.build());

        SessionConfig sessionConfig = validatingBuilder.build();

        assertThat(sessionConfig.getSessionStateCallbacks())
                .containsExactly(sessionCallback0, sessionCallback1);
        assertThat(sessionConfig.getDeviceStateCallbacks())
                .containsExactly(deviceCallback0, deviceCallback1);
        assertThat(sessionConfig.getRepeatingCameraCaptureCallbacks())
                .containsExactly(
                        repeatingCallback0, cameraCallback0, repeatingCallback1, cameraCallback1);
        assertThat(sessionConfig.getSingleCameraCaptureCallbacks())
                .containsExactly(cameraCallback0, cameraCallback1);
        assertThat(sessionConfig.getErrorListeners()).containsExactly(errorListener0,
                errorListener1);
    }

    @Test
    public void combineTwoSessionsTagsValid() {
        SessionConfig session0 = createSessionConfigWithTag("TEST00", 0);
        SessionConfig session1 = createSessionConfigWithTag("TEST01", "String");

        SessionConfig.ValidatingBuilder validatingBuilder = new SessionConfig.ValidatingBuilder();
        validatingBuilder.add(session0);
        validatingBuilder.add(session1);

        SessionConfig sessionCombined = validatingBuilder.build();

        assertThat(validatingBuilder.isValid()).isTrue();

        TagBundle tag = sessionCombined.getRepeatingCaptureConfig().getTagBundle();

        assertThat(tag.getTag("TEST00")).isEqualTo(0);
        assertThat(tag.getTag("TEST01")).isEqualTo("String");
    }

    private SessionConfig createSessionConfigWithTag(String key, Object tagValue) {
        SessionConfig.Builder builder1 = new SessionConfig.Builder();
        builder1.addSurface(mMockSurface1);
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        builder1.addTag(key, tagValue);

        return builder1.build();
    }
}
