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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
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
     * The position animation spec of the associated pane to the scope. [AnimatedPane] will use this
     * value to perform pane animations during scaffold state changes.
     */
    val positionAnimationSpec: FiniteAnimationSpec<IntOffset>

    /**
     * The size animation spec of the associated pane to the scope. [AnimatedPane] will use this
     * value to perform pane animations during scaffold state changes.
     */
    val sizeAnimationSpec: FiniteAnimationSpec<IntSize>

    /**
     * The delayed position animation spec of the associated pane to the scope. [AnimatedPane] will
     * use this value to perform pane position animations during scaffold state changes when an
     * animation needs to be played with a delay.
     */
    val delayedPositionAnimationSpec: FiniteAnimationSpec<IntOffset>

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
 * A class to collect motion-relevant data of a specific pane.
 *
 * @property motion The specified [PaneMotion] of the pane.
 * @property currentSize The current measured size of the pane that it should animate from.
 * @property currentPosition The current placement of the pane that it should animate from, with the
 *   offset relative to the associated pane scaffold's local coordinates.
 * @property targetSize The target measured size of the pane that it should animate to.
 * @property targetPosition The target placement of the pane that it should animate to, with the
 *   offset relative to the associated pane scaffold's local coordinates.
 */
@ExperimentalMaterial3AdaptiveApi
class PaneMotionData internal constructor() {
    var motion: PaneMotion = DefaultPaneMotion.NoMotion
        internal set

    var currentSize: IntSize = IntSize.Zero
        internal set

    var currentPosition: IntOffset = IntOffset.Zero
        internal set

    var targetSize: IntSize = IntSize.Zero
        internal set

