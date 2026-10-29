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

/**
 * Tests for [personQuery], [PersonQueryScope], [PersonCriteriaScope] and
 * [SharedPersonCriteriaScope].
 */
class PersonQueryTest {

    @Test
    fun personQuery_singleDirectoryForms_areEqual() {
        val a = personQuery(VOIP) { addPhones(PHONE) }
        val b = personQuery { directory(VOIP) { addPhones(PHONE) } }
        val c = personQuery(listOf(VOIP)) { addPhones(PHONE) }

        assertThat(a).isEqualTo(b)
        assertThat(a).isEqualTo(c)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun personQuery_multipleDirectories_sharesCriteria() {
        val shared = personQuery(listOf(VOIP, CHAT)) { addPhones(PHONE) }

        assertThat(shared)
            .isEqualTo(
                personQuery {
                    directory(VOIP) { addPhones(PHONE) }
                    directory(CHAT) { addPhones(PHONE) }
                }
            )
    }

    @Test
    fun personQuery_directoryAndDirectoriesForSamePackage_mergeCriteria() {
        val query = personQuery {
            directory(VOIP) { addLookupKeys("user_1") }
            directories(VOIP, CHAT) { addPhones(PHONE) }
            directory(VOIP) { addPhones(PHONE, OTHER_PHONE) }
            directories(listOf(CHAT)) { addEmails(EMAIL) }
        }

        assertThat(query)
            .isEqualTo(
                personQuery {
                    directory(VOIP) {
                        addLookupKeys("user_1")
                        addPhones(PHONE, OTHER_PHONE)
                    }
                    directory(CHAT) {
                        addPhones(PHONE)
                        addEmails(EMAIL)
                    }
                }
            )
    }

    @Test
    fun personQuery_differentDirectories_areNotEqual() {
        assertThat(personQuery(VOIP) { addPhones(PHONE) })
            .isNotEqualTo(personQuery(CHAT) { addPhones(PHONE) })
    }

    @Test
    fun personQuery_withoutDirectories_throws() {
        assertThrows(IllegalArgumentException::class.java) { personQuery {} }
    }

    @Test
    fun personQuery_withEmptyPackageList_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            personQuery(emptyList()) { addPhones(PHONE) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery { directories { addPhones(PHONE) } }
        }
    }

