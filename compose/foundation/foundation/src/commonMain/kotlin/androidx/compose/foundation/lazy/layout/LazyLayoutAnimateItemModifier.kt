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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset

internal data class LazyLayoutAnimateItemElement(
    private val enterTransition: EnterTransition?,
    private val exitTransition: ExitTransition?,
    private val placementSpec: FiniteAnimationSpec<IntOffset>?,
) : ModifierNodeElement<LazyLayoutAnimationSpecsNode>() {
    constructor(
        fadeInSpec: FiniteAnimationSpec<Float>?,
        placementSpec: FiniteAnimationSpec<IntOffset>?,
        fadeOutSpec: FiniteAnimationSpec<Float>?,
    ) : this(
        enterTransition = fadeInSpec?.let { fadeIn(animationSpec = it, initialAlpha = 0f) },
        exitTransition = fadeOutSpec?.let { fadeOut(animationSpec = it, targetAlpha = 0f) },
        placementSpec = placementSpec,
    )

    override fun create(): LazyLayoutAnimationSpecsNode =
        LazyLayoutAnimationSpecsNode(enterTransition, exitTransition, placementSpec)

    override fun update(node: LazyLayoutAnimationSpecsNode) {
        node.enterTransition = enterTransition
        node.exitTransition = exitTransition
        node.placementSpec = placementSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateItem"
        properties["enterTransition"] = enterTransition
        properties["exitTransition"] = exitTransition
        properties["placementSpec"] = placementSpec
    }
}

internal class LazyLayoutAnimationSpecsNode(
    var enterTransition: EnterTransition?,
    var exitTransition: ExitTransition?,
    var placementSpec: FiniteAnimationSpec<IntOffset>?,
) : Modifier.Node(), ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?): Any = this@LazyLayoutAnimationSpecsNode
}
