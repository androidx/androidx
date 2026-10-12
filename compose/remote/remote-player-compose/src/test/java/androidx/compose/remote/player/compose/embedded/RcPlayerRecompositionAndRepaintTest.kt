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
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests verifying recomposition, state update, and layout/repaint frequencies across different
 * document categories:
 * - Expression using millis (`FLOAT_CONTINUOUS_SEC` / `FLOAT_ANIMATION_TIME`)
 * - Expression using seconds (`FLOAT_TIME_IN_SEC`)
 * - Expression using month (`FLOAT_CALENDAR_MONTH`)
 * - Animated float (Compose UI animations via `animateRemoteFloatAsState`)
 * - Static document (none)
 * - Additional cases (discrete time + immediate user interaction, Canvas-only draw)
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerRecompositionAndRepaintTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun setUp() {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
    }

    private data class FrameActivity(val snapshotWrites: Int, val boundsChanges: Int)

    private fun loadCoreDocument(bytes: ByteArray, clock: RemoteClock): CoreDocument =
        CoreDocument(clock).apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }

    private fun widthOf(contentDescription: String): Float =
        rule.onNodeWithContentDescription(contentDescription).getUnclippedBoundsInRoot().let {
            it.right.value - it.left.value
        }

    private fun setPlayerContent(document: CoreDocument) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(modifier = Modifier.size(400.dp)) { RcPlayer(document = document) }
        }
        // Advance 4 initial frames (64ms) so LaunchedEffect and initial composition settle.
        rule.mainClock.advanceTimeBy(64)
    }

    /**
     * Advances virtual time by [totalMillis] in [stepMillis] frame increments, recording:
     * - [FrameActivity.snapshotWrites]: number of Snapshot state apply batches triggered (each time
     *   `GraphTimeState.updateTime`, `Animatable`, or `ValueChange` mutates state to trigger
     *   recomposition/redraw).
     * - [FrameActivity.boundsChanges]: number of frames where the rendered width of
     *   [trackedNodeDescription] changed on screen.
     */
    private fun advanceInFramesAndMeasure(
        totalMillis: Long,
        stepMillis: Long = 16L,
        trackedNodeDescription: String? = null,
    ): FrameActivity {
        var snapshotWrites = 0
        var boundsChanges = 0
        var previousWidth = trackedNodeDescription?.let { widthOf(it) }
        val handle = Snapshot.registerApplyObserver { changed, _ ->
            if (changed.isNotEmpty()) {
                snapshotWrites++
            }
        }
        try {
            var remaining = totalMillis
            while (remaining > 0) {
                val step = minOf(stepMillis, remaining)
                rule.mainClock.advanceTimeBy(step)
                remaining -= step
                if (trackedNodeDescription != null && previousWidth != null) {
                    val currentWidth = widthOf(trackedNodeDescription)
                    if (abs(currentWidth - previousWidth) > 0.01f) {
                        boundsChanges++
                        previousWidth = currentWidth
                    }
                }
            }
        } finally {
            handle.dispose()
        }
        return FrameActivity(snapshotWrites = snapshotWrites, boundsChanges = boundsChanges)
    }

    @Test
    fun documentWithMillisExpression_updatesEveryFrame() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T00:00:01.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        // Scale by 100f so each 16ms frame advances width by 1.6px (> 1 integer px)
                        val widthExpr = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 100f
                        RemoteBox(
                            modifier =
                                RemoteModifier.semantics { contentDescription = "millisBox".rs }
                                    .width(widthExpr)
                                    .height(10.rdp)
                        )
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        val preprocessed = preprocessDocument(document)
        assertThat(preprocessed.hasContinuousTime).isTrue()

        setPlayerContent(document)

        // Over 10 frames (160ms within the same second), a millis-dependent expression updates
        // state and layout on every frame.
        val activity =
            advanceInFramesAndMeasure(
                totalMillis = 160L,
                stepMillis = 16L,
                trackedNodeDescription = "millisBox",
            )
        assertThat(activity.snapshotWrites).isAtLeast(9)
        assertThat(activity.boundsChanges).isAtLeast(9)
    }

    @Test
    fun documentWithSecondsExpression_sleepsIntraSecondAndUpdatesOncePerSecond() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Start at 01:02:03.100Z (~836ms before the next second boundary after initial 64ms settle)
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        val widthExpr = RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC) * 2f
                        RemoteBox(
                            modifier =
                                RemoteModifier.semantics { contentDescription = "secBox".rs }
                                    .width(widthExpr)
                                    .height(10.rdp)
                        )
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        val preprocessed = preprocessDocument(document)
        assertThat(preprocessed.hasContinuousTime).isFalse()
        assertThat(preprocessed.hasDiscreteTime).isTrue()

        setPlayerContent(document)

        // Advance 20 frames (320ms: 01:02:03.164Z -> 01:02:03.484Z) strictly within second 03:
        // Zero snapshot writes and zero layout changes (coroutine sleeps in delay()).
        val intraSecond =
            advanceInFramesAndMeasure(
                totalMillis = 320L,
                stepMillis = 16L,
                trackedNodeDescription = "secBox",
            )
        assertThat(intraSecond.snapshotWrites).isEqualTo(0)
        assertThat(intraSecond.boundsChanges).isEqualTo(0)

        // Advance 3000ms (crossing 3 whole-second boundaries: :04, :05, :06):
        // Layout updates once per second boundary (3 times total across 187 frames).
        val threeSeconds =
            advanceInFramesAndMeasure(
                totalMillis = 3000L,
                stepMillis = 16L,
                trackedNodeDescription = "secBox",
            )
        assertThat(threeSeconds.boundsChanges).isEqualTo(3)
    }

    @Test
    fun documentWithMonthExpression_doesNotRecomposeAcrossSecondsOrHoursUntilMonthBoundary() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // Start 1.5s before the end of January: 2026-01-31T23:59:58.500Z
            val baseInstant = Instant.parse("2026-01-31T23:59:58.500Z")
            val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val monthWidth = RemoteFloat(RemoteContext.FLOAT_CALENDAR_MONTH) * 20f
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "monthBox".rs }
                                        .width(monthWidth)
                                        .height(10.rdp)
                            )
                        },
                    )
                    .bytes

            val document = loadCoreDocument(bytes, fixedClock)
            val preprocessed = preprocessDocument(document)
            assertThat(preprocessed.hasContinuousTime).isFalse()
            assertThat(preprocessed.hasDiscreteTime).isTrue()

            setPlayerContent(document)
            assertThat(widthOf("monthBox")).isWithin(0.1f).of(20f) // January = 1 * 20

            // Advance 1000ms (to 2026-01-31T23:59:59.564Z): crosses a second boundary,
            // but CALENDAR_MONTH remains 1f -> zero layout/repaint changes on monthBox!
            val crossSecondSameMonth =
                advanceInFramesAndMeasure(
                    totalMillis = 1000L,
                    stepMillis = 16L,
                    trackedNodeDescription = "monthBox",
                )
            assertThat(crossSecondSameMonth.boundsChanges).isEqualTo(0)
            assertThat(widthOf("monthBox")).isWithin(0.1f).of(20f)

            // Advance another 1000ms (to 2026-02-01T00:00:00.564Z): crosses midnight into February!
            // CALENDAR_MONTH changes from 1f to 2f -> triggers exactly 1 layout change!
            val crossMonthBoundary =
                advanceInFramesAndMeasure(
                    totalMillis = 1000L,
                    stepMillis = 16L,
                    trackedNodeDescription = "monthBox",
                )
            assertThat(crossMonthBoundary.boundsChanges).isEqualTo(1)
            assertThat(widthOf("monthBox")).isWithin(0.1f).of(40f) // February = 2 * 20
        }

    @Test
    fun documentWithAnimatedFloat_isIdleBeforeAndAfterAnimationAndUpdatesDuringAnimation() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
            val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val target = rememberMutableRemoteFloat(50f)
                            val animated =
                                animateRemoteFloatAsState(
                                    targetValue = target,
                                    animationSpec = remoteTween(durationMillis = 200),
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "animBox".rs }
                                        .clickable(valueChange(target, 150f.rf))
                                        .width(animated)
                                        .height(10.rdp)
                            )
                        },
                    )
                    .bytes

            val document = loadCoreDocument(bytes, fixedClock)
            val preprocessed = preprocessDocument(document)
            // Compose UI animations do NOT keep RcPlayer's background time loop running:
            assertThat(preprocessed.hasContinuousTime).isFalse()
            assertThat(preprocessed.hasDiscreteTime).isFalse()

            setPlayerContent(document)
            // Allow initial 200ms appearance animation to complete and settle.
            rule.mainClock.advanceTimeBy(250)

            // 1. While idle (before click), advancing 500ms causes zero updates.
            val idleBefore =
                advanceInFramesAndMeasure(
                    totalMillis = 500L,
                    stepMillis = 16L,
                    trackedNodeDescription = "animBox",
                )
            assertThat(idleBefore.snapshotWrites).isEqualTo(0)
            assertThat(idleBefore.boundsChanges).isEqualTo(0)

            // 2. Trigger animation via click: during the 200ms animation window, frames run and
            // update state & layout continuously.
            rule.onNodeWithContentDescription("animBox").performClick()
            val duringAnim =
                advanceInFramesAndMeasure(
                    totalMillis = 300L,
                    stepMillis = 16L,
                    trackedNodeDescription = "animBox",
                )
            assertThat(duringAnim.snapshotWrites).isAtLeast(5)
            assertThat(duringAnim.boundsChanges).isAtLeast(5)
            assertThat(widthOf("animBox")).isWithin(0.1f).of(150f)

            // 3. After animation completes, advancing another 500ms settles back to zero updates.
            val idleAfter =
                advanceInFramesAndMeasure(
                    totalMillis = 500L,
                    stepMillis = 16L,
                    trackedNodeDescription = "animBox",
                )
            assertThat(idleAfter.snapshotWrites).isEqualTo(0)
            assertThat(idleAfter.boundsChanges).isEqualTo(0)
        }

    @Test
    fun staticDocument_neverRecomposesOrRepaintsAfterInitialRender() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        RemoteBox(
                            modifier =
                                RemoteModifier.semantics { contentDescription = "staticBox".rs }
                                    .size(100.rdp)
                        ) {
                            RemoteText(text = "Static Content".rs)
                        }
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        val preprocessed = preprocessDocument(document)
        assertThat(preprocessed.hasContinuousTime).isFalse()
        assertThat(preprocessed.hasDiscreteTime).isFalse()

        setPlayerContent(document)

        // Advance across 5 full seconds: zero snapshot writes and zero layout changes.
        val activity =
            advanceInFramesAndMeasure(
                totalMillis = 5000L,
                stepMillis = 16L,
                trackedNodeDescription = "staticBox",
            )
        assertThat(activity.snapshotWrites).isEqualTo(0)
        assertThat(activity.boundsChanges).isEqualTo(0)
    }

    @Test
    fun documentWithDiscreteTimeAndInteractiveState_updatesImmediatelyOnInteractionWithoutWaitingForNextSecond() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            // Start at 01:02:03.100Z -> ~836ms left until the next second boundary
            val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
            val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val interactiveExtra = rememberMutableRemoteFloat(0f)
                            val secWidth =
                                RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC) * 2f + interactiveExtra
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics {
                                            contentDescription = "interactiveSecBox".rs
                                        }
                                        .clickable(valueChange(interactiveExtra, 40f.rf))
                                        .width(secWidth)
                                        .height(10.rdp)
                            )
                        },
                    )
                    .bytes

            val document = loadCoreDocument(bytes, fixedClock)
            setPlayerContent(document)

            // At t = 64ms (01:02:03.164Z): width is 123 * 2 + 0 = 246f
            assertThat(widthOf("interactiveSecBox")).isWithin(0.1f).of(246f)

            // Perform click while discrete loop is asleep mid-second (still ~830ms before :04):
            rule.onNodeWithContentDescription("interactiveSecBox").performClick()
            // Advance 2 frames (32ms -> 01:02:03.196Z, still >800ms before :04):
            // Interactive state update renders immediately without waiting for the next second!
            rule.mainClock.advanceTimeBy(32)
            assertThat(widthOf("interactiveSecBox")).isWithin(0.1f).of(286f)

            // Advance 900ms across the :04 second boundary (to 01:02:04.096Z):
            // The discrete second loop wakes up on schedule and advances TIME_IN_SEC to 124
            // (width = 124 * 2 + 40 = 288f).
            rule.mainClock.advanceTimeBy(900)
            assertThat(widthOf("interactiveSecBox")).isWithin(0.1f).of(288f)
        }

    @Test
    fun canvasOnlyContinuousTimeExpression_repaintsEveryFrameWithoutRelayout() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseInstant = Instant.parse("2026-01-01T01:02:03.100Z")
        val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

        val bytes =
            captureSingleRemoteDocument(
                    context = context,
                    content = {
                        val dynamicRight = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 2f
                        RemoteCanvas(
                            modifier =
                                RemoteModifier.semantics { contentDescription = "canvasNode".rs }
                                    .size(100.rdp)
                        ) {
                            drawRect(
                                paint = RemotePaint().apply { color = RemoteColor(Color.Red) },
                                topLeft = RemoteOffset(0f.rf, 0f.rf),
                                size = RemoteSize(dynamicRight, 50f.rf),
                            )
                        }
                    },
                )
                .bytes

        val document = loadCoreDocument(bytes, fixedClock)
        setPlayerContent(document)

        // Over 10 frames (160ms): continuous time updates state every frame to invalidate canvas
        // draw, while layout bounds remain unchanged (0 boundsChanges).
        val activity =
            advanceInFramesAndMeasure(
                totalMillis = 160L,
                stepMillis = 16L,
                trackedNodeDescription = "canvasNode",
            )
        assertThat(activity.snapshotWrites).isAtLeast(9)
        assertThat(activity.boundsChanges).isEqualTo(0)
    }
}
