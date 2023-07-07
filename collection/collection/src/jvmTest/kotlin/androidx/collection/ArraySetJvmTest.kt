/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection

import kotlin.test.assertNull
import org.junit.Test

class ArraySetJvmTest {

    @Test
    fun toArray_emptyTypedDestination() {
        val set = ArraySet<Int>()
        for (i in 0..5) {
            set.add(i)
        }

        // Forces casting, otherwise may not pick up certain failures. Typing just the destination
        // array or return type is not sufficient to test on JVM.
        @Suppress("UNUSED_VARIABLE")
        val result: Array<Int> = set.toArray(emptyArray())
    }

    @Test
    fun toArray_nullsLastElement() {
        val set = ArraySet<Int>()
        for (i in 0..4) {
            set.add(i)
        }

        val result: Array<Int> = set.toArray(Array(10) { -1 })
        assertNull(result[5])
    }
}
