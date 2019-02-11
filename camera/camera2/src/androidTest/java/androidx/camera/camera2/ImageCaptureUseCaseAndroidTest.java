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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.camera.core.AppConfiguration;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraUtil;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.Exif;
import androidx.camera.core.FakeUseCaseConfiguration;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCase.Metadata;
import androidx.camera.core.ImageCaptureUseCase.OnImageCapturedListener;
import androidx.camera.core.ImageCaptureUseCase.OnImageSavedListener;
import androidx.camera.core.ImageCaptureUseCase.UseCaseError;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.ImageProxy;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

@RunWith(AndroidJUnit4.class)
public final class ImageCaptureUseCaseAndroidTest {
    private static final Size DEFAULT_RESOLUTION = new Size(1920, 1080);
    private static final LensFacing BACK_LENS_FACING = LensFacing.BACK;

    private HandlerThread handlerThread;
    private Handler handler;
    private BaseCamera camera;
    private ImageCaptureUseCaseConfiguration defaultConfiguration;
    private ImageCaptureUseCase.OnImageCapturedListener onImageCapturedListener;
    private ImageCaptureUseCase.OnImageCapturedListener mockImageCapturedListener;
    private ImageCaptureUseCase.OnImageSavedListener onImageSavedListener;
    private ImageCaptureUseCase.OnImageSavedListener mockImageSavedListener;
    private ImageProxy capturedImage;
    private Semaphore semaphore;
    private FakeRepeatingUseCase repeatingUseCase;
    private FakeUseCaseConfiguration fakeRepeatingConfiguration;
    private String cameraId;

    private ImageCaptureUseCaseConfiguration createNonRotatedConfiguration()
            throws CameraInfoUnavailableException {
        // Create a configuration with target rotation that matches the sensor rotation.
        // This assumes a back-facing camera (facing away from screen)
        String backCameraId = CameraX.getCameraWithLensFacing(BACK_LENS_FACING);
        int sensorRotation = CameraX.getCameraInfo(backCameraId).getSensorRotationDegrees();

        int surfaceRotation = Surface.ROTATION_0;
        switch (sensorRotation) {
            case 0:
                surfaceRotation = Surface.ROTATION_0;
                break;
            case 90:
                surfaceRotation = Surface.ROTATION_90;
                break;
            case 180:
                surfaceRotation = Surface.ROTATION_180;
                break;
            case 270:
                surfaceRotation = Surface.ROTATION_270;
                break;
            default:
                throw new IllegalStateException("Invalid sensor rotation: " + sensorRotation);
        }

        return new ImageCaptureUseCaseConfiguration.Builder()
                .setLensFacing(BACK_LENS_FACING)
                .setTargetRotation(surfaceRotation)
                .build();
    }

