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

package androidx.camera.camera2.impl;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.DeviceProperties;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.O)
public final class ImageCaptureOptionUnpackerTest {

    private static final String MANUFACTURE_GOOGLE = "Google";
    private static final String MANUFACTURE_NOT_GOOGLE = "ANY";
    private static final String MODEL_PIXEL_2 = "Pixel 2";
    private static final String MODEL_PIXEL_3 = "Pixel 3";
    private static final String MODEL_NOT_SUPPORT_HDR = "ANY";
    private static final int API_LEVEL_25 = Build.VERSION_CODES.N_MR1;
    private static final int API_LEVEL_26 = Build.VERSION_CODES.O;

    private static final DeviceProperties PROPERTIES_PIXEL_2_API26 = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_26);

    private static final DeviceProperties PROPERTIES_PIXEL_3_API26 = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_PIXEL_3,
            API_LEVEL_26);

    private static final DeviceProperties PROPERTIES_PIXEL_2_NOT_SUPPORT_API =
            DeviceProperties.create(
                    MANUFACTURE_GOOGLE,
                    MODEL_PIXEL_2,
                    API_LEVEL_25);

    private static final DeviceProperties PROPERTIES_PIXEL_3_NOT_SUPPORT_API =
            DeviceProperties.create(
                    MANUFACTURE_GOOGLE,
                    MODEL_PIXEL_3,
                    API_LEVEL_25);

    private static final DeviceProperties PROPERTIES_NOT_GOOGLE = DeviceProperties.create(
            MANUFACTURE_NOT_GOOGLE,
            MODEL_PIXEL_2,
            API_LEVEL_26);

    private static final DeviceProperties PROPERTIES_NOT_SUPPORT_MODEL = DeviceProperties.create(
            MANUFACTURE_GOOGLE,
            MODEL_NOT_SUPPORT_HDR,
            API_LEVEL_26);


    private ImageCaptureOptionUnpacker mUnpacker;

    @Before
    public void setUp() {
        mUnpacker = ImageCaptureOptionUnpacker.INSTANCE;
    }

    @Test
    public void unpackWithoutCaptureMode() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_2_API26);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    public void unpackWithValidPixel2AndMinLatency() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_2_API26);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isEqualTo(false);
    }

    @Test
    public void unpackWithValidPixel2AndMaxQuality() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_2_API26);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isEqualTo(true);
    }

    @Test
    public void unpackWithPixel2NotSupportApiLevelAndMinLatency() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_2_NOT_SUPPORT_API);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    public void unpackWithPixel2NotSupportApiLevelAndMaxQuality() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_2_NOT_SUPPORT_API);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    public void unpackWithValidPixel3AndMinLatency() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_3_API26);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isEqualTo(false);
    }

    @Test
    public void unpackWithValidPixel3AndMaxQuality() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_3_API26);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isEqualTo(true);
    }

    @Test
    public void unpackWithPixel3NotSupportApiLevelAndMinLatency() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_3_NOT_SUPPORT_API);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    public void unpackWithPixel3NotSupportApiLevelAndMaxQuality() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_PIXEL_3_NOT_SUPPORT_API);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    public void unpackWithNotSupportManufacture() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_NOT_GOOGLE);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }

    @Test
    public void unpackWithNotSupportModel() {
        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        mUnpacker.setDeviceProperty(PROPERTIES_NOT_SUPPORT_MODEL);
        mUnpacker.unpack(imageCaptureConfig, captureBuilder);

        CaptureConfig captureConfig = captureBuilder.build();
        Camera2Config camera2Config = new Camera2Config(captureConfig.getImplementationOptions());
        assertThat(camera2Config.getCaptureRequestOption(CaptureRequest.CONTROL_ENABLE_ZSL, null))
                .isNull();
    }
}
