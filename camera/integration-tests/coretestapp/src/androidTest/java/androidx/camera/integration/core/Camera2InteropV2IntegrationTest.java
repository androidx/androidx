/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.integration.core;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.hardware.DataSpace;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceProfiles;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.impl.CameraUtil;
import androidx.camera.testing.impl.SurfaceTextureProvider;
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for {@link Camera2Interop} configurator APIs ({@code forUseCase},
 * {@code forImageCapture}, {@code forSessionConfig}, and {@code forCameraControl}) written in Java
 * to verify complete Java API interoperability and catch overload ambiguity issues.
 */
@ExperimentalCamera2Interop
@LargeTest
@RunWith(AndroidJUnit4.class)
public class Camera2InteropV2IntegrationTest {

    @Rule
    public TestRule mUseCamera =
            CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
                    new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
            );

    private ProcessCameraProvider mCameraProvider;
    private CameraSelector mCameraSelector;
    private ExecutorService mTestExecutor;
    private FakeLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mCameraSelector = CameraUtil.assumeFirstAvailableCameraSelector();

        mCameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS);
        mLifecycleOwner = new FakeLifecycleOwner();
        mLifecycleOwner.startAndResume();

        assumeTrue(mCameraProvider.hasCamera(mCameraSelector));
        mTestExecutor = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "test-executor-thread"));
    }

    @After
    public void tearDown() throws Exception {
        if (mCameraProvider != null) {
            mCameraProvider.shutdownAsync().get(10, TimeUnit.SECONDS);
        }
        if (mTestExecutor != null) {
            mTestExecutor.shutdown();
        }
    }

    // =========================================================================================
    // Section 1: Camera2Interop.forUseCase Tests
    // =========================================================================================

    @Test
    public void canConfigureUseCaseInteropOutputOptions() throws Exception {
        // Arrange
        Preview.Builder builder = new Preview.Builder();
        builder.setInterop(
                Camera2Interop.forUseCase(interop -> {
                    if (Build.VERSION.SDK_INT >= 24) {
                        interop.setSurfaceGroupId(1);
                    }
                    if (Build.VERSION.SDK_INT >= 28) {
                        interop.setPhysicalCameraId("0");
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        interop.addSensorPixelModeUsed(0);
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        interop.setStreamUseCase(0L); // STREAM_USE_CASE_DEFAULT
                        interop.setTimestampBase(0); // TIMESTAMP_BASE_DEFAULT
                        interop.setMirrorMode(0); // MIRROR_MODE_AUTO
                        interop.setDynamicRangeProfile(1L); // DYNAMIC_RANGE_PROFILE_STANDARD
                    }
                })
        );
        Preview preview = builder.build();

        // Act & Assert
        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    preview);
            cameraRef.set(camera);
        });
        assertThat(cameraRef.get()).isNotNull();
    }

    @Test
    public void canConfigureImageAnalysisWithUseCaseInterop() throws Exception {
        // Arrange
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setInterop(
                Camera2Interop.forUseCase(interop -> {
                    if (Build.VERSION.SDK_INT >= 24) {
                        interop.setSurfaceGroupId(1);
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        interop.setStreamUseCase(0L);
                    }
                })
        );
        ImageAnalysis imageAnalysis = builder.build();

        // Act & Assert
        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.setAnalyzer(CameraXExecutors.highPriorityExecutor(), ImageProxy::close);
            Camera camera = mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    imageAnalysis);
            cameraRef.set(camera);
        });
        assertThat(cameraRef.get()).isNotNull();
    }

    // =========================================================================================
    // Section 2: Camera2Interop.forImageCapture Tests
    // =========================================================================================

    @Test
    public void canConfigureImageCaptureOutputOptions() throws Exception {
        // Arrange
        ImageCapture.Builder builder = new ImageCapture.Builder();
        builder.setInterop(
                Camera2Interop.forImageCapture(interop -> {
                    if (Build.VERSION.SDK_INT >= 24) {
                        interop.setSurfaceGroupId(1);
                    }
                    if (Build.VERSION.SDK_INT >= 28) {
                        interop.setPhysicalCameraId("0");
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        interop.addSensorPixelModeUsed(0);
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        interop.setStreamUseCase(0L);
                        interop.setTimestampBase(0);
                        interop.setMirrorMode(0);
                        interop.setDynamicRangeProfile(1L);
                    }
                })
        );
        ImageCapture imageCapture = builder.build();

        // Act & Assert
        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Camera camera = mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    imageCapture);
            cameraRef.set(camera);
        });
        assertThat(cameraRef.get()).isNotNull();
    }

    @Test
    public void canConfigureUseCaseInteropStillOptions() throws Exception {
        // Arrange
        CountDownLatch stillCaptureLatch = new CountDownLatch(1);
        AtomicReference<String> callbackThreadName = new AtomicReference<>();
        AtomicReference<CaptureRequest> requestHolder = new AtomicReference<>();

        Preview preview = new Preview.Builder().build();

        CameraCaptureSession.CaptureCallback stillCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        callbackThreadName.set(Thread.currentThread().getName());
                        requestHolder.set(request);
                        stillCaptureLatch.countDown();
                    }
                };

        ImageCapture.Builder builder = new ImageCapture.Builder();
        builder.setInterop(
                Camera2Interop.forImageCapture(interop -> {
                    interop.setStillCaptureCallback(mTestExecutor, stillCallback);
                    interop.setStillCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_OFF
                    );
                    interop.setStillCaptureRequestTemplateType(CameraDevice.TEMPLATE_PREVIEW);
                })
        );
        ImageCapture imageCapture = builder.build();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider
                    .bindToLifecycle(mLifecycleOwner, mCameraSelector, preview, imageCapture);
        });

        // Act
        CountDownLatch imageReceivedLatch = new CountDownLatch(1);
        imageCapture.takePicture(
                CameraXExecutors.mainThreadExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        image.close();
                        imageReceivedLatch.countDown();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );

        // Assert
        assertThat(imageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(stillCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(callbackThreadName.get()).isEqualTo("test-executor-thread");
        assertThat(requestHolder.get().get(CaptureRequest.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW);
        assertThat(requestHolder.get().get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
    }

    @Test
    public void canConfigureImageCaptureStillCallbackDirectExecutor() throws Exception {
        // Arrange
        CountDownLatch stillCaptureLatch = new CountDownLatch(1);
        Preview preview = new Preview.Builder().build();

        CameraCaptureSession.CaptureCallback stillCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        stillCaptureLatch.countDown();
                    }
                };

        ImageCapture.Builder builder = new ImageCapture.Builder();
        builder.setInterop(
                Camera2Interop.forImageCapture(interop -> {
                    interop.setStillCaptureCallback(stillCallback);
                })
        );
        ImageCapture imageCapture = builder.build();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(
                    mLifecycleOwner, mCameraSelector, preview, imageCapture);
        });

        // Act
        CountDownLatch imageReceivedLatch = new CountDownLatch(1);
        imageCapture.takePicture(
                CameraXExecutors.mainThreadExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        image.close();
                        imageReceivedLatch.countDown();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );

        // Assert
        assertThat(imageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(stillCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void stillCaptureDoesNotTriggerPreviewInteropCallback() throws Exception {
        // Arrange
        CountDownLatch stillCaptureLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> stillRequestHolder = new AtomicReference<>();

        CountDownLatch previewCaptureLatch = new CountDownLatch(10);
        List<CaptureRequest> previewRequests = Collections.synchronizedList(new ArrayList<>());

        Preview preview = new Preview.Builder().build();

        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder();
        imageCaptureBuilder.setInterop(
                Camera2Interop.forImageCapture(interop -> {
                    interop.setStillCaptureCallback(
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(
                                        @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result
                                ) {
                                    stillRequestHolder.set(request);
                                    stillCaptureLatch.countDown();
                                }
                            }
                    );
                })
        );
        ImageCapture imageCapture = imageCaptureBuilder.build();

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview,
                imageCapture);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setRepeatingCaptureCallback(
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(
                                        @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result
                                ) {
                                    previewRequests.add(request);
                                    previewCaptureLatch.countDown();
                                }
                            }
                    );
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Act
        CountDownLatch imageReceivedLatch = new CountDownLatch(1);
        imageCapture.takePicture(
                CameraXExecutors.mainThreadExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        image.close();
                        imageReceivedLatch.countDown();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );

        // Assert
        assertThat(previewCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(imageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(stillCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(previewRequests).doesNotContain(stillRequestHolder.get());
    }

    // =========================================================================================
    // Section 3: Camera2Interop.forSessionConfig Tests
    // =========================================================================================

    @Test
    public void canConfigureSessionConfigInterop() throws Exception {
        // Arrange
        CountDownLatch deviceLatch = new CountDownLatch(1);
        CountDownLatch sessionLatch = new CountDownLatch(1);
        CountDownLatch captureLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<TotalCaptureResult> capturedTotalResult = new AtomicReference<>();

        Preview preview = new Preview.Builder().build();

        CameraDevice.StateCallback deviceStateCallback =
                new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        deviceLatch.countDown();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                    }
                };

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setDeviceStateCallback(mTestExecutor, deviceStateCallback);
                    interop.setSessionStateCallback(
                            mTestExecutor,
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession session) {
                                    sessionLatch.countDown();
                                }

                                @Override
                                public void onConfigureFailed(
                                        @NonNull CameraCaptureSession session) {
                                }
                            }
                    );
                    interop.setRepeatingCaptureCallback(
                            mTestExecutor,
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(
                                        @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result
                                ) {
                                    capturedRequest.set(request);
                                    capturedTotalResult.set(result);
                                    captureLatch.countDown();
                                }
                            }
                    );
                    interop.setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_OFF
                    );
                    interop.setRepeatingCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW);
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Assert
        assertThat(deviceLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(sessionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();

        CaptureRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
    }

    @Test
    public void canConfigureDefaultCallback() throws Exception {
        // Arrange
        CountDownLatch deviceLatch = new CountDownLatch(1);
        CountDownLatch sessionLatch = new CountDownLatch(1);
        CountDownLatch captureLatch = new CountDownLatch(1);

        Preview preview = new Preview.Builder().build();

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setDeviceStateCallback(
                            new CameraDevice.StateCallback() {
                                @Override
                                public void onOpened(@NonNull CameraDevice camera) {
                                    deviceLatch.countDown();
                                }

                                @Override
                                public void onDisconnected(@NonNull CameraDevice camera) {
                                }

                                @Override
                                public void onError(@NonNull CameraDevice camera, int error) {
                                }
                            }
                    );
                    interop.setSessionStateCallback(
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession session) {
                                    sessionLatch.countDown();
                                }

                                @Override
                                public void onConfigureFailed(
                                        @NonNull CameraCaptureSession session) {
                                }
                            }
                    );
                    interop.setRepeatingCaptureCallback(
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(
                                        @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result
                                ) {
                                    captureLatch.countDown();
                                }
                            }
                    );
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Assert
        assertThat(deviceLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(sessionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void repeatingCaptureCallback_invokedOncePerFrame_whenFourUseCasesBound()
            throws Exception {
        // Arrange
        Map<Long, Integer> frameCounts = Collections.synchronizedMap(new HashMap<>());
        CountDownLatch totalFramesLatch = new CountDownLatch(15);

        CameraCaptureSession.CaptureCallback repeatingCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        synchronized (frameCounts) {
                            int current = frameCounts.containsKey(result.getFrameNumber())
                                    ? frameCounts.get(result.getFrameNumber())
                                    : 0;
                            frameCounts.put(result.getFrameNumber(), current + 1);
                        }
                        totalFramesLatch.countDown();
                    }
                };

        Preview preview = new Preview.Builder().build();
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(CameraXExecutors.highPriorityExecutor(), ImageProxy::close);
        VideoCapture<Recorder> videoCapture = VideoCapture.withOutput(
                new Recorder.Builder().build());

        SessionConfig.Builder sessionConfigBuilder =
                new SessionConfig.Builder(preview, imageCapture, videoCapture, imageAnalysis);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setRepeatingCaptureCallback(mTestExecutor, repeatingCallback);
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Assert
        assertThat(totalFramesLatch.await(10, TimeUnit.SECONDS)).isTrue();

        // Verify that every frame number received was called exactly once (no duplicates per frame)
        List<Long> duplicateFrames = new ArrayList<>();
        synchronized (frameCounts) {
            for (Map.Entry<Long, Integer> entry : frameCounts.entrySet()) {
                if (entry.getValue() > 1) {
                    duplicateFrames.add(entry.getKey());
                }
            }
        }
        assertThat(duplicateFrames).isEmpty();
    }

    @Test
    public void repeatingCaptureCallback_inSessionConfig_isNotInvokedForOneShotRequest()
            throws Exception {
        // Arrange
        CountDownLatch repeatingCallbackLatch = new CountDownLatch(5);
        CountDownLatch stillCaptureCallbackLatch = new CountDownLatch(1);

        CameraCaptureSession.CaptureCallback repeatingCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        repeatingCallbackLatch.countDown();
                        Integer intent = request.get(CaptureRequest.CONTROL_CAPTURE_INTENT);
                        if (intent != null
                                && intent == CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE) {
                            stillCaptureCallbackLatch.countDown();
                        }
                    }
                };

        Preview preview = new Preview.Builder().build();
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview,
                imageCapture);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setRepeatingCaptureCallback(mTestExecutor, repeatingCallback);
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Verify repeating callback is invoked for preview repeating requests
        assertThat(repeatingCallbackLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Act: trigger a one-shot request via imageCapture.takePicture
        CountDownLatch pictureTakenLatch = new CountDownLatch(1);
        imageCapture.takePicture(
                CameraXExecutors.mainThreadExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        image.close();
                        pictureTakenLatch.countDown();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                    }
                }
        );

        // Assert: still picture succeeds AND repeatingCaptureCallback in SessionConfig is NOT
        // invoked for the one-shot request
        assertThat(pictureTakenLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(stillCaptureCallbackLatch.await(3, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void canConfigureColorSpaceAndVerifyImageColorSpace() throws Exception {
        CameraCharacteristics characteristics =
                Camera2Interop.getCameraCharacteristics(mCameraProvider.getCameraInfo(
                        mCameraSelector));
        ColorSpaceProfiles profiles =
                characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES);
        assumeTrue(profiles != null);
        Set<ColorSpace.Named> supportedColorSpaces =
                profiles.getSupportedColorSpaces(ImageFormat.YUV_420_888);
        assumeTrue(supportedColorSpaces.contains(ColorSpace.Named.DISPLAY_P3));

        // Arrange
        CountDownLatch frameLatch = new CountDownLatch(1);
        AtomicReference<Integer> capturedDataSpace = new AtomicReference<>();

        Preview preview = new Preview.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        imageAnalysis.setAnalyzer(mTestExecutor, imageProxy -> {
            if (imageProxy.getImage() != null) {
                capturedDataSpace.set(imageProxy.getImage().getDataSpace());
                frameLatch.countDown();
            }
            imageProxy.close();
        });

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview,
                imageAnalysis);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setColorSpace(ColorSpace.Named.DISPLAY_P3);
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Assert frame received and data space matches DISPLAY_P3
        assertThat(frameLatch.await(5, TimeUnit.SECONDS)).isTrue();
        Integer dataSpace = capturedDataSpace.get();
        assertThat(dataSpace).isEqualTo(DataSpace.DATASPACE_DISPLAY_P3);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void canConfigureSessionParameterAndSessionType() throws Exception {
        // Arrange
        CountDownLatch sessionLatch = new CountDownLatch(1);
        CountDownLatch captureLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();
        Preview preview = new Preview.Builder().build();

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setSessionType(0); // SESSION_REGULAR
                    interop.setSessionParameter(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON
                    );
                    interop.setSessionStateCallback(
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession session) {
                                    sessionLatch.countDown();
                                }

                                @Override
                                public void onConfigureFailed(
                                        @NonNull CameraCaptureSession session) {
                                }
                            }
                    );
                    interop.setRepeatingCaptureCallback(
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(
                                        @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result
                                ) {
                                    capturedRequest.set(request);
                                    captureLatch.countDown();
                                }
                            }
                    );
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Assert options are correctly applied to SessionConfig
        assertThat(sessionConfig.getSessionType()).isEqualTo(0);

        Config.Option<?> sessionParamOption = null;
        for (Config.Option<?> opt : sessionConfig.getInteropConfig().listOptions()) {
            if (opt.getId().equals("camera2.sessionParameter.option."
                    + CaptureRequest.CONTROL_AE_MODE.getName())) {
                sessionParamOption = opt;
                break;
            }
        }
        assertThat(sessionParamOption).isNotNull();
        assertThat(sessionConfig.getInteropConfig().retrieveOption(sessionParamOption))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Assert
        assertThat(sessionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void canClearCaptureRequestOptionInSessionConfig() throws Exception {
        // Arrange
        CountDownLatch captureLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();
        Preview preview = new Preview.Builder().build();

        SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder(preview);
        sessionConfigBuilder.setInterop(
                Camera2Interop.forSessionConfig(interop -> {
                    interop.setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_OFF
                    );
                    interop.clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE);
                    interop.setRepeatingCaptureCallback(
                            new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureCompleted(
                                        @NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull TotalCaptureResult result
                                ) {
                                    capturedRequest.set(request);
                                    captureLatch.countDown();
                                }
                            }
                    );
                })
        );
        SessionConfig sessionConfig = sessionConfigBuilder.build();

        // Act
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, sessionConfig);
        });

        // Assert
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
                .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
    }

    // =========================================================================================
    // Section 4: Camera2Interop.forCameraControl Tests
    // =========================================================================================

    @Test
    public void canConfigureCameraControlInterop() throws Exception {
        // Arrange
        CountDownLatch captureLatch = new CountDownLatch(5);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();
        AtomicReference<String> callbackThreadName = new AtomicReference<>();

        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider
                    .bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();
        assertThat(camera).isNotNull();

        CameraCaptureSession.CaptureCallback repeatingCallback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        callbackThreadName.set(Thread.currentThread().getName());
                        capturedRequest.set(request);
                        captureLatch.countDown();
                    }
                };

        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureRequestTemplate(
                                    CameraDevice.TEMPLATE_RECORD);
                            interop.setRepeatingCaptureCallback(mTestExecutor, repeatingCallback);
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_OFF
                            );
                        })
                )
                .get(10, TimeUnit.SECONDS);

        // Assert
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(callbackThreadName.get()).isEqualTo("test-executor-thread");

        CaptureRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
        assertThat(request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD);

        // Act: Clear option
        CountDownLatch captureLatch2 = new CountDownLatch(5);
        AtomicReference<CaptureRequest> capturedRequest2 = new AtomicReference<>();
        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(
                                    mTestExecutor,
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(
                                                @NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull TotalCaptureResult result
                                        ) {
                                            capturedRequest2.set(request);
                                            captureLatch2.countDown();
                                        }
                                    }
                            );
                            interop.clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE);
                        })
                )
                .get(10, TimeUnit.SECONDS);

        assertThat(captureLatch2.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest request2 = capturedRequest2.get();
        assertThat(request2).isNotNull();
        assertThat(request2.get(CaptureRequest.CONTROL_AE_MODE))
                .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
    }

    @Test
    public void canConfigureCameraControlDirectExecutorCallback() throws Exception {
        // Arrange
        CountDownLatch captureLatch = new CountDownLatch(5);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();

        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider
                    .bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();

        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(
                                                @NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull TotalCaptureResult result
                                        ) {
                                            capturedRequest.set(request);
                                            captureLatch.countDown();
                                        }
                                    }
                            );
                        })
                )
                .get(10, TimeUnit.SECONDS);

        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedRequest.get()).isNotNull();
    }

    @Test
    public void canClearAllCaptureRequestOptions() throws Exception {
        // Arrange
        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider
                    .bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();

        // Set capture request options
        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_OFF
                            );
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                                    1
                            );
                        })
                )
                .get(10, TimeUnit.SECONDS);

        // Act: Clear all capture request options
        CountDownLatch captureLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();
        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(
                                    mTestExecutor,
                                    new CameraCaptureSession.CaptureCallback() {
                                        @Override
                                        public void onCaptureCompleted(
                                                @NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull TotalCaptureResult result
                                        ) {
                                            Integer aeMode = request.get(
                                                    CaptureRequest.CONTROL_AE_MODE);
                                            Integer expComp = request.get(
                                                    CaptureRequest
                                                            .CONTROL_AE_EXPOSURE_COMPENSATION);
                                            boolean isAeOff =
                                                    aeMode != null && aeMode
                                                            == CaptureRequest.CONTROL_AE_MODE_OFF;
                                            boolean isExpComp1 = expComp != null && expComp == 1;
                                            if (!isAeOff && !isExpComp1) {
                                                capturedRequest.set(request);
                                                captureLatch.countDown();
                                            }
                                        }
                                    }
                            );
                            interop.clearAllCaptureRequestOptions();
                        })
                )
                .get(10, TimeUnit.SECONDS);

        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest request2 = capturedRequest.get();
        assertThat(request2).isNotNull();
        assertThat(request2.get(CaptureRequest.CONTROL_AE_MODE))
                .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
        assertThat(request2.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)).isNotEqualTo(1);
    }

    @Test
    public void multipleCaptureRequestOptionsSetInSingleUpdateWhenAppliedViaCameraControl()
            throws Exception {
        // Arrange
        CountDownLatch captureLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> capturedRequest = new AtomicReference<>();

        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider
                    .bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();

        CameraCaptureSession.CaptureCallback repeatingCallback =
                new CameraCaptureSession.CaptureCallback() {
                    private boolean mIsFirstCapture = true;

                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        Integer aeMode = request.get(CaptureRequest.CONTROL_AE_MODE);
                        Integer afMode = request.get(CaptureRequest.CONTROL_AF_MODE);
                        boolean isAeOff =
                                aeMode != null && aeMode == CaptureRequest.CONTROL_AE_MODE_OFF;
                        boolean isAfOff =
                                afMode != null && afMode == CaptureRequest.CONTROL_AF_MODE_OFF;
                        if (isAeOff && isAfOff && mIsFirstCapture) {
                            capturedRequest.set(request);
                            captureLatch.countDown();
                        }
                        mIsFirstCapture = false;
                    }
                };

        // Act: Apply multiple capture request options in one applyInteropAsync call
        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(mTestExecutor, repeatingCallback);
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_OFF
                            );
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_OFF
                            );
                        })
                )
                .get(10, TimeUnit.SECONDS);

        // Assert: Both options are updated in the same repeating request call
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
        assertThat(request.get(CaptureRequest.CONTROL_AF_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF);
    }

    @Test
    public void applyInteropAsyncReturnsListenableFutureThatCompletesWhenUpdated()
            throws Exception {
        // Arrange
        CountDownLatch captureWithKeyLatch = new CountDownLatch(1);
        CountDownLatch futureCompletionLatch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> updatedRequestRef = new AtomicReference<>();
        AtomicBoolean keyUpdatedBeforeOrAtFutureCompletion = new AtomicBoolean(false);

        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider
                    .bindToLifecycle(mLifecycleOwner, mCameraSelector, preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();

        // Act: Apply interop with option AND repeating capture callback
        CameraCaptureSession.CaptureCallback step1Callback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        Integer aeMode = request.get(CaptureRequest.CONTROL_AE_MODE);
                        boolean isAeOff =
                                aeMode != null && aeMode == CaptureRequest.CONTROL_AE_MODE_OFF;
                        if (isAeOff) {
                            updatedRequestRef.set(request);
                            captureWithKeyLatch.countDown();
                        }
                    }
                };

        ListenableFuture<Void> future =
                camera.getCameraControl().applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(mTestExecutor, step1Callback);
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_OFF
                            );
                        })
                );

        // Attach a listener to verify the state at the exact moment the future completes
        future.addListener(
                () -> {
                    if (updatedRequestRef.get() != null) {
                        keyUpdatedBeforeOrAtFutureCompletion.set(true);
                    }
                    futureCompletionLatch.countDown();
                },
                mTestExecutor
        );

        // Assert 1: The ListenableFuture completes successfully
        assertThat(future.get(10, TimeUnit.SECONDS)).isNull();
        assertThat(futureCompletionLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Assert 2: Verify repeating request callbacks confirmed AE_MODE_OFF update
        assertThat(captureWithKeyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest request = updatedRequestRef.get();
        assertThat(request).isNotNull();
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);

        // Assert 3: Verify future completed as a result of/after option was updated
        assertThat(keyUpdatedBeforeOrAtFutureCompletion.get()).isTrue();
    }

    @Test
    public void applyInteropAsyncFailsWithOperationCanceledExceptionWhenOverwritten()
            throws Exception {
        // Arrange
        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();

        // Act: Issue first interop call and immediately override with a second interop call
        ListenableFuture<Void> future1 =
                camera.getCameraControl().applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_OFF
                            );
                        })
                );

        ListenableFuture<Void> future2 =
                camera.getCameraControl().applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON
                            );
                        })
                );

        // Assert: Second future completes successfully
        assertThat(future2.get(10, TimeUnit.SECONDS)).isNull();

        // Assert: First future failed because it was canceled by the newer request
        assertThat(future1.isDone()).isTrue();
        ExecutionException exception = assertThrows(ExecutionException.class, future1::get);
        assertThat(exception.getCause())
                .isInstanceOf(CameraControl.OperationCanceledException.class);
    }

    @Test
    public void cameraControlInteropOptionsAreAdditiveUntilExplicitlyCleared() throws Exception {
        // Arrange
        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();

        // Step 1: Set AE_MODE to CONTROL_AE_MODE_OFF
        CountDownLatch step1Latch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> step1RequestRef = new AtomicReference<>();
        CameraCaptureSession.CaptureCallback step1Callback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        Integer aeMode = request.get(CaptureRequest.CONTROL_AE_MODE);
                        boolean isAeOff =
                                aeMode != null && aeMode == CaptureRequest.CONTROL_AE_MODE_OFF;
                        if (isAeOff) {
                            step1RequestRef.set(request);
                            step1Latch.countDown();
                        }
                    }
                };

        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(mTestExecutor, step1Callback);
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_OFF
                            );
                        })
                )
                .get(10, TimeUnit.SECONDS);

        assertThat(step1Latch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest step1Request = step1RequestRef.get();
        assertThat(step1Request).isNotNull();
        assertThat(step1Request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);

        // Step 2: Set AF_MODE to CONTROL_AF_MODE_OFF without mentioning AE_MODE
        CountDownLatch step2Latch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> step2RequestRef = new AtomicReference<>();
        CameraCaptureSession.CaptureCallback step2Callback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        Integer aeMode = request.get(CaptureRequest.CONTROL_AE_MODE);
                        Integer afMode = request.get(CaptureRequest.CONTROL_AF_MODE);
                        boolean isAeOff =
                                aeMode != null && aeMode == CaptureRequest.CONTROL_AE_MODE_OFF;
                        boolean isAfOff =
                                afMode != null && afMode == CaptureRequest.CONTROL_AF_MODE_OFF;
                        if (isAeOff && isAfOff) {
                            step2RequestRef.set(request);
                            step2Latch.countDown();
                        }
                    }
                };

        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(mTestExecutor, step2Callback);
                            interop.setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_OFF
                            );
                        })
                )
                .get(10, TimeUnit.SECONDS);

        // Assert Step 2: Both AE_MODE and AF_MODE present in request (additive)
        assertThat(step2Latch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest step2Request = step2RequestRef.get();
        assertThat(step2Request).isNotNull();
        assertThat(step2Request.get(CaptureRequest.CONTROL_AE_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
        assertThat(step2Request.get(CaptureRequest.CONTROL_AF_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF);

        // Step 3: Explicitly clear AE_MODE key
        CountDownLatch step3Latch = new CountDownLatch(1);
        AtomicReference<CaptureRequest> step3RequestRef = new AtomicReference<>();
        CameraCaptureSession.CaptureCallback step3Callback =
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(
                            @NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request,
                            @NonNull TotalCaptureResult result
                    ) {
                        Integer aeMode = request.get(CaptureRequest.CONTROL_AE_MODE);
                        Integer afMode = request.get(CaptureRequest.CONTROL_AF_MODE);
                        boolean isAeOff =
                                aeMode != null && aeMode == CaptureRequest.CONTROL_AE_MODE_OFF;
                        boolean isAfOff =
                                afMode != null && afMode == CaptureRequest.CONTROL_AF_MODE_OFF;
                        if (!isAeOff && isAfOff) {
                            step3RequestRef.set(request);
                            step3Latch.countDown();
                        }
                    }
                };

        camera.getCameraControl()
                .applyInteropAsync(
                        Camera2Interop.forCameraControl(interop -> {
                            interop.setRepeatingCaptureCallback(mTestExecutor, step3Callback);
                            interop.clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE);
                        })
                )
                .get(10, TimeUnit.SECONDS);

        // Assert Step 3: AF_MODE is still OFF, but AE_MODE was cleared
        assertThat(step3Latch.await(5, TimeUnit.SECONDS)).isTrue();
        CaptureRequest step3Request = step3RequestRef.get();
        assertThat(step3Request).isNotNull();
        assertThat(step3Request.get(CaptureRequest.CONTROL_AE_MODE))
                .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF);
        assertThat(step3Request.get(CaptureRequest.CONTROL_AF_MODE))
                .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF);
    }

    // =========================================================================================
    // Section 5: Camera2Interop Static Metadata Utilities
    // =========================================================================================

    @Test
    public void canUseMetadataApis() throws Exception {
        // Arrange
        Preview preview = new Preview.Builder().build();

        AtomicReference<Camera> cameraRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera = mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    preview);
            cameraRef.set(camera);
        });
        Camera camera = cameraRef.get();
        assertThat(camera).isNotNull();
        CameraInfo cameraInfo = camera.getCameraInfo();

        // Assert
        // Test Camera2Interop.getCameraId(CameraInfo)
        String cameraId = Camera2Interop.getCameraId(cameraInfo);
        assertThat(cameraId).isNotEmpty();

        // Test Camera2Interop.getCameraCharacteristics(CameraInfo)
        CameraCharacteristics characteristics = Camera2Interop.getCameraCharacteristics(cameraInfo);
        assertThat(characteristics).isNotNull();

        // Test Camera2Interop.getCameraSelectorFromCameraId(String)
        CameraSelector selectorFromId = Camera2Interop.getCameraSelectorFromCameraId(cameraId);
        assertThat(selectorFromId).isNotNull();

        // Test Camera2Interop.getCameraFilterFromCameraId(String)
        CameraFilter filter = Camera2Interop.getCameraFilterFromCameraId(cameraId);
        assertThat(filter).isNotNull();

        // Verify we can bind with the new selector
        Preview preview2 = new Preview.Builder().build();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            preview2.setSurfaceProvider(
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            );
            Camera camera2 = mCameraProvider.bindToLifecycle(mLifecycleOwner, selectorFromId,
                    preview2);
            assertThat(Camera2Interop.getCameraId(camera2.getCameraInfo())).isEqualTo(cameraId);
        });
    }
}
