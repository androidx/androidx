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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Unit test for {@link ImageAnalysis}.
 */
@MediumTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, shadows = {ShadowCameraX.class,
        ShadowImageReader.class})
public class ImageAnalysisTest {

    private static final int QUEUE_DEPTH = 8;
    private static final Image MOCK_IMAGE_1 = createMockImage(1);
    private static final Image MOCK_IMAGE_2 = createMockImage(2);
    private static final Image MOCK_IMAGE_3 = createMockImage(3);

    private Handler mCallbackHandler;
    private Handler mBackgroundHandler;
    private Executor mBackgroundExecutor;
    private List<Image> mImagesReceived;
    private ImageAnalysis mImageAnalysis;

    private HandlerThread mBackgroundThread;

    @Before
    public void setUp() {
        HandlerThread callbackThread = new HandlerThread("Callback");
        callbackThread.start();
        mCallbackHandler = new Handler(callbackThread.getLooper());

        mBackgroundThread = new HandlerThread("Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundExecutor = CameraXExecutors.newHandlerExecutor(mBackgroundHandler);

        mImagesReceived = new ArrayList<>();

        ShadowImageReader.clear();

        FakeCameraFactory cameraFactory = new FakeCameraFactory();
        cameraFactory.insertDefaultBackCamera(ShadowCameraX.DEFAULT_CAMERA_ID,
                () -> new FakeCamera(ShadowCameraX.DEFAULT_CAMERA_ID));

        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).setCameraFactory(cameraFactory).build();

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(CameraX::unbindAll);
        mImagesReceived.clear();
        CameraX.shutdown().get();
    }

    @Test
    public void nonBlockingAnalyzerClosed_imageNotAnalyzed() {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        // Receive images from camera feed.
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_1);
        flushHandler(mBackgroundHandler);
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_2);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImagesReceived).isEmpty();

        // Flush callback handler and image1 is received.
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).containsExactly(MOCK_IMAGE_1);

        // Clear ImageAnalysis and flush both handlers. No more image should be received because
        // it's closed.
        mImageAnalysis.clear();
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).containsExactly(MOCK_IMAGE_1);
    }

    @Test
    public void blockingAnalyzerClosed_imageNotAnalyzed() {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);

        // Act.
        // Receive images from camera feed.
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_1);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImagesReceived).isEmpty();

        // Flush callback handler and it's still empty because it's close.
        mImageAnalysis.clear();
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).isEmpty();
    }

    @Test
    public void keepOnlyLatestStrategy_doesNotBlock() {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        // Receive images from camera feed.
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_1);
        flushHandler(mBackgroundHandler);
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_2);
        flushHandler(mBackgroundHandler);
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_3);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImagesReceived).isEmpty();

        // Flush callback handler and image1 is received.
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).containsExactly(MOCK_IMAGE_1);

        // Flush both handlers and the previous cached image3 is received (image2 was dropped). The
        // code alternates the 2 threads so they have to be both flushed to proceed.
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).containsExactly(MOCK_IMAGE_1, MOCK_IMAGE_3);

        // Flush both handlers and no more frame.
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).containsExactly(MOCK_IMAGE_1, MOCK_IMAGE_3);
    }

    @Test
    public void blockProducerStrategy_doesNotDropFrames() {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);

        // Act.
        // Receive images from camera feed.
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_1);
        flushHandler(mBackgroundHandler);
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_2);
        flushHandler(mBackgroundHandler);
        ShadowImageReader.triggerCallbackWithImage(MOCK_IMAGE_3);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImagesReceived).isEmpty();

        // Flush callback handler and 3 frames received.
        flushHandler(mCallbackHandler);
        assertThat(mImagesReceived).containsExactly(MOCK_IMAGE_1, MOCK_IMAGE_2, MOCK_IMAGE_3);
    }

    private void setUpImageAnalysisWithStrategy(
            @ImageAnalysis.BackpressureStrategy int backpressureStrategy) {
        mImageAnalysis = new ImageAnalysis.Builder()
                .setBackgroundExecutor(mBackgroundExecutor)
                .setImageQueueDepth(QUEUE_DEPTH)
                .setBackpressureStrategy(backpressureStrategy)
                .build();

        mImageAnalysis.setAnalyzer(CameraXExecutors.newHandlerExecutor(mCallbackHandler),
                (image) -> {
                    mImagesReceived.add(image.getImage());
                    image.close();
                }
        );

        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CameraX.bindToLifecycle(lifecycleOwner, cameraSelector, mImageAnalysis);
            lifecycleOwner.startAndResume();
        });
    }

    /**
     * Flushes a {@link Handler} to run all pending tasks.
     *
     * @param handler the {@link Handler} to flush.
     */
    private static void flushHandler(Handler handler) {
        ((ShadowLooper) Shadow.extract(handler.getLooper())).idle();
    }

    private static Image createMockImage(long timestamp) {
        Image mockImage = mock(Image.class);
        when(mockImage.getTimestamp()).thenReturn(timestamp);
        return mockImage;
    }
}
