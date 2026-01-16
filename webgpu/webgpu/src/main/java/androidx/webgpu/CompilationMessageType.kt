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
        [CompilationMessageType.Error, CompilationMessageType.Warning, CompilationMessageType.Info]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The severity level of a message generated during shader module compilation. */
public annotation class CompilationMessageType {
    public companion object {

        /** The message indicates an error (e.g., syntax error). */
        public const val Error: Int = 0x00000001

        /** The message indicates a warning (e.g., deprecated syntax). */
        public const val Warning: Int = 0x00000002

        /** The message provides general information. */
        public const val Info: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(0x00000001 to "Error", 0x00000002 to "Warning", 0x00000003 to "Info")

        public fun toString(@CompilationMessageType value: Int): String =
            names[value] ?: value.toString()
    }
}
