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
        [CallbackMode.WaitAnyOnly, CallbackMode.AllowProcessEvents, CallbackMode.AllowSpontaneous]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines how pending callbacks are handled in the immediate mode. */
public annotation class CallbackMode {
    public companion object {

        /** Only wait for the specific future, no event processing. */
        public const val WaitAnyOnly: Int = 0x00000001

        /** Allow processing of events and invoking callbacks during wait. */
        public const val AllowProcessEvents: Int = 0x00000002

        /** Allow processing of spontaneous events (like uncaptured errors) during wait. */
        public const val AllowSpontaneous: Int = 0x00000003
        internal val names: Map<Int, String> =
            mapOf(
                0x00000001 to "WaitAnyOnly",
                0x00000002 to "AllowProcessEvents",
                0x00000003 to "AllowSpontaneous",
            )

        public fun toString(@CallbackMode value: Int): String = names[value] ?: value.toString()
    }
}
