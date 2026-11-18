/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core;

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_NV21;
import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888;
import static androidx.camera.core.ImageAnalysis.STRATEGY_BLOCK_PRODUCER;
import static androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST;
import static androidx.camera.testing.impl.TestImageUtil.createYuvFakeImageProxy;
import static androidx.camera.testing.impl.TestImageUtil.getAverageDiff;
import static androidx.camera.testing.impl.TestImageUtil.rotateBitmap;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.os.StrictMode;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.impl.ImageProxyUtil;
import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ImageAnalysisDeviceTest {

    private final CameraInternal mMockCameraInternal = mock(CameraInternal.class);
    private final ImageAnalysis.Analyzer mMockAnalyzer = mock(ImageAnalysis.Analyzer.class);

    @Test
    @UiThreadTest
    public void becomesActive_whenHasAnalyzer() {
        when(mMockCameraInternal.getCameraInfoInternal()).thenReturn(new FakeCameraInfoInternal());
        ImageAnalysis useCase = new ImageAnalysis.Builder().setBackpressureStrategy(
                STRATEGY_KEEP_ONLY_LATEST).build();

        useCase.bindToCamera(mMockCameraInternal, null, null, null);

        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);

        verify(mMockCameraInternal, times(1)).onUseCaseActive(useCase);
    }

    @Test
    @UiThreadTest
    public void becomesInactive_whenNoAnalyzer() {
        when(mMockCameraInternal.getCameraInfoInternal()).thenReturn(new FakeCameraInfoInternal());
        ImageAnalysis useCase = new ImageAnalysis.Builder().setBackpressureStrategy(
                STRATEGY_KEEP_ONLY_LATEST).build();

        useCase.bindToCamera(mMockCameraInternal, null, null, null);
        useCase.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);
        useCase.clearAnalyzer();

        verify(mMockCameraInternal, times(1)).onUseCaseInactive(useCase);
    }

    @Test
    public void rotationWorksWhenChangingRotationFrom0To180()
            throws Exception {
        // 1. Create an ImageAnalysis UseCase with OutputRotationEnabled.
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setSessionOptionUnpacker((resolution2, config, builder) -> {
                })
                .setCaptureOptionUnpacker((config, builder) -> {
                })
                .build();

        // 2. Invoke bindToCamera with a FakeCamera that will make initial relative rotation be 0
        FakeCameraInfoInternal cameraInfoInternal = new FakeCameraInfoInternal("0", 0,
                CameraSelector.LENS_FACING_BACK);
        FakeCamera fakeCamera = new FakeCamera("0", null, cameraInfoInternal);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.bindToCamera(fakeCamera, null, null, null);
        });

        // 3. Invoke onSuggestedStreamSpecUpdated with proper resolution to create pipeline
        Size resolution = new Size(8, 4);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.onSuggestedStreamSpecUpdated(StreamSpec.builder(resolution).build(),
                    null);
        });

        // 4. Set an Analyzer to the ImageAnalysis
        imageAnalysis.setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);

        // 5. Extract the mImageAnalysisAbstractAnalyzer
        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysis.mImageAnalysisAbstractAnalyzer;

        FakeImageInfo fakeImageInfo = new FakeImageInfo();
        FakeImageProxy imageProxy = createYuvFakeImageProxy(fakeImageInfo, resolution.getWidth(),
                resolution.getHeight(), ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);

        // 6. Verify first analysis diff
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        Bitmap expectedBitmap = ImageUtil.createBitmapFromImageProxy(imageProxy);
        verifyAnalysis(abstractAnalyzer, imageProxy, expectedBitmap, imageProxyArgumentCaptor);

        // 7. Change target rotation of ImageAnalysis to 180
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysis.setTargetRotation(Surface.ROTATION_180);
        });

        // 8. Invoke analyzeImage with the color blocks ImageProxy again.
        FakeImageProxy imageProxy2 = createYuvFakeImageProxy(fakeImageInfo,
                resolution.getWidth(), resolution.getHeight(),
                ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);

        // 9. Verify second analysis diff
        Bitmap expectedBitmap2 = rotateBitmap(expectedBitmap, 180);
        verifyAnalysis(abstractAnalyzer, imageProxy2, expectedBitmap2, imageProxyArgumentCaptor);
    }

    @Test
    public void strictMode_noLeakedClosable_whenRotationChanges() throws Exception {
        StrictMode.VmPolicy oldVmPolicy = StrictMode.getVmPolicy();
        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());

        try {
            // 1. Create an ImageAnalysis UseCase with OutputRotationEnabled.
            final ImageAnalysis[] imageAnalysisRef = new ImageAnalysis[1];
            imageAnalysisRef[0] = new ImageAnalysis.Builder()
                    .setOutputImageRotationEnabled(true)
                    .setSessionOptionUnpacker((resolution2, config, builder) -> {
                    })
                    .setCaptureOptionUnpacker((config, builder) -> {
                    })
                    .build();

            // 2. Invoke bindToCamera
            final FakeCameraInfoInternal[] cameraInfoInternalRef = new FakeCameraInfoInternal[1];
            cameraInfoInternalRef[0] = new FakeCameraInfoInternal("0", 0,
                    CameraSelector.LENS_FACING_BACK);
            final FakeCamera[] fakeCameraRef = new FakeCamera[1];
            fakeCameraRef[0] = new FakeCamera("0", null, cameraInfoInternalRef[0]);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                imageAnalysisRef[0].setTargetRotation(Surface.ROTATION_0);
                imageAnalysisRef[0].bindToCamera(fakeCameraRef[0], null, null, null);
            });

            // 3. Invoke onSuggestedStreamSpecUpdated
            Size resolution = new Size(8, 4);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                imageAnalysisRef[0].onSuggestedStreamSpecUpdated(
                        StreamSpec.builder(resolution).build(), null);
            });

            // 4. Set analyzer
            imageAnalysisRef[0].setAnalyzer(CameraXExecutors.mainThreadExecutor(), mMockAnalyzer);

            // 5. Run analysis with initial rotation (0)
            final ImageAnalysisAbstractAnalyzer[] abstractAnalyzerRef =
                    new ImageAnalysisAbstractAnalyzer[1];
            abstractAnalyzerRef[0] = imageAnalysisRef[0].mImageAnalysisAbstractAnalyzer;
            FakeImageInfo fakeImageInfo = new FakeImageInfo();
            final FakeImageProxy[] imageProxyRef = new FakeImageProxy[1];
            imageProxyRef[0] = createYuvFakeImageProxy(fakeImageInfo, resolution.getWidth(),
                    resolution.getHeight(), ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);

            ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                    ArgumentCaptor.forClass(ImageProxy.class);
            final Bitmap[] expectedBitmapRef = new Bitmap[1];
            expectedBitmapRef[0] = ImageUtil.createBitmapFromImageProxy(imageProxyRef[0]);
            verifyAnalysis(abstractAnalyzerRef[0], imageProxyRef[0], expectedBitmapRef[0],
                    imageProxyArgumentCaptor);

            // 6. Change rotation to 180 to trigger recreation
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                imageAnalysisRef[0].setTargetRotation(Surface.ROTATION_180);
            });

            // 7. Run analysis again with rotated image
            final FakeImageProxy[] imageProxy2Ref = new FakeImageProxy[1];
            imageProxy2Ref[0] = createYuvFakeImageProxy(fakeImageInfo,
                    resolution.getWidth(), resolution.getHeight(),
                    ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);
            final Bitmap[] expectedBitmap2Ref = new Bitmap[1];
            expectedBitmap2Ref[0] = rotateBitmap(expectedBitmapRef[0], 180);
            ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor2 =
                    ArgumentCaptor.forClass(ImageProxy.class);
            verifyAnalysis(abstractAnalyzerRef[0], imageProxy2Ref[0], expectedBitmap2Ref[0],
                    imageProxyArgumentCaptor2);

            // 8. Clean up analyzer and unbind the pipeline
            SafeCloseImageReaderProxy recreatedReader =
                    abstractAnalyzerRef[0].getProcessedImageReaderProxy();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                imageAnalysisRef[0].clearAnalyzer();
                imageAnalysisRef[0].unbindFromCamera(fakeCameraRef[0]);
            });

            assertThat(recreatedReader).isNotNull();
            assertThat(recreatedReader.isClosed()).isTrue();
            assertThat(recreatedReader.getOutstandingImages()).isEqualTo(0);

            // Clear local references to allow GC collection
            imageAnalysisRef[0] = null;
            abstractAnalyzerRef[0] = null;
            imageProxyRef[0] = null;
            imageProxy2Ref[0] = null;
            expectedBitmapRef[0] = null;
            expectedBitmap2Ref[0] = null;
            imageProxyArgumentCaptor = null;
            imageProxyArgumentCaptor2 = null;
            fakeCameraRef[0] = null;
            cameraInfoInternalRef[0] = null;

            // 9. Force GC and finalizers to run to trigger StrictMode leak detection
            for (int i = 0; i < 5; i++) {
                Runtime.getRuntime().gc();
                System.runFinalization();
            }
        } finally {
            StrictMode.setVmPolicy(oldVmPolicy);
        }
    }

    @Test
    public void analyzeImage_closesProcessedImage_whenExecutorRejects() throws Exception {
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("Mock rejection");
        };

        final ImageAnalysis[] imageAnalysisRef = new ImageAnalysis[1];
        imageAnalysisRef[0] = new ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setSessionOptionUnpacker((resolution2, config, builder) -> {
                })
                .setCaptureOptionUnpacker((config, builder) -> {
                })
                .build();

        final FakeCameraInfoInternal[] cameraInfoInternalRef = new FakeCameraInfoInternal[1];
        cameraInfoInternalRef[0] = new FakeCameraInfoInternal("0", 0,
                CameraSelector.LENS_FACING_BACK);
        final FakeCamera[] fakeCameraRef = new FakeCamera[1];
        fakeCameraRef[0] = new FakeCamera("0", null, cameraInfoInternalRef[0]);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysisRef[0].setTargetRotation(Surface.ROTATION_0);
            imageAnalysisRef[0].bindToCamera(fakeCameraRef[0], null, null, null);
        });

        Size resolution = new Size(8, 4);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysisRef[0].onSuggestedStreamSpecUpdated(
                    StreamSpec.builder(resolution).build(), null);
        });

        imageAnalysisRef[0].setAnalyzer(rejectingExecutor, mMockAnalyzer);
        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysisRef[0].mImageAnalysisAbstractAnalyzer;

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysisRef[0].setTargetRotation(Surface.ROTATION_180);
        });

        FakeImageInfo fakeImageInfo = new FakeImageInfo();
        FakeImageProxy imageProxy = createYuvFakeImageProxy(fakeImageInfo, resolution.getWidth(),
                resolution.getHeight(), ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420, true);

        try {
            abstractAnalyzer.analyzeImage(imageProxy).get(1,
                    TimeUnit.SECONDS);
            Assert.fail("Expected execution to fail due to executor rejection");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(
                    RejectedExecutionException.class);
        } finally {
            imageProxy.close();
        }

        SafeCloseImageReaderProxy recreatedReader =
                abstractAnalyzer.getProcessedImageReaderProxy();
        assertThat(recreatedReader).isNotNull();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            imageAnalysisRef[0].clearAnalyzer();
            imageAnalysisRef[0].unbindFromCamera(fakeCameraRef[0]);
        });

        assertThat(recreatedReader.isClosed()).isTrue();
        assertThat(recreatedReader.getOutstandingImages()).isEqualTo(0);

        imageAnalysisRef[0] = null;
        fakeCameraRef[0] = null;
        cameraInfoInternalRef[0] = null;
    }

    @Test
    public void nonBlockingAnalyzer_rgba_closesProcessedImage_whenExecutorSilentlyDropsTasks()
            throws Exception {
        verifyProcessedImageClosesWhenExecutorDropsTasks(
                STRATEGY_KEEP_ONLY_LATEST, 0, "ImageAnalysisDropTestBg");
    }

    @Test
    public void nonBlockingAnalyzer_rgba_doesNotExceedMaxImages_whenAnalyzerThrowsExceptionOrError()
            throws Exception {
        AtomicReference<Throwable> bgUncaughtRef = new AtomicReference<>();
        ExecutorService bgExecutor =
                newRecordingExecutor("ImageAnalysisThrowTestBg", bgUncaughtRef);
        AtomicReference<Throwable> userUnexpectedUncaughtRef = new AtomicReference<>();
        ExecutorService userThreadExecutor =
                newRecordingExecutor("ImageAnalysisThrowTestUser", userUnexpectedUncaughtRef);
        AtomicInteger rethrownErrorCount = new AtomicInteger(0);
        Executor userExecutor =
                command ->
                        userThreadExecutor.execute(
                                () -> {
                                    try {
                                        command.run();
                                    } catch (AssertionError e) {
                                        if ("Simulated analyzer error".equals(e.getMessage())) {
                                            rethrownErrorCount.incrementAndGet();
                                        } else {
                                            userUnexpectedUncaughtRef.compareAndSet(null, e);
                                        }
                                    }
                                });

        Size resolution = new Size(8, 4);
        ImageAnalysis imageAnalysis =
                buildTestImageAnalysis(
                        OUTPUT_IMAGE_FORMAT_RGBA_8888, STRATEGY_KEEP_ONLY_LATEST, 0, bgExecutor);
        FakeCamera fakeCamera = bindAnalysisToFakeCamera(imageAnalysis, resolution);

        CountDownLatch[] enteredLatches = createLatches(4);
        CountDownLatch[] proceedLatches = createLatches(4);
        CountDownLatch enteredSlowFrameLatch = new CountDownLatch(1);
        CountDownLatch releaseSlowFrameLatch = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger(0);

        imageAnalysis.setAnalyzer(
                userExecutor,
                image -> {
                    int index = invocations.getAndIncrement();
                    if (index < 4) {
                        enteredLatches[index].countDown();
                        awaitQuietly(proceedLatches[index]);
                        if (index < 2) {
                            throw new RuntimeException("Simulated analyzer exception");
                        }
                        throw new AssertionError("Simulated analyzer error");
                    }
                    if (index == 4) {
                        enteredSlowFrameLatch.countDown();
                        awaitQuietly(releaseSlowFrameLatch);
                    }
                    image.close();
                });

        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysis.mImageAnalysisAbstractAnalyzer;
        SafeCloseImageReaderProxy processedReader = abstractAnalyzer.getProcessedImageReaderProxy();
        assertThat(processedReader).isNotNull();

        try {
            long ts = 1L;
            submitFrame(bgExecutor, abstractAnalyzer, resolution, ts++);

            for (int i = 0; i < 4; i++) {
                awaitLatchWithGcFallback(enteredLatches[i]);
                submitFrame(bgExecutor, abstractAnalyzer, resolution, ts++);
                proceedLatches[i].countDown();
            }

            awaitLatchWithGcFallback(enteredSlowFrameLatch);
            for (int i = 0; i < 4; i++) {
                submitFrame(bgExecutor, abstractAnalyzer, resolution, ts++);
            }

            assertNoBackgroundError(bgUncaughtRef);
            assertThat(processedReader.getOutstandingImages()).isEqualTo(1);

            releaseSlowFrameLatch.countDown();
            userThreadExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            bgExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            userThreadExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);
            bgExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);

            assertNoBackgroundError(bgUncaughtRef);
            assertThat(userUnexpectedUncaughtRef.get()).isNull();
            assertThat(rethrownErrorCount.get()).isEqualTo(2);
            assertThat(processedReader.getOutstandingImages()).isEqualTo(0);
        } finally {
            countDownAll(proceedLatches);
            releaseSlowFrameLatch.countDown();
            tearDownBoundAnalysis(imageAnalysis, fakeCamera, userThreadExecutor, bgExecutor);
        }
    }

    @Test
    public void nonBlockingAnalyzer_rgba_closesProcessedImage_whenExecutorThrowsRuntimeException()
            throws Exception {
        AtomicReference<Throwable> bgUncaughtRef = new AtomicReference<>();
        ExecutorService bgExecutor =
                newRecordingExecutor("ImageAnalysisExecThrowTestBg", bgUncaughtRef);

        Size resolution = new Size(8, 4);
        ImageAnalysis imageAnalysis =
                buildTestImageAnalysis(
                        OUTPUT_IMAGE_FORMAT_RGBA_8888, STRATEGY_KEEP_ONLY_LATEST, 0, bgExecutor);
        FakeCamera fakeCamera = bindAnalysisToFakeCamera(imageAnalysis, resolution);

        Executor throwingExecutor =
                command -> {
                    throw new IllegalStateException("Custom executor failure");
                };
        imageAnalysis.setAnalyzer(throwingExecutor, mMockAnalyzer);
        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysis.mImageAnalysisAbstractAnalyzer;
        SafeCloseImageReaderProxy processedReader = abstractAnalyzer.getProcessedImageReaderProxy();
        assertThat(processedReader).isNotNull();

        try {
            long ts = 1L;
            for (int i = 0; i < 6; i++) {
                FakeImageProxy frame = createRgbaTestYuvFrame(resolution, ts++);
                FakeImageProxy cached = createRgbaTestYuvFrame(resolution, ts++);
                bgExecutor
                        .submit(
                                () -> {
                                    abstractAnalyzer.onValidImageAvailable(frame);
                                    abstractAnalyzer.onValidImageAvailable(cached);
                                })
                        .get(5, TimeUnit.SECONDS);
                bgExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);
                if (bgUncaughtRef.get() != null) {
                    break;
                }
            }

            assertNoBackgroundError(bgUncaughtRef);
            assertThat(processedReader.getOutstandingImages()).isEqualTo(0);
        } finally {
            tearDownBoundAnalysis(imageAnalysis, fakeCamera, bgExecutor);
        }
    }

    @Test
    public void blockingAnalyzer_rgba_closesProcessedImage_whenExecutorSilentlyDropsTasks()
            throws Exception {
        verifyProcessedImageClosesWhenExecutorDropsTasks(
                STRATEGY_BLOCK_PRODUCER, 4, "ImageAnalysisBlockDropBg");
    }

    @Test
    public void
            nonBlockingAnalyzer_nv21_doesNotDeliverConcurrently_whenAnalyzerThrowsCheckedException()
                    throws Exception {
        AtomicReference<Throwable> bgUncaughtRef = new AtomicReference<>();
        ExecutorService bgExecutor = newRecordingExecutor("ImageAnalysisNv21Bg", bgUncaughtRef);
        ExecutorService userExecutor = Executors.newFixedThreadPool(4);

        Size resolution = new Size(8, 4);
        ImageAnalysis imageAnalysis =
                buildTestImageAnalysis(
                        OUTPUT_IMAGE_FORMAT_NV21, STRATEGY_KEEP_ONLY_LATEST, 0, bgExecutor);
        FakeCamera fakeCamera = bindAnalysisToFakeCamera(imageAnalysis, resolution);

        CountDownLatch[] enteredLatches = createLatches(4);
        CountDownLatch[] proceedLatches = createLatches(4);
        CountDownLatch fifthCompletedLatch = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger(0);
        AtomicInteger inFlight = new AtomicInteger(0);
        AtomicInteger maxInFlight = new AtomicInteger(0);

        imageAnalysis.setAnalyzer(
                userExecutor,
                image -> {
                    updateMax(maxInFlight, inFlight.incrementAndGet());
                    try {
                        int index = invocations.getAndIncrement();
                        if (index < 4) {
                            enteredLatches[index].countDown();
                            awaitQuietly(proceedLatches[index]);
                            sneakyThrow(new IOException("Simulated checked exception"));
                        }
                        image.close();
                        if (index == 4) {
                            fifthCompletedLatch.countDown();
                        }
                    } finally {
                        inFlight.decrementAndGet();
                    }
                });

        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysis.mImageAnalysisAbstractAnalyzer;
        try {
            long ts = 1L;
            submitFrame(bgExecutor, abstractAnalyzer, resolution, ts++);

            for (int i = 0; i < 4; i++) {
                assertThat(enteredLatches[i].await(5, TimeUnit.SECONDS)).isTrue();
                submitFrame(bgExecutor, abstractAnalyzer, resolution, ts++);
                proceedLatches[i].countDown();
            }

            assertThat(fifthCompletedLatch.await(5, TimeUnit.SECONDS)).isTrue();
            bgExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);

            assertNoBackgroundError(bgUncaughtRef);
            assertThat(maxInFlight.get()).isEqualTo(1);
        } finally {
            countDownAll(proceedLatches);
            tearDownBoundAnalysis(imageAnalysis, fakeCamera, userExecutor, bgExecutor);
        }
    }

    private void verifyProcessedImageClosesWhenExecutorDropsTasks(
            int backpressureStrategy, int imageQueueDepth, String threadName) throws Exception {
        AtomicReference<Throwable> bgUncaughtRef = new AtomicReference<>();
        ExecutorService bgExecutor = newRecordingExecutor(threadName, bgUncaughtRef);

        Size resolution = new Size(8, 4);
        ImageAnalysis imageAnalysis =
                buildTestImageAnalysis(
                        OUTPUT_IMAGE_FORMAT_RGBA_8888,
                        backpressureStrategy,
                        imageQueueDepth,
                        bgExecutor);
        FakeCamera fakeCamera = bindAnalysisToFakeCamera(imageAnalysis, resolution);

        Executor droppingExecutor =
                command -> {
                    // Intentionally drop tasks to simulate Handler.post() returning false or
                    // DiscardPolicy.
                };
        imageAnalysis.setAnalyzer(droppingExecutor, mMockAnalyzer);
        ImageAnalysisAbstractAnalyzer abstractAnalyzer =
                imageAnalysis.mImageAnalysisAbstractAnalyzer;
        SafeCloseImageReaderProxy processedReader = abstractAnalyzer.getProcessedImageReaderProxy();
        assertThat(processedReader).isNotNull();

        try {
            for (long ts = 1L; ts <= 8L; ts++) {
                submitFrame(bgExecutor, abstractAnalyzer, resolution, ts);
                runGcAndFinalization();
                bgExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);
                if (bgUncaughtRef.get() != null) {
                    break;
                }
            }
            runGcAndFinalization();
            bgExecutor.submit(() -> {}).get(5, TimeUnit.SECONDS);

            assertNoBackgroundError(bgUncaughtRef);
            assertThat(processedReader.getOutstandingImages()).isEqualTo(0);
        } finally {
            tearDownBoundAnalysis(imageAnalysis, fakeCamera, bgExecutor);
        }
    }

    private static ExecutorService newRecordingExecutor(
            String threadName, AtomicReference<Throwable> uncaughtRef) {
        return Executors.newSingleThreadExecutor(
                runnable -> {
                    Thread thread = new Thread(runnable, threadName);
                    thread.setUncaughtExceptionHandler(
                            (t, e) -> uncaughtRef.compareAndSet(null, e));
                    return thread;
                });
    }

    private static ImageAnalysis buildTestImageAnalysis(
            int outputImageFormat,
            int backpressureStrategy,
            int imageQueueDepth,
            Executor bgExecutor) {
        ImageAnalysis.Builder builder =
                new ImageAnalysis.Builder()
                        .setOutputImageFormat(outputImageFormat)
                        .setBackpressureStrategy(backpressureStrategy)
                        .setBackgroundExecutor(bgExecutor)
                        .setSessionOptionUnpacker((resolution2, config, sessionBuilder) -> {})
                        .setCaptureOptionUnpacker((config, captureBuilder) -> {});
        if (imageQueueDepth > 0) {
            builder.setImageQueueDepth(imageQueueDepth);
        }
        return builder.build();
    }

    private static FakeCamera bindAnalysisToFakeCamera(
            ImageAnalysis imageAnalysis, Size resolution) {
        FakeCameraInfoInternal cameraInfoInternal =
                new FakeCameraInfoInternal("0", 0, CameraSelector.LENS_FACING_BACK);
        FakeCamera fakeCamera = new FakeCamera("0", null, cameraInfoInternal);
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            imageAnalysis.bindToCamera(fakeCamera, null, null, null);
                            imageAnalysis.onSuggestedStreamSpecUpdated(
                                    StreamSpec.builder(resolution).build(), null);
                        });
        return fakeCamera;
    }

    private static void tearDownBoundAnalysis(
            ImageAnalysis imageAnalysis,
            FakeCamera fakeCamera,
            ExecutorService... executorsToShutdown)
            throws InterruptedException {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            imageAnalysis.clearAnalyzer();
                            imageAnalysis.unbindFromCamera(fakeCamera);
                        });
        for (ExecutorService executor : executorsToShutdown) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void submitFrame(
            ExecutorService bgExecutor,
            ImageAnalysisAbstractAnalyzer abstractAnalyzer,
            Size resolution,
            long timestamp)
            throws Exception {
        FakeImageProxy frame = createRgbaTestYuvFrame(resolution, timestamp);
        bgExecutor
                .submit(() -> abstractAnalyzer.onValidImageAvailable(frame))
                .get(5, TimeUnit.SECONDS);
    }

    private static void assertNoBackgroundError(AtomicReference<Throwable> bgUncaughtRef) {
        if (bgUncaughtRef.get() != null) {
            throw new AssertionError(
                    "Background executor threw uncaught exception", bgUncaughtRef.get());
        }
    }

    private static CountDownLatch[] createLatches(int count) {
        CountDownLatch[] latches = new CountDownLatch[count];
        for (int i = 0; i < count; i++) {
            latches[i] = new CountDownLatch(1);
        }
        return latches;
    }

    private static void countDownAll(CountDownLatch[] latches) {
        for (CountDownLatch latch : latches) {
            latch.countDown();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitLatchWithGcFallback(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(500, TimeUnit.MILLISECONDS)) {
            runGcAndFinalization();
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void updateMax(AtomicInteger maxValue, int currentValue) {
        while (true) {
            int prevMax = maxValue.get();
            if (currentValue <= prevMax || maxValue.compareAndSet(prevMax, currentValue)) {
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    private static FakeImageProxy createRgbaTestYuvFrame(Size resolution, long timestamp) {
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTimestamp(timestamp);
        return createYuvFakeImageProxy(
                imageInfo,
                resolution.getWidth(),
                resolution.getHeight(),
                ImageProxyUtil.YUV_FORMAT_PLANE_DATA_TYPE_I420,
                false);
    }

    // Best-effort GC trigger needed to collect dropped CallbackToFutureAdapter.Completer instances.
    private static void runGcAndFinalization() throws InterruptedException {
        CountDownLatch finalizedLatch = new CountDownLatch(1);
        createUnreachableFinalizer(finalizedLatch);
        Runtime.getRuntime().gc();
        System.runFinalization();
        finalizedLatch.await(200, TimeUnit.MILLISECONDS);
    }

    private static void createUnreachableFinalizer(CountDownLatch latch) {
        new Object() {
            @SuppressWarnings({"deprecation", "removal"})
            @Override
            protected void finalize() {
                latch.countDown();
            }
        };
    }

    private void verifyAnalysis(
            ImageAnalysisAbstractAnalyzer analyzer,
            ImageProxy imageToAnalyze,
            Bitmap expectedBitmap,
            ArgumentCaptor<ImageProxy> captor) throws Exception {
        analyzer.analyzeImage(imageToAnalyze).get(1, TimeUnit.SECONDS);
        verify(mMockAnalyzer).analyze(captor.capture());

        ImageProxy outputImage = captor.getValue();
        Bitmap resultBitmap = ImageUtil.createBitmapFromImageProxy(outputImage);

        assertThat(getAverageDiff(resultBitmap, expectedBitmap)).isEqualTo(0);
        outputImage.close();
        reset(mMockAnalyzer);
    }
}
