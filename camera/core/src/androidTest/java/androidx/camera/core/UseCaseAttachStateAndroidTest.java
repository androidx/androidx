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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.testing.fakes.FakeAppConfiguration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class UseCaseAttachStateAndroidTest {
    private final LensFacing cameraLensFacing0 = LensFacing.BACK;
    private final LensFacing cameraLensFacing1 = LensFacing.FRONT;
    private final CameraDevice mockCameraDevice = Mockito.mock(CameraDevice.class);
    private final CameraCaptureSession mockCameraCaptureSession =
            Mockito.mock(CameraCaptureSession.class);

    private String cameraId;

    @Before
    public void setUp() {
        AppConfiguration appConfiguration = FakeAppConfiguration.create();
        CameraFactory cameraFactory = appConfiguration.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            cameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        CameraX.init(ApplicationProvider.getApplicationContext(), appConfiguration);
    }

    @Test
    public void setSingleUseCaseOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder = useCaseAttachState.getOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))
                .containsExactly(fakeUseCase.surface);

        sessionConfiguration.getDeviceStateCallback().onOpened(mockCameraDevice);
        verify(fakeUseCase.deviceStateCallback, times(1)).onOpened(mockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase.sessionStateCallback, times(1)).onConfigured(mockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.cameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setTwoUseCasesOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration0 =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase0 = new TestUseCase(configuration0, cameraId);
        FakeUseCaseConfiguration configuration1 =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase1 = new TestUseCase(configuration1, cameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase0);
        useCaseAttachState.setUseCaseOnline(fakeUseCase1);

        SessionConfiguration.ValidatingBuilder builder = useCaseAttachState.getOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))
                .containsExactly(fakeUseCase0.surface, fakeUseCase1.surface);

        sessionConfiguration.getDeviceStateCallback().onOpened(mockCameraDevice);
        verify(fakeUseCase0.deviceStateCallback, times(1)).onOpened(mockCameraDevice);
        verify(fakeUseCase1.deviceStateCallback, times(1)).onOpened(mockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase0.sessionStateCallback, times(1)).onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase1.sessionStateCallback, times(1)).onConfigured(mockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase0.cameraCaptureCallback, times(1)).onCaptureCompleted(null);
        verify(fakeUseCase1.cameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseActiveOnly() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        useCaseAttachState.setUseCaseActive(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder =
                useCaseAttachState.getActiveAndOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(sessionConfiguration.getSurfaces()).isEmpty();

        sessionConfiguration.getDeviceStateCallback().onOpened(mockCameraDevice);
        verify(fakeUseCase.deviceStateCallback, never()).onOpened(mockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase.sessionStateCallback, never()).onConfigured(mockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.cameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseActiveAndOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder =
                useCaseAttachState.getActiveAndOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(DeferrableSurfaces.surfaceList(sessionConfiguration.getSurfaces()))
                .containsExactly(fakeUseCase.surface);

        sessionConfiguration.getDeviceStateCallback().onOpened(mockCameraDevice);
        verify(fakeUseCase.deviceStateCallback, times(1)).onOpened(mockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase.sessionStateCallback, times(1)).onConfigured(mockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.cameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseOffline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseOffline(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder = useCaseAttachState.getOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(sessionConfiguration.getSurfaces()).isEmpty();

        sessionConfiguration.getDeviceStateCallback().onOpened(mockCameraDevice);
        verify(fakeUseCase.deviceStateCallback, never()).onOpened(mockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase.sessionStateCallback, never()).onConfigured(mockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.cameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseInactive() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);
        useCaseAttachState.setUseCaseInactive(fakeUseCase);

        SessionConfiguration.ValidatingBuilder builder =
                useCaseAttachState.getActiveAndOnlineBuilder();
        SessionConfiguration sessionConfiguration = builder.build();
        assertThat(sessionConfiguration.getSurfaces()).isEmpty();

        sessionConfiguration.getDeviceStateCallback().onOpened(mockCameraDevice);
        verify(fakeUseCase.deviceStateCallback, never()).onOpened(mockCameraDevice);

        sessionConfiguration.getSessionStateCallback().onConfigured(mockCameraCaptureSession);
        verify(fakeUseCase.sessionStateCallback, never()).onConfigured(mockCameraCaptureSession);

        sessionConfiguration.getCameraCaptureCallback().onCaptureCompleted(null);
        verify(fakeUseCase.cameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void updateUseCase() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing0)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        useCaseAttachState.setUseCaseOnline(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);

        // The original template should be PREVIEW.
        SessionConfiguration firstSessionConfiguration =
                useCaseAttachState.getActiveAndOnlineBuilder().build();
        assertThat(firstSessionConfiguration.getTemplateType())
                .isEqualTo(CameraDevice.TEMPLATE_PREVIEW);

        // Change the template to STILL_CAPTURE.
        SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
        builder.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);
        fakeUseCase.attachToCamera(cameraId, builder.build());

        useCaseAttachState.updateUseCase(fakeUseCase);

        // The new template should be STILL_CAPTURE.
        SessionConfiguration secondSessionConfiguration =
                useCaseAttachState.getActiveAndOnlineBuilder().build();
        assertThat(secondSessionConfiguration.getTemplateType())
                .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE);
    }

    @Test
    public void setUseCaseOnlineWithWrongCamera() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing1)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCaseAttachState.setUseCaseOnline(fakeUseCase));
    }

    @Test
    public void setUseCaseActiveWithWrongCamera() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(cameraId);
        FakeUseCaseConfiguration configuration =
                new FakeUseCaseConfiguration.Builder()
                        .setTargetName("UseCase")
                        .setLensFacing(cameraLensFacing1)
                        .build();
        TestUseCase fakeUseCase = new TestUseCase(configuration, cameraId);

        assertThrows(
                IllegalArgumentException.class,
                () -> useCaseAttachState.setUseCaseActive(fakeUseCase));
    }

    private static class TestUseCase extends FakeUseCase {
        private final Surface surface = Mockito.mock(Surface.class);
        private final CameraDevice.StateCallback deviceStateCallback =
                Mockito.mock(CameraDevice.StateCallback.class);
        private final CameraCaptureSession.StateCallback sessionStateCallback =
                Mockito.mock(CameraCaptureSession.StateCallback.class);
        private final CameraCaptureCallback cameraCaptureCallback =
                Mockito.mock(CameraCaptureCallback.class);

        TestUseCase(FakeUseCaseConfiguration configuration, String cameraId) {
            super(configuration);
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(cameraId, new Size(640, 480));
            updateSuggestedResolution(suggestedResolutionMap);
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            builder.addSurface(new ImmediateSurface(surface));
            builder.setDeviceStateCallback(deviceStateCallback);
            builder.setSessionStateCallback(sessionStateCallback);
            builder.setCameraCaptureCallback(cameraCaptureCallback);

            LensFacing lensFacing =
                    ((CameraDeviceConfiguration) getUseCaseConfiguration()).getLensFacing();
            try {
                String cameraId = CameraX.getCameraWithLensFacing(lensFacing);
                attachToCamera(cameraId, builder.build());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Unable to attach to camera with LensFacing " + lensFacing, e);
            }
            return suggestedResolutionMap;
        }
    }
}
