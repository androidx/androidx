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

package androidx.compose.material3.common

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

/**
 * Reserves at least 48.dp in size to disambiguate touch interactions if the element would measure
 * smaller.
 *
 * https://m3.material.io/foundations/designing/structure#dab862b1-e042-4c40-b680-b484b9f077f6
 *
 * This uses the Material recommended minimum size of 48.dp x 48.dp, which may not the same as the
 * system enforced minimum size. The minimum clickable / touch target size (48.dp by default) is
 * controlled by the system via [ViewConfiguration] and automatically expanded at the touch input
 * layer.
 *
 * This modifier is not needed for touch target expansion to happen. It only affects layout, to make
 * sure there is adequate space for touch target expansion.
 *
 * Because layout constraints are affected by modifier order, for this modifier to take effect, it
 * must come before any size modifiers on the element that might limit its constraints.
 *
 * @see LocalMinimumInteractiveComponentSize
 */
@Stable
fun Modifier.minimumInteractiveComponentSize(): Modifier = this then MinimumInteractiveModifier

internal object MinimumInteractiveModifier : ModifierNodeElement<MinimumInteractiveModifierNode>() {

    override fun create(): MinimumInteractiveModifierNode = MinimumInteractiveModifierNode()

    override fun update(node: MinimumInteractiveModifierNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "minimumInteractiveComponentSize"
        // TODO: b/214589635 - surface this information through the layout inspector in a better way
        //  - for now just add some information to help developers debug what this size represents.
        properties["README"] =
            "Reserves at least 48.dp in size to disambiguate touch " +
                "interactions if the element would measure smaller"
    }

    override fun hashCode(): Int = identifyHashCode(this)

    override fun equals(other: Any?) = (other === this)
}

internal expect inline fun identifyHashCode(value: Any): Int

internal class MinimumInteractiveModifierNode :
    Modifier.Node(), CompositionLocalConsumerModifierNode, LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val size = currentValueOf(LocalMinimumInteractiveComponentSize).coerceAtLeast(0.dp)
        val placeable = measurable.measure(constraints)
        val enforcement = isAttached && (size.isSpecified && size > 0.dp)

        val sizePx = if (size.isSpecified) size.roundToPx() else 0
        // Be at least as big as the minimum dimension in both dimensions
        val width =
            if (enforcement) {
                maxOf(placeable.width, sizePx)
            } else {
                placeable.width
            }
        val height =
            if (enforcement) {
                maxOf(placeable.height, sizePx)
            } else {
                placeable.height
            }

        return layout(width, height) {
            val centerX = ((width - placeable.width) / 2f).roundToInt()
            val centerY = ((height - placeable.height) / 2f).roundToInt()
            placeable.place(centerX, centerY)
        }
    }
}

/**
 * CompositionLocal that configures the minimum touch target size for Material components (such as
 * [Button]) to ensure they are accessible. If a component has a visual size that is lower than the
 * minimum touch target size, extra space outside the component will be included. If set to 0.dp,
 * there will be no extra space, and so it is possible that if the component is placed near the edge
 * of a layout / near to another component without any padding, there will not be enough space for
 * an accessible touch target.
 */
val LocalMinimumInteractiveComponentSize: ProvidableCompositionLocal<Dp> =
    staticCompositionLocalOf {
        48.dp
    }
