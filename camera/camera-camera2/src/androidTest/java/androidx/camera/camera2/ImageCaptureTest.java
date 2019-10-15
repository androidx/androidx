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

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.impl.util.FakeRepeatingUseCase;
import androidx.camera.core.AppConfig;
import androidx.camera.core.BaseCamera;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.Exif;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.ImageCaptureError;
import androidx.camera.core.ImageCapture.Metadata;
import androidx.camera.core.ImageCapture.OnImageCapturedCallback;
import androidx.camera.core.ImageCapture.OnImageSavedCallback;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@FlakyTest
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageCaptureTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    // Use most supported resolution for different supported hardware level devices,
    // especially for legacy devices.
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size SECONDARY_RESOLUTION = new Size(320, 240);
    private static final LensFacing BACK_LENS_FACING = LensFacing.BACK;

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private ExecutorService mListenerExecutor;
    private BaseCamera mCamera;
    private ImageCaptureConfig mDefaultConfig;
    private OnImageCapturedCallback mOnImageCapturedCallback;
    private OnImageCapturedCallback mMockImageCapturedListener;
    private ImageCapture.OnImageSavedCallback mOnImageSavedCallback;
    private ImageCapture.OnImageSavedCallback mMockImageSavedCallback;
    private ImageProxy mCapturedImage;
    private Semaphore mSemaphore;
    private FakeRepeatingUseCase mRepeatingUseCase;
    private FakeUseCaseConfig mFakeUseCaseConfig;
    private String mCameraId;

    private ImageCaptureConfig createNonRotatedConfiguration()
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

        return new ImageCaptureConfig.Builder()
                .setLensFacing(BACK_LENS_FACING)
                .setTargetRotation(surfaceRotation)
                .build();
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        mListenerExecutor = Executors.newSingleThreadExecutor();
        Context context = ApplicationProvider.getApplicationContext();
        AppConfig appConfig = Camera2AppConfig.create(context);
        CameraFactory cameraFactory = appConfig.getCameraFactory(null);
        CameraX.initialize(context, appConfig);
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(BACK_LENS_FACING);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + BACK_LENS_FACING, e);
        }
        mDefaultConfig = new ImageCaptureConfig.Builder().build();

        mCamera = cameraFactory.getCamera(mCameraId);
        mCapturedImage = null;
        mSemaphore = new Semaphore(/*permits=*/ 0);
        mOnImageCapturedCallback =
                new OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        mCapturedImage = image;
                        // Signal that the image has been captured.
                        mSemaphore.release();
                    }
                };
        mMockImageCapturedListener = mock(OnImageCapturedCallback.class);
        mMockImageSavedCallback = mock(OnImageSavedCallback.class);
        mOnImageSavedCallback =
                new OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        mMockImageSavedCallback.onImageSaved(file);
                        // Signal that an image was saved
                        mSemaphore.release();
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureError error, @NonNull String message,
                            @Nullable Throwable cause) {
                        mMockImageSavedCallback.onError(error, message, cause);
                        // Signal that there was an error
                        mSemaphore.release();
                    }
                };

        mFakeUseCaseConfig = new FakeUseCaseConfig.Builder().build();
        mRepeatingUseCase = new FakeRepeatingUseCase(mFakeUseCaseConfig);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (mCamera != null) {
            mCamera.close();
            if (mCapturedImage != null) {
                mCapturedImage.close();
            }
        }
        if (mListenerExecutor != null) {
            mListenerExecutor.shutdown();
        }
        CameraX.shutdown().get();
    }

    @Test
    public void capturedImageHasCorrectProperties() throws InterruptedException {
        ImageCaptureConfig config =
                new ImageCaptureConfig.Builder().build();
        ImageCapture useCase = new ImageCapture(config);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        useCase.takePicture(mListenerExecutor, mOnImageCapturedCallback);
        // Wait for the signal that the image has been captured.
        mSemaphore.acquire();

        assertThat(new Size(mCapturedImage.getWidth(), mCapturedImage.getHeight()))
                .isEqualTo(DEFAULT_RESOLUTION);
        assertThat(mCapturedImage.getFormat()).isEqualTo(useCase.getImageFormat());
    }

    @Test
    public void canCaptureMultipleImages() throws InterruptedException {
        ImageCapture useCase = new ImageCapture(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(
                    mListenerExecutor,
                    new OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                            mMockImageCapturedListener.onCaptureSuccess(image, rotationDegrees);
                            image.close();

                            // Signal that an image has been captured.
                            mSemaphore.release();
                        }
                    });
        }

        // Wait for the signal that all images have been captured.
        mSemaphore.acquire(numImages);

        verify(mMockImageCapturedListener, times(numImages)).onCaptureSuccess(any(ImageProxy.class),
                anyInt());
    }

    @Test
    public void canCaptureMultipleImagesWithMaxQuality() throws InterruptedException {
        ImageCaptureConfig config =
                new ImageCaptureConfig.Builder()
                        .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                        .build();
        ImageCapture useCase = new ImageCapture(config);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(
                    mListenerExecutor,
                    new OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                            mMockImageCapturedListener.onCaptureSuccess(image, rotationDegrees);
                            image.close();

                            // Signal that an image has been captured.
                            mSemaphore.release();
                        }
                    });
        }

        // Wait for the signal that all images have been captured.
        mSemaphore.acquire(numImages);

        verify(mMockImageCapturedListener, times(numImages)).onCaptureSuccess(any(ImageProxy.class),
                anyInt());
    }

    @Test
    public void saveCanSucceed() throws InterruptedException, IOException {
        ImageCapture useCase = new ImageCapture(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        useCase.takePicture(saveLocation, mListenerExecutor, mOnImageSavedCallback);

        // Wait for the signal that the image has been saved.
        mSemaphore.acquire();

        verify(mMockImageSavedCallback).onImageSaved(eq(saveLocation));
    }

    @Test
    public void canSaveFile_withRotation()
            throws InterruptedException, IOException, CameraInfoUnavailableException {
        ImageCapture useCase = new ImageCapture(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, mOnImageSavedCallback);

        // Wait for the signal that the image has been saved.
        mSemaphore.acquire();

        // Retrieve the sensor orientation
        int rotationDegrees = CameraX.getCameraInfo(mCameraId).getSensorRotationDegrees();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getRotation()).isEqualTo(rotationDegrees);
    }

    @Test
    public void canSaveFile_flippedHorizontal()
            throws InterruptedException, IOException, CameraInfoUnavailableException {
        // Use a non-rotated configuration since some combinations of rotation + flipping vertically
        // can be equivalent to flipping horizontally
        ImageCapture useCase = new ImageCapture(createNonRotatedConfiguration());
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.isReversedHorizontal = true;
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, mOnImageSavedCallback);

        // Wait for the signal that the image has been saved.
        mSemaphore.acquire();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedHorizontally()).isTrue();
    }

    @Test
    public void canSaveFile_flippedVertical()
            throws InterruptedException, IOException, CameraInfoUnavailableException {
        // Use a non-rotated configuration since some combinations of rotation + flipping
        // horizontally can be equivalent to flipping vertically
        ImageCapture useCase = new ImageCapture(createNonRotatedConfiguration());
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.isReversedVertical = true;
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, mOnImageSavedCallback);

        // Wait for the signal that the image has been saved.
        mSemaphore.acquire();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedVertically()).isTrue();
    }

    @Test
    public void canSaveFile_withAttachedLocation() throws InterruptedException, IOException {
        ImageCapture useCase = new ImageCapture(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Location location = new Location("ImageCaptureTest");
        Metadata metadata = new Metadata();
        metadata.location = location;
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, mOnImageSavedCallback);

        // Wait for the signal that the image has been saved.
        mSemaphore.acquire();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getLocation().getProvider()).isEqualTo(location.getProvider());
    }

    @Test
    public void canSaveMultipleFiles() throws InterruptedException, IOException {
        ImageCapture useCase = new ImageCapture(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            File saveLocation = File.createTempFile("test" + i, ".jpg");
            saveLocation.deleteOnExit();

            useCase.takePicture(saveLocation, mListenerExecutor, mOnImageSavedCallback);
        }

        // Wait for the signal that all images have been saved.
        mSemaphore.acquire(numImages);

        verify(mMockImageSavedCallback, times(numImages)).onImageSaved(any(File.class));
    }

    @Test
    public void saveWillFail_whenInvalidFilePathIsUsed() throws InterruptedException {
        ImageCapture useCase = new ImageCapture(mDefaultConfig);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        // Note the invalid path
        File saveLocation = new File("/not/a/real/path.jpg");
        useCase.takePicture(saveLocation, mListenerExecutor, mOnImageSavedCallback);

        // Wait for the signal that an error occurred.
        mSemaphore.acquire();

        verify(mMockImageSavedCallback)
                .onError(eq(ImageCaptureError.FILE_IO_ERROR), anyString(), any(Throwable.class));
    }

    @Suppress // TODO(b/133171096): Remove once this no longer throws an IllegalStateException
    @Test
    public void updateSessionConfigWithSuggestedResolution() throws InterruptedException {
        ImageCaptureConfig config =
                new ImageCaptureConfig.Builder().build();
        ImageCapture useCase = new ImageCapture(config);
        useCase.addStateChangeListener(mCamera);
        final Size[] sizes = {SECONDARY_RESOLUTION, DEFAULT_RESOLUTION};

        for (Size size : sizes) {
            Map<String, Size> suggestedResolutionMap = new HashMap<>();
            suggestedResolutionMap.put(mCameraId, size);
            // Update SessionConfig with resolution setting
            useCase.updateSuggestedResolution(suggestedResolutionMap);
            CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);

            useCase.takePicture(mListenerExecutor, mOnImageCapturedCallback);
            // Wait for the signal that the image has been captured.
            mSemaphore.acquire();

            assertThat(new Size(mCapturedImage.getWidth(), mCapturedImage.getHeight()))
                    .isEqualTo(size);

            // Detach use case from camera device to run next resolution setting
            CameraUtil.detachUseCaseFromCamera(mCamera, useCase);
        }
    }

    @Test
    public void camera2InteropCaptureSessionCallbacks() throws InterruptedException {
        ImageCaptureConfig.Builder configBuilder = new ImageCaptureConfig.Builder();
        CameraCaptureSession.CaptureCallback captureCallback =
                mock(CameraCaptureSession.CaptureCallback.class);
        new Camera2Config.Extender(configBuilder).setSessionCaptureCallback(captureCallback);
        ImageCapture useCase = new ImageCapture(configBuilder.build());
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        useCase.takePicture(mListenerExecutor, mOnImageCapturedCallback);
        // Wait for the signal that the image has been captured.
        mSemaphore.acquire();

        // Note: preview callbacks also fire on interop listener.
        ArgumentMatcher<CaptureRequest> matcher = new ArgumentMatcher<CaptureRequest>() {
            @Override
            public boolean matches(CaptureRequest captureRequest) {
                return captureRequest.get(CaptureRequest.CONTROL_CAPTURE_INTENT)
                        == CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE;
            }
        };
        // Because interop listener will get both image capture and preview callbacks, ensure
        // that there is one CAPTURE_INTENT_STILL_CAPTURE from all onCaptureCompleted() callbacks.
        verify(captureCallback, times(1)).onCaptureCompleted(
                any(CameraCaptureSession.class),
                argThat(matcher),
                any(TotalCaptureResult.class));
    }

    @Test
    public void takePicture_withBufferFormatRaw10()
            throws InterruptedException, CameraAccessException {
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(mCameraId);
        StreamConfigurationMap map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] resolutions = map.getOutputSizes(ImageFormat.RAW10);
        // Ignore this tests on devices that do not support RAW10 image format.
        Assume.assumeTrue(resolutions != null);
        Assume.assumeTrue(resolutions.length > 0);
        Size resolution = resolutions[0];

        ImageCaptureConfig config =
                new ImageCaptureConfig.Builder()
                        .setBufferFormat(ImageFormat.RAW10)
                        .build();
        ImageCapture useCase = new ImageCapture(config);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, resolution);
        mInstrumentation.runOnMainSync(
                () -> useCase.updateSuggestedResolution(suggestedResolutionMap));
        CameraUtil.openCameraWithUseCase(mCameraId, mCamera, useCase, mRepeatingUseCase);
        useCase.addStateChangeListener(mCamera);

        useCase.takePicture(mListenerExecutor, mOnImageCapturedCallback);

        // Wait for the signal that the image has been saved.
        mSemaphore.acquire();

        assertThat(mCapturedImage.getFormat()).isEqualTo(ImageFormat.RAW10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withBufferFormatAndCaptureProcessor_throwsException() {
        ImageCaptureConfig config =
                new ImageCaptureConfig.Builder()
                        .setBufferFormat(ImageFormat.RAW_SENSOR)
                        .setCaptureProcessor(mock(CaptureProcessor.class))
                        .build();
        new ImageCapture(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_maxCaptureStageInvalid_throwsException() {
        ImageCaptureConfig config = new ImageCaptureConfig.Builder().setMaxCaptureStages(0).build();
        new ImageCapture(config);
    }

    @Test
    public void captureStagesAbove1_withoutCaptureProcessor() {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();

        CaptureBundle captureBundle = new CaptureBundle() {
            @Override
            public List<CaptureStage> getCaptureStages() {
                return Collections.unmodifiableList(new ArrayList<>(
                        Arrays.asList(
                                new FakeCaptureStage(0, new CaptureConfig.Builder().build()),
                                new FakeCaptureStage(1, new CaptureConfig.Builder().build()))));
            }
        };

        ImageCaptureConfig.Builder configBuilder =
                new ImageCaptureConfig.Builder().setCaptureBundle(captureBundle);
        ImageCapture imageCapture = new ImageCapture(configBuilder.build());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(lifecycle, imageCapture);
                lifecycle.startAndResume();
            }
        });

        OnImageCapturedCallback mockOnImageCaptureListener = mock(OnImageCapturedCallback.class);
        imageCapture.takePicture(mListenerExecutor, mockOnImageCaptureListener);

        verify(mockOnImageCaptureListener, timeout(3000)).onError(any(ImageCaptureError.class),
                anyString(), any(IllegalArgumentException.class));

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.unbind(imageCapture);
            }
        });
    }

    @Test
    public void captureStageExceedMaxCaptureStage_whenIssueTakePicture() {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();

        // Initial the captureStages not greater than the maximum count to bypass the
        // CaptureStage count checking during bindToLifeCycle.
        List<CaptureStage> captureStages = new ArrayList<>();
        captureStages.add(new FakeCaptureStage(0, new CaptureConfig.Builder().build()));

        CaptureBundle captureBundle = new CaptureBundle() {
            @Override
            public List<CaptureStage> getCaptureStages() {
                return Collections.unmodifiableList(captureStages);
            }
        };

        ImageCaptureConfig config = new ImageCaptureConfig.Builder()
                .setMaxCaptureStages(1)
                .setCaptureBundle(captureBundle)
                .setCaptureProcessor(mock(CaptureProcessor.class))
                .build();
        ImageCapture imageCapture = new ImageCapture(config);

        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(lifecycle, imageCapture);
            lifecycle.startAndResume();
        });

        // Add an additional capture stage to test the case
        // captureStage.size() >　mMaxCaptureStages during takePicture.
        captureStages.add(new FakeCaptureStage(1, new CaptureConfig.Builder().build()));

        OnImageCapturedCallback mockOnImageCaptureListener = mock(OnImageCapturedCallback.class);

        // Take 2 photos.
        imageCapture.takePicture(mListenerExecutor, mockOnImageCaptureListener);
        imageCapture.takePicture(mListenerExecutor, mockOnImageCaptureListener);

        // It should get onError() callback twice.
        verify(mockOnImageCaptureListener, timeout(3000).times(2)).onError(
                any(ImageCaptureError.class), anyString(), any(IllegalArgumentException.class));

        mInstrumentation.runOnMainSync(() -> CameraX.unbind(imageCapture));
    }

    @Test
    public void onCaptureCancelled_onErrorCAMERA_CLOSED()
            throws InterruptedException {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();
        ImageCaptureConfig config = new ImageCaptureConfig.Builder()
                .setLensFacing(BACK_LENS_FACING)
                .build();
        ImageCapture imageCapture = new ImageCapture(config);
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(lifecycle, imageCapture);
            lifecycle.startAndResume();
        });

        FakeCameraControl fakeCameraControl = new FakeCameraControl(mock(
                CameraControlInternal.ControlUpdateListener.class));
        imageCapture.attachCameraControl(mCameraId, fakeCameraControl);
        CountDownLatch captureSubmittedLatch = new CountDownLatch(1);
        OnImageCapturedCallback onImageCapturedCallback = mock(OnImageCapturedCallback.class);

        imageCapture.takePicture(mListenerExecutor, onImageCapturedCallback);

        captureSubmittedLatch.await(500, TimeUnit.MILLISECONDS);
        fakeCameraControl.notifyAllRequestOnCaptureCancelled();

        ArgumentCaptor<ImageCaptureError> errorCaptor =
                ArgumentCaptor.forClass(ImageCaptureError.class);
        verify(onImageCapturedCallback, timeout(500).times(1)).onError(errorCaptor.capture(),
                any(String.class),
                any(Throwable.class));
        assertThat(errorCaptor.getValue()).isEqualTo(ImageCaptureError.CAMERA_CLOSED);

        mInstrumentation.runOnMainSync(() -> CameraX.unbind(imageCapture));
    }

    @Test
    public void onRequestFailed_OnErrorCAPTURE_FAILED() throws InterruptedException {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();
        ImageCaptureConfig config = new ImageCaptureConfig.Builder()
                .setLensFacing(BACK_LENS_FACING)
                .build();
        ImageCapture imageCapture = new ImageCapture(config);
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(lifecycle, imageCapture);
            lifecycle.startAndResume();
        });

        FakeCameraControl fakeCameraControl = new FakeCameraControl(mock(
                CameraControlInternal.ControlUpdateListener.class));
        imageCapture.attachCameraControl(mCameraId, fakeCameraControl);
        CountDownLatch captureSubmittedLatch = new CountDownLatch(1);
        OnImageCapturedCallback onImageCapturedCallback = mock(OnImageCapturedCallback.class);
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            captureSubmittedLatch.countDown();
        });

        imageCapture.takePicture(mListenerExecutor, onImageCapturedCallback);

        captureSubmittedLatch.await(500, TimeUnit.MILLISECONDS);
        fakeCameraControl.notifyAllRequestsOnCaptureFailed();

        ArgumentCaptor<ImageCaptureError> errorCaptor =
                ArgumentCaptor.forClass(ImageCaptureError.class);
        verify(onImageCapturedCallback, timeout(500).times(1)).onError(errorCaptor.capture(),
                any(String.class),
                any(Throwable.class));
        assertThat(errorCaptor.getValue()).isEqualTo(ImageCaptureError.CAPTURE_FAILED);

        mInstrumentation.runOnMainSync(() -> CameraX.unbind(imageCapture));
    }
}
