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
import static androidx.camera.camera2.internal.StreamUseCaseUtil.shouldUseStreamUseCase;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT;
import static androidx.camera.core.DynamicRange.BIT_DEPTH_8_BIT;
import static androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY;
import static androidx.camera.core.ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.SupportedSurfaceCombination.FeatureSettings;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
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
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.impl.fakes.FakeUseCase;
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig;
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Config(minSdk = 33)
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class StreamUseCaseTest {
    private CameraCharacteristics mCameraCharacteristics;
    private static final String CAMERA_ID_0 = "0";
    private static final Long TEST_STREAM_USE_CASE_OPTION_VALUE = Long.valueOf(
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
    private static final @ImageCapture.CaptureMode int TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE =
            CAPTURE_MODE_MAXIMIZE_QUALITY;

    DeferrableSurface mMockSurface1 = new DeferrableSurface() {
        private final ListenableFuture<Surface> mSurfaceFuture = ResolvableFuture.create();

        @NonNull
        @Override
        protected ListenableFuture<Surface> provideSurface() {
            // Return a never complete future.
            return mSurfaceFuture;
        }
    };

    DeferrableSurface mMockSurface2 = new DeferrableSurface() {
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
        mMockSurface1.close();
        mMockSurface2.close();
    }

    @Test
    public void populateSurfaceToStreamUseCaseMapping_singlePreview() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        MutableOptionsBundle optionsBundle = MutableOptionsBundle.create();
        optionsBundle.insertOption(STREAM_USE_CASE_STREAM_SPEC_OPTION,
                Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        SessionConfig sessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface1)
                        .addImplementationOptions(new Camera2ImplConfig(optionsBundle)).build();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(sessionConfig);
        ArrayList<UseCaseConfig<?>> useCaseConfigs = new ArrayList<>();
        useCaseConfigs.add(useCaseConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(sessionConfigs, useCaseConfigs,
                streamUseCaseMap);
        assertTrue(streamUseCaseMap.get(mMockSurface1) == Long.valueOf(
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
    }

    @Test
    public void populateSurfaceToStreamUseCaseMapping_imageCaptureAndMeteringRepeat() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        MutableOptionsBundle optionsBundle = MutableOptionsBundle.create();
        optionsBundle.insertOption(STREAM_USE_CASE_STREAM_SPEC_OPTION,
                Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        SessionConfig imageCaptureSessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface1)
                        .addImplementationOptions(new Camera2ImplConfig(optionsBundle)).build();
        SessionConfig meteringRepeatingSessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface2).build();
        UseCaseConfig<?> imageCaptureConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.YUV_420_888);
        UseCaseConfig<?> meteringRepeatingConfig = getFakeUseCaseConfigWithOptions(true, false,
                false, UseCaseConfigFactory.CaptureType.METERING_REPEATING, ImageFormat.PRIVATE);
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(imageCaptureSessionConfig);
        sessionConfigs.add(meteringRepeatingSessionConfig);
        ArrayList<UseCaseConfig<?>> useCaseConfigs = new ArrayList<>();
        useCaseConfigs.add(imageCaptureConfig);
        useCaseConfigs.add(meteringRepeatingConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(sessionConfigs, useCaseConfigs,
                streamUseCaseMap);
        assertTrue(streamUseCaseMap.get(mMockSurface1) == Long.valueOf(
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE));
        assertTrue(streamUseCaseMap.get(mMockSurface2) == Long.valueOf(
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
    }

    @Test
    public void populateSurfaceToStreamUseCaseMapping_previewAndNoSurfaceVideoCapture() {
        Map<DeferrableSurface, Long> streamUseCaseMap = new HashMap<>();
        MutableOptionsBundle previewOptionsBundle = MutableOptionsBundle.create();
        previewOptionsBundle.insertOption(STREAM_USE_CASE_STREAM_SPEC_OPTION,
                Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        SessionConfig previewSessionConfig =
                new SessionConfig.Builder()
                        .addSurface(mMockSurface1)
                        .addImplementationOptions(
                                new Camera2ImplConfig(previewOptionsBundle)).build();
        UseCaseConfig<?> previewConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        MutableOptionsBundle videoOptionsBundle = MutableOptionsBundle.create();
        videoOptionsBundle.insertOption(STREAM_USE_CASE_STREAM_SPEC_OPTION,
                Long.valueOf(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        // VideoCapture doesn't contain a surface
        SessionConfig videoCaptureSessionConfig =
                new SessionConfig.Builder()
                        .addImplementationOptions(
                                new Camera2ImplConfig(videoOptionsBundle)).build();
        UseCaseConfig<?> videoCaptureConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE, ImageFormat.PRIVATE);
        ArrayList<SessionConfig> sessionConfigs = new ArrayList<>();
        sessionConfigs.add(previewSessionConfig);
        sessionConfigs.add(videoCaptureSessionConfig);
        ArrayList<UseCaseConfig<?>> useCaseConfigs = new ArrayList<>();
        useCaseConfigs.add(previewConfig);
        useCaseConfigs.add(videoCaptureConfig);
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(sessionConfigs, useCaseConfigs,
                streamUseCaseMap);
        assertThat(streamUseCaseMap.size()).isEqualTo(1);
        assertThat(streamUseCaseMap.get(mMockSurface1)).isEqualTo(Long.valueOf(
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
    }

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

    @Test
    public void isStreamUseCaseSupported_streamUseCaseNotAvailable() {
        assertFalse(StreamUseCaseUtil.isStreamUseCaseSupported(
                getCameraCharacteristicsCompat(true)));
    }

    @Test
    public void shouldUseStreamUseCase_cameraModeNotSupported() {
        FeatureSettings featureSettings = FeatureSettings.of(
                CameraMode.CONCURRENT_CAMERA,
                BIT_DEPTH_8_BIT,
                /*isPreviewStabilizationOn=*/false,
                /*isUltraHdrOn=*/ false
        );
        assertFalse(shouldUseStreamUseCase(featureSettings));
    }

    @Test
    public void shouldUseStreamUseCase_bitDepthNotSupported() {
        FeatureSettings featureSettings = FeatureSettings.of(
                CameraMode.DEFAULT,
                BIT_DEPTH_10_BIT,
                /*isPreviewStabilizationOn=*/false,
                /*isUltraHdrOn=*/ false
        );
        assertFalse(shouldUseStreamUseCase(featureSettings));
    }

    @Test
    public void containsZslUseCase_isZslUseCase() {
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG);
        List<UseCaseConfig<?>> useCaseConfigList = new ArrayList<>();
        useCaseConfigList.add(useCaseConfig);
        assertTrue(StreamUseCaseUtil.containsZslUseCase(new ArrayList<>(), useCaseConfigList));
    }

    @Test
    public void containsZslUseCase_isZslUseCase_ZslDisabled() {
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, true, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG);
        List<UseCaseConfig<?>> useCaseConfigList = new ArrayList<>();
        useCaseConfigList.add(useCaseConfig);
        assertFalse(StreamUseCaseUtil.containsZslUseCase(new ArrayList<>(), useCaseConfigList));
    }

    @Test
    public void containsZslUseCase_isZslSurface() {
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, false, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG));
        assertTrue(StreamUseCaseUtil.containsZslUseCase(attachedSurfaces, new ArrayList<>()));
    }

    @Test
    public void containsZslUseCase_isZslSurface_ZslDisabled() {
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, true, true,
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG));
        assertFalse(StreamUseCaseUtil.containsZslUseCase(attachedSurfaces, new ArrayList<>()));
    }

    @Test
    public void populateStreamUseCaseStreamSpecOption_camera2InteropOverride_singleNewUseCase() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                getCameraCharacteristicsCompat(), new ArrayList<>(), suggestedStreamSpecMap,
                new HashMap<>());
        assertTrue(suggestedStreamSpecMap.get(
                useCaseConfig).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

    @Test
    public void populateStreamUseCaseStreamSpecOption_camera2InteropOverride_singleSurface() {
        List<AttachedSurfaceInfo> attachedSurfaces = new ArrayList<>();
        attachedSurfaces.add(getFakeAttachedSurfaceInfo(true, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap = new HashMap<>();
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                getCameraCharacteristicsCompat(), attachedSurfaces, new HashMap<>(),
                attachedSurfaceStreamSpecMap
        );
        assertTrue(attachedSurfaceStreamSpecMap.get(
                attachedSurfaces.get(0)).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION) == TEST_STREAM_USE_CASE_OPTION_VALUE);
    }

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
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                getCameraCharacteristicsCompat(), attachedSurfaces, suggestedStreamSpecMap,
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
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
                getCameraCharacteristicsCompat(), attachedSurfaces, suggestedStreamSpecMap,
                attachedSurfaceStreamSpecMap
        );
    }

    @Test
    public void areStreamUseCasesAvailableForSurfaceConfigs_success() {
        List<SurfaceConfig> surfaceConfigList = new ArrayList<>();
        surfaceConfigList.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        assertTrue(StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                getCameraCharacteristicsCompat(false), surfaceConfigList));

    }

    @Test
    public void areStreamUseCasesAvailableForSurfaceConfigs_fail() {
        List<SurfaceConfig> surfaceConfigList = new ArrayList<>();
        surfaceConfigList.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        assertFalse(StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                getCameraCharacteristicsCompat(true), surfaceConfigList));

    }

    @Test
    public void areCaptureTypesEligible_success() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        List<SurfaceConfig> originalSurfaceConfigs = new ArrayList<>();
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW));
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        surfaceConfigAttachedSurfaceInfoMap.put(0,
                getFakeAttachedSurfaceInfo(false, false, false,
                        UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        surfaceConfigUseCaseConfigMap.put(1,
                getFakeUseCaseConfigWithOptions(false, false, false,
                        UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE, ImageFormat.PRIVATE));

        assertTrue(StreamUseCaseUtil.areCaptureTypesEligible(surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase));
    }

    @Test
    public void areCaptureTypesEligible_fail() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        List<SurfaceConfig> originalSurfaceConfigs = new ArrayList<>();
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW));
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        surfaceConfigAttachedSurfaceInfoMap.put(0,
                getFakeAttachedSurfaceInfo(false, false, false,
                        UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        surfaceConfigUseCaseConfigMap.put(1,
                getFakeUseCaseConfigWithOptions(false, false, false,
                        UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE));

        assertFalse(StreamUseCaseUtil.areCaptureTypesEligible(surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase));
    }

    @Test(expected = AssertionError.class)
    public void areCaptureTypesEligible_mappingError() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        List<SurfaceConfig> originalSurfaceConfigs = new ArrayList<>();
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW));
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        surfaceConfigUseCaseConfigMap.put(1,
                getFakeUseCaseConfigWithOptions(false, false, false,
                        UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE, ImageFormat.PRIVATE));

        StreamUseCaseUtil.areCaptureTypesEligible(surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase);
    }

    @Test
    public void areCaptureTypesEligible_streamSharing_previewVideoStill_success() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL));
        UseCaseConfigFactory useCaseConfigFactory = new FakeUseCaseConfigFactory();
        Set<UseCase> children = new HashSet<>();
        children.add(new FakeUseCase(new FakeUseCaseConfig.Builder().getUseCaseConfig(),
                UseCaseConfigFactory.CaptureType.PREVIEW));
        children.add(new FakeUseCase(new FakeUseCaseConfig.Builder().getUseCaseConfig(),
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE));
        children.add(new FakeUseCase(new FakeUseCaseConfig.Builder().getUseCaseConfig(),
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE));
        StreamSharing streamSharing = new StreamSharing(new FakeCamera(), null,
                CompositionSettings.DEFAULT, CompositionSettings.DEFAULT, children,
                useCaseConfigFactory);
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        surfaceConfigUseCaseConfigMap.put(0,
                streamSharing.getDefaultConfig(true, useCaseConfigFactory));

        assertTrue(StreamUseCaseUtil.areCaptureTypesEligible(surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase));
    }

    @Test
    public void areCaptureTypesEligible_streamSharing_videoRecord_success() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        List<UseCaseConfigFactory.CaptureType> captureTypes = new ArrayList<>();
        captureTypes.add(UseCaseConfigFactory.CaptureType.PREVIEW);
        captureTypes.add(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE);
        surfaceConfigAttachedSurfaceInfoMap.put(0,
                AttachedSurfaceInfo.create(SurfaceConfig.create(
                                SurfaceConfig.ConfigType.PRIV,
                                SurfaceConfig.ConfigSize.PREVIEW
                        ),
                        ImageFormat.PRIVATE,
                        SizeUtil.RESOLUTION_720P,
                        DynamicRange.SDR,
                        captureTypes,
                        /*implementationOptions=*/null,
                        /*targetFrameRate=*/null));

        assertTrue(StreamUseCaseUtil.areCaptureTypesEligible(surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase));
    }

    @Test
    public void areCaptureTypesEligible_streamSharing_fail() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        List<UseCaseConfigFactory.CaptureType> captureTypes = new ArrayList<>();
        captureTypes.add(UseCaseConfigFactory.CaptureType.PREVIEW);
        captureTypes.add(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE);
        captureTypes.add(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE);
        surfaceConfigAttachedSurfaceInfoMap.put(0,
                AttachedSurfaceInfo.create(SurfaceConfig.create(
                                SurfaceConfig.ConfigType.PRIV,
                                SurfaceConfig.ConfigSize.PREVIEW
                        ),
                        ImageFormat.PRIVATE,
                        SizeUtil.RESOLUTION_720P,
                        DynamicRange.SDR,
                        captureTypes,
                        /*implementationOptions=*/null,
                        /*targetFrameRate=*/null));

        assertFalse(StreamUseCaseUtil.areCaptureTypesEligible(surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase));
    }

    @Test
    public void populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs_success() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        List<SurfaceConfig> originalSurfaceConfigs = new ArrayList<>();
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW));
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        AttachedSurfaceInfo attachedSurfaceInfo = getFakeAttachedSurfaceInfo(false, false, false,
                UseCaseConfigFactory.CaptureType.PREVIEW, ImageFormat.PRIVATE);
        surfaceConfigAttachedSurfaceInfoMap.put(0, attachedSurfaceInfo);
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(false, false, false,
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE, ImageFormat.PRIVATE);
        surfaceConfigUseCaseConfigMap.put(1, useCaseConfig);
        @NonNull Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap =
                new HashMap<>();
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));

        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
                suggestedStreamSpecMap, attachedSurfaceStreamSpecMap,
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase);

        assertTrue(attachedSurfaceStreamSpecMap.get(
                attachedSurfaceInfo).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW);
        assertTrue(suggestedStreamSpecMap.get(
                useCaseConfig).getImplementationOptions().retrieveOption(
                STREAM_USE_CASE_STREAM_SPEC_OPTION)
                == CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD);
    }

    @Test(expected = AssertionError.class)
    public void populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs_mappingError() {
        List<SurfaceConfig> surfaceConfigsWithStreamUseCase = new ArrayList<>();
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW));
        surfaceConfigsWithStreamUseCase.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD,
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD));
        List<SurfaceConfig> originalSurfaceConfigs = new ArrayList<>();
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW));
        originalSurfaceConfigs.add(SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.RECORD));
        Map<Integer, AttachedSurfaceInfo> surfaceConfigAttachedSurfaceInfoMap =
                new HashMap<>();
        @NonNull Map<Integer, UseCaseConfig<?>> surfaceConfigUseCaseConfigMap =
                new HashMap<>();
        UseCaseConfig<?> useCaseConfig = getFakeUseCaseConfigWithOptions(false, false, false,
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE, ImageFormat.PRIVATE);
        surfaceConfigUseCaseConfigMap.put(1, useCaseConfig);
        @NonNull Map<AttachedSurfaceInfo, StreamSpec> attachedSurfaceStreamSpecMap =
                new HashMap<>();
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecMap = new HashMap<>();
        suggestedStreamSpecMap.put(useCaseConfig,
                getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig));

        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
                suggestedStreamSpecMap, attachedSurfaceStreamSpecMap,
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap, surfaceConfigsWithStreamUseCase);
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
