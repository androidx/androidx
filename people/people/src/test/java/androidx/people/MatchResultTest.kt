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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MatchResultTest {

    @Test
    fun handled_allowsEmptyMatches() {
        val result = MatchResult.Handled(emptySet<GroupMatch>())

        assertThat(result.matches).isEmpty()
        assertThat(result).isEqualTo(MatchResult.Handled(emptySet<GroupMatch>()))
        assertThat(result).isNotEqualTo(MatchResult.Unhandled)
        assertThat(result.toString()).isEqualTo("MatchResult.Handled(matches=[])")
    }

    @Test
    fun handled_defensivelyCopiesMatches() {
        val groupMatch = GroupMatch(Group(GroupId("com.example.chat", "g1"), "Family"), setOf(G1))
        val mutable = mutableSetOf(groupMatch)

        val result = MatchResult.Handled(mutable)
        mutable.clear()

        assertThat(result.matches).containsExactly(groupMatch)
    }

    @Test
    fun handled_equalityHashCodeAndToString() {
        val match1 = GroupMatch(Group(GroupId("com.example.chat", "g1"), "Family"), setOf(G1))
        val match2 = GroupMatch(Group(GroupId("com.example.chat", "g2"), "Friends"), setOf(G1))
        val a = MatchResult.Handled(setOf(match1))
        val b = MatchResult.Handled(setOf(match1))
        val c = MatchResult.Handled(setOf(match2))

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a).isNotEqualTo(c)
        assertThat(a).isNotEqualTo(MatchResult.Handled(emptySet<GroupMatch>()))
        assertThat(a).isNotEqualTo(MatchResult.Unhandled)
        assertThat(a.toString()).isEqualTo("MatchResult.Handled(matches=${setOf(match1)})")
    }

    @Test
    fun unhandled_isSingletonAndFormatsCleanly() {
        val unhandled: MatchResult<PersonMatch> = MatchResult.Unhandled

        assertThat(unhandled).isSameInstanceAs(MatchResult.Unhandled)
        assertThat(unhandled.toString()).isEqualTo("MatchResult.Unhandled")
    }

    private companion object {
        val G1 = Criterion.GroupLookupKey("g1")
    }
}
