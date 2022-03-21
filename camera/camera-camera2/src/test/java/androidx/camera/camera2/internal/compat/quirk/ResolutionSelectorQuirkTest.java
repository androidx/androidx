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

package androidx.camera.camera2.internal.compat.quirk;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

/**
 * Unit tests for {@link ExtraCroppingQuirk}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ResolutionSelectorQuirkTest {


    @Test
    public void samsungDistortionHasQuirks() {
        ReflectionHelpers.setStaticField(Build.class, "BRAND", "samsung");
        // Default is false
        assertThat(ExtraCroppingQuirk.load()).isFalse();

        // Test all samsung models
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-T580");
        assertThat(ExtraCroppingQuirk.load()).isTrue();
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-J710MN");
        assertThat(ExtraCroppingQuirk.load()).isTrue();
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-A320FL");
        assertThat(ExtraCroppingQuirk.load()).isTrue();
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-G570M");
        assertThat(ExtraCroppingQuirk.load()).isTrue();
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-G610M");
        assertThat(ExtraCroppingQuirk.load()).isTrue();
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "SM-G610F");
        assertThat(ExtraCroppingQuirk.load()).isTrue();
    }
}
