/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.window.layout.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CombineLatestConsumerAdapterTest {

    @Test
    fun testCombineLatest() {
        var result: String? = null
        val adapter =
            CombineLatestConsumerAdapter<Int, String, String>({ i, s -> "$i$s" }, { result = it })

        adapter.consumerT.accept(1)
        assertNull(result)

        adapter.consumerU.accept("a")
        assertEquals("1a", result)

        adapter.consumerT.accept(2)
        assertEquals("2a", result)

        adapter.consumerU.accept("b")
        assertEquals("2b", result)
    }
}
