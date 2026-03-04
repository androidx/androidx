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

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

/** Defines size of a corner in pixels. For example for rounded shape it can be a corner radius. */
@Immutable
public interface RemoteCornerSize {

    /** Converts the [RemoteCornerSize] to pixels in RemoteFloat. */
    public fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat
}

/**
 * Creates [RemoteCornerSize] with provided size.
 *
 * @param size the corner size defined in [RemoteDp].
 */
public fun RemoteCornerSize(size: RemoteDp): RemoteCornerSize = RemoteDpCornerSize(size)

internal data class RemoteDpCornerSize(val size: RemoteDp) : RemoteCornerSize {
    override fun toString(): String = "CornerSize(size = ${size.value}.dp)"

    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat {
        return size.toPx(density)
    }
}

/**
 * Creates [RemoteCornerSize] with provided size.
 *
 * @param size the corner size defined in pixels.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteCornerSize(size: RemoteFloat): RemoteCornerSize = PxCornerSize(size)

internal data class PxCornerSize(val size: RemoteFloat) : RemoteCornerSize {
    override fun toString(): String = "CornerSize(size = $size.px)"

    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat {
        return size
    }
}

/**
 * Creates [RemoteCornerSize] with provided size.
 *
 * @param percent the corner size defined in percents of the shape's smaller side. Can't be negative
 *   or larger then 100 percents.
 */
public fun RemoteCornerSize(@IntRange(from = 0, to = 100) percent: Int): RemoteCornerSize =
    RemotePercentCornerSize(percent)

/**
 * Creates [RemoteCornerSize] with provided size.
 *
 * @param percent the corner size defined in float percents of the shape's smaller side. Can't be
 *   negative or larger then 100 percents.
 */
internal data class RemotePercentCornerSize(val percent: Int) : RemoteCornerSize {
    override fun toString(): String = "CornerSize(size = $percent%)"

    override fun toPx(shapeSize: RemoteSize, density: RemoteDensity): RemoteFloat {
        return shapeSize.minDimension * (percent.toFloat().rf / 100f)
    }
}
