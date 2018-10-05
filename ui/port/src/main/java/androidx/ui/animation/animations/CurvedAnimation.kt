/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.animation.animations

import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationStatus
import androidx.ui.animation.Curve
import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.runtimeType
import kotlin.math.roundToInt

/**
 * An animation that applies a curve to another animation.
 *
 * [CurvedAnimation] is useful when you want to apply a non-linear [Curve] to
 * an animation object wrapped in the [CurvedAnimation].
 *
 * For example, the following code snippet shows how you can apply a curve to a
 * linear animation produced by an [AnimationController]:
 *
 * ``` dart
 *     final AnimationController controller =
 *         new AnimationController(duration: const Duration(milliseconds: 500));
 *     final CurvedAnimation animation =
 *         new CurvedAnimation(parent: controller, curve: Curves.ease);
 *```
 * Depending on the given curve, the output of the [CurvedAnimation] could have
 * a wider range than its input. For example, elastic curves such as
 * [Curves.elasticIn] will significantly overshoot or undershoot the default
 * range of 0.0 to 1.0.
 *
 * If you want to apply a [Curve] to a [Tween], consider using [CurveTween].
 */
class CurvedAnimation(
    /** The animation to which this animation applies a curve. */
    parent: Animation<Double>,
    /** The curve to use in the forward direction. */
    private val curve: Curve,
    /**
     * The curve to use in the reverse direction.
     *
     * If the parent animation changes direction without first reaching the
     * [AnimationStatus.COMPLETED] or [AnimationStatus.DISMISSED] status, the
     * [CurvedAnimation] stays on the same curve (albeit in the opposite
     * direction) to avoid visual discontinuities.
     *
     * If you use a non-null [reverseCurve], you might want to hold this object
     * in a [State] object rather than recreating it each time your widget builds
     * in order to take advantage of the state in this object that avoids visual
     * discontinuities.
     *
     * If this field is null, uses [curve] in both directions.
     */
    private val reverseCurve: Curve? = null
) : AnimationWithParentMixin<Double>(parent) {

    /**
     * The direction used to select the current curve.
     *
     * The curve direction is only reset when we hit the beginning or the end of
     * the timeline to avoid discontinuities in the value of any variables this
     * a animation is used to animate.
     */
    private var curveDirection: AnimationStatus? = null

    private fun updateCurveDirection(status: AnimationStatus) {
        curveDirection = when (status) {
            AnimationStatus.DISMISSED,
            AnimationStatus.COMPLETED -> {
                null
            }
            AnimationStatus.FORWARD -> {
                curveDirection ?: AnimationStatus.FORWARD
            }
            AnimationStatus.REVERSE -> {
                curveDirection ?: AnimationStatus.REVERSE
            }
        }
    }

    init {
        updateCurveDirection(parent.status)
        parent.addStatusListener(this::updateCurveDirection)
    }

    private val useForwardCurve: Boolean
        get() = reverseCurve == null ||
                (curveDirection ?: parent.status) != AnimationStatus.REVERSE

    override val value: Double
        get() {
            val activeCurve = if (useForwardCurve) curve else reverseCurve

            val t = parent.value
            if (t == Double.NaN) {
                toString()
            }
            if (activeCurve == null)
                return t
            if (t == 0.0 || t == 1.0) {
                assert {
                    val transformedValue = activeCurve.transform(t)
                    val roundedTransformedValue = transformedValue.roundToInt().toDouble()
                    if (roundedTransformedValue != t) {
                        throw FlutterError(
                            "Invalid curve endpoint at $t.\n" +
                                    "Curves must map 0.0 to near zero and 1.0 to near one but " +
                                    "${activeCurve.runtimeType()} mapped $t to $transformedValue" +
                                    ", which is near $roundedTransformedValue."
                        )
                    }
                    true
                }
                return t
            }
            return activeCurve.transform(t)
        }

    override fun toString(): String {
        if (reverseCurve == null)
            return "$parent\u27A9$curve"
        if (useForwardCurve)
            return "$parent\u27A9$curve\u2092\u2099/$reverseCurve"
        return "$parent\u27A9$curve/$reverseCurve\u2092\u2099"
    }
}