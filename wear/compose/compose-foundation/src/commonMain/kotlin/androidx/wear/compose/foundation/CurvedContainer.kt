/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable

/**
 * Layout scope used for curved containers.
 * This is the base of a DSL that specifies what components can be added to a curved layout.
 */
@LayoutScopeMarker
public class CurvedScope internal constructor(
    internal val curvedLayoutDirection: CurvedLayoutDirection
) {
    internal val nodes = mutableListOf<CurvedChild>()
    internal fun add(node: CurvedChild, modifier: CurvedModifier) {
        nodes.add(modifier.wrap(node))
    }
    internal fun initialize(contentBuilder: CurvedScope.() -> Unit) {
        nodes.clear()
        apply(contentBuilder)
    }
}

/**
 * Base class for sub-layouts
 */
internal abstract class ContainerChild(
    curvedLayoutDirection: CurvedLayoutDirection,
    internal val reverseLayout: Boolean,
    private val contentBuilder: CurvedScope.() -> Unit
) : CurvedChild() {
    private val curvedContainerScope = CurvedScope(curvedLayoutDirection)
    internal val children get() = curvedContainerScope.nodes

    internal val childrenInLayoutOrder get() = children.indices.map { ix ->
        children[if (reverseLayout) children.size - 1 - ix else ix]
    }

    @Composable
    override fun SubComposition() {
        // Ensure we subscribe this composition to any state reads on contentBuilder,
        // and we keep our internal tree in sync with compose's tree.
        curvedContainerScope.initialize(contentBuilder)
        children.forEach {
            it.SubComposition()
        }
    }

    override fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ) = children.fold(index) { currentIndex, node ->
            with(node) {
                initializeMeasure(measurables, currentIndex)
            }
        }

    override fun DrawScope.draw() = children.forEach { with(it) { draw() } }

    override fun (Placeable.PlacementScope).placeIfNeeded() {
        children.forEach { with(it) { placeIfNeeded() } }
    }
}
