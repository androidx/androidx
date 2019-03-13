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

import static java.lang.Thread.sleep;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeImageInfo;
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
public final class ProcessingImageReaderTest {
    private static final int CAPTURE_ID_0 = 0;
    private static final int CAPTURE_ID_1 = 1;
    private static final long TIMESTAMP_0 = 0L;
    private static final long TIMESTAMP_1 = 1000L;
    private final CaptureStage mCaptureStage0 = new FakeCaptureStage(CAPTURE_ID_0, null);
    private final CaptureStage mCaptureStage1 = new FakeCaptureStage(CAPTURE_ID_1, null);
    private final FakeImageReaderProxy mImageReaderProxy = new FakeImageReaderProxy();
    private final CaptureBundle mCaptureBundle = new CaptureBundle();
    private final CaptureProcessor mCaptureProcessor = new CaptureProcessor() {
        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {

        }

        @Override
        public void process(ImageProxyBundle bundle) {

        }
    };
    private final Semaphore mSemaphore = new Semaphore(0);
    private ProcessingImageReader mProcessingImageReader;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Before
    public void setUp() {
        mBackgroundThread = new HandlerThread("CallbackThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Test
    public void canSetFuturesInSettableImageProxyBundle() throws InterruptedException {
        mCaptureBundle.addCaptureStage(mCaptureStage0);
        mCaptureBundle.addCaptureStage(mCaptureStage1);
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor captureProcessor = new CaptureProcessor() {
            @Override
            public void onOutputSurface(Surface surface, int imageFormat) {

            }

            @Override
            public void process(ImageProxyBundle bundle) {
                try {
                    // CaptureProcessor.process should be called once all ImageProxies on the
                    // initial lists are ready. Then checks if the output has matched timestamp.
                    assertThat(bundle.getImageProxy(CAPTURE_ID_0).get(0,
                            TimeUnit.SECONDS).getTimestamp()).isEqualTo(TIMESTAMP_0);
                    assertThat(bundle.getImageProxy(CAPTURE_ID_1).get(0,
                            TimeUnit.SECONDS).getTimestamp()).isEqualTo(TIMESTAMP_1);
                } catch (Exception e) {

                }
                mSemaphore.release();
            }
        };
        mProcessingImageReader = new ProcessingImageReader(
                mImageReaderProxy, mBackgroundHandler, mCaptureBundle, captureProcessor);

        // Feeds ImageProxy with all capture id on the initial list.
        triggerImageAvailable(CAPTURE_ID_0, TIMESTAMP_0);
        sleep(500);
        triggerImageAvailable(CAPTURE_ID_1, TIMESTAMP_1);

        mSemaphore.acquire();
    }

    @Test(expected = IllegalArgumentException.class)
    public void imageReaderSizeIsSmallerThanCaptureBundle() {
        ImageReaderProxy imageReaderProxy = new FakeImageReaderProxy();

        // Creates a ProcessingImageReader with maximum Image number smaller than CaptureBundle
        // size.
        ((FakeImageReaderProxy) imageReaderProxy).setMaxImages(1);
        mCaptureBundle.addCaptureStage(mCaptureStage0);
        mCaptureBundle.addCaptureStage(mCaptureStage1);

        // Expects to throw exception when creating ProcessingImageReader.
        mProcessingImageReader = new ProcessingImageReader(imageReaderProxy, mBackgroundHandler,
                mCaptureBundle, mCaptureProcessor);
    }

    private void triggerImageAvailable(int captureId, long timestamp) {
        FakeImageProxy image = new FakeImageProxy();
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTag(captureId);
        imageInfo.setTimestamp(timestamp);
        image.setImageInfo(imageInfo);
        image.setTimestamp(timestamp);
        mImageReaderProxy.setImageProxy(image);
        mImageReaderProxy.triggerImageAvailable();
    }

}
