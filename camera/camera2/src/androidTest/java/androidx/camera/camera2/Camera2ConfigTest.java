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

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;

import androidx.camera.camera2.impl.Camera2CaptureCallbacks;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraCaptureSessionStateCallbacks;
import androidx.camera.core.CameraDeviceStateCallbacks;
import androidx.camera.core.Config;
import androidx.camera.testing.fakes.FakeConfig;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class Camera2ConfigTest {
    private static final int INVALID_TEMPLATE_TYPE = -1;
    private static final int INVALID_COLOR_CORRECTION_MODE = -1;
    private static final CameraCaptureSession.CaptureCallback SESSION_CAPTURE_CALLBACK =
            Camera2CaptureCallbacks.createComboCallback();
    private static final CameraCaptureSession.StateCallback SESSION_STATE_CALLBACK =
            CameraCaptureSessionStateCallbacks.createNoOpCallback();
    private static final CameraDevice.StateCallback DEVICE_STATE_CALLBACK =
            CameraDeviceStateCallbacks.createNoOpCallback();
    private static final CameraEventCallbacks CAMERA_EVENT_CALLBACKS =
            CameraEventCallbacks.createEmptyCallback();

    @Test
    public void emptyConfigurationDoesNotContainTemplateType() {
        FakeConfig.Builder builder = new FakeConfig.Builder();
        Camera2Config config = new Camera2Config(builder.build());

        assertThat(config.getCaptureRequestTemplate(INVALID_TEMPLATE_TYPE))
                .isEqualTo(INVALID_TEMPLATE_TYPE);
    }

    @Test
    public void canExtendWithTemplateType() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Config.Extender(builder)
                .setCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(config.getCaptureRequestTemplate(INVALID_TEMPLATE_TYPE))
                .isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void canExtendWithSessionCaptureCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Config.Extender(builder).setSessionCaptureCallback(SESSION_CAPTURE_CALLBACK);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(config.getSessionCaptureCallback(/*valueIfMissing=*/ null))
                .isSameInstanceAs(SESSION_CAPTURE_CALLBACK);
    }

    @Test
    public void canExtendWithSessionStateCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Config.Extender(builder).setSessionStateCallback(SESSION_STATE_CALLBACK);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(config.getSessionStateCallback(/*valueIfMissing=*/ null))
                .isSameInstanceAs(SESSION_STATE_CALLBACK);
    }

    @Test
    public void canExtendWithDeviceStateCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Config.Extender(builder).setDeviceStateCallback(DEVICE_STATE_CALLBACK);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(config.getDeviceStateCallback(/*valueIfMissing=*/ null))
                .isSameInstanceAs(DEVICE_STATE_CALLBACK);
    }

    @Test
    public void canExtendWithCameraEventCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Config.Extender(builder).setCameraEventCallback(CAMERA_EVENT_CALLBACKS);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(config.getCameraEventCallback(/*valueIfMissing=*/ null))
                .isSameAs(CAMERA_EVENT_CALLBACKS);
    }

    @Test
    public void canSetAndRetrieveCaptureRequestKeys() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        Range<Integer> fakeRange = new Range<>(0, 30);
        new Camera2Config.Extender(builder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(
                config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        /*valueIfMissing=*/ null))
                .isSameInstanceAs(fakeRange);
        assertThat(
                config.getCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        INVALID_COLOR_CORRECTION_MODE))
                .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST);
    }

    @Test
    public void canSetAndRetrieveCaptureRequestKeys_fromOptionIds() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        Range<Integer> fakeRange = new Range<>(0, 30);
        new Camera2Config.Extender(builder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST)
                // Insert one non capture request option to ensure it gets filtered out
                .setCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW);

        Camera2Config config = new Camera2Config(builder.build());

        config.findOptions(
                "camera2.captureRequest.option",
                new Config.OptionMatcher() {
                    @Override
                    public boolean onOptionMatched(Config.Option<?> option) {
                        // The token should be the capture request key
                        assertThat(option.getToken())
                                .isAnyOf(
                                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                        CaptureRequest.COLOR_CORRECTION_MODE);
                        return true;
                    }
                });

        assertThat(config.listOptions()).hasSize(3);
    }

    @Test
    public void canSetAndRetrieveCaptureRequestKeys_byBuilder() {
        Range<Integer> fakeRange = new Range<>(0, 30);
        Camera2Config.Builder builder =
                new Camera2Config.Builder()
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                        .setCaptureRequestOption(
                                CaptureRequest.COLOR_CORRECTION_MODE,
                                CameraMetadata.COLOR_CORRECTION_MODE_FAST);

        Camera2Config config = new Camera2Config(builder.build());

        assertThat(
                config.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        /*valueIfMissing=*/ null))
                .isSameInstanceAs(fakeRange);
        assertThat(
                config.getCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        INVALID_COLOR_CORRECTION_MODE))
                .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST);
    }

    @Test
    public void canInsertAllOptions_byBuilder() {
        Range<Integer> fakeRange = new Range<>(0, 30);
        Camera2Config.Builder builder =
                new Camera2Config.Builder()
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                        .setCaptureRequestOption(
                                CaptureRequest.COLOR_CORRECTION_MODE,
                                CameraMetadata.COLOR_CORRECTION_MODE_FAST);

        Camera2Config config1 = new Camera2Config(builder.build());

        Camera2Config.Builder builder2 =
                new Camera2Config.Builder()
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AWB_MODE,
                                CaptureRequest.CONTROL_AWB_MODE_AUTO)
                        .insertAllOptions(config1);

        Camera2Config config2 = new Camera2Config(builder2.build());

        assertThat(
                config2.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        /*valueIfMissing=*/ null))
                .isSameInstanceAs(fakeRange);
        assertThat(
                config2.getCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        INVALID_COLOR_CORRECTION_MODE))
                .isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST);
        assertThat(
                config2.getCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE, /*valueIfMissing=*/ 0))
                .isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
        assertThat(config2.getCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, 0))
                .isEqualTo(CaptureRequest.CONTROL_AWB_MODE_AUTO);
    }
}
