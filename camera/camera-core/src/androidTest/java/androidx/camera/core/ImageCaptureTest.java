/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.camera.testing.AndroidUtil.isEmulatorAndAPI21;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.provider.MediaStore;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeCameraCoordinator;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrument tests for {@link ImageCapture}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class ImageCaptureTest {
    private CameraCoordinator mCameraCoordinator;
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private Matrix mSensorToBufferTransformMatrix;

    @Before
    public void setup() {
        FakeCamera fakeCamera = new FakeCamera("fakeCameraId");

        FakeCameraDeviceSurfaceManager fakeCameraDeviceSurfaceManager =
                new FakeCameraDeviceSurfaceManager();
        fakeCameraDeviceSurfaceManager.setSuggestedStreamSpec("fakeCameraId",
                ImageCaptureConfig.class,
                StreamSpec.builder(new Size(640, 480)).build());

        UseCaseConfigFactory useCaseConfigFactory = new FakeUseCaseConfigFactory();
        mCameraCoordinator = new FakeCameraCoordinator();
        mCameraUseCaseAdapter = new CameraUseCaseAdapter(
                new LinkedHashSet<>(Collections.singleton(fakeCamera)),
                mCameraCoordinator,
                fakeCameraDeviceSurfaceManager,
                useCaseConfigFactory);

        mSensorToBufferTransformMatrix = new Matrix();
        mSensorToBufferTransformMatrix.setScale(10, 10);
    }

    @After
    public void tearDown() {
        if (mCameraUseCaseAdapter != null) {
            mInstrumentation.runOnMainSync(() -> {
                mCameraUseCaseAdapter.removeUseCases(mCameraUseCaseAdapter.getUseCases());
            });
        }
    }

    @Test
    public void onCaptureCancelled_onErrorCAMERA_CLOSED() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });

        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                getCameraControlImplementation(mCameraUseCaseAdapter.getCameraControl());

        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            // Notify the cancel after the capture request has been successfully submitted
            fakeCameraControl.notifyAllRequestOnCaptureCancelled();
        });

        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(), callback));

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(1000).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAMERA_CLOSED);
    }

    @Test
    public void onRequestFailed_OnErrorCAPTURE_FAILED() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });

        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                getCameraControlImplementation(mCameraUseCaseAdapter.getCameraControl());
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            // Notify the failure after the capture request has been successfully submitted
            fakeCameraControl.notifyAllRequestsOnCaptureFailed();
        });

        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(),
                        callback));


        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(1000).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAPTURE_FAILED);
    }

    @Test
    public void captureWithMinLatencyByImageCapturedCallback_jpegQualityIs95() {
        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageCapturedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, 95)).isTrue();
    }

    @Test
    public void captureWithMaxQualityByImageCapturedCallback_jpegQualityIs100() {
        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageCapturedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, 100)).isTrue();
    }

    @Test
    public void captureWithMinLatencyByImageCapturedCallback_jpegQualityOverwrittenBy100() {
        int jpegQuality = 100;
        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setJpegQuality(jpegQuality).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageCapturedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, jpegQuality)).isTrue();
    }

    @Test
    public void captureWithMaxQualityByImageCapturedCallback_jpegQualityOverwrittenBy1() {
        int jpegQuality = 1;
        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setJpegQuality(jpegQuality).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageCapturedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, jpegQuality)).isTrue();
    }

    @Test
    public void captureWithoutCropSettingByImageSavedCallback_jpegQualitySameAsSettingValue() {
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder().setJpegQuality(jpegQuality).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageSavedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, jpegQuality)).isTrue();
    }

    @Test
    public void captureWithCropAspectRatioByImageSavedCallbackAndMinLatencyMode_jpegQualityIs95() {
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(jpegQuality).build();
        imageCapture.setCropAspectRatio(new Rational(1, 1));
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageSavedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, 95)).isTrue();
    }

    @Test
    public void captureWithCropAspectRatioByImageSavedCallbackAndMaxQualityMode_jpegQualityIs100() {
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(jpegQuality).build();
        imageCapture.setCropAspectRatio(new Rational(1, 1));
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageSavedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, 100)).isTrue();
    }

    @Test
    public void captureWithCropAspectRatioByImageCapturedCallback_jpegQualitySameAsSettingValue() {
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder().setJpegQuality(jpegQuality).build();
        imageCapture.setCropAspectRatio(new Rational(1, 1));
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageCapturedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, jpegQuality)).isTrue();
    }

    @Test
    public void captureWithViewPortByImageSavedCallbackAndMinLatencyMode_jpegQualityIs95() {
        mCameraUseCaseAdapter.setViewPort(new ViewPort.Builder(new Rational(1, 1),
                Surface.ROTATION_0).build());
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(jpegQuality).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageSavedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, 95)).isTrue();
    }

    @Test
    public void captureWithViewPortByImageSavedCallbackAndMaxQualityMode_jpegQualityIs100() {
        mCameraUseCaseAdapter.setViewPort(new ViewPort.Builder(new Rational(1, 1),
                Surface.ROTATION_0).build());
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(jpegQuality).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageSavedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, 100)).isTrue();
    }

    @Test
    public void captureWithViewPortByImageCapturedCallback_jpegQualitySameAsSettingValue() {
        mCameraUseCaseAdapter.setViewPort(new ViewPort.Builder(new Rational(1, 1),
                Surface.ROTATION_0).build());
        int jpegQuality = 50;
        ImageCapture imageCapture = new ImageCapture.Builder().setJpegQuality(jpegQuality).build();
        List<CaptureConfig> captureConfigs = captureImage(imageCapture,
                ImageCapture.OnImageCapturedCallback.class);
        assertThat(hasJpegQuality(captureConfigs, jpegQuality)).isTrue();
    }

    private FakeCameraControl getCameraControlImplementation(CameraControl cameraControl) {
        CameraControlInternal impl = ((CameraControlInternal) cameraControl).getImplementation();
        return (FakeCameraControl) impl;
    }

    @NonNull
    private List<CaptureConfig> captureImage(@NonNull ImageCapture imageCapture,
            @NonNull Class<?> callbackClass) {
        // Arrange.
        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });

        ScheduledExecutorService repeatingScheduledExecutorService = null;

        // Sets repeating capture result to the imageCapture's session config repeating capture
        // callbacks to make ImageCapture#preTakePicture can be completed when capture mode is
        // set as CAPTURE_MODE_MAXIMIZE_QUALITY.
        if (imageCapture.getCaptureMode() == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) {
            FakeCameraCaptureResult fakeCameraCaptureResult = new FakeCameraCaptureResult();
            fakeCameraCaptureResult.setAfState(CameraCaptureMetaData.AfState.LOCKED_FOCUSED);
            fakeCameraCaptureResult.setAeState(CameraCaptureMetaData.AeState.CONVERGED);
            fakeCameraCaptureResult.setAwbState(CameraCaptureMetaData.AwbState.CONVERGED);

            repeatingScheduledExecutorService = Executors.newScheduledThreadPool(1);

            repeatingScheduledExecutorService.scheduleAtFixedRate(() -> {
                for (CameraCaptureCallback callback :
                        imageCapture.getSessionConfig().getRepeatingCameraCaptureCallbacks()) {
                    callback.onCaptureCompleted(fakeCameraCaptureResult);
                }
            }, 0, 50, TimeUnit.MILLISECONDS);
        }

        FakeCameraControl fakeCameraControl =
                getCameraControlImplementation(mCameraUseCaseAdapter.getCameraControl());
        FakeCameraControl.OnNewCaptureRequestListener mockCaptureRequestListener =
                mock(FakeCameraControl.OnNewCaptureRequestListener.class);
        fakeCameraControl.setOnNewCaptureRequestListener(mockCaptureRequestListener);

        // Act.
        mInstrumentation.runOnMainSync(
                () -> {
                    if (callbackClass == ImageCapture.OnImageCapturedCallback.class) {
                        imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(),
                                mock(ImageCapture.OnImageCapturedCallback.class));
                    } else if (callbackClass == ImageCapture.OnImageSavedCallback.class) {
                        ContentResolver contentResolver =
                                ApplicationProvider.getApplicationContext().getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(
                                        contentResolver,
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues).build(),
                                CameraXExecutors.mainThreadExecutor(),
                                mock(ImageCapture.OnImageSavedCallback.class));
                    } else {
                        throw new IllegalArgumentException("Unexpected callback type for taking "
                                + "picture!");
                    }
                });

        // Assert.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CaptureConfig>> argumentCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mockCaptureRequestListener,
                timeout(1000).times(1)).onNewCaptureRequests(argumentCaptor.capture());

        List<CaptureConfig> captureConfigs = argumentCaptor.getValue();
        if (repeatingScheduledExecutorService != null) {
            repeatingScheduledExecutorService.shutdown();
        }

        return captureConfigs;
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentException_setInvalidJpegQuality0() {
        new ImageCapture.Builder().setJpegQuality(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentException_setInvalidJpegQuality101() {
        new ImageCapture.Builder().setJpegQuality(101);
    }

    @Test
    public void dispatchImage_cropRectIsUpdatedBasedOnExifOrientation()
            throws InterruptedException, IOException {
        assumeFalse(isEmulatorAndAPI21());
        // Arrange: assume the sensor buffer is 6x4, the crop rect is (0, 0) - (2, 1) and the
        // rotation degrees is 90°.
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<ImageProxy> imageProxyReference = new AtomicReference<>();
        ImageCapture.ImageCaptureRequest request = new ImageCapture.ImageCaptureRequest(
                /*rotationDegrees*/90,
                /*jpegQuality*/100,
                /*targetRatio*/ null,
                /*viewPortCropRect*/ new Rect(0, 0, 2, 1),
                mSensorToBufferTransformMatrix,
                CameraXExecutors.mainThreadExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        imageProxyReference.set(image);
                        semaphore.release();
                        image.close();
                    }
                });

        // Act: dispatch a image that has been rotated in the HAL. After 90° rotation the buffer
        // becomes 4x6 and orientation is normal.
        request.dispatchImage(createJpegImageProxy(4, 6, ExifInterface.ORIENTATION_NORMAL));
        semaphore.tryAcquire(3, TimeUnit.SECONDS);

        // Assert: that the rotation is 0 and the crop rect has been updated.
        assertThat(imageProxyReference.get().getImageInfo().getRotationDegrees()).isEqualTo(0);
        assertThat(imageProxyReference.get().getCropRect()).isEqualTo(new Rect(3, 0, 4, 2));
        assertThat(imageProxyReference.get().getImageInfo()
                .getSensorToBufferTransformMatrix()).isEqualTo(mSensorToBufferTransformMatrix);
    }

    /**
     * Creates a {@link ImageProxy} with given width, height and exif orientation.
     *
     * @param exifOrientation orientation integers defined in {@link ExifInterface}.
     */
    private ImageProxy createJpegImageProxy(int width, int height,
            int exifOrientation) throws IOException {
        // Create a temporary jpeg file with given width/height.
        File jpegFile = File.createTempFile("fake_jpeg_with_exif", "jpeg",
                mInstrumentation.getContext().getCacheDir());
        jpegFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(jpegFile)) {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(
                    Bitmap.CompressFormat.JPEG, 100, out);
        }

        // Save the exif orientation to the jpeg file.
        ExifInterface exifInterface = new ExifInterface(jpegFile.getAbsolutePath());
        exifInterface.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                String.valueOf(exifOrientation));
        exifInterface.saveAttributes();

        // Load the jpeg file into a ByteBuffer.
        ByteBuffer byteData;
        try (FileInputStream inputStream = new FileInputStream(jpegFile)) {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            int read;
            while (true) {
                read = inputStream.read(buffer);
                if (read == -1) {
                    break;
                }
                outStream.write(buffer, 0, read);
            }
            byteData = ByteBuffer.wrap(outStream.toByteArray());
        }

        // Create a FakeImageProxy from the ByteBuffer.
        FakeImageProxy fakeImageProxy = new FakeImageProxy(new FakeImageInfo());
        fakeImageProxy.setFormat(ImageFormat.JPEG);
        ImageProxy.PlaneProxy planeProxy = new ImageProxy.PlaneProxy() {

            @Override
            public int getRowStride() {
                return 0;
            }

            @Override
            public int getPixelStride() {
                return 0;
            }

            @NonNull
            @Override
            public ByteBuffer getBuffer() {
                return byteData;
            }
        };
        fakeImageProxy.setPlanes(new ImageProxy.PlaneProxy[]{planeProxy});
        return fakeImageProxy;
    }

    @Test
    public void setFlashModeDuringPictureTaken() throws InterruptedException {
        // Arrange.
        ImageCapture imageCapture =
                new ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_OFF).build();

        mInstrumentation.runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(imageCapture));
            } catch (CameraUseCaseAdapter.CameraException ignore) {
            }
        });

        ImageCapture.OnImageCapturedCallback callback = mock(
                ImageCapture.OnImageCapturedCallback.class);
        FakeCameraControl fakeCameraControl =
                getCameraControlImplementation(mCameraUseCaseAdapter.getCameraControl());
        CountDownLatch latch = new CountDownLatch(1);
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            latch.countDown();
        });

        // Act.
        mInstrumentation.runOnMainSync(
                () -> imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(), callback));
        latch.await(3, TimeUnit.SECONDS);
        // Flash mode should not be changed during picture taken.
        imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);

        // Assert.
        assertThat(fakeCameraControl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_OFF);

        // Act.
        // Complete the picture taken, then new flash mode should be applied.
        fakeCameraControl.notifyAllRequestsOnCaptureFailed();

        // Assert.
        verify(callback, timeout(1000).times(1)).onError(any());
        assertThat(fakeCameraControl.getFlashMode()).isEqualTo(ImageCapture.FLASH_MODE_ON);
    }

    @Test
    public void correctViewPortRectInResolutionInfo_withCropAspectRatioSetting() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureOptionUnpacker((config, builder) -> {
                })
                .setSessionOptionUnpacker((resolution, config, builder) -> {
                }).build();
        imageCapture.setCropAspectRatio(new Rational(16, 9));

        mInstrumentation.runOnMainSync(() -> {
                    try {
                        mCameraUseCaseAdapter.addUseCases(Collections.singletonList(imageCapture));
                    } catch (CameraUseCaseAdapter.CameraException e) {
                    }
                }
        );

        ResolutionInfo resolutionInfo = imageCapture.getResolutionInfo();
        assertThat(resolutionInfo.getCropRect()).isEqualTo(new Rect(0, 60, 640, 420));
    }

    private boolean hasJpegQuality(List<CaptureConfig> captureConfigs, int jpegQuality) {
        for (CaptureConfig captureConfig : captureConfigs) {
            if (jpegQuality == captureConfig.getImplementationOptions().retrieveOption(
                    CaptureConfig.OPTION_JPEG_QUALITY)) {
                return true;
            }
        }
        return false;
    }
}
