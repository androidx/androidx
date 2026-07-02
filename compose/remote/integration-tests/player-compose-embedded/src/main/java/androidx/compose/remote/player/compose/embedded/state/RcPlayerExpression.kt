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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.CollectionsAccess
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.easing.CubicEasing
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.getFloatExpressionsReflection
import androidx.compose.remote.player.compose.embedded.mapEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

public sealed class RemoteOp : InspectableValue {
    public abstract fun eval(): Float

    override val inspectableElements: Sequence<ValueElement>
        get() = emptySequence()

    override val valueOverride: Any?
        get() = eval()
}

public class ConstantOp(public val value: Float) : RemoteOp() {
    override fun eval(): Float = value

    override fun toString(): String = value.toString()

    override val valueOverride: Any? = value
}

public class VarOp(public val id: Int, public val state: State<Float>) : RemoteOp() {
    override fun eval(): Float = state.value

    override fun toString(): String = "[$id]"

    override val valueOverride: Any? = state.value
    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("id", id), ValueElement("value", state.value))
}

public class AddOp(public val left: RemoteOp, public val right: RemoteOp) : RemoteOp() {
    override fun eval(): Float = left.eval() + right.eval()

    override fun toString(): String = "($left + $right)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("left", left), ValueElement("right", right))
}

public class SubOp(public val left: RemoteOp, public val right: RemoteOp) : RemoteOp() {
    override fun eval(): Float = left.eval() - right.eval()

    override fun toString(): String = "($left - $right)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("left", left), ValueElement("right", right))
}

public class MulOp(public val left: RemoteOp, public val right: RemoteOp) : RemoteOp() {
    override fun eval(): Float = left.eval() * right.eval()

    override fun toString(): String = "($left * $right)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("left", left), ValueElement("right", right))
}

public class DivOp(public val left: RemoteOp, public val right: RemoteOp) : RemoteOp() {
    override fun eval(): Float = left.eval() / right.eval()

    override fun toString(): String = "($left / $right)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("left", left), ValueElement("right", right))
}

public class MadOp(public val a: RemoteOp, public val b: RemoteOp, public val c: RemoteOp) :
    RemoteOp() {
    override fun eval(): Float = a.eval() * b.eval() + c.eval()

    override fun toString(): String = "($a * $b + $c)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("a", a), ValueElement("b", b), ValueElement("c", c))
}

public class LerpOp(
    public val start: RemoteOp,
    public val end: RemoteOp,
    public val fraction: RemoteOp,
) : RemoteOp() {
    override fun eval(): Float {
        val vs = start.eval()
        val ve = end.eval()
        val vf = fraction.eval()
        return vs + (ve - vs) * vf
    }

    override fun toString(): String = "lerp($start, $end, $fraction)"

    override val inspectableElements: Sequence<ValueElement>
        get() =
            sequenceOf(
                ValueElement("start", start),
                ValueElement("end", end),
                ValueElement("fraction", fraction),
            )
}

public class MinOp(public val left: RemoteOp, public val right: RemoteOp) : RemoteOp() {
    override fun eval(): Float = Math.min(left.eval(), right.eval())

    override fun toString(): String = "min($left, $right)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("left", left), ValueElement("right", right))
}

/** Cubic-bezier easing operator: `cubic(x1, y1, x2, y2, pos)` (mirrors core OP_CUBIC). */
public class CubicOp(
    public val x1: RemoteOp,
    public val y1: RemoteOp,
    public val x2: RemoteOp,
    public val y2: RemoteOp,
    public val pos: RemoteOp,
) : RemoteOp() {
    private val easing = CubicEasing()

    override fun eval(): Float {
        easing.setup(x1.eval(), y1.eval(), x2.eval(), y2.eval())
        return easing.get(pos.eval())
    }

    override fun toString(): String = "cubic($x1, $y1, $x2, $y2, $pos)"

    override val inspectableElements: Sequence<ValueElement>
        get() =
            sequenceOf(
                ValueElement("x1", x1),
                ValueElement("y1", y1),
                ValueElement("x2", x2),
                ValueElement("y2", y2),
                ValueElement("pos", pos),
            )
}

/**
 * Generic single-operand operator (e.g. sqrt, abs, sin); [fn] is applied to the evaluated operand.
 */
