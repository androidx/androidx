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
import static org.mockito.Mockito.doAnswer;
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

import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.impl.util.FakeRepeatingUseCase;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraControlInternal;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureBundle;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.Exif;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.CaptureMode;
import androidx.camera.core.ImageCapture.ImageCaptureError;
import androidx.camera.core.ImageCapture.Metadata;
import androidx.camera.core.ImageCapture.OnImageCapturedCallback;
import androidx.camera.core.ImageCapture.OnImageSavedCallback;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.LensFacing;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageCaptureTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    @LensFacing
    private static final int BACK_LENS_FACING = LensFacing.BACK;
    private static final CameraSelector BACK_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(BACK_LENS_FACING).build();

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private ExecutorService mListenerExecutor;
    private ImageCapture.Builder mDefaultBuilder;
    private FakeRepeatingUseCase mRepeatingUseCase;
    private FakeUseCaseConfig mFakeUseCaseConfig;
    private String mCameraId;
    private FakeLifecycleOwner mLifecycleOwner;

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

        return new ImageCapture.Builder()
                .setTargetRotation(surfaceRotation)
                .getUseCaseConfig();
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
        mDefaultBuilder = new ImageCapture.Builder();

        mFakeUseCaseConfig = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        mRepeatingUseCase = new FakeRepeatingUseCase(mFakeUseCaseConfig, BACK_SELECTOR);
        mLifecycleOwner = new FakeLifecycleOwner();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(() -> CameraX.unbindAll());
        }

        if (mListenerExecutor != null) {
            mListenerExecutor.shutdown();
        }
        CameraX.shutdown().get();
    }

    @Test
    public void capturedImageHasCorrectSize() {
        ImageCapture useCase = new ImageCapture.Builder().setTargetResolution(
                DEFAULT_RESOLUTION).setTargetRotation(Surface.ROTATION_0).build();

        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        AtomicReference<ImageProperties> imageProperties = new AtomicReference<>(null);
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(imageProperties);
        useCase.takePicture(mListenerExecutor, callback);
        // Wait for the signal that the image has been captured.
        verify(callback, timeout(1000)).onCaptureSuccess(any(ImageProxy.class),
                anyInt());

        Size sizeEnvelope = imageProperties.get().size;
        // Some devices may not be able to fit the requested resolution within the image
        // boundaries. In this case, they should always fall back to a guaranteed resolution of
        // 640 x 480.
        // TODO(b/143734827): Create a more robust test for the case of guaranteed resolution
        if (!Objects.equals(sizeEnvelope, GUARANTEED_RESOLUTION)) {
            int rotationDegrees = imageProperties.get().rotationDegrees;
            // If the image data is rotated by 90 or 270, we need to ensure our desired width fits
            // within the height of this image and our desired height fits in the width.
            if (rotationDegrees == 270 || rotationDegrees == 90) {
                sizeEnvelope = new Size(sizeEnvelope.getHeight(), sizeEnvelope.getWidth());
            }

            // Ensure the width and height can be cropped from the source image
            assertThat(sizeEnvelope.getWidth()).isAtLeast(DEFAULT_RESOLUTION.getWidth());
            assertThat(sizeEnvelope.getHeight()).isAtLeast(DEFAULT_RESOLUTION.getHeight());
        }
    }

    @Test
    public void canCaptureMultipleImages() {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(mListenerExecutor, callback);
        }

        // Wait for the signal that the image has been captured.
        verify(callback, timeout(5000).times(numImages)).onCaptureSuccess(any(ImageProxy.class),
                anyInt());
    }

    @Test
    public void canCaptureMultipleImagesWithMaxQuality() {
        ImageCapture useCase = new ImageCapture.Builder()
                .setCaptureMode(CaptureMode.MAXIMIZE_QUALITY)
                .build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, mRepeatingUseCase,
                            useCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(mListenerExecutor, callback);
        }

        verify(callback, timeout(10000).times(numImages)).onCaptureSuccess(any(ImageProxy.class),
                anyInt());
    }

    @Test
    public void saveCanSucceed() throws IOException {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, mListenerExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(2000)).onImageSaved(eq(saveLocation));
    }

    @Test
    public void canSaveFile_withRotation() throws IOException {
        ImageCapture useCase = new ImageCapture.Builder().setTargetRotation(
                Surface.ROTATION_0).build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, mListenerExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(2000)).onImageSaved(eq(saveLocation));

        // Retrieve the sensor orientation
        int rotationDegrees = CameraX.getCameraInfo(mCameraId).getSensorRotationDegrees();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getRotation()).isEqualTo(rotationDegrees);
    }

    @Test
    public void canSaveFile_flippedHorizontal()
            throws IOException, CameraInfoUnavailableException {
        // Use a non-rotated configuration since some combinations of rotation + flipping vertically
        // can be equivalent to flipping horizontally
        ImageCapture useCase = ImageCapture.Builder.fromConfig(
                createNonRotatedConfiguration()).build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.setReversedHorizontal(true);
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(2000)).onImageSaved(eq(saveLocation));

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedHorizontally()).isTrue();
    }

    @Test
    public void canSaveFile_flippedVertical()
            throws IOException, CameraInfoUnavailableException {
        // Use a non-rotated configuration since some combinations of rotation + flipping
        // horizontally can be equivalent to flipping vertically
        ImageCapture useCase = ImageCapture.Builder.fromConfig(
                createNonRotatedConfiguration()).build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.setReversedVertical(true);
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(2000)).onImageSaved(eq(saveLocation));

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedVertically()).isTrue();
    }

    @Test
    public void canSaveFile_withAttachedLocation() throws IOException {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Location location = new Location("ImageCaptureTest");
        Metadata metadata = new Metadata();
        metadata.setLocation(location);
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, metadata, mListenerExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(2000)).onImageSaved(eq(saveLocation));

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getLocation().getProvider()).isEqualTo(location.getProvider());
    }

    @Test
    public void canSaveMultipleFiles() throws IOException {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            File saveLocation = File.createTempFile("test" + i, ".jpg");
            saveLocation.deleteOnExit();

            useCase.takePicture(saveLocation, mListenerExecutor, callback);
        }

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(5000).times(numImages)).onImageSaved(any(File.class));
    }

    @Test
    public void saveWillFail_whenInvalidFilePathIsUsed() {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        // Note the invalid path
        File saveLocation = new File("/not/a/real/path.jpg");
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, mListenerExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(2000))
                .onError(eq(ImageCaptureError.FILE_IO_ERROR), anyString(), any(Throwable.class));
    }

    @Test
    @UseExperimental(markerClass = ExperimentalCamera2Interop.class)
    public void camera2InteropCaptureSessionCallbacks() {
        ImageCapture.Builder builder = new ImageCapture.Builder();
        CameraCaptureSession.CaptureCallback captureCallback =
                mock(CameraCaptureSession.CaptureCallback.class);
        new Camera2Interop.Extender<>(builder).setSessionCaptureCallback(captureCallback);
        ImageCapture useCase = builder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        useCase.takePicture(mListenerExecutor, callback);
        // Wait for the signal that the image has been captured.
        verify(callback, timeout(1000)).onCaptureSuccess(any(ImageProxy.class), anyInt());

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
    public void takePicture_withBufferFormatRaw10() throws CameraAccessException {
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(mCameraId);
        StreamConfigurationMap map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] resolutions = map.getOutputSizes(ImageFormat.RAW10);
        // Ignore this tests on devices that do not support RAW10 image format.
        Assume.assumeTrue(resolutions != null);
        Assume.assumeTrue(resolutions.length > 0);
        Size resolution = resolutions[0];

        ImageCapture useCase = new ImageCapture.Builder()
                .setBufferFormat(ImageFormat.RAW10)
                .build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase);
                    mLifecycleOwner.startAndResume();
                });

        AtomicReference<ImageProperties> imageProperties = new AtomicReference<>();
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(imageProperties);
        useCase.takePicture(mListenerExecutor, callback);
        // Wait for the signal that the image has been captured.
        verify(callback, timeout(1000)).onCaptureSuccess(any(ImageProxy.class), anyInt());

        assertThat(imageProperties.get().format).isEqualTo(ImageFormat.RAW10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withBufferFormatAndCaptureProcessor_throwsException() {
        new ImageCapture.Builder()
                .setBufferFormat(ImageFormat.RAW_SENSOR)
                .setCaptureProcessor(mock(CaptureProcessor.class))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_maxCaptureStageInvalid_throwsException() {
        new ImageCapture.Builder().setMaxCaptureStages(0).build();
    }

    @Test
    public void captureStagesAbove1_withoutCaptureProcessor() {
        CaptureBundle captureBundle = new CaptureBundle() {
            @Override
            public List<CaptureStage> getCaptureStages() {
                return Collections.unmodifiableList(new ArrayList<>(
                        Arrays.asList(
                                new FakeCaptureStage(0, new CaptureConfig.Builder().build()),
                                new FakeCaptureStage(1, new CaptureConfig.Builder().build()))));
            }
        };

        ImageCapture imageCapture = new ImageCapture.Builder().setCaptureBundle(
                captureBundle).build();

        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture);
            mLifecycleOwner.startAndResume();
        });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        imageCapture.takePicture(mListenerExecutor, callback);

        verify(callback, timeout(3000)).onError(any(Integer.class),
                anyString(), any(IllegalArgumentException.class));

    }

    @Test
    public void captureStageExceedMaxCaptureStage_whenIssueTakePicture() {
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

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setMaxCaptureStages(1)
                .setCaptureBundle(captureBundle)
                .setCaptureProcessor(mock(CaptureProcessor.class))
                .build();

        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture);
            mLifecycleOwner.startAndResume();
        });

        // Add an additional capture stage to test the case
        // captureStage.size() >　mMaxCaptureStages during takePicture.
        captureStages.add(new FakeCaptureStage(1, new CaptureConfig.Builder().build()));

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);

        // Take 2 photos.
        imageCapture.takePicture(mListenerExecutor, callback);
        imageCapture.takePicture(mListenerExecutor, callback);

        // It should get onError() callback twice.
        verify(callback, timeout(3000).times(2)).onError(any(Integer.class), anyString(),
                any(IllegalArgumentException.class));

    }

    @Test
    public void onCaptureCancelled_onErrorCAMERA_CLOSED() throws InterruptedException {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture);
            mLifecycleOwner.startAndResume();
        });

        FakeCameraControl fakeCameraControl = new FakeCameraControl(mock(
                CameraControlInternal.ControlUpdateCallback.class));
        imageCapture.attachCameraControl(mCameraId, fakeCameraControl);
        CountDownLatch captureSubmittedLatch = new CountDownLatch(1);
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);

        imageCapture.takePicture(mListenerExecutor, callback);

        captureSubmittedLatch.await(500, TimeUnit.MILLISECONDS);
        fakeCameraControl.notifyAllRequestOnCaptureCancelled();

        ArgumentCaptor<Integer> errorCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(callback, timeout(500).times(1)).onError(errorCaptor.capture(),
                any(String.class),
                any(Throwable.class));
        assertThat(errorCaptor.getValue()).isEqualTo(ImageCaptureError.CAMERA_CLOSED);
    }

    @Test
    public void onRequestFailed_OnErrorCAPTURE_FAILED() throws InterruptedException {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture);
            mLifecycleOwner.startAndResume();
        });

        FakeCameraControl fakeCameraControl = new FakeCameraControl(mock(
                CameraControlInternal.ControlUpdateCallback.class));
        imageCapture.attachCameraControl(mCameraId, fakeCameraControl);
        CountDownLatch captureSubmittedLatch = new CountDownLatch(1);
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        fakeCameraControl.setOnNewCaptureRequestListener(captureConfigs -> {
            captureSubmittedLatch.countDown();
        });

        imageCapture.takePicture(mListenerExecutor, callback);

        captureSubmittedLatch.await(500, TimeUnit.MILLISECONDS);
        fakeCameraControl.notifyAllRequestsOnCaptureFailed();

        ArgumentCaptor<Integer> errorCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(callback, timeout(500).times(1)).onError(errorCaptor.capture(),
                any(String.class),
                any(Throwable.class));
        assertThat(errorCaptor.getValue()).isEqualTo(ImageCaptureError.CAPTURE_FAILED);
    }

    @Test
    public void onStateOffline_abortAllCaptureRequests() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture);
            mLifecycleOwner.startAndResume();
        });

        FakeCameraControl fakeCameraControl = new FakeCameraControl(mock(
                CameraControlInternal.ControlUpdateCallback.class));
        imageCapture.attachCameraControl(mCameraId, fakeCameraControl);
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);

        imageCapture.takePicture(mListenerExecutor, callback);
        imageCapture.takePicture(mListenerExecutor, callback);
        imageCapture.takePicture(mListenerExecutor, callback);

        mInstrumentation.runOnMainSync(() -> imageCapture.onStateOffline(mCameraId));

        ArgumentCaptor<Integer> errorCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(callback, timeout(500).times(3)).onError(errorCaptor.capture(),
                any(String.class),
                any(Throwable.class));
        assertThat(errorCaptor.getAllValues()).containsExactly(
                ImageCaptureError.CAMERA_CLOSED,
                ImageCaptureError.CAMERA_CLOSED,
                ImageCaptureError.CAMERA_CLOSED);
    }

    @Test
    public void takePictureReturnsErrorNO_CAMERA_whenNotBound() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        imageCapture.takePicture(mListenerExecutor, callback);

        ArgumentCaptor<Integer> errorCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(callback, timeout(500)).onError(errorCaptor.capture(), any(String.class),
                any(Throwable.class));
        assertThat(errorCaptor.getValue()).isEqualTo(ImageCaptureError.INVALID_CAMERA);
    }

    private static final class ImageProperties {
        public Size size;
        public int format;
        public int rotationDegrees;
    }

    private OnImageCapturedCallback createMockOnImageCapturedCallback(
            @Nullable AtomicReference<ImageProperties> resultProperties) {
        OnImageCapturedCallback callback = mock(OnImageCapturedCallback.class);
        doAnswer(
                i -> {
                    ImageProxy image = i.getArgument(0);
                    if (resultProperties != null) {
                        int rotationDegrees = i.getArgument(1);
                        ImageProperties imageProperties = new ImageProperties();
                        imageProperties.size = new Size(image.getWidth(), image.getHeight());
                        imageProperties.format = image.getFormat();
                        imageProperties.rotationDegrees = rotationDegrees;
                        resultProperties.set(imageProperties);
                    }
                    image.close();
                    return null;
                }).when(callback).onCaptureSuccess(any(ImageProxy.class),
                anyInt());

        return callback;
    }
}
