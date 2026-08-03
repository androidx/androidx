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

package androidx.navigation3.runtime

import androidx.kruth.assertThat
import kotlin.test.Test

class NavEntryTest {

    @Test
    fun getContentKey_dataObject() {
        val entry1 = NavEntry(TestSubinterface1.Home) {}
        val entry2 = NavEntry(TestSubinterface2.Home) {}
        println("cfok android ${entry1.contentKey}")

        assertThat(entry1.contentKey).isNotEqualTo(entry2.contentKey)
    }

    @Test
    fun getContentKey_dataClass() {
        val key1 = TestDataClass(1)
        val key2 = TestDataClass(2)
        val entry1 = NavEntry(key1) {}
        val entry2 = NavEntry(key2) {}
        assertThat(entry1.contentKey).isNotEqualTo(entry2.contentKey)
    }

    private interface TestInterface

    private interface TestSubinterface1 : TestInterface {
        data object Home : TestSubinterface1
    }

    private interface TestSubinterface2 : TestInterface {
        data object Home : TestSubinterface2
    }

    data class TestDataClass(val arg: Int)
}
