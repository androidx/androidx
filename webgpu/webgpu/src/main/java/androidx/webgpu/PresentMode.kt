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
            PresentMode.Undefined,
            PresentMode.Fifo,
            PresentMode.FifoRelaxed,
            PresentMode.Immediate,
            PresentMode.Mailbox,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines how presented frames are swapped and displayed. */
public annotation class PresentMode {
    public companion object {

        /** An undefined present mode. */
        public const val Undefined: Int = 0x00000000

        /** Frames are presented at the vertical blanking interval (vsync). */
        public const val Fifo: Int = 0x00000001

        /** Similar to FIFO, but allows tearing if the application is too slow (lower latency). */
        public const val FifoRelaxed: Int = 0x00000002

        /** Frames are presented immediately, allowing tearing (lowest latency). */
        public const val Immediate: Int = 0x00000003

        /**
         * Allows the use of a queue to avoid blocking the application on vsync, similar to triple
         * buffering.
         */
        public const val Mailbox: Int = 0x00000004
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "Fifo",
                0x00000002 to "FifoRelaxed",
                0x00000003 to "Immediate",
                0x00000004 to "Mailbox",
            )

        public fun toString(@PresentMode value: Int): String = names[value] ?: value.toString()
    }
}
