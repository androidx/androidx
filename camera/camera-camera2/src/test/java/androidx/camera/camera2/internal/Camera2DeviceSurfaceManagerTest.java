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

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.InitializationException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.core.impl.ExtendableUseCaseConfigFactory;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.SurfaceConfig.ConfigSize;
import androidx.camera.core.impl.SurfaceConfig.ConfigType;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.Configs;
import androidx.camera.testing.StreamConfigurationMapUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Robolectric test for {@link Camera2DeviceSurfaceManager} class */
@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP,
        maxSdk = Build.VERSION_CODES.P //TODO (b/149669465) : Some robolectric tests will fail on Q
)
public final class Camera2DeviceSurfaceManagerTest {
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

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CameraDeviceSurfaceManager mSurfaceManager;
    private FakeCameraFactory mCameraFactory;

    @Before
    public void setUp() {
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealWidth(mDisplaySize.getWidth());
        Shadows.shadowOf(windowManager.getDefaultDisplay()).setRealHeight(mDisplaySize.getHeight());

        when(mMockCamcorderProfileHelper.hasProfile(anyInt(), anyInt())).thenReturn(true);

        setupCamera();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }

    @Test
    public void checkLegacySurfaceCombinationSupportedInLegacyDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLegacySupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LEGACY_CAMERA_ID, combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LEGACY_CAMERA_ID, combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LEGACY_CAMERA_ID, combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLegacyDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LEGACY_CAMERA_ID, combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLimitedSurfaceCombinationSupportedInLimitedDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLimitedSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LIMITED_CAMERA_ID, combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationNotSupportedInLimitedDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LIMITED_CAMERA_ID, combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInLimitedDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LIMITED_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LIMITED_CAMERA_ID, combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkFullSurfaceCombinationSupportedInFullDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getFullSupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            FULL_CAMERA_ID, combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationNotSupportedInFullDevice()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, FULL_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            FULL_CAMERA_ID, combination.getSurfaceConfigList());
            assertFalse(isSupported);
        }
    }

    @Test
    public void checkLevel3SurfaceCombinationSupportedInLevel3Device()
            throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEVEL3_CAMERA_ID, mMockCamcorderProfileHelper);

        List<SurfaceCombination> combinationList =
                supportedSurfaceCombination.getLevel3SupportedCombinationList();

        for (SurfaceCombination combination : combinationList) {
            boolean isSupported =
                    mSurfaceManager.checkSupported(
                            LEVEL3_CAMERA_ID, combination.getSurfaceConfigList());
            assertTrue(isSupported);
        }
    }

    @Test
    public void suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice() {
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

        boolean exceptionHappened = false;

        try {
            // Will throw IllegalArgumentException
            mSurfaceManager.getSuggestedResolutions(LEGACY_CAMERA_ID, Collections.emptyList(),
                    Configs.useCaseConfigListFromUseCaseList(useCases));
        } catch (IllegalArgumentException e) {
            exceptionHappened = true;
        }

        assertTrue(exceptionHappened);
    }

    @Test
    public void getSuggestedResolutionsForMixedUseCaseInLimitedDevice() {
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
                mSurfaceManager.getSuggestedResolutions(LIMITED_CAMERA_ID, Collections.emptyList(),
                        Configs.useCaseConfigListFromUseCaseList(useCases));

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        assertThat(suggestedResolutionMap).containsEntry(imageCapture.getUseCaseConfig(),
                mRecordSize);
        assertThat(suggestedResolutionMap).containsEntry(videoCapture.getUseCaseConfig(),
                mMaximumVideoSize);
        assertThat(suggestedResolutionMap).containsEntry(preview.getUseCaseConfig(), mPreviewSize);
    }

    @Test
    public void transformSurfaceConfigWithYUVAnalysisSize() {
        SurfaceConfig surfaceConfig = mSurfaceManager.transformSurfaceConfig(
                LEGACY_CAMERA_ID, ImageFormat.YUV_420_888, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.ANALYSIS);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVPreviewSize() {
        SurfaceConfig surfaceConfig = mSurfaceManager.transformSurfaceConfig(
                LEGACY_CAMERA_ID, ImageFormat.YUV_420_888, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVRecordSize() {
        SurfaceConfig surfaceConfig = mSurfaceManager.transformSurfaceConfig(
                LEGACY_CAMERA_ID, ImageFormat.YUV_420_888, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, SurfaceConfig.ConfigSize.RECORD);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVMaximumSize() {
        SurfaceConfig surfaceConfig = mSurfaceManager.transformSurfaceConfig(
                LEGACY_CAMERA_ID, ImageFormat.YUV_420_888, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, ConfigSize.MAXIMUM);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithYUVNotSupportSize() {
        SurfaceConfig surfaceConfig = mSurfaceManager.transformSurfaceConfig(
                LEGACY_CAMERA_ID,
                ImageFormat.YUV_420_888,
                new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.NOT_SUPPORT);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGAnalysisSize() {
        SurfaceConfig surfaceConfig =
                mSurfaceManager.transformSurfaceConfig(
                        LEGACY_CAMERA_ID, ImageFormat.JPEG, mAnalysisSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, ConfigSize.ANALYSIS);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGPreviewSize() {
        SurfaceConfig surfaceConfig =
                mSurfaceManager.transformSurfaceConfig(
                        LEGACY_CAMERA_ID, ImageFormat.JPEG, mPreviewSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGRecordSize() {
        SurfaceConfig surfaceConfig =
                mSurfaceManager.transformSurfaceConfig(
                        LEGACY_CAMERA_ID, ImageFormat.JPEG, mRecordSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGMaximumSize() {
        SurfaceConfig surfaceConfig =
                mSurfaceManager.transformSurfaceConfig(
                        LEGACY_CAMERA_ID, ImageFormat.JPEG, mMaximumSize);
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void transformSurfaceConfigWithJPEGNotSupportSize() {
        SurfaceConfig surfaceConfig =
                mSurfaceManager.transformSurfaceConfig(
                        LEGACY_CAMERA_ID,
                        ImageFormat.JPEG,
                        new Size(mMaximumSize.getWidth() + 1, mMaximumSize.getHeight() + 1));
        SurfaceConfig expectedSurfaceConfig =
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.NOT_SUPPORT);
        assertEquals(expectedSurfaceConfig, surfaceConfig);
    }

    @Test
    public void getMaximumSizeForImageFormat() {
        Size maximumYUVSize =
                mSurfaceManager.getMaxOutputSize(LEGACY_CAMERA_ID, ImageFormat.YUV_420_888);
        assertEquals(mMaximumSize, maximumYUVSize);
        Size maximumJPEGSize = mSurfaceManager.getMaxOutputSize(LEGACY_CAMERA_ID, ImageFormat.JPEG);
        assertEquals(mMaximumSize, maximumJPEGSize);
    }

    @Test
    public void checkPreviewAspectRatioInLegacyDevice() throws CameraUnavailableException {
        SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(
                        mContext, LEGACY_CAMERA_ID, mMockCamcorderProfileHelper);

        Rational targetAspectRatio = new Rational(9, 16);
        PreviewConfig previewConfig = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .getUseCaseConfig();

        Rational resultAspectRatio = mSurfaceManager.getCorrectedAspectRatio(LEGACY_CAMERA_ID,
                previewConfig.getTargetRotation(Surface.ROTATION_0));

        Size maxJpegSize = supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG);
        Rational maxJpegAspectRatio = new Rational(maxJpegSize.getHeight(), maxJpegSize.getWidth());

        if (Build.VERSION.SDK_INT == 21) {
            // Checks targetAspectRatio and maxJpegAspectRatio, which is the ratio of maximum size
            // in the mSupportedSizes, are not equal to make sure this test case is valid.
            assertFalse(targetAspectRatio.equals(maxJpegAspectRatio));
            assertTrue(resultAspectRatio.equals(maxJpegAspectRatio));
        } else {
            // Checks no correction is needed.
            assertThat(resultAspectRatio).isNull();
        }
    }

    private void setupCamera() {
        mCameraFactory = new FakeCameraFactory();

        addCamera(
                LEGACY_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, null,
                CameraCharacteristics.LENS_FACING_FRONT);

        addCamera(
                LIMITED_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                null,
                CameraCharacteristics.LENS_FACING_BACK);

        addCamera(
                FULL_CAMERA_ID, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, null,
                CameraCharacteristics.LENS_FACING_BACK);
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

        shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapUtil.generateFakeStreamConfigurationMap(
                        mSupportedFormats, mSupportedSizes));

        @CameraSelector.LensFacing int lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
                lensFacing);
        mCameraFactory.insertCamera(lensFacingEnum, cameraId, () -> new FakeCamera(cameraId, null,
                new Camera2CameraInfoImpl(cameraId, characteristics,
                        mock(Camera2CameraControl.class))));
    }

    private void initCameraX() {
        CameraXConfig cameraXConfig = createFakeAppConfig();
        CameraX.initialize(mContext, cameraXConfig);
        mSurfaceManager = CameraX.getSurfaceManager();
    }

    private CameraXConfig createFakeAppConfig() {

        // Create the DeviceSurfaceManager for Camera2
        CameraDeviceSurfaceManager.Provider surfaceManagerProvider = context -> {
            try {
                return new Camera2DeviceSurfaceManager(mContext, mMockCamcorderProfileHelper);
            } catch (CameraUnavailableException e) {
                throw new InitializationException(e);
            }
        };

        // Create default configuration factory
        UseCaseConfigFactory.Provider factoryProvider = context -> {
            ExtendableUseCaseConfigFactory configFactory = new ExtendableUseCaseConfigFactory();
            configFactory.installDefaultProvider(
                    ImageAnalysisConfig.class, new ImageAnalysisConfigProvider(context));
            configFactory.installDefaultProvider(
                    ImageCaptureConfig.class, new ImageCaptureConfigProvider(context));
            configFactory.installDefaultProvider(
                    VideoCaptureConfig.class, new VideoCaptureConfigProvider(context));
            configFactory.installDefaultProvider(
                    PreviewConfig.class, new PreviewConfigProvider(context));
            return configFactory;
        };

        CameraXConfig.Builder appConfigBuilder =
                new CameraXConfig.Builder()
                        .setCameraFactoryProvider((ignored0, ignored1) -> mCameraFactory)
                        .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
                        .setUseCaseConfigFactoryProvider(factoryProvider);

        return appConfigBuilder.build();
    }
}
