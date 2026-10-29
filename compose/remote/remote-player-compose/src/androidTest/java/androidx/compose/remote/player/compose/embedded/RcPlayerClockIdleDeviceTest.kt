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

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.RemoteContext
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
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device checks that the embedded player ([RcPlayer]) only advances document time while
 * something that is composed, laid out or drawn reads it. Unlike the Robolectric tests, draw passes
 * here come from the device's own frames.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RcPlayerClockIdleDeviceTest {

    @get:Rule val rule = RcPlayerTestRule()

    /** Advances one frame of the Compose clock, then waits for the device to draw it. */
    private fun frame() {
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        awaitNextDeviceFrame()
    }

    /**
     * Waits until the device has finished its next Choreographer frame, including that frame's
     * traversal (measure, layout and draw) if one is scheduled. A frame callback runs in the
     * frame's animation phase, before the traversal; a message posted from it runs after the whole
     * frame. Waiting happens on the test thread, so the main looper keeps processing messages.
     */
    private fun awaitNextDeviceFrame() {
        val frameDone = CountDownLatch(1)
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            Choreographer.getInstance().postFrameCallback {
                mainHandler.post { frameDone.countDown() }
            }
        }
        assertThat(frameDone.await(FRAME_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
    }

    /** Runs [frames] frames and returns how many of them applied a snapshot state change. */
    private fun framesWithStateChanges(frames: Int): Int {
        val changed = AtomicBoolean(false)
        var changedFrames = 0
        val handle = Snapshot.registerApplyObserver { set, _ ->
            if (set.isNotEmpty()) changed.set(true)
        }
        try {
            repeat(frames) {
                changed.set(false)
                frame()
                if (changed.get()) changedFrames++
            }
        } finally {
            handle.dispose()
        }
        return changedFrames
    }

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
                            .size(40.rdp)
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
        RemoteCanvas(
            modifier =
                RemoteModifier.semantics { contentDescription = "bar".rs }.size(100.rdp, 20.rdp)
        ) {
            drawRect(
                paint = RemotePaint().apply { color = RemoteColor(Color.Red) },
                topLeft = RemoteOffset(0f.rf, 0f.rf),
                size = RemoteSize((RemoteFloat(time) % 1f.rf) * 100f.rf, 20f.rf),
            )
        }
    }

    @Test
    fun clockInInactiveStateBranch_stopsTicking() {
        setToggledStateContent { state ->
            if (!state) RemoteText("static".rs) else TimeBar(RemoteContext.FLOAT_CONTINUOUS_SEC)
        }
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
        framesWithStateChanges(200)

        // A parked clock wakes for one tick when something on the device reads time again (for
        // example a recomposition the system triggers), so allow one. A clock that never parks
        // ticks about three times over these 200 frames (3.2s).
        assertThat(framesWithStateChanges(200)).isAtMost(1)
    }

    private companion object {
        const val FRAME_TIMEOUT_SECONDS = 5L
    }
}
