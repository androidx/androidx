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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlin.math.min

/**
 * Scope for performing pane motions within a pane scaffold. It provides the spec and necessary info
 * to decide a pane's [EnterTransition] and [ExitTransition], as well as how bounds morphing will be
 * performed.
 */
@Suppress("PrimitiveInCollection") // No way to get underlying Long of IntSize or IntOffset
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldMotionScope {
    /**
     * The animation specs of the associated pane to the scope. [AnimatedPane] will use this value
     * to perform pane animations during scaffold state changes.
     */
    val animationSpecs: PaneAnimationSpecs

    /**
     * The delayed animation specs of the associated pane to the scope. [AnimatedPane] will use this
     * value to perform pane animations during scaffold state changes when an animation needs to be
     * played with a delay.
     */
    val delayedAnimationSpecs: PaneAnimationSpecs

    /**
     * The scaffold's current size. Note that the value of the field will only be updated during
     * measurement of the scaffold and before the first measurement the value will be
     * [IntSize.Zero].
     *
     * Note that this field is not backed by snapshot states so it's supposed to be only read
     * proactively by the motion logic "on-the-fly" when the scaffold motion is happening.
     */
    val scaffoldSize: IntSize

    /**
     * [PaneMotionData] of all panes in the scaffold corresponding to the scaffold's current state
     * transition and motion settings, listed in panes' horizontal order.
     *
     * The size of position values of [PaneMotionData] in the list will only be update during
     * measurement of the scaffold and before the first measurement their values will be
     * [IntSize.Zero] or [IntOffset.Zero].
     *
     * Note that the aforementioned fields are not backed by snapshot states so they are supposed to
     * be only read proactively by the motion logic "on-the-fly" when the scaffold motion is
     * happening.
     */
    val paneMotionDataList: List<PaneMotionData>
}

/**
 * This class specifies animation specs to use in pane motions for different type of animations like
 * position, size, or bounds animations.
 *
 * @param boundsAnimationSpec the [FiniteAnimationSpec] used to animate panes' bounds when the
 *   specified pane motion is [PaneMotion.AnimateBounds].
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class PaneAnimationSpecs(
    val boundsAnimationSpec: FiniteAnimationSpec<IntRect>,
) {
    /**
     * The [FiniteAnimationSpec] used to animate panes' positions when the specified pane motion is
     * sliding in or out without size change. The spec will be derived from the provided
     * [boundsAnimationSpec] the using the corresponding top-left coordinates.
     */
    val offsetAnimationSpec: FiniteAnimationSpec<IntOffset> =
        DerivedOffsetAnimationSpec(boundsAnimationSpec)

    /**
     * The [FiniteAnimationSpec] used to animate panes' sizes when the specified pane motion is
     * expanding or shrinking without position change. The spec will be derived from the provided
     * [boundsAnimationSpec] by using the corresponding sizes.
     */
    val sizeAnimationSpec: FiniteAnimationSpec<IntSize> =
        DerivedSizeAnimationSpec(boundsAnimationSpec)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaneAnimationSpecs) return false
        if (boundsAnimationSpec != other.boundsAnimationSpec) return false
        return true
    }

    override fun hashCode(): Int {
        return boundsAnimationSpec.hashCode()
    }

    override fun toString(): String {
        return "PaneAnimationSpecs(boundsAnimationSpec=$boundsAnimationSpec)"
    }
}

/**
 * A class to collect motion-relevant data of a specific pane.
 *
 * @property motion The specified [PaneMotion] of the pane.
 * @property originSize The origin measured size of the pane that it should animate from.
 * @property originPosition The origin placement of the pane that it should animate from, with the
 *   offset relative to the associated pane scaffold's local coordinates.
 * @property targetSize The target measured size of the pane that it should animate to.
 * @property targetPosition The target placement of the pane that it should animate to, with the
 *   offset relative to the associated pane scaffold's local coordinates.
 */
@ExperimentalMaterial3AdaptiveApi
class PaneMotionData internal constructor() {
    var motion: PaneMotion = PaneMotion.NoMotion
        internal set

    var originSize: IntSize = IntSize.Zero
        internal set

    var originPosition: IntOffset = IntOffset.Zero
        internal set

    var targetSize: IntSize = IntSize.Zero
        internal set

    var targetPosition: IntOffset = IntOffset.Zero
        internal set

