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

import androidx.camera.testing.HandlerUtil;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private MetadataImageReader mMetadataImageReader;

    @Before
    public void setUp() {
        mBackgroundThread = new HandlerThread("CallbackThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        createMetadataImageReaderWithCapacity(8);
        mCameraCaptureResult0.setTimestamp(TIMESTAMP_0);
        mCameraCaptureResult1.setTimestamp(TIMESTAMP_1);
    }

    @Test
    public void canBindImageToImageInfoWithSameTimestamp() throws InterruptedException {
        // Triggers CaptureCompleted with two different CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);

        final AtomicReference<ImageProxy> firstReceivedImageProxy = new AtomicReference<>();

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // Checks if the output Image has ImageInfo with same timestamp.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        firstReceivedImageProxy.set(resultImage);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers ImageAvailable with one Image.
        triggerImageAvailable(TIMESTAMP_0);

        mSemaphore.acquire();

        final AtomicReference<ImageProxy> secondReceivedImageProxy = new AtomicReference<>();


        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // Checks if the MetadataImageReader can output the other matched
                        // ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        secondReceivedImageProxy.set(resultImage);

                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers ImageAvailable with another Image with different timestamp.
        triggerImageAvailable(TIMESTAMP_1);
        mSemaphore.acquire();

        assertThat(firstReceivedImageProxy.get().getTimestamp()).isEqualTo(TIMESTAMP_0);
        assertThat(firstReceivedImageProxy.get().getImageInfo().getTimestamp()).isEqualTo(
                TIMESTAMP_0);

        assertThat(secondReceivedImageProxy.get().getTimestamp()).isEqualTo(TIMESTAMP_1);
        assertThat(secondReceivedImageProxy.get().getImageInfo().getTimestamp()).isEqualTo(
                TIMESTAMP_1);
    }

    @Test
    public void canBindImageInfoToImageWithSameTimestamp() throws InterruptedException {
        // Triggers ImageAvailable with two different Image.
        triggerImageAvailable(TIMESTAMP_0);
        triggerImageAvailable(TIMESTAMP_1);

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // Checks if the output contains the first matched ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getTimestamp()).isEqualTo(TIMESTAMP_0);
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_0);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers CaptureCompleted with one CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mSemaphore.acquire();

        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // Checks if the MetadataImageReader can output the other ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getTimestamp()).isEqualTo(TIMESTAMP_1);
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_1);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers CaptureCompleted with another CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);
        mSemaphore.acquire();
    }

    @Test
    public void canNotFindAMatch() throws InterruptedException {
        // Triggers CaptureCompleted with two different CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);

        final AtomicBoolean receivedImage = new AtomicBoolean(false);

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // If the Metadata still get a match, fail the test case.
                        receivedImage.set(true);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
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
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);

        final AtomicReference<ImageProxy> receivedImage = new AtomicReference<>();

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // The First ImageProxy is output without closing.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        receivedImage.set(resultImage);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Feeds the first Image.
        triggerImageAvailable(TIMESTAMP_0);
        mSemaphore.acquire();

        assertThat(receivedImage.get().getTimestamp()).isEqualTo(TIMESTAMP_0);
        assertThat(receivedImage.get().getImageInfo().getTimestamp()).isEqualTo(
                TIMESTAMP_0);

        final AtomicBoolean hasReceivedSecondImage = new AtomicBoolean(false);
        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // The second ImageProxy should be dropped, otherwise fail the test case.
                        hasReceivedSecondImage.set(true);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Feeds the second Image.
        assertThat(mImageReader.triggerImageAvailable(null, TIMESTAMP_1, 50,
                TimeUnit.MILLISECONDS)).isFalse();

        HandlerUtil.waitForLooperToIdle(mBackgroundHandler);

        assertThat(hasReceivedSecondImage.get()).isFalse();
    }

    @Test
    public void doesNotBlockOnMismatchingImageInfoAndImageProxy() throws InterruptedException {
        createMetadataImageReaderWithCapacity(3);

        final AtomicBoolean imageReceived = new AtomicBoolean(false);

        mMetadataImageReader.setOnImageAvailableListener(
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // No image should be available since there should be no matches found
                        imageReceived.set(true);
                    }
                },
                mBackgroundHandler
        );

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
        mMetadataImageReader = new MetadataImageReader(mImageReader, null);
    }

    private void triggerImageAvailable(long timestamp) throws InterruptedException {
        mImageReader.triggerImageAvailable(null, timestamp);
    }

    private void triggerImageInfoAvailable(long timestamp) {
        FakeCameraCaptureResult.Builder builder = new FakeCameraCaptureResult.Builder();
        builder.setTimestamp(timestamp);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(builder.build());
    }
}