    @Test
    fun personQuery_withEmptyPackageName_throws() {
        assertThrows(IllegalArgumentException::class.java) { personQuery("") { addPhones(PHONE) } }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery(listOf(VOIP, "")) { addPhones(PHONE) }
        }
    }

    @Test
    fun personCriteriaScope_addsEveryCriterionType() {
        val query =
            personQuery(VOIP) {
                addPhones(PHONE, OTHER_PHONE)
                addEmails(EMAIL)
                addLookupKeys("user_1")
                addGroupLookupKeys("group_1")
            }

        assertThat(query.criteriaFor(VOIP)).contains(Criterion.Phone(PHONE))
        assertThat(query.criteriaFor(VOIP)).contains(Criterion.Phone(OTHER_PHONE))
        assertThat(query.criteriaFor(VOIP)).contains(Criterion.Email(EMAIL))
        assertThat(query.criteriaFor(VOIP)).contains(Criterion.PersonLookupKey("user_1"))
        assertThat(query.criteriaFor(VOIP)).contains(Criterion.GroupLookupKey("group_1"))
    }

    @Test
    fun contains_isScopedToTheDirectoryAndCriterionType() {
        val query = personQuery {
            directory(VOIP) { addPhones(PHONE) }
            directory(CHAT) { addEmails(EMAIL) }
        }

        assertThat(query.criteriaFor(VOIP)).contains(Criterion.Phone(PHONE))
        assertThat(query.criteriaFor(CHAT)).contains(Criterion.Email(EMAIL))
        assertThat(query.criteriaFor(CHAT)).doesNotContain(Criterion.Phone(PHONE))
        assertThat(query.criteriaFor(VOIP)).doesNotContain(Criterion.Phone(OTHER_PHONE))
        // Same value, different criterion type.
        assertThat(query.criteriaFor(VOIP)).doesNotContain(Criterion.Email(PHONE))
        assertThat(query.criteriaFor("com.example.sms")).doesNotContain(Criterion.Phone(PHONE))
    }

    @Test
    fun isEmpty_isFalseForBuiltQueries_andTrueForAnEmptyPartition() {
        assertThat(personQuery(VOIP) { addPhones(PHONE) }.isEmpty()).isFalse()
        // The DSL rejects empty queries; results report empty partitions, built like this.
        assertThat(personQueryOf(emptyMap()).isEmpty()).isTrue()
        assertThat(personQueryOf(mapOf(VOIP to emptySet())).isEmpty()).isTrue()
        assertThat(personQueryOf(mapOf(VOIP to emptySet()))).isEqualTo(personQueryOf(emptyMap()))
    }

    @Test
    fun personCriteriaScope_accumulatesRepeatedCallsAndDropsDuplicates() {
        val query =
            personQuery(VOIP) {
                addPhones(PHONE)
                addPhones(listOf(OTHER_PHONE, PHONE))
                addLookupKeys("p1")
                addLookupKeys(listOf("p1"))
            }

        assertThat(query)
            .isEqualTo(
                personQuery(VOIP) {
                    addPhones(PHONE, OTHER_PHONE)
                    addLookupKeys("p1")
                }
            )
    }

    @Test
    fun personCriteriaScope_iterableAndVarargOverloads_areEquivalent() {
        assertThat(
                personQuery(VOIP) {
                    addPhones(listOf(PHONE))
                    addEmails(listOf(EMAIL))
                    addLookupKeys(listOf("p1"))
                    addGroupLookupKeys(listOf("g1"))
                }
            )
            .isEqualTo(
                personQuery(VOIP) {
                    addPhones(PHONE)
                    addEmails(EMAIL)
                    addLookupKeys("p1")
                    addGroupLookupKeys("g1")
                }
            )
    }

    @Test
    fun personCriteriaScope_withoutCriteria_throws() {
        assertThrows(IllegalArgumentException::class.java) { personQuery(VOIP) {} }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery { directory(VOIP) {} }
        }
    }

    @Test
    fun personCriteriaScope_addMethodsWithNoValues_throw() {
        assertThrows(IllegalArgumentException::class.java) { personQuery(VOIP) { addPhones() } }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery(VOIP) { addEmails(emptyList()) }
        }
        assertThrows(IllegalArgumentException::class.java) { personQuery(VOIP) { addLookupKeys() } }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery(VOIP) { addGroupLookupKeys(emptyList()) }
        }
    }

    @Test
    fun sharedPersonCriteriaScope_accumulatesCriteria() {
        assertThat(
                personQuery(listOf(VOIP)) {
                    addPhones(PHONE)
                    addEmails(listOf(EMAIL))
                }
            )
            .isEqualTo(
                personQuery(VOIP) {
                    addPhones(PHONE)
                    addEmails(EMAIL)
                }
            )
    }

    @Test
    fun sharedPersonCriteriaScope_validatesCriteria() {
        assertThrows(IllegalArgumentException::class.java) { personQuery(listOf(VOIP)) {} }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery(listOf(VOIP)) { addPhones() }
        }
        assertThrows(IllegalArgumentException::class.java) {
            personQuery(listOf(VOIP)) { addEmails(emptyList()) }
        }
    }

    @Test
    fun plus_unionsCriteriaPerDirectory() {
        val a = personQuery {
            directory(VOIP) { addPhones(PHONE) }
            directory(CHAT) { addEmails(EMAIL) }
        }
        val b = personQuery(VOIP) { addPhones(PHONE, OTHER_PHONE) }

        assertThat(a + b)
            .isEqualTo(
                personQuery {
                    directory(VOIP) { addPhones(PHONE, OTHER_PHONE) }
                    directory(CHAT) { addEmails(EMAIL) }
                }
            )
    }

    @Test
    fun minus_removesCriteriaPerDirectoryAndDropsEmptiedDirectories() {
        val query = personQuery {
            directory(VOIP) {
                addPhones(PHONE)
                addEmails(EMAIL)
            }
            directory(CHAT) { addPhones(PHONE) }
        }
        val other = personQuery {
            directory(VOIP) { addPhones(PHONE) }
            directory(CHAT) { addPhones(PHONE) }
        }

        assertThat(query - other).isEqualTo(personQuery(VOIP) { addEmails(EMAIL) })
        assertThat((query - query).isEmpty()).isTrue()
    }

    @Test
    fun minus_keepsTheOrderOfTheCriteria() {
        val query = personQuery(VOIP) { addPhones("+3", "+1", "+2") }

        val result = query - personQuery(VOIP) { addPhones("+1") }

        assertThat(result.criteriaFor(VOIP))
            .containsExactly(Criterion.Phone("+3"), Criterion.Phone("+2"))
            .inOrder()
    }

    @Test
    fun directoryPackagesAndCriteriaFor_exposeTheQuery() {
        val query = personQuery {
            directory(VOIP) { addPhones(PHONE, OTHER_PHONE) }
            directory(CHAT) { addEmails(EMAIL) }
        }

        assertThat(query.directoryPackages).containsExactly(VOIP, CHAT).inOrder()
        assertThat(query.criteriaFor(VOIP))
            .containsExactly(Criterion.Phone(PHONE), Criterion.Phone(OTHER_PHONE))
            .inOrder()
        assertThat(query.criteriaFor(CHAT)).containsExactly(Criterion.Email(EMAIL))
        assertThat(query.criteriaFor("com.example.other")).isEmpty()
    }

    private companion object {
        const val VOIP = "com.example.voip"
        const val CHAT = "com.example.chat"
        const val PHONE = "+16505550100"
        const val OTHER_PHONE = "+16505550101"
        const val EMAIL = "alice@example.com"
    }
}