    internal var isOriginSizeAndPositionSet = false
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneMotionData.targetLeft
    get() = targetPosition.x

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneMotionData.targetRight
    get() = targetPosition.x + targetSize.width

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneMotionData.currentLeft
    get() = originPosition.x

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneMotionData.currentRight
    get() = originPosition.x + originSize.width

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionScope.slideInFromLeftOffset: Int
    get() {
        // The sliding in distance from left will either be:
        // 1. The target offset of the left edge of the pane after all panes that are sliding in
        //    from left, so to account for the spacer size between the sliding panes and other
        //    panes.
        // 2. If no such panes exist, use the right edge of the last pane that is sliding in from
        //    left, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        paneMotionDataList.fastForEachReversed {
            if (
                it.motion == PaneMotion.EnterFromLeft ||
                    it.motion == PaneMotion.EnterFromLeftDelayed
            ) {
                return -(previousPane?.targetLeft ?: it.targetRight)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown ||
                    it.motion.type == PaneMotion.Type.Entering
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionScope.slideInFromRightOffset: Int
    get() {
        // The sliding in distance from right will either be:
        // 1. The target offset of the right edge of the pane before all panes that are sliding in
        //    from right, so to account for the spacer size between the sliding panes and other
        //    panes.
        // 2. If no such panes exist, use the left edge of the first pane that is sliding in from
        //    right, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        paneMotionDataList.fastForEach {
            if (
                it.motion == PaneMotion.EnterFromRight ||
                    it.motion == PaneMotion.EnterFromRightDelayed
            ) {
                return scaffoldSize.width - (previousPane?.targetRight ?: it.targetLeft)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown ||
                    it.motion.type == PaneMotion.Type.Entering
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionScope.slideOutToLeftOffset: Int
    get() {
        // The sliding out distance to left will either be:
        // 1. The current offset of the left edge of the pane after all panes that are sliding out
        //    to left, so to account for the spacer size between the sliding panes and other panes.
        // 2. If no such panes exist, use the right edge of the last pane that is sliding out to
        //    left, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        paneMotionDataList.fastForEachReversed {
            if (it.motion == PaneMotion.ExitToLeft) {
                return -(previousPane?.currentLeft ?: it.currentRight)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown || it.motion.type == PaneMotion.Type.Exiting
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionScope.slideOutToRightOffset: Int
    get() {
        // The sliding out distance to right will either be:
        // 1. The current offset of the right edge of the pane before all panes that are sliding out
        //    to right, so to account for the spacer size between the sliding panes and other panes.
        // 2. If no such panes exist, use the left edge of the first pane that is sliding out to
        //    right, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        paneMotionDataList.fastForEach {
            if (it.motion == PaneMotion.ExitToRight) {
                return scaffoldSize.width - (previousPane?.currentRight ?: it.currentLeft)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown || it.motion.type == PaneMotion.Type.Exiting
            ) {
                previousPane = it
            }
        }
        return 0
    }

/** Interface to specify a custom pane enter/exit motion when a pane's visibility changes. */
@ExperimentalMaterial3AdaptiveApi
interface PaneMotion {
    /** The [EnterTransition] of a pane under the given [PaneScaffoldMotionScope]. */
    val PaneScaffoldMotionScope.enterTransition: EnterTransition

    /** The [ExitTransition] of a pane under the given [PaneScaffoldMotionScope]. */
    val PaneScaffoldMotionScope.exitTransition: ExitTransition

    /** The type of the motion, like exiting, entering, etc. See [Type]. */
    val type: Type

    /**
     * Indicates the current type of pane motion, like if the pane is entering or exiting, or is
     * kept showing or hidden.
     */
    @ExperimentalMaterial3AdaptiveApi
    @JvmInline
    value class Type private constructor(val value: Int) {
        override fun toString(): String {
            return "PaneMotion.Type[${
                when(this) {
                    Hidden -> "Hidden"
                    Exiting -> "Exiting"
                    Entering -> "Entering"
                    Shown -> "Shown"
                    else -> "Unknown value=$value"
                }
            }]"
        }

        companion object {
            /** Indicates the pane is kept hidden during the current motion. */
            val Hidden = Type(0)

            /** Indicates the pane is exiting or hiding during the current motion. */
            val Exiting = Type(1)

            /** Indicates the pane is entering or showing during the current motion. */
            val Entering = Type(2)

            /** Indicates the pane is keeping being shown during the current motion. */
            val Shown = Type(3)

            internal fun calculate(
                previousValue: PaneAdaptedValue,
                currentValue: PaneAdaptedValue
            ): Type {
                val wasShown = if (previousValue == PaneAdaptedValue.Hidden) 0 else 1
                val isShown = if (currentValue == PaneAdaptedValue.Hidden) 0 else 2
                return Type(wasShown or isShown)
            }
        }
    }

    private abstract class DefaultImpl(val name: String, override val type: Type) : PaneMotion {
        override val PaneScaffoldMotionScope.enterTransition
            get() = EnterTransition.None

        override val PaneScaffoldMotionScope.exitTransition
            get() = ExitTransition.None

        override fun toString() = name
    }

    companion object {
        /** The default pane motion that no animation will be performed. */
        val NoMotion: PaneMotion = object : DefaultImpl("NoMotion", Type.Hidden) {}

        /**
         * The default pane motion that will animate panes bounds with the given animation specs
         * during motion. Note that this should only be used when the associated pane is keeping
         * showing during the motion.
         */
        val AnimateBounds: PaneMotion = object : DefaultImpl("AnimateBounds", Type.Shown) {}

        /**
         * The default pane motion that will slide panes in from left. Note that this should only be
         * used when the associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromLeft: PaneMotion =
            object : DefaultImpl("EnterFromLeft", Type.Entering) {
                override val PaneScaffoldMotionScope.enterTransition
                    get() =
                        slideInHorizontally(animationSpecs.offsetAnimationSpec) {
                            slideInFromLeftOffset
                        }
            }

        /**
         * The default pane motion that will slide panes in from right. Note that this should only
         * be used when the associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromRight: PaneMotion =
            object : DefaultImpl("EnterFromRight", Type.Entering) {
                override val PaneScaffoldMotionScope.enterTransition
                    get() =
                        slideInHorizontally(animationSpecs.offsetAnimationSpec) {
                            slideInFromRightOffset
                        }
            }

        /**
         * The default pane motion that will slide panes in from left with a delay, usually to avoid
         * the interference of other exiting panes. Note that this should only be used when the
         * associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromLeftDelayed: PaneMotion =
            object : DefaultImpl("EnterFromLeftDelayed", Type.Entering) {
                override val PaneScaffoldMotionScope.enterTransition
                    get() =
                        slideInHorizontally(delayedAnimationSpecs.offsetAnimationSpec) {
                            slideInFromLeftOffset
                        }
            }

        /**
         * The default pane motion that will slide panes in from right with a delay, usually to
         * avoid the interference of other exiting panes. Note that this should only be used when
         * the associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromRightDelayed: PaneMotion =
            object : DefaultImpl("EnterFromRightDelayed", Type.Entering) {
                override val PaneScaffoldMotionScope.enterTransition
                    get() =
                        slideInHorizontally(delayedAnimationSpecs.offsetAnimationSpec) {
                            slideInFromRightOffset
                        }
            }

        /**
         * The default pane motion that will slide panes out to left. Note that this should only be
         * used when the associated pane is exiting - i.e. becoming hidden from a visible state.
         */
        val ExitToLeft: PaneMotion =
            object : DefaultImpl("ExitToLeft", Type.Exiting) {
                override val PaneScaffoldMotionScope.exitTransition
                    get() =
                        slideOutHorizontally(animationSpecs.offsetAnimationSpec) {
                            slideOutToLeftOffset
                        }
            }

        /**
         * The default pane motion that will slide panes out to right. Note that this should only be
         * used when the associated pane is exiting - i.e. becoming hidden from a visible state.
         */
        val ExitToRight: PaneMotion =
            object : DefaultImpl("ExitToRight", Type.Exiting) {
                override val PaneScaffoldMotionScope.exitTransition
                    get() =
                        slideOutHorizontally(animationSpecs.offsetAnimationSpec) {
                            slideOutToRightOffset
                        }
            }

        /**
         * The default pane motion that will expand panes from a zero size. Note that this should
         * only be used when the associated pane is entering - i.e. becoming visible from a hidden
         * state.
         */
        val EnterWithExpand: PaneMotion =
            object : DefaultImpl("EnterWithExpand", Type.Entering) {
                // TODO(conradchen): Expand with position change
                override val PaneScaffoldMotionScope.enterTransition
                    get() =
                        expandHorizontally(
                            animationSpecs.sizeAnimationSpec,
                            Alignment.CenterHorizontally
                        )
            }

        /**
         * The default pane motion that will shrink panes until it's gone. Note that this should
         * only be used when the associated pane is exiting - i.e. becoming hidden from a visible
         * state.
         */
        val ExitWithShrink: PaneMotion =
            object : DefaultImpl("ExitWithShrink", Type.Exiting) {
                // TODO(conradchen): Shrink with position change
                override val PaneScaffoldMotionScope.exitTransition
                    get() =
                        shrinkHorizontally(
                            animationSpecs.sizeAnimationSpec,
                            Alignment.CenterHorizontally
                        )
            }
    }
}

@ExperimentalMaterial3AdaptiveApi
internal fun <T> calculatePaneMotion(
    previousScaffoldValue: PaneScaffoldValue<T>,
    currentScaffoldValue: PaneScaffoldValue<T>,
    paneOrder: PaneScaffoldHorizontalOrder<T>
): List<PaneMotion> {
    val numOfPanes = paneOrder.size
    val paneMotionTypes = Array(numOfPanes) { PaneMotion.Type.Hidden }
    val paneMotions = MutableList(numOfPanes) { PaneMotion.NoMotion }
    var firstShownPaneIndex = numOfPanes
    var firstEnteringPaneIndex = numOfPanes
    var lastShownPaneIndex = -1
    var lastEnteringPaneIndex = -1
    // First pass, to decide the entering/exiting status of each pane, and collect info for
    // deciding, given a certain pane, if there's a pane on its left or on its right that is
    // entering or keep showing during the transition.
    // Also set up the motions of all panes that keep showing to AnimateBounds.
    paneOrder.forEachIndexed { i, role ->
        paneMotionTypes[i] =
            PaneMotion.Type.calculate(previousScaffoldValue[role], currentScaffoldValue[role])
        when (paneMotionTypes[i]) {
            PaneMotion.Type.Shown -> {
                firstShownPaneIndex = min(firstShownPaneIndex, i)
                lastShownPaneIndex = max(lastShownPaneIndex, i)
                paneMotions[i] = PaneMotion.AnimateBounds
            }
            PaneMotion.Type.Entering -> {
                firstEnteringPaneIndex = min(firstEnteringPaneIndex, i)
                lastEnteringPaneIndex = max(lastEnteringPaneIndex, i)
            }
        }
    }
    // Second pass, to decide the exiting motions of all exiting panes.
    // Also collects info for the next pass to decide the entering motions of entering panes.
    var hasPanesExitToRight = false
    var hasPanesExitToLeft = false
    var firstPaneExitToRightIndex = numOfPanes
    var lastPaneExitToLeftIndex = -1
    paneOrder.forEachIndexed { i, _ ->
        val hasShownPanesOnLeft = firstShownPaneIndex < i
        val hasEnteringPanesOnLeft = firstEnteringPaneIndex < i
        val hasShownPanesOnRight = lastShownPaneIndex > i
        val hasEnteringPanesOnRight = lastEnteringPaneIndex > i
        if (paneMotionTypes[i] == PaneMotion.Type.Exiting) {
            paneMotions[i] =
                if (!hasShownPanesOnRight && !hasEnteringPanesOnRight) {
                    // No panes will interfere the motion on the right, exit to right.
                    hasPanesExitToRight = true
                    firstPaneExitToRightIndex = min(firstPaneExitToRightIndex, i)
                    PaneMotion.ExitToRight
                } else if (!hasShownPanesOnLeft && !hasEnteringPanesOnLeft) {
                    // No panes will interfere the motion on the left, exit to left.
                    hasPanesExitToLeft = true
                    lastPaneExitToLeftIndex = max(lastPaneExitToLeftIndex, i)
                    PaneMotion.ExitToLeft
                } else if (!hasShownPanesOnRight) {
                    // Only showing panes can interfere the motion on the right, exit to right.
                    hasPanesExitToRight = true
                    firstPaneExitToRightIndex = min(firstPaneExitToRightIndex, i)
                    PaneMotion.ExitToRight
                } else if (!hasShownPanesOnLeft) { // Only showing panes on left
                    // Only showing panes can interfere the motion on the left, exit to left.
                    hasPanesExitToLeft = true
                    lastPaneExitToLeftIndex = max(lastPaneExitToLeftIndex, i)
                    PaneMotion.ExitToLeft
                } else {
                    // Both sides has panes that keep being visible during transition, shrink to
                    // exit
                    PaneMotion.ExitWithShrink
                }
        }
    }
    // Third pass, to decide the entering motions of all entering panes.
    paneOrder.forEachIndexed { i, _ ->
        val hasShownPanesOnLeft = firstShownPaneIndex < i
        val hasShownPanesOnRight = lastShownPaneIndex > i
        val hasLeftPaneExitToRight = firstPaneExitToRightIndex < i
        val hasRightPaneExitToLeft = lastPaneExitToLeftIndex > i
        // For a given pane, if there's another pane that keeps showing on its right, or there's
        // a pane on its right that's exiting to its left, the pane cannot enter from right since
        // doing so will either interfere with the showing pane, or cause incorrect order of the
        // pane position during the transition. In other words, this case is considered "blocking".
        // Same on the other side.
        val noBlockingPanesOnRight = !hasShownPanesOnRight && !hasRightPaneExitToLeft
        val noBlockingPanesOnLeft = !hasShownPanesOnLeft && !hasLeftPaneExitToRight
        if (paneMotionTypes[i] == PaneMotion.Type.Entering) {
            paneMotions[i] =
                if (noBlockingPanesOnRight && !hasPanesExitToRight) {
                    // No panes will block the motion on the right, enter from right.
                    PaneMotion.EnterFromRight
                } else if (noBlockingPanesOnLeft && !hasPanesExitToLeft) {
                    // No panes will block the motion on the left, enter from left.
                    PaneMotion.EnterFromLeft
                } else if (noBlockingPanesOnRight) {
                    // Only hiding panes can interfere the motion on the right, enter from right.
                    PaneMotion.EnterFromRightDelayed
                } else if (noBlockingPanesOnLeft) {
                    // Only hiding panes can interfere the motion on the left, enter from left.
                    PaneMotion.EnterFromLeftDelayed
                } else {
                    // Both sides has panes that keep being visible during transition, expand to
                    // enter
                    PaneMotion.EnterWithExpand
                }
        }
    }
    return paneMotions
}

internal val IntRectToVector: TwoWayConverter<IntRect, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = {
            AnimationVector4D(
                it.left.toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat()
            )
        },
        convertFromVector = {
            IntRect(
                it.v1.fastRoundToInt(),
                it.v2.fastRoundToInt(),
                it.v3.fastRoundToInt(),
                it.v4.fastRoundToInt()
            )
        }
    )

internal class DerivedSizeAnimationSpec(private val boundsSpec: FiniteAnimationSpec<IntRect>) :
    FiniteAnimationSpec<IntSize> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<IntSize, V>
    ): VectorizedFiniteAnimationSpec<V> =
        boundsSpec.vectorize(
            object : TwoWayConverter<IntRect, V> {
                override val convertFromVector: (V) -> IntRect = { vector ->
                    with(converter.convertFromVector(vector)) { IntRect(0, 0, width, height) }
                }
                override val convertToVector: (IntRect) -> V = { bounds ->
                    converter.convertToVector(bounds.size)
                }
            }
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedSizeAnimationSpec) return false
        return boundsSpec == other.boundsSpec
    }

    override fun hashCode(): Int = boundsSpec.hashCode()
}

internal class DerivedOffsetAnimationSpec(private val boundsSpec: FiniteAnimationSpec<IntRect>) :
    FiniteAnimationSpec<IntOffset> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<IntOffset, V>
    ): VectorizedFiniteAnimationSpec<V> =
        boundsSpec.vectorize(
            object : TwoWayConverter<IntRect, V> {
                override val convertFromVector: (V) -> IntRect = { vector ->
                    with(converter.convertFromVector(vector)) { IntRect(x, y, x, y) }
                }
                override val convertToVector: (IntRect) -> V = { bounds ->
                    converter.convertToVector(bounds.topLeft)
                }
            }
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedOffsetAnimationSpec) return false
        return boundsSpec == other.boundsSpec
    }

    override fun hashCode(): Int = boundsSpec.hashCode()
}
