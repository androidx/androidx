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

import static junit.framework.TestCase.fail;

import android.os.Handler;
import android.os.HandlerThread;

import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class MetadataImageReaderTest {
    private static final long TIMESTAMP_0 = 0L;
    private static final long TIMESTAMP_1 = 1000L;
    private static final long TIMESTAMP_NONEXISTANT = 5000L;
    private final FakeImageReaderProxy mImageReader = new FakeImageReaderProxy();
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

    @Test(timeout = 500)
    public void canBindImageToImageInfoWithSameTimestamp() throws InterruptedException {
        // Triggers CaptureCompleted with two different CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // Checks if the output Image has ImageInfo with same timestamp.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getTimestamp()).isEqualTo(TIMESTAMP_0);
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_0);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers ImageAvailable with one Image.
        triggerImageAvailable(TIMESTAMP_0);

        mSemaphore.acquire();

        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // Checks if the MetadataImageReader can output the other matched
                        // ImageProxy.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getTimestamp()).isEqualTo(TIMESTAMP_1);
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_1);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers ImageAvailable with another Image with different timestamp.
        triggerImageAvailable(TIMESTAMP_1);
        mSemaphore.acquire();
    }

    @Test(timeout = 500)
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

    @Test(timeout = 500)
    public void canNotFindAMatch() throws InterruptedException {
        // Triggers CaptureCompleted with two different CaptureResult.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // If the Metadata still get a match, fail the test case.
                        fail("Match should not be found.");
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Triggers ImageAvailable with an Image which contains a timestamp doesn't match any of
        // the CameraResult.
        triggerImageAvailable(TIMESTAMP_NONEXISTANT);
        // Waits for a period of time.
        mSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 500)
    public void maxImageHasBeenAcquired() throws InterruptedException {
        // Creates a MetadataImageReader with only one capacity.
        createMetadataImageReaderWithCapacity(1);

        // Feeds two CaptureResult into it.
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult0);
        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(mCameraCaptureResult1);

        ImageReaderProxy.OnImageAvailableListener outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // The First ImageProxy is output without closing.
                        ImageProxy resultImage = imageReader.acquireNextImage();
                        assertThat(resultImage.getTimestamp()).isEqualTo(TIMESTAMP_0);
                        assertThat(resultImage.getImageInfo().getTimestamp()).isEqualTo(
                                TIMESTAMP_0);
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Feeds the first Image.
        triggerImageAvailable(TIMESTAMP_0);
        mSemaphore.acquire();

        outputListener =
                new ImageReaderProxy.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReaderProxy imageReader) {
                        // The second ImageProxy should be dropped, otherwise fail the test case.
                        fail("Should not exceed maximum Image number.");
                        mSemaphore.release();
                    }
                };
        mMetadataImageReader.setOnImageAvailableListener(outputListener, mBackgroundHandler);
        // Feeds the second Image.
        triggerImageAvailable(TIMESTAMP_1);
        // Waits for a time period.
        mSemaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
    }

    private void createMetadataImageReaderWithCapacity(int maxImages) {
        mImageReader.setMaxImages(maxImages);
        mMetadataImageReader = new MetadataImageReader(mImageReader, null);
    }

    private void triggerImageAvailable(long timestamp) {
        FakeImageProxy image = new FakeImageProxy();
        image.setTimestamp(timestamp);
        mImageReader.setImageProxy(image);
        mImageReader.triggerImageAvailable();
    }
}
