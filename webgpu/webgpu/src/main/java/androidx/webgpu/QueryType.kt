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
@IntDef(value = [QueryType.Occlusion, QueryType.Timestamp])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)

/** The type of information recorded by a query set. */
public annotation class QueryType {
    public companion object {

        /** Measures the number of samples or primitives that pass depth/stencil tests. */
        public const val Occlusion: Int = 0x00000001

        /** Records a GPU timestamp at a point in the command stream. */
        public const val Timestamp: Int = 0x00000002
        internal val names: Map<Int, String> =
            mapOf(0x00000001 to "Occlusion", 0x00000002 to "Timestamp")

        public fun toString(@QueryType value: Int): String = names[value] ?: value.toString()
    }
}