public class UnaryOp(
    private val name: String,
    public val operand: RemoteOp,
    private val fn: (Float) -> Float,
) : RemoteOp() {
    override fun eval(): Float = fn(operand.eval())

    override fun toString(): String = "$name($operand)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("operand", operand))
}

/** Generic two-operand operator (e.g. mod, max, pow, atan2). [left] is the deeper stack operand. */
public class BinaryOp(
    private val name: String,
    public val left: RemoteOp,
    public val right: RemoteOp,
    private val fn: (Float, Float) -> Float,
) : RemoteOp() {
    override fun eval(): Float = fn(left.eval(), right.eval())

    override fun toString(): String = "$name($left, $right)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("left", left), ValueElement("right", right))
}

/** Generic three-operand operator (e.g. ifElse, clamp, smoothStep); [a] is the deepest operand. */
public class TernaryOp(
    private val name: String,
    public val a: RemoteOp,
    public val b: RemoteOp,
    public val c: RemoteOp,
    private val fn: (Float, Float, Float) -> Float,
) : RemoteOp() {
    override fun eval(): Float = fn(a.eval(), b.eval(), c.eval())

    override fun toString(): String = "$name($a, $b, $c)"

    override val inspectableElements: Sequence<ValueElement>
        get() = sequenceOf(ValueElement("a", a), ValueElement("b", b), ValueElement("c", c))
}

/**
 * Integer opcodes that the lazy [RemoteOp] tree cannot model and that route to [ImperativeRpnOp]:
 * register store/load (eval-order mutable state), the RAND family (stateful / non-pure), VAR1..3
 * (external var array), and the array/collection ops (need a [CollectionsAccess]).
 */
private val IMPERATIVE_OPCODES: Set<Int> =
    listOf(
            AnimatedFloatExpression.RAND,
            AnimatedFloatExpression.RAND_SEED,
            AnimatedFloatExpression.NOISE_FROM,
            AnimatedFloatExpression.RAND_IN_RANGE,
            AnimatedFloatExpression.STORE_RO,
            AnimatedFloatExpression.STORE_R1,
            AnimatedFloatExpression.STORE_R2,
            AnimatedFloatExpression.STORE_R3,
            AnimatedFloatExpression.LOAD_R0,
            AnimatedFloatExpression.LOAD_R1,
            AnimatedFloatExpression.LOAD_R2,
            AnimatedFloatExpression.LOAD_R3,
            AnimatedFloatExpression.VAR1,
            AnimatedFloatExpression.VAR2,
            AnimatedFloatExpression.VAR3,
            AnimatedFloatExpression.A_DEREF,
            AnimatedFloatExpression.A_MAX,
            AnimatedFloatExpression.A_MIN,
            AnimatedFloatExpression.A_SUM,
            AnimatedFloatExpression.A_AVG,
            AnimatedFloatExpression.A_LEN,
            AnimatedFloatExpression.A_SPLINE,
            AnimatedFloatExpression.A_SPLINE_LOOP,
            AnimatedFloatExpression.A_SUM_TILL,
            AnimatedFloatExpression.A_SUM_XY,
            AnimatedFloatExpression.A_SUM_SQR,
            AnimatedFloatExpression.A_LERP,
        )
        .map { AnimatedFloatExpression.fromNaN(it) }
        .toSet()

/**
 * True if [exp] references an opcode the [RemoteOp] tree can't represent (see [IMPERATIVE_OPCODES])
 * or a data-variable / array id (consumed by the array ops) — in which case [parseRpn] returns an
 * [ImperativeRpnOp] instead of building the tree.
 */
private fun needsImperativeEval(exp: FloatArray): Boolean =
    exp.any { v ->
        v.isNaN() &&
            ((AnimatedFloatExpression.isMathOperator(v) &&
                AnimatedFloatExpression.fromNaN(v) in IMPERATIVE_OPCODES) ||
                NanMap.isDataVariable(v))
    }

