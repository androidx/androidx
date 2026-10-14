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
class PersonQueryTest {

    @Test
    fun personQuery_exposesEveryCriterionItWasGiven() {
        val query =
            personQuery("com.example.voip") {
                addPhones("+16505550100", "+16505550101")
                addEmails("alice@example.com")
                addLookupKeys("user_1")
                addGroupLookupKeys("group_1")
            }

        assertThat(query.directoryPackage).isEqualTo("com.example.voip")
        assertThat(query.phones).containsExactly("+16505550100", "+16505550101").inOrder()
        assertThat(query.emails).containsExactly("alice@example.com")
        assertThat(query.lookupKeys).containsExactly("user_1")
        assertThat(query.groupLookupKeys).containsExactly("group_1")
    }

    @Test
    fun personQuery_accumulatesRepeatedCalls() {
        val query =
            personQuery("com.example.voip") {
                addPhones("+16505550100")
                addPhones("+16505550101")
                addEmails("alice@example.com")
                addEmails("bob@example.com")
                addLookupKeys("p1")
                addLookupKeys("p2")
                addGroupLookupKeys("g1")
                addGroupLookupKeys("g2")
            }

        assertThat(query.phones).containsExactly("+16505550100", "+16505550101").inOrder()
        assertThat(query.emails).containsExactly("alice@example.com", "bob@example.com").inOrder()
        assertThat(query.lookupKeys).containsExactly("p1", "p2").inOrder()
        assertThat(query.groupLookupKeys).containsExactly("g1", "g2").inOrder()
    }

    @Test
    fun addPhones_withNoValues_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQuery("com.example.voip") { addPhones() }
        }
    }

    @Test
    fun addEmails_withNoValues_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQuery("com.example.voip") { addEmails() }
        }
    }

    @Test
    fun addLookupKeys_withNoValues_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQuery("com.example.voip") { addLookupKeys() }
        }
    }

    @Test
    fun addGroupLookupKeys_withNoValues_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQuery("com.example.voip") { addGroupLookupKeys() }
        }
    }

    @Test
    fun personQuery_withoutCriteria_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQuery("com.example.voip") {}
        }
    }

    @Test
    fun addPhones_withCollection_setsPhones() {
        val query =
            personQuery("com.example.voip") {
                addPhones(listOf("+16505550100", "+16505550101"))
            }

        assertThat(query.phones).containsExactly("+16505550100", "+16505550101").inOrder()
    }

    @Test
    fun addEmails_withCollection_setsEmails() {
        val query =
            personQuery("com.example.voip") {
                addEmails(listOf("alice@example.com"))
            }

        assertThat(query.emails).containsExactly("alice@example.com")
    }

    @Test
    fun addLookupKeys_withCollection_setsLookupKeys() {
        val query =
            personQuery("com.example.voip") {
                addLookupKeys(listOf("p1"))
            }

        assertThat(query.lookupKeys).containsExactly("p1")
    }

    @Test
    fun addGroupLookupKeys_withCollection_setsGroupLookupKeys() {
        val query =
            personQuery("com.example.voip") {
                addGroupLookupKeys(listOf("g1"))
            }

        assertThat(query.groupLookupKeys).containsExactly("g1")
    }

    @Test
    fun personQueries_withVarargs_createsQueryForEachPackage() {
        val queries =
            personQueries("com.example.voip", "com.example.chat") {
                addPhones("+16505550100")
            }

        assertThat(queries).hasSize(2)
        assertThat(queries[0].directoryPackage).isEqualTo("com.example.voip")
        assertThat(queries[0].phones).containsExactly("+16505550100")
        assertThat(queries[1].directoryPackage).isEqualTo("com.example.chat")
        assertThat(queries[1].phones).containsExactly("+16505550100")
    }

    @Test
    fun personQueries_withIterable_createsQueryForEachPackage() {
        val queries =
            personQueries(listOf("com.example.voip", "com.example.chat")) {
                addEmails("alice@example.com")
            }

        assertThat(queries).hasSize(2)
        assertThat(queries[0].directoryPackage).isEqualTo("com.example.voip")
        assertThat(queries[0].emails).containsExactly("alice@example.com")
        assertThat(queries[1].directoryPackage).isEqualTo("com.example.chat")
        assertThat(queries[1].emails).containsExactly("alice@example.com")
    }

    @Test
    fun personQueries_withEmptyVarargs_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQueries { addPhones("+16505550100") }
        }
    }

    @Test
    fun personQueries_withEmptyIterable_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            personQueries(emptyList()) { addPhones("+16505550100") }
        }
    }
}
