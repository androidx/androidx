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

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.impl.HandlerUtil;
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class MetadataImageReaderTest {
    private static final long TIMESTAMP_0 = 0L;
    private static final long TIMESTAMP_1 = 1000L;
    private static final long TIMESTAMP_NONEXISTANT = 5000L;
    private FakeImageReaderProxy mImageReader;
    private final FakeCameraCaptureResult mCameraCaptureResult0 = new FakeCameraCaptureResult();
    private final FakeCameraCaptureResult mCameraCaptureResult1 = new FakeCameraCaptureResult();

    private final Semaphore mSemaphore = new Semaphore(0);
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Executor mBackgroundExecutor;

    private MetadataImageReader mMetadataImageReader;

    @Before
    public void setUp() {
        mBackgroundThread = new HandlerThread("CallbackThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mBackgroundExecutor = CameraXExecutors.newHandlerExecutor(mBackgroundHandler);

        createMetadataImageReaderWithCapacity(8);
        mCameraCaptureResult0.setTimestamp(TIMESTAMP_0);
        mCameraCaptureResult1.setTimestamp(TIMESTAMP_1);
    }

    @After
    public void tearDown() {
        if (mImageReader != null) {
            mImageReader.close();
        }

        if (mMetadataImageReader != null) {
            mMetadataImageReader.close();
        }

        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
    }

    @Test
    public void canBindImageToImageInfoWithSameTimestamp() throws InterruptedException {
        // Triggers CaptureCompleted with two different CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult1);

        final AtomicReference<ImageProxy> firstReceivedImageProxy = new AtomicReference<>();

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        // Checks if the output Image has ImageInfo with same timestamp.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        firstReceivedImageProxy.set(resultImage);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);
        // Triggers ImageAvailable with one Image.
        triggerImageAvailable(TIMESTAMP_0);

        mSemaphore.acquire();

        final AtomicReference<ImageProxy> secondReceivedImageProxy = new AtomicReference<>();


        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        // Checks if the MetadataImageReader can output the other matched
                        // ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        secondReceivedImageProxy.set(resultImage);

                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);
        // Triggers ImageAvailable with another Image with different timestamp.
        triggerImageAvailable(TIMESTAMP_1);
        mSemaphore.acquire();

        assertThat(firstReceivedImageProxy.get().getImageInfo().getTimestamp()).isEqualTo(
                TIMESTAMP_0);

        assertThat(secondReceivedImageProxy.get().getImageInfo().getTimestamp()).isEqualTo(
                TIMESTAMP_1);
    }

    @Test
    public void canBindImageInfoToImageWithSameTimestamp() throws InterruptedException {
        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        // Checks if the output contains the first matched ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_0);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);

        // Triggers ImageAvailable with two different Image.
        triggerImageAvailable(TIMESTAMP_0);
        triggerImageAvailable(TIMESTAMP_1);

        // Triggers CaptureCompleted with one CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult0);
        mSemaphore.acquire();

        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        // Checks if the MetadataImageReader can output the other ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_1);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);
        // Triggers CaptureCompleted with another CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult1);
        mSemaphore.acquire();
    }

    @Test
    public void clearOnImageAvailableListener() throws InterruptedException {
        AtomicInteger mListenCount = new AtomicInteger(0);
        ImageReaderProxy.OnImageAvailableListener outputListener =
                (imageReader) -> {
                    // Count how many times the output listener is triggered with an ImageProxy
                    ImageProxy resultImage = imageReader.acquireNextImage();
                    if (resultImage != null) {
                        mListenCount.getAndIncrement();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);

        // Triggers ImageAvailable with two different Image.
        triggerImageAvailable(TIMESTAMP_0);
        triggerImageAvailable(TIMESTAMP_1);

        // Triggers CaptureCompleted with one CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult0);

        // Make sure the first image has been received before clearing the listener
        HandlerUtil.waitForLooperToIdle(mBackgroundHandler);
        mMetadataImageReader.clearOnImageAvailableListener();

        // Triggers CaptureCompleted with another CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult1);

        HandlerUtil.waitForLooperToIdle(mBackgroundHandler);

        // The second image will not have been received by the listener
        assertThat(mListenCount.get()).isEqualTo(1);
    }

    @Test
    public void canNotFindAMatch() throws InterruptedException {
        // Triggers CaptureCompleted with two different CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult1);

        final AtomicBoolean receivedImage = new AtomicBoolean(false);

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        // If the Metadata still get a match, fail the test case.
                        receivedImage.set(true);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);
        // Triggers ImageAvailable with an Image which contains a timestamp doesn't match any of
        // the CameraResult.
        triggerImageAvailable(TIMESTAMP_NONEXISTANT);

        HandlerUtil.waitForLooperToIdle(mBackgroundHandler);

        assertThat(receivedImage.get()).isFalse();
    }

    @Test
    public void maxImageHasBeenAcquired() throws InterruptedException {
        // Creates a MetadataImageReader with only one capacity.
        createMetadataImageReaderWithCapacity(1);

        // Feeds two CaptureResult into it.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                mCameraCaptureResult1);

        final AtomicReference<ImageProxy> receivedImage = new AtomicReference<>();

        ImageReaderProxy.OnImageAvailableListener outputListener = imageReader -> {
                        // The First ImageProxy is output without closing.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        receivedImage.set(resultImage);
                        mSemaphore.release();
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);
        // Feeds the first Image.
        triggerImageAvailable(TIMESTAMP_0);
        mSemaphore.acquire();

        assertThat(receivedImage.get().getImageInfo().getTimestamp()).isEqualTo(
                TIMESTAMP_0);

        Semaphore secondSemaphore = new Semaphore(0);
        outputListener = imageReader -> {
            // The second ImageProxy should be dropped, otherwise fail the test case.
            secondSemaphore.release();
        };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundExecutor);
        // Feeds the second Image.
        TagBundle tagBundle = TagBundle.create(new Pair<>("FakeCaptureStageId", 0));
        mImageReader.triggerImageAvailable(tagBundle, TIMESTAMP_1);
        HandlerUtil.waitForLooperToIdle(mBackgroundHandler);

        // OnImageAvailableListener won't be called for the 2nd image.
        assertThat(secondSemaphore.tryAcquire(1, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    public void doesNotBlockOnMismatchingImageInfoAndImageProxy() throws InterruptedException {
        createMetadataImageReaderWithCapacity(3);

        final AtomicBoolean imageReceived = new AtomicBoolean(false);

        mMetadataImageReader.setOnImageAvailableListener(
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(@NonNull ImageReaderProxy imageReader) {
                        // No image should be available since there should be no matches found
                        imageReceived.set(true);
                    }
                },
                mBackgroundExecutor);

        // Trigger ImageInfo and ImageProxy to be pushed into the MetadataImageReader, which should
        // discard older data without matches so it does not get blocked.
        for (int i = 0; i < 5; i++) {
            triggerImageAvailable(i * 2);
            triggerImageInfoAvailable(i * 2 + 1);
        }

        HandlerUtil.waitForLooperToIdle(mBackgroundHandler);

        assertThat(imageReceived.get()).isFalse();
    }

    private void createMetadataImageReaderWithCapacity(int maxImages) {
        mImageReader = new FakeImageReaderProxy(maxImages);
        mMetadataImageReader = new MetadataImageReader(mImageReader);
    }

    private void triggerImageAvailable(long timestamp) throws InterruptedException {
        mImageReader.triggerImageAvailable(TagBundle.create(new Pair<>("FakeCaptureStageId",
                        null)), timestamp);
    }

    private void triggerImageInfoAvailable(long timestamp) {
        FakeCameraCaptureResult.Builder builder = new FakeCameraCaptureResult.Builder();
        builder.setTimestamp(timestamp);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(CaptureConfig.DEFAULT_ID,
                builder.build());
    }
}
