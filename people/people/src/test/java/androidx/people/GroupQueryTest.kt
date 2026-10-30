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

/** Tests for [groupQuery], [GroupQueryScope] and [GroupCriteriaScope]. */
class GroupQueryTest {

    @Test
    fun groupQuery_singleDirectoryForms_areEqual() {
        val a = groupQuery(CHAT) { addLookupKeys("g1") }
        val b = groupQuery { directory(CHAT) { addLookupKeys("g1") } }

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun groupQuery_samePackageTwice_mergesCriteria() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1") }
            directory(VOIP) { addLookupKeys("v1") }
            directory(CHAT) { addLookupKeys("g1", "g2") }
        }

        assertThat(query)
            .isEqualTo(
                groupQuery {
                    directory(CHAT) { addLookupKeys("g1", "g2") }
                    directory(VOIP) { addLookupKeys("v1") }
                }
            )
    }

    @Test
    fun groupQuery_differentDirectories_areNotEqual() {
        assertThat(groupQuery(CHAT) { addLookupKeys("g1") })
            .isNotEqualTo(groupQuery(VOIP) { addLookupKeys("g1") })
    }

    @Test
    fun groupQuery_withoutDirectories_throws() {
        assertThrows(IllegalArgumentException::class.java) { groupQuery {} }
    }

    @Test
    fun groupQuery_withEmptyPackageName_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            groupQuery("") { addLookupKeys("g1") }
        }
    }

    @Test
    fun groupCriteriaScope_accumulatesLookupKeysWithoutDuplicates() {
        val query =
            groupQuery(CHAT) {
                addLookupKeys("g1", "g2")
                addLookupKeys(listOf("g3", "g1"))
            }

        assertThat(query).isEqualTo(groupQuery(CHAT) { addLookupKeys("g1", "g2", "g3") })
        assertThat(query.criteriaFor(CHAT)).contains(Criterion.GroupLookupKey("g3"))
    }

    @Test
    fun contains_isScopedToTheDirectoryAndCriterionType() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        assertThat(query.criteriaFor(CHAT)).contains(Criterion.GroupLookupKey("g1"))
        assertThat(query.criteriaFor(VOIP)).contains(Criterion.GroupLookupKey("v1"))
        assertThat(query.criteriaFor(VOIP)).doesNotContain(Criterion.GroupLookupKey("g1"))
        assertThat(query.criteriaFor(CHAT)).doesNotContain(Criterion.PersonLookupKey("g1"))
        assertThat(query.criteriaFor("com.example.sms"))
            .doesNotContain(Criterion.GroupLookupKey("g1"))
    }

    @Test
    fun isEmpty_isFalseForBuiltQueries_andTrueForAnEmptyPartition() {
        assertThat(groupQuery(CHAT) { addLookupKeys("g1") }.isEmpty()).isFalse()
        // The DSL rejects empty queries; results report empty partitions, built like this.
        assertThat(groupQueryOf(emptyMap()).isEmpty()).isTrue()
        assertThat(groupQueryOf(mapOf(CHAT to emptySet())).isEmpty()).isTrue()
    }

    @Test
    fun groupCriteriaScope_withoutCriteria_throws() {
        assertThrows(IllegalArgumentException::class.java) { groupQuery(CHAT) {} }
        assertThrows(IllegalArgumentException::class.java) { groupQuery(CHAT) { addLookupKeys() } }
        assertThrows(IllegalArgumentException::class.java) {
            groupQuery(CHAT) { addLookupKeys(emptyList()) }
        }
    }

    @Test
    fun plus_unionsCriteriaPerDirectory() {
        val a = groupQuery(CHAT) { addLookupKeys("g1") }
        val b = groupQuery {
            directory(CHAT) { addLookupKeys("g1", "g2") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        assertThat(a + b)
            .isEqualTo(
                groupQuery {
                    directory(CHAT) { addLookupKeys("g1", "g2") }
                    directory(VOIP) { addLookupKeys("v1") }
                }
            )
    }

    @Test
    fun minus_removesCriteriaPerDirectoryAndDropsEmptiedDirectories() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1", "g2") }
            directory(VOIP) { addLookupKeys("v1") }
        }
        val other = groupQuery {
            directory(CHAT) { addLookupKeys("g1") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        assertThat(query - other).isEqualTo(groupQuery(CHAT) { addLookupKeys("g2") })
        assertThat((query - query).isEmpty()).isTrue()
    }

    @Test
    fun directoryPackagesAndCriteriaFor_exposeTheQuery() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1", "g2") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        assertThat(query.directoryPackages).containsExactly(CHAT, VOIP).inOrder()
        assertThat(query.criteriaFor(CHAT))
            .containsExactly(Criterion.GroupLookupKey("g1"), Criterion.GroupLookupKey("g2"))
            .inOrder()
        assertThat(query.criteriaFor("com.example.other")).isEmpty()
    }

    private companion object {
        const val VOIP = "com.example.voip"
        const val CHAT = "com.example.chat"
    }
}
