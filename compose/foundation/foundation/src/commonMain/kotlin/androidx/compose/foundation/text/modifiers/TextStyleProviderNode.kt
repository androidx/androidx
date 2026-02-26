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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.style.OuterNodeKey
import androidx.compose.foundation.style.StyleOuterNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.text.TextStyle
import kotlin.jvm.JvmInline

/**
 * Compose phase that is requesting the inherited [TextStyle]. In the [Layout] phase, only
 * [TextStyle] properties that affect layout are computed. All other properties are left as the
 * default values. For [Draw], only the properties that affect drawing text are computed. For [All],
 * all the properties are computed.
 */
@JvmInline
internal value class StylePhase private constructor(internal val value: Int) {
    companion object {
        /** A request to compute the inherited [TextStyle] properties that affect layout. */
        val Layout: StylePhase = StylePhase(1)

        /** A request to compute the inherited [TextStyle] properties that affect drawing. */
        val Draw: StylePhase = StylePhase(2)

        /** A request to compute all the inherited [TextStyle] properties. */
        val All: StylePhase = StylePhase(0.inv())
    }
}

/**
 * A [TextStyleProviderNode] is a modifier node that provides inherited text properties.
 *
 * Use [inheritedTextStyle] to find the node and request the inherited properties.
 */
internal interface TextStyleProviderNode : TraversableNode {
    fun computeInheritedTextStyle(phase: StylePhase, fallback: TextStyle): TextStyle
}

/**
 * Request the inherited [TextStyle]. This should be used in a node to determine the inherited text
 * style properties. This either returns the inherited text style or [fallback]. The specified
 * properties of [TextStyle] are will override the inherited values. That is, the [fallback] value
 * is merged with the inherited styles before being returned by this function.
 */
internal fun DelegatableNode.inheritedTextStyle(phase: StylePhase, fallback: TextStyle): TextStyle {
    var result: TextStyle = fallback
    traverseAncestors(OuterNodeKey) {
        if (it is StyleOuterNode) {
            result = it.computeInheritedTextStyle(phase, fallback)
            false
        } else true
    }
    return result
}
