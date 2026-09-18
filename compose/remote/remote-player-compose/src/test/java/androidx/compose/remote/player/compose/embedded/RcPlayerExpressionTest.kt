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

import androidx.collection.emptyIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.ArrayAccess
import androidx.compose.remote.core.operations.utilities.CollectionsAccess
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.player.compose.embedded.state.AddOp
import androidx.compose.remote.player.compose.embedded.state.DivOp
import androidx.compose.remote.player.compose.embedded.state.LerpOp
import androidx.compose.remote.player.compose.embedded.state.MadOp
import androidx.compose.remote.player.compose.embedded.state.MulOp
import androidx.compose.remote.player.compose.embedded.state.SubOp
import androidx.compose.remote.player.compose.embedded.state.expressionDependsOnAnimation
import androidx.compose.remote.player.compose.embedded.state.parseRpn
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

class RcPlayerExpressionTest {

    @Test
    fun testParseSimpleAdd() {
        val exp = floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree).isInstanceOf(AddOp::class.java)
        assertThat(tree.eval()).isEqualTo(3f)
    }

    @Test
    fun testParseSimpleSub() {
        val exp = floatArrayOf(5f, 2f, AnimatedFloatExpression.SUB)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree).isInstanceOf(SubOp::class.java)
        assertThat(tree.eval()).isEqualTo(3f)
    }

    @Test
    fun testParseSimpleMul() {
        val exp = floatArrayOf(3f, 4f, AnimatedFloatExpression.MUL)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree).isInstanceOf(MulOp::class.java)
        assertThat(tree.eval()).isEqualTo(12f)
    }

    @Test
    fun testParseSimpleDiv() {
        val exp = floatArrayOf(12f, 3f, AnimatedFloatExpression.DIV)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree).isInstanceOf(DivOp::class.java)
        assertThat(tree.eval()).isEqualTo(4f)
    }

    @Test
    fun testParseMad() {
        val exp = floatArrayOf(2f, 3f, 4f, AnimatedFloatExpression.MAD)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree).isInstanceOf(MadOp::class.java)
        assertThat(tree.eval()).isEqualTo(10f) // 2 * 3 + 4
    }

    @Test
    fun testParseLerp() {
        val exp = floatArrayOf(10f, 20f, 0.5f, AnimatedFloatExpression.LERP)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree).isInstanceOf(LerpOp::class.java)
        assertThat(tree.eval()).isEqualTo(15f)
    }

    @Test
    fun testParseWithVariable() {
        val varId = 42
        val varValue = 10f
        val state = mutableStateOf(varValue)
        val stateMap = mapOf(varId to state)

        val exp = floatArrayOf(Utils.asNan(varId), 5f, AnimatedFloatExpression.ADD)
        val tree = parseRpn(exp, stateMap)
        assertThat(tree.eval()).isEqualTo(15f)

        state.value = 20f
        assertThat(tree.eval()).isEqualTo(25f)
    }

    @Test
    fun testInspectableValue() {
        val exp = floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD)
        val tree = parseRpn(exp, emptyMap())

        val elements = tree.inspectableElements.toList()
        assertThat(elements).hasSize(2)
        assertThat(elements[0].name).isEqualTo("left")
        assertThat(elements[1].name).isEqualTo("right")
    }

    @Test
    fun testToString() {
        val exp = floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD)
        val tree = parseRpn(exp, emptyMap())
        assertThat(tree.toString()).isEqualTo("(1.0 + 2.0)")
    }

    @Test
    fun testDivByZeroIsNaN() {
        val exp = floatArrayOf(0f, 0f, AnimatedFloatExpression.DIV) // 0/0 produces NaN
        assertThat(parseRpn(exp, emptyMap()).eval().isNaN()).isTrue()
    }

    // --- Operator coverage: verify parseRpn against the canonical core evaluator -----------------

    /** Evaluates [exp] with both parseRpn and core AnimatedFloatExpression; asserts they agree. */
    private fun assertMatchesCore(vararg exp: Float) {
        val expected = AnimatedFloatExpression().eval(exp.copyOf(), exp.size)
        val actual = parseRpn(exp.copyOf(), emptyMap()).eval()
        if (expected.isNaN()) {
            assertThat(actual.isNaN()).isTrue()
        } else {
            assertThat(actual).isWithin(1e-3f).of(expected)
        }
    }

    @Test
    fun testBinaryOperators() {
        assertMatchesCore(7f, 3f, AnimatedFloatExpression.MOD)
        assertMatchesCore(2f, 5f, AnimatedFloatExpression.MAX)
        assertMatchesCore(2f, 5f, AnimatedFloatExpression.MIN)
        assertMatchesCore(2f, 10f, AnimatedFloatExpression.POW)
        assertMatchesCore(-3f, 1f, AnimatedFloatExpression.COPY_SIGN)
        assertMatchesCore(1f, 1f, AnimatedFloatExpression.ATAN2)
        assertMatchesCore(3f, 4f, AnimatedFloatExpression.HYPOT)
        assertMatchesCore(5f, 3f, AnimatedFloatExpression.STEP)
        assertMatchesCore(3f, 4f, AnimatedFloatExpression.SQUARE_SUM)
        assertMatchesCore(7f, 3f, AnimatedFloatExpression.PINGPONG)
    }

    @Test
    fun testUnaryOperators() {
        assertMatchesCore(16f, AnimatedFloatExpression.SQRT)
        assertMatchesCore(-3f, AnimatedFloatExpression.ABS)
        assertMatchesCore(-2f, AnimatedFloatExpression.SIGN)
        assertMatchesCore(1f, AnimatedFloatExpression.EXP)
        assertMatchesCore(3.7f, AnimatedFloatExpression.FLOOR)
        assertMatchesCore(100f, AnimatedFloatExpression.LOG)
        assertMatchesCore(2.718281828f, AnimatedFloatExpression.LN)
        assertMatchesCore(3.5f, AnimatedFloatExpression.ROUND)
        assertMatchesCore(0.5f, AnimatedFloatExpression.SIN)
        assertMatchesCore(0.5f, AnimatedFloatExpression.COS)
        assertMatchesCore(0.5f, AnimatedFloatExpression.TAN)
        assertMatchesCore(0.5f, AnimatedFloatExpression.ASIN)
        assertMatchesCore(0.5f, AnimatedFloatExpression.ACOS)
        assertMatchesCore(1f, AnimatedFloatExpression.ATAN)
        assertMatchesCore(27f, AnimatedFloatExpression.CBRT)
        assertMatchesCore(3.14159f, AnimatedFloatExpression.DEG)
        assertMatchesCore(180f, AnimatedFloatExpression.RAD)
        assertMatchesCore(3.2f, AnimatedFloatExpression.CEIL)
        assertMatchesCore(4f, AnimatedFloatExpression.SQUARE)
        assertMatchesCore(8f, AnimatedFloatExpression.LOG2)
        assertMatchesCore(4f, AnimatedFloatExpression.INV)
        assertMatchesCore(3.75f, AnimatedFloatExpression.FRACT)
        assertMatchesCore(5f, AnimatedFloatExpression.CHANGE_SIGN)
    }

    @Test
    fun testTernaryOperators() {
        assertMatchesCore(2f, 3f, 4f, AnimatedFloatExpression.MAD)
        assertMatchesCore(0f, 10f, 0.25f, AnimatedFloatExpression.LERP)
        assertMatchesCore(1f, 2f, 1f, AnimatedFloatExpression.IFELSE)
        assertMatchesCore(1f, 2f, -1f, AnimatedFloatExpression.IFELSE)
        assertMatchesCore(9f, 5f, 0f, AnimatedFloatExpression.CLAMP)
        assertMatchesCore(-3f, 5f, 0f, AnimatedFloatExpression.CLAMP)
        assertMatchesCore(0.3f, 1f, 0f, AnimatedFloatExpression.SMOOTH_STEP)
        // CUBIC bezier easing: (x1,y1,x2,y2,pos) -> eased value.
        assertMatchesCore(0.25f, 0.1f, 0.25f, 1.0f, 0.5f, AnimatedFloatExpression.CUBIC)
        assertMatchesCore(0.4f, 0f, 0.2f, 1f, 0.3f, AnimatedFloatExpression.CUBIC)
    }

    @Test
    fun testStackManipulationAndCompoundExpressions() {
        // DUP: x dup * == x^2
        assertMatchesCore(6f, AnimatedFloatExpression.DUP, AnimatedFloatExpression.MUL)
        // SWAP: 3 7 swap - == 7 - 3 == 4
        assertMatchesCore(3f, 7f, AnimatedFloatExpression.SWAP, AnimatedFloatExpression.SUB)
        // sqrt(3*3 + 4*4) == 5
        assertMatchesCore(
            3f,
            3f,
            AnimatedFloatExpression.MUL,
            4f,
            4f,
            AnimatedFloatExpression.MUL,
            AnimatedFloatExpression.ADD,
            AnimatedFloatExpression.SQRT,
        )
        // max(abs(-5), min(2, 9)) == 5
        assertMatchesCore(
            -5f,
            AnimatedFloatExpression.ABS,
            2f,
            9f,
            AnimatedFloatExpression.MIN,
            AnimatedFloatExpression.MAX,
        )
    }

    @Test
    fun testRandEvaluatesInRangeViaImperativePath() {
        // RAND routes through the imperative evaluator (delegating to the core); it yields [0,1).
        val tree = parseRpn(floatArrayOf(AnimatedFloatExpression.RAND), emptyMap())
        val v = tree.eval()
        assertThat(v).isAtLeast(0f)
        assertThat(v).isLessThan(1f)
    }

    @Test
    fun testRegisterStoreLoadViaImperativePath() {
        // 5 STORE_R0 3 LOAD_R0 ADD == 3 + 5 == 8 (registers need eval-order state -> imperative
        // path).
        assertMatchesCore(
            5f,
            AnimatedFloatExpression.STORE_RO,
            3f,
            AnimatedFloatExpression.LOAD_R0,
            AnimatedFloatExpression.ADD,
        )
    }

    @Test
    fun testNoiseFromIsDeterministicViaImperativePath() {
        // NOISE_FROM is a pure hash (no RNG state); the imperative path must match the core
        // exactly.
        assertMatchesCore(0.5f, AnimatedFloatExpression.NOISE_FROM)
        assertMatchesCore(42f, AnimatedFloatExpression.NOISE_FROM)
    }

    @Test
    fun testArrayOpsResolveThroughCollectionsAccess() {
        // A_SUM / A_MAX / A_LEN over a float array supplied via a CollectionsAccess. The array id
        // is a
        // data-variable NaN (in the array id-region) consumed by the op; the imperative evaluator
        // passes it through verbatim and the core decodes it.
        val arrayId = NanMap.START_ARRAY
        val data = floatArrayOf(2f, 5f, 3f)
        val ca =
            object : CollectionsAccess {
                override fun getFloatValue(id: Int, index: Int): Float = data[index]

                override fun getFloats(id: Int): FloatArray? = if (id == arrayId) data else null

                override fun getDynamicFloats(id: Int): FloatArray? = getFloats(id)

                override fun getArray(id: Int): ArrayAccess? = null

                override fun getListLength(id: Int): Int = if (id == arrayId) data.size else 0

                override fun getId(listId: Int, index: Int): Int = 0
            }
        val arrayNan = NanMap.asNan(arrayId)
        assertThat(
                parseRpn(floatArrayOf(arrayNan, AnimatedFloatExpression.A_SUM), emptyMap(), ca)
                    .eval()
            )
            .isEqualTo(10f)
        assertThat(
                parseRpn(floatArrayOf(arrayNan, AnimatedFloatExpression.A_MAX), emptyMap(), ca)
                    .eval()
            )
            .isEqualTo(5f)
        assertThat(
                parseRpn(floatArrayOf(arrayNan, AnimatedFloatExpression.A_LEN), emptyMap(), ca)
                    .eval()
            )
            .isEqualTo(3f)
    }

    @Test
    fun testHostFloatOverrideBeatsAuthoredExpression() {
        val realState = SnapshotRemoteComposeState()
        val op = FloatExpression(100, floatArrayOf(2f, 3f, AnimatedFloatExpression.ADD), null)
        val opsMap = mutableIntObjectMapOf<Operation>()
        opsMap[100] = op
        val timeState = mutableStateOf(0f)
        val graph = GraphContext(realState, opsMap, timeState, RemoteClock.SYSTEM)

        assertThat(graph.getFloat(100)).isEqualTo(5f)
        assertThat(realState.isFloatOverridden(100)).isFalse()

        realState.overrideFloat(100, 42f)
        assertThat(realState.isFloatOverridden(100)).isTrue()
        assertThat(graph.getFloat(100)).isEqualTo(42f)
    }

    @Test
    fun testContinuousSecResolvesInGraphContext() {
        val realState = SnapshotRemoteComposeState()
        val baseInstant = Instant.parse("2026-01-01T12:30:45.123Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val graph = GraphContext(realState, emptyIntObjectMap(), clock = clock)
        graph.updateTime(5000f)

        // At t = 5s (12:30:50.123Z):
        assertThat(graph.getFloat(RemoteContext.ID_ANIMATION_TIME)).isEqualTo(5f)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_HR)).isEqualTo(12f)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_MIN)).isEqualTo(750f)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_SEC)).isEqualTo(1850f)
        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isWithin(0.001f).of(1850.123f)
        assertThat(graph.getInteger(RemoteContext.ID_EPOCH_SECOND))
            .isEqualTo((baseInstant.epochSecond + 5).toInt())

        // Advance to t = 65s (12:31:50.123Z):
        graph.updateTime(65000f)
        assertThat(graph.getFloat(RemoteContext.ID_ANIMATION_TIME)).isEqualTo(65f)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_HR)).isEqualTo(12f)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_MIN)).isEqualTo(751f)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_SEC)).isEqualTo(1910f)
        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isWithin(0.001f).of(1910.123f)
        assertThat(graph.getInteger(RemoteContext.ID_EPOCH_SECOND))
            .isEqualTo((baseInstant.epochSecond + 65).toInt())
    }

    @Test
    fun testTimeFieldsOnlyInvalidateWhenTheirSpecificUnitChanges() {
        val realState = SnapshotRemoteComposeState()
        val baseInstant = Instant.parse("2026-01-01T12:30:45.100Z")
        val clock = SystemClock(Clock.fixed(baseInstant, ZoneOffset.UTC))
        val graph = GraphContext(realState, emptyIntObjectMap(), clock = clock)

        var contEvals = 0
        var secEvals = 0
        var minEvals = 0
        var hrEvals = 0
        var monthEvals = 0

        val contDerived = derivedStateOf {
            contEvals++
            graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)
        }
        val secDerived = derivedStateOf {
            secEvals++
            graph.getFloat(RemoteContext.ID_TIME_IN_SEC)
        }
        val minDerived = derivedStateOf {
            minEvals++
            graph.getFloat(RemoteContext.ID_TIME_IN_MIN)
        }
        val hrDerived = derivedStateOf {
            hrEvals++
            graph.getFloat(RemoteContext.ID_TIME_IN_HR)
        }
        val monthDerived = derivedStateOf {
            monthEvals++
            graph.getFloat(RemoteContext.ID_CALENDAR_MONTH)
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
            Snapshot.withMutableSnapshot { graph.updateTime(frame * 16f) }
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
        Snapshot.withMutableSnapshot { graph.updateTime(1000f) }
        assertThat(secDerived.value).isEqualTo(1846f)
        assertThat(minDerived.value).isEqualTo(750f)
        assertThat(hrDerived.value).isEqualTo(12f)
        assertThat(monthDerived.value).isEqualTo(1f)
        assertThat(secEvals).isEqualTo(2)
        assertThat(minEvals).isEqualTo(1)
        assertThat(hrEvals).isEqualTo(1)
        assertThat(monthEvals).isEqualTo(1)

        // Advance past minute boundary to t = 15000ms (12:31:00.100Z)
        Snapshot.withMutableSnapshot { graph.updateTime(15000f) }
        assertThat(minDerived.value).isEqualTo(751f)
        assertThat(hrDerived.value).isEqualTo(12f)
        assertThat(monthDerived.value).isEqualTo(1f)
        assertThat(minEvals).isEqualTo(2)
        assertThat(hrEvals).isEqualTo(1)
        assertThat(monthEvals).isEqualTo(1)
    }

    @Test
    fun testExpressionDependsOnAnimationDetectsNestedAnimation() {
        val inner =
            FloatExpression(1, floatArrayOf(10f), null).apply {
                mFloatAnimation = FloatAnimation(1f)
            }
        val outer =
            FloatExpression(2, floatArrayOf(Utils.asNan(1), 5f, AnimatedFloatExpression.ADD), null)
        val standalone = FloatExpression(3, floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD), null)

        val map = mapOf(1 to inner, 2 to outer, 3 to standalone)
        assertThat(expressionDependsOnAnimation(map, 1)).isTrue()
        assertThat(expressionDependsOnAnimation(map, 2)).isTrue()
        assertThat(expressionDependsOnAnimation(map, 3)).isFalse()
    }

    @Test
    fun testExpressionDependsOnAnimationHandlesCyclesSafely() {
        val exprA =
            FloatExpression(
                10,
                floatArrayOf(Utils.asNan(11), 1f, AnimatedFloatExpression.ADD),
                null,
            )
        val exprB =
            FloatExpression(
                11,
                floatArrayOf(Utils.asNan(10), 1f, AnimatedFloatExpression.ADD),
                null,
            )
        val cycleMap = mapOf(10 to exprA, 11 to exprB)
        assertThat(expressionDependsOnAnimation(cycleMap, 10)).isFalse()
    }

    @Test
    fun testBuildComputedOpIndexIncludesLayoutComponentCanvasOperations() {
        val expr =
            FloatExpression(
                100,
                floatArrayOf(10f, 20f, AnimatedFloatExpression.ADD),
                null,
            )
        val canvasOps = CanvasOperations().apply { mList.add(expr) }
        val layout =
            LayoutComponent(null, 1, 0, 0f, 0f, 100f, 100f).apply {
                setCanvasOperations(canvasOps)
            }

        val index = buildComputedOpIndex(listOf(layout))
        assertThat(index.containsKey(100)).isTrue()
        assertThat(index[100]).isSameInstanceAs(expr)
    }

    @Test
    fun testIsExpressionTimeDependentDetectsTimeVariables() {
        val continuousSecExpr =
            FloatExpression(1, floatArrayOf(Utils.asNan(RemoteContext.ID_CONTINUOUS_SEC)), null)
        val animationTimeExpr =
            FloatExpression(8, floatArrayOf(Utils.asNan(RemoteContext.ID_ANIMATION_TIME)), null)
        val epochSecondExpr =
            FloatExpression(2, floatArrayOf(Utils.asNan(RemoteContext.ID_EPOCH_SECOND)), null)
        val timeInSecExpr =
            FloatExpression(3, floatArrayOf(Utils.asNan(RemoteContext.ID_TIME_IN_SEC)), null)
        val timeInMinExpr =
            FloatExpression(4, floatArrayOf(Utils.asNan(RemoteContext.ID_TIME_IN_MIN)), null)
        val timeInHrExpr =
            FloatExpression(5, floatArrayOf(Utils.asNan(RemoteContext.ID_TIME_IN_HR)), null)
        val dayOfMonthExpr =
            FloatExpression(6, floatArrayOf(Utils.asNan(RemoteContext.ID_DAY_OF_MONTH)), null)
        val plainExpr = FloatExpression(7, floatArrayOf(1f, 2f, AnimatedFloatExpression.ADD), null)

        val deltaTimeExpr =
            FloatExpression(
                9,
                floatArrayOf(Utils.asNan(RemoteContext.ID_ANIMATION_DELTA_TIME)),
                null,
            )

        assertThat(isExpressionContinuousTimeDependent(continuousSecExpr)).isTrue()
        assertThat(isExpressionDiscreteTimeDependent(continuousSecExpr)).isFalse()
        assertThat(isExpressionContinuousTimeDependent(animationTimeExpr)).isTrue()
        assertThat(isExpressionDiscreteTimeDependent(animationTimeExpr)).isFalse()
        assertThat(isContinuousTimeVariable(RemoteContext.ID_ANIMATION_DELTA_TIME)).isFalse()
        assertThat(isExpressionContinuousTimeDependent(deltaTimeExpr)).isFalse()
        assertThat(isExpressionDiscreteTimeDependent(deltaTimeExpr)).isFalse()
        assertThat(isExpressionTimeDependent(deltaTimeExpr)).isFalse()

        assertThat(isExpressionDiscreteTimeDependent(epochSecondExpr)).isTrue()
        assertThat(isExpressionContinuousTimeDependent(epochSecondExpr)).isFalse()
        assertThat(isExpressionDiscreteTimeDependent(timeInSecExpr)).isTrue()
        assertThat(isExpressionDiscreteTimeDependent(timeInMinExpr)).isTrue()
        assertThat(isExpressionDiscreteTimeDependent(timeInHrExpr)).isTrue()
        assertThat(isExpressionDiscreteTimeDependent(dayOfMonthExpr)).isTrue()

        assertThat(isExpressionTimeDependent(continuousSecExpr)).isTrue()
        assertThat(isExpressionTimeDependent(epochSecondExpr)).isTrue()
        assertThat(isExpressionTimeDependent(timeInSecExpr)).isTrue()
        assertThat(isExpressionTimeDependent(timeInMinExpr)).isTrue()
        assertThat(isExpressionTimeDependent(timeInHrExpr)).isTrue()
        assertThat(isExpressionTimeDependent(dayOfMonthExpr)).isTrue()
        assertThat(isExpressionTimeDependent(plainExpr)).isFalse()
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
                        // Even if a custom TimeSnapshot implementation returns fractional timeInSec
                        // when queried at a non-boundary timestamp, querying at boundaryMillis
                        // ensures timeInSec is integer-aligned and continuousSec does not
                        // double-count fractional milliseconds.
                        override fun getTimeInSec(): Float =
                            baseSnap.minute * 60f +
                                baseSnap.second +
                                baseSnap.millisOfSecond * 1e-3f
                    }
                }
            }

        val graph =
            GraphContext(SnapshotRemoteComposeState(), emptyIntObjectMap(), clock = trackingClock)
        // Initial updateTime(0f) at 12:30:45.450Z -> must query snapshot at 12:30:45.000Z
        assertThat(queriedTimestamps).hasSize(1)
        assertThat(queriedTimestamps[0] % 1000L).isEqualTo(0L)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_SEC)).isEqualTo(1845f)
        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isWithin(1e-3f).of(1845.450f)

        // Advance within the same second to 12:30:45.750Z (frameMillis = 300f)
        graph.updateTime(300f)
        assertThat(queriedTimestamps).hasSize(1)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_SEC)).isEqualTo(1845f)
        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isWithin(1e-3f).of(1845.750f)

        // Advance across second boundary to 12:30:46.200Z (frameMillis = 750f)
        graph.updateTime(750f)
        assertThat(queriedTimestamps).hasSize(2)
        assertThat(queriedTimestamps[1] % 1000L).isEqualTo(0L)
        assertThat(graph.getFloat(RemoteContext.ID_TIME_IN_SEC)).isEqualTo(1846f)
        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isWithin(1e-3f).of(1846.200f)
    }

    @Test
    fun testFloatResultsFeedIntegerReader() {
        val floatExpr =
            FloatExpression(
                50,
                floatArrayOf(1.75f),
                null,
            )
        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(50, floatExpr)
            }
        val realState = SnapshotRemoteComposeState()
        val graph =
            GraphContext(
                realState = realState,
                computedOps = computedOps,
                timeMillis = mutableStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )
        assertThat(graph.getFloat(50)).isEqualTo(1.75f)
        assertThat(graph.getInteger(50)).isEqualTo(1)

        realState.updateFloat(51, 2.75f)
        assertThat(realState.getFloat(51)).isEqualTo(2.75f)
        assertThat(realState.getInteger(51)).isEqualTo(2)
    }

    @Test(timeout = 5_000L)
    fun diamondFloatExpressionDag_evaluatesLinearlyAndTracksSnapshotDependencies() {
        val realState = SnapshotRemoteComposeState()
        val rootId = 100
        realState.updateFloat(rootId, 0f)

        val computedOps = mutableIntObjectMapOf<Operation>()
        var aId = rootId
        var bId = 101
        computedOps.put(
            bId,
            FloatExpression(
                bId,
                floatArrayOf(Utils.asNan(rootId), 1f, AnimatedFloatExpression.ADD),
                null,
            ),
        )

        var nextId = 102
        repeat(32) {
            val nextA = nextId++
            val nextB = nextId++
            computedOps.put(
                nextA,
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
                ),
            )
            computedOps.put(
                nextB,
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
                ),
            )
            aId = nextA
            bId = nextB
        }

        val finalId = nextId
        computedOps.put(
            finalId,
            FloatExpression(
                finalId,
                floatArrayOf(Utils.asNan(aId), Utils.asNan(bId), AnimatedFloatExpression.ADD),
                null,
            ),
        )

        val graph =
            GraphContext(
                realState = realState,
                computedOps = computedOps,
                timeMillis = mutableStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        // Note: (a + b)*0.5 + (a - b)*0.5 == a at every stage, so finalCoord == a_31.
        // At root = 0f: a_0 = 0, b_0 = 1 -> a_1 = 0.5, b_1 = -0.5 -> a_2 = 0, b_2 = 0.5 ...
        val observerA = derivedStateOf { graph.getFloat(finalId) }
        val observerB = derivedStateOf { graph.getFloat(finalId) }

        val initialA = observerA.value
        val initialB = observerB.value
        assertThat(initialA).isEqualTo(initialB)

        // Mutate root in a new snapshot and verify both observers invalidate and recompute in O(D).
        Snapshot.withMutableSnapshot {
            realState.updateFloat(rootId, 4f)
        }

        val updatedA = observerA.value
        val updatedB = observerB.value
        assertThat(updatedA).isEqualTo(updatedB)
        assertThat(updatedA).isNotEqualTo(initialA)
    }
}
