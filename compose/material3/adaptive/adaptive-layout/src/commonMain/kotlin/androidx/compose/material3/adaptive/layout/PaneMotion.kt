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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import kotlin.math.max
import kotlin.math.min

@ExperimentalMaterial3AdaptiveApi
internal interface PaneMotion {
    // TODO (conradchen): Implement the following fields
    // val enterTransition: EnterTransition
    // val exitTransition: ExitTransition
    // val animateBoundsModifier: Modifier
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
        val ExitWithShrink = DefaultPaneMotion(8)
        val EnterWithExpand = DefaultPaneMotion(9)
    }

    override fun toString(): String =
        when (value) {
            0 -> "NoMotion"
            1 -> "AnimateBounds"
            2 -> "EnterFromLeft"
            3 -> "EnterFromRight"
            4 -> "EnterFromLeftDelayed"
            5 -> "EnterFromRightDelayed"
            6 -> "ExitToLeft"
            7 -> "ExitToRight"
            8 -> "ExitWithShrink"
            9 -> "EnterWithExpand"
            else -> "Undefined($value)"
        }
}

@ExperimentalMaterial3AdaptiveApi
@VisibleForTesting
internal fun <T> calculatePaneMotion(
    previousScaffoldValue: PaneScaffoldValue<T>,
    currentScaffoldValue: PaneScaffoldValue<T>,
    paneOrder: PaneScaffoldHorizontalOrder<T>
): Array<PaneMotion> {
    val numOfPanes = paneOrder.size
    val paneStatus = Array(numOfPanes) { PaneMotionStatus.Hidden }
    val paneMotions = Array<PaneMotion>(numOfPanes) { DefaultPaneMotion.NoMotion }
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
