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
class DirectoryInfoTest {

    @Test
    fun properties_whenConstructed_exposeGivenValues() {
        val directory = DirectoryInfo("com.example.voip", "com.example.voip.people")

        assertThat(directory.packageName).isEqualTo("com.example.voip")
        assertThat(directory.authority).isEqualTo("com.example.voip.people")
    }

    @Test
    fun equals_withSameValues_returnsTrue() {
        val directory = DirectoryInfo("com.example.voip", "com.example.voip.people")
        val same = DirectoryInfo("com.example.voip", "com.example.voip.people")

        assertThat(same).isEqualTo(directory)
        assertThat(same.hashCode()).isEqualTo(directory.hashCode())
    }

    @Test
    fun equals_withDifferentValues_returnsFalse() {
        val directory = DirectoryInfo("com.example.voip", "com.example.voip.people")
        val other = DirectoryInfo("com.example.chat", "com.example.voip.people")

        assertThat(other).isNotEqualTo(directory)
    }
}
