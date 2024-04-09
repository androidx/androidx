/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.allVerticalHingeBounds
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.occludingVerticalHingeBounds
import androidx.compose.material3.adaptive.separatingVerticalHingeBounds
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Calculates the recommended [PaneScaffoldDirective] from a given [WindowAdaptiveInfo]. Use this
 * method with [currentWindowAdaptiveInfo] to acquire Material-recommended adaptive layout
 * settings of the current activity window.
 *
 * See more details on the [Material design guideline site]
 * (https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 *
 * @param windowAdaptiveInfo [WindowAdaptiveInfo] that collects useful information in making
 *        layout adaptation decisions like [WindowSizeClass].
 * @param verticalHingePolicy [HingePolicy] that decides how layouts are supposed to address
 *        vertical hinges.
 * @return an [PaneScaffoldDirective] to be used to decide adaptive layout states.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculatePaneScaffoldDirective(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating
): PaneScaffoldDirective {
    val maxHorizontalPartitions: Int
    val horizontalPartitionSpacerSize: Dp
    when (windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            maxHorizontalPartitions = 1
            horizontalPartitionSpacerSize = 0.dp
        }
        WindowWidthSizeClass.MEDIUM -> {
            maxHorizontalPartitions = 1
            horizontalPartitionSpacerSize = 0.dp
        }
        else -> {
            maxHorizontalPartitions = 2
            horizontalPartitionSpacerSize = 24.dp
        }
    }
    val maxVerticalPartitions: Int
    val verticalPartitionSpacerSize: Dp

    // TODO(conradchen): Confirm the table top mode settings
    if (windowAdaptiveInfo.windowPosture.isTabletop) {
        maxVerticalPartitions = 2
        verticalPartitionSpacerSize = 24.dp
    } else {
        maxVerticalPartitions = 1
        verticalPartitionSpacerSize = 0.dp
    }

    // TODO(conradchen): add 412.dp for L/XL window size class when they are available
    val defaultPanePreferredWidth = 360.dp

    return PaneScaffoldDirective(
        maxHorizontalPartitions,
        horizontalPartitionSpacerSize,
        maxVerticalPartitions,
        verticalPartitionSpacerSize,
        defaultPanePreferredWidth,
        getExcludedVerticalBounds(windowAdaptiveInfo.windowPosture, verticalHingePolicy)
    )
}

