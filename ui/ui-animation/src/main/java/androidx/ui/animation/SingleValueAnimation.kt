/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.animation

import androidx.animation.AnimationBuilder
import androidx.animation.AnimationEndReason
import androidx.animation.AnimationVector
import androidx.animation.AnimationVector1D
import androidx.animation.AnimationVector2D
import androidx.animation.AnimationVector4D
import androidx.animation.PhysicsBuilder
import androidx.animation.TwoWayConverter
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.AnimationClockAmbient
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.unit.Bounds
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxBounds
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Position
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize

private const val DpVisibilityThreshold = 0.1f
private const val PxVisibilityThreshold = 0.5f

// Dp-based visibility threshold
private val DpVisibilityThreshold1D = AnimationVector1D(DpVisibilityThreshold)
private val DpVisibilityThreshold2D = AnimationVector2D(
    DpVisibilityThreshold,
    DpVisibilityThreshold
)
private val DpVisibilityThreshold4D = AnimationVector4D(
    DpVisibilityThreshold,
    DpVisibilityThreshold,
    DpVisibilityThreshold,
    DpVisibilityThreshold
)

// Px-based visibility threshold
private val PxVisibilityThreshold1D = AnimationVector1D(PxVisibilityThreshold)
private val PxVisibilityThreshold2D = AnimationVector2D(
    PxVisibilityThreshold,
    PxVisibilityThreshold
)
private val PxVisibilityThreshold4D = AnimationVector4D(
    PxVisibilityThreshold,
    PxVisibilityThreshold,
    PxVisibilityThreshold,
    PxVisibilityThreshold
)

