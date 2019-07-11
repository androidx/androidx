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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2CameraControl;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeferrableSurfaces;
import androidx.camera.core.OnFocusListener;
import androidx.camera.core.Preview;
import androidx.camera.core.Preview.OnPreviewOutputUpdateListener;
import androidx.camera.core.Preview.PreviewOutput;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import org.junit.Before;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class PreviewTest {
    // Use most supported resolution for different supported hardware level devices,
    // especially for legacy devices.
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size SECONDARY_RESOLUTION = new Size(320, 240);

    private PreviewConfig mDefaultConfig;
    @Mock
    private OnPreviewOutputUpdateListener mMockListener;
    private String mCameraId;

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
        CameraX.init(context, appConfig);

        // init CameraX before creating Preview to get preview size with CameraX's context
        mDefaultConfig = Preview.DEFAULT_CONFIG.getConfig(LensFacing.BACK);
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
    public void focusRegionCanBeSet() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        CameraControlInternal cameraControl = mock(CameraControlInternal.class);
        useCase.attachCameraControl(mCameraId, cameraControl);

        Rect rect = new Rect(/*left=*/ 200, /*top=*/ 200, /*right=*/ 800, /*bottom=*/ 800);
        useCase.focus(rect, rect, mock(OnFocusListener.class));

        ArgumentCaptor<Rect> rectArgumentCaptor1 = ArgumentCaptor.forClass(Rect.class);
        ArgumentCaptor<Rect> rectArgumentCaptor2 = ArgumentCaptor.forClass(Rect.class);
        verify(cameraControl).focus(rectArgumentCaptor1.capture(), rectArgumentCaptor2.capture(),
                any(Executor.class), any(OnFocusListener.class));
        assertThat(rectArgumentCaptor1.getValue()).isEqualTo(rect);
        assertThat(rectArgumentCaptor2.getValue()).isEqualTo(rect);
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
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
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
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
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
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
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
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
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
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
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

        final AtomicInteger calledCount = new AtomicInteger(0);
        useCase.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        calledCount.incrementAndGet();
                    }
                });

        int initialCount = calledCount.get();

        useCase.updateSuggestedResolution(
                Collections.singletonMap(mCameraId, SECONDARY_RESOLUTION));

        int countAfterUpdate = calledCount.get();

        assertThat(initialCount).isEqualTo(1);
        assertThat(countAfterUpdate).isEqualTo(2);
    }

    @Test
    @UiThreadTest
    public void previewOutput_updatesWithTargetRotation() {
        Preview useCase = new Preview(mDefaultConfig);
        useCase.setTargetRotation(Surface.ROTATION_0);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        final AtomicReference<PreviewOutput> latestPreviewOutput = new AtomicReference<>();
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        latestPreviewOutput.set(previewOutput);
                    }
                });

        Preview.PreviewOutput initialOutput = latestPreviewOutput.get();

        useCase.setTargetRotation(Surface.ROTATION_90);

        assertThat(initialOutput).isNotNull();
        assertThat(initialOutput.getSurfaceTexture())
                .isEqualTo(latestPreviewOutput.get().getSurfaceTexture());
        assertThat(initialOutput.getRotationDegrees())
                .isNotEqualTo(latestPreviewOutput.get().getRotationDegrees());
    }

    // Must not run on main thread
    @FlakyTest
    @Test
    public void previewOutput_isResetByReleasedSurface()
            throws InterruptedException, ExecutionException {
        final Preview useCase = new Preview(mDefaultConfig);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final Semaphore semaphore = new Semaphore(0);

        mainHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        useCase.updateSuggestedResolution(
                                Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

                        useCase.setOnPreviewOutputUpdateListener(
                                new Preview.OnPreviewOutputUpdateListener() {
                                    @Override
                                    public void onUpdated(PreviewOutput previewOutput) {
                                        // Release the surface texture
                                        previewOutput.getSurfaceTexture().release();

                                        semaphore.release();
                                    }
                                });
                    }
                });

        // Wait for the surface texture to be released
        semaphore.acquire();

        // Cause the surface to reset
        useCase.getSessionConfig(mCameraId).getSurfaces().get(0).getSurface().get();

        // Wait for the surface to reset
        semaphore.acquire();
    }

    @Test
    @UiThreadTest
    public void outputIsPublished_whenListenerIsSetBefore()
            throws InterruptedException, ExecutionException {
        Preview useCase = new Preview(mDefaultConfig);

        final SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        final FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnPreviewOutputUpdateListener(
                new OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
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
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(PreviewOutput previewOutput) {
                        surfaceTextureCallable0.setSurfaceTexture(
                                previewOutput.getSurfaceTexture());
                        future0.run();
                    }
                });
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
    }

    private CameraControlInternal getFakeCameraControl() {
        return new Camera2CameraControl(
                new CameraControlInternal.ControlUpdateListener() {
                    @Override
                    public void onCameraControlUpdateSessionConfig(
                            @NonNull SessionConfig sessionConfig) {
                    }

                    @Override
                    public void onCameraControlCaptureRequests(
                            @NonNull List<CaptureConfig> captureConfigs) {

                    }
                },
                CameraXExecutors.mainThreadExecutor(),
                CameraXExecutors.mainThreadExecutor());
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
