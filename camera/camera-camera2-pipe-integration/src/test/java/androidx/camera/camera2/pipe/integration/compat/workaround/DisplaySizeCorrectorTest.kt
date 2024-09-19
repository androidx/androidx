/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.os.Build
import android.util.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DisplaySizeCorrectorTest {
    @Test
    fun returnCorrectDisplaySizeForProblematicDevice() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "REDMI NOTE 8")
        // See SmallDisplaySizeQuirk for the device display size
        assertThat(DisplaySizeCorrector().displaySize).isEqualTo(Size(1080, 2340))
    }

    @Test
    fun returnNullDisplaySizeForProblematicDevice() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Fake-Model")
        // See SmallDisplaySizeQuirk for the device display size
        assertThat(DisplaySizeCorrector().displaySize).isNull()
    }
}
