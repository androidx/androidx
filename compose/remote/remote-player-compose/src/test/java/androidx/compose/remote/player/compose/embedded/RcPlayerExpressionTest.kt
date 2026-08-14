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
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Utils
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
import androidx.compose.runtime.mutableStateOf
import com.google.common.truth.Truth.assertThat
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
        val timeState = mutableStateOf(5000f)
        val graph = GraphContext(realState, emptyIntObjectMap(), timeState, RemoteClock.SYSTEM)

        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isEqualTo(5f)

        timeState.value = 8000f
        assertThat(graph.getFloat(RemoteContext.ID_CONTINUOUS_SEC)).isEqualTo(8f)
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
}
