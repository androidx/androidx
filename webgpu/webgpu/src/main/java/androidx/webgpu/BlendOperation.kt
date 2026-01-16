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
            BlendOperation.Undefined,
            BlendOperation.Add,
            BlendOperation.Subtract,
            BlendOperation.ReverseSubtract,
            BlendOperation.Min,
            BlendOperation.Max,
        ]
)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Defines the mathematical operation to be performed when blending color components. */
public annotation class BlendOperation {
    public companion object {

        /** An undefined blend operation. */
        public const val Undefined: Int = 0x00000000

        /** The source and destination color components are added. */
        public const val Add: Int = 0x00000001

        /** The destination color component is subtracted from the source color component. */
        public const val Subtract: Int = 0x00000002

        /** The source color component is subtracted from the destination color component. */
        public const val ReverseSubtract: Int = 0x00000003

        /** The minimum of the source and destination color components is taken. */
        public const val Min: Int = 0x00000004

        /** The maximum of the source and destination color components is taken. */
        public const val Max: Int = 0x00000005
        internal val names: Map<Int, String> =
            mapOf(
                0x00000000 to "Undefined",
                0x00000001 to "Add",
                0x00000002 to "Subtract",
                0x00000003 to "ReverseSubtract",
                0x00000004 to "Min",
                0x00000005 to "Max",
            )

        public fun toString(@BlendOperation value: Int): String = names[value] ?: value.toString()
    }
}
