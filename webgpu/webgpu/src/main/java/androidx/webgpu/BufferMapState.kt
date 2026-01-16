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
@IntDef(value = [BufferMapState.Unmapped, BufferMapState.Pending, BufferMapState.Mapped])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The current state of a GPU buffer's mapping. */
public annotation class BufferMapState {
    public companion object {

        /** The buffer is not mapped. */
        public const val Unmapped: Int = 0x00000001

        /** A mapAsync operation is in progress. */
        public const val Pending: Int = 0x00000002

        /** The buffer is currently mapped and accessible by the CPU. */
        public const val Mapped: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(0x00000001 to "Unmapped", 0x00000002 to "Pending", 0x00000003 to "Mapped")

        public fun toString(@BufferMapState value: Int): String = names[value] ?: value.toString()
    }
}
