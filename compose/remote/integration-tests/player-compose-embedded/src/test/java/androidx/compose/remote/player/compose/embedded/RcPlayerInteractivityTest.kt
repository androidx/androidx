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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.lambdaAction
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.CUBIC_LINEAR
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.acos
import androidx.compose.remote.creation.compose.state.animateRemoteFloat
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
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerInteractivityTest {

    @get:Rule val rule = createComposeRule()

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
                            val visibilityState =
                                rememberMutableRemoteInt(Component.Visibility.VISIBLE)

                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
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
                            val visibilityState =
                                rememberMutableRemoteInt(Component.Visibility.VISIBLE)
                            // One operand is a (non-constant) variable, so this authors an
                            // IntegerExpression rather than folding to a constant.
                            val visibilityExpr = visibilityState * 1.ri

                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
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
                            val n = rememberMutableRemoteInt(0)
                            val label = n.toRemoteString()
                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color(0xFF3F51B5).rc)
                                            .clickable(action = valueChange(n, 7.ri))
                                )
                                androidx.compose.remote.creation.compose.layout.RemoteText(label)
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
                            val n = rememberMutableRemoteInt(0)
                            val doubled = n * 2.ri // intermediate IntegerExpression, not displayed
                            val label = doubled.toRemoteString()
                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp, 40.rdp)
                                            .background(Color(0xFF3F51B5).rc)
                                            .clickable(action = valueChange(n, 3.ri))
                                )
                                androidx.compose.remote.creation.compose.layout.RemoteText(label)
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
                            val target = rememberMutableRemoteFloat(123.45f)
                            // Animate from 0 to `target` over 1s, linearly.
                            val animatedSize =
                                animateRemoteFloat(
                                    target,
                                    duration = 1f,
                                    type = CUBIC_LINEAR,
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
                            // width = height = TIME_IN_SEC * 100 (px). The embedded player bridges
                            // TIME_IN_SEC to the frame loop's elapsed millis / 1000, so this grows
                            // monotonically from 0 as the clock advances.
                            val size = RemoteFloat(RemoteContext.FLOAT_TIME_IN_SEC) * 100f
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
                            val v = rememberMutableRemoteFloat(60f)
                            val half = rememberMutableRemoteFloat(0.5f)
                            val one = rememberMutableRemoteFloat(1f)
                            val thousand = rememberMutableRemoteFloat(1000f)
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

                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(400.rdp)
                            ) {
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
                assert(kotlin.math.abs(w - control) < 1f) {
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
                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(400.rdp)
                            ) {
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
            assert(kotlin.math.abs(maxW - 50f) < 3f) {
                "max-constrained width $maxW, expected ~50dp"
            }
            assert(kotlin.math.abs(minW - 80f) < 3f) {
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
                            androidx.compose.remote.creation.compose.layout.RemoteFlowRow(
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
            assert(kotlin.math.abs(f0.top.value - f1.top.value) < 1f) {
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
                            androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
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
                            androidx.compose.remote.creation.compose.layout.RemoteFitBox(
                                modifier = RemoteModifier.size(100.rdp)
                            ) {
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
                            val scrollState = rememberRemoteScrollState()
                            androidx.compose.remote.creation.compose.layout.RemoteColumn(
                                modifier = RemoteModifier.size(200.rdp)
                            ) {
                                // A 100dp viewport scrolling a 400dp-tall column.
                                RemoteBox(
                                    modifier =
                                        RemoteModifier.size(100.rdp)
                                            .verticalScroll(scrollState)
                                            .semantics { contentDescription = "scroller".rs }
                                ) {
                                    androidx.compose.remote.creation.compose.layout.RemoteColumn {
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
}
