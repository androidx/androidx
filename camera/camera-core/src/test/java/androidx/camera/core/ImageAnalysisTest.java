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

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.Iterables;

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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, shadows = {ShadowCameraX.class})
public class ImageAnalysisTest {

    private static final int QUEUE_DEPTH = 8;
    private static final String IMAGE_TAG = "IMAGE_TAG";
    private static final long TIMESTAMP_1 = 1;
    private static final long TIMESTAMP_2 = 2;
    private static final long TIMESTAMP_3 = 3;

    private Handler mCallbackHandler;
    private Handler mBackgroundHandler;
    private Executor mBackgroundExecutor;
    private List<ImageProxy> mImageProxiesReceived;
    private ImageAnalysis mImageAnalysis;
    private FakeImageReaderProxy mFakeImageReaderProxy;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        mFakeImageReaderProxy = new FakeImageReaderProxy(QUEUE_DEPTH);
        HandlerThread callbackThread = new HandlerThread("Callback");
        callbackThread.start();
        mCallbackHandler = new Handler(callbackThread.getLooper());

        HandlerThread backgroundThread = new HandlerThread("Background");
        backgroundThread.start();
        mBackgroundHandler = new Handler(backgroundThread.getLooper());
        mBackgroundExecutor = CameraXExecutors.newHandlerExecutor(mBackgroundHandler);

        mImageProxiesReceived = new ArrayList<>();

        CameraFactory.Provider cameraFactoryProvider = (ignored1, ignored2) -> {
            FakeCameraFactory cameraFactory = new FakeCameraFactory();
            cameraFactory.insertDefaultBackCamera(ShadowCameraX.DEFAULT_CAMERA_ID,
                    () -> new FakeCamera(ShadowCameraX.DEFAULT_CAMERA_ID));
            return cameraFactory;
        };

        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).setCameraFactoryProvider(cameraFactoryProvider).build();

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig).get();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(CameraX::unbindAll);
        mImageProxiesReceived.clear();
        CameraX.shutdown().get();
    }

    @Test
    public void resultSize_isEqualToSurfaceSize() throws InterruptedException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);

        // Assert.
        assertThat(Iterables.getOnlyElement(mImageProxiesReceived).getHeight()).isEqualTo(
                mFakeImageReaderProxy.getHeight());
        assertThat(Iterables.getOnlyElement(mImageProxiesReceived).getWidth()).isEqualTo(
                mFakeImageReaderProxy.getWidth());
    }

    @Test
    public void nonBlockingAnalyzerClosed_imageNotAnalyzed() throws InterruptedException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_2);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and image1 is received.
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1);

        // Clear ImageAnalysis and flush both handlers. No more image should be received because
        // it's closed.
        mImageAnalysis.clear();
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1);
    }

    @Test
    public void blockingAnalyzerClosed_imageNotAnalyzed() throws InterruptedException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and it's still empty because it's close.
        mImageAnalysis.clear();
        flushHandler(mCallbackHandler);
        assertThat(mImageProxiesReceived).isEmpty();
    }

    @Test
    public void keepOnlyLatestStrategy_doesNotBlock() throws InterruptedException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_2);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_3);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and image1 is received.
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1);

        // Flush both handlers and the previous cached image3 is received (image2 was dropped). The
        // code alternates the 2 threads so they have to be both flushed to proceed.
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1, TIMESTAMP_3);

        // Flush both handlers and no more frame.
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1, TIMESTAMP_3);
    }

    @Test
    public void blockProducerStrategy_doesNotDropFrames() throws InterruptedException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_2);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(IMAGE_TAG, TIMESTAMP_3);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and 3 frames received.
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived())
                .containsExactly(TIMESTAMP_1, TIMESTAMP_2, TIMESTAMP_3);
    }

    private void setUpImageAnalysisWithStrategy(
            @ImageAnalysis.BackpressureStrategy int backpressureStrategy) {
        mImageAnalysis = new ImageAnalysis.Builder()
                .setBackgroundExecutor(mBackgroundExecutor)
                .setImageQueueDepth(QUEUE_DEPTH)
                .setBackpressureStrategy(backpressureStrategy)
                .setImageReaderProxyProvider(
                        (width, height, format, queueDepth, usage) -> mFakeImageReaderProxy)
                .build();

        mImageAnalysis.setAnalyzer(CameraXExecutors.newHandlerExecutor(mCallbackHandler),
                (image) -> {
                    mImageProxiesReceived.add(image);
                    image.close();
                }
        );

        FakeLifecycleOwner lifecycleOwner = new FakeLifecycleOwner();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CameraX.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                    mImageAnalysis);
            lifecycleOwner.startAndResume();
        });
    }

    private List<Long> getImageTimestampsReceived() {
        List<Long> imagesReceived = new ArrayList<>();
        for (ImageProxy imageProxy : mImageProxiesReceived) {
            imagesReceived.add(imageProxy.getImageInfo().getTimestamp());
        }
        return imagesReceived;
    }

    /**
     * Flushes a {@link Handler} to run all pending tasks.
     *
     * @param handler the {@link Handler} to flush.
     */
    private static void flushHandler(Handler handler) {
        ((ShadowLooper) Shadow.extract(handler.getLooper())).idle();
    }
}
