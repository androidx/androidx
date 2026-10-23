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
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.DataListFloat
import androidx.compose.remote.core.operations.DataListIds
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.IntegerExpression
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.TextLookupInt
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.ComponentVisibilityOperation
import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.HeightModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.WidthModifierOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.easing.Easing
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerExpressionTest {

    @get:Rule val rule = RcPlayerTestRule()

    /**
     * Evaluates one or more RPN [FloatExpression]s through [RcPlayer]'s compiled animated
     * expression pipeline (`mSrcAnimation = floatArrayOf(0f)` triggers
     * `rememberAnimatedRemoteFloat` -> `rememberRemoteExpression` -> `parseRpn`) and returns the
     * evaluated values from [RcPlayerState].
     */
    private fun evalRpnBatch(
        vararg expressions: FloatArray,
        setupOps: List<Operation> = emptyList(),
    ): List<Float> {
        val doc = CoreDocument(RemoteClock.SYSTEM)
        doc.operations.addAll(setupOps)
        expressions.forEachIndexed { index, exp ->
            val id = 100 + index
            doc.operations.add(FloatExpression(id, exp.copyOf(), floatArrayOf(0f)))
            doc.operations.add(NamedVariable(id, NamedVariable.FLOAT_TYPE, "USER:e$index"))
        }
        val playerState = RcPlayerState(doc)
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = playerState) }
        }
        rule.waitForIdle()
        return expressions.indices.map { index -> playerState.floatState("e$index").value }
    }

    private fun evalSingleRpn(vararg exp: Float): Float = evalRpnBatch(exp).single()

    @Test
    fun testParseSimpleAdd() {
        assertThat(evalSingleRpn(1f, 2f, AnimatedFloatExpression.ADD)).isEqualTo(3f)
    }

    @Test
    fun testParseSimpleSub() {
        assertThat(evalSingleRpn(5f, 2f, AnimatedFloatExpression.SUB)).isEqualTo(3f)
    }

    @Test
    fun testParseSimpleMul() {
        assertThat(evalSingleRpn(3f, 4f, AnimatedFloatExpression.MUL)).isEqualTo(12f)
    }

    @Test
    fun testParseSimpleDiv() {
        assertThat(evalSingleRpn(12f, 3f, AnimatedFloatExpression.DIV)).isEqualTo(4f)
    }

    @Test
    fun testParseMad() {
        assertThat(evalSingleRpn(2f, 3f, 4f, AnimatedFloatExpression.MAD))
            .isEqualTo(10f) // 2 * 3 + 4
    }

    @Test
    fun testParseLerp() {
        assertThat(evalSingleRpn(10f, 20f, 0.5f, AnimatedFloatExpression.LERP)).isEqualTo(15f)
    }

    @Test
    fun testParseWithVariable() {
        val varId = 42
        val exprId = 43
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(FloatConstant(varId, 10f))
                operations.add(NamedVariable(varId, NamedVariable.FLOAT_TYPE, "USER:input"))
                operations.add(
                    FloatExpression(
                        exprId,
                        floatArrayOf(Utils.asNan(varId), 5f, AnimatedFloatExpression.ADD),
                        floatArrayOf(0f),
                    )
                )
                operations.add(NamedVariable(exprId, NamedVariable.FLOAT_TYPE, "USER:output"))
            }
        val playerState = RcPlayerState(doc)
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(state = playerState) }
        }
        rule.waitForIdle()
        assertThat(playerState.floatState("output").value).isEqualTo(15f)

        playerState.floatState("input").value = 20f
        rule.waitForIdle()
        assertThat(playerState.floatState("output").value).isEqualTo(25f)
    }

    @Test
    fun testInspectableValue() {
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(
                    FloatExpression(
                        100,
                        floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD),
                        null,
                    )
                )
                operations.add(NamedVariable(100, NamedVariable.FLOAT_TYPE, "USER:sum"))
            }
        val playerState = RcPlayerState(doc)
        val operations =
            playerState.inspectableElements.first { it.name == "operations" }.value as List<*>
        val exprInspectable =
            operations.filterIsInstance<InspectableValue>().first {
                it.inspectableElements.any { e -> e.name == "kind" && e.value == "FloatExpression" }
            }
        val elements = exprInspectable.inspectableElements.associate { it.name to it.value }
        assertThat(exprInspectable.nameFallback).isEqualTo("rcVariable")
        assertThat(elements["variableId"]).isEqualTo(100)
        assertThat(elements["value"]).isEqualTo(3f)
    }

    @Test
    fun testToString() {
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(
                    FloatExpression(
                        100,
                        floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD),
                        null,
                    )
                )
            }
        val playerState = RcPlayerState(doc)
        val operations =
            playerState.inspectableElements.first { it.name == "operations" }.value as List<*>
        val exprInspectable = operations.filterIsInstance<InspectableValue>().single()
        assertThat(exprInspectable.valueOverride.toString()).contains("+")
    }

    @Test
    fun testDivByZeroIsNaN() {
        assertThat(evalSingleRpn(0f, 0f, AnimatedFloatExpression.DIV).isNaN()).isTrue()
    }

    // --- Operator coverage: verify RcPlayer RPN compilation against canonical core evaluator -----

    private fun assertBatchMatchesCore(vararg expressions: FloatArray) {
        val actuals = evalRpnBatch(*expressions)
        expressions.forEachIndexed { index, exp ->
            val expected = AnimatedFloatExpression().eval(exp.copyOf(), exp.size)
            val actual = actuals[index]
            if (expected.isNaN()) {
                assertWithMessage("Expression #$index").that(actual.isNaN()).isTrue()
            } else {
                assertWithMessage("Expression #$index").that(actual).isWithin(1e-3f).of(expected)
            }
        }
    }

    @Test
    fun testBinaryOperators() {
        assertBatchMatchesCore(
            floatArrayOf(7f, 3f, AnimatedFloatExpression.MOD),
            floatArrayOf(2f, 5f, AnimatedFloatExpression.MAX),
            floatArrayOf(2f, 5f, AnimatedFloatExpression.MIN),
            floatArrayOf(2f, 10f, AnimatedFloatExpression.POW),
            floatArrayOf(-3f, 1f, AnimatedFloatExpression.COPY_SIGN),
            floatArrayOf(1f, 1f, AnimatedFloatExpression.ATAN2),
            floatArrayOf(3f, 4f, AnimatedFloatExpression.HYPOT),
            floatArrayOf(5f, 3f, AnimatedFloatExpression.STEP),
            floatArrayOf(3f, 4f, AnimatedFloatExpression.SQUARE_SUM),
            floatArrayOf(7f, 3f, AnimatedFloatExpression.PINGPONG),
        )
    }

    @Test
    fun testUnaryOperators() {
        assertBatchMatchesCore(
            floatArrayOf(16f, AnimatedFloatExpression.SQRT),
            floatArrayOf(-3f, AnimatedFloatExpression.ABS),
            floatArrayOf(-2f, AnimatedFloatExpression.SIGN),
            floatArrayOf(1f, AnimatedFloatExpression.EXP),
            floatArrayOf(3.7f, AnimatedFloatExpression.FLOOR),
            floatArrayOf(100f, AnimatedFloatExpression.LOG),
            floatArrayOf(2.718281828f, AnimatedFloatExpression.LN),
            floatArrayOf(3.5f, AnimatedFloatExpression.ROUND),
            floatArrayOf(0.5f, AnimatedFloatExpression.SIN),
            floatArrayOf(0.5f, AnimatedFloatExpression.COS),
            floatArrayOf(0.5f, AnimatedFloatExpression.TAN),
            floatArrayOf(0.5f, AnimatedFloatExpression.ASIN),
            floatArrayOf(0.5f, AnimatedFloatExpression.ACOS),
            floatArrayOf(1f, AnimatedFloatExpression.ATAN),
            floatArrayOf(27f, AnimatedFloatExpression.CBRT),
            floatArrayOf(3.14159f, AnimatedFloatExpression.DEG),
            floatArrayOf(180f, AnimatedFloatExpression.RAD),
            floatArrayOf(3.2f, AnimatedFloatExpression.CEIL),
            floatArrayOf(4f, AnimatedFloatExpression.SQUARE),
            floatArrayOf(8f, AnimatedFloatExpression.LOG2),
            floatArrayOf(4f, AnimatedFloatExpression.INV),
            floatArrayOf(3.75f, AnimatedFloatExpression.FRACT),
            floatArrayOf(5f, AnimatedFloatExpression.CHANGE_SIGN),
        )
    }

    @Test
    fun testTernaryOperators() {
        assertBatchMatchesCore(
            floatArrayOf(2f, 3f, 4f, AnimatedFloatExpression.MAD),
            floatArrayOf(0f, 10f, 0.25f, AnimatedFloatExpression.LERP),
            floatArrayOf(1f, 2f, 1f, AnimatedFloatExpression.IFELSE),
            floatArrayOf(1f, 2f, -1f, AnimatedFloatExpression.IFELSE),
            floatArrayOf(9f, 5f, 0f, AnimatedFloatExpression.CLAMP),
            floatArrayOf(-3f, 5f, 0f, AnimatedFloatExpression.CLAMP),
            floatArrayOf(0.3f, 1f, 0f, AnimatedFloatExpression.SMOOTH_STEP),
            // CUBIC bezier easing: (x1,y1,x2,y2,pos) -> eased value.
            floatArrayOf(0.25f, 0.1f, 0.25f, 1.0f, 0.5f, AnimatedFloatExpression.CUBIC),
            floatArrayOf(0.4f, 0f, 0.2f, 1f, 0.3f, AnimatedFloatExpression.CUBIC),
        )
    }

    @Test
    fun testStackManipulationAndCompoundExpressions() {
        assertBatchMatchesCore(
            // DUP: x dup * == x^2
            floatArrayOf(6f, AnimatedFloatExpression.DUP, AnimatedFloatExpression.MUL),
            // SWAP: 3 7 swap - == 7 - 3 == 4
            floatArrayOf(3f, 7f, AnimatedFloatExpression.SWAP, AnimatedFloatExpression.SUB),
            // sqrt(3*3 + 4*4) == 5
            floatArrayOf(
                3f,
                3f,
                AnimatedFloatExpression.MUL,
                4f,
                4f,
                AnimatedFloatExpression.MUL,
                AnimatedFloatExpression.ADD,
                AnimatedFloatExpression.SQRT,
            ),
            // max(abs(-5), min(2, 9)) == 5
            floatArrayOf(
                -5f,
                AnimatedFloatExpression.ABS,
                2f,
                9f,
                AnimatedFloatExpression.MIN,
                AnimatedFloatExpression.MAX,
            ),
        )
    }

    @Test
    fun testRandEvaluatesInRangeViaImperativePath() {
        val v = evalSingleRpn(AnimatedFloatExpression.RAND)
        assertThat(v).isAtLeast(0f)
        assertThat(v).isLessThan(1f)
    }

    @Test
    fun testRegisterStoreLoadViaImperativePath() {
        assertBatchMatchesCore(
            floatArrayOf(
                5f,
                AnimatedFloatExpression.STORE_RO,
                3f,
                AnimatedFloatExpression.LOAD_R0,
                AnimatedFloatExpression.ADD,
            )
        )
    }

    @Test
    fun testNoiseFromIsDeterministicViaImperativePath() {
        assertBatchMatchesCore(
            floatArrayOf(0.5f, AnimatedFloatExpression.NOISE_FROM),
            floatArrayOf(42f, AnimatedFloatExpression.NOISE_FROM),
        )
    }

    @Test
    fun testArrayOpsResolveThroughCollectionsAccess() {
        val arrayId = NanMap.START_ARRAY
        val arrayNan = NanMap.asNan(arrayId)
        val results =
            evalRpnBatch(
                floatArrayOf(arrayNan, AnimatedFloatExpression.A_SUM),
                floatArrayOf(arrayNan, AnimatedFloatExpression.A_MAX),
                floatArrayOf(arrayNan, AnimatedFloatExpression.A_LEN),
                setupOps = listOf(DataListFloat(arrayId, floatArrayOf(2f, 5f, 3f))),
            )
        assertThat(results).containsExactly(10f, 5f, 3f).inOrder()
    }

    @Test
    fun testHostFloatOverrideBeatsAuthoredExpression() {
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(
                    FloatExpression(
                        100,
                        floatArrayOf(2f, 3f, AnimatedFloatExpression.ADD),
                        null,
                    )
                )
                operations.add(NamedVariable(100, NamedVariable.FLOAT_TYPE, "USER:sum"))
            }
        val state = RcPlayerState(doc)

        assertThat(state.floatState("sum").value).isEqualTo(5f)

        state.floatState("sum").value = 42f
        assertThat(state.floatState("sum").value).isEqualTo(42f)

        state.clearOverride("sum")
        assertThat(state.floatState("sum").value).isEqualTo(5f)
    }

    private fun CoreDocument.addTimeReadFloat(varId: Int, systemTimeId: Int, name: String) {
        operations.add(FloatExpression(varId, floatArrayOf(Utils.asNan(systemTimeId)), null))
        operations.add(NamedVariable(varId, NamedVariable.FLOAT_TYPE, "USER:$name"))
    }

    private fun CoreDocument.addTimeReadInt(varId: Int, systemTimeId: Int, name: String) {
        operations.add(
            IntegerExpression(
                varId,
                0b101,
                intArrayOf(systemTimeId, 0, IntegerExpressionEvaluator.I_ADD),
            )
        )
        operations.add(NamedVariable(varId, NamedVariable.INT_TYPE, "USER:$name"))
    }

    @Test
    fun testContinuousSecResolvesInGraphContext() {
        val baseInstant = Instant.parse("2026-01-01T12:30:45.123Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val doc =
            CoreDocument(clock).apply {
                addTimeReadFloat(100, RemoteContext.ID_ANIMATION_TIME, "animTime")
                addTimeReadFloat(101, RemoteContext.ID_TIME_IN_HR, "hr")
                addTimeReadFloat(102, RemoteContext.ID_TIME_IN_MIN, "min")
                addTimeReadFloat(103, RemoteContext.ID_TIME_IN_SEC, "sec")
                addTimeReadFloat(104, RemoteContext.ID_CONTINUOUS_SEC, "contSec")
                addTimeReadInt(105, RemoteContext.ID_EPOCH_SECOND, "epoch")
            }
        val state = RcPlayerState(doc)
        state.updateTime(5000f)

        // At t = 5s (12:30:50.123Z):
        assertThat(state.floatState("animTime").value).isEqualTo(5f)
        assertThat(state.floatState("hr").value).isEqualTo(12f)
        assertThat(state.floatState("min").value).isEqualTo(750f)
        assertThat(state.floatState("sec").value).isEqualTo(1850f)
        assertThat(state.floatState("contSec").value).isWithin(0.001f).of(1850.123f)
        assertThat(state.intState("epoch").value).isEqualTo((baseInstant.epochSecond + 5).toInt())

        // Advance to t = 65s (12:31:50.123Z):
        state.updateTime(65000f)
        assertThat(state.floatState("animTime").value).isEqualTo(65f)
        assertThat(state.floatState("hr").value).isEqualTo(12f)
        assertThat(state.floatState("min").value).isEqualTo(751f)
        assertThat(state.floatState("sec").value).isEqualTo(1910f)
        assertThat(state.floatState("contSec").value).isWithin(0.001f).of(1910.123f)
        assertThat(state.intState("epoch").value).isEqualTo((baseInstant.epochSecond + 65).toInt())
    }

    @Test
    fun testTimeFieldsOnlyInvalidateWhenTheirSpecificUnitChanges() {
        val baseInstant = Instant.parse("2026-01-01T12:30:45.100Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val doc =
            CoreDocument(clock).apply {
                addTimeReadFloat(100, RemoteContext.ID_CONTINUOUS_SEC, "cont")
                addTimeReadFloat(101, RemoteContext.ID_TIME_IN_SEC, "sec")
                addTimeReadFloat(102, RemoteContext.ID_TIME_IN_MIN, "min")
                addTimeReadFloat(103, RemoteContext.ID_TIME_IN_HR, "hr")
                addTimeReadFloat(104, RemoteContext.ID_CALENDAR_MONTH, "month")
            }
        val state = RcPlayerState(doc)

        var contEvals = 0
        var secEvals = 0
        var minEvals = 0
        var hrEvals = 0
        var monthEvals = 0

        val contDerived = derivedStateOf {
            contEvals++
            state.floatState("cont").value
        }
        val secDerived = derivedStateOf {
            secEvals++
            state.floatState("sec").value
        }
        val minDerived = derivedStateOf {
            minEvals++
            state.floatState("min").value
        }
        val hrDerived = derivedStateOf {
            hrEvals++
            state.floatState("hr").value
        }
        val monthDerived = derivedStateOf {
            monthEvals++
            state.floatState("month").value
        }

        // Initial reads
        assertThat(contDerived.value).isWithin(0.001f).of(1845.1f)
        assertThat(secDerived.value).isEqualTo(1845f)
        assertThat(minDerived.value).isEqualTo(750f)
        assertThat(hrDerived.value).isEqualTo(12f)
        assertThat(monthDerived.value).isEqualTo(1f)
        assertThat(contEvals).isEqualTo(1)
        assertThat(secEvals).isEqualTo(1)
        assertThat(minEvals).isEqualTo(1)
        assertThat(hrEvals).isEqualTo(1)
        assertThat(monthEvals).isEqualTo(1)

        // Advance 10 frames (16ms each = 160ms total, still within second 45 at .260Z)
        for (frame in 1..10) {
            Snapshot.withMutableSnapshot { state.updateTime(frame * 16f) }
            contDerived.value
            secDerived.value
            minDerived.value
            hrDerived.value
            monthDerived.value
        }

        // Continuous sec re-evaluated on every frame; quantized fields did NOT re-evaluate at all!
        assertThat(contEvals).isEqualTo(11)
        assertThat(secEvals).isEqualTo(1)
        assertThat(minEvals).isEqualTo(1)
        assertThat(hrEvals).isEqualTo(1)
        assertThat(monthEvals).isEqualTo(1)

        // Advance past second boundary to t = 1000ms (12:30:46.100Z)
        Snapshot.withMutableSnapshot { state.updateTime(1000f) }
        assertThat(secDerived.value).isEqualTo(1846f)
        assertThat(minDerived.value).isEqualTo(750f)
        assertThat(hrDerived.value).isEqualTo(12f)
        assertThat(monthDerived.value).isEqualTo(1f)
        assertThat(secEvals).isEqualTo(2)
        assertThat(minEvals).isEqualTo(1)
        assertThat(hrEvals).isEqualTo(1)
        assertThat(monthEvals).isEqualTo(1)

        // Advance past minute boundary to t = 15000ms (12:31:00.100Z)
        Snapshot.withMutableSnapshot { state.updateTime(15000f) }
        assertThat(minDerived.value).isEqualTo(751f)
        assertThat(hrDerived.value).isEqualTo(12f)
        assertThat(monthDerived.value).isEqualTo(1f)
        assertThat(minEvals).isEqualTo(2)
        assertThat(hrEvals).isEqualTo(1)
        assertThat(monthEvals).isEqualTo(1)
    }

    private fun buildLayoutDocumentWithWidthExpression(
        widthVarId: Int,
        writePreOps: (WireBuffer) -> Unit,
    ): CoreDocument = buildDocument {
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
    fun testExpressionDependsOnAnimationDetectsNestedAnimation() {
        // inner animates linearly from initialValue = 0f to target = 100f over 1s;
        // outer = inner + 10f has no animation of its own, so layout width must track inner's
        // live animated progression from 10dp -> ~60dp -> 110dp.
        val animSpec = FloatAnimation.packToFloatArray(1f, Easing.CUBIC_LINEAR, null, 0f, Float.NaN)
        val doc =
            buildLayoutDocumentWithWidthExpression(92) { wire ->
                FloatExpression.apply(wire, 91, floatArrayOf(100f), animSpec)
                FloatExpression.apply(
                    wire,
                    92,
                    floatArrayOf(Utils.asNan(91), 10f, AnimatedFloatExpression.ADD),
                    null,
                )
            }

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = doc) }
        }
        rule.mainClock.advanceTimeBy(32)
        val startWidth =
            rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width.value
        assertThat(startWidth).isLessThan(30f)

        rule.mainClock.advanceTimeBy(500)
        val midWidth =
            rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width.value
        assertThat(midWidth).isGreaterThan(35f)
        assertThat(midWidth).isLessThan(95f)

        rule.mainClock.advanceTimeBy(1000)
        val endWidth =
            rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width.value
        assertThat(endWidth).isWithin(1f).of(110f)
    }

    @Test(timeout = 5_000L)
    fun testExpressionDependsOnAnimationHandlesCyclesSafely() {
        val doc =
            buildLayoutDocumentWithWidthExpression(91) { wire ->
                FloatExpression.apply(
                    wire,
                    91,
                    floatArrayOf(Utils.asNan(92), 1f, AnimatedFloatExpression.ADD),
                    null,
                )
                FloatExpression.apply(
                    wire,
                    92,
                    floatArrayOf(Utils.asNan(91), 1f, AnimatedFloatExpression.ADD),
                    null,
                )
            }

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = doc) }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot()
    }

    @Test
    fun testBuildComputedOpIndexIncludesLayoutComponentCanvasOperations() {
        val expr =
            FloatExpression(
                100,
                floatArrayOf(10f, 20f, AnimatedFloatExpression.ADD),
                null,
            )
        val named = NamedVariable(100, NamedVariable.FLOAT_TYPE, "USER:canvasExpr")
        val canvasOps =
            CanvasOperations().apply {
                mList.add(expr)
                mList.add(named)
            }
        val layout =
            LayoutComponent(null, 1, 0, 0f, 0f, 100f, 100f).apply {
                setCanvasOperations(canvasOps)
            }
        val doc = CoreDocument(RemoteClock.SYSTEM).apply { operations.add(layout) }
        val state = RcPlayerState(doc)

        assertThat(state.floatState("canvasExpr").value).isEqualTo(30f)
    }

    @Test
    fun testIsExpressionTimeDependentDetectsTimeVariables() {
        assertThat(
                rule.observeClockCadence(
                    FloatExpression(
                        100,
                        floatArrayOf(Utils.asNan(RemoteContext.ID_CONTINUOUS_SEC)),
                        null,
                    )
                )
            )
            .isEqualTo(ClockCadence.CONTINUOUS_FRAME)
    }

    @Test
    fun testAnimationTimeDrivesContinuousFramesWhileDeltaTimeIsStatic() {
        assertThat(
                rule.observeClockCadence(
                    FloatExpression(
                        100,
                        floatArrayOf(Utils.asNan(RemoteContext.ID_ANIMATION_TIME)),
                        null,
                    )
                )
            )
            .isEqualTo(ClockCadence.CONTINUOUS_FRAME)
    }

    @Test
    fun testDeltaTimeExpressionDoesNotRunBackgroundClock() {
        assertThat(
                rule.observeClockCadence(
                    FloatExpression(
                        100,
                        floatArrayOf(Utils.asNan(RemoteContext.ID_ANIMATION_DELTA_TIME)),
                        null,
                    )
                )
            )
            .isEqualTo(ClockCadence.STATIC)
    }

    @Test
    fun testSnapshotQueriedAtWholeSecondBoundariesWithoutDoubleFractionalSec() {
        val baseInstant = Instant.parse("2026-01-01T12:30:45.450Z")
        val underlying = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val queriedTimestamps = mutableListOf<Long>()
        val trackingClock =
            object : RemoteClock by underlying {
                override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot {
                    val m = millis ?: underlying.millis()
                    queriedTimestamps.add(m)
                    val baseSnap = underlying.snapshot(m)
                    return object : RemoteClock.TimeSnapshot by baseSnap {
                        override fun getTimeInSec(): Float =
                            baseSnap.minute * 60f +
                                baseSnap.second +
                                baseSnap.millisOfSecond * 1e-3f
                    }
                }
            }

        val doc =
            CoreDocument(trackingClock).apply {
                addTimeReadFloat(100, RemoteContext.ID_TIME_IN_SEC, "sec")
                addTimeReadFloat(101, RemoteContext.ID_CONTINUOUS_SEC, "contSec")
            }
        val state = RcPlayerState(doc)

        // Note: CoreDocument initialization queries clock once during RemoteContext setup; clear
        // setup queries after capturing RcPlayerState's initial boundary check.
        val playerBoundaryQueries = queriedTimestamps.filter { it % 1000L == 0L }
        assertThat(playerBoundaryQueries).isNotEmpty()
        assertThat(state.floatState("sec").value).isEqualTo(1845f)
        assertThat(state.floatState("contSec").value).isWithin(1e-3f).of(1845.450f)

        val countBeforeIntraSecond = queriedTimestamps.size
        // Advance within the same second to 12:30:45.750Z (frameMillis = 300f)
        state.updateTime(300f)
        assertThat(queriedTimestamps).hasSize(countBeforeIntraSecond)
        assertThat(state.floatState("sec").value).isEqualTo(1845f)
        assertThat(state.floatState("contSec").value).isWithin(1e-3f).of(1845.750f)

        // Advance across second boundary to 12:30:46.200Z (frameMillis = 750f)
        state.updateTime(750f)
        assertThat(queriedTimestamps).hasSize(countBeforeIntraSecond + 1)
        assertThat(queriedTimestamps.last() % 1000L).isEqualTo(0L)
        assertThat(state.floatState("sec").value).isEqualTo(1846f)
        assertThat(state.floatState("contSec").value).isWithin(1e-3f).of(1846.200f)
    }

    @Test
    fun testFloatResultsFeedIntegerReader() {
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(FloatExpression(50, floatArrayOf(1.75f), null))
                operations.add(NamedVariable(50, NamedVariable.FLOAT_TYPE, "USER:exprVal"))
                operations.add(FloatConstant(51, 2.75f))
                operations.add(NamedVariable(51, NamedVariable.FLOAT_TYPE, "USER:constVal"))
            }
        val state = RcPlayerState(doc)
        assertThat(state.floatState("exprVal").value).isEqualTo(1.75f)
        assertThat(state.intState("exprVal").value).isEqualTo(1)

        assertThat(state.floatState("constVal").value).isEqualTo(2.75f)
        assertThat(state.intState("constVal").value).isEqualTo(2)
    }

    @Test(timeout = 5_000L)
    fun diamondFloatExpressionDag_evaluatesLinearlyAndTracksSnapshotDependencies() {
        val doc = CoreDocument(RemoteClock.SYSTEM)
        val rootId = 100
        doc.operations.add(FloatConstant(rootId, 0f))
        doc.operations.add(NamedVariable(rootId, NamedVariable.FLOAT_TYPE, "USER:root"))

        var aId = rootId
        var bId = 101
        doc.operations.add(
            FloatExpression(
                bId,
                floatArrayOf(Utils.asNan(rootId), 1f, AnimatedFloatExpression.ADD),
                null,
            )
        )

        var nextId = 102
        repeat(32) {
            val nextA = nextId++
            val nextB = nextId++
            doc.operations.add(
                FloatExpression(
                    nextA,
                    floatArrayOf(
                        Utils.asNan(aId),
                        Utils.asNan(bId),
                        AnimatedFloatExpression.ADD,
                        0.5f,
                        AnimatedFloatExpression.MUL,
                    ),
                    null,
                )
            )
            doc.operations.add(
                FloatExpression(
                    nextB,
                    floatArrayOf(
                        Utils.asNan(aId),
                        Utils.asNan(bId),
                        AnimatedFloatExpression.SUB,
                        0.5f,
                        AnimatedFloatExpression.MUL,
                    ),
                    null,
                )
            )
            aId = nextA
            bId = nextB
        }

        val finalId = nextId
        doc.operations.add(
            FloatExpression(
                finalId,
                floatArrayOf(Utils.asNan(aId), Utils.asNan(bId), AnimatedFloatExpression.ADD),
                null,
            )
        )
        doc.operations.add(NamedVariable(finalId, NamedVariable.FLOAT_TYPE, "USER:final"))

        val state = RcPlayerState(doc)
        val observerA = derivedStateOf { state.floatState("final").value }
        val observerB = derivedStateOf { state.floatState("final").value }

        val initialA = observerA.value
        val initialB = observerB.value
        assertThat(initialA).isEqualTo(initialB)

        // Mutate root in a new snapshot and verify both observers invalidate and recompute in O(D).
        Snapshot.withMutableSnapshot {
            state.floatState("root").value = 4f
        }

        val updatedA = observerA.value
        val updatedB = observerB.value
        assertThat(updatedA).isEqualTo(updatedB)
        assertThat(updatedA).isNotEqualTo(initialA)
    }

    @Test
    fun updateTime_updatesBothGraphContextAndCurrentTimeMillisState() {
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val doc =
            CoreDocument(clock).apply {
                addTimeReadFloat(100, RemoteContext.ID_ANIMATION_TIME, "animTime")
            }
        val state = RcPlayerState(doc)

        state.updateTime(250f)

        val currentMillis = state.currentTimeMillis
        assertThat(currentMillis).isEqualTo(250f)
        assertThat(state.floatState("animTime").value).isWithin(0.0001f).of(0.25f)
    }

    @Test
    fun preprocessDocument_reservedSystemVariableId_doesNotRemap() {
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val doc =
            CoreDocument(clock).apply {
                operations.add(FloatConstant(RemoteContext.ID_OFFSET_TO_UTC, 123.456f))
                addTimeReadFloat(100, RemoteContext.ID_OFFSET_TO_UTC, "utcOffset")
            }

        val state = RcPlayerState(doc)

        // Reserved system variable ID (10 = ID_OFFSET_TO_UTC) is not clobbered by FloatConstant;
        // the player resolves the system clock variable (0.0f in UTC).
        assertThat(state.floatState("utcOffset").value).isEqualTo(0f)
    }

    @Test
    fun testDiscreteTimeVariablesResolveViaGetIntegerAndIntegerExpression() {
        // 2026-03-15T12:30:45Z is a Sunday (ISO day-of-week = 7), day-of-year = 74
        val baseInstant = Instant.parse("2026-03-15T12:30:45.123Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.ofHours(2)))
        val exprId = 100
        val doc =
            CoreDocument(clock).apply {
                operations.add(
                    IntegerExpression(
                        exprId,
                        0b101,
                        intArrayOf(
                            RemoteContext.ID_TIME_IN_SEC,
                            10,
                            IntegerExpressionEvaluator.I_ADD,
                        ),
                    )
                )
                operations.add(NamedVariable(exprId, NamedVariable.INT_TYPE, "USER:secPlus10"))
                addTimeReadInt(101, RemoteContext.ID_TIME_IN_SEC, "sec")
                addTimeReadInt(102, RemoteContext.ID_TIME_IN_MIN, "min")
                addTimeReadInt(103, RemoteContext.ID_TIME_IN_HR, "hr")
                addTimeReadInt(104, RemoteContext.ID_CALENDAR_MONTH, "month")
                addTimeReadInt(105, RemoteContext.ID_DAY_OF_MONTH, "dayOfMonth")
                addTimeReadInt(106, RemoteContext.ID_WEEK_DAY, "weekDay")
                addTimeReadInt(107, RemoteContext.ID_DAY_OF_YEAR, "dayOfYear")
                addTimeReadInt(108, RemoteContext.ID_YEAR, "year")
                addTimeReadInt(109, RemoteContext.ID_OFFSET_TO_UTC, "offsetUtc")
                addTimeReadInt(110, RemoteContext.ID_EPOCH_SECOND, "epoch")
            }
        val state = RcPlayerState(doc)

        assertThat(state.intState("sec").value).isEqualTo(1845)
        assertThat(state.intState("min").value).isEqualTo(870)
        assertThat(state.intState("hr").value).isEqualTo(14)
        assertThat(state.intState("month").value).isEqualTo(3)
        assertThat(state.intState("dayOfMonth").value).isEqualTo(15)
        assertThat(state.intState("weekDay").value).isEqualTo(7)
        assertThat(state.intState("dayOfYear").value).isEqualTo(74)
        assertThat(state.intState("year").value).isEqualTo(2026)
        assertThat(state.intState("offsetUtc").value).isEqualTo(7200)
        assertThat(state.intState("epoch").value).isEqualTo(baseInstant.epochSecond.toInt())
        assertThat(state.intState("secPlus10").value).isEqualTo(1855)

        // Advance by 5 seconds:
        state.updateTime(5000f)
        assertThat(state.intState("sec").value).isEqualTo(1850)
        assertThat(state.intState("secPlus10").value).isEqualTo(1860)
    }

    @Test
    fun preprocessDocument_integerExpressionWithTimeVariable_setsHasDiscreteTime() {
        assertThat(
                rule.observeClockCadence(
                    IntegerExpression(
                        100,
                        0b101,
                        intArrayOf(
                            RemoteContext.ID_TIME_IN_SEC,
                            1,
                            IntegerExpressionEvaluator.I_ADD,
                        ),
                    )
                )
            )
            .isEqualTo(ClockCadence.DISCRETE_SECOND)
    }

    @Test
    fun preprocessDocument_floatExpressionWithContinuousTimeVariable_setsHasContinuousTime() {
        assertThat(
                rule.observeClockCadence(
                    FloatExpression(
                        100,
                        floatArrayOf(Utils.asNan(RemoteContext.ID_CONTINUOUS_SEC)),
                        null,
                    )
                )
            )
            .isEqualTo(ClockCadence.CONTINUOUS_FRAME)
    }

    @Test
    fun preprocessDocument_textFromFloatWithTimeVariable_setsHasDiscreteTime() {
        assertThat(
                rule.observeClockCadence(
                    TextFromFloat(100, Utils.asNan(RemoteContext.ID_TIME_IN_SEC), 2, 0, 0)
                )
            )
            .isEqualTo(ClockCadence.DISCRETE_SECOND)
    }

    @Test
    fun preprocessDocument_textLookupIntWithTimeVariable_setsHasDiscreteTime() {
        val t0 = TextData(201, "Mon")
        val listIds = DataListIds(200, IntArray(16) { 201 })
        assertThat(
                rule.observeClockCadence(
                    t0,
                    listIds,
                    TextLookupInt(100, 200, RemoteContext.ID_WEEK_DAY),
                )
            )
            .isEqualTo(ClockCadence.DISCRETE_SECOND)
    }

    @Test
    fun preprocessDocument_componentVisibilityWithContinuousTime_setsHasContinuousTime() {
        assertThat(
                rule.observeClockCadence(
                    ComponentVisibilityOperation(RemoteContext.ID_CONTINUOUS_SEC)
                )
            )
            .isEqualTo(ClockCadence.CONTINUOUS_FRAME)
    }

    @Test
    fun preprocessDocument_noTimeReferences_doesNotEnableClock() {
        assertThat(
                rule.observeClockCadence(
                    FloatExpression(100, floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD), null),
                    IntegerExpression(
                        101,
                        0b000,
                        intArrayOf(1, 2, IntegerExpressionEvaluator.I_ADD),
                    ),
                    TextFromFloat(102, 4f, 2, 0, 0),
                )
            )
            .isEqualTo(ClockCadence.STATIC)
    }

    @Test
    fun springAnimatedFloatExpression_evaluatesCorrectlyInGraphContextAfterSettling() {
        val springSpec = floatArrayOf(0f, 1400f, 74.833f, 0.001f, 0f)
        val doc =
            CoreDocument(RemoteClock.SYSTEM).apply {
                operations.add(FloatExpression(100, floatArrayOf(1f), springSpec))
                operations.add(NamedVariable(100, NamedVariable.FLOAT_TYPE, "USER:springVal"))
            }
        val state = RcPlayerState(doc)

        // Subsequent evaluations must return 1f (not null -> 0f) even after setup settled the
        // spring.
        assertThat(state.floatState("springVal").value).isEqualTo(1f)
        assertThat(state.floatState("springVal").value).isEqualTo(1f)
    }

    @Test
    fun testExpressionDependsOnAnimationDetectsSpringAnimation() {
        val springSpec = floatArrayOf(0f, 1400f, 74.833f, 0.001f, 0f)
        val doc =
            buildLayoutDocumentWithWidthExpression(93) { wire ->
                FloatConstant.apply(wire, 91, 0f)
                NamedVariable.apply(wire, 91, NamedVariable.FLOAT_TYPE, "USER:target")
                FloatExpression.apply(wire, 92, floatArrayOf(Utils.asNan(91)), springSpec)
                FloatExpression.apply(
                    wire,
                    93,
                    floatArrayOf(Utils.asNan(92), 10f, AnimatedFloatExpression.ADD),
                    null,
                )
            }
        val state = RcPlayerState(doc)

        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) { RcPlayer(state = state) }
        }
        rule.mainClock.advanceTimeBy(32)
        assertThat(
                rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width.value
            )
            .isWithin(1f)
            .of(10f)

        Snapshot.withMutableSnapshot { state.floatState("target").value = 100f }
        rule.mainClock.advanceTimeBy(80)
        val midWidth =
            rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width.value
        assertThat(midWidth).isGreaterThan(15f)
        assertThat(midWidth).isLessThan(108f)

        rule.mainClock.advanceTimeBy(1000)
        val endWidth =
            rule.onNodeWithContentDescription("target").getUnclippedBoundsInRoot().width.value
        assertThat(endWidth).isWithin(1f).of(110f)
    }
}
