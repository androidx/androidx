/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder.internal.quirk

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

/** Instrument tests for [SurfaceViewNotCroppedByParentQuirk]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceViewNotCroppedByParentQuirkTest {

    @Test
    fun quirkExistsOnRedMiNote10() {
        // Arrange.
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "M2101K7AG")
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Xiaomi")

        // Act.
        val quirk = DeviceQuirks.get(SurfaceViewNotCroppedByParentQuirk::class.java)

        // Assert.
        assertThat(quirk).isNotNull()
    }
}
