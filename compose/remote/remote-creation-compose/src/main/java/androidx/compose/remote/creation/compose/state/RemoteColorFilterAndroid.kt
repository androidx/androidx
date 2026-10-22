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

package androidx.compose.remote.creation.compose.state

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.toRemoteBlendMode
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.toArgb

/**
 * A remote-compatible color filter that wraps a Compose [androidx.compose.ui.graphics.ColorFilter].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeRemoteColorFilter(
    public val composeColorFilter: androidx.compose.ui.graphics.ColorFilter
) : RemoteColorFilter {
    override fun applyToPaintBundle(
        paintBundle: PaintBundle,
        creationState: RemoteComposeCreationState,
    ) {
        val colorFilter = composeColorFilter
        if (colorFilter is BlendModeColorFilter) {
            paintBundle.setColorFilter(
                colorFilter.color.toArgb(),
                colorFilter.blendMode.toRemoteBlendMode().value,
            )
        } else {
            TODO("Color filter not supported: $colorFilter")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComposeRemoteColorFilter) return false
        return composeColorFilter == other.composeColorFilter
    }

    override fun hashCode(): Int {
        return composeColorFilter.hashCode()
    }
}

/** Creates a [RemoteBlendModeColorFilter] using Compose UI [BlendMode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteBlendModeColorFilter(
    color: RemoteColor,
    blendMode: BlendMode,
): RemoteBlendModeColorFilter = RemoteBlendModeColorFilter(color, blendMode.toRemoteBlendMode())
