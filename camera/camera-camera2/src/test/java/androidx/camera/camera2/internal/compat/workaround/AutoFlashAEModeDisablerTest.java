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

package androidx.camera.camera2.internal.compat.workaround;

import static com.google.common.truth.Truth.assertThat;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class AutoFlashAEModeDisablerTest {
    @Test
    public void changeAeAutoFlashToAeOn_onSamsungA300H() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-A300H");

        int aeMode = new AutoFlashAEModeDisabler()
                .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void changeOnAutoFlashToOn_onSamsungA300YZ() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-A300YZ");

        int aeMode = new AutoFlashAEModeDisabler()
                .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void keepAeOn_onSamsungA300H() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-A300H");

        int aeMode = new AutoFlashAEModeDisabler()
                .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON);

        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void keepAeAlwaysOn_onSamsungA300H() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-A300H");

        int aeMode = new AutoFlashAEModeDisabler()
                .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);

        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
    }

    @Test
    public void changeOnAutoFlashToOn_onSamsungJ5() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-J510FN");

        int aeMode = new AutoFlashAEModeDisabler()
                .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON);
    }

    @Test
    public void keepAeAutoFlash_onSamsungOtherDevices() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-A3XXX");

        int aeMode = new AutoFlashAEModeDisabler()
                .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
    }
}
