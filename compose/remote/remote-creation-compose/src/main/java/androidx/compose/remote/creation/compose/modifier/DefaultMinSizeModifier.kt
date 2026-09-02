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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation.Type
import androidx.compose.remote.creation.compose.state.RemoteDp

private fun RemoteModifier.Element.isWidthConstrained(): Boolean =
    (this is WidthModifier && this.type != Type.WRAP) ||
        (this is WidthInModifier && this.min != null)

private fun RemoteModifier.Element.isHeightConstrained(): Boolean =
    (this is HeightModifier && this.type != Type.WRAP) ||
        (this is HeightInModifier && this.min != null)

/**
 * Constrain the size of the wrapped layout only when it would be otherwise unconstrained: the
 * [minWidth] and [minHeight] constraints are only applied when the corresponding dimension has not
 * been constrained by another modifier (e.g. [width], [height], [size], or [fillMaxWidth]).
 *
 * @param minWidth The minimum width to be used if width is otherwise unconstrained.
 * @param minHeight The minimum height to be used if height is otherwise unconstrained.
 */
public fun RemoteModifier.defaultMinSize(
    minWidth: RemoteDp? = null,
    minHeight: RemoteDp? = null,
): RemoteModifier {
    var result = this
    if (minWidth != null && !any { it.isWidthConstrained() }) {
        result = result.widthIn(min = minWidth)
    }
    if (minHeight != null && !any { it.isHeightConstrained() }) {
        result = result.heightIn(min = minHeight)
    }
    return result
}
