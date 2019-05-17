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

import static org.mockito.Mockito.mock;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraCaptureCallback;
import androidx.camera.core.CaptureConfig;
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
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class Camera2CaptureOptionUnpackerTest {

    private Camera2CaptureOptionUnpacker mUnpacker;

    @Before
    public void setUp() {
        mUnpacker = Camera2CaptureOptionUnpacker.INSTANCE;
    }

    @Test
    public void unpackerExtractsCaptureCallbacks() {
        ImageCaptureConfig.Builder imageCaptureConfigBuilder = new ImageCaptureConfig.Builder();
        CaptureCallback captureCallback = mock(CaptureCallback.class);

        new Camera2Config.Extender(imageCaptureConfigBuilder)
                .setSessionCaptureCallback(captureCallback);

        CaptureConfig.Builder captureBuilder = new CaptureConfig.Builder();
        mUnpacker.unpack(imageCaptureConfigBuilder.build(), captureBuilder);
        CaptureConfig captureConfig = captureBuilder.build();

        CameraCaptureCallback cameraCaptureCallback =
                captureConfig.getCameraCaptureCallbacks().get(0);
        assertThat(((CaptureCallbackContainer) cameraCaptureCallback).getCaptureCallback())
                .isEqualTo(captureCallback);
    }
}
