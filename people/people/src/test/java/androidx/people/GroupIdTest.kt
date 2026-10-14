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
class GroupIdTest {

    @Test
    fun properties_whenConstructed_exposeGivenValues() {
        val id = GroupId(lookupKey = "group_101", directoryPackage = "com.example.chat")

        assertThat(id.lookupKey).isEqualTo("group_101")
        assertThat(id.directoryPackage).isEqualTo("com.example.chat")
    }

    @Test
    fun equals_withSameValues_returnsTrue() {
        val id = GroupId("group_101", "com.example.chat")
        val same = GroupId("group_101", "com.example.chat")

        assertThat(same).isEqualTo(id)
        assertThat(same.hashCode()).isEqualTo(id.hashCode())
    }

    @Test
    fun equals_withDifferentValues_returnsFalse() {
        val id = GroupId("group_101", "com.example.chat")
        val other = GroupId("group_999", "com.example.chat")

        assertThat(other).isNotEqualTo(id)
    }
}
