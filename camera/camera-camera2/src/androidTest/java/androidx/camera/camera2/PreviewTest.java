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

import static androidx.camera.core.PreviewUtil.createPreviewSurfaceCallback;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Build;
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
import androidx.camera.core.Preview.OnPreviewOutputUpdateListener;
import androidx.camera.core.Preview.PreviewOutput;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.PreviewUtil;
import androidx.camera.core.SessionConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private static final Preview.OnPreviewOutputUpdateListener
            MOCK_ON_PREVIEW_OUTPUT_UPDATE_LISTENER =
            mock(Preview.OnPreviewOutputUpdateListener.class);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private CameraInternal mCameraInternal;

    private PreviewConfig mDefaultConfig;
    @Mock
    private OnPreviewOutputUpdateListener mMockListener;
    private String mCameraId;
    private Semaphore mSurfaceFutureSemaphore;
    private Preview.PreviewSurfaceCallback mPreviewSurfaceCallbackWithFrameAvailableListener =
            createPreviewSurfaceCallback(new PreviewUtil.SurfaceTextureCallback() {
                @Override
                public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture) {
                    surfaceTexture.setOnFrameAvailableListener(
                            surfaceTexture1 -> mSurfaceFutureSemaphore.release());
                }

                @Override
                public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                    surfaceTexture.release();
                }
            });


    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        // Instantiates OnPreviewOutputUpdateListener before each test run.
        mMockListener = mock(OnPreviewOutputUpdateListener.class);
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

    @Test(expected = IllegalStateException.class)
    @UiThreadTest
    public void setPreviewSurfaceCallbackThenOnPreviewOutputUpdateListener_throwsException() {
        Preview preview = new Preview(mDefaultConfig);
        preview.setPreviewSurfaceCallback(MOCK_PREVIEW_SURFACE_CALLBACK);
        preview.setOnPreviewOutputUpdateListener(MOCK_ON_PREVIEW_OUTPUT_UPDATE_LISTENER);
    }

    @Test(expected = IllegalStateException.class)
    @UiThreadTest
    public void setOnPreviewOutputUpdateListenerThenPreviewSurfaceCallback_throwsException() {
        Preview preview = new Preview(mDefaultConfig);
        preview.setOnPreviewOutputUpdateListener(MOCK_ON_PREVIEW_OUTPUT_UPDATE_LISTENER);
        preview.setPreviewSurfaceCallback(MOCK_PREVIEW_SURFACE_CALLBACK);
    }

    @FlakyTest
    @Test
    @UiThreadTest
    public void useCaseIsConstructedWithDefaultConfiguration() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(useCase.getSessionConfig(mCameraId).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0).isValid()).isTrue();
    }

    @FlakyTest
    @Test
    @UiThreadTest
    public void useCaseIsConstructedWithCustomConfiguration() {
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(LensFacing.BACK).build();
        Preview useCase = new Preview(config);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(useCase.getSessionConfig(mCameraId).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0).isValid()).isTrue();
    }

    @Test
    @UiThreadTest
    public void zoomRegionCanBeSet() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        CameraControlInternal cameraControl = mock(CameraControlInternal.class);
        useCase.attachCameraControl(mCameraId, cameraControl);

        Rect rect = new Rect(/*left=*/ 200, /*top=*/ 200, /*right=*/ 800, /*bottom=*/ 800);
        useCase.zoom(rect);

        ArgumentCaptor<Rect> rectArgumentCaptor = ArgumentCaptor.forClass(Rect.class);
        verify(cameraControl).setCropRegion(rectArgumentCaptor.capture());
        assertThat(rectArgumentCaptor.getValue()).isEqualTo(rect);
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

    @FlakyTest
    @Test
    @UiThreadTest
    public void surfaceTextureIsNotReleased()
            throws InterruptedException, ExecutionException, TimeoutException {
        // This test only target SDK >= 26
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        SurfaceTexture surfaceTexture0 = future0.get(1, TimeUnit.SECONDS);
        surfaceTexture0.release();

        final SurfaceTextureCallable surfaceTextureCallable1 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future1 = new FutureTask<>(surfaceTextureCallable1);
        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable1.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future1.run();
                    }
                });
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture1 = future1.get(1, TimeUnit.SECONDS);

        assertThat(surfaceTexture1.isReleased()).isFalse();
    }

    @Test
    @UiThreadTest
    public void listenedSurfaceTextureIsNotReleased_whenCleared()
            throws InterruptedException, ExecutionException, TimeoutException {
        // This test only target SDK >= 26
        if (Build.VERSION.SDK_INT <= 26) {
            return;
        }
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future = new FutureTask<>(surfaceTextureCallable);

        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future.run();
                    }
                });

        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture = future.get(1, TimeUnit.SECONDS);

        useCase.clear();

        assertThat(surfaceTexture.isReleased()).isFalse();
    }

    @Test
    @UiThreadTest
    public void surfaceTexture_isListenedOnlyOnce()
            throws InterruptedException, ExecutionException, TimeoutException {
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture0 = future0.get();

        final SurfaceTextureCallable surfaceTextureCallable1 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future1 = new FutureTask<>(surfaceTextureCallable1);
        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable1.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future1.run();
                    }
                });

        SurfaceTexture surfaceTexture1 = future1.get(1, TimeUnit.SECONDS);

        assertThat(surfaceTexture0).isNotSameInstanceAs(surfaceTexture1);
    }

    @FlakyTest
    @Test
    @UiThreadTest
    public void updateSessionConfigWithSuggestedResolution() {
        PreviewConfig config = new PreviewConfig.Builder().setLensFacing(LensFacing.BACK).build();
        Preview useCase = new Preview(config);

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

    @MediumTest
    @Test
    @UiThreadTest
    public void previewOutputListenerCanBeSetAndRetrieved() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        Preview.OnPreviewOutputUpdateListener previewOutputListener =
                useCase.getOnPreviewOutputUpdateListener();
        useCase.setOnPreviewOutputUpdateListener(mMockListener);

        OnPreviewOutputUpdateListener retrievedPreviewOutputListener =
                useCase.getOnPreviewOutputUpdateListener();

        assertThat(previewOutputListener).isNull();
        assertThat(retrievedPreviewOutputListener).isSameInstanceAs(mMockListener);
    }

    @Test
    @UiThreadTest
    public void clear_removePreviewOutputListener() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        useCase.setOnPreviewOutputUpdateListener(mMockListener);
        useCase.clear();

        assertThat(useCase.getOnPreviewOutputUpdateListener()).isNull();
    }

    @Test
    @UiThreadTest
    public void previewOutput_isResetOnUpdatedResolution() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        useCase.setOnPreviewOutputUpdateListener(AsyncTask.SERIAL_EXECUTOR, mMockListener);
        verify(mMockListener, timeout(3000)).onUpdated(any(PreviewOutput.class));

        useCase.updateSuggestedResolution(
                Collections.singletonMap(mCameraId, SECONDARY_RESOLUTION));

        verify(mMockListener, timeout(3000).times(2)).onUpdated(any(PreviewOutput.class));
    }

    @Test
    @UiThreadTest
    public void previewOutput_invokedByExecutor() {
        Executor mockExecutor = mock(Executor.class);

        Preview useCase = new Preview(mDefaultConfig);

        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        lifecycleOwner.startAndResume();
        CameraX.bindToLifecycle(lifecycleOwner, useCase);

        useCase.setOnPreviewOutputUpdateListener(mockExecutor,
                mock(OnPreviewOutputUpdateListener.class));

        verify(mockExecutor, timeout(1000)).execute(any(Runnable.class));
    }

    @Test
    public void updateSuggestedResolution_getsFrame() throws InterruptedException {
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);

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
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
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

    @Test
    @UiThreadTest
    public void previewOutput_updatesWithTargetRotation() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.setTargetRotation(Surface.ROTATION_0);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        ArgumentCaptor<PreviewOutput> previewOutput = ArgumentCaptor.forClass(PreviewOutput.class);
        useCase.setOnPreviewOutputUpdateListener(AsyncTask.SERIAL_EXECUTOR, mMockListener);

        useCase.setTargetRotation(Surface.ROTATION_90);

        verify(mMockListener, timeout(3000).times(2)).onUpdated(previewOutput.capture());
        assertThat(previewOutput.getAllValues()).hasSize(2);
        Preview.PreviewOutput initialOutput = previewOutput.getAllValues().get(0);
        Preview.PreviewOutput latestPreviewOutput = previewOutput.getAllValues().get(1);

        assertThat(initialOutput).isNotNull();
        assertThat(initialOutput.getSurfaceTexture())
                .isEqualTo(latestPreviewOutput.getSurfaceTexture());
        assertThat(initialOutput.getRotationDegrees())
                .isNotEqualTo(latestPreviewOutput.getRotationDegrees());
    }

    @Test
    @UiThreadTest
    public void outputIsPublished_whenListenerIsSetBefore()
            throws InterruptedException, ExecutionException {
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull Preview.PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });

        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
    }

    @Test
    @UiThreadTest
    public void outputIsPublished_whenListenerIsSetAfter()
            throws InterruptedException, ExecutionException {
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        useCase.setOnPreviewOutputUpdateListener(
                AsyncTask.SERIAL_EXECUTOR,
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(@NonNull PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
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

    private static final class SurfaceTextureCallable implements Callable<SurfaceTexture> {
        SurfaceTexture mSurfaceTexture;

        void setSurfaceTexture(SurfaceTexture surfaceTexture) {
            this.mSurfaceTexture = surfaceTexture;
        }

        @Override
        public SurfaceTexture call() {
            return mSurfaceTexture;
        }
    }
}