/**
 * Linear, imperative RPN evaluator delegating to the core [AnimatedFloatExpression] for expressions
 * the lazy [RemoteOp] tree can't model — register store/load (eval-order mutable state), the RAND
 * family, VAR1..3, and array/collection ops. Reactive scalar variables are resolved from
 * [varStates] at eval time (so a wrapping `derivedStateOf` tracks them); data-variable / array ids
 * and operators pass through verbatim, and array ops resolve through [collections].
 *
 * Faithful by construction: rather than reimplement ~25 opcodes (and the spline/easing caches and
 * register state they depend on), it builds the same pre-resolved buffer the core's
 * `FloatExpression.updateVariables` would and calls the core evaluator. Registers, the cubic-easing
 * cache, and the spline cache live on the held instance (matching the core's per-instance
 * lifetime); the RAND seed is the core's process-static `Random`, exactly as in the legacy View
 * player.
 */
public class ImperativeRpnOp(
    private val exp: FloatArray,
    private val varStates: Map<Int, State<Float>>,
    private val collections: CollectionsAccess?,
) : RemoteOp() {
    private val core = AnimatedFloatExpression()
    private val buffer = FloatArray(exp.size)

    override fun eval(): Float {
        for (i in exp.indices) {
            val v = exp[i]
            val scalar =
                v.isNaN() &&
                    !AnimatedFloatExpression.isMathOperator(v) &&
                    varStates.containsKey(Utils.idFromNan(v))
            // Resolve known reactive scalar variables from their live state (matching the core's
            // updateVariables pass); pass operators, constants, and data-variable/array ids through
            // verbatim for the core evaluator to interpret.
            buffer[i] = if (scalar) varStates.getValue(Utils.idFromNan(v)).value else v
        }
        return if (collections != null) {
            core.eval(collections, buffer, buffer.size)
        } else {
            core.eval(buffer, buffer.size)
        }
    }

    override fun toString(): String = "imperativeRpn(${exp.size} ops)"
}

