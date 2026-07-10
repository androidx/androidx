/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

internal data class LazyLayoutAnimateItemElement(
    private val fadeInSpec: FiniteAnimationSpec<Float>?,
    private val placementSpec: FiniteAnimationSpec<IntOffset>?,
    private val fadeOutSpec: FiniteAnimationSpec<Float>?,
) : ModifierNodeElement<LazyLayoutAnimationSpecsNode>() {

    override fun create(): LazyLayoutAnimationSpecsNode =
        LazyLayoutAnimationSpecsNode(fadeInSpec, placementSpec, fadeOutSpec)

    override fun update(node: LazyLayoutAnimationSpecsNode) {
        node.fadeInSpec = fadeInSpec
        node.placementSpec = placementSpec
        node.fadeOutSpec = fadeOutSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateItem"
        properties["fadeInSpec"] = fadeInSpec
        properties["placementSpec"] = placementSpec
        properties["fadeOutSpec"] = fadeOutSpec
    }
}

internal class LazyLayoutAnimationSpecsNode(
    var fadeInSpec: FiniteAnimationSpec<Float>?,
    var placementSpec: FiniteAnimationSpec<IntOffset>?,
    var fadeOutSpec: FiniteAnimationSpec<Float>?,
) : Modifier.Node(), ParentDataModifierNode {

    override fun Density.modifyParentData(parentData: Any?): Any = this@LazyLayoutAnimationSpecsNode
}
