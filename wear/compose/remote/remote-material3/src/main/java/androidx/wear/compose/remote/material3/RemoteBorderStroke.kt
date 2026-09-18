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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.runtime.Immutable

/**
 * Represents a border stroke style to be drawn around a Remote Compose component.
 *
 * @param width The stroke width of the border.
 * @param brush The [RemoteBrush] to be used to draw the border stroke.
 */
@Immutable
public class RemoteBorderStroke(
    public val width: RemoteDp,
    public val brush: RemoteBrush,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteBorderStroke) return false
        return width == other.width && brush == other.brush
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + brush.hashCode()
        return result
    }

    override fun toString(): String = "RemoteBorderStroke(width=$width, brush=$brush)"

    /** Creates a copy of this [RemoteBorderStroke] with the specified [width] and [brush]. */
    public fun copy(
        width: RemoteDp = this.width,
        brush: RemoteBrush = this.brush,
    ): RemoteBorderStroke = RemoteBorderStroke(width, brush)
}

/**
 * Creates a [RemoteBorderStroke] with a solid [color].
 *
 * @param width The stroke width of the border.
 * @param color The [RemoteColor] of the border.
 */
public fun RemoteBorderStroke(width: RemoteDp, color: RemoteColor): RemoteBorderStroke =
    RemoteBorderStroke(width, RemoteBrush.solidColor(color))
