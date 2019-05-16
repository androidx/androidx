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
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class ProcessingImageReaderTest {
    private static final int CAPTURE_ID_0 = 0;
    private static final int CAPTURE_ID_1 = 1;
    private static final long TIMESTAMP_0 = 0L;
    private static final long TIMESTAMP_1 = 1000L;
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
    private final FakeImageReaderProxy mImageReaderProxy = new FakeImageReaderProxy();
    private CaptureBundle mCaptureBundle;
    private Handler mMainHandler;

    @Before
    public void setUp() {
        mMainHandler = new Handler(Looper.getMainLooper());
        mCaptureBundle = CaptureBundles.createCaptureBundle(mCaptureStage0, mCaptureStage1);
    }

    @Test
    public void canSetFuturesInSettableImageProxyBundle()
            throws InterruptedException, TimeoutException, ExecutionException {
        final AtomicReference<ImageProxyBundle> bundleRef = new AtomicReference<>();
        // Sets the callback from ProcessingImageReader to start processing
        CaptureProcessor captureProcessor = new CaptureProcessor() {
            @Override
            public void onOutputSurface(Surface surface, int imageFormat) {

            }

            @Override
            public void process(ImageProxyBundle bundle) {
                bundleRef.set(bundle);
            }

            @Override
            public void onResolutionUpdate(Size size) {

            }
        };
        new ProcessingImageReader(
                mImageReaderProxy, mMainHandler, mCaptureBundle, captureProcessor);

        // Feeds ImageProxy with all capture id on the initial list.
        triggerImageAvailable(CAPTURE_ID_0, TIMESTAMP_0);
        triggerImageAvailable(CAPTURE_ID_1, TIMESTAMP_1);

        // Ensure all posted tasks finish running
        ShadowLooper.runUiThreadTasks();

        ImageProxyBundle bundle = bundleRef.get();
        assertThat(bundle).isNotNull();

        // CaptureProcessor.process should be called once all ImageProxies on the
        // initial lists are ready. Then checks if the output has matched timestamp.
        assertThat(bundle.getImageProxy(CAPTURE_ID_0).get(0,
                TimeUnit.SECONDS).getTimestamp()).isEqualTo(TIMESTAMP_0);
        assertThat(bundle.getImageProxy(CAPTURE_ID_1).get(0,
                TimeUnit.SECONDS).getTimestamp()).isEqualTo(TIMESTAMP_1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void imageReaderSizeIsSmallerThanCaptureBundle() {
        ImageReaderProxy imageReaderProxy = new FakeImageReaderProxy();

        // Creates a ProcessingImageReader with maximum Image number smaller than CaptureBundle
        // size.
        ((FakeImageReaderProxy) imageReaderProxy).setMaxImages(1);

        // Expects to throw exception when creating ProcessingImageReader.
        new ProcessingImageReader(imageReaderProxy, mMainHandler,
                mCaptureBundle, NOOP_PROCESSOR);
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
