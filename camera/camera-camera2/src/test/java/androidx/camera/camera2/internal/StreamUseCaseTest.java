/*
 * Copyright 2023 The Android Open Source Project
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

import static android.os.Build.VERSION.SDK_INT;

import static androidx.camera.camera2.internal.StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION;
import static androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY;
import static androidx.camera.core.ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.media.MediaCodec;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.camera.core.streamsharing.StreamSharing;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class StreamUseCaseTest {

    private CameraCharacteristics mCameraCharacteristics;
    private static final String CAMERA_ID_0 = "0";
    private static final Long TEST_STREAM_USE_CASE_OPTION_VALUE = Long.valueOf(
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
    private static final @ImageCapture.CaptureMode int TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE =
            CAPTURE_MODE_MAXIMIZE_QUALITY;

    DeferrableSurface mMockSurface = new DeferrableSurface() {
        private final ListenableFuture<Surface> mSurfaceFuture = ResolvableFuture.create();

        @NonNull
        @Override
        protected ListenableFuture<Surface> provideSurface() {
            // Return a never complete future.
            return mSurfaceFuture;
        }
    };

    @Before
    public void setup() {
        mCameraCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics();
    }

    @After
    public void tearDown() {
        mMockSurface.close();
    }

    @SdkSuppress(maxSdkVersion = 32, minSdkVersion = 21)
    @Test
    public void getStreamUseCaseFromOsNotSupported() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(Preview.class);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                new ArrayList<>(), streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.isEmpty());
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseFromUseCaseEmptyUseCase() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                new ArrayList<>(), streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.isEmpty());
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseFromUseCaseNoPreview() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(FakeUseCase.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.isEmpty());
    }

    @Test
    public void getStreamUseCaseFromUseCaseStreamSharing() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(StreamSharing.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD);
    }

    @Test
    public void getStreamUseCaseFromUseCasePreview() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(Preview.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseFromUseCaseZSL() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(Preview.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface)
                        .setTemplateType(
                                CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.isEmpty());
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseFromUseCaseImageAnalysis() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(ImageAnalysis.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseFromUseCaseConfigsImageCapture() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(ImageCapture.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseFromUseCaseConfigsVideoCapture() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(MediaCodec.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseWithNullAvailableUseCases() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(FakeUseCase.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap,
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCameraCharacteristics,
                        CAMERA_ID_0), true);
        assertTrue(streamUseCaseMap.isEmpty());
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamUseCaseWithEmptyAvailableUseCases() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(Preview.class);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs,
                streamUseCaseMap,
                getCameraCharacteristicsCompatWithEmptyUseCases(),
                true);
        assertTrue(streamUseCaseMap.isEmpty());
    }

    @Test
    public void getStreamUseCaseFromCamera2Interop() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(Preview.class);
        MutableOptionsBundle testStreamUseCaseConfig = MutableOptionsBundle.create();
        testStreamUseCaseConfig.insertOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION, 3L);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).addImplementationOptions(
                                testStreamUseCaseConfig).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface) == 3L);
    }

    @Test
    public void getUnsupportedStreamUseCaseFromCamera2Interop() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        mMockSurface.setContainerClass(Preview.class);
        MutableOptionsBundle testStreamUseCaseConfig = MutableOptionsBundle.create();
        testStreamUseCaseConfig.insertOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION, -1L);
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface).addImplementationOptions(
                                testStreamUseCaseConfig).build();
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
                sessionConfigs, streamUseCaseMap, getCameraCharacteristicsCompat(), true);
        assertTrue(streamUseCaseMap.get(mMockSurface)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void getStreamSpecImplementationOptions() {
        Camera2ImplConfig result =
                StreamUseCaseUtil.getStreamSpecImplementationOptions(
                        getFakeUseCaseConfigWithOptions(true, false, false,
                                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        assertTrue(result.retrieveOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION)
                == TEST_STREAM_USE_CASE_OPTION_VALUE);
        assertFalse(result.retrieveOption(UseCaseConfig.OPTION_ZSL_DISABLED));
        assertTrue(result.retrieveOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE)
                == TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE);
        assertTrue(result.retrieveOption(UseCaseConfig.OPTION_INPUT_FORMAT)
                == ImageFormat.PRIVATE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_streamUseCaseNotAvailable() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(
                getCameraCharacteristicsCompat(true),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertFalse(suggestedStreamSpecMap.get(useCaseConfig)
                .getImplementationOptions().containsOption(
                        STREAM_USE_CASE_STREAM_SPEC_OPTION));
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_cameraModeNotSupported() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(
                getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.CONCURRENT_CAMERA,
                        DynamicRange.BIT_DEPTH_8_BIT), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertFalse(suggestedStreamSpecMap.get(useCaseConfig)
                .getImplementationOptions().containsOption(
                        STREAM_USE_CASE_STREAM_SPEC_OPTION));
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_bitDepthNotSupported() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(
                getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_10_BIT), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertFalse(suggestedStreamSpecMap.get(useCaseConfig)
                .getImplementationOptions().containsOption(
                        STREAM_USE_CASE_STREAM_SPEC_OPTION));
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_isZslUseCase() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertFalse(suggestedStreamSpecMap.get(useCaseConfig)
                .getImplementationOptions().containsOption(
                        STREAM_USE_CASE_STREAM_SPEC_OPTION));
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_isZslUseCase_ZslDisabled() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, true, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertTrue(suggestedStreamSpecMap.get(
                useCaseConfig).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_isZslSurface() {
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, false, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG));
        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), attachedSurfaces, new HashMap<>(),
                attachedSurfaceStreamSpecMap
        );
        assertFalse(attachedSurfaceStreamSpecMap.containsKey(attachedSurfaces.get(0)));
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_isZslSurface_ZslDisabled() {
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, true, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG));
        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), attachedSurfaces, new HashMap<>(),
                attachedSurfaceStreamSpecMap
        );
        assertTrue(attachedSurfaceStreamSpecMap.get(
                attachedSurfaces.get(0)).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_camera2InteropOverride_singleNewUseCase() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertTrue(suggestedStreamSpecMap.get(
                useCaseConfig).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_camera2InteropOverride_singleSurface() {
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), attachedSurfaces, new HashMap<>(),
                attachedSurfaceStreamSpecMap
        );
        assertTrue(attachedSurfaceStreamSpecMap.get(
                attachedSurfaces.get(0)).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

    @SdkSuppress(minSdkVersion = 33)
    @Test
    public void populateStreamUseCaseStreamSpecOption_camera2InteropOverride_useCaseAndSurface() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), attachedSurfaces, suggestedStreamSpecMap,
                attachedSurfaceStreamSpecMap
        );
        assertTrue(suggestedStreamSpecMap.get(
                useCaseConfig).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
        assertTrue(attachedSurfaceStreamSpecMap.get(
                attachedSurfaces.get(0)).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void populateStreamUseCaseStreamSpecOption_camera2InteropOverride_missingOverride() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(false, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOption(getCameraCharacteristicsCompat(),
                SupportedSurfaceCombination.FeatureSettings.of(CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT), attachedSurfaces, suggestedStreamSpecMap,
                attachedSurfaceStreamSpecMap
        );
    }

    private UseCaseConfig<?> getFakeUseCaseConfigWithOptions(boolean camera2InteropOverride,
            boolean isZslDisabled, boolean isZslCaptureMode,
            UseCaseConfigFactory.CaptureType captureType, int imageFormat) {
        FakeUseCaseConfig.Builder fakeUseCaseConfigBuilder = new FakeUseCaseConfig.Builder(
                captureType);
        MutableConfig fakeConfig = fakeUseCaseConfigBuilder.getMutableConfig();
        if (camera2InteropOverride) {
            fakeConfig.insertOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION,
                    TEST_STREAM_USE_CASE_OPTION_VALUE);
        }
        fakeConfig.insertOption(UseCaseConfig.OPTION_ZSL_DISABLED, isZslDisabled);
        fakeConfig.insertOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE,
                isZslCaptureMode ? CAPTURE_MODE_ZERO_SHUTTER_LAG
                        : TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE);
        fakeConfig.insertOption(ImageCaptureConfig.OPTION_INPUT_FORMAT, imageFormat);
        return fakeUseCaseConfigBuilder.getUseCaseConfig();
    }

    private AttachedSurfaceInfo getFakeAttachedSurfaceInfo(boolean camera2InteropOverride,
            boolean isZslDisabled, boolean isZslCaptureMode,
            UseCaseConfigFactory.CaptureType captureType, int imageFormat) {
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(camera2InteropOverride,
                isZslDisabled, isZslCaptureMode, captureType, imageFormat);
        List<UseCaseConfigFactory.CaptureType> captureTypes = new ArrayList<>();
        captureTypes.add(useCaseConfig.getCaptureType());
        return AttachedSurfaceInfo.create(SurfaceConfig.create(
                        SurfaceConfig.ConfigType.PRIV,
                        SurfaceConfig.ConfigSize.PREVIEW
                ),
                useCaseConfig.getInputFormat(),
                SizeUtil.RESOLUTION_720P,
                DynamicRange.SDR,
                captureTypes,
                StreamUseCaseUtil.getStreamSpecImplementationOptions(useCaseConfig),
                /*targetFrameRate=*/null);
    }

    private StreamSpec getFakeStreamSpecFromFakeUseCaseConfig(UseCaseConfig<?> fakeUseCaseConfig) {
        return StreamSpec.builder(SizeUtil.RESOLUTION_720P)
                .setDynamicRange(DynamicRange.UNSPECIFIED)
                .setImplementationOptions(
                        StreamUseCaseUtil.getStreamSpecImplementationOptions(fakeUseCaseConfig)
                ).build();
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompat() {
        return getCameraCharacteristicsCompat(false);
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompat(
            boolean noAvailableStreamUseCase) {
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(mCameraCharacteristics);
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU && !noAvailableStreamUseCase) {
            long[] uc = new long[]{CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD};
            shadowCharacteristics0.set(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES, uc);
        }
        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                mCameraCharacteristics, CAMERA_ID_0);
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompatWithEmptyUseCases() {
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(mCameraCharacteristics);
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shadowCharacteristics0.set(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES,
                    new long[]{});
        }
        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                mCameraCharacteristics, CAMERA_ID_0);
    }
}
