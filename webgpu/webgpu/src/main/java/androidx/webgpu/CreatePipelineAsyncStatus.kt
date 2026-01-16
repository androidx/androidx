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
            CreatePipelineAsyncStatus.Success,
            CreatePipelineAsyncStatus.CallbackCancelled,
            CreatePipelineAsyncStatus.ValidationError,
            CreatePipelineAsyncStatus.InternalError,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Status codes for asynchronous pipeline creation operations. */
public annotation class CreatePipelineAsyncStatus {
    public companion object {

        /** The pipeline was successfully created. */
        public const val Success: Int = 0x00000001

        /** The asynchronous creation operation was cancelled. */
        public const val CallbackCancelled: Int = 0x00000002

        /** The pipeline descriptor contained invalid data. */
        public const val ValidationError: Int = 0x00000003

        /** An unexpected error occurred within the implementation. */
        public const val InternalError: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "Success",
                0x00000002 to "CallbackCancelled",
                0x00000003 to "ValidationError",
                0x00000004 to "InternalError",
            )

        public fun toString(@CreatePipelineAsyncStatus value: Int): String =
            names[value] ?: value.toString()
    }
}