    @Before
    public void setUp() {
        handlerThread = new HandlerThread("CaptureThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        Context context = ApplicationProvider.getApplicationContext();
        AppConfiguration appConfig = Camera2AppConfiguration.create(context);
        CameraFactory cameraFactory = appConfig.getCameraFactory(null);
        CameraX.init(context, appConfig);
        try {
            cameraId = cameraFactory.cameraIdForLensFacing(BACK_LENS_FACING);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + BACK_LENS_FACING, e);
        }
        defaultConfiguration = new ImageCaptureUseCaseConfiguration.Builder().build();

        camera = cameraFactory.getCamera(cameraId);
        capturedImage = null;
        semaphore = new Semaphore(/*permits=*/ 0);
        onImageCapturedListener =
                new OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        capturedImage = image;
                        // Signal that the image has been captured.
                        semaphore.release();
                    }
                };
        mockImageCapturedListener = Mockito.mock(OnImageCapturedListener.class);
        mockImageSavedListener = Mockito.mock(OnImageSavedListener.class);
        onImageSavedListener =
                new OnImageSavedListener() {
                    @Override
                    public void onImageSaved(File file) {
                        mockImageSavedListener.onImageSaved(file);
                        // Signal that an image was saved
                        semaphore.release();
                    }

                    @Override
                    public void onError(
                            UseCaseError error, String message, @Nullable Throwable cause) {
                        mockImageSavedListener.onError(error, message, cause);
                        // Signal that there was an error
                        semaphore.release();
                    }
                };

        fakeRepeatingConfiguration = new FakeUseCaseConfiguration.Builder().build();
        repeatingUseCase = new FakeRepeatingUseCase(fakeRepeatingConfiguration);
    }

    @After
    public void tearDown() {
        handlerThread.quitSafely();
        camera.release();
        if (capturedImage != null) {
            capturedImage.close();
        }
    }

    @Test
    public void capturedImageHasCorrectProperties() throws InterruptedException {
        ImageCaptureUseCaseConfiguration configuration =
                new ImageCaptureUseCaseConfiguration.Builder().setCallbackHandler(handler).build();
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(configuration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        useCase.takePicture(onImageCapturedListener);
        // Wait for the signal that the image has been captured.
        semaphore.acquire();

        assertThat(new Size(capturedImage.getWidth(), capturedImage.getHeight()))
                .isEqualTo(DEFAULT_RESOLUTION);
        assertThat(capturedImage.getFormat()).isEqualTo(useCase.getImageFormat());
    }

    @Test(timeout = 5000)
    public void canCaptureMultipleImages() throws InterruptedException {
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(
                    new ImageCaptureUseCase.OnImageCapturedListener() {
                        @Override
                        public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                            mockImageCapturedListener.onCaptureSuccess(image, rotationDegrees);
                            image.close();

                            // Signal that an image has been captured.
                            semaphore.release();
                        }
                    });
        }

        // Wait for the signal that all images have been captured.
        semaphore.acquire(numImages);

        verify(mockImageCapturedListener, times(numImages)).onCaptureSuccess(any(), anyInt());
    }

    @Test(timeout = 10000)
    public void canCaptureMultipleImagesWithMaxQuality() throws InterruptedException {
        ImageCaptureUseCaseConfiguration configuration =
                new ImageCaptureUseCaseConfiguration.Builder()
                        .setCaptureMode(ImageCaptureUseCase.CaptureMode.MAX_QUALITY)
                        .build();
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(configuration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(
                    new ImageCaptureUseCase.OnImageCapturedListener() {
                        @Override
                        public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                            mockImageCapturedListener.onCaptureSuccess(image, rotationDegrees);
                            image.close();

                            // Signal that an image has been captured.
                            semaphore.release();
                        }
                    });
        }

        // Wait for the signal that all images have been captured.
        semaphore.acquire(numImages);

        verify(mockImageCapturedListener, times(numImages)).onCaptureSuccess(any(), anyInt());
    }

    @Test(timeout = 5000)
    public void saveCanSucceed() throws InterruptedException, IOException {
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        useCase.takePicture(saveLocation, onImageSavedListener);

        // Wait for the signal that the image has been saved.
        semaphore.acquire();

        verify(mockImageSavedListener).onImageSaved(eq(saveLocation));
    }

    @Test(timeout = 5000)
    public void canSaveFile_withRotation()
            throws InterruptedException, IOException, CameraInfoUnavailableException {
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        useCase.takePicture(saveLocation, onImageSavedListener, metadata);

        // Wait for the signal that the image has been saved.
        semaphore.acquire();

        // Retrieve the sensor orientation
        int rotationDegrees = CameraX.getCameraInfo(cameraId).getSensorRotationDegrees();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getRotation()).isEqualTo(rotationDegrees);
    }

    @Test(timeout = 5000)
    public void canSaveFile_flippedHorizontal()
            throws InterruptedException, IOException, CameraInfoUnavailableException {
        // Use a non-rotated configuration since some combinations of rotation + flipping vertically
        // can
        // be equivalent to flipping horizontally
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(createNonRotatedConfiguration());
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.isReversedHorizontal = true;
        useCase.takePicture(saveLocation, onImageSavedListener, metadata);

        // Wait for the signal that the image has been saved.
        semaphore.acquire();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedHorizontally()).isTrue();
    }

    @Test(timeout = 5000)
    public void canSaveFile_flippedVertical()
            throws InterruptedException, IOException, CameraInfoUnavailableException {
        // Use a non-rotated configuration since some combinations of rotation + flipping
        // horizontally
        // can be equivalent to flipping vertically
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(createNonRotatedConfiguration());
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.isReversedVertical = true;
        useCase.takePicture(saveLocation, onImageSavedListener, metadata);

        // Wait for the signal that the image has been saved.
        semaphore.acquire();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedVertically()).isTrue();
    }

    @Test(timeout = 5000)
    public void canSaveFile_withAttachedLocation() throws InterruptedException, IOException {
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Location location = new Location("ImageCaptureUseCaseAndroidTest");
        Metadata metadata = new Metadata();
        metadata.location = location;
        useCase.takePicture(saveLocation, onImageSavedListener, metadata);

        // Wait for the signal that the image has been saved.
        semaphore.acquire();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getLocation().getProvider()).isEqualTo(location.getProvider());
    }

    @Test(timeout = 5000)
    public void canSaveMultipleFiles() throws InterruptedException, IOException {
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            File saveLocation = File.createTempFile("test" + i, ".jpg");
            saveLocation.deleteOnExit();

            useCase.takePicture(saveLocation, onImageSavedListener);
        }

        // Wait for the signal that all images have been saved.
        semaphore.acquire(numImages);

        verify(mockImageSavedListener, times(numImages)).onImageSaved(anyObject());
    }

    @Test(timeout = 5000)
    public void saveWillFail_whenInvalidFilePathIsUsed() throws InterruptedException {
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(defaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(cameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);
        useCase.addStateChangeListener(camera);

        // Note the invalid path
        File saveLocation = new File("/not/a/real/path.jpg");
        useCase.takePicture(saveLocation, onImageSavedListener);

        // Wait for the signal that an error occurred.
        semaphore.acquire();

        verify(mockImageSavedListener)
                .onError(eq(UseCaseError.FILE_IO_ERROR), anyString(), anyObject());
    }

    @Test(timeout = 5000)
    public void updateSessionConfigurationWithSuggestedResolution() throws InterruptedException {
        ImageCaptureUseCaseConfiguration configuration =
                new ImageCaptureUseCaseConfiguration.Builder().setCallbackHandler(handler).build();
        ImageCaptureUseCase useCase = new ImageCaptureUseCase(configuration);
        useCase.addStateChangeListener(camera);
        final Size[] sizes = {new Size(1920, 1080), new Size(640, 480)};

        for (Size size : sizes) {
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(cameraId, size);
            // Update SessionConfiguration with resolution setting
            useCase.updateSuggestedResolution(suggestedResolutionMap);
            CameraUtil.openCameraWithUseCase(camera, useCase, repeatingUseCase);

            useCase.takePicture(onImageCapturedListener);
            // Wait for the signal that the image has been captured.
            semaphore.acquire();

            assertThat(new Size(capturedImage.getWidth(), capturedImage.getHeight()))
                    .isEqualTo(size);

            // Detach use case from camera device to run next resolution setting
            CameraUtil.detachUseCaseFromCamera(camera, useCase);
        }
    }
}
