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
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntOffset

/**
 * Holds the transitions that can be applied to the different panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class ThreePaneMotion internal constructor(
    internal val animationSpec: FiniteAnimationSpec<IntOffset> = snap(),
    private val firstPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val firstPaneExitTransition: ExitTransition = ExitTransition.None,
    private val secondPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val secondPaneExitTransition: ExitTransition = ExitTransition.None,
    private val thirdPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val thirdPaneExitTransition: ExitTransition = ExitTransition.None
) {

    /**
     * Resolves and returns the [EnterTransition] for the given [ThreePaneScaffoldRole]
     * at the given [ThreePaneScaffoldHorizontalOrder].
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
     * Resolves and returns the [ExitTransition] for the given [ThreePaneScaffoldRole]
     * at the given [ThreePaneScaffoldHorizontalOrder].
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
        if (this.animationSpec != other.animationSpec) return false
        if (this.firstPaneEnterTransition != other.firstPaneEnterTransition) return false
        if (this.firstPaneExitTransition != other.firstPaneExitTransition) return false
        if (this.secondPaneEnterTransition != other.secondPaneEnterTransition) return false
        if (this.secondPaneExitTransition != other.secondPaneExitTransition) return false
        if (this.thirdPaneEnterTransition != other.thirdPaneEnterTransition) return false
        if (this.thirdPaneExitTransition != other.thirdPaneExitTransition) return false
        return true
    }

    override fun hashCode(): Int {
        var result = animationSpec.hashCode()
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
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun calculateThreePaneMotion(
    previousScaffoldValue: ThreePaneScaffoldValue,
    currentScaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder
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
        1 -> when (PaneAdaptedValue.Expanded) {
            previousScaffoldValue[paneOrder.firstPane] -> {
                ThreePaneMotionDefaults.movePanesToLeftMotion
            }

            previousScaffoldValue[paneOrder.thirdPane] -> {
                ThreePaneMotionDefaults.movePanesToRightMotion
            }

            currentScaffoldValue[paneOrder.thirdPane] -> {
                ThreePaneMotionDefaults.movePanesToLeftMotion
            }

            else -> {
                ThreePaneMotionDefaults.movePanesToRightMotion
            }
        }

        2 -> when {
            previousScaffoldValue[paneOrder.firstPane] == PaneAdaptedValue.Expanded &&
                currentScaffoldValue[paneOrder.firstPane] == PaneAdaptedValue.Expanded -> {
                // The first pane stays, the right two panes switch
                ThreePaneMotionDefaults.switchRightTwoPanesMotion
            }

            previousScaffoldValue[paneOrder.thirdPane] == PaneAdaptedValue.Expanded &&
                currentScaffoldValue[paneOrder.thirdPane] == PaneAdaptedValue.Expanded -> {
                // The third pane stays, the left two panes switch
                ThreePaneMotionDefaults.switchLeftTwoPanesMotion
            }

            // Implies the second pane stays hereafter
            currentScaffoldValue[paneOrder.thirdPane] == PaneAdaptedValue.Expanded -> {
                // The third pane shows, all panes move left
                ThreePaneMotionDefaults.movePanesToLeftMotion
            }

            else -> {
                // The first pane shows, all panes move right
                ThreePaneMotionDefaults.movePanesToRightMotion
            }
        }

        else -> {
            // Should not happen
            ThreePaneMotion.NoMotion
        }
    }
}

@ExperimentalMaterial3AdaptiveApi
internal object ThreePaneMotionDefaults {
    /**
     * A default [SpringSpec] for the panes motion.
     */
    // TODO(conradchen): open this to public when we support motion customization
    val PaneSpringSpec: SpringSpec<IntOffset> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 600f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    private val slideInFromLeft = slideInHorizontally(PaneSpringSpec) { -it }
    private val slideInFromRight = slideInHorizontally(PaneSpringSpec) { it }
    private val slideOutToLeft = slideOutHorizontally(PaneSpringSpec) { -it }
    private val slideOutToRight = slideOutHorizontally(PaneSpringSpec) { it }

    val movePanesToRightMotion = ThreePaneMotion(
        PaneSpringSpec,
        slideInFromLeft,
        slideOutToRight,
        slideInFromLeft,
        slideOutToRight,
        slideInFromLeft,
        slideOutToRight
    )

    val movePanesToLeftMotion = ThreePaneMotion(
        PaneSpringSpec,
        slideInFromRight,
        slideOutToLeft,
        slideInFromRight,
        slideOutToLeft,
        slideInFromRight,
        slideOutToLeft
    )

    val switchLeftTwoPanesMotion = ThreePaneMotion(
        PaneSpringSpec,
        slideInFromLeft,
        slideOutToLeft,
        slideInFromLeft,
        slideOutToLeft,
        EnterTransition.None,
        ExitTransition.None
    )

    val switchRightTwoPanesMotion = ThreePaneMotion(
        PaneSpringSpec,
        EnterTransition.None,
        ExitTransition.None,
        slideInFromRight,
        slideOutToRight,
        slideInFromRight,
        slideOutToRight
    )
}
