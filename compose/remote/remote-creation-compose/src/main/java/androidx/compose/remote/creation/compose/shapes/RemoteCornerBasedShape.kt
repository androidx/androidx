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

package androidx.compose.remote.creation.compose.shapes

import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.ui.unit.LayoutDirection

/**
 * Base class for [RemoteShape]s defined by four [RemoteCornerSize]s.
 *
 * @param topStart a size of the top start corner
 * @param topEnd a size of the top end corner
 * @param bottomEnd a size of the bottom end corner
 * @param bottomStart a size of the bottom start corner
 * @see RemoteRoundedCornerShape for an example of the usage.
 */
public abstract class RemoteCornerBasedShape(
    public val topStart: RemoteCornerSize,
    public val topEnd: RemoteCornerSize,
    public val bottomEnd: RemoteCornerSize,
    public val bottomStart: RemoteCornerSize,
) : RemoteShape {

    final override fun createOutline(
        size: RemoteSize,
        density: RemoteDensity,
        layoutDirection: LayoutDirection,
    ): RemoteOutline = createOutline(size, density, layoutDirection, 0f.rf)

    /**
     * Creates a [RemoteOutline] for this shape, optionally configured for drawing a stroked border.
     *
     * @param size the outer size of the component boundary
     * @param density the remote density to apply to the shape
     * @param layoutDirection the current layout direction
     * @param strokeWidth the stroke width of the border (0 if drawing a solid background fill).
     *   When positive, each corner radius is inset by `strokeWidth / 2` and the outline bounds are
     *   inset by `strokeWidth / 2` to keep the centered stroke within component bounds.
     * @param offset the top-left offset of the outline (defaults to `(strokeWidth/2,
     *   strokeWidth/2)` for stroked borders)
     */
    public fun createOutline(
        size: RemoteSize,
        density: RemoteDensity,
        layoutDirection: LayoutDirection,
        strokeWidth: RemoteFloat = 0f.rf,
        offset: RemoteOffset = RemoteOffset(strokeWidth / 2f, strokeWidth / 2f),
    ): RemoteOutline {
        var topStart = topStart.toPx(size, density)
        var topEnd = topEnd.toPx(size, density)
        var bottomEnd = bottomEnd.toPx(size, density)
        var bottomStart = bottomStart.toPx(size, density)

        val minDimension = size.minDimension
        val shouldScaleStart = (topStart + bottomStart).isGreaterThan(minDimension)
        val shouldScaleEnd = (topEnd + bottomEnd).isGreaterThan(minDimension)
        val scaleStart = minDimension / (topStart + bottomStart)
        val scaleEnd = minDimension / (topEnd + bottomEnd)

        topStart = shouldScaleStart.select(ifTrue = topStart * scaleStart, ifFalse = topStart)
        bottomStart =
            shouldScaleStart.select(ifTrue = bottomStart * scaleStart, ifFalse = bottomStart)
        topEnd = shouldScaleEnd.select(ifTrue = topEnd * scaleEnd, ifFalse = topEnd)
        bottomEnd = shouldScaleEnd.select(ifTrue = bottomEnd * scaleEnd, ifFalse = bottomEnd)

        val halfStroke = strokeWidth / 2f
        topStart = max(topStart - halfStroke, 0f)
        topEnd = max(topEnd - halfStroke, 0f)
        bottomEnd = max(bottomEnd - halfStroke, 0f)
        bottomStart = max(bottomStart - halfStroke, 0f)

        return createOutline(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
            size = RemoteSize(size.width - strokeWidth, size.height - strokeWidth),
            offset = offset,
        )
    }

    /**
     * Creates [RemoteOutline] of this shape.
     *
     * @param topStart the resolved size of the top start corner
     * @param topEnd the resolved size for the top end corner
     * @param bottomEnd the resolved size for the bottom end corner
     * @param bottomStart the resolved size for the bottom start corner
     * @param size the resolved size of the shape outline
     * @param offset the top-left offset of the shape outline
     */
    public abstract fun createOutline(
        topStart: RemoteFloat,
        topEnd: RemoteFloat,
        bottomEnd: RemoteFloat,
        bottomStart: RemoteFloat,
        size: RemoteSize? = null,
        offset: RemoteOffset = RemoteOffset.Zero,
    ): RemoteOutline

    /**
     * Creates a copy of this Shape with new corner sizes.
     *
     * @param topStart a size of the top start corner
     * @param topEnd a size of the top end corner
     * @param bottomEnd a size of the bottom end corner
     * @param bottomStart a size of the bottom start corner
     */
    public abstract fun copy(
        topStart: RemoteCornerSize = this.topStart,
        topEnd: RemoteCornerSize = this.topEnd,
        bottomEnd: RemoteCornerSize = this.bottomEnd,
        bottomStart: RemoteCornerSize = this.bottomStart,
    ): RemoteCornerBasedShape
}
