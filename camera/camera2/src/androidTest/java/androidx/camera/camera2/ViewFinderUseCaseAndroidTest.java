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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.AppConfiguration;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CaptureRequestConfiguration;
import androidx.camera.core.DeferrableSurfaces;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCase.OnViewFinderOutputUpdateListener;
import androidx.camera.core.ViewFinderUseCase.ViewFinderOutput;
import androidx.camera.core.ViewFinderUseCaseConfiguration;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ViewFinderUseCaseAndroidTest {
    private static final Size DEFAULT_RESOLUTION = new Size(1920, 1080);
    private static final Size SECONDARY_RESOLUTION = new Size(1280, 720);

    private ViewFinderUseCaseConfiguration mDefaultConfiguration;
    @Mock
    private OnViewFinderOutputUpdateListener mMockListener;
    private String mCameraId;

    @Before
    public void setUp() {
        // Instantiates OnViewFinderOutputUpdateListener before each test run.
        mMockListener = Mockito.mock(OnViewFinderOutputUpdateListener.class);
        Context context = ApplicationProvider.getApplicationContext();
        AppConfiguration appConfig = Camera2AppConfiguration.create(context);
        CameraFactory cameraFactory = appConfig.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        CameraX.init(context, appConfig);

        // init CameraX before creating ViewFinderUseCase to get preview size with CameraX's context
        mDefaultConfiguration = ViewFinderUseCase.DEFAULT_CONFIG.getConfiguration();
    }

    @Test
    @UiThreadTest
    public void useCaseIsConstructedWithDefaultConfiguration() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(
                        useCase.getSessionConfiguration(mCameraId).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0).isValid()).isTrue();
    }

    @Test
    @UiThreadTest
    public void useCaseIsConstructedWithCustomConfiguration() {
        ViewFinderUseCaseConfiguration configuration =
                new ViewFinderUseCaseConfiguration.Builder().setLensFacing(LensFacing.BACK).build();
        ViewFinderUseCase useCase = new ViewFinderUseCase(configuration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        List<Surface> surfaces =
                DeferrableSurfaces.surfaceList(
                        useCase.getSessionConfiguration(mCameraId).getSurfaces());

        assertThat(surfaces.size()).isEqualTo(1);
        assertThat(surfaces.get(0).isValid()).isTrue();
    }

    @Test
    @UiThreadTest
    public void focusRegionCanBeSet() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        CameraControl cameraControl = getFakeCameraControl();
        useCase.attachCameraControl(mCameraId, cameraControl);

        Rect rect = new Rect(/*left=*/ 200, /*top=*/ 200, /*right=*/ 800, /*bottom=*/ 800);
        useCase.focus(rect, rect);

        Camera2Configuration configuration =
                new Camera2Configuration(cameraControl.getSingleRequestImplOptions());
        MeteringRectangle[] aeMeteringRects =
                configuration.getCaptureRequestOption(CaptureRequest.CONTROL_AE_REGIONS, null);
        MeteringRectangle[] afMeteringRects =
                configuration.getCaptureRequestOption(CaptureRequest.CONTROL_AF_REGIONS, null);
        MeteringRectangle[] awbMeteringRects =
                configuration.getCaptureRequestOption(CaptureRequest.CONTROL_AWB_REGIONS, null);
        assertThat(aeMeteringRects).hasLength(1);
        assertThat(afMeteringRects).hasLength(1);
        assertThat(awbMeteringRects).hasLength(1);

        assertThat(aeMeteringRects[0].getRect()).isEqualTo(rect);
        assertThat(afMeteringRects[0].getRect()).isEqualTo(rect);
        assertThat(awbMeteringRects[0].getRect()).isEqualTo(rect);
    }

    @Test
    @UiThreadTest
    public void zoomRegionCanBeSet() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        CameraControl cameraControl = getFakeCameraControl();
        useCase.attachCameraControl(mCameraId, cameraControl);

        Rect rect = new Rect(/*left=*/ 200, /*top=*/ 200, /*right=*/ 800, /*bottom=*/ 800);
        useCase.zoom(rect);

        Camera2Configuration configuration =
                new Camera2Configuration(cameraControl.getSingleRequestImplOptions());
        Rect cropRect =
                configuration.getCaptureRequestOption(CaptureRequest.SCALER_CROP_REGION, null);
        assertThat(cropRect).isEqualTo(rect);
    }

    @Test
    @UiThreadTest
    public void torchModeCanBeSet() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        CameraControl cameraControl = getFakeCameraControl();
        useCase.attachCameraControl(mCameraId, cameraControl);

        useCase.enableTorch(true);

        assertThat(useCase.isTorchOn()).isTrue();
    }

    @Test(timeout = 5000)
    @UiThreadTest
    public void surfaceTextureIsNotReleased()
            throws InterruptedException, ExecutionException, TimeoutException {
        // This test only target SDK >= 26
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);

        SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable0.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future0.run();
                });
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        SurfaceTexture surfaceTexture0 = future0.get(1, TimeUnit.SECONDS);
        surfaceTexture0.release();

        SurfaceTextureCallable surfaceTextureCallable1 = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future1 = new FutureTask<>(surfaceTextureCallable1);
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable1.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future1.run();
                });
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture1 = future1.get(1, TimeUnit.SECONDS);

        assertThat(surfaceTexture1.isReleased()).isFalse();
    }

    @Test(timeout = 5000)
    @UiThreadTest
    public void listenedSurfaceTextureIsNotReleased_whenCleared()
            throws InterruptedException, ExecutionException, TimeoutException {
        // This test only target SDK >= 26
        if (Build.VERSION.SDK_INT <= 26) {
            return;
        }
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);

        SurfaceTextureCallable surfaceTextureCallable = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future = new FutureTask<>(surfaceTextureCallable);

        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future.run();
                });

        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture = future.get(1, TimeUnit.SECONDS);

        useCase.clear();

        assertThat(surfaceTexture.isReleased()).isFalse();
    }

    @Test(timeout = 5000)
    @UiThreadTest
    public void surfaceTexture_isListenedOnlyOnce()
            throws InterruptedException, ExecutionException, TimeoutException {

        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);

        SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable0.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future0.run();
                });

        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture0 = future0.get();

        SurfaceTextureCallable surfaceTextureCallable1 = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future1 = new FutureTask<>(surfaceTextureCallable1);
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable1.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future1.run();
                });

        SurfaceTexture surfaceTexture1 = future1.get(1, TimeUnit.SECONDS);

        assertThat(surfaceTexture0).isNotSameAs(surfaceTexture1);
    }

    @Test
    @UiThreadTest
    public void updateSessionConfigurationWithSuggestedResolution() {
        ViewFinderUseCaseConfiguration configuration =
                new ViewFinderUseCaseConfiguration.Builder().setLensFacing(LensFacing.BACK).build();
        ViewFinderUseCase useCase = new ViewFinderUseCase(configuration);

        final Size[] sizes = {new Size(1920, 1080), new Size(640, 480)};

        for (Size size : sizes) {
            useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, size));

            List<Surface> surfaces =
                    DeferrableSurfaces.surfaceList(
                            useCase.getSessionConfiguration(mCameraId).getSurfaces());

            assertWithMessage("Failed at Size: " + size).that(surfaces).hasSize(1);
            assertWithMessage("Failed at Size: " + size).that(surfaces.get(0).isValid()).isTrue();
        }
    }

    @Test
    @UiThreadTest
    public void viewFinderOutputListenerCanBeSetAndRetrieved() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        OnViewFinderOutputUpdateListener viewFinderOutputListener =
                useCase.getOnViewFinderOutputUpdateListener();
        useCase.setOnViewFinderOutputUpdateListener(mMockListener);

        OnViewFinderOutputUpdateListener retrievedViewFinderOutputListener =
                useCase.getOnViewFinderOutputUpdateListener();

        assertThat(viewFinderOutputListener).isNull();
        assertThat(retrievedViewFinderOutputListener).isSameAs(mMockListener);
    }

    @Test
    @UiThreadTest
    public void clear_removeViewFinderOutputListener() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        useCase.setOnViewFinderOutputUpdateListener(mMockListener);
        useCase.clear();

        assertThat(useCase.getOnViewFinderOutputUpdateListener()).isNull();
    }

    @Test
    @UiThreadTest
    public void viewFinderOutput_isResetOnUpdatedResolution() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        AtomicInteger calledCount = new AtomicInteger(0);
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    calledCount.incrementAndGet();
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
    public void viewFinderOutput_updatesWithTargetRotation() {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        useCase.setTargetRotation(Surface.ROTATION_0);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        AtomicReference<ViewFinderOutput> latestViewFinderOutput = new AtomicReference<>();
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    latestViewFinderOutput.set(viewFinderOutput);
                });

        ViewFinderOutput initialOutput = latestViewFinderOutput.get();

        useCase.setTargetRotation(Surface.ROTATION_90);

        assertThat(initialOutput).isNotNull();
        assertThat(initialOutput.getSurfaceTexture())
                .isEqualTo(latestViewFinderOutput.get().getSurfaceTexture());
        assertThat(initialOutput.getRotationDegrees())
                .isNotEqualTo(latestViewFinderOutput.get().getRotationDegrees());
    }

    // Must not run on main thread
    @Test(timeout = 5000)
    public void viewFinderOutput_isResetByReleasedSurface()
            throws InterruptedException, ExecutionException {
        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Semaphore semaphore = new Semaphore(0);

        mainHandler.post(
                () -> {
                    useCase.updateSuggestedResolution(
                            Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

                    useCase.setOnViewFinderOutputUpdateListener(
                            viewFinderOutput -> {
                                // Release the surface texture
                                viewFinderOutput.getSurfaceTexture().release();

                                semaphore.release();
                            });
                });

        // Wait for the surface texture to be released
        semaphore.acquire();

        // Cause the surface to reset
        useCase.getSessionConfiguration(mCameraId).getSurfaces().get(0).getSurface().get();

        // Wait for the surface to reset
        semaphore.acquire();
    }

    @Test(timeout = 5000)
    @UiThreadTest
    public void outputIsPublished_whenListenerIsSetBefore()
            throws InterruptedException, ExecutionException {

        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);

        SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable0.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future0.run();
                });

        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
    }

    @Test(timeout = 5000)
    @UiThreadTest
    public void outputIsPublished_whenListenerIsSetAfter()
            throws InterruptedException, ExecutionException {

        ViewFinderUseCase useCase = new ViewFinderUseCase(mDefaultConfiguration);

        SurfaceTextureCallable surfaceTextureCallable0 = new SurfaceTextureCallable();
        FutureTask<SurfaceTexture> future0 = new FutureTask<>(surfaceTextureCallable0);
        useCase.updateSuggestedResolution(Collections.singletonMap(mCameraId, DEFAULT_RESOLUTION));

        useCase.setOnViewFinderOutputUpdateListener(
                viewFinderOutput -> {
                    surfaceTextureCallable0.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                    future0.run();
                });
        SurfaceTexture surfaceTexture0 = future0.get();

        assertThat(surfaceTexture0).isNotNull();
    }

    private CameraControl getFakeCameraControl() {
        return new Camera2CameraControl(
                new Camera2RequestRunner() {
                    @Override
                    public void submitSingleRequest(
                            CaptureRequestConfiguration singleRequestConfig) {
                    }

                    @Override
                    public void updateRepeatingRequest() {
                    }
                },
                new Handler());
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
