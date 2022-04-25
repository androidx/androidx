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

import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageSaver.OnImageSavedCallback;
import androidx.camera.core.ImageSaver.SaveError;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Instrument tests for {@link ImageSaver}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class ImageSaverTest {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;
    private static final int CROP_WIDTH = 100;
    private static final int CROP_HEIGHT = 100;
    private static final int Y_PIXEL_STRIDE = 1;
    private static final int Y_ROW_STRIDE = WIDTH;
    private static final int UV_PIXEL_STRIDE = 1;
    private static final int UV_ROW_STRIDE = WIDTH / 2;
    private static final int DEFAULT_JPEG_QUALITY = 100;
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

    private static final String TAG = "ImageSaverTest";
    private static final String INVALID_DATA_PATH = "/invalid_path";

    private static final String TAG_TO_IGNORE = ExifInterface.TAG_COMPRESSION;
    private static final String TAG_TO_IGNORE_VALUE = "6";
    private static final String TAG_TO_COPY = ExifInterface.TAG_MAKE;
    private static final String TAG_TO_COPY_VALUE = "make";

    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

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
    private ByteBuffer mJpegDataBuffer;

    private final Semaphore mSemaphore = new Semaphore(0);
    private final ImageSaver.OnImageSavedCallback mMockCallback =
            mock(ImageSaver.OnImageSavedCallback.class);
    private final ImageSaver.OnImageSavedCallback mSyncCallback =
            new OnImageSavedCallback() {
                @Override
                public void onImageSaved(
                        @NonNull ImageCapture.OutputFileResults outputFileResults) {
                    mMockCallback.onImageSaved(outputFileResults);
                    mSemaphore.release();
                }

                @Override
                public void onError(@NonNull SaveError saveError, @NonNull String message,
                        @Nullable Throwable cause) {
                    Logger.d(TAG, message, cause);
                    mMockCallback.onError(saveError, message, cause);
                    mSemaphore.release();
                }
            };

    private ExecutorService mBackgroundExecutor;
    private ContentResolver mContentResolver;

    @Before
    public void setup() throws IOException {
        assumeFalse("Skip for Cuttlefish.", Build.MODEL.contains("Cuttlefish"));
        createDefaultPictureFolderIfNotExist();
        mJpegDataBuffer = createJpegBufferWithExif();
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

        // Set up a background executor for callbacks
        mBackgroundExecutor = Executors.newSingleThreadExecutor();

        mContentResolver = ApplicationProvider.getApplicationContext().getContentResolver();
    }

    @After
    public void tearDown() {
        if (mBackgroundExecutor != null) {
            mBackgroundExecutor.shutdown();
        }
    }

    private ByteBuffer createJpegBufferWithExif() throws IOException {
        // Create a jpeg file with the test data.
        File tempFile = File.createTempFile("jpeg_with_exif", ".jpg");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(Base64.decode(JPEG_IMAGE_DATA_BASE_64, Base64.DEFAULT));
        }

        // Add exif tag to the jpeg file and save.
        ExifInterface saveExif = new ExifInterface(tempFile.toString());
        saveExif.setAttribute(TAG_TO_IGNORE, TAG_TO_IGNORE_VALUE);
        saveExif.setAttribute(TAG_TO_COPY, TAG_TO_COPY_VALUE);
        saveExif.saveAttributes();

        // Verify that the tags are saved correctly.
        ExifInterface verifyExif = new ExifInterface(tempFile.getPath());
        assertThat(verifyExif.getAttribute(TAG_TO_IGNORE)).isEqualTo(TAG_TO_IGNORE_VALUE);
        assertThat(verifyExif.getAttribute(TAG_TO_COPY)).isEqualTo(TAG_TO_COPY_VALUE);

        // Read the jpeg file and return it as a ByteBuffer.
        byte[] buffer = new byte[1024];
        try (FileInputStream in = new FileInputStream(tempFile);
             ByteArrayOutputStream out = new ByteArrayOutputStream(1024)) {
            int read;
            while (true) {
                read = in.read(buffer);
                if (read == -1) break;
                out.write(buffer, 0, read);
            }
            return ByteBuffer.wrap(out.toByteArray());
        }
    }

    @SuppressWarnings("deprecation")
    private void createDefaultPictureFolderIfNotExist() {
        File pictureFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!pictureFolder.exists()) {
            pictureFolder.mkdir();
        }
    }

    private ImageSaver getDefaultImageSaver(ImageProxy image, File file) {
        return getDefaultImageSaver(image,
                new ImageCapture.OutputFileOptions.Builder(file).build());
    }

    private ImageSaver getDefaultImageSaver(@NonNull ImageProxy image) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        return getDefaultImageSaver(image,
                new ImageCapture.OutputFileOptions.Builder(mContentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build());
    }

    private ImageSaver getDefaultImageSaver(ImageProxy image, OutputStream outputStream) {
        return getDefaultImageSaver(image,
                new ImageCapture.OutputFileOptions.Builder(outputStream).build());
    }

    private ImageSaver getDefaultImageSaver(ImageProxy image,
            ImageCapture.OutputFileOptions outputFileOptions) {
        return new ImageSaver(
                image,
                outputFileOptions,
                /*orientation=*/ 0,
                DEFAULT_JPEG_QUALITY,
                mBackgroundExecutor,
                mBackgroundExecutor,
                mSyncCallback);
    }

    @Test
    public void savedImage_exifIsCopiedToCroppedImage() throws IOException, InterruptedException {
        // Arrange.
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        // Act.
        getDefaultImageSaver(mMockJpegImage, saveLocation).run();
        mSemaphore.acquire();
        verify(mMockCallback).onImageSaved(any());

        // Assert.
        ExifInterface exifInterface = new ExifInterface(saveLocation.getPath());
        assertThat(exifInterface.getAttribute(TAG_TO_IGNORE)).isNotEqualTo(TAG_TO_IGNORE_VALUE);
        assertThat(exifInterface.getAttribute(TAG_TO_COPY)).isEqualTo(TAG_TO_COPY_VALUE);
    }

    @Test
    public void canSaveYuvImage_withNonExistingFile() throws InterruptedException {
        File saveLocation = new File(ApplicationProvider.getApplicationContext().getCacheDir(),
                "test" + System.currentTimeMillis() + ".jpg");
        saveLocation.deleteOnExit();
        // make sure file does not exist
        if (saveLocation.exists()) {
            saveLocation.delete();
        }
        assertThat(!saveLocation.exists());

        getDefaultImageSaver(mMockYuvImage, saveLocation).run();
        mSemaphore.acquire();

        verify(mMockCallback).onImageSaved(any());
    }

    @Test
    public void canSaveYuvImage_withExistingFile() throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        assertThat(saveLocation.exists());

        getDefaultImageSaver(mMockYuvImage, saveLocation).run();
        mSemaphore.acquire();

        verify(mMockCallback).onImageSaved(any());
    }

    @Test
    public void saveToUri() throws InterruptedException, FileNotFoundException {
        // Act.
        getDefaultImageSaver(mMockYuvImage).run();
        mSemaphore.acquire();

        // Assert.
        // Verify success callback is called.
        ArgumentCaptor<ImageCapture.OutputFileResults> outputFileResultsArgumentCaptor =
                ArgumentCaptor.forClass(ImageCapture.OutputFileResults.class);
        verify(mMockCallback).onImageSaved(outputFileResultsArgumentCaptor.capture());

        // Verify save location Uri is available.
        Uri saveLocationUri = outputFileResultsArgumentCaptor.getValue().getSavedUri();
        assertThat(saveLocationUri).isNotNull();

        // Loads image and verify width and height.
        ParcelFileDescriptor pfd = mContentResolver.openFileDescriptor(saveLocationUri, "r");
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
        assertThat(bitmap.getWidth()).isEqualTo(CROP_WIDTH);
        assertThat(bitmap.getHeight()).isEqualTo(CROP_HEIGHT);

        // Clean up.
        mContentResolver.delete(saveLocationUri, null, null);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void saveToUriWithEmptyCollection_onErrorCalled() throws InterruptedException {
        // Arrange.
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.DATA, INVALID_DATA_PATH);
        ImageSaver imageSaver = getDefaultImageSaver(mMockYuvImage,
                new ImageCapture.OutputFileOptions.Builder(mContentResolver,
                        Uri.EMPTY,
                        contentValues).build());

        // Act.
        imageSaver.run();
        mSemaphore.acquire();

        // Assert.
        verify(mMockCallback).onError(eq(SaveError.FILE_IO_FAILED), any(), any());
    }

    @Test
    public void saveToOutputStream() throws InterruptedException, IOException {
        // Arrange.
        File file = File.createTempFile("test", ".jpg");
        file.deleteOnExit();

        // Act.
        try (OutputStream outputStream = new FileOutputStream(file)) {
            getDefaultImageSaver(mMockYuvImage, outputStream).run();
            mSemaphore.acquire();
        }

        // Assert.
        verify(mMockCallback).onImageSaved(any());
        // Loads image and verify width and height.
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        assertThat(bitmap.getWidth()).isEqualTo(CROP_WIDTH);
        assertThat(bitmap.getHeight()).isEqualTo(CROP_HEIGHT);
    }

    @Test
    public void saveToClosedOutputStream_onErrorCalled() throws InterruptedException,
            IOException {
        // Arrange.
        File file = File.createTempFile("test", ".jpg");
        file.deleteOnExit();
        OutputStream outputStream = new FileOutputStream(file);
        outputStream.close();

        // Act.
        getDefaultImageSaver(mMockYuvImage, outputStream).run();
        mSemaphore.acquire();

        // Assert.
        verify(mMockCallback).onError(eq(SaveError.FILE_IO_FAILED), anyString(),
                any(Throwable.class));
    }

    @Test
    public void canSaveJpegImage() throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        getDefaultImageSaver(mMockJpegImage, saveLocation).run();
        mSemaphore.acquire();

        verify(mMockCallback).onImageSaved(any());
    }

    @Test
    public void saveToFile_uriIsSet() throws InterruptedException, IOException {
        // Arrange.
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        // Act.
        getDefaultImageSaver(mMockJpegImage, saveLocation).run();
        mSemaphore.acquire();

        // Assert.
        ArgumentCaptor<ImageCapture.OutputFileResults> argumentCaptor =
                ArgumentCaptor.forClass(ImageCapture.OutputFileResults.class);
        verify(mMockCallback).onImageSaved(argumentCaptor.capture());
        String savedPath = Objects.requireNonNull(
                argumentCaptor.getValue().getSavedUri()).getPath();
        assertThat(savedPath).isEqualTo(saveLocation.getPath());
    }

    @Test
    public void errorCallbackWillBeCalledOnInvalidPath() throws InterruptedException {
        // Invalid filename should cause error
        File saveLocation = new File("/not/a/real/path.jpg");

        getDefaultImageSaver(mMockJpegImage, saveLocation).run();
        mSemaphore.acquire();

        verify(mMockCallback).onError(eq(SaveError.FILE_IO_FAILED), anyString(),
                any(Throwable.class));
    }

    @Test
    public void imageIsClosedOnSuccess() throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        getDefaultImageSaver(mMockJpegImage, saveLocation).run();

        mSemaphore.acquire();

        verify(mMockJpegImage).close();
    }

    @Test
    public void imageIsClosedOnError() throws InterruptedException {
        // Invalid filename should cause error
        File saveLocation = new File("/not/a/real/path.jpg");

        getDefaultImageSaver(mMockJpegImage, saveLocation).run();
        mSemaphore.acquire();

        verify(mMockJpegImage).close();
    }

    private void imageCanBeCropped(ImageProxy image) throws InterruptedException, IOException {
        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();

        getDefaultImageSaver(image, saveLocation).run();
        mSemaphore.acquire();

        Bitmap bitmap = BitmapFactory.decodeFile(saveLocation.getPath());
        assertThat(bitmap.getWidth()).isEqualTo(CROP_WIDTH);
        assertThat(bitmap.getHeight()).isEqualTo(CROP_HEIGHT);
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
