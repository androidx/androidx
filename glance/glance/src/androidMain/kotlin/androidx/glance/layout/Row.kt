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

package androidx.glance.layout

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmittableRow : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier
    public var horizontalAlignment: Alignment.Horizontal = Alignment.Start
    public var verticalAlignment: Alignment.Vertical = Alignment.Top
}

/** Scope defining modifiers only available on rows. */
public interface RowScope {
    /**
     * Size the element's width to split the available space with other weighted sibling elements
     * in the [Row]. The parent will divide the horizontal space remaining after measuring
     * unweighted child elements and distribute it according to the weights, the default weight
     * being 1.
     */
    fun GlanceModifier.defaultWeight(): GlanceModifier
}

private object RowScopeImplInstance : RowScope {
    override fun GlanceModifier.defaultWeight(): GlanceModifier {
        return this.then(WidthModifier(Dimension.Expand))
    }
}

/**
 * A layout composable with [content], which lays its children out in a Row.
 *
 * By default, the [Row] will size itself to fit the content, unless a [Dimension] constraint has
 * been provided. When children are smaller than the size of the [Row], they will be placed
 * within the available space subject to [verticalAlignment] and [horizontalAlignment].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param horizontalAlignment The horizontal alignment to apply to the set of children, when they do
 *   not consume the full width of the [Row] (i.e. whether to push the children towards the start,
 *   center or end of the [Row]).
 * @param verticalAlignment The horizontal alignment to apply to children when they are smaller
 *  than the height of the [Row]
 * @param content The content inside the [Row]
 */
@Composable
public fun Row(
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    ComposeNode<EmittableRow, Applier>(
        factory = ::EmittableRow,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(verticalAlignment) { this.verticalAlignment = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
        },
        content = { RowScopeImplInstance.content() }
    )
}
