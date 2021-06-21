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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.CameraManagerCompat;
import androidx.camera.camera2.internal.compat.workaround.ExcludedSupportedSizesContainer;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.InitializationException;
import androidx.camera.core.impl.CameraDeviceSurfaceManager;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.core.app.ApplicationProvider;

import org.apache.maven.artifact.ant.shaded.ReflectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;
import org.robolectric.shadows.ShadowCameraManager;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Unit tests for {@link ExcludedSupportedSizesContainer}'s usage within
 * {@link SupportedSurfaceCombination}.
 */
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
    private final CamcorderProfile mMockCamcorderProfile = Mockito.mock(CamcorderProfile.class);

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

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final CameraManagerCompat mCameraManager = CameraManagerCompat.from(mContext);

    @Before
    public void setUp() throws IllegalAccessException {
        when(mMockCamcorderProfileHelper.hasProfile(anyInt(), anyInt())).thenReturn(true);
        ReflectionUtils.setVariableValueInObject(mMockCamcorderProfile, "videoFrameWidth", 3840);
        ReflectionUtils.setVariableValueInObject(mMockCamcorderProfile, "videoFrameHeight", 2160);
        when(mMockCamcorderProfileHelper.get(anyInt(), anyInt())).thenReturn(mMockCamcorderProfile);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void sizesCanBeExcluded_withProperCaseBrandAndDeviceName() throws Exception {
        sizesCanBeExcluded(TEST_BRAND_NAME, TEST_DEVICE_NAME);
    }

    @Test
    public void sizesCanBeExcluded_withLowerCaseBrandAndDeviceName() throws Exception {
        sizesCanBeExcluded(TEST_BRAND_NAME.toLowerCase(), TEST_DEVICE_NAME.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    private void sizesCanBeExcluded(@NonNull String brand, @NonNull String device)
            throws Exception {
        // Mock the environment to simulate a device that some supported sizes will be excluded.
        Map<Field, Object> fieldSettingsMap = new HashMap<>();
        fieldSettingsMap.put(Build.class.getField("BRAND"), brand);
        fieldSettingsMap.put(Build.class.getField("DEVICE"), device);
        setFakeBuildEnvironments(fieldSettingsMap);

        // Setup fake camera with supported sizes.
        setupCamera();

        final SupportedSurfaceCombination supportedSurfaceCombination =
                new SupportedSurfaceCombination(mContext, BACK_CAMERA_ID, mCameraManager,
                        mMockCamcorderProfileHelper);

        List<Size> excludedSizes = Arrays.asList(
                new Size[]{new Size(4160, 3120), new Size(4000, 3000)});

        // Check the original mSupportedSizes contains the excluded sizes to avoid
        // mSupportedSizes modified unexpectedly.
        assertThat(Arrays.asList(mSupportedSizes)).containsAtLeastElementsIn(excludedSizes);

        // Make the fake use case have JPEG format since those sizes are excluded for JPEG format.
        final FakeUseCase useCase = new FakeUseCaseConfig.Builder()
                .setBufferFormat(ImageFormat.JPEG)
                .build();
        final List<Size> resultList = supportedSurfaceCombination.getSupportedOutputSizes(
                useCase.getCurrentConfig());
        assertThat(resultList).containsNoneIn(excludedSizes);
    }

    private void setFakeBuildEnvironments(@NonNull Map<Field, Object> fieldSettingsMap)
            throws Exception {
        for (Field field : fieldSettingsMap.keySet()) {
            field.setAccessible(true);
            field.set(null, fieldSettingsMap.get(field));
        }
    }

    private void setupCamera() {
        CameraCharacteristics characteristics =
                ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics = Shadow.extract(characteristics);
        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING,
                CameraCharacteristics.LENS_FACING_BACK);
        shadowCharacteristics.set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        shadowCharacteristics.set(
                CameraCharacteristics.SENSOR_ORIENTATION, DEFAULT_SENSOR_ORIENTATION);

        CameraManager cameraManager = (CameraManager) ApplicationProvider.getApplicationContext()
                .getSystemService(Context.CAMERA_SERVICE);
        ((ShadowCameraManager) Shadow.extract(cameraManager)).addCamera(BACK_CAMERA_ID,
                characteristics);

        StreamConfigurationMap mockMap = mock(StreamConfigurationMap.class);
        when(mockMap.getOutputSizes(anyInt())).thenReturn(mSupportedSizes);

        // ImageFormat.PRIVATE was supported since API level 23. Before that, the supported
        // output sizes need to be retrieved via SurfaceTexture.class.
        when(mockMap.getOutputSizes(SurfaceTexture.class)).thenReturn(mSupportedSizes);
        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap);

        int lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
                CameraCharacteristics.LENS_FACING_BACK);

        final FakeCameraFactory cameraFactory = new FakeCameraFactory();
        cameraFactory.insertCamera(lensFacingEnum, BACK_CAMERA_ID,
                () -> new FakeCamera(BACK_CAMERA_ID, null,
                        new Camera2CameraInfoImpl(BACK_CAMERA_ID,
                                getCameraCharacteristicsCompat(BACK_CAMERA_ID))));

        initCameraX(cameraFactory);
    }

    private void initCameraX(final FakeCameraFactory cameraFactory) {
        CameraDeviceSurfaceManager.Provider surfaceManagerProvider =
                (context, cameraManager, availableCameraIds) -> {
                    try {
                        return new Camera2DeviceSurfaceManager(context,
                                mMockCamcorderProfileHelper,
                                (CameraManagerCompat) cameraManager, availableCameraIds);
                    } catch (CameraUnavailableException e) {
                        throw new InitializationException(e);
                    }
                };

        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                Camera2Config.defaultConfig())
                .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
                .setCameraFactoryProvider((ignored0, ignored1, ignored2) -> cameraFactory)
                .build();
        CameraX.initialize(mContext, cameraXConfig);
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompat(String cameraId)
            throws CameraAccessException {
        CameraManager cameraManager =
                (CameraManager) ApplicationProvider.getApplicationContext().getSystemService(
                        Context.CAMERA_SERVICE);

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                cameraManager.getCameraCharacteristics(cameraId));
    }
}
