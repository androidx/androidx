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

import static junit.framework.TestCase.assertTrue;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.media.MediaCodec;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.testing.fakes.FakeUseCase;
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
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class StreamUseCaseTest {

    private CameraCharacteristics mCameraCharacteristics;

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
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCameraCharacteristics),
                true);
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

    private CameraCharacteristicsCompat getCameraCharacteristicsCompat() {
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(mCameraCharacteristics);
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            long[] uc = new long[]{CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL,
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD};
            shadowCharacteristics0.set(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES, uc);
        }
        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                mCameraCharacteristics);
    }

    private CameraCharacteristicsCompat getCameraCharacteristicsCompatWithEmptyUseCases() {
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(mCameraCharacteristics);
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shadowCharacteristics0.set(CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES,
                    new long[]{});
        }
        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                mCameraCharacteristics);
    }
}
