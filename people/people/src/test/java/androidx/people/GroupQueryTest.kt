/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GroupQueryTest {

    @Test
    fun groupQuery_exposesEveryCriterionItWasGiven() {
        val query =
            groupQuery("com.example.chat") {
                addLookupKeys("g1", "g2")
            }

        assertThat(query.directoryPackage).isEqualTo("com.example.chat")
        assertThat(query.lookupKeys).containsExactly("g1", "g2").inOrder()
    }

    @Test
    fun groupQuery_accumulatesRepeatedCalls() {
        val query =
            groupQuery("com.example.chat") {
                addLookupKeys("g1")
                addLookupKeys("g2")
            }

        assertThat(query.lookupKeys).containsExactly("g1", "g2").inOrder()
    }

    @Test
    fun addLookupKeys_withNoValues_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            groupQuery("com.example.chat") {
                addLookupKeys()
            }
        }
    }

    @Test
    fun groupQuery_withoutCriteria_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            groupQuery("com.example.chat") {}
        }
    }

    @Test
    fun addLookupKeys_withCollection_setsLookupKeys() {
        val query =
            groupQuery("com.example.chat") {
                addLookupKeys(listOf("g1", "g2"))
            }

        assertThat(query.lookupKeys).containsExactly("g1", "g2").inOrder()
    }
}
