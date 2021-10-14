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
import androidx.glance.Modifier

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EmittableColumn : EmittableWithChildren() {
    override var modifier: Modifier = Modifier
    public var verticalAlignment: Alignment.Vertical = Alignment.Top
    public var horizontalAlignment: Alignment.Horizontal = Alignment.Start
}

/** Scope defining modifiers only available on rows. */
public interface ColumnScope {
    /**
     * Size the element's height to split the available space with other weighted sibling elements
     * in the [Column]. The parent will divide the vertical space remaining after measuring
     * unweighted child elements and distribute it according to the weights, the default weight
     * being 1.
     */
    fun Modifier.defaultWeight(): Modifier
}

private object ColumnScopeImplInstance : ColumnScope {
    override fun Modifier.defaultWeight(): Modifier =
        this.then(HeightModifier(Dimension.Expand))
}

/**
 * A layout composable with [content], which lays its children out in a Column.
 *
 * By default, the [Column] will size itself to fit the content, unless a [Dimension] constraint has
 * been provided. When children are smaller than the size of the [Column], they will be placed
 * within the available space subject to [horizontalAlignment] and [verticalAlignment].
 *
 * @param modifier The modifier to be applied to the layout.
 * @param verticalAlignment The vertical alignment to apply to the set of children, when they do not
 *   consume the full height of the [Column] (i.e. whether to push the children towards the top,
 *   center or bottom of the [Column]).
 * @param horizontalAlignment The horizontal alignment to apply to children when they are smaller
 *  than the width of the [Column]
 * @param content The content inside the [Column]
 */
@Composable
public fun Column(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    ComposeNode<EmittableColumn, Applier>(
        factory = ::EmittableColumn,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(horizontalAlignment) { this.horizontalAlignment = it }
            this.set(verticalAlignment) { this.verticalAlignment = it }
        },
        content = { ColumnScopeImplInstance.content() }
    )
}
