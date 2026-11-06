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

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

/** Rule used to determine how the interior of a path is calculated. */
@Immutable
@JvmInline
public value class RemotePathFillType internal constructor(internal val value: Int) {
    override fun toString(): String =
        when (this) {
            NonZero -> "NonZero"
            EvenOdd -> "EvenOdd"
            else -> "Unknown"
        }

    public companion object {
        /** The interior is defined by a non-zero sum of signed edge crossings. */
        public val NonZero: RemotePathFillType = RemotePathFillType(0)

        /** The interior is defined by an odd number of edge crossings. */
        public val EvenOdd: RemotePathFillType = RemotePathFillType(1)
    }
}
