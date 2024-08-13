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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.jvm.JvmStatic

/** Holds the transitions that can be applied to the different panes. */
@ExperimentalMaterial3AdaptiveApi
@Immutable
internal open class ThreePaneMotion
internal constructor(
    internal val positionAnimationSpec: FiniteAnimationSpec<IntOffset> = snap(),
    internal val sizeAnimationSpec: FiniteAnimationSpec<IntSize> = snap(),
    private val firstPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val firstPaneExitTransition: ExitTransition = ExitTransition.None,
    private val secondPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val secondPaneExitTransition: ExitTransition = ExitTransition.None,
    private val thirdPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val thirdPaneExitTransition: ExitTransition = ExitTransition.None
) {

    /**
     * Resolves and returns the [EnterTransition] for the given [ThreePaneScaffoldRole] at the given
     * [ThreePaneScaffoldHorizontalOrder].
     */
    fun enterTransition(
        role: ThreePaneScaffoldRole,
        paneOrder: ThreePaneScaffoldHorizontalOrder
    ): EnterTransition {
        // Quick return in case this instance is the NoMotion one.
        if (this === NoMotion) return EnterTransition.None

        return when (paneOrder.indexOf(role)) {
            0 -> firstPaneEnterTransition
            1 -> secondPaneEnterTransition
            else -> thirdPaneEnterTransition
        }
    }

    /**
     * Resolves and returns the [ExitTransition] for the given [ThreePaneScaffoldRole] at the given
     * [ThreePaneScaffoldHorizontalOrder].
     */
    fun exitTransition(
        role: ThreePaneScaffoldRole,
        paneOrder: ThreePaneScaffoldHorizontalOrder
    ): ExitTransition {
        // Quick return in case this instance is the NoMotion one.
        if (this === NoMotion) return ExitTransition.None

        return when (paneOrder.indexOf(role)) {
            0 -> firstPaneExitTransition
            1 -> secondPaneExitTransition
            else -> thirdPaneExitTransition
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneMotion) return false
        if (this.positionAnimationSpec != other.positionAnimationSpec) return false
        if (this.sizeAnimationSpec != other.sizeAnimationSpec) return false
        if (this.firstPaneEnterTransition != other.firstPaneEnterTransition) return false
        if (this.firstPaneExitTransition != other.firstPaneExitTransition) return false
        if (this.secondPaneEnterTransition != other.secondPaneEnterTransition) return false
        if (this.secondPaneExitTransition != other.secondPaneExitTransition) return false
        if (this.thirdPaneEnterTransition != other.thirdPaneEnterTransition) return false
        if (this.thirdPaneExitTransition != other.thirdPaneExitTransition) return false
        return true
    }

    override fun hashCode(): Int {
        var result = positionAnimationSpec.hashCode()
        result = 31 * result + sizeAnimationSpec.hashCode()
        result = 31 * result + firstPaneEnterTransition.hashCode()
        result = 31 * result + firstPaneExitTransition.hashCode()
        result = 31 * result + secondPaneEnterTransition.hashCode()
        result = 31 * result + secondPaneExitTransition.hashCode()
        result = 31 * result + thirdPaneEnterTransition.hashCode()
        result = 31 * result + thirdPaneExitTransition.hashCode()
        return result
    }

    companion object {
        /**
         * A ThreePaneMotion with all transitions set to [EnterTransition.None] and
         * [ExitTransition.None].
         */
        val NoMotion = ThreePaneMotion()

        @JvmStatic
        protected fun slideInFromLeft(spacerSize: Int) =
            slideInHorizontally(ThreePaneMotionDefaults.PanePositionAnimationSpec) {
                -it - spacerSize
            }

        @JvmStatic
        protected fun slideInFromLeftDelayed(spacerSize: Int) =
            slideInHorizontally(ThreePaneMotionDefaults.PanePositionAnimationSpecDelayed) {
                -it - spacerSize
            }

        @JvmStatic
        protected fun slideInFromRight(spacerSize: Int) =
            slideInHorizontally(ThreePaneMotionDefaults.PanePositionAnimationSpec) {
                it + spacerSize
            }

        @JvmStatic
        protected fun slideInFromRightDelayed(spacerSize: Int) =
            slideInHorizontally(ThreePaneMotionDefaults.PanePositionAnimationSpecDelayed) {
                it + spacerSize
            }

        @JvmStatic
        protected fun slideOutToLeft(spacerSize: Int) =
            slideOutHorizontally(ThreePaneMotionDefaults.PanePositionAnimationSpec) {
                -it - spacerSize
            }

        @JvmStatic
        protected fun slideOutToRight(spacerSize: Int) =
            slideOutHorizontally(ThreePaneMotionDefaults.PanePositionAnimationSpec) {
                it + spacerSize
            }
    }
}

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class MovePanesToLeftMotion(private val spacerSize: Int) :
    ThreePaneMotion(
        ThreePaneMotionDefaults.PanePositionAnimationSpec,
        ThreePaneMotionDefaults.PaneSizeAnimationSpec,
        slideInFromRight(spacerSize),
        slideOutToLeft(spacerSize),
        slideInFromRight(spacerSize),
        slideOutToLeft(spacerSize),
        slideInFromRight(spacerSize),
        slideOutToLeft(spacerSize)
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MovePanesToLeftMotion) return false
        if (this.spacerSize != other.spacerSize) return false
        return true
    }

    override fun hashCode(): Int {
        return spacerSize
    }
}

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class MovePanesToRightMotion(private val spacerSize: Int) :
    ThreePaneMotion(
        ThreePaneMotionDefaults.PanePositionAnimationSpec,
        ThreePaneMotionDefaults.PaneSizeAnimationSpec,
        slideInFromLeft(spacerSize),
        slideOutToRight(spacerSize),
        slideInFromLeft(spacerSize),
        slideOutToRight(spacerSize),
        slideInFromLeft(spacerSize),
        slideOutToRight(spacerSize)
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MovePanesToRightMotion) return false
        if (this.spacerSize != other.spacerSize) return false
        return true
    }

    override fun hashCode(): Int {
        return spacerSize
    }
}

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class SwitchLeftTwoPanesMotion(private val spacerSize: Int) :
    ThreePaneMotion(
        ThreePaneMotionDefaults.PanePositionAnimationSpec,
        ThreePaneMotionDefaults.PaneSizeAnimationSpec,
        slideInFromLeftDelayed(spacerSize),
        slideOutToLeft(spacerSize),
        slideInFromLeftDelayed(spacerSize),
        slideOutToLeft(spacerSize),
        EnterTransition.None,
        ExitTransition.None
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwitchLeftTwoPanesMotion) return false
        if (this.spacerSize != other.spacerSize) return false
        return true
    }

    override fun hashCode(): Int {
        return spacerSize
    }
}

