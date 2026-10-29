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
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GroupTest {

    @Test
    fun properties_whenProvided_exposeGivenValues() {
        val group =
            Group(
                id = GroupId("eng_team", "com.example.chat"),
                displayName = "Engineering Team",
            )

        assertThat(group.displayName).isEqualTo("Engineering Team")
    }

    @Test
    fun properties_whenOptionalPropertiesEmpty_exposeNullOrEmpty() {
        val group =
            Group(
                id = GroupId("eng_team", "com.example.chat"),
                displayName = null,
            )

        assertThat(group.displayName).isNull()
    }

    @Test
    fun groupMatch_exposesGroupAndCriteria_andComparesCriteria() {
        val group = Group(id = GroupId("eng_team", "com.example.chat"), displayName = null)
        val criteria = setOf<Criterion>(Criterion.GroupLookupKey("eng_team"))

        val match = GroupMatch(group, criteria)

        assertThat(match.group).isEqualTo(group)
        assertThat(match.criteria).isEqualTo(criteria)
        assertThat(match).isEqualTo(GroupMatch(group, criteria))
        assertThat(match.hashCode()).isEqualTo(GroupMatch(group, criteria).hashCode())
        assertThat(match)
            .isNotEqualTo(GroupMatch(group, setOf(Criterion.GroupLookupKey("other_team"))))
    }

    @Test
    fun groupMatch_withoutCriteria_throws() {
        val group = Group(id = GroupId("eng_team", "com.example.chat"), displayName = null)

        assertThrows(IllegalArgumentException::class.java) { GroupMatch(group, emptySet()) }
    }
}
