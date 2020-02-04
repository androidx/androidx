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

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 23) // This test uses ImageWriter which is supported from api 23.
public final class ProcessingSurfaceTest {

    private static final Size RESOLUTION = new Size(640, 480);
    private static final int FORMAT = ImageFormat.YUV_420_888;
    private static final CallbackDeferrableSurface NO_OP_CALLBACK_DEFERRABLE_SURFACE =
            new CallbackDeferrableSurface(RESOLUTION, CameraXExecutors.directExecutor(),
                    new Preview.SurfaceProvider() {
                @NonNull
                @Override
                public ListenableFuture<Surface> provideSurface(@NonNull Size resolution,
                                @NonNull ListenableFuture<Void> surfaceReleaseFuture) {
                    ImageReaderProxy imageReaderProxy =
                            ImageReaderProxys.createIsolatedReader(
                                    resolution.getWidth(), resolution.getHeight(),
                                    ImageFormat.YUV_420_888, 2);
                    return Futures.immediateFuture(imageReaderProxy.getSurface());
                }
            });

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CaptureStage mCaptureStage = new CaptureStage.DefaultCaptureStage();
    private List<ProcessingSurface> mProcessingSurfaces = new ArrayList<>();

    /*
     * Capture processor that simply writes out an empty image to exercise the pipeline
     * ImageWriter unable to dequeue Image that writes into SurfaceTexture since format is PRIVATE
     * on APIs prior to 28.
     */
    @RequiresApi(23)
    private CaptureProcessor mCaptureProcessor = new CaptureProcessor() {
        ImageWriter mImageWriter;

        @Override
        public void onOutputSurface(Surface surface, int imageFormat) {
            mImageWriter = ImageWriter.newInstance(surface, 2);
        }

        @Override
        public void process(ImageProxyBundle bundle) {
            try {
                ListenableFuture<ImageProxy> imageProxyListenableFuture = bundle.getImageProxy(
                        mCaptureStage.getId());
                ImageProxy imageProxy = imageProxyListenableFuture.get(100, TimeUnit.MILLISECONDS);
                Image image = mImageWriter.dequeueInputImage();
                image.setTimestamp(imageProxy.getImageInfo().getTimestamp());
                mImageWriter.queueInputImage(image);
            } catch (ExecutionException | TimeoutException | InterruptedException e) {
            }
        }

        @Override
        public void onResolutionUpdate(Size size) {

        }
    };

    @Before
    public void setup() {
        mBackgroundThread = new HandlerThread("CallbackThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @After
    public void tearDown() {
        for (ProcessingSurface processingSurface : mProcessingSurfaces) {
            processingSurface.release();
        }
        mProcessingSurfaces.clear();
        mBackgroundThread.getLooper().quitSafely();
    }

    @Test
    public void validInputSurface() throws ExecutionException, InterruptedException {
        ProcessingSurface processingSurface = createProcessingSurface(
                NO_OP_CALLBACK_DEFERRABLE_SURFACE);

        Surface surface = processingSurface.getSurface().get();

        assertThat(surface).isNotNull();
    }

    @Test
    public void writeToInputSurface_userOutputSurfaceReceivesFrame() throws ExecutionException,
            InterruptedException {
        // Arrange.
        final Semaphore frameReceivedSemaphore = new Semaphore(0);

        // Create ProcessingSurface with user Surface.
        ProcessingSurface processingSurface = createProcessingSurface(
                new CallbackDeferrableSurface(RESOLUTION, CameraXExecutors.directExecutor(),
                        new Preview.SurfaceProvider() {
                            @NonNull
                            @Override
                            public ListenableFuture<Surface> provideSurface(
                                    @NonNull Size resolution,
                                    @NonNull ListenableFuture<Void> surfaceReleaseFuture) {
                                ImageReaderProxy imageReaderProxy =
                                        ImageReaderProxys.createIsolatedReader(
                                                resolution.getWidth(), resolution.getHeight(),
                                                ImageFormat.YUV_420_888, 2);

                                imageReaderProxy.setOnImageAvailableListener(
                                        new ImageReaderProxy.OnImageAvailableListener() {
                                            @Override
                                            public void onImageAvailable(
                                                    @NonNull ImageReaderProxy imageReader) {
                                                frameReceivedSemaphore.release();
                                            }
                                        }, CameraXExecutors.directExecutor());
                                return Futures.immediateFuture(imageReaderProxy.getSurface());
                            }
                        }));

        // Act: Send one frame to processingSurface.
        triggerImage(processingSurface, 1);

        // Assert: verify that the frame has been received or time-out after 3 second.
        assertThat(frameReceivedSemaphore.tryAcquire(3, TimeUnit.SECONDS)).isTrue();
    }

    private ProcessingSurface createProcessingSurface(
            CallbackDeferrableSurface callbackDeferrableSurface) {
        ProcessingSurface processingSurface = new ProcessingSurface(
                RESOLUTION.getWidth(),
                RESOLUTION.getHeight(),
                FORMAT,
                mBackgroundHandler,
                mCaptureStage,
                mCaptureProcessor,
                callbackDeferrableSurface);
        mProcessingSurfaces.add(processingSurface);
        return processingSurface;
    }

    @Test
    public void getSurfaceThrowsExceptionWhenClosed() {
        ProcessingSurface processingSurface =
                createProcessingSurface(NO_OP_CALLBACK_DEFERRABLE_SURFACE);

        processingSurface.close();

        // Exception should be thrown here
        ListenableFuture<Surface> futureSurface = processingSurface.getSurface();

        Throwable cause = null;
        try {
            futureSurface.get();
        } catch (ExecutionException | InterruptedException e) {
            cause = e.getCause();
        }

        assertThat(cause).isInstanceOf(DeferrableSurface.SurfaceClosedException.class);

    }

    @Test(expected = IllegalStateException.class)
    public void getCameraCaptureCallbackThrowsExceptionWhenReleased() {
        ProcessingSurface processingSurface =
                createProcessingSurface(NO_OP_CALLBACK_DEFERRABLE_SURFACE);

        processingSurface.release();

        // Exception should be thrown here
        processingSurface.getCameraCaptureCallback();
    }

    @RequiresApi(28)
    void triggerImage(ProcessingSurface processingSurface, long timestamp)
            throws ExecutionException, InterruptedException {
        Surface surface = processingSurface.getSurface().get();

        ImageWriter imageWriter = ImageWriter.newInstance(surface, 2);

        Image image = imageWriter.dequeueInputImage();
        image.setTimestamp(timestamp);
        imageWriter.queueInputImage(image);

        CameraCaptureCallback callback = processingSurface.getCameraCaptureCallback();

        FakeCameraCaptureResult cameraCaptureResult = new FakeCameraCaptureResult();
        cameraCaptureResult.setTimestamp(timestamp);
        cameraCaptureResult.setTag(mCaptureStage.getId());
        callback.onCaptureCompleted(cameraCaptureResult);

    }
}
