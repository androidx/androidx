/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Reserves at least 48.dp in size to disambiguate touch interactions if the element would measure
 * smaller.
 *
 * https://m2.material.io/design/usability/accessibility.html#layout-and-typography
 *
 * This uses the Material recommended minimum size of 48.dp x 48.dp, which may not the same as the
 * system enforced minimum size. The minimum clickable / touch target size (48.dp by default) is
 * controlled by the system via ViewConfiguration` and automatically expanded at the touch input layer.
 *
 * This modifier is not needed for touch target expansion to happen. It only affects layout, to make
 * sure there is adequate space for touch target expansion.
 */
fun Modifier.minimumInteractiveComponentSize(): Modifier = this then MinimumInteractiveModifier

internal object MinimumInteractiveModifier :
    ModifierNodeElement<MinimumInteractiveModifierNode>() {

    override fun create(): MinimumInteractiveModifierNode = MinimumInteractiveModifierNode()

    override fun update(node: MinimumInteractiveModifierNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "minimumInteractiveComponentSize"
        // TODO: b/214589635 - surface this information through the layout inspector in a better way
        //  - for now just add some information to help developers debug what this size represents.
        properties["README"] = "Reserves at least 48.dp in size to disambiguate touch " +
            "interactions if the element would measure smaller"
    }

    override fun hashCode(): Int = System.identityHashCode(this)
    override fun equals(other: Any?) = (other === this)
}

internal class MinimumInteractiveModifierNode :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    LayoutModifierNode {

    @OptIn(ExperimentalMaterialApi::class)
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val enforcement = isAttached && currentValueOf(LocalMinimumInteractiveComponentEnforcement)
        val size = minimumInteractiveComponentSize
        val placeable = measurable.measure(constraints)

        // Be at least as big as the minimum dimension in both dimensions
        val width = if (enforcement) {
            maxOf(placeable.width, size.width.roundToPx())
        } else {
            placeable.width
        }
        val height = if (enforcement) {
            maxOf(placeable.height, size.height.roundToPx())
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
 * CompositionLocal that configures whether Material components that have a visual size that is
 * lower than the minimum touch target size for accessibility (such as [Button]) will include
 * extra space outside the component to ensure that they are accessible. If set to false there
 * will be no extra space, and so it is possible that if the component is placed near the edge of
 * a layout / near to another component without any padding, there will not be enough space for
 * an accessible touch target.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalMaterialApi
@ExperimentalMaterialApi
val LocalMinimumInteractiveComponentEnforcement: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { true }

/**
 * CompositionLocal that configures whether Material components that have a visual size that is
 * lower than the minimum touch target size for accessibility (such as [Button]) will include
 * extra space outside the component to ensure that they are accessible. If set to false there
 * will be no extra space, and so it is possible that if the component is placed near the edge of
 * a layout / near to another component without any padding, there will not be enough space for
 * an accessible touch target.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalMaterialApi
@ExperimentalMaterialApi
@Deprecated(
    message = "Use LocalMinimumInteractiveComponentEnforcement instead.",
    replaceWith = ReplaceWith(
        "LocalMinimumInteractiveComponentEnforcement"
    ),
    level = DeprecationLevel.WARNING
)
val LocalMinimumTouchTargetEnforcement: ProvidableCompositionLocal<Boolean> =
    LocalMinimumInteractiveComponentEnforcement

private class MinimumInteractiveComponentSizeModifier(val size: DpSize) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val placeable = measurable.measure(constraints)

        // Be at least as big as the minimum dimension in both dimensions
        val width = maxOf(placeable.width, size.width.roundToPx())
        val height = maxOf(placeable.height, size.height.roundToPx())

        return layout(width, height) {
            val centerX = ((width - placeable.width) / 2f).roundToInt()
            val centerY = ((height - placeable.height) / 2f).roundToInt()
            placeable.place(centerX, centerY)
        }
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? MinimumInteractiveComponentSizeModifier ?: return false
        return size == otherModifier.size
    }

    override fun hashCode(): Int {
        return size.hashCode()
    }
}

private val minimumInteractiveComponentSize: DpSize = DpSize(48.dp, 48.dp)
