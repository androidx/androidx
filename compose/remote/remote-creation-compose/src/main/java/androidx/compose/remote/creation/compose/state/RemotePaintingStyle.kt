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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

/** Strategies for painting shapes and paths on a remote canvas. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemotePaintingStyle internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Fill -> "Fill"
            Stroke -> "Stroke"
            else -> "Unknown"
        }

    public companion object {
        /** Apply the paint to the inside of the shape. */
        @Stable public val Fill: RemotePaintingStyle = RemotePaintingStyle(PaintBundle.STYLE_FILL)

        /** Apply the paint to the edge of the shape. */
        @Stable
        public val Stroke: RemotePaintingStyle = RemotePaintingStyle(PaintBundle.STYLE_STROKE)
    }
}
