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
import androidx.camera.core.CameraUnavailableException;
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
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.Configs;
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
import java.util.Collections;
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
    public void checkLegacySurfaceCombinationSupportedInLegacyDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkLegacySurfaceCombinationSubListSupportedInLegacyDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertThat(isSupported).isTrue();
    }

    @Test
    public void checkLimitedSurfaceCombinationNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isFalse();
        }
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isFalse();
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isFalse();
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSupportedInLimitedDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSubListSupportedInLimited3Device()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertThat(isSupported).isTrue();
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLimitedDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isFalse();
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLimitedDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isFalse();
        }
    }

    @Test
    public void checkFullSurfaceCombinationSupportedInFullDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkFullSurfaceCombinationSubListSupportedInFullDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertThat(isSupported).isTrue();
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInFullDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isFalse();
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSupportedInRawDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkLegacySurfaceCombinationSupportedInRawDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkFullSurfaceCombinationSupportedInRawDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkRawSurfaceCombinationSupportedInRawDevice()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                new int[]{CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW});
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getRAWSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSupportedInLevel3Device()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(combination.getSurfaceConfigList());
            assertThat(isSupported).isTrue();
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSubListSupportedInLevel3Device()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        boolean isSupported =
                isAllSubConfigListSupported(supportedSurfaceCombination, combinationList);
        assertThat(isSupported).isTrue();
    }

    @Test
    public void checkTargetAspectRatioForPreviewInLegacyDevice()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));
        Size previewSize = suggestedResolutionMap.get(preview.getUseCaseConfig());
        Rational resultAspectRatio = new Rational(previewSize.getHeight(), previewSize.getWidth());

        if (Build.VERSION.SDK_INT == 21) {
            // Checks targetAspectRatio and maxJpegAspectRatio, which is the ratio of maximum size
            // in the mSupportedSizes, are not equal to make sure this test case is valid.
            assertThat(targetAspectRatio).isNotEqualTo(maxJpegAspectRatio);
            assertThat(previewAspectRatio).isEqualTo(maxJpegAspectRatio);
            assertThat(correctedAspectRatio).isEqualTo(maxJpegAspectRatio);
            assertThat(resultAspectRatio).isEqualTo(maxJpegAspectRatio);
        } else {
            // Checks no correction is needed.
            assertThat(correctedAspectRatio).isNull();
            assertThat(resultAspectRatio).isEqualTo(targetAspectRatio);
        }
    }

    @Test
    public void checkDefaultAspectRatioAndResolutionForMixedUseCase()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        Size previewSize = suggestedResolutionMap.get(preview.getUseCaseConfig());
        Size imageCaptureSize = suggestedResolutionMap.get(imageCapture.getUseCaseConfig());
        Size imageAnalysisSize = suggestedResolutionMap.get(imageAnalysis.getUseCaseConfig());

        Rational previewAspectRatio = new Rational(previewSize.getWidth(), previewSize.getHeight());
        Rational imageCaptureAspectRatio = new Rational(imageCaptureSize.getWidth(),
                imageCaptureSize.getHeight());
        Rational imageAnalysisAspectRatio = new Rational(imageAnalysisSize.getWidth(),
                imageAnalysisSize.getHeight());

        // Checks the default aspect ratio.
        assertThat(previewAspectRatio).isEqualTo(ASPECT_RATIO_4_3);
        assertThat(imageCaptureAspectRatio).isEqualTo(ASPECT_RATIO_4_3);
        assertThat(imageAnalysisAspectRatio).isEqualTo(ASPECT_RATIO_4_3);

        // Checks the default resolution.
        assertThat(imageAnalysisSize).isEqualTo(mAnalysisSize);
    }

    @Test
    public void checkSmallSizesAreFilteredOutByDefaultSize480p()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        // Checks the preconditions.
        final Size preconditionSize = new Size(256, 144);
        final Rational targetRatio = new Rational(displayHeight, displayWidth);
        ArrayList<Size> sizeList = new ArrayList<>(Arrays.asList(mSupportedSizes));
        assertThat(sizeList).contains(preconditionSize);
        for (Size s : mSupportedSizes) {
            Rational supportedRational = new Rational(s.getWidth(), s.getHeight());
            assertThat(supportedRational).isNotEqualTo(targetRatio);
        }

        // Checks the mechanism has filtered out the sizes which are smaller than default size 480p.
        Size previewSize = suggestedResolutionMap.get(preview.getUseCaseConfig());
        assertThat(previewSize).isNotEqualTo(preconditionSize);
    }

    @Test
    public void checkAspectRatioMatchedSizeCanBeSelected() throws CameraUnavailableException {
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

            Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                    supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                            Collections.singletonList(imageCapture.getUseCaseConfig()));

            assertThat(targetResolution).isEqualTo(
                    suggestedResolutionMap.get(imageCapture.getUseCaseConfig()));
        }
    }

    @Test
    public void checkCorrectAspectRatioNotMatchedSizeCanBeSelected()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        // Sets target resolution as 1200x720, all supported resolutions will be put into aspect
        // ratio not matched list. Then, 1280x720 will be the nearest matched one. Finally,
        // checks whether 1280x720 is selected or not.
        Size targetResolution = new Size(1200, 720);

        ImageCapture imageCapture = new ImageCapture.Builder().setTargetResolution(
                targetResolution).setTargetRotation(Surface.ROTATION_90).build();

        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Arrays.asList(imageCapture.getUseCaseConfig()));

        assertThat(new Size(1280, 720)).isEqualTo(
                suggestedResolutionMap.get(imageCapture.getUseCaseConfig()));
    }


    @Test
    public void suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        assertThat(suggestedResolutionMap).isNotEqualTo(3);
    }

    @Test
    public void getSuggestedResolutionsForMixedUseCaseInLimitedDevice()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        assertThat(suggestedResolutionMap).containsEntry(imageCapture.getUseCaseConfig(),
                mRecordSize);
        assertThat(suggestedResolutionMap).containsEntry(videoCapture.getUseCaseConfig(),
                mMaximumVideoSize);
        assertThat(suggestedResolutionMap).containsEntry(preview.getUseCaseConfig(), mPreviewSize);
    }

    @Test
    public void getSuggestedResolutionsWithSameSupportedListForDifferentUseCases()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        assertThat(suggestedResolutionMap).containsEntry(imageCapture.getUseCaseConfig(),
                mPreviewSize);
        assertThat(suggestedResolutionMap).containsEntry(preview.getUseCaseConfig(), mPreviewSize);
        assertThat(suggestedResolutionMap).containsEntry(imageAnalysis.getUseCaseConfig(),
                mPreviewSize);
    }

    @Test
    public void setTargetAspectRatioForMixedUseCases() throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        Size previewSize = suggestedResolutionMap.get(preview.getUseCaseConfig());
        Size imageCaptureSize = suggestedResolutionMap.get(imageCapture.getUseCaseConfig());
        Size imageAnalysisSize = suggestedResolutionMap.get(imageAnalysis.getUseCaseConfig());

        assertThat(hasMatchingAspectRatio(previewSize, ASPECT_RATIO_16_9)).isTrue();
        assertThat(hasMatchingAspectRatio(imageCaptureSize, ASPECT_RATIO_16_9)).isTrue();
        assertThat(hasMatchingAspectRatio(imageAnalysisSize, ASPECT_RATIO_16_9)).isTrue();
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
        assertThat(previewExceptionHappened).isTrue();

        boolean imageCaptureExceptionHappened = false;
        ImageCapture.Builder imageCaptureConfigBuilder = new ImageCapture.Builder()
                .setTargetResolution(mDisplaySize)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);
        try {
            imageCaptureConfigBuilder.build();
        } catch (IllegalArgumentException e) {
            imageCaptureExceptionHappened = true;
        }
        assertThat(imageCaptureExceptionHappened).isTrue();

        boolean imageAnalysisExceptionHappened = false;
        ImageAnalysis.Builder imageAnalysisConfigBuilder = new ImageAnalysis.Builder()
                .setTargetResolution(mDisplaySize)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);
        try {
            imageAnalysisConfigBuilder.build();
        } catch (IllegalArgumentException e) {
            imageAnalysisExceptionHappened = true;
        }
        assertThat(imageAnalysisExceptionHappened).isTrue();
    }

    @Test
    public void getSuggestedResolutionsForCustomizedSupportedResolutions()
            throws CameraUnavailableException {
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
        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        // Checks all suggested resolutions will become 640x480.
        assertThat(suggestedResolutionMap).containsEntry(imageCapture.getUseCaseConfig(),
                mAnalysisSize);
        assertThat(suggestedResolutionMap).containsEntry(videoCapture.getUseCaseConfig(),
                mAnalysisSize);
        assertThat(suggestedResolutionMap).containsEntry(preview.getUseCaseConfig(), mAnalysisSize);
    }

    @Test
    public void transformSurfaceConfigWithYUVAnalysisSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ANALYSIS);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVPreviewSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVRecordSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVMaximumSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVNotSupportSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.YUV_420_888,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.NOT_SUPPORT);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGAnalysisSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ANALYSIS);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGPreviewSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGRecordSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGMaximumSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGNotSupportSize() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfig surfaceConfig =
                supportedSurfaceCombination.transformSurfaceConfig(
                        ImageFormat.JPEG,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.NOT_SUPPORT);
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig);
    }

    @Test
    public void getMaximumSizeForImageFormat() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);
        Size maximumYUVSize =
                supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.YUV_420_888);
        assertThat(maximumYUVSize).isEqualTo(mMaximumSize);
        Size maximumJPEGSize =
                supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG);
        assertThat(maximumJPEGSize).isEqualTo(mMaximumSize);
    }

    @Test
    public void isAspectRatioMatchWithSupportedMod16Resolution()
            throws CameraUnavailableException {
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

        Map<UseCaseConfig<?>, Size> suggestedResolutionMap =
                supportedSurfaceCombination.getSuggestedResolutions(Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        assertThat(suggestedResolutionMap).containsEntry(preview.getUseCaseConfig(), mMod16Size);
        assertThat(suggestedResolutionMap).containsEntry(imageCapture.getUseCaseConfig(),
                mMod16Size);
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
        assertThat(Arrays.asList(sizes)).isEqualTo(Arrays.asList(mSupportedSizes));
    }

    @Test
    public void getSupportedOutputSizes_noConfigSettings() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. No any aspect ratio related setting. The returned sizes list will be sorted in
        // descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatio4x3() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase =
                new FakeUseCaseConfig.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 4/3 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatio16x9() throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolution1080x1920InRotation0()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolutionLargerThan640x480()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolutionSmallerThan640x480()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(320, 240),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(800, 450)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolution1800x1440NearTo4x3()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom4x3_targetResolution1800x1440NearTo4x3()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom16x9_targetResolution1800x1440NearTo4x3()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_targetResolution1280x600NearTo16x9()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 16/9 are near to aspect ratio of 1280/600
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Sizes of 4/3 are far to aspect ratio of 1280/600
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom16x9_targetResolution1280x600NearTo16x9()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 16/9 are near to aspect ratio of 1280/600
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Sizes of 4/3 are far to aspect ratio of 1280/600
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_aspectRatioCustom4x3_targetResolution1280x600NearTo16x9()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Sizes of 16/9 are near to aspect ratio of 1280/600
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Sizes of 4/3 are far to aspect ratio of 1280/600
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_maxResolution1280x720() throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase =
                new FakeUseCaseConfig.Builder().setMaxResolution(new Size(1280, 720)).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 or
        // larger than 1280x720 will be removed. The returned sizes list will be sorted in
        // descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_defaultResolution1280x720_noTargetResolution()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setDefaultResolution(new Size(1280,
                720)).build();

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. If there is no target resolution setting, it will be overwritten by default
        // resolution as 1280x720. Unnecessary big enough sizes will also be removed. The
        // returned sizes list will be sorted in descending order.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_defaultResolution1280x720_targetResolution1920x1080()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
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
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_fallbackToGuaranteedResolution_whenNotFulfillConditions()
            throws CameraUnavailableException {
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
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_whenMaxSizeSmallerThanDefaultMiniSize()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setMaxResolution(
                new Size(320, 240)).build();

        // There is default minimum size 640x480 setting. Originally, sizes smaller than 640x480
        // will be removed. Due to maximal size bound is smaller than the default minimum size
        // bound and it is also smaller than 640x480, the default minimum size bound will be
        // ignored. Then, sizes equal to or smaller than 320x240 will be kept in the result list.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_whenMaxSizeSmallerThanSmallTargetResolution()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setMaxResolution(
                new Size(320, 180)).setTargetResolution(new Size(320, 240)).setTargetRotation(
                Surface.ROTATION_90).build();

        // The default minimum size 640x480 will be overwritten by the target resolution 320x240.
        // Originally, sizes smaller than 320x240 will be removed. Due to maximal size bound is
        // smaller than the minimum size bound and it is also smaller than 640x480, the minimum
        // size bound will be ignored. Then, sizes equal to or smaller than 320x180 will be kept
        // in the result list.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(320, 180),
                new Size(256, 144)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_whenBothMaxAndTargetResolutionsSmallerThan640x480()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setMaxResolution(
                new Size(320, 240)).setTargetResolution(new Size(320, 180)).setTargetRotation(
                Surface.ROTATION_90).build();

        // The default minimum size 640x480 will be overwritten by the target resolution 320x180.
        // Originally, sizes smaller than 320x180 will be removed. Due to maximal size bound is
        // smaller than the minimum size bound and it is also smaller than 640x480, the minimum
        // size bound will be ignored. Then, all sizes equal to or smaller than 320x320 will be
        // kept in the result list.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(320, 180),
                new Size(256, 144),
                new Size(320, 240)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_whenMaxSizeSmallerThanBigTargetResolution()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setMaxResolution(
                new Size(1920, 1080)).setTargetResolution(new Size(3840, 2160)).setTargetRotation(
                Surface.ROTATION_90).build();

        // Because the target size 3840x2160 is larger than 640x480, it won't overwrite the
        // default minimum size 640x480. Sizes smaller than 640x480 will be removed. The target
        // resolution will also call setTargetAspectRatioCustom to set matching aspect ratio.
        // Therefore, sizes of aspect ratio 16/9 will be in front of the returned sizes list and
        // the list is sorted in descending order. Other items will be put in the following that
        // are sorted by aspect ratio delta and then area size.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                // Matched AspectRatio items, sorted by area size.
                new Size(1920, 1080),
                new Size(1280, 720),
                new Size(960, 544),
                new Size(800, 450),

                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                new Size(1280, 960),
                new Size(640, 480)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_whenNoSizeBetweenMaxSizeAndTargetResolution()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setMaxResolution(
                new Size(320, 200)).setTargetResolution(new Size(320, 190)).setTargetRotation(
                Surface.ROTATION_90).build();

        // The default minimum size 640x480 will be overwritten by the target resolution 320x190.
        // Originally, sizes smaller than 320x190 will be removed. Due to there is no available
        // size between the maximal size and the minimum size bound and the maximal size is
        // smaller than 640x480, the default minimum size bound will be ignored. Then, sizes
        // equal to or smaller than 320x200 will be kept in the result list.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(320, 180),
                new Size(256, 144)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test
    public void getSupportedOutputSizes_whenTargetResolutionSmallerThanAnySize()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetResolution(
                new Size(192, 144)).setTargetRotation(Surface.ROTATION_90).build();

        // The default minimum size 640x480 will be overwritten by the target resolution 192x144.
        // Because 192x144 is smaller than any size in the supported list, no one will be
        // filtered out by it. The result list will only keep one big enough size of aspect ratio
        // 4:3 and 16:9.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(320, 240),
                new Size(256, 144)
        });
        assertThat(resultList).isEqualTo(expectedList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getSupportedOutputSizes_whenMaxResolutionSmallerThanAnySize()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setMaxResolution(
                new Size(192, 144)).build();

        // All sizes will be filtered out by the max resolution 192x144 setting and an
        // IllegalArgumentException will be thrown.
        supportedSurfaceCombination.getSupportedOutputSizes(useCase.getUseCaseConfig());
    }

    @Test
    public void getSupportedOutputSizes_whenMod16IsIgnoredForSmallSizes()
            throws CameraUnavailableException {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, new Size[]{
                new Size(640, 480),
                new Size(320, 240),
                new Size(320, 180),
                new Size(296, 144),
                new Size(256, 144)
        });
        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, CAMERA_ID, mMockCamcorderProfileHelper);

        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setTargetResolution(
                new Size(185, 90)).setTargetRotation(Surface.ROTATION_90).build();

        // The default minimum size 640x480 will be overwritten by the target resolution 185x90
        // (18.5:9). If mod 16 calculation is not ignored for the sizes smaller than 640x480, the
        // size 256x144 will be considered to match 18.5:9 and then become the first item in the
        // result list. After ignoring mod 16 calculation for small sizes, 256x144 will still be
        // kept as a 16:9 resolution as the result.
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getUseCaseConfig());
        List<Size> expectedList = Arrays.asList(new Size[]{
                new Size(296, 144),
                new Size(256, 144),
                new Size(320, 240)
        });
        assertThat(resultList).isEqualTo(expectedList);
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
                new Camera2CameraInfoImpl(CAMERA_ID, characteristics,
                        mock(Camera2CameraControl.class))));

        initCameraX();
    }

    private void initCameraX() {
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                Camera2Config.defaultConfig())
                .setCameraFactoryProvider((ignored0, ignored1) -> mCameraFactory)
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
