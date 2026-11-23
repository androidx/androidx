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

import androidx.people.internal.failedGroupQueryResultOf
import androidx.people.internal.groupQueryResultOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GroupQueryResultTest {

    private val g1 = Criterion.GroupLookupKey("g1")
    private val g2 = Criterion.GroupLookupKey("g2")
    private val g3 = Criterion.GroupLookupKey("g3")
    private val family = Group(GroupId(CHAT, "g1"), "Family")
    private val friends = Group(GroupId(CHAT, "g2"), "Friends")

    @Test
    fun partition_splitsTheQueryIntoHandledAndUnhandled() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1", "g2", "g3") }
            directory(VOIP) { addLookupKeys("v1") }
        }
        val unhandled = groupQuery {
            directory(CHAT) { addLookupKeys("g2") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        val result =
            groupQueryResultOf(
                query,
                matches = setOf(GroupMatch(family, setOf(g1))),
                unhandledQuery = unhandled,
            )

        assertThat(result.matches).containsExactly(GroupMatch(family, setOf(g1)))
        assertThat(result.handledQuery).isEqualTo(groupQuery(CHAT) { addLookupKeys("g1", "g3") })
        assertThat(result.unhandledQuery).isEqualTo(unhandled)
    }

    @Test
    fun partition_allCriteriaHandledWhenUnhandledIsEmpty() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        val result =
            groupQueryResultOf(
                query,
                matches = setOf(GroupMatch(family, setOf(g1))),
                unhandledQuery = NO_CRITERIA,
            )

        assertThat(result.handledQuery).isEqualTo(query)
        assertThat(result.unhandledQuery.isEmpty()).isTrue()
    }

    @Test
    fun getResultFor_returnsHandledOrUnhandledScopedToDirectoryAndCriterion() {
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1", "g2", "g3") }
            directory(VOIP) { addLookupKeys("g1", "g2") }
        }
        val voipG1 = Group(GroupId(VOIP, "g1"), "VoIP g1")

        val result =
            groupQueryResultOf(
                query,
                matches =
                    setOf(
                        GroupMatch(family, setOf(g1)),
                        GroupMatch(friends, setOf(g1, g2)),
                        GroupMatch(voipG1, setOf(g1)),
                    ),
                unhandledQuery = groupQuery(CHAT) { addLookupKeys("g3") },
            )

        assertThat(result.getResultFor(CHAT, g1))
            .isEqualTo(
                MatchResult.Handled(
                    setOf(GroupMatch(family, setOf(g1)), GroupMatch(friends, setOf(g1, g2)))
                )
            )
        assertThat(result.getResultFor(VOIP, g1))
            .isEqualTo(MatchResult.Handled(setOf(GroupMatch(voipG1, setOf(g1)))))
        // Evaluated with no match -> Handled(emptySet()).
        assertThat(result.getResultFor(VOIP, g2))
            .isEqualTo(MatchResult.Handled(emptySet<GroupMatch>()))
        // In unhandledQuery -> Unhandled.
        assertThat(result.getResultFor(CHAT, g3)).isEqualTo(MatchResult.Unhandled)
        // Not part of the query at all -> Unhandled.
        assertThat(result.getResultFor(SMS, g1)).isEqualTo(MatchResult.Unhandled)
        assertThat(result.getResultFor(VOIP, g3)).isEqualTo(MatchResult.Unhandled)
    }

    @Test
    fun failedResult_reportsTheWholeQueryAsUnhandled() {
        val query = groupQuery(CHAT) { addLookupKeys("g1", "g2") }

        val result = failedGroupQueryResultOf(query)

        assertThat(result.matches).isEmpty()
        assertThat(result.handledQuery.isEmpty()).isTrue()
        assertThat(result.unhandledQuery).isEqualTo(query)
        assertThat(result.getResultFor(CHAT, g1)).isEqualTo(MatchResult.Unhandled)
    }

    @Test
    fun equality_isByMatchesAndPartition() {
        val query = groupQuery(CHAT) { addLookupKeys("g1", "g2") }
        val matches = setOf(GroupMatch(family, setOf(g1)))
        val a = groupQueryResultOf(query, matches, NO_CRITERIA)
        val b = groupQueryResultOf(query, matches.toSet(), NO_CRITERIA)

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a)
            .isNotEqualTo(
                groupQueryResultOf(query, matches, groupQuery(CHAT) { addLookupKeys("g2") })
            )
    }

    private companion object {
        const val CHAT = "com.example.chat"
        const val VOIP = "com.example.voip"
        const val SMS = "com.example.sms"
        val NO_CRITERIA = groupQueryOf(emptyMap())
    }
}
