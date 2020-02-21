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

import static androidx.camera.testing.SurfaceTextureProvider.createSurfaceTextureProvider;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    private static final Preview.SurfaceProvider MOCK_PREVIEW_SURFACE_PROVIDER =
            mock(Preview.SurfaceProvider.class);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private Preview.Builder mDefaultBuilder;

    private CameraSelector mCameraSelector;
    private FakeLifecycleOwner mLifecycleOwner;
    private Size mPreviewResolution;
    private Semaphore mSurfaceFutureSemaphore;
    private Semaphore mSaveToReleaseSemaphore;
    private Preview.SurfaceProvider mSurfaceProviderWithFrameAvailableListener =
            createSurfaceTextureProvider(new SurfaceTextureProvider.SurfaceTextureCallback() {
                @Override
                public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                        @NonNull Size resolution) {
                    mPreviewResolution = resolution;
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
    public void setUp() throws ExecutionException, InterruptedException {
        assumeTrue(CameraUtil.deviceHasCamera());
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = Camera2Config.defaultConfig();
        CameraX.initialize(context, cameraXConfig).get();

        // init CameraX before creating Preview to get preview size with CameraX's context
        mDefaultBuilder = Preview.Builder.fromConfig(Preview.DEFAULT_CONFIG.getConfig(null));
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
        mSaveToReleaseSemaphore = new Semaphore(/*permits=*/ 0);
        mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        mLifecycleOwner = new FakeLifecycleOwner();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }

        // Ensure all cameras are released for the next test
        CameraX.shutdown().get();
    }

    @Test
    public void surfaceProvider_isUsedAfterSetting() {
        Preview.SurfaceProvider surfaceProvider = mock(Preview.SurfaceProvider.class);
        doAnswer(args -> ((SurfaceRequest) args.getArgument(0)).willNotProvideSurface()).when(
                surfaceProvider).onSurfaceRequested(
                any(SurfaceRequest.class));

        mInstrumentation.runOnMainSync(() -> {
            Preview preview = mDefaultBuilder.build();
            preview.setSurfaceProvider(surfaceProvider);

            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            mLifecycleOwner.startAndResume();
        });

        verify(surfaceProvider, timeout(3000)).onSurfaceRequested(
                any(SurfaceRequest.class));
    }

    @Test
    public void previewDetached_onSafeToReleaseCalled() throws InterruptedException {
        // Arrange.
        Preview preview = new Preview.Builder().build();

        // Act.
        mInstrumentation.runOnMainSync(() -> {
            preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor(),
                    mSurfaceProviderWithFrameAvailableListener);
            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
        });

        mLifecycleOwner.startAndResume();

        // Wait until preview gets frame.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
        // Destroy lifecycle to trigger release.
        mLifecycleOwner.pauseAndStop();
        mLifecycleOwner.destroy();

        // Assert.
        assertThat(mSaveToReleaseSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setPreviewSurfaceProviderBeforeBind_getsFrame() throws InterruptedException {
        mInstrumentation.runOnMainSync(() -> {
            // Arrange.
            Preview preview = mDefaultBuilder.build();
            preview.setSurfaceProvider(mSurfaceProviderWithFrameAvailableListener);

            // Act.
            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            mLifecycleOwner.startAndResume();
        });

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Suppress // TODO(b/143703289): Remove suppression once provider can be set after bind
    @Test
    public void setPreviewSurfaceProviderAfterBind_getsFrame() throws InterruptedException {
        mInstrumentation.runOnMainSync(() -> {
            // Arrange.
            Preview preview = mDefaultBuilder.build();
            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);

            // Act.
            preview.setSurfaceProvider(mSurfaceProviderWithFrameAvailableListener);
            mLifecycleOwner.startAndResume();
        });

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void canSupportGuaranteedSize()
            throws InterruptedException, CameraInfoUnavailableException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        for (int i = 0; i <= 1; i++) {
            final int lensFacing = i;
            if (!CameraUtil.hasCameraWithLensFacing(lensFacing)) {
                continue;
            }

            // Checks camera device sensor degrees to set correct target rotation value to make sure
            // the exactly matching result size 640x480 can be selected if the device supports it.
            String cameraId = CameraX.getCameraFactory().cameraIdForLensFacing(lensFacing);
            boolean isRotateNeeded = (CameraX.getCameraInfo(cameraId).getSensorRotationDegrees(
                    Surface.ROTATION_0) % 180) != 0;
            Preview preview = new Preview.Builder().setTargetResolution(
                    GUARANTEED_RESOLUTION).setTargetRotation(
                    isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();

            mInstrumentation.runOnMainSync(() -> {
                preview.setSurfaceProvider(
                        mSurfaceProviderWithFrameAvailableListener);
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(
                        lensFacing).build();
                CameraX.bindToLifecycle(mLifecycleOwner, cameraSelector, preview);
                mLifecycleOwner.startAndResume();
            });

            // Assert.
            assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

            // Check whether 640x480 is selected for the preview use case. This test can also check
            // whether the guaranteed resolution 640x480 is really supported for SurfaceTexture
            // format on the devices when running the test.
            assertEquals(GUARANTEED_RESOLUTION, mPreviewResolution);

            // Reset the environment to run test for the other lens facing camera device.
            mInstrumentation.runOnMainSync(() -> {
                CameraX.unbindAll();
                mLifecycleOwner.pauseAndStop();
            });
        }
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
