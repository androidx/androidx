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

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.LensFacing;
import androidx.camera.core.Preview;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.core.util.Preconditions;
import androidx.test.annotation.UiThreadTest;
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

    private static final Preview.PreviewSurfaceProvider MOCK_PREVIEW_SURFACE_PROVIDER =
            mock(Preview.PreviewSurfaceProvider.class);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private Preview.Builder mDefaultBuilder;

    private CameraSelector mCameraSelector;
    private String mCameraId;
    private FakeLifecycleOwner mLifecycleOwner;
    private Semaphore mSurfaceFutureSemaphore;
    private Semaphore mSaveToReleaseSemaphore;
    private Preview.PreviewSurfaceProvider mPreviewSurfaceProviderWithFrameAvailableListener =
            createSurfaceTextureProvider(new SurfaceTextureProvider.SurfaceTextureCallback() {
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
        CameraXConfig cameraXConfig = Camera2Config.defaultConfig(context);
        CameraX.initialize(context, cameraXConfig);
        CameraFactory cameraFactory = Preconditions.checkNotNull(cameraXConfig.getCameraFactory(
                /*valueIfMissing=*/ null));
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }

        // init CameraX before creating Preview to get preview size with CameraX's context
        mDefaultBuilder = Preview.Builder.fromConfig(
                Preview.DEFAULT_CONFIG.getConfig(LensFacing.BACK));
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
        mSaveToReleaseSemaphore = new Semaphore(/*permits=*/ 0);
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(LensFacing.BACK).build();
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
    @UiThreadTest
    public void getAndSetPreviewSurfaceProvider() {
        Preview preview = mDefaultBuilder.build();
        preview.setPreviewSurfaceProvider(MOCK_PREVIEW_SURFACE_PROVIDER);
        assertThat(preview.getPreviewSurfaceProvider()).isEqualTo(MOCK_PREVIEW_SURFACE_PROVIDER);
    }

    @Test
    @UiThreadTest
    public void removePreviewSurfaceProvider() {
        Preview preview = mDefaultBuilder.build();
        preview.setPreviewSurfaceProvider(MOCK_PREVIEW_SURFACE_PROVIDER);
        preview.setPreviewSurfaceProvider(null);
        assertThat(preview.getPreviewSurfaceProvider()).isNull();
    }

    @Test
    public void previewDetached_onSafeToReleaseCalled() throws InterruptedException {
        // Arrange.
        Preview preview = new Preview.Builder().build();

        // Act.
        mInstrumentation.runOnMainSync(() -> {
            preview.setPreviewSurfaceProvider(CameraXExecutors.mainThreadExecutor(),
                    mPreviewSurfaceProviderWithFrameAvailableListener);
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
            preview.setPreviewSurfaceProvider(mPreviewSurfaceProviderWithFrameAvailableListener);

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
            preview.setPreviewSurfaceProvider(mPreviewSurfaceProviderWithFrameAvailableListener);
            mLifecycleOwner.startAndResume();
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
