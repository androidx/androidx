/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk

import androidx.camera.core.impl.QuirkSettings
import androidx.camera.core.impl.QuirkSettingsHolder
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class AudioTimestampFramePositionIncorrectQuirkTest(
    private val brand: String,
    private val model: String,
    private val expectedEnabled: Boolean,
) {
    @Before
    fun setUp() {
        resetDeviceQuirks()
    }

    @After
    fun tearDown() {
        resetDeviceQuirks()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "brand={0}, model={1}, enabled={2}")
        fun data(): List<Array<Any>> =
            listOf(
                arrayOf("Google", "Pixel 10", true),
                arrayOf("Google", "Pixel 10 Pro", true),
                arrayOf("Google", "Pixel 10 Pro XL", true),
                arrayOf("Google", "Pixel 10 Pro Fold", true),
                arrayOf("google", "pixel 10 pro", true),
                arrayOf("Oppo", "CPH1920", true),
                arrayOf("LGE", "LG-M250", true),
                arrayOf("Google", "Pixel 9", false),
                arrayOf("Google", "Pixel 9 Pro", false),
                arrayOf("Google", "Pixel 9 Pro Fold", false),
                arrayOf("Samsung", "SM-S928U1", false),
                arrayOf("Unknown", "Pixel 10", false),
            )
    }

    @Test
    fun deviceQuirksLoadsQuirkForAffectedModelsOnly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel(model)
        reloadDeviceQuirks()

        assertThat(DeviceQuirks.get(AudioTimestampFramePositionIncorrectQuirk::class.java) != null)
            .isEqualTo(expectedEnabled)
    }

    private fun resetDeviceQuirks() {
        ShadowBuild.reset()
        reloadDeviceQuirks()
    }

    private fun reloadDeviceQuirks() {
        QuirkSettingsHolder.instance().set(QuirkSettings.withAllQuirksDisabled())
        QuirkSettingsHolder.instance().set(QuirkSettings.withDefaultBehavior())
    }
}
