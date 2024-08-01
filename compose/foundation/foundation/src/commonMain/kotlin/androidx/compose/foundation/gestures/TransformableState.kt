/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope

/**
 * State of [transformable]. Allows for a granular control of how different gesture transformations
 * are consumed by the user as well as to write custom transformation methods using [transform]
 * suspend function.
 */
@JvmDefaultWithCompatibility
interface TransformableState {
    /**
     * Call this function to take control of transformations and gain the ability to send transform
     * events via [TransformScope.transformBy]. All actions that change zoom, pan or rotation values
     * must be performed within a [transform] block (even if they don't call any other methods on
     * this object) in order to guarantee that mutual exclusion is enforced.
     *
     * If [transform] is called from elsewhere with the [transformPriority] higher or equal to
     * ongoing transform, ongoing transform will be canceled.
     */
    suspend fun transform(
        transformPriority: MutatePriority = MutatePriority.Default,
        block: suspend TransformScope.() -> Unit
    )

    /**
     * Whether this [TransformableState] is currently transforming by gesture or programmatically or
     * not.
     */
    val isTransformInProgress: Boolean
}

/** Scope used for suspending transformation operations */
@JvmDefaultWithCompatibility
interface TransformScope {
    /**
     * Attempts to transform by [zoomChange] in relative multiplied value, by [panChange] in pixels
     * and by [rotationChange] in degrees.
     *
     * @param zoomChange scale factor multiplier change for zoom
     * @param panChange panning offset change, in [Offset] pixels
     * @param rotationChange change of the rotation in degrees
     */
    fun transformBy(
        zoomChange: Float = 1f,
        panChange: Offset = Offset.Zero,
        rotationChange: Float = 0f
    )
}

/**
 * Default implementation of [TransformableState] interface that contains necessary information
 * about the ongoing transformations and provides smooth transformation capabilities.
 *
 * This is the simplest way to set up a [transformable] modifier. When constructing this
 * [TransformableState], you must provide a [onTransformation] lambda, which will be invoked
 * whenever pan, zoom or rotation happens (by gesture input or any [TransformableState.transform]
 * call) with the deltas from the previous event.
 *
 * @param onTransformation callback invoked when transformation occurs. The callback receives the
 *   change from the previous event. It's relative scale multiplier for zoom, [Offset] in pixels for
 *   pan and degrees for rotation. Callers should update their state in this lambda.
 */
fun TransformableState(
    onTransformation: (zoomChange: Float, panChange: Offset, rotationChange: Float) -> Unit
): TransformableState = DefaultTransformableState(onTransformation)

/**
 * Create and remember default implementation of [TransformableState] interface that contains
 * necessary information about the ongoing transformations and provides smooth transformation
 * capabilities.
 *
 * This is the simplest way to set up a [transformable] modifier. When constructing this
 * [TransformableState], you must provide a [onTransformation] lambda, which will be invoked
 * whenever pan, zoom or rotation happens (by gesture input or any [TransformableState.transform]
 * call) with the deltas from the previous event.
 *
 * @param onTransformation callback invoked when transformation occurs. The callback receives the
 *   change from the previous event. It's relative scale multiplier for zoom, [Offset] in pixels for
 *   pan and degrees for rotation. Callers should update their state in this lambda.
 */
@Composable
fun rememberTransformableState(
    onTransformation: (zoomChange: Float, panChange: Offset, rotationChange: Float) -> Unit
): TransformableState {
    val lambdaState = rememberUpdatedState(onTransformation)
    return remember { TransformableState { z, p, r -> lambdaState.value.invoke(z, p, r) } }
}

/**
 * Animate zoom by a ratio of [zoomFactor] over the current size and suspend until its finished.
 *
 * @param zoomFactor ratio over the current size by which to zoom. For example, if [zoomFactor] is
 *   `3f`, zoom will be increased 3 fold from the current value.
 * @param animationSpec [AnimationSpec] to be used for animation
 */