/**
 * Calculates the recommended [PaneScaffoldDirective] from a given [WindowAdaptiveInfo]. Use this
 * method with [currentWindowAdaptiveInfo] to acquire Material-recommended dense-mode adaptive
 * layout settings of the current activity window. Note that this function results in a dual-pane
 * layout when the [WindowWidthSizeClass] is [WindowWidthSizeClass.MEDIUM], while
 * [calculatePaneScaffoldDirective] results in a single-pane layout instead. We recommend to use
 * [calculatePaneScaffoldDirective], unless you have a strong use case to show two panes on
 * a medium-width window, which can make your layout look too packed.
 *
 * See more details on the [Material design guideline site]
 * (https://m3.material.io/foundations/layout/applying-layout/window-size-classes).
 *
 * @param windowAdaptiveInfo [WindowAdaptiveInfo] that collects useful information in making
 *        layout adaptation decisions like [WindowSizeClass].
 * @param verticalHingePolicy [HingePolicy] that decides how layouts are supposed to address
 *        vertical hinges.
 * @return an [PaneScaffoldDirective] to be used to decide adaptive layout states.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    verticalHingePolicy: HingePolicy = HingePolicy.AvoidSeparating
): PaneScaffoldDirective {
    val isMediumWidth =
        windowAdaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM
    return with(calculatePaneScaffoldDirective(windowAdaptiveInfo, verticalHingePolicy)) {
        copy(
            maxHorizontalPartitions = if (isMediumWidth) 2 else maxHorizontalPartitions,
            horizontalPartitionSpacerSize = if (isMediumWidth) {
                24.dp
            } else {
                horizontalPartitionSpacerSize
            }
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun getExcludedVerticalBounds(posture: Posture, hingePolicy: HingePolicy): List<Rect> {
    return when (hingePolicy) {
        HingePolicy.AvoidSeparating -> posture.separatingVerticalHingeBounds
        HingePolicy.AvoidOccluding -> posture.occludingVerticalHingeBounds
        HingePolicy.AlwaysAvoid -> posture.allVerticalHingeBounds
        else -> emptyList()
    }
}

/**
 * Top-level directives about how a pane scaffold should be arranged and spaced, like how many
 * partitions the layout can be split into and what should be the gutter size.
 *
 * @property maxHorizontalPartitions the max number of partitions along the horizontal axis
 *           the layout can be split into.
 * @property horizontalPartitionSpacerSize Size of the spacers between horizontal partitions.
 *           It's equivalent to the left/right margins the horizontal partitions.
 * @property maxVerticalPartitions the max number of partitions along the vertical axis the layout
 *           can be split into.
 * @property verticalPartitionSpacerSize Size of the spacers between vertical partitions.
 *           It's equivalent to the top/bottom margins of the vertical partitions.
 * @property defaultPanePreferredWidth Default preferred width of panes that will be used by
 *           the scaffold if there's no [Modifier.preferredWidth] provided with a pane. See
 *           [Modifier.preferredWidth] for more info about how and when preferred width will be
 *           used.
 * @property excludedBounds the bounds of all areas in the window that the layout needs to avoid
 *           displaying anything upon it. Usually these bounds represent where physical hinges are.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
// TODO(conradchen): Hide the constructor. Please use the copy() method instead.
class PaneScaffoldDirective(
    val maxHorizontalPartitions: Int,
    val horizontalPartitionSpacerSize: Dp,
    val maxVerticalPartitions: Int,
    val verticalPartitionSpacerSize: Dp,
    val defaultPanePreferredWidth: Dp,
    val excludedBounds: List<Rect>
) {
    /**
     * Returns a new copy of [PaneScaffoldDirective] with specified fields overwritten. Use
     * this method to create a custom [PaneScaffoldDirective] from the default instance or
     * the result of [calculatePaneScaffoldDirective].
     *
     * @param maxHorizontalPartitions the max number of partitions along the horizontal axis
     *        the layout can be split into.
     * @param horizontalPartitionSpacerSize Size of the spacers between horizontal partitions.
     *        It's equivalent to the left/right margins the horizontal partitions.
     * @param maxVerticalPartitions the max number of partitions along the vertical axis the layout
     *        can be split into.
     * @param verticalPartitionSpacerSize Size of the spacers between vertical partitions.
     *        It's equivalent to the top/bottom margins of the vertical partitions.
     * @param defaultPanePreferredWidth Default preferred width of panes that will be used by
     *        the scaffold if there's no [Modifier.preferredWidth] provided with a pane.
     * @param excludedBounds the bounds of all areas in the window that the layout needs to avoid
     *        displaying anything upon it. Usually these bounds represent where physical hinges are.
     */
    fun copy(
        maxHorizontalPartitions: Int = this.maxHorizontalPartitions,
        horizontalPartitionSpacerSize: Dp = this.horizontalPartitionSpacerSize,
        maxVerticalPartitions: Int = this.maxVerticalPartitions,
        verticalPartitionSpacerSize: Dp = this.verticalPartitionSpacerSize,
        defaultPanePreferredWidth: Dp = this.defaultPanePreferredWidth,
        @Suppress("ListIterator") // No guarantee to be an array list
        excludedBounds: List<Rect> = this.excludedBounds.toList()
    ): PaneScaffoldDirective = PaneScaffoldDirective(
        maxHorizontalPartitions,
        horizontalPartitionSpacerSize,
        maxVerticalPartitions,
        verticalPartitionSpacerSize,
        defaultPanePreferredWidth,
        excludedBounds
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaneScaffoldDirective) return false
        if (maxHorizontalPartitions != other.maxHorizontalPartitions) return false
        if (horizontalPartitionSpacerSize != other.horizontalPartitionSpacerSize) return false
        if (maxVerticalPartitions != other.maxVerticalPartitions) return false
        if (verticalPartitionSpacerSize != other.verticalPartitionSpacerSize) return false
        if (defaultPanePreferredWidth != other.defaultPanePreferredWidth) return false
        return true
    }

    override fun hashCode(): Int {
        var result = maxHorizontalPartitions
        result = 31 * result + horizontalPartitionSpacerSize.hashCode()
        result = 31 * result + maxVerticalPartitions
        result = 31 * result + verticalPartitionSpacerSize.hashCode()
        result = 31 * result + defaultPanePreferredWidth.hashCode()
        return result
    }

    override fun toString(): String {
        return "PaneScaffoldDirective(maxHorizontalPartitions=$maxHorizontalPartitions, " +
            "horizontalPartitionSpacerSize=$horizontalPartitionSpacerSize, " +
            "maxVerticalPartitions=$maxVerticalPartitions, " +
            "verticalPartitionSpacerSize=$verticalPartitionSpacerSize, " +
            "defaultPanePreferredWidth=$defaultPanePreferredWidth, " +
            "number of excluded bounds=${excludedBounds.size})"
    }

    companion object {
        /**
         * A default instance of [PaneScaffoldDirective] that suggests a single-pane layout that
         * occupies the full window. To create a customized [PaneScaffoldDirective], you can use
         * [PaneScaffoldDirective.copy] on the default instance to create a copy with custom values.
         */
        val Default = PaneScaffoldDirective(
            maxHorizontalPartitions = 1,
            horizontalPartitionSpacerSize = 0.dp,
            maxVerticalPartitions = 1,
            verticalPartitionSpacerSize = 0.dp,
            defaultPanePreferredWidth = 360.dp,
            excludedBounds = emptyList()
        )
    }
}

/** Policies that indicate how hinges are supposed to be addressed in an adaptive layout. */
@Immutable
@kotlin.jvm.JvmInline
value class HingePolicy private constructor(private val value: Int) {
    override fun toString(): String {
        return "HingePolicy." + when (this) {
            AlwaysAvoid -> "AlwaysAvoid"
            AvoidSeparating -> "AvoidOccludingAndSeparating"
            AvoidOccluding -> "AvoidOccludingOnly"
            NeverAvoid -> "NeverAvoid"
            else -> ""
        }
    }

    companion object {
        /** When rendering content in a layout, always avoid where hinges are. */
        val AlwaysAvoid = HingePolicy(0)
        /**
         * When rendering content in a layout, avoid hinges that are separating. Note that an
         * occluding hinge is supposed to be separating as well but not vice versa.
         */
        val AvoidSeparating = HingePolicy(1)
        /**
         * When rendering content in a layout, avoid hinges that are occluding. Note that an
         * occluding hinge is supposed to be separating as well but not vice versa.
         */
        val AvoidOccluding = HingePolicy(2)
        /** When rendering content in a layout, never avoid any hinges, separating or not. */
        val NeverAvoid = HingePolicy(3)
    }
}
