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
    value =
        [
            StorageTextureAccess.BindingNotUsed,
            StorageTextureAccess.Undefined,
            StorageTextureAccess.WriteOnly,
            StorageTextureAccess.ReadOnly,
            StorageTextureAccess.ReadWrite,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The access mode for a storage texture binding in a shader. */
public annotation class StorageTextureAccess {
    public companion object {

        /** The binding is unused and should be ignored. */
        public const val BindingNotUsed: Int = 0x00000000

        /** An undefined storage texture access mode. */
        public const val Undefined: Int = 0x00000001

        /** The storage texture can only be written to by the shader. */
        public const val WriteOnly: Int = 0x00000002

        /** The storage texture can only be read from by the shader. */
        public const val ReadOnly: Int = 0x00000003

        /** The storage texture can be read from and written to by the shader. */
        public const val ReadWrite: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "BindingNotUsed",
                0x00000001 to "Undefined",
                0x00000002 to "WriteOnly",
                0x00000003 to "ReadOnly",
                0x00000004 to "ReadWrite",
            )

        public fun toString(@StorageTextureAccess value: Int): String =
            names[value] ?: value.toString()
    }
}