suspend fun TransformableState.animateZoomBy(
    zoomFactor: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    requirePrecondition(zoomFactor > 0) { "zoom value should be greater than 0" }
    var previous = 1f
    transform {
        AnimationState(initialValue = previous).animateTo(zoomFactor, animationSpec) {
            val scaleFactor = if (previous == 0f) 1f else this.value / previous
            transformBy(zoomChange = scaleFactor)
            previous = this.value
        }
    }
}

/**
 * Animate rotate by a ratio of [degrees] clockwise and suspend until its finished.
 *
 * @param degrees the degrees by which to rotate clockwise
 * @param animationSpec [AnimationSpec] to be used for animation
 */
suspend fun TransformableState.animateRotateBy(
    degrees: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    var previous = 0f
    transform {
        AnimationState(initialValue = previous).animateTo(degrees, animationSpec) {
            val delta = this.value - previous
            transformBy(rotationChange = delta)
            previous = this.value
        }
    }
}

/**
 * Animate pan by [offset] Offset in pixels and suspend until its finished
 *
 * @param offset offset to pan, in pixels
 * @param animationSpec [AnimationSpec] to be used for pan animation
 */
suspend fun TransformableState.animatePanBy(
    offset: Offset,
    animationSpec: AnimationSpec<Offset> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    var previous = Offset.Zero
    transform {
        AnimationState(typeConverter = Offset.VectorConverter, initialValue = previous).animateTo(
            offset,
            animationSpec
        ) {
            val delta = this.value - previous
            transformBy(panChange = delta)
            previous = this.value
        }
    }
}

/**
 * Animate zoom, pan, and rotation simultaneously and suspend until the animation is finished.
 *
 * Zoom is animated by a ratio of [zoomFactor] over the current size. Pan is animated by [panOffset]
 * in pixels. Rotation is animated by the value of [rotationDegrees] clockwise. Any of these
 * parameters can be set to a no-op value that will result in no animation of that parameter. The
 * no-op values are the following: `1f` for [zoomFactor], `Offset.Zero` for [panOffset], and `0f`
 * for [rotationDegrees].
 *
 * @sample androidx.compose.foundation.samples.TransformableAnimateBySample
 * @param zoomFactor ratio over the current size by which to zoom. For example, if [zoomFactor] is
 *   `3f`, zoom will be increased 3 fold from the current value.
 * @param panOffset offset to pan, in pixels
 * @param rotationDegrees the degrees by which to rotate clockwise
 * @param zoomAnimationSpec [AnimationSpec] to be used for animating zoom
 * @param panAnimationSpec [AnimationSpec] to be used for animating offset
 * @param rotationAnimationSpec [AnimationSpec] to be used for animating rotation
 */
@ExperimentalFoundationApi
suspend fun TransformableState.animateBy(
    zoomFactor: Float,
    panOffset: Offset,
    rotationDegrees: Float,
    zoomAnimationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow),
    panAnimationSpec: AnimationSpec<Offset> = SpringSpec(stiffness = Spring.StiffnessLow),
    rotationAnimationSpec: AnimationSpec<Float> = SpringSpec(stiffness = Spring.StiffnessLow)
) {
    requirePrecondition(zoomFactor > 0) { "zoom value should be greater than 0" }
    var previousState = AnimationData(zoom = 1f, offset = Offset.Zero, degrees = 0f)
    val targetState = AnimationData(zoomFactor, panOffset, rotationDegrees)
    val animationSpec =
        DelegatingAnimationSpec(zoomAnimationSpec, panAnimationSpec, rotationAnimationSpec)
    transform {
        AnimationState(
                typeConverter = AnimationDataConverter,
                initialValue = previousState,
                initialVelocity = ZeroAnimationVelocity
            )
            .animateTo(targetState, animationSpec) {
                transformBy(
                    zoomChange =
                        if (previousState.zoom == 0f) 1f else value.zoom / previousState.zoom,
                    rotationChange = value.degrees - previousState.degrees,
                    panChange = value.offset - previousState.offset
                )
                previousState = value
            }
    }
}

