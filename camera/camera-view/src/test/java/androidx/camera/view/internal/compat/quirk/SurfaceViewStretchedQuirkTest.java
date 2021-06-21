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
 * Unit test for {@link SurfaceViewStretchedQuirk}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SurfaceViewStretchedQuirkTest {

    @Test
    public void quirkExistsOnSamsungGalaxyZFold2() {
        // Arrange.
        ReflectionHelpers.setStaticField(Build.class, "DEVICE", "F2Q");
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "SAMSUNG");

        // Act.
        final SurfaceViewStretchedQuirk quirk = DeviceQuirks.get(SurfaceViewStretchedQuirk.class);

        // Assert.
        assertThat(quirk).isNotNull();
    }
}
