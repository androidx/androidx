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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation
import kotlin.math.sqrt

/** Specification for animations in Remote Compose. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed interface RemoteAnimationSpec {
    /**
     * Creates an animated [RemoteFloat] based on the provided [targetValue] and animation
     * parameters.
     *
     * @param targetValue The target value to animate towards.
     * @param initialValue Optional initial value if animating from a start value, or `null`.
     * @param wrap Optional wrap modulo bound (e.g. 360f for angles), or `null`.
     * @return An animated [RemoteFloat].
     */
    public fun animate(
        targetValue: RemoteFloat,
        initialValue: Float? = null,
        wrap: Float? = null,
    ): RemoteFloat
}

/** Defines the easing curve used for Remote Compose animations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteEasing
internal constructor(internal val type: Int, internal val spec: FloatArray? = null) {
    public companion object {
        public val Standard: RemoteEasing = RemoteEasing(CUBIC_STANDARD)
        public val Linear: RemoteEasing = RemoteEasing(CUBIC_LINEAR)
        public val Accelerate: RemoteEasing = RemoteEasing(CUBIC_ACCELERATE)
        public val Decelerate: RemoteEasing = RemoteEasing(CUBIC_DECELERATE)
        public val Anticipate: RemoteEasing = RemoteEasing(CUBIC_ANTICIPATE)
        public val Overshoot: RemoteEasing = RemoteEasing(CUBIC_OVERSHOOT)
        public val Bounce: RemoteEasing = RemoteEasing(EASE_OUT_BOUNCE)
        public val Elastic: RemoteEasing = RemoteEasing(EASE_OUT_ELASTIC)

        public fun Cubic(x1: Float, y1: Float, x2: Float, y2: Float): RemoteEasing =
            RemoteEasing(CUBIC_CUSTOM, floatArrayOf(x1, y1, x2, y2))

        public fun Spline(points: FloatArray): RemoteEasing =
            RemoteEasing(SPLINE_CUSTOM, points.copyOf())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteEasing) return false
        if (type != other.type) return false
        if (spec != null) {
            if (other.spec == null) return false
            if (!spec.contentEquals(other.spec)) return false
        } else if (other.spec != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + (spec?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String = "RemoteEasing(type=$type, spec=${spec?.contentToString()})"
}

/**
 * Easing-based animation specification with a fixed duration.
 *
 * @property durationMillis The duration of the animation in milliseconds.
 * @property easing The easing curve to apply.
 */
internal class RemoteTweenSpec(
    val durationMillis: Int = 300,
    val easing: RemoteEasing = RemoteEasing.Standard,
) : RemoteAnimationSpec {
    init {
        require(durationMillis >= 0) { "durationMillis must be non-negative: $durationMillis" }
    }

    override fun animate(
        targetValue: RemoteFloat,
        initialValue: Float?,
        wrap: Float?,
    ): RemoteFloat {
        val durationSec = durationMillis / 1000f
        val anim =
            FloatAnimation.packToFloatArray(
                durationSec,
                easing.type,
                easing.spec,
                initialValue ?: Float.NaN,
                wrap ?: Float.NaN,
            )
        return AnimatedRemoteFloat(input = targetValue, anim = anim)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteTweenSpec) return false
        if (durationMillis != other.durationMillis) return false
        if (easing != other.easing) return false
        return true
    }

    override fun hashCode(): Int {
        var result = durationMillis
        result = 31 * result + easing.hashCode()
        return result
    }

    override fun toString(): String =
        "RemoteTweenSpec(durationMillis=$durationMillis, easing=$easing)"
}

/**
 * Physics-based spring animation specification.
 *
 * @property stiffness The stiffness/tension of the spring.
 * @property dampingRatio The damping ratio (1.0 = critically damped, < 1.0 = bouncy).
 * @property stopThreshold The threshold at which the spring is considered at rest.
 * @property boundaryMode Engine boundary mode (0 = standard/no bounds).
 */
