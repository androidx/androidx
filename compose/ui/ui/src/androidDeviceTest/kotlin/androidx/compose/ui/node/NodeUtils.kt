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

package androidx.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.InspectorInfo

/**
 * Remove the root modifier nodes as they are not relevant from the perspective of the tests. There
 * are 3 nodes: RootModifierNode, FocusTargetNode, and DragAndDropNode.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun <T> List<T>.trimRootModifierNodes(): List<T> = dropLast(3)

internal fun Modifier.elementOf(node: Modifier.Node): Modifier {
    return this.then(ElementOf { node })
}

private data class ElementOf<T : Modifier.Node>(val factory: () -> T) : ModifierNodeElement<T>() {
    override fun create(): T = factory()

    override fun update(node: T) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "testNode"
    }
}

@Composable
internal fun ReverseMeasureLayout(modifier: Modifier, vararg contents: @Composable () -> Unit) =
    SubcomposeLayout(modifier) { constraints ->
        var layoutWidth = constraints.minWidth
        var layoutHeight = constraints.minHeight
        val subcomposes = mutableListOf<List<Placeable>>()

        // Measure in reverse order
        contents.reversed().forEachIndexed { index, content ->
            subcomposes.add(
                0,
                subcompose(index, content).map {
                    it.measure(constraints).also { placeable ->
                        layoutWidth = maxOf(layoutWidth, placeable.width)
                        layoutHeight = maxOf(layoutHeight, placeable.height)
                    }
                },
            )
        }

        layout(layoutWidth, layoutHeight) {

            // But place in direct order - it sets direct draw order
            subcomposes.forEach { placeables -> placeables.forEach { it.place(0, 0) } }
        }
    }
