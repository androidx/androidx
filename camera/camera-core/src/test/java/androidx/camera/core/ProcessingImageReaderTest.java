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

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.ImageFormat;
import android.os.Build;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeImageReaderProxy;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// UnstableApiUsage is needed because PausedExecutorService is marked @Beta
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class ProcessingImageReaderTest {
    private static final int CAPTURE_ID_0 = 0;
    private static final int CAPTURE_ID_1 = 1;
    private static final int CAPTURE_ID_2 = 2;
    private static final int CAPTURE_ID_3 = 3;
    private static final long TIMESTAMP_0 = 0L;
    private static final long TIMESTAMP_1 = 1000L;
    private static final long TIMESTAMP_2 = 2000L;
    private static final long TIMESTAMP_3 = 4000L;
    private static final CaptureProcessor NOOP_PROCESSOR = new CaptureProcessor() {
        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {

        }

        @Override
        public void process(ImageProxyBundle bundle) {

        }

        @Override
        public void onResolutionUpdate(Size size) {

        }
    };
    private static PausedExecutorService sPausedExecutor;
    private final CaptureStage mCaptureStage0 = new FakeCaptureStage(CAPTURE_ID_0, null);
    private final CaptureStage mCaptureStage1 = new FakeCaptureStage(CAPTURE_ID_1, null);
    private final CaptureStage mCaptureStage2 = new FakeCaptureStage(CAPTURE_ID_2, null);
    private final CaptureStage mCaptureStage3 = new FakeCaptureStage(CAPTURE_ID_3, null);
    private final FakeImageReaderProxy mImageReaderProxy = new FakeImageReaderProxy(8);
    private MetadataImageReader mMetadataImageReader;
    private CaptureBundle mCaptureBundle;
    private String mTagBundleKey;

    @BeforeClass
    public static void setUpClass() {
        sPausedExecutor = new PausedExecutorService();
    }

    @AfterClass
    public static void tearDownClass() {
        sPausedExecutor.shutdown();
    }

    @Before
    public void setUp() {
        mCaptureBundle = CaptureBundles.createCaptureBundle(mCaptureStage0, mCaptureStage1);
        mTagBundleKey = Integer.toString(mCaptureBundle.hashCode());
        mMetadataImageReader = new MetadataImageReader(mImageReaderProxy);
    }

    @After
    public void cleanUp() {
        // Ensure the PausedExecutorService is drained
        sPausedExecutor.runAll();
    }

    @Test
    public void canSetFuturesInSettableImageProxyBundle()
            throws InterruptedException, TimeoutException, ExecutionException {
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor captureProcessor = mock(CaptureProcessor.class);
        ProcessingImageReader processingImageReader = new ProcessingImageReader.Builder(
                mMetadataImageReader, mCaptureBundle, captureProcessor).setPostProcessExecutor(
                sPausedExecutor).build();
        processingImageReader.setOnImageAvailableListener(mock(
                ImageReaderProxy.OnImageAvailableListener.class),
                CameraXExecutors.mainThreadExecutor());
        Map<Integer, Long> resultMap = new HashMap<>();
        resultMap.put(CAPTURE_ID_0, TIMESTAMP_0);
        resultMap.put(CAPTURE_ID_1, TIMESTAMP_1);

        // Cache current CaptureBundle as the TagBundle key for generate the fake image
        mTagBundleKey = processingImageReader.getTagBundleKey();
        triggerAndVerify(captureProcessor, resultMap);
        Mockito.reset(captureProcessor);

        CaptureBundle captureBundle = CaptureBundles.createCaptureBundle(mCaptureStage2,
                mCaptureStage3);
        processingImageReader.setCaptureBundle(captureBundle);

        // Reset the key for TagBundle because the CaptureBundle is renewed
        mTagBundleKey = processingImageReader.getTagBundleKey();

        Map<Integer, Long> resultMap1 = new HashMap<>();
        resultMap1.put(CAPTURE_ID_2, TIMESTAMP_2);
        resultMap1.put(CAPTURE_ID_3, TIMESTAMP_3);
        triggerAndVerify(captureProcessor, resultMap1);
    }

    private void triggerAndVerify(CaptureProcessor captureProcessor,
            Map<Integer, Long> captureIdToTime)
            throws InterruptedException, ExecutionException, TimeoutException {
        // Feeds ImageProxy with all capture id on the initial list.
        for (Integer id : captureIdToTime.keySet()) {
            triggerImageAvailable(id, captureIdToTime.get(id));
        }

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle();

        // Run processing
        sPausedExecutor.runAll();

        ArgumentCaptor<ImageProxyBundle> imageProxyBundleCaptor =
                ArgumentCaptor.forClass(ImageProxyBundle.class);
        verify(captureProcessor, times(1)).process(imageProxyBundleCaptor.capture());
        assertThat(imageProxyBundleCaptor.getValue()).isNotNull();

        // CaptureProcessor.process should be called once all ImageProxies on the
        // initial lists are ready. Then checks if the output has matched timestamp.
        for (Integer id : captureIdToTime.keySet()) {
            assertThat(imageProxyBundleCaptor.getValue().getImageProxy(id).get(0,
                    TimeUnit.SECONDS).getImageInfo().getTimestamp()).isEqualTo(
                    captureIdToTime.get(id));
        }
    }

    // Make sure that closing the ProcessingImageReader while the CaptureProcessor is processing
    // the image is safely done so that the CaptureProcessor will not be accessing closed images
    @Test
    public void canCloseWhileProcessingIsOccurring()
            throws InterruptedException {
        // Sets the callback from ProcessingImageReader to start processing
        WaitingCaptureProcessor waitingCaptureProcessor = new WaitingCaptureProcessor();
        ProcessingImageReader processingImageReader = new ProcessingImageReader.Builder(
                mMetadataImageReader, mCaptureBundle, waitingCaptureProcessor).build();
        processingImageReader.setOnImageAvailableListener(mock(
                ImageReaderProxy.OnImageAvailableListener.class),
                CameraXExecutors.mainThreadExecutor());
        Map<Integer, Long> resultMap = new HashMap<>();
        resultMap.put(CAPTURE_ID_0, TIMESTAMP_0);
        resultMap.put(CAPTURE_ID_1, TIMESTAMP_1);

        // Cache current CaptureBundle as the TagBundle key for generate the fake image
        mTagBundleKey = processingImageReader.getTagBundleKey();

        // Trigger the Images so that the CaptureProcessor starts
        for (Map.Entry<Integer, Long> idTimestamp : resultMap.entrySet()) {
            triggerImageAvailable(idTimestamp.getKey(), idTimestamp.getValue());
        }

        // Ensure tasks are posted to the processing executor
        shadowOf(getMainLooper()).idle();

        // Wait for CaptureProcessor.process() to start so that it is in the middle of processing
        assertThat(waitingCaptureProcessor.waitForProcessingToStart(3000)).isTrue();

        processingImageReader.close();

        // Allow the CaptureProcessor to continue processing. Calling finishProcessing() will
        // cause the CaptureProcessor to start accessing the ImageProxy. If the ImageProxy has
        // already been closed then we will time out at waitForProcessingToComplete().
        waitingCaptureProcessor.finishProcessing();

        // The processing will only complete if no exception was thrown during the processing
        // which causes it to return prematurely.
        assertThat(waitingCaptureProcessor.waitForProcessingToComplete(3000)).isTrue();
    }

    // Tests that a ProcessingImageReader can be closed while in the process of receiving
    // ImageProxies for an ImageProxyBundle.
    @Test
    public void closeImageHalfway() throws InterruptedException {
        // Sets the callback from ProcessingImageReader to start processing
        ProcessingImageReader processingImageReader = new ProcessingImageReader.Builder(
                mMetadataImageReader, mCaptureBundle, NOOP_PROCESSOR).setPostProcessExecutor(
                sPausedExecutor).build();
        processingImageReader.setOnImageAvailableListener(mock(
                ImageReaderProxy.OnImageAvailableListener.class),
                CameraXExecutors.mainThreadExecutor());

        // Cache current CaptureBundle as the TagBundle key for generate the fake image
        mTagBundleKey = processingImageReader.getTagBundleKey();
        triggerImageAvailable(CAPTURE_ID_0, TIMESTAMP_0);

        // Ensure the first image is received by the ProcessingImageReader
        shadowOf(getMainLooper()).idle();

        // The ProcessingImageReader is closed after receiving the first image, but before
        // receiving enough images for the entire ImageProxyBundle.
        processingImageReader.close();

        assertThat(mImageReaderProxy.isClosed()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void imageReaderSizeIsSmallerThanCaptureBundle() {
        // Creates a ProcessingImageReader with maximum Image number smaller than CaptureBundle
        // size.
        ImageReaderProxy imageReaderProxy = new FakeImageReaderProxy(1);
        MetadataImageReader metadataImageReader = new MetadataImageReader(imageReaderProxy);

        // Expects to throw exception when creating ProcessingImageReader.
        new ProcessingImageReader.Builder(metadataImageReader, mCaptureBundle,
                NOOP_PROCESSOR).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void captureStageExceedMaxCaptureStage_setCaptureBundleThrowsException() {
        // Creates a ProcessingImageReader with maximum Image number.
        ProcessingImageReader processingImageReader = new ProcessingImageReader.Builder(100, 100,
                ImageFormat.YUV_420_888, 2, mCaptureBundle, mock(CaptureProcessor.class)).build();

        // Expects to throw exception when invoke the setCaptureBundle method with a
        // CaptureBundle size greater than maximum image number.
        processingImageReader.setCaptureBundle(
                CaptureBundles.createCaptureBundle(mCaptureStage1, mCaptureStage2, mCaptureStage3));
    }

    @Test
    public void imageReaderFormatIsOutputFormat() {
        // Creates a ProcessingImageReader with input format YUV_420_888 and output JPEG
        ProcessingImageReader processingImageReader = new ProcessingImageReader.Builder(100, 100,
                ImageFormat.YUV_420_888, 2, mCaptureBundle,
                mock(CaptureProcessor.class)).setOutputFormat(ImageFormat.JPEG).build();

        assertThat(processingImageReader.getImageFormat()).isEqualTo(ImageFormat.JPEG);
    }

    private void triggerImageAvailable(int captureId, long timestamp) throws InterruptedException {
        TagBundle tagBundle = TagBundle.create(new Pair<>(mTagBundleKey, captureId));
        mImageReaderProxy.triggerImageAvailable(tagBundle, timestamp);
        FakeCameraCaptureResult.Builder builder = new FakeCameraCaptureResult.Builder();
        builder.setTimestamp(timestamp);
        builder.setTag(tagBundle);

        mMetadataImageReader.getCameraCaptureCallback().onCaptureCompleted(builder.build());
    }

    // Only allows for processing once.
    private static class WaitingCaptureProcessor implements CaptureProcessor {
        // Block processing so that the ProcessingImageReader can be closed before the
        // CaptureProcessor has finished accessing the ImageProxy and ImageProxyBundle
        private final CountDownLatch mProcessingLatch = new CountDownLatch(1);

        // To wait for processing to start. This makes sure that the ProcessingImageReader can be
        // closed after processing has started
        private final CountDownLatch mProcessingStartLatch = new CountDownLatch(1);

        // Block processing from completing. This ensures that the CaptureProcessor has finished
        // accessing the ImageProxy and ImageProxyBundle successfully.
        private final CountDownLatch mProcessingComplete = new CountDownLatch(1);

        WaitingCaptureProcessor() {
        }

        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {
        }

        @Override
        public void process(ImageProxyBundle bundle) {
            mProcessingStartLatch.countDown();
            try {
                mProcessingLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            ImageProxy imageProxy;
            try {
                imageProxy = bundle.getImageProxy(CAPTURE_ID_0).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return;
            }

            // Try to get the crop rect. If the image has already been closed it will thrown an
            // IllegalStateException
            try {
                imageProxy.getFormat();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }

            mProcessingComplete.countDown();
        }

        @Override
        public void onResolutionUpdate(Size size) {
        }

        void finishProcessing() {
            mProcessingLatch.countDown();
        }

        /** Returns false if it fails to start processing. */
        boolean waitForProcessingToStart(long timeout) {
            try {
                return mProcessingStartLatch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }

        /** Returns false if processing does not complete. */
        boolean waitForProcessingToComplete(long timeout) {
            try {
                return mProcessingComplete.await(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }
}
