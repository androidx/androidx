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

@file:Suppress("RestrictedApiAndroidX") // Referring to background, remote-testing

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.basicMarquee
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device marquee motion: the marquee modifier must actually scroll its overflowing content. (Not
 * verifiable under Robolectric — basicMarquee doesn't animate without a focused window; the static
 * render/clip behavior is covered by RcPlayerSemanticsModifierTest.)
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class RcPlayerMarqueeMotionTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    @Test
    fun marqueeContentScrollsOverTime() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                content = {
                    RemoteBox(modifier = RemoteModifier.size(60.rdp, 20.rdp).basicMarquee()) {
                        RemoteRow {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(60.rdp, 20.rdp)
                                        .background(Color(0xFFCC0000).rc)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(60.rdp, 20.rdp)
                                        .background(Color(0xFF0000CC).rc)
                            )
                        }
                    }
                },
            )
        }

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(modifier = Modifier.size(80.dp).testTag("marquee")) {
                RcPlayer(document = document)
            }
        }
        rule.mainClock.advanceTimeByFrame()

        val d = rule.density.density
        fun band(): List<Int> {
            val bmp = rule.onNodeWithTag("marquee").captureToImage().asAndroidBitmap()
            val y = (10 * d).toInt()
            return (0 until (60 * d).toInt()).map { x -> bmp.getPixel(x, y) }
        }
        val before = band()
        assertTrue(
            "The red half must be visible before scrolling",
            before.count { android.graphics.Color.red(it) > 150 } > 0,
        )
        // 2s at the authored 20dp/s scrolls the blue half into the 60dp container.
        rule.mainClock.advanceTimeBy(2000)
        val after = band()
        val changed = before.indices.count { before[it] != after[it] }
        assertTrue("Marquee content must scroll (changed=$changed)", changed > 3)
    }
}
