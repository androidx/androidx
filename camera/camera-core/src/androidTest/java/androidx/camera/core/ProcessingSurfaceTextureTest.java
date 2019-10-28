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
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 23)
public final class ProcessingSurfaceTextureTest {

    private final Size mResolution = new Size(640, 480);
    private final int mFormat = ImageFormat.YUV_420_888;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CaptureStage mCaptureStage = new CaptureStage.DefaultCaptureStage();

    /*
     * Capture processor that simply writes out an empty image to exercise the pipeline
     * ImageWriter unable to dequeue Image that writes into SurfaceTexture since format is PRIVATE
     * on APIs prior to 28.
     */
    @RequiresApi(28)
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
                image.setTimestamp(imageProxy.getTimestamp());
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
        mBackgroundThread.getLooper().quit();
    }

    @Test
    public void validInputSurface() throws ExecutionException, InterruptedException {
        ProcessingSurfaceTexture processingSurfaceTexture = createProcessingSurfaceTexture();

        Surface surface = processingSurfaceTexture.getSurface().get();

        assertThat(surface).isNotNull();
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void writeToInputSurface_outputSurfaceReceivesFrame() throws ExecutionException,
            InterruptedException {
        // Arrange.
        ProcessingSurfaceTexture processingSurfaceTexture = createProcessingSurfaceTexture();
        SurfaceTexture outputSurface = processingSurfaceTexture.getSurfaceTexture();

        final Semaphore frameReceivedSemaphore = new Semaphore(0);
        outputSurface.setOnFrameAvailableListener(
                new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        frameReceivedSemaphore.release();
                    }
                },
                mBackgroundHandler);

        // Act: Send one frame to processingSurfaceTexture.
        triggerImage(processingSurfaceTexture, 1);

        // Assert: verify that the frame has been received or time-out after 1 second.
        assertThat(frameReceivedSemaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void writeToInputSurface_userOutputSurfaceReceivesFrame() throws ExecutionException,
            InterruptedException {
        // Arrange.
        // Create a user provided Surface.
        SurfaceTexture userSurfaceTexture = new SurfaceTexture(0);
        userSurfaceTexture.setDefaultBufferSize(mResolution.getWidth(), mResolution.getHeight());
        userSurfaceTexture.detachFromGLContext();
        ListenableFuture<Surface> surfaceFuture =
                Futures.immediateFuture(new Surface(userSurfaceTexture));

        // Create ProcessingSurfaceTexture with user Surface.
        ProcessingSurfaceTexture processingSurfaceTexture = createProcessingSurfaceTexture(
                createCallbackDeferrableSurface(mResolution,
                        CameraXExecutors.directExecutor(), surfaceFuture));
        final Semaphore frameReceivedSemaphore = new Semaphore(0);
        userSurfaceTexture.setOnFrameAvailableListener(
                surfaceTexture -> frameReceivedSemaphore.release(),
                mBackgroundHandler);

        // Act: Send one frame to processingSurfaceTexture.
        triggerImage(processingSurfaceTexture, 1);

        // Assert: verify that the frame has been received or time-out after 1 second.
        assertThat(frameReceivedSemaphore.tryAcquire(1, TimeUnit.SECONDS)).isTrue();
    }

    private ProcessingSurfaceTexture createProcessingSurfaceTexture() {
        return createProcessingSurfaceTexture(null);
    }

    private ProcessingSurfaceTexture createProcessingSurfaceTexture(
            CallbackDeferrableSurface callbackDeferrableSurface) {
        return new ProcessingSurfaceTexture(
                mResolution.getWidth(),
                mResolution.getHeight(),
                mFormat,
                mBackgroundHandler,
                mCaptureStage,
                mCaptureProcessor,
                callbackDeferrableSurface);
    }

    private CallbackDeferrableSurface createCallbackDeferrableSurface(Size resolution,
            Executor callbackExecutor, ListenableFuture<Surface> surfaceListenableFuture) {
        return new CallbackDeferrableSurface(resolution, callbackExecutor,
                new Preview.PreviewSurfaceCallback() {
                    @NonNull
                    @Override
                    public ListenableFuture<Surface> createSurfaceFuture(@NonNull Size resolution,
                            int imageFormat) {
                        return surfaceListenableFuture;
                    }

                    @Override
                    public void onSafeToRelease(@NonNull ListenableFuture<Surface> surfaceFuture) {
                        try {
                            surfaceFuture.get().release();
                        } catch (ExecutionException | InterruptedException e) {
                            // no-op
                        }
                    }
                });
    }

    @Test
    public void getSurfaceThrowsExceptionWhenClosed() {
        ProcessingSurfaceTexture processingSurfaceTexture = createProcessingSurfaceTexture();

        processingSurfaceTexture.close();

        // Exception should be thrown here
        ListenableFuture<Surface> futureSurface = processingSurfaceTexture.getSurface();

        Throwable cause = null;
        try {
            futureSurface.get();
        } catch (ExecutionException | InterruptedException e) {
            cause = e.getCause();
        }

        assertThat(cause).isInstanceOf(DeferrableSurface.SurfaceClosedException.class);

    }

    @Test(expected = IllegalStateException.class)
    public void getSurfaceTextureThrowsExceptionWhenReleased() {
        ProcessingSurfaceTexture processingSurfaceTexture = createProcessingSurfaceTexture();

        processingSurfaceTexture.release();

        // Exception should be thrown here
        processingSurfaceTexture.getSurfaceTexture();
    }

    @Test(expected = IllegalStateException.class)
    public void getCameraCaptureCallbackThrowsExceptionWhenReleased() {
        ProcessingSurfaceTexture processingSurfaceTexture = createProcessingSurfaceTexture();

        processingSurfaceTexture.release();

        // Exception should be thrown here
        processingSurfaceTexture.getCameraCaptureCallback();
    }

    @RequiresApi(28)
    void triggerImage(ProcessingSurfaceTexture processingSurfaceTexture, long timestamp)
            throws ExecutionException, InterruptedException {
        Surface surface = processingSurfaceTexture.getSurface().get();

        ImageWriter imageWriter = ImageWriter.newInstance(surface, 2);

        Image image = imageWriter.dequeueInputImage();
        image.setTimestamp(timestamp);
        imageWriter.queueInputImage(image);

        CameraCaptureCallback callback = processingSurfaceTexture.getCameraCaptureCallback();

        FakeCameraCaptureResult cameraCaptureResult = new FakeCameraCaptureResult();
        cameraCaptureResult.setTimestamp(timestamp);
        cameraCaptureResult.setTag(mCaptureStage.getId());
        callback.onCaptureCompleted(cameraCaptureResult);

    }
}
