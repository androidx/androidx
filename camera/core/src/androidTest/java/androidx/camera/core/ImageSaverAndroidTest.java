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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.util.Rational;
import androidx.camera.core.ImageSaver.OnImageSavedListener;
import androidx.camera.core.ImageSaver.SaveError;
import androidx.test.runner.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(AndroidJUnit4.class)
public class ImageSaverAndroidTest {

  private static final int WIDTH = 160;
  private static final int HEIGHT = 120;
  private static final int Y_PIXEL_STRIDE = 1;
  private static final int Y_ROW_STRIDE = WIDTH;
  private static final int UV_PIXEL_STRIDE = 1;
  private static final int UV_ROW_STRIDE = WIDTH / 2;

  // The image used here has a YUV_420_888 format.
  @Mock private final ImageProxy mockYuvImage = mock(ImageProxy.class);
  @Mock private final ImageProxy.PlaneProxy yPlane = mock(ImageProxy.PlaneProxy.class);
  @Mock private final ImageProxy.PlaneProxy uPlane = mock(ImageProxy.PlaneProxy.class);
  @Mock private final ImageProxy.PlaneProxy vPlane = mock(ImageProxy.PlaneProxy.class);
  private final ByteBuffer yBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT);
  private final ByteBuffer uBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
  private final ByteBuffer vBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);

  @Mock private final ImageProxy mockJpegImage = mock(ImageProxy.class);
  @Mock private final ImageProxy.PlaneProxy jpegDataPlane = mock(ImageProxy.PlaneProxy.class);
  private final String jpegImageDataBase64 =
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
  private final ByteBuffer jpegDataBuffer =
      ByteBuffer.wrap(Base64.decode(jpegImageDataBase64, Base64.DEFAULT));

  private final Semaphore semaphore = new Semaphore(0);
  private final ImageSaver.OnImageSavedListener mockListener =
      mock(ImageSaver.OnImageSavedListener.class);
  private final ImageSaver.OnImageSavedListener syncListener =
      new OnImageSavedListener() {
        @Override
        public void onImageSaved(File file) {
          mockListener.onImageSaved(file);
          semaphore.release();
        }

        @Override
        public void onError(SaveError saveError, String message, @Nullable Throwable cause) {
          mockListener.onError(saveError, message, cause);
          semaphore.release();
        }
      };

  private HandlerThread backgroundThread;
  private Handler backgroundHandler;

  @Before
  public void setup() {
    // The YUV image's behavior.
    when(mockYuvImage.getFormat()).thenReturn(ImageFormat.YUV_420_888);
    when(mockYuvImage.getWidth()).thenReturn(WIDTH);
    when(mockYuvImage.getHeight()).thenReturn(HEIGHT);

    when(yPlane.getBuffer()).thenReturn(yBuffer);
    when(yPlane.getPixelStride()).thenReturn(Y_PIXEL_STRIDE);
    when(yPlane.getRowStride()).thenReturn(Y_ROW_STRIDE);

    when(uPlane.getBuffer()).thenReturn(uBuffer);
    when(uPlane.getPixelStride()).thenReturn(UV_PIXEL_STRIDE);
    when(uPlane.getRowStride()).thenReturn(UV_ROW_STRIDE);

    when(vPlane.getBuffer()).thenReturn(vBuffer);
    when(vPlane.getPixelStride()).thenReturn(UV_PIXEL_STRIDE);
    when(vPlane.getRowStride()).thenReturn(UV_ROW_STRIDE);
    when(mockYuvImage.getPlanes()).thenReturn(new ImageProxy.PlaneProxy[] {yPlane, uPlane, vPlane});

    // The JPEG image's behavior
    when(mockJpegImage.getFormat()).thenReturn(ImageFormat.JPEG);
    when(mockJpegImage.getWidth()).thenReturn(WIDTH);
    when(mockJpegImage.getHeight()).thenReturn(HEIGHT);

    when(jpegDataPlane.getBuffer()).thenReturn(jpegDataBuffer);
    when(mockJpegImage.getPlanes()).thenReturn(new ImageProxy.PlaneProxy[] {jpegDataPlane});

    // Set up a background thread/handler for callbacks
    backgroundThread = new HandlerThread("CallbackThread");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
  }

  @After
  public void tearDown() {
    backgroundThread.quitSafely();
  }

  private ImageSaver getDefaultImageSaver(ImageProxy image, File file) {
    return new ImageSaver(
        image,
        file,
        /*orientation=*/ 0,
        /*reversedHorizontal=*/ false,
        /*reversedVertical=*/ false,
        /*location=*/ null,
        /*cropAspectRatio=*/ null,
        syncListener,
        backgroundHandler);
  }

  @Test
  public void canSaveYuvImage() throws InterruptedException, IOException {
    File saveLocation = File.createTempFile("test", ".jpg");
    saveLocation.deleteOnExit();

    ImageSaver imageSaver = getDefaultImageSaver(mockYuvImage, saveLocation);

    imageSaver.run();

    semaphore.acquire();

    verify(mockListener).onImageSaved(anyObject());
  }

  @Test
  public void canSaveJpegImage() throws InterruptedException, IOException {
    File saveLocation = File.createTempFile("test", ".jpg");
    saveLocation.deleteOnExit();

    ImageSaver imageSaver = getDefaultImageSaver(mockJpegImage, saveLocation);

    imageSaver.run();

    semaphore.acquire();

    verify(mockListener).onImageSaved(anyObject());
  }

  @Test
  public void errorCallbackWillBeCalledOnInvalidPath() throws InterruptedException, IOException {
    // Invalid filename should cause error
    File saveLocation = new File("/not/a/real/path.jpg");

    ImageSaver imageSaver = getDefaultImageSaver(mockJpegImage, saveLocation);

    imageSaver.run();

    semaphore.acquire();

    verify(mockListener).onError(eq(SaveError.FILE_IO_FAILED), anyString(), anyObject());
  }

  @Test
  public void imageIsClosedOnSuccess() throws InterruptedException, IOException {
    File saveLocation = File.createTempFile("test", ".jpg");
    saveLocation.deleteOnExit();

    ImageSaver imageSaver = getDefaultImageSaver(mockJpegImage, saveLocation);

    imageSaver.run();

    semaphore.acquire();

    verify(mockJpegImage).close();
  }

  @Test
  public void imageIsClosedOnError() throws InterruptedException, IOException {
    // Invalid filename should cause error
    File saveLocation = new File("/not/a/real/path.jpg");

    ImageSaver imageSaver = getDefaultImageSaver(mockJpegImage, saveLocation);

    imageSaver.run();

    semaphore.acquire();

    verify(mockJpegImage).close();
  }

  private void imageCanBeCropped(ImageProxy image) throws InterruptedException, IOException {
    File saveLocation = File.createTempFile("test", ".jpg");
    saveLocation.deleteOnExit();

    Rational viewRatio = new Rational(1, 1);

    ImageSaver imageSaver = new ImageSaver(
        image,
        saveLocation,
        /*orientation=*/ 0,
        /*reversedHorizontal=*/ false,
        /*reversedVertical=*/ false,
        /*location=*/ null,
        /*cropAspectRatio=*/ viewRatio,
        syncListener,
        backgroundHandler
    );
    imageSaver.run();

    semaphore.acquire();

    Bitmap bitmap = BitmapFactory.decodeFile(saveLocation.getPath());
    assertThat(bitmap.getWidth()).isEqualTo(bitmap.getHeight());
  }

  @Test
  public void jpegImageCanBeCropped() throws InterruptedException, IOException {
    imageCanBeCropped(mockJpegImage);
  }

  @Test
  public void yuvImageCanBeCropped() throws InterruptedException, IOException {
    imageCanBeCropped(mockYuvImage);
  }
}
