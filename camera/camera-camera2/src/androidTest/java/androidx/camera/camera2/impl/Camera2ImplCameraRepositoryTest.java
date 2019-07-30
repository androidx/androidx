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

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks.DeviceStateCallback;
import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks.SessionStateCallback;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraRepository;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCaseConfig;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Contains tests for {@link androidx.camera.core.CameraRepository} which require an actual
 * implementation to run.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class Camera2ImplCameraRepositoryTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    private CameraRepository mCameraRepository;
    private UseCaseGroup mUseCaseGroup;
    private FakeUseCaseConfig mConfig;
    private CallbackAttachingFakeUseCase mUseCase;
    private CameraFactory mCameraFactory;
    private CountDownLatch mLatchForDeviceClose;
    private CameraDevice.StateCallback mDeviceStateCallback;

    private String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
        try {
            return mCameraFactory.cameraIdForLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());

        mLatchForDeviceClose = new CountDownLatch(1);
        mDeviceStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                mLatchForDeviceClose.countDown();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
            }
        };

        mCameraRepository = new CameraRepository();
        mCameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
        mCameraRepository.init(mCameraFactory);
        mUseCaseGroup = new UseCaseGroup();

        FakeUseCaseConfig.Builder configBuilder =
                new FakeUseCaseConfig.Builder()
                        .setLensFacing(LensFacing.BACK);
        new Camera2Config.Extender(configBuilder).setDeviceStateCallback(mDeviceStateCallback);
        mConfig = configBuilder.build();
        String mCameraId = getCameraIdForLensFacingUnchecked(mConfig.getLensFacing());
        mUseCase = new CallbackAttachingFakeUseCase(mConfig, mCameraId);
        mUseCaseGroup.addUseCase(mUseCase);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (CameraUtil.deviceHasCamera()) {
            mCameraRepository.onGroupInactive(mUseCaseGroup);

            // Wait camera to be closed.
            if (mLatchForDeviceClose != null) {
                mLatchForDeviceClose.await(2, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void cameraDeviceCallsAreForwardedToCallback() throws InterruptedException {
        mUseCase.addStateChangeListener(
                mCameraRepository.getCamera(
                        getCameraIdForLensFacingUnchecked(mConfig.getLensFacing())));
        mUseCase.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        // Wait for the CameraDevice.onOpened callback.
        mUseCase.mDeviceStateCallback.waitForOnOpened(1);

        mCameraRepository.onGroupInactive(mUseCaseGroup);

        // Wait for the CameraDevice.onClosed callback.
        mUseCase.mDeviceStateCallback.waitForOnClosed(1);
    }

    @Test
    public void cameraSessionCallsAreForwardedToCallback() throws InterruptedException {
        mUseCase.addStateChangeListener(
                mCameraRepository.getCamera(
                        getCameraIdForLensFacingUnchecked(mConfig.getLensFacing())));
        mUseCase.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        // Wait for the CameraCaptureSession.onConfigured callback.
        mUseCase.mSessionStateCallback.waitForOnConfigured(1);

        // Camera doesn't currently call CaptureSession.release(), because it is recommended that
        // we don't explicitly call CameraCaptureSession.close(). Rather, we rely on another
        // CameraCaptureSession to get opened. See
        // https://developer.android.com/reference/android/hardware/camera2/CameraCaptureSession
        // .html#close()
    }

    /** A fake use case which attaches to a camera with various callbacks. */
    private static class CallbackAttachingFakeUseCase extends FakeUseCase {
        private final DeviceStateCallback mDeviceStateCallback = new DeviceStateCallback();
        private final SessionStateCallback mSessionStateCallback = new SessionStateCallback();
        private final SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);

        CallbackAttachingFakeUseCase(FakeUseCaseConfig config, String cameraId) {
            super(config);
            // Use most supported resolution for different supported hardware level devices,
            // especially for legacy devices.
            mSurfaceTexture.setDefaultBufferSize(640, 480);

            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            builder.addSurface(new ImmediateSurface(new Surface(mSurfaceTexture)));
            builder.addDeviceStateCallback(mDeviceStateCallback);
            builder.addSessionStateCallback(mSessionStateCallback);

            attachToCamera(cameraId, builder.build());
        }

        // we need to set Camera2OptionUnpacker to the Config to enable the camera2 callback hookup.
        @Override
        protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(CameraX.LensFacing lensFacing) {
            return new FakeUseCaseConfig.Builder()
                    .setLensFacing(lensFacing)
                    .setSessionOptionUnpacker(new Camera2SessionOptionUnpacker());
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            return suggestedResolutionMap;
        }

        void doNotifyActive() {
            super.notifyActive();
        }
    }
}
