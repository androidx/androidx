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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceModifierElement

/**
 * Scale the contents of the composable by the scale factor along horizontal, vertical, and depth
 * axes. Scaling does not change the measured size of the composable content during layout. Measured
 * size of @SubspaceComposable elements can be controlled using Size Modifiers. Scale factor should
 * be a positive number.
 *
 * @param scale - Multiplier to scale content along vertical, horizontal, depth axes.
 */
public fun SubspaceModifier.scale(scale: Float): SubspaceModifier = this.then(ScaleElement(scale))

private class ScaleElement(private val scale: Float) : SubspaceModifierElement<ScaleNode>() {

    init {
        require(scale > 0.0f) { "scale values must be > 0.0f" }
    }

    override fun create(): ScaleNode = ScaleNode(scale)

    override fun update(node: ScaleNode) {
        node.scale = scale
    }

    override fun hashCode(): Int {
        return scale.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScaleElement) return false

        return scale == other.scale
    }
}

private class ScaleNode(public var scale: Float) : SubspaceModifier.Node(), CoreEntityNode {
    override fun modifyCoreEntity(coreEntity: CoreEntity) {
        coreEntity.scale = scale
    }
}
