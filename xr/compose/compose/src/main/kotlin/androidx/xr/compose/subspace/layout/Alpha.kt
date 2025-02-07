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

import androidx.annotation.FloatRange
import androidx.xr.compose.subspace.node.SubspaceModifierElement

/**
 * Sets the opacity of this element (and its children) to a value between [0..1]. An alpha value of
 * 0.0f means fully transparent while a value of 1.0f is completely opaque. Elements with
 * semi-transparent alpha values (> 0.0 but < 1.0f) will be rendered using alpha-blending.
 *
 * @param alpha - Opacity of this element (and its children). Must be between `0` and `1`,
 *   inclusive. Values < `0` or > `1` will be clamped.
 */
public fun SubspaceModifier.alpha(
    @FloatRange(from = 0.0, to = 1.0) alpha: Float
): SubspaceModifier = this.then(AlphaElement(alpha))

private class AlphaElement(private var alpha: Float) : SubspaceModifierElement<AlphaNode>() {
    init {
        alpha = alpha.coerceIn(0.0f, 1.0f)
    }

    override fun create(): AlphaNode = AlphaNode(alpha)

    override fun update(node: AlphaNode) {
        node.alpha = alpha
    }

    override fun hashCode(): Int {
        return alpha.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlphaElement) return false

        return alpha == other.alpha
    }
}

private class AlphaNode(public var alpha: Float) : SubspaceModifier.Node(), CoreEntityNode {
    override fun modifyCoreEntity(coreEntity: CoreEntity) {
        coreEntity.setAlpha(alpha)
    }
}
