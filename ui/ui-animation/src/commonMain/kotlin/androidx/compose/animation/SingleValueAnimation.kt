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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.IntToVectorConverter
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.AnimationClockAmbient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.unit.Bounds
import androidx.ui.unit.Dp
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import androidx.ui.unit.Position
import androidx.ui.unit.PxBounds
import androidx.ui.unit.dp

internal const val DpVisibilityThreshold = 0.1f
internal const val PxVisibilityThreshold = 0.5f

// Dp-based visibility threshold
private val DpVisibilityThreshold4D = AnimationVector4D(
    DpVisibilityThreshold,
    DpVisibilityThreshold,
    DpVisibilityThreshold,
    DpVisibilityThreshold
)

// Px-based visibility threshold
private val PxVisibilityThreshold4D = AnimationVector4D(
    PxVisibilityThreshold,
    PxVisibilityThreshold,
    PxVisibilityThreshold,
    PxVisibilityThreshold
)

private val defaultAnimation = SpringSpec<Float>()

/**
 * Fire-and-forget animation [Composable] for [Float]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedFloat].
 *
 * @sample androidx.compose.animation.samples.VisibilityTransition
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. [SpringSpec]
 *                 will be used by default.
 * @param visibilityThreshold An optional threshold for deciding when the animation value is
 *                            considered close enough to the target.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Float,
    animSpec: AnimationSpec<Float> = defaultAnimation,
    visibilityThreshold: Float = 0.01f,
    endListener: ((Float) -> Unit)? = null
): Float {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val anim = remember {
        AnimatedFloatModel(target, clock, visibilityThreshold)
    }

    val resolvedAnimSpec =
        if (animSpec == defaultAnimation) {
            remember(visibilityThreshold) { SpringSpec(visibilityThreshold = visibilityThreshold) }
        } else {
            animSpec
        }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, resolvedAnimSpec) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, resolvedAnimSpec)
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
 * @sample androidx.compose.animation.samples.ColorTransition
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Color,
    animSpec: AnimationSpec<Color> = remember { SpringSpec() },
    endListener: ((Color) -> Unit)? = null
): Color {
    val converter = remember(target.colorSpace) { ColorToVectorConverter(target.colorSpace) }
    return animate(target, converter, animSpec, endListener = endListener)
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
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Dp,
    animSpec: AnimationSpec<Dp> = remember {
        SpringSpec(visibilityThreshold = DpVisibilityThreshold.dp)
    },
    endListener: ((Dp) -> Unit)? = null
): Dp {
    return animate(target, DpToVectorConverter, animSpec, endListener = endListener)
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
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Position,
    animSpec: AnimationSpec<Position> = remember {
        SpringSpec(
            visibilityThreshold = Position(DpVisibilityThreshold.dp, DpVisibilityThreshold.dp)
        )
    },
    endListener: ((Position) -> Unit)? = null
): Position {
    return animate(
        target, PositionToVectorConverter, animSpec, endListener = endListener
    )
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
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Size,
    animSpec: AnimationSpec<Size> = remember {
        SpringSpec(visibilityThreshold = Size(PxVisibilityThreshold, PxVisibilityThreshold))
    },
    endListener: ((Size) -> Unit)? = null
): Size {
    return animate(target, SizeToVectorConverter, animSpec, endListener = endListener)
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
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Bounds,
    animSpec: AnimationSpec<Bounds> = remember {
        SpringSpec(
            visibilityThreshold = BoundsToVectorConverter.convertFromVector
                (DpVisibilityThreshold4D)
        )
    },
    endListener: ((Bounds) -> Unit)? = null
): Bounds {
    return animate(
        target,
        BoundsToVectorConverter,
        animSpec,
        endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [Offset]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val position : Offset = animate(
 *        if (selected) Offset(0.px, 0.px) else Offset(20.px, 20.px))
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Offset,
    animSpec: AnimationSpec<Offset> = remember {
        SpringSpec(visibilityThreshold = Offset(PxVisibilityThreshold, PxVisibilityThreshold))
    },
    endListener: ((Offset) -> Unit)? = null
): Offset {
    return animate(
        target, OffsetToVectorConverter, animSpec, endListener = endListener
    )
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
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: PxBounds,
    animSpec: AnimationSpec<PxBounds> = remember {
        SpringSpec(
            visibilityThreshold =
            PxBoundsToVectorConverter.convertFromVector(PxVisibilityThreshold4D)
        )
    },
    endListener: ((PxBounds) -> Unit)? = null
): PxBounds {
    return animate(
        target, PxBoundsToVectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [Int]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: Int,
    animSpec: AnimationSpec<Int> = remember {
        SpringSpec(visibilityThreshold = 1)
    },
    endListener: ((Int) -> Unit)? = null
): Int {
    return animate(
        target, IntToVectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntOffset]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: IntOffset,
    animSpec: AnimationSpec<IntOffset> = remember {
        SpringSpec(visibilityThreshold = IntOffset(1, 1))
    },
    endListener: ((IntOffset) -> Unit)? = null
): IntOffset {
    return animate(
        target, IntPxPositionToVectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntSize]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animate] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animate(
    target: IntSize,
    animSpec: AnimationSpec<IntSize> = remember {
        SpringSpec(visibilityThreshold = IntSize(1, 1))
    },
    endListener: ((IntSize) -> Unit)? = null
): IntSize {
    return animate(
        target, IntSizeToVectorConverter, animSpec, endListener = endListener
    )
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
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the target to end the animation.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun <T : AnimationVector> animate(
    target: T,
    animSpec: AnimationSpec<T> = remember {
        SpringSpec(visibilityThreshold = visibilityThreshold)
    },
    visibilityThreshold: T? = null,
    endListener: ((T) -> Unit)? = null
): T {
    return animate(
        target,
        remember { TwoWayConverter<T, T>({ it }, { it }) },
        animSpec,
        endListener = endListener
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
 * @sample androidx.compose.animation.samples.ArbitraryValueTypeTransition
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the target to end the animation.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun <T, V : AnimationVector> animate(
    target: T,
    converter: TwoWayConverter<T, V>,
    animSpec: AnimationSpec<T> = remember {
        SpringSpec(visibilityThreshold = visibilityThreshold)
    },
    visibilityThreshold: T? = null,
    endListener: ((T) -> Unit)? = null
): T {
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val anim = remember(clock, converter) {
        AnimatedValueModel(target, converter, clock, visibilityThreshold)
    }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, animSpec) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, animSpec)
        }
    }
    return anim.value
}