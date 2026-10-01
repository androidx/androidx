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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.internal.RemotePrimaryGestureIndicator
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Screenshot tests for [RemotePrimaryGestureIndicator].
 *
 * The animation is sampled at the keyframes of its 617ms cycle, i.e. where each of its four tweens
 * ends, so that every distinct pose of the double pinch has a golden.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemotePrimaryGestureIndicatorTest {

    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(200f, 200f))

    @Test
    fun primaryGestureIndicator_atRest() {
        verifyIndicatorAt(progress = 0f)
    }

    @Test
    fun primaryGestureIndicator_firstPinch() {
        verifyIndicatorAt(progress = FirstPinchProgress)
    }

    @Test
    fun primaryGestureIndicator_partialRelease() {
        verifyIndicatorAt(progress = PartialReleaseProgress)
    }

    @Test
    fun primaryGestureIndicator_secondPinch() {
        verifyIndicatorAt(progress = SecondPinchProgress)
    }

    @Test
    fun primaryGestureIndicator_cycleEnd() {
        verifyIndicatorAt(progress = 1f)
    }

    private fun verifyIndicatorAt(progress: Float) {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemotePrimaryGestureIndicator(progress = progress.rf) }
        }
    }

    private companion object {
        const val CycleMillis = 617f

        /** End of the first pinch-in tween (131ms). */
        const val FirstPinchProgress = 131f / CycleMillis

        /** End of the partial-release tween (300ms). */
        const val PartialReleaseProgress = 300f / CycleMillis

        /** End of the second pinch-in tween (433ms). */
        const val SecondPinchProgress = 433f / CycleMillis
    }
}
