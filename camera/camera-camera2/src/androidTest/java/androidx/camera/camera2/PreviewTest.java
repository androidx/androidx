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
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.SamsungPreviewTargetAspectRatioQuirk;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.GLUtil;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class PreviewTest {

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest();

    private static final String ANY_THREAD_NAME = "any-thread-name";
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final CameraSelector mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private Preview.Builder mDefaultBuilder;
    private Size mPreviewResolution;
    private Semaphore mSurfaceFutureSemaphore;
    private Semaphore mSafeToReleaseSemaphore;
    private Context mContext;
    private CameraUseCaseAdapter mCamera;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        mContext = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = Camera2Config.defaultConfig();
        CameraX.initialize(mContext, cameraXConfig).get();

        // init CameraX before creating Preview to get preview size with CameraX's context
        mDefaultBuilder = Preview.Builder.fromConfig(Preview.DEFAULT_CONFIG.getConfig());
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
        mSafeToReleaseSemaphore = new Semaphore(/*permits=*/ 0);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (mCamera != null) {
            mInstrumentation.runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        // Ensure all cameras are released for the next test
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void surfaceProvider_isUsedAfterSetting() {
        final Preview.SurfaceProvider surfaceProvider = mock(Preview.SurfaceProvider.class);
        doAnswer(args -> {
            SurfaceTexture surfaceTexture = new SurfaceTexture(0);
            surfaceTexture.setDefaultBufferSize(640, 480);
            Surface surface = new Surface(surfaceTexture);
            ((SurfaceRequest) args.getArgument(0)).provideSurface(surface,
                    CameraXExecutors.directExecutor(), result -> {
                        surfaceTexture.release();
                        surface.release();
                    });
            return null;
        }).when(surfaceProvider).onSurfaceRequested(any(SurfaceRequest.class));

        final Preview preview = mDefaultBuilder.build();

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> preview.setSurfaceProvider(surfaceProvider));

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        verify(surfaceProvider, timeout(3000)).onSurfaceRequested(any(SurfaceRequest.class));
    }

    @Test
    public void previewDetached_onSafeToReleaseCalled() throws InterruptedException {
        // Arrange.
        final Preview preview = new Preview.Builder().build();

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() ->
                preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor(),
                getSurfaceProvider(null))
        );
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Wait until preview gets frame.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        // Remove the UseCase from the camera
        mCamera.removeUseCases(Collections.singleton(preview));

        // Assert.
        assertThat(mSafeToReleaseSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setSurfaceProviderBeforeBind_getsFrame() throws InterruptedException {
        // Arrange.
        final Preview preview = mDefaultBuilder.build();
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> preview.setSurfaceProvider(getSurfaceProvider(null)));

        // Act.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setSurfaceProviderBeforeAttach_providesSurfaceOnWorkerExecutorThread()
            throws InterruptedException {
        final AtomicReference<String> threadName = new AtomicReference<>();

        // Arrange.
        final Preview preview = mDefaultBuilder.build();
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() ->
                preview.setSurfaceProvider(getWorkExecutorWithNamedThread(),
                getSurfaceProvider(threadName::set))
        );

        // Act.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME);
    }

    @Test
    public void setSurfaceProviderAfterAttach_getsFrame() throws InterruptedException {
        // Arrange.
        Preview preview = mDefaultBuilder.build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> preview.setSurfaceProvider(getSurfaceProvider(null)));

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setSurfaceProviderAfterBind_providesSurfaceOnWorkerExecutorThread()
            throws InterruptedException {
        final AtomicReference<String> threadName = new AtomicReference<>();

        // Arrange.
        Preview preview = mDefaultBuilder.build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() ->
                preview.setSurfaceProvider(getWorkExecutorWithNamedThread(),
                        getSurfaceProvider(threadName::set))
        );

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).isEqualTo(ANY_THREAD_NAME);
    }

    @Test
    public void canSupportGuaranteedSizeFront()
            throws InterruptedException, CameraInfoUnavailableException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(CameraSelector.LENS_FACING_FRONT));

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        Integer sensorOrientation = CameraUtil.getSensorOrientation(
                CameraSelector.LENS_FACING_FRONT);
        boolean isRotateNeeded = (sensorOrientation % 180) != 0;
        Preview preview = new Preview.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).setTargetRotation(
                isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();

        // Skips the test if the SamsungPreviewTargetAspectRatioQuirk is applied on the device.
        assumeFalse(isSamsungPreviewTargetAspectRatioQuirkApplied(preview.getCurrentConfig()));

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> preview.setSurfaceProvider(getSurfaceProvider(null)));
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_FRONT_CAMERA, preview);

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        // Check whether 640x480 is selected for the preview use case. This test can also check
        // whether the guaranteed resolution 640x480 is really supported for SurfaceTexture
        // format on the devices when running the test.
        assertEquals(GUARANTEED_RESOLUTION, mPreviewResolution);
    }

    @Test
    public void canSupportGuaranteedSizeBack()
            throws InterruptedException, CameraInfoUnavailableException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(CameraSelector.LENS_FACING_BACK));

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        Integer sensorOrientation = CameraUtil.getSensorOrientation(
                CameraSelector.LENS_FACING_BACK);
        boolean isRotateNeeded = (sensorOrientation % 180) != 0;
        Preview preview = new Preview.Builder().setTargetResolution(
                GUARANTEED_RESOLUTION).setTargetRotation(
                isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();

        // Skips the test if the SamsungPreviewTargetAspectRatioQuirk is applied on the device.
        assumeFalse(isSamsungPreviewTargetAspectRatioQuirkApplied(preview.getCurrentConfig()));

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> preview.setSurfaceProvider(getSurfaceProvider(null)));
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_BACK_CAMERA, preview);

        // Assert.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        // Check whether 640x480 is selected for the preview use case. This test can also check
        // whether the guaranteed resolution 640x480 is really supported for SurfaceTexture
        // format on the devices when running the test.
        assertEquals(GUARANTEED_RESOLUTION, mPreviewResolution);
    }

    private boolean isSamsungPreviewTargetAspectRatioQuirkApplied(@NonNull Config config) {
        SamsungPreviewTargetAspectRatioQuirk samsungQuirk =
                DeviceQuirks.get(SamsungPreviewTargetAspectRatioQuirk.class);

        return samsungQuirk != null && samsungQuirk.require16_9(config);
    }

    @Test
    public void setMultipleNonNullSurfaceProviders_getsFrame() throws InterruptedException {
        final Preview preview = mDefaultBuilder.build();

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(getSurfaceProvider(null));
        });
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(getSurfaceProvider(null));
        });

        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void setMultipleNullableSurfaceProviders_getsFrame() throws InterruptedException {
        final Preview preview = mDefaultBuilder.build();

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> {
            // Set a different SurfaceProvider which will provide a different surface to be used
            // for preview.
            preview.setSurfaceProvider(getSurfaceProvider(null));
        });
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() -> {
            // Set the SurfaceProvider to null in order to force the Preview into an inactive
            // state before setting a different SurfaceProvider for preview.
            preview.setSurfaceProvider(null);
            preview.setSurfaceProvider(getSurfaceProvider(null));
        });

        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() {
        Preview useCase = new Preview.Builder().build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        ImageOutputConfig config = (ImageOutputConfig) useCase.getCurrentConfig();
        assertThat(config.getTargetAspectRatio()).isEqualTo(AspectRatio.RATIO_4_3);
    }

    @Test
    public void defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));
        Preview useCase = new Preview.Builder().setTargetResolution(GUARANTEED_RESOLUTION).build();

        assertThat(useCase.getCurrentConfig().containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)).isFalse();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_BACK_CAMERA, useCase);

        assertThat(useCase.getCurrentConfig().containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)).isFalse();
    }

    @Test
    public void useCaseConfigCanBeReset_afterUnbind() {
        final Preview preview = mDefaultBuilder.build();
        UseCaseConfig<?> initialConfig = preview.getCurrentConfig();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        mInstrumentation.runOnMainSync(() -> {
            mCamera.removeUseCases(Collections.singleton(preview));
        });

        UseCaseConfig<?> configAfterUnbinding = preview.getCurrentConfig();
        assertThat(initialConfig.equals(configAfterUnbinding)).isTrue();
    }

    @Test
    public void targetRotationIsRetained_whenUseCaseIsReused() {
        Preview useCase = mDefaultBuilder.build();

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.setTargetRotation(Surface.ROTATION_180);

        mInstrumentation.runOnMainSync(() -> {
            // Unbind the use case.
            mCamera.removeUseCases(Collections.singleton(useCase));
        });

        // Check the target rotation is kept when the use case is unbound.
        assertThat(useCase.getTargetRotation()).isEqualTo(Surface.ROTATION_180);

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, useCase);
        assertThat(useCase.getTargetRotation()).isEqualTo(Surface.ROTATION_180);
    }

    @Test
    public void useCaseCanBeReusedInSameCamera() throws InterruptedException {
        final Preview preview = mDefaultBuilder.build();

        mInstrumentation.runOnMainSync(() -> {
            preview.setSurfaceProvider(getSurfaceProvider(null));
        });

        // This is the first time the use case bound to the lifecycle.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Check the frame available callback is called.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        mInstrumentation.runOnMainSync(() -> {
            // Unbind and rebind the use case to the same lifecycle.
            mCamera.removeUseCases(Collections.singleton(preview));
        });

        assertThat(mSafeToReleaseSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        // Recreate the semaphore to monitor the frame available callback.
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
        // Rebind the use case to the same camera.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Check the frame available callback can be called after reusing the use case.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void useCaseCanBeReusedInDifferentCamera() throws InterruptedException {
        final Preview preview = mDefaultBuilder.build();

        mInstrumentation.runOnMainSync(() -> {
            preview.setSurfaceProvider(getSurfaceProvider(null));
        });

        // This is the first time the use case bound to the lifecycle.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_BACK_CAMERA, preview);

        // Check the frame available callback is called.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        mInstrumentation.runOnMainSync(() -> {
            // Unbind and rebind the use case to the same lifecycle.
            mCamera.removeUseCases(Collections.singleton(preview));
        });

        assertThat(mSafeToReleaseSemaphore.tryAcquire(5, TimeUnit.SECONDS)).isTrue();

        // Recreate the semaphore to monitor the frame available callback.
        mSurfaceFutureSemaphore = new Semaphore(/*permits=*/ 0);
        // Rebind the use case to different camera.
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext,
                CameraSelector.DEFAULT_FRONT_CAMERA, preview);

        // Check the frame available callback can be called after reusing the use case.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void returnValidTargetRotation_afterUseCaseIsCreated() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        assertThat(imageCapture.getTargetRotation()).isNotEqualTo(
                ImageOutputConfig.INVALID_ROTATION);
    }

    @Test
    public void returnCorrectTargetRotation_afterUseCaseIsAttached() {
        Preview preview = new Preview.Builder().setTargetRotation(
                Surface.ROTATION_180).build();
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);
        assertThat(preview.getTargetRotation()).isEqualTo(Surface.ROTATION_180);
    }

    @Test
    public void setNullSurfaceProvider_shouldStopPreview() throws InterruptedException {
        // Arrange.
        final Preview preview = new Preview.Builder().build();

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() ->
                preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor(),
                        createSurfaceTextureProvider(
                                new SurfaceTextureProvider.SurfaceTextureCallback() {
                                    private boolean mIsReleased = false;
                                    @Override
                                    public void onSurfaceTextureReady(
                                            @NonNull SurfaceTexture surfaceTexture,
                                            @NonNull Size resolution) {
                                        surfaceTexture.attachToGLContext(
                                                GLUtil.getTexIdFromGLContext());
                                        surfaceTexture.setOnFrameAvailableListener(st -> {
                                            mSurfaceFutureSemaphore.release();
                                            synchronized (this) {
                                                if (!mIsReleased) {
                                                    surfaceTexture.updateTexImage();
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onSafeToRelease(
                                            @NonNull SurfaceTexture surfaceTexture) {
                                        synchronized (this) {
                                            mIsReleased = true;
                                            surfaceTexture.release();
                                        }
                                    }
                                })));
        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Assert.
        // Wait until preview gets frame.
        assertThat(mSurfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        // Act.
        // TODO(b/160261462) move off of main thread when setSurfaceProvider does not need to be
        //  done on the main thread
        mInstrumentation.runOnMainSync(() ->
                preview.setSurfaceProvider(CameraXExecutors.mainThreadExecutor(), null)
        );

        // Assert.
        // No frame coming for 3 seconds in 10 seconds timeout.
        assertThat(noFrameCome(3000L, 10000L)).isTrue();
    }

    private Executor getWorkExecutorWithNamedThread() {
        final ThreadFactory threadFactory = runnable -> new Thread(runnable, ANY_THREAD_NAME);
        return Executors.newSingleThreadExecutor(threadFactory);
    }

    private Preview.SurfaceProvider getSurfaceProvider(
            @Nullable final Consumer<String> threadNameConsumer) {
        return createSurfaceTextureProvider(new SurfaceTextureProvider.SurfaceTextureCallback() {
            @Override
            public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                    @NonNull Size resolution) {
                if (threadNameConsumer != null) {
                    threadNameConsumer.accept(Thread.currentThread().getName());
                }
                mPreviewResolution = resolution;
                surfaceTexture.setOnFrameAvailableListener(st -> mSurfaceFutureSemaphore.release());
            }

            @Override
            public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                surfaceTexture.release();
                mSafeToReleaseSemaphore.release();
            }
        });
    }

    /*
     * Check if there is no frame callback for `noFrameIntervalMs` milliseconds, then it will
     * return true; If the total check time is over `timeoutMs` milliseconds, then it will return
     * false.
     */
    private boolean noFrameCome(long noFrameIntervalMs, long timeoutMs)
            throws InterruptedException {
        if (noFrameIntervalMs <= 0 || timeoutMs <= 0) {
            throw new IllegalArgumentException("Time can't be negative value.");
        }
        if (timeoutMs < noFrameIntervalMs) {
            throw new IllegalArgumentException(
                    "timeoutMs should be larger than noFrameIntervalMs.");
        }
        final long checkFrequency = 200L;
        long totalCheckTime = 0L;
        long zeroFrameTimer = 0L;
        do {
            Thread.sleep(checkFrequency);
            if (mSurfaceFutureSemaphore.availablePermits() > 0) {
                // Has frame, reset timer and frame count.
                zeroFrameTimer = 0;
                mSurfaceFutureSemaphore.drainPermits();
            } else {
                zeroFrameTimer += checkFrequency;
            }
            if (zeroFrameTimer > noFrameIntervalMs) {
                return true;
            }
            totalCheckTime += checkFrequency;
        } while (totalCheckTime < timeoutMs);

        return false;
    }
}
