/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.view.internal.compat.quirk;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

/**
 * Unit tests for {@link PreviewOneThirdWiderQuirk}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class PreviewOneThirdWiderQuirkTest {

    @Test
    public void quirkExistsOnSamsungA3() {
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "A3Y17LTE");
        assertPreviewShouldBeCroppedBy25Percent();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.O)
    public void quirkExistsOnSamsungJ5PrimeApi26AndAbove() {
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "ON5XELTE");
        assertPreviewShouldBeCroppedBy25Percent();
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.N_MR1)
    public void quirkDoesNotExistOnSamsungJ5PrimeApi25AndBelow() {
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "ON5XELTE");
        assertThat(DeviceQuirks.get(PreviewOneThirdWiderQuirk.class)).isNull();
    }

    private void assertPreviewShouldBeCroppedBy25Percent() {
        final PreviewOneThirdWiderQuirk quirk = DeviceQuirks.get(PreviewOneThirdWiderQuirk.class);
        assertThat(quirk).isNotNull();
        assertThat(quirk.getCropRectScaleX()).isEqualTo(0.75F);
    }
}