internal class RemoteSpringSpec(
    val stiffness: Float = 50f,
    val dampingRatio: Float = 1f,
    val stopThreshold: Float = 0.001f,
    val boundaryMode: Int = 0,
) : RemoteAnimationSpec {
    init {
        require(stiffness > 0) { "stiffness must be greater than 0: $stiffness" }
        require(dampingRatio >= 0) { "dampingRatio must be non-negative: $dampingRatio" }
        require(stopThreshold > 0) { "stopThreshold must be greater than 0: $stopThreshold" }
    }

    override fun animate(
        targetValue: RemoteFloat,
        initialValue: Float?,
        wrap: Float?,
    ): RemoteFloat {
        require(initialValue == null) { "initialValue is not supported for RemoteSpringSpec" }
        require(wrap == null) { "wrap is not supported for RemoteSpringSpec" }
        val dampingCoefficient = 2f * dampingRatio * sqrt(stiffness)
        val encodedBoundaryMode = Float.fromBits(boundaryMode)
        val anim =
            floatArrayOf(
                0f, // duration = 0f explicitly signals SpringStopEngine
                stiffness,
                dampingCoefficient,
                stopThreshold,
                encodedBoundaryMode,
            )
        return AnimatedRemoteFloat(input = targetValue, anim = anim)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteSpringSpec) return false
        if (stiffness != other.stiffness) return false
        if (dampingRatio != other.dampingRatio) return false
        if (stopThreshold != other.stopThreshold) return false
        if (boundaryMode != other.boundaryMode) return false
        return true
    }

    override fun hashCode(): Int {
        var result = stiffness.hashCode()
        result = 31 * result + dampingRatio.hashCode()
        result = 31 * result + stopThreshold.hashCode()
        result = 31 * result + boundaryMode
        return result
    }

    override fun toString(): String =
        "RemoteSpringSpec(stiffness=$stiffness, dampingRatio=$dampingRatio, stopThreshold=$stopThreshold, boundaryMode=$boundaryMode)"
}

/**
 * Creates a [RemoteAnimationSpec] with the given duration in milliseconds and easing curve.
 *
 * @param durationMillis Duration of the animation in milliseconds.
 * @param easing Easing curve to apply.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun remoteTween(
    durationMillis: Int = 300,
    easing: RemoteEasing = RemoteEasing.Standard,
): RemoteAnimationSpec = RemoteTweenSpec(durationMillis = durationMillis, easing = easing)

/**
 * Creates a [RemoteAnimationSpec] for physics-based spring animations.
 *
 * @param stiffness Spring stiffness.
 * @param dampingRatio Damping ratio (1.0 = no bounce, < 1.0 = bouncy).
 * @param stopThreshold Threshold for stopping velocity/displacement.
 * @param boundaryMode Boundary mode for the spring.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun remoteSpring(
    stiffness: Float = 50f,
    dampingRatio: Float = 1f,
    stopThreshold: Float = 0.001f,
    boundaryMode: Int = 0,
): RemoteAnimationSpec =
    RemoteSpringSpec(
        stiffness = stiffness,
        dampingRatio = dampingRatio,
        stopThreshold = stopThreshold,
        boundaryMode = boundaryMode,
    )

/**
 * Returns a [RemoteFloat] that animates towards [targetValue] whenever it changes.
 *
 * @param targetValue The target value to animate towards.
 * @param animationSpec The specification for the animation (e.g. [remoteTween] or [remoteSpring]).
 * @param initialValue Optional initial value if animating from a start value.
 * @param wrap Optional wrap modulo bound (e.g. 360f for angles).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun animateRemoteFloatAsState(
    targetValue: RemoteFloat,
    animationSpec: RemoteAnimationSpec = remoteTween(),
    initialValue: Float? = null,
    wrap: Float? = null,
): RemoteFloat =
    animationSpec.animate(targetValue = targetValue, initialValue = initialValue, wrap = wrap)

/**
 * Returns a [RemoteDp] that animates towards [targetValue] whenever it changes.
 *
 * @param targetValue The target value to animate towards.
 * @param animationSpec The specification for the animation (e.g. [remoteTween] or [remoteSpring]).
 * @param initialValue Optional initial value if animating from a start value.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun animateRemoteDpAsState(
    targetValue: RemoteDp,
    animationSpec: RemoteAnimationSpec = remoteTween(),
    initialValue: Float? = null,
): RemoteDp =
    RemoteDp(
        value =
            animateRemoteFloatAsState(
                targetValue = targetValue.value,
                animationSpec = animationSpec,
                initialValue = initialValue,
            )
    )
