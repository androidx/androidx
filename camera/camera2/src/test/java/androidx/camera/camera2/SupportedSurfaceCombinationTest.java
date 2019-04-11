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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Rational;
import android.util.Size;
import android.view.WindowManager;

import androidx.camera.core.AppConfiguration;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfiguration;
import androidx.camera.core.ImageFormatConstants;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfiguration;
import androidx.camera.core.SurfaceCombination;
import androidx.camera.core.SurfaceConfiguration;
import androidx.camera.core.SurfaceConfiguration.ConfigurationSize;
import androidx.camera.core.SurfaceConfiguration.ConfigurationType;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfiguration;
import androidx.camera.testing.StreamConfigurationMapUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

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
import java.util.List;
import java.util.Map;

/** Robolectric test for {@link SupportedSurfaceCombination} class */
@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class SupportedSurfaceCombinationTest {
    private static final String LEGACY_CAMERA_ID = "0";
    private static final String LIMITED_CAMERA_ID = "1";
    private static final String FULL_CAMERA_ID = "2";
    private static final String LEVEL3_CAMERA_ID = "3";
    private static final int DEFAULT_SENSOR_ORIENTATION = 90;
    private final Size mDisplaySize = new Size(1280, 720);
    private final Size mAnalysisSize = new Size(640, 480);
    private final Size mPreviewSize = mDisplaySize;
    private final Size mRecordSize = new Size(3840, 2160);
    private final Size mMaximumSize = new Size(4032, 3024);
    private final Size mMaximumVideoSize = new Size(1920, 1080);
    private final CamcorderProfileHelper mMockCamcorderProfileHelper =
            Mockito.mock(CamcorderProfileHelper.class);

    /**
     * Except for ImageFormat.JPEG or ImageFormat.YUV, other image formats will be mapped to
     * ImageFormat.PRIVATE (0x22) including SurfaceTexture or MediaCodec classes. Before Android
     * level 23, there is no ImageFormat.PRIVATE. But there is same internal code 0x22 for internal
     * corresponding format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED. Therefore, set 0x22 as default
     * image formate.
     */
    private final int[] mSupportedFormats =
            new int[]{
                    ImageFormat.YUV_420_888,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_JPEG,
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            };

    private final Size[] mSupportedSizes =
            new Size[]{
                    new Size(4032, 3024),
                    new Size(3840, 2160),
                    new Size(1920, 1080),
                    new Size(1280, 720),
                    new Size(640, 480),
                    new Size(320, 240),
                    new Size(320, 180)
            };

    private final Context mContext = RuntimeEnvironment.application.getApplicationContext();

    @Before
    public void setUp() {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealWidth(mDisplaySize.getWidth());
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealHeight(mDisplaySize.getHeight());

        when(mMockCamcorderProfileHelper.hasProfile(anyInt(), anyInt())).thenReturn(true);

        setupCamera();
    }

    @Test
    public void checkLegacySurfaceCombinationSupportedInLegacyDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLegacySurfaceCombinationSubListSupportedInLegacyDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        boolean isSupported =
                isAllSubConfigurationListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLegacyDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLegacyDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSupportedInLimitedDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSubListSupportedInLimited3Device() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigurationListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLimitedDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationSupportedInFullDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationSubListSupportedInFullDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        boolean isSupported =
                isAllSubConfigurationListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSupportedInLevel3Device() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEVEL3_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    supportedSurfaceCombination.checkSupported(
                            combination.getSurfaceConfigurationList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSubListSupportedInLevel3Device() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEVEL3_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        boolean isSupported =
                isAllSubConfigurationListSupported(supportedSurfaceCombination, combinationList);
        assertTrue(isSupported);
    }

    @Test
    public void suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        Rational aspectRatio = new Rational(16, 9);
        PreviewConfiguration.Builder previewConfigBuilder = new PreviewConfiguration.Builder();
        VideoCaptureConfiguration.Builder videoCaptureConfigBuilder =
                new VideoCaptureConfiguration.Builder();
        ImageCaptureConfiguration.Builder imageCaptureConfigBuilder =
                new ImageCaptureConfiguration.Builder();

        previewConfigBuilder.setTargetAspectRatio(aspectRatio);
        videoCaptureConfigBuilder.setTargetAspectRatio(aspectRatio);
        imageCaptureConfigBuilder.setTargetAspectRatio(aspectRatio);

        imageCaptureConfigBuilder.setLensFacing(LensFacing.BACK);
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());
        videoCaptureConfigBuilder.setLensFacing(LensFacing.BACK);
        VideoCapture videoCapture = new VideoCapture(videoCaptureConfigBuilder.build());
        previewConfigBuilder.setLensFacing(LensFacing.BACK);
        Preview preview = new Preview(previewConfigBuilder.build());

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
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        Rational aspectRatio = new Rational(16, 9);
        PreviewConfiguration.Builder previewConfigBuilder = new PreviewConfiguration.Builder();
        VideoCaptureConfiguration.Builder videoCaptureConfigBuilder =
                new VideoCaptureConfiguration.Builder();
        ImageCaptureConfiguration.Builder imageCaptureConfigBuilder =
                new ImageCaptureConfiguration.Builder();

        previewConfigBuilder.setTargetAspectRatio(aspectRatio);
        videoCaptureConfigBuilder.setTargetAspectRatio(aspectRatio);
        imageCaptureConfigBuilder.setTargetAspectRatio(aspectRatio);

        imageCaptureConfigBuilder.setLensFacing(LensFacing.BACK);
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfigBuilder.build());
        videoCaptureConfigBuilder.setLensFacing(LensFacing.BACK);
        VideoCapture videoCapture = new VideoCapture(videoCaptureConfigBuilder.build());
        previewConfigBuilder.setLensFacing(LensFacing.BACK);
        Preview preview = new Preview(previewConfigBuilder.build());

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
    public void transformSurfaceConfigurationWithYUVAnalysisSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.YUV_420_888, mAnalysisSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.YUV, ConfigurationSize.ANALYSIS);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithYUVPreviewSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.YUV_420_888, mPreviewSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.YUV, ConfigurationSize.PREVIEW);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithYUVRecordSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.YUV_420_888, mRecordSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.YUV, ConfigurationSize.RECORD);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithYUVMaximumSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.YUV_420_888, mMaximumSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.YUV, ConfigurationSize.MAXIMUM);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithYUVNotSupportSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.YUV_420_888,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.YUV, ConfigurationSize.NOT_SUPPORT);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithJPEGAnalysisSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.JPEG, mAnalysisSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.JPEG, ConfigurationSize.ANALYSIS);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithJPEGPreviewSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.JPEG, mPreviewSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.JPEG, ConfigurationSize.PREVIEW);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithJPEGRecordSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.JPEG, mRecordSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.JPEG, ConfigurationSize.RECORD);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithJPEGMaximumSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.JPEG, mMaximumSize);
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.JPEG, ConfigurationSize.MAXIMUM);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void transformSurfaceConfigurationWithJPEGNotSupportSize() {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);
        SurfaceConfiguration surfaceConfiguration =
                supportedSurfaceCombination.transformSurfaceConfiguration(
                        ImageFormat.JPEG,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfiguration expectedSurfaceConfiguration =
                SurfaceConfiguration.create(ConfigurationType.JPEG, ConfigurationSize.NOT_SUPPORT);
        assertEquals(expectedSurfaceConfiguration, surfaceConfiguration);
    }

    @Test
    public void getMaximumSizeForImageFormat() {
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

    private void setupCamera() {
        addBackFacingCamera(
                LEGACY_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, null);
        addBackFacingCamera(
                LIMITED_CAMERA_ID,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                null);
        addBackFacingCamera(
                FULL_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, null);
        addBackFacingCamera(
                LEVEL3_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3, null);
        initCameraX();
    }

    private void addBackFacingCamera(String cameraId, int hardwareLevel, int[] capabilities) {
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

        ((ShadowCameraManager) Shadow.extract(
                ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE)))
                .addCamera(cameraId, characteristics);

        shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapUtil.generateFakeStreamConfigurationMap(
                        mSupportedFormats, mSupportedSizes));
    }

    private void initCameraX() {
        AppConfiguration appConfig = Camera2AppConfiguration.create(mContext);
        CameraX.init(mContext, appConfig);
    }

    private boolean isAllSubConfigurationListSupported(
            SupportedSurfaceCombination supportedSurfaceCombination,
            List<SurfaceCombination> combinationList) {
        boolean isSupported = true;

        for (SurfaceCombination combination : combinationList) {
            List<SurfaceConfiguration> configurationList =
                    combination.getSurfaceConfigurationList();
            int length = configurationList.size();

            if (length <= 1) {
                continue;
            }

            for (int index = 0; index < length; index++) {
                List<SurfaceConfiguration> subConfigurationList = new ArrayList<>();
                subConfigurationList.addAll(configurationList);
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
