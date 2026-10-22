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

import androidx.annotation.RestrictTo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.operations.utilities.NanMap
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import androidx.compose.remote.player.compose.embedded.LocalCoreDocument
import androidx.compose.remote.player.compose.embedded.LocalRemoteContext
import androidx.compose.remote.player.compose.embedded.doc.mapEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import kotlin.math.sqrt

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberRemoteExpression(id: Int): State<Float> {
    val document = LocalCoreDocument.current
    val expr =
        document.getFloatExpressions()[id]
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
    val varStates = HashMap<Int, State<Float>>(varIds.size)
    for (i in 0 until varIds.size) {
        val varId = varIds[i]
        varStates[varId] = rememberRemoteFloatAsState(varId)
    }

    // Array/collection ops (A_*) resolve their data through the document context's
    // CollectionsAccess.
    val collections = LocalRemoteContext.current.collectionsAccess

    val tree =
        remember(document, expr, collections) { parseRpn(expr.mSrcValue, varStates, collections) }
    return remember(tree) { derivedStateOf { tree.eval() } }
}

private sealed interface FloatAnimationSpec {
    val initialValue: Float

    data class Tween(
        val durationMillis: Int,
        val easingType: Int,
        override val initialValue: Float,
    ) : FloatAnimationSpec

    data class Spring(
        val stiffness: Float,
        val dampingRatio: Float,
        val stopThreshold: Float,
    ) : FloatAnimationSpec {
        override val initialValue: Float
            get() = Float.NaN
    }
}

/**
 * Resolves an animation-bearing [FloatExpression] (one with a non-null `mFloatAnimation` or
 * `mSpring` encoded in `mSrcAnimation`) as a Compose-native animated [State].
 *
 * The expression's *source* value — its RPN over its variables, evaluated by
 * [rememberRemoteExpression] and ignoring the animation — is taken as the animation target. A
 * Compose [Animatable], seeded at the authored initial value, animates toward that target with the
 * spec's duration/easing or spring physics whenever the target changes. Animation is therefore
 * driven entirely by Compose's frame clock (here, via [Animatable.animateTo]); the player's frame
 * loop and the core's per-frame animation math are not involved. This is the "it's just a
 * `State<Float>`" layer: it sidesteps the core treating an appearance animation's initial value as
 * its target on first evaluation, and gives a real, interruptible, value-change animation when the
 * source is reactive.
 *
 * The spec is parsed fresh from the immutable `mSrcAnimation` array rather than read off the live
 * `mFloatAnimation`, whose initial value the core overwrites with the target during its first
 * `updateVariables` pass.
 */
@Composable
internal fun rememberAnimatedRemoteFloat(id: Int): State<Float> {
    val document = LocalCoreDocument.current
    val expr =
        document.getFloatExpressions()[id]
            ?: throw IllegalArgumentException("Expression not found for id $id")

    // The value the animation moves toward: the expression's source, reactively.
    val targetState = rememberRemoteExpression(id)

    val spec =
        remember(expr) {
            val srcAnim = expr.mSrcAnimation
            if (srcAnim != null && srcAnim.size > 4 && srcAnim[0] == 0f) {
                val stiffness = srcAnim[1]
                val dampingCoefficient = srcAnim[2]
                val dampingRatio =
                    if (stiffness > 0f) {
                        dampingCoefficient / (2f * sqrt(stiffness))
                    } else {
                        1f
                    }
                FloatAnimationSpec.Spring(
                    stiffness = stiffness,
                    dampingRatio = dampingRatio,
                    stopThreshold = srcAnim[3],
                )
            } else {
                val authored =
                    if (srcAnim != null) {
                        FloatAnimation(*srcAnim)
                    } else {
                        expr.mFloatAnimation ?: FloatAnimation(1f)
                    }
                FloatAnimationSpec.Tween(
                    durationMillis = (authored.duration * 1000f).toInt().coerceAtLeast(0),
                    easingType = authored.type,
                    initialValue = authored.initialValue,
                )
            }
        }

    val animatable =
        remember(id) {
            Animatable(if (spec.initialValue.isNaN()) targetState.value else spec.initialValue)
        }

    val target = targetState.value
    LaunchedEffect(animatable, target, spec) {
        when (spec) {
            is FloatAnimationSpec.Tween -> {
                if (spec.durationMillis <= 0) {
                    animatable.snapTo(target)
                } else if (animatable.value != target || animatable.isRunning) {
                    animatable.animateTo(
                        target,
                        tween(
                            durationMillis = spec.durationMillis,
                            easing = mapEasing(spec.easingType),
                        ),
                    )
                }
            }
            is FloatAnimationSpec.Spring -> {
                if (animatable.value != target || animatable.isRunning) {
                    animatable.animateTo(
                        target,
                        spring(
                            dampingRatio = spec.dampingRatio,
                            stiffness = spec.stiffness,
                            visibilityThreshold = spec.stopThreshold,
                        ),
                    )
                }
            }
        }
    }
    return animatable.asState()
}
