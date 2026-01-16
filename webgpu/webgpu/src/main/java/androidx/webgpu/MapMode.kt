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
@IntDef(flag = true, value = [MapMode.None, MapMode.Read, MapMode.Write])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** Flags specifying the desired access mode when mapping a buffer. */
public annotation class MapMode {
    public companion object {

        /** No access mode specified. */
        public const val None: Int = 0x00000000
        public const val Read: Int = 0x00000001
        public const val Write: Int = 0x00000002
        internal val names: Map<Int, String> =
            mapOf(0x00000000 to "None", 0x00000001 to "Read", 0x00000002 to "Write")

        public fun toString(@MapMode value: Int): String = names[value] ?: value.toString()
    }
}
