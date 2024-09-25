/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEachIndexed

/**
 * Calculates the default [ThreePaneMotion] of [ListDetailPaneScaffold] according to the given
 * [ThreePaneScaffoldState]'s current and target values.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ThreePaneScaffoldState.calculateListDetailPaneScaffoldMotion(): ThreePaneMotion =
    calculateThreePaneMotion(ListDetailPaneScaffoldDefaults.PaneOrder)

/**
 * Calculates the default [ThreePaneMotion] of [ListDetailPaneScaffold] according to the target and
 * the previously remembered [ThreePaneScaffoldValue].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun calculateListDetailPaneScaffoldMotion(
    targetScaffoldValue: ThreePaneScaffoldValue
): ThreePaneMotion =
    calculateThreePaneMotion(targetScaffoldValue, ListDetailPaneScaffoldDefaults.PaneOrder)

/**
 * Calculates the default [ThreePaneMotion] of [SupportingPaneScaffold] according to the given
 * [ThreePaneScaffoldState]'s current and target values.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ThreePaneScaffoldState.calculateSupportingPaneScaffoldMotion(): ThreePaneMotion =
    calculateThreePaneMotion(SupportingPaneScaffoldDefaults.PaneOrder)

/**
 * Calculates the default [ThreePaneMotion] of [SupportingPaneScaffold] according to the target and
 * the previously remembered [ThreePaneScaffoldValue].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun calculateSupportingPaneScaffoldMotion(
    targetScaffoldValue: ThreePaneScaffoldValue
): ThreePaneMotion =
    calculateThreePaneMotion(targetScaffoldValue, SupportingPaneScaffoldDefaults.PaneOrder)

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun ThreePaneScaffoldState.calculateThreePaneMotion(
    paneOrder: ThreePaneScaffoldHorizontalOrder
): ThreePaneMotion {
    class ThreePaneMotionHolder(var value: ThreePaneMotion)

    val resultHolder = remember { ThreePaneMotionHolder(ThreePaneMotion.NoMotion) }
    if (currentState != targetState) {
        // Only update motions when the state changes to prevent unnecessary recomposition at the
        // end of state transitions.
        val ltrPaneOrder = paneOrder.toLtrOrder(LocalLayoutDirection.current)
        val paneMotions = calculatePaneMotion(currentState, targetState, ltrPaneOrder)
        resultHolder.value =
            ThreePaneMotion(
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Primary)],
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Secondary)],
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Tertiary)]
            )
    }
    return resultHolder.value
}

@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun calculateThreePaneMotion(
    targetScaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder
): ThreePaneMotion {
    class ThreePaneScaffoldValueHolder(var value: ThreePaneScaffoldValue)

    val layoutDirection = LocalLayoutDirection.current
    val ltrPaneOrder =
        remember(paneOrder, layoutDirection) { paneOrder.toLtrOrder(layoutDirection) }
    val previousScaffoldValue = remember { ThreePaneScaffoldValueHolder(targetScaffoldValue) }
    val threePaneMotion =
        remember(targetScaffoldValue, ltrPaneOrder) {
            val previousValue = previousScaffoldValue.value
            previousScaffoldValue.value = targetScaffoldValue
            val paneMotions = calculatePaneMotion(previousValue, targetScaffoldValue, ltrPaneOrder)
            ThreePaneMotion(
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Primary)],
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Secondary)],
                paneMotions[ltrPaneOrder.indexOf(ThreePaneScaffoldRole.Tertiary)]
            )
        }
    return threePaneMotion
}

/**
 * The class that provides motion settings for three pane scaffolds like [ListDetailPaneScaffold]
 * and [SupportingPaneScaffold].
 *
 * @param primaryPaneMotion the specified [PaneMotion] of the primary pane, i.e.,
 *   [ListDetailPaneScaffoldRole.Detail] or [SupportingPaneScaffoldRole.Main].
 * @param secondaryPaneMotion the specified [PaneMotion] of the secondary pane, i.e.,
 *   [ListDetailPaneScaffoldRole.List] or [SupportingPaneScaffoldRole.Supporting].
 * @param tertiaryPaneMotion the specified [PaneMotion] of the tertiary pane, i.e.,
 *   [ListDetailPaneScaffoldRole.Extra] or [SupportingPaneScaffoldRole.Extra].
 * @param animationSpecs the specified [PaneAnimationSpecs] when animating pane motions.
 * @param delayedAnimationSpecs the specified [PaneAnimationSpecs] when animating pane motions with
 *   a delay to emphasize entering panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneMotion(
    internal val primaryPaneMotion: PaneMotion,
    internal val secondaryPaneMotion: PaneMotion,
    internal val tertiaryPaneMotion: PaneMotion,
    val animationSpecs: PaneAnimationSpecs = ThreePaneMotionDefaults.AnimationSpecs,
    val delayedAnimationSpecs: PaneAnimationSpecs = ThreePaneMotionDefaults.AnimationSpecsDelayed
) {
    /**
     * Makes a copy of [ThreePaneMotion] with override values.
     *
     * @param primaryPaneMotion the specified [PaneMotion] of the primary pane, i.e.,
     *   [ListDetailPaneScaffoldRole.Detail] or [SupportingPaneScaffoldRole.Main].
     * @param secondaryPaneMotion the specified [PaneMotion] of the secondary pane, i.e.,
     *   [ListDetailPaneScaffoldRole.List] or [SupportingPaneScaffoldRole.Supporting].
     * @param tertiaryPaneMotion the specified [PaneMotion] of the tertiary pane, i.e.,
     *   [ListDetailPaneScaffoldRole.Extra] or [SupportingPaneScaffoldRole.Extra].
     * @param animationSpecs the specified [PaneAnimationSpecs] when animating pane motions.
     * @param delayedAnimationSpecs the specified [PaneAnimationSpecs] when animating pane motions
     *   with a delay to emphasize entering panes.
     */
    fun copy(
        primaryPaneMotion: PaneMotion = this.primaryPaneMotion,
        secondaryPaneMotion: PaneMotion = this.secondaryPaneMotion,
        tertiaryPaneMotion: PaneMotion = this.tertiaryPaneMotion,
        animationSpecs: PaneAnimationSpecs = this.animationSpecs,
        delayedAnimationSpecs: PaneAnimationSpecs = this.delayedAnimationSpecs
    ): ThreePaneMotion =
        ThreePaneMotion(
            primaryPaneMotion,
            secondaryPaneMotion,
            tertiaryPaneMotion,
            animationSpecs,
            delayedAnimationSpecs
        )

    /**
     * Gets the specified [PaneMotion] of a given pane role.
     *
     * @param role the specified role of the pane, see [ListDetailPaneScaffoldRole] and
     *   [SupportingPaneScaffoldRole].
     */
    operator fun get(role: ThreePaneScaffoldRole): PaneMotion =
        when (role) {
            ThreePaneScaffoldRole.Primary -> primaryPaneMotion
            ThreePaneScaffoldRole.Secondary -> secondaryPaneMotion
            ThreePaneScaffoldRole.Tertiary -> tertiaryPaneMotion
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneMotion) return false
        if (primaryPaneMotion != other.primaryPaneMotion) return false
        if (secondaryPaneMotion != other.secondaryPaneMotion) return false
        if (tertiaryPaneMotion != other.tertiaryPaneMotion) return false
        if (animationSpecs != other.animationSpecs) return false
        if (delayedAnimationSpecs != other.delayedAnimationSpecs) return false
        return true
    }

    override fun hashCode(): Int {
        var result = primaryPaneMotion.hashCode()
        result = 31 * result + secondaryPaneMotion.hashCode()
        result = 31 * result + tertiaryPaneMotion.hashCode()
        result = 31 * result + animationSpecs.hashCode()
        result = 31 * result + delayedAnimationSpecs.hashCode()
        return result
    }

    override fun toString(): String {
        return "ThreePaneMotion(" +
            "primaryPaneMotion=$primaryPaneMotion, " +
            "secondaryPaneMotion=$secondaryPaneMotion, " +
            "tertiaryPaneMotion=$tertiaryPaneMotion, " +
            "animationSpecs=$animationSpecs, " +
            "delayedAnimationSpecs=$delayedAnimationSpecs)"
    }

    companion object {
        /** A default [ThreePaneMotion] instance that specifies no motions. */
        val NoMotion =
            ThreePaneMotion(PaneMotion.NoMotion, PaneMotion.NoMotion, PaneMotion.NoMotion)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Suppress("PrimitiveInCollection") // No way to get underlying Long of IntSize or IntOffset
internal class ThreePaneScaffoldMotionScopeImpl : PaneScaffoldMotionScope {
    private lateinit var threePaneMotion: ThreePaneMotion

    override val animationSpecs: PaneAnimationSpecs
        get() = threePaneMotion.animationSpecs

    override val delayedAnimationSpecs: PaneAnimationSpecs
        get() = threePaneMotion.delayedAnimationSpecs

    override var scaffoldSize: IntSize = IntSize.Zero
    override val paneMotionDataList: List<PaneMotionData> =
        listOf(PaneMotionData(), PaneMotionData(), PaneMotionData())

    internal fun updateThreePaneMotion(
        threePaneMotion: ThreePaneMotion,
        ltrOrder: ThreePaneScaffoldHorizontalOrder
    ) {
        this.paneMotionDataList.fastForEachIndexed { index, it ->
            val role = ltrOrder[index]
            it.motion = threePaneMotion[role]
            it.isOriginSizeAndPositionSet = false
        }
        this.threePaneMotion = threePaneMotion
    }
}

internal class DelayedSpringSpec<T>(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    private val delayedRatio: Float,
    visibilityThreshold: T? = null
) : FiniteAnimationSpec<T> {
    private val originalSpringSpec = spring(dampingRatio, stiffness, visibilityThreshold)

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> =
        DelayedVectorizedSpringSpec(originalSpringSpec.vectorize(converter), delayedRatio)
}

private class DelayedVectorizedSpringSpec<V : AnimationVector>(
    val originalVectorizedSpringSpec: VectorizedFiniteAnimationSpec<V>,
    val delayedRatio: Float,
) : VectorizedFiniteAnimationSpec<V> {
    var delayedTimeNanos: Long = 0
    var cachedInitialValue: V? = null
    var cachedTargetValue: V? = null
    var cachedInitialVelocity: V? = null
    var cachedOriginalDurationNanos: Long = 0

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return if (playTimeNanos <= delayedTimeNanos) {
            initialValue
        } else {
            originalVectorizedSpringSpec.getValueFromNanos(
                playTimeNanos - delayedTimeNanos,
                initialValue,
                targetValue,
                initialVelocity
            )
        }
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ): V {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return if (playTimeNanos <= delayedTimeNanos) {
            initialVelocity
        } else {
            originalVectorizedSpringSpec.getVelocityFromNanos(
                playTimeNanos - delayedTimeNanos,
                initialValue,
                targetValue,
                initialVelocity
            )
        }
    }

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return cachedOriginalDurationNanos + delayedTimeNanos
    }

    private fun updateDelayedTimeNanosIfNeeded(
        initialValue: V,
        targetValue: V,
        initialVelocity: V
    ) {
        if (
            initialValue != cachedInitialValue ||
                targetValue != cachedTargetValue ||
                initialVelocity != cachedInitialVelocity
        ) {
            cachedOriginalDurationNanos =
                originalVectorizedSpringSpec.getDurationNanos(
                    initialValue,
                    targetValue,
                    initialVelocity
                )
            delayedTimeNanos = (cachedOriginalDurationNanos * delayedRatio).toLong()
        }
    }
}

/** The default settings of three pane motions. */
@ExperimentalMaterial3AdaptiveApi
object ThreePaneMotionDefaults {
    private val IntRectVisibilityThreshold = IntRect(1, 1, 1, 1)

    /** The default [FiniteAnimationSpec] of pane animations. */
    val AnimationSpecs: PaneAnimationSpecs =
        PaneAnimationSpecs(
            spring(
                dampingRatio = 0.8f,
                stiffness = 380f,
                visibilityThreshold = IntRectVisibilityThreshold
            )
        )

    /**
     * The default [FiniteAnimationSpec] of pane animations with a delay. It's by default used in
     * the case when an enter pane will intersect with exit panes, we delay the entering animation
     * to emphasize the entering transition.
     */
    val AnimationSpecsDelayed: PaneAnimationSpecs =
        PaneAnimationSpecs(
            DelayedSpringSpec(
                dampingRatio = 0.8f,
                stiffness = 380f,
                delayedRatio = 0.1f,
                visibilityThreshold = IntRectVisibilityThreshold
            )
        )
}
