/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.CaptionBar
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.DisplayCutout
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Ime
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.MandatorySystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.NavigationBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.StatusBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.TappableElement
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Waterfall
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requestRemeasure
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEachIndexed

internal actual fun findDisplayCutouts(placementScope: Placeable.PlacementScope): List<RectRulers> {
    var node = placementScope.coordinates?.findRootCoordinates() as? NodeCoordinator
    while (node != null) {
        node.visitNodes(Nodes.Traversable) { traversableNode ->
            if (traversableNode.traverseKey === RulerKey) {
                return (traversableNode as RulerProviderModifierNode).displayCutoutRulers
            }
        }
        node = node.wrapped
    }
    return emptyList()
}

internal actual fun findInsetsAnimationProperties(
    placementScope: Placeable.PlacementScope,
    windowInsetsRulers: WindowInsetsRulers
): WindowInsetsAnimation {
    return NoWindowInsetsAnimation
}

internal data class RulerProviderModifierElement(
    val windowInsets: PlatformWindowInsets
): ModifierNodeElement<RulerProviderModifierNode>() {
    override fun create(): RulerProviderModifierNode = RulerProviderModifierNode(windowInsets)
    override fun update(node: RulerProviderModifierNode) {
        node.windowInsets = windowInsets
    }
}

private const val RulerKey = "androidx.compose.ui.layout.WindowInsetsRulers"

internal class RulerProviderModifierNode(
    windowInsets: PlatformWindowInsets,
) : Modifier.Node(), LayoutModifierNode, TraversableNode {

    val displayCutoutRulers = mutableStateListOf<RectRulers>()

    var windowInsets: PlatformWindowInsets = windowInsets
        set(value) {
            if (field !== value) {
                field = value
                requestRemeasure()
            }
        }

    fun rulerLambda(width: Int, height: Int): RulerScope.() -> Unit = {
        provideInsetsValues(CaptionBar, windowInsets.captionBar, width, height)
        provideInsetsValues(DisplayCutout, windowInsets.displayCutout, width, height)
        provideInsetsValues(Ime, windowInsets.ime, width, height)
        provideInsetsValues(MandatorySystemGestures, windowInsets.mandatorySystemGestures, width, height)
        provideInsetsValues(NavigationBars, windowInsets.navigationBars, width, height)
        provideInsetsValues(StatusBars, windowInsets.statusBars, width, height)
        provideInsetsValues(SystemGestures, windowInsets.systemGestures, width, height)
        provideInsetsValues(TappableElement, windowInsets.tappableElement, width, height)
        provideInsetsValues(Waterfall, windowInsets.waterfall, width, height)

        val displayCutouts = windowInsets.displayCutouts

        if (displayCutouts.isEmpty()) {
            displayCutoutRulers.clear()
        } else if (displayCutouts.size < displayCutoutRulers.size){
            displayCutoutRulers.removeRange(displayCutouts.size, displayCutoutRulers.size)
        } else {
            repeat(displayCutouts.size - displayCutoutRulers.size) {
                displayCutoutRulers += RectRulers("display cutout rect ${displayCutoutRulers.size}")
            }
        }

        displayCutouts.fastForEachIndexed { index, rect ->
            val rulers = displayCutoutRulers[index]
            rulers.left provides rect.left
            rulers.top provides rect.top
            rulers.right provides rect.right
            rulers.bottom provides rect.bottom
        }
    }

    private fun RulerScope.provideInsetsValues(
        rulers: WindowInsetsRulers,
        platformInsets: PlatformInsets,
        width: Int,
        height: Int
    ) {
        provideInsetsValues(rulers.current, platformInsets, width, height)
        provideInsetsValues(rulers.maximum, platformInsets, width, height)
    }

    private fun RulerScope.provideInsetsValues(
        rulers: RectRulers,
        insets: PlatformInsets,
        width: Int,
        height: Int,
    ) {
        val left = insets.left
        val top = insets.top
        val right = (width - insets.right)
        val bottom = (height - insets.bottom)

        rulers.left provides left.toFloat()
        rulers.top provides top.toFloat()
        rulers.right provides right.toFloat()
        rulers.bottom provides bottom.toFloat()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val width = placeable.width
        val height = placeable.height
        return layout(width, height, rulers = rulerLambda(width, height)) { placeable.place(0, 0) }
    }

    override val traverseKey: Any
        get() = RulerKey
}