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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.ImageFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
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

    private final CaptureStage mCaptureStage0 = new FakeCaptureStage(CAPTURE_ID_0, null);
    private final CaptureStage mCaptureStage1 = new FakeCaptureStage(CAPTURE_ID_1, null);
    private final CaptureStage mCaptureStage2 = new FakeCaptureStage(CAPTURE_ID_2, null);
    private final CaptureStage mCaptureStage3 = new FakeCaptureStage(CAPTURE_ID_3, null);
    private final FakeImageReaderProxy mImageReaderProxy = new FakeImageReaderProxy(8);
    private CaptureBundle mCaptureBundle;

    @Before
    public void setUp() {
        mCaptureBundle = CaptureBundles.createCaptureBundle(mCaptureStage0, mCaptureStage1);
    }

    @Test
    public void canSetFuturesInSettableImageProxyBundle()
            throws InterruptedException, TimeoutException, ExecutionException {
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor captureProcessor = mock(CaptureProcessor.class);
        ProcessingImageReader processingImageReader = new ProcessingImageReader(
                mImageReaderProxy, AsyncTask.THREAD_POOL_EXECUTOR, mCaptureBundle,
                captureProcessor);
        processingImageReader.setOnImageAvailableListener(mock(
                ImageReaderProxy.OnImageAvailableListener.class),
                CameraXExecutors.mainThreadExecutor());
        Map<Integer, Long> resultMap = new HashMap<>();
        resultMap.put(CAPTURE_ID_0, TIMESTAMP_0);
        resultMap.put(CAPTURE_ID_1, TIMESTAMP_1);
        triggerAndVerify(captureProcessor, resultMap);
        Mockito.reset(captureProcessor);

        processingImageReader.setCaptureBundle(CaptureBundles.createCaptureBundle(mCaptureStage2,
                mCaptureStage3));
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

        // Ensure all posted tasks finish running
        ShadowLooper.runUiThreadTasks();

        ArgumentCaptor<ImageProxyBundle> imageProxyBundleCaptor =
                ArgumentCaptor.forClass(ImageProxyBundle.class);
        verify(captureProcessor, timeout(3000).times(1)).process(imageProxyBundleCaptor.capture());
        assertThat(imageProxyBundleCaptor.getValue()).isNotNull();

        // CaptureProcessor.process should be called once all ImageProxies on the
        // initial lists are ready. Then checks if the output has matched timestamp.
        for (Integer id : captureIdToTime.keySet()) {
            assertThat(imageProxyBundleCaptor.getValue().getImageProxy(id).get(0,
                    TimeUnit.SECONDS).getImageInfo().getTimestamp()).isEqualTo(
                    captureIdToTime.get(id));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void imageReaderSizeIsSmallerThanCaptureBundle() {
        // Creates a ProcessingImageReader with maximum Image number smaller than CaptureBundle
        // size.
        ImageReaderProxy imageReaderProxy = new FakeImageReaderProxy(1);

        // Expects to throw exception when creating ProcessingImageReader.
        new ProcessingImageReader(imageReaderProxy, AsyncTask.THREAD_POOL_EXECUTOR, mCaptureBundle,
                NOOP_PROCESSOR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void captureStageExceedMaxCaptureStage_setCaptureBundleThrowsException() {
        // Creates a ProcessingImageReader with maximum Image number.
        ProcessingImageReader processingImageReader = new ProcessingImageReader(100, 100,
                ImageFormat.YUV_420_888, 2, AsyncTask.THREAD_POOL_EXECUTOR, mCaptureBundle,
                mock(CaptureProcessor.class));

        // Expects to throw exception when invoke the setCaptureBundle method with a
        // CaptureBundle size greater than maximum image number.
        processingImageReader.setCaptureBundle(
                CaptureBundles.createCaptureBundle(mCaptureStage1, mCaptureStage2, mCaptureStage3));
    }

    private void triggerImageAvailable(int captureId, long timestamp) throws InterruptedException {
        mImageReaderProxy.triggerImageAvailable(captureId, timestamp);
    }

}
