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

package androidx.webgpu

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention(AnnotationRetention.SOURCE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@IntDef(
    flag = true,
    value =
        [
            TextureUsage.None,
            TextureUsage.CopySrc,
            TextureUsage.CopyDst,
            TextureUsage.TextureBinding,
            TextureUsage.StorageBinding,
            TextureUsage.RenderAttachment,
        ],
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Flags specifying the intended usages of a GPU texture. */
public annotation class TextureUsage {
    public companion object {

        /** No specified usage. */
        public const val None: Int = 0x00000000

        /** Allows the texture to be used as a source for copy operations. */
        public const val CopySrc: Int = 0x00000001

        /** Allows the texture to be used as a destination for copy operations. */
        public const val CopyDst: Int = 0x00000002

        /** Allows the texture to be used as a sampled texture in a shader. */
        public const val TextureBinding: Int = 0x00000004

        /** Allows the texture to be used as a storage texture in a shader. */
        public const val StorageBinding: Int = 0x00000008

        /**
         * Allows the texture to be used as a color or depth/stencil attachment in a render pass.
         */
        public const val RenderAttachment: Int = 0x00000010
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "None",
                0x00000001 to "CopySrc",
                0x00000002 to "CopyDst",
                0x00000004 to "TextureBinding",
                0x00000008 to "StorageBinding",
                0x00000010 to "RenderAttachment",
            )

        public fun toString(@TextureUsage value: Int): String = names[value] ?: value.toString()
    }
}
