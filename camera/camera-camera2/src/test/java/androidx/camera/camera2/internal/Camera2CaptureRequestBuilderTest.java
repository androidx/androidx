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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.view.Surface;

import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class Camera2CaptureRequestBuilderTest {

    @Test
    public void buildCaptureRequestWithNullCameraDevice() throws CameraAccessException {
        CameraDevice cameraDevice = null;
        CaptureConfig captureConfig = new CaptureConfig.Builder().build();

        CaptureRequest captureRequest = Camera2CaptureRequestBuilder.build(captureConfig,
                cameraDevice, new HashMap<DeferrableSurface, Surface>(), true);

        assertThat(captureRequest).isNull();
    }

}
