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
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable as hostClickable
import androidx.compose.foundation.combinedClickable as hostCombinedClickable
import androidx.compose.foundation.horizontalScroll as hostHorizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll as hostVerticalScroll
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.TouchListener
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.core.operations.layout.modifiers.ComponentModifiers
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.lambdaAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteScrollState
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.combinedClickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.Hoist
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.acos
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.asin
import androidx.compose.remote.creation.compose.state.atan
import androidx.compose.remote.creation.compose.state.atan2
import androidx.compose.remote.creation.compose.state.cbrt
import androidx.compose.remote.creation.compose.state.ceil
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.copySign
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.exp
import androidx.compose.remote.creation.compose.state.floor
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.ln
import androidx.compose.remote.creation.compose.state.log
import androidx.compose.remote.creation.compose.state.mad
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.pow
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberRemoteFloatExpression
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.round
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.sign
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.sqrt
import androidx.compose.remote.creation.compose.state.tan
import androidx.compose.remote.creation.compose.state.toDeg
import androidx.compose.remote.creation.compose.state.toRad
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs as mathAbs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerInteractivityTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val experimentalProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    @Test
    fun testButtonClickTogglesVisibility() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // A clickable button that flips a state to GONE, and a target box (carrying an explicit
            // semantics contentDescription so it is query-able) whose visibility is driven by that
            // state. This verifies the full embedded click path end to end:
            // performClick -> ClickModifier -> ValueChange -> visibility modifier -> re-render.
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            // RemoteCompose visibility constants are GONE=0, VISIBLE=1 (not the
                            // Android View 0/4/8 values).
                            val visibilityState = remember {
                                MutableRemoteInt(Component.Visibility.VISIBLE)
                            }

                            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                                // Clickable "button": sets the target's visibility to GONE (8).
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color(0xFF3F51B5).rc)
                                            .clickable(
                                                action =
                                                    valueChange(
                                                        visibilityState,
                                                        Component.Visibility.GONE.ri,
                                                    )
                                            )
                                )

                                // Target: visible initially, hidden after the click.
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "target".rs
                                            }
                                            .visibility(visibilityState)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFFFFC107).rc)
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.waitForIdle()

            // Before: the target component is present.
            rule.onNodeWithContentDescription("target").assertExists()

            // Click the single clickable node (no reliance on text semantics).
            rule.onNode(hasClickAction()).performClick()
            rule.waitForIdle() // allow ValueChange + recomposition

            // After: the click drove the state to GONE; the embedded player stops composing the
            // component, so its node is gone — proving the click -> ValueChange -> visibility path.
            rule.onNodeWithContentDescription("target").assertDoesNotExist()
        }
    }

    // Like the visibility test above, but the target's visibility is driven by an *integer
    // expression* (`visibilityState * 1`) rather than the variable directly. This proves the
    // embedded player resolves IntegerExpressions reactively (rememberRemoteIntExpression): the
    // click
    // mutates the input variable, the expression recomputes (1*1 -> visible, 0*1 -> gone), and the
    // target node disappears. Before the reactive integer-expression path, the expression was
    // evaluated only once at setup and would never have updated.
    @Test
    fun integerExpressionDrivesVisibilityReactively() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val visibilityState = remember {
                                MutableRemoteInt(Component.Visibility.VISIBLE)
                            }
                            // One operand is a (non-constant) variable, so this authors an
                            // IntegerExpression rather than folding to a constant.
                            val visibilityExpr = visibilityState * 1.ri

                            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color(0xFF3F51B5).rc)
                                            .clickable(
                                                action =
                                                    valueChange(
                                                        visibilityState,
                                                        Component.Visibility.GONE.ri,
                                                    )
                                            )
                                )

                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics {
                                                contentDescription = "exprTarget".rs
                                            }
                                            .visibility(visibilityExpr)
                                            .size(100.rdp, 40.rdp)
                                            .background(Color(0xFFFFC107).rc)
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.waitForIdle()
            rule.onNodeWithContentDescription("exprTarget").assertExists()

            rule.onNode(hasClickAction()).performClick()
            rule.waitForIdle()

            // The expression recomputed to 0 (GONE) reactively, so the target stops composing.
            rule.onNodeWithContentDescription("exprTarget").assertDoesNotExist()
        }
    }

    // A *derived value* (here derived text — `n.toRemoteString()`, a TextFromFloat op) must update
    // when its input changes. The click value-changes `n`; the embedded player recomputes the
    // derived
    // op inside its derivedStateOf resolver (rememberRemoteStringAsState over LocalValueOps), so
    // the
    // displayed text changes from "0" to "7". Before derived values were made reactive, the text
    // was
    // computed once at setup and never updated.
    @Test
    fun derivedTextUpdatesReactively() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val n = remember { MutableRemoteInt(0) }
                            val label = n.toRemoteString()
                            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color(0xFF3F51B5).rc)
                                            .clickable(action = valueChange(n, 7.ri))
                                )
                                RemoteText(label)
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.waitForIdle()
            rule.onNodeWithText("0").assertExists()

            rule.onNode(hasClickAction()).performClick()
            rule.waitForIdle()

            rule.onNodeWithText("7").assertExists()
            rule.onNodeWithText("0").assertDoesNotExist()
        }
    }

    // A *chained* derivation: text <- (n * 2) <- n. The displayed text depends on an integer
    // expression that depends on the host variable, and the intermediate expression is not itself
    // displayed. The compose-state-driven recompute re-runs the document's dependency-ordered
    // updateVariables across passes, so changing n flips the text 0 -> 6 even though nothing reads
    // the intermediate directly.
    @Test
    fun chainedDerivedValueUpdatesReactively() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val n = remember { MutableRemoteInt(0) }
                            val doubled = n * 2.ri // intermediate IntegerExpression, not displayed
                            val label = doubled.toRemoteString()
                            RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color(0xFF3F51B5).rc)
                                            .clickable(action = valueChange(n, 3.ri))
                                )
                                RemoteText(label)
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }

            rule.waitForIdle()
            rule.onNodeWithText("0").assertExists()

            rule.onNode(hasClickAction()).performClick()
            rule.waitForIdle()

            rule.onNodeWithText("6").assertExists()
        }
    }

    // Authors a `mFloatAnimation`-backed size via `animateRemoteFloat` (animate-in from 0 to 123.45
    // over 1s, linear) and asserts the embedded player grows the box across time samples. This is
    // the
    // appearance-animation path, made to work by the Compose-native animation layer: the dimension
    // resolves the raw variable id, the expression carries a `mFloatAnimation`, so resolution
    // routes
    // to `rememberAnimatedRemoteFloat`, which seeds a Compose `Animatable` at the authored initial
    // value and `animateTo`s the expression's (reactive) source target with the spec's duration and
    // easing. The animation is driven by Compose's own frame clock — neither the core's per-frame
    // animation math nor the player's frame loop is involved — so it sidesteps the core overwriting
    // an appearance animation's initial value with its target on first evaluation.
    @Test
    fun testAnimationSupport() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val target = remember { MutableRemoteFloat(123.45f) }
                            // Animate from 0 to `target` over 1s, linearly.
                            val animatedSize =
                                animateRemoteFloatAsState(
                                    targetValue = target,
                                    animationSpec =
                                        remoteTween(
                                            durationMillis = 1000,
                                            easing = RemoteEasing.Linear,
                                        ),
                                    initialValue = 0f,
                                )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "animated".rs }
                                        .width(animatedSize)
                                        .height(animatedSize)
                            )
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.mainClock.autoAdvance = false

            rule.setContent {
                Box(modifier = Modifier.size(200.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }

            // Target the animated RemoteBox directly via its contentDescription.
            val boxNode = rule.onNodeWithContentDescription("animated")

            fun widthOf() =
                boxNode.getUnclippedBoundsInRoot().let { it.right.value - it.left.value }

            rule.mainClock.advanceTimeBy(100)
            val initialWidth = widthOf()

            rule.mainClock.advanceTimeBy(400)
            val midWidth = widthOf()

            rule.mainClock.advanceTimeBy(400)
            val endWidth = widthOf()

            // The size animates 0 -> 123.45, so each later sample must be strictly larger.
            assert(midWidth > initialWidth) {
                "Expected mid width ($midWidth) > initial width ($initialWidth)"
            }
            assert(endWidth > midWidth) { "Expected end width ($endWidth) > mid width ($midWidth)" }
        }
    }

    // A time-driven size — width/height keyed to `TIME_IN_SEC * scale` — must grow as the clock
    // advances. This is the genuinely-supported, fully compose-native animation path and is what
    // the
    // reactive-dimension fix in WidthModifier/HeightModifier unblocks: the size op references a
    // FloatExpression containing ID_TIME_IN_SEC, so `isTimeDependent` keeps the frame loop running,
    // and resolving the op's *raw* variable id routes through `rememberRemoteExpression`'s
    // `derivedStateOf` tree, whose ID_TIME_IN_SEC leaf is bridged to `LocalCurrentTimeMillis`. No
    // polling, no `Animatable` — the layout recomposes from the time state each frame, like
    // Compose.
    // (Reading the core-flattened `getValue()` instead, as before the fix, left the size frozen at
    // its t=0 snapshot.)
    @Test
    fun testTimeDrivenSizeAnimates() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            // width = height = ANIMATION_TIME * 100 (px). The embedded player
                            // bridges ANIMATION_TIME to the frame loop's elapsed millis / 1000,
                            // so this grows monotonically from 0 as the clock advances.
                            val size = RemoteFloat(RemoteContext.FLOAT_ANIMATION_TIME) * 100f
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "timed".rs }
                                        .width(size)
                                        .height(size)
                            )
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.mainClock.autoAdvance = false

            rule.setContent {
                Box(modifier = Modifier.size(300.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }

            val boxNode = rule.onNodeWithContentDescription("timed")

            fun widthOf() =
                boxNode.getUnclippedBoundsInRoot().let { it.right.value - it.left.value }

            rule.mainClock.advanceTimeBy(200)
            val initialWidth = widthOf()

            rule.mainClock.advanceTimeBy(400)
            val midWidth = widthOf()

            rule.mainClock.advanceTimeBy(400)
            val endWidth = widthOf()

            // Size scales with elapsed time, so each later sample must be strictly larger.
            assert(midWidth > initialWidth) {
                "Expected mid width ($midWidth) > initial width ($initialWidth)"
            }
            assert(endWidth > midWidth) { "Expected end width ($endWidth) > mid width ($midWidth)" }
        }
    }

    @Test
    fun testWallClockTimeVariablesWithFixedClock() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val baseInstant = Instant.parse("2026-01-01T01:02:03.500Z")
            val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val hrWidth = RemoteFloat(RemoteContext.FLOAT_TIME_IN_HR) * 10f
                            val minWidth = RemoteFloat(RemoteContext.FLOAT_TIME_IN_MIN) * 2f
                            val secWidth = RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC) * 2f
                            val contWidth = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 2f

                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "hrBox".rs }
                                        .width(hrWidth)
                                        .height(10.rdp)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "minBox".rs }
                                        .width(minWidth)
                                        .height(10.rdp)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "secBox".rs }
                                        .width(secWidth)
                                        .height(10.rdp)
                            )
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics { contentDescription = "contBox".rs }
                                        .width(contWidth)
                                        .height(10.rdp)
                            )
                        },
                    )
                    .bytes

            val document =
                CoreDocument(fixedClock).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.mainClock.autoAdvance = false

            rule.setContent {
                Box(modifier = Modifier.size(300.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            // Advance 2 frames (32ms) so LaunchedEffect starts and captures startMillis at
            // frameMillis = 0.
            rule.mainClock.advanceTimeBy(32)

            fun widthOf(desc: String) =
                rule.onNodeWithContentDescription(desc).getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            // At t=0 (01:02:03.500Z):
            // TIME_IN_HR = 1 (width = 10), TIME_IN_MIN = 1*60 + 2 = 62 (width = 124),
            // TIME_IN_SEC = 2*60 + 3 = 123 (width = 246), CONTINUOUS_SEC = 123.5 (width = 247)
            assertThat(widthOf("hrBox")).isWithin(0.1f).of(10f)
            assertThat(widthOf("minBox")).isWithin(0.1f).of(124f)
            assertThat(widthOf("secBox")).isWithin(0.1f).of(246f)
            assertThat(widthOf("contBox")).isWithin(0.1f).of(247f)

            // Advance 32 frames (32 * 16ms = 512ms) to 01:02:04.012Z:
            // TIME_IN_SEC advances to 124 (width = 248), CONTINUOUS_SEC is ~124.012 (width = 248)
            rule.mainClock.advanceTimeBy(512)
            assertThat(widthOf("secBox")).isWithin(0.1f).of(248f)
            assertThat(widthOf("contBox")).isWithin(0.1f).of(248f)

            // Advance another 32 frames (512ms, total 1024ms) to 01:02:04.524Z:
            // TIME_IN_SEC remains 124 (width = 248), CONTINUOUS_SEC is ~124.524 (width = 249)
            rule.mainClock.advanceTimeBy(512)
            assertThat(widthOf("secBox")).isWithin(0.1f).of(248f)
            assertThat(widthOf("contBox")).isWithin(0.1f).of(249f)
        }
    }

    @Test
    fun testDiscreteTimeDocumentSleepsBetweenSecondBoundaries() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val baseInstant = Instant.parse("2026-01-01T01:02:03.200Z")
            val fixedClock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))

            // Document reads ONLY discrete time variables (FLOAT_TIME_IN_SEC), with no continuous
            // variables (no FLOAT_CONTINUOUS_SEC / FLOAT_ANIMATION_TIME).
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val secWidth = RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC) * 2f
                            RemoteBox(
                                modifier =
                                    RemoteModifier.semantics {
                                            contentDescription = "secOnlyBox".rs
                                        }
                                        .width(secWidth)
                                        .height(10.rdp)
                            )
                        },
                    )
                    .bytes

            val document =
                CoreDocument(fixedClock).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.mainClock.autoAdvance = false

            rule.setContent {
                Box(modifier = Modifier.size(300.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            // Start the LaunchedEffect (captures startMillis and enters delay(millisToNextSecond))
            rule.mainClock.advanceTimeBy(32)

            fun widthOf(desc: String) =
                rule.onNodeWithContentDescription(desc).getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            // At t=0 (01:02:03.200Z): TIME_IN_SEC = 123 (width = 246)
            assertThat(widthOf("secOnlyBox")).isWithin(0.1f).of(246f)

            // Advance 500ms (total 532ms -> 01:02:03.732Z): still within second 03;
            // coroutine is asleep in delay() until 01:02:04.000Z.
            rule.mainClock.advanceTimeBy(500)
            assertThat(widthOf("secOnlyBox")).isWithin(0.1f).of(246f)

            // Advance 300ms (total 832ms -> 01:02:04.032Z): crosses second boundary;
            // delay() resumes and updates TIME_IN_SEC to 124 (width = 248).
            rule.mainClock.advanceTimeBy(300)
            assertThat(widthOf("secOnlyBox")).isWithin(0.1f).of(248f)

            // Advance another full second (1000ms -> 01:02:05.032Z):
            // TIME_IN_SEC advances to 125 (width = 250).
            rule.mainClock.advanceTimeBy(1000)
            assertThat(widthOf("secOnlyBox")).isWithin(0.1f).of(250f)
        }
    }

    // End-to-end pipeline coverage for every RPN operator the creation API can author. Each box is
    // sized by an operator expression that evaluates to the same 60px width as a constant control
    // box. Crucially, each operator is fed a *variable* (rememberMutableRemoteFloat) input so the
    // creation API can't constant-fold the operator away — the opcode really lands in the document
    // and is exercised by the player. Rendering through RcPlayer and asserting equal measured
    // widths
    // proves the full path: capture -> FloatExpression -> dimension modifier ->
    // rememberRemoteExpression -> parseRpn -> eval -> layout. (Operator *math* is exhaustively
    // checked
    // against the core evaluator in RcPlayerExpressionTest; this proves they drive layout when
    // played.)
    @Test
    fun everyOperatorDrivesLayoutEndToEnd() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val renderedTags = mutableListOf<String>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            // Variable inputs (defeat constant folding); all resolve so each
                            // expression equals 60.
                            val v = remember { MutableRemoteFloat(60f) }
                            val half = remember { MutableRemoteFloat(0.5f) }
                            val one = remember { MutableRemoteFloat(1f) }
                            val thousand = remember { MutableRemoteFloat(1000f) }
                            val a = v * 0.6f // 36
                            val b = v * 0.8f // 48

                            val cases: List<Pair<String, RemoteFloat>> =
                                listOf(
                                    "control" to v,
                                    "add" to v + 0f,
                                    "sub" to v - 0f,
                                    "mul" to v * 1f,
                                    "div" to v / 1f,
                                    "mod" to v.rem(100f),
                                    "neg" to -(-v),
                                    "max" to max(v, 10f),
                                    "min" to min(v, 100f),
                                    "pow" to pow(v, 1f),
                                    "sqrt" to sqrt(v * v),
                                    "cbrt" to cbrt(v * v * v),
                                    "abs" to abs(-v),
                                    "sign" to sign(v) * v,
                                    "copySign" to copySign(v, 1f),
                                    "expLn" to exp(ln(v)),
                                    "ceil" to ceil(v - 0.8f),
                                    "floor" to floor(v + 0.8f),
                                    "round" to round(v - 0.3f),
                                    "sinAsin" to sin(asin(half)) * 120f,
                                    "cosAcos" to cos(acos(half)) * 120f,
                                    "tanAtan" to tan(atan(one)) * v,
                                    "atan2" to atan2(one, 1f) * 76.39437f,
                                    "degRad" to toDeg(toRad(v)),
                                    "log" to log(thousand) * 20f,
                                    "clamp" to clamp(v * 4f, 0f, 60f),
                                    "lerp" to lerp(0f, 120f, half),
                                    "mad" to mad(v, one, RemoteFloat(0f)),
                                    // compound: sqrt(36^2 + 48^2) == 60
                                    "compound" to sqrt(a * a + b * b),
                                )

                            RemoteColumn(modifier = RemoteModifier.size(400.rdp)) {
                                cases.forEach { (tag, expr) ->
                                    renderedTags.add(tag)
                                    RemoteBox(
                                        modifier =
                                            RemoteModifier.semantics { contentDescription = tag.rs }
                                                .width(expr)
                                                .height(8.rdp)
                                    )
                                }
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(400.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            rule.waitForIdle()

            fun widthOf(tag: String) =
                rule.onNodeWithContentDescription(tag).getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            val control = widthOf("control")
            assert(control > 0f) { "Control box has zero width — pipeline not rendering" }
            renderedTags.forEach { tag ->
                val w = widthOf(tag)
                assert(mathAbs(w - control) < 1f) {
                    "Operator '$tag' produced width $w but expected ~$control (all evaluate to 60px)"
                }
            }
        }
    }

    // widthIn(min/max) is authored as a DimensionConstraintsModifierOperation by the creation API;
    // before it was wired in the player it was silently dropped. Verify both bounds actually clamp
    // the measured layout: a max-constrained box around an oversized child stays ~max, and a
    // min-constrained box around a tiny child grows to ~min.
    @Test
    fun dimensionConstraintsClampMeasuredSize() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteColumn(modifier = RemoteModifier.size(400.rdp)) {
                                // max: child wants 200 but the box is capped at 50.
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "maxc".rs }
                                            .widthIn(max = 50.rdp)
                                ) {
                                    RemoteBox(modifier = RemoteModifier.size(200.rdp))
                                }
                                // min: child wants 10 but the box floors at 80.
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "minc".rs }
                                            .widthIn(min = 80.rdp)
                                ) {
                                    RemoteBox(modifier = RemoteModifier.size(10.rdp))
                                }
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(400.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            rule.waitForIdle()

            fun widthOf(tag: String) =
                rule.onNodeWithContentDescription(tag).getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            val maxW = widthOf("maxc")
            val minW = widthOf("minc")
            assert(mathAbs(maxW - 50f) < 3f) {
                "max-constrained width $maxW, expected ~50dp"
            }
            assert(mathAbs(minW - 80f) < 3f) {
                "min-constrained width $minW, expected ~80dp"
            }
        }
    }

    // FlowLayout previously ignored maxItemsInEachRow (bare FlowRow). With it honored, 3 boxes at
    // maxItemsInEachRow=2 must wrap: f0/f1 on row one, f2 on row two. (Without the fix all three
    // fit
    // on one row in the 300dp flow, so this discriminates the behavior.)
    @Test
    fun flowLayoutWrapsAtMaxItemsInEachRow() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        profile = experimentalProfile,
                        content = {
                            RemoteFlowRow(
                                modifier = RemoteModifier.size(300.rdp),
                                maxItemsInEachRow = 2,
                            ) {
                                repeat(3) { i ->
                                    RemoteBox(
                                        modifier =
                                            RemoteModifier.semantics {
                                                    contentDescription = "f$i".rs
                                                }
                                                .size(50.rdp)
                                    )
                                }
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(300.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            rule.waitForIdle()

            fun boundsOf(tag: String) =
                rule.onNodeWithContentDescription(tag).getUnclippedBoundsInRoot()

            val f0 = boundsOf("f0")
            val f1 = boundsOf("f1")
            val f2 = boundsOf("f2")

            // f0 and f1 share the first row.
            assert(mathAbs(f0.top.value - f1.top.value) < 1f) {
                "f0/f1 should share a row (tops ${f0.top.value} vs ${f1.top.value})"
            }
            // f2 wrapped to the next row (its top is at/below f0's bottom).
            assert(f2.top.value >= f0.bottom.value - 1f) {
                "f2 should wrap below row one (f2.top=${f2.top.value}, f0.bottom=${f0.bottom.value})"
            }
        }
    }

    // A clickable that fires a named host action must invoke RcPlayer.onNamedAction with the
    // resolved name + value. Verifies the host-action wiring (HostNamedActionOperation ->
    // callback).
    @Test
    fun hostNamedActionInvokesCallback() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp)
                                        .clickable(action = hostAction("nav".rs, 7.ri))
                            )
                        },
                    )
                    .bytes
            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            var firedName: String? = null
            var firedValue: Any? = null
            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(
                        document = document,
                        onNamedAction = { name, value, _ ->
                            firedName = name
                            firedValue = value
                        },
                    )
                }
            }
            rule.waitForIdle()
            rule.onNode(hasClickAction()).performClick()
            rule.waitForIdle()

            assert(firedName == "nav") { "Expected named action 'nav', got $firedName" }
            assert((firedValue as? Number)?.toInt() == 7) { "Expected value 7, got $firedValue" }
        }
    }

    // A collapsible column shorter than its content drops the overflowing children: a 100dp-tall
    // column with three 40dp children keeps the first two (80dp fits) and collapses the third. With
    // no priority modifiers every child defaults to Float.MAX_VALUE, so the tie drops the last one.
    @Test
    fun collapsibleColumnDropsOverflowingChildren() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteCollapsibleColumn(modifier = RemoteModifier.size(100.rdp)) {
                                repeat(3) { i ->
                                    RemoteBox(
                                        modifier =
                                            RemoteModifier.semantics {
                                                    contentDescription = "c$i".rs
                                                }
                                                .size(40.rdp)
                                    )
                                }
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(200.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            rule.waitForIdle()

            rule.onNodeWithContentDescription("c0").assertIsDisplayed()
            rule.onNodeWithContentDescription("c1").assertIsDisplayed()
            // The third child overflowed the 100dp budget and was collapsed (measured, not placed).
            rule.onNodeWithContentDescription("c2").assertIsNotDisplayed()
        }
    }

    // A RemoteFitBox displays only the first child whose natural size fits the available space. In
    // a
    // 100dp box, a 200dp child doesn't fit and a 50dp child does, so only the 50dp child is shown.
    @Test
    fun remoteFitBoxDisplaysTheChildThatFits() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            RemoteFitBox(modifier = RemoteModifier.size(100.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "big".rs }
                                            .size(200.rdp)
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.semantics { contentDescription = "small".rs }
                                            .size(50.rdp)
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(300.dp).testTag("playerParent")) {
                    RcPlayer(document = document)
                }
            }
            rule.waitForIdle()

            // The 200dp child doesn't fit the 100dp box; the 50dp one does and is the chosen child.
            rule.onNodeWithContentDescription("big").assertIsNotDisplayed()
            rule.onNodeWithContentDescription("small").assertIsDisplayed()
        }
    }

    // The document's root content description (Header DOC_CONTENT_DESCRIPTION /
    // RootContentDescription)
    // must label the player for accessibility — exposed as a semantics contentDescription on the
    // root.
    @Test
    fun rootContentDescriptionLabelsThePlayer() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = { RemoteBox(modifier = RemoteModifier.size(100.rdp)) },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                    setContentDescription("a weather card")
                }

            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }
            rule.waitForIdle()

            rule.onNodeWithContentDescription("a weather card").assertIsDisplayed()
        }
    }

    // Scrolling publishes the live offset to the document's scroll-position variable, so
    // expressions
    // bound to it react. A marker box's width is bound to that variable; after swiping the
    // scrollable
    // area the marker must widen — proving the offset reached the variable (not just native
    // scroll).
    @Test
    fun scrollPublishesPositionToBoundVariable() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            val scrollState = remember { RemoteScrollState() }
                            RemoteColumn(modifier = RemoteModifier.size(200.rdp)) {
                                // A 100dp viewport scrolling a 400dp-tall column.
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp)
                                            .verticalScroll(scrollState)
                                            .semantics { contentDescription = "scroller".rs }
                                ) {
                                    RemoteColumn {
                                        repeat(5) {
                                            RemoteBox(modifier = RemoteModifier.size(80.rdp))
                                        }
                                    }
                                }
                                // Marker width is bound to the scroll-position variable.
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.width(scrollState.positionState)
                                            .height(10.rdp)
                                            .semantics { contentDescription = "marker".rs }
                                )
                            }
                        },
                    )
                    .bytes

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(documentBytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) }
            }
            rule.waitForIdle()

            fun markerWidth() =
                rule.onNodeWithContentDescription("marker").getUnclippedBoundsInRoot().let {
                    it.right.value - it.left.value
                }

            val before = markerWidth()
            rule.onNodeWithContentDescription("scroller").performTouchInput { swipeUp() }
            rule.waitForIdle()
            val after = markerWidth()

            assert(after > before + 1f) {
                "Scrolling should publish the offset to the bound variable (marker width); " +
                    "before=$before after=$after"
            }
        }
    }

    @Test
    fun lambdaActionIsStable() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .clickable(action = lambdaAction { /* empty */ })
                )
            }

            // Capture 1
            val capturedDocument1 =
                captureSingleRemoteDocument(context = context, content = content)

            // Capture 2
            val capturedDocument2 =
                captureSingleRemoteDocument(context = context, content = content)

            // Verify stability of keys/ids
            val keys1 = mutableSetOf<Int>()
            capturedDocument1.lambdas.forEach { key, _ -> keys1.add(key) }
            val keys2 = mutableSetOf<Int>()
            capturedDocument2.lambdas.forEach { key, _ -> keys2.add(key) }

            assert(keys1.isNotEmpty()) { "Expected at least one lambda" }
            assert(keys1 == keys2) { "Expected lambda IDs to be stable, but got $keys1 and $keys2" }
        }
    }

    @Test
    fun lambdaActionInvokesCallback() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            var lambdaCalled = false

            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .clickable(action = lambdaAction { lambdaCalled = true })
                )
            }

            val capturedDocument = captureSingleRemoteDocument(context = context, content = content)

            // Verify it works
            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) {
                    RcPlayer(capturedDocument = capturedDocument)
                }
            }
            rule.waitForIdle()
            rule.onNode(hasClickAction()).performClick()
            rule.waitForIdle()

            assert(lambdaCalled) { "Expected lambda to be called" }
        }
    }

    @Test
    fun testStateLayoutMultiPageScoring() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            var uploadedScore = -1

            val content: @Composable @RemoteComposable () -> Unit = {
                val currentPage = remember { MutableRemoteInt(0) }
                val choices = (0..2).map { remember { MutableRemoteInt(-1) } }
                val answers = listOf(1, 2, 0)
                val scores = (0..2).map { i -> choices[i].isEqualTo(answers[i].ri).toRemoteInt() }
                val totalScore = remember { scores.reduce { acc, s -> acc + s } }

                RemoteBox(modifier = RemoteModifier.size(400.rdp)) {
                    Hoist(totalScore)
                    RemoteStateLayout(currentState = currentPage, 0, 1, 2, 3) { pageIndex: Int ->
                        if (pageIndex < 3) {
                            RemoteBox(modifier = RemoteModifier.size(400.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp)
                                            .semantics {
                                                contentDescription = "P${pageIndex}_Opt_Correct".rs
                                            }
                                            .clickable(
                                                valueChange(
                                                    choices[pageIndex],
                                                    answers[pageIndex].ri,
                                                )
                                            )
                                )
                                val nextAction =
                                    if (pageIndex < 2) {
                                        combinedAction(
                                            valueChange(currentPage, (pageIndex + 1).ri),
                                            valueChange(choices[pageIndex + 1], (-1).ri),
                                        )
                                    } else {
                                        valueChange(currentPage, (pageIndex + 1).ri)
                                    }
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(50.rdp)
                                            .semantics {
                                                contentDescription = "P${pageIndex}_Next".rs
                                            }
                                            .clickable(nextAction)
                                )
                            }
                        } else {
                            val retakeAction =
                                combinedAction(
                                    valueChange(currentPage, 0.ri),
                                    *choices.map { valueChange(it, (-1).ri) }.toTypedArray(),
                                )
                            RemoteBox(modifier = RemoteModifier.size(200.rdp)) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp)
                                            .semantics { contentDescription = "Submit".rs }
                                            .clickable(hostAction("submit".rs, value = totalScore))
                                )
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp)
                                            .padding(top = 100.rdp)
                                            .semantics { contentDescription = "Retake".rs }
                                            .clickable(retakeAction)
                                )
                            }
                        }
                    }
                }
            }

            val capturedDocument = captureSingleRemoteDocument(context = context, content = content)
            rule.setContent {
                Box(modifier = Modifier.size(400.dp)) {
                    RcPlayer(
                        capturedDocument = capturedDocument,
                        onNamedAction = { name, value, _ ->
                            if (name == "submit") {
                                uploadedScore = (value as? Number)?.toInt() ?: -1
                            }
                        },
                    )
                }
            }
            rule.waitForIdle()

            // Page 0: click correct option then Next
            rule.onNodeWithContentDescription("P0_Opt_Correct").performClick()
            rule.waitForIdle()
            rule.onNodeWithContentDescription("P0_Next").performClick()
            rule.waitForIdle()

            // Page 1: click correct option then Next
            rule.onNodeWithContentDescription("P1_Opt_Correct").performClick()
            rule.waitForIdle()
            rule.onNodeWithContentDescription("P1_Next").performClick()
            rule.waitForIdle()

            // Page 2: click correct option then Next
            rule.onNodeWithContentDescription("P2_Opt_Correct").performClick()
            rule.waitForIdle()
            rule.onNodeWithContentDescription("P2_Next").performClick()
            rule.waitForIdle()

            // Page 3: Submit
            rule.onNodeWithContentDescription("Submit").performClick()
            rule.waitForIdle()

            assertThat(uploadedScore).isEqualTo(3)

            // Click Retake
            rule.onNodeWithContentDescription("Retake").performClick()
            rule.waitForIdle()
        }
    }

    @Test
    fun multiClickModifier_dispatchesSingleDoubleAndLongClicks() {
        val modifiers =
            ComponentModifiers().apply {
                add(
                    MultiClickModifier(MultiClickModifier.CLICK_TYPE_SINGLE).apply {
                        list.add(HostActionOperation(101))
                    }
                )
                add(
                    MultiClickModifier(MultiClickModifier.CLICK_TYPE_DOUBLE).apply {
                        list.add(HostActionOperation(102))
                    }
                )
                add(
                    MultiClickModifier(MultiClickModifier.CLICK_TYPE_LONG).apply {
                        list.add(HostActionOperation(103))
                    }
                )
            }

        val remoteContext = AndroidRemoteContext()
        val document = CoreDocument(RemoteClock.SYSTEM)
        val triggeredIds = mutableListOf<Int>()
        rule.setContent {
            CompositionLocalProvider(
                LocalCoreDocument provides document,
                LocalRemoteContext provides remoteContext,
                LocalRemoteActionHandler provides { id, _ -> triggeredIds.add(id) },
            ) {
                Box(modifier = modifiers.toModifier().size(100.dp))
            }
        }
        rule.waitForIdle()

        rule.onNode(hasClickAction()).performClick()
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        assertThat(triggeredIds).contains(101)

        rule.onNode(hasClickAction()).performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        assertThat(triggeredIds).contains(102)

        rule.onNode(hasClickAction()).performTouchInput { longClick() }
        rule.waitForIdle()
        assertThat(triggeredIds).contains(103)
    }

    @Test
    fun goneComponent_collapsesLayoutBoundsToZero() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val content: @Composable @RemoteComposable () -> Unit = {
                RemoteRow {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp)
                                .visibility(Component.Visibility.GONE.ri)
                                .semantics { contentDescription = "GoneBox".rs }
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(80.rdp).semantics {
                                contentDescription = "VisibleSibling".rs
                            }
                    )
                }
            }
            val capturedDocument = captureSingleRemoteDocument(context = context, content = content)
            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) {
                    RcPlayer(capturedDocument = capturedDocument)
                }
            }
            rule.waitForIdle()

            // Because the preceding GONE box collapses to 0x0 in layout, VisibleSibling starts at
            // left = 0dp.
            val siblingBounds =
                rule.onNodeWithContentDescription("VisibleSibling").getUnclippedBoundsInRoot()
            assertThat(siblingBounds.left).isEqualTo(0.dp)
        }
    }

    @Test
    fun snapshotRemoteComposeState_preservesLiteralIntegerIdsAndCachedIntegers() {
        val state = SnapshotRemoteComposeState()

        // Caching, updating, or overriding floats under small IDs (< START_ID) must not clobber
        // literal integer ID lookups such as Component.Visibility.VISIBLE (1) or INVISIBLE (2).
        state.cacheFloat(1, 3.14f)
        state.updateFloat(1, 99.7f)
        state.cacheFloat(2, 2.71f)
        state.overrideFloat(2, 88.4f)
        state.clearFloatOverride(2)
        assertThat(state.getInteger(Component.Visibility.VISIBLE))
            .isEqualTo(Component.Visibility.VISIBLE)
        assertThat(state.getInteger(Component.Visibility.INVISIBLE))
            .isEqualTo(Component.Visibility.INVISIBLE)

        // Explicit integer updates (including small IDs and regular variable IDs) are still
        // returned.
        state.updateInteger(21, 42)
        state.updateInteger(100, 999)
        assertThat(state.getInteger(21)).isEqualTo(42)
        assertThat(state.getInteger(100)).isEqualTo(999)

        // Updating, overriding, and clearing float overrides on those same IDs must not alter
        // their integer values.
        state.updateFloat(21, 1.5f)
        state.overrideFloat(21, 9.5f)
        state.clearFloatOverride(21)
        state.updateFloat(100, 2.5f)
        state.overrideFloat(100, 8.5f)
        state.clearFloatOverride(100)
        assertThat(state.getInteger(21)).isEqualTo(42)
        assertThat(state.getInteger(100)).isEqualTo(999)

        // Float variables (>= START_ID) without explicit integer updates reflect their current
        // float value when read via getInteger (e.g. RemoteFloat.toRemoteInt()), even if read
        // before the first float update.
        assertThat(state.getInteger(101)).isEqualTo(0)
        state.updateFloat(101, 37.9f)
        assertThat(state.getInteger(101)).isEqualTo(37)
        state.overrideFloat(101, 64.2f)
        state.updateFloat(101, 99.9f)
        assertThat(state.getInteger(101)).isEqualTo(64)
        state.clearFloatOverride(101)
        state.updateFloat(101, 37.9f)
        assertThat(state.getInteger(101)).isEqualTo(37)
    }

    @Test
    fun rootPointerEvents_doNotInterfereWithComponentScroll() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val content: @Composable @RemoteComposable () -> Unit = {
                val scrollState = remember { RemoteScrollState() }
                RemoteColumn(modifier = RemoteModifier.size(100.rdp)) {
                    RemoteColumn(
                        modifier =
                            RemoteModifier.size(100.rdp, 80.rdp)
                                .verticalScroll(scrollState)
                                .semantics { contentDescription = "TouchRoot".rs }
                    ) {
                        repeat(4) { index ->
                            RemoteBox(
                                modifier =
                                    RemoteModifier.size(100.rdp, 40.rdp).semantics {
                                        contentDescription = "Item$index".rs
                                    }
                            )
                        }
                    }
                    RemoteBox(
                        modifier =
                            RemoteModifier.width(scrollState.positionState)
                                .height(10.rdp)
                                .semantics { contentDescription = "ScrollPosition".rs }
                    )
                }
            }
            val capturedDocument = captureSingleRemoteDocument(context = context, content = content)
            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(capturedDocument.bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) }
            }
            rule.waitForIdle()

            fun scrollOffset(): Float =
                rule
                    .onNodeWithContentDescription("ScrollPosition")
                    .fetchSemanticsNode()
                    .boundsInRoot
                    .width

            assertThat(scrollOffset()).isWithin(0.1f).of(0f)

            rule.onNodeWithContentDescription("TouchRoot").performTouchInput { swipeUp() }
            rule.waitForIdle()

            val expectedMaxScroll = 80f
            assertThat(scrollOffset()).isWithin(1f).of(expectedMaxScroll)
            assertThat(
                    rule.onNodeWithContentDescription("Item0").getUnclippedBoundsInRoot().top.value
                )
                .isWithin(1f)
                .of(-expectedMaxScroll)

            rule.onNodeWithContentDescription("TouchRoot").performTouchInput { swipeDown() }
            rule.waitForIdle()

            assertThat(scrollOffset()).isWithin(0.1f).of(0f)
            assertThat(
                    rule.onNodeWithContentDescription("Item0").getUnclippedBoundsInRoot().top.value
                )
                .isWithin(0.5f)
                .of(0f)
        }
    }

    /**
     * The player's root pointer handler publishes the touch position to ID_TOUCH_POS_X/Y on down
     * and drag, for documents that read it outside any component.
     */
    @Test
    fun rootPointerEvents_publishTouchPosition() {
        val writer = RemoteComposeWriterAndroid(100, 100, "test", AndroidxRcPlatformServices())
        writer.root {
            writer.getRcPaint().setColor(0xFFFFFFFF.toInt()).setStyle(0).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
            writer.getRcPaint().setColor(0xFFFF0000.toInt()).setStyle(0).commit()
            writer.drawRect(0f, 0f, RemoteContext.FLOAT_TOUCH_POS_X, 100f)
        }
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(writer.buffer(), 0, writer.bufferSize()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(100.dp).testTag("player")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()

        fun isRedAtFraction(fraction: Float): Boolean {
            val bitmap = rule.onNodeWithTag("player").captureToImage().asAndroidBitmap()
            val pixel = bitmap.getPixel((fraction * bitmap.width).toInt(), bitmap.height / 2)
            return AndroidColor.red(pixel) > 200 && AndroidColor.green(pixel) < 50
        }

        assertThat(isRedAtFraction(0.5f)).isFalse()

        rule.onNodeWithTag("player").performTouchInput { down(Offset(width * 0.3f, height / 2f)) }
        rule.waitForIdle()
        assertThat(isRedAtFraction(0.2f)).isTrue()
        assertThat(isRedAtFraction(0.5f)).isFalse()

        rule.onNodeWithTag("player").performTouchInput { moveTo(Offset(width * 0.8f, height / 2f)) }
        rule.waitForIdle()
        assertThat(isRedAtFraction(0.5f)).isTrue()
        assertThat(isRedAtFraction(0.9f)).isFalse()

        rule.onNodeWithTag("player").performTouchInput { up() }
    }

    /**
     * A TouchExpression directly under the root is bound by core to the RootLayoutComponent, so its
     * hit bounds are the player's size and it responds to touches anywhere on the player.
     */
    @Test
    fun rootPointerEvents_driveRootLevelTouchExpression() {
        val playerWidthPx = with(rule.density) { 100.dp.toPx() }
        rule.setRemoteContent {
            val creationState = LocalRemoteComposeCreationState.current
            // Touch X as a percentage of the player width. The 0.25 offset keeps the value off an
            // integer boundary so toRemoteInt is stable whether it truncates or rounds.
            val touchPercent = rememberRemoteFloatExpression {
                val touchId =
                    creationState.document.addTouch(
                        -1f,
                        -1f,
                        100f,
                        TouchExpression.STOP_ABSOLUTE_POS,
                        0f,
                        0,
                        null,
                        null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                        100f / playerWidthPx,
                        AnimatedFloatExpression.MUL,
                        0.25f,
                        AnimatedFloatExpression.ADD,
                    )
                RemoteFloat(touchId)
            }
            // Hoisted before any layout, so the TouchExpression is emitted directly under the root.
            Hoist(touchPercent)
            RemoteBox(
                modifier =
                    RemoteModifier.size(100.rdp).semantics { contentDescription = "Player".rs }
            ) {
                RemoteText(
                    touchPercent
                        .isLessThan(0f.rf)
                        .select("Unset".rs, touchPercent.toRemoteInt().toRemoteString())
                )
            }
        }

        val player = rule.onNodeWithContentDescription("Player")
        rule.onNodeWithText("Unset").assertExists()

        player.performTouchInput { down(Offset(width * 0.75f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("75").assertExists()

        player.performTouchInput { moveTo(Offset(width * 0.25f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("25").assertExists()

        player.performTouchInput { up() }
    }

    @Test
    fun clickableComponent_consumesWhenClickingInteractiveComponent() {
        var parentClicked = false
        val actionLog = mutableListOf<String>()
        rule.setRemoteContent(
            onNamedAction = { name, value, _ -> actionLog.add("$name:$value") },
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp).hostClickable { parentClicked = true }) {
                    content()
                }
            },
        ) {
            LeftBoxInteractiveContent(isClickable = true, isScrollable = false)
        }

        rule.onNodeWithContentDescription("LeftBox").performClick()
        rule.waitForIdle()

        assertThat(actionLog).containsExactly("myActionName:1")
        assertThat(parentClicked).isFalse()
    }

    @Test
    fun clickableComponent_consecutiveClicks_dispatchesEachClick() {
        var clickCount = 0
        rule.setRemoteContent(
            onNamedAction = { name, _, _ ->
                if (name == "myActionName") {
                    clickCount++
                }
            },
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp)) { content() }
            },
        ) {
            LeftBoxInteractiveContent(isClickable = true, isScrollable = false)
        }

        val leftBox = rule.onNodeWithContentDescription("LeftBox")
        leftBox.performClick()
        rule.waitForIdle()
        assertThat(clickCount).isEqualTo(1)

        leftBox.performClick()
        rule.waitForIdle()
        assertThat(clickCount).isEqualTo(2)
    }

    @Test
    fun clickableComponent_doesNotConsumeWhenClickingNonInteractiveComponent() {
        var parentClicked = false
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp).hostClickable { parentClicked = true }) {
                    content()
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = true, isScrollable = false)
        }

        // Click on the right side (inside the non-clickable box).
        rule.onNodeWithContentDescription("RightBox").performClick()
        rule.waitForIdle()

        assertThat(parentClicked).isTrue()
    }

    @Test
    fun clickableComponent_doesNotClickWhenReleasingDragGesture() {
        var actionTriggered = false
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            onNamedAction = { name, _, _ ->
                if (name == "myActionName") {
                    actionTriggered = true
                }
            },
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            },
        ) {
            LeftBoxInteractiveContent(isClickable = true, isScrollable = false)
        }

        // Swipe vertically starting inside the clickable left box -> host Column scrolls and click
        // action does not trigger on drag release.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()

        assertThat(actionTriggered).isFalse()
        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun touchUpComponent_doesNotTriggerActionWhenReleasingDragGesture() {
        var actionTriggered = false
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            onNamedAction = { name, _, _ ->
                if (name == "myActionName") {
                    actionTriggered = true
                }
            },
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            },
        ) {
            LeftBoxInteractiveContent(
                isClickable = false,
                isScrollable = false,
                isTouchUp = true,
            )
        }

        // A clean tap fires onTouchUp.
        rule.onNodeWithContentDescription("LeftBox").performClick()
        rule.waitForIdle()
        assertThat(actionTriggered).isTrue()

        actionTriggered = false
        // Swiping starting inside the touchUp box scrolls the host Column and cancels onTouchUp.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()

        assertThat(actionTriggered).isFalse()
        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun scrollableComponent_propagatesClickToParentWhenClickingNonClickableScrollableComponent() {
        var parentClicked = false
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp).hostClickable { parentClicked = true }) {
                    content()
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        // Click on the left box (scrollable, not clickable) -> propagates to Compose host parent.
        rule.onNodeWithContentDescription("LeftBox").performClick()
        rule.waitForIdle()

        assertThat(parentClicked).isTrue()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollableComponent_propagatesDoubleTapToParentWhenClickingNonClickableScrollableComponent() {
        var singleClickCount = 0
        var doubleClickCount = 0
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .hostCombinedClickable(
                                onClick = { singleClickCount++ },
                                onDoubleClick = { doubleClickCount++ },
                            )
                ) {
                    content()
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        // Double-click on the left box (scrollable, not clickable) -> propagates to Compose host
        // parent.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()

        assertThat(doubleClickCount).isEqualTo(1)
        assertThat(singleClickCount).isEqualTo(0)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollableComponent_propagatesLongClickToParentWhenClickingNonClickableScrollableComponent() {
        var parentLongClicked = false
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .hostCombinedClickable(
                                onClick = {},
                                onLongClick = { parentLongClicked = true },
                            )
                ) {
                    content()
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        rule.onNodeWithContentDescription("LeftBox").performTouchInput { longClick() }
        rule.waitForIdle()

        assertThat(parentLongClicked).isTrue()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollableComponent_propagatesLongClickDuringHoldBeforeTouchUp() {
        var parentLongClicked = false
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .hostCombinedClickable(
                                onClick = {},
                                onLongClick = { parentLongClicked = true },
                            )
                ) {
                    content()
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        val leftBox = rule.onNodeWithContentDescription("LeftBox")
        leftBox.performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100L)
        }
        rule.mainClock.advanceTimeBy(600L)
        rule.waitForIdle()

        // Host parent receives long click during the hold phase before finger is released.
        assertThat(parentLongClicked).isTrue()

        leftBox.performTouchInput { up() }
        rule.waitForIdle()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollableComponent_releasingLongPressDoesNotTriggerRegularClick() {
        var parentClicked = false
        var parentLongClicked = false
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(
                    modifier =
                        Modifier.size(300.dp)
                            .hostCombinedClickable(
                                onClick = { parentClicked = true },
                                onLongClick = { parentLongClicked = true },
                            )
                ) {
                    content()
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        rule.onNodeWithContentDescription("LeftBox").performTouchInput { longClick() }
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()

        assertThat(parentLongClicked).isTrue()
        assertThat(parentClicked).isFalse()
    }

    @Test
    fun scrollableComponent_onlyConsumesWhenScrollingInteractiveComponent() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        // 1. Swipe vertically on the left box (scrollable) -> consumed by the remote scrollable
        // box, so the host Column does NOT scroll.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertThat(hostScrollState.value).isEqualTo(0)

        // 2. Swipe vertically on the right box (non-scrollable) -> not consumed by RcPlayer, so
        // the host Column DOES scroll.
        rule.onNodeWithContentDescription("RightBox").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun scrollableComponent_withContentThatFits_doesNotBlockHostScroll() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            }
        ) {
            LeftBoxInteractiveContent(isScrollable = true, scrollContentFits = true)
        }

        // The remote scroller has nothing to scroll, so the drag scrolls the host.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun scrollableComponent_insideHostLazyColumn_onlyScrollsHostOnNonScrollableArea() {
        lateinit var hostLazyListState: LazyListState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostLazyListState = rememberLazyListState()
                LazyColumn(state = hostLazyListState, modifier = Modifier.size(300.dp)) {
                    item { Box(modifier = Modifier.size(300.dp)) { content() } }
                    item { Box(modifier = Modifier.size(300.dp)) }
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = true)
        }

        // 1. Swipe vertically on the scrollable left box -> host LazyColumn does not scroll.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertThat(hostLazyListState.firstVisibleItemIndex).isEqualTo(0)
        assertThat(hostLazyListState.firstVisibleItemScrollOffset).isEqualTo(0)

        // 2. Swipe vertically on the non-scrollable right box -> host LazyColumn scrolls.
        rule.onNodeWithContentDescription("RightBox").performTouchInput { swipeUp() }
        rule.waitForIdle()
        val scrolled =
            hostLazyListState.firstVisibleItemIndex > 0 ||
                hostLazyListState.firstVisibleItemScrollOffset > 0
        assertThat(scrolled).isTrue()
    }

    @Test
    fun nonInteractiveComponent_insideScrollableHostColumn_scrollsHostOnVerticalDrag() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            }
        ) {
            LeftBoxInteractiveContent(isClickable = false, isScrollable = false)
        }

        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()

        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun clickableWithScrollableComponent_insideScrollableHostColumn_doesNotScrollHostOnVerticalDrag() {
        var actionTriggered = false
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            onNamedAction = { name, _, _ ->
                if (name == "myActionName") {
                    actionTriggered = true
                }
            },
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            },
        ) {
            LeftBoxInteractiveContent(isClickable = true, isScrollable = true)
        }

        rule.onNodeWithContentDescription("LeftBox").performTouchInput { swipeUp() }
        rule.waitForIdle()

        assertThat(actionTriggered).isFalse()
        assertThat(hostScrollState.value).isEqualTo(0)
    }

    @Test
    fun clickableWithTouchExpression_insideScrollableHostColumn_doesNotScrollHostOnVerticalDrag() {
        var actionTriggered = false
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            onNamedAction = { name, _, _ ->
                if (name == "myActionName") {
                    actionTriggered = true
                }
            },
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(modifier = Modifier.size(300.dp).hostVerticalScroll(hostScrollState)) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            },
        ) {
            LeftBoxInteractiveContent(isClickable = true, hasTouchExpression = true)
        }

        rule.onNodeWithText("0").assertExists()

        // Dragging inside the left box (which has clickable + TouchExpression) drives the
        // TouchExpression and is consumed by RcPlayer, so the host Column does not scroll and
        // the click action does not fire.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput {
            swipe(
                start = Offset(width / 2f, 200f),
                end = Offset(width / 2f, 100f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()

        assertThat(actionTriggered).isFalse()
        assertThat(hostScrollState.value).isEqualTo(0)
        // TouchExpression in delta mode started at 0, dragged by -100 clamped to [0, 300] ->
        // dragging down from 100 to 220 increases value to 120.
        rule.onNodeWithContentDescription("LeftBox").performTouchInput {
            swipe(
                start = Offset(width / 2f, 100f),
                end = Offset(width / 2f, 220f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("120").assertExists()
    }

    @Test
    fun verticallyScrollableComponent_insideHorizontallyScrollableHostRow_scrollsHostWhenDragIsHorizontal() {
        lateinit var hostHorizontalScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostHorizontalScrollState = rememberScrollState()
                Row(
                    modifier = Modifier.size(300.dp).hostHorizontalScroll(hostHorizontalScrollState)
                ) {
                    Box(modifier = Modifier.size(300.dp)) { content() }
                    Box(modifier = Modifier.size(300.dp))
                }
            }
        ) {
            val scrollState = remember { RemoteScrollState() }
            RemoteColumn(
                modifier =
                    RemoteModifier.size(300.rdp).verticalScroll(scrollState).semantics {
                        contentDescription = "VerticalScrollPlayer".rs
                    }
            ) {
                repeat(25) { RemoteBox(modifier = RemoteModifier.size(300.rdp, 44.rdp)) }
            }
        }

        // Swipe horizontally on the vertically scrollable player -> host Row scrolls horizontally.
        rule.onNodeWithContentDescription("VerticalScrollPlayer").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        assertThat(hostHorizontalScrollState.value).isGreaterThan(0)
    }

    @Test
    fun combinedClickable_handlesClickDoubleAndLongClickWithHostActionAndValueChange() {
        val actionLog = mutableListOf<String>()
        rule.setRemoteContent(
            profile = experimentalProfile,
            onNamedAction = { name, value, _ -> actionLog.add("$name:$value") },
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(200.dp)) { content() }
            },
        ) {
            val state = remember { MutableRemoteInt(0) }
            val label = state.toRemoteString()
            RemoteColumn(modifier = RemoteModifier.size(200.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp)
                            .semantics { contentDescription = "CombinedBox".rs }
                            .combinedClickable(
                                onClick =
                                    combinedAction(
                                        valueChange(state, 10.ri),
                                        hostAction("single".rs, 10.ri),
                                    ),
                                onDoubleClick =
                                    combinedAction(
                                        valueChange(state, 20.ri),
                                        hostAction("double".rs, 20.ri),
                                    ),
                                onLongClick =
                                    combinedAction(
                                        valueChange(state, 30.ri),
                                        hostAction("long".rs, 30.ri),
                                    ),
                            )
                )
                RemoteText(label)
            }
        }

        val node = rule.onNodeWithContentDescription("CombinedBox")

        // Single click triggers both ValueChange (10) and HostAction ("single:10").
        node.performClick()
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        rule.onNodeWithText("10").assertExists()
        assertThat(actionLog).contains("single:10")

        // Double click triggers both ValueChange (20) and HostAction ("double:20").
        node.performTouchInput { doubleClick() }
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        rule.onNodeWithText("20").assertExists()
        assertThat(actionLog).contains("double:20")

        // Long click triggers both ValueChange (30) and HostAction ("long:30").
        node.performTouchInput { longClick() }
        rule.mainClock.advanceTimeBy(400L)
        rule.waitForIdle()
        rule.onNodeWithText("30").assertExists()
        assertThat(actionLog).contains("long:30")
    }

    @Composable
    @RemoteComposable
    private fun LeftBoxInteractiveContent(
        isClickable: Boolean = false,
        isScrollable: Boolean = false,
        isTouchUp: Boolean = false,
        hasTouchExpression: Boolean = false,
        scrollContentFits: Boolean = false,
    ) {
        val scrollState = remember { RemoteScrollState() }
        RemoteRow(modifier = RemoteModifier.size(300.rdp)) {
            var leftModifier =
                RemoteModifier.size(150.rdp, 300.rdp).semantics {
                    contentDescription = "LeftBox".rs
                }
            if (isTouchUp) {
                leftModifier =
                    leftModifier
                        .onTouchDown(hostAction("touchDown".rs, 0.ri))
                        .onTouchUp(hostAction("myActionName".rs, 1.ri))
            }
            if (isClickable) {
                leftModifier = leftModifier.clickable(hostAction("myActionName".rs, 1.ri))
            }
            if (isScrollable) {
                leftModifier = leftModifier.verticalScroll(scrollState)
            }
            RemoteBox(modifier = leftModifier) {
                if (isScrollable && !hasTouchExpression) {
                    // Content taller than the 300dp viewport, unless it should fit.
                    val contentHeight = if (scrollContentFits) 300.rdp else 900.rdp
                    RemoteBox(modifier = RemoteModifier.size(150.rdp, contentHeight))
                }
                if (hasTouchExpression) {
                    val touchYFloat = remember { MutableRemoteFloat(0f) }
                    RemoteCanvas(modifier = RemoteModifier.size(150.rdp, 300.rdp)) {
                        val doc = remoteComposeCreationState.document
                        val outputId =
                            Utils.idFromNan(
                                touchYFloat.getFloatIdForCreationState(remoteComposeCreationState)
                            )
                        doc.buffer.addTouchExpression(
                            outputId,
                            0f,
                            0f,
                            300f,
                            0f,
                            0,
                            floatArrayOf(
                                RemoteContext.FLOAT_TOUCH_POS_Y,
                                1f,
                                Rc.FloatExpression.MUL,
                            ),
                            TouchExpression.STOP_INSTANTLY,
                            null,
                            null,
                        )
                    }
                    RemoteText(touchYFloat.toRemoteInt().toRemoteString())
                }
            }
            RemoteBox(
                modifier =
                    RemoteModifier.size(150.rdp, 300.rdp).semantics {
                        contentDescription = "RightBox".rs
                    }
            )
        }
    }

    @Test
    fun touchExpression_horizontalTouchFraction_updatesMutableRemoteFloatAndDependentExpressions() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp)) { content() }
            }
        ) {
            // MutableRemoteFloat is declared before RemoteCanvas, first referenced inside
            // RemoteCanvas (which emits its initial FloatExpression(-1f) inside CanvasOperations
            // and draws with it), and then read by dependent conditional expressions outside.
            val selectedFraction = remember { MutableRemoteFloat(-1f) }
            RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                RemoteRow(modifier = RemoteModifier.size(300.rdp, 100.rdp)) {
                    // 50dp spacer on the left so ChartCanvas has rootX = 50
                    RemoteBox(modifier = RemoteModifier.size(50.rdp, 100.rdp))
                    RemoteCanvas(
                        modifier =
                            RemoteModifier.size(200.rdp, 100.rdp).semantics {
                                contentDescription = "ChartCanvas".rs
                            }
                    ) {
                        val doc = remoteComposeCreationState.document
                        val outputId =
                            Utils.idFromNan(
                                selectedFraction.getFloatIdForCreationState(
                                    remoteComposeCreationState
                                )
                            )
                        val rootX = doc.addComponentRootXValue()
                        val width = doc.addComponentWidthValue()
                        doc.buffer.addTouchExpression(
                            outputId,
                            -1f,
                            0f,
                            1f,
                            0f,
                            0,
                            floatArrayOf(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                rootX,
                                AnimatedFloatExpression.SUB,
                                width,
                                AnimatedFloatExpression.DIV,
                            ),
                            TouchExpression.STOP_ABSOLUTE_POS,
                            null,
                            null,
                        )
                        // Draw using selectedFraction so Canvas redraws on every touch update,
                        // verifying CanvasOperations does not reset selectedFraction back to -1f.
                        drawCircle(
                            paint = null,
                            radius = 4f.rf,
                            center = RemoteOffset(selectedFraction * width.rf, height / 2f),
                        )
                    }
                }
                val label =
                    selectedFraction
                        .isLessThan(0f.rf)
                        .select("None".rs, (selectedFraction * 100f).toRemoteInt().toRemoteString())
                RemoteText(label)
            }
        }

        val chartCanvas = rule.onNodeWithContentDescription("ChartCanvas")

        // 1. Before touch: default value is -1f (< 0f) -> "None"
        rule.onNodeWithText("None").assertExists()

        // 2. Single click at center of ChartCanvas (local x = 100, root x = 150, width = 200 ->
        // 0.5f -> "50")
        chartCanvas.performClick()
        rule.waitForIdle()
        rule.onNodeWithText("50").assertExists()

        // 3. Touch down and drag across multiple positions, including clamping at [0f, 1f] bounds
        chartCanvas.performTouchInput { down(Offset(width * 0.25f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("25").assertExists()

        chartCanvas.performTouchInput { moveTo(Offset(width * 0.75f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("75").assertExists()

        // Drag past right edge -> clamped to max (1f -> "100")
        chartCanvas.performTouchInput { moveTo(Offset(width * 1.5f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("100").assertExists()

        // Drag past left edge -> clamped to min (0f -> "0")
        chartCanvas.performTouchInput { moveTo(Offset(-50f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("0").assertExists()

        chartCanvas.performTouchInput { up() }
        rule.waitForIdle()
        rule.onNodeWithText("0").assertExists()

        // 4. Subsequent tap at 20% of width -> "20"
        chartCanvas.performTouchInput { click(Offset(width * 0.2f, height / 2f)) }
        rule.waitForIdle()
        rule.onNodeWithText("20").assertExists()
    }

    @Test
    fun touchExpression_verticalTouchPosition_withRootYOffsetAndDeltaMode_accumulatesAcrossDrags() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp)) { content() }
            }
        ) {
            val touchValue = remember { MutableRemoteFloat(10f) }
            RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                // 40dp top spacer so VerticalCanvas has rootY = 40
                RemoteBox(modifier = RemoteModifier.size(300.rdp, 40.rdp))
                RemoteCanvas(
                    modifier =
                        RemoteModifier.size(100.rdp, 200.rdp).semantics {
                            contentDescription = "VerticalCanvas".rs
                        }
                ) {
                    val doc = remoteComposeCreationState.document
                    val outputId =
                        Utils.idFromNan(
                            touchValue.getFloatIdForCreationState(remoteComposeCreationState)
                        )
                    val rootY = doc.addComponentRootYValue()
                    val height = doc.addComponentHeightValue()
                    // Delta mode (STOP_INSTANTLY): starts at defValue = 10f, adds delta of
                    // ((FLOAT_TOUCH_POS_Y - rootY) / height) * 100f on each drag, clamped to [0,
                    // 100]
                    doc.buffer.addTouchExpression(
                        outputId,
                        10f,
                        0f,
                        100f,
                        0f,
                        0,
                        floatArrayOf(
                            RemoteContext.FLOAT_TOUCH_POS_Y,
                            rootY,
                            AnimatedFloatExpression.SUB,
                            height,
                            AnimatedFloatExpression.DIV,
                            100f,
                            AnimatedFloatExpression.MUL,
                        ),
                        TouchExpression.STOP_INSTANTLY,
                        null,
                        null,
                    )
                }
                RemoteText(touchValue.toRemoteInt().toRemoteString())
            }
        }

        val verticalCanvas = rule.onNodeWithContentDescription("VerticalCanvas")

        // Before touch: default value of nested TouchExpression is initialized to 10
        rule.onNodeWithText("10").assertExists()

        // First vertical drag: from y = 40 (20% of 200dp) to y = 120 (60% of 200dp) -> delta +40 ->
        // 50
        verticalCanvas.performTouchInput {
            swipe(
                start = Offset(width / 2f, height * 0.2f),
                end = Offset(width / 2f, height * 0.6f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("50").assertExists()

        // Second vertical drag: from y = 160 (80% of 200dp) to y = 100 (50% of 200dp) -> delta -30
        // -> 20
        verticalCanvas.performTouchInput {
            swipe(
                start = Offset(width / 2f, height * 0.8f),
                end = Offset(width / 2f, height * 0.5f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("20").assertExists()
    }

    @Test
    fun touchExpression_inDrawWithContentModifier_updatesExpression() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp)) { content() }
            }
        ) {
            val touchFraction = remember { MutableRemoteFloat(-1f) }
            RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                RemoteRow(modifier = RemoteModifier.size(300.rdp, 100.rdp)) {
                    // 60dp left offset so DrawWithContentBox has rootX = 60
                    RemoteBox(modifier = RemoteModifier.size(60.rdp, 100.rdp))
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(200.rdp, 100.rdp)
                                .semantics { contentDescription = "DrawWithContentBox".rs }
                                .drawWithContent {
                                    val doc = remoteComposeCreationState.document
                                    val outputId =
                                        Utils.idFromNan(
                                            touchFraction.getFloatIdForCreationState(
                                                remoteComposeCreationState
                                            )
                                        )
                                    val rootX = doc.addComponentRootXValue()
                                    val width = doc.addComponentWidthValue()
                                    doc.buffer.addTouchExpression(
                                        outputId,
                                        -1f,
                                        0f,
                                        1f,
                                        0f,
                                        0,
                                        floatArrayOf(
                                            RemoteContext.FLOAT_TOUCH_POS_X,
                                            rootX,
                                            AnimatedFloatExpression.SUB,
                                            width,
                                            AnimatedFloatExpression.DIV,
                                        ),
                                        TouchExpression.STOP_ABSOLUTE_POS,
                                        null,
                                        null,
                                    )
                                    drawContent()
                                }
                    )
                }
                val text =
                    touchFraction
                        .isLessThan(0f.rf)
                        .select("Unset".rs, (touchFraction * 100f).toRemoteInt().toRemoteString())
                RemoteText(text)
            }
        }

        val box = rule.onNodeWithContentDescription("DrawWithContentBox")

        // Before touch: -1f -> "Unset"
        rule.onNodeWithText("Unset").assertExists()

        // Click at center (50%) -> "50"
        box.performClick()
        rule.waitForIdle()
        rule.onNodeWithText("50").assertExists()

        // Drag from 10% to 80% -> "80"
        box.performTouchInput {
            swipe(
                start = Offset(width * 0.1f, height / 2f),
                end = Offset(width * 0.8f, height / 2f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("80").assertExists()
    }

    @Test
    fun touchExpression_directlyOnComponent_notInCanvas_updatesExpression() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp)) { content() }
            }
        ) {
            val creationState = LocalRemoteComposeCreationState.current
            val touchFraction = rememberRemoteFloatExpression {
                val doc = creationState.document
                val rootX = doc.addComponentRootXValue()
                val width = doc.addComponentWidthValue()
                val touchId =
                    doc.addTouch(
                        -1f,
                        0f,
                        1f,
                        TouchExpression.STOP_ABSOLUTE_POS,
                        0f,
                        0,
                        null,
                        null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                        rootX,
                        AnimatedFloatExpression.SUB,
                        width,
                        AnimatedFloatExpression.DIV,
                    )
                RemoteFloat(touchId)
            }
            RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                RemoteRow(modifier = RemoteModifier.size(300.rdp, 100.rdp)) {
                    // 50dp left offset so DirectTouchBox has rootX = 50
                    RemoteBox(modifier = RemoteModifier.size(50.rdp, 100.rdp))
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(200.rdp, 100.rdp).semantics {
                                contentDescription = "DirectTouchBox".rs
                            }
                    ) {
                        Hoist(touchFraction)
                    }
                }
                val text =
                    touchFraction
                        .isLessThan(0f.rf)
                        .select("Unset".rs, (touchFraction * 100f).toRemoteInt().toRemoteString())
                RemoteText(text)
            }
        }

        val box = rule.onNodeWithContentDescription("DirectTouchBox")
        rule.onNodeWithText("Unset").assertExists()

        box.performClick()
        rule.waitForIdle()
        rule.onNodeWithText("50").assertExists()

        box.performTouchInput {
            swipe(
                start = Offset(width * 0.25f, height / 2f),
                end = Offset(width * 0.75f, height / 2f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("75").assertExists()
    }

    @Test
    fun touchExpression_onPaddedComponent_usesComponentOuterCoordinates() {
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(300.dp)) { content() }
            }
        ) {
            val creationState = LocalRemoteComposeCreationState.current
            val touchFraction = rememberRemoteFloatExpression {
                val doc = creationState.document
                val rootX = doc.addComponentRootXValue()
                val width = doc.addComponentWidthValue()
                val touchId =
                    doc.addTouch(
                        -1f,
                        0f,
                        1f,
                        TouchExpression.STOP_ABSOLUTE_POS,
                        0f,
                        0,
                        null,
                        null,
                        RemoteContext.FLOAT_TOUCH_POS_X,
                        rootX,
                        AnimatedFloatExpression.SUB,
                        width,
                        AnimatedFloatExpression.DIV,
                    )
                RemoteFloat(touchId)
            }
            RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                RemoteRow(modifier = RemoteModifier.size(300.rdp, 100.rdp)) {
                    RemoteBox(modifier = RemoteModifier.size(50.rdp, 100.rdp))
                    // The padding moves the TouchExpression's pointerInput inside the box, but
                    // ComponentRootX / ComponentWidth (and core's touch positions) describe the
                    // box's outer bounds.
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(200.rdp, 100.rdp)
                                .semantics { contentDescription = "PaddedTouchBox".rs }
                                .padding(start = 40.rdp)
                    ) {
                        Hoist(touchFraction)
                    }
                }
                val text =
                    touchFraction
                        .isLessThan(0f.rf)
                        .select("Unset".rs, (touchFraction * 100f).toRemoteInt().toRemoteString())
                RemoteText(text)
            }
        }

        rule.onNodeWithText("Unset").assertExists()

        // Center of the outer box: (100 - 0) / 200 of its width. Without mapping into the outer
        // box this read the padding-relative position, (100 - 40) / 200.
        rule.onNodeWithContentDescription("PaddedTouchBox").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("50").assertExists()
    }

    /** A profile whose documents opt into the legacy (root coordinates) touch version 0. */
    private fun legacyTouchProfile(): Profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, _ ->
            RemoteComposeWriterAndroid(
                profile,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profile.operationsProfiles),
                RemoteComposeWriter.hTag(
                    Header.DOC_DENSITY_BEHAVIOR,
                    creationDisplayInfo.densityBehavior,
                ),
                RemoteComposeWriter.hTag(Header.FEATURE_TOUCH_VERSION, 0),
            )
        }

    @Test
    fun touchExpression_legacyTouchVersion_usesRootCoordinatesAndAncestorPositions() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val capturedDocument =
                captureSingleRemoteDocument(context = context, profile = legacyTouchProfile()) {
                    val touchFraction = remember { MutableRemoteFloat(-1f) }
                    RemoteColumn(modifier = RemoteModifier.size(300.rdp)) {
                        // 40dp top offset on the outer column
                        RemoteBox(modifier = RemoteModifier.size(300.rdp, 40.rdp))
                        RemoteRow(modifier = RemoteModifier.size(300.rdp, 100.rdp)) {
                            // 60dp left offset on the inner row so LegacyCanvas is at root (60, 40)
                            RemoteBox(modifier = RemoteModifier.size(60.rdp, 100.rdp))
                            RemoteCanvas(
                                modifier =
                                    RemoteModifier.size(200.rdp, 100.rdp).semantics {
                                        contentDescription = "LegacyCanvas".rs
                                    }
                            ) {
                                val doc = remoteComposeCreationState.document
                                val outputId =
                                    Utils.idFromNan(
                                        touchFraction.getFloatIdForCreationState(
                                            remoteComposeCreationState
                                        )
                                    )
                                val rootX = doc.addComponentRootXValue()
                                val width = doc.addComponentWidthValue()
                                doc.buffer.addTouchExpression(
                                    outputId,
                                    -1f,
                                    0f,
                                    1f,
                                    0f,
                                    0,
                                    floatArrayOf(
                                        RemoteContext.FLOAT_TOUCH_POS_X,
                                        rootX,
                                        AnimatedFloatExpression.SUB,
                                        width,
                                        AnimatedFloatExpression.DIV,
                                    ),
                                    TouchExpression.STOP_ABSOLUTE_POS,
                                    null,
                                    null,
                                )
                            }
                        }
                        val text =
                            touchFraction
                                .isLessThan(0f.rf)
                                .select(
                                    "Unset".rs,
                                    (touchFraction * 100f).toRemoteInt().toRemoteString(),
                                )
                        RemoteText(text)
                    }
                }

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(capturedDocument.bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
            assertThat(document.featureIntValue(Header.FEATURE_TOUCH_VERSION)).isEqualTo(0)

            rule.setContent {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) }
            }
            rule.waitForIdle()

            val canvas = rule.onNodeWithContentDescription("LegacyCanvas")
            rule.onNodeWithText("Unset").assertExists()

            // Click at center of LegacyCanvas (local x=100 -> root x=160 -> 50%)
            canvas.performClick()
            rule.waitForIdle()
            rule.onNodeWithText("50").assertExists()

            // Drag from 20% to 80% inside LegacyCanvas
            canvas.performTouchInput {
                swipe(
                    start = Offset(width * 0.2f, height / 2f),
                    end = Offset(width * 0.8f, height / 2f),
                    durationMillis = 100,
                )
            }
            rule.waitForIdle()
            rule.onNodeWithText("80").assertExists()
        }
    }

    @Test
    fun touchExpression_legacyTouchVersion_hitTestFollowsMovedComponent() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val capturedDocument =
                captureSingleRemoteDocument(context = context, profile = legacyTouchProfile()) {
                    val touchFraction = remember { MutableRemoteFloat(-1f) }
                    RemoteColumn(modifier = RemoteModifier.fillMaxWidth().height(300.rdp)) {
                        val text =
                            touchFraction
                                .isLessThan(0f.rf)
                                .select(
                                    "Unset".rs,
                                    (touchFraction * 100f).toRemoteInt().toRemoteString(),
                                )
                        RemoteText(text)
                        // The weighted spacer pushes LegacyCanvas to the right edge, so it moves
                        // (without resizing) when the host width changes.
                        RemoteRow(modifier = RemoteModifier.fillMaxWidth().height(100.rdp)) {
                            RemoteBox(modifier = RemoteModifier.weight(1f).height(100.rdp))
                            RemoteCanvas(
                                modifier =
                                    RemoteModifier.size(100.rdp, 100.rdp).semantics {
                                        contentDescription = "LegacyCanvas".rs
                                    }
                            ) {
                                val doc = remoteComposeCreationState.document
                                val outputId =
                                    Utils.idFromNan(
                                        touchFraction.getFloatIdForCreationState(
                                            remoteComposeCreationState
                                        )
                                    )
                                val rootX = doc.addComponentRootXValue()
                                val width = doc.addComponentWidthValue()
                                doc.buffer.addTouchExpression(
                                    outputId,
                                    -1f,
                                    0f,
                                    1f,
                                    0f,
                                    0,
                                    floatArrayOf(
                                        RemoteContext.FLOAT_TOUCH_POS_X,
                                        rootX,
                                        AnimatedFloatExpression.SUB,
                                        width,
                                        AnimatedFloatExpression.DIV,
                                    ),
                                    TouchExpression.STOP_ABSOLUTE_POS,
                                    null,
                                    null,
                                )
                            }
                        }
                    }
                }

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(capturedDocument.bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            val hostWidth = mutableStateOf(300.dp)
            rule.setContent {
                Box(modifier = Modifier.size(hostWidth.value, 300.dp)) {
                    RcPlayer(document = document)
                }
            }
            rule.waitForIdle()
            rule.onNodeWithText("Unset").assertExists()

            // LegacyCanvas moves from root x=200 to x=100 without changing size.
            hostWidth.value = 200.dp
            rule.waitForIdle()

            // Tap the center of the moved LegacyCanvas (root x=150). Bounds from before the move
            // (x=200..300) would reject the touch and leave the value unset.
            rule.onNodeWithContentDescription("LegacyCanvas").performClick()
            rule.waitForIdle()
            rule.onNodeWithText("50").assertExists()
        }
    }

    @Test
    fun touchUpAndClickable_onSameComponent_firesBothOnTapAndOnlyTouchUpAfterUnconsumedDrag() {
        val actionLog = mutableListOf<String>()
        rule.setRemoteContent(
            onNamedAction = { name, value, _ -> actionLog.add("$name:$value") },
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(200.dp)) { content() }
            },
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(150.rdp)
                        .clickable(hostAction("click".rs, 1.ri))
                        .onTouchDown(hostAction("down".rs, 1.ri))
                        .onTouchUp(hostAction("up".rs, 1.ri))
                        .onTouchCancel(hostAction("cancel".rs, 1.ri))
                        .semantics { contentDescription = "TouchAndClickBox".rs }
            )
        }

        val box = rule.onNodeWithContentDescription("TouchAndClickBox")

        // 1. A tap on a component with both clickable and onTouchUp must fire down, up, AND click
        // (not cancel).
        box.performClick()
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1", "up:1", "click:1")

        // 2. An unconsumed drag that moves past the component bounds still fires down and up on
        // release (matching RemoteComposeView), without firing click or cancel.
        actionLog.clear()
        box.performTouchInput {
            swipe(
                start = Offset(width * 0.2f, height / 2f),
                end = Offset(width * 1.2f, height / 2f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1", "up:1").inOrder()
    }

    @Test
    fun touchActions_componentRemovedMidGesture_firesTouchCancel() {
        val actionLog = mutableListOf<String>()
        val showContent = mutableStateOf(true)
        rule.setRemoteContent(
            onNamedAction = { name, value, _ -> actionLog.add("$name:$value") },
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(200.dp).testTag("host")) {
                    if (showContent.value) {
                        content()
                    }
                }
            },
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(150.rdp)
                        .onTouchDown(hostAction("down".rs, 1.ri))
                        .onTouchUp(hostAction("up".rs, 1.ri))
                        .onTouchCancel(hostAction("cancel".rs, 1.ri))
            )
        }

        rule.onNodeWithTag("host").performTouchInput { down(Offset(20f, 20f)) }
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1")

        // Disposing the handler mid-gesture must still end the gesture.
        showContent.value = false
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1", "cancel:1").inOrder()

        rule.onNodeWithTag("host").performTouchInput { up() }
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1", "cancel:1").inOrder()
    }

    @Test
    fun touchExpression_andRootPointerEvents_multiTouch_ignoresSecondFingerAndHover() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val capturedDocument =
                captureSingleRemoteDocument(context = context) {
                    val touchFraction = remember { MutableRemoteFloat(-1f) }
                    RemoteColumn(modifier = RemoteModifier.size(200.rdp)) {
                        RemoteCanvas(
                            modifier =
                                RemoteModifier.size(200.rdp, 100.rdp).semantics {
                                    contentDescription = "MultiTouchCanvas".rs
                                }
                        ) {
                            val doc = remoteComposeCreationState.document
                            val outputId =
                                Utils.idFromNan(
                                    touchFraction.getFloatIdForCreationState(
                                        remoteComposeCreationState
                                    )
                                )
                            val width = doc.addComponentWidthValue()
                            doc.buffer.addTouchExpression(
                                outputId,
                                -1f,
                                0f,
                                1f,
                                0f,
                                0,
                                floatArrayOf(
                                    RemoteContext.FLOAT_TOUCH_POS_X,
                                    width,
                                    AnimatedFloatExpression.DIV,
                                ),
                                TouchExpression.STOP_ABSOLUTE_POS,
                                null,
                                null,
                            )
                        }
                        val text =
                            touchFraction
                                .isLessThan(0f.rf)
                                .select(
                                    "Unset".rs,
                                    (touchFraction * 100f).toRemoteInt().toRemoteString(),
                                )
                        RemoteText(text)
                    }
                }

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(capturedDocument.bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            rule.setContent {
                Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) }
            }
            rule.waitForIdle()

            val canvas = rule.onNodeWithContentDescription("MultiTouchCanvas")

            // 1. Mouse hover move with nothing pressed should not drive the TouchExpression.
            canvas.performMouseInput {
                enter(Offset(20f, 20f))
                moveTo(Offset(80f, 20f))
                exit(Offset(80f, 20f))
            }
            rule.waitForIdle()
            rule.onNodeWithText("Unset").assertExists()

            // 2. Multi-touch: first finger down at 20% (x=40), second finger down at 90% (x=180)
            // and up, then first finger moves to 60% (x=120) and releases.
            // The second finger must not hijack the tracked pointer.
            canvas.performTouchInput {
                down(pointerId = 0, position = Offset(width * 0.2f, height / 2f))
                advanceEventTime(16L)
                down(pointerId = 1, position = Offset(width * 0.9f, height / 2f))
                advanceEventTime(16L)
                moveTo(pointerId = 1, position = Offset(width * 0.95f, height / 2f))
                up(pointerId = 1)
                advanceEventTime(16L)
                moveTo(pointerId = 0, position = Offset(width * 0.6f, height / 2f))
                advanceEventTime(16L)
                up(pointerId = 0)
            }
            rule.waitForIdle()

            rule.onNodeWithText("60").assertExists()
        }
    }

    @Test
    fun touchExpression_dispatchesExactlyOneDownAndOneUpPerGesture_forComponentCanvasAndDrawContent() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val capturedDocument =
                captureSingleRemoteDocument(context = context) {
                    val creationState = LocalRemoteComposeCreationState.current
                    val directVal = rememberRemoteFloatExpression {
                        val doc = creationState.document
                        val touchId =
                            doc.addTouch(
                                0f,
                                0f,
                                200f,
                                TouchExpression.STOP_ABSOLUTE_POS,
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                            )
                        RemoteFloat(touchId)
                    }
                    val canvasVal = remember { MutableRemoteFloat(0f) }
                    val drawContentVal = remember { MutableRemoteFloat(0f) }
                    RemoteColumn(modifier = RemoteModifier.size(200.rdp, 300.rdp)) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(200.rdp, 80.rdp).semantics {
                                    contentDescription = "DirectTarget".rs
                                }
                        ) {
                            Hoist(directVal)
                        }
                        RemoteCanvas(
                            modifier =
                                RemoteModifier.size(200.rdp, 80.rdp).semantics {
                                    contentDescription = "CanvasTarget".rs
                                }
                        ) {
                            val doc = remoteComposeCreationState.document
                            val id =
                                Utils.idFromNan(
                                    canvasVal.getFloatIdForCreationState(remoteComposeCreationState)
                                )
                            doc.buffer.addTouchExpression(
                                id,
                                0f,
                                0f,
                                200f,
                                0f,
                                0,
                                floatArrayOf(RemoteContext.FLOAT_TOUCH_POS_X),
                                TouchExpression.STOP_ABSOLUTE_POS,
                                null,
                                null,
                            )
                        }
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(200.rdp, 80.rdp)
                                    .semantics { contentDescription = "DrawContentTarget".rs }
                                    .drawWithContent {
                                        val doc = remoteComposeCreationState.document
                                        val id =
                                            Utils.idFromNan(
                                                drawContentVal.getFloatIdForCreationState(
                                                    remoteComposeCreationState
                                                )
                                            )
                                        doc.buffer.addTouchExpression(
                                            id,
                                            0f,
                                            0f,
                                            200f,
                                            0f,
                                            0,
                                            floatArrayOf(RemoteContext.FLOAT_TOUCH_POS_X),
                                            TouchExpression.STOP_ABSOLUTE_POS,
                                            null,
                                            null,
                                        )
                                        drawContent()
                                    }
                        )
                    }
                }

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(capturedDocument.bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }

            val state = RcPlayerState(document)
            // Verify that component-scoped TouchExpressions are not registered as global document
            // mTouchListeners.
            val touchListenersField =
                CoreDocument::class.java.getDeclaredField("mTouchListeners").apply {
                    isAccessible = true
                }
            @Suppress("UNCHECKED_CAST")
            val globalTouchListeners = touchListenersField.get(document) as Set<TouchListener>
            assertThat(globalTouchListeners.filterIsInstance<TouchExpression>()).isEmpty()

            val downCounts = mutableMapOf<Int, Int>()
            val upCounts = mutableMapOf<Int, Int>()
            state.preprocessed.touchExpressions.forEach { te ->
                downCounts[te.id] = 0
                upCounts[te.id] = 0
            }

            // Replace each TouchExpression in preprocessed.componentTouchExpressionsMap with a
            // delegating TouchExpression that counts touchDown and touchUp calls.
            @Suppress("UNCHECKED_CAST")
            val map =
                state.preprocessed.componentTouchExpressionsMap
                    as Map<Int, MutableList<TouchExpression>>
            map.values.forEach { list ->
                for (i in list.indices) {
                    val delegate = list[i]
                    val spy =
                        object :
                            TouchExpression(
                                delegate.id,
                                delegate.mSrcExp ?: FloatArray(0),
                                0f,
                                0f,
                                200f,
                                0,
                                0f,
                                TouchExpression.STOP_ABSOLUTE_POS,
                                floatArrayOf(),
                                null,
                            ) {
                            override fun updateVariables(context: RemoteContext) {
                                delegate.updateVariables(context)
                            }

                            override fun apply(context: RemoteContext) {
                                delegate.apply(context)
                            }

                            override fun touchDown(context: RemoteContext, x: Float, y: Float) {
                                downCounts[delegate.id] = (downCounts[delegate.id] ?: 0) + 1
                                delegate.touchDown(context, x, y)
                            }

                            override fun touchDrag(context: RemoteContext, x: Float, y: Float) {
                                delegate.touchDrag(context, x, y)
                            }

                            override fun touchUp(
                                context: RemoteContext,
                                x: Float,
                                y: Float,
                                dx: Float,
                                dy: Float,
                            ) {
                                upCounts[delegate.id] = (upCounts[delegate.id] ?: 0) + 1
                                delegate.touchUp(context, x, y, dx, dy)
                            }
                        }
                    list[i] = spy
                }
            }

            rule.setContent {
                Box(modifier = Modifier.size(200.dp, 300.dp)) { RcPlayer(state = state) }
            }
            rule.waitForIdle()

            assertThat(state.preprocessed.touchExpressions).hasSize(3)
            val ids = state.preprocessed.touchExpressions.map { it.id }

            rule.onNodeWithContentDescription("DirectTarget").performClick()
            rule.waitForIdle()
            assertThat(downCounts[ids[0]]).isEqualTo(1)
            assertThat(upCounts[ids[0]]).isEqualTo(1)
            assertThat(downCounts[ids[1]]).isEqualTo(0)
            assertThat(downCounts[ids[2]]).isEqualTo(0)

            rule.onNodeWithContentDescription("CanvasTarget").performTouchInput {
                swipe(
                    start = Offset(width * 0.2f, height / 2f),
                    end = Offset(width * 0.8f, height / 2f),
                    durationMillis = 100,
                )
            }
            rule.waitForIdle()
            assertThat(downCounts[ids[1]]).isEqualTo(1)
            assertThat(upCounts[ids[1]]).isEqualTo(1)

            rule.onNodeWithContentDescription("DrawContentTarget").performClick()
            rule.waitForIdle()
            assertThat(downCounts[ids[2]]).isEqualTo(1)
            assertThat(upCounts[ids[2]]).isEqualTo(1)
        }
    }

    @Test
    fun touchGesture_playerRemovedMidGesture_endsComponentGesture() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val capturedDocument =
                captureSingleRemoteDocument(context = context) {
                    val creationState = LocalRemoteComposeCreationState.current
                    val directVal = rememberRemoteFloatExpression {
                        val touchId =
                            creationState.document.addTouch(
                                0f,
                                0f,
                                200f,
                                TouchExpression.STOP_ABSOLUTE_POS,
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_X,
                            )
                        RemoteFloat(touchId)
                    }
                    RemoteBox(modifier = RemoteModifier.size(200.rdp, 80.rdp)) { Hoist(directVal) }
                }

            val document =
                CoreDocument(RemoteClock.SYSTEM).apply {
                    ByteArrayInputStream(capturedDocument.bytes).use {
                        initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                    }
                }
            val state = RcPlayerState(document)
            val componentTouch =
                state.preprocessed.componentTouchExpressionsMap.values.single().single()
            val touchDownField =
                TouchExpression::class.java.getDeclaredField("mTouchDown").apply {
                    isAccessible = true
                }

            val showPlayer = mutableStateOf(true)
            rule.setContent {
                Box(modifier = Modifier.size(200.dp, 300.dp).testTag("host")) {
                    if (showPlayer.value) {
                        RcPlayer(state = state)
                    }
                }
            }
            rule.waitForIdle()

            rule.onNodeWithTag("host").performTouchInput { down(Offset(100f, 40f)) }
            rule.waitForIdle()
            assertThat(touchDownField.getBoolean(componentTouch)).isTrue()

            // Disposing the player mid-gesture must end the component gesture.
            showPlayer.value = false
            rule.waitForIdle()
            assertThat(touchDownField.getBoolean(componentTouch)).isFalse()

            rule.onNodeWithTag("host").performTouchInput { up() }
            rule.waitForIdle()
            assertThat(touchDownField.getBoolean(componentTouch)).isFalse()
        }
    }

    @Test
    fun touchExpression_horizontalSliderInsideVerticalHostScroll_allowsVerticalHostScroll() {
        lateinit var hostScrollState: ScrollState
        rule.setRemoteContent(
            playComposableWrapper = { content ->
                hostScrollState = rememberScrollState()
                Column(
                    modifier =
                        Modifier.size(200.dp, 150.dp)
                            .testTag("HostVerticalScroll")
                            .hostVerticalScroll(hostScrollState)
                ) {
                    Box(modifier = Modifier.size(200.dp, 100.dp)) { content() }
                    Box(modifier = Modifier.size(200.dp, 200.dp))
                }
            }
        ) {
            val sliderFraction = remember { MutableRemoteFloat(0f) }
            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 100.rdp)) {
                RemoteCanvas(
                    modifier =
                        RemoteModifier.size(200.rdp, 80.rdp).semantics {
                            contentDescription = "HorizontalSlider".rs
                        }
                ) {
                    val doc = remoteComposeCreationState.document
                    val outputId =
                        Utils.idFromNan(
                            sliderFraction.getFloatIdForCreationState(remoteComposeCreationState)
                        )
                    val width = doc.addComponentWidthValue()
                    doc.buffer.addTouchExpression(
                        outputId,
                        0f,
                        0f,
                        1f,
                        0f,
                        0,
                        floatArrayOf(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            width,
                            AnimatedFloatExpression.DIV,
                        ),
                        TouchExpression.STOP_ABSOLUTE_POS,
                        null,
                        null,
                    )
                }
                RemoteText((sliderFraction * 100f).toRemoteInt().toRemoteString())
            }
        }

        val slider = rule.onNodeWithContentDescription("HorizontalSlider")
        assertThat(hostScrollState.value).isEqualTo(0)

        // 1. Horizontal swipe on the slider updates the slider expression without scrolling the
        // vertical host Column.
        slider.performTouchInput {
            swipe(
                start = Offset(width * 0.2f, height / 2f),
                end = Offset(width * 0.8f, height / 2f),
                durationMillis = 100,
            )
        }
        rule.waitForIdle()
        assertThat(hostScrollState.value).isEqualTo(0)
        rule.onNodeWithText("80").assertExists()

        // 2. Vertical swipe starting directly on the horizontal TouchExpression slider is not
        // consumed on the vertical axis, allowing the outer vertical host Column to scroll.
        slider.performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertThat(hostScrollState.value).isGreaterThan(0)
    }

    @Test
    fun snapshotRemoteComposeState_getFloatBeforeGetInteger_preservesIntegerAndLargePrecision() {
        val state = SnapshotRemoteComposeState()
        val intId = 100
        val largeIntId = 101
        val floatId = 102
        val largeIntValue = 16_777_217 // 2^24 + 1, cannot be represented exactly as IEEE-754 Float

        // Reading an unwritten ID through getFloat before updating/reading it as an integer must
        // not cache 0f into the snapshot float map in a way that shadows subsequent integer reads.
        assertThat(state.getFloat(intId)).isEqualTo(0f)
        state.updateInteger(intId, 42)
        assertThat(state.getInteger(intId)).isEqualTo(42)

        // Large integers above 2^24 must preserve exact 32-bit integer precision even when read
        // through getFloat first (which loses precision in IEEE-754 Float).
        state.updateInteger(largeIntId, largeIntValue)
        assertThat(state.getFloat(largeIntId)).isEqualTo(largeIntValue.toFloat())
        assertThat(state.getInteger(largeIntId)).isEqualTo(largeIntValue)

        // Reading a float ID through getInteger falls back to converting the known float entry.
        state.updateFloat(floatId, 99.5f)
        assertThat(state.getInteger(floatId)).isEqualTo(99)
    }

    @Test
    fun touchActionsBeforeClickable_onSameComponent_firesDownUpAndClickOnTap() {
        val actionLog = mutableListOf<String>()
        rule.setRemoteContent(
            onNamedAction = { name, value, _ -> actionLog.add("$name:$value") },
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(200.dp)) { content() }
            },
        ) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(150.rdp)
                        .onTouchDown(hostAction("down".rs, 1.ri))
                        .onTouchUp(hostAction("up".rs, 1.ri))
                        .onTouchCancel(hostAction("cancel".rs, 1.ri))
                        .clickable(hostAction("click".rs, 1.ri))
                        .semantics { contentDescription = "TouchThenClickBox".rs }
            )
        }

        // touchActions is outer here, so the click consumes the release first. That must still
        // count as TouchUp, not TouchCancel.
        rule.onNodeWithContentDescription("TouchThenClickBox").performClick()
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1", "up:1", "click:1")
    }

    @Test
    fun touchActions_recompositionMidGesture_doesNotEndGesture() {
        val actionLog = mutableListOf<String>()
        rule.setRemoteContent(
            onNamedAction = { name, value, _ -> actionLog.add("$name:$value") },
            profile = experimentalProfile,
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(200.dp).testTag("host")) { content() }
            },
        ) {
            val boxWidth = remember { MutableRemoteFloat(150f) }
            RemoteBox(
                modifier =
                    RemoteModifier.width(boxWidth)
                        .height(150.rdp)
                        // Touch down resizes the box, which recomposes its modifiers mid-gesture.
                        .onTouchDown(hostAction("down".rs, 1.ri))
                        .onTouchDown(valueChange(boxWidth, 160f.rf))
                        .onTouchUp(hostAction("up".rs, 1.ri))
                        .onTouchCancel(hostAction("cancel".rs, 1.ri))
            )
        }

        rule.onNodeWithTag("host").performTouchInput { down(Offset(20f, 20f)) }
        rule.waitForIdle()
        rule.onNodeWithTag("host").performTouchInput { up() }
        rule.waitForIdle()
        assertThat(actionLog).containsExactly("down:1", "up:1").inOrder()
    }
}
