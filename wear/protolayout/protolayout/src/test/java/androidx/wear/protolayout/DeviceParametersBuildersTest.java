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

package androidx.wear.protolayout;

import static com.google.common.truth.Truth.assertThat;

import androidx.wear.protolayout.expression.VersionBuilders;
import androidx.wear.protolayout.proto.DeviceParametersProto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DeviceParametersBuildersTest {

    @Test
    public void deviceParameters() {
        float fontScale = 0.5f;
        int major = 1;
        int minor = 2;

        VersionBuilders.VersionInfo rendererVersion =
                new VersionBuilders.VersionInfo.Builder().setMajor(major).setMinor(minor).build();
        DeviceParametersBuilders.DeviceParameters deviceParameters =
                new DeviceParametersBuilders.DeviceParameters.Builder()
                        .setFontScale(fontScale)
                        .setRendererSchemaVersion(rendererVersion)
                        .build();

        DeviceParametersProto.DeviceParameters deviceParametersProto = deviceParameters.toProto();
        assertThat(deviceParametersProto.getFontScale()).isEqualTo(fontScale);
        assertThat(deviceParametersProto.getRendererSchemaVersion().getMajor()).isEqualTo(major);
        assertThat(deviceParametersProto.getRendererSchemaVersion().getMinor()).isEqualTo(minor);
    }

    @Test
    public void capabilities() {
        float fontScale = 0.5f;

        DeviceParametersBuilders.DeviceParameters deviceParameters =
                new DeviceParametersBuilders.DeviceParameters.Builder()
                        .setFontScale(fontScale)
                        .build();

        assertThat(deviceParameters.getFontScale()).isEqualTo(fontScale);
    }
}