private val ZeroAnimationVelocity = AnimationData(zoom = 0f, offset = Offset.Zero, degrees = 0f)

private class DelegatingAnimationSpec(
    private val zoomAnimationSpec: AnimationSpec<Float>,
    private val offsetAnimationSpec: AnimationSpec<Offset>,
    private val rotationAnimationSpec: AnimationSpec<Float>
) : AnimationSpec<AnimationData> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<AnimationData, V>
    ): VectorizedAnimationSpec<V> {
        val vectorizedZoomAnimationSpec = zoomAnimationSpec.vectorize(Float.VectorConverter)
        val vectorizedOffsetAnimationSpec = offsetAnimationSpec.vectorize(Offset.VectorConverter)
        val vectorizedRotationAnimationSpec = rotationAnimationSpec.vectorize(Float.VectorConverter)
        return object : VectorizedFiniteAnimationSpec<V> {
            override fun getDurationNanos(
                initialValue: V,
                targetValue: V,
                initialVelocity: V
            ): Long {
                val initialAnimationData = converter.convertFromVector(initialValue)
                val targetAnimationData = converter.convertFromVector(targetValue)
                val initialVelocityAnimationData = converter.convertFromVector(initialVelocity)

                return maxOf(
                    vectorizedZoomAnimationSpec.getDurationNanos(
                        initialAnimationData.zoomVector(),
                        targetAnimationData.zoomVector(),
                        initialVelocityAnimationData.zoomVector()
                    ),
                    vectorizedOffsetAnimationSpec.getDurationNanos(
                        initialAnimationData.offsetVector(),
                        targetAnimationData.offsetVector(),
                        initialVelocityAnimationData.offsetVector()
                    ),
                    vectorizedRotationAnimationSpec.getDurationNanos(
                        initialAnimationData.degreesVector(),
                        targetAnimationData.degreesVector(),
                        initialVelocityAnimationData.degreesVector()
                    )
                )
            }

            override fun getVelocityFromNanos(
                playTimeNanos: Long,
                initialValue: V,
                targetValue: V,
                initialVelocity: V
            ): V {
                val initialAnimationData = converter.convertFromVector(initialValue)
                val targetAnimationData = converter.convertFromVector(targetValue)
                val initialVelocityAnimationData = converter.convertFromVector(initialVelocity)

                val zoomVelocity =
                    vectorizedZoomAnimationSpec.getVelocityFromNanos(
                        playTimeNanos,
                        initialAnimationData.zoomVector(),
                        targetAnimationData.zoomVector(),
                        initialVelocityAnimationData.zoomVector()
                    )
                val offsetVelocity =
                    vectorizedOffsetAnimationSpec.getVelocityFromNanos(
                        playTimeNanos,
                        initialAnimationData.offsetVector(),
                        targetAnimationData.offsetVector(),
                        initialVelocityAnimationData.offsetVector()
                    )
                val rotationVelocity =
                    vectorizedRotationAnimationSpec.getVelocityFromNanos(
                        playTimeNanos,
                        initialAnimationData.degreesVector(),
                        targetAnimationData.degreesVector(),
                        initialVelocityAnimationData.degreesVector()
                    )

                return packToAnimationVector(zoomVelocity, offsetVelocity, rotationVelocity)
            }

            override fun getValueFromNanos(
                playTimeNanos: Long,
                initialValue: V,
                targetValue: V,
                initialVelocity: V
            ): V {
                val initialAnimationData = converter.convertFromVector(initialValue)
                val targetAnimationData = converter.convertFromVector(targetValue)
                val initialVelocityAnimationData = converter.convertFromVector(initialVelocity)

                val zoomValue =
                    vectorizedZoomAnimationSpec.getValueFromNanos(
                        playTimeNanos,
                        initialAnimationData.zoomVector(),
                        targetAnimationData.zoomVector(),
                        initialVelocityAnimationData.zoomVector()
                    )
                val offsetValue =
                    vectorizedOffsetAnimationSpec.getValueFromNanos(
                        playTimeNanos,
                        initialAnimationData.offsetVector(),
                        targetAnimationData.offsetVector(),
                        initialVelocityAnimationData.offsetVector()
                    )
                val rotationValue =
                    vectorizedRotationAnimationSpec.getValueFromNanos(
                        playTimeNanos,
                        initialAnimationData.degreesVector(),
                        targetAnimationData.degreesVector(),
                        initialVelocityAnimationData.degreesVector()
                    )

                return packToAnimationVector(zoomValue, offsetValue, rotationValue)
            }

            private fun AnimationData.zoomVector() =
                Float.VectorConverter.convertToVector(this.zoom)

            private fun AnimationData.offsetVector() =
                Offset.VectorConverter.convertToVector(Offset(this.offset.x, this.offset.y))

            private fun AnimationData.degreesVector() =
                Float.VectorConverter.convertToVector(this.degrees)

            private fun packToAnimationVector(
                zoom: AnimationVector1D,
                offset: AnimationVector2D,
                rotation: AnimationVector1D
            ): V =
                converter.convertToVector(
                    AnimationData(zoom.value, Offset(offset.v1, offset.v2), rotation.value)
                )
        }
    }
}

