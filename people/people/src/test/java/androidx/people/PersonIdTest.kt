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
class PersonIdTest {

    @Test
    fun properties_whenConstructed_exposeGivenValues() {
        val id = PersonId(lookupKey = "user_42", directoryPackage = "com.example.voip")

        assertThat(id.lookupKey).isEqualTo("user_42")
        assertThat(id.directoryPackage).isEqualTo("com.example.voip")
    }

    @Test
    fun equals_withSameValues_returnsTrue() {
        val id = PersonId("user_42", "com.example.voip")
        val same = PersonId("user_42", "com.example.voip")

        assertThat(same).isEqualTo(id)
        assertThat(same.hashCode()).isEqualTo(id.hashCode())
    }

    @Test
    fun equals_withDifferentValues_returnsFalse() {
        val id = PersonId("user_42", "com.example.voip")
        val other = PersonId("user_99", "com.example.voip")

        assertThat(other).isNotEqualTo(id)
    }
}
