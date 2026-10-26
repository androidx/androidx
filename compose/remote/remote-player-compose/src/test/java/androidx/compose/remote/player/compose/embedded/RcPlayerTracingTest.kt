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

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTrace

/**
 * Tests verifying that embedded player operations not covered by standard Compose tracing or
 * Compose Composition tracing emit explicit Android trace sections (`androidx.tracing.trace`):
 * - `RcPlayer:preprocessDocument`
 * - `RcPlayer:initializeRemoteContext`
 * - `RcPlayer:updateTime`
 * - `RcPlayer:executeClickActions`
 * - `RcPlayer:executeOperations`
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerTracingTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Before
    fun setUp() {
        ShadowTrace.setEnabled(true)
        ShadowTrace.reset()
    }

    @After
    fun tearDown() {
        ShadowTrace.setEnabled(false)
        ShadowTrace.reset()
    }

    private fun loadCoreDocument(bytes: ByteArray, clock: RemoteClock): CoreDocument =
        CoreDocument(clock).apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }

    @Test
    fun initialComposition_emitsPreprocessAndInitializeRemoteContextTraceSections() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = { RemoteBox(modifier = RemoteModifier.size(100.rdp)) },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        ShadowTrace.reset()

        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        val recorded = ShadowTrace.getPreviousSections()
        assertThat(recorded).contains("RcPlayer:preprocessDocument")
        assertThat(recorded).contains("RcPlayer:initializeRemoteContext")
    }

    @Test
    fun continuousTimeFrameLoop_emitsUpdateTimeTraceSection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        val widthExpr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 2f
                        RemoteBox(modifier = RemoteModifier.width(widthExpr).height(10.rdp))
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        rule.mainClock.autoAdvance = false
        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.mainClock.advanceTimeBy(32)

        ShadowTrace.reset()
        rule.mainClock.advanceTimeBy(16)

        val recorded = ShadowTrace.getPreviousSections()
        assertThat(recorded).contains("RcPlayer:updateTime")
    }

    @Test
    fun clickInteraction_emitsExecuteClickActionsTraceSection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        val state = remember { MutableRemoteFloat(10f) }
                        RemoteBox(
                            modifier =
                                RemoteModifier.semantics { contentDescription = "clickTarget".rs }
                                    .clickable(valueChange(state, 50f.rf))
                                    .width(state)
                                    .height(20.rdp)
                        )
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        ShadowTrace.reset()
        rule.onNodeWithContentDescription("clickTarget").performClick()
        rule.waitForIdle()

        val recorded = ShadowTrace.getPreviousSections()
        assertThat(recorded).contains("RcPlayer:executeClickActions")
    }

    @Test
    fun canvasDrawPass_emitsExecuteOperationsTraceSection() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                            drawRect(
                                paint = RemotePaint().apply { color = RemoteColor(Color.Blue) },
                                topLeft = RemoteOffset(0f.rf, 0f.rf),
                                size = RemoteSize(50f.rf, 50f.rf),
                            )
                        }
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        ShadowTrace.reset()

        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()
        rule.onRoot().captureToImage()

        val recorded = ShadowTrace.getPreviousSections()
        assertThat(recorded).contains("RcPlayer:executeOperations")
    }
}
