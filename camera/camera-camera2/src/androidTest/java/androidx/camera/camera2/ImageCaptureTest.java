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

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.internal.util.FakeRepeatingUseCase;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCapture.Metadata;
import androidx.camera.core.ImageCapture.OnImageCapturedCallback;
import androidx.camera.core.ImageCapture.OnImageSavedCallback;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCameraControl;
import androidx.camera.testing.fakes.FakeCaptureStage;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
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
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ImageCaptureTest {
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);
    private static final Size GUARANTEED_RESOLUTION = new Size(640, 480);
    @CameraSelector.LensFacing
    private static final int BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK;
    private static final CameraSelector BACK_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(BACK_LENS_FACING).build();

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private ImageCapture.Builder mDefaultBuilder;
    private FakeRepeatingUseCase mRepeatingUseCase;
    private FakeUseCaseConfig mFakeUseCaseConfig;
    private String mCameraId;
    private FakeLifecycleOwner mLifecycleOwner;
    private Executor mMainExecutor;

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
    public void setUp() throws ExecutionException, InterruptedException {
        assumeTrue(CameraUtil.deviceHasCamera());

        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = Camera2Config.defaultConfig();
        CameraFactory cameraFactory = Preconditions.checkNotNull(
                cameraXConfig.getCameraFactoryProvider(null)).newInstance(context);

        CameraX.initialize(context, cameraXConfig).get();
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(BACK_LENS_FACING);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + BACK_LENS_FACING, e);
        }
        mDefaultBuilder = new ImageCapture.Builder();

        mFakeUseCaseConfig = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        mRepeatingUseCase = new FakeRepeatingUseCase(mFakeUseCaseConfig);
        mLifecycleOwner = new FakeLifecycleOwner();

        mMainExecutor = ContextCompat.getMainExecutor(context);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(() -> CameraX.unbindAll());
        }
        CameraX.shutdown().get();
    }

    @Test
    public void capturedImageHasCorrectSize() throws ExecutionException, InterruptedException {
        ImageCapture useCase = new ImageCapture.Builder().setTargetResolution(
                DEFAULT_RESOLUTION).setTargetRotation(Surface.ROTATION_0).build();

        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        ResolvableFuture<ImageProperties> imageProperties = ResolvableFuture.create();
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(imageProperties);
        useCase.takePicture(mMainExecutor, callback);
        // Wait for the signal that the image has been captured.
        verify(callback, timeout(3000)).onCaptureSuccess(any(ImageProxy.class));

        Size sizeEnvelope = imageProperties.get().size;
        // Some devices may not be able to fit the requested resolution within the image
        // boundaries. In this case, they should always fall back to a guaranteed resolution of
        // 640 x 480.
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
    public void canSupportGuaranteedSize()
            throws CameraInfoUnavailableException, ExecutionException, InterruptedException {
        // CameraSelector.LENS_FACING_FRONT/LENS_FACING_BACK are defined as constant int 0 and 1.
        // Using for-loop to check both front and back device cameras can support the guaranteed
        // 640x480 size.
        for (int i = 0; i <= 1; i++) {
            final int lensFacing = i;
            if (!CameraUtil.hasCameraWithLensFacing(lensFacing)) {
                continue;
            }

            // Checks camera device sensor degrees to set correct target rotation value to make sure
            // the exactly matching result size 640x480 can be selected if the device supports it.
            String cameraId = CameraX.getCameraFactory().cameraIdForLensFacing(lensFacing);
            boolean isRotateNeeded = (CameraX.getCameraInfo(cameraId).getSensorRotationDegrees(
                    Surface.ROTATION_0) % 180) != 0;
            ImageCapture useCase = new ImageCapture.Builder().setTargetResolution(
                    GUARANTEED_RESOLUTION).setTargetRotation(
                    isRotateNeeded ? Surface.ROTATION_90 : Surface.ROTATION_0).build();

            mInstrumentation.runOnMainSync(
                    () -> {
                        CameraSelector cameraSelector =
                                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
                        CameraX.bindToLifecycle(mLifecycleOwner, cameraSelector, useCase,
                                mRepeatingUseCase);
                        mLifecycleOwner.startAndResume();
                    });

            ResolvableFuture<ImageProperties> imageProperties = ResolvableFuture.create();
            OnImageCapturedCallback callback = createMockOnImageCapturedCallback(imageProperties);
            useCase.takePicture(mMainExecutor, callback);
            // Wait for the signal that the image has been captured.
            verify(callback, timeout(3000)).onCaptureSuccess(any(ImageProxy.class));

            // Check the captured image exactly matches 640x480 size. This test can also check
            // whether the guaranteed resolution 640x480 is really supported for JPEG format on the
            // devices when running the test.
            assertEquals(GUARANTEED_RESOLUTION, imageProperties.get().size);

            // Reset the environment to run test for the other lens facing camera device.
            mInstrumentation.runOnMainSync(() -> {
                CameraX.unbindAll();
                mLifecycleOwner.pauseAndStop();
                mRepeatingUseCase = new FakeRepeatingUseCase(mFakeUseCaseConfig);
            });
        }
    }

    @Test
    public void canCaptureMultipleImages() {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            useCase.takePicture(mMainExecutor, callback);
        }

        // Wait for the signal that the image has been captured.
        verify(callback, timeout(15000).times(numImages)).onCaptureSuccess(any(ImageProxy.class));
    }

    @Test
    public void canCaptureMultipleImagesWithMaxQuality() {
        ImageCapture useCase = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
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
            useCase.takePicture(mMainExecutor, callback);
        }

        verify(callback, timeout(20000).times(numImages)).onCaptureSuccess(any(ImageProxy.class));
    }

    @Test
    public void saveCanSucceed() throws IOException {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, mMainExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(3000)).onImageSaved();
    }

    @Test
    public void canSaveFile_withRotation() throws IOException {
        // TODO(b/147448711) Add back in once cuttlefish has correct user cropping functionality.
        Assume.assumeFalse("Cuttlefish does not correctly handle crops. Unable to test.",
                android.os.Build.MODEL.contains("Cuttlefish"));
        ImageCapture useCase = new ImageCapture.Builder().setTargetRotation(
                Surface.ROTATION_0).build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, mMainExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(3000)).onImageSaved();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);

        File saveLocationRotated90 = File.createTempFile("testRotated90", ".jpg");
        saveLocationRotated90.deleteOnExit();
        OnImageSavedCallback callbackRotated90 = mock(OnImageSavedCallback.class);
        useCase.setTargetRotation(Surface.ROTATION_90);
        useCase.takePicture(saveLocationRotated90, mMainExecutor, callbackRotated90);

        // Wait for the signal that the image has been saved.
        verify(callbackRotated90, timeout(3000)).onImageSaved();

        // Retrieve the exif from the image
        Exif exifRotated90 = Exif.createFromFile(saveLocationRotated90);

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the rotated capture is scaled to fit within
        // the sensor region
        double aspectRatioThreshold = 0.01;

        // If rotation is equal then buffers were rotated by HAL so the aspect ratio should be
        // the same. Otherwise the aspect ratio should be rotated by 90 degrees.
        if (exif.getRotation() == exifRotated90.getRotation()) {
            double aspectRatio = (double) exif.getWidth() / exif.getHeight();
            double aspectRatioRotated90 =
                    (double) exifRotated90.getWidth() / exifRotated90.getHeight();
            assertThat(Math.abs(aspectRatio - aspectRatioRotated90)).isLessThan(
                    aspectRatioThreshold);
        } else {
            double aspectRatio = (double) exif.getHeight() / exif.getWidth();
            double aspectRatioRotated90 =
                    (double) exifRotated90.getWidth() / exifRotated90.getHeight();
            assertThat(Math.abs(aspectRatio - aspectRatioRotated90)).isLessThan(
                    aspectRatioThreshold);
        }
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
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.setReversedHorizontal(true);
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, metadata, mMainExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(3000)).onImageSaved();

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
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Metadata metadata = new Metadata();
        metadata.setReversedVertical(true);
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, metadata, mMainExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(3000)).onImageSaved();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.isFlippedVertically()).isTrue();
    }

    @Test
    public void canSaveFile_withAttachedLocation() throws IOException {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        File saveLocation = File.createTempFile("test", ".jpg");
        saveLocation.deleteOnExit();
        Location location = new Location("ImageCaptureTest");
        Metadata metadata = new Metadata();
        metadata.setLocation(location);
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, metadata, mMainExecutor, callback);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(3000)).onImageSaved();

        // Retrieve the exif from the image
        Exif exif = Exif.createFromFile(saveLocation);
        assertThat(exif.getLocation().getProvider()).isEqualTo(location.getProvider());
    }

    @Test
    public void canSaveMultipleFiles() throws IOException {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        int numImages = 5;
        for (int i = 0; i < numImages; ++i) {
            File saveLocation = File.createTempFile("test" + i, ".jpg");
            saveLocation.deleteOnExit();

            useCase.takePicture(saveLocation, mMainExecutor, callback);
        }

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(15000).times(numImages)).onImageSaved();
    }

    @Test
    public void saveWillFail_whenInvalidFilePathIsUsed() {
        ImageCapture useCase = mDefaultBuilder.build();
        mInstrumentation.runOnMainSync(
                () -> {
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        // Note the invalid path
        File saveLocation = new File("/not/a/real/path.jpg");
        OnImageSavedCallback callback = mock(OnImageSavedCallback.class);
        useCase.takePicture(saveLocation, mMainExecutor, callback);

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor =
                ArgumentCaptor.forClass(ImageCaptureException.class);

        // Wait for the signal that the image has been saved.
        verify(callback, timeout(3000)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_FILE_IO);
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
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        useCase.takePicture(mMainExecutor, callback);
        // Wait for the signal that the image has been captured.
        verify(callback, timeout(3000)).onCaptureSuccess(any(ImageProxy.class));

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
            throws CameraAccessException, ExecutionException, InterruptedException {
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
                    CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, useCase,
                            mRepeatingUseCase);
                    mLifecycleOwner.startAndResume();
                });

        ResolvableFuture<ImageProperties> imageProperties = ResolvableFuture.create();
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(imageProperties);
        useCase.takePicture(mMainExecutor, callback);
        // Wait for the signal that the image has been captured.
        verify(callback, timeout(3000)).onCaptureSuccess(any(ImageProxy.class));

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
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture,
                    mRepeatingUseCase);
            mLifecycleOwner.startAndResume();
        });

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        imageCapture.takePicture(mMainExecutor, callback);

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor =
                ArgumentCaptor.forClass(ImageCaptureException.class);
        verify(callback, timeout(3000)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getCause()).isInstanceOf(
                IllegalArgumentException.class);
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
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture,
                    mRepeatingUseCase);
            mLifecycleOwner.startAndResume();
        });

        // Add an additional capture stage to test the case
        // captureStage.size() >　mMaxCaptureStages during takePicture.
        captureStages.add(new FakeCaptureStage(1, new CaptureConfig.Builder().build()));

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);

        // Take 2 photos.
        imageCapture.takePicture(mMainExecutor, callback);
        imageCapture.takePicture(mMainExecutor, callback);

        // It should get onError() callback twice.
        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(3000).times(2)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getCause()).isInstanceOf(
                IllegalArgumentException.class);

    }

    @Test
    public void onCaptureCancelled_onErrorCAMERA_CLOSED() throws InterruptedException {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture,
                    mRepeatingUseCase);
            mLifecycleOwner.startAndResume();
        });

        FakeCameraControl fakeCameraControl = new FakeCameraControl(mock(
                CameraControlInternal.ControlUpdateCallback.class));
        imageCapture.attachCameraControl(mCameraId, fakeCameraControl);
        CountDownLatch captureSubmittedLatch = new CountDownLatch(1);
        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);

        imageCapture.takePicture(mMainExecutor, callback);

        captureSubmittedLatch.await(500, TimeUnit.MILLISECONDS);
        fakeCameraControl.notifyAllRequestOnCaptureCancelled();

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(500).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAMERA_CLOSED);
    }

    @Test
    public void onRequestFailed_OnErrorCAPTURE_FAILED() throws InterruptedException {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture,
                    mRepeatingUseCase);
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

        imageCapture.takePicture(mMainExecutor, callback);

        captureSubmittedLatch.await(500, TimeUnit.MILLISECONDS);
        fakeCameraControl.notifyAllRequestsOnCaptureFailed();

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(500).times(1)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_CAPTURE_FAILED);
    }

    @Test
    public void onStateOffline_abortAllCaptureRequests() throws InterruptedException {
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, BACK_SELECTOR, imageCapture,
                    mRepeatingUseCase);
            mLifecycleOwner.startAndResume();
        });

        CountingCallback callback = new CountingCallback(3, 500);

        imageCapture.takePicture(mMainExecutor, callback);
        imageCapture.takePicture(mMainExecutor, callback);
        imageCapture.takePicture(mMainExecutor, callback);

        mInstrumentation.runOnMainSync(() -> imageCapture.onStateOffline(mCameraId));

        assertThat(callback.getNumOnCaptureSuccess() + callback.getNumOnError()).isEqualTo(3);

        for (Integer imageCaptureError : callback.getImageCaptureErrors()) {
            assertThat(imageCaptureError).isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED);
        }
    }

    @Test
    public void takePictureReturnsErrorNO_CAMERA_whenNotBound() {
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        OnImageCapturedCallback callback = createMockOnImageCapturedCallback(null);
        imageCapture.takePicture(mMainExecutor, callback);

        final ArgumentCaptor<ImageCaptureException> exceptionCaptor = ArgumentCaptor.forClass(
                ImageCaptureException.class);
        verify(callback, timeout(500)).onError(exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getImageCaptureError()).isEqualTo(
                ImageCapture.ERROR_INVALID_CAMERA);
    }

    private static final class ImageProperties {
        public Size size;
        public int format;
        public int rotationDegrees;
    }

    private OnImageCapturedCallback createMockOnImageCapturedCallback(
            @Nullable ResolvableFuture<ImageProperties> resultProperties) {
        OnImageCapturedCallback callback = mock(OnImageCapturedCallback.class);
        doAnswer(
                i -> {
                    ImageProxy image = i.getArgument(0);
                    if (resultProperties != null) {
                        ImageProperties imageProperties = new ImageProperties();
                        imageProperties.size = new Size(image.getWidth(), image.getHeight());
                        imageProperties.format = image.getFormat();
                        imageProperties.rotationDegrees = image.getImageInfo().getRotationDegrees();
                        resultProperties.set(imageProperties);
                    }
                    image.close();
                    return null;
                }).when(callback).onCaptureSuccess(any(ImageProxy.class));

        return callback;
    }

    private static class CountingCallback extends OnImageCapturedCallback {
        CountDownLatch mCountDownLatch;
        long mTimeout;
        private int mNumOnCaptureSuccess = 0;
        private int mNumOnErrorSuccess = 0;

        List<Integer> mImageCaptureErrors = new ArrayList<>();

        CountingCallback(int numTakePictures, long timeout) {
            mTimeout = timeout;
            mCountDownLatch = new CountDownLatch(numTakePictures);
        }

        int getNumOnCaptureSuccess() throws InterruptedException {
            mCountDownLatch.await(mTimeout, TimeUnit.MILLISECONDS);
            return mNumOnCaptureSuccess;
        }

        int getNumOnError() throws InterruptedException {
            mCountDownLatch.await(mTimeout, TimeUnit.MILLISECONDS);
            return mNumOnErrorSuccess;
        }

        List<Integer> getImageCaptureErrors() throws InterruptedException {
            mCountDownLatch.await(mTimeout, TimeUnit.MILLISECONDS);
            return mImageCaptureErrors;
        }

        @Override
        public void onCaptureSuccess(@NonNull ImageProxy image) {
            mNumOnCaptureSuccess++;
            mCountDownLatch.countDown();
        }

        @Override
        public void onError(@NonNull final ImageCaptureException exception) {
            mNumOnErrorSuccess++;
            mImageCaptureErrors.add(exception.getImageCaptureError());
            mCountDownLatch.countDown();
        }
    }
}
