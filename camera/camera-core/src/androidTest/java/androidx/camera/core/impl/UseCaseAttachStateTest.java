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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;

import androidx.camera.testing.DeferrableSurfacesUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class UseCaseAttachStateTest {
    private final CameraDevice mMockCameraDevice = mock(CameraDevice.class);
    private final CameraCaptureSession mMockCameraCaptureSession =
            mock(CameraCaptureSession.class);

    private String mCameraId = "cameraId";

    @Test
    public void setSingleUseCaseOnline() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        TestUseCaseDataProvider fakeUseCase = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseAttached(fakeUseCase.getName(),
                fakeUseCase.getSessionConfig());

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
        TestUseCaseDataProvider fakeUseCase0 = new TestUseCaseDataProvider();
        TestUseCaseDataProvider fakeUseCase1 = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseAttached(fakeUseCase0.getName(),
                fakeUseCase0.getSessionConfig());
        useCaseAttachState.setUseCaseAttached(fakeUseCase1.getName(),
                fakeUseCase1.getSessionConfig());

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
        TestUseCaseDataProvider fakeUseCase = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseActive(fakeUseCase.getName(), fakeUseCase.getSessionConfig());

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
        TestUseCaseDataProvider fakeUseCase = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseAttached(fakeUseCase.getName(),
                fakeUseCase.getSessionConfig());
        useCaseAttachState.setUseCaseActive(fakeUseCase.getName(), fakeUseCase.getSessionConfig());

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
        TestUseCaseDataProvider testUseCaseDataProvider = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseAttached(testUseCaseDataProvider.getName(),
                testUseCaseDataProvider.getSessionConfig());
        useCaseAttachState.setUseCaseDetached(testUseCaseDataProvider.getName());

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        assertThat(sessionConfig.getSurfaces()).isEmpty();

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(testUseCaseDataProvider.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(testUseCaseDataProvider.mSessionStateCallback, never()).onConfigured(
                mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(testUseCaseDataProvider.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void setUseCaseInactive() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        TestUseCaseDataProvider testUseCaseDataProvider = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseAttached(testUseCaseDataProvider.getName(),
                testUseCaseDataProvider.getSessionConfig());
        useCaseAttachState.setUseCaseActive(testUseCaseDataProvider.getName(),
                testUseCaseDataProvider.getSessionConfig());
        useCaseAttachState.setUseCaseInactive(testUseCaseDataProvider.getName());

        SessionConfig.ValidatingBuilder builder = useCaseAttachState.getActiveAndAttachedBuilder();
        SessionConfig sessionConfig = builder.build();
        assertThat(sessionConfig.getSurfaces()).isEmpty();

        for (CameraDevice.StateCallback callback : sessionConfig.getDeviceStateCallbacks()) {
            callback.onOpened(mMockCameraDevice);
        }
        verify(testUseCaseDataProvider.mDeviceStateCallback, never()).onOpened(mMockCameraDevice);

        for (CameraCaptureSession.StateCallback callback
                : sessionConfig.getSessionStateCallbacks()) {
            callback.onConfigured(mMockCameraCaptureSession);
        }
        verify(testUseCaseDataProvider.mSessionStateCallback, never()).onConfigured(
                mMockCameraCaptureSession);

        for (CameraCaptureCallback callback : sessionConfig.getRepeatingCameraCaptureCallbacks()) {
            callback.onCaptureCompleted(null);
        }
        verify(testUseCaseDataProvider.mCameraCaptureCallback, never()).onCaptureCompleted(null);
    }

    @Test
    public void retainUseCaseAttachedOrder() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);

        List<SessionConfig> sessionConfigs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestUseCaseDataProvider fakeUseCase = new TestUseCaseDataProvider();
            useCaseAttachState.setUseCaseAttached(fakeUseCase.getName(),
                    fakeUseCase.getSessionConfig());
            sessionConfigs.add(fakeUseCase.getSessionConfig());
        }

        List<SessionConfig> attachedSessionConfigs =
                new ArrayList<>(useCaseAttachState.getAttachedSessionConfigs());

        assertThat(attachedSessionConfigs).isEqualTo(sessionConfigs);
    }

    @Test
    public void updateUseCase() {
        UseCaseAttachState useCaseAttachState = new UseCaseAttachState(mCameraId);
        TestUseCaseDataProvider testUseCaseDataProvider = new TestUseCaseDataProvider();

        useCaseAttachState.setUseCaseAttached(testUseCaseDataProvider.getName(),
                testUseCaseDataProvider.getSessionConfig());
        useCaseAttachState.setUseCaseActive(testUseCaseDataProvider.getName(),
                testUseCaseDataProvider.getSessionConfig());

        // The original template should be PREVIEW.
        SessionConfig firstSessionConfig = useCaseAttachState.getActiveAndAttachedBuilder().build();
        assertThat(firstSessionConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);

        // Change the template to STILL_CAPTURE.
        testUseCaseDataProvider.setTemplateType(CameraDevice.TEMPLATE_STILL_CAPTURE);

        useCaseAttachState.updateUseCase(testUseCaseDataProvider.getName(),
                testUseCaseDataProvider.getSessionConfig());

        // The new template should be STILL_CAPTURE.
        SessionConfig secondSessionConfig =
                useCaseAttachState.getActiveAndAttachedBuilder().build();
        assertThat(secondSessionConfig.getTemplateType())
                .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE);
    }

    private static class TestUseCaseDataProvider {
        private final Surface mSurface = mock(Surface.class);
        private final CameraDevice.StateCallback mDeviceStateCallback =
                mock(CameraDevice.StateCallback.class);
        private final CameraCaptureSession.StateCallback mSessionStateCallback =
                mock(CameraCaptureSession.StateCallback.class);
        private final CameraCaptureCallback mCameraCaptureCallback =
                mock(CameraCaptureCallback.class);
        private DeferrableSurface mDeferrableSurface;
        private int mTemplateType = CameraDevice.TEMPLATE_PREVIEW;

        private SessionConfig mSessionConfig;

        TestUseCaseDataProvider() {
            buildSessionConfig();
        }

        void setTemplateType(int templateType) {
            mTemplateType = templateType;
            buildSessionConfig();
        }

        SessionConfig getSessionConfig() {
            return mSessionConfig;
        }

        String getName() {
            return toString();
        }

        private void buildSessionConfig() {
            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(mTemplateType);
            if (mDeferrableSurface != null) {
                mDeferrableSurface.close();
            }
            mDeferrableSurface = new ImmediateSurface(mSurface);
            builder.addSurface(mDeferrableSurface);
            builder.addDeviceStateCallback(mDeviceStateCallback);
            builder.addSessionStateCallback(mSessionStateCallback);
            builder.addRepeatingCameraCaptureCallback(mCameraCaptureCallback);

            mSessionConfig = builder.build();
        }
    }
}
