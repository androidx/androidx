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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.BlendMode

/** Represents a color filter that can be used with remote paint. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public sealed interface RemoteColorFilter

/**
 * A remote-compatible color filter that applies a blend mode with a specified color.
 *
 * @property color The [RemoteColor] to use for the tint.
 * @property blendMode The [BlendMode] to use for blending.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBlendModeColorFilter(
    public val color: RemoteColor,
    public val blendMode: BlendMode,
) : RemoteColorFilter {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteBlendModeColorFilter) return false
        return color == other.color && blendMode == other.blendMode
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + blendMode.hashCode()
        return result
    }
}

/**
 * A remote-compatible color filter that wraps a Compose [androidx.compose.ui.graphics.ColorFilter].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeRemoteColorFilter(
    public val composeColorFilter: androidx.compose.ui.graphics.ColorFilter
) : RemoteColorFilter {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComposeRemoteColorFilter) return false
        return composeColorFilter == other.composeColorFilter
    }

    override fun hashCode(): Int {
        return composeColorFilter.hashCode()
    }
}