/**
 * Fire-and-forget animation [Composable] for [Float]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedFloat].
 *
 * @sample androidx.ui.animation.samples.VisibilityTransition
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold for deciding when the animation value is
 *                            considered close enough to the target.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Float,
    animBuilder: AnimationBuilder<Float> = remember { PhysicsBuilder() },
    visibilityThreshold: Float = 0.01f,
    endListener: ((Float) -> Unit)? = null
): Float {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val anim = remember {
        AnimatedFloatModel(target, clock, visibilityThreshold)
    }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, animBuilder) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, animBuilder)
        }
    }
    return anim.value
}

/**
 * Fire-and-forget animation [Composable] for [Color]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedColor].
 *
 * @sample androidx.ui.animation.samples.ColorTransition
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Color,
    animBuilder: AnimationBuilder<Color> = remember { PhysicsBuilder() },
    endListener: ((Color) -> Unit)? = null
): Color {
    val converter = remember(target.colorSpace) { ColorToVectorConverter(target.colorSpace) }
    return animate(target, converter, animBuilder, endListener = endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Dp]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *     val height : Dp = animate(if (collapsed) 10.dp else 20.dp)
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Dp,
    animBuilder: AnimationBuilder<Dp> = remember { PhysicsBuilder() },
    endListener: ((Dp) -> Unit)? = null
): Dp {
    return animate(target, DpToVectorConverter, animBuilder, DpVisibilityThreshold1D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Position]. Once such an animation is created, it will
 * be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *     val position : Position = animate(
 *         if (selected) Position(0.dp, 0.dp) else Position(20.dp, 20.dp))
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Position,
    animBuilder: AnimationBuilder<Position> = remember { PhysicsBuilder() },
    endListener: ((Position) -> Unit)? = null
): Position {
    return animate(
        target, PositionToVectorConverter, animBuilder, DpVisibilityThreshold2D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Size]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *     val size : Size = animate(
 *         if (selected) Size(20f, 20f) else Size(10f, 10f))
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Size,
    animBuilder: AnimationBuilder<Size> = remember { PhysicsBuilder() },
    endListener: ((Size) -> Unit)? = null
): Size {
    return animate(target, SizeToVectorConverter, animBuilder, DpVisibilityThreshold2D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Bounds]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val bounds : Bounds = animate(
 *        if (collapsed) Bounds(0.dp, 0.dp, 10.dp, 20.dp) else Bounds(0.dp, 0.dp, 100.dp, 200.dp))
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Bounds,
    animBuilder: AnimationBuilder<Bounds> = remember { PhysicsBuilder() },
    endListener: ((Bounds) -> Unit)? = null
): Bounds {
    return animate(
        target,
        BoundsToVectorConverter,
        animBuilder,
        DpVisibilityThreshold4D,
        endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for pixels. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val height : Float = animate(if (collapsed) 10f else 20f)
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Float,
    animBuilder: AnimationBuilder<Float> = remember { PhysicsBuilder() },
    endListener: ((Float) -> Unit)? = null
): Float {
    return animate(target, PxToVectorConverter, animBuilder, PxVisibilityThreshold1D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [PxSize]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *     val size : PxSize = animate(if (selected) PxSize(20.px, 20.px) else PxSize(10.px, 10.px))
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: PxSize,
    animBuilder: AnimationBuilder<PxSize> = remember { PhysicsBuilder() },
    endListener: ((PxSize) -> Unit)? = null
): PxSize {
    return animate(
        target, PxSizeToVectorConverter, animBuilder, PxVisibilityThreshold2D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [PxPosition]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val position : PxPosition = animate(
 *        if (selected) PxPosition(0.px, 0.px) else PxPosition(20.px, 20.px))
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: PxPosition,
    animBuilder: AnimationBuilder<PxPosition> = remember { PhysicsBuilder() },
    endListener: ((PxPosition) -> Unit)? = null
): PxPosition {
    return animate(
        target, PxPositionToVectorConverter, animBuilder, PxVisibilityThreshold2D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [PxBounds]. Once such an animation is created, it will
 * be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val bounds : PxBounds = animate(
 *        if (enabled) PxBounds(0.px, 0.px, 100.px, 100.px) else PxBounds(8.px, 8.px, 80.px, 80.px))
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: PxBounds,
    animBuilder: AnimationBuilder<PxBounds> = remember { PhysicsBuilder() },
    endListener: ((PxBounds) -> Unit)? = null
): PxBounds {
    return animate(
        target, PxBoundsToVectorConverter, animBuilder, PxVisibilityThreshold4D,
        endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntPx]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: IntPx,
    animBuilder: AnimationBuilder<IntPx> = remember { PhysicsBuilder() },
    endListener: ((IntPx) -> Unit)? = null
): IntPx {
    return animate(
        target, IntPxToVectorConverter, animBuilder, PxVisibilityThreshold1D,
        endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntPxPosition]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: IntPxPosition,
    animBuilder: AnimationBuilder<IntPxPosition> = remember { PhysicsBuilder() },
    endListener: ((IntPxPosition) -> Unit)? = null
): IntPxPosition {
    return animate(
        target, IntPxPositionToVectorConverter, animBuilder, PxVisibilityThreshold2D,
        endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntPxSize]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: IntPxSize,
    animBuilder: AnimationBuilder<IntPxSize> = remember { PhysicsBuilder() },
    endListener: ((IntPxSize) -> Unit)? = null
): IntPxSize {
    return animate(
        target, IntPxSizeToVectorConverter, animBuilder, PxVisibilityThreshold2D,
        endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntPxBounds]. Once such an animation is created,
 * it will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter
 * the course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: IntPxBounds,
    animBuilder: AnimationBuilder<IntPxBounds> = remember { PhysicsBuilder() },
    endListener: ((IntPxBounds) -> Unit)? = null
): IntPxBounds {
    return animate(
        target, IntPxBoundsToVectorConverter, animBuilder, PxVisibilityThreshold4D, endListener)
}

/**
 * Fire-and-forget animation [Composable] for [AnimationVector]. Once such an animation is created,
 * it will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter
 * the course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the target to end the animation.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun <T : AnimationVector> animate(
    target: T,
    animBuilder: AnimationBuilder<T> = remember { PhysicsBuilder() },
    visibilityThreshold: T? = null,
    endListener: ((T) -> Unit)? = null
): T {
    return animate(
        target,
        remember { TwoWayConverter<T, T>({ it }, { it }) },
        animBuilder,
        visibilityThreshold,
        endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for any value. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter
 * the course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.ui.animation.samples.ArbitraryValueTypeTransition
 *
 * @param target Target value of the animation
 * @param animBuilder The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the target to end the animation.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun <T, V : AnimationVector> animate(
    target: T,
    converter: TwoWayConverter<T, V>,
    animBuilder: AnimationBuilder<T> = remember { PhysicsBuilder() },
    visibilityThreshold: V? = null,
    endListener: ((T) -> Unit)? = null
): T {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val anim = remember(clock, converter) {
        AnimatedValueModel(target, converter, clock, visibilityThreshold)
    }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, animBuilder) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, animBuilder)
        }
    }
    return anim.value
}