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

package androidx.camera.camera2.internal.compat.workaround

import android.os.Build
import androidx.camera.camera2.internal.compat.quirk.AutoFlashUnderExposedQuirk
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.impl.Quirks
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class OverrideAeModeForStillCaptureTest {
    @Test
    fun flashStarted_shouldSetAeModeAlwaysFlash() {
        // Arrange:  enable the quirk.
        val quirksList = listOf(AutoFlashUnderExposedQuirk())
        val overrideAeModeForStillCapture = OverrideAeModeForStillCapture(Quirks(quirksList))

        // Act:  trigger Ae precapture.
        overrideAeModeForStillCapture.onAePrecaptureStarted()

        // Assert:  set to ALWAYS_FLASH when flash mode is auto.
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_AUTO))
            .isTrue()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_OFF))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_ON))
            .isFalse()
    }

    @Test
    fun initial_notSetAeModeAlwaysFlash() {
        // Arrange:  enable the quirk.
        val quirksList = listOf(AutoFlashUnderExposedQuirk())
        val overrideAeModeForStillCapture = OverrideAeModeForStillCapture(Quirks(quirksList))

        // Act: no Ae precapture was triggered.

        // Assert
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_AUTO))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_OFF))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_ON))
            .isFalse()
    }

    @Test
    fun flashFinished_notSetAeModeAlwaysFlash() {
        // Arrange:  enable the quirk.
        val quirksList = listOf(AutoFlashUnderExposedQuirk())
        val overrideAeModeForStillCapture = OverrideAeModeForStillCapture(Quirks(quirksList))

        // Act: Ae precapture started and finished.
        overrideAeModeForStillCapture.onAePrecaptureStarted()
        overrideAeModeForStillCapture.onAePrecaptureFinished()

        // Assert
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_AUTO))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_OFF))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_ON))
            .isFalse()
    }

    @Test
    fun quirkNotEnabled_notSetAeModeAlwaysFlash() {
        // Arrange:  Do not enable the quirk.
        val overrideAeModeForStillCapture = OverrideAeModeForStillCapture(Quirks(emptyList()))

        // Act:  trigger Ae precapture.
        overrideAeModeForStillCapture.onAePrecaptureStarted()

        // Assert:  do not override.
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_AUTO))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_OFF))
            .isFalse()
        assertThat(overrideAeModeForStillCapture.shouldSetAeModeAlwaysFlash(FLASH_MODE_ON))
            .isFalse()
    }
}
