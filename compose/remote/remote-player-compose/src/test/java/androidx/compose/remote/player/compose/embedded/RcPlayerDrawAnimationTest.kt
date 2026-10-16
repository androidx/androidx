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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Animated values read by draw operations (e.g. in `drawWithContent`) in [RcPlayer]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerDrawAnimationTest {

    @get:Rule val rule = RcPlayerTestRule()

    /** b/574455716 */
    @Test
    fun animatedFloat_inDrawWithContentPaintColor_animates() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = {
                Box(modifier = Modifier.size(100.dp).testTag("player")) { it() }
            }
        ) {
            val toggled = rememberMutableRemoteBoolean(false)
            val progress =
                animateRemoteFloatAsState(
                    targetValue = toggled.select(1f.rf, 0f.rf),
                    animationSpec =
                        remoteTween(durationMillis = 1000, easing = RemoteEasing.Linear),
                )
            val paint = RemotePaint { style = PaintingStyle.Fill }
            RemoteBox(
                RemoteModifier.size(100.rdp)
                    .clickable(valueChange(toggled, toggled.select(false.rb, true.rb)))
                    .drawWithContent {
                        paint.color = Color.Black.rc.copy(green = progress)
                        drawRect(paint)
                    }
            )
        }

        fun green(): Float {
            val pixels = rule.onNodeWithTag("player").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2].green
        }

        rule.mainClock.advanceTimeBy(100)
        assertThat(green()).isWithin(0.05f).of(0f)

        rule.onNode(hasClickAction()).performClick()
        rule.mainClock.advanceTimeByFrame()

        // Halfway through the 1000ms linear animation.
        rule.mainClock.advanceTimeBy(500)
        assertThat(green()).isWithin(0.15f).of(0.5f)

        rule.mainClock.advanceTimeBy(600)
        assertThat(green()).isWithin(0.05f).of(1f)
    }
}
