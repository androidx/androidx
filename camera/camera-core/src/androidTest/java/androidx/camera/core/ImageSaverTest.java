/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageSaver.OnImageSavedListener;
import androidx.camera.core.ImageSaver.SaveError;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ImageSaverTest {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;
    private static final int CROP_WIDTH = 100;
    private static final int CROP_HEIGHT = 100;
    private static final int Y_PIXEL_STRIDE = 1;
    private static final int Y_ROW_STRIDE = WIDTH;
    private static final int UV_PIXEL_STRIDE = 1;
    private static final int UV_ROW_STRIDE = WIDTH / 2;
    private static final String JPEG_IMAGE_DATA_BASE_64 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB"
                    + "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEB"
                    + "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAB4AKADASIA"
                    + "AhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQA"
                    + "AAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3"
                    + "ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWm"
                    + "p6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEA"
                    + "AwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSEx"
                    + "BhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElK"
                    + "U1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3"
                    + "uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD/AD/6"
                    + "KKK/8/8AP/P/AAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
                    + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKA"
                    + "CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAK"
                    + "KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo"
                    + "ooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
                    + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooA//9k=";
    // The image used here has a YUV_420_888 format.
    @Mock
    private final ImageProxy mMockYuvImage = mock(ImageProxy.class);
    @Mock
    private final ImageProxy.PlaneProxy mYPlane = mock(ImageProxy.PlaneProxy.class);
    @Mock
    private final ImageProxy.PlaneProxy mUPlane = mock(ImageProxy.PlaneProxy.class);
    @Mock
    private final ImageProxy.PlaneProxy mVPlane = mock(ImageProxy.PlaneProxy.class);
    private final ByteBuffer mYBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT);
    private final ByteBuffer mUBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
    private final ByteBuffer mVBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
    @Mock
    private final ImageProxy mMockJpegImage = mock(ImageProxy.class);
    @Mock
    private final ImageProxy.PlaneProxy mJpegDataPlane = mock(ImageProxy.PlaneProxy.class);
    private final ByteBuffer mJpegDataBuffer =
            ByteBuffer.wrap(Base64.decode(JPEG_IMAGE_DATA_BASE_64, Base64.DEFAULT));

    private final Semaphore mSemaphore = new Semaphore(0);
    private final ImageSaver.OnImageSavedListener mMockListener =
            mock(ImageSaver.OnImageSavedListener.class);
    private final ImageSaver.OnImageSavedListener mSyncListener =
            new OnImageSavedListener() {
                @Override
                public void onImageSaved(File file) {
                    mMockListener.onImageSaved(file);
                    mSemaphore.release();
                }

                @Override
                public void onError(
                        SaveError saveError, String message, @Nullable Throwable cause) {
                    mMockListener.onError(saveError, message, cause);
                    mSemaphore.release();
                }
            };

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Before
    public void setup() {
        // The YUV image's behavior.
        when(mMockYuvImage.getFormat()).thenReturn(ImageFormat.YUV_420_888);
        when(mMockYuvImage.getWidth()).thenReturn(WIDTH);
        when(mMockYuvImage.getHeight()).thenReturn(HEIGHT);

        when(mYPlane.getBuffer()).thenReturn(mYBuffer);
        when(mYPlane.getPixelStride()).thenReturn(Y_PIXEL_STRIDE);
        when(mYPlane.getRowStride()).thenReturn(Y_ROW_STRIDE);

        when(mUPlane.getBuffer()).thenReturn(mUBuffer);
        when(mUPlane.getPixelStride()).thenReturn(UV_PIXEL_STRIDE);
        when(mUPlane.getRowStride()).thenReturn(UV_ROW_STRIDE);

        when(mVPlane.getBuffer()).thenReturn(mVBuffer);
        when(mVPlane.getPixelStride()).thenReturn(UV_PIXEL_STRIDE);
        when(mVPlane.getRowStride()).thenReturn(UV_ROW_STRIDE);
        when(mMockYuvImage.getPlanes())
                .thenReturn(new ImageProxy.PlaneProxy[]{mYPlane, mUPlane, mVPlane});
        when(mMockYuvImage.getCropRect()).thenReturn(new Rect(0, 0, CROP_WIDTH, CROP_HEIGHT));

        // The JPEG image's behavior
        when(mMockJpegImage.getFormat()).thenReturn(ImageFormat.JPEG);
        when(mMockJpegImage.getWidth()).thenReturn(WIDTH);
        when(mMockJpegImage.getHeight()).thenReturn(HEIGHT);
        when(mMockJpegImage.getCropRect()).thenReturn(new Rect(0, 0, CROP_WIDTH, CROP_HEIGHT));

        when(mJpegDataPlane.getBuffer()).thenReturn(mJpegDataBuffer);
        when(mMockJpegImage.getPlanes()).thenReturn(new ImageProxy.PlaneProxy[]{mJpegDataPlane});

        // Set up a background thread/handler for callbacks
        mBackgroundThread = new HandlerThread("CallbackThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @After
    public void tearDown() {
        mBackgroundThread.quitSafely();
    }

    private ImageSaver getDefaultImageSaver(ImageProxy image, File file) {
        return new ImageSaver(
                image,
                file,
                /*orientation=*/ 0,
                /*reversedHorizontal=*/ false,
                /*reversedVertical=*/ false,
                /*location=*/ null,
                mSyncListener,
                mBackgroundHandler);
    }

    @Test
    public void canSaveYuvImage() throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        ImageSaver imageSaver = getDefaultImageSaver(mMockYuvImage, saveLocation);

        imageSaver.run();

        mSemaphore.acquire();

        verify(mMockListener).onImageSaved(any(File.class));
    }

    @Test
    public void canSaveJpegImage() throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        ImageSaver imageSaver = getDefaultImageSaver(mMockJpegImage, saveLocation);

        imageSaver.run();

        mSemaphore.acquire();

        verify(mMockListener).onImageSaved(any(File.class));
    }

    @Test
    public void errorCallbackWillBeCalledOnInvalidPath() throws InterruptedException {
        // Invalid filename should cause error
        File saveLocation = new File("/not/a/real/path.jpg");

        ImageSaver imageSaver = getDefaultImageSaver(mMockJpegImage, saveLocation);

        imageSaver.run();

        mSemaphore.acquire();

        verify(mMockListener).onError(eq(SaveError.FILE_IO_FAILED), anyString(),
                any(Throwable.class));
    }

    @Test
    public void imageIsClosedOnSuccess() throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        ImageSaver imageSaver = getDefaultImageSaver(mMockJpegImage, saveLocation);

        imageSaver.run();

        mSemaphore.acquire();

        verify(mMockJpegImage).close();
    }

    @Test
    public void imageIsClosedOnError() throws InterruptedException, IOException {
        // Invalid filename should cause error
        File saveLocation = new File("/not/a/real/path.jpg");

        ImageSaver imageSaver = getDefaultImageSaver(mMockJpegImage, saveLocation);

        imageSaver.run();

        mSemaphore.acquire();

        verify(mMockJpegImage).close();
    }

    private void imageCanBeCropped(ImageProxy image) throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        ImageSaver imageSaver =
                new ImageSaver(
                        image,
                        saveLocation,
                        /*orientation=*/ 0,
                        /*reversedHorizontal=*/ false,
                        /*reversedVertical=*/ false,
                        /*location=*/ null,
                        mSyncListener,
                        mBackgroundHandler);
        imageSaver.run();

        mSemaphore.acquire();

        Bitmap bitmap = BitmapFactory.decodeFile(saveLocation.getPath());
        assertThat(bitmap.getWidth()).isEqualTo(bitmap.getHeight());
    }

    @Test
    public void jpegImageCanBeCropped() throws InterruptedException, IOException {
        imageCanBeCropped(mMockJpegImage);
    }

    @Test
    public void yuvImageCanBeCropped() throws InterruptedException, IOException {
        imageCanBeCropped(mMockYuvImage);
    }
}
