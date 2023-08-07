/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import kotlin.test.Test
import kotlin.test.assertEquals

class StringFormatTest {

    @Test
    fun no_args() {
        val result = "str".format()
        assertEquals("str", result)
    }

    @Test
    fun one_arg() {
        val result = "str %1\$".format(1)
        assertEquals("str 1", result)
    }

    @Test
    fun two_args() {
        val result = "str %1\$ %2\$".format(1, 2)
        assertEquals("str 1 2", result)
    }

    @Test
    fun too_many_args() {
        val result = "str %1\$".format(1, 2)
        assertEquals("str 1", result)
    }

    @Test
    fun not_enough_args() {
        // Behavior is not specified. We can consider different variants.
        val result = "str %1\$".format()
        assertEquals("str %1\$", result)
    }

}