public fun parseRpn(
    exp: FloatArray,
    varStates: Map<Int, State<Float>>,
    collections: CollectionsAccess? = null,
): RemoteOp {
    if (needsImperativeEval(exp)) {
        return ImperativeRpnOp(exp, varStates, collections)
    }
    val stack = mutableListOf<RemoteOp>()
    fun pop(): RemoteOp = stack.removeAt(stack.size - 1)
    fun unary(name: String, fn: (Float) -> Float) {
        val operand = pop()
        stack.add(UnaryOp(name, operand, fn))
    }
    fun binary(name: String, fn: (Float, Float) -> Float) {
        val right = pop()
        val left = pop()
        stack.add(BinaryOp(name, left, right, fn))
    }
    fun ternary(name: String, fn: (Float, Float, Float) -> Float) {
        val c = pop()
        val b = pop()
        val a = pop()
        stack.add(TernaryOp(name, a, b, c, fn))
    }
    for (v in exp) {
        if (v.isNaN()) {
            if (AnimatedFloatExpression.isMathOperator(v)) {
                val opCode = AnimatedFloatExpression.fromNaN(v)
                when (opCode) {
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ADD) -> {
                        val right = stack.removeAt(stack.size - 1)
                        val left = stack.removeAt(stack.size - 1)
                        stack.add(AddOp(left, right))
                    }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SUB) -> {
                        val right = stack.removeAt(stack.size - 1)
                        val left = stack.removeAt(stack.size - 1)
                        stack.add(SubOp(left, right))
                    }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.MUL) -> {
                        val right = stack.removeAt(stack.size - 1)
                        val left = stack.removeAt(stack.size - 1)
                        stack.add(MulOp(left, right))
                    }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.DIV) -> {
                        val right = stack.removeAt(stack.size - 1)
                        val left = stack.removeAt(stack.size - 1)
                        stack.add(DivOp(left, right))
                    }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.MIN) -> {
                        val right = stack.removeAt(stack.size - 1)
                        val left = stack.removeAt(stack.size - 1)
                        stack.add(MinOp(left, right))
                    }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.MAD) -> {
                        val c = stack.removeAt(stack.size - 1)
                        val b = stack.removeAt(stack.size - 1)
                        val a = stack.removeAt(stack.size - 1)
                        stack.add(MadOp(a, b, c))
                    }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.LERP) -> {
                        val fraction = stack.removeAt(stack.size - 1)
                        val end = stack.removeAt(stack.size - 1)
                        val start = stack.removeAt(stack.size - 1)
                        stack.add(LerpOp(start, end, fraction))
                    }

                    // Binary (pop 2). Semantics mirror AnimatedFloatExpression.opEval; `left` is
                    // the
                    // deeper stack operand.
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.MOD) ->
                        binary("%") { l, r -> l % r }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.MAX) ->
                        binary("max") { l, r -> maxOf(l, r) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.POW) ->
                        binary("pow") { l, r -> l.pow(r) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.COPY_SIGN) ->
                        binary("copySign") { l, r -> Math.copySign(l, r) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ATAN2) ->
                        binary("atan2") { l, r -> atan2(l, r) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.HYPOT) ->
                        binary("hypot") { l, r -> hypot(l, r) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.STEP) ->
                        binary("step") { l, r -> if (l > r) 1f else 0f }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SQUARE_SUM) ->
                        binary("squareSum") { l, r -> l * l + r * r }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.PINGPONG) ->
                        binary("pingPong") { l, r ->
                            val span = r * 2
                            val t = l % span
                            if (t < r) t else span - t
                        }

                    // Unary (pop 1).
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SQRT) ->
                        unary("sqrt") { sqrt(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ABS) ->
                        unary("abs") { abs(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SIGN) ->
                        unary("sign") { sign(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.EXP) ->
                        unary("exp") { exp(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.FLOOR) ->
                        unary("floor") { floor(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.LOG) ->
                        unary("log") { log10(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.LN) ->
                        unary("ln") { ln(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ROUND) ->
                        unary("round") { Math.round(it).toFloat() }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SIN) ->
                        unary("sin") { sin(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.COS) ->
                        unary("cos") { cos(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.TAN) ->
                        unary("tan") { tan(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ASIN) ->
                        unary("asin") { asin(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ACOS) ->
                        unary("acos") { acos(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.ATAN) ->
                        unary("atan") { atan(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.CBRT) ->
                        unary("cbrt") { it.toDouble().pow(1.0 / 3.0).toFloat() }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.DEG) ->
                        unary("deg") { it * 57.29578f }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.RAD) ->
                        unary("rad") { it * 0.017453292f }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.CEIL) ->
                        unary("ceil") { ceil(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SQUARE) ->
                        unary("square") { it * it }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.LOG2) ->
                        unary("log2") { log2(it) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.INV) ->
                        unary("inv") { 1f / it }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.FRACT) ->
                        unary("fract") { it - it.toInt() }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.CHANGE_SIGN) ->
                        unary("neg") { -it }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.NOP) -> {
                        // No-op: leaves the stack unchanged (matches core OP_NOP).
                    }

                    // Ternary (pop 3); `a` is the deepest operand.
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.IFELSE) ->
                        ternary("ifElse") { a, b, c -> if (c > 0f) b else a }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.CLAMP) ->
                        ternary("clamp") { a, b, c -> minOf(maxOf(a, c), b) }
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SMOOTH_STEP) ->
                        ternary("smoothStep") { a, b, c ->
                            when {
                                a < c -> 0f
                                a > b -> 1f
                                else -> {
                                    val t = (a - c) / (b - c)
                                    t * t * (3f - 2f * t)
                                }
                            }
                        }

                    // Stack manipulation (resolved at parse time).
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.DUP) ->
                        stack.add(stack.last())
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.SWAP) -> {
                        val top = pop()
                        val next = pop()
                        stack.add(top)
                        stack.add(next)
                    }

                    // Cubic bezier easing: operands x1, y1, x2, y2, pos (pos on top).
                    AnimatedFloatExpression.fromNaN(AnimatedFloatExpression.CUBIC) -> {
                        val pos = pop()
                        val y2 = pop()
                        val x2 = pop()
                        val y1 = pop()
                        val x1 = pop()
                        stack.add(CubicOp(x1, y1, x2, y2, pos))
                    }

                    else -> {
                        // Unsupported operator (array/collection ops, RAND family, register
                        // store/load, CUBIC, VARn, CMDn, …). Degrade to 0 rather than crashing the
                        // whole render; the trailing imbalance is tolerated by the stack-collapse
                        // fallback at the end of parseRpn.
                        println(
                            "Warning: unsupported RPN operator ${AnimatedFloatExpression.toMathName(v)} " +
                                "($opCode); substituting 0"
                        )
                        stack.add(ConstantOp(0f))
                    }
                }
            } else {
                val id = Utils.idFromNan(v)
                val state =
                    varStates[id] ?: throw IllegalArgumentException("State not found for id $id")
                stack.add(VarOp(id, state))
            }
        } else {
            stack.add(ConstantOp(v))
        }
    }
    // A well-formed expression collapses to a single op. If an unsupported operator left the stack
    // unbalanced (see the `else` branch above), don't crash — take the last result, or 0 if empty.
    return when {
        stack.size == 1 -> stack[0]
        stack.isEmpty() -> ConstantOp(0f)
        else -> {
            println("Warning: RPN expression did not collapse to a single op (size=${stack.size})")
            stack.last()
        }
    }
}

