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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageFormatConstants;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.SurfaceCombination;
import androidx.camera.core.SurfaceConfig;
import androidx.camera.core.SurfaceConfig.ConfigSize;
import androidx.camera.core.SurfaceConfig.ConfigType;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.StreamConfigurationMapUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Robolectric test for {@link SupportedSurfaceCombination} class */
@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class SupportedSurfaceCombinationTest {
    private static final String LEGACY_CAMERA_ID = "0";
    private static final String LIMITED_CAMERA_ID = "1";
    private static final String FULL_CAMERA_ID = "2";
    private static final String RAW_CAMERA_ID = "3";
    private static final String LEVEL3_CAMERA_ID = "4";
    private static final int DEFAULT_SENSOR_ORIENTATION = 90;
    private static final Rational ASPECT_RATIO_4_3 = new Rational(4, 3);
    private static final Rational ASPECT_RATIO_16_9 = new Rational(16, 9);
    private final Size mDisplaySize = new Size(720, 1280);
    private final Size mAnalysisSize = new Size(640, 480);
    private final Size mPreviewSize = new Size(1280, 720);
    private final Size mRecordSize = new Size(3840, 2160);
    private final Size mMaximumSize = new Size(4032, 3024);
    private final Size mMaximumVideoSize = new Size(1920, 1080);
    private final Size mMod16Size = new Size(960, 544);
    private final CamcorderProfileHelper mMockCamcorderProfileHelper =
            Mockito.mock(CamcorderProfileHelper.class);
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    /**
     * Except for ImageFormat.JPEG, ImageFormat.YUV, and ImageFormat.RAW_SENSOR, other image formats
     * will be mapped to ImageFormat.PRIVATE (0x22) including SurfaceTexture or MediaCodec classes.
     * Before Android level 23, there is no ImageFormat.PRIVATE. But there is same internal code
     * 0x22 for internal corresponding format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED. Therefore,
     * set 0x22 as default image format.
     */
    private final int[] mSupportedFormats =
            new int[]{
                    ImageFormat.YUV_420_888,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_JPEG,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            };

    private final int[] mSupportedFormatsWithRaw =
            new int[]{
                    ImageFormat.YUV_420_888,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_JPEG,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    ImageFormat.RAW_SENSOR
            };

    private final Size[] mSupportedSizes =
            new Size[]{
                    new Size(4032, 3024),
                    new Size(3840, 2160),
                    new Size(1920, 1080),
                    new Size(1280, 720),
                    new Size(1280, 720), // duplicate the size since Nexus 5X emulator has the case.
                    new Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
                    new Size(800, 450),
                    new Size(640, 480),
                    new Size(320, 240),
                    new Size(320, 180),
                    new Size(256, 144) // For checkSmallSizesAreFilteredOut test.
            };

    private final Context mContext = RuntimeEnvironment.application.getApplicationContext();
    private FakeCameraFactory mCameraFactory;

    @Before
    public void setUp() {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealWidth(mDisplaySize.getWidth());
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealHeight(mDisplaySize.getHeight());

        when(mMockCamcorderProfileHelper.hasProfile(anyInt(), anyInt())).thenReturn(true);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(() -> CameraX.unbindAll());
        }
        CameraX.shutdown().get();
    }

    @Test
    public void checkLegacySurfaceCombinationSupportedInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLegacySurfaceCombinationSubListSupportedInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSupportedInLimitedDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSubListSupportedInLimited3Device() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationSupportedInFullDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationSubListSupportedInFullDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSupportedInRawDevice() {
        setupCamera(/* supportsRaw= */ true);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, RAW_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLegacySurfaceCombinationSupportedInRawDevice() {
        setupCamera(/* supportsRaw= */ true);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, RAW_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationSupportedInRawDevice() {
        setupCamera(/* supportsRaw= */ true);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, RAW_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkRawSurfaceCombinationSupportedInRawDevice() {
        setupCamera(/* supportsRaw= */ true);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, RAW_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getRAWSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSupportedInLevel3Device() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEVEL3_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSubListSupportedInLevel3Device() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEVEL3_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkTargetAspectRatioForPreviewInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        Rational targetAspectRatio = new Rational(9, 16);
        final Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        // Ensure we are bound to a camera to ensure aspect ratio correction is applied.
        FakeLifecycleOwner fakeLifecycle = new FakeLifecycleOwner();
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_FRONT).build();
        CameraX.bindToLifecycle(fakeLifecycle, cameraSelector, preview);

        PreviewConfig config = (PreviewConfig) preview.getUseCaseConfig();
        Rational previewAspectRatio = config.getTargetAspectRatioCustom();

        Rational correctedAspectRatio =
                supportedSurfaceCombination.getCorrectedAspectRatio(
                        config.getTargetRotation(Surface.ROTATION_0));

        Size maxJpegSize = supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG);
        Rational maxJpegAspectRatio = new Rational(maxJpegSize.getHeight(), maxJpegSize.getWidth());

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);
        Size previewSize = suggestedResolutionMap.get(preview);
        Rational resultAspectRatio = new Rational(previewSize.getHeight(), previewSize.getWidth());

        if (Build.VERSION.SDK_INT == 21) {
            // Checks targetAspectRatio and maxJpegAspectRatio, which is the ratio of maximum size
            // in the mSupportedSizes, are not equal to make sure this test case is valid.
            assertFalse(targetAspectRatio.equals(maxJpegAspectRatio));
            assertTrue(previewAspectRatio.equals(maxJpegAspectRatio));
            assertTrue(correctedAspectRatio.equals(maxJpegAspectRatio));
            assertTrue(resultAspectRatio.equals(maxJpegAspectRatio));
        } else {
            // Checks no correction is needed.
            assertThat(correctedAspectRatio).isNull();
            assertTrue(resultAspectRatio.equals(targetAspectRatio));
        }
    }

    @Test
    public void checkDefaultAspectRatioAndResolutionForMixedUseCase() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        Preview preview = new Preview.Builder().build();
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(preview);
        useCases.add(imageCapture);
        useCases.add(imageAnalysis);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        Size previewSize = suggestedResolutionMap.get(preview);
        Size imageCaptureSize = suggestedResolutionMap.get(imageCapture);
        Size imageAnalysisSize = suggestedResolutionMap.get(imageAnalysis);

        Rational previewAspectRatio = new Rational(previewSize.getWidth(), previewSize.getHeight());
        Rational imageCaptureAspectRatio = new Rational(imageCaptureSize.getWidth(),
                imageCaptureSize.getHeight());
        Rational imageAnalysisAspectRatio = new Rational(imageAnalysisSize.getWidth(),
                imageAnalysisSize.getHeight());

        // Checks the default aspect ratio.
        assertTrue(previewAspectRatio.equals(ASPECT_RATIO_4_3));
        assertTrue(imageCaptureAspectRatio.equals(ASPECT_RATIO_4_3));
        assertTrue(imageAnalysisAspectRatio.equals(ASPECT_RATIO_4_3));

        // Checks the default resolution.
        assertTrue(imageAnalysisSize.equals(mAnalysisSize));
    }

    @Test
    public void checkDefaultLensFacingForMixedUseCase() {
        setupCamera(/* supportsRaw= */ false);

        Preview preview = new Preview.Builder().build();
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        VideoCapture videoCapture = new VideoCaptureConfig.Builder().build();

        PreviewConfig previewConfig = (PreviewConfig) preview.getUseCaseConfig();
        ImageCaptureConfig imageCaptureConfig =
                (ImageCaptureConfig) imageCapture.getUseCaseConfig();
        ImageAnalysisConfig imageAnalysisConfig =
                (ImageAnalysisConfig) imageAnalysis.getUseCaseConfig();
        VideoCaptureConfig videoCaptureConfig =
                (VideoCaptureConfig) videoCapture.getUseCaseConfig();

        Integer previewLensFacing = previewConfig.getLensFacing(null);
        Integer imageCaptureLensFacing = imageCaptureConfig.getLensFacing(null);
        Integer imageAnalysisLensFacing = imageAnalysisConfig.getLensFacing(null);
        Integer videoCaptureLensFacing = videoCaptureConfig.getLensFacing(null);

        assertThat(previewLensFacing).isNotNull();
        assertThat(imageCaptureLensFacing).isNotNull();
        assertThat(imageAnalysisLensFacing).isNotNull();
        assertThat(videoCaptureLensFacing).isNotNull();
    }

    @Test
    public void checkSmallSizesAreFilteredOutByDefaultSize480p() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);
        /* This test case is for b/139018208 that get small resolution 144x256 with below
        conditions:
        1. The target aspect ratio is set to the screen size 1080 x 2220 (9:18.5).
        2. The camera doesn't provide any 9:18.5 resolution and the size 144x256(9:16)
         is considered the 9:18.5 mod16 version.
        3. There is no other bigger resolution matched the target aspect ratio.
        */
        final int displayWidth = 1080;
        final int displayHeight = 2220;
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(displayHeight, displayWidth))
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        // Checks the preconditions.
        final Size preconditionSize = new Size(256, 144);
        final Rational targetRatio = new Rational(displayHeight, displayWidth);
        ArrayList<Size> sizeList = new ArrayList<>(Arrays.asList(mSupportedSizes));
        assertTrue(sizeList.contains(preconditionSize));
        for (Size s : mSupportedSizes) {
            Rational supportedRational = new Rational(s.getWidth(), s.getHeight());
            assertFalse(supportedRational.equals(targetRatio));
        }

        // Checks the mechanism has filtered out the sizes which are smaller than default size 480p.
        Size previewSize = suggestedResolutionMap.get(preview);
        assertTrue(!previewSize.equals(preconditionSize));
    }

    @Test
    public void checkSmallTargetResolutionIsNotFilteredOutBy480p() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        // The resolution selection will filter out the sizes which are smaller than min(640x480,
        // TARGET_RESOLUTION)
        final Size targetResolution = new Size(240, 320);
        Preview preview = new Preview.Builder()
                .setTargetResolution(targetResolution)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        // Checks the returned size is 320x240, if we set the target resolution which is smaller
        // than 480p.
        final Size resultSize = new Size(320, 240);
        Size previewSize = suggestedResolutionMap.get(preview);
        assertTrue(previewSize.equals(resultSize));
    }

    @Test
    public void suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        VideoCapture videoCapture = new VideoCaptureConfig.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(imageCapture);
        useCases.add(videoCapture);
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        assertTrue(suggestedResolutionMap.size() != 3);
    }

    @Test
    public void getSuggestedResolutionsForMixedUseCaseInLimitedDevice() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        VideoCapture videoCapture = new VideoCaptureConfig.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(imageCapture);
        useCases.add(videoCapture);
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        assertThat(suggestedResolutionMap).containsEntry(imageCapture, mRecordSize);
        assertThat(suggestedResolutionMap).containsEntry(videoCapture, mMaximumVideoSize);
        assertThat(suggestedResolutionMap).containsEntry(preview, mPreviewSize);
    }

    @Test
    public void getSuggestedResolutionsWithSameSupportedListForDifferentUseCases() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        /* This test case is for b/132603284 that divide by zero issue crash happened in below
        conditions:
        1. There are duplicated two 1280x720 supported sizes for ImageCapture and Preview.
        2. supportedOutputSizes for ImageCapture and Preview in
        SupportedSurfaceCombination#getAllPossibleSizeArrangements are the same.
        */
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetResolution(mDisplaySize)
                .build();
        Preview preview = new Preview.Builder()
                .setTargetResolution(mDisplaySize)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(mDisplaySize)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(imageCapture);
        useCases.add(preview);
        useCases.add(imageAnalysis);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        assertThat(suggestedResolutionMap).containsEntry(imageCapture, mPreviewSize);
        assertThat(suggestedResolutionMap).containsEntry(preview, mPreviewSize);
        assertThat(suggestedResolutionMap).containsEntry(imageAnalysis, mPreviewSize);
    }

    @Test
    public void setTargetAspectRatioForMixedUseCases() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(imageCapture);
        useCases.add(preview);
        useCases.add(imageAnalysis);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        Size previewSize = suggestedResolutionMap.get(preview);
        Size imageCaptureSize = suggestedResolutionMap.get(imageCapture);
        Size imageAnalysisSize = suggestedResolutionMap.get(imageAnalysis);

        Rational previewAspectRatio = new Rational(previewSize.getWidth(), previewSize.getHeight());
        Rational imageCaptureAspectRatio = new Rational(imageCaptureSize.getWidth(),
                imageCaptureSize.getHeight());
        Rational imageAnalysisAspectRatio = new Rational(imageAnalysisSize.getWidth(),
                imageAnalysisSize.getHeight());

        assertTrue(previewAspectRatio.equals(ASPECT_RATIO_16_9));
        assertTrue(imageCaptureAspectRatio.equals(ASPECT_RATIO_16_9));
        assertTrue(imageAnalysisAspectRatio.equals(ASPECT_RATIO_16_9));
    }

    @Test
    public void throwsWhenSetBothTargetResolutionAndAspectRatioForDifferentUseCases() {
        setupCamera(/* supportsRaw= */ false);

        boolean previewExceptionHappened = false;
        Preview.Builder previewBuilder = new Preview.Builder()
                .setTargetResolution(mDisplaySize)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);
        try {
            previewBuilder.build();
        } catch (IllegalArgumentException e) {
            previewExceptionHappened = true;
        }
        assertTrue(previewExceptionHappened);

        boolean imageCaptureExceptionHappened = false;
        ImageCapture.Builder imageCaptureConfigBuilder = new ImageCapture.Builder()
                .setTargetResolution(mDisplaySize)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);
        try {
            imageCaptureConfigBuilder.build();
        } catch (IllegalArgumentException e) {
            imageCaptureExceptionHappened = true;
        }
        assertTrue(imageCaptureExceptionHappened);

        boolean imageAnalysisExceptionHappened = false;
        ImageAnalysis.Builder imageAnalysisConfigBuilder = new ImageAnalysis.Builder()
                .setTargetResolution(mDisplaySize)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);
        try {
            imageAnalysisConfigBuilder.build();
        } catch (IllegalArgumentException e) {
            imageAnalysisExceptionHappened = true;
        }
        assertTrue(imageAnalysisExceptionHappened);
    }

    @Test
    public void getSuggestedResolutionsForCustomizedSupportedResolutions() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<Pair<Integer, Size[]>> formatResolutionsPairList = new ArrayList<>();
        formatResolutionsPairList.add(Pair.create(ImageFormat.JPEG, new Size[]{mAnalysisSize}));
        formatResolutionsPairList.add(
                Pair.create(ImageFormat.YUV_420_888, new Size[]{mAnalysisSize}));
        formatResolutionsPairList.add(Pair.create(ImageFormat.PRIVATE, new Size[]{mAnalysisSize}));

        // Sets use cases customized supported resolutions to 640x480 only.
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setSupportedResolutions(formatResolutionsPairList)
                .build();
        VideoCapture videoCapture = new VideoCaptureConfig.Builder()
                .setSupportedResolutions(formatResolutionsPairList)
                .build();
        Preview preview = new Preview.Builder()
                .setSupportedResolutions(formatResolutionsPairList)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(imageCapture);
        useCases.add(videoCapture);
        useCases.add(preview);
        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);

        // Checks all suggested resolutions will become 640x480.
        assertThat(suggestedResolutionMap).containsEntry(imageCapture, mAnalysisSize);
        assertThat(suggestedResolutionMap).containsEntry(videoCapture, mAnalysisSize);
        assertThat(suggestedResolutionMap).containsEntry(preview, mAnalysisSize);
    }

    @Test
    public void transformSurfaceConfigWithYUVAnalysisSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ANALYSIS);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVPreviewSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVRecordSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVMaximumSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVNotSupportSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.NOT_SUPPORT);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGAnalysisSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ANALYSIS);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGPreviewSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGRecordSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGMaximumSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGNotSupportSize() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.NOT_SUPPORT);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void getMaximumSizeForImageFormat() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        Size maximumYUVSize =
                supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.YUV_420_888);
        assertEquals(mMaximumSize, maximumYUVSize);
        Size maximumJPEGSize =
                supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG);
        assertEquals(mMaximumSize, maximumJPEGSize);
    }

    @Test
    public void isAspectRatioMatchWithSupportedMod16Resolution() {
        setupCamera(/* supportsRaw= */ false);
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setDefaultResolution(mMod16Size)
                .build();
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setDefaultResolution(mMod16Size)
                .build();

        List<UseCase> useCases = new ArrayList<>();
        useCases.add(preview);
        useCases.add(imageCapture);

        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null, useCases);
        assertThat(suggestedResolutionMap).containsEntry(preview, mMod16Size);
        assertThat(suggestedResolutionMap).containsEntry(imageCapture, mMod16Size);
    }

    private void setupCamera(boolean supportsRaw) {
        mCameraFactory = new FakeCameraFactory();
        addCamera(
                LEGACY_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, null,
                CameraCharacteristics.LENS_FACING_FRONT);
        mCameraFactory.setDefaultCameraIdForLensFacing(CameraSelector.LENS_FACING_FRONT,
                LEGACY_CAMERA_ID);

        addCamera(
                LIMITED_CAMERA_ID,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                null, CameraCharacteristics.LENS_FACING_BACK);
        mCameraFactory.setDefaultCameraIdForLensFacing(CameraSelector.LENS_FACING_BACK,
                LIMITED_CAMERA_ID);

        addCamera(
                FULL_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, null,
                CameraCharacteristics.LENS_FACING_BACK);
        if (supportsRaw) {
            addCamera(
                    RAW_CAMERA_ID,
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                    new int[]{
                            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
                    },
                    CameraCharacteristics.LENS_FACING_BACK);
        }
        addCamera(
                LEVEL3_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3, null,
                CameraCharacteristics.LENS_FACING_BACK);
        initCameraX();
    }

    private void addCamera(String cameraId, int hardwareLevel, int[] capabilities, int lensFacing) {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(
                CameraCharacteristics.LENS_FACING, lensFacing);

        shadowCharacteristics.set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, hardwareLevel);

        shadowCharacteristics.set(
                CameraCharacteristics.SENSOR_ORIENTATION, DEFAULT_SENSOR_ORIENTATION);

        if (capabilities != null) {
            shadowCharacteristics.set(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities);
        }


        CameraManager cameraManager = (CameraManager) ApplicationProvider.getApplicationContext()
                .getSystemService(Context.CAMERA_SERVICE);

        ((ShadowCameraManager) Shadow.extract(cameraManager))
                .addCamera(cameraId, characteristics);

        int[] supportedFormats = isRawSupported(capabilities)
                ? mSupportedFormatsWithRaw : mSupportedFormats;

        shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapUtil.generateFakeStreamConfigurationMap(
                        supportedFormats, mSupportedSizes));

        @CameraSelector.LensFacing int lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
                lensFacing);
        mCameraFactory.insertCamera(lensFacingEnum, cameraId, () -> new FakeCamera(cameraId, null,
                new Camera2CameraInfoImpl(cameraId, characteristics, mock(ZoomControl.class),
                        mock(TorchControl.class))));
    }

    private void initCameraX() {
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                Camera2Config.defaultConfig(mContext))
                .setCameraFactory(mCameraFactory)
                .build();
        CameraX.initialize(mContext, cameraXConfig);
    }

    private boolean isRawSupported(int[] capabilities) {
        if (capabilities == null) {
            return false;
        }
        for (int capability : capabilities) {
            if (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllSubConfigListSupported(
            SupportedSurfaceCombination supportedSurfaceCombination,
            List<SurfaceCombination> combinationList) {
        boolean isSupported = true;

        for (SurfaceCombination combination : combinationList) {
            List<SurfaceConfig> configList = combination.getSurfaceConfigList();
            int length = configList.size();

            if (length <= 1) {
                continue;
            }

            for (int index = 0; index < length; index++) {
                List<SurfaceConfig> subConfigurationList = new ArrayList<>();
                subConfigurationList.addAll(configList);
                subConfigurationList.remove(index);

                isSupported &= supportedSurfaceCombination.checkSupported(subConfigurationList);

                if (!isSupported) {
                    return false;
                }
            }
        }

        return isSupported;
    }
}
