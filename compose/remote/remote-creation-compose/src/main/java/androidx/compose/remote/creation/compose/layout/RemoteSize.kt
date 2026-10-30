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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

/**
 * An immutable, 2D floating-point size with [width] and [height] represented as [RemoteFloat]s.
 *
 * This class is used in remote creation to represent dimensions that may be backed by remote state.
 *
 * @property width The horizontal dimension.
 * @property height The vertical dimension.
 */
@Immutable
public class RemoteSize(
    public val width: RemoteFloat,
    public val height: RemoteFloat,
) {

    /**
     * Returns a [RemoteSize] with the width and height decreased by [offset]'s x and y coordinates.
     *
     * @param offset The [RemoteOffset] to subtract from this size.
     */
    public fun offsetSize(offset: RemoteOffset): RemoteSize {
        return RemoteSize(width - offset.x, height - offset.y)
    }

    /** The lesser of the magnitudes of the [width] and the [height]. */
    public val minDimension: RemoteFloat
        get() = width.min(height)

    /** The offset to the center of this [RemoteSize]. */
    public val center: RemoteOffset
        get() = RemoteOffset(width / 2f, height / 2f)

    public companion object {
        /** A [RemoteSize] with [width] and [height] set to 0. */
        public val Zero: RemoteSize = RemoteSize(0f.rf, 0f.rf)
    }
}
