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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.MatrixRotate
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The embedded player ([RcPlayer]) picks its clock loop (per frame, per second, or none) from the
 * time variables a document reads, including operations that bind a time variable directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerClockDetectionTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun buildLayoutDocumentWithWidthExpression(
        widthVarId: Int,
        clock: RemoteClock = RemoteClock.SYSTEM,
        writePreOps: (WireBuffer) -> Unit,
    ): CoreDocument =
        buildDocument(clock = clock) {
            val descId = writer.addText("target")
            writePreOps(writer.buffer.buffer)
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                startBox(
                    RecordingModifier().then { w ->
                        val wire = w.buffer.buffer
                        CoreSemantics().apply { mContentDescriptionId = descId }.write(wire)
                        WidthModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            Utils.asNan(widthVarId),
                        )
                        HeightModifierOperation.apply(
                            wire,
                            DimensionModifierOperation.Type.EXACT.ordinal,
                            20f,
                        )
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
                endBox()
            }
        }

    @Test
    fun staticDocument_doesNotAdvanceTimeOnFrames() {
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(
                    FloatExpression(100, floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD), null)
                )
            }
        val state = RcPlayerState(doc)

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = state) }
        }
        repeat(10) { rule.mainClock.advanceTimeBy(20) }

        // Time stays at the t = 0 seeded at setup: the player waits for no frame to start a clock.
        assertThat(state.currentTimeMillis).isEqualTo(0f)
    }

    @Test
    fun drawOpBoundDirectlyToContinuousSec_runsContinuousClock() {
        // No FloatExpression: the matrix op reads the time variable itself.
        assertThat(
                rule.observeClockCadence(
                    MatrixRotate(Utils.asNan(RemoteContext.ID_CONTINUOUS_SEC), 0f, 0f)
                )
            )
            .isEqualTo(ClockCadence.CONTINUOUS_FRAME)
    }

    @Test
    fun drawOpBoundDirectlyToTimeInSec_runsDiscreteClock() {
        assertThat(
                rule.observeClockCadence(
                    MatrixRotate(Utils.asNan(RemoteContext.ID_TIME_IN_SEC), 0f, 0f)
                )
            )
            .isEqualTo(ClockCadence.DISCRETE_SECOND)
    }

    @Test
    fun widthModifierBoundDirectlyToContinuousSec_runsContinuousClock() {
        assertThat(
                rule.observeClockCadence { clock ->
                    buildLayoutDocumentWithWidthExpression(
                        RemoteContext.ID_CONTINUOUS_SEC,
                        clock,
                    ) {}
                }
            )
            .isEqualTo(ClockCadence.CONTINUOUS_FRAME)
    }

    @Test
    fun widthModifierBoundDirectlyToTimeInSec_runsDiscreteClock() {
        assertThat(
                rule.observeClockCadence { clock ->
                    buildLayoutDocumentWithWidthExpression(RemoteContext.ID_TIME_IN_SEC, clock) {}
                }
            )
            .isEqualTo(ClockCadence.DISCRETE_SECOND)
    }
}
