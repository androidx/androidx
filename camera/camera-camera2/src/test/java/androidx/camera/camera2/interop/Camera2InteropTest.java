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

package androidx.camera.camera2.interop;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Range;

import androidx.annotation.OptIn;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.Camera2CaptureCallbacks;
import androidx.camera.camera2.internal.CameraCaptureSessionStateCallbacks;
import androidx.camera.camera2.internal.CameraDeviceStateCallbacks;
import androidx.camera.core.impl.Config;
import androidx.camera.testing.fakes.FakeConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@org.robolectric.annotation.Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(markerClass = ExperimentalCamera2Interop.class)
public final class Camera2InteropTest {
    private static final int INVALID_TEMPLATE_TYPE = -1;
    private static final int INVALID_COLOR_CORRECTION_MODE = -1;
    private static final CameraCaptureSession.CaptureCallback SESSION_CAPTURE_CALLBACK =
            Camera2CaptureCallbacks.createComboCallback();
    private static final CameraCaptureSession.StateCallback SESSION_STATE_CALLBACK =
            CameraCaptureSessionStateCallbacks.createNoOpCallback();
    private static final CameraDevice.StateCallback DEVICE_STATE_CALLBACK =
            CameraDeviceStateCallbacks.createNoOpCallback();

    @Test
    public void canExtendWithTemplateType() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Interop.Extender<>(builder)
                .setCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW);

        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        assertThat(config.getCaptureRequestTemplate(INVALID_TEMPLATE_TYPE))
                .isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void canExtendWithSessionCaptureCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Interop.Extender<>(builder).setSessionCaptureCallback(SESSION_CAPTURE_CALLBACK);
        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        assertThat(config.getSessionCaptureCallback(/*valueIfMissing=*/ null))
                .isSameInstanceAs(SESSION_CAPTURE_CALLBACK);
    }

    @Test
    public void canExtendWithSessionStateCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Interop.Extender<>(builder).setSessionStateCallback(SESSION_STATE_CALLBACK);

        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        assertThat(config.getSessionStateCallback(/*valueIfMissing=*/ null))
                .isSameInstanceAs(SESSION_STATE_CALLBACK);
    }

    @Test
    public void canExtendWithDeviceStateCallback() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        new Camera2Interop.Extender<>(builder).setDeviceStateCallback(DEVICE_STATE_CALLBACK);

        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        assertThat(config.getDeviceStateCallback(/*valueIfMissing=*/ null))
                .isSameInstanceAs(DEVICE_STATE_CALLBACK);
    }

    @Test
    public void canSetAndRetrieveCaptureRequestKeys() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        Range<Integer> fakeRange = new Range<>(0, 30);
        new Camera2Interop.Extender<>(builder).setCaptureRequestOption(
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
    public void canSetAndRetrieveCaptureRequestKeys_fromOptionIds() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        Range<Integer> fakeRange = new Range<>(0, 30);
        new Camera2Interop.Extender<>(builder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange)
                .setCaptureRequestOption(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_FAST)
                // Insert one non capture request option to ensure it gets filtered out
                .setCaptureRequestTemplate(CameraDevice.TEMPLATE_PREVIEW);

        Camera2ImplConfig config = new Camera2ImplConfig(builder.build());

        config.findOptions(
                Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM,
                option -> {
                    // The token should be the capture request key
                    assertThat(option.getToken())
                            .isAnyOf(
                                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                    CaptureRequest.COLOR_CORRECTION_MODE);
                    return true;
                });

        assertThat(config.listOptions()).hasSize(3);
    }

    @Test
    public void captureRequestOptionPriorityIsAlwaysOverride() {
        FakeConfig.Builder builder = new FakeConfig.Builder();

        Range<Integer> fakeRange = new Range<>(0, 30);
        new Camera2Interop.Extender<>(builder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange);

        Config config = builder.build();
        config.findOptions(
                Camera2ImplConfig.CAPTURE_REQUEST_ID_STEM,
                option -> {
                    assertThat(config.getOptionPriority(option))
                            .isEqualTo(Config.OptionPriority.ALWAYS_OVERRIDE);
                    return true;
                });
    }
}
