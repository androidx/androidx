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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.jvm.JvmInline

/** Styles to use for line segment joins. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@JvmInline
public value class RemoteStrokeJoin internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Miter -> "Miter"
            Round -> "Round"
            Bevel -> "Bevel"
            else -> "Unknown"
        }

    public companion object {
        /** Joins between line segments form sharp corners. */
        @Stable public val Miter: RemoteStrokeJoin = RemoteStrokeJoin(0)

        /** Joins between line segments are rounded. */
        @Stable public val Round: RemoteStrokeJoin = RemoteStrokeJoin(1)

        /**
         * Joins between line segments connect the outer corners of each segment with a straight
         * line.
         */
        @Stable public val Bevel: RemoteStrokeJoin = RemoteStrokeJoin(2)
    }
}