    var targetPosition: IntOffset = IntOffset.Zero
        internal set
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneScaffoldMotionScope.slideInFromLeftOffset: Int
    get() {
        // Find the right edge offset of the rightmost pane that enters from its left
        paneMotionDataList.fastForEachReversed {
            if (
                it.motion == DefaultPaneMotion.EnterFromLeft ||
                    it.motion == DefaultPaneMotion.EnterFromLeftDelayed
            ) {
                return -it.targetPosition.x - it.targetSize.width
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneScaffoldMotionScope.slideInFromRightOffset: Int
    get() {
        // Find the left edge offset of the leftmost pane that enters from its right
        paneMotionDataList.fastForEach {
            if (
                it.motion == DefaultPaneMotion.EnterFromRight ||
                    it.motion == DefaultPaneMotion.EnterFromRightDelayed
            ) {
                return scaffoldSize.width - it.targetPosition.x
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneScaffoldMotionScope.slideOutToLeftOffset: Int
    get() {
        // Find the right edge offset of the rightmost pane that exits to its left
        paneMotionDataList.fastForEachReversed {
            if (it.motion == DefaultPaneMotion.ExitToLeft) {
                return -it.currentPosition.x - it.currentSize.width
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneScaffoldMotionScope.slideOutToRightOffset: Int
    get() {
        // Find the left edge offset of the leftmost pane that exits to its right
        paneMotionDataList.fastForEach {
            if (it.motion == DefaultPaneMotion.ExitToRight) {
                return scaffoldSize.width - it.currentPosition.x
            }
        }
        return 0
    }

/** Interface to specify a custom pane enter/exit motion when a pane's visibility changes. */
@ExperimentalMaterial3AdaptiveApi
interface PaneMotion {
    /** The [EnterTransition] of a pane under the given [PaneScaffoldMotionScope] */
    val PaneScaffoldMotionScope.enterTransition: EnterTransition

    /** The [ExitTransition] of a pane under the given [PaneScaffoldMotionScope] */
    val PaneScaffoldMotionScope.exitTransition: ExitTransition
}

@ExperimentalMaterial3AdaptiveApi
@JvmInline
internal value class DefaultPaneMotion private constructor(val value: Int) : PaneMotion {
    companion object {
        val NoMotion = DefaultPaneMotion(0)
        val AnimateBounds = DefaultPaneMotion(1)
        val EnterFromLeft = DefaultPaneMotion(2)
        val EnterFromRight = DefaultPaneMotion(3)
        val EnterFromLeftDelayed = DefaultPaneMotion(4)
        val EnterFromRightDelayed = DefaultPaneMotion(5)
        val ExitToLeft = DefaultPaneMotion(6)
        val ExitToRight = DefaultPaneMotion(7)
        val EnterWithExpand = DefaultPaneMotion(8)
        val ExitWithShrink = DefaultPaneMotion(9)
    }

    override val PaneScaffoldMotionScope.enterTransition: EnterTransition
        get() =
            when (this@DefaultPaneMotion) {
                EnterFromLeft ->
                    slideInHorizontally(positionAnimationSpec) { slideInFromLeftOffset }
                EnterFromRight ->
                    slideInHorizontally(positionAnimationSpec) { slideInFromRightOffset }
                EnterFromLeftDelayed ->
                    slideInHorizontally(delayedPositionAnimationSpec) { slideInFromLeftOffset }
                EnterFromRightDelayed ->
                    slideInHorizontally(delayedPositionAnimationSpec) { slideInFromRightOffset }
                // TODO(conradche): Figure out how to expand with position change
                EnterWithExpand ->
                    expandHorizontally(sizeAnimationSpec, Alignment.CenterHorizontally)
                else -> EnterTransition.None
            }

    override val PaneScaffoldMotionScope.exitTransition: ExitTransition
        get() =
            when (this@DefaultPaneMotion) {
                ExitToLeft -> slideOutHorizontally(positionAnimationSpec) { slideOutToLeftOffset }
                ExitToRight -> slideOutHorizontally(positionAnimationSpec) { slideOutToRightOffset }
                // TODO(conradche): Figure out how to shrink with position change
                ExitWithShrink ->
                    shrinkHorizontally(sizeAnimationSpec, Alignment.CenterHorizontally)
                else -> ExitTransition.None
            }

    override fun toString(): String =
        when (this) {
            NoMotion -> "NoMotion"
            AnimateBounds -> "AnimateBounds"
            EnterFromLeft -> "EnterFromLeft"
            EnterFromRight -> "EnterFromRight"
            EnterFromLeftDelayed -> "EnterFromLeftDelayed"
            EnterFromRightDelayed -> "EnterFromRightDelayed"
            ExitToLeft -> "ExitToLeft"
            ExitToRight -> "ExitToRight"
            EnterWithExpand -> "EnterWithExpand"
            ExitWithShrink -> "ExitWithShrink"
            else -> "Undefined($value)"
        }
}

@ExperimentalMaterial3AdaptiveApi
internal fun <T> calculatePaneMotion(
    previousScaffoldValue: PaneScaffoldValue<T>,
    currentScaffoldValue: PaneScaffoldValue<T>,
    paneOrder: PaneScaffoldHorizontalOrder<T>
): List<PaneMotion> {
    val numOfPanes = paneOrder.size
    val paneStatus = Array(numOfPanes) { PaneMotionStatus.Hidden }
    val paneMotions = MutableList<PaneMotion>(numOfPanes) { DefaultPaneMotion.NoMotion }
    var firstShownPaneIndex = numOfPanes
    var firstEnteringPaneIndex = numOfPanes
    var lastShownPaneIndex = -1
    var lastEnteringPaneIndex = -1
    // First pass, to decide the entering/exiting status of each pane, and collect info for
    // deciding, given a certain pane, if there's a pane on its left or on its right that is
    // entering or keep showing during the transition.
    // Also set up the motions of all panes that keep showing to AnimateBounds.
    paneOrder.forEachIndexed { i, role ->
        paneStatus[i] =
            PaneMotionStatus.calculate(previousScaffoldValue[role], currentScaffoldValue[role])
        when (paneStatus[i]) {
            PaneMotionStatus.Shown -> {
                firstShownPaneIndex = min(firstShownPaneIndex, i)
                lastShownPaneIndex = max(lastShownPaneIndex, i)
                paneMotions[i] = DefaultPaneMotion.AnimateBounds
            }
            PaneMotionStatus.Entering -> {
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
        if (paneStatus[i] == PaneMotionStatus.Exiting) {
            paneMotions[i] =
                if (!hasShownPanesOnRight && !hasEnteringPanesOnRight) {
                    // No panes will interfere the motion on the right, exit to right.
                    hasPanesExitToRight = true
                    firstPaneExitToRightIndex = min(firstPaneExitToRightIndex, i)
                    DefaultPaneMotion.ExitToRight
                } else if (!hasShownPanesOnLeft && !hasEnteringPanesOnLeft) {
                    // No panes will interfere the motion on the left, exit to left.
                    hasPanesExitToLeft = true
                    lastPaneExitToLeftIndex = max(lastPaneExitToLeftIndex, i)
                    DefaultPaneMotion.ExitToLeft
                } else if (!hasShownPanesOnRight) {
                    // Only showing panes can interfere the motion on the right, exit to right.
                    hasPanesExitToRight = true
                    firstPaneExitToRightIndex = min(firstPaneExitToRightIndex, i)
                    DefaultPaneMotion.ExitToRight
                } else if (!hasShownPanesOnLeft) { // Only showing panes on left
                    // Only showing panes can interfere the motion on the left, exit to left.
                    hasPanesExitToLeft = true
                    lastPaneExitToLeftIndex = max(lastPaneExitToLeftIndex, i)
                    DefaultPaneMotion.ExitToLeft
                } else {
                    // Both sides has panes that keep being visible during transition, shrink to
                    // exit
                    DefaultPaneMotion.ExitWithShrink
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
        if (paneStatus[i] == PaneMotionStatus.Entering) {
            paneMotions[i] =
                if (noBlockingPanesOnRight && !hasPanesExitToRight) {
                    // No panes will block the motion on the right, enter from right.
                    DefaultPaneMotion.EnterFromRight
                } else if (noBlockingPanesOnLeft && !hasPanesExitToLeft) {
                    // No panes will block the motion on the left, enter from left.
                    DefaultPaneMotion.EnterFromLeft
                } else if (noBlockingPanesOnRight) {
                    // Only hiding panes can interfere the motion on the right, enter from right.
                    DefaultPaneMotion.EnterFromRightDelayed
                } else if (noBlockingPanesOnLeft) {
                    // Only hiding panes can interfere the motion on the left, enter from left.
                    DefaultPaneMotion.EnterFromLeftDelayed
                } else {
                    // Both sides has panes that keep being visible during transition, expand to
                    // enter
                    DefaultPaneMotion.EnterWithExpand
                }
        }
    }
    return paneMotions
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@JvmInline
private value class PaneMotionStatus private constructor(val value: Int) {
    companion object {
        val Hidden = PaneMotionStatus(0)
        val Exiting = PaneMotionStatus(1)
        val Entering = PaneMotionStatus(2)
        val Shown = PaneMotionStatus(3)

        fun calculate(
            previousValue: PaneAdaptedValue,
            currentValue: PaneAdaptedValue
        ): PaneMotionStatus {
            val wasShown = if (previousValue == PaneAdaptedValue.Hidden) 0 else 1
            val isShown = if (currentValue == PaneAdaptedValue.Hidden) 0 else 2
            return PaneMotionStatus(wasShown or isShown)
        }
    }
}
