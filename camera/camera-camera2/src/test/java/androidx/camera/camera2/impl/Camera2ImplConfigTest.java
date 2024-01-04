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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Range;

import androidx.camera.core.impl.Config;
import androidx.camera.testing.impl.fakes.FakeConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class Camera2ImplConfigTest {
    private static final int INVALID_TEMPLATE_TYPE = -1;
    private static final int INVALID_COLOR_CORRECTION_MODE = -1;

    @Test
    public void emptyConfigurationDoesNotContainTemplateType() {
        FakeConfig.Builder builder = new FakeConfig.Builder();
        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        assertThat(config.getCaptureRequestTemplate(INVALID_TEMPLATE_TYPE))
                .isEqualTo(INVALID_TEMPLATE_TYPE);
    }

    @Test
    public void canSetAndRetrieveCaptureRequestKeys_byBuilder() {
        Range<Integer> fakeRange = new Range<>(0, 30);
        Camera2ImplConfig.Builder builder =
                new Camera2ImplConfig.Builder()
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                        .setCaptureRequestOption(
                                CaptureRequest.COLOR_CORRECTION_MODE,
                                CameraMetadata.COLOR_CORRECTION_MODE_FAST);

        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        assertThat(
                config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        /*valueIfMissing=*/ null))
                .isEqualTo(fakeRange);
        assertThat(
                config.getCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        INVALID_COLOR_CORRECTION_MODE))
                .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST);
    }

    @Test
    public void canSetCaptureRequestOptionWithPriority() {
        Camera2ImplConfig.Builder builder =
                new Camera2ImplConfig.Builder()
                        .setCaptureRequestOptionWithPriority(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_OFF,
                                Config.OptionPriority.ALWAYS_OVERRIDE);

        Camera2ImplConfig config = builder.build();
        config.findOptions(
                Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM,
                option -> {
                    assertThat(option.getToken())
                            .isEqualTo(CaptureRequest.CONTROL_AF_MODE);
                    assertThat(config.retrieveOption(option))
                            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF);
                    assertThat(config.getOptionPriority(option))
                            .isEqualTo(Config.OptionPriority.ALWAYS_OVERRIDE);
                    return true;
                });
    }

    @Test
    public void canInsertAllOptions_byBuilder() {
        Range<Integer> fakeRange = new Range<>(0, 30);
        Camera2ImplConfig.Builder builder =
                new Camera2ImplConfig.Builder()
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                        .setCaptureRequestOption(
                                CaptureRequest.COLOR_CORRECTION_MODE,
                                CameraMetadata.COLOR_CORRECTION_MODE_FAST);

        Camera2ImplConfig config1 = new Camera2ImplConfig(builder.build());

        Camera2ImplConfig.Builder builder2 =
                new Camera2ImplConfig.Builder()
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AWB_MODE,
                                CaptureRequest.CONTROL_AWB_MODE_AUTO)
                        .insertAllOptions(config1);

        Camera2ImplConfig config2 = new Camera2ImplConfig(builder2.build());

        assertThat(
                config2.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        /*valueIfMissing=*/ null))
                .isEqualTo(fakeRange);
        assertThat(
                config2.getCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        INVALID_COLOR_CORRECTION_MODE))
                .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST);
        assertThat(
                config2.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, /*valueIfMissing=*/ 0))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
        assertThat(config2.getCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE, 0))
                .isEqualTo(CaptureRequest.CONTROL_AWB_MODE_AUTO);
    }

    @Test
    public void captureRequestOptionPriorityIsOPTIONAL() {
        Range<Integer> range = new Range<>(0, 30);
        Camera2ImplConfig.Builder builder =
                new Camera2ImplConfig.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range);

        Config config = builder.build();
        config.findOptions(
                Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM,
                option -> {
                    assertThat(config.getOptionPriority(option))
                            .isEqualTo(Config.OptionPriority.OPTIONAL);
                    return true;
                });
    }
}
