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

/** Defines what happens when an edge of a shader falls within the shape being painted. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
@kotlin.jvm.JvmInline
public value class RemoteTileMode internal constructor(public val value: Int) {
    override fun toString(): String =
        when (this) {
            Clamp -> "Clamp"
            Repeated -> "Repeated"
            Mirror -> "Mirror"
            Decal -> "Decal"
            else -> "Unknown"
        }

    public companion object {
        /** Edge is clamped to the edge of the shader. */
        public val Clamp: RemoteTileMode = RemoteTileMode(0)

        /** Shader is repeated horizontally and vertically. */
        public val Repeated: RemoteTileMode = RemoteTileMode(1)

        /** Shader is repeated horizontally and vertically, alternating mirror images. */
        public val Mirror: RemoteTileMode = RemoteTileMode(2)

        /**
         * Render the shader only within the original bounds, leaving outer areas transparent black.
         */
        public val Decal: RemoteTileMode = RemoteTileMode(3)
    }
}
