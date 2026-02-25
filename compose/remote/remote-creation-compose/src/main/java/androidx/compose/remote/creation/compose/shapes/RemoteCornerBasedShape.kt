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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.shapes

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    ): RemoteOutline {
        var topStart = topStart.toPx(size, density)
        var topEnd = topEnd.toPx(size, density)
        var bottomEnd = bottomEnd.toPx(size, density)
        var bottomStart = bottomStart.toPx(size, density)

        val minDimension = size.minDimension
        val shouldScaleStart = (topStart + bottomStart).gt(minDimension)
        val shouldScaleEnd = (topEnd + bottomEnd).gt(minDimension)
        val scaleStart = minDimension / (topStart + bottomStart)
        val scaleEnd = minDimension / (topEnd + bottomEnd)

        topStart = shouldScaleStart.select(ifTrue = topStart * scaleStart, ifFalse = topStart)
        bottomStart =
            shouldScaleStart.select(ifTrue = bottomStart * scaleStart, ifFalse = bottomStart)
        topEnd = shouldScaleEnd.select(ifTrue = topEnd * scaleEnd, ifFalse = topEnd)
        bottomEnd = shouldScaleEnd.select(ifTrue = bottomEnd * scaleEnd, ifFalse = bottomEnd)

        return createOutline(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
        )
    }

    /**
     * Creates [RemoteOutline] of this shape.
     *
     * @param topStart the resolved size of the top start corner
     * @param topEnd the resolved size for the top end corner
     * @param bottomEnd the resolved size for the bottom end corner
     * @param bottomStart the resolved size for the bottom start corner
     */
    public abstract fun createOutline(
        topStart: RemoteFloat,
        topEnd: RemoteFloat,
        bottomEnd: RemoteFloat,
        bottomStart: RemoteFloat,
    ): RemoteOutline
}
