/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.collection.buildObjectIntMap
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressEnabled
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressIndeterminate
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressIndicatorCustomColor
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressIndicatorDisabled
import androidx.wear.compose.remote.material3.previews.RemoteCircularProgressNoGapCustomAngle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteCircularProgressIndicatorTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            clock = TestClock(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun indicator_enabled() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            Center { RemoteCircularProgressEnabled() }
        }
    }

    @Test
    fun indicator_enabled_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = LayoutDirection.Rtl,
        ) {
            Center { RemoteCircularProgressEnabled() }
        }
    }

    @Test
    fun indicator_indeterminate() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            Center { RemoteCircularProgressIndeterminate() }
        }
    }

    @Test
    fun indicator_disabled() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            Center { RemoteCircularProgressIndicatorDisabled() }
        }
    }

    @Test
    fun indicator_customColors() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            Center { RemoteCircularProgressIndicatorCustomColor() }
        }
    }

    @Test
    fun indicator_customEndAngle_and_noGap() {
        remoteComposeTestRule.runScreenshotTest(creationDisplayInfo = creationDisplayInfo) {
            Center { RemoteCircularProgressNoGapCustomAngle() }
        }
    }

    @Test
    fun indicator_dynamic_color() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
        }
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = Color.Black,
            colorOverrides = colorOverrides,
        ) {
            Center(RemoteModifier.fillMaxSize()) { RemoteCircularProgressEnabled() }
        }
    }

    @Composable
    @RemoteComposable
    private fun Center(
        modifier: RemoteModifier = RemoteModifier.fillMaxSize(),
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        RemoteBox(modifier, contentAlignment = RemoteAlignment.Center, content = content)
    }
}

@Suppress("RestrictedApiAndroidX")
private class TestClock(val baseTimeMillis: Long = 10 * 3600000L + 10 * 60000L) : RemoteClock {
    var offsetMillis: Long = 500

    override fun millis() = baseTimeMillis + offsetMillis

    override fun nanoTime() = (baseTimeMillis + offsetMillis) * 1_000_000L

    override fun getZoneId() = "UTC"

    override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot {
        val m = millis ?: (baseTimeMillis + offsetMillis)
        return ManualTimeSnapshot(m)
    }

    @Suppress("RestrictedApiAndroidX")
    private class ManualTimeSnapshot(val m: Long) : RemoteClock.TimeSnapshot {
        override fun getMillis() = m

        override fun getYear() = 2026

        override fun getMonth() = 2

        override fun getDayOfMonth() = 13

        override fun getDayOfYear() = 44

        override fun getHour() = (m / 3600000).toInt() % 24

        override fun getMinute() = (m / 60000).toInt() % 60

        override fun getSecond() = (m / 1000).toInt() % 60

        override fun getMillisOfSecond() = (m % 1000).toInt()

        override fun getDayOfWeek() = 5

        override fun getOffsetSeconds() = 0
    }
}
