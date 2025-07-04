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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Allow specifying semantic properties on a curved component. Note that currently only
 * [contentDescription] and [traversalIndex] are supported, and they can be applied to curved text
 * and curvedComposable
 *
 * Sample for setting content description and traversal order:
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedSemanticsSample
 * @param properties The properties to apply, [SemanticsPropertyReceiver] will be provided in the
 *   scope to allow access for common properties and its values.
 */
public fun CurvedModifier.semantics(properties: CurvedSemanticsScope.() -> Unit): CurvedModifier =
    this.then { child ->
        SemanticWrapper(child = child, isClearingSemantics = false, properties = properties)
    }

/**
 * Allow specifying semantic properties on a curved component, and clearing the existing properties.
 * Note that currently only [contentDescription] and [traversalIndex] are supported, and they can be
 * applied to curved text and curvedComposable
 *
 * Sample for clearing semantics:
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedClearSemanticsSample
 * @param properties The properties to apply, [SemanticsPropertyReceiver] will be provided in the
 *   scope to allow access for common properties and its values.
 */
public fun CurvedModifier.clearAndSetSemantics(
    properties: CurvedSemanticsScope.() -> Unit
): CurvedModifier =
    this.then { child ->
        SemanticWrapper(child = child, isClearingSemantics = true, properties = properties)
    }

/**
 * CurvedSemanticsScope is the scope provided by semantics lambda blocks, letting you set semantic
 * properties.
 */
public class CurvedSemanticsScope {
    /**
     * Developer-set content description of the semantics node.
     *
     * If this is not set, accessibility services will present the text of this node as the content.
     */
    public var contentDescription: String? = null

    /**
     * A value to manually control screenreader traversal order.
     *
     * This API can be used to customize TalkBack traversal order. When the `traversalIndex`
     * property is set on a traversalGroup or on a screenreader-focusable node, then the sorting
     * algorithm will prioritize nodes with smaller `traversalIndex`s earlier. The default
     * traversalIndex value is zero, and traversalIndices are compared at a peer level.
     *
     * For example,` traversalIndex = -1f` can be used to force a top bar to be ordered earlier, and
     * `traversalIndex = 1f` to make a bottom bar ordered last, in the edge cases where this does
     * not happen by default. As another example, if you need to reorder two Buttons within a Row,
     * then you can set `isTraversalGroup = true` on the Row, and set `traversalIndex` on one of the
     * Buttons.
     *
     * Note that if `traversalIndex` seems to have no effect, be sure to set `isTraversalGroup =
     * true` as well in a parent node.
     */
    public var traversalIndex: Float = Float.NaN
}

internal class SemanticWrapper(
    child: CurvedChild,
    private val isClearingSemantics: Boolean,
    private val properties: CurvedSemanticsScope.() -> Unit,
) : BaseCurvedChildWrapper(child) {
    @Composable
    override fun SubComposition(semanticProperties: CurvedSemanticProperties) {
        // Call properties here so we are recomposed when properties change
        val scope = CurvedSemanticsScope().apply(properties)
        wrapped.SubComposition(
            semanticProperties.merge(
                CurvedSemanticProperties(
                    scope.contentDescription,
                    scope.traversalIndex,
                    isClearingSemantics,
                )
            )
        )
    }
}
