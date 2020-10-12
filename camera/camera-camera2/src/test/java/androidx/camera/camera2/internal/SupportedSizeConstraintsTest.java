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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.emory.mathcs.backport.java.util.Arrays;

/** Robolectric test for {@link SupportedSizeConstraints} class */
@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SupportedSizeConstraintsTest {
    private static final String BACK_CAMERA_ID = "0";
    private static final int DEFAULT_SENSOR_ORIENTATION = 90;
    private static final String TEST_BRAND_NAME = "OnePlus";
    private static final String TEST_DEVICE_NAME = "OnePlus6T";
    private final CamcorderProfileHelper mMockCamcorderProfileHelper =
            Mockito.mock(CamcorderProfileHelper.class);
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

    private final Size[] mSupportedSizes =
            new Size[]{
                    new Size(4160, 3120), // will be excluded by some OEM devices for JPEG format.
                    new Size(4032, 3024),
                    new Size(4000, 3000), // will be excluded by some OEM devices for JPEG format.
                    new Size(3840, 2160),
                    new Size(1920, 1440),
                    new Size(1920, 1080),
                    new Size(1280, 960),
                    new Size(1280, 720),
                    new Size(640, 480),
                    new Size(320, 240)
            };

    private final Context mContext = RuntimeEnvironment.application.getApplicationContext();
    private FakeCameraFactory mCameraFactory;

    @Before
    @SuppressWarnings("deprecation") /* defaultDisplay */
    public void setUp() {
        when(mMockCamcorderProfileHelper.hasProfile(anyInt(), anyInt())).thenReturn(true);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompat(String cameraId)
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                cameraManager.getCameraCharacteristics(cameraId));
    }

    @Test
    public void sizesCanBeExcluded() throws Exception {
        // Mock the environment to simulate a device that some supported sizes will be excluded.
        Map<Field, Object> fieldSettingsMap = new HashMap<>();
        fieldSettingsMap.put(Build.class.getField("BRAND"), TEST_BRAND_NAME);
        fieldSettingsMap.put(Build.class.getField("DEVICE"), TEST_DEVICE_NAME);
        setFakeBuildEnvironments(fieldSettingsMap);

        // Setup fake camera with supported sizes.
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, mSupportedSizes,
                null);
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(BACK_CAMERA_ID);

        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, BACK_CAMERA_ID, CameraManagerCompat.from(mContext),
                mMockCamcorderProfileHelper);

        List<Size> excludedSizes = Arrays.asList(
                new Size[]{new Size(4160, 3120), new Size(4000, 3000)});

        // Check the original mSupportedSizes contains the excluded sizes to avoid
        // mSupportedSizes modified unexpectedly.
        assertThat(Arrays.asList(mSupportedSizes).containsAll(excludedSizes)).isTrue();
        // Make the fake use case have JPEG format since those sizes are excluded for JPEG format.
        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setBufferFormat(
                ImageFormat.JPEG).build();
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getCurrentConfig());

        for (Size size : excludedSizes) {
            assertThat(resultList.contains(size)).isFalse();
        }
    }

    @Test
    public void sizesCanBeExcluded_withLowerCaseBrandDeviceName() throws Exception {
        // Mock the environment to simulate a device that some supported sizes will be excluded.
        Map<Field, Object> fieldSettingsMap = new HashMap<>();
        fieldSettingsMap.put(Build.class.getField("BRAND"), TEST_BRAND_NAME.toLowerCase());
        fieldSettingsMap.put(Build.class.getField("DEVICE"), TEST_DEVICE_NAME.toLowerCase());
        setFakeBuildEnvironments(fieldSettingsMap);
        // Setup fake camera with supported sizes.
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, mSupportedSizes,
                null);
        CameraCharacteristicsCompat characteristicsCompat =
                getCameraCharacteristicsCompat(BACK_CAMERA_ID);

        SupportedSurfaceCombination supportedSurfaceCombination = new SupportedSurfaceCombination(
                mContext, BACK_CAMERA_ID, CameraManagerCompat.from(mContext),
                mMockCamcorderProfileHelper);

        List<Size> excludedSizes = Arrays.asList(
                new Size[]{new Size(4160, 3120), new Size(4000, 3000)});

        // Check the original mSupportedSizes contains the excluded sizes to avoid
        // mSupportedSizes modified unexpectedly.
        assertThat(Arrays.asList(mSupportedSizes).containsAll(excludedSizes)).isTrue();
        // Make the fake use case have JPEG format since those sizes are excluded for JPEG format.
        FakeUseCase useCase = new FakeUseCaseConfig.Builder().setBufferFormat(
                ImageFormat.JPEG).build();
        List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getCurrentConfig());

        for (Size size : excludedSizes) {
            assertThat(resultList.contains(size)).isFalse();
        }
    }

    static void setFakeBuildEnvironments(@NonNull Map<Field, Object> fieldSettingsMap)
            throws Exception {
        for (Field field : fieldSettingsMap.keySet()) {
            field.setAccessible(true);
            field.set(null, fieldSettingsMap.get(field));
        }
    }

    private void setupCamera(int hardwareLevel, @NonNull Size[] supportedSizes,
            @Nullable int[] capabilities) {
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
                .addCamera(BACK_CAMERA_ID, characteristics);

        int[] supportedFormats = mSupportedFormats;

        StreamConfigurationMap mockMap = mock(StreamConfigurationMap.class);
        when(mockMap.getOutputSizes(anyInt())).thenReturn(supportedSizes);
        // ImageFormat.PRIVATE was supported since API level 23. Before that, the supported
        // output sizes need to be retrieved via SurfaceTexture.class.
        when(mockMap.getOutputSizes(SurfaceTexture.class)).thenReturn(mSupportedSizes);
        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap);

        @CameraSelector.LensFacing int lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
                CameraCharacteristics.LENS_FACING_BACK);

        mCameraFactory.insertCamera(lensFacingEnum, BACK_CAMERA_ID,
                () -> new FakeCamera(BACK_CAMERA_ID, null,
                        new Camera2CameraInfoImpl(BACK_CAMERA_ID,
                                getCameraCharacteristicsCompat(BACK_CAMERA_ID),
                                mock(Camera2CameraControlImpl.class))));
        initCameraX();
    }

    private void initCameraX() {
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                Camera2Config.defaultConfig())
                .setCameraFactoryProvider((ignored0, ignored1) -> mCameraFactory)
                .build();
        CameraX.initialize(mContext, cameraXConfig);
    }
}
