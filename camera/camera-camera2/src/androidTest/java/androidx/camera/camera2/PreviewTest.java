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

package androidx.camera.camera2;

import static androidx.camera.core.PreviewSurfaceProviders.createSurfaceTextureProvider;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInternal;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeferrableSurfaces;
import androidx.camera.core.LensFacing;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.PreviewSurfaceProviders;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class PreviewTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    // Use most supported resolution for different supported hardware level devices,
    // especially for legacy devices.
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size SECONDARY_RESOLUTION = new Size(320, 240);

    private static final Preview.PreviewSurfaceCallback MOCK_PREVIEW_SURFACE_CALLBACK =
            mock(Preview.PreviewSurfaceCallback.class);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private CameraInternal mCameraInternal;

    private PreviewConfig mDefaultConfig;
    private String mCameraId;
    private Semaphore mSurfaceFutureSemaphore;
    private Semaphore mSaveToReleaseSemaphore;
    private Preview.PreviewSurfaceCallback mPreviewSurfaceCallbackWithFrameAvailableListener =
            createSurfaceTextureProvider(new PreviewSurfaceProviders.SurfaceTextureCallback() {
                @Override
                public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                        @NonNull Size resolution) {
                    surfaceTexture.setOnFrameAvailableListener(
                            surfaceTexture1 -> mSurfaceFutureSemaphore.release());
                }

                @Override
                public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                    surfaceTexture.release();
                    mSaveToReleaseSemaphore.release();
                }
            });


    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        Context context = ApplicationProvider.getApplicationContext();
        AppConfig appConfig = Camera2AppConfig.create(context);
        CameraFactory cameraFactory = appConfig.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        CameraX.initialize(context, appConfig);
        mCameraInternal = cameraFactory.getCamera(mCameraId);

        // init CameraX before creating Preview to get preview size with CameraX's context
        mDefaultConfig = Preview.DEFAULT_CONFIG.getConfig(LensFacing.BACK);
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
        mSaveToReleaseSemaphore = new Semaphore(/*permits=*/ 0);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }

        // Ensure all cameras are released for the next test
        CameraX.shutdown().get();
        if (mCameraInternal != null) {
            mCameraInternal.release().get();
        }
    }

    @Test
    @UiThreadTest
    public void getAndSetPreviewSurfaceCallback() {
        Preview preview = new Preview(mDefaultConfig);
        preview.setPreviewSurfaceCallback(MOCK_PREVIEW_SURFACE_CALLBACK);
        assertThat(preview.getPreviewSurfaceCallback()).isEqualTo(MOCK_PREVIEW_SURFACE_CALLBACK);
    }

    @Test
    @UiThreadTest
    public void removePreviewSurfaceCallback() {
        Preview preview = new Preview(mDefaultConfig);
        preview.setPreviewSurfaceCallback(MOCK_PREVIEW_SURFACE_CALLBACK);
        preview.setPreviewSurfaceCallback(null);
        assertThat(preview.getPreviewSurfaceCallback()).isNull();
    }

    @Test
    @UiThreadTest
    public void useCaseIsConstructedWithDefaultConfiguration() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.setPreviewSurfaceCallback(CameraXExecutors.highPriorityExecutor(),
                mPreviewSurfaceCallbackWithFrameAvailableListener);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(useCase.getSessionConfig(mCameraId).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0).isValid()).isTrue();
    }

    @Test
    @UiThreadTest
    public void useCaseIsConstructedWithCustomConfiguration() {
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(LensFacing.BACK).build();
        Preview useCase = new Preview(config);
        useCase.setPreviewSurfaceCallback(CameraXExecutors.highPriorityExecutor(),
                mPreviewSurfaceCallbackWithFrameAvailableListener);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(useCase.getSessionConfig(mCameraId).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0).isValid()).isTrue();
    }

    @Test
    @UiThreadTest
    public void torchModeCanBeSet() {
        Preview useCase = new Preview(mDefaultConfig);
        CameraControlInternal cameraControl = getFakeCameraControl();
        useCase.attachCameraControl(mCameraId, cameraControl);

        useCase.enableTorch(true);

        assertThat(useCase.isTorchOn()).isTrue();
    }

    @Test
    @UiThreadTest
    public void updateSessionConfigWithSuggestedResolution() {
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(LensFacing.BACK).build();
        Preview useCase = new Preview(config);
        useCase.setPreviewSurfaceCallback(CameraXExecutors.highPriorityExecutor(),
                mPreviewSurfaceCallbackWithFrameAvailableListener);

        final Size[] sizes = {DEFAULT_RESOLUTION, SECONDARY_RESOLUTION};

        for (Size size : sizes) {
            useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, size));

            List<Surface> surfaces =
                    DeferrableSurfaces.surfaceList(
                            useCase.getSessionConfig(mCameraId).getSurfaces());

            assertWithMessage("Failed at Size: " + size).that(surfaces).hasSize(1);
            assertWithMessage("Failed at Size: " + size).that(surfaces.get(0).isValid()).isTrue();
        }
    }

    @Test
    public void previewDetached_onSafeToReleaseCalled() throws InterruptedException {
        // Arrange.
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(
                LensFacing.BACK).build();
        Preview preview = new Preview(config);

        // Act.
        mInstrumentation.runOnMainSync(() -> {
            preview.setPreviewSurfaceCallback(CameraXExecutors.highPriorityExecutor(),
                    mPreviewSurfaceCallbackWithFrameAvailableListener);
            preview.updateSuggestedResolution(
                    Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
            CameraUtil.openCameraWithUseCase(mCameraId, mCameraInternal, preview);
        });
        // Wait until preview gets frame.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
        // Clear and detach to trigger release.
        preview.clear();
        CameraUtil.detachUseCaseFromCamera(mCameraInternal, preview);

        // Assert.
        assertThat(mSaveToReleaseSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void updateSuggestedResolution_getsFrame() throws InterruptedException {
        mInstrumentation.runOnMainSync(() -> {
            // Arrange.
            Preview preview = new Preview(mDefaultConfig);
            preview.setPreviewSurfaceCallback(mPreviewSurfaceCallbackWithFrameAvailableListener);

            // Act.
            preview.updateSuggestedResolution(
                    Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
            CameraUtil.openCameraWithUseCase(mCameraId, mCameraInternal, preview);

        });

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setPreviewSurfaceCallback_getsFrame() throws InterruptedException {
        mInstrumentation.runOnMainSync(() -> {
            // Arrange.
            Preview preview = new Preview(mDefaultConfig);
            preview.updateSuggestedResolution(
                    Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

            // Act.
            preview.setPreviewSurfaceCallback(mPreviewSurfaceCallbackWithFrameAvailableListener);
            CameraUtil.openCameraWithUseCase(mCameraId, mCameraInternal, preview);
        });

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    private CameraControlInternal getFakeCameraControl() {
        return new FakeCameraControl(new CameraControlInternal.ControlUpdateCallback() {
            @Override
            public void onCameraControlUpdateSessionConfig(@NonNull SessionConfig sessionConfig) {
            }

            @Override
            public void onCameraControlCaptureRequests(
                    @NonNull List<CaptureConfig> captureConfigs) {
            }
        });
    }
}
