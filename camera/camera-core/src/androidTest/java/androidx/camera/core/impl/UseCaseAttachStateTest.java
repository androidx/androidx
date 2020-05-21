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

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.DeferrableSurfacesUtil;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class UseCaseAttachStateTest {
    private final CameraDevice mMockCameraDevice = mock(CameraDevice.class);
    private final CameraCaptureSession mMockCameraCaptureSession =
            mock(CameraCaptureSession.class);

    private String mCameraId;
    private List<TestUseCase> mTestUseCases = new ArrayList<>();

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        CameraXConfig cameraXConfig = FakeAppConfig.create();
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig).get();
        mCameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK);
        if (mCameraId == null) {
            throw new IllegalArgumentException("Unable to attach to camera with LensFacing "
                    + CameraSelector.LENS_FACING_BACK);
        }
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        for (TestUseCase useCase : mTestUseCases) {
            useCase.clear();
        }
        mTestUseCases.clear();
        CameraX.shutdown().get();
    }

    @Test
    public void setSingleUseCaseOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config =
                new FakeUseCaseConfig.Builder()
                        .setTargetName("UseCase")
                        .getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseAttached(fakeUseCase);

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces).containsExactly(fakeUseCase.mSurface);

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(fakeUseCase.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(fakeUseCase.mSessionStateCallback, times(1)).onConfigured(mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(fakeUseCase.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setTwoUseCasesOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config0 = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase0 = createTestUseCase(config0, CameraSelector.DEFAULT_BACK_CAMERA);
        FakeUseCaseConfig config1 = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase1 = createTestUseCase(config1, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseAttached(fakeUseCase0);
        useCaseAttachState.setUseCaseAttached(fakeUseCase1);

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces).containsExactly(fakeUseCase0.mSurface, fakeUseCase1.mSurface);

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(fakeUseCase0.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);
        verify(fakeUseCase1.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(fakeUseCase0.mSessionStateCallback, times(1)).onConfigured(
                mMockCameraCaptureSession);
        verify(fakeUseCase1.mSessionStateCallback, times(1)).onConfigured(
                mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(fakeUseCase0.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
        verify(fakeUseCase1.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseActiveOnly() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseActive(fakeUseCase);

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getActiveAndAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        assertThat(sessionConfig.getSurfaces()).isEmpty();

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(fakeUseCase.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(fakeUseCase.mSessionStateCallback, never()).onConfigured(mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(fakeUseCase.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseActiveAndOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseAttached(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getActiveAndAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.getSurfaces());
        assertThat(surfaces).containsExactly(fakeUseCase.mSurface);

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(fakeUseCase.mDeviceStateCallback, times(1)).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(fakeUseCase.mSessionStateCallback, times(1)).onConfigured(mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(fakeUseCase.mCameraCaptureCallback, times(1)).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseOffline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseAttached(fakeUseCase);
        useCaseAttachState.setUseCaseDetached(fakeUseCase);

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        assertThat(sessionConfig.getSurfaces()).isEmpty();

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(fakeUseCase.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(fakeUseCase.mSessionStateCallback, never()).onConfigured(mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(fakeUseCase.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseInactive() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseAttached(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);
        useCaseAttachState.setUseCaseInactive(fakeUseCase);

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getActiveAndAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        assertThat(sessionConfig.getSurfaces()).isEmpty();

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(fakeUseCase.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(fakeUseCase.mSessionStateCallback, never()).onConfigured(mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(fakeUseCase.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void updateUseCase() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_BACK_CAMERA);

        useCaseAttachState.setUseCaseAttached(fakeUseCase);
        useCaseAttachState.setUseCaseActive(fakeUseCase);

        // The original template should be PREVIEW.
        SessionConfig firstSessionConfig = useCaseAttachState.getActiveAndAttachedBuilder().build();
        assertThat(firstSessionConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);

        // Change the template to STILL_CAPTURE.
        SessionConfig.Builder builder = new SessionConfig.Builder();
        builder.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);
        fakeUseCase.updateSessionConfig(builder.build());

        useCaseAttachState.updateUseCase(fakeUseCase);

        // The new template should be STILL_CAPTURE.
        SessionConfig secondSessionConfig =
                useCaseAttachState.getActiveAndAttachedBuilder().build();
        assertThat(secondSessionConfig.getTemplateType())
                .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setUseCaseOnlineWithWrongCamera() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));

        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_FRONT_CAMERA);

        // Should throw IllegalArgumentException
        useCaseAttachState.setUseCaseAttached(fakeUseCase);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setUseCaseActiveWithWrongCamera() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));

        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        FakeUseCaseConfig config = new FakeUseCaseConfig.Builder().setTargetName(
                "UseCase").getUseCaseConfig();
        TestUseCase fakeUseCase = createTestUseCase(config, CameraSelector.DEFAULT_FRONT_CAMERA);

        // Should throw IllegalArgumentException
        useCaseAttachState.setUseCaseActive(fakeUseCase);
    }

    private TestUseCase createTestUseCase(FakeUseCaseConfig config, CameraSelector selector) {
        TestUseCase testUseCase = new TestUseCase(config, selector);
        mTestUseCases.add(testUseCase);
        return testUseCase;
    }

    private static class TestUseCase extends FakeUseCase {
        private final Surface mSurface = mock(Surface.class);
        private final CameraDevice.StateCallback mDeviceStateCallback =
                mock(CameraDevice.StateCallback.class);
        private final CameraCaptureSession.StateCallback mSessionStateCallback =
                mock(CameraCaptureSession.StateCallback.class);
        private final CameraCaptureCallback mCameraCaptureCallback =
                mock(CameraCaptureCallback.class);
        private DeferrableSurface mDeferrableSurface;

        TestUseCase(FakeUseCaseConfig config, CameraSelector selector) {
            super(config);
            onAttach(CameraX.getCameraWithCameraSelector(selector));
            updateSuggestedResolution(new Size(640, 480));
        }

        @Override
        @NonNull
        protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
            mDeferrableSurface = new ImmediateSurface(mSurface);
            builder.addSurface(mDeferrableSurface);
            builder.addDeviceStateCallback(mDeviceStateCallback);
            builder.addSessionStateCallback(mSessionStateCallback);
            builder.addRepeatingCameraCaptureCallback(mCameraCaptureCallback);

            updateSessionConfig(builder.build());

            return suggestedResolution;
        }

        @Override
        public void clear() {
            super.clear();
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
        }

        @Override
        public void updateSessionConfig(@NonNull SessionConfig sessionConfig) {
            super.updateSessionConfig(sessionConfig);
        }

    }
}
