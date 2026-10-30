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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TimeAttribute
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowView

/**
 * The embedded player ([RcPlayer]) only advances document time while something that is composed,
 * laid out or drawn reads it.
 *
 * These tests turn on Robolectric's real draw traversals, so draw passes run on every frame as on a
 * device, and step both the Compose test clock and the main looper.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerClockIdleTest {

    @get:Rule(order = 0) val realDraws: TestRule = RealDrawTraversalsRule()

    @get:Rule(order = 1) val rule = RcPlayerTestRule()

    /** Advances one frame of the Compose clock and lets the main looper run a draw traversal. */
    private fun frame() {
        rule.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(16))
    }

    /** Runs [frames] frames and returns how many of them applied a snapshot state change. */
    private fun framesWithStateChanges(frames: Int): Int =
        rule.framesWithStateChanges(frames, ::frame)

    private fun setToggledStateContent(branch: @Composable (Boolean) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = { Box(modifier = Modifier.size(300.dp)) { it() } }
        ) {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteColumn {
                RemoteBox(
                    modifier =
                        RemoteModifier.semantics { contentDescription = "toggle".rs }
                            .size(20.rdp)
                            .clickable(onClick = valueChangeAction(checked, !checked))
                )
                RemoteStateLayout(currentState = checked) { state -> branch(state) }
            }
        }
        frame()
    }

    /** A canvas whose red bar's width follows [time] (wrapped to one second). */
    @Composable
    private fun TimeBar(time: Float) {
        Bar(width = (RemoteFloat(time) % 1f.rf) * 100f.rf)
    }

    /** A canvas with a red bar of [width]. */
    @Composable
    private fun Bar(width: RemoteFloat) {
        RemoteCanvas(
            modifier =
                RemoteModifier.semantics { contentDescription = "bar".rs }.size(100.rdp, 20.rdp)
        ) {
            drawRect(
                paint = RemotePaint().apply { color = RemoteColor(Color.Red) },
                topLeft = RemoteOffset(0f.rf, 0f.rf),
                size = RemoteSize(width, 20f.rf),
            )
        }
    }

    /** Plays [document] in a 100dp box with the Compose clock under manual control. */
    private fun playDocument(document: CoreDocument): RcPlayerState {
        val state = RcPlayerState(document)
        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) } }
        frame()
        return state
    }

    /**
     * A raw document that draws a red square only inside the window of an impulse that lasts
     * [duration] seconds and starts at [start] (seconds of animation time).
     */
    private fun impulseDocument(duration: Float, start: Float): CoreDocument = buildDocument {
        writer.impulse(duration, start) {
            painter.setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
            writer.impulseProcess {
                painter.setColor(AndroidColor.RED).commit()
                writer.drawRect(0f, 0f, 100f, 100f)
            }
        }
    }

    @Test
    fun clockInInactiveStateBranch_stopsTicking() {
        setToggledStateContent { state ->
            if (!state) RemoteText("static".rs) else TimeBar(RemoteContext.FLOAT_CONTINUOUS_SEC)
        }
        // Let the player see a draw pass and find that nothing reads time.
        framesWithStateChanges(10)

        assertThat(framesWithStateChanges(30)).isEqualTo(0)
    }

    @Test
    fun clockInStateBranch_startsTickingWhenShown() {
        setToggledStateContent { state ->
            if (!state) RemoteText("static".rs) else TimeBar(RemoteContext.FLOAT_CONTINUOUS_SEC)
        }
        framesWithStateChanges(10)
        assertThat(framesWithStateChanges(10)).isEqualTo(0)

        rule.onNodeWithContentDescription("toggle").performClick()
        // Let the state transition finish.
        framesWithStateChanges(60)

        assertThat(framesWithStateChanges(30)).isAtLeast(28)
    }

    @Test
    fun drawOnlyClock_keepsTickingEveryFrame() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = { Box(modifier = Modifier.size(300.dp)) { it() } }
        ) {
            TimeBar(RemoteContext.FLOAT_CONTINUOUS_SEC)
        }
        framesWithStateChanges(10)

        assertThat(framesWithStateChanges(60)).isAtLeast(58)
    }

    @Test
    fun secondsClockInInactiveStateBranch_stopsTicking() {
        setToggledStateContent { state ->
            if (!state) RemoteText("static".rs) else TimeBar(RemoteContext.FLOAT_TIME_IN_SEC)
        }
        // A seconds readout ticks once per second; it stops after two unread seconds.
        framesWithStateChanges(200)

        assertThat(framesWithStateChanges(200)).isEqualTo(0)
    }

    @Test
    fun secondsClock_keepsTickingWhileShown() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = { Box(modifier = Modifier.size(300.dp)) { it() } }
        ) {
            TimeBar(RemoteContext.FLOAT_TIME_IN_SEC)
        }
        framesWithStateChanges(200)

        // About 200 frames of 16ms span three second boundaries.
        assertThat(framesWithStateChanges(200)).isAtLeast(2)
    }

    @Test
    fun slowClockFieldsInInactiveStateBranch_stopTicking() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteText("static".rs)
            } else {
                RemoteColumn {
                    // These fields change hourly, daily, yearly or never, so they never change
                    // twice while the test runs.
                    TimeBar(RemoteContext.FLOAT_TIME_IN_HR)
                    TimeBar(RemoteContext.FLOAT_DAY_OF_MONTH)
                    TimeBar(RemoteContext.FLOAT_WEEK_DAY)
                    TimeBar(RemoteContext.FLOAT_YEAR)
                    TimeBar(RemoteContext.FLOAT_OFFSET_TO_UTC)
                }
            }
        }
        // Two unread seconds (and probes) plus margin.
        framesWithStateChanges(400)

        assertThat(framesWithStateChanges(200)).isEqualTo(0)
    }

    @Test
    fun timeAttributeInInactiveStateBranch_stopsTicking() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteText("static".rs)
            } else {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp, 20.rdp)) {
                    // A time attribute makes the document refer to every calendar field,
                    // including the year and the UTC offset.
                    val hour =
                        document.timeAttribute(
                            document.addTimeLong(System.currentTimeMillis()),
                            TimeAttribute.TIME_IN_HR,
                        )
                    drawRect(
                        paint = RemotePaint().apply { color = RemoteColor(Color.Red) },
                        topLeft = RemoteOffset(0f.rf, 0f.rf),
                        size = RemoteSize(RemoteFloat(hour), 20f.rf),
                    )
                }
            }
        }
        framesWithStateChanges(400)

        assertThat(framesWithStateChanges(200)).isEqualTo(0)
    }

    @Test
    fun hourClock_keepsTickingWhileShown() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = { Box(modifier = Modifier.size(300.dp)) { it() } }
        ) {
            TimeBar(RemoteContext.FLOAT_TIME_IN_HR)
        }
        framesWithStateChanges(400)

        // A shown hour readout keeps the loop ticking once per second, so it changes on time. About
        // 200 frames of 16ms span three second boundaries.
        assertThat(framesWithStateChanges(200)).isAtLeast(2)
    }

    @Test
    fun animatedClockExpressionInInactiveStateBranch_stopsTicking() {
        setToggledStateContent { state ->
            if (!state) {
                RemoteText("static".rs)
            } else {
                Bar(
                    width =
                        animateRemoteFloatAsState(
                            (RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) % 1f.rf) * 100f.rf,
                            remoteTween(durationMillis = 300),
                        )
                )
            }
        }
        framesWithStateChanges(10)

        assertThat(framesWithStateChanges(30)).isEqualTo(0)
    }

    @Test
    fun animatedClockExpression_keepsTickingWhileShown() {
        rule.mainClock.autoAdvance = false
        rule.setRemoteContent(
            playComposableWrapper = { Box(modifier = Modifier.size(300.dp)) { it() } }
        ) {
            Bar(
                width =
                    animateRemoteFloatAsState(
                        (RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) % 1f.rf) * 100f.rf,
                        remoteTween(durationMillis = 300),
                    )
            )
        }
        framesWithStateChanges(10)

        assertThat(framesWithStateChanges(30)).isAtLeast(28)
    }

    @Test
    fun impulseWhoseWindowHasClosed_doesNotTick() {
        playDocument(impulseDocument(duration = 0.2f, start = -10f))
        framesWithStateChanges(10)

        assertThat(framesWithStateChanges(60)).isEqualTo(0)
    }

    @Test
    fun impulse_ticksOnlyUntilItsWindowCloses() {
        // Active from 1s to 1.4s of animation time.
        playDocument(impulseDocument(duration = 0.4f, start = 1f))

        // Before the window opens the loop ticks every frame, so it opens on time.
        assertThat(framesWithStateChanges(30)).isAtLeast(28)
        assertNotRed(rule.captureRootBitmap(), 5, 5)

        // About 1.2s: inside the window.
        framesWithStateChanges(45)
        assertRed(rule.captureRootBitmap(), 5, 5)

        // Well after the window closed, nothing reads time any more.
        framesWithStateChanges(60)
        assertThat(framesWithStateChanges(30)).isEqualTo(0)
        assertNotRed(rule.captureRootBitmap(), 5, 5)
    }

    @Test
    fun impulseWithClosedWindow_wakesTheClockWhenRestarted() {
        val document = buildDocument {
            val start = writer.addNamedFloat("start", -2000f)
            writer.impulse(1000f, start) {
                painter.setColor(AndroidColor.RED).commit()
                writer.drawRect(0f, 0f, 100f, 100f)
                writer.impulseProcess {
                    painter.setColor(AndroidColor.RED).commit()
                    writer.drawRect(0f, 0f, 100f, 100f)
                }
            }
        }
        val state = playDocument(document)
        framesWithStateChanges(10)
        assertThat(framesWithStateChanges(30)).isEqualTo(0)

        // Reopen the window around the current time: [0s, 1000s].
        var start by state.floatState("start")
        rule.runOnUiThread { start = 0f }

        assertThat(framesWithStateChanges(10)).isAtLeast(8)
        assertRed(rule.captureRootBitmap(), 5, 5)
    }
}

/**
 * Turns on Robolectric's real draw traversals (and with them real view animations, see
 * [ShadowView.setUseRealDrawTraversals]) for one test, so draw passes run on every frame as on a
 * device. Must wrap the rule that launches the test activity: a window added before this has no
 * surface to draw into.
 */
private class RealDrawTraversalsRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                ShadowView.setUseRealDrawTraversals(true)
                try {
                    base.evaluate()
                } finally {
                    ShadowView.setUseRealDrawTraversals(false)
                }
            }
        }
}
