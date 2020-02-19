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

package androidx.camera.camera2.internal;

import static androidx.camera.camera2.internal.SupportedSurfaceCombination.hasMatchingAspectRatio;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceConfig.ConfigSize;
import androidx.camera.core.impl.SurfaceConfig.ConfigType;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.StreamConfigurationMapUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        maxSdk = Build.VERSION_CODES.P //TODO (b/149669465) : Some robolectric tests will fail on Q
)
public final class SupportedSurfaceCombinationTest {
    private static final String CAMERA_ID = "0";
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
                    new Size(1920, 1440),
                    new Size(1920, 1080),
                    new Size(1280, 960),
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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkTargetAspectRatioForPreviewInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        Rational targetAspectRatio = new Rational(9, 16);
        final Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        // Ensure we are bound to a camera to ensure aspect ratio correction is applied.
        FakeLifecycleOwner fakeLifecycle = new FakeLifecycleOwner();
        CameraX.bindToLifecycle(fakeLifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview);

        PreviewConfig config = (PreviewConfig) preview.getUseCaseConfig();
        // The targetAspectRatioCustom value will only be set in Legacy + API 21 combination. For
        // other combinations, it shouldn't be set since there is targetAspectRatio set for the
        // use case.
        Rational previewAspectRatio = config.getTargetAspectRatioCustom(null);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        Preview preview = new Preview.Builder().build();
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        // Preview/ImageCapture/ImageAnalysis' default config settings that will be applied after
        // bound to lifecycle. Calling bindToLifecycle here to make sure sizes matching to
        // default aspect ratio will be selected.
        FakeLifecycleOwner fakeLifecycle = new FakeLifecycleOwner();
        CameraX.bindToLifecycle(fakeLifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview,
                imageCapture, imageAnalysis);

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
    public void checkSmallSizesAreFilteredOutByDefaultSize480p() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
    public void checkAspectRatioMatchedSizeCanBeSelected() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        // Sets each of mSupportedSizes as target resolution and also sets target rotation as
        // Surface.ROTATION to make it aligns the sensor direction and then exactly the same size
        // will be selected as the result. This test can also verify that size smaller than
        // 640x480 can be selected after set as target resolution.
        for (Size targetResolution : mSupportedSizes) {
            ImageCapture imageCapture = new ImageCapture.Builder().setTargetResolution(
                    targetResolution).setTargetRotation(Surface.ROTATION_90).build();

            Map<UseCase, Size> suggestedResolutionMap =
                    supportedSurfaceCombination.getSuggestedResolutions(null,
                            Arrays.asList(imageCapture));

            assertEquals(targetResolution, suggestedResolutionMap.get(imageCapture));
        }
    }

    @Test
    public void checkCorrectAspectRatioNotMatchedSizeCanBeSelected() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        // Sets target resolution as 1200x720, all supported resolutions will be put into aspect
        // ratio not matched list. Then, 1280x720 will be the nearest matched one. Finally,
        // checks whether 1280x720 is selected or not.
        Size targetResolution = new Size(1200, 720);

        ImageCapture imageCapture = new ImageCapture.Builder().setTargetResolution(
                targetResolution).setTargetRotation(Surface.ROTATION_90).build();

        Map<UseCase, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(null,
                        Arrays.asList(imageCapture));

        assertEquals(new Size(1280, 720), suggestedResolutionMap.get(imageCapture));
    }


    @Test
    public void suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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

        assertTrue(hasMatchingAspectRatio(previewSize, ASPECT_RATIO_16_9));
        assertTrue(hasMatchingAspectRatio(imageCaptureSize, ASPECT_RATIO_16_9));
        assertTrue(hasMatchingAspectRatio(imageAnalysisSize, ASPECT_RATIO_16_9));
    }

    @Test
    public void throwsWhenSetBothTargetResolutionAndAspectRatioForDifferentUseCases() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ANALYSIS);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVNotSupportSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ANALYSIS);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGNotSupportSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
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
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        Size maximumYUVSize =
                supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.YUV_420_888);
        assertEquals(mMaximumSize, maximumYUVSize);
        Size maximumJPEGSize =
                supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG);
        assertEquals(mMaximumSize, maximumJPEGSize);
    }

    @Test
    public void isAspectRatioMatchWithSupportedMod16Resolution() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

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

    @Test
    public void sortByCompareSizesByArea_canSortSizesCorrectly() {
        Size[] sizes = new Size[mSupportedSizes.length];

        // Generates a unsorted array from mSupportedSizes.
        int centerIndex = mSupportedSizes.length / 2;
        // Puts 2nd half sizes in the front
        for (int i = centerIndex; i < mSupportedSizes.length; i++) {
            sizes[i - centerIndex] = mSupportedSizes[i];
        }
        // Puts 1st half sizes inversely in the tail
        for (int j = centerIndex - 1; j >= 0; j--) {
            sizes[mSupportedSizes.length - j - 1] = mSupportedSizes[j];
        }

        // The testing sizes array will be equal to mSupportedSizes after sorting.
        Arrays.sort(sizes, new SupportedSurfaceCombination.CompareSizesByArea(true));
        assertEquals(Arrays.asList(mSupportedSizes), Arrays.asList(sizes));
    }

    @Test
    public void getSupportedOutputSizes_noConfigSettings() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. No any aspect ratio related setting. The returned sizes list will be sorted in
        // descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(4032, 3024),
                new Size(3840, 2160),
                new Size(1920, 1440),
                new Size(1920, 1080),
                new Size(1280, 960),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatio4x3() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase =
                new FakeUseCaseConfig.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 4/3 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(4032, 3024),
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(3840, 2160),
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatio16x9() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase =
                new FakeUseCaseConfig.Builder().setTargetAspectRatio(
                        AspectRatio.RATIO_16_9).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(3840, 2160),
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(4032, 3024),
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolution1080x1920InRotation0() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetResolution(
                new Size(1080, 1920)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will be calibrated by default target rotation 0 degree. The target
        // resolution will also call setTargetAspectRatioCustom to set matching aspect ratio.
        // Therefore, sizes of aspect ratio 16/9 will be in front of the returned sizes list and
        // the list is sorted in descending order. Other items will be put in the following that
        // are sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolutionLargerThan640x480() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetRotation(
                Surface.ROTATION_90).setTargetResolution(new Size(1280, 960)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Target resolution larger than 640x480 won't overwrite
        // minimum size setting. Sizes smaller than 640x480 will be removed. The target
        // resolution will also call setTargetAspectRatioCustom to set matching aspect ratio.
        // Therefore, sizes of aspect ratio 4/3 will be in front of the returned sizes list and
        // the list is sorted in descending order. Other items will be put in the following that
        // are sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(1280, 960),
                new Size(640, 480),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolutionSmallerThan640x480() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetRotation(
                Surface.ROTATION_90).setTargetResolution(new Size(320, 240)).build();

        // Unnecessary big enough sizes will be removed from the result list. Minimum size will
        // be overwritten as 320x240. Sizes smaller than 320x240 will also be removed. The target
        // resolution will also call setTargetAspectRatioCustom to set matching aspect ratio.
        // Therefore, sizes of aspect ratio 4/3 will be in front of the returned sizes list and
        // the list is sorted in descending order. Other items will be put in the following that
        // are sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(320, 240),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(800, 450)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolution1800x1440NearTo4x3() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetRotation(
                Surface.ROTATION_90).setTargetResolution(new Size(1800, 1440)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will also call setTargetAspectRatioCustom to set matching aspect
        // ratio. Size 1800x1440 is near to 4/3, therefore, sizes of aspect ratio 4/3 will be in
        // front of the returned sizes list and the list is sorted in descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 4/3 are near to aspect ratio of 1800/1440
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480),

                // Sizes of 16/9 are far to aspect ratio of 1800/1440
                new Size(3840, 2160),
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom4x3_targetResolution1800x1440NearTo4x3() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetAspectRatioCustom(
                ASPECT_RATIO_4_3).setTargetRotation(Surface.ROTATION_90).setTargetResolution(
                new Size(1800, 1440)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will also call setTargetAspectRatioCustom to overwrite original target
        // aspect ratio. Size 1800x1440 is near to 4/3, therefore, sizes of aspect ratio 4/3 will
        // be in front of the returned sizes list and the list is sorted in descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 4/3 are near to aspect ratio of 1800/1440
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480),

                // Sizes of 16/9 are far to aspect ratio of 1800/1440
                new Size(3840, 2160),
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom16x9_targetResolution1800x1440NearTo4x3() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetAspectRatioCustom(
                ASPECT_RATIO_16_9).setTargetRotation(Surface.ROTATION_90).setTargetResolution(
                new Size(1800, 1440)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will also call setTargetAspectRatioCustom to overwrite original
        // target aspect ratio. Size 1800x1440 is near to 4/3, therefore, sizes of aspect ratio
        // 4/3 will be in front of the returned sizes list and the list is sorted in descending
        // order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 4/3 are near to aspect ratio of 1800/1440
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480),

                // Sizes of 16/9 are far to aspect ratio of 1800/1440
                new Size(3840, 2160),
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolution1280x600NearTo16x9() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetResolution(
                new Size(1280, 600)).setTargetRotation(Surface.ROTATION_90).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will also call setTargetAspectRatioCustom to set matching aspect
        // ratio. Size 1280x600 is near to 16/9, therefore, sizes of aspect ratio 16/9 will be in
        // front of the returned sizes list and the list is sorted in descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 16/9 are near to aspect ratio of 1280/600
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Sizes of 4/3 are far to aspect ratio of 1280/600
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom16x9_targetResolution1280x600NearTo16x9() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetAspectRatioCustom(
                ASPECT_RATIO_16_9).setTargetRotation(Surface.ROTATION_90).setTargetResolution(
                new Size(1280, 600)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will also call setTargetAspectRatioCustom to to overwrite original
        // target aspect ratio. Size 1280x600 is near to 16/9, therefore, sizes of aspect ratio
        // 16/9 will be in front of the returned sizes list and the list is sorted in descending
        // order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 16/9 are near to aspect ratio of 1280/600
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Sizes of 4/3 are far to aspect ratio of 1280/600
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom4x3_targetResolution1280x600NearTo16x9() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetAspectRatioCustom(
                ASPECT_RATIO_4_3).setTargetRotation(Surface.ROTATION_90).setTargetResolution(
                new Size(1280, 600)).build();

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will also call setTargetAspectRatioCustom to to overwrite original
        // target aspect ratio. Size 1280x600 is near to 16/9, therefore, sizes of aspect ratio
        // 16/9 will be in front of the returned sizes list and the list is sorted in descending
        // order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 16/9 are near to aspect ratio of 1280/600
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Sizes of 4/3 are far to aspect ratio of 1280/600
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_maxResolution1280x720() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase =
                new FakeUseCaseConfig.Builder().setMaxResolution(new Size(1280, 720)).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 or
        // larger than 1280x720 will be removed. The returned sizes list will be sorted in
        // descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_defaultResolution1280x720_noTargetResolution() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setDefaultResolution(new Size(1280,
                720)).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. If there is no target resolution setting, it will be overwritten by default
        // resolution as 1280x720. Unnecessary big enough sizes will also be removed. The
        // returned sizes list will be sorted in descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_defaultResolution1280x720_targetResolution1920x1080() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setDefaultResolution(
                new Size(1280, 720)).setTargetRotation(Surface.ROTATION_90).setTargetResolution(
                new Size(1920, 1080)).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. There is target resolution 1920x1080, it won't be overwritten by default
        // resolution 1280x720. Unnecessary big enough sizes will also be removed. Sizes of
        // aspect ratio 16/9 will be in front of the returned sizes list and the list is sorted
        // in descending order.  Other items will be put in the following that are sorted by
        // aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(1920, 1440),
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    @Test
    public void getSupportedOutputSizes_fallbackToGuaranteedResolution_whenNotFulfillConditions() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetResolution(
                new Size(1920, 1080)).setTargetRotation(Surface.ROTATION_90).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. There is target resolution 1920x1080 (16:9). Even 640x480 does not match 16:9
        // requirement, it will still be returned to use.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(useCase);
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(640, 480)
        });
        assertEquals(expectedList, resultList);
    }

    private void setupCamera(int hardwareLevel) {
        setupCamera(hardwareLevel, mSupportedSizes, null);
    }

    private void setupCamera(int hardwareLevel, int[] capabilities) {
        setupCamera(hardwareLevel, mSupportedSizes, capabilities);
    }

    private void setupCamera(int hardwareLevel, Size[] supportedSizes) {
        setupCamera(hardwareLevel, supportedSizes, null);
    }

    private void setupCamera(int hardwareLevel, Size[] supportedSizes, int[] capabilities) {
        mCameraFactory = new FakeCameraFactory();
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();

        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(
                CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK);

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
                .addCamera(CAMERA_ID, characteristics);

        int[] supportedFormats = isRawSupported(capabilities)
                ? mSupportedFormatsWithRaw : mSupportedFormats;

        shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapUtil.generateFakeStreamConfigurationMap(supportedFormats,
                        supportedSizes));

        @CameraSelector.LensFacing int lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
                CameraCharacteristics.LENS_FACING_BACK);

        mCameraFactory.insertCamera(lensFacingEnum, CAMERA_ID, () -> new FakeCamera(CAMERA_ID, null,
                new Camera2CameraInfoImpl(CAMERA_ID, characteristics, mock(ZoomControl.class),
                        mock(TorchControl.class))));

        initCameraX();
    }

    private void initCameraX() {
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                Camera2Config.defaultConfig())
                .setCameraFactoryProvider(ignored -> mCameraFactory)
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
