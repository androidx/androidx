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

package androidx.camera.testing.fakes;

import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.core.impl.SurfaceConfig.ConfigSize.PREVIEW;
import static androidx.camera.core.impl.SurfaceConfig.ConfigType.YUV;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import android.os.Build;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.CameraMode;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.SurfaceConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link FakeCameraDeviceSurfaceManager}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FakeCameraDeviceSurfaceManagerTest {

    private static final int FAKE_WIDTH0 = 400;
    private static final int FAKE_HEIGHT0 = 300;

    private static final int FAKE_WIDTH1 = 800;
    private static final int FAKE_HEIGHT1 = 600;

    private static final String FAKE_CAMERA_ID0 = "0";
    private static final String FAKE_CAMERA_ID1 = "1";

    private FakeCameraDeviceSurfaceManager mFakeCameraDeviceSurfaceManager;

    private FakeUseCaseConfig mFakeUseCaseConfig;

    @Before
    public void setUp() {
        mFakeCameraDeviceSurfaceManager = new FakeCameraDeviceSurfaceManager();
        mFakeUseCaseConfig = new FakeUseCaseConfig.Builder().getUseCaseConfig();

        mFakeCameraDeviceSurfaceManager.setSuggestedStreamSpec(FAKE_CAMERA_ID0,
                mFakeUseCaseConfig.getClass(),
                StreamSpec.builder(new Size(FAKE_WIDTH0, FAKE_HEIGHT0)).build());
        mFakeCameraDeviceSurfaceManager.setSuggestedStreamSpec(FAKE_CAMERA_ID1,
                mFakeUseCaseConfig.getClass(),
                StreamSpec.builder(new Size(FAKE_WIDTH1, FAKE_HEIGHT1)).build());
    }

    @Test
    public void validSurfaceCombination_noException() {
        UseCaseConfig<?> preview = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        UseCaseConfig<?> analysis = new ImageAnalysis.Builder().getUseCaseConfig();
        mFakeCameraDeviceSurfaceManager.getSuggestedStreamSpecs(
                CameraMode.DEFAULT,
                FAKE_CAMERA_ID0,
                emptyList(),
                createConfigOutputSizesMap(preview, analysis));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSurfaceAndConfigCombination_throwException() {
        UseCaseConfig<?> preview = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        UseCaseConfig<?> video = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        AttachedSurfaceInfo analysis = AttachedSurfaceInfo.create(
                        SurfaceConfig.create(YUV, PREVIEW),
                        YUV_420_888,
                        new Size(1, 1),
                        DynamicRange.SDR,
                        singletonList(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS),
                        preview,
                        new Range<>(30, 30));
        mFakeCameraDeviceSurfaceManager.getSuggestedStreamSpecs(
                CameraMode.DEFAULT,
                FAKE_CAMERA_ID0,
                singletonList(analysis), createConfigOutputSizesMap(preview, video));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConfigCombination_throwException() {
        UseCaseConfig<?> preview = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        UseCaseConfig<?> video = new FakeUseCaseConfig.Builder().getUseCaseConfig();
        UseCaseConfig<?> analysis = new ImageAnalysis.Builder().getUseCaseConfig();
        mFakeCameraDeviceSurfaceManager.getSuggestedStreamSpecs(
                CameraMode.DEFAULT,
                FAKE_CAMERA_ID0,
                Collections.emptyList(), createConfigOutputSizesMap(preview, video, analysis));
    }

    @Test
    public void canRetrieveInsertedSuggestedStreamSpecs() {
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecsCamera0 =
                mFakeCameraDeviceSurfaceManager.getSuggestedStreamSpecs(
                        CameraMode.DEFAULT,
                        FAKE_CAMERA_ID0,
                        Collections.emptyList(), createConfigOutputSizesMap(mFakeUseCaseConfig));
        Map<UseCaseConfig<?>, StreamSpec> suggestedStreamSpecCamera1 =
                mFakeCameraDeviceSurfaceManager.getSuggestedStreamSpecs(
                        CameraMode.DEFAULT,
                        FAKE_CAMERA_ID1,
                        Collections.emptyList(), createConfigOutputSizesMap(mFakeUseCaseConfig));

        assertThat(suggestedStreamSpecsCamera0.get(mFakeUseCaseConfig)).isEqualTo(
                StreamSpec.builder(new Size(FAKE_WIDTH0, FAKE_HEIGHT0)).build());
        assertThat(suggestedStreamSpecCamera1.get(mFakeUseCaseConfig)).isEqualTo(
                StreamSpec.builder(new Size(FAKE_WIDTH1, FAKE_HEIGHT1)).build());
    }

    private Map<UseCaseConfig<?>, List<Size>> createConfigOutputSizesMap(
            @NonNull UseCaseConfig<?>... useCaseConfigs) {
        Map<UseCaseConfig<?>, List<Size>> configOutputSizesMap = new HashMap<>();
        for (UseCaseConfig<?> useCaseConfig : useCaseConfigs) {
            configOutputSizesMap.put(useCaseConfig, Collections.emptyList());
        }
        return configOutputSizesMap;
    }
}