private object AnimationDataConverter : TwoWayConverter<AnimationData, AnimationVector4D> {
    override val convertToVector: (AnimationData) -> AnimationVector4D
        get() = { AnimationVector4D(it.zoom, it.offset.x, it.offset.y, it.degrees) }

    override val convertFromVector: (AnimationVector4D) -> AnimationData
        get() = { AnimationData(zoom = it.v1, offset = Offset(it.v2, it.v3), degrees = it.v4) }
}

private data class AnimationData(val zoom: Float, val offset: Offset, val degrees: Float)

/**
 * Zoom without animation by a ratio of [zoomFactor] over the current size and suspend until it's
 * set.
 *
 * @param zoomFactor ratio over the current size by which to zoom
 */
suspend fun TransformableState.zoomBy(zoomFactor: Float) = transform {
    transformBy(zoomFactor, Offset.Zero, 0f)
}

/**
 * Rotate without animation by a [degrees] degrees and suspend until it's set.
 *
 * @param degrees degrees by which to rotate
 */
suspend fun TransformableState.rotateBy(degrees: Float) = transform {
    transformBy(1f, Offset.Zero, degrees)
}

/**
 * Pan without animation by a [offset] Offset in pixels and suspend until it's set.
 *
 * @param offset offset in pixels by which to pan
 */
suspend fun TransformableState.panBy(offset: Offset) = transform { transformBy(1f, offset, 0f) }

/**
 * Stop and suspend until any ongoing [TransformableState.transform] with priority
 * [terminationPriority] or lower is terminated.
 *
 * @param terminationPriority transformation that runs with this priority or lower will be stopped
 */
suspend fun TransformableState.stopTransformation(
    terminationPriority: MutatePriority = MutatePriority.Default
) {
    this.transform(terminationPriority) {
        // do nothing, just lock the mutex so other scroll actors are cancelled
    }
}

private class DefaultTransformableState(
    val onTransformation: (zoomChange: Float, panChange: Offset, rotationChange: Float) -> Unit
) : TransformableState {

    private val transformScope: TransformScope =
        object : TransformScope {
            override fun transformBy(zoomChange: Float, panChange: Offset, rotationChange: Float) =
                onTransformation(zoomChange, panChange, rotationChange)
        }

    private val transformMutex = MutatorMutex()

    private val isTransformingState = mutableStateOf(false)

    override suspend fun transform(
        transformPriority: MutatePriority,
        block: suspend TransformScope.() -> Unit
    ): Unit = coroutineScope {
        transformMutex.mutateWith(transformScope, transformPriority) {
            isTransformingState.value = true
            try {
                block()
            } finally {
                isTransformingState.value = false
            }
        }
    }

    override val isTransformInProgress: Boolean
        get() = isTransformingState.value
}
