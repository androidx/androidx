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

package androidx.camera.core.impl.utils;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.LensFacing;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraSelectorUtilTest {

    @Test
    public void convertedCameraDeviceConfig_hasFrontLensFacing() {
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(LensFacing.FRONT).build();
        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        assertThat(convertedConfig.getLensFacing()).isEqualTo(LensFacing.FRONT);
    }

    @Test
    public void  convertedCameraDeviceConfig_hasBackLensFacing() {
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(LensFacing.BACK).build();
        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        assertThat(convertedConfig.getLensFacing()).isEqualTo(LensFacing.BACK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void  convertedCameraDeviceConfig_doesNotContainFilterForEmptySelector() {
        CameraSelector cameraSelector = new CameraSelector.Builder().build();
        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        convertedConfig.getCameraIdFilter();
    }
}