@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class SwitchRightTwoPanesMotion(private val spacerSize: Int) :
    ThreePaneMotion(
        ThreePaneMotionDefaults.PanePositionAnimationSpec,
        ThreePaneMotionDefaults.PaneSizeAnimationSpec,
        EnterTransition.None,
        ExitTransition.None,
        slideInFromRightDelayed(spacerSize),
        slideOutToRight(spacerSize),
        slideInFromRightDelayed(spacerSize),
        slideOutToRight(spacerSize)
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwitchRightTwoPanesMotion) return false
        if (this.spacerSize != other.spacerSize) return false
        return true
    }

    override fun hashCode(): Int {
        return spacerSize
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun calculateThreePaneMotion(
    previousScaffoldValue: ThreePaneScaffoldValue,
    currentScaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    spacerSize: Int
): ThreePaneMotion {
    if (previousScaffoldValue.equals(currentScaffoldValue)) {
        return ThreePaneMotion.NoMotion
    }
    val previousExpandedCount = previousScaffoldValue.expandedCount
    val currentExpandedCount = currentScaffoldValue.expandedCount
    if (previousExpandedCount != currentExpandedCount) {
        // TODO(conradchen): Address this case
        return ThreePaneMotion.NoMotion
    }
    return when (previousExpandedCount) {
        1 ->
            when (PaneAdaptedValue.Expanded) {
                previousScaffoldValue[paneOrder.firstPane] -> {
                    MovePanesToLeftMotion(spacerSize)
                }
                previousScaffoldValue[paneOrder.thirdPane] -> {
                    MovePanesToRightMotion(spacerSize)
                }
                currentScaffoldValue[paneOrder.thirdPane] -> {
                    MovePanesToLeftMotion(spacerSize)
                }
                else -> {
                    MovePanesToRightMotion(spacerSize)
                }
            }
        2 ->
            when {
                previousScaffoldValue[paneOrder.firstPane] == PaneAdaptedValue.Expanded &&
                    currentScaffoldValue[paneOrder.firstPane] == PaneAdaptedValue.Expanded -> {
                    // The first pane stays, the right two panes switch
                    SwitchRightTwoPanesMotion(spacerSize)
                }
                previousScaffoldValue[paneOrder.thirdPane] == PaneAdaptedValue.Expanded &&
                    currentScaffoldValue[paneOrder.thirdPane] == PaneAdaptedValue.Expanded -> {
                    // The third pane stays, the left two panes switch
                    SwitchLeftTwoPanesMotion(spacerSize)
                }

                // Implies the second pane stays hereafter
                currentScaffoldValue[paneOrder.thirdPane] == PaneAdaptedValue.Expanded -> {
                    // The third pane shows, all panes move left
                    MovePanesToLeftMotion(spacerSize)
                }
                else -> {
                    // The first pane shows, all panes move right
                    MovePanesToRightMotion(spacerSize)
                }
            }
        else -> {
            // Should not happen
            ThreePaneMotion.NoMotion
        }
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

@ExperimentalMaterial3AdaptiveApi
internal object ThreePaneMotionDefaults {
    // TODO(conradchen): open this to public when we support motion customization
    val PanePositionAnimationSpec: SpringSpec<IntOffset> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 600f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    // TODO(conradchen): open this to public when we support motion customization
    val PanePositionAnimationSpecDelayed: DelayedSpringSpec<IntOffset> =
        DelayedSpringSpec(
            dampingRatio = 0.8f,
            stiffness = 600f,
            delayedRatio = 0.1f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    // TODO(conradchen): open this to public when we support motion customization
    val PaneSizeAnimationSpec: SpringSpec<IntSize> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 600f,
            visibilityThreshold = IntSize.VisibilityThreshold
        )
}
