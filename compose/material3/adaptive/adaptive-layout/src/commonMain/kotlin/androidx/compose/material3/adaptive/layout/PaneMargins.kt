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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

// TODO(conradchen): move the modifier declarations to PaneScaffoldPaneScope when we can publish it.
/**
 * This modifier specifies the associated pane's margins according to the provided
 * [WindowInsetsRulers]. Note that if multiple window inset rulers are provided, the scaffold will
 * decide the actual margins by taking the union of these insets - i.e. the one creating the largest
 * margins will be used.
 *
 * @param windowInsets the window insets the pane wants to respect.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun Modifier.paneMargins(vararg windowInsets: WindowInsetsRulers) =
    paneMargins(PaddingValues(), windowInsets.toList())

// TODO(conradchen): move the modifier declarations to PaneScaffoldPaneScope when we can publish it.
/**
 * This modifier specifies the associated pane's margins according to specified fixed margins and
 * the provided [WindowInsetsRulers], if any. Note that the scaffold will decide the actual margins
 * by taking the union of the fixed margins and the provided insets - i.e. the one creating the
 * largest margins will be used.
 *
 * @param fixedMargins fixed margins to use for the pane.
 * @param windowInsets the window insets the pane wants to respect.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun Modifier.paneMargins(
    fixedMargins: PaddingValues,
    vararg windowInsets: WindowInsetsRulers
) = paneMargins(fixedMargins, windowInsets.toList())

@Composable
private fun Modifier.paneMargins(
    fixedMargins: PaddingValues,
    windowInsets: List<WindowInsetsRulers>
) =
    this.then(
        PaneMarginsElement(
            PaneMarginsImpl(
                fixedMargins,
                windowInsets,
                LocalDensity.current,
                LocalLayoutDirection.current
            )
        )
    )

private data class PaneMarginsElement(val paneMargins: PaneMargins) :
    ModifierNodeElement<PaneMarginsNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "paneMargins"
        properties["paneMargins"] = paneMargins
    }

    override fun create(): PaneMarginsNode {
        return PaneMarginsNode(paneMargins)
    }

    override fun update(node: PaneMarginsNode) {
        node.paneMargins = paneMargins
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }
}

private class PaneMarginsNode(var paneMargins: PaneMargins) :
    ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentData) ?: PaneScaffoldParentData()).also {
            it.paneMargins = paneMargins
        }
}

@Immutable
internal interface PaneMargins {
    fun Placeable.PlacementScope.getPaneLeft(measuredLeft: Int) = measuredLeft

    fun Placeable.PlacementScope.getPaneTop(measuredTop: Int) = measuredTop

    fun Placeable.PlacementScope.getPaneRight(measuredRight: Int, parentRight: Int) = measuredRight

    fun Placeable.PlacementScope.getPaneBottom(measuredBottom: Int, parentBottom: Int) =
        measuredBottom

    companion object {
        val Unspecified = object : PaneMargins {}
    }
}

@Immutable
internal class PaneMarginsImpl(
    fixedMargins: PaddingValues = PaddingValues(),
    windowInsets: List<WindowInsetsRulers>,
    density: Density,
    layoutDirection: LayoutDirection
) : PaneMargins {
    private val fixedMarginLeft =
        with(density) { fixedMargins.calculateLeftPadding(layoutDirection).roundToPx() }
    private val fixedMarginTop = with(density) { fixedMargins.calculateTopPadding().roundToPx() }
    private val fixedMarginRight =
        with(density) { fixedMargins.calculateRightPadding(layoutDirection).roundToPx() }
    private val fixedMarginBottom =
        with(density) { fixedMargins.calculateBottomPadding().roundToPx() }
    private val rulers = windowInsets

    override fun Placeable.PlacementScope.getPaneLeft(measuredLeft: Int): Int =
        maxOf(
            measuredLeft,
            fixedMarginLeft,
            rulers.maxOfOrNull { it.left.current(0f).roundToInt() } ?: 0
        )

    override fun Placeable.PlacementScope.getPaneTop(measuredTop: Int): Int =
        maxOf(
            measuredTop,
            fixedMarginTop,
            rulers.maxOfOrNull { it.top.current(0f).roundToInt() } ?: 0
        )

    override fun Placeable.PlacementScope.getPaneRight(measuredRight: Int, parentRight: Int): Int =
        minOf(
            measuredRight,
            parentRight - fixedMarginRight,
            rulers.minOfOrNull { it.right.current(Float.MAX_VALUE).roundToInt() } ?: parentRight
        )

    override fun Placeable.PlacementScope.getPaneBottom(
        measuredBottom: Int,
        parentBottom: Int
    ): Int =
        minOf(
            measuredBottom,
            parentBottom - fixedMarginBottom,
            rulers.minOfOrNull { it.bottom.current(Float.MAX_VALUE).roundToInt() } ?: parentBottom
        )
}

// TODO(conradchen): Move to use the foundation definition when it's available
internal class WindowInsetsRulers {
    val left = VerticalRuler()
    val top = HorizontalRuler()
    val right = VerticalRuler()
    val bottom = HorizontalRuler()
}