@Composable
public fun rememberRemoteExpression(id: Int): State<Float> {
    val document = LocalCoreDocument.current
    val expr =
        document.getFloatExpressionsReflection()[id]
            ?: throw IllegalArgumentException("Expression not found for id $id")

    // Find all variables in the expression
    val varIds =
        remember(expr) {
            val ids = mutableSetOf<Int>()
            for (v in expr.mSrcValue) {
                if (
                    v.isNaN() &&
                        !AnimatedFloatExpression.isMathOperator(v) &&
                        !NanMap.isDataVariable(v)
                ) {
                    ids.add(Utils.idFromNan(v))
                }
            }
            ids.toList()
        }

    // Create states for all variables
    val varStates = varIds.associateWith { rememberRemoteFloatAsState(it) }

    // Array/collection ops (A_*) resolve their data through the document context's
    // CollectionsAccess.
    val collections = LocalRemoteContext.current.collectionsAccess

    val tree =
        remember(document, expr, collections) { parseRpn(expr.mSrcValue, varStates, collections) }
    return remember(tree) { derivedStateOf { tree.eval() } }
}

private data class FloatAnimationSpec(
    val durationMillis: Int,
    val easingType: Int,
    val initialValue: Float,
)

/**
 * Resolves an animation-bearing [FloatExpression] (one with a non-null `mFloatAnimation`) as a
 * Compose-native animated [State].
 *
 * The expression's *source* value — its RPN over its variables, evaluated by
 * [rememberRemoteExpression] and ignoring the animation — is taken as the animation target. A
 * Compose [Animatable], seeded at the authored initial value, animates toward that target with the
 * spec's duration and easing whenever the target changes. Animation is therefore driven entirely by
 * Compose's frame clock (here, via [Animatable.animateTo]); the player's frame loop and the core's
 * per-frame animation math are not involved. This is the "it's just a `State<Float>`" layer: it
 * sidesteps the core treating an appearance animation's initial value as its target on first
 * evaluation, and gives a real, interruptible, value-change animation when the source is reactive.
 *
 * The spec is parsed fresh from the immutable `mSrcAnimation` array rather than read off the live
 * `mFloatAnimation`, whose initial value the core overwrites with the target during its first
 * `updateVariables` pass.
 */
@Composable
internal fun rememberAnimatedRemoteFloat(id: Int): State<Float> {
    val document = LocalCoreDocument.current
    val expr =
        document.getFloatExpressionsReflection()[id]
            ?: throw IllegalArgumentException("Expression not found for id $id")

    // The value the animation moves toward: the expression's source, reactively.
    val targetState = rememberRemoteExpression(id)

    val spec =
        remember(expr) {
            val authored = FloatAnimation(*(expr.mSrcAnimation ?: floatArrayOf(1f)))
            FloatAnimationSpec(
                durationMillis = (authored.duration * 1000f).toInt().coerceAtLeast(0),
                easingType = authored.type,
                initialValue = authored.initialValue,
            )
        }

    val animatable =
        remember(id) {
            Animatable(if (spec.initialValue.isNaN()) targetState.value else spec.initialValue)
        }

    val target = targetState.value
    LaunchedEffect(animatable, target, spec) {
        if (spec.durationMillis <= 0) {
            animatable.snapTo(target)
        } else {
            animatable.animateTo(
                target,
                tween(durationMillis = spec.durationMillis, easing = mapEasing(spec.easingType)),
            )
        }
    }
    return animatable.asState()
}
