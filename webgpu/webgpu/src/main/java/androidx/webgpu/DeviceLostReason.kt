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
            DeviceLostReason.Unknown,
            DeviceLostReason.Destroyed,
            DeviceLostReason.CallbackCancelled,
            DeviceLostReason.FailedCreation,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Specifies the reason a GPU device was lost and became unusable. */
public annotation class DeviceLostReason {
    public companion object {

        /** The reason for the loss is unknown. */
        public const val Unknown: Int = 0x00000001

        /** The application explicitly called destroy() on the device. */
        public const val Destroyed: Int = 0x00000002

        /** The device loss callback was cancelled. */
        public const val CallbackCancelled: Int = 0x00000003

        /** The device failed to be created correctly. */
        public const val FailedCreation: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "Unknown",
                0x00000002 to "Destroyed",
                0x00000003 to "CallbackCancelled",
                0x00000004 to "FailedCreation",
            )

        public fun toString(@DeviceLostReason value: Int): String = names[value] ?: value.toString()
    }
}
