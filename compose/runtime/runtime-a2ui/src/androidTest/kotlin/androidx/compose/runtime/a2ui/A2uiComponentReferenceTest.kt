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

package androidx.compose.runtime.a2ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiComponentReferenceTest {

    @Test
    fun equalsAndHashCode() {
        // Group 1: same ID, null path
        val group1a = A2uiComponentReference("id1")
        val group1b = A2uiComponentReference("id1", null)

        // Group 2: different ID, null path
        val group2 = A2uiComponentReference("id2")

        // Group 3: same ID as Group 1, but with a specific path
        val group3a = A2uiComponentReference("id1", "/path/a")
        val group3b = A2uiComponentReference("id1", "/path/a")

        // Group 4: same ID as Group 1, but with a different path
        val group4 = A2uiComponentReference("id1", "/path/b")

        // Group 5: same path as Group 3, but different ID
        val group5 = A2uiComponentReference("id2", "/path/a")

        // Test equality and hashCode within groups
        assertThat(group1a).isEqualTo(group1b)
        assertThat(group1b).isEqualTo(group1a)
        assertThat(group1a.hashCode()).isEqualTo(group1b.hashCode())

        assertThat(group3a).isEqualTo(group3b)
        assertThat(group3b).isEqualTo(group3a)
        assertThat(group3a.hashCode()).isEqualTo(group3b.hashCode())

        // Test inequality across different groups
        val distinctGroups = listOf(group1a, group2, group3a, group4, group5)

        for (i in distinctGroups.indices) {
            for (j in i + 1 until distinctGroups.size) {
                val item1 = distinctGroups[i]
                val item2 = distinctGroups[j]

                // Assert inequality in both directions
                assertThat(item1).isNotEqualTo(item2)
                assertThat(item2).isNotEqualTo(item1)
            }
        }

        assertThat(group1a).isEqualTo(group1a)
        assertThat(group1a).isNotEqualTo(null)
        assertThat(group1a).isNotEqualTo(Any())
    }
}
